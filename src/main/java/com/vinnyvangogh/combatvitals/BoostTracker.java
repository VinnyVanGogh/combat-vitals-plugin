package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import net.runelite.api.Skill;

/**
 * Turns the stream of stat updates into boost events.
 *
 * The client reports a skill's boosted level, not what happened to it. The
 * change is only meaningful against the previous value, so this remembers
 * one level per skill and classifies each update: the boost rose, dropped by
 * one, or dropped back to base.
 *
 * Pure: levels in, events out, no client.
 */
final class BoostTracker
{
	enum Change
	{
		/** No change, or a change this plugin does not describe. */
		NONE,
		/** The boosted level rose above base. */
		RISE,
		/** A boost dropped by one and is still above base. */
		DROP,
		/** A boost dropped by one and reached base. */
		EXPIRED
	}

	private final int[] lastBoosted = new int[Skill.values().length];

	BoostTracker()
	{
		Arrays.fill(lastBoosted, -1);
	}

	/** Whether the skill has been seen at all; the first update only seeds. */
	boolean isSeeded(Skill skill)
	{
		return lastBoosted[skill.ordinal()] != -1;
	}

	/** Seeds without reporting, for a plugin enabled mid-session. */
	void seed(Skill skill, int boosted)
	{
		lastBoosted[skill.ordinal()] = boosted;
	}

	/**
	 * @param skill   the skill that changed
	 * @param real    its base level
	 * @param boosted its boosted level now
	 * @return what happened to its boost
	 */
	Change observe(Skill skill, int real, int boosted)
	{
		final int last = lastBoosted[skill.ordinal()];
		lastBoosted[skill.ordinal()] = boosted;

		if (last == -1 || boosted == last)
		{
			return Change.NONE;
		}

		if (boosted > last && boosted > real)
		{
			return Change.RISE;
		}

		// Only a one-level fall from above base is the drain timer. A larger
		// fall is damage or a world hop, and a fall below base is a drain.
		if (boosted == last - 1 && last > real)
		{
			return boosted > real ? Change.DROP : Change.EXPIRED;
		}

		return Change.NONE;
	}

	void clear()
	{
		Arrays.fill(lastBoosted, -1);
	}
}
