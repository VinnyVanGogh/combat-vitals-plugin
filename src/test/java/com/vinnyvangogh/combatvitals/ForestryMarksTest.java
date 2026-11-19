package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ForestryMarksTest
{
	private static final Object ROOT = new Object();
	private static final Object BUSH = new Object();

	@Test
	public void nothingSpawnedMeansNothingToOutline()
	{
		final ForestryMarks marks = new ForestryMarks();
		assertTrue(marks.isEmpty());
		assertTrue(marks.wanted(kind -> true).isEmpty());
	}

	@Test
	public void aDespawnedPartIsNoLongerOutlined()
	{
		final ForestryMarks marks = new ForestryMarks();
		marks.spawned(SkillingEvents.Forestry.RISING_ROOTS, ROOT);
		marks.despawned(SkillingEvents.Forestry.RISING_ROOTS, ROOT);
		assertTrue(marks.wanted(kind -> true).isEmpty());
	}

	@Test
	public void onlyTheSwitchedOnKindsAreWanted()
	{
		final ForestryMarks marks = new ForestryMarks();
		marks.spawned(SkillingEvents.Forestry.RISING_ROOTS, ROOT);
		marks.spawned(SkillingEvents.Forestry.FLOWERING_BUSH, BUSH);
		final Set<SkillingEvents.Forestry> on = EnumSet.of(SkillingEvents.Forestry.FLOWERING_BUSH);
		final List<Object> wanted = marks.wanted(on::contains);
		assertEquals(1, wanted.size());
		assertTrue(wanted.contains(BUSH));
		assertFalse(wanted.contains(ROOT));
	}

	@Test
	public void clearForgetsEverythingAndNullsAreIgnored()
	{
		final ForestryMarks marks = new ForestryMarks();
		marks.spawned(SkillingEvents.Forestry.BEE_HIVE, ROOT);
		marks.spawned(null, ROOT);
		marks.spawned(SkillingEvents.Forestry.BEE_HIVE, null);
		marks.despawned(SkillingEvents.Forestry.POACHERS, ROOT);
		marks.clear();
		assertTrue(marks.isEmpty());
	}

	@Test
	public void everyForestryKindHasASwitch()
	{
		final CombatVitalsConfig config = new CombatVitalsConfig()
		{
		};
		for (SkillingEvents.Forestry kind : SkillingEvents.Forestry.values())
		{
			assertTrue(kind + " defaults on", ForestryHighlightOverlay.wants(config, kind));
		}
	}
}
