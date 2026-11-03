package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class NativeHealthBarHiderTest
{
	private static final int FIRST = NativeHealthBarHider.SPRITE_IDS[0];

	/** Stands in for the client's sprite override map; values compared by identity. */
	private Map<Integer, Object> overrides;
	private NativeHealthBarHider<Object> hider;

	@Before
	public void setUp()
	{
		overrides = new HashMap<>();
		hider = new NativeHealthBarHider<>(overrides, id -> new Object());
	}

	@Test
	public void optionIsOffByDefault()
	{
		final CombatVitalsConfig config = new CombatVitalsConfig()
		{
		};
		assertFalse(config.hideGameHealthBar());
	}

	@Test
	public void coversEveryStandardBarFrontAndBack()
	{
		// 30, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160: the same 22 ids
		// Interface Styles' HD health bars override.
		assertEquals(22, NativeHealthBarHider.SPRITE_IDS.length);
		assertEquals(22, Arrays.stream(NativeHealthBarHider.SPRITE_IDS).distinct().count());
	}

	@Test
	public void applyInstallsAndRestoreRemovesEverything()
	{
		assertTrue(hider.apply());
		assertTrue(hider.isActive());
		assertEquals(NativeHealthBarHider.SPRITE_IDS.length, overrides.size());

		assertTrue(hider.restore());
		assertFalse(hider.isActive());
		assertTrue(overrides.isEmpty());
	}

	@Test
	public void applyIsIdempotent()
	{
		assertTrue(hider.apply());
		assertFalse(hider.apply());
	}

	@Test
	public void restoreWithoutApplyDoesNothing()
	{
		final Object foreign = new Object();
		overrides.put(FIRST, foreign);

		assertFalse(hider.restore());
		assertSame(foreign, overrides.get(FIRST));
	}

	@Test
	public void restoreHandsBackWhatItDisplaced()
	{
		// Interface Styles' HD bars were there first.
		final Object hd = new Object();
		overrides.put(FIRST, hd);

		hider.apply();
		assertNotSame(hd, overrides.get(FIRST));

		hider.restore();
		assertSame(hd, overrides.get(FIRST));
		assertEquals(1, overrides.size());
	}

	@Test
	public void reassertsAfterAnotherPluginOverwrites()
	{
		hider.apply();
		final Object mine = overrides.get(FIRST);

		// Interface Styles turned HD bars on while ours was active.
		final Object hd = new Object();
		overrides.put(FIRST, hd);

		assertTrue(hider.apply());
		assertSame(mine, overrides.get(FIRST));

		// Turning ours off leaves the HD bar in place.
		hider.restore();
		assertSame(hd, overrides.get(FIRST));
	}

	@Test
	public void forgetsADisplacedSpriteItsOwnerCleared()
	{
		final Object hd = new Object();
		overrides.put(FIRST, hd);
		hider.apply();

		// Interface Styles turned HD bars off: it removes every id, ours too.
		for (int id : NativeHealthBarHider.SPRITE_IDS)
		{
			overrides.remove(id);
		}

		assertTrue(hider.apply());
		hider.restore();

		// The HD sprite must not come back after its owner dropped it.
		assertNull(overrides.get(FIRST));
		assertTrue(overrides.isEmpty());
	}

	@Test
	public void restoreLeavesAnIdSomeoneElseTookOver()
	{
		hider.apply();
		final Object later = new Object();
		overrides.put(FIRST, later);

		hider.restore();
		assertSame(later, overrides.get(FIRST));
	}

	@Test
	public void skipsSpritesThatCannotBeBuiltYet()
	{
		final NativeHealthBarHider<Object> notReady = new NativeHealthBarHider<>(overrides, id -> null);
		assertFalse(notReady.apply());
		assertFalse(notReady.isActive());
		assertTrue(overrides.isEmpty());
	}
}
