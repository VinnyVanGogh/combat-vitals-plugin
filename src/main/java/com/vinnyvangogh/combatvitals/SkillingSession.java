package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import net.runelite.api.Skill;

/**
 * This session's count and rates per skill, for the badge's session row:
 * how many fish were caught, logs chopped or ores mined, and the XP and
 * actions per hour.
 *
 * Every experience gain is one action: a catch, a log, an ore, a
 * pickpocket. The first reading of a skill after login is its baseline,
 * never a gain. Rates run from the first gain, and that gain's own XP is
 * not counted toward them because it was earned before the clock started.
 *
 * Pure: XP totals and wall-clock milliseconds in, counts and rates out.
 */
final class SkillingSession
{
	/** Rates are not shown until the first gain is this old: two quick catches are not an hourly rate. */
	static final long MIN_RATE_MILLIS = 60_000;

	private static final long HOUR = 3_600_000;

	private final int[] lastXp = new int[Skill.values().length];
	private final int[] actions = new int[lastXp.length];
	private final long[] firstGain = new long[lastXp.length];
	private final long[] xpSinceFirst = new long[lastXp.length];

	SkillingSession()
	{
		clear();
	}

	void xp(Skill skill, int xp, long nowMillis)
	{
		final int i = skill.ordinal();
		final int previous = lastXp[i];
		lastXp[i] = xp;
		if (previous < 0 || xp <= previous)
		{
			return;
		}

		if (actions[i] == 0)
		{
			firstGain[i] = nowMillis;
		}
		else
		{
			xpSinceFirst[i] += xp - previous;
		}
		actions[i]++;
	}

	int actions(Skill skill)
	{
		return actions[skill.ordinal()];
	}

	/** Milliseconds since this session's first gain in the skill, or -1 before it. */
	long elapsed(Skill skill, long nowMillis)
	{
		final int i = skill.ordinal();
		return actions[i] == 0 ? -1 : Math.max(0, nowMillis - firstGain[i]);
	}

	/** XP per hour since the first gain, or -1 until {@link #MIN_RATE_MILLIS} has passed. */
	int xpPerHour(Skill skill, long nowMillis)
	{
		final int i = skill.ordinal();
		return perHour(xpSinceFirst[i], i, nowMillis);
	}

	/** Actions per hour since the first gain, or -1 until {@link #MIN_RATE_MILLIS} has passed. */
	int actionsPerHour(Skill skill, long nowMillis)
	{
		final int i = skill.ordinal();
		return perHour(actions[i] - 1, i, nowMillis);
	}

	private int perHour(long amount, int i, long nowMillis)
	{
		final long elapsed = nowMillis - firstGain[i];
		if (actions[i] == 0 || elapsed < MIN_RATE_MILLIS)
		{
			return -1;
		}
		return (int) (amount * HOUR / elapsed);
	}

	/** The reset option: counts start again, the XP baselines stay so no gain is invented. */
	void reset()
	{
		Arrays.fill(actions, 0);
		Arrays.fill(firstGain, 0);
		Arrays.fill(xpSinceFirst, 0);
	}

	/** Logout: everything goes, baselines included. */
	void clear()
	{
		Arrays.fill(lastXp, -1);
		reset();
	}
}
