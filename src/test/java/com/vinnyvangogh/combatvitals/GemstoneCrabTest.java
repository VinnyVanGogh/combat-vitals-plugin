package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class GemstoneCrabTest
{
	private static GemstoneCrab.Reading step(GemstoneCrab c, boolean crab, boolean shell, double f, long t,
		GemstoneCrab.Alerts a)
	{
		return c.step(crab, shell, f, t, a);
	}

	@Test
	public void fullBarIsTenMinutes()
	{
		assertEquals(GemstoneCrab.FIGHT_MS, GemstoneCrab.remaining(1.0, 0, 0));
		assertEquals(300_000L, GemstoneCrab.remaining(0.5, 0, 0));
		assertEquals(0L, GemstoneCrab.remaining(0, 0, 0));
	}

	@Test
	public void observedRateOverridesTheTenMinutes()
	{
		// 10% in 60s is 10 minutes in all, so 90% left is 9 minutes: the same as the wiki figure.
		assertEquals(540_000L, GemstoneCrab.remaining(0.9, 60_000, 0.1));
		// 20% in 60s is a faster bar: 80% left is 4 minutes.
		assertEquals(240_000L, GemstoneCrab.remaining(0.8, 60_000, 0.2));
		// Too short a span, or too little drop, is noise: fall back to the wiki's ten minutes.
		assertEquals(480_000L, GemstoneCrab.remaining(0.8, 10_000, 0.2));
		assertEquals(480_000L, GemstoneCrab.remaining(0.8, 60_000, 0.01));
	}

	@Test
	public void unreadableBarUsesTheClockAndSaysSo()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		final GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		step(c, true, false, -1, 0, a);
		final GemstoneCrab.Reading r = step(c, true, false, -1, 60_000, new GemstoneCrab.Alerts());
		assertEquals(GemstoneCrab.Source.CLOCK, r.source);
		assertEquals(540_000L, r.remainingMs);
		// The bar cannot be read, so the percent is the timer's: 9 of 10 minutes left.
		assertEquals(90, r.percent());
		assertTrue(r.percentEstimated());
	}

	@Test
	public void aReadableBarIsExactAndNeverMarkedAsAnEstimate()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		final GemstoneCrab.Reading r = step(c, true, false, 0.42, 0, new GemstoneCrab.Alerts());
		assertEquals(42, r.percent());
		assertFalse(r.percentEstimated());
	}

	@Test
	public void noCrabMeansNoPercent()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		final GemstoneCrab.Reading r = step(c, false, false, -1, 0, new GemstoneCrab.Alerts());
		assertEquals(-1, r.percent());
		assertFalse(r.percentEstimated());
	}

	@Test
	public void spawnAlertFiresOnceAndAgainForTheNextCrab()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		step(c, true, false, 1.0, 0, a);
		assertTrue(a.spawned);
		a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.99, 600, a);
		assertFalse(a.spawned);

		// It burrows, the shell goes, a new crab appears.
		step(c, false, true, -1, 10_000, new GemstoneCrab.Alerts());
		step(c, false, false, -1, 10_000 + GemstoneCrab.SHELL_MS + 1, new GemstoneCrab.Alerts());
		a = new GemstoneCrab.Alerts();
		step(c, true, false, 1.0, 200_000, a);
		assertTrue(a.spawned);
	}

	@Test
	public void burrowWarningFiresOnceAtTheWarnLevel()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.5, 0, a);
		assertFalse(a.aboutToBurrow);
		// 0.05 of the bar is 30 seconds.
		a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.05, 1000, a);
		assertTrue(a.aboutToBurrow);
		a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.04, 2000, a);
		assertFalse(a.aboutToBurrow);
	}

	@Test
	public void warnOfZeroNeverWarns()
	{
		final GemstoneCrab c = new GemstoneCrab(0);
		final GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.0, 0, a);
		assertFalse(a.aboutToBurrow);
	}

	@Test
	public void burrowIsTheCrabGoingAndGoNowFiresOnce()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		step(c, true, false, 0.1, 0, new GemstoneCrab.Alerts());
		GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		GemstoneCrab.Reading r = step(c, false, true, -1, 5000, a);
		assertEquals(GemstoneCrab.Phase.SHELL, r.phase);
		assertTrue(c.burrowed());
		assertTrue(a.goNow);
		assertEquals(GemstoneCrab.SHELL_MS, r.shellMs);
		a = new GemstoneCrab.Alerts();
		r = step(c, false, true, -1, 35_000, a);
		assertFalse(a.goNow);
		assertEquals(GemstoneCrab.SHELL_MS - 30_000, r.shellMs);
	}

	@Test
	public void crabLeavingSceneWithHpLeftIsNotABurrow()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.6, 0, a);
		a = new GemstoneCrab.Alerts();
		final GemstoneCrab.Reading r = step(c, false, false, -1, 5000, a);
		assertEquals(GemstoneCrab.Phase.NONE, r.phase);
		assertFalse(a.goNow);
		assertFalse(c.burrowed());
		// Same crab walks back: no second spawn alert, no go alert.
		a = new GemstoneCrab.Alerts();
		step(c, true, false, 0.59, 8000, a);
		assertFalse(a.spawned);
		assertFalse(a.goNow);
	}

	@Test
	public void crabGoneWithTimerRunOutIsNotABurrowUntilTheShellAppears()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		step(c, true, false, 0.01, 0, new GemstoneCrab.Alerts());
		GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		GemstoneCrab.Reading r = step(c, false, false, -1, 1000, a);
		assertEquals(GemstoneCrab.Phase.NONE, r.phase);
		assertFalse(a.goNow);
		a = new GemstoneCrab.Alerts();
		r = step(c, false, true, -1, 2000, a);
		assertEquals(GemstoneCrab.Phase.SHELL, r.phase);
		assertTrue(a.goNow);
	}

	@Test
	public void warningFiresButGoNeverWhileTheCrabIsUp()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		final GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		final GemstoneCrab.Reading r = step(c, true, false, 0.03, 0, a);
		assertTrue(a.aboutToBurrow);
		assertFalse(a.goNow);
		assertFalse(c.burrowed());
		assertEquals(GemstoneCrab.Phase.CRAB, r.phase);
	}

	@Test
	public void nothingAboutMeansNoReadingAndNoAlerts()
	{
		final GemstoneCrab c = new GemstoneCrab(30);
		final GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		final GemstoneCrab.Reading r = step(c, false, false, -1, 0, a);
		assertEquals(GemstoneCrab.Phase.NONE, r.phase);
		assertFalse(a.spawned || a.aboutToBurrow || a.goNow);
		assertFalse(c.active());
	}

	@Test
	public void clockFormat()
	{
		assertEquals("10:00", GemstoneCrab.clock(600_000));
		assertEquals("0:05", GemstoneCrab.clock(4_200));
		assertEquals("0:00", GemstoneCrab.clock(-5));
	}

	@Test
	public void teleportingOrChangingPlaneLeavesTheCrabArea()
	{
		assertFalse(GemstoneCrab.left(1000, 2000, 0, 1020, 1990, 0));
		assertFalse(GemstoneCrab.left(1000, 2000, 0, 1032, 2032, 0));
		assertTrue(GemstoneCrab.left(1000, 2000, 0, 1033, 2000, 0));
		assertTrue(GemstoneCrab.left(1000, 2000, 0, 3000, 3000, 0));
		assertTrue(GemstoneCrab.left(1000, 2000, 0, 1000, 2000, 1));
	}

	@Test
	public void onlyACaveNearTheCrabIsOne()
	{
		assertTrue(GemstoneCrab.nearCrab(1000, 2000, 0, 1012, 1988, 0));
		assertFalse(GemstoneCrab.nearCrab(1000, 2000, 0, 1013, 2000, 0));
		assertFalse(GemstoneCrab.nearCrab(1000, 2000, 0, 3000, 3000, 0));
		assertFalse(GemstoneCrab.nearCrab(1000, 2000, 0, 1000, 2000, 1));
	}

	@Test
	public void teleportingAwayClearsTheCardAndArrowAndReturningResumesOnTheNextSpawn()
	{
		final GemstoneCrabWatcher w = new GemstoneCrabWatcher(null, null);
		final WorldPoint crabSpot = new WorldPoint(1000, 2000, 0);
		final WorldPoint far = new WorldPoint(3200, 3200, 0);
		long t = 0;

		// Fighting the crab, then it burrows: the shell is up and so is the arrow.
		w.observe(true, false, 0.5, crabSpot, crabSpot, t += 600);
		assertEquals(GemstoneCrab.Phase.CRAB, w.latest().phase);
		w.observe(false, true, -1, crabSpot, crabSpot, t += 600);
		assertEquals(GemstoneCrab.Phase.SHELL, w.latest().phase);
		assertTrue(w.arrowUp());

		// Teleport away mid-burrow: the card and the arrow are gone, nothing is due.
		assertNull(w.observe(false, false, -1, null, far, t += 600));
		assertNull(w.latest());
		assertFalse(w.arrowUp());

		// Still away, nothing seen: it stays cleared.
		w.observe(false, false, -1, null, far, t += 600);
		assertFalse(w.arrowUp());

		// Back at the crab: nothing from before returns, and the next spawn shows again.
		w.observe(false, false, -1, null, crabSpot, t += 600);
		assertFalse(w.arrowUp());
		w.observe(true, false, 1.0, crabSpot, crabSpot, t += 600);
		assertEquals(GemstoneCrab.Phase.CRAB, w.latest().phase);
		w.observe(false, true, -1, crabSpot, crabSpot, t += 600);
		assertEquals(GemstoneCrab.Phase.SHELL, w.latest().phase);
		assertTrue(w.arrowUp());
	}
}
