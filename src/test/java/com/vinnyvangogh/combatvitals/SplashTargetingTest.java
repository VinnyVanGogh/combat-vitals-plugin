package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import org.junit.Test;

/** Splash only follows an NPC that was attacked, the toggle exists, and the first attack stays cheap. */
public class SplashTargetingTest
{
	@Test
	public void attackSelectsTheNpc()
	{
		assertEquals(7, SplashWatcher.attackClick("Attack", 7, -1));
		assertEquals(7, SplashWatcher.attackClick("attack", 7, 3));
	}

	@Test
	public void talkingToOrPickpocketingTheTargetClearsIt()
	{
		assertEquals(-1, SplashWatcher.attackClick("Talk-to", 7, 7));
		assertEquals(-1, SplashWatcher.attackClick("Pickpocket", 7, 7));
	}

	@Test
	public void interactingWithAnotherNpcLeavesTheAttackTarget()
	{
		assertEquals(3, SplashWatcher.attackClick("Talk-to", 7, 3));
		assertEquals(-1, SplashWatcher.attackClick("Pickpocket", 7, -1));
	}

	@Test
	public void splashModeHasASidebarToggleOutsideAllOn()
	{
		final OverlayToggles.Toggle t = OverlayToggles.find("splashMode");
		assertTrue(t.extra);
		assertFalse(t.defaultOn);
	}

	@Test
	public void menuOptionShowsTheState()
	{
		assertEquals("Splashing mode: on", SplashOverlay.modeOption(true));
		assertEquals("Splashing mode: off", SplashOverlay.modeOption(false));
	}

	@Test
	public void firstAttackPathStaysUnderTwoMillisecondsAfterWarmUp()
	{
		final SplashWatcher watcher = new SplashWatcher(null, null, null);
		watcher.warm();
		final long start = System.nanoTime();
		final SplashTracker tracker = new SplashTracker(Collections.emptyMap());
		final SplashTracker.Result r = tracker.step(
			new SplashTracker.Input(5, 9, 2, 3, "Sand Crab", 20, 60, 1.0), 10);
		final int[] cost = SpellCasts.cost("Fire Strike");
		final SpellCasts.Supply supply = new SpellCasts.Supply();
		SplashReadout.castsText(SpellCasts.casts(cost, supply));
		final double ms = (System.nanoTime() - start) / 1e6;
		System.out.printf("first attack path: %.3f ms%n", ms);
		assertTrue(r.chanceLine == null);
		assertTrue("first attack took " + ms + " ms", ms < 2.0);
	}
}
