package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CaveArrowTest
{
	@Test
	public void notShownWhileTheCrabIsUp()
	{
		final CaveArrow a = new CaveArrow();
		assertFalse(a.update(true, false, 100, 100, 0));
		assertFalse(a.update(false, false, 100, 100, 0));
	}

	@Test
	public void burrowShowsItAndItStaysWithNoTimeout()
	{
		final CaveArrow a = new CaveArrow();
		assertTrue(a.update(false, true, 100, 100, 0));
		for (int i = 0; i < 10_000; i++)
		{
			assertTrue(a.update(false, true, 101, 100, 0));
		}
		// Even once the shell is out of the scene, it stays until used.
		assertTrue(a.update(false, false, 102, 101, 0));
	}

	@Test
	public void goingThroughTheCaveHidesItAndItDoesNotComeBack()
	{
		final CaveArrow a = new CaveArrow();
		a.update(false, true, 100, 100, 0);
		assertFalse(a.update(false, true, 140, 160, 0));
		assertFalse(a.update(false, true, 140, 161, 0));
		// A new shell later is a new burrow.
		a.update(false, false, 140, 161, 0);
		assertTrue(a.update(false, true, 140, 161, 0));
	}

	@Test
	public void changingPlaneCountsAsThrough()
	{
		final CaveArrow a = new CaveArrow();
		a.update(false, true, 100, 100, 0);
		assertFalse(a.update(false, true, 100, 100, 1));
	}

	@Test
	public void nextCrabSpawnHidesIt()
	{
		final CaveArrow a = new CaveArrow();
		a.update(false, true, 100, 100, 0);
		assertFalse(a.update(true, false, 101, 100, 0));
		assertFalse(a.shown());
	}
}
