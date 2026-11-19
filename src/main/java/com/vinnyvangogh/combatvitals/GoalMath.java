package com.vinnyvangogh.combatvitals;

import java.util.Locale;

/** Progress, rate and ETA for a {@link Goal}. Pure arithmetic. */
final class GoalMath
{
	static final long HOUR_SECONDS = 3_600L;
	static final long DAY_SECONDS = 86_400L;
	static final long WEEK_SECONDS = 7 * DAY_SECONDS;
	/** The game's weekly reset is Wednesday 00:00 UTC; 1970-01-01 was a Thursday, so Wednesday is day 6. */
	private static final long WEEK_OFFSET = 6 * DAY_SECONDS;

	private GoalMath()
	{
	}

	/** Progress from the baseline to the goal value, 0 to 1. A goal already met at creation reads 1. */
	static double fraction(long baseline, long current, long goalValue)
	{
		if (current >= goalValue)
		{
			return 1;
		}
		final long span = goalValue - baseline;
		if (span <= 0)
		{
			return 0;
		}
		return Math.max(0, Math.min(1, (double) (current - baseline) / span));
	}

	static long remaining(long current, long goalValue)
	{
		return Math.max(0, goalValue - current);
	}

	/** Gained per hour over the elapsed seconds; 0 until there is a gain and a minute of time to divide by. */
	static double perHour(long gained, long elapsedSeconds)
	{
		if (gained <= 0 || elapsedSeconds < 60)
		{
			return 0;
		}
		return gained * 3600.0 / elapsedSeconds;
	}

	/** Seconds to go at this rate, or -1 when there is no rate to go on. 0 when nothing remains. */
	static long etaSeconds(long remaining, double perHour)
	{
		if (remaining <= 0)
		{
			return 0;
		}
		if (perHour <= 0)
		{
			return -1;
		}
		return (long) Math.ceil(remaining * 3600.0 / perHour);
	}

	/** "2h 05m", "14m", "45s", or "no rate yet". */
	static String etaText(long seconds)
	{
		if (seconds < 0)
		{
			return "no rate yet";
		}
		final long h = seconds / 3600;
		final long m = (seconds % 3600) / 60;
		if (h > 0)
		{
			return String.format(Locale.ROOT, "%dh %02dm", h, m);
		}
		return m > 0 ? m + "m" : seconds + "s";
	}

	/** How many more actions at {@code xpPerAction}, rounded up; -1 with no per-action value. */
	static long actionsLeft(long remainingXp, double xpPerAction)
	{
		if (remainingXp <= 0)
		{
			return 0;
		}
		return xpPerAction <= 0 ? -1 : (long) Math.ceil(remainingXp / xpPerAction);
	}

	static String number(long n)
	{
		return java.text.NumberFormat.getIntegerInstance(Locale.US).format(n);
	}

	/** "Mining level 90", "Mining 5,000,000 xp", "Zulrah x100". */
	static String title(Goal g)
	{
		switch (g.type)
		{
			case SKILL_LEVEL:
				return g.label + " level " + g.target;
			case SKILL_XP:
				return g.label + " " + number(g.target) + " xp";
			case BOSS_KC:
			case ITEM:
				return g.label + " x" + number(g.target);
			case DAILY:
				return "Daily: " + g.label;
			default:
				return "Weekly: " + g.label;
		}
	}

	/** "120,000 xp to go (45%), 800 actions, 2h 05m at 60,000 xp/hr (session), ... (last hour)". */
	static String summary(GoalBook.Progress p)
	{
		final StringBuilder sb = new StringBuilder(number(p.left)).append(" xp to go (").append(p.percent)
			.append("%)");
		if (p.actionsLeft > 0)
		{
			sb.append(", ").append(number(p.actionsLeft)).append(" actions left");
		}
		sb.append(", ").append(rate("session", p.left, p.sessionRate));
		sb.append(", ").append(rate("last hour", p.left, p.hourRate));
		return sb.toString();
	}

	private static String rate(String name, long left, double perHour)
	{
		final String at = perHour > 0 ? " at " + number(Math.round(perHour)) + " xp/hr" : "";
		return etaText(etaSeconds(left, perHour)) + at + " (" + name + ")";
	}

	/** The level {@code levels} above {@code level}, capped at 99. */
	static int defaultLevel(int level, int levels)
	{
		return Math.min(99, Math.max(1, level) + levels);
	}

	/** Start of the UTC day containing {@code nowSeconds}. */
	static long dayStart(long nowSeconds)
	{
		return nowSeconds - Math.floorMod(nowSeconds, DAY_SECONDS);
	}

	/** Start of the game week (Wednesday 00:00 UTC) containing {@code nowSeconds}. */
	static long weekStart(long nowSeconds)
	{
		return nowSeconds - Math.floorMod(nowSeconds - WEEK_OFFSET, WEEK_SECONDS);
	}

	/** The start of the period {@code type} is in at {@code nowSeconds}; 0 for goals that never reset. */
	static long periodStart(Goal.Type type, long nowSeconds)
	{
		switch (type)
		{
			case DAILY:
				return dayStart(nowSeconds);
			case WEEKLY:
				return weekStart(nowSeconds);
			default:
				return 0;
		}
	}

	/** True when a daily or weekly goal belongs to an earlier period than {@code nowSeconds} and so starts again. */
	static boolean due(Goal goal, long nowSeconds)
	{
		return goal.periodStart > 0 && periodStart(goal.type, nowSeconds) > goal.periodStart;
	}
}
