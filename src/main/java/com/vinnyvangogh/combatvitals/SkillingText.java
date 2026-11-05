package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.Locale;
import net.runelite.api.Constants;
import net.runelite.api.Skill;

/**
 * Turns the skilling snapshot into the badge text.
 *
 * Pure: no client, no config object, no state, so the wording is unit
 * testable without a running game.
 */
final class SkillingText
{
	/**
	 * The level written over a fish you cannot catch yet. Red, and "Lv 15"
	 * rather than a bare "15", which read as a quantity (Board, 2026-11-04).
	 */
	static final Color LOCKED_LEVEL = new Color(255, 64, 64);

	private SkillingText()
	{
	}

	/** "Lv 15", over a dimmed fish. */
	static String lockedLabel(int level)
	{
		return "Lv " + level;
	}

	/** "Needs Fishing 15", the hover text on a dimmed fish. */
	static String lockedTooltip(String skillName, int level)
	{
		return "Needs " + skillName + " " + level;
	}

	/** "Not fishing": the header once you stop. */
	static String idleHeader(Skill skill)
	{
		return "Not " + activity(skill);
	}

	/** The activity's name in a sentence, "fishing", "hunting". */
	private static String activity(Skill skill)
	{
		switch (skill)
		{
			case HUNTER:
				return "hunting";
			case RUNECRAFT:
				return "runecrafting";
			default:
				return skill.getName().toLowerCase(Locale.ROOT);
		}
	}

	/** What one action is called in the session count: "Caught 12". */
	private static String done(Skill skill)
	{
		switch (skill)
		{
			case FISHING:
			case HUNTER:
				return "Caught";
			case WOODCUTTING:
				return "Chopped";
			case MINING:
				return "Mined";
			case THIEVING:
				return "Stolen";
			case RUNECRAFT:
				return "Crafted";
			default:
				return "Actions";
		}
	}

	/**
	 * The session row: "Caught 12 | 30,000 xp/h | 600/h".
	 *
	 * @param xpPerHour      -1 while not yet known
	 * @param actionsPerHour -1 while not yet known
	 * @param count          show the session count
	 * @param rates          show the hourly rates
	 * @return the row, or null when both halves are off
	 */
	static String session(Skill skill, int actions, int xpPerHour, int actionsPerHour, boolean count, boolean rates)
	{
		if (!count && !rates)
		{
			return null;
		}

		final StringBuilder text = new StringBuilder();
		if (count)
		{
			text.append(done(skill)).append(' ').append(actions);
		}
		if (rates)
		{
			if (text.length() > 0)
			{
				text.append(" | ");
			}
			text.append(xpPerHour < 0 ? "--" : String.format("%,d", xpPerHour)).append(" xp/h");
			if (actionsPerHour >= 0)
			{
				text.append(" | ").append(String.format("%,d", actionsPerHour)).append("/h");
			}
		}
		return text.toString();
	}

	/**
	 * @param skillName   the skill, e.g. "Fishing"
	 * @param real        base level
	 * @param boosted     boosted level now
	 * @param seconds     seconds until the boost drops by one, or -1 when unknown
	 * @param estimated   the timer is anchored on login, not on an observed drop
	 * @param specPercent remaining special attack energy, or -1 when no spec tool is wielded
	 * @param source      where the boost came from, or null
	 * @param warning     the cue to show
	 * @return the text to draw
	 */
	static String text(String skillName, int real, int boosted, int seconds, boolean estimated,
		int specPercent, String source, SkillingState.Warning warning)
	{
		final StringBuilder text = new StringBuilder(skillName).append(' ').append(boosted);

		if (warning == SkillingState.Warning.EXPIRED)
		{
			text.append(" boost ended");
		}
		else if (boosted != real)
		{
			final int boost = boosted - real;
			text.append(" (").append(boost > 0 ? "+" : "").append(boost).append(')');

			if (warning == SkillingState.Warning.DROPPED)
			{
				text.append(" dropped");
			}
			else if (boost > 0 && seconds >= 0)
			{
				text.append(' ').append(estimated ? "~" : "").append(seconds).append('s');
			}
		}

		if (specPercent >= 0)
		{
			text.append(" | Spec ").append(specPercent).append('%');
		}

		if (source != null && boosted > real && warning != SkillingState.Warning.EXPIRED)
		{
			text.append(" | ").append(source);
		}

		return text.toString();
	}

	/**
	 * Seconds left, counted down between ticks so the number moves every
	 * second rather than every 0.6 s. Rounds up: a boost with 0.4 s left has
	 * not dropped yet.
	 *
	 * @param ticks        ticks until the drop
	 * @param msSinceTick  milliseconds since the last game tick
	 */
	static int seconds(int ticks, long msSinceTick)
	{
		if (ticks < 0)
		{
			return -1;
		}
		final long ms = ticks * (long) Constants.GAME_TICK_LENGTH - Math.max(0, msSinceTick);
		return (int) Math.max(0, (ms + 999) / 1000);
	}
}
