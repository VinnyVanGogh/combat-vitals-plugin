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
		final int[][] lines = SessionColumns.lines(EVERY_STAT.length, true, false);

		assertEquals(1, lines.length);
		// The old layout made every column the widest's 96: 384 wide.
		assertEquals(284, SessionColumns.width(EVERY_STAT, lines));
	}

	@Test
	public void fullPutsTheCountOnItsOwnLineAndTheRatesUnderIt()
	{
		final int[][] lines = SessionColumns.lines(EVERY_STAT.length, true, true);

		assertEquals(2, lines.length);
		assertArrayEquals(new int[]{0, 1}, lines[0]);
		assertArrayEquals(new int[]{1, 4}, lines[1]);
		assertEquals("the rates line is the wider", 188, SessionColumns.width(EVERY_STAT, lines));
	}

	/**
	 * Board's screenshots, 2026-11-06: woodcutting and thieving put "xp/h" on
	 * its own line and mining and fishing did not, because the old rule
	 * wrapped by width. The split no longer looks at any width, so it is
	 * the same for every skill.
	 */
	@Test
	public void theSplitIsTheSameWhateverTheWidths()
	{
		for (int cells = 2; cells <= 4; cells++)
		{
			final int[][] lines = SessionColumns.lines(cells, true, true);
			assertEquals(2, lines.length);
			assertArrayEquals(new int[]{0, 1}, lines[0]);
			assertArrayEquals(new int[]{1, cells}, lines[1]);
		}
	}

	@Test
	public void theCountAloneIsOneLine()
	{
		final int[][] lines = SessionColumns.lines(1, true, true);

		assertEquals(1, lines.length);
		assertArrayEquals(new int[]{0, 1}, lines[0]);
	}

	@Test
	public void ratesWithoutTheCountStayOnOneLine()
	{
		final int[][] lines = SessionColumns.lines(3, false, true);

		assertEquals(1, lines.length);
		assertArrayEquals(new int[]{0, 3}, lines[0]);
	}

	@Test
	public void compactNeverWraps()
	{
		final int[][] lines = SessionColumns.lines(EVERY_STAT.length, true, false);

		assertEquals(1, lines.length);
		assertArrayEquals(new int[]{0, 4}, lines[0]);
	}

	@Test
	public void noCellsIsOneEmptyLine()
	{
		final int[][] lines = SessionColumns.lines(0, false, true);

		assertEquals(1, lines.length);
		assertEquals(0, SessionColumns.width(new int[0], lines));
		assertEquals(0, SessionColumns.fit(new int[0], lines[0], 100).length);
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
