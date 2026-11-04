package com.vinnyvangogh.combatvitals;

import java.util.List;
import lombok.Getter;
import net.runelite.client.game.FishingSpot;

/**
 * Picks the fishing spot to go to next: the nearest one of the same spot
 * type (net/bait, lure/bait, cage/harpoon...) that offers a fish the player
 * can catch with the method they were using. Spots loaded in the scene win;
 * the bundled list of known locations ({@link KnownFishingSpots}) is only
 * used when none is loaded.
 *
 * Pure and stateless.
 */
final class NextFishingSpot
{
	/** Tiles added to the distance of a spot on another plane, so any spot on this plane wins. */
	private static final int OTHER_PLANE = 10_000;

	/** A candidate: a spot NPC in the scene, or a known location from the bundled list. */
	@Getter
	static final class Spot
	{
		private final FishingSpot type;
		private final int x;
		private final int y;
		private final int plane;
		private final boolean members;
		private final int minLevel;
		private final String name;

		/** The spot's NPC when it is loaded in the scene, null for a known location. */
		private final Object npc;

		private Spot(FishingSpot type, int x, int y, int plane, boolean members, int minLevel, String name, Object npc)
		{
			this.type = type;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.members = members;
			this.minLevel = minLevel;
			this.name = name;
			this.npc = npc;
		}

		/** A spot NPC in the scene: being there already proves the world lets the player reach it. */
		static Spot loaded(FishingSpot type, int x, int y, int plane, Object npc)
		{
			return new Spot(type, x, y, plane, false, 0, null, npc);
		}

		/** A location from the bundled list, with its members flag and any level the location itself needs. */
		static Spot known(FishingSpot type, int x, int y, int plane, boolean members, int minLevel, String name)
		{
			return new Spot(type, x, y, plane, members, minLevel, name, null);
		}
	}

	/** What the next spot has to be. */
	static final class Need
	{
		private final FishingSpot type;
		private final String method;
		private final int level;
		private final boolean membersWorld;
		private final Object exclude;

		/**
		 * @param type         the spot type being fished
		 * @param method       the menu option it was fished with ("Cage"), or null when unknown
		 * @param level        the player's boosted Fishing level
		 * @param membersWorld whether members-only locations count
		 * @param exclude      the spot NPC being fished now, never the next one; may be null
		 */
		Need(FishingSpot type, String method, int level, boolean membersWorld, Object exclude)
		{
			this.type = type;
			this.method = method;
			this.level = level;
			this.membersWorld = membersWorld;
			this.exclude = exclude;
		}
	}

	private NextFishingSpot()
	{
	}

	/** The nearest loaded spot, or failing that the nearest known location; null when neither fits. */
	static Spot choose(List<Spot> loaded, List<Spot> known, int x, int y, int plane, Need need)
	{
		final Spot spot = nearest(loaded, x, y, plane, need);
		return spot != null ? spot : nearest(known, x, y, plane, need);
	}

	/**
	 * The nearest eligible spot by tile distance (Chebyshev, the way the game
	 * counts). A tie goes to the lower x, then the lower y, so the answer never
	 * depends on the order the scene lists its NPCs in.
	 */
	static Spot nearest(Iterable<Spot> candidates, int x, int y, int plane, Need need)
	{
		if (need.type == null || !offersCatch(need))
		{
			return null;
		}

		Spot best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Spot spot : candidates)
		{
			if (!eligible(spot, need))
			{
				continue;
			}
			final int distance = distance(spot, x, y, plane);
			if (best == null || distance < bestDistance || (distance == bestDistance && before(spot, best)))
			{
				best = spot;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static boolean eligible(Spot spot, Need need)
	{
		return spot.type == need.type
			&& (spot.npc == null || spot.npc != need.exclude)
			&& (!spot.members || need.membersWorld)
			&& spot.minLevel <= need.level;
	}

	/**
	 * Whether the method lands any fish at this level on this spot type. A
	 * method the table does not know is let through: the player was fishing
	 * with it, so it catches something.
	 */
	private static boolean offersCatch(Need need)
	{
		final List<FishingCatches.Fish> fish = FishingCatches.forOption(need.type, need.method);
		return fish.isEmpty() || !FishingCatches.catchable(fish, need.level).isEmpty();
	}

	private static int distance(Spot spot, int x, int y, int plane)
	{
		final int tiles = Math.max(Math.abs(spot.x - x), Math.abs(spot.y - y));
		return spot.plane == plane ? tiles : tiles + OTHER_PLANE;
	}

	private static boolean before(Spot a, Spot b)
	{
		return a.x != b.x ? a.x < b.x : a.y < b.y;
	}
}
