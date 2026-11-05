package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class IdleBadgeTest
{
	private static final int SECONDS = 60;

	private IdleBadge idle;

	@Before
	public void setUp()
	{
		idle = new IdleBadge();
	}

	@Test
	public void nothingDoneYetIsNotIdle()
	{
		idle.tick(null, false, 0);
		assertNull(idle.idleSkill(IdleBadgeMode.UNTIL_NEXT, SECONDS, 0));
	}

	@Test
	public void workingIsNotIdle()
	{
		idle.tick(Skill.FISHING, false, 0);
		assertNull(idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 0));
	}

	@Test
	public void stoppingFishingLeavesTheBadgeUpAsNotFishing()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		assertEquals(Skill.FISHING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 1_000));
	}

	@Test
	public void fishingIdleFishingGoesBackToWorking()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		idle.tick(Skill.FISHING, false, 2_000);
		assertNull(idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 2_000));

		// And a second stop starts a fresh idle period, not the old one.
		idle.tick(null, false, 100_000);
		assertEquals(Skill.FISHING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 159_000));
	}

	@Test
	public void timedIdleEndsAfterItsSeconds()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		assertEquals(Skill.FISHING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 60_999));
		assertNull(idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 61_000));
	}

	@Test
	public void untilNextOutlastsTheTimeout()
	{
		idle.tick(Skill.MINING, false, 0);
		idle.tick(null, false, 1_000);
		assertEquals(Skill.MINING, idle.idleSkill(IdleBadgeMode.UNTIL_NEXT, SECONDS, 3_600_000));
	}

	@Test
	public void offNeverShowsIdle()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		assertNull(idle.idleSkill(IdleBadgeMode.OFF, SECONDS, 1_000));
	}

	@Test
	public void combatEndsIdle()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		idle.tick(null, true, 2_000);
		assertNull(idle.idleSkill(IdleBadgeMode.UNTIL_NEXT, SECONDS, 2_000));

		// Still nothing once the fight is over: the fishing is not resurrected.
		idle.tick(null, false, 30_000);
		assertNull(idle.idleSkill(IdleBadgeMode.UNTIL_NEXT, SECONDS, 30_000));
	}

	@Test
	public void beingHitWhileStillFishingKeepsTheActivity()
	{
		idle.tick(Skill.FISHING, true, 0);
		idle.tick(null, false, 1_000);
		assertEquals(Skill.FISHING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 1_000));
	}

	@Test
	public void anotherActivityReplacesTheIdleSkill()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		idle.tick(Skill.WOODCUTTING, false, 2_000);
		idle.tick(null, false, 3_000);
		assertEquals(Skill.WOODCUTTING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 3_000));
	}

	@Test
	public void clearForgetsEverything()
	{
		idle.tick(Skill.FISHING, false, 0);
		idle.tick(null, false, 1_000);
		idle.clear();
		assertNull(idle.idleSkill(IdleBadgeMode.UNTIL_NEXT, SECONDS, 1_000));
	}
}
