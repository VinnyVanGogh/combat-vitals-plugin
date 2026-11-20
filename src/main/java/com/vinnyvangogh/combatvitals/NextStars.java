package com.vinnyvangogh.combatvitals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Picks the shooting stars worth hopping to: only stars in the regions the
 * player selected, only tiers within {@link #TIER_WINDOW} of the highest tier
 * they can mine, and of those the {@link #LIMIT} closest.
 *
 * Distance heuristic: straight-line tiles from the player to the star's
 * region anchor (the star's own tile when the source gives one). Stars on a
 * different plane are not offered. Ties go to the higher tier, then the lower
 * world, so the order is stable between refreshes.
 *
 * Pure and stateless. The star list itself comes from a source the player
 * opted into; this class only filters and sorts it.
 */
final class NextStars
{
	/** How many tiers below the highest mineable one still count. */
	static final int TIER_WINDOW = 2;

	/** How many stars are offered. */
	static final int LIMIT = 3;

	/** One active star as reported by the source. */
	static final class Star
	{
		final int world;
		final String region;
		final int tier;
		final int x;
		final int y;
		final int plane;
		final Instant expires;

		Star(int world, String region, int tier, int x, int y, int plane, Instant expires)
		{
			this.world = world;
			this.region = region;
			this.tier = tier;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.expires = expires;
		}
	}

	private NextStars()
	{
	}

	/**
	 * @param stars     every star the source reports
	 * @param regions   the player's selected regions (case-insensitive); empty means none selected, so none offered
	 * @param maxTier   highest tier the player can mine, see {@link ShootingStars#maxTier}
	 * @param playerX   player tile x
	 * @param playerY   player tile y
	 * @param playerPlane player plane
	 * @param now       the current time; stars already expired are dropped
	 */
	static List<Star> pick(List<Star> stars, Set<String> regions, int maxTier,
		int playerX, int playerY, int playerPlane, Instant now)
	{
		List<Star> keep = new ArrayList<>();
		if (maxTier < 1 || regions.isEmpty())
		{
			return keep;
		}
		for (Star s : stars)
		{
			if (s.tier > maxTier || s.tier < maxTier - TIER_WINDOW + 1 || s.tier < 1)
			{
				continue;
			}
			if (!inRegions(s.region, regions))
			{
				continue;
			}
			if (s.plane != playerPlane)
			{
				continue;
			}
			if (s.expires != null && !s.expires.isAfter(now))
			{
				continue;
			}
			keep.add(s);
		}
		keep.sort(Comparator
			.comparingLong((Star s) -> distanceSquared(s, playerX, playerY))
			.thenComparing(Comparator.comparingInt((Star s) -> s.tier).reversed())
			.thenComparingInt(s -> s.world));
		return keep.size() > LIMIT ? new ArrayList<>(keep.subList(0, LIMIT)) : keep;
	}

	private static boolean inRegions(String region, Set<String> regions)
	{
		if (region == null)
		{
			return false;
		}
		for (String r : regions)
		{
			if (r.equalsIgnoreCase(region))
			{
				return true;
			}
		}
		return false;
	}

	private static long distanceSquared(Star s, int x, int y)
	{
		long dx = s.x - x;
		long dy = s.y - y;
		return dx * dx + dy * dy;
	}
}
