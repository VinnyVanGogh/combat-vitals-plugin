package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The pillar fill glides to a new value rather than jumping, and never lags a reset. */
public class PillarAnimationTest
{
	@Test
	public void theFirstValueIsShownAtOnce()
	{
		final PillarAnimation a = new PillarAnimation();
		assertEquals(0.4, a.next(0.4, 1_000), 0);
	}

	@Test
	public void aRiseGlidesThereWithinTheDuration()
	{
		final PillarAnimation a = new PillarAnimation();
		a.next(0.2, 0);
		final double early = a.next(0.6, 50);
		assertTrue("moved: " + early, early > 0.2);
		assertTrue("not there yet: " + early, early < 0.6);
		final double later = a.next(0.6, 150);
		assertTrue("closer: " + later, later > early);
		assertEquals(0.6, a.next(0.6, 50 + PillarAnimation.DURATION_MILLIS * 3), 0);
	}

	@Test
	public void aSmallFallGlidesToo()
	{
		final PillarAnimation a = new PillarAnimation();
		a.next(0.6, 0);
		final double v = a.next(0.5, 50);
		assertTrue(v < 0.6 && v > 0.5);
	}

	@Test
	public void aLevelUpOrABankEmptyingSnaps()
	{
		final PillarAnimation a = new PillarAnimation();
		a.next(0.98, 0);
		assertEquals(0.03, a.next(0.03, 16), 0);
	}

	@Test
	public void aLongPauseSnaps()
	{
		final PillarAnimation a = new PillarAnimation();
		a.next(0.1, 0);
		assertEquals(0.9, a.next(0.9, 60_000), 0);
	}

	@Test
	public void resetForgetsTheLastValue()
	{
		final PillarAnimation a = new PillarAnimation();
		a.next(0.1, 0);
		a.reset();
		assertEquals(0.7, a.next(0.7, 10), 0);
	}
}
