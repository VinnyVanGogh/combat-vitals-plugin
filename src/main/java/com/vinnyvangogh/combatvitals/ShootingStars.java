package com.vinnyvangogh.combatvitals;

import java.time.Duration;

/**
 * Numbers for the shooting-star readout: which tier the player can mine, how
 * much of a layer is left, stardust per hour and the time to the next layer.
 *
 * Tier t needs Mining level 10 * t (tiers 1 to 9), per the OSRS wiki's
 * Shooting Stars page. Pure: values in, values out.
 */
final class ShootingStars
{
	static final int MIN_TIER = 1;
	static final int MAX_TIER = 9;

	private ShootingStars()
	{
	}

	/** Mining level needed to mine a star of this tier. */
	static int levelFor(int tier)
	{
		return tier * 10;
	}

	/** Highest tier a player with this Mining level can mine, 0 below level 10. */
	static int maxTier(int miningLevel)
	{
		return Math.max(0, Math.min(MAX_TIER, miningLevel / 10));
	}

	/** Percent of the current layer left, 0 to 100; 0 when the layer size is unknown. */
	static int layerPercentLeft(int remaining, int layerSize)
	{
		if (layerSize <= 0)
		{
			return 0;
		}
		int clamped = Math.max(0, Math.min(layerSize, remaining));
		return (int) Math.round(clamped * 100.0 / layerSize);
	}

	/** Stardust per hour from the session total and the time mined; 0 before any time has passed. */
	static int perHour(int amount, Duration mined)
	{
		long ms = mined.toMillis();
		if (ms <= 0)
		{
			return 0;
		}
		return (int) Math.round(amount * 3_600_000.0 / ms);
	}

	/**
	 * Time to break the current layer at the rate seen so far, or null when no
	 * rate is known yet. {@code removedSoFar} and {@code mined} give the rate.
	 */
	static Duration timeToNextLayer(int remaining, int removedSoFar, Duration mined)
	{
		if (removedSoFar <= 0 || mined.isZero() || mined.isNegative() || remaining <= 0)
		{
			return null;
		}
		return Duration.ofMillis(Math.round(remaining * (double) mined.toMillis() / removedSoFar));
	}
}
