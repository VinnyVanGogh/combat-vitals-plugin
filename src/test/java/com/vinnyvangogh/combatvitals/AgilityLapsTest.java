package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class AgilityLapsTest
{
	private AgilityLaps laps;

	@Before
	public void setUp()
	{
		laps = new AgilityLaps();
	}

	@Test
	public void nothingBeforeTheFirstLap()
	{
		assertNull(laps.label());
	}

	@Test
	public void theLapMessageCountsALap()
	{
		assertTrue(laps.message("Your Seers' Village Rooftop lap count is: 1,204.", 0));
		assertEquals("1 lap", laps.label());
	}

	@Test
	public void otherMessagesAreNotLaps()
	{
		assertFalse(laps.message("Your Zulrah kill count is: 12.", 0));
		assertFalse(laps.message("You gain some experience.", 0));
		assertNull(laps.label());
	}

	@Test
	public void theSecondLapTimesTheFirst()
	{
		laps.message("Your Canifis Rooftop lap count is: 5.", 10_000);
		laps.message("Your Canifis Rooftop lap count is: 6.", 68_000);
		assertEquals("2 laps, last 58s", laps.label());
	}

	@Test
	public void marksOfGracePickedUpAreCounted()
	{
		laps.marksHeld(3);
		laps.marksHeld(4);
		laps.marksHeld(6);
		assertEquals("3 marks", laps.label());
		laps.message("Your Canifis Rooftop lap count is: 5.", 0);
		assertEquals("1 lap, 3 marks", laps.label());
	}

	@Test
	public void theFirstMarksSeenAndSpendingThemAreNotPickups()
	{
		laps.marksHeld(40);
		laps.marksHeld(0);
		laps.marksHeld(1);
		assertEquals("1 mark", laps.label());
	}

	@Test
	public void clearStartsAgain()
	{
		laps.message("Your Canifis Rooftop lap count is: 5.", 0);
		laps.clear();
		assertNull(laps.label());
	}
}
