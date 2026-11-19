package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Experience;

/**
 * One goal of the Goals page: our own, not the game's XP-drop goals.
 * {@code baseline} is where the count stood when the goal was made, so the
 * bar shows progress made toward it. Skill goals count experience; item,
 * boss kill count, daily and weekly goals count whatever {@code current}
 * holds (kill counts are read from the chat, the rest are ticked by hand).
 *
 * Stored per account as one line per goal, tab-separated, in one hidden
 * config key ({@link #encode(List)}); the label is stripped of tabs and
 * newlines, so a line never breaks.
 */
final class Goal
{
	enum Type
	{
		SKILL_LEVEL, SKILL_XP, ITEM, BOSS_KC, DAILY, WEEKLY
	}

	final long id;
	final Type type;
	/** The skill's name for a skill goal, else what the player typed. */
	final String label;
	/** A level for SKILL_LEVEL, else a count or experience. */
	final long target;
	final long baseline;
	/** Counted by hand or from the chat; skill goals read the live experience instead. */
	final long current;
	/** Start of the day or week this count belongs to, epoch seconds; 0 for goals that never reset. */
	final long periodStart;
	final boolean reached;

	Goal(long id, Type type, String label, long target, long baseline, long current, long periodStart,
		boolean reached)
	{
		this.id = id;
		this.type = type;
		this.label = label.replace('\t', ' ').replace('\n', ' ').trim();
		this.target = target;
		this.baseline = baseline;
		this.current = current;
		this.periodStart = periodStart;
		this.reached = reached;
	}

	boolean skill()
	{
		return type == Type.SKILL_LEVEL || type == Type.SKILL_XP;
	}

	/** The value the goal is reached at, in the same unit as the count (experience for a level goal). */
	long goalValue()
	{
		if (type == Type.SKILL_LEVEL)
		{
			return Experience.getXpForLevel((int) Math.max(1, Math.min(target, Experience.MAX_REAL_LEVEL)));
		}
		return target;
	}

	Goal withCurrent(long value)
	{
		return new Goal(id, type, label, target, baseline, value, periodStart, reached);
	}

	Goal withReached(boolean value)
	{
		return new Goal(id, type, label, target, baseline, current, periodStart, value);
	}

	Goal reset(long newPeriodStart)
	{
		return new Goal(id, type, label, target, 0, 0, newPeriodStart, false);
	}

	String line()
	{
		return id + "\t" + type.name() + "\t" + label + "\t" + target + "\t" + baseline + "\t" + current + "\t"
			+ periodStart + "\t" + (reached ? 1 : 0);
	}

	static String encode(List<Goal> goals)
	{
		final StringBuilder out = new StringBuilder();
		for (Goal g : goals)
		{
			out.append(g.line()).append('\n');
		}
		return out.toString();
	}

	/** Reads what {@link #encode} wrote; a line that does not parse is dropped, never fatal. */
	static List<Goal> decode(String text)
	{
		final List<Goal> goals = new ArrayList<>();
		if (text == null)
		{
			return goals;
		}
		for (String line : text.split("\n"))
		{
			final String[] f = line.split("\t", -1);
			if (f.length != 8)
			{
				continue;
			}
			try
			{
				goals.add(new Goal(Long.parseLong(f[0]), Type.valueOf(f[1]), f[2], Long.parseLong(f[3]),
					Long.parseLong(f[4]), Long.parseLong(f[5]), Long.parseLong(f[6]), "1".equals(f[7])));
			}
			catch (IllegalArgumentException e)
			{
				// A hand-edited or future line: skip it rather than lose the others.
			}
		}
		return goals;
	}
}
