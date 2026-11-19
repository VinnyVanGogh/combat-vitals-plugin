package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** The "Can't catch" note on an impling click (Board, 2026-11-19). */
public class ImplingGearTest
{
	private static final List<String> NONE = Collections.emptyList();
	private static final List<String> NET = Collections.singletonList("Butterfly net");
	private static final List<String> JAR = Collections.singletonList("Impling jar");

	@Test
	public void hunterOneWithNoNetIsToldTheNetOrBarehandedLevel()
	{
		final List<String> missing = ImplingGear.missing("Baby impling", NONE, NONE, 1, false);
		assertEquals(Collections.singletonList("a butterfly net or 27 Hunter"), missing);
		assertEquals("Can't catch Baby impling: needs a butterfly net or 27 Hunter",
			ImplingGear.text("Baby impling", missing));
	}

	@Test
	public void ninetyNineHunterNeedsNoNetOrJarOnTheSurface()
	{
		assertTrue(ImplingGear.missing("Lucky impling", NONE, NONE, 99, false).isEmpty());
		assertNull(ImplingGear.text("Lucky impling", NONE));
	}

	@Test
	public void barehandedAtThirtyCatchesABaby()
	{
		assertTrue(ImplingGear.missing("Baby impling", NONE, NONE, 30, false).isEmpty());
		assertEquals(1, ImplingGear.missing("Baby impling", NONE, NONE, 26, false).size());
	}

	@Test
	public void puroPuroWithoutAJarNeedsOne()
	{
		assertEquals(Collections.singletonList("an impling jar"),
			ImplingGear.missing("Baby impling", NET, NONE, 30, true));
		assertTrue(ImplingGear.missing("Baby impling", NET, JAR, 30, true).isEmpty());
	}

	@Test
	public void aNetCountsOnlyWhenWornAndMustMeetTheLevel()
	{
		// carried but not wielded: treated as barehanded
		assertEquals(1, ImplingGear.missing("Baby impling", NONE, NET, 20, false).size());
		assertEquals(Collections.singletonList("58 Hunter"),
			ImplingGear.missing("Nature impling", Arrays.asList("Magic butterfly net"), NONE, 57, false));
	}

	@Test
	public void anUnknownImplingSkipsTheLevelAndANonImplingSaysNothing()
	{
		assertTrue(ImplingGear.missing("Strange impling", NET, NONE, 1, false).isEmpty());
		assertTrue(ImplingGear.missing("Highwayman", NONE, NONE, 1, true).isEmpty());
	}
}
