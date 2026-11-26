package com.vinnyvangogh.combatvitals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item-name knowledge behind the pillar sources, with no client types so it is unit tested: how much
 * a food heals, how many doses a potion holds, whether a weapon uses its own stack as ammo, and what
 * a charges chat message says. Healing values are the OSRS Wiki's for the common foods; anything not
 * in the table counts as food with unknown healing rather than a guess.
 */
final class PillarSupplies
{
	private static final Map<String, Integer> HEAL = new HashMap<>();

	private static final Pattern DOSES = Pattern.compile("\\((\\d)\\)$");
	private static final Pattern PRAYER_POTION =
		Pattern.compile("^(prayer potion|super restore|sanfew serum)\\(\\d\\)$");
	private static final Pattern STAMINA = Pattern.compile("^stamina potion\\(\\d\\)$");

	/** "Your trident of the swamp has 2,317 charges remaining." and the like: the item and the count. */
	private static final Pattern CHARGES = Pattern.compile(
		"(?i)^your (.+?) (?:has|have currently|currently has|now has) ([\\d,]+) (?:charges?|darts?|scales?)\\b");
	private static final Pattern BLOWPIPE_CHECK = Pattern.compile(
		"(?i)darts?:?\\s*([\\d,]+)[^\\d]+scales?:?\\s*([\\d,]+)|scales?:?\\s*([\\d,]+)[^\\d]+darts?:?\\s*([\\d,]+)");

	/** The most each chargeable weapon holds, by a word in its name (OSRS Wiki). */
	private static final Map<String, Integer> CHARGE_MAX = new HashMap<>();

	static
	{
		food("shark", 20);
		food("manta ray", 22);
		food("dark crab", 22);
		food("anglerfish", 22);
		food("sea turtle", 21);
		food("tuna potato", 22);
		food("cooked karambwan", 18);
		food("monkfish", 16);
		food("swordfish", 14);
		food("lobster", 12);
		food("bass", 13);
		food("tuna", 10);
		food("trout", 7);
		food("salmon", 9);
		food("cod", 7);
		food("pike", 8);
		food("herring", 5);
		food("sardine", 3);
		food("shrimps", 3);
		food("cake", 4);
		food("pizza", 7);
		food("pie", 6);
		food("potato with cheese", 16);
		food("egg potato", 11);
		food("jug of wine", 11);

		CHARGE_MAX.put("trident of the seas", 10_000);
		CHARGE_MAX.put("trident of the swamp", 2_500);
		CHARGE_MAX.put("sanguinesti", 20_000);
		CHARGE_MAX.put("tumeken", 20_000);
		CHARGE_MAX.put("blowpipe", 16_383);
	}

	private PillarSupplies()
	{
	}

	private static void food(String name, int heal)
	{
		HEAL.put(name, heal);
	}

	/** True for an item that is eaten: its inventory actions include Eat. */
	static boolean isFood(String[] inventoryActions)
	{
		if (inventoryActions == null)
		{
			return false;
		}
		for (String action : inventoryActions)
		{
			if ("Eat".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * What one portion of {@code itemName} heals; -1 when the table does not know it. A partly eaten
	 * pie or cake ("1/2 ...") heals less than the table's whole one, so those read as unknown.
	 */
	static int heal(String itemName)
	{
		if (itemName == null)
		{
			return -1;
		}
		final String name = itemName.toLowerCase(Locale.ROOT).trim();
		if (name.startsWith("1/") || name.startsWith("2/") || name.startsWith("half") || name.contains("raw")
			|| name.contains("burnt"))
		{
			return -1;
		}
		Integer best = HEAL.get(name);
		if (best != null)
		{
			return best;
		}
		int longest = 0;
		for (Map.Entry<String, Integer> e : HEAL.entrySet())
		{
			if (name.contains(e.getKey()) && e.getKey().length() > longest)
			{
				longest = e.getKey().length();
				best = e.getValue();
			}
		}
		return best == null ? -1 : best;
	}

	/** Doses of a prayer-restoring potion ("Prayer potion(3)" is 3); 0 for anything else. */
	static int prayerDoses(String itemName)
	{
		return itemName != null && PRAYER_POTION.matcher(itemName.toLowerCase(Locale.ROOT).trim()).matches()
			? doses(itemName) : 0;
	}

	/** Doses of a stamina potion; 0 for anything else. */
	static int staminaDoses(String itemName)
	{
		return itemName != null && STAMINA.matcher(itemName.toLowerCase(Locale.ROOT).trim()).matches()
			? doses(itemName) : 0;
	}

	private static int doses(String itemName)
	{
		final Matcher m = DOSES.matcher(itemName.trim());
		return m.find() ? Integer.parseInt(m.group(1)) : 0;
	}

	/** Weapons that are their own ammo: the count in the weapon slot is what runs out. */
	static boolean thrown(String weaponName)
	{
		if (weaponName == null)
		{
			return false;
		}
		final String name = weaponName.toLowerCase(Locale.ROOT);
		return name.contains("dart") && !name.contains("blowpipe") || name.contains("chinchompa")
			|| name.contains("javelin") || name.contains("throwing") || name.contains("knife")
			|| name.contains("thrownaxe");
	}

	static boolean blowpipe(String weaponName)
	{
		return weaponName != null && weaponName.toLowerCase(Locale.ROOT).contains("blowpipe");
	}

	/** A charges message's item name (lower case) and count, or null when it is not one. */
	static Charge charge(String message)
	{
		if (message == null)
		{
			return null;
		}
		final Matcher m = CHARGES.matcher(message.trim());
		if (!m.find())
		{
			return null;
		}
		final String item = m.group(1).toLowerCase(Locale.ROOT);
		for (String key : CHARGE_MAX.keySet())
		{
			if (item.contains(key))
			{
				return new Charge(item, Integer.parseInt(m.group(2).replace(",", "")), CHARGE_MAX.get(key));
			}
		}
		return null;
	}

	/** A blowpipe check's {darts, scales}, or null. */
	static int[] blowpipeCheck(String message)
	{
		if (message == null)
		{
			return null;
		}
		final Matcher m = BLOWPIPE_CHECK.matcher(message);
		if (!m.find())
		{
			return null;
		}
		if (m.group(1) != null)
		{
			return new int[] {num(m.group(1)), num(m.group(2))};
		}
		return new int[] {num(m.group(4)), num(m.group(3))};
	}

	private static int num(String digits)
	{
		return Integer.parseInt(digits.replace(",", ""));
	}

	/** What a charges message said: the item, its charges, and the most it holds. */
	static final class Charge
	{
		final String item;
		final int charges;
		final int max;

		Charge(String item, int charges, int max)
		{
			this.item = item;
			this.charges = charges;
			this.max = max;
		}
	}

	/** The most charges the equipped weapon holds, or -1 when it is not a chargeable one this knows. */
	static int chargeMax(String weaponName)
	{
		if (weaponName == null)
		{
			return -1;
		}
		final String name = weaponName.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Integer> e : CHARGE_MAX.entrySet())
		{
			if (name.contains(e.getKey()))
			{
				return e.getValue();
			}
		}
		return -1;
	}
}
