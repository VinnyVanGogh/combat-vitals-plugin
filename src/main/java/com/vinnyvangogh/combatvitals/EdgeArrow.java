package com.vinnyvangogh.combatvitals;

import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * Where to draw the arrow that points at an off-screen next fishing spot:
 * on the edge of the game view, inset a little, along the line from the
 * view's centre toward the spot, and turned to face it.
 *
 * Pure: rectangles and directions in, points and polygons out.
 */
final class EdgeArrow
{
	/** Arrow length and half its width at the base, in pixels. */
	static final int LENGTH = 16;
	static final int HALF_WIDTH = 8;

	/** How far inside the view's edge the arrow's tip sits. */
	static final int MARGIN = 12;

	private EdgeArrow()
	{
	}

	/** Whether a point is on screen, at least {@code margin} inside the view. */
	static boolean inside(Rectangle view, int x, int y, int margin)
	{
		return x >= view.x + margin && x < view.x + view.width - margin
			&& y >= view.y + margin && y < view.y + view.height - margin;
	}

	/**
	 * The arrow's tip: where a ray from the view's centre in direction
	 * ({@code dx}, {@code dy}) meets the view inset by {@code margin}.
	 *
	 * @return {x, y}, or null with no direction or no room
	 */
	static int[] tip(Rectangle view, double dx, double dy, int margin)
	{
		final double halfWidth = view.width / 2.0 - margin;
		final double halfHeight = view.height / 2.0 - margin;
		if ((dx == 0 && dy == 0) || halfWidth <= 0 || halfHeight <= 0)
		{
			return null;
		}

		final double toSide = dx == 0 ? Double.MAX_VALUE : halfWidth / Math.abs(dx);
		final double toTopOrBottom = dy == 0 ? Double.MAX_VALUE : halfHeight / Math.abs(dy);
		final double t = Math.min(toSide, toTopOrBottom);
		final double centreX = view.x + view.width / 2.0;
		final double centreY = view.y + view.height / 2.0;
		return new int[]{(int) Math.round(centreX + dx * t), (int) Math.round(centreY + dy * t)};
	}

	/** A triangle with its tip at ({@code x}, {@code y}), pointing along ({@code dx}, {@code dy}). */
	static Polygon head(int x, int y, double dx, double dy)
	{
		final double length = Math.hypot(dx, dy);
		final double ux = length == 0 ? 1 : dx / length;
		final double uy = length == 0 ? 0 : dy / length;
		final double baseX = x - ux * LENGTH;
		final double baseY = y - uy * LENGTH;
		final Polygon polygon = new Polygon();
		polygon.addPoint(x, y);
		polygon.addPoint((int) Math.round(baseX - uy * HALF_WIDTH), (int) Math.round(baseY + ux * HALF_WIDTH));
		polygon.addPoint((int) Math.round(baseX + uy * HALF_WIDTH), (int) Math.round(baseY - ux * HALF_WIDTH));
		return polygon;
	}
}
