package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/** The badge's wording for the round 3 settings: level locks, idle, XP display, inventory count, session. */
public class BadgeRowsTest
{
	@Test
	public void aLockedFishReadsAsALevelNotAQuantity()
	{
		assertEquals("Lv 15", SkillingText.lockedLabel(15));
		assertEquals("Needs Fishing 15", SkillingText.lockedTooltip("Fishing", 15));
	}

	@Test
	public void theLockedLevelIsRed()
	{
		final Color c = SkillingText.LOCKED_LEVEL;
		assertTrue("red dominates", c.getRed() > 200 && c.getGreen() < 100 && c.getBlue() < 100);
	}

	@Test
	public void idleHeaderNamesTheActivity()
	{
		assertEquals("Not fishing", SkillingText.idleHeader(Skill.FISHING));
		assertEquals("Not woodcutting", SkillingText.idleHeader(Skill.WOODCUTTING));
		assertEquals("Not mining", SkillingText.idleHeader(Skill.MINING));
		assertEquals("Not thieving", SkillingText.idleHeader(Skill.THIEVING));
		assertEquals("Not hunting", SkillingText.idleHeader(Skill.HUNTER));
		assertEquals("Not runecrafting", SkillingText.idleHeader(Skill.RUNECRAFT));
		assertEquals("Not cooking", SkillingText.idleHeader(Skill.COOKING));
	}

	@Test
	public void xpDisplayChoosesTheNumbers()
	{
		final int lv13 = Experience.getXpForLevel(13);
		final XpProgress p = XpProgress.of(lv13 - 1);
		assertEquals("Lv 12 -> 13: 99% (1 xp)", p.text(XpDisplay.BOTH));
		assertEquals("Lv 12 -> 13: 99%", p.text(XpDisplay.PERCENT));
		assertEquals("Lv 12 -> 13: 1 xp", p.text(XpDisplay.XP_LEFT));
		assertEquals(p.text(XpDisplay.BOTH), p.text());
	}

	@Test
	public void xpDisplayAt99()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(99) + 1000);
		assertEquals("Lv 99 -> 200m: 0%", p.text(XpDisplay.PERCENT));
		assertEquals("Lv 99 -> 200m: " + String.format("%,d", p.getXpLeft()) + " xp", p.text(XpDisplay.XP_LEFT));
		assertEquals("Lv 99: 200m xp", XpProgress.of(Experience.MAX_SKILL_XP).text(XpDisplay.XP_LEFT));
	}

	@Test
	public void inventoryCountsFreeOrUsed()
	{
		assertEquals("Inv 26/28 free", InventoryFill.label(26, InventoryCount.FREE));
		assertEquals("Inv 2/28 used", InventoryFill.label(26, InventoryCount.USED));
		assertNull(InventoryFill.label(-1, InventoryCount.USED));
	}
}
