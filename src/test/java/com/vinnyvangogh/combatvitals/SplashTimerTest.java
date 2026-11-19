package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class SplashTimerTest
{
	@Test
	public void theSessionStartsAtTheFirstCastAndKeepsRunning()
	{
		final SplashTimer t = new SplashTimer();
		assertEquals(-1, t.elapsed(1000));
		t.cast(1000);
		t.cast(1600);
		assertEquals(2000, t.elapsed(3000));
	}

	@Test
	public void aShortGapKeepsTheSession()
	{
		final SplashTimer t = new SplashTimer();
		t.cast(0);
		for (int i = 0; i < SplashTracker.FORGET_TICKS - 1; i++)
		{
			t.idle();
		}
		assertEquals(10_000, t.elapsed(10_000));
	}

	@Test
	public void aLongGapEndsItAndTheNextCastStartsFresh()
	{
		final SplashTimer t = new SplashTimer();
		t.cast(0);
		for (int i = 0; i < SplashTracker.FORGET_TICKS; i++)
		{
			t.idle();
		}
		assertEquals(-1, t.elapsed(10_000));
		t.cast(20_000);
		assertEquals(500, t.elapsed(20_500));
	}

	@Test
	public void castingBetweenIdleTicksResetsTheGapCount()
	{
		final SplashTimer t = new SplashTimer();
		t.cast(0);
		for (int i = 0; i < SplashTracker.FORGET_TICKS - 1; i++)
		{
			t.idle();
		}
		t.cast(6000);
		for (int i = 0; i < SplashTracker.FORGET_TICKS - 1; i++)
		{
			t.idle();
		}
		assertEquals(12_000, t.elapsed(12_000));
	}

	@Test
	public void resetClearsTheSession()
	{
		final SplashTimer t = new SplashTimer();
		t.cast(0);
		t.reset();
		assertEquals(-1, t.elapsed(5));
	}

	@Test
	public void logoutReadsActiveWhileYouUseTheClient()
	{
		// 5 minutes in ticks of ms; input 1 s ago.
		assertEquals("Active", SplashTimer.logoutText(1000, 300_000));
	}

	@Test
	public void logoutCountsDownOnceIdle()
	{
		assertEquals("Logout in 4:00", SplashTimer.logoutText(60_000, 300_000));
		assertEquals(240_000, SplashTimer.logoutLeft(60_000, 300_000));
	}

	@Test
	public void inputResetsTheLogoutClock()
	{
		// 10 s idle then input: idle goes back to 0, the full timeout is back.
		assertEquals(290_000, SplashTimer.logoutLeft(10_000, 300_000));
		assertEquals(300_000, SplashTimer.logoutLeft(0, 300_000));
	}

	@Test
	public void unknownTimeoutShowsNothing()
	{
		assertNull(SplashTimer.logoutText(1000, 0));
		assertNull(SplashTimer.logoutText(-1, 300_000));
		assertEquals(-1, SplashTimer.logoutLeft(1000, -5));
	}

	@Test
	public void logoutNeverGoesNegative()
	{
		assertEquals(0, SplashTimer.logoutLeft(400_000, 300_000));
	}

	@Test
	public void clocksFormat()
	{
		assertEquals("0:09", SplashTimer.clock(9_000));
		assertEquals("3:07", SplashTimer.clock(187_000));
		assertEquals("1:02:09", SplashTimer.clock(3_729_000));
	}
}
