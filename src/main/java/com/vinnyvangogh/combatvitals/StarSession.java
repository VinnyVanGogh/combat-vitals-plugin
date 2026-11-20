package com.vinnyvangogh.combatvitals;

import java.time.Duration;
import java.util.Locale;

/**
 * Stardust earned this session while mining a shooting star, and the text of
 * the star readout. The client exposes the star's tier through its object id
 * and the stardust through the inventory; neither the star's remaining health
 * nor its layer size is exposed, so neither is shown.
 *
 * Not thread safe; the plugin touches it from the client thread only.
 */
final class StarSession
{
	private int held = -1;
	private int earned;
	private long minedMillis;

	/*
	 * The crashed-star object ids. They are STAR_SIZE_NINE_STAR to
	 * STAR_SIZE_ONE_STAR in the client's gameval ObjectID1 class, which is
	 * package-private in runelite-api 1.13.2 and cannot be imported, so the
	 * values are repeated here. Tier 9 is 41020, tier 8 is 41021, and tiers
	 * 7 down to 1 run 41223 up to 41229.
	 */
	private static final int STAR_TIER_9 = 41020;
	private static final int STAR_TIER_8 = 41021;
	private static final int STAR_TIER_7 = 41223;
	private static final int STAR_TIER_1 = 41229;

	/** Tier 1 to 9 of a crashed star object, or 0 when the id is not a star. */
	static int tierOfObject(int objectId)
	{
		if (objectId == STAR_TIER_9)
		{
			return 9;
		}
		if (objectId == STAR_TIER_8)
		{
			return 8;
		}
		if (objectId >= STAR_TIER_7 && objectId <= STAR_TIER_1)
		{
			return 7 - (objectId - STAR_TIER_7);
		}
		return 0;
	}

	/** Stardust carried changed. A rise is earned; a fall (banked, used) is not a loss. */
	void dustHeld(int count)
	{
		if (held >= 0 && count > held)
		{
			earned += count - held;
		}
		held = count;
	}

	/** One game tick; {@code mining} while the player is working a star. */
	void tick(boolean mining)
	{
		if (mining)
		{
			minedMillis += 600;
		}
	}

	int earned()
	{
		return earned;
	}

	int held()
	{
		return Math.max(0, held);
	}

	int perHour()
	{
		return ShootingStars.perHour(earned, Duration.ofMillis(minedMillis));
	}

	void reset()
	{
		held = -1;
		earned = 0;
		minedMillis = 0;
	}

	/** "Star T7 | 1,240 dust (3,100/h) | carrying 310 | max T8". */
	String label(int tier, int miningLevel)
	{
		final int max = ShootingStars.maxTier(miningLevel);
		final StringBuilder sb = new StringBuilder("Star T").append(tier);
		if (tier > max)
		{
			sb.append(" (needs ").append(ShootingStars.levelFor(tier)).append(')');
		}
		sb.append(" | ").append(String.format(Locale.ROOT, "%,d", earned)).append(" dust");
		if (perHour() > 0)
		{
			sb.append(String.format(Locale.ROOT, " (%,d/h)", perHour()));
		}
		sb.append(" | carrying ").append(held());
		sb.append(" | max T").append(max);
		return sb.toString();
	}
}
