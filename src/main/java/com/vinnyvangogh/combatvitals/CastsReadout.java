package com.vinnyvangogh.combatvitals;

/**
 * What the standalone "Casts left" overlay draws for one tick: the spell, the
 * runes it costs per cast (item ids, for the icons), and the casts left.
 */
final class CastsReadout
{
	final String spell;
	/** "142 casts left", "Unlimited casts" or "Out of Chaos runes". */
	final String castsText;
	/** Casts left, or {@link SpellCasts#INFINITE}. */
	final int casts;
	/** The casts are at or under the user's warn level. */
	final boolean low;
	final int[] runeItems;
	final int[] runeCosts;

	CastsReadout(String spell, String castsText, int casts, boolean low, int[] runeItems, int[] runeCosts)
	{
		this.spell = spell;
		this.castsText = castsText;
		this.casts = casts;
		this.low = low;
		this.runeItems = runeItems;
		this.runeCosts = runeCosts;
	}
}
