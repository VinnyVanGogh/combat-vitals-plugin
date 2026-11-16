package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Constants;

/**
 * Three checks on the Blast Furnace pump (Board, 2026-11-14), only from what
 * the game already shows the player.
 *
 * <h3>World</h3>
 * Pumping only pays where other players keep the furnace going. The OSRS
 * Wiki: "The current unofficial world for this is 302 ... Alternatively world
 * 303 can be as active as world 302"
 * (https://oldschool.runescape.wiki/w/Pump_(Blast_Furnace), read 2026-11-14).
 * The list is the user's; empty turns the check off. The alert fires once per
 * world: hopping clears it ({@link #clear}).
 *
 * <h3>Nobody stoking</h3>
 * "In order to receive experience, the Blast Furnace must be filled with
 * Coke. In the event that the Blast Furnace is empty, experience gains will
 * be halted until it is filled" (same page). Pumping gives 2 Strength
 * experience every tick, so the pumping animation playing for
 * {@link #NO_XP_TICKS} ticks with none means the stove is out and nobody is
 * refuelling it. Experience again clears it and arms the next alert.
 *
 * <h3>Coke</h3>
 * The stove looks different by how much coke is in it: scenery
 * {@link #STOVE_NOT_MUCH} "There's not much coke in the stove", {@link #STOVE_FAIR}
 * "a fair amount", {@link #STOVE_LOTS} "lots"; 30 spadefuls fill it
 * (https://oldschool.runescape.wiki/w/Stove_(Blast_Furnace), infobox id1 to
 * id3, read 2026-11-14). The game has no named varbit for the coke itself
 * (RuneLite's gameval VarbitID, read 2026-11-14), so the burn rate is
 * measured: the ticks between two drops in a row, with no refuel between,
 * are one step. From a drop, the coke left is that step for each look still
 * to burn through, including the one just entered. A refuel keeps the rate
 * but loses the place in the step until the next drop. Nothing is shown
 * until a step has been timed, so a stove first seen at login never counts
 * down or alerts. The steps are assumed even, which is unverified in game.
 *
 * Pure: ticks in, alerts out, no client. Not thread safe; the plugin touches
 * it from the client thread only.
 */
final class PumpChecks
{
	/** The worlds a new profile checks against. */
	static final String DEFAULT_WORLDS = "302, 303";

	static final int STOVE_NOT_MUCH = 9085;
	static final int STOVE_FAIR = 9086;
	static final int STOVE_LOTS = 9087;

	/** Ticks of the pumping animation without experience before the stove reads as empty (~5 s). */
	static final int NO_XP_TICKS = 8;

	private static final Pattern NUMBER = Pattern.compile("\\d{1,4}");
	private static final TrainingMethod PUMP = TrainingMethod.BLAST_FURNACE_PUMP;

	private int world = -1;
	private boolean wrongWorld;
	private boolean worldWarned;

	private int noXpTicks;
	private boolean noStoking;
	private boolean stokingWarned;

	/** The stove's look: 0 not much, 1 fair, 2 lots; -1 unseen. */
	private int stoveLevel = -1;
	/** The tick of the last drop with no refuel since, or -1. */
	private int dropTick = -1;
	/** Ticks one look lasts, from two drops in a row, or -1 until timed. */
	private int stepTicks = -1;
	private int cokeTicksLeft = -1;
	private boolean cokeWarned;

	/** "302, 303" to its numbers; anything not a world number is skipped. */
	static Set<Integer> worlds(String text)
	{
		if (text == null)
		{
			return Collections.emptySet();
		}
		final Set<Integer> out = new HashSet<>();
		final Matcher m = NUMBER.matcher(text);
		while (m.find())
		{
			final boolean whole = (m.start() == 0 || !Character.isDigit(text.charAt(m.start() - 1)))
				&& (m.end() == text.length() || !Character.isDigit(text.charAt(m.end())));
			if (whole)
			{
				out.add(Integer.parseInt(m.group()));
			}
		}
		return out;
	}

	/** The stove's coke by its look, 0 to 2, or -1 for any other object. */
	static int stoveLevel(int objectId)
	{
		switch (objectId)
		{
			case STOVE_NOT_MUCH:
				return 0;
			case STOVE_FAIR:
				return 1;
			case STOVE_LOTS:
				return 2;
			default:
				return -1;
		}
	}

	static boolean isStove(int objectId)
	{
		return stoveLevel(objectId) >= 0;
	}

	/** The stove as seen this tick, -1 when not in view. */
	void stove(int level, int tick)
	{
		if (level < 0 || level == stoveLevel)
		{
			return;
		}
		if (stoveLevel >= 0 && level == stoveLevel - 1)
		{
			if (dropTick >= 0)
			{
				stepTicks = tick - dropTick;
			}
			dropTick = tick;
		}
		else
		{
			// First sight, a refuel or a jump: the rate stands, the place in the step is lost.
			dropTick = -1;
			if (stoveLevel >= 0 && level > stoveLevel)
			{
				cokeWarned = false;
			}
		}
		stoveLevel = level;
	}

	/**
	 * Once per game tick, after the training session's.
	 *
	 * @param running    a pump session is on
	 * @param animating  the local player has an animation playing
	 * @param gained     Strength experience came this tick
	 * @param worlds     the pumping worlds, empty for no world check
	 * @param warnMillis how long before the coke runs out to warn
	 */
	List<ActivityAlerts.Alert> tick(int tick, boolean running, boolean animating, boolean gained, int world,
		Set<Integer> worlds, long warnMillis)
	{
		this.world = world;
		if (!running)
		{
			wrongWorld = false;
			noXpTicks = 0;
			noStoking = false;
			cokeTicksLeft = -1;
			return Collections.emptyList();
		}

		final List<ActivityAlerts.Alert> out = new ArrayList<>(0);
		add(out, checkWorld(worlds));
		add(out, checkStoking(animating, gained));
		add(out, checkCoke(tick, warnMillis));
		return out;
	}

	private ActivityAlerts.Type checkWorld(Set<Integer> worlds)
	{
		wrongWorld = !worlds.isEmpty() && !worlds.contains(world);
		if (!wrongWorld || worldWarned)
		{
			return null;
		}
		worldWarned = true;
		return ActivityAlerts.Type.WRONG_WORLD;
	}

	private ActivityAlerts.Type checkStoking(boolean animating, boolean gained)
	{
		noXpTicks = gained || !animating ? 0 : noXpTicks + 1;
		noStoking = noXpTicks >= NO_XP_TICKS;
		stokingWarned &= !gained;
		if (!noStoking || stokingWarned)
		{
			return null;
		}
		stokingWarned = true;
		return ActivityAlerts.Type.NO_STOKING;
	}

	private ActivityAlerts.Type checkCoke(int tick, long warnMillis)
	{
		cokeTicksLeft = stepTicks <= 0 || dropTick < 0 ? -1
			: Math.max(0, (stoveLevel + 1) * stepTicks - (tick - dropTick));
		if (cokeTicksLeft < 0 || cokeTicksLeft * (long) Constants.GAME_TICK_LENGTH > warnMillis || cokeWarned)
		{
			return null;
		}
		cokeWarned = true;
		return ActivityAlerts.Type.COKE_LOW;
	}

	/** The connection came back: the experience gap before it does not count. */
	void reconnected()
	{
		noXpTicks = 0;
		noStoking = false;
	}

	/** Logged out or hopped: a new world and a stove not yet seen. */
	void clear()
	{
		world = -1;
		wrongWorld = false;
		worldWarned = false;
		noXpTicks = 0;
		noStoking = false;
		stokingWarned = false;
		stoveLevel = -1;
		dropTick = -1;
		stepTicks = -1;
		cokeTicksLeft = -1;
		cokeWarned = false;
	}

	boolean wrongWorld()
	{
		return wrongWorld;
	}

	boolean noStoking()
	{
		return noStoking;
	}

	/** Ticks until the stove runs out as of the last tick, or -1 until known. */
	int cokeTicksLeft()
	{
		return cokeTicksLeft;
	}

	/** "Coke runs out in 1:30", or null until known. */
	String cokeText()
	{
		return cokeTicksLeft < 0 ? null
			: "Coke runs out in " + SkillingText.clock(cokeTicksLeft * (long) Constants.GAME_TICK_LENGTH);
	}

	/** The badge's warning line, "World 420: not a pumping world | Stove: no one stoking", or null. */
	String warning()
	{
		final String w = wrongWorld ? "World " + world + ": not a pumping world" : null;
		final String s = noStoking ? "Stove: no one stoking" : null;
		if (w == null)
		{
			return s;
		}
		return s == null ? w : w + " | " + s;
	}

	private static void add(List<ActivityAlerts.Alert> out, ActivityAlerts.Type type)
	{
		if (type != null)
		{
			out.add(new ActivityAlerts.Alert(type, PUMP.skill(), PUMP));
		}
	}
}
