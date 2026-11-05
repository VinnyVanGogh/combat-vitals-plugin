package com.vinnyvangogh.combatvitals;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
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
 *
 * Whether the spot moved is decided here, from the spot being fished and
 * whether the player has clicked or walked elsewhere since, never from the
 * player's interaction at the moment of the despawn: the game clears that
 * around the same tick the spot moves (Board in-game test, 2026-11-04).
 */
@Slf4j
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

	/**
	 * Game ticks the fished spot is still "ours" after the interaction with it
	 * ends. The game clears the interaction around the tick the spot moves, in
	 * either order, so the despawn can land just after it.
	 */
	static final int GRACE_TICKS = 2;

	private final Supplier<FishingSpotPath> mode;
	private final Router router;

	/** The spot type and method last fished; null before any fishing. */
	private FishingSpot type;
	private String method;

	/**
	 * The spot NPC being fished, until the player clicks or walks elsewhere,
	 * or {@link #GRACE_TICKS} after the interaction with it ended. Its
	 * despawning or turning into another spot is the spot moving.
	 */
	private Object fished;

	/** Ticks left of {@link #fished} once the interaction ended; -1 while still on it. */
	private int graceLeft = -1;

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
	void fishingOn(FishingSpot type, String method, Object spot)
	{
		clearTarget();
		due = false;
		this.type = type;
		this.method = method;
		fished = spot;
		graceLeft = -1;
	}

	/** The player stopped interacting; the fished spot stays ours for {@link #GRACE_TICKS}. */
	void interactionEnded()
	{
		if (fished != null && graceLeft < 0)
		{
			graceLeft = GRACE_TICKS;
		}
	}

	/**
	 * The player clicked an NPC, an object or a tile, or turned to an NPC.
	 * Anything but the spot being fished means they left it, so it going is
	 * not it moving.
	 *
	 * @param npc the NPC clicked or turned to; null for a tile or object
	 */
	void turnedTo(Object npc)
	{
		if (npc != fished)
		{
			fished = null;
		}
	}

	/** The spot being fished turned into something else in place. */
	void changed(Object npc)
	{
		if (npc != null && npc == fished)
		{
			fishedSpotMoved();
		}
	}

	/** The spot being fished moved. Each fished spot moves once. */
	private void fishedSpotMoved()
	{
		fished = null;
		if (mode.get() == FishingSpotPath.ON_SPOT_MOVE)
		{
			log.debug("Fishing spot moved: searching for the next {} spot", type);
			request();
		}
	}

	/** "Path to next spot" on the badge. */
	void requested()
	{
		if (mode.get() != FishingSpotPath.OFF)
		{
			log.debug("Path to next spot clicked: searching for the next {} spot", type);
			request();
		}
	}

	/**
	 * An NPC despawned. The spot being fished going is the spot moving; the
	 * spot being walked to going means finding the next one again.
	 */
	void despawned(Object npc)
	{
		if (npc == null)
		{
			return;
		}
		if (npc == fished)
		{
			fishedSpotMoved();
		}
		if (target != null && target.getNpc() == npc && mode.get() != FishingSpotPath.OFF)
		{
			log.debug("Next fishing spot moved before it was reached: searching again");
			request();
		}
	}

	/** Runs a due search, once, and ages the fished spot's grace. */
	void tick(Picker picker)
	{
		ageGrace();
		if (!due)
		{
			return;
		}
		due = false;

		final NextFishingSpot.Spot next = picker.pick(type, method);
		if (next == null)
		{
			log.debug("No next {} spot found for \"{}\" (instance, level or none known)", type, method);
			clearTarget();
			return;
		}
		log.debug("Picked {} spot at ({}, {}, {})", next.getType(), next.getX(), next.getY(), next.getPlane());

		if (router.available() && router.route(next))
		{
			log.debug("Sent next fishing spot to Shortest Path");
			target = next;
			routed = true;
			return;
		}

		// No Shortest Path: the spot itself is highlighted, when it is loaded.
		log.debug("Shortest Path not available: highlighting the spot instead");
		clearTarget();
		target = next;
	}

	private void ageGrace()
	{
		if (graceLeft > 0 && --graceLeft == 0)
		{
			fished = null;
			graceLeft = -1;
		}
	}

	/** Logged out, hopped or shut down: clears our path and forgets the spot. */
	void reset()
	{
		clearTarget();
		due = false;
		type = null;
		method = null;
		fished = null;
		graceLeft = -1;
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
