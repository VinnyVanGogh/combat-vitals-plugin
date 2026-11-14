package com.vinnyvangogh.combatvitals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * What an alert sounds like: decides between RuneLite's own notification
 * sound, a bundled clip, a voice line joined from parts, and the user's own
 * file, and builds the audio for the plugin to hand to RuneLite's
 * {@code AudioPlayer}. Deciding is pure ({@link #plan}); playing is the
 * plugin's.
 */
final class AlertSounds
{
	/** The user's folder, under {@code RuneLite.RUNELITE_DIR}. */
	static final String USER_FOLDER = "combat-vitals" + File.separator + "sounds";

	/** What to play for one alert. */
	static final class Plan
	{
		/** RuneLite's own sound, through {@code Notifier}: the default, and every fallback. */
		final boolean runelite;
		/** Class resource paths to play joined in order; empty unless bundled. */
		final List<String> resources;
		/** The user's file, or null. */
		final File file;
		/** "My sounds" was chosen but the file is not there. */
		final boolean missingFile;

		private Plan(boolean runelite, List<String> resources, File file, boolean missingFile)
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

		static Plan missing(File file)
		{
			return new Plan(true, Collections.emptyList(), file, true);
		}

		static Plan bundled(List<String> resources)
		{
			return new Plan(false, resources, null, false);
		}

		static Plan file(File file)
		{
			return new Plan(false, Collections.emptyList(), file, false);
		}
	}

	private final Set<ActivityAlerts.Type> hinted = EnumSet.noneOf(ActivityAlerts.Type.class);

	/**
	 * @param item           the item that last landed in the inventory, for voice lines; may be null
	 * @param resourceExists whether a class resource path is bundled
	 * @param userDir        the user's sounds folder
	 * @param fileExists     whether a file exists and can be read
	 */
	static Plan plan(AlertSound sound, ActivityAlerts.Alert alert, String item, VoiceLines voice,
		Predicate<String> resourceExists, File userDir, Predicate<File> fileExists)
	{
		if (sound == null || sound == AlertSound.RUNELITE)
		{
			return Plan.runelite();
		}
		if (sound == AlertSound.MY_SOUNDS)
		{
			final File file = new File(userDir, fileName(alert.type));
			return fileExists.test(file) ? Plan.file(file) : Plan.missing(file);
		}
		final List<String> resources = sound == AlertSound.VOICE
			? voice.compose(alert, item, resourceExists)
			: packClip(sound, resourceExists);
		return resources.isEmpty() ? Plan.runelite() : Plan.bundled(resources);
	}

	private static List<String> packClip(AlertSound sound, Predicate<String> resourceExists)
	{
		final String resource = sound.getResource();
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

	/** Volume 0 to 100 as the gain RuneLite's player takes, in dB, as {@code Notifier} converts it. */
	static float gain(int volume)
	{
		final int clamped = Math.max(1, Math.min(100, volume));
		return (float) (Math.log10(clamped / 100.0) * 20);
	}

	/**
	 * Joins several WAV clips into one WAV, so a voice line plays as one sound
	 * with no gaps from separate lines. Every part is converted to the first
	 * part's format when it differs.
	 *
	 * @param open opens a clip's bytes by its resource path; null if missing
	 */
	static byte[] join(List<String> parts, Function<String, InputStream> open)
		throws IOException, UnsupportedAudioFileException
	{
		AudioFormat format = null;
		final ByteArrayOutputStream pcm = new ByteArrayOutputStream();
		for (String part : parts)
		{
			final InputStream raw = open.apply(part);
			if (raw == null)
			{
				throw new IOException("missing clip " + part);
			}
			try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw)))
			{
				if (format == null)
				{
					format = in.getFormat();
				}
				copy(inFormat(in, format), pcm);
			}
		}
		if (format == null)
		{
			throw new IOException("no clips");
		}

		final byte[] data = pcm.toByteArray();
		final AudioInputStream joined = new AudioInputStream(new ByteArrayInputStream(data), format,
			data.length / format.getFrameSize());
		final ByteArrayOutputStream wav = new ByteArrayOutputStream();
		AudioSystem.write(joined, AudioFileFormat.Type.WAVE, wav);
		return wav.toByteArray();
	}

	/** The clip in the joined line's format; a clip that cannot be converted is a checked error, not a crash. */
	private static AudioInputStream inFormat(AudioInputStream in, AudioFormat format) throws UnsupportedAudioFileException
	{
		if (in.getFormat().matches(format))
		{
			return in;
		}
		if (!AudioSystem.isConversionSupported(format, in.getFormat()))
		{
			throw new UnsupportedAudioFileException("cannot join " + in.getFormat() + " to " + format);
		}
		return AudioSystem.getAudioInputStream(format, in);
	}

	private static void copy(InputStream in, ByteArrayOutputStream out) throws IOException
	{
		final byte[] buffer = new byte[8192];
		int read;
		while ((read = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, read);
		}
	}
}
