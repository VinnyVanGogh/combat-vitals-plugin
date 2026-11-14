package com.vinnyvangogh.combatvitals;

import java.util.HashMap;
import java.util.Map;

/**
 * Which item last landed in the inventory, for the voice line's "yew logs".
 * Like {@link InventoryCatches}, but for any item: the first inventory seen
 * is a baseline, so logging in with a full inventory gains nothing.
 */
final class GainedItems
{
	private Map<Integer, Integer> counts;

	/** @return the id of an item whose count rose since the last call, or -1 */
	int observe(int[] ids, int[] quantities)
	{
		final Map<Integer, Integer> now = new HashMap<>();
		for (int i = 0; i < ids.length && i < quantities.length; i++)
		{
			if (ids[i] > 0 && quantities[i] > 0)
			{
				now.merge(ids[i], quantities[i], Integer::sum);
			}
		}

		final Map<Integer, Integer> before = counts;
		counts = now;
		if (before == null)
		{
			return -1;
		}
		for (Map.Entry<Integer, Integer> entry : now.entrySet())
		{
			if (entry.getValue() > before.getOrDefault(entry.getKey(), 0))
			{
				return entry.getKey();
			}
		}
		return -1;
	}

	void clear()
	{
		counts = null;
	}
}
