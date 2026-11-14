package com.vinnyvangogh.combatvitals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Constants;
import net.runelite.api.Skill;

/**
 * What an action uses up, learned from the inventory, so the badge can say
 * how many are left: "14 left, done in ~25s".
 *
 * No recipe list. Each experience drop compares the inventory with the one
 * at the previous drop; whatever went down is what one action takes (a
 * grimy herb; a clean herb and a vial; five bars; a nature rune and the
 * item). Actions left is the scarcest of those, in whole actions. An
 * inventory that lands a tick after the experience is picked up on the next
 * drop. The bank opening rebases, so a deposit is never an input.
 *
 * The pace is the gap between drops, smoothed; a gap too long to be the
 * action itself (a walk, a bank trip) is not counted.
 *
 * Pure: inventories as item id to quantity, and ticks, in; counts out.
 */
final class ProductionInputs
{
	/** A gap between drops longer than this (about 12 s) is a pause, not the pace. */
	static final int MAX_PACE_TICKS = 20;

	private Skill skill;
	private Map<Integer, Integer> baseline;
	private final Map<Integer, Integer> perAction = new HashMap<>();
	private int lastActionTick = -1;
	private double ticksPerAction = -1;

	/**
	 * One action in {@code skill}: experience arrived this tick.
	 *
	 * @param before the inventory at the end of the previous tick
	 * @param now    the inventory now
	 */
	void action(Skill skill, Map<Integer, Integer> before, Map<Integer, Integer> now, int tick)
	{
		if (skill != this.skill)
		{
			forget();
			this.skill = skill;
		}

		final Map<Integer, Integer> used = decreases(baseline != null ? baseline : before, now);
		if (!used.isEmpty())
		{
			perAction.clear();
			perAction.putAll(used);
		}
		baseline = new HashMap<>(now);

		if (lastActionTick >= 0)
		{
			final int gap = tick - lastActionTick;
			if (gap > 0 && gap <= MAX_PACE_TICKS)
			{
				ticksPerAction = ticksPerAction < 0 ? gap : 0.7 * ticksPerAction + 0.3 * gap;
			}
		}
		lastActionTick = tick;
	}

	/** The bank opened: the next action compares with the tick before it, not with the last action. */
	void rebase()
	{
		baseline = null;
		lastActionTick = -1;
	}

	/** Whole actions the inventory still holds inputs for, or -1 while not known for this skill. */
	int left(Skill skill, Map<Integer, Integer> inventory)
	{
		if (skill != this.skill || perAction.isEmpty())
		{
			return -1;
		}
		int left = Integer.MAX_VALUE;
		for (Map.Entry<Integer, Integer> input : perAction.entrySet())
		{
			left = Math.min(left, inventory.getOrDefault(input.getKey(), 0) / input.getValue());
		}
		return left;
	}

	/** "14 left, done in ~25s", "14 left" while the pace is unknown, "None left", or null when unknown. */
	String label(Skill skill, Map<Integer, Integer> inventory)
	{
		final int left = left(skill, inventory);
		if (left < 0)
		{
			return null;
		}
		if (left == 0)
		{
			return "None left";
		}
		if (ticksPerAction < 0)
		{
			return left + " left";
		}
		final long millis = Math.round(left * ticksPerAction * Constants.GAME_TICK_LENGTH);
		return left + " left, done in ~" + SessionRow.duration(millis);
	}

	void forget()
	{
		skill = null;
		baseline = null;
		perAction.clear();
		lastActionTick = -1;
		ticksPerAction = -1;
	}

	private static Map<Integer, Integer> decreases(Map<Integer, Integer> from, Map<Integer, Integer> to)
	{
		if (from == null)
		{
			return Collections.emptyMap();
		}
		final Map<Integer, Integer> used = new HashMap<>();
		for (Map.Entry<Integer, Integer> item : from.entrySet())
		{
			final int drop = item.getValue() - to.getOrDefault(item.getKey(), 0);
			if (drop > 0)
			{
				used.put(item.getKey(), drop);
			}
		}
		return used;
	}
}
