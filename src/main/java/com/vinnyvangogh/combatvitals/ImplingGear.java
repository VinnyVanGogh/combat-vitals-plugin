package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Whether a Catch click on an impling can work, so a click that cannot is
 * never counted as hunting (Board, 2026-11-19: a Hunter 1 player with no net
 * clicked a baby impling and the badge read "Not hunting" while they fought a
 * Highwayman).
 *
 * An impling needs a butterfly net, carried or wielded, an impling jar, and
 * the Hunter level for that kind. Levels are the OSRS wiki "Impling" page's
 * from memory and not re-read this session; an impling not in the table
 * skips the level check, so an unknown name can only hide the note, never
 * show a wrong one.
 *
 * Pure: item names, a level and a name in, the missing things out.
 */
final class ImplingGear
{
	/** Impling kind (lower case, without "impling") to the Hunter level it needs. */
	private static final Map<String, Integer> LEVELS = new LinkedHashMap<>();

	static
	{
		LEVELS.put("baby", 17);
		LEVELS.put("young", 22);
		LEVELS.put("gourmet", 28);
		LEVELS.put("earth", 36);
		LEVELS.put("essence", 42);
		LEVELS.put("eclectic", 50);
		LEVELS.put("nature", 58);
		LEVELS.put("magpie", 65);
		LEVELS.put("ninja", 74);
		LEVELS.put("crystal", 80);
		LEVELS.put("dragon", 83);
		LEVELS.put("lucky", 89);
	}

	private ImplingGear()
	{
	}

	/** @return whether the target's name is an impling */
	static boolean isImpling(String name)
	{
		return name != null && name.toLowerCase(Locale.ROOT).contains("impling");
	}

	/**
	 * @param name      the impling's name, e.g. "Baby impling"
	 * @param itemNames everything carried or worn, any case
	 * @param level     the boosted Hunter level
	 * @return what is missing, in badge order: net, jar, level; empty when
	 *         nothing is or the target is not an impling
	 */
	static List<String> missing(String name, Collection<String> itemNames, int level)
	{
		if (!isImpling(name))
		{
			return Collections.emptyList();
		}

		boolean net = false;
		boolean jar = false;
		for (String item : itemNames)
		{
			if (item == null)
			{
				continue;
			}
			final String lower = item.toLowerCase(Locale.ROOT);
			net |= lower.endsWith("butterfly net");
			jar |= lower.equals("impling jar");
		}

		final List<String> out = new ArrayList<>(3);
		if (!net)
		{
			out.add("a butterfly net");
		}
		if (!jar)
		{
			out.add("an impling jar");
		}
		final int needed = levelFor(name);
		if (needed > 0 && level < needed)
		{
			out.add(needed + " Hunter");
		}
		return out;
	}

	/**
	 * "Can't catch Baby impling: needs 17 Hunter / a butterfly net", or null
	 * when nothing is missing.
	 */
	static String text(String name, List<String> missing)
	{
		return missing == null || missing.isEmpty()
			? null : "Can't catch " + name + ": needs " + String.join(" / ", missing);
	}

	/** The Hunter level the named impling needs, or 0 when it is not in the table. */
	static int levelFor(String name)
	{
		final String lower = name.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Integer> kind : LEVELS.entrySet())
		{
			if (lower.startsWith(kind.getKey() + " impling"))
			{
				return kind.getValue();
			}
		}
		return 0;
	}
}
