package com.vinnyvangogh.combatvitals;

import java.util.Locale;

/** Progress, rate and ETA for a {@link Goal}. Pure arithmetic. */
final class GoalMath
{
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
