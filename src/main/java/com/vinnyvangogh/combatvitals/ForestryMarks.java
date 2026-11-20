package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The parts of Forestry events that are on the scene right now, kept so the
 * highlight overlay can outline them: Rising Roots and Struggling Sapling
 * (objects) and the other events' NPCs. Observation only, the same spawn ids
 * the base RuneLite Woodcutting plugin (BSD 2-Clause) highlights, which
 * {@link SkillingEvents} already carries for the alerts.
 *
 * Unlike the alerts, nothing here waits for you to be chopping or near: a
 * mark is there while the thing is.
 *
 * Pure: whatever the client spawned in, whatever is wanted out. Client thread only.
 */
final class ForestryMarks
{
	private final Map<SkillingEvents.Forestry, Set<Object>> marks = new EnumMap<>(SkillingEvents.Forestry.class);

	void spawned(SkillingEvents.Forestry kind, Object thing)
	{
		if (kind != null && thing != null)
		{
			marks.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(thing);
		}
	}

	void despawned(SkillingEvents.Forestry kind, Object thing)
	{
		final Set<Object> set = marks.get(kind);
		if (set != null)
		{
			set.remove(thing);
		}
	}

	void clear()
	{
		marks.clear();
	}

	boolean isEmpty()
	{
		for (Set<Object> set : marks.values())
		{
			if (!set.isEmpty())
			{
				return false;
			}
		}
		return true;
	}

	/** The things to outline, for the event kinds the player has switched on. */
	List<Object> wanted(Predicate<SkillingEvents.Forestry> enabled)
	{
		if (isEmpty())
		{
			return Collections.emptyList();
		}
		final List<Object> out = new ArrayList<>();
		for (Map.Entry<SkillingEvents.Forestry, Set<Object>> entry : marks.entrySet())
		{
			if (enabled.test(entry.getKey()))
			{
				out.addAll(entry.getValue());
			}
		}
		return out;
	}
}
