package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Which spells the player can cast now, the remove-gear ranking, and the card's state labels. */
public class CastableSpellsTest
{
	private static List<String> names(List<SpellCasts.Castable> list)
	{
		final List<String> out = new ArrayList<>();
		for (SpellCasts.Castable c : list)
		{
			out.add(c.name);
		}
		return out;
	}

	private static SpellCasts.Supply fireStrikeRunes()
	{
		return new SpellCasts.Supply().rune(SpellCasts.Rune.AIR, 100).rune(SpellCasts.Rune.FIRE, 100)
			.rune(SpellCasts.Rune.MIND, 100);
	}

	// castable detection

	@Test
	public void noRunesMeansNothingIsCastable()
	{
		assertTrue(SpellCasts.castable(99, false, false, false, new SpellCasts.Supply()).isEmpty());
	}

	@Test
	public void theMagicLevelLimitsTheList()
	{
		final List<String> low = names(SpellCasts.castable(13, false, false, false, fireStrikeRunes()));
		// Only what air, fire and mind pay for, at level 13: Fire Strike needs 13, Wind Strike 1.
		assertEquals(Arrays.asList("Fire Strike", "Wind Strike"), low);
		assertEquals(Arrays.asList("Wind Strike"), names(SpellCasts.castable(12, false, false, false, fireStrikeRunes())));
		assertEquals(0, SpellCasts.castable(0, false, false, false, fireStrikeRunes()).size());
	}

	@Test
	public void aBoostedLevelCounts()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.AIR, 50)
			.rune(SpellCasts.Rune.FIRE, 50).rune(SpellCasts.Rune.WRATH, 5);
		assertEquals("Fire Surge", SpellCasts.castable(95, false, false, false, s).get(0).name);
		assertTrue(names(SpellCasts.castable(94, false, false, false, s)).stream().noneMatch("Fire Surge"::equals));
	}

	@Test
	public void theBestSpellIsTheStrongestCastable()
	{
		final SpellCasts.Supply s = fireStrikeRunes().rune(SpellCasts.Rune.CHAOS, 10);
		assertEquals("Fire Bolt", SpellCasts.castable(40, false, false, false, s).get(0).name);
	}

	@Test
	public void ancientSpellsNeedTheAncientBook()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.WATER, 100)
			.rune(SpellCasts.Rune.BLOOD, 100).rune(SpellCasts.Rune.DEATH, 100);
		assertTrue(SpellCasts.castable(99, false, false, false, s).isEmpty());
		assertEquals("Ice Barrage",SpellCasts.castable(99, true, false, false, s).get(0).name);
		assertTrue(SpellCasts.ancient("Ice Barrage"));
		assertFalse(SpellCasts.ancient("Fire Surge"));
	}

	@Test
	public void theStandardBookHidesAncientSpellsEvenWithTheirRunes()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.CHAOS, 100)
			.rune(SpellCasts.Rune.DEATH, 100).rune(SpellCasts.Rune.AIR, 100).rune(SpellCasts.Rune.FIRE, 100);
		for (SpellCasts.Castable c : SpellCasts.castable(99, false, false, false, s))
		{
			assertFalse(c.name, SpellCasts.ancient(c.name));
		}
	}

	@Test
	public void spellsNeedingAStaffAreHiddenWithoutIt()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.FIRE, 100)
			.rune(SpellCasts.Rune.DEATH, 100).rune(SpellCasts.Rune.MIND, 100);
		assertFalse(names(SpellCasts.castable(99, false, false, false, s)).contains("Iban Blast"));
		assertFalse(names(SpellCasts.castable(99, false, false, false, s)).contains("Magic Dart"));
		assertTrue(names(SpellCasts.castable(99, false, true, false, s)).contains("Iban Blast"));
		assertTrue(names(SpellCasts.castable(99, false, false, true, s)).contains("Magic Dart"));
	}

	@Test
	public void aStaffThatSuppliesTheRuneMakesTheSpellCastable()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.AIR, 10).rune(SpellCasts.Rune.MIND, 10);
		assertTrue(SpellCasts.castable(99, false, false, false, s).stream().noneMatch(c -> c.name.equals("Fire Strike")));
		s.item("Staff of fire");
		final List<String> withStaff = names(SpellCasts.castable(99, false, false, false, s));
		assertTrue(withStaff.contains("Fire Strike"));
	}

	@Test
	public void combinationRunesAndTheRunePouchCount()
	{
		// A pouch is read into the same supply as the inventory: here as plain runes and a combination rune.
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.MIND, 5)
			.combo(SpellCasts.Combo.SMOKE, 6);
		assertTrue(names(SpellCasts.castable(99, false, false, false, s)).contains("Fire Strike"));
	}

	@Test
	public void runesBankedMeansTheListEmptiesAgain()
	{
		final SpellCasts.Supply withRunes = fireStrikeRunes();
		assertFalse(SpellCasts.castable(99, false, false, false, withRunes).isEmpty());
		assertTrue(SpellCasts.castable(99, false, false, false, new SpellCasts.Supply()).isEmpty());
	}

	// remove-list ranking

	private static GearAdvice.Worn w(String n, int b)
	{
		return new GearAdvice.Worn(n, b);
	}

	@Test
	public void alreadySplashingNeedsNoAdvice()
	{
		assertTrue(GearAdvice.remove(Arrays.asList(w("Staff", 15)), -64).isEmpty());
	}

	@Test
	public void itemsAreRankedByHowMuchTheyLowerTheBonus()
	{
		final List<GearAdvice.Step> steps = GearAdvice.remove(
			Arrays.asList(w("Wizard hat", 3), w("Staff of fire", 12), w("Amulet of magic", 10), w("Boots", -2)), -53);
		assertEquals(3, steps.size());
		assertEquals("Staff of fire", steps.get(0).name);
		assertEquals(12, steps.get(0).lowersBy);
		assertEquals("Amulet of magic", steps.get(1).name);
		assertEquals("Wizard hat", steps.get(2).name);
	}

	@Test
	public void itemsThatRaiseWhenRemovedAreNeverListed()
	{
		final List<GearAdvice.Step> steps = GearAdvice.remove(Arrays.asList(w("Boots", -8), w("Hat", 0)), -53);
		assertTrue(steps.isEmpty());
	}

	@Test
	public void eachStepSaysWhetherItReachesTheSplashBonus()
	{
		// -53 needs 11 off: the 12 alone does it.
		final List<GearAdvice.Step> one = GearAdvice.remove(Arrays.asList(w("Staff", 12), w("Hat", 3)), -53);
		assertEquals(-65, one.get(0).bonusAfter);
		assertTrue(one.get(0).reaches);
		// -53 with 8 and 5: the 8 alone gets -61, both get -66.
		final List<GearAdvice.Step> two = GearAdvice.remove(Arrays.asList(w("Staff", 8), w("Hat", 5)), -53);
		assertFalse(two.get(0).reaches);
		assertTrue(two.get(1).reaches);
		assertEquals(-66, two.get(1).bonusAfter);
	}

	@Test
	public void theListIsCapped()
	{
		final List<GearAdvice.Worn> many = new ArrayList<>();
		for (int i = 1; i <= 8; i++)
		{
			many.add(w("Item " + i, i));
		}
		assertEquals(GearAdvice.MAX_ROWS, GearAdvice.remove(many, 0).size());
	}

	// state labels

	private static SplashReadout card(SplashTracker.Status status)
	{
		final SplashTracker.Result r = new SplashTracker.Result(status, "Magic bonus: -53", null, null);
		return new SplashReadout(r, "Fire Strike", null, -2, false, new int[0], new int[0], 0, 0, 0, 0);
	}

	@Test
	public void splashingMeansNoChanceToHit()
	{
		assertEquals("Splashing", card(SplashTracker.Status.SAFE).state());
	}

	@Test
	public void hittingMeansARealChance()
	{
		assertEquals("Hitting", card(SplashTracker.Status.WARN).state());
		assertEquals("Hitting", card(SplashTracker.Status.LOW).state());
		assertEquals("Hitting", card(SplashTracker.Status.UNKNOWN).state());
	}

	@Test
	public void aStoppedCardIsIdleNeverCasting()
	{
		assertEquals("Idle", card(SplashTracker.Status.SAFE).stopped(10).state());
		assertEquals("Idle", card(SplashTracker.Status.WARN).stopped(10).state());
	}

	@Test
	public void aCastsOnlyCardIsIdleWhenNothingIsBeingCast()
	{
		final CastsReadout c = new CastsReadout("Fire Strike", "142 casts left", 142, false, new int[]{1}, new int[]{1});
		assertEquals("Casting", SplashReadout.castsOnly(c, 0).state());
		c.casting = false;
		c.lastCastAt = 0;
		final SplashReadout idle = SplashReadout.castsOnly(c, 0);
		assertEquals("Idle", idle.state());
		assertTrue(idle.isIdle());
	}
}
