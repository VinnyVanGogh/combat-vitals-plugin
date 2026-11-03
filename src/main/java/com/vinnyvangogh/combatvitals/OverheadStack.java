package com.vinnyvangogh.combatvitals;

import java.awt.Rectangle;

/**
 * Stacks the boxes this plugin draws over your own head so they never
 * overlap: your health bar where the readout always sat, the skilling badge
 * above it when both are up, and (RUNELITE-29) the overhead chat copy above
 * whichever is topmost.
 *
 * Pure: rectangles in, pixels out.
 */
final class OverheadStack
{
	/** Pixels left between two stacked boxes. */
	static final int GAP = 2;

	private OverheadStack()
	{
	}

	/**
	 * @param below the box already drawn at the anchor, or null when none is
	 * @return how many pixels to raise the next box so it clears {@code below}
	 */
	static int liftAbove(Rectangle below)
	{
		return below == null ? 0 : below.height + GAP;
	}

	/** The smallest box covering both, either of which may be absent. */
	static Rectangle union(Rectangle a, Rectangle b)
	{
		if (a == null)
		{
			return b;
		}
		if (b == null)
		{
			return a;
		}
		return a.union(b);
	}
}
