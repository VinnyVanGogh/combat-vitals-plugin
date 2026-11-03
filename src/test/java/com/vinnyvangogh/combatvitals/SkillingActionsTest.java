package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The fishing spot shows the Fishing badge and not the health bar; the
 * goblin shows the health bar and not a badge. Both decisions come from the
 * NPC's menu, so both are testable without a client.
 */
public class SkillingActionsTest
{
	private static final String[] FISHING_SPOT = {"Net", "Bait", null, null, null};
	private static final String[] HARPOON_SPOT = {"Cage", "Harpoon", null, null, null};
	private static final String[] BARBARIAN_SPOT = {"Use-rod", null, null, null, null};
	private static final String[] GOBLIN = {"Attack", null, "Talk-to", null, null};
	private static final String[] BANKER = {"Bank", "Talk-to", "Collect", null, null};
	private static final String[] MAN = {"Talk-to", "Attack", "Pickpocket", null, null};
	private static final String[] IMPLING = {"Catch", null, null, null, null};

	@Test
	public void fishingSpotShowsTheFishingBadgeNotTheHealthBar()
	{
		assertFalse("not a fight", CombatState.attackable(FISHING_SPOT, -1));
		assertEquals(Skill.FISHING, SkillingActions.skill(FISHING_SPOT, "Fishing spot"));
	}

	@Test
	public void everyFishingStyleIsFishing()
	{
		assertEquals(Skill.FISHING, SkillingActions.skill(HARPOON_SPOT, "Fishing spot"));
		assertEquals(Skill.FISHING, SkillingActions.skill(BARBARIAN_SPOT, "Fishing spot"));
		assertEquals(Skill.FISHING, SkillingActions.skill(new String[] {"Big Net", "Harpoon"}, "Fishing spot"));
		assertEquals(Skill.FISHING, SkillingActions.skill(new String[] {"Small Net", "Bait"}, "Fishing spot"));
		assertEquals(Skill.FISHING, SkillingActions.skill(new String[] {"Lure", "Bait"}, "Rod Fishing spot"));
		assertEquals(Skill.FISHING, SkillingActions.skill(new String[] {"Fish", null}, "Fishing spot"));
	}

	@Test
	public void combatTargetStillShowsTheHealthBar()
	{
		assertTrue(CombatState.attackable(GOBLIN, 2));
		assertNull("no badge for a goblin", SkillingActions.skill(GOBLIN, "Goblin"));
	}

	@Test
	public void bankerShowsNeither()
	{
		assertFalse(CombatState.attackable(BANKER, -1));
		assertNull(SkillingActions.skill(BANKER, "Banker"));
	}

	@Test
	public void pickpocketIsThieving()
	{
		// A Man is attackable and pickpocketable. The plugin asks attackability
		// first, so this only matters for NPCs that cannot be fought, but the
		// table itself should still answer.
		assertEquals(Skill.THIEVING, SkillingActions.skill(MAN, "Man"));
	}

	@Test
	public void catchIsHunterUnlessItIsAShoal()
	{
		assertEquals(Skill.HUNTER, SkillingActions.skill(IMPLING, "Baby impling"));
		assertEquals(Skill.FISHING, SkillingActions.skill(IMPLING, "Fish shoal"));
	}

	@Test
	public void matchingIgnoresCase()
	{
		assertEquals(Skill.FISHING, SkillingActions.skill(new String[] {"NET"}, null));
	}

	@Test
	public void nullsAreNothing()
	{
		assertNull(SkillingActions.skill(null, "Fishing spot"));
		assertNull(SkillingActions.skill(new String[] {null, null}, null));
	}
}
