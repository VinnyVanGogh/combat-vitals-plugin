package com.vinnyvangogh.combatvitals;

/**
 * Lays out the badge's session row: each column as wide as its own cell,
 * not as wide as the widest (Board's in-game test, 2026-11-04: with every
 * stat on the badge was far too wide).
 *
 * In the Full layout the count has a line of its own and the rates go on
 * the line under it, for every skill alike. Wrapping only when the row was
 * wider than the rest of the badge made the layout depend on how wide the
 * skill's header happened to be: woodcutting and thieving put "xp/h" on its
 * own line while mining and fishing kept one (Board, 2026-11-06).
 *
 * Pure: cell counts and widths in pixels, padding included, in; lines and
 * column widths out.
 */
final class SessionColumns
{
	private SessionColumns()
	{
	}

	/**
	 * The row's lines, each as {from, to} cell indexes, top line first.
	 *
	 * @param cells      how many cells the row has
	 * @param countFirst the first cell is the count ("24 caught in 3m")
	 * @param wrap       whether two lines are allowed (the Full layout)
	 */
	static int[][] lines(int cells, boolean countFirst, boolean wrap)
	{
		if (!wrap || !countFirst || cells < 2)
		{
			return new int[][]{{0, cells}};
		}
		return new int[][]{{0, 1}, {1, cells}};
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
