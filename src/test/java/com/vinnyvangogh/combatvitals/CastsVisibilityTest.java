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
	public void theSpellBeingCastWinsInBothModes()
	{
		assertEquals("Fire Surge", CastsVisibility.spell(true, "Fire Surge", false, "Ice Barrage", "Wind Strike"));
		assertEquals("Fire Surge", CastsVisibility.spell(true, "Fire Surge", true, "Ice Barrage", "Wind Strike"));
	}

	@Test
	public void contextualModeHasNoSpellWhenNotCasting()
	{
		assertNull(CastsVisibility.spell(false, null, false, "Ice Barrage", "Wind Strike"));
	}

	@Test
	public void alwaysShowUsesThePickedSpellThenTheLastOne()
	{
		assertEquals("Ice Barrage", CastsVisibility.spell(false, null, true, " Ice Barrage ", "Wind Strike"));
		assertEquals("Wind Strike", CastsVisibility.spell(false, null, true, "", "Wind Strike"));
		assertEquals("Wind Strike", CastsVisibility.spell(false, null, true, null, "Wind Strike"));
		assertNull(CastsVisibility.spell(false, null, true, "", null));
	}

	@Test
	public void aCastingSpellTheTableDoesNotKnowStillFallsBackInAlwaysShow()
	{
		assertEquals("Ice Barrage", CastsVisibility.spell(true, null, true, "Ice Barrage", null));
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
