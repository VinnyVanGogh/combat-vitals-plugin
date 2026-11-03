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
		assertEquals(Skill.THIEVING, SkillingActions.skill(MAN, "Man"));
	}

	@Test
	public void theClickedOptionDecidesForAnAttackableNpc()
	{
		// A Man can be fought and pickpocketed. The option clicked says which.
		assertEquals(Skill.THIEVING, SkillingActions.forOption("Pickpocket", "Man"));
		assertNull("Attack is a fight, not a skill", SkillingActions.forOption("Attack", "Man"));
		assertNull(SkillingActions.forOption("Talk-to", "Man"));
	}

	@Test
	public void objectOptionsNameTheirSkill()
	{
		assertEquals(Skill.WOODCUTTING, SkillingActions.forOption("Chop down", "Yew tree"));
		assertEquals(Skill.WOODCUTTING, SkillingActions.forOption("Chop", "Redwood tree"));
		assertEquals(Skill.MINING, SkillingActions.forOption("Mine", "Iron rocks"));
		assertEquals(Skill.THIEVING, SkillingActions.forOption("Steal-from", "Silk stall"));
		assertEquals(Skill.RUNECRAFT, SkillingActions.forOption("Craft-rune", "Altar"));
	}

	@Test
	public void unknownObjectOptionsAreNothing()
	{
		assertNull(SkillingActions.forOption("Bank", "Bank booth"));
		assertNull(SkillingActions.forOption("Open", "Door"));
		assertNull(SkillingActions.forOption("Prospect", "Iron rocks"));
		assertNull(SkillingActions.forOption(null, "Yew tree"));
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
