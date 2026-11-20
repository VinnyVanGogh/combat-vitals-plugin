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

		/** Stores the goals as they stand. Never deletes: an empty text is stored as an empty text. */
		void save(String text);

		/** Deletes the stored goals. Only an explicit removal of the last goal calls this. */
		void unset();
	}

	private final Storage storage;
	private final List<Goal> goals = new ArrayList<>();
	/** The first experience seen per skill this login and when, so a rate is this session's. */
	private final Map<String, long[]> seen = new HashMap<>();
	private final Map<String, Long> xp = new HashMap<>();
	private long nextId = 1;
	/** The account hash the held goals were loaded for; null when none. */
	private Long loadedFor;
	/** The account whose stored goals read as absent: the profile may not be ready yet, so it is retried. */
	private Long pendingFor;
	/** Retries made for the pending account; loading stops being retried at MAX_RETRIES. */
	private int retries;
	private static final int MAX_RETRIES = 12;
	/** Experience samples per skill over the last hour: {epoch seconds, experience}, oldest first. */
	private final Map<String, java.util.ArrayDeque<long[]>> samples = new HashMap<>();
	private java.util.function.ToDoubleFunction<String> actionXp = skill -> 0;

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

	/**
	 * Loads the saved goals unless this account's are already held. LOGGED_IN fires after every loading
	 * screen and world hop as well as a login, and a reload then could replace the goals with an empty read.
	 * Returns whether it loaded.
	 */
	synchronized boolean loadFor(long account)
	{
		if (loadedFor != null && loadedFor == account)
		{
			return false;
		}
		final String text = storage.load();
		if (text == null)
		{
			// Nothing stored, or the RuneScape profile is not ready yet and reads as nothing. Keep what is held
			// for this account (a goal added meanwhile) and ask again later, rather than call it loaded.
			if (pendingFor != null && pendingFor == account)
			{
				// An account with no saved goals reads as absent forever; stop asking after a few tries.
				retries++;
				return false;
			}
			retries = 0;
			goals.clear();
			pendingFor = account;
			return true;
		}
		goals.clear();
		goals.addAll(Goal.decode(text));
		for (Goal g : goals)
		{
			nextId = Math.max(nextId, g.id + 1);
		}
		loadedFor = account;
		pendingFor = null;
		return true;
	}

	/** True while no stored goals have been read for the logged-in account, so a retry is worth making. */
	synchronized boolean needsLoad()
	{
		return loadedFor == null && retries < MAX_RETRIES;
	}

	/** Where the per-action experience of a skill comes from: the skilling badge's session. */
	synchronized void actionXp(java.util.function.ToDoubleFunction<String> source)
	{
		this.actionXp = source;
	}

	/** Forgets the session numbers and the goals held (logout, or another account). */
	synchronized void clear()
	{
		loadedFor = null;
		pendingFor = null;
		retries = 0;
		goals.clear();
		seen.clear();
		xp.clear();
		samples.clear();
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

	private void sample(String skill, long experience, long nowSeconds)
	{
		final java.util.ArrayDeque<long[]> q = samples.computeIfAbsent(skill, k -> new java.util.ArrayDeque<>());
		q.addLast(new long[] {nowSeconds, experience});
		// Keep one sample at or before the window's start as its anchor; drop the ones older than that.
		while (q.size() > 1)
		{
			final java.util.Iterator<long[]> it = q.iterator();
			it.next();
			if (it.next()[0] > nowSeconds - GoalMath.HOUR_SECONDS)
			{
				break;
			}
			q.removeFirst();
		}
	}

	/** Experience per hour over the last hour in a skill, 0 until there is a minute of it. */
	synchronized double xpLastHour(String skill, long nowSeconds)
	{
		final java.util.ArrayDeque<long[]> q = samples.get(skill);
		final Long now = xp.get(skill);
		if (q == null || q.isEmpty() || now == null)
		{
			return 0;
		}
		final long[] anchor = q.peekFirst();
		final long from = Math.max(anchor[0], nowSeconds - GoalMath.HOUR_SECONDS);
		return GoalMath.perHour(now - anchor[1], nowSeconds - from);
	}

	/** Where a skill goal stands and how long it will take: the Goals page and the Skilling tab both show this. */
	static final class Progress
	{
		final long left;
		final int percent;
		final double sessionRate;
		final double hourRate;
		final double actionXp;
		final long actionsLeft;

		Progress(long left, int percent, double sessionRate, double hourRate, double actionXp, long actionsLeft)
		{
			this.left = left;
			this.percent = percent;
			this.sessionRate = sessionRate;
			this.hourRate = hourRate;
			this.actionXp = actionXp;
			this.actionsLeft = actionsLeft;
		}
	}

	/** Progress of a skill goal; null for any other kind. */
	synchronized Progress progress(Goal g, long nowSeconds)
	{
		if (!g.skill())
		{
			return null;
		}
		final long value = value(g);
		final long goalValue = g.goalValue();
		final long left = GoalMath.remaining(value, goalValue);
		final int percent = (int) Math.round(100 * GoalMath.fraction(g.baseline, value, goalValue));
		final double perAction = actionXp.applyAsDouble(g.label);
		return new Progress(left, percent, xpPerHour(g.label, nowSeconds), xpLastHour(g.label, nowSeconds),
			perAction, GoalMath.actionsLeft(left, perAction));
	}

	/** One line per unmet skill goal in {@code skill}, for the Skilling tab. */
	synchronized List<String> skillLines(String skill, long nowSeconds)
	{
		return skillLines(java.util.Collections.singleton(skill), nowSeconds);
	}

	/** One line per unmet skill goal in any of {@code skills} (the game's names), for the Combat tab. */
	synchronized List<String> skillLines(java.util.Collection<String> skills, long nowSeconds)
	{
		final List<String> out = new ArrayList<>();
		for (Goal g : goals)
		{
			if (g.skill() && skills.contains(g.label) && !g.reached)
			{
				out.add("Goal " + GoalMath.title(g) + ": " + GoalMath.summary(progress(g, nowSeconds)));
			}
		}
		return out;
	}

	/** The same unmet skill goals as compact rows (title, progress bar, one detail line) for the Live cards. */
	synchronized List<LiveVitals.GoalLine> skillGoals(java.util.Collection<String> skills, long nowSeconds)
	{
		final List<LiveVitals.GoalLine> out = new ArrayList<>();
		for (Goal g : goals)
		{
			if (g.skill() && skills.contains(g.label) && !g.reached)
			{
				final Progress p = progress(g, nowSeconds);
				out.add(new LiveVitals.GoalLine(GoalMath.title(g),
					new LiveVitals.Bar(Math.max(0, Math.min(1.0, p.percent / 100.0)), p.percent + "%",
						PanelStyle.ACCENT), GoalMath.compact(p)));
			}
		}
		return out;
	}

	synchronized List<LiveVitals.GoalLine> skillGoals(String skill, long nowSeconds)
	{
		return skillGoals(java.util.Collections.singleton(skill), nowSeconds);
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
			// The one place the stored list may go away: the player removed the last goal.
			if (goals.isEmpty())
			{
				storage.unset();
			}
			else
			{
				save();
			}
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
		sample(skill, experience, nowSeconds);
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

	/**
	 * Marks goals that have just been met and returns them. Saves only when one was met: an experience drop
	 * that meets nothing changes nothing worth writing, and writing then could overwrite the stored goals
	 * with a list that was never loaded.
	 */
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
		if (!met.isEmpty())
		{
			save();
		}
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
