package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The player's goals for the logged-in account, and the live numbers that
 * move them: experience from stat changes, kill counts from the chat. Every
 * method is synchronized, since the client thread feeds it and the Swing
 * thread reads and edits it. Nothing here touches the client or the disk:
 * {@link Storage} is where a change is saved.
 */
final class GoalBook
{
	/** Where the goals are kept; the plugin's is the account's own config profile. */
	interface Storage
	{
		String load();

		void save(String text);
	}

	private final Storage storage;
	private final List<Goal> goals = new ArrayList<>();
	/** The first experience seen per skill this login and when, so a rate is this session's. */
	private final Map<String, long[]> seen = new HashMap<>();
	private final Map<String, Long> xp = new HashMap<>();
	private long nextId = 1;

	GoalBook(Storage storage)
	{
		this.storage = storage;
	}

	/** Loads the saved goals, replacing what is held; call on login. */
	synchronized void load()
	{
		goals.clear();
		goals.addAll(Goal.decode(storage.load()));
		for (Goal g : goals)
		{
			nextId = Math.max(nextId, g.id + 1);
		}
	}

	/** Forgets the session numbers and the goals held (logout, or another account). */
	synchronized void clear()
	{
		goals.clear();
		seen.clear();
		xp.clear();
	}

	synchronized List<Goal> goals()
	{
		return new ArrayList<>(goals);
	}

	synchronized long xp(String skill)
	{
		final Long v = xp.get(skill);
		return v == null ? 0 : v;
	}

	/** This session's experience per hour in a skill, 0 until there is a minute of it. */
	synchronized double xpPerHour(String skill, long nowSeconds)
	{
		final long[] first = seen.get(skill);
		final Long now = xp.get(skill);
		return first == null || now == null ? 0 : GoalMath.perHour(now - first[0], nowSeconds - first[1]);
	}

	/** The current value of a goal: live experience for a skill goal, else what is counted. */
	synchronized long value(Goal g)
	{
		return g.skill() ? xp(g.label) : g.current;
	}

	synchronized Goal addSkill(String skill, boolean level, long target, long nowSeconds)
	{
		final long baseline = xp(skill);
		return add(new Goal(0, level ? Goal.Type.SKILL_LEVEL : Goal.Type.SKILL_XP, skill, target, baseline, 0,
			0, false));
	}

	synchronized Goal addCounted(Goal.Type type, String label, long target, long nowSeconds)
	{
		return add(new Goal(0, type, label, target, 0, 0, GoalMath.periodStart(type, nowSeconds), false));
	}

	private Goal add(Goal template)
	{
		final Goal g = new Goal(nextId++, template.type, template.label, template.target, template.baseline,
			template.current, template.periodStart, false);
		// Already met when made: shown full, never announced as news.
		goals.add(value(g) >= g.goalValue() ? g.withReached(true) : g);
		save();
		return g;
	}

	synchronized void remove(long id)
	{
		if (goals.removeIf(g -> g.id == id))
		{
			save();
		}
	}

	/** Adds {@code delta} to a hand-counted goal; skill goals and boss kill counts read their own number. */
	synchronized List<Goal> bump(long id, long delta)
	{
		for (int i = 0; i < goals.size(); i++)
		{
			final Goal g = goals.get(i);
			if (g.id == id && !g.skill() && g.type != Goal.Type.BOSS_KC)
			{
				goals.set(i, g.withCurrent(Math.max(0, g.current + delta)));
				return settle();
			}
		}
		return new ArrayList<>();
	}

	/** A skill's experience from a stat change. Returns the goals this reached, each once. */
	synchronized List<Goal> onXp(String skill, long experience, long nowSeconds)
	{
		seen.putIfAbsent(skill, new long[] {experience, nowSeconds});
		xp.put(skill, experience);
		return settle();
	}

	/** A kill count the chat gave: "Your Zulrah kill count is: 12". Goals on that boss take the number. */
	synchronized List<Goal> onKillCount(String boss, long count)
	{
		for (int i = 0; i < goals.size(); i++)
		{
			final Goal g = goals.get(i);
			if (g.type == Goal.Type.BOSS_KC && g.label.equalsIgnoreCase(boss))
			{
				goals.set(i, g.withCurrent(count));
			}
		}
		return settle();
	}

	/** Starts a daily or weekly goal again once its period is over. */
	synchronized void rollover(long nowSeconds)
	{
		boolean changed = false;
		for (int i = 0; i < goals.size(); i++)
		{
			final Goal g = goals.get(i);
			if (GoalMath.due(g, nowSeconds))
			{
				goals.set(i, g.reset(GoalMath.periodStart(g.type, nowSeconds)));
				changed = true;
			}
		}
		if (changed)
		{
			save();
		}
	}

	/** Marks goals that have just been met, saves, and returns them. */
	private List<Goal> settle()
	{
		final List<Goal> met = new ArrayList<>();
		for (int i = 0; i < goals.size(); i++)
		{
			final Goal g = goals.get(i);
			if (!g.reached && value(g) >= g.goalValue())
			{
				final Goal done = g.withReached(true);
				goals.set(i, done);
				met.add(done);
			}
		}
		save();
		return met;
	}

	private void save()
	{
		storage.save(Goal.encode(goals));
	}

	/** "Zulrah" from "Your Zulrah kill count is: 12", or null. Chat line with markup already stripped. */
	static String killCountBoss(String chat)
	{
		final String lower = chat.toLowerCase(Locale.ROOT);
		final int end = lower.indexOf(" kill count is: ");
		if (!lower.startsWith("your ") || end < 0)
		{
			return null;
		}
		return chat.substring(5, end).trim();
	}

	/** 12 from "Your Zulrah kill count is: 12." (commas allowed); -1 when it does not parse. */
	static long killCountValue(String chat)
	{
		final int at = chat.toLowerCase(Locale.ROOT).indexOf(" kill count is: ");
		if (at < 0)
		{
			return -1;
		}
		final String digits = chat.substring(at + " kill count is: ".length()).replaceAll("[^0-9]", "");
		try
		{
			return digits.isEmpty() ? -1 : Long.parseLong(digits);
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}
}
