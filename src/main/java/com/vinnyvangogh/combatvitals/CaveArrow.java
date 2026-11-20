package com.vinnyvangogh.combatvitals;

/**
 * When the Gemstone Crab cave arrow is up, with no client in it. It comes up
 * the moment the crab's shell appears (the crab has burrowed, so the cave is
 * usable), and then stays, with no timeout, until the player has gone
 * through the cave (they are suddenly far from where they stood, or on
 * another plane) or the next crab spawns.
 */
final class CaveArrow
{
	/** Crawling through a cave puts the player this many tiles or more from where they stood. */
	static final int THROUGH_TILES = 12;

	private boolean shown;
	/** The player went through for this shell; it must not bring the arrow back. */
	private boolean used;
	private int x;
	private int y;
	private int plane;

	boolean shown()
	{
		return shown;
	}

	void reset()
	{
		shown = false;
		used = false;
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
			if (playerPlane != plane || Math.max(Math.abs(playerX - x), Math.abs(playerY - y)) >= THROUGH_TILES)
			{
				shown = false;
				used = true;
			}
		}
		else if (shellSeen && !used)
		{
			shown = true;
			x = playerX;
			y = playerY;
			plane = playerPlane;
		}
		if (!shellSeen && !shown)
		{
			// The shell is gone: the next one is a fresh burrow.
			used = false;
		}
		return shown;
	}
}
