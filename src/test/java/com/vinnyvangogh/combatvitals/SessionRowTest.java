package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/** The session row's cells: count and timer, short rates with the action's noun, time to level. */
public class SessionRowTest
{
	private static final long MINUTE = 60_000;

	@Test
	public void shortNumbers()
	{
		assertEquals("0", SessionRow.shortNumber(0));
		assertEquals("459", SessionRow.shortNumber(459));
		assertEquals("999", SessionRow.shortNumber(999));
		assertEquals("1k", SessionRow.shortNumber(1_000));
		assertEquals("1.2k", SessionRow.shortNumber(1_234));
		assertEquals("10.6k", SessionRow.shortNumber(10_597));
		assertEquals("10k", SessionRow.shortNumber(10_000));
		assertEquals("100k", SessionRow.shortNumber(99_960));
		assertEquals("125k", SessionRow.shortNumber(125_400));
		assertEquals("999k", SessionRow.shortNumber(999_400));
		assertEquals("1m", SessionRow.shortNumber(999_500));
		assertEquals("1m", SessionRow.shortNumber(999_950));
		assertEquals("1.2m", SessionRow.shortNumber(1_234_567));
		assertEquals("200m", SessionRow.shortNumber(200_000_000));
	}

	@Test
	public void countSaysWhatWasDoneAndForHowLong()
	{
		assertEquals("24 caught in 3m", SessionRow.count(Skill.FISHING, 24, 3 * MINUTE + 10_000));
		assertEquals("3 chopped in 45s", SessionRow.count(Skill.WOODCUTTING, 3, 45_000));
		assertEquals("7 mined in 1h 05m", SessionRow.count(Skill.MINING, 7, 65 * MINUTE));
		assertEquals("2 stolen in 0s", SessionRow.count(Skill.THIEVING, 2, 0));
		assertEquals("0 caught", SessionRow.count(Skill.HUNTER, 0, -1));
		assertEquals("5 crafted in 2m", SessionRow.count(Skill.RUNECRAFT, 5, 2 * MINUTE));
		assertEquals("1 done in 1m", SessionRow.count(Skill.COOKING, 1, MINUTE));
	}

	@Test
	public void ratesAreShortAndNameTheAction()
	{
		assertEquals("10.6k xp/h", SessionRow.xpRate(10_597));
		assertEquals("-- xp/h", SessionRow.xpRate(-1));
		assertEquals("459 fish/h", SessionRow.actionRate(Skill.FISHING, 459));
		assertEquals("1.2k logs/h", SessionRow.actionRate(Skill.WOODCUTTING, 1_200));
		assertEquals("300 ores/h", SessionRow.actionRate(Skill.MINING, 300));
		assertEquals("900 steals/h", SessionRow.actionRate(Skill.THIEVING, 900));
		assertEquals("40 catches/h", SessionRow.actionRate(Skill.HUNTER, 40));
		assertEquals("60 crafts/h", SessionRow.actionRate(Skill.RUNECRAFT, 60));
		assertNull(SessionRow.actionRate(Skill.FISHING, -1));
	}

	@Test
	public void timeToLevel()
	{
		// 65 xp left at 10,597 xp/h is 22 seconds.
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(17) - 65);
		assertEquals("lvl 17 in 22s", SessionRow.timeToLevel(p, 10_597));

		// 6,000 xp left at 60,000 xp/h is 6 minutes.
		final XpProgress q = XpProgress.of(Experience.getXpForLevel(61) - 6_000);
		assertEquals("lvl 61 in 6m", SessionRow.timeToLevel(q, 60_000));

		final XpProgress far = XpProgress.of(Experience.getXpForLevel(60));
		assertEquals("lvl 61 in 2h 18m",
			SessionRow.timeToLevel(far, (int) (far.getXpLeft() * 60L / 138)));
	}

	@Test
	public void timeToLevelNeedsARateAndALevel()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(17) - 65);
		assertNull("no rate yet", SessionRow.timeToLevel(p, -1));
		assertNull("zero rate", SessionRow.timeToLevel(p, 0));
		assertNull("99 has no next level", SessionRow.timeToLevel(XpProgress.of(Experience.getXpForLevel(99)), 50_000));
		assertNull("200m", SessionRow.timeToLevel(XpProgress.of(Experience.MAX_SKILL_XP), 50_000));
	}

	@Test
	public void durations()
	{
		assertEquals("0s", SessionRow.duration(0));
		assertEquals("59s", SessionRow.duration(59_999));
		assertEquals("1m", SessionRow.duration(MINUTE));
		assertEquals("59m", SessionRow.duration(59 * MINUTE + 59_000));
		assertEquals("1h 00m", SessionRow.duration(60 * MINUTE));
		assertEquals("23h 59m", SessionRow.duration(24 * 60 * MINUTE - 1));
		assertEquals("2d 3h", SessionRow.duration((51 * 60 + 30) * MINUTE));
	}

	@Test
	public void cellsInOrderWithEveryHalf()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(61) - 6_000);
		final List<SessionRow.Cell> cells = SessionRow.cells(Skill.FISHING, 24, 3 * MINUTE, 60_000, 459, p,
			true, true, true);
		assertEquals(4, cells.size());
		assertEquals("24 caught in 3m | 60k xp/h | 459 fish/h | lvl 61 in 6m", joined(cells));
		assertEquals(SessionRow.Kind.COUNT, cells.get(0).getKind());
		assertEquals(SessionRow.Kind.TIME_TO_LEVEL, cells.get(3).getKind());
	}

	@Test
	public void cellsHonourTheSwitches()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(18) - 6_000);
		assertEquals("24 caught in 3m", joined(SessionRow.cells(Skill.FISHING, 24, 3 * MINUTE, 60_000, 459, p,
			true, false, true)));
		assertEquals("60k xp/h | 459 fish/h", joined(SessionRow.cells(Skill.FISHING, 24, 3 * MINUTE, 60_000, 459, p,
			false, true, false)));
		assertTrue(SessionRow.cells(Skill.FISHING, 24, 3 * MINUTE, 60_000, 459, p, false, false, true).isEmpty());
	}

	@Test
	public void unknownRatesLeaveOutWhatIsNotKnown()
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(18) - 6_000);
		assertEquals("1 caught in 10s | -- xp/h", joined(SessionRow.cells(Skill.FISHING, 1, 10_000, -1, -1, p,
			true, true, true)));
	}

	@Test
	public void eachStatHasItsOwnSoftShadeOfTheSkillColour()
	{
		final Color skill = new Color(80, 140, 255);
		Color previous = null;
		for (SessionRow.Kind kind : SessionRow.Kind.values())
		{
			final Color c = SessionRow.tint(skill, kind);
			assertTrue(kind + " lighter than the skill", c.getRed() >= skill.getRed() && c.getGreen() >= skill.getGreen());
			assertTrue(kind + " keeps the skill's hue", c.getBlue() >= c.getRed());
			assertNotEquals(kind + " differs from the one before", previous, c);
			previous = c;
		}
	}

	private static String joined(List<SessionRow.Cell> cells)
	{
		return cells.stream().map(SessionRow.Cell::getText).collect(Collectors.joining(" | "));
	}
}
