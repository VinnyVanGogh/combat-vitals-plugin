package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SplashAutoTest
{
	@Test
	public void switchesOnAtTheThresholdAndOffAboveIt()
	{
		final SplashAuto auto = new SplashAuto();
		assertEquals(Boolean.TRUE, auto.step(-40, -40, false));
		assertNull(auto.step(-40, -40, true));
		assertEquals(Boolean.FALSE, auto.step(-39, -40, true));
		assertNull(auto.step(-39, -40, false));
	}

	@Test
	public void aManualSwitchWinsUntilTheGearCrossesTheThreshold()
	{
		final SplashAuto auto = new SplashAuto();
		assertEquals(Boolean.TRUE, auto.step(-60, -40, false));
		// The player turns Splashing off by hand while the gear still says splash.
		auto.manualSwitch();
		assertNull(auto.step(-60, -40, false));
		assertNull(auto.step(-70, -40, false));
		// Gear changes to a normal staff, then back to splash gear: auto is in charge again.
		assertNull(auto.step(10, -40, false));
		assertEquals(Boolean.TRUE, auto.step(-60, -40, false));
	}

	@Test
	public void aSwitchMadeBeforeTheFirstLookIsKept()
	{
		final SplashAuto auto = new SplashAuto();
		auto.manualSwitch();
		assertNull(auto.step(-60, -40, false));
	}

	@Test
	public void theThresholdIsConfigurable()
	{
		final SplashAuto auto = new SplashAuto();
		assertEquals(Boolean.TRUE, auto.step(-20, -10, false));
	}
}
