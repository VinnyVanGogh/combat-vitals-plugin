package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The accuracy maths against the OSRS Wiki's "Damage per second/Magic" steps
 * (https://oldschool.runescape.wiki/w/Damage_per_second/Magic). Each expected
 * number is worked by hand from the wiki's formulas in the comment above it.
 */
public class SplashMathTest
{
	private static final double EPSILON = 1e-4;

	@Test
	public void effectiveLevelIsLevelTimesPrayerRoundedDownPlusNine()
	{
		assertEquals(108, SplashMath.effectiveLevel(99, 1.0));
		// 99 * 1.25 = 123.75, rounded down to 123, + 9.
		assertEquals(132, SplashMath.effectiveLevel(99, 1.25));
		// 55 * 1.05 = 57.75, rounded down to 57, + 9.
		assertEquals(66, SplashMath.effectiveLevel(55, 1.05));
	}

	@Test
	public void accuracyRollIsEffectiveLevelTimesBonusPlus64()
	{
		assertEquals(108 * 64, SplashMath.accuracyRoll(108, 0));
		assertEquals(132 * 164, SplashMath.accuracyRoll(132, 100));
		assertEquals(0, SplashMath.accuracyRoll(108, -64));
		assertEquals(-108, SplashMath.accuracyRoll(108, -65));
	}

	@Test
	public void defenceRollIsNpcLevelPlusNineTimesDefencePlus64()
	{
		// Mithril dragon on the wiki: Magic 168, magic defence +30: (9 + 168) * (30 + 64).
		assertEquals(177 * 94, SplashMath.defenceRoll(168, 30));
		assertEquals(0, SplashMath.defenceRoll(1, -70));
	}

	@Test
	public void higherAccuracyThanDefenceUsesTheFirstFormula()
	{
		// Acc 6912 (99 Magic, +0), Def 640 (Magic 1, +0): 1 - (640 + 2) / (2 * (6912 + 1)) = 1 - 642 / 13826.
		assertEquals(0.953564, SplashMath.hitChance(99, 1.0, 0, 1, 0), EPSILON);
	}

	@Test
	public void equalRollsUseTheSecondFormula()
	{
		// Acc 6912, Def (9 + 99) * 64 = 6912: not greater, so 6912 / (2 * 6913).
		assertEquals(0.499928, SplashMath.hitChance(99, 1.0, 0, 99, 0), EPSILON);
	}

	@Test
	public void lowerAccuracyThanDefenceUsesTheSecondFormula()
	{
		// Acc 6912, Def 16638 (Mithril dragon): 6912 / (2 * 16639).
		assertEquals(0.207704, SplashMath.hitChance(99, 1.0, 0, 168, 30), EPSILON);
	}

	@Test
	public void augurySetupAgainstAWeakTarget()
	{
		// Acc 132 * 164 = 21648, Def 640: 1 - 642 / (2 * 21649).
		assertEquals(0.985173, SplashMath.hitChance(99, 1.25, 100, 1, 0), EPSILON);
	}

	@Test
	public void aBonusOfMinus64OrLowerNeverHits()
	{
		assertEquals(0, SplashMath.hitChance(99, 1.25, -64, 1, 0), 0);
		assertEquals(0, SplashMath.hitChance(99, 1.25, -65, 1, 0), 0);
		assertEquals(0, SplashMath.hitChance(99, 1.25, -200, 1, -64), 0);
	}

	@Test
	public void justAboveTheSplashBonusIsASmallChance()
	{
		// Bonus -63: acc 108 * 1 = 108, Def 640: 108 / (2 * 641).
		assertEquals(0.084243, SplashMath.hitChance(99, 1.0, -63, 1, 0), EPSILON);
	}

	@Test
	public void splashesFromMinus64()
	{
		assertTrue(SplashMath.splashes(-64));
		assertTrue(SplashMath.splashes(-120));
		assertFalse(SplashMath.splashes(-63));
		assertFalse(SplashMath.splashes(0));
	}

	@Test
	public void aTargetWithNoDefenceRollIsHitAlmostAlways()
	{
		// Def 0: 1 - 2 / (2 * (acc + 1)) never divides by zero.
		assertEquals(1 - 1d / 6913, SplashMath.hitChance(6912, 0), 1e-9);
	}
}
