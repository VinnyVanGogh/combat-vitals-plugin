package com.vinnyvangogh.combatvitals;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Points the game's own hint arrow at the next fishing spot, tree or rock,
 * and owns when it goes away again.
 *
 * Only an arrow this class set is ever cleared. A hint arrow that is already
 * up and is not ours (a quest, a clue) is never overwritten: pointing is
 * skipped. An arrow of ours that the game replaced or cleared is forgotten,
 * never "cleared" again over whatever replaced it.
 *
 * The arrow follows a spot NPC when it moves (the game does that); a tree or
 * rock is a tile. It is dropped once the player has come within the
 * configured distance of the target and then walked farther than it again:
 * a far target (a known fishing location off in the world) must not be
 * dropped the moment it is set.
 *
 * Client thread only. The game is reached through {@link Pointer}, so the
 * whole lifecycle is testable without a client.
 */
@Slf4j
final class NextSpotArrow
{
	/** The game's hint arrow. */
	interface Pointer
	{
		/** Any hint arrow is up, ours or not. */
		boolean hasArrow();

		/** The arrow up now is pointing at this target. */
		boolean showing(Target target);

		void show(Target target);

		/** Takes down whatever arrow is up. */
		void hide();
	}

	/** What the arrow points at: a spot NPC (followed by the game) or a tile. */
	static final class Target
	{
		private final Object npc;
		private final int x;
		private final int y;
		private final int plane;

		private Target(Object npc, int x, int y, int plane)
		{
			this.npc = npc;
			this.x = x;
			this.y = y;
			this.plane = plane;
		}

		/** A spot NPC, standing on this tile now. */
		static Target npc(Object npc, int x, int y, int plane)
		{
			return new Target(npc, x, y, plane);
		}

		/** A tile: a tree, a rock, or a known location with no NPC loaded. */
		static Target tile(int x, int y, int plane)
		{
			return new Target(null, x, y, plane);
		}

		/** The NPC, or null for a tile. */
		Object npc()
		{
			return npc;
		}

		int x()
		{
			return x;
		}

		int y()
		{
			return y;
		}

		int plane()
		{
			return plane;
		}
	}

	private final Pointer pointer;
	private final BooleanSupplier enabled;
	private final IntSupplier maxDistance;

	/** What this class pointed at and has not cleared; null when it has no arrow up. */
	private Target ours;

	/** The player has been within the distance of {@link #ours} since it was set. */
	private boolean arrived;

	NextSpotArrow(Pointer pointer, BooleanSupplier enabled, IntSupplier maxDistance)
	{
		this.pointer = pointer;
		this.enabled = enabled;
		this.maxDistance = maxDistance;
	}

	/** Whether the setting is on. */
	boolean enabled()
	{
		return enabled.getAsBoolean();
	}

	/**
	 * Points the arrow at the target, replacing an arrow of ours.
	 *
	 * @return whether it is pointing there now; false when the setting is
	 * off or another arrow (not ours) is up
	 */
	boolean point(Target target)
	{
		if (!enabled.getAsBoolean() || target == null)
		{
			return false;
		}
		forgetIfTaken();
		if (ours == null && pointer.hasArrow())
		{
			log.debug("A hint arrow that is not ours is up: not pointing");
			return false;
		}
		pointer.show(target);
		ours = target;
		arrived = false;
		return true;
	}

	/** Takes down the arrow, if it is ours. */
	void clear()
	{
		if (ours == null)
		{
			return;
		}
		if (pointer.showing(ours))
		{
			pointer.hide();
		}
		ours = null;
		arrived = false;
	}

	/**
	 * One game tick: forgets an arrow the game took over, and drops ours once
	 * the player has come and gone, or the setting went off.
	 */
	void tick(int playerX, int playerY, int playerPlane)
	{
		if (ours == null)
		{
			return;
		}
		if (!enabled.getAsBoolean())
		{
			clear();
			return;
		}
		if (forgetIfTaken())
		{
			return;
		}
		if (ours.plane() != playerPlane)
		{
			return;
		}
		final int distance = Math.max(Math.abs(ours.x() - playerX), Math.abs(ours.y() - playerY));
		if (distance <= maxDistance.getAsInt())
		{
			arrived = true;
		}
		else if (arrived)
		{
			log.debug("Player is more than {} tiles from the next spot: clearing the arrow", maxDistance.getAsInt());
			clear();
		}
	}

	/** An arrow of ours is up. */
	boolean active()
	{
		return ours != null;
	}

	/** Whether the arrow of ours is pointing at this NPC. */
	boolean targets(Object npc)
	{
		return ours != null && npc != null && ours.npc() == npc;
	}

	/** Whether the arrow of ours is pointing at this tile. */
	boolean targetsTile(int x, int y, int plane)
	{
		return ours != null && ours.npc() == null && ours.x() == x && ours.y() == y && ours.plane() == plane;
	}

	/** An arrow of ours that is no longer up was taken by the game: forget it without touching what replaced it. */
	private boolean forgetIfTaken()
	{
		if (ours != null && !pointer.showing(ours))
		{
			ours = null;
			arrived = false;
			return true;
		}
		return false;
	}
}
