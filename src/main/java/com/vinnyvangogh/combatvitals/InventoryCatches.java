package com.vinnyvangogh.combatvitals;

import java.util.HashMap;
import java.util.Map;

/**
 * Spots a fish landing in the inventory, for when the catch message is
 * filtered out of the chat or the fish stacks (minnows, karambwanji).
 *
 * Only the fish in {@link FishingCatches} are counted, so a full inventory
 * costs one pass over its slots per change. The first snapshot is a
 * baseline, never a catch: a plugin enabled with a bag of sharks has not
 * just caught one.
 *
 * Pure: ids and quantities in, an item id out. Not thread safe; the plugin
 * touches it from the client thread only.
 */
final class InventoryCatches
{
	private Map<Integer, Integer> counts;

	/**
	 * @param ids        the item id in each inventory slot
	 * @param quantities the quantity in each slot, parallel to {@code ids}
	 * @return the item id of the fish just caught, the highest level one if
	 *         two landed at once, or -1 when nothing was
	 */
	int observe(int[] ids, int[] quantities)
	{
		final Map<Integer, Integer> now = new HashMap<>();
		for (int i = 0; i < ids.length && i < quantities.length; i++)
		{
			if (FishingCatches.byItem(ids[i]) != null)
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

		int caught = -1;
		int caughtLevel = -1;
		for (Map.Entry<Integer, Integer> entry : now.entrySet())
		{
			if (entry.getValue() > before.getOrDefault(entry.getKey(), 0))
			{
				final int level = FishingCatches.byItem(entry.getKey()).getLevel();
				if (level > caughtLevel)
				{
					caught = entry.getKey();
					caughtLevel = level;
				}
			}
		}
		return caught;
	}

	void clear()
	{
		counts = null;
	}
}
