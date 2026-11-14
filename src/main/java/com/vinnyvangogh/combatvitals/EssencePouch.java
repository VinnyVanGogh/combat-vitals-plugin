package com.vinnyvangogh.combatvitals;

import java.util.Locale;
import lombok.Getter;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * How much essence each pouch holds as it degrades. Ported from the base
 * client's {@code net.runelite.client.plugins.runecraft.EssencePouchOverlay}
 * (BSD-2-Clause, runelite master bb0fa42, lines 46-103 and 122): pairs of
 * (degradation at or above, capacity), highest first, and the Colossal
 * pouch scaled down below 85 Runecraft. The amount held and the degradation
 * come from varbits the plugin reads; this is only the table.
 *
 * Pure: degradation and level in, capacity out.
 */
@Getter
enum EssencePouch
{
	SMALL("Small pouch", 3, new int[]{}, VarbitID.SMALL_ESSENCE_POUCH, -1, false),
	MEDIUM("Medium pouch", 6, new int[]{800, 0, 400, 3},
		VarbitID.MEDIUM_ESSENCE_POUCH, VarPlayerID.RCU_POUCH_DEGRADATION_MED, false),
	LARGE("Large pouch", 9, new int[]{1000, 0, 800, 3, 600, 5, 400, 7},
		VarbitID.LARGE_ESSENCE_POUCH, VarPlayerID.RCU_POUCH_DEGRADATION_LARGE, false),
	GIANT("Giant pouch", 12, new int[]{1200, 0, 1000, 3, 800, 5, 600, 6, 400, 7, 300, 8, 200, 9},
		VarbitID.GIANT_ESSENCE_POUCH, VarPlayerID.RCU_POUCH_DEGRADATION_GIANT, false),
	COLOSSAL("Colossal pouch", 40, new int[]{1020, 0, 1015, 5, 995, 10, 950, 15, 870, 20, 745, 25, 565, 30, 320, 35},
		VarbitID.COLOSSAL_ESSENCE_POUCH, VarbitID.RCU_POUCH_DEGRADATION_COLOSSAL, true);

	private final String itemName;
	private final int maxFill;
	private final int[] degradation;
	/** Essence held, a varbit. */
	private final int amountVarbit;
	/** Degradation, a varp, or a varbit when {@link #degradationIsVarbit}; -1 for the small pouch, which never degrades. */
	private final int degradationVar;
	private final boolean degradationIsVarbit;

	EssencePouch(String itemName, int maxFill, int[] degradation, int amountVarbit, int degradationVar,
		boolean degradationIsVarbit)
	{
		this.itemName = itemName;
		this.maxFill = maxFill;
		this.degradation = degradation;
		this.amountVarbit = amountVarbit;
		this.degradationVar = degradationVar;
		this.degradationIsVarbit = degradationIsVarbit;
	}

	/** The pouch an item is, by name; null for anything else, the rune pouch included. */
	static EssencePouch fromItemName(String name)
	{
		if (name == null)
		{
			return null;
		}
		final String lower = name.toLowerCase(Locale.ROOT).trim();
		for (EssencePouch pouch : values())
		{
			if (pouch.itemName.toLowerCase(Locale.ROOT).equals(lower))
			{
				return pouch;
			}
		}
		return null;
	}

	/**
	 * @param degradation the pouch's degradation varp or varbit; 0 for the small pouch
	 * @param runecraft   real Runecraft level, for the Colossal pouch
	 */
	int capacity(int degradation, int runecraft)
	{
		int limit = maxFill;
		for (int i = 0; i < this.degradation.length; i += 2)
		{
			if (degradation >= this.degradation[i])
			{
				limit = this.degradation[i + 1];
				break;
			}
		}
		return limit > 0 && this == COLOSSAL ? Math.max(1, limit * colossalMax(runecraft) / maxFill) : limit;
	}

	private static int colossalMax(int runecraft)
	{
		if (runecraft >= 85)
		{
			return 40;
		}
		if (runecraft >= 75)
		{
			return 27;
		}
		return runecraft >= 50 ? 16 : 8;
	}
}
