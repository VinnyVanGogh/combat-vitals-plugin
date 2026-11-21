package com.vinnyvangogh.combatvitals;

/**
 * When the standalone "Casts left" overlay shows. Pure rules, no client
 * types, so they are unit tested: the watcher feeds in what it read this tick.
 *
 * Two modes. Contextual (the default) shows it only while a combat spell is
 * being cast: autocast with a staff, or a hand-cast in the last few ticks.
 * Always show keeps it up permanently, for the spell last cast or the one
 * picked in settings.
 */
final class CastsVisibility
{
	/** COMBAT_WEAPON_CATEGORY values of weapons that can autocast: Staff and Bladed staff. */
	static final int WEAPON_STAFF = 18;
	static final int WEAPON_BLADED_STAFF = 21;

	private CastsVisibility()
	{
	}

	/**
	 * Is a combat spell being cast? The autocast flag can stay set while a
	 * melee weapon is wielded, so it only counts with a staff or bladed staff;
	 * a hand-cast counts for its window of ticks whatever is wielded.
	 */
	static boolean casting(boolean autocastSet, int weaponCategory, int manualTicks)
	{
		final boolean staff = weaponCategory == WEAPON_STAFF || weaponCategory == WEAPON_BLADED_STAFF;
		return (autocastSet && staff) || manualTicks > 0;
	}

	/**
	 * As {@link #casting(boolean, int, int)}, but an autocast only counts while the player is actually attacking:
	 * the autocast flag stays set with a staff wielded, so without this a player who walked off and started
	 * chopping trees would still read as casting.
	 *
	 * @param attacking the player is interacting with the NPC they clicked Attack on
	 */
	static boolean casting(boolean autocastSet, int weaponCategory, int manualTicks, boolean attacking)
	{
		return casting(autocastSet && attacking, weaponCategory, manualTicks);
	}

	/**
	 * The spell the card is about: the one being cast, else the one picked in settings, else the strongest the
	 * player can cast now (the first of {@code castable}, already strongest first), else null.
	 */
	static String shown(boolean casting, String castingSpell, boolean always, String picked,
		java.util.List<SpellCasts.Castable> castable)
	{
		if (casting && castingSpell != null)
		{
			return castingSpell;
		}
		if (!always)
		{
			return null;
		}
		if (picked != null && !picked.trim().isEmpty())
		{
			return picked.trim();
		}
		return castable.isEmpty() ? null : castable.get(0).name;
	}

	/**
	 * Should the standalone overlay draw?
	 *
	 * @param enabled        the Casts left setting
	 * @param always         Always show, instead of only while casting
	 * @param casting        a spell is being cast right now ({@link #casting})
	 * @param known          the spell has a rune cost in the table
	 * @param casts          casts left, or {@link SpellCasts#INFINITE}
	 * @param onlyBelow      show only under this many casts; 0 always shows
	 * @param splashCardUp   the splashing card is on screen
	 * @param mergeIntoSplash the card carries the casts line while it is up, so this hides
	 */
	static boolean visible(boolean enabled, boolean always, boolean casting, boolean known, int casts,
		int onlyBelow, boolean splashCardUp, boolean mergeIntoSplash)
	{
		if (!enabled || !known)
		{
			return false;
		}
		if (splashCardUp && mergeIntoSplash)
		{
			return false;
		}
		if (!casting && !always)
		{
			return false;
		}
		if (onlyBelow > 0)
		{
			return casts != SpellCasts.INFINITE && casts < onlyBelow;
		}
		return true;
	}
}
