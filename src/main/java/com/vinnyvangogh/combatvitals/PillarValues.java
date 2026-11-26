package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Turns a {@link PillarSource} and a {@link PillarSnapshot} into the {@link Pillar} to paint: how
 * full, what colour, the short number, the hover text. Pure: values in, a pillar out. A source
 * with nothing to show (no ammo equipped, no spell known) gives null and its part draws empty.
 */
final class PillarValues
{
	/** What the bars are measured against where the game has no maximum of its own. */
	static final int FOOD_FULL = 28;
	static final int PRAYER_DOSES_FULL = 32;
	static final int STAMINA_DOSES_FULL = 12;
	static final int CASTS_FULL = 100;

	/** The player's choices for how sources look. */
	interface Settings
	{
		Color colour(PillarSource source);

		/** The target bar's colour at this fill, as the health bar's own gradient. */
		Color targetColour(double fraction);

		/** Shown on a running-out bar: ammo or casts at or below the warning level. */
		Color lowColour();

		/** The icon at the top of the pillar for this source, or null. */
		BufferedImage icon(PillarSource source);

		/** Ammo count that fills the bar, and the count at or below which it warns; 0 for no warning. */
		int ammoFull();

		int ammoLow();

		/** Rune count that fills the Runes bar. */
		int runeFull();

		PillarRune rune();

		/** Casts at or below which the casts bar warns; 0 for none. */
		int castsLow();
	}

	private PillarValues()
	{
	}

	/** The pillar for a source, or null when it has nothing to show right now. */
	static Pillar of(PillarSource source, PillarSnapshot s, Settings settings)
	{
		if (source == null || !source.custom())
		{
			return null;
		}
		final Pillar.PillarBuilder p;
		switch (source)
		{
			case HITPOINTS:
				p = ratio(s.getHp(), s.getMaxHp(), "Hitpoints");
				break;
			case PRAYER:
				p = ratio(s.getPrayer(), s.getMaxPrayer(), "Prayer");
				break;
			case RUN_ENERGY:
				p = s.getRunEnergy() < 0 ? null : percent(s.getRunEnergy() / 100.0, "Run energy " + s.getRunEnergy() + "%");
				break;
			case SPEC:
				p = s.getSpecPercent() < 0 ? null : percent(s.getSpecPercent() / 100.0, "Special attack " + s.getSpecPercent() + "%");
				break;
			case AMMO:
				p = ammo(s, settings);
				break;
			case RUNES:
				p = runes(s, settings);
				break;
			case CASTS:
				p = casts(s, settings);
				break;
			case CHARGES:
				p = charges(s);
				break;
			case CANNONBALLS:
				p = s.getCannonballs() < 0 ? null
					: count(s.getCannonballs(), settings.ammoFull(), 0, "Cannonballs: " + s.getCannonballs());
				break;
			case FOOD:
				p = food(s);
				break;
			case PRAYER_DOSES:
				p = s.getPrayerDoses() < 0 ? null : doses(s.getPrayerDoses(), PRAYER_DOSES_FULL,
					"Prayer and restore doses: " + s.getPrayerDoses());
				break;
			case STAMINA:
				p = s.getStaminaDoses() < 0 ? null : doses(s.getStaminaDoses(), STAMINA_DOSES_FULL,
					"Stamina potion doses: " + s.getStaminaDoses());
				break;
			case BOOST:
				p = boost(s);
				break;
			case TARGET_HP:
				p = target(s);
				break;
			default:
				p = null;
				break;
		}
		if (p == null)
		{
			return null;
		}
		final Pillar built = p.build();
		final Color colour = source == PillarSource.TARGET_HP ? settings.targetColour(built.getFraction())
			: built.getColour() != null ? built.getColour() : settings.colour(source);
		return built.toBuilder().colour(colour).icon(settings.icon(source)).bottom(source.getTag()).build();
	}

	/** hp/max style: the number is the current value, the bar is its share of the maximum. */
	private static Pillar.PillarBuilder ratio(int now, int max, String name)
	{
		if (now < 0 || max <= 0)
		{
			return null;
		}
		return Pillar.builder().fraction(clamp((double) now / max)).number(Integer.toString(now))
			.tooltip(name + ": " + now + "/" + max);
	}

	private static Pillar.PillarBuilder percent(double fraction, String tooltip)
	{
		return Pillar.builder().fraction(clamp(fraction)).number(Math.round(fraction * 100) + "%").tooltip(tooltip);
	}

	private static Pillar.PillarBuilder count(long n, int full, int low, String tooltip)
	{
		return Pillar.builder().fraction(clamp((double) n / Math.max(1, full))).number(PillarText.compact(n))
			.tooltip(tooltip);
	}

	private static Pillar.PillarBuilder doses(int n, int full, String tooltip)
	{
		return Pillar.builder().fraction(clamp((double) n / full)).number(Integer.toString(n)).tooltip(tooltip);
	}

	private static Pillar.PillarBuilder ammo(PillarSnapshot s, Settings settings)
	{
		if (s.getAmmoCount() < 0)
		{
			return null;
		}
		final String name = s.getAmmoName() == null ? "Ammo" : s.getAmmoName();
		final StringBuilder tip = new StringBuilder(name).append(": ").append(String.format("%,d", s.getAmmoCount()));
		if (s.getAmmoExtra() >= 0)
		{
			tip.append(", scales ").append(String.format("%,d", s.getAmmoExtra()));
		}
		final boolean low = settings.ammoLow() > 0 && s.getAmmoCount() <= settings.ammoLow();
		if (low)
		{
			tip.append(" (low)");
		}
		final Pillar.PillarBuilder p = count(s.getAmmoCount(), settings.ammoFull(), settings.ammoLow(), tip.toString());
		return low ? p.colour(settings.lowColour()) : p;
	}

	private static Pillar.PillarBuilder runes(PillarSnapshot s, Settings settings)
	{
		final long n = s.rune(settings.rune());
		if (n < 0)
		{
			return null;
		}
		return count(n, settings.runeFull(), 0, settings.rune().getName() + " runes: " + String.format("%,d", n));
	}

	private static Pillar.PillarBuilder casts(PillarSnapshot s, Settings settings)
	{
		final int left = s.getCastsLeft();
		if (left == PillarSnapshot.NO_SPELL)
		{
			return null;
		}
		final String spell = s.getCastsSpell() == null ? "Spell" : s.getCastsSpell();
		if (left == PillarSnapshot.INFINITE)
		{
			return Pillar.builder().fraction(1).number("inf").tooltip(spell + ": unlimited casts");
		}
		final boolean low = settings.castsLow() > 0 && left <= settings.castsLow();
		final Pillar.PillarBuilder p = Pillar.builder().fraction(clamp((double) left / CASTS_FULL))
			.number(PillarText.compact(left)).tooltip(spell + ": " + left + (left == 1 ? " cast" : " casts") + " left"
				+ (low ? " (low)" : ""));
		return low ? p.colour(settings.lowColour()) : p;
	}

	private static Pillar.PillarBuilder charges(PillarSnapshot s)
	{
		if (s.getCharges() < 0)
		{
			return null;
		}
		final int max = s.getChargesMax() > 0 ? s.getChargesMax() : Math.max(1, s.getCharges());
		final String name = s.getChargesName() == null ? "Weapon" : s.getChargesName();
		return Pillar.builder().fraction(clamp((double) s.getCharges() / max)).number(PillarText.compact(s.getCharges()))
			.tooltip(name + ": " + String.format("%,d", s.getCharges()) + " charges");
	}

	private static Pillar.PillarBuilder food(PillarSnapshot s)
	{
		if (s.getFoodCount() < 0)
		{
			return null;
		}
		final String heal = s.getFoodHeal() > 0 ? ", heals " + s.getFoodHeal() : "";
		final String unknown = s.getFoodUnknown() > 0 ? " (" + s.getFoodUnknown() + " with unknown healing)" : "";
		return Pillar.builder().fraction(clamp((double) s.getFoodCount() / FOOD_FULL))
			.number(Integer.toString(s.getFoodCount()))
			.tooltip("Food: " + s.getFoodCount() + heal + unknown);
	}

	private static Pillar.PillarBuilder boost(PillarSnapshot s)
	{
		if (s.getBoostLeft() < 0 || s.getBoostPeak() <= 0)
		{
			return null;
		}
		final String skill = s.getBoostSkill() == null ? "Stat" : s.getBoostSkill();
		return Pillar.builder().fraction(clamp((double) s.getBoostLeft() / s.getBoostPeak()))
			.number("+" + s.getBoostLeft())
			.tooltip(skill + " boost: +" + s.getBoostLeft() + " of +" + s.getBoostPeak());
	}

	/**
	 * Your target's health bar and name, as the base Opponent Information plugin shows them, and
	 * nothing else about it, whether it is an NPC or a player (Plugin Hub: no player scouting).
	 */
	private static Pillar.PillarBuilder target(PillarSnapshot s)
	{
		if (s.getTargetFraction() < 0 || s.getTargetName() == null)
		{
			return null;
		}
		final int hp = s.getTargetHp();
		final String health = hp >= 0 && s.getTargetMax() > 0 ? hp + "/" + s.getTargetMax()
			: Math.round(s.getTargetFraction() * 100) + "%";
		return Pillar.builder().fraction(clamp(s.getTargetFraction()))
			.number(PillarText.targetNumber(hp, s.getTargetFraction()))
			.tooltip("Target: " + s.getTargetName() + " " + health);
	}

	private static double clamp(double fraction)
	{
		return Math.max(0, Math.min(1, fraction));
	}
}
