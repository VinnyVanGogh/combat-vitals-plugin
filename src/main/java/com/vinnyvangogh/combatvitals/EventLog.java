package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One skilling event at a time (a Forestry event, say): its name, when it
 * started, the experience you earned in the skill while it ran, and the
 * rewards that came in. {@link #finish()} gives the line for the chat.
 *
 * Unverified in game: the reward names are from memory of the OSRS Wiki's
 * Forestry page; an item not listed here is simply not counted.
 *
 * Pure: names, experience and item names in, a summary out.
 */
final class EventLog
{
	private static final Set<String> REWARDS = new HashSet<>(Arrays.asList(
		"anima-infused bark", "sturdy beehive parts", "fox whistle", "petal garland", "funky shaped log",
		"log brace"));

	private String name;
	private int startTick;
	private int xp;
	private final Map<String, Integer> rewards = new LinkedHashMap<>();

	static boolean isReward(String item)
	{
		return item != null && REWARDS.contains(item.trim().toLowerCase(java.util.Locale.ROOT));
	}

	boolean isOpen()
	{
		return name != null;
	}

	/**
	 * An event began.
	 *
	 * @return the summary of the event this one replaced, or null when none was open
	 */
	String started(String eventName, int tick)
	{
		final String previous = finish();
		name = eventName;
		startTick = tick;
		return previous;
	}

	int startTick()
	{
		return startTick;
	}

	/** Experience earned in the event's skill; counted only while one is open. */
	void xp(int amount)
	{
		if (name != null && amount > 0)
		{
			xp += amount;
		}
	}

	/** An item came in; counted only while an event is open and the item is a known reward. */
	void reward(String item)
	{
		if (name != null && isReward(item))
		{
			rewards.merge(item.trim(), 1, Integer::sum);
		}
	}

	/** Closes the open event and says how it went, or null when none was open. */
	String finish()
	{
		if (name == null)
		{
			return null;
		}
		final StringBuilder out = new StringBuilder(name).append(": ");
		if (xp == 0 && rewards.isEmpty())
		{
			out.append("no contribution");
		}
		else
		{
			boolean first = true;
			if (xp > 0)
			{
				out.append(xp).append(" xp");
				first = false;
			}
			for (Map.Entry<String, Integer> reward : rewards.entrySet())
			{
				out.append(first ? "" : ", ").append(reward.getValue()).append(" x ").append(reward.getKey());
				first = false;
			}
		}
		clear();
		return out.toString();
	}

	void clear()
	{
		name = null;
		xp = 0;
		rewards.clear();
	}
}
