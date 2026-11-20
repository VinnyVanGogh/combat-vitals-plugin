package com.vinnyvangogh.combatvitals;

import java.util.Locale;
import net.runelite.api.Skill;

/**
 * The all-time counterpart of the sidebar's "Total" row: how long you have trained each skill, and how much XP and
 * how many actions that gave, kept across logins (the plugin stores {@link #save} in the account's profile config).
 *
 * <p>Time is kept per skill and also for "Combat" (any combat skill active) and "Overall" (any skill active), so
 * those two do not double count a minute spent training two skills at once. XP and actions are this login's
 * {@link SkillingSession} total on top of what earlier logins left ({@link #commit} folds a login in).
 *
 * <p>Pure: wall-clock milliseconds and the session total in, numbers out.
 */
final class AllTime
{
	/** The bucket of "any combat skill is active". */
	static final int COMBAT = Skill.values().length;
	/** The bucket of "any skill is active". */
	static final int OVERALL = COMBAT + 1;
	private static final int SIZE = OVERALL + 1;

	/** A gap between ticks longer than this adds nothing: a logout or a stall is not time spent training. */
	static final long MAX_STEP_MILLIS = 10_000;

	/** What one bucket adds up to. */
	static final class Stats
	{
		static final Stats NONE = new Stats(0, 0, 0);

		final long millis;
		final long xp;
		final long actions;

		Stats(long millis, long xp, long actions)
		{
			this.millis = millis;
			this.xp = xp;
			this.actions = actions;
		}

		boolean isEmpty()
		{
			return millis == 0 && xp == 0 && actions == 0;
		}
	}

	private final long[] millis = new long[SIZE];
	private final long[] baseXp = new long[Skill.values().length];
	private final long[] baseActions = new long[baseXp.length];
	private long lastTick = -1;
	private boolean dirty;

	/** Once per game tick: adds the time since the last tick to every bucket that is being trained now. */
	void tick(long nowMillis, SkillingSession total)
	{
		final long step = lastTick < 0 ? 0 : nowMillis - lastTick;
		lastTick = nowMillis;
		if (step <= 0 || step > MAX_STEP_MILLIS)
		{
			return;
		}
		boolean any = false;
		boolean combat = false;
		for (Skill skill : Skill.values())
		{
			if (total.actions(skill) == 0 || total.paused(skill, nowMillis))
			{
				continue;
			}
			millis[skill.ordinal()] += step;
			any = true;
			combat |= isCombat(skill);
		}
		if (combat)
		{
			millis[COMBAT] += step;
		}
		if (any)
		{
			millis[OVERALL] += step;
			dirty = true;
		}
	}

	/** Totals of one skill, with this login's {@code total} on top of the earlier logins'. */
	Stats skill(Skill skill, SkillingSession total)
	{
		final int i = skill.ordinal();
		return new Stats(millis[i], baseXp[i] + total.xpGained(skill), baseActions[i] + total.actions(skill));
	}

	/** Totals of the combat skills ({@link LiveVitals#COMBAT_SKILLS}). */
	Stats combat(SkillingSession total)
	{
		return sum(LiveVitals.COMBAT_SKILLS, COMBAT, total);
	}

	/** Totals of every skill. */
	Stats overall(SkillingSession total)
	{
		return sum(Skill.values(), OVERALL, total);
	}

	private Stats sum(Skill[] skills, int bucket, SkillingSession total)
	{
		long xp = 0;
		long actions = 0;
		for (Skill skill : skills)
		{
			final Stats s = skill(skill, total);
			xp += s.xp;
			actions += s.actions;
		}
		return new Stats(millis[bucket], xp, actions);
	}

	/** Logout: this login's gains become part of the earlier logins'. Call before the session total is cleared. */
	void commit(SkillingSession total)
	{
		for (Skill skill : Skill.values())
		{
			final int i = skill.ordinal();
			if (total.actions(skill) > 0)
			{
				baseXp[i] += total.xpGained(skill);
				baseActions[i] += total.actions(skill);
				dirty = true;
			}
		}
		lastTick = -1;
	}

	/** True when something changed since the last {@link #saved}. */
	boolean isDirty()
	{
		return dirty;
	}

	void saved()
	{
		dirty = false;
	}

	/** Everything forgotten, for a different account: nothing is carried to its numbers. */
	void clear()
	{
		java.util.Arrays.fill(millis, 0);
		java.util.Arrays.fill(baseXp, 0);
		java.util.Arrays.fill(baseActions, 0);
		lastTick = -1;
		dirty = false;
	}

	/** One text for the profile config: "SKILL=millis/xp/actions;..." for every skill with something, plus the two buckets. */
	String save(SkillingSession total)
	{
		final StringBuilder out = new StringBuilder();
		for (Skill skill : Skill.values())
		{
			final Stats s = skill(skill, total);
			if (!s.isEmpty())
			{
				entry(out, skill.name(), s);
			}
		}
		if (millis[COMBAT] != 0)
		{
			entry(out, "COMBAT", new Stats(millis[COMBAT], 0, 0));
		}
		if (millis[OVERALL] != 0)
		{
			entry(out, "OVERALL", new Stats(millis[OVERALL], 0, 0));
		}
		return out.toString();
	}

	private static void entry(StringBuilder out, String name, Stats s)
	{
		if (out.length() > 0)
		{
			out.append(';');
		}
		out.append(name).append('=').append(s.millis).append('/').append(s.xp).append('/').append(s.actions);
	}

	/**
	 * Adds what an earlier login left, from {@link #save}'s text; a malformed entry is skipped. Adds, never
	 * replaces, so numbers made before the profile was known are kept. Call once per login.
	 */
	void load(String text)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		for (String entry : text.split(";"))
		{
			final int eq = entry.indexOf('=');
			if (eq < 0)
			{
				continue;
			}
			final String[] n = entry.substring(eq + 1).split("/");
			final String name = entry.substring(0, eq).toUpperCase(Locale.ROOT);
			try
			{
				final long ms = Math.max(0, Long.parseLong(n[0]));
				final long xp = n.length > 1 ? Math.max(0, Long.parseLong(n[1])) : 0;
				final long actions = n.length > 2 ? Math.max(0, Long.parseLong(n[2])) : 0;
				if ("COMBAT".equals(name))
				{
					millis[COMBAT] += ms;
				}
				else if ("OVERALL".equals(name))
				{
					millis[OVERALL] += ms;
				}
				else
				{
					final int i = Skill.valueOf(name).ordinal();
					millis[i] += ms;
					baseXp[i] += xp;
					baseActions[i] += actions;
				}
			}
			catch (IllegalArgumentException e)
			{
				// A skill this version does not know, or a number that is not one: skipped.
				continue;
			}
		}
	}

	private static boolean isCombat(Skill skill)
	{
		for (Skill s : LiveVitals.COMBAT_SKILLS)
		{
			if (s == skill)
			{
				return true;
			}
		}
		return false;
	}
}
