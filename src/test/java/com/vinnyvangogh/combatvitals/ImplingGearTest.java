package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/** The "Can't catch" note on an impling click (Board, 2026-11-19). */
public class ImplingGearTest
{
	@Test
	public void hunterOneWithNoNetIsToldWhatIsMissing()
	{
		assertEquals(Arrays.asList("a butterfly net", "an impling jar", "17 Hunter"),
			ImplingGear.missing("Baby impling", Collections.<String>emptyList(), 1));
		assertEquals("Can't catch Baby impling: needs a butterfly net / an impling jar / 17 Hunter",
			ImplingGear.text("Baby impling", ImplingGear.missing("Baby impling", Collections.<String>emptyList(), 1)));
	}

	@Test
	public void aWieldedOrCarriedNetAndAJarAtLevelNeedNothing()
	{
		assertTrue(ImplingGear.missing("Baby impling", Arrays.asList("Magic butterfly net", "Impling jar"), 17).isEmpty());
		assertNull(ImplingGear.text("Baby impling", Collections.<String>emptyList()));
	}

	@Test
	public void eachKindHasItsOwnLevel()
	{
		assertEquals(Collections.singletonList("58 Hunter"),
			ImplingGear.missing("Nature impling", Arrays.asList("Butterfly net", "Impling jar"), 57));
	}

	@Test
	public void anUnknownImplingSkipsTheLevelAndANonImplingSaysNothing()
	{
		assertTrue(ImplingGear.missing("Strange impling", Arrays.asList("Butterfly net", "Impling jar"), 1).isEmpty());
		assertTrue(ImplingGear.missing("Highwayman", Collections.<String>emptyList(), 1).isEmpty());
	}
}
