package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Polygon;
import java.awt.Rectangle;
import org.junit.Test;

/** The arrow at the edge of the game view that points at an off-screen next fishing spot. */
public class EdgeArrowTest
{
	/** A fixed-mode game view: 512 x 334 at (4, 4). */
	private static final Rectangle VIEW = new Rectangle(4, 4, 512, 334);
	private static final int M = EdgeArrow.MARGIN;

	@Test
	public void insideMeansOnScreenWithRoomToSpare()
	{
		assertTrue(EdgeArrow.inside(VIEW, 260, 171, M));
		assertFalse("off the right", EdgeArrow.inside(VIEW, 600, 171, M));
		assertFalse("on the very edge", EdgeArrow.inside(VIEW, 4, 171, M));
		assertFalse("above", EdgeArrow.inside(VIEW, 260, -30, M));
	}

	@Test
	public void straightRightMeetsTheRightEdge()
	{
		assertArrayEquals(new int[]{4 + 512 - M, 4 + 167}, EdgeArrow.tip(VIEW, 100, 0, M));
	}

	@Test
	public void straightUpMeetsTheTop()
	{
		assertArrayEquals(new int[]{4 + 256, 4 + M}, EdgeArrow.tip(VIEW, 0, -5, M));
	}

	@Test
	public void aDiagonalMeetsWhicheverEdgeIsNearer()
	{
		// The view is wider than tall: a 45 degree ray reaches the bottom first.
		final int[] tip = EdgeArrow.tip(VIEW, 1, 1, M);
		assertEquals(4 + 334 - M, tip[1]);
		assertTrue("short of the right edge", tip[0] < 4 + 512 - M);
	}

	@Test
	public void theTipIsAlwaysInsideTheView()
	{
		for (int degrees = 0; degrees < 360; degrees += 7)
		{
			final double radians = Math.toRadians(degrees);
			final int[] tip = EdgeArrow.tip(VIEW, Math.cos(radians), Math.sin(radians), M);
			assertTrue(degrees + "deg", VIEW.contains(tip[0], tip[1]));
		}
	}

	@Test
	public void noDirectionOrNoRoomGivesNoArrow()
	{
		assertNull(EdgeArrow.tip(VIEW, 0, 0, M));
		assertNull(EdgeArrow.tip(new Rectangle(0, 0, 20, 20), 1, 0, M));
	}

	@Test
	public void theHeadPointsAlongTheDirection()
	{
		final Polygon head = EdgeArrow.head(100, 50, 1, 0);

		assertEquals(3, head.npoints);
		assertEquals("tip", 100, head.xpoints[0]);
		assertEquals("base behind the tip", 100 - EdgeArrow.LENGTH, head.xpoints[1]);
		assertEquals(100 - EdgeArrow.LENGTH, head.xpoints[2]);
		assertEquals(2 * EdgeArrow.HALF_WIDTH, Math.abs(head.ypoints[1] - head.ypoints[2]));
	}
}
