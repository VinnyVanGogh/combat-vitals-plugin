package com.vinnyvangogh.combatvitals;

/**
 * Max hit and chance to hit against an NPC, from the OSRS Wiki's
 * "Maximum melee hit", "Maximum ranged hit", "Maximum magic hit" and
 * "Damage per second" pages. Pure arithmetic, no client: the Max Hit page
 * feeds it what the player wears and picks.
 *
 * Every step rounds down, in the wiki's order. Gear multipliers that are not
 * a plain percentage (Slayer helm on task, Salve amulet, Dharok's) go in as
 * one {@code otherMultiplier} the player types, which is applied last.
 * Not modelled: special attacks, set effects, defender damage reductions.
 */
final class MaxHit
{
	/** The wiki's hidden stance bonus to the effective level. */
	static final int STANCE_AGGRESSIVE = 3;
	static final int STANCE_ACCURATE = 3;
	static final int STANCE_CONTROLLED = 1;
	static final int STANCE_NONE = 0;

	private MaxHit()
	{
	}

	/** floor(floor(level * prayer) + stance + 8) * void, rounded down. */
	static int effectiveLevel(int level, double prayer, int stance, double voidBonus)
	{
		final int base = (int) Math.floor(level * prayer) + stance + 8;
		return (int) Math.floor(base * voidBonus);
	}

	/** Melee or ranged: floor(0.5 + effective * (strengthBonus + 64) / 640), then the other multiplier. */
	static int physical(int level, double prayer, int stance, double voidBonus, int strengthBonus,
		double otherMultiplier)
	{
		final int effective = effectiveLevel(level, prayer, stance, voidBonus);
		final int base = (int) Math.floor(0.5 + effective * (strengthBonus + 64) / 640.0);
		return (int) Math.floor(base * otherMultiplier);
	}

	/** Magic: the spell's base max hit times (1 + magic damage bonus), rounded down. {@code damagePercent} is e.g. 12.5. */
	static int magic(int spellBase, double damagePercent, double otherMultiplier)
	{
		final int boosted = (int) Math.floor(spellBase * (1 + damagePercent / 100.0));
		return (int) Math.floor(boosted * otherMultiplier);
	}

	/** floor(floor(level * prayer) + stance + 8) * void, then times (attackBonus + 64). */
	static long attackRoll(int level, double prayer, int stance, double voidBonus, int attackBonus)
	{
		return (long) effectiveLevel(level, prayer, stance, voidBonus) * (attackBonus + 64);
	}

	/** (npcLevel + 9) * (npcDefenceBonus + 64); the NPC has no prayer, stance or void. */
	static long defenceRoll(int npcLevel, int npcDefenceBonus)
	{
		return SplashMath.defenceRoll(npcLevel, npcDefenceBonus);
	}

	/** The wiki's chance to hit, 0..1. */
	static double hitChance(long attackRoll, long defenceRoll)
	{
		return SplashMath.hitChance(attackRoll, defenceRoll);
	}

	/**
	 * Average damage per attack: chance to hit times half the max hit
	 * (a hit is uniform 0..max; zeros are already inside the average).
	 */
	static double expectedHit(int maxHit, double hitChance)
	{
		return hitChance * maxHit / 2.0;
	}

	/** Average damage per second for a weapon speed in game ticks (0.6 s each); 0 for a speed under 1. */
	static double dps(double expectedHit, int speedTicks)
	{
		return speedTicks < 1 ? 0 : expectedHit / (speedTicks * 0.6);
	}
}
