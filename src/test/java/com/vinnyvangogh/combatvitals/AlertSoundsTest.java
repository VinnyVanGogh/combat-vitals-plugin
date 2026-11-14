package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
	private static final File DIR = new File("no-such-runelite/combat-vitals/sounds");
	private static final Predicate<String> BUNDLED = p -> AlertSounds.class.getResource(p) != null;
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
		assertEquals(new File(DIR, "gathering-stopped.wav"), plan.file);

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
		assertEquals(new File(DIR, "gathering-stopped.wav"), plan.file);
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
			p -> false, DIR, f -> true);
		assertTrue(plan.runelite);
		assertFalse(plan.missingFile);
	}

	@Test
	public void unrenderedVoiceFallsBackToRuneLite()
	{
		final AlertSounds.Plan plan = AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			p -> false, DIR, f -> true);
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
			if (sound.getResource() == null)
			{
				continue;
			}
			try (InputStream in = AlertSounds.class.getResourceAsStream(sound.getResource()))
			{
				assertNotNull(sound + " is not bundled", in);
				try (AudioInputStream audio = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in)))
				{
					assertTrue(sound + " is empty", audio.getFrameLength() > 0);
				}
			}

			final AlertSounds.Plan plan = AlertSounds.plan(sound, STOPPED, null, voice, BUNDLED, DIR, f -> false);
			assertFalse(plan.runelite);
			assertEquals(Collections.singletonList(sound.getResource()), plan.resources);
		}
	}

	@Test
	public void voiceLinePlansTheComposedClips()
	{
		final AlertSounds.Plan plan = AlertSounds.plan(AlertSound.VOICE, STOPPED, "Yew logs", voice,
			p -> true, DIR, f -> false);
		assertFalse(plan.runelite);
		assertEquals(Arrays.asList("voice/stopped.wav", "voice/verb-chopping.wav", "voice/res-yew-logs.wav"),
			plan.resources);
	}

	@Test
	public void joinedClipsKeepEveryFrame() throws Exception
	{
		final AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
		final Map<String, byte[]> clips = new HashMap<>();
		clips.put("a", wav(format, 1600));
		clips.put("b", wav(format, 800));
		clips.put("c", wav(format, 400));

		final byte[] joined = AlertSounds.join(Arrays.asList("a", "b", "c"),
			p -> new ByteArrayInputStream(clips.get(p)));
		try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(joined)))
		{
			assertEquals(2800, audio.getFrameLength());
			assertTrue(audio.getFormat().matches(format));
		}
	}

	/** A part that is not audio fails as a checked error the plugin logs, never a crash. */
	@Test(expected = javax.sound.sampled.UnsupportedAudioFileException.class)
	public void joiningSomethingThatIsNotAudioFailsCleanly() throws Exception
	{
		final Map<String, byte[]> clips = new HashMap<>();
		clips.put("a", wav(new AudioFormat(16000f, 16, 1, true, false), 160));
		clips.put("b", "not a wav file at all".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
		AlertSounds.join(Arrays.asList("a", "b"), p -> new ByteArrayInputStream(clips.get(p)));
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

	private AlertSounds.Plan plan(AlertSound sound, Predicate<File> fileExists)
	{
		return AlertSounds.plan(sound, STOPPED, null, voice, BUNDLED, DIR, fileExists);
	}

	private static byte[] wav(AudioFormat format, int frames) throws Exception
	{
		final byte[] pcm = new byte[frames * format.getFrameSize()];
		for (int i = 0; i < pcm.length; i++)
		{
			pcm[i] = (byte) (i * 7);
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, frames),
			AudioFileFormat.Type.WAVE, out);
		return out.toByteArray();
	}
}
