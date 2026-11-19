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
 * Per the OSRS wiki "Impling" page (read 2026-11-19): a wielded butterfly net
 * needs the impling's level; barehanded needs that level + 10; an impling jar
 * is only required inside Puro-Puro, outside it the impling is looted at
 * once. An impling not in the table skips the level check, so an unknown name
 * can only hide the note, never show a wrong one.
 *
 * Pure: item names, a level, a name and a place in, the missing things out.
 */
final class ImplingGear
{
	/** Barehanded catching needs this many Hunter levels more than with a net. */
	static final int BAREHANDED_EXTRA = 10;

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
	 * @param name       the impling's name, e.g. "Baby impling"
	 * @param worn       the names of what is worn, any case; only a worn net counts
	 * @param inventory  the names of what is carried, any case
	 * @param level      the boosted Hunter level
	 * @param inPuroPuro whether the player is inside Puro-Puro
	 * @return what is missing, in badge order; empty when nothing is or the
	 *         target is not an impling
	 */
	static List<String> missing(String name, Collection<String> worn, Collection<String> inventory,
		int level, boolean inPuroPuro)
	{
		if (!isImpling(name))
		{
			return Collections.emptyList();
		}

		boolean net = false;
		for (String item : worn)
		{
			net |= item != null && item.toLowerCase(Locale.ROOT).endsWith("butterfly net");
		}
		boolean jar = false;
		for (String item : inventory)
		{
			jar |= item != null && item.equalsIgnoreCase("impling jar");
		}

		final List<String> out = new ArrayList<>(2);
		final int needed = levelFor(name);
		if (net)
		{
			if (needed > 0 && level < needed)
			{
				out.add(needed + " Hunter");
			}
		}
		else if (needed <= 0)
		{
			out.add("a butterfly net");
		}
		else if (level < needed + BAREHANDED_EXTRA)
		{
			out.add("a butterfly net or " + (needed + BAREHANDED_EXTRA) + " Hunter");
		}
		if (inPuroPuro && !jar)
		{
			out.add("an impling jar");
		}
		return out;
	}

	/**
	 * "Can't catch Baby impling: needs a butterfly net or 27 Hunter", or null
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
