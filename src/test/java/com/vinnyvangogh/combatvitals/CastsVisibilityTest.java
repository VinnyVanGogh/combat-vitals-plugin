package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** When the standalone Casts left overlay shows: contextual and Always show. */
public class CastsVisibilityTest
{
	private static final int STAFF = CastsVisibility.WEAPON_STAFF;
	private static final int BLADED = CastsVisibility.WEAPON_BLADED_STAFF;
	private static final int SWORD = 9;

	// casting

	@Test
	public void autocastWithAStaffIsCasting()
	{
		assertTrue(CastsVisibility.casting(true, STAFF, 0));
		assertTrue(CastsVisibility.casting(true, BLADED, 0));
	}

	@Test
	public void autocastFlagLeftOnWithAMeleeWeaponIsNotCasting()
	{
		assertFalse(CastsVisibility.casting(true, SWORD, 0));
	}

	@Test
	public void staffWithoutAutocastIsNotCasting()
	{
		assertFalse(CastsVisibility.casting(false, STAFF, 0));
	}

	@Test
	public void aHandCastCountsForItsWindowWhateverIsWielded()
	{
		assertTrue(CastsVisibility.casting(false, SWORD, SplashWatcher.MANUAL_TICKS));
		assertTrue(CastsVisibility.casting(false, SWORD, 1));
		assertFalse(CastsVisibility.casting(false, SWORD, 0));
	}

	// the spell

	@Test
	public void autocastFlagLeftOnWhileNotAttackingIsNotCasting()
	{
		// Walked away and started woodcutting with the staff still wielded.
		assertFalse(CastsVisibility.casting(true, STAFF, 0, false));
		assertTrue(CastsVisibility.casting(true, STAFF, 0, true));
		assertTrue(CastsVisibility.casting(false, SWORD, 3, false));
	}

	private static java.util.List<SpellCasts.Castable> best(String... names)
	{
		final java.util.List<SpellCasts.Castable> out = new java.util.ArrayList<>();
		for (String n : names)
		{
			out.add(new SpellCasts.Castable(n, 1, new int[10], new SpellCasts.Result(5, null)));
		}
		return out;
	}

	@Test
	public void theSpellBeingCastWinsInBothModes()
	{
		assertEquals("Fire Surge", CastsVisibility.shown(true, "Fire Surge", false, "Ice Barrage", best("Wind Strike")));
		assertEquals("Fire Surge", CastsVisibility.shown(true, "Fire Surge", true, "Ice Barrage", best("Wind Strike")));
	}

	@Test
	public void contextualModeHasNoSpellWhenNotCasting()
	{
		assertNull(CastsVisibility.shown(false, null, false, "Ice Barrage", best("Wind Strike")));
	}

	@Test
	public void alwaysShowUsesThePickedSpellThenTheBestCastable()
	{
		assertEquals("Ice Barrage", CastsVisibility.shown(false, null, true, " Ice Barrage ", best("Wind Strike")));
		assertEquals("Fire Surge", CastsVisibility.shown(false, null, true, "", best("Fire Surge", "Wind Strike")));
		assertEquals("Fire Surge", CastsVisibility.shown(false, null, true, null, best("Fire Surge", "Wind Strike")));
		assertNull(CastsVisibility.shown(false, null, true, "", best()));
	}

	@Test
	public void aCastingSpellTheTableDoesNotKnowStillFallsBackInAlwaysShow()
	{
		assertEquals("Ice Barrage", CastsVisibility.shown(true, null, true, "Ice Barrage", best()));
	}

	// visible: contextual (always off)

	@Test
	public void contextualShowsWhileCasting()
	{
		assertTrue(CastsVisibility.visible(true, false, true, true, 50, 0, false, true));
	}

	@Test
	public void contextualHidesWhenNotCasting()
	{
		assertFalse(CastsVisibility.visible(true, false, false, true, 50, 0, false, true));
	}

	// visible: Always show

	@Test
	public void alwaysShowStaysUpWhenNotCasting()
	{
		assertTrue(CastsVisibility.visible(true, true, false, true, 50, 0, false, true));
	}

	@Test
	public void alwaysShowStillNeedsAKnownSpell()
	{
		assertFalse(CastsVisibility.visible(true, true, false, false, 0, 0, false, true));
	}

	// switches

	@Test
	public void offHidesItInBothModes()
	{
		assertFalse(CastsVisibility.visible(false, false, true, true, 50, 0, false, true));
		assertFalse(CastsVisibility.visible(false, true, true, true, 50, 0, false, true));
	}

	@Test
	public void anUnknownSpellHidesIt()
	{
		assertFalse(CastsVisibility.visible(true, false, true, false, 0, 0, false, true));
	}

	// merge

	@Test
	public void mergedIntoTheSplashCardItHidesWhileTheCardIsUp()
	{
		assertFalse(CastsVisibility.visible(true, false, true, true, 50, 0, true, true));
		assertFalse(CastsVisibility.visible(true, true, true, true, 50, 0, true, true));
	}

	@Test
	public void withMergeOffBothShow()
	{
		assertTrue(CastsVisibility.visible(true, false, true, true, 50, 0, true, false));
	}

	@Test
	public void withoutTheSplashCardMergeChangesNothing()
	{
		assertTrue(CastsVisibility.visible(true, false, true, true, 50, 0, false, true));
	}

	// the threshold

	@Test
	public void onlyBelowShowsUnderTheLimit()
	{
		assertTrue(CastsVisibility.visible(true, false, true, true, 9, 10, false, true));
		assertFalse(CastsVisibility.visible(true, false, true, true, 10, 10, false, true));
		assertFalse(CastsVisibility.visible(true, false, true, true, 500, 10, false, true));
	}

	@Test
	public void zeroCastsLeftShowsUnderAnyLimit()
	{
		assertTrue(CastsVisibility.visible(true, false, true, true, 0, 1, false, true));
	}

	@Test
	public void unlimitedCastsNeverShowUnderALimit()
	{
		assertFalse(CastsVisibility.visible(true, false, true, true, SpellCasts.INFINITE, 10, false, true));
		assertTrue(CastsVisibility.visible(true, false, true, true, SpellCasts.INFINITE, 0, false, true));
	}

	@Test
	public void theThresholdAppliesInAlwaysShowToo()
	{
		assertFalse(CastsVisibility.visible(true, true, false, true, 50, 10, false, true));
		assertTrue(CastsVisibility.visible(true, true, false, true, 5, 10, false, true));
	}
}
