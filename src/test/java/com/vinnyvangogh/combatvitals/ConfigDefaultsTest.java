package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * The two hide options default on (Board, 2026-11-04). A new profile has no
 * stored value, so the interface default is what it gets. An existing user's
 * saved value is stored under the key name and wins over any default, so the
 * key names must not change: renaming one would silently reset it.
 */
public class ConfigDefaultsTest
{
	private static final CombatVitalsConfig NEW_PROFILE = new CombatVitalsConfig()
	{
	};

	@Test
	public void newProfileHidesTheGamesChatUnderTheReadout()
	{
		assertTrue(NEW_PROFILE.hideGameOverheadChat());
	}

	@Test
	public void newProfileHidesTheGamesHealthBar()
	{
		assertTrue(NEW_PROFILE.hideGameHealthBar());
	}

	@Test
	public void newProfileNeverPathsToTheNextFishingSpot()
	{
		assertEquals(FishingSpotPath.OFF, NEW_PROFILE.fishingSpotPath());
	}

	@Test
	public void savedValuesKeepTheirKeys() throws NoSuchMethodException
	{
		assertEquals("hideGameOverheadChat", keyName("hideGameOverheadChat"));
		assertEquals("hideGameHealthBar", keyName("hideGameHealthBar"));
		assertEquals("fishingSpotPath", keyName("fishingSpotPath"));
	}

	private static String keyName(String method) throws NoSuchMethodException
	{
		return CombatVitalsConfig.class.getMethod(method).getAnnotation(ConfigItem.class).keyName();
	}
}
