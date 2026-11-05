package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Bosses and high-HP NPCs can show a different display from everything else
 * (group member feedback via the Board, 2026-11-04: "%hp on bosses only ...
 * hitpoints only for everything else").
 */
public class BigTargetsTest
{
	private static final int N = 200;

	@Test
	public void bossViaTheGamesHudIsBig()
	{
		assertTrue(BigTargets.isBoss(true, "Some new boss"));
		assertTrue(BigTargets.isBig(BigTargetRule.BOSSES, true, -1, N));
	}

	@Test
	public void bossViaTheBundledListIsBigWithoutTheHud()
	{
		assertTrue(BigTargets.isBoss(false, "Vorkath"));
		assertTrue(BigTargets.isBoss(false, "general graardor"));
		assertTrue(BigTargets.isBoss(false, "Kree'arra"));
		assertFalse(BigTargets.isBoss(false, "Rock Crab"));
		assertFalse(BigTargets.isBoss(false, null));
	}

	@Test
	public void thresholdIsInclusiveAtN()
	{
		assertFalse(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, false, N - 1, N));
		assertTrue(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, false, N, N));
		assertTrue(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, false, N + 1, N));
		assertFalse(BigTargets.isBig(BigTargetRule.HP_ONLY, false, N - 1, N));
		assertTrue(BigTargets.isBig(BigTargetRule.HP_ONLY, false, N, N));
		assertTrue(BigTargets.isBig(BigTargetRule.HP_ONLY, false, N + 1, N));
	}

	@Test
	public void unknownMaxHpFallsToEverythingElse()
	{
		assertFalse(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, false, -1, N));
		assertFalse(BigTargets.isBig(BigTargetRule.HP_ONLY, false, -1, N));
		assertFalse(BigTargets.isBig(BigTargetRule.HP_ONLY, false, 0, N));
	}

	@Test
	public void bossesOnlyIgnoresMaxHp()
	{
		assertFalse(BigTargets.isBig(BigTargetRule.BOSSES, false, 5000, N));
		assertTrue(BigTargets.isBig(BigTargetRule.BOSSES, true, 10, N));
	}

	@Test
	public void bossesAndHpTakesEither()
	{
		assertTrue(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, true, 10, N));
		assertTrue(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, false, 250, N));
		assertFalse(BigTargets.isBig(BigTargetRule.BOSSES_AND_HP, false, 10, N));
	}

	@Test
	public void hpOnlyIgnoresBossStatus()
	{
		assertFalse(BigTargets.isBig(BigTargetRule.HP_ONLY, true, 10, N));
		assertTrue(BigTargets.isBig(BigTargetRule.HP_ONLY, true, 250, N));
	}

	@Test
	public void sameAsDisplayKeepsTheMainSetting()
	{
		for (DisplayMode mode : DisplayMode.values())
		{
			assertEquals(mode, TargetDisplay.SAME.resolve(mode));
		}
		assertEquals(DisplayMode.PERCENTAGE, TargetDisplay.PERCENTAGE.resolve(DisplayMode.HITPOINTS));
		assertEquals(DisplayMode.HITPOINTS, TargetDisplay.HITPOINTS.resolve(DisplayMode.BOTH));
		assertEquals(DisplayMode.BOTH, TargetDisplay.BOTH.resolve(DisplayMode.PERCENTAGE));
	}

	@Test
	public void eachSideGetsItsOwnChoice()
	{
		assertEquals(DisplayMode.PERCENTAGE,
			BigTargets.mode(true, TargetDisplay.PERCENTAGE, TargetDisplay.HITPOINTS, DisplayMode.BOTH));
		assertEquals(DisplayMode.HITPOINTS,
			BigTargets.mode(false, TargetDisplay.PERCENTAGE, TargetDisplay.HITPOINTS, DisplayMode.BOTH));
	}

	@Test
	public void defaultsReproduceTodaysDisplayForAnExistingProfile()
	{
		final CombatVitalsConfig profile = new CombatVitalsConfig()
		{
		};
		assertEquals(TargetDisplay.SAME, profile.bigTargetDisplay());
		assertEquals(TargetDisplay.SAME, profile.otherTargetDisplay());
		assertEquals(BigTargetRule.BOSSES_AND_HP, profile.bigTargetRule());
		assertEquals(200, profile.bigTargetMaxHp());
		for (DisplayMode saved : DisplayMode.values())
		{
			for (boolean big : new boolean[]{true, false})
			{
				assertEquals(saved, BigTargets.mode(big, profile.bigTargetDisplay(), profile.otherTargetDisplay(), saved));
			}
		}
	}
}
