package com.vinnyvangogh.combatvitals;

/**
 * When the Gemstone Crab cave arrow is up, with no client in it. It comes up
 * the moment the crab's shell appears (the crab has burrowed, so the cave is
 * usable), and then stays, with no timeout, until the player has gone
 * through the cave or the next crab spawns.
 *
 * <p>Going through the cave is a teleport: the player is somewhere else from
 * one tick to the next, or on another plane. Walking or running covers at
 * most two tiles a tick, so walking to the cave, however far, never hides the
 * arrow.
 */
final class CaveArrow
{
	/** Further than this in one tick is a teleport, not walking or running (two tiles a tick at most). */
	static final int JUMP_TILES = 5;

	private boolean shown;
	/** The player went through for this shell; it must not bring the arrow back. */
	private boolean used;
	private boolean hasLast;
	private int lastX;
	private int lastY;
	private int lastPlane;

	boolean shown()
	{
		return shown;
	}

	void reset()
	{
		shown = false;
		used = false;
		hasLast = false;
	}

	/**
	 * One tick.
	 *
	 * @param crabSeen  a crab is in the scene (the next one has spawned)
	 * @param shellSeen the shell is in the scene
	 * @return whether the arrow is up
	 */
	boolean update(boolean crabSeen, boolean shellSeen, int playerX, int playerY, int playerPlane)
	{
		if (crabSeen)
		{
			reset();
			return false;
		}
		if (shown)
		{
			if (hasLast && (playerPlane != lastPlane
				|| Math.max(Math.abs(playerX - lastX), Math.abs(playerY - lastY)) > JUMP_TILES))
			{
				shown = false;
				used = true;
			}
		}
		else if (shellSeen && !used)
		{
			shown = true;
		}
		if (!shellSeen && !shown)
		{
			// The shell is gone: the next one is a fresh burrow.
			used = false;
		}
		hasLast = true;
		lastX = playerX;
		lastY = playerY;
		lastPlane = playerPlane;
		return shown;
	}
}
