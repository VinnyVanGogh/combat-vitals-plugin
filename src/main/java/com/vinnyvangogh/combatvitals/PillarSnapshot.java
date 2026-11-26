package com.vinnyvangogh.combatvitals;

import lombok.Builder;
import lombok.Getter;

/**
 * Everything the pillar sources read, taken once a game tick on the client thread so the overlay
 * only formats numbers each frame. -1 means the value is not known (nothing equipped, never seen).
 * Only the player's own state, plus the health bar of the one target the player is fighting.
 */
@Getter
@Builder(toBuilder = true)
final class PillarSnapshot
{
	/** {@link #castsLeft} when the spell cannot run out. */
	static final int INFINITE = SpellCasts.INFINITE;

	/** {@link #castsLeft} when no spell is known. */
	static final int NO_SPELL = -2;

	@Builder.Default private final int hp = -1;
	@Builder.Default private final int maxHp = -1;
	@Builder.Default private final int prayer = -1;
	@Builder.Default private final int maxPrayer = -1;

	/** 0 to 100. */
	@Builder.Default private final int runEnergy = -1;

	/** 0 to 100. */
	@Builder.Default private final int specPercent = -1;

	/** Arrows, bolts or throwing weapons carried for the weapon; or a blowpipe's darts. */
	@Builder.Default private final int ammoCount = -1;
	private final String ammoName;

	/** A blowpipe's scales; -1 for anything else. */
	@Builder.Default private final int ammoExtra = -1;

	/** Per rune, in {@link PillarRune} order: inventory plus rune pouch. */
	private final long[] runeCounts;

	@Builder.Default private final int castsLeft = NO_SPELL;
	private final String castsSpell;

	@Builder.Default private final int charges = -1;
	@Builder.Default private final int chargesMax = -1;
	private final String chargesName;

	@Builder.Default private final int cannonballs = -1;

	@Builder.Default private final int foodCount = -1;
	@Builder.Default private final int foodHeal = 0;
	/** Food carried with no known heal, left out of {@link #foodHeal}. */
	@Builder.Default private final int foodUnknown = 0;

	@Builder.Default private final int prayerDoses = -1;
	@Builder.Default private final int staminaDoses = -1;

	@Builder.Default private final int boostLeft = -1;
	@Builder.Default private final int boostPeak = -1;
	private final String boostSkill;

	/** The NPC or player being fought: its name and health bar, nothing else about it. */
	private final String targetName;
	@Builder.Default private final double targetFraction = -1;
	@Builder.Default private final int targetHp = -1;
	@Builder.Default private final int targetMax = -1;

	long rune(PillarRune rune)
	{
		return runeCounts == null ? -1 : runeCounts[rune.ordinal()];
	}
}
