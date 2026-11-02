package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SkillingTextTest
{
	private static final SkillingState.Warning NONE = SkillingState.Warning.NONE;

	@Test
	public void noBoostShowsTheLevelOnly()
	{
		assertEquals("Fishing 76", SkillingText.text("Fishing", 76, 76, 45, true, -1, null, NONE));
	}

	@Test
	public void boostShowsPlusNAndATimer()
	{
		assertEquals("Fishing 79 (+3) 45s", SkillingText.text("Fishing", 76, 79, 45, false, -1, null, NONE));
	}

	@Test
	public void loginAnchoredTimerIsMarkedAsAnEstimate()
	{
		assertEquals("Fishing 79 (+3) ~45s", SkillingText.text("Fishing", 76, 79, 45, true, -1, null, NONE));
	}

	@Test
	public void unknownTimerIsOmitted()
	{
		assertEquals("Fishing 79 (+3)", SkillingText.text("Fishing", 76, 79, -1, false, -1, null, NONE));
	}

	@Test
	public void specToolShowsSpecEnergy()
	{
		assertEquals("Fishing 76 | Spec 100%", SkillingText.text("Fishing", 76, 76, -1, false, 100, null, NONE));
		assertEquals("Fishing 79 (+3) 45s | Spec 0% | Dragon harpoon special",
			SkillingText.text("Fishing", 76, 79, 45, false, 0, "Dragon harpoon special", NONE));
	}

	@Test
	public void sourceIsNamed()
	{
		assertEquals("Fishing 79 (+3) 45s | Fishing potion",
			SkillingText.text("Fishing", 76, 79, 45, false, -1, "Fishing potion", NONE));
	}

	@Test
	public void sourceIsNotShownWithoutABoost()
	{
		assertEquals("Fishing 76", SkillingText.text("Fishing", 76, 76, -1, false, -1, "Fishing potion", NONE));
	}

	@Test
	public void dropWarningReplacesTheTimer()
	{
		assertEquals("Fishing 78 (+2) dropped",
			SkillingText.text("Fishing", 76, 78, 100, false, -1, null, SkillingState.Warning.DROPPED));
	}

	@Test
	public void expiryWarningReplacesTheBoost()
	{
		assertEquals("Fishing 76 boost ended",
			SkillingText.text("Fishing", 76, 76, 100, false, -1, "Fishing potion", SkillingState.Warning.EXPIRED));
	}

	@Test
	public void drainedStatShowsMinusAndNoTimer()
	{
		assertEquals("Fishing 74 (-2)", SkillingText.text("Fishing", 76, 74, 45, false, -1, null, NONE));
	}

	@Test
	public void overflowCountsTheFishBeyondTheIcons()
	{
		assertEquals(null, SkillingText.overflow(0, 3));
		assertEquals(null, SkillingText.overflow(3, 3));
		assertEquals("+1", SkillingText.overflow(4, 3));
		assertEquals("+2", SkillingText.overflow(5, 3));
	}

	@Test
	public void secondsCountDownBetweenTicks()
	{
		assertEquals(60, SkillingText.seconds(100, 0));
		assertEquals(60, SkillingText.seconds(100, 300));
		assertEquals(59, SkillingText.seconds(100, 1000));
		assertEquals(1, SkillingText.seconds(1, 0));
		assertEquals(1, SkillingText.seconds(1, 599));
		assertEquals(0, SkillingText.seconds(0, 0));
		assertEquals(-1, SkillingText.seconds(-1, 0));
	}
}
