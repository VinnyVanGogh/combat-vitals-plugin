package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The drain cycle: 100 ticks, 150 with Preserve, anchored on login or on an
 * observed drop. The Preserve cases are the Boosts plugin's own model.
 */
public class BoostTimerTest
{
	@Test
	public void unknownUntilAnchored()
	{
		BoostTimer timer = new BoostTimer();
		assertFalse(timer.isKnown());
		assertEquals(-1, timer.ticksUntilDrop(500, false));
	}

	@Test
	public void loginAnchorsAnEstimate()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorLogin(1000);

		assertTrue(timer.isKnown());
		assertFalse("an estimate until a drop confirms it", timer.isConfirmed());
		assertEquals(100, timer.ticksUntilDrop(1000, false));
		assertEquals(60, timer.ticksUntilDrop(1040, false));
		assertEquals(BoostTimer.CYCLE, timer.cycleLength());
	}

	@Test
	public void anObservedDropConfirmsThePhase()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorLogin(1000);
		timer.anchorDrop(1030);

		assertTrue(timer.isConfirmed());
		assertEquals(100, timer.ticksUntilDrop(1030, false));
		assertEquals(1, timer.ticksUntilDrop(1129, false));
		assertEquals(0, timer.ticksUntilDrop(1130, false));
	}

	@Test
	public void rollingKeepsThePhaseWhenNothingWasBoosted()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorDrop(0);
		assertEquals(0, timer.ticksUntilDrop(100, false));

		timer.roll(100);
		assertTrue("confidence survives a roll", timer.isConfirmed());
		assertEquals(100, timer.ticksUntilDrop(100, false));
		assertEquals(50, timer.ticksUntilDrop(150, false));
	}

	@Test
	public void rollingDoesNothingWhileUnknown()
	{
		BoostTimer timer = new BoostTimer();
		timer.roll(100);
		assertFalse(timer.isKnown());
	}

	@Test
	public void preserveStretchesTheCycleToNinetySeconds()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorDrop(0);

		// Preserve on from the start: the cycle is 150 ticks throughout.
		assertEquals(150, timer.ticksUntilDrop(0, true));
		assertEquals(80, timer.ticksUntilDrop(70, true));
		assertEquals(BoostTimer.PRESERVE_CYCLE, timer.cycleLength());
		assertEquals(10, timer.ticksUntilDrop(140, true));
	}

	@Test
	public void preserveSwitchedOnLateDoesNotHelpThisCycle()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorDrop(0);

		assertEquals(20, timer.ticksUntilDrop(80, false));
		// Switched on inside the final quarter, after tick 75: too late.
		assertEquals(15, timer.ticksUntilDrop(85, true));
	}

	@Test
	public void preserveSwitchedOffInsideTheFinalQuarterLosesTheExtension()
	{
		// The Boosts plugin's model: Preserve has to stay on through the final
		// quarter (ticks 75 to 100) to earn the extra 50 ticks.
		BoostTimer timer = new BoostTimer();
		timer.anchorDrop(0);

		assertEquals(80, timer.ticksUntilDrop(70, true));
		assertEquals(70, timer.ticksUntilDrop(80, true));
		assertEquals("off at 90: back to the 100 tick cycle", 10, timer.ticksUntilDrop(90, false));
	}

	@Test
	public void preserveHeldThroughTheFinalQuarterKeepsTheExtension()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorDrop(0);

		// On before the final quarter starts, and kept on through it.
		assertEquals(80, timer.ticksUntilDrop(70, true));
		assertEquals(70, timer.ticksUntilDrop(80, true));
		assertEquals(50, timer.ticksUntilDrop(100, true));
		// Off after the quarter is complete: the 150 tick cycle stands.
		assertEquals(40, timer.ticksUntilDrop(110, true));
		assertEquals(BoostTimer.PRESERVE_CYCLE, timer.cycleLength());
	}

	@Test
	public void clearForgetsThePhase()
	{
		BoostTimer timer = new BoostTimer();
		timer.anchorDrop(0);
		timer.clear();

		assertFalse(timer.isKnown());
		assertFalse(timer.isConfirmed());
		assertEquals(-1, timer.ticksUntilDrop(10, false));
	}

	@Test
	public void combatStatsRunOnTheOtherTimer()
	{
		assertTrue(BoostTimer.onSkillingTimer(Skill.FISHING));
		assertTrue(BoostTimer.onSkillingTimer(Skill.WOODCUTTING));
		assertTrue(BoostTimer.onSkillingTimer(Skill.THIEVING));
		assertFalse(BoostTimer.onSkillingTimer(Skill.ATTACK));
		assertFalse(BoostTimer.onSkillingTimer(Skill.HITPOINTS));
		assertFalse(BoostTimer.onSkillingTimer(Skill.PRAYER));
	}
}
