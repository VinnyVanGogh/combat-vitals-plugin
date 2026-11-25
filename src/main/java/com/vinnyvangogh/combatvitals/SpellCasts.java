package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How many casts of a combat spell the player has left: the runes in the
 * inventory and rune pouch, combination runes standing in for the two runes
 * they combine, and staves and tomes that stand in for a rune without limit.
 * Written from the game's own rune costs (OSRS Wiki spell pages); none of the
 * Remaining Casts plugin's code is used. No client types, so it is unit tested.
 */
final class SpellCasts
{
	/** {@link Result#casts} when the spell can be cast without limit. */
	static final int INFINITE = -1;

	private static final long CAP = 10_000_000L;

	enum Rune
	{
		AIR("Air"), WATER("Water"), EARTH("Earth"), FIRE("Fire"), MIND("Mind"), CHAOS("Chaos"), DEATH("Death"),
		BLOOD("Blood"), SOUL("Soul"), WRATH("Wrath");

		final String label;

		Rune(String label)
		{
			this.label = label;
		}
	}

	/** A combination rune: each one pays for one rune of either element. */
	enum Combo
	{
		DUST(Rune.AIR, Rune.EARTH), MIST(Rune.AIR, Rune.WATER), MUD(Rune.WATER, Rune.EARTH),
		SMOKE(Rune.AIR, Rune.FIRE), STEAM(Rune.WATER, Rune.FIRE), LAVA(Rune.EARTH, Rune.FIRE);

		final Rune a;
		final Rune b;

		Combo(Rune a, Rune b)
		{
			this.a = a;
			this.b = b;
		}
	}

	/** What the player carries. */
	static final class Supply
	{
		final long[] runes = new long[Rune.values().length];
		final long[] combos = new long[Combo.values().length];
		final boolean[] infinite = new boolean[Rune.values().length];

		Supply rune(Rune rune, long count)
		{
			runes[rune.ordinal()] += count;
			return this;
		}

		Supply combo(Combo combo, long count)
		{
			combos[combo.ordinal()] += count;
			return this;
		}

		Supply unlimited(Rune rune)
		{
			infinite[rune.ordinal()] = true;
			return this;
		}

		/** A staff or tome by its item name; false when it is neither. */
		boolean item(String itemName)
		{
			if (itemName == null)
			{
				return false;
			}
			final String name = itemName.toLowerCase(Locale.ROOT);
			boolean any = false;
			final Matcher tome = TOME.matcher(name);
			if (tome.find())
			{
				unlimited(Rune.valueOf(tome.group(1).toUpperCase(Locale.ROOT)));
				return true;
			}
			Matcher m = STAFF_OF.matcher(name);
			if (!m.find())
			{
				m = STAFF.matcher(name);
				if (!m.find())
				{
					return false;
				}
			}
			final String kind = m.group(1).toUpperCase(Locale.ROOT);
			for (Combo combo : Combo.values())
			{
				if (combo.name().equals(kind))
				{
					unlimited(combo.a).unlimited(combo.b);
					any = true;
				}
			}
			if (!any)
			{
				unlimited(Rune.valueOf(kind));
			}
			return true;
		}
	}

	/** Casts left, or {@link #INFINITE}, and the rune that runs out first (null when infinite). */
	static final class Result
	{
		final int casts;
		final Rune limiting;

		Result(int casts, Rune limiting)
		{
			this.casts = casts;
			this.limiting = limiting;
		}
	}

	private static final Pattern TOME = Pattern.compile("^tome of (fire|water)(?! \\(empty\\))");
	private static final Pattern STAFF_OF = Pattern.compile("staff of (air|water|earth|fire)\\b");
	private static final Pattern STAFF =
		Pattern.compile("(air|water|earth|fire|dust|mist|mud|smoke|steam|lava) (?:battlestaff|staff)\\b");

	private static final Map<String, int[]> SPELLS = new HashMap<>();
	private static final Map<Integer, String> AUTOCAST = new HashMap<>();
	private static final Map<String, Integer> LEVELS = new HashMap<>();
	private static final Set<String> ANCIENT = new HashSet<>();

	private SpellCasts()
	{
	}

	/** The spell's rune cost by rune ordinal, or null for a spell not in the table. */
	static int[] cost(String spellName)
	{
		return spellName == null ? null : SPELLS.get(spellName.toLowerCase(Locale.ROOT).trim());
	}

	/**
	 * The standard spellbook's autocast spell for the game's autocast varbit,
	 * or null when it is not one this knows. Unverified in game: the overlay
	 * prints the spell name so a wrong one is visible.
	 */
	static String autocastName(int varbit)
	{
		return AUTOCAST.get(varbit);
	}

	/** One spell the player can cast now. */
	static final class Castable
	{
		final String name;
		final int level;
		final int[] cost;
		final Result result;

		Castable(String name, int level, int[] cost, Result result)
		{
			this.name = name;
			this.level = level;
			this.cost = cost;
			this.result = result;
		}
	}

	/** The Magic level a spell needs, or 0 for a spell not in the table. */
	static int level(String spellName)
	{
		final Integer level = spellName == null ? null : LEVELS.get(spellName.toLowerCase(Locale.ROOT).trim());
		return level == null ? 0 : level;
	}

	/** Is the spell on the ancient spellbook (else the standard one)? */
	static boolean ancient(String spellName)
	{
		return spellName != null && ANCIENT.contains(spellName.toLowerCase(Locale.ROOT).trim());
	}

	/**
	 * Every combat spell the player can cast right now with at least one cast of runes, strongest (highest
	 * level) first: on the spellbook they have open, within their (boosted) Magic level, and with the staff the
	 * spell needs worn. Spells with no runes behind them are left out, so an empty list means nothing is
	 * castable and the card hides.
	 *
	 * @param iban Iban's staff is worn (Iban Blast needs it)
	 * @param dart a slayer's staff or staff of the dead is worn (Magic Dart needs it)
	 */
	static List<Castable> castable(int magicLevel, boolean ancientBook, boolean iban, boolean dart, Supply supply)
	{
		final List<Castable> out = new ArrayList<>();
		for (Map.Entry<String, int[]> e : SPELLS.entrySet())
		{
			final String key = e.getKey();
			final int level = LEVELS.getOrDefault(key, Integer.MAX_VALUE);
			if (level > magicLevel || ANCIENT.contains(key) != ancientBook)
			{
				continue;
			}
			if (key.equals("iban blast") && !iban || key.equals("magic dart") && !dart)
			{
				continue;
			}
			final Result r = casts(e.getValue(), supply);
			if (r.casts == INFINITE || r.casts > 0)
			{
				out.add(new Castable(title(key), level, e.getValue(), r));
			}
		}
		out.sort(Comparator.comparingInt((Castable c) -> c.level).reversed().thenComparing(c -> c.name));
		return out;
	}

	private static String title(String key)
	{
		final StringBuilder sb = new StringBuilder();
		for (String word : key.split(" "))
		{
			sb.append(sb.length() == 0 ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return sb.toString();
	}

	static Result casts(int[] cost, Supply supply)
	{
		if (feasible(cost, supply, CAP))
		{
			return new Result(INFINITE, null);
		}
		final int n = search(cost, supply);
		Rune limiting = null;
		int best = -1;
		for (Rune rune : Rune.values())
		{
			if (cost[rune.ordinal()] == 0)
			{
				continue;
			}
			final int[] without = cost.clone();
			without[rune.ordinal()] = 0;
			final int more = search(without, supply);
			if (more > best)
			{
				best = more;
				limiting = rune;
			}
		}
		return new Result(n, limiting);
	}

	private static int search(int[] cost, Supply supply)
	{
		if (feasible(cost, supply, CAP))
		{
			return Integer.MAX_VALUE;
		}
		long lo = 0;
		long hi = CAP;
		while (lo < hi)
		{
			final long mid = (lo + hi + 1) / 2;
			if (feasible(cost, supply, mid))
			{
				lo = mid;
			}
			else
			{
				hi = mid - 1;
			}
		}
		return (int) lo;
	}

	/** Can {@code n} casts be paid for? Hall's condition for the combination runes over the four elements. */
	private static boolean feasible(int[] cost, Supply s, long n)
	{
		final long[] deficit = new long[4];
		for (Rune rune : Rune.values())
		{
			final int i = rune.ordinal();
			if (cost[i] == 0 || s.infinite[i])
			{
				continue;
			}
			final long need = cost[i] * n - s.runes[i];
			if (i >= 4)
			{
				if (need > 0)
				{
					return false;
				}
			}
			else if (need > 0)
			{
				deficit[i] = need;
			}
		}
		for (int mask = 1; mask < 16; mask++)
		{
			long demand = 0;
			for (int e = 0; e < 4; e++)
			{
				if ((mask & (1 << e)) != 0)
				{
					demand += deficit[e];
				}
			}
			if (demand == 0)
			{
				continue;
			}
			long supply = 0;
			for (Combo combo : Combo.values())
			{
				if ((mask & (1 << combo.a.ordinal())) != 0 || (mask & (1 << combo.b.ordinal())) != 0)
				{
					supply += s.combos[combo.ordinal()];
				}
			}
			if (demand > supply)
			{
				return false;
			}
		}
		return true;
	}

	private static int[] cost(int air, int water, int earth, int fire, int mind, int chaos, int death, int blood,
		int soul, int wrath)
	{
		return new int[] {air, water, earth, fire, mind, chaos, death, blood, soul, wrath};
	}

	private static void spell(String name, int[] cost)
	{
		SPELLS.put(name.toLowerCase(Locale.ROOT), cost);
	}

	static
	{
		// Strike, Bolt, Blast, Wave, Surge: air / water / earth / fire.
		final String[] tiers = {"Strike", "Bolt", "Blast", "Wave", "Surge"};
		final String[] elements4 = {"Wind", "Water", "Earth", "Fire"};
		// Air is shared by the four elements; water and earth have their own columns because they differ from
		// it at Wave and Surge. Water Wave/Surge, Earth Surge and Crumble Undead checked against the OSRS Wiki
		// spell pages 2026-11-19; the rest are unchecked.
		final int[] air = {1, 2, 3, 5, 7};
		final int[] water = {1, 2, 3, 7, 10};
		final int[] earth = {2, 3, 4, 7, 10};
		final int[] fire = {3, 4, 5, 7, 10};
		final int[] fireAir = {2, 3, 4, 5, 7};
		for (int t = 0; t < tiers.length; t++)
		{
			final int mind = t == 0 ? 1 : 0;
			final int chaos = t == 1 ? 1 : 0;
			final int death = t == 2 ? 1 : 0;
			final int blood = t == 3 ? 1 : 0;
			final int wrath = t == 4 ? 1 : 0;
			spell("Wind " + tiers[t], cost(air[t], 0, 0, 0, mind, chaos, death, blood, 0, wrath));
			spell("Water " + tiers[t], cost(air[t], water[t], 0, 0, mind, chaos, death, blood, 0, wrath));
			spell("Earth " + tiers[t], cost(air[t], 0, earth[t], 0, mind, chaos, death, blood, 0, wrath));
			spell("Fire " + tiers[t], cost(fireAir[t], 0, 0, fire[t], mind, chaos, death, blood, 0, wrath));
		}
		spell("Crumble Undead", cost(2, 0, 2, 0, 0, 1, 0, 0, 0, 0));
		spell("Iban Blast", cost(0, 0, 0, 5, 0, 0, 1, 0, 0, 0));
		spell("Magic Dart", cost(0, 0, 0, 0, 4, 0, 1, 0, 0, 0));
		// Ancient Magicks.
		spell("Smoke Rush", cost(1, 0, 0, 1, 0, 2, 2, 0, 0, 0));
		spell("Smoke Burst", cost(2, 0, 0, 2, 0, 4, 2, 0, 0, 0));
		spell("Smoke Blitz", cost(2, 0, 0, 2, 0, 0, 2, 2, 0, 0));
		spell("Smoke Barrage", cost(4, 0, 0, 4, 0, 0, 4, 2, 0, 0));
		spell("Shadow Rush", cost(1, 0, 0, 0, 0, 2, 2, 0, 1, 0));
		spell("Shadow Burst", cost(1, 0, 0, 0, 0, 4, 2, 0, 2, 0));
		spell("Shadow Blitz", cost(2, 0, 0, 0, 0, 0, 2, 2, 2, 0));
		spell("Shadow Barrage", cost(4, 0, 0, 0, 0, 0, 4, 2, 3, 0));
		spell("Blood Rush", cost(0, 0, 0, 0, 0, 2, 2, 1, 0, 0));
		spell("Blood Burst", cost(0, 0, 0, 0, 0, 4, 2, 2, 0, 0));
		spell("Blood Blitz", cost(0, 0, 0, 0, 0, 0, 2, 4, 0, 0));
		spell("Blood Barrage", cost(0, 0, 0, 0, 0, 0, 4, 4, 1, 0));
		spell("Ice Rush", cost(0, 2, 0, 0, 0, 2, 2, 0, 0, 0));
		spell("Ice Burst", cost(0, 4, 0, 0, 0, 4, 2, 0, 0, 0));
		spell("Ice Blitz", cost(0, 3, 0, 0, 0, 0, 2, 2, 0, 0));
		spell("Ice Barrage", cost(0, 6, 0, 0, 0, 0, 4, 2, 0, 0));

		// Magic levels (OSRS Wiki spell pages; Surge 81/85/90/95 and Fire Surge 95 seen on the wiki 2026-11-20, the
		// rest from memory of the same pages: unverified in game). Ancient spells need the ancient spellbook open,
		// which already means Desert Treasure I is done.
		final int[][] levels = {{1, 5, 9, 13}, {17, 23, 29, 35}, {41, 47, 53, 59}, {62, 65, 70, 75}, {81, 85, 90, 95}};
		for (int t = 0; t < tiers.length; t++)
		{
			for (int k = 0; k < elements4.length; k++)
			{
				LEVELS.put((elements4[k] + " " + tiers[t]).toLowerCase(Locale.ROOT), levels[t][k]);
			}
		}
		LEVELS.put("crumble undead", 39);
		LEVELS.put("magic dart", 50);
		LEVELS.put("iban blast", 50);
		final String[] ancientNames = {"Smoke", "Shadow", "Blood", "Ice"};
		final int[][] ancientLevels = {{50, 62, 74, 86}, {52, 64, 76, 88}, {56, 68, 80, 92}, {58, 70, 82, 94}};
		final String[] ancientTierNames = {"Rush", "Burst", "Blitz", "Barrage"};
		for (int k = 0; k < ancientNames.length; k++)
		{
			for (int t = 0; t < ancientTierNames.length; t++)
			{
				final String key = (ancientNames[k] + " " + ancientTierNames[t]).toLowerCase(Locale.ROOT);
				LEVELS.put(key, ancientLevels[k][t]);
				ANCIENT.add(key);
			}
		}

		// AUTOCAST_SPELL counts from 1 at Wind Strike (0 is no spell): strike, bolt, blast, wave in element order
		// (1 to 16), Crumble Undead 17, Magic Dart 18, the ancient rushes to barrages 31 to 46 (smoke, shadow,
		// blood, ice per tier), Iban Blast 47 and the four surges 48 to 51. Read 2026-11-19 from the Hub plugin
		// Autocasting's Spell enum (jcarbelbide/autocasting), as numbers only; not confirmed in game.
		final String[] elements = {"Wind", "Water", "Earth", "Fire"};
		int value = 1;
		for (int t = 0; t < 4; t++)
		{
			for (String element : elements)
			{
				AUTOCAST.put(value++, element + " " + tiers[t]);
			}
		}
		AUTOCAST.put(17, "Crumble Undead");
		AUTOCAST.put(18, "Magic Dart");
		final String[] ancientTiers = {"Rush", "Burst", "Blitz", "Barrage"};
		final String[] ancientKinds = {"Smoke", "Shadow", "Blood", "Ice"};
		value = 31;
		for (String tier : ancientTiers)
		{
			for (String kind : ancientKinds)
			{
				AUTOCAST.put(value++, kind + " " + tier);
			}
		}
		AUTOCAST.put(47, "Iban Blast");
		value = 48;
		for (String element : elements)
		{
			AUTOCAST.put(value++, element + " Surge");
		}
	}
}
