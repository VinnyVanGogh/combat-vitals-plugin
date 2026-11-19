package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * Fallbacks first: a missing file, a missing pack clip and an unrendered
 * voice line all play RuneLite's sound, and the missing file is mentioned
 * once. Then the bundled packs load from the jar, and joined clips keep
 * every frame.
 */
public class AlertSoundsTest
{
	@Test
	public void gameVolumeMuteSilencesOwnSounds()
	{
		assertEquals(0, AlertSounds.effectiveVolume(40, true, 0));
		assertEquals(0, AlertSounds.effectiveVolume(40, true, -5));
	}

	@Test
	public void gameVolumeScalesAndClamps()
	{
		assertEquals(40, AlertSounds.effectiveVolume(40, true, 127));
		assertEquals(40, AlertSounds.effectiveVolume(40, true, 500));
		assertEquals(20, AlertSounds.effectiveVolume(40, true, 64));
	}

	@Test
	public void notFollowingKeepsOwnVolume()
	{
		assertEquals(40, AlertSounds.effectiveVolume(40, false, 0));
	}

	private static final Predicate<String> BUNDLED = p -> AlertSounds.class.getResource(p) != null;
	/** tools/voice/generate_voice.py: RATE = 16000, setnchannels(1), setsampwidth(2); little-endian signed. */
	private static final AudioFormat VOICE = new AudioFormat(16000f, 16, 1, true, false);
	private static final ActivityAlerts.Alert STOPPED =
		new ActivityAlerts.Alert(ActivityAlerts.Type.GATHERING_STOPPED, Skill.WOODCUTTING);

	private final VoiceLines voice = VoiceLines.load();

	@Test
	public void missingUserFileFallsBackToRuneLiteWithOneHint()
	{
		final AlertSounds sounds = new AlertSounds();
		final AlertSounds.Plan plan = plan(AlertSound.MY_SOUNDS, f -> false);
		assertTrue(plan.runelite);
		assertTrue(plan.missingFile);
		assertEquals("gathering-stopped.wav", plan.file);

		assertTrue("first time: hint", sounds.shouldHint(plan, STOPPED.type));
		assertFalse("then quiet", sounds.shouldHint(plan, STOPPED.type));
		assertFalse(sounds.shouldHint(plan, STOPPED.type));
		assertTrue("another alert's own hint", sounds.shouldHint(plan, ActivityAlerts.Type.INVENTORY_FULL));
	}

	@Test
	public void noHintWhenNothingIsMissing()
	{
		final AlertSounds sounds = new AlertSounds();
		assertFalse(sounds.shouldHint(plan(AlertSound.RUNELITE, f -> false), STOPPED.type));
		assertFalse(sounds.shouldHint(plan(AlertSound.MY_SOUNDS, f -> true), STOPPED.type));
	}

	@Test
	public void userFileIsPlayedWhenThere()
	{
		final AlertSounds.Plan plan = plan(AlertSound.MY_SOUNDS, f -> true);
		assertFalse(plan.runelite);
		assertEquals("gathering-stopped.wav", plan.file);
	}

	@Test
	public void userFileNamesStayInsideTheFolder()
	{
		for (ActivityAlerts.Type type : ActivityAlerts.Type.values())
		{
			final String name = AlertSounds.fileName(type);
			assertTrue(name, name.matches("[a-z-]+\\.wav"));
		}
	}

	@Test
	public void missingPackClipFallsBackToRuneLite()
	{
		final AlertSounds.Plan plan = AlertSounds.plan(AlertSound.SOFT_CHIME, STOPPED, null, voice,
			p -> false, f -> true);
		assertTrue(plan.runelite);
		assertFalse(plan.missingFile);
	}

	@Test
	public void unrenderedVoiceFallsBackToRuneLite()
	{
		final AlertSounds.Plan plan = AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			p -> false, f -> true);
		assertTrue(plan.runelite);
	}

	@Test
	public void runeLiteIsTheDefaultAndPlaysNothingOfOurOwn()
	{
		final AlertSounds.Plan plan = plan(AlertSound.RUNELITE, f -> true);
		assertTrue(plan.runelite);
		assertTrue(plan.resources.isEmpty());
		assertNull(plan.file);
	}

	@Test
	public void everyPackLoadsFromTheJar() throws Exception
	{
		for (AlertSound sound : AlertSound.values())
		{
			if (sound.getClipPath() == null)
			{
				continue;
			}
			try (InputStream in = AlertSounds.class.getResourceAsStream(sound.getClipPath()))
			{
				assertNotNull(sound + " is not bundled", in);
				try (AudioInputStream audio = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in)))
				{
					assertTrue(sound + " is empty", audio.getFrameLength() > 0);
				}
			}

			final AlertSounds.Plan plan = AlertSounds.plan(sound, STOPPED, null, voice, BUNDLED, f -> false);
			assertFalse(plan.runelite);
			assertEquals(Collections.singletonList(sound.getClipPath()), plan.resources);
		}
	}

	@Test
	public void voiceLinePlansTheComposedClips()
	{
		final AlertSounds.Plan plan = AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			p -> true, f -> false);
		assertFalse(plan.runelite);
		assertEquals(Arrays.asList("voice/stopped.wav", "voice/verb-chopping.wav", "voice/res-yew-logs.wav"),
			plan.resources);
	}

	/** The joined line is one valid WAV, read back by javax.sound itself, with every sample in order. */
	@Test
	public void joinedClipsPlayAsOneValidWav() throws Exception
	{
		final Map<String, byte[]> clips = new HashMap<>();
		clips.put("a", wav(VOICE, 1600));
		clips.put("b", wav(VOICE, 800));
		clips.put("c", wav(VOICE, 400));

		final byte[] joined = AlertSounds.join(Arrays.asList("a", "b", "c"),
			p -> new ByteArrayInputStream(clips.get(p)));
		try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(joined)))
		{
			assertEquals(2800, audio.getFrameLength());
			assertTrue(audio.getFormat().matches(VOICE));
			final ByteArrayOutputStream expected = new ByteArrayOutputStream();
			expected.write(pcm(VOICE, 1600));
			expected.write(pcm(VOICE, 800));
			expected.write(pcm(VOICE, 400));
			assertArrayEquals(expected.toByteArray(), readAll(audio));
		}
	}

	/** Chunks other than fmt and data (a LIST tag from an editor) are skipped, an odd one with its pad byte. */
	@Test
	public void otherChunksAreSkipped() throws Exception
	{
		final byte[] plain = wav(VOICE, 10);
		final byte[] extra = {'L', 'I', 'S', 'T', 3, 0, 0, 0, 'a', 'b', 'c', 0};
		final ByteArrayOutputStream tagged = new ByteArrayOutputStream();
		tagged.write(plain, 0, 36); // RIFF header and fmt chunk
		tagged.write(extra);
		tagged.write(plain, 36, plain.length - 36);

		final byte[] joined = AlertSounds.join(Collections.singletonList("a"),
			p -> new ByteArrayInputStream(tagged.toByteArray()));
		assertArrayEquals(plain, joined);
	}

	/** A part in another format is refused as a checked error the plugin logs, never converted or crashed on. */
	@Test(expected = java.io.IOException.class)
	public void joiningAClipInAnotherFormatIsRefused() throws Exception
	{
		final Map<String, byte[]> clips = new HashMap<>();
		clips.put("a", wav(VOICE, 160));
		clips.put("b", wav(new AudioFormat(22050f, 16, 1, true, false), 160));
		AlertSounds.join(Arrays.asList("a", "b"), p -> new ByteArrayInputStream(clips.get(p)));
	}

	/** A part that is not audio fails as a checked error the plugin logs, never a crash. */
	@Test(expected = java.io.IOException.class)
	public void joiningSomethingThatIsNotAudioFailsCleanly() throws Exception
	{
		final Map<String, byte[]> clips = new HashMap<>();
		clips.put("a", wav(VOICE, 160));
		clips.put("b", "not a wav file at all".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
		AlertSounds.join(Arrays.asList("a", "b"), p -> new ByteArrayInputStream(clips.get(p)));
	}

	/** A voice clip in another format counts as missing when planning, so RuneLite's sound plays instead. */
	@Test
	public void voiceClipInAnotherFormatFallsBackToRuneLite() throws Exception
	{
		final byte[] good = wav(VOICE, 160);
		final byte[] wrong = wav(new AudioFormat(22050f, 16, 1, true, false), 160);
		final Predicate<String> oneWrong = AlertSounds.playable(
			p -> new ByteArrayInputStream(p.equals("voice/stopped.wav") ? wrong : good));

		final AlertSounds.Plan plan = AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			oneWrong, f -> false);
		assertFalse(plan.resources.contains("voice/stopped.wav"));
		assertTrue("all clips fine: the line plays", !AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			AlertSounds.playable(p -> new ByteArrayInputStream(good)), f -> false).runelite);

		final AlertSounds.Plan allWrong = AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			AlertSounds.playable(p -> new ByteArrayInputStream(wrong)), f -> false);
		assertTrue(allWrong.runelite);
		assertTrue(allWrong.resources.isEmpty());
	}

	/** Pack clips play whole through AudioPlayer, so they keep their own format. */
	@Test
	public void packClipsAreNotHeldToTheVoiceFormat()
	{
		final Predicate<String> bundled = AlertSounds.playable(AlertSounds.class::getResourceAsStream);
		for (AlertSound sound : AlertSound.values())
		{
			if (sound.getClipPath() != null)
			{
				assertTrue(sound.name(), bundled.test(sound.getClipPath()));
			}
		}
		assertFalse(bundled.test("voice/no-such-clip.wav"));
	}

	/**
	 * Every bundled voice clip is in the one format the joiner takes, the
	 * format tools/voice/generate_voice.py writes, and javax.sound agrees.
	 */
	@Test
	public void everyVoiceClipIsInTheJoinFormat() throws Exception
	{
		int checked = 0;
		for (String clip : voice.clips().keySet())
		{
			final String path = VoiceLines.path(clip);
			try (InputStream in = AlertSounds.class.getResourceAsStream(path))
			{
				if (in == null)
				{
					continue; // not rendered yet: plans fall back, covered above
				}
				try (AudioInputStream audio = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in)))
				{
					assertTrue(path + " is " + audio.getFormat(), audio.getFormat().matches(VOICE));
				}
			}
			checked++;
		}
		assertTrue("voice clips are bundled", checked > 50);
		assertEquals(1, AlertSounds.Wav.VOICE.channels);
		assertEquals((int) VOICE.getSampleRate(), AlertSounds.Wav.VOICE.sampleRate);
		assertEquals(VOICE.getSampleSizeInBits(), AlertSounds.Wav.VOICE.bits);
	}

	@Test(expected = java.io.IOException.class)
	public void joiningAMissingClipFails() throws Exception
	{
		AlertSounds.join(Collections.singletonList("gone"), p -> null);
	}

	@Test
	public void volumeIsConvertedAsRuneLiteDoes()
	{
		assertEquals(0f, AlertSounds.gain(100), 0.001f);
		assertEquals(-7.96f, AlertSounds.gain(40), 0.01f);
		assertEquals(-40f, AlertSounds.gain(0), 0.001f);
		assertEquals(0f, AlertSounds.gain(150), 0.001f);
	}

	private AlertSounds.Plan plan(AlertSound sound, Predicate<String> fileExists)
	{
		return AlertSounds.plan(sound, STOPPED, null, voice, BUNDLED, fileExists);
	}

	private static byte[] pcm(AudioFormat format, int frames)
	{
		final byte[] pcm = new byte[frames * format.getFrameSize()];
		for (int i = 0; i < pcm.length; i++)
		{
			pcm[i] = (byte) (i * 7 + frames);
		}
		return pcm;
	}

	/** A WAV written by javax.sound, so the hand-made reader is checked against the JDK's writer. */
	private static byte[] wav(AudioFormat format, int frames) throws Exception
	{
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm(format, frames)), format, frames),
			AudioFileFormat.Type.WAVE, out);
		return out.toByteArray();
	}

	private static byte[] readAll(InputStream in) throws Exception
	{
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[4096];
		int read;
		while ((read = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}
}
