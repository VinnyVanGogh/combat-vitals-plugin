package com.vinnyvangogh.combatvitals;

import net.runelite.api.Skill;

/**
 * Tracks the game's stat-drain cycle so the badge can say how long a boost
 * has left before it drops by one.
 *
 * The cycle runs in the background whether or not anything is boosted: every
 * 100 ticks (60 s) a boosted stat drops by one, or every 150 ticks (90 s)
 * once Preserve has been on for the tail of the cycle. The client is never
 * told where in the cycle it is, so the phase has to be inferred. Two things
 * anchor it: logging in, which the OSRS wiki says restarts the segment, and
 * an observed drop, which is the only hard evidence. The badge marks the
 * login anchor as an estimate until a drop confirms it.
 *
 * The preserve arithmetic is ported from RuneLite's Boosts plugin
 * ({@code BoostsPlugin#getChangeDownTicks}, BSD-2), which carries the only
 * tested model of how Preserve stretches the cycle.
 *
 * Pure: ticks in, ticks out, no client. Not thread safe; the plugin touches
 * it from the client thread only.
 */
final class BoostTimer
{
	/** Ticks per drain cycle, and with Preserve. */
	static final int CYCLE = 100;
	static final int PRESERVE_CYCLE = 150;

	/** Tick the cycle last (re)started, or -1 while unknown. */
	private int lastChangeDown = -1;

	/** Whether an observed drop, rather than login, set the phase. */
	private boolean confirmed;

	private boolean preserveBeenActive;

	/** The non-combat stats share one drain timer; combat stats run another. */
	static boolean onSkillingTimer(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
			case STRENGTH:
			case DEFENCE:
			case RANGED:
			case MAGIC:
			case HITPOINTS:
			case PRAYER:
				return false;
			default:
				return true;
		}
	}

	/** Logging in restarts the segment. An estimate until a drop confirms it. */
	void anchorLogin(int tick)
	{
		lastChangeDown = tick;
		confirmed = false;
		preserveBeenActive = false;
	}

	/** A boosted stat was seen dropping by one: the cycle restarted now. */
	void anchorDrop(int tick)
	{
		lastChangeDown = tick;
		confirmed = true;
	}

	boolean isKnown()
	{
		return lastChangeDown != -1;
	}

	boolean isConfirmed()
	{
		return confirmed;
	}

	/**
	 * Ticks until the next drop, or -1 while the phase is unknown.
	 *
	 * Call once per tick: the preserve bookkeeping is stateful, exactly as in
	 * the Boosts plugin it is ported from.
	 */
	int ticksUntilDrop(int tick, boolean preserveActive)
	{
		if (lastChangeDown == -1)
		{
			return -1;
		}

		final int since = tick - lastChangeDown;

		// Preserve only extends the cycle if it was on for the final quarter,
		// seconds 45 to 75, after which the cycle is 150 ticks long.
		if ((preserveActive && (since < 75 || preserveBeenActive)) || since > 125)
		{
			preserveBeenActive = true;
			return PRESERVE_CYCLE - since;
		}

		preserveBeenActive = false;
		return since > CYCLE ? 125 - since : CYCLE - since;
	}

	/** Length of the cycle currently in force, for drawing a bar that drains. */
	int cycleLength()
	{
		return preserveBeenActive ? PRESERVE_CYCLE : CYCLE;
	}

	/**
	 * The cycle ran out without a drop being seen, because nothing was
	 * boosted. It restarts now, keeping whatever confidence it had.
	 */
	void roll(int tick)
	{
		if (lastChangeDown != -1)
		{
			lastChangeDown = tick;
		}
	}

	void clear()
	{
		lastChangeDown = -1;
		confirmed = false;
		preserveBeenActive = false;
	}
}
