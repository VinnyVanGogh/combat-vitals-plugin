package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RunecraftPouchesTest
{
	@Test
	public void fillStopsAtCapacity()
	{
		final RunecraftPouches p = new RunecraftPouches(RunecraftPouches.Kind.LARGE);
		assertEquals(9, p.fill(28));
		assertEquals(0, p.fill(28));
		assertEquals(9, p.held());
		assertEquals(1, p.fills());
	}

	@Test
	public void emptyReturnsHeldAndIgnoresEmptyPouch()
	{
		final RunecraftPouches p = new RunecraftPouches(RunecraftPouches.Kind.GIANT);
		assertEquals(0, p.empty());
		assertEquals(0, p.empties());
		p.fill(5);
		assertEquals(5, p.empty());
		assertEquals(0, p.held());
		assertEquals(1, p.empties());
	}

	@Test
	public void negativeAndZeroFillsAreIgnored()
	{
		final RunecraftPouches p = new RunecraftPouches(RunecraftPouches.Kind.MEDIUM);
		assertEquals(0, p.fill(-4));
		assertEquals(0, p.fill(0));
		assertEquals(0, p.fills());
	}

	@Test
	public void degradeCountsDownAndRepairRestarts()
	{
		final RunecraftPouches p = new RunecraftPouches(RunecraftPouches.Kind.GIANT);
		for (int i = 0; i < 10; i++)
		{
			p.fill(12);
			p.empty();
		}
		assertEquals(0, p.usesLeft());
		assertTrue(p.needsRepair());
		p.repaired();
		assertEquals(10, p.usesLeft());
		assertFalse(p.needsRepair());
	}

	@Test
	public void unknownLimitReportsMinusOneAndNeverNeedsRepair()
	{
		final RunecraftPouches p = new RunecraftPouches(RunecraftPouches.Kind.SMALL);
		p.fill(3);
		assertEquals(-1, p.usesLeft());
		assertFalse(p.needsRepair());
	}
}
