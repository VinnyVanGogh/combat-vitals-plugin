package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What a fishing option needs, and what of it the player is missing, so the
 * badge can say so the moment the spot is clicked instead of after the
 * game's "You need a Fishing Rod to bait these fish." (Board, 2026-11-04).
 *
 * Read only: the click goes through to the game unchanged.
 *
 * Requirements per option, from the OSRS wiki "Fishing" and "Barbarian
 * Training" pages, read 2026-11-04:
 * <ul>
 * <li>Net / Small Net: a small fishing net.</li>
 * <li>Big Net: a big fishing net.</li>
 * <li>Bait: a fishing rod (plain, pearl or oily) and bait.</li>
 * <li>Lure: a fly fishing rod and feathers.</li>
 * <li>Cage: a lobster pot.</li>
 * <li>Harpoon: any harpoon, worn or carried; or nothing at all once
 * Barbarian Training has taught barehanded fishing, which needs 20
 * Fishing levels more than the fish does.</li>
 * <li>Use-rod: a barbarian rod and bait (feathers, fishing bait, fish
 * offcuts, roe or caviar).</li>
 * <li>Fish (karambwan): a karambwan vessel.</li>
 * </ul>
 *
 * Errs towards saying nothing. Items are matched by name, so every variant
 * of a tool counts, and every bait is accepted for every bait option: a
 * spot that wants one particular bait (sandworms, dark fishing bait) is
 * left to the game rather than risk a warning that is wrong. Any option not
 * listed gives no warning.
 *
 * Pure: item names and levels in, the missing items out.
 */
final class FishingGear
{
	/** Barehanded fishing needs this many levels over the fish (wiki, "Barbarian Training"). */
	static final int BAREHANDED_EXTRA_LEVELS = 20;

	/** The lowest barehanded catch, tuna, for a harpoon spot whose fish are unknown. */
	static final int BAREHANDED_MIN_LEVEL = 55;

	/** The player's kit at the moment of the click. */
	static final class Kit
	{
		private final List<String> items;
		private final boolean barehandedTrained;
		private final int level;

		/**
		 * @param itemNames         every item carried or worn, any case
		 * @param barehandedTrained Barbarian Training's barehanded fishing is learned
		 * @param level             the boosted Fishing level
		 */
		Kit(Collection<String> itemNames, boolean barehandedTrained, int level)
		{
			final List<String> lower = new ArrayList<>(itemNames.size());
			for (String name : itemNames)
			{
				if (name != null)
				{
					lower.add(name.toLowerCase(Locale.ROOT));
				}
			}
			this.items = lower;
			this.barehandedTrained = barehandedTrained;
			this.level = level;
		}

		boolean has(Predicate<String> item)
		{
			for (String name : items)
			{
				if (item.test(name))
				{
					return true;
				}
			}
			return false;
		}
	}

	/** One thing an option needs: its name on the badge, and the items that count as it. */
	private static final class Need
	{
		private final String label;
		private final Predicate<String> item;

		Need(String label, Predicate<String> item)
		{
			this.label = label;
			this.item = item;
		}
	}

	/** Harpoon is the one option with a way round its tool, so it is not in the table. */
	private static final String HARPOON = "harpoon";

	/** Option, lower case, to what it needs, tool first. */
	private static final Map<String, List<Need>> NEEDS = new HashMap<>();

	static
	{
		final Need smallNet = new Need("Small fishing net", n -> n.equals("small fishing net"));
		NEEDS.put("net", Collections.singletonList(smallNet));
		NEEDS.put("small net", Collections.singletonList(smallNet));
		NEEDS.put("big net", Collections.singletonList(new Need("Big fishing net", n -> n.equals("big fishing net"))));
		NEEDS.put("bait", Arrays.asList(
			new Need("Fishing rod", FishingGear::baitRod),
			new Need("bait", FishingGear::bait)));
		NEEDS.put("lure", Arrays.asList(
			new Need("Fly fishing rod", n -> n.endsWith("fly fishing rod")),
			new Need("feathers", FishingGear::feather)));
		NEEDS.put("cage", Collections.singletonList(new Need("Lobster pot", n -> n.equals("lobster pot"))));
		NEEDS.put("use-rod", Arrays.asList(
			new Need("Barbarian rod", n -> n.endsWith("barbarian rod")),
			new Need("bait", FishingGear::barbarianBait)));
		NEEDS.put("fish", Collections.singletonList(new Need("Karambwan vessel", n -> n.equals("karambwan vessel"))));
	}

	private FishingGear()
	{
	}

	/**
	 * @param option the fishing option clicked: Net, Bait, Lure...; any case
	 * @param fish   what the option lands on this spot ({@link FishingCatches#forOption}),
	 *               empty when unknown, which skips the level check
	 * @return what is missing, in the order the badge names it: the tool,
	 *         the bait, the level; empty when nothing is, or the option is
	 *         not one this knows
	 */
	static List<String> missing(String option, List<FishingCatches.Fish> fish, Kit kit)
	{
		if (option == null)
		{
			return Collections.emptyList();
		}

		final String lower = option.toLowerCase(Locale.ROOT).trim();
		final List<Need> needs = NEEDS.get(lower);
		if (needs == null && !HARPOON.equals(lower))
		{
			return Collections.emptyList();
		}

		final List<String> out = new ArrayList<>(3);
		final int level = lowest(fish);
		if (needs == null)
		{
			harpoon(out, kit, level);
		}
		else
		{
			for (Need need : needs)
			{
				if (!kit.has(need.item))
				{
					out.add(need.label);
				}
			}
		}

		if (level > 0 && kit.level < level)
		{
			out.add("Fishing " + level);
		}
		return out;
	}

	/**
	 * "Needs: Fishing rod + bait", or null when nothing is missing.
	 */
	static String text(List<String> missing)
	{
		return missing == null || missing.isEmpty() ? null : "Needs: " + String.join(" + ", missing);
	}

	/**
	 * A harpoon, or barehanded: trained, and 20 levels over the spot's
	 * lowest harpoon fish (tuna's 55 when the spot is unknown).
	 */
	private static void harpoon(List<String> out, Kit kit, int lowestFish)
	{
		if (kit.has(n -> n.contains("harpoon")))
		{
			return;
		}

		final int barehanded = lowestFish > 0 ? lowestFish + BAREHANDED_EXTRA_LEVELS : BAREHANDED_MIN_LEVEL;
		if (kit.barehandedTrained && kit.level >= barehanded)
		{
			return;
		}
		out.add("Harpoon");
	}

	/** Fishing rod, Pearl fishing rod, Oily fishing rod; not a fly fishing rod. */
	private static boolean baitRod(String name)
	{
		return name.endsWith("fishing rod") && !name.endsWith("fly fishing rod");
	}

	/** Fishing bait (and dark), sandworms, fish chunks: every bait-option bait. */
	private static boolean bait(String name)
	{
		return name.endsWith("fishing bait") || name.equals("sandworms") || name.equals("fish chunks");
	}

	/** Feather, and the coloured and stripy ones. */
	private static boolean feather(String name)
	{
		return name.endsWith("feather") || name.endsWith("feathers");
	}

	private static boolean barbarianBait(String name)
	{
		return feather(name) || name.endsWith("fishing bait") || name.equals("fish offcuts")
			|| name.equals("roe") || name.equals("caviar");
	}

	/** The lowest level in the list, or 0 when it is empty. */
	private static int lowest(List<FishingCatches.Fish> fish)
	{
		int lowest = 0;
		for (FishingCatches.Fish f : fish)
		{
			if (lowest == 0 || f.getLevel() < lowest)
			{
				lowest = f.getLevel();
			}
		}
		return lowest;
	}
}
