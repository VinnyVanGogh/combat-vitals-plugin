package com.vinnyvangogh.combatvitals;

/**
 * Picks the nearest tree or rock of the same kind to go to next. Pure and
 * stateless.
 */
final class NextObjectSpot
{
	/** Tiles added to a candidate on another plane, so any on this plane wins. */
	private static final int OTHER_PLANE = 10_000;

	/** A loaded tree or rock the player can work. */
	static final class Candidate
	{
		private final int x;
		private final int y;
		private final int plane;

		Candidate(int x, int y, int plane)
		{
			this.x = x;
			this.y = y;
			this.plane = plane;
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

	private NextObjectSpot()
	{
	}

	/**
	 * The nearest candidate by tile distance (Chebyshev, the way the game
	 * counts), never the excluded tile. A tie goes to the lower x, then the
	 * lower y, so the answer never depends on iteration order.
	 *
	 * @return the candidate, or null when there is none
	 */
	static Candidate nearest(Iterable<Candidate> candidates, int x, int y, int plane,
		int excludeX, int excludeY, int excludePlane)
	{
		Candidate best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Candidate candidate : candidates)
		{
			if (candidate.x == excludeX && candidate.y == excludeY && candidate.plane == excludePlane)
			{
				continue;
			}
			final int distance = Math.max(Math.abs(candidate.x - x), Math.abs(candidate.y - y))
				+ (candidate.plane == plane ? 0 : OTHER_PLANE);
			if (best == null || distance < bestDistance
				|| (distance == bestDistance && (candidate.x < best.x || (candidate.x == best.x && candidate.y < best.y))))
			{
				best = candidate;
				bestDistance = distance;
			}
		}
		return best;
	}
}
