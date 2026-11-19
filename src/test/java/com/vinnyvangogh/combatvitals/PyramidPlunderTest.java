package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PyramidPlunderTest
{
	@Test
	public void outOfRangeRoomHasNoLevel()
	{
		assertEquals(-1, PyramidPlunder.levelForRoom(0));
		assertEquals(-1, PyramidPlunder.levelForRoom(9));
		assertEquals(21, PyramidPlunder.levelForRoom(1));
		assertEquals(91, PyramidPlunder.levelForRoom(8));
	}

	@Test
	public void inactiveShowsNothingAndIgnoresGains()
	{
		final PyramidPlunder p = new PyramidPlunder();
		p.addXp(100);
		p.addLoot(500, 1);
		p.setSecondsLeft(10);
		assertFalse(p.active());
		assertEquals(0, p.roomXp());
		assertEquals(0L, p.roomLoot());
		assertEquals("", p.timerText());
		assertEquals("", p.roomText());
		assertFalse(p.urgent());
	}

	@Test
	public void newRoomResetsTheTallySameRoomKeepsIt()
	{
		final PyramidPlunder p = new PyramidPlunder();
		p.setRoom(2);
		p.addXp(50);
		p.addLoot(300, 2);
		p.setRoom(2);
		assertEquals(50, p.roomXp());
		assertEquals(300L, p.roomLoot());
		assertEquals(2, p.roomItems());
		p.setRoom(3);
		assertEquals(0, p.roomXp());
		assertEquals(0L, p.roomLoot());
		assertEquals(0, p.roomItems());
		assertEquals("Room 3/8", p.roomText());
	}

	@Test
	public void rejectsNegativeGains()
	{
		final PyramidPlunder p = new PyramidPlunder();
		p.setRoom(1);
		p.addXp(-5);
		p.addLoot(-10, 1);
		p.addLoot(10, 0);
		assertEquals(0, p.roomXp());
		assertEquals(0L, p.roomLoot());
		// A negative-value item still counts as one item taken; its value clamps to 0.
		assertEquals(1, p.roomItems());
	}

	@Test
	public void timerFormatsAndGoesUrgent()
	{
		final PyramidPlunder p = new PyramidPlunder();
		p.setRoom(1);
		p.setSecondsLeft(272);
		assertEquals("4:32", p.timerText());
		assertFalse(p.urgent());
		p.setSecondsLeft(65);
		assertEquals("1:05", p.timerText());
		p.setSecondsLeft(30);
		assertTrue(p.urgent());
		p.setSecondsLeft(-1);
		assertEquals("", p.timerText());
		assertFalse(p.urgent());
	}

	@Test
	public void nextRoomNeedsTheLevelAndStopsAtEight()
	{
		final PyramidPlunder p = new PyramidPlunder();
		p.setRoom(1);
		assertFalse(p.nextRoomOpen(30));
		assertTrue(p.nextRoomOpen(31));
		p.setRoom(8);
		assertFalse(p.nextRoomOpen(99));
	}

	@Test
	public void clearEndsTheRun()
	{
		final PyramidPlunder p = new PyramidPlunder();
		p.setRoom(4);
		p.addXp(10);
		p.clear();
		assertFalse(p.active());
		assertEquals(0, p.roomXp());
	}
}
