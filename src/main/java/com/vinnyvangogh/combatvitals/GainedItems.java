package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which item last landed in the inventory, for the voice line's "yew logs".
 * Like {@link InventoryCatches}, but for any item: the first inventory seen
 * is a baseline, so logging in with a full inventory gains nothing.
 */
final class GainedItems
{
	private Map<Integer, Integer> counts;
	private List<Integer> gained = Collections.emptyList();

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
		gained = new ArrayList<>();
		if (before == null)
		{
			return -1;
		}
		for (Map.Entry<Integer, Integer> entry : now.entrySet())
		{
			if (entry.getValue() > before.getOrDefault(entry.getKey(), 0))
			{
				gained.add(entry.getKey());
			}
		}
		return gained.isEmpty() ? -1 : gained.get(0);
	}

	/** Every item whose count rose in the last {@link #observe}, not only the first. */
	List<Integer> gained()
	{
		return Collections.unmodifiableList(gained);
	}

	void clear()
	{
		counts = null;
		gained = Collections.emptyList();
	}
}
