package com.vinnyvangogh.combatvitals;

/**
 * The OSRS magic accuracy formula, for the splashing read-out. Pure maths,
 * no client types.
 *
 * Source: OSRS Wiki, "Damage per second/Magic", steps two to five
 * (https://oldschool.runescape.wiki/w/Damage_per_second/Magic, read
 * 2026-11-18):
 * <pre>
 * effective level = floor(floor(magic level * prayer bonus) + 9)
 * accuracy roll   = effective level * (equipment magic bonus + 64)
 * defence roll    = (9 + NPC magic level) * (NPC magic defence + 64)
 * if accuracy &gt; defence:  hit chance = 1 - (defence + 2) / (2 * (accuracy + 1))
 * else:                   hit chance = accuracy / (2 * (defence + 1))
 * </pre>
 * Splashing: OSRS Wiki, "Splashing": "wearing items with bonuses that total
 * -64 Magic attack bonus or lower". At -64 the accuracy roll is 0, so the
 * hit chance is 0 whatever the target, which is why {@link #splashes} needs
 * no monster data.
 *
 * Not modelled (the wiki's other modifiers): Void magic, the +2 for Accurate
 * on powered staves, slayer helm and salve amulet. Each only raises the
 * accuracy roll, and none applies to a -64 splash setup.
 */
final class SplashMath
{
	/** The Magic attack bonus at or below which the accuracy roll is 0 (wiki: Splashing). */
	static final int SPLASH_BONUS = -64;

	private SplashMath()
	{
	}

	/** Steps one to three of the wiki's effective level: level (boosted), prayer, then +9. */
	static int effectiveLevel(int magicLevel, double prayerBonus)
	{
		return (int) Math.floor(magicLevel * prayerBonus) + 9;
	}

	static long accuracyRoll(int effectiveLevel, int magicBonus)
	{
		return (long) effectiveLevel * (magicBonus + 64);
	}

	/** The target's defence roll. A bonus below -64 would make it negative; the game cannot roll below 0. */
	static long defenceRoll(int npcMagicLevel, int npcMagicDefence)
	{
		return Math.max(0L, (long) (9 + npcMagicLevel) * (npcMagicDefence + 64));
	}

	/** The chance a cast is not a splash, 0 to 1. */
	static double hitChance(long accuracyRoll, long defenceRoll)
	{
		if (accuracyRoll <= 0)
		{
			return 0;
		}
		if (accuracyRoll > defenceRoll)
		{
			return 1 - (defenceRoll + 2d) / (2d * (accuracyRoll + 1));
		}
		return accuracyRoll / (2d * (defenceRoll + 1));
	}

	static double hitChance(int magicLevel, double prayerBonus, int magicBonus, int npcMagicLevel,
		int npcMagicDefence)
	{
		return hitChance(accuracyRoll(effectiveLevel(magicLevel, prayerBonus), magicBonus),
			defenceRoll(npcMagicLevel, npcMagicDefence));
	}

	/** True when the Magic attack bonus alone guarantees a splash on any target. */
	static boolean splashes(int magicBonus)
	{
		return magicBonus <= SPLASH_BONUS;
	}
}
