package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import net.runelite.api.Point;
import org.junit.Test;

/**
 * The overhead overlay asks the client where each actor's text goes. An NPC
 * being removed after a kill can throw from inside that call (the Board's
 * in-game test, 2026-11-04); the frame must skip it, not fail.
 */
public class OverheadOnScreenTest
{
	@Test
	public void aLocationIsPassedThrough()
	{
		final Point at = new Point(10, 20);

		assertSame(at, CombatVitalsOverheadOverlay.onScreen(() -> at));
	}

	@Test
	public void offScreenStaysNull()
	{
		assertNull(CombatVitalsOverheadOverlay.onScreen(() -> null));
	}

	@Test
	public void anActorTheClientIsRemovingIsSkippedNotThrown()
	{
		assertNull(CombatVitalsOverheadOverlay.onScreen(() ->
		{
			throw new NullPointerException("model gone mid-frame");
		}));
	}

	@Test
	public void anyClientRuntimeExceptionIsSkipped()
	{
		assertNull(CombatVitalsOverheadOverlay.onScreen(() ->
		{
			throw new ArrayIndexOutOfBoundsException(-1);
		}));
	}
}
