package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class AggressionTimerTest
{
	private AggressionTimer timer;

	@Before
	public void setUp()
	{
		timer = new AggressionTimer();
	}

	@Test
	public void idleUntilAnNpcAttacksYou()
	{
		timer.tick(3000, 3000, 0, 301);
		assertEquals(AggressionTimer.State.IDLE, timer.state(5));
		assertNull(timer.text(5));
	}

	@Test
	public void attackStartsTenMinuteCountdown()
	{
		timer.aggressed(3000, 3000, 0, 301, 100);
		assertEquals(AggressionTimer.State.RUNNING, timer.state(100));
		assertEquals(AggressionTimer.TIMER_TICKS, timer.remainingTicks(100));
		assertEquals("Aggressive for 10:00", timer.text(100));
		assertEquals("Aggressive for 9:54", timer.text(110));
	}

	@Test
	public void aSecondAttackDoesNotRestartTheTimer()
	{
		timer.aggressed(3000, 3000, 0, 301, 100);
		timer.aggressed(3002, 3000, 0, 301, 400);
		assertEquals(AggressionTimer.TIMER_TICKS - 300, timer.remainingTicks(400));
	}

	@Test
	public void expiresIntoCalm()
	{
		timer.aggressed(3000, 3000, 0, 301, 0);
		final int end = AggressionTimer.TIMER_TICKS;
		assertEquals(AggressionTimer.State.CALM, timer.state(end));
		assertEquals("NPCs are no longer aggressive here", timer.text(end));
		assertEquals(0, timer.remainingTicks(end));
	}

	@Test
	public void leavingTheSafeAreaResetsIt()
	{
		timer.aggressed(3000, 3000, 0, 301, 0);
		timer.tick(3010, 3010, 0, 301);
		assertEquals(AggressionTimer.State.RUNNING, timer.state(1));
		timer.tick(3011, 3000, 0, 301);
		assertEquals(AggressionTimer.State.IDLE, timer.state(2));
	}

	@Test
	public void leavingAfterCalmArmsItAgain()
	{
		timer.aggressed(3000, 3000, 0, 301, 0);
		timer.tick(3000, 3000, 0, 301);
		assertEquals(AggressionTimer.State.CALM, timer.state(AggressionTimer.TIMER_TICKS));
		timer.tick(3050, 3000, 0, 301);
		assertEquals(AggressionTimer.State.IDLE, timer.state(AggressionTimer.TIMER_TICKS + 1));
	}

	@Test
	public void worldHopOrPlaneChangeResets()
	{
		timer.aggressed(3000, 3000, 0, 301, 0);
		timer.tick(3000, 3000, 0, 302);
		assertEquals(AggressionTimer.State.IDLE, timer.state(1));
		timer.aggressed(3000, 3000, 0, 302, 2);
		timer.tick(3000, 3000, 1, 302);
		assertEquals(AggressionTimer.State.IDLE, timer.state(3));
	}

	@Test
	public void areaIs21By21AroundTheStart()
	{
		timer.aggressed(3000, 3000, 0, 301, 0);
		assertTrue(timer.inArea(2990, 3010));
		assertFalse(timer.inArea(2989, 3000));
		assertFalse(timer.inArea(3000, 3011));
		assertEquals(2990, timer.minX());
		assertEquals(3010, timer.maxX());
	}

	@Test
	public void clearGoesIdle()
	{
		timer.aggressed(3000, 3000, 0, 301, 0);
		timer.clear();
		assertEquals(AggressionTimer.State.IDLE, timer.state(1));
	}
}
