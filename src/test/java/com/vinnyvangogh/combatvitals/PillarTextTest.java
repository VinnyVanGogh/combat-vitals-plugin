package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import net.runelite.api.Experience;
import org.junit.Test;

/** What each pillar says: the number under its icon, its hover text, its tick marks and its colours. */
public class PillarTextTest
{
	/** 82% of the way from 16 to 17, 65 xp short is not 82%, so pick the XP to make both true. */
	private static XpProgress fishing16()
	{
		final int from = Experience.getXpForLevel(16);
		final int to = Experience.getXpForLevel(17);
		return XpProgress.of(from + (int) Math.ceil((to - from) * 0.82));
	}

	@Test
	public void xpNumberFollowsTheXpDisplaySetting()
	{
		final XpProgress p = fishing16();
		assertEquals("82%", PillarText.xpNumber(p, XpDisplay.PERCENT));
		assertEquals("82%", PillarText.xpNumber(p, XpDisplay.BOTH));
		assertEquals(Integer.toString(p.getXpLeft()), PillarText.xpNumber(p, XpDisplay.XP_LEFT));
	}

	/**
	 * Board, 2026-11-09: every skill's XP pillar reads a percent, "3%" and
	 * "82%" alike. The screenshots of 2026-11-06 showed "3%" beside "82"
	 * because only a wide percentage lost its sign when drawn.
	 */
	@Test
	public void xpPercentKeepsItsSignAtEveryValue()
	{
		for (int level : new int[]{2, 16, 40, 60, 98})
		{
			final int from = Experience.getXpForLevel(level);
			final int to = Experience.getXpForLevel(level + 1);
			for (double share : new double[]{0.03, 0.5, 0.82})
			{
				final String number = PillarText.xpNumber(XpProgress.of(from + (int) ((to - from) * share)),
					XpDisplay.PERCENT);
				assertTrue(number, number.matches("\\d{1,2}%"));
			}
		}
	}

	@Test
	public void xpLeftIsShortenedToFitThePillar()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(60));
		assertEquals(PillarText.compact(p.getXpLeft()), PillarText.xpNumber(p, XpDisplay.XP_LEFT));
		assertTrue(PillarText.xpNumber(p, XpDisplay.XP_LEFT).length() <= 4);
	}

	@Test
	public void compactNumbersAreFourCharactersAtMost()
	{
		assertEquals("65", PillarText.compact(65));
		assertEquals("1.2k", PillarText.compact(1_234));
		assertEquals("10k", PillarText.compact(9_999));
		assertEquals("29k", PillarText.compact(28_546));
		assertEquals("999k", PillarText.compact(999_400));
		assertEquals("1m", PillarText.compact(999_600));
		assertEquals("1.2m", PillarText.compact(1_234_567));
		assertEquals("13m", PillarText.compact(13_034_431));
		assertEquals("200m", PillarText.compact(200_000_000));
		for (long n = 1; n <= 200_000_000; n = n * 3 + 7)
		{
			assertTrue(n + " -> " + PillarText.compact(n), PillarText.compact(n).length() <= 4);
		}
	}

	@Test
	public void xpNumberAt200mIsMax()
	{
		assertEquals("Max", PillarText.xpNumber(XpProgress.of(Experience.MAX_SKILL_XP), XpDisplay.PERCENT));
		assertEquals("Max", PillarText.xpNumber(XpProgress.of(Experience.MAX_SKILL_XP), XpDisplay.XP_LEFT));
	}

	@Test
	public void xpLevelBoundaries()
	{
		assertEquals("16", PillarText.xpBottom(fishing16()));
		assertEquals("99", PillarText.xpBottom(XpProgress.of(Experience.getXpForLevel(99))));
	}

	@Test
	public void inventoryNumberIsFreeOrUsed()
	{
		assertEquals("18", PillarText.inventoryNumber(18, InventoryCount.FREE));
		assertEquals("10", PillarText.inventoryNumber(18, InventoryCount.USED));
		assertEquals("0", PillarText.inventoryNumber(0, InventoryCount.FREE));
		assertNull(PillarText.inventoryNumber(-1, InventoryCount.FREE));
	}

	@Test
	public void targetNumberIsItsHitpointsOrElseThePercent()
	{
		assertEquals("12", PillarText.targetNumber(12, 0.24));
		assertEquals("0", PillarText.targetNumber(0, 0));
		assertEquals("82%", PillarText.targetNumber(-1, 0.82));
	}

	@Test
	public void targetHitpointsExactOrUnknown()
	{
		assertEquals(12, PillarText.targetHp(12, null, 50));
		assertEquals(-1, PillarText.targetHp(-1, null, 50));
		assertEquals(-1, PillarText.targetHp(-1, null, 0));
	}

	@Test
	public void tooltips()
	{
		final XpProgress p = fishing16();
		assertEquals("Fishing XP: 82% to level 17 (" + String.format("%,d", p.getXpLeft()) + " xp left)",
			PillarText.xpTooltip("Fishing", p));
		assertEquals("Inventory: 10 of 28 free", PillarText.inventoryTooltip(10, InventoryCount.FREE));
		assertEquals("Inventory: 18 of 28 used", PillarText.inventoryTooltip(10, InventoryCount.USED));
		assertNull(PillarText.inventoryTooltip(-1, InventoryCount.FREE));
		assertEquals("Target: Rock Crab 12/50", PillarText.targetTooltip("Rock Crab", 12, 50, 0.24));
		assertEquals("Target: Goblin 82%", PillarText.targetTooltip("Goblin", -1, 0, 0.82));
	}

	@Test
	public void xpTooltipPast99()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(99) + 1000);
		assertEquals("Fishing XP: 0% to 200m (" + String.format("%,d", p.getXpLeft()) + " xp left)",
			PillarText.xpTooltip("Fishing", p));
		assertEquals("Fishing XP: 200m", PillarText.xpTooltip("Fishing", XpProgress.of(Experience.MAX_SKILL_XP)));
	}

	@Test
	public void inventoryTicksMarkEveryOneOf28Slots()
	{
		final int[] ticks = PillarText.ticks(10, 252, InventoryFill.SLOTS);
		assertEquals(27, ticks.length);
		assertEquals(10 + 9, ticks[0]);
		assertEquals(10 + 126, ticks[13]);
		assertEquals(10 + 243, ticks[26]);
		for (int i = 1; i < ticks.length; i++)
		{
			final int gap = ticks[i] - ticks[i - 1];
			assertTrue("slot " + i + " gap " + gap, gap == 9);
		}
	}

	@Test
	public void xpTicksMarkEveryQuarter()
	{
		assertArrayEquals(new int[]{63, 126, 189}, PillarText.ticks(0, 252, 4));
		assertArrayEquals(new int[]{68, 136, 204}, PillarText.ticks(0, 272, 4));
	}

	@Test
	public void noTicksForOneDivisionOrNoRoom()
	{
		assertEquals(0, PillarText.ticks(0, 252, 1).length);
		assertEquals(0, PillarText.ticks(0, 0, 4).length);
	}

	@Test
	public void inventoryRampRunsGreenYellowRed()
	{
		final Color green = Color.GREEN;
		final Color yellow = Color.YELLOW;
		final Color red = Color.RED;
		assertEquals(green, InventoryFill.colour(0, green, yellow, red));
		assertEquals(yellow, InventoryFill.colour(0.5, green, yellow, red));
		assertEquals(red, InventoryFill.colour(1, green, yellow, red));
		final Color quarter = InventoryFill.colour(0.25, green, yellow, red);
		assertTrue(quarter.getRed() > 0 && quarter.getRed() < 255 && quarter.getGreen() == 255);
	}

	@Test
	public void labelsModes()
	{
		assertTrue(PillarLabels.ICON_AND_NUMBER.icon() && PillarLabels.ICON_AND_NUMBER.number());
		assertTrue(PillarLabels.ICON_ONLY.icon());
		assertFalse(PillarLabels.ICON_ONLY.number());
		assertFalse(PillarLabels.NONE.icon() || PillarLabels.NONE.number());
	}
}
