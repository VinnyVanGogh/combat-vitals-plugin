package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The fallbacks first: an unknown item, an unknown activity and a missing
 * clip must still say something sensible, or nothing so RuneLite's sound
 * plays. Then the bundled list composes the Board's example.
 */
public class VoiceLinesTest
{
	private static final Predicate<String> ALL = p -> true;
	private static final Predicate<String> NONE = p -> false;

	private final VoiceLines lines = VoiceLines.load();

	@Test
	public void theWordListLoadsFromTheJarResource()
	{
		assertNotNull("getResourceAsStream", VoiceLines.class.getResourceAsStream(VoiceLines.LIST));
		assertFalse(lines.clips().isEmpty());
	}

	@Test
	public void unknownItemSaysTheActivityOnly()
	{
		assertEquals(paths("stopped", "verb-chopping"),
			lines.compose(stopped(Skill.WOODCUTTING), "Bird nest", ALL));
		assertEquals(paths("stopped", "verb-fishing"),
			lines.compose(stopped(Skill.FISHING), null, ALL));
	}

	@Test
	public void anItemFromAnotherSkillIsNotSaid()
	{
		// Logs left over from woodcutting are not what you were fishing.
		assertEquals(paths("stopped", "verb-fishing"),
			lines.compose(stopped(Skill.FISHING), "Yew logs", ALL));
	}

	@Test
	public void unknownActivityFallsBackToStoppedGathering()
	{
		assertEquals(paths("stopped-gathering"), lines.compose(stopped(null), "Yew logs", ALL));
		assertEquals(paths("stopped-gathering"), lines.compose(stopped(Skill.AGILITY), null, ALL));
	}

	@Test
	public void aMissingClipFallsBackToStoppedGathering()
	{
		final Predicate<String> noYew = p -> !p.equals(VoiceLines.path("res-yew-logs"));
		assertEquals(paths("stopped-gathering"),
			lines.compose(stopped(Skill.WOODCUTTING), "Yew logs", noYew));
	}

	@Test
	public void nothingBundledMeansNothingToSay()
	{
		assertTrue(lines.compose(stopped(Skill.WOODCUTTING), "Yew logs", NONE).isEmpty());
		assertTrue(lines.compose(alert(ActivityAlerts.Type.INVENTORY_FULL, Skill.FISHING), null, NONE).isEmpty());
	}

	@Test
	public void picksTheVerbAndResource()
	{
		assertEquals(paths("stopped", "verb-chopping", "res-yew-logs"),
			lines.compose(stopped(Skill.WOODCUTTING), "Yew logs", ALL));
		assertEquals(paths("stopped", "verb-fishing", "res-lobsters"),
			lines.compose(stopped(Skill.FISHING), "Raw lobster", ALL));
		assertEquals(paths("stopped", "verb-mining", "res-iron-ore"),
			lines.compose(stopped(Skill.MINING), "Iron ore", ALL));
		assertEquals(paths("stopped", "verb-hunting", "res-red-chinchompas"),
			lines.compose(stopped(Skill.HUNTER), "Red chinchompa", ALL));
	}

	@Test
	public void otherAlertsHaveTheirOwnLine()
	{
		assertEquals(paths("inventory-full"),
			lines.compose(alert(ActivityAlerts.Type.INVENTORY_FULL, Skill.FISHING), "Raw lobster", ALL));
		assertEquals(paths("finished", "verb-cooking"),
			lines.compose(alert(ActivityAlerts.Type.PRODUCTION_FINISHED, Skill.COOKING), null, ALL));
		assertEquals(paths("stopped", "verb-thieving"),
			lines.compose(alert(ActivityAlerts.Type.THIEVING_STOPPED, Skill.THIEVING), null, ALL));
		for (ActivityAlerts.Type type : ActivityAlerts.Type.values())
		{
			assertFalse(type + " has no line",
				lines.compose(alert(type, Skill.FISHING), null, ALL).isEmpty());
		}
	}

	@Test
	public void resourcesAreReadWhateverTheCase()
	{
		assertEquals("res-yew-logs", lines.resource(Skill.WOODCUTTING, "YEW LOGS"));
		assertNull(lines.resource(Skill.WOODCUTTING, null));
		assertNull(lines.resource(null, "Yew logs"));
	}

	/**
	 * Once the clips are rendered (tools/voice/generate_voice.py), every clip
	 * the list names must be in the jar, so no line silently falls back.
	 * Skipped while none is bundled.
	 */
	/**
	 * Listed in lines.tsv but not rendered yet: run tools/voice/generate_voice.py, add the WAV and
	 * remove it here. Until then the alert falls back to RuneLite's sound (voice is optional, task-200059fb).
	 */
	private static final java.util.Set<String> PENDING_CLIPS = java.util.Collections.singleton("skilling-event");

	@Test
	public void everyListedClipIsBundledOnceAnyIs()
	{
		final Map<String, String> clips = lines.clips();
		final boolean any = clips.keySet().stream()
			.anyMatch(c -> VoiceLines.class.getResource(VoiceLines.path(c)) != null);
		assumeTrue("voice clips not rendered yet", any);
		for (String clip : clips.keySet())
		{
			if (PENDING_CLIPS.contains(clip))
			{
				continue;
			}
			assertNotNull(clip + " (\"" + clips.get(clip) + "\") is not bundled",
				VoiceLines.class.getResourceAsStream(VoiceLines.path(clip)));
		}
	}

	@Test
	public void skillingEventsSayWhatExactlyHappened()
	{
		assertEquals(paths("bird-nest"),
			lines.compose(alert(ActivityAlerts.Type.BIRD_NEST, Skill.WOODCUTTING), null, ALL));
		assertEquals(paths("clue-hard"), lines.compose(
			new ActivityAlerts.Alert(ActivityAlerts.Type.CLUE_DROP, null, "Clue geode (hard)"), null, ALL));
		assertEquals(paths("clue-nest-fell"), lines.compose(
			new ActivityAlerts.Alert(ActivityAlerts.Type.BIRD_NEST, null, "Clue nest (easy)"), null, ALL));
		assertEquals(paths("pet-duplicate"), lines.compose(
			new ActivityAlerts.Alert(ActivityAlerts.Type.PET, null, SkillingEvents.DUPLICATE), null, ALL));
		for (SkillingEvents.Forestry kind : SkillingEvents.Forestry.values())
		{
			final List<String> said = lines.compose(
				new ActivityAlerts.Alert(ActivityAlerts.Type.FORESTRY_EVENT, null, kind.title), null, ALL);
			assertEquals(kind.name(), 1, said.size());
			assertTrue(kind + ": " + said, said.get(0).contains("forestry-"));
			assertFalse(kind + " has its own line", said.equals(paths("forestry-event")));
		}
	}

	@Test
	public void aMissingDetailClipFallsBackToTheAlertsLine()
	{
		final Predicate<String> noTier = p -> !p.equals(VoiceLines.path("clue-hard"));
		assertEquals(paths("clue-drop"), lines.compose(
			new ActivityAlerts.Alert(ActivityAlerts.Type.CLUE_DROP, null, "Clue geode (hard)"), null, noTier));
	}

	private static ActivityAlerts.Alert stopped(Skill skill)
	{
		return alert(ActivityAlerts.Type.GATHERING_STOPPED, skill);
	}

	private static ActivityAlerts.Alert alert(ActivityAlerts.Type type, Skill skill)
	{
		return new ActivityAlerts.Alert(type, skill);
	}

	private static List<String> paths(String... ids)
	{
		final List<String> out = new ArrayList<>();
		for (String id : ids)
		{
			out.add(VoiceLines.path(id));
		}
		return out;
	}
}
