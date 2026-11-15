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
	/** The XP the session's gains are counted from: the first reading, or the total at a reset. */
	private final int[] baseXp = new int[lastXp.length];
	private final long[] lastGain = new long[lastXp.length];

	SkillingSession()
	{
		clear();
	}

	void xp(Skill skill, int xp, long nowMillis)
	{
		final int i = skill.ordinal();
		final int previous = lastXp[i];
		lastXp[i] = xp;
		if (previous < 0)
		{
			baseXp[i] = xp;
			return;
		}
		if (xp <= previous)
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
		lastGain[i] = nowMillis;
	}

	/** XP gained this session, the first gain included; 0 before it. */
	int xpGained(Skill skill)
	{
		final int i = skill.ordinal();
		return actions[i] == 0 ? 0 : lastXp[i] - baseXp[i];
	}

	/** Levels gained this session, from the XP table (past 99 counts nothing). */
	int levelsGained(Skill skill)
	{
		final int i = skill.ordinal();
		return actions[i] == 0 ? 0
			: XpProgress.of(lastXp[i]).getLevel() - XpProgress.of(baseXp[i]).getLevel();
	}

	/** The skill of {@code skills} gained in most recently this session, or null for none. */
	Skill latest(Skill[] skills)
	{
		Skill latest = null;
		long when = 0;
		for (Skill s : skills)
		{
			final int i = s.ordinal();
			if (actions[i] > 0 && (latest == null || lastGain[i] > when))
			{
				latest = s;
				when = lastGain[i];
			}
		}
		return latest;
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
		Arrays.fill(lastGain, 0);
		System.arraycopy(lastXp, 0, baseXp, 0, lastXp.length);
	}

	/** Logout: everything goes, baselines included. */
	void clear()
	{
		Arrays.fill(lastXp, -1);
		reset();
	}
}
