package com.vinnyvangogh.combatvitals;

import java.util.function.Supplier;
import net.runelite.client.game.FishingSpot;

/**
 * When to find the next fishing spot, and what to do with it: send it to
 * Shortest Path, or highlight it when Shortest Path is not there.
 *
 * Requests (the spot moving, the badge's right-click) only mark a search as
 * due; the search itself runs once on the next game tick. A spot that moves
 * twice in one tick therefore sends one path, and a target that moves again
 * before the player gets there is searched for again, so the path always
 * points at the latest spot. Starting to fish clears what this sent, and
 * nothing else: a path the player set up themselves is never touched.
 *
 * Client thread only. The game is reached through {@link Router} and
 * {@link Picker}, so the whole flow is testable without a client.
 */
final class SpotPathing
{
	/** Shortest Path, or whatever stands in for it. */
	interface Router
	{
		/** Installed and switched on. */
		boolean available();

		/** @return whether the path went out */
		boolean route(NextFishingSpot.Spot spot);

		/** Clears the path this sent. */
		void clear();
	}

	/** Finds the next spot of this type, worked with this method, or null. */
	interface Picker
	{
		NextFishingSpot.Spot pick(FishingSpot type, String method);
	}

	private final Supplier<FishingSpotPath> mode;
	private final Router router;

	/** The spot type and method last fished; null before any fishing. */
	private FishingSpot type;
	private String method;

	private boolean due;

	/** The spot pathed to or highlighted, until the player fishes again. */
	private NextFishingSpot.Spot target;

	/** Whether {@link #target} went to Shortest Path, so there is a path of ours to clear. */
	private boolean routed;

	SpotPathing(Supplier<FishingSpotPath> mode, Router router)
	{
		this.mode = mode;
		this.router = router;
	}

	/** The player started fishing a spot: any path of ours is done with. */
	void fishingOn(FishingSpot type, String method)
	{
		clearTarget();
		due = false;
		this.type = type;
		this.method = method;
	}

	/** The spot being fished moved or despawned. */
	void spotMoved()
	{
		if (mode.get() == FishingSpotPath.ON_SPOT_MOVE)
		{
			request();
		}
	}

	/** "Path to next spot" on the badge. */
	void requested()
	{
		if (mode.get() != FishingSpotPath.OFF)
		{
			request();
		}
	}

	/** An NPC despawned; when it is the spot being walked to, find the next one again. */
	void despawned(Object npc)
	{
		if (npc != null && target != null && target.getNpc() == npc && mode.get() != FishingSpotPath.OFF)
		{
			request();
		}
	}

	/** Runs a due search, once. */
	void tick(Picker picker)
	{
		if (!due)
		{
			return;
		}
		due = false;

		final NextFishingSpot.Spot next = picker.pick(type, method);
		if (next == null)
		{
			clearTarget();
			return;
		}

		if (router.available() && router.route(next))
		{
			target = next;
			routed = true;
			return;
		}

		// No Shortest Path: the spot itself is highlighted, when it is loaded.
		clearTarget();
		target = next;
	}

	/** Logged out, hopped or shut down: clears our path and forgets the spot. */
	void reset()
	{
		clearTarget();
		due = false;
		type = null;
		method = null;
	}

	/** The spot NPC to highlight, when there is one and no path went out for it. */
	Object highlighted()
	{
		return target == null || routed ? null : target.getNpc();
	}

	NextFishingSpot.Spot target()
	{
		return target;
	}

	private void request()
	{
		if (type != null)
		{
			due = true;
		}
	}

	private void clearTarget()
	{
		if (routed)
		{
			router.clear();
		}
		routed = false;
		target = null;
	}
}
