package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import java.awt.Rectangle;
import org.junit.Test;

/**
 * The badge and your own health bar both sit over your head. When both are
 * up the badge moves above the bar, and the overhead chat copy (RUNELITE-29)
 * goes above whichever is topmost.
 */
public class OverheadStackTest
{
	@Test
	public void nothingBelowMeansNoLift()
	{
		assertEquals(0, OverheadStack.liftAbove(null));
	}

	@Test
	public void theBadgeClearsTheBarAndTheGap()
	{
		final Rectangle bar = new Rectangle(100, 200, 60, 16);
		assertEquals(16 + OverheadStack.GAP, OverheadStack.liftAbove(bar));
	}

	@Test
	public void twoBoxesStackedDoNotOverlap()
	{
		final Rectangle bar = new Rectangle(100, 200, 60, 16);
		final int lift = OverheadStack.liftAbove(bar);
		final Rectangle badge = new Rectangle(90, 200 - lift, 80, 16);

		assertEquals("badge bottom sits GAP above the bar top", bar.y - OverheadStack.GAP, badge.y + badge.height);
		assertEquals(false, badge.intersects(bar));
	}

	@Test
	public void topmostIsTheUnionForTheChatCopy()
	{
		final Rectangle bar = new Rectangle(100, 200, 60, 16);
		final Rectangle badge = new Rectangle(90, 180, 80, 16);
		final Rectangle union = OverheadStack.union(bar, badge);

		assertEquals(new Rectangle(90, 180, 80, 36), union);
		assertEquals("one box alone is itself", bar, OverheadStack.union(bar, null));
		assertEquals(badge, OverheadStack.union(null, badge));
		assertEquals(null, OverheadStack.union(null, null));
	}
}
