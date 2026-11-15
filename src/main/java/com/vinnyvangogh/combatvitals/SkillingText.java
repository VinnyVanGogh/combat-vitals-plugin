package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
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

	/** "Not pumping" for a training method, or the skill's header without one. */
	static String idleHeader(Skill skill, TrainingMethod method)
	{
		return method == null ? idleHeader(skill) : "Not " + method.verb();
	}

	/** "Pumping (Blast Furnace) | Strength": the header's name for a training method, or the skill's name. */
	static String headerName(Skill skill, TrainingMethod method)
	{
		return method == null ? skill.getName() : method.label() + " | " + skill.getName();
	}

	/**
	 * "AFK logout in 4:12 | Pump stops in 97:30": the training method's line,
	 * either half left out while unknown, null with both. Any input resets the
	 * game's idle logout (a click, a camera drag, typing), not only clicking
	 * the method's object, so the first half reads "Active" while the player is
	 * (Board, 2026-11-14) and counts down only once they have been idle a while.
	 *
	 * @param reclickMillis until the idle logout, or -1
	 * @param active        input within {@link TrainingSession#ACTIVE_MS}
	 * @param thing         what the game stops ("Pump"), or null for plain "Stops in"
	 * @param stopTicks     game ticks until the game stops the method, or -1
	 */
	static String methodTimers(long reclickMillis, boolean active, String thing, int stopTicks)
	{
		final String reclick = reclickMillis < 0 ? null : active ? "Active" : "AFK logout in " + clock(reclickMillis);
		final String stops = stopTicks < 0 ? null
			: (thing == null ? "Stops in " : thing + " stops in ") + clock(stopTicks * (long) Constants.GAME_TICK_LENGTH);
		if (reclick == null)
		{
			return stops;
		}
		return stops == null ? reclick : reclick + " | " + stops;
	}

	/** "97:05": whole minutes, then seconds, rounded up so 0:00 means now. */
	static String clock(long millis)
	{
		final long seconds = (Math.max(0, millis) + 999) / 1000;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}

	/**
	 * "Not woodcutting | in combat": the header with the fight's tag
	 * ({@link BadgeCombat#tag}), or the header alone without one. The bar is
	 * the header's own separator, the one Spec and the boost source use.
	 */
	static String withCombatTag(String header, String tag)
	{
		return tag == null ? header : header + " | " + tag;
	}

	/** The activity's name in a sentence, "fishing", "hunting". */
	private static String activity(Skill skill)
	{
		final String named = ACTIVITY.get(skill);
		return named != null ? named : skill.getName().toLowerCase(Locale.ROOT);
	}

	/** Skills whose name is not already the activity: "Not prayer" reads wrong. */
	private static final Map<Skill, String> ACTIVITY = new EnumMap<>(Skill.class);

	static
	{
		ACTIVITY.put(Skill.HUNTER, "hunting");
		ACTIVITY.put(Skill.RUNECRAFT, "runecrafting");
		ACTIVITY.put(Skill.HERBLORE, "doing herblore");
		ACTIVITY.put(Skill.PRAYER, "training prayer");
		ACTIVITY.put(Skill.MAGIC, "casting");
		ACTIVITY.put(Skill.AGILITY, "training agility");
		ACTIVITY.put(Skill.CONSTRUCTION, "building");
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
