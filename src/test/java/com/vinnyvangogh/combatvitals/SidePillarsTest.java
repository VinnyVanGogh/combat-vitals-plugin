package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Rectangle;
import org.junit.Test;

/**
 * The expected numbers are Status Bars' own placement
 * ({@code StatusBarsOverlay#render}, {@code Viewport}), worked by hand.
 */
public class SidePillarsTest
{
	private static final int X = 500;
	private static final int Y = 200;
	private static final int W = 240;

	private static SidePillars.StatusBars statusBars(boolean left, boolean right, int width)
	{
		return new SidePillars.StatusBars(true, left, right, width);
	}

	@Test
	public void fixedWithoutStatusBarsTakesBothPillars()
	{
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
			SidePillars.StatusBars.OFF);
		assertEquals(new Rectangle(X - 20, Y + 4, 20, 252), layout.getLeft());
		assertEquals(new Rectangle(X + W, Y + 4, 20, 252), layout.getRight());
	}

	@Test
	public void resizedBoxMatchesFixed()
	{
		final SidePillars.Layout fixed = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
			SidePillars.StatusBars.OFF);
		final SidePillars.Layout box = SidePillars.layout(SidePillars.Viewport.RESIZED_BOX, X, Y, W,
			SidePillars.StatusBars.OFF);
		assertEquals(fixed.getLeft(), box.getLeft());
		assertEquals(fixed.getRight(), box.getRight());
	}

	@Test
	public void resizedBottomWithoutStatusBarsSitsLeftOfThePanel()
	{
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.RESIZED_BOTTOM, 1000, 500, W,
			SidePillars.StatusBars.OFF);
		assertEquals(new Rectangle(1010 - 61, 500, 20, 272), layout.getLeft());
		assertEquals(new Rectangle(1010 - 35, 500, 20, 272), layout.getRight());
	}

	@Test
	public void fixedWithStatusBarsOnBothStacksLeftOfIt()
	{
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
			statusBars(true, true, 20));
		assertEquals(X - 20 - 22, layout.getRight().x);
		assertEquals(X - 20 - 44, layout.getLeft().x);
		assertEquals(Y + 4, layout.getLeft().y);
	}

	@Test
	public void fixedWithOnlyTheRightStatusBarUsesTheFreeLeftPillar()
	{
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
			statusBars(false, true, 20));
		assertEquals(X - 20, layout.getRight().x);
		assertEquals(X - 20 - 22, layout.getLeft().x);
	}

	@Test
	public void statusBarsWithBothBarsDisabledLeavesThePillarsFree()
	{
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
			statusBars(false, false, 20));
		assertEquals(X - 20, layout.getLeft().x);
		assertEquals(X + W, layout.getRight().x);
	}

	@Test
	public void resizedBottomFollowsStatusBarsWidth()
	{
		// Width 30: Status Bars' left bar moves 2 x 10 further left.
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.RESIZED_BOTTOM, 1000, 500, W,
			statusBars(true, true, 30));
		assertEquals(1010 - 61 - 20 - 22, layout.getRight().x);
		assertEquals(1010 - 61 - 20 - 44, layout.getLeft().x);
		assertEquals(272, layout.getLeft().height);
	}

	@Test
	public void neverOverlapsStatusBarsOrItself()
	{
		final boolean[] flags = {false, true};
		for (SidePillars.Viewport viewport : SidePillars.Viewport.values())
		{
			for (boolean left : flags)
			{
				for (boolean right : flags)
				{
					for (int width : new int[] {3, 20, 40})
					{
						checkNoOverlap(viewport, statusBars(left, right, width));
					}
				}
			}
		}
	}

	private static void checkNoOverlap(SidePillars.Viewport viewport, SidePillars.StatusBars statusBars)
	{
		final String at = viewport + " " + statusBars.left + "/" + statusBars.right + " w" + statusBars.width;
		final SidePillars.Layout ours = SidePillars.layout(viewport, X, Y, W, statusBars);
		final Rectangle[] theirs = SidePillars.statusBarSlots(viewport, X, Y, W, statusBars);
		assertFalse(at, ours.getLeft().intersects(ours.getRight()));
		assertTrue(at, ours.getLeft().x < ours.getRight().x);
		if (statusBars.left)
		{
			assertFalse(at, ours.getLeft().intersects(theirs[0]));
			assertFalse(at, ours.getRight().intersects(theirs[0]));
		}
		if (statusBars.right)
		{
			assertFalse(at, ours.getLeft().intersects(theirs[1]));
			assertFalse(at, ours.getRight().intersects(theirs[1]));
		}
	}

	@Test
	public void neverOverTheSidePanelInTheClassicLayouts()
	{
		for (boolean left : new boolean[] {false, true})
		{
			for (boolean right : new boolean[] {false, true})
			{
				final SidePillars.Layout ours = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
					statusBars(left, right, 20));
				final Rectangle panel = new Rectangle(X, Y, W, 300);
				assertFalse(ours.getLeft().intersects(panel));
				assertFalse(ours.getRight().intersects(panel));
			}
		}
	}

	@Test
	public void sidesPickTheirBar()
	{
		final SidePillars.Layout layout = SidePillars.layout(SidePillars.Viewport.FIXED, X, Y, W,
			SidePillars.StatusBars.OFF);
		assertEquals(layout.getLeft(), layout.get(PillarSide.LEFT));
		assertEquals(layout.getRight(), layout.get(PillarSide.RIGHT));
		assertEquals(PillarSide.RIGHT, PillarSide.LEFT.other());
	}
}
