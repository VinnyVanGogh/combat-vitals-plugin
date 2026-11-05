package com.vinnyvangogh.combatvitals;

/**
 * Lays out the badge's session row: each column as wide as its own cell,
 * not as wide as the widest, and the row split over two lines when on one
 * it would be wider than the rest of the badge (Board's in-game test,
 * 2026-11-04: with every stat on the badge was far too wide).
 *
 * Pure: cell widths in pixels, padding included, in; lines and column
 * widths out.
 */
final class SessionColumns
{
	private SessionColumns()
	{
	}

	/**
	 * The row's lines, each as {from, to} cell indexes, top line first.
	 *
	 * @param widths each cell's width, its padding included
	 * @param limit  the width the rest of the badge sets; 0 or less when the
	 *               session row is all there is, which never wraps
	 * @param wrap   whether two lines are allowed (the Full layout)
	 */
	static int[][] lines(int[] widths, int limit, boolean wrap)
	{
		final int n = widths.length;
		if (!wrap || n < 2 || limit <= 0 || sum(widths, 0, n) <= limit)
		{
			return new int[][]{{0, n}};
		}

		// Split where the wider of the two lines is narrowest; on a tie the
		// top line keeps more cells, so it reads left to right, top first.
		int split = 1;
		int best = Integer.MAX_VALUE;
		for (int i = n - 1; i >= 1; i--)
		{
			final int wider = Math.max(sum(widths, 0, i), sum(widths, i, n));
			if (wider < best)
			{
				best = wider;
				split = i;
			}
		}
		return new int[][]{{0, split}, {split, n}};
	}

	/** The widest line's natural width. */
	static int width(int[] widths, int[][] lines)
	{
		int widest = 0;
		for (int[] line : lines)
		{
			widest = Math.max(widest, sum(widths, line[0], line[1]));
		}
		return widest;
	}

	/**
	 * One line's columns stretched to fill the badge: every column keeps its
	 * own width and the spare pixels are shared out evenly, the leftover
	 * ones to the leftmost columns.
	 */
	static int[] fit(int[] widths, int[] line, int lineWidth)
	{
		final int count = line[1] - line[0];
		final int[] columns = new int[count];
		if (count == 0)
		{
			return columns;
		}

		final int spare = Math.max(0, lineWidth - sum(widths, line[0], line[1]));
		for (int i = 0; i < count; i++)
		{
			columns[i] = widths[line[0] + i] + spare / count + (i < spare % count ? 1 : 0);
		}
		return columns;
	}

	private static int sum(int[] widths, int from, int to)
	{
		int total = 0;
		for (int i = from; i < to; i++)
		{
			total += widths[i];
		}
		return total;
	}
}
