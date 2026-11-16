package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.Skill;
import org.junit.Test;

public class RecentGainTest
{
	@Test
	public void theLoginBaselineIsNeverAGain()
	{
		RecentGain g = new RecentGain();
		g.xp(Skill.STRENGTH, 4_000, 10);
		assertFalse(g.gained(Skill.STRENGTH, 10));
	}

	@Test
	public void aStrengthDropCountsOnItsTickAndTheNext()
	{
		RecentGain g = new RecentGain();
		g.xp(Skill.STRENGTH, 4_000, 10);
		g.xp(Skill.STRENGTH, 4_002, 12);
		assertTrue(g.gained(Skill.STRENGTH, 12));
		assertTrue(g.gained(Skill.STRENGTH, 13));
		assertFalse("two ticks later it is the animation's call again", g.gained(Skill.STRENGTH, 14));
	}

	@Test
	public void anotherSkillsDropIsNotPumping()
	{
		RecentGain g = new RecentGain();
		g.xp(Skill.STRENGTH, 4_000, 10);
		g.xp(Skill.HITPOINTS, 100, 10);
		g.xp(Skill.HITPOINTS, 120, 12);
		assertFalse(g.gained(Skill.STRENGTH, 12));
		assertFalse(g.gained(null, 12));
	}

	@Test
	public void clearMakesTheNextValueABaseline()
	{
		RecentGain g = new RecentGain();
		g.xp(Skill.STRENGTH, 4_000, 10);
		g.clear();
		g.xp(Skill.STRENGTH, 9_000, 20);
		assertFalse(g.gained(Skill.STRENGTH, 20));
	}
}
