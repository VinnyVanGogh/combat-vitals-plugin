package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Board's in-game test, 2026-11-06: after walking away the badge took the
 * whole linger (the 5 s timeout, about 8 ticks) to read "Not fishing". It
 * must switch within a tick or two of the player clearly stopping, and never
 * flicker while fishing.
 */
public class SkillingStopTest
{
	private static final long SPOT_SIDE = SpotPathing.tile(3000, 3000, 0);
	private static final long ONE_STEP = SpotPathing.tile(3001, 3000, 0);
	private static final long TWO_STEPS = SpotPathing.tile(3002, 3000, 0);

	private SkillingStop stop;

	@Before
	public void setUp()
	{
		stop = new SkillingStop();
	}

	/** Ticks until {@link SkillingStop#tick} says stopped, at most {@code limit}; -1 if it never does. */
	private int ticksUntilStopped(int limit, boolean working, long[] tiles, boolean animating, boolean fishing)
	{
		for (int i = 0; i < limit; i++)
		{
			if (stop.tick(working, tiles[Math.min(i, tiles.length - 1)], animating, fishing))
			{
				return i + 1;
			}
		}
		return -1;
	}

	@Test
	public void fishingThroughAnimationGapsNeverStops()
	{
		// On the spot the whole time; the animation drops out between catches.
		for (int i = 0; i < 200; i++)
		{
			assertFalse("tick " + i, stop.tick(true, SPOT_SIDE, i % 5 != 0, true));
		}
	}

	@Test
	public void walkingOffStopsOnTheFirstStep()
	{
		stop.tick(true, SPOT_SIDE, true, true);

		// The walk click ends the interaction; the next tick the player is a tile on.
		assertEquals(1, ticksUntilStopped(10, false, new long[]{ONE_STEP, TWO_STEPS}, false, true));
	}

	@Test
	public void walkingOffAPickpocketTargetStopsToo()
	{
		stop.tick(true, SPOT_SIDE, true, false);

		assertEquals(1, ticksUntilStopped(10, false, new long[]{ONE_STEP}, false, false));
	}

	@Test
	public void theInteractionEndingWhileStandingStillStopsFishingInTwoTicks()
	{
		stop.tick(true, SPOT_SIDE, true, true);

		// The spot moved or the inventory filled: the player stands there.
		assertEquals(SkillingStop.STILL_TICKS,
			ticksUntilStopped(10, false, new long[]{SPOT_SIDE}, false, true));
	}

	@Test
	public void pickpocketingBetweenAttemptsKeepsTheLinger()
	{
		stop.tick(true, SPOT_SIDE, true, false);

		// Each attempt ends the interaction and the player stands still: not stopped here,
		// the ordinary linger decides.
		assertEquals(-1, ticksUntilStopped(20, false, new long[]{SPOT_SIDE}, false, false));
	}

	@Test
	public void walkingToTheNextSpotAfterClickingItIsNotStopping()
	{
		stop.tick(true, SPOT_SIDE, true, true);

		// The click on the new spot is pending until its interaction arrives.
		assertEquals(-1, ticksUntilStopped(10, true, new long[]{ONE_STEP, TWO_STEPS}, false, true));
	}

	@Test
	public void walkingUpToTheSpotWhileOnItIsNotStopping()
	{
		assertFalse(stop.tick(true, TWO_STEPS, false, true));
		assertFalse(stop.tick(true, ONE_STEP, false, true));
		assertFalse(stop.tick(true, SPOT_SIDE, true, true));
	}

	@Test
	public void stillAnimatingAfterTheInteractionEndsIsNotStopped()
	{
		stop.tick(true, SPOT_SIDE, true, true);

		assertEquals(-1, ticksUntilStopped(10, false, new long[]{SPOT_SIDE}, true, true));
	}

	@Test
	public void anUnreadTileIsNotAMove()
	{
		stop.tick(true, SPOT_SIDE, true, true);

		assertFalse(stop.tick(false, SkillingStop.UNKNOWN, true, true));
		assertFalse("a tile read again after an unknown one is not compared with it",
			stop.tick(false, SPOT_SIDE, true, true));
	}

	@Test
	public void resetForgetsTheLastTile()
	{
		stop.tick(true, SPOT_SIDE, true, true);
		stop.reset();

		assertFalse("the first tile after a reset is not a move", stop.tick(false, TWO_STEPS, true, true));
	}

	@Test
	public void goingBackToWorkResetsTheStillCount()
	{
		stop.tick(true, SPOT_SIDE, true, true);
		assertFalse(stop.tick(false, SPOT_SIDE, false, true));
		assertFalse("back on the spot", stop.tick(true, SPOT_SIDE, false, true));
		assertFalse("the count starts again", stop.tick(false, SPOT_SIDE, false, true));
		assertTrue(stop.tick(false, SPOT_SIDE, false, true));
	}
}
