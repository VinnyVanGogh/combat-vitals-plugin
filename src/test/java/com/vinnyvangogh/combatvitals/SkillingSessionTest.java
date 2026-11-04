package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class SkillingSessionTest
{
	private static final long MINUTE = 60_000;

	private SkillingSession session;

	@Before
	public void setUp()
	{
		session = new SkillingSession();
	}

	@Test
	public void theFirstReadingIsABaselineNotAGain()
	{
		session.xp(Skill.FISHING, 1_000_000, 0);
		assertEquals(0, session.actions(Skill.FISHING));
	}

	@Test
	public void everyGainIsOneAction()
	{
		session.xp(Skill.FISHING, 1000, 0);
		session.xp(Skill.FISHING, 1050, 1000);
		session.xp(Skill.FISHING, 1100, 2000);
		assertEquals(2, session.actions(Skill.FISHING));
	}

	@Test
	public void aRepeatOfTheSameXpIsNoAction()
	{
		// StatChanged fires for boosts with the experience unchanged.
		session.xp(Skill.FISHING, 1000, 0);
		session.xp(Skill.FISHING, 1000, 1000);
		assertEquals(0, session.actions(Skill.FISHING));
	}

	@Test
	public void skillsAreCountedApart()
	{
		session.xp(Skill.FISHING, 1000, 0);
		session.xp(Skill.COOKING, 1000, 0);
		session.xp(Skill.COOKING, 1100, 1000);
		assertEquals(0, session.actions(Skill.FISHING));
		assertEquals(1, session.actions(Skill.COOKING));
	}

	@Test
	public void ratesAreUnknownUntilAMinuteHasPassed()
	{
		session.xp(Skill.FISHING, 0, 0);
		session.xp(Skill.FISHING, 50, 0);
		session.xp(Skill.FISHING, 100, 30_000);
		assertEquals(-1, session.xpPerHour(Skill.FISHING, 59_999));
		assertEquals(-1, session.actionsPerHour(Skill.FISHING, 59_999));
	}

	@Test
	public void xpPerHourCountsFromTheFirstGain()
	{
		// First gain at t=0 starts the clock; its own 50 xp were earned before
		// the clock started, so only the 10 gains after it count.
		session.xp(Skill.FISHING, 0, 0);
		session.xp(Skill.FISHING, 50, 0);
		for (int i = 1; i <= 10; i++)
		{
			session.xp(Skill.FISHING, 50 + i * 50, i * 6_000L);
		}

		// 500 xp and 10 actions in one minute.
		assertEquals(30_000, session.xpPerHour(Skill.FISHING, MINUTE));
		assertEquals(600, session.actionsPerHour(Skill.FISHING, MINUTE));
		assertEquals(11, session.actions(Skill.FISHING));
	}

	@Test
	public void ratesFallWhileYouStandStill()
	{
		session.xp(Skill.MINING, 0, 0);
		session.xp(Skill.MINING, 100, 0);
		session.xp(Skill.MINING, 200, MINUTE);
		assertEquals(6_000, session.xpPerHour(Skill.MINING, MINUTE));
		assertEquals(3_000, session.xpPerHour(Skill.MINING, 2 * MINUTE));
	}

	@Test
	public void resetZeroesTheCountersButKeepsTheBaseline()
	{
		session.xp(Skill.FISHING, 1000, 0);
		session.xp(Skill.FISHING, 1100, 1000);
		session.reset();
		assertEquals(0, session.actions(Skill.FISHING));

		// The baseline survives: the next drop is one action, not a jump from 0.
		session.xp(Skill.FISHING, 1150, 2000);
		assertEquals(1, session.actions(Skill.FISHING));
	}

	@Test
	public void clearForgetsTheBaselineToo()
	{
		session.xp(Skill.FISHING, 1000, 0);
		session.clear();
		session.xp(Skill.FISHING, 5000, 1000);
		assertEquals(0, session.actions(Skill.FISHING));
	}

	@Test
	public void anUnseenSkillHasNoRate()
	{
		assertEquals(0, session.actions(Skill.HUNTER));
		assertEquals(-1, session.xpPerHour(Skill.HUNTER, MINUTE));
	}
}
