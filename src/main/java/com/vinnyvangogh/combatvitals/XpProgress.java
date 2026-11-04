package com.vinnyvangogh.combatvitals;

import lombok.Getter;
import net.runelite.api.Experience;

/**
 * How far through the current level a skill's experience is, for the XP
 * line on the skilling badge.
 *
 * Levels come from the game's XP table ({@link Experience}) and stop at 99:
 * past 99 there is no next level, so the target becomes the 200m cap.
 *
 * Pure: an XP total in, a level, a fraction and a line of text out.
 */
@Getter
final class XpProgress
{
	private final int level;

	/** The level being worked toward, or -1 at 99, where the target is 200m XP. */
	private final int nextLevel;

	/** 0 at the start of the level, 1 only at 200m; never 1 short of a level. */
	private final double fraction;

	private final int xpLeft;

	private XpProgress(int level, int nextLevel, double fraction, int xpLeft)
	{
		this.level = level;
		this.nextLevel = nextLevel;
		this.fraction = fraction;
		this.xpLeft = xpLeft;
	}

	static XpProgress of(int xp)
	{
		final int total = Math.max(0, Math.min(xp, Experience.MAX_SKILL_XP));
		final int level = Math.min(Experience.getLevelForXp(total), Experience.MAX_REAL_LEVEL);

		final boolean maxed = level >= Experience.MAX_REAL_LEVEL;
		final int from = Experience.getXpForLevel(level);
		final int to = maxed ? Experience.MAX_SKILL_XP : Experience.getXpForLevel(level + 1);

		return new XpProgress(level, maxed ? -1 : level + 1, (double) (total - from) / (to - from), to - total);
	}

	/** e.g. "Lv 12 -> 13: 64% (1,234 xp)", "Lv 99 -> 200m: 6%", "Lv 99: 200m xp". */
	String text()
	{
		// Rounded down, so 1 xp short of a level reads 99%, never 100%.
		final int percent = (int) Math.floor(fraction * 100);
		if (nextLevel > 0)
		{
			return String.format("Lv %d -> %d: %d%% (%,d xp)", level, nextLevel, percent, xpLeft);
		}
		return xpLeft > 0 ? String.format("Lv %d -> 200m: %d%%", level, percent) : "Lv " + level + ": 200m xp";
	}
}
