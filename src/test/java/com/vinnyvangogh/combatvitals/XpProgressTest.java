package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import net.runelite.api.Experience;
import org.junit.Test;

public class XpProgressTest
{
	private static final double EPSILON = 1e-9;

	@Test
	public void freshSkillIsLevelOneWithNothingDone()
	{
		final XpProgress p = XpProgress.of(0);
		assertEquals(1, p.getLevel());
		assertEquals(2, p.getNextLevel());
		assertEquals(0.0, p.getFraction(), EPSILON);
		assertEquals(83, p.getXpLeft());
		assertEquals("Lv 1 -> 2: 0% (83 xp)", p.text());
	}

	@Test
	public void exactlyOnALevelBoundaryStartsTheNextLevelAtZero()
	{
		final int lv13 = Experience.getXpForLevel(13);
		final XpProgress p = XpProgress.of(lv13);
		assertEquals(13, p.getLevel());
		assertEquals(0.0, p.getFraction(), EPSILON);
		assertEquals(Experience.getXpForLevel(14) - lv13, p.getXpLeft());
	}

	@Test
	public void oneXpShortOfALevelIsNeverShownAsAHundredPercent()
	{
		final int lv13 = Experience.getXpForLevel(13);
		final XpProgress p = XpProgress.of(lv13 - 1);
		assertEquals(12, p.getLevel());
		assertEquals(1, p.getXpLeft());
		assertEquals("Lv 12 -> 13: 99% (1 xp)", p.text());
	}

	@Test
	public void midLevelFractionIsTheShareOfThisLevelsXp()
	{
		final int from = Experience.getXpForLevel(12);
		final int to = Experience.getXpForLevel(13);
		final int xp = from + (to - from) / 2;
		final XpProgress p = XpProgress.of(xp);
		assertEquals(12, p.getLevel());
		assertEquals((double) (xp - from) / (to - from), p.getFraction(), EPSILON);
	}

	@Test
	public void largeXpLeftIsGroupedForReading()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(80));
		assertEquals("Lv 80 -> 81: 0% (" + String.format("%,d", p.getXpLeft()) + " xp)", p.text());
	}

	@Test
	public void ninetyNineCountsTowardTwoHundredMillion()
	{
		final int lv99 = Experience.getXpForLevel(Experience.MAX_REAL_LEVEL);
		final XpProgress p = XpProgress.of(lv99);
		assertEquals(99, p.getLevel());
		assertEquals(-1, p.getNextLevel());
		assertEquals(0.0, p.getFraction(), EPSILON);
		assertEquals(Experience.MAX_SKILL_XP - lv99, p.getXpLeft());
		assertEquals("Lv 99 -> 200m: 0%", p.text());
	}

	@Test
	public void virtualLevelsStillReadAsNinetyNine()
	{
		// 50m xp is virtual level 110+; the badge's real level stops at 99.
		final XpProgress p = XpProgress.of(50_000_000);
		assertEquals(99, p.getLevel());
		assertEquals("Lv 99 -> 200m: 19%", p.text());
	}

	@Test
	public void twoHundredMillionIsFullAndSaysSo()
	{
		final XpProgress p = XpProgress.of(Experience.MAX_SKILL_XP);
		assertEquals(1.0, p.getFraction(), EPSILON);
		assertEquals(0, p.getXpLeft());
		assertEquals("Lv 99: 200m xp", p.text());
	}

	@Test
	public void negativeXpIsTreatedAsNone()
	{
		assertEquals(1, XpProgress.of(-5).getLevel());
		assertEquals(0.0, XpProgress.of(-5).getFraction(), EPSILON);
	}
}
