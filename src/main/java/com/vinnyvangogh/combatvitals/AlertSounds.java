package com.vinnyvangogh.combatvitals;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.google.common.base.Charsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What an alert sounds like: decides between RuneLite's own notification
 * sound, a bundled clip, a voice line joined from parts, and the user's own
 * file, and builds the audio for the plugin to hand to RuneLite's
 * {@code AudioPlayer}. Deciding is pure ({@link #plan}); playing is the
 * plugin's.
 */
final class AlertSounds
{
	/** The user's folder, under the plugin's data directory ({@code .runelite/plugin-data/combat-vitals}). */
	static final String USER_FOLDER = "sounds";

	/** What to play for one alert. */
	static final class Plan
	{
		/** RuneLite's own sound, through {@code Notifier}: the default, and every fallback. */
		final boolean runelite;
		/** Class resource paths to play joined in order; empty unless bundled. */
		final List<String> resources;
		/** The user's file's name in {@link #USER_FOLDER}, or null; the plugin opens it through the Filepath API. */
		final String file;
		/** "My sounds" was chosen but the file is not there. */
		final boolean missingFile;

		private Plan(boolean runelite, List<String> resources, String file, boolean missingFile)
		{
			this.runelite = runelite;
			this.resources = resources;
			this.file = file;
			this.missingFile = missingFile;
		}

		static Plan runelite()
		{
			return new Plan(true, Collections.emptyList(), null, false);
		}

		static Plan missing(String file)
		{
			return new Plan(true, Collections.emptyList(), file, true);
		}

		static Plan bundled(List<String> resources)
		{
			return new Plan(false, resources, null, false);
		}

		static Plan file(String file)
		{
			return new Plan(false, Collections.emptyList(), file, false);
		}
	}

	private final Set<ActivityAlerts.Type> hinted = EnumSet.noneOf(ActivityAlerts.Type.class);

	/**
	 * @param item           the item that last landed in the inventory, for voice lines; may be null
	 * @param resourceExists whether a class resource path is bundled
	 * @param fileExists     whether a file of that name is in the user's sounds folder and readable
	 */
	static Plan plan(AlertSound sound, ActivityAlerts.Alert alert, String item, VoiceLines voice,
		Predicate<String> resourceExists, Predicate<String> fileExists)
	{
		if (sound == null || sound == AlertSound.RUNELITE)
		{
			return Plan.runelite();
		}
		if (sound == AlertSound.MY_SOUNDS)
		{
			final String file = fileName(alert.type);
			return fileExists.test(file) ? Plan.file(file) : Plan.missing(file);
		}
		final List<String> resources = sound == AlertSound.VOICE
			? voice.compose(alert, item, resourceExists)
			: packClip(sound, resourceExists);
		return resources.isEmpty() ? Plan.runelite() : Plan.bundled(resources);
	}

	private static List<String> packClip(AlertSound sound, Predicate<String> resourceExists)
	{
		final String resource = sound.getClipPath();
		return resource != null && resourceExists.test(resource)
			? Collections.singletonList(resource)
			: Collections.emptyList();
	}

	/** "gathering-stopped.wav": the user's file for one alert. */
	static String fileName(ActivityAlerts.Type type)
	{
		return type.name().toLowerCase(Locale.ROOT).replace('_', '-') + ".wav";
	}

	/**
	 * Whether to tell the player that their file is missing: once per alert
	 * while the client runs, not on every alert. Client thread only.
	 */
	boolean shouldHint(Plan plan, ActivityAlerts.Type type)
	{
		return plan.missingFile && hinted.add(type);
	}

	static final String FOLLOW_KEY = "alertFollowGameVolume";
	static final String FOLLOW_RESET_KEY = "followGameVolumeReset";

	/**
	 * Follow game volume once shipped defaulting on, and RuneLite stored that default as if the
	 * player had ticked it, so the later default change never reached them. Clears it once; after
	 * the marker is set the player's own choice is never touched again.
	 */
	static void resetStaleFollowGameVolume(ConfigStore store)
	{
		if (Boolean.TRUE.equals(store.get(FOLLOW_RESET_KEY, Boolean.class)))
		{
			return;
		}
		if (Boolean.TRUE.equals(store.get(FOLLOW_KEY, Boolean.class)))
		{
			store.setRaw(FOLLOW_KEY, "false");
		}
		store.setRaw(FOLLOW_RESET_KEY, "true");
	}

	/** True when the game's sound effects are muted while following them, so every own alert is silent. */
	static boolean silentByGameVolume(boolean followGame, int gameVolume)
	{
		return effectiveVolume(100, followGame, gameVolume) <= 0;
	}

	/** The game's sound effects volume runs 0 to 127. */
	static final int GAME_VOLUME_MAX = 127;

	/**
	 * The volume to play at: the alert's own, scaled by the game's sound
	 * effects volume when following it. 0 means play nothing.
	 */
	static int effectiveVolume(int volume, boolean followGame, int gameVolume)
	{
		if (!followGame)
		{
			return volume;
		}
		final int game = Math.max(0, Math.min(GAME_VOLUME_MAX, gameVolume));
		return Math.round(volume * game / (float) GAME_VOLUME_MAX);
	}

	/** Volume 0 to 100 as the gain RuneLite's player takes, in dB, as {@code Notifier} converts it. */
	static float gain(int volume)
	{
		final int clamped = Math.max(1, Math.min(100, volume));
		return (float) (Math.log10(clamped / 100.0) * 20);
	}

	/**
	 * Whether a bundled sound can be played as planned: it is there, and a
	 * voice clip is also in {@link Wav#VOICE}, the one format {@link #join}
	 * takes. A voice clip in any other format counts as missing, so the line
	 * falls back to RuneLite's sound instead of failing when it plays. Reads
	 * a voice clip's header only.
	 *
	 * @param open opens a clip's bytes by its resource path; null if missing
	 */
	static Predicate<String> playable(Function<String, InputStream> open)
	{
		return path ->
		{
			try (InputStream in = open.apply(path))
			{
				if (in == null)
				{
					return false;
				}
				return !path.startsWith(VoiceLines.FOLDER) || Wav.VOICE.equals(Wav.read(in, false).format);
			}
			catch (IOException e)
			{
				return false;
			}
		};
	}

	/**
	 * Joins several WAV clips into one WAV, so a voice line plays as one sound
	 * with no gaps from separate lines. Every part must be in {@link Wav#VOICE};
	 * nothing is converted, and a part in any other format is refused.
	 *
	 * @param open opens a clip's bytes by its resource path; null if missing
	 */
	static byte[] join(List<String> parts, Function<String, InputStream> open) throws IOException
	{
		if (parts.isEmpty())
		{
			throw new IOException("no clips");
		}
		final ByteArrayOutputStream pcm = new ByteArrayOutputStream();
		for (String part : parts)
		{
			try (InputStream in = open.apply(part))
			{
				if (in == null)
				{
					throw new IOException("missing clip " + part);
				}
				final Wav wav = Wav.read(in, true);
				if (!Wav.VOICE.equals(wav.format))
				{
					throw new IOException("cannot join " + part + ": " + wav.format + ", not " + Wav.VOICE);
				}
				pcm.write(wav.data);
			}
		}
		return Wav.write(Wav.VOICE, pcm.toByteArray());
	}

	/**
	 * A WAV file read and written by hand, so the plugin never touches the
	 * Java sound API: the Plugin Hub requires RuneLite's
	 * {@code AudioPlayer} for all audio. Only plain PCM is understood.
	 */
	static final class Wav
	{
		/**
		 * The voice clips' format, as tools/voice/generate_voice.py writes
		 * them: 16000 Hz, mono, 16-bit PCM.
		 */
		static final Format VOICE = new Format(1, 16000, 16);

		private static final int PCM = 1;

		/** A PCM format: channels, sample rate and bits per sample. */
		static final class Format
		{
			final int channels;
			final int sampleRate;
			final int bits;

			Format(int channels, int sampleRate, int bits)
			{
				this.channels = channels;
				this.sampleRate = sampleRate;
				this.bits = bits;
			}

			int frameSize()
			{
				return channels * ((bits + 7) / 8);
			}

			@Override
			public boolean equals(Object o)
			{
				if (!(o instanceof Format))
				{
					return false;
				}
				final Format f = (Format) o;
				return channels == f.channels && sampleRate == f.sampleRate && bits == f.bits;
			}

			@Override
			public int hashCode()
			{
				return (channels * 31 + sampleRate) * 31 + bits;
			}

			@Override
			public String toString()
			{
				return sampleRate + " Hz " + channels + " ch " + bits + "-bit PCM";
			}
		}

		final Format format;
		/** The PCM samples; empty when only the header was read. */
		final byte[] data;

		private Wav(Format format, byte[] data)
		{
			this.format = format;
			this.data = data;
		}

		/**
		 * Reads a RIFF/WAVE file's format and, when {@code withData}, its
		 * samples. Chunks other than "fmt " and "data" are skipped.
		 *
		 * @throws IOException when it is not a PCM WAV file
		 */
		static Wav read(InputStream raw, boolean withData) throws IOException
		{
			final DataInputStream in = new DataInputStream(raw);
			if (!"RIFF".equals(tag(in)))
			{
				throw new IOException("not a RIFF file");
			}
			in.readInt();
			if (!"WAVE".equals(tag(in)))
			{
				throw new IOException("not a WAVE file");
			}
			Format format = null;
			while (true)
			{
				final String id = tag(in);
				final long size = Integer.toUnsignedLong(Integer.reverseBytes(in.readInt()));
				if ("fmt ".equals(id))
				{
					format = readFormat(in, size);
				}
				else if ("data".equals(id))
				{
					if (format == null)
					{
						throw new IOException("data before fmt");
					}
					return new Wav(format, withData ? readData(in, size, format) : new byte[0]);
				}
				else
				{
					skip(in, size + (size & 1));
				}
			}
		}

		private static Format readFormat(DataInputStream in, long size) throws IOException
		{
			if (size < 16)
			{
				throw new IOException("short fmt chunk");
			}
			final int encoding = Short.toUnsignedInt(Short.reverseBytes(in.readShort()));
			final int channels = Short.toUnsignedInt(Short.reverseBytes(in.readShort()));
			final int rate = Integer.reverseBytes(in.readInt());
			in.readInt(); // byte rate
			in.readShort(); // block align
			final int bits = Short.toUnsignedInt(Short.reverseBytes(in.readShort()));
			skip(in, size - 16 + (size & 1));
			if (encoding != PCM)
			{
				throw new IOException("not PCM: encoding " + encoding);
			}
			return new Format(channels, rate, bits);
		}

		private static byte[] readData(DataInputStream in, long size, Format format) throws IOException
		{
			if (size > Integer.MAX_VALUE)
			{
				throw new IOException("data chunk too large");
			}
			final byte[] data = new byte[(int) size];
			in.readFully(data);
			// Whole frames only, so the joined line never shifts a sample by a byte.
			final int frame = Math.max(1, format.frameSize());
			return data.length % frame == 0 ? data : Arrays.copyOf(data, data.length - data.length % frame);
		}

		/** A canonical 44-byte-header PCM WAV of {@code pcm}. */
		static byte[] write(Format format, byte[] pcm)
		{
			final ByteBuffer out = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
			out.put(ascii("RIFF")).putInt(36 + pcm.length).put(ascii("WAVE"));
			out.put(ascii("fmt ")).putInt(16)
				.putShort((short) PCM)
				.putShort((short) format.channels)
				.putInt(format.sampleRate)
				.putInt(format.sampleRate * format.frameSize())
				.putShort((short) format.frameSize())
				.putShort((short) format.bits);
			out.put(ascii("data")).putInt(pcm.length).put(pcm);
			return out.array();
		}

		private static String tag(DataInputStream in) throws IOException
		{
			final byte[] id = new byte[4];
			in.readFully(id);
			return new String(id, Charsets.US_ASCII);
		}

		private static byte[] ascii(String s)
		{
			return s.getBytes(Charsets.US_ASCII);
		}

		private static void skip(DataInputStream in, long n) throws IOException
		{
			for (long left = n; left > 0; left--)
			{
				if (in.read() < 0)
				{
					throw new EOFException("truncated chunk");
				}
			}
		}
	}
}
