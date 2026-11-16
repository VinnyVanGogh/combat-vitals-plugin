package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

/**
 * The XP-drop skills: Herblore first (Board, 2026-11-14: "the skilling one
 * doesn't show when I'm doing herblore"), then the Make-X skills, Firemaking,
 * Prayer, Magic, Agility, Construction and Sailing.
 */
public class XpSkillingTest
{
	private XpSkilling xp;
	private int tick;

	@Before
	public void setUp()
	{
		xp = new XpSkilling();
		tick = 100;
		// Login reports every skill: a baseline, never a gain.
		for (Skill skill : Skill.values())
		{
			xp.xp(skill, 1_000);
		}
		assertEquals(XpSkilling.Step.NONE, step(false, false));
	}

	private XpSkilling.Step step(boolean animating, boolean blocked)
	{
		return xp.tick(++tick, animating, blocked);
	}

	private void gain(Skill skill, int amount)
	{
		// Each gain is on top of what was there; a fresh counter per call keeps it simple.
		xp.xp(skill, 1_000 + amount);
	}

	@Test
	public void cleaningAHerbRaisesTheHerbloreBadge()
	{
		gain(Skill.HERBLORE, 3);
		assertEquals(XpSkilling.Step.ENGAGE, step(true, false));
		assertEquals(Skill.HERBLORE, xp.skill());
		assertTrue(xp.working());
		assertTrue(xp.acted());
	}

	@Test
	public void everyListedSkillIsTracked()
	{
		for (Skill skill : new Skill[]{Skill.HERBLORE, Skill.COOKING, Skill.FLETCHING, Skill.CRAFTING,
			Skill.SMITHING, Skill.FIREMAKING, Skill.PRAYER, Skill.MAGIC, Skill.AGILITY, Skill.CONSTRUCTION,
			Skill.SAILING})
		{
			assertTrue(skill + " is tracked", XpSkilling.tracked(skill));
		}
	}

	@Test
	public void theNpcAndObjectSkillsAndFarmingAreNot()
	{
		for (Skill skill : new Skill[]{Skill.FISHING, Skill.WOODCUTTING, Skill.MINING, Skill.THIEVING,
			Skill.RUNECRAFT, Skill.HUNTER, Skill.FARMING, Skill.ATTACK, Skill.HITPOINTS, Skill.SLAYER})
		{
			assertFalse(skill + " is not tracked", XpSkilling.tracked(skill));
			gain(skill, 10);
			assertEquals(XpSkilling.Step.NONE, step(true, false));
			assertNull(xp.skill());
		}
	}

	@Test
	public void theFirstReadingIsABaselineNotAGain()
	{
		final XpSkilling fresh = new XpSkilling();
		fresh.xp(Skill.HERBLORE, 5_000);
		assertEquals(XpSkilling.Step.NONE, fresh.tick(1, true, false));
		assertNull(fresh.skill());
	}

	@Test
	public void theAnimationKeepsItUpBetweenDrops()
	{
		gain(Skill.HERBLORE, 3);
		step(true, false);
		for (int i = 0; i < 30; i++)
		{
			assertEquals(XpSkilling.Step.NONE, step(true, false));
		}
		assertTrue(xp.working());
	}

	@Test
	public void noDropAndNoAnimationStopsItAfterTheSkillsGap()
	{
		gain(Skill.HERBLORE, 3);
		step(true, false);
		for (int i = 1; i < XpSkilling.idleTicks(Skill.HERBLORE); i++)
		{
			assertEquals(XpSkilling.Step.NONE, step(false, false));
		}
		assertEquals(XpSkilling.Step.DISENGAGE, step(false, false));
		assertFalse(xp.working());
		// The skill stays known for the linger and the "Not doing herblore" badge.
		assertEquals(Skill.HERBLORE, xp.skill());
	}

	@Test
	public void agilityRunsBetweenObstaclesWithoutAnimating()
	{
		assertTrue(XpSkilling.idleTicks(Skill.AGILITY) > XpSkilling.idleTicks(Skill.HERBLORE));
		gain(Skill.AGILITY, 20);
		step(true, false);
		for (int i = 0; i < XpSkilling.idleTicks(Skill.HERBLORE) + 2; i++)
		{
			assertEquals(XpSkilling.Step.NONE, step(false, false));
		}
		assertTrue(xp.working());
	}

	@Test
	public void aDropAfterStoppingRaisesItAgain()
	{
		gain(Skill.COOKING, 30);
		step(true, false);
		for (int i = 0; i < XpSkilling.idleTicks(Skill.COOKING); i++)
		{
			step(false, false);
		}
		assertFalse(xp.working());
		gain(Skill.COOKING, 60);
		assertEquals(XpSkilling.Step.ENGAGE, step(true, false));
		assertTrue(xp.working());
	}

	@Test
	public void anotherSkillsDropSwitchesTheBadge()
	{
		gain(Skill.FLETCHING, 5);
		step(true, false);
		gain(Skill.CRAFTING, 5);
		assertEquals(XpSkilling.Step.ENGAGE, step(true, false));
		assertEquals(Skill.CRAFTING, xp.skill());
	}

	@Test
	public void superheatIsMagicNotSmithing()
	{
		gain(Skill.MAGIC, 53);
		gain(Skill.SMITHING, 6);
		assertEquals(XpSkilling.Step.ENGAGE, step(true, false));
		assertEquals(Skill.MAGIC, xp.skill());
	}

	@Test
	public void aCombatSpellIsNotSkilling()
	{
		// A combat spell gives Hitpoints experience in the same tick; an alch does not.
		gain(Skill.MAGIC, 30);
		gain(Skill.HITPOINTS, 10);
		assertEquals(XpSkilling.Step.NONE, step(true, false));
		assertNull(xp.skill());
	}

	@Test
	public void blockedByAnotherBadgeOrAFightNothingStarts()
	{
		// Cooking from an infernal harpoon while the fishing badge is up.
		gain(Skill.COOKING, 50);
		assertEquals(XpSkilling.Step.NONE, step(true, true));
		assertNull(xp.skill());
	}

	@Test
	public void aFightReleasesARunningBadge()
	{
		gain(Skill.PRAYER, 50);
		step(true, false);
		assertEquals(XpSkilling.Step.RELEASE, step(true, true));
		assertNull(xp.skill());
	}

	@Test
	public void forgetLeavesTheBaselinesSoTheNextGainCounts()
	{
		gain(Skill.HERBLORE, 3);
		step(true, false);
		xp.forget();
		assertNull(xp.skill());
		gain(Skill.HERBLORE, 6);
		assertEquals(XpSkilling.Step.ENGAGE, step(true, false));
	}

	@Test
	public void actedIsOnlyTheTickOfAGain()
	{
		gain(Skill.HERBLORE, 3);
		step(true, false);
		assertTrue(xp.acted());
		step(true, false);
		assertFalse(xp.acted());
	}
}
