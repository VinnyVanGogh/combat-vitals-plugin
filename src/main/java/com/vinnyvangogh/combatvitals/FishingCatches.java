package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.FishingSpot;

/**
 * What a fishing spot gives for each of its menu options, and from what
 * level.
 *
 * The spot type comes from the base client's {@link FishingSpot}, which
 * maps spot NPC ids to the wiki's spot names ("Shrimp, Anchovies, Sardine,
 * Herring", "Lobster, Swordfish, Tuna", ...). The same option means
 * different fish on different spots: Net is shrimps on one and monkfish on
 * another, Harpoon is tuna on one and shark on another, so the table is
 * keyed on both.
 *
 * Levels and methods are from the OSRS wiki, "Fishing" (skill table) and
 * "Fishing spot" (options per spot type), read 2026-11-02. Spots the wiki
 * lists but this table does not (aerial fishing, quest spots, Civitas illa
 * Fortis park, squid) fall back to the skill and level alone.
 *
 * Pure and stateless.
 */
final class FishingCatches
{
	/** One catch: the name as the wiki and the catch message spell it, its level, and its raw item for the icon. */
	@Getter
	static final class Fish
	{
		private final String name;
		private final int level;
		private final int itemId;

		Fish(String name, int level, int itemId)
		{
			this.name = name;
			this.level = level;
			this.itemId = itemId;
		}

		@Override
		public String toString()
		{
			return name + " (" + level + ")";
		}
	}

	/** The menu options, lower case, that one method on a spot can be named by. */
	private enum Method
	{
		NET("net", "small net"),
		BIG_NET("big net"),
		BAIT("bait"),
		LURE("lure"),
		CAGE("cage"),
		HARPOON("harpoon"),
		ROD("use-rod"),
		FISH("fish");

		private final String[] options;

		Method(String... options)
		{
			this.options = options;
		}

		static Method of(String option)
		{
			if (option == null)
			{
				return null;
			}
			final String lower = option.toLowerCase(Locale.ROOT).trim();
			for (Method method : values())
			{
				for (String name : method.options)
				{
					if (name.equals(lower))
					{
						return method;
					}
				}
			}
			return null;
		}
	}

	private static final Fish SHRIMPS = new Fish("Shrimps", 1, ItemID.RAW_SHRIMP);
	private static final Fish KARAMBWANJI = new Fish("Karambwanji", 5, ItemID.TBWT_RAW_KARAMBWANJI);
	private static final Fish SARDINE = new Fish("Sardine", 5, ItemID.RAW_SARDINE);
	private static final Fish GUPPY = new Fish("Guppy", 7, ItemID.RAW_GUPPY);
	private static final Fish HERRING = new Fish("Herring", 10, ItemID.RAW_HERRING);
	private static final Fish ANCHOVIES = new Fish("Anchovies", 15, ItemID.RAW_ANCHOVIES);
	private static final Fish MACKEREL = new Fish("Mackerel", 16, ItemID.RAW_MACKEREL);
	private static final Fish TROUT = new Fish("Trout", 20, ItemID.RAW_TROUT);
	private static final Fish CAVEFISH = new Fish("Cavefish", 20, ItemID.RAW_CAVEFISH);
	private static final Fish COD = new Fish("Cod", 23, ItemID.RAW_COD);
	private static final Fish PIKE = new Fish("Pike", 25, ItemID.RAW_PIKE);
	private static final Fish SLIMY_EEL = new Fish("Slimy eel", 28, ItemID.MORT_SLIMEY_EEL);
	private static final Fish SALMON = new Fish("Salmon", 30, ItemID.RAW_SALMON);
	private static final Fish TETRA = new Fish("Tetra", 33, ItemID.RAW_TETRA);
	private static final Fish FROG_SPAWN = new Fish("Frog spawn", 33, ItemID.GIANT_FROGSPAWN);
	private static final Fish TUNA = new Fish("Tuna", 35, ItemID.RAW_TUNA);
	private static final Fish HARPOONFISH = new Fish("Harpoonfish", 35, ItemID.TEMPOROSS_RAW_HARPOONFISH);
	private static final Fish CAVE_EEL = new Fish("Cave eel", 38, ItemID.RAW_CAVE_EEL);
	private static final Fish RAINBOW_FISH = new Fish("Rainbow fish", 38, ItemID.HUNTING_RAW_FISH_SPECIAL);
	private static final Fish LOBSTER = new Fish("Lobster", 40, ItemID.RAW_LOBSTER);
	private static final Fish BASS = new Fish("Bass", 46, ItemID.RAW_BASS);
	private static final Fish CATFISH = new Fish("Catfish", 46, ItemID.RAW_CATFISH);
	private static final Fish LEAPING_TROUT = new Fish("Leaping trout", 48, ItemID.BRUT_SPAWNING_TROUT);
	private static final Fish SWORDFISH = new Fish("Swordfish", 50, ItemID.RAW_SWORDFISH);
	private static final Fish LAVA_EEL = new Fish("Lava eel", 53, ItemID.RAW_LAVA_EEL);
	private static final Fish LEAPING_SALMON = new Fish("Leaping salmon", 58, ItemID.BRUT_SPAWNING_SALMON);
	private static final Fish MONKFISH = new Fish("Monkfish", 62, ItemID.RAW_MONKFISH);
	private static final Fish KARAMBWAN = new Fish("Karambwan", 65, ItemID.TBWT_RAW_KARAMBWAN);
	private static final Fish LEAPING_STURGEON = new Fish("Leaping sturgeon", 70, ItemID.BRUT_STURGEON);
	private static final Fish SHARK = new Fish("Shark", 76, ItemID.RAW_SHARK);
	private static final Fish INFERNAL_EEL = new Fish("Infernal eel", 80, ItemID.INFERNAL_EEL);
	private static final Fish ANGLERFISH = new Fish("Anglerfish", 82, ItemID.RAW_ANGLERFISH);
	private static final Fish MINNOW = new Fish("Minnow", 82, ItemID.MINNOW);
	private static final Fish DARK_CRAB = new Fish("Dark crab", 85, ItemID.RAW_DARK_CRAB);
	private static final Fish SACRED_EEL = new Fish("Sacred eel", 87, ItemID.SNAKEBOSS_EEL);

	private static final Map<FishingSpot, Map<Method, List<Fish>>> TABLE = new EnumMap<>(FishingSpot.class);
	private static final Map<Integer, Fish> BY_ITEM = new HashMap<>();
	private static final Map<String, Fish> BY_NAME = new HashMap<>();

	/** "You catch some shrimps." / "You catch a sardine." / "You catch an anglerfish." */
	private static final Pattern CATCH = Pattern.compile("^You catch (?:a|an|some) ([a-z ]+?)[.!]?$", Pattern.CASE_INSENSITIVE);

	static
	{
		// Wiki "Fishing spot", land and boat tables, in level order per option.
		row(FishingSpot.SHRIMP, Method.NET, SHRIMPS, ANCHOVIES);
		row(FishingSpot.SHRIMP, Method.BAIT, SARDINE, HERRING);
		row(FishingSpot.TUTORIAL_SHRIMP, Method.NET, SHRIMPS);
		row(FishingSpot.KARAMBWANJI, Method.NET, SHRIMPS, KARAMBWANJI);

		row(FishingSpot.LOBSTER, Method.CAGE, LOBSTER);
		row(FishingSpot.LOBSTER, Method.HARPOON, TUNA, SWORDFISH);
		row(FishingSpot.ETCETERIA_LOBSTER, Method.CAGE, LOBSTER);
		row(FishingSpot.ETCETERIA_LOBSTER, Method.HARPOON, TUNA, SWORDFISH);
		row(FishingSpot.DARK_CRAB, Method.CAGE, LOBSTER, DARK_CRAB);

		row(FishingSpot.SHARK, Method.BIG_NET, MACKEREL, COD, BASS);
		row(FishingSpot.SHARK, Method.HARPOON, SHARK);

		row(FishingSpot.MONKFISH, Method.NET, MONKFISH);
		row(FishingSpot.MONKFISH, Method.HARPOON, TUNA, SWORDFISH);

		row(FishingSpot.SALMON, Method.LURE, TROUT, SALMON, RAINBOW_FISH);
		row(FishingSpot.SALMON, Method.BAIT, PIKE);
		row(FishingSpot.BARB_FISH, Method.ROD, LEAPING_TROUT, LEAPING_SALMON, LEAPING_STURGEON);

		row(FishingSpot.CAVE_EEL, Method.NET, FROG_SPAWN);
		row(FishingSpot.CAVE_EEL, Method.BAIT, SLIMY_EEL, CAVE_EEL);
		row(FishingSpot.SLIMY_EEL, Method.BAIT, SLIMY_EEL);
		row(FishingSpot.LAVA_EEL, Method.BAIT, LAVA_EEL);
		row(FishingSpot.INFERNAL_EEL, Method.BAIT, INFERNAL_EEL);
		row(FishingSpot.SACRED_EEL, Method.BAIT, SACRED_EEL);
		row(FishingSpot.ANGLERFISH, Method.BAIT, ANGLERFISH);

		row(FishingSpot.KARAMBWAN, Method.FISH, KARAMBWAN);
		row(FishingSpot.MINNOW, Method.NET, MINNOW);
		row(FishingSpot.HARPOONFISH, Method.HARPOON, HARPOONFISH);

		row(FishingSpot.CAMDOZAAL_TETRA, Method.NET, GUPPY, CAVEFISH, TETRA);
		row(FishingSpot.CAMDOZAAL_TETRA, Method.BIG_NET, TETRA, CATFISH);
	}

	private static void row(FishingSpot spot, Method method, Fish... fish)
	{
		final List<Fish> list = Collections.unmodifiableList(new ArrayList<>(java.util.Arrays.asList(fish)));
		TABLE.computeIfAbsent(spot, s -> new EnumMap<>(Method.class)).put(method, list);
		for (Fish f : fish)
		{
			BY_ITEM.putIfAbsent(f.itemId, f);
			BY_NAME.putIfAbsent(f.name.toLowerCase(Locale.ROOT), f);
		}
	}

	private FishingCatches()
	{
	}

	/**
	 * @param spot   the spot type, or null when {@link FishingSpot#findSpot} did not know the NPC
	 * @param option the menu option clicked on it, e.g. "Net"; case-insensitive
	 * @return every fish that option can land, lowest level first; empty when
	 *         either is unknown, so the badge falls back to the skill and level
	 */
	static List<Fish> forOption(FishingSpot spot, String option)
	{
		final Method method = Method.of(option);
		if (spot == null || method == null)
		{
			return Collections.emptyList();
		}

		final Map<Method, List<Fish>> rows = TABLE.get(spot);
		if (rows == null)
		{
			return Collections.emptyList();
		}
		final List<Fish> fish = rows.get(method);
		return fish == null ? Collections.emptyList() : fish;
	}

	/** The fish whose level the player has, in the order given. */
	static List<Fish> catchable(List<Fish> fish, int level)
	{
		final List<Fish> out = new ArrayList<>(fish.size());
		for (Fish f : fish)
		{
			if (f.level <= level)
			{
				out.add(f);
			}
		}
		return out;
	}

	/** One fish the spot can give, and whether the player's level lands it now. */
	@Getter
	static final class Catch
	{
		private final Fish fish;
		private final boolean catchable;

		Catch(Fish fish, boolean catchable)
		{
			this.fish = fish;
			this.catchable = catchable;
		}
	}

	/**
	 * Every fish the method can give, for the badge's catch icons: the ones
	 * the level lands first, the last catch leading
	 * ({@link #highlightFirst}), then the ones still above the level, lowest
	 * level first.
	 */
	static List<Catch> options(List<Fish> fish, int level, int lastCaughtItemId)
	{
		final List<Catch> out = new ArrayList<>(fish.size());
		for (Fish f : highlightFirst(catchable(fish, level), lastCaughtItemId))
		{
			out.add(new Catch(f, true));
		}
		for (Fish f : fish)
		{
			if (f.level > level)
			{
				out.add(new Catch(f, false));
			}
		}
		return out;
	}

	/**
	 * The last catch first, so the icon the player is actually filling the
	 * inventory with leads. A catch that is not in the list (another method,
	 * or nothing caught yet) leaves the level order alone.
	 */
	static List<Fish> highlightFirst(List<Fish> fish, int lastCaughtItemId)
	{
		Fish last = null;
		for (Fish f : fish)
		{
			if (f.itemId == lastCaughtItemId)
			{
				last = f;
				break;
			}
		}
		if (last == null)
		{
			return fish;
		}

		final List<Fish> out = new ArrayList<>(fish.size());
		out.add(last);
		for (Fish f : fish)
		{
			if (f != last)
			{
				out.add(f);
			}
		}
		return out;
	}

	/** The fish whose raw item this is, or null for anything that is not a fish in the table. */
	static Fish byItem(int itemId)
	{
		return BY_ITEM.get(itemId);
	}

	/**
	 * @param message a game message
	 * @return the fish a "You catch ..." message names, or null for any other message
	 */
	static Fish fromCatchMessage(String message)
	{
		if (message == null)
		{
			return null;
		}
		final Matcher matcher = CATCH.matcher(message.trim());
		if (!matcher.matches())
		{
			return null;
		}

		// "shrimps" and "minnows" are plural in the message; "Minnow" is
		// singular in the table and "Shrimps" plural, so try both ways.
		final String caught = matcher.group(1).toLowerCase(Locale.ROOT).trim();
		Fish fish = BY_NAME.get(caught);
		if (fish == null && caught.endsWith("s"))
		{
			fish = BY_NAME.get(caught.substring(0, caught.length() - 1));
		}
		if (fish == null)
		{
			fish = BY_NAME.get(caught + "s");
		}
		return fish;
	}
}
