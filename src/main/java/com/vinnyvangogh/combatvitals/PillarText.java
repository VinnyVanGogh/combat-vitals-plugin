package com.vinnyvangogh.combatvitals;

/**
 * What each side pillar says: the short number under its icon, the level
 * at its foot, its hover text, and where its tick marks go.
 *
 * A pillar is 20 px wide, so the number is three or four characters at
 * most; the hover text says the rest in words.
 *
 * Pure: values in, text and positions out.
 */
final class PillarText
{
	private PillarText()
	{
	}

	/**
	 * "82", the percentage without its sign, or the XP left, "65" or "1.2k",
	 * per the XP display setting; "Max" at 200m. Never a sign: "82%" is wider
	 * than the pillar and lost it when drawn while "3%" kept it, so pillars
	 * read "3%" beside "82" (Board's screenshots, 2026-11-06). The hover text
	 * says which it is.
	 */
	static String xpNumber(XpProgress xp, XpDisplay display)
	{
		if (maxed(xp))
		{
			return "Max";
		}
		// Both reads as the percentage: there is room for one number.
		return display == XpDisplay.XP_LEFT ? compact(xp.getXpLeft()) : Integer.toString(percent(xp));
	}

	/**
	 * Four characters at most, for 20 px: "65", "1.2k", "29k", "1.2m", "13m".
	 * The session row's short numbers, without the decimal from 10k and 10m.
	 */
	static String compact(long n)
	{
		if (n < 9_950 || (n >= 999_500 && n < 9_950_000))
		{
			return SessionRow.shortNumber(n);
		}
		return n < 999_500 ? Math.round(n / 1_000.0) + "k" : Math.round(n / 1_000_000.0) + "m";
	}

	/** The level being worked through, at the foot of the XP pillar: "16". */
	static String xpBottom(XpProgress xp)
	{
		return Integer.toString(xp.getLevel());
	}

	/** "Fishing XP: 82% to level 17 (65 xp left)", "Fishing XP: 6% to 200m (...)", "Fishing XP: 200m". */
	static String xpTooltip(String skillName, XpProgress xp)
	{
		if (maxed(xp))
		{
			return skillName + " XP: 200m";
		}
		final String to = xp.getNextLevel() > 0 ? "level " + xp.getNextLevel() : "200m";
		return String.format("%s XP: %d%% to %s (%,d xp left)", skillName, percent(xp), to, xp.getXpLeft());
	}

	/** Free slots, or used ones, per the Inventory count setting: "18"; null while unread. */
	static String inventoryNumber(int empty, InventoryCount count)
	{
		if (empty < 0)
		{
			return null;
		}
		return Integer.toString(count == InventoryCount.USED ? InventoryFill.SLOTS - empty : empty);
	}

	/** "Inventory: 10 of 28 free", "Inventory: 18 of 28 used"; null while unread. */
	static String inventoryTooltip(int empty, InventoryCount count)
	{
		final String number = inventoryNumber(empty, count);
		return number == null ? null
			: "Inventory: " + number + " of " + InventoryFill.SLOTS + (count == InventoryCount.USED ? " used" : " free");
	}

	/**
	 * The target's hitpoints: exact when known, else the middle of the
	 * recovered range, else -1 (only the bar's fraction is known).
	 */
	static int targetHp(int exactHealth, HealthRecovery.Range range, int maxHealth)
	{
		if (exactHealth >= 0)
		{
			return exactHealth;
		}
		return range != null && maxHealth > 0 ? range.midpoint() : -1;
	}

	/** "12", or "82%" when only the fraction is known. */
	static String targetNumber(int hp, double fraction)
	{
		return hp >= 0 ? Integer.toString(hp) : Math.round(fraction * 100) + "%";
	}

	/** "Target: Rock Crab 12/50", or "Target: Goblin 82%". */
	static String targetTooltip(String name, int hp, int maxHealth, double fraction)
	{
		final String health = hp >= 0 && maxHealth > 0 ? hp + "/" + maxHealth : Math.round(fraction * 100) + "%";
		return "Target: " + name + " " + health;
	}

	/**
	 * Where the marks between {@code divisions} equal parts of a track go,
	 * top to bottom: 3 for quarters, 27 between 28 slots.
	 *
	 * @param top    the track's top y
	 * @param height the track's height
	 */
	static int[] ticks(int top, int height, int divisions)
	{
		if (divisions <= 1 || height <= 0)
		{
			return new int[0];
		}
		final int[] ticks = new int[divisions - 1];
		for (int i = 1; i < divisions; i++)
		{
			ticks[i - 1] = top + (int) Math.round((double) height * i / divisions);
		}
		return ticks;
	}

	/** Rounded down, as the badge: 1 xp short of a level reads 99%. */
	private static int percent(XpProgress xp)
	{
		return (int) Math.floor(xp.getFraction() * 100);
	}

	private static boolean maxed(XpProgress xp)
	{
		return xp.getNextLevel() < 0 && xp.getXpLeft() <= 0;
	}
}
