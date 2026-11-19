package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SpellCastsTest
{
	private static SpellCasts.Result casts(String spell, SpellCasts.Supply supply)
	{
		final int[] cost = SpellCasts.cost(spell);
		assertNotNull(spell, cost);
		return SpellCasts.casts(cost, supply);
	}

	@Test
	public void plainRunesTheScarcestOneLimits()
	{
		// Fire Strike: 3 fire, 2 air, 1 mind.
		final SpellCasts.Supply s = new SpellCasts.Supply()
			.rune(SpellCasts.Rune.FIRE, 300).rune(SpellCasts.Rune.AIR, 200).rune(SpellCasts.Rune.MIND, 40);
		final SpellCasts.Result r = casts("Fire Strike", s);
		assertEquals(40, r.casts);
		assertEquals(SpellCasts.Rune.MIND, r.limiting);
	}

	@Test
	public void noRunesIsZeroCasts()
	{
		assertEquals(0, casts("Wind Strike", new SpellCasts.Supply()).casts);
	}

	@Test
	public void aStaffOfFireMakesFireInfiniteSoOthersLimit()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply()
			.rune(SpellCasts.Rune.AIR, 100).rune(SpellCasts.Rune.MIND, 1000);
		assertTrue(s.item("Staff of fire"));
		final SpellCasts.Result r = casts("Fire Strike", s);
		assertEquals(50, r.casts);
		assertEquals(SpellCasts.Rune.AIR, r.limiting);
	}

	@Test
	public void aBattlestaffOrMysticStaffCountsToo()
	{
		for (String name : new String[] {"Air battlestaff", "Mystic air staff", "Staff of air", "Air battlestaff (or)"})
		{
			final SpellCasts.Supply s = new SpellCasts.Supply();
			assertTrue(name, s.item(name));
			assertTrue(name, s.infinite[SpellCasts.Rune.AIR.ordinal()]);
		}
		assertFalse(new SpellCasts.Supply().item("Staff of the dead"));
		assertFalse(new SpellCasts.Supply().item("Abyssal whip"));
		assertFalse(new SpellCasts.Supply().item(null));
	}

	@Test
	public void aCombinationStaffIsBothElements()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply();
		assertTrue(s.item("Mystic smoke staff"));
		assertTrue(s.infinite[SpellCasts.Rune.AIR.ordinal()]);
		assertTrue(s.infinite[SpellCasts.Rune.FIRE.ordinal()]);
		assertFalse(s.infinite[SpellCasts.Rune.WATER.ordinal()]);
	}

	@Test
	public void aSpellWithOnlyInfiniteRunesNeverRunsOut()
	{
		// Wind Strike: 1 air, 1 mind. Staff of air covers air, but mind is finite.
		final SpellCasts.Supply s = new SpellCasts.Supply();
		s.item("Staff of air");
		assertEquals(0, casts("Wind Strike", s).casts);
		s.unlimited(SpellCasts.Rune.MIND);
		final SpellCasts.Result r = casts("Wind Strike", s);
		assertEquals(SpellCasts.INFINITE, r.casts);
		assertNull(r.limiting);
	}

	@Test
	public void aCombinationRunePaysForEitherOfItsElements()
	{
		// Fire Strike: 3 fire, 2 air, 1 mind. 50 smoke runes cover fire or air, 10 mind: 10 casts.
		final SpellCasts.Supply s = new SpellCasts.Supply()
			.combo(SpellCasts.Combo.SMOKE, 50).rune(SpellCasts.Rune.MIND, 10);
		assertEquals(10, casts("Fire Strike", s).casts);
		// 5 per cast from smoke alone: 50 smoke is 10 casts, so 11 mind does not help.
		s.rune(SpellCasts.Rune.MIND, 1);
		assertEquals(10, casts("Fire Strike", s).casts);
	}

	@Test
	public void combinationRunesAreSharedNotCountedTwice()
	{
		// Mist (air+water) 10 only: Water Strike needs 1 water + 1 air per cast, one mist pays for ONE of them.
		final SpellCasts.Supply s = new SpellCasts.Supply()
			.combo(SpellCasts.Combo.MIST, 10).rune(SpellCasts.Rune.MIND, 100);
		assertEquals(5, casts("Water Strike", s).casts);
	}

	@Test
	public void combinationRunesMixWithPlainRunes()
	{
		// Earth Strike: 2 earth, 1 air, 1 mind. 40 earth + 30 dust (earth or air) + 100 mind:
		// n casts need 2n earth + n air; supply earth 40, dust 30 => n=23: needs 46 earth, 23 air: deficit 6+23=29 <= 30.
		final SpellCasts.Supply s = new SpellCasts.Supply()
			.rune(SpellCasts.Rune.EARTH, 40).combo(SpellCasts.Combo.DUST, 30).rune(SpellCasts.Rune.MIND, 100);
		assertEquals(23, casts("Earth Strike", s).casts);
	}

	@Test
	public void sunfireOrOtherRunesNeverGoNegative()
	{
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.DEATH, 3);
		// Ice Barrage: 6 water 4 death 2 blood; no water at all.
		assertEquals(0, casts("Ice Barrage", s).casts);
	}

	@Test
	public void aTomeOfFireOrWaterMakesThatRuneInfinite()
	{
		final SpellCasts.Supply fire = new SpellCasts.Supply();
		assertTrue(fire.item("Tome of fire"));
		assertTrue(fire.infinite[SpellCasts.Rune.FIRE.ordinal()]);
		final SpellCasts.Supply water = new SpellCasts.Supply();
		assertTrue(water.item("Tome of water"));
		assertTrue(water.infinite[SpellCasts.Rune.WATER.ordinal()]);
		// An empty tome gives nothing.
		final SpellCasts.Supply empty = new SpellCasts.Supply();
		assertFalse(empty.item("Tome of fire (empty)"));
		assertFalse(empty.infinite[SpellCasts.Rune.FIRE.ordinal()]);
	}

	@Test
	public void theTomeAndAStaffTogetherCoverFireSurge()
	{
		// Fire Surge: 10 fire, 7 air, 1 wrath. Tome gives fire, staff gives air, 25 wrath.
		final SpellCasts.Supply s = new SpellCasts.Supply().rune(SpellCasts.Rune.WRATH, 25);
		s.item("Tome of fire");
		s.item("Staff of air");
		final SpellCasts.Result r = casts("Fire Surge", s);
		assertEquals(25, r.casts);
		assertEquals(SpellCasts.Rune.WRATH, r.limiting);
	}

	@Test
	public void aRunePouchIsJustMoreRunes()
	{
		// Pouch: 100 chaos, 200 air, and inventory 50 earth: Earth Bolt is 2 air, 3 earth, 1 chaos.
		final SpellCasts.Supply s = new SpellCasts.Supply()
			.rune(SpellCasts.Rune.CHAOS, 100).rune(SpellCasts.Rune.AIR, 200).rune(SpellCasts.Rune.EARTH, 50);
		final SpellCasts.Result r = casts("Earth Bolt", s);
		assertEquals(16, r.casts);
		assertEquals(SpellCasts.Rune.EARTH, r.limiting);
	}

	@Test
	public void spellNamesAreCaseAndSpaceTolerantAndUnknownIsNull()
	{
		assertNotNull(SpellCasts.cost("  ICE BARRAGE "));
		assertNull(SpellCasts.cost("Varrock Teleport"));
		assertNull(SpellCasts.cost(null));
	}

	@Test
	public void theCostTablePinsTheElementalSpells()
	{
		// {air, water, earth, fire} per tier; the extra rune is mind, chaos, death, blood, wrath.
		final String[] tiers = {"Strike", "Bolt", "Blast", "Wave", "Surge"};
		final int[][] wind = {{1}, {2}, {3}, {5}, {7}};
		final int[][] water = {{1, 1}, {2, 2}, {3, 3}, {5, 7}, {7, 10}};
		final int[][] earth = {{1, 2}, {2, 3}, {3, 4}, {5, 7}, {7, 10}};
		final int[][] fire = {{2, 3}, {3, 4}, {4, 5}, {5, 7}, {7, 10}};
		for (int t = 0; t < tiers.length; t++)
		{
			final int[] extra = new int[10];
			extra[new int[] {4, 5, 6, 7, 9}[t]] = 1;
			assertCost("Wind " + tiers[t], extra, wind[t][0], 0, 0, 0);
			assertCost("Water " + tiers[t], extra, water[t][0], water[t][1], 0, 0);
			assertCost("Earth " + tiers[t], extra, earth[t][0], 0, earth[t][1], 0);
			assertCost("Fire " + tiers[t], extra, fire[t][0], 0, 0, fire[t][1]);
		}
	}

	private static void assertCost(String spell, int[] extra, int air, int water, int earth, int fire)
	{
		final int[] expected = extra.clone();
		expected[0] = air;
		expected[1] = water;
		expected[2] = earth;
		expected[3] = fire;
		assertArrayEquals(spell, expected, SpellCasts.cost(spell));
	}

	@Test
	public void crumbleUndeadIsTwoAirTwoEarthOneChaos()
	{
		assertArrayEquals(new int[] {2, 0, 2, 0, 0, 1, 0, 0, 0, 0}, SpellCasts.cost("Crumble Undead"));
	}

	@Test
	public void theBoardsFourFixedSpells()
	{
		assertEquals(7, SpellCasts.cost("Water Wave")[SpellCasts.Rune.WATER.ordinal()]);
		assertEquals(10, SpellCasts.cost("Water Surge")[SpellCasts.Rune.WATER.ordinal()]);
		assertEquals(10, SpellCasts.cost("Earth Surge")[SpellCasts.Rune.EARTH.ordinal()]);
		assertEquals(2, SpellCasts.cost("Crumble Undead")[SpellCasts.Rune.AIR.ordinal()]);
	}

	@Test
	public void theAutocastVarbitCountsFromOneAtWindStrike()
	{
		assertNull(SpellCasts.autocastName(0));
		assertEquals("Wind Strike", SpellCasts.autocastName(1));
		assertEquals("Water Strike", SpellCasts.autocastName(2));
		assertEquals("Fire Strike", SpellCasts.autocastName(4));
		assertEquals("Wind Bolt", SpellCasts.autocastName(5));
		assertEquals("Fire Blast", SpellCasts.autocastName(12));
		assertEquals("Wind Wave", SpellCasts.autocastName(13));
		assertEquals("Fire Wave", SpellCasts.autocastName(16));
		assertEquals("Crumble Undead", SpellCasts.autocastName(17));
		assertEquals("Magic Dart", SpellCasts.autocastName(18));
		assertNull(SpellCasts.autocastName(19));
		assertEquals("Smoke Rush", SpellCasts.autocastName(31));
		assertEquals("Ice Rush", SpellCasts.autocastName(34));
		assertEquals("Smoke Burst", SpellCasts.autocastName(35));
		assertEquals("Ice Barrage", SpellCasts.autocastName(46));
		assertEquals("Iban Blast", SpellCasts.autocastName(47));
		assertEquals("Wind Surge", SpellCasts.autocastName(48));
		assertEquals("Fire Surge", SpellCasts.autocastName(51));
		assertNull(SpellCasts.autocastName(52));
	}

	@Test
	public void theAutocastTableNamesRealSpells()
	{
		for (int v = 0; v < 100; v++)
		{
			final String name = SpellCasts.autocastName(v);
			if (name != null)
			{
				assertNotNull(name, SpellCasts.cost(name));
			}
		}
	}
}
