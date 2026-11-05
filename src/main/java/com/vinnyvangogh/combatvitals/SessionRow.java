package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * The badge's session row as columns: "24 caught in 3m", "10.6k xp/h",
 * "459 fish/h", "lvl 18 in 6m". Each cell is one stat with its own soft
 * shade of the skill colour; the renderer draws them in equal columns with
 * a thin rule between, in place of the old "Caught 24 | 10,597 xp/h | 459/h".
 *
 * Pure: counts, rates and milliseconds in, short text out.
 */
final class SessionRow
{
	private static final long SECOND = 1_000;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;

	enum Kind
	{
		COUNT,
		XP_RATE,
		ACTION_RATE,
		TIME_TO_LEVEL
	}

	@Value
	static class Cell
	{
		Kind kind;
		String text;
	}

	private SessionRow()
	{
	}

	/**
	 * @param elapsedMillis  since the session's first gain, or -1 before it
	 * @param xpPerHour      -1 while not yet known
	 * @param actionsPerHour -1 while not yet known
	 * @param timeToLevel    add the time to the next level when the rates are on
	 * @return the cells, left to right; empty when both the count and the rates are off
	 */
	static List<Cell> cells(Skill skill, int actions, long elapsedMillis, int xpPerHour, int actionsPerHour,
		XpProgress xp, boolean count, boolean rates, boolean timeToLevel)
	{
		if (!count && !rates)
		{
			return Collections.emptyList();
		}

		final List<Cell> cells = new ArrayList<>(4);
		if (count)
		{
			cells.add(new Cell(Kind.COUNT, count(skill, actions, elapsedMillis)));
		}
		if (rates)
		{
			cells.add(new Cell(Kind.XP_RATE, xpRate(xpPerHour)));
			add(cells, Kind.ACTION_RATE, actionRate(skill, actionsPerHour));
			if (timeToLevel)
			{
				add(cells, Kind.TIME_TO_LEVEL, timeToLevel(xp, xpPerHour));
			}
		}
		return cells;
	}

	private static void add(List<Cell> cells, Kind kind, String text)
	{
		if (text != null)
		{
			cells.add(new Cell(kind, text));
		}
	}

	/** "24 caught in 3m"; "0 caught" before the first gain. */
	static String count(Skill skill, int actions, long elapsedMillis)
	{
		final String done = actions + " " + done(skill);
		return elapsedMillis < 0 ? done : done + " in " + duration(elapsedMillis);
	}

	/** "10.6k xp/h", or "-- xp/h" for the first minute. */
	static String xpRate(int xpPerHour)
	{
		return (xpPerHour < 0 ? "--" : shortNumber(xpPerHour)) + " xp/h";
	}

	/** "459 fish/h", or null while not yet known. */
	static String actionRate(Skill skill, int actionsPerHour)
	{
		return actionsPerHour < 0 ? null : shortNumber(actionsPerHour) + " " + noun(skill) + "/h";
	}

	/**
	 * "lvl 18 in 6m" at this rate, or null with no rate yet, a rate of 0, or
	 * no level left to reach (99 and past: the 200m cap is not a level).
	 */
	static String timeToLevel(XpProgress xp, int xpPerHour)
	{
		if (xpPerHour <= 0 || xp.getNextLevel() < 0)
		{
			return null;
		}
		final long millis = xp.getXpLeft() * HOUR / xpPerHour;
		return "lvl " + xp.getNextLevel() + " in " + duration(millis);
	}

	/** "45s", "3m", "1h 05m", "2d 3h": two units at most, rounded down. */
	static String duration(long millis)
	{
		final long ms = Math.max(0, millis);
		if (ms < MINUTE)
		{
			return ms / SECOND + "s";
		}
		if (ms < HOUR)
		{
			return ms / MINUTE + "m";
		}
		if (ms < DAY)
		{
			return String.format(Locale.ROOT, "%dh %02dm", ms / HOUR, ms % HOUR / MINUTE);
		}
		return ms / DAY + "d " + ms % DAY / HOUR + "h";
	}

	/** 459, 1.2k, 10.6k, 125k, 1.2m: three significant figures at most, ".0" dropped. */
	static String shortNumber(long n)
	{
		if (n < 1_000)
		{
			return Long.toString(n);
		}
		// 999,500 and up would round to "1000k"; it reads as 1m.
		return n < 999_500 ? scaled(n / 1_000.0, "k") : scaled(n / 1_000_000.0, "m");
	}

	private static String scaled(double value, String unit)
	{
		if (value >= 99.95)
		{
			return Math.round(value) + unit;
		}
		final String text = String.format(Locale.ROOT, "%.1f", value);
		return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + unit;
	}

	/**
	 * A soft shade of the skill colour per stat, lighter for each, so the
	 * columns tell apart at a glance and all read as the one skill.
	 */
	static Color tint(Color skill, Kind kind)
	{
		final double white = 0.35 + 0.15 * kind.ordinal();
		return new Color(
			lighten(skill.getRed(), white),
			lighten(skill.getGreen(), white),
			lighten(skill.getBlue(), white));
	}

	private static int lighten(int channel, double white)
	{
		return (int) Math.round(channel + (255 - channel) * white);
	}

	/** What was done, after the count: "24 caught". */
	private static String done(Skill skill)
	{
		switch (skill)
		{
			case FISHING:
			case HUNTER:
				return "caught";
			case WOODCUTTING:
				return "chopped";
			case MINING:
				return "mined";
			case THIEVING:
				return "stolen";
			case RUNECRAFT:
				return "crafted";
			default:
				return "done";
		}
	}

	/** One action, in the per-hour rate: "459 fish/h". */
	private static String noun(Skill skill)
	{
		switch (skill)
		{
			case FISHING:
				return "fish";
			case WOODCUTTING:
				return "logs";
			case MINING:
				return "ores";
			case THIEVING:
				return "steals";
			case HUNTER:
				return "catches";
			case RUNECRAFT:
				return "crafts";
			default:
				return "actions";
		}
	}
}
