package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SessionColumnsTest
{
	/** "[fish] 24 caught in 3m", "10.6k xp/h", "459 fish/h", "lvl 18 in 6m", padding included. */
	private static final int[] EVERY_STAT = {96, 58, 60, 70};

	@Test
	public void columnsAreSizedToTheirOwnCellNotTheWidest()
	{
		final int[][] lines = SessionColumns.lines(EVERY_STAT, 400, true);

		assertEquals(1, lines.length);
		// The old layout made every column the widest's 96: 384 wide.
		assertEquals(284, SessionColumns.width(EVERY_STAT, lines));
	}

	@Test
	public void aRowWiderThanTheBadgeWrapsToTwoLines()
	{
		final int[][] lines = SessionColumns.lines(EVERY_STAT, 160, true);

		assertEquals(2, lines.length);
		assertArrayEquals(new int[]{0, 2}, lines[0]);
		assertArrayEquals(new int[]{2, 4}, lines[1]);
		assertEquals("the wider line is as narrow as a split can make it", 154,
			SessionColumns.width(EVERY_STAT, lines));
	}

	@Test
	public void compactNeverWraps()
	{
		final int[][] lines = SessionColumns.lines(EVERY_STAT, 100, false);

		assertEquals(1, lines.length);
		assertArrayEquals(new int[]{0, 4}, lines[0]);
	}

	@Test
	public void aSessionRowWithNothingElseOnTheBadgeNeverWraps()
	{
		assertEquals(1, SessionColumns.lines(EVERY_STAT, 0, true).length);
	}

	@Test
	public void oneCellCannotWrap()
	{
		assertEquals(1, SessionColumns.lines(new int[]{300}, 100, true).length);
	}

	@Test
	public void noCellsIsOneEmptyLine()
	{
		final int[][] lines = SessionColumns.lines(new int[0], 100, true);

		assertEquals(1, lines.length);
		assertEquals(0, SessionColumns.width(new int[0], lines));
		assertEquals(0, SessionColumns.fit(new int[0], lines[0], 100).length);
	}

	@Test
	public void aRowThatFitsExactlyStaysOnOneLine()
	{
		assertEquals(1, SessionColumns.lines(EVERY_STAT, 284, true).length);
		assertEquals(2, SessionColumns.lines(EVERY_STAT, 283, true).length);
	}

	@Test
	public void spareWidthIsSharedEvenlyLeftoverToTheLeft()
	{
		final int[] columns = SessionColumns.fit(new int[]{10, 20, 30}, new int[]{0, 3}, 65);

		assertArrayEquals(new int[]{12, 22, 31}, columns);
		assertEquals(65, columns[0] + columns[1] + columns[2]);
	}

	@Test
	public void aLineAlreadyFullIsNotShrunk()
	{
		assertArrayEquals(new int[]{58, 60}, SessionColumns.fit(EVERY_STAT, new int[]{1, 3}, 50));
	}
}
