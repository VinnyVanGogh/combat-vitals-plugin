package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Max hit and chance to hit against the OSRS Wiki's formulas ("Maximum melee
 * hit", "Maximum magic hit", "Damage per second/Melee"). Each expected number
 * is worked by hand in the comment above it.
 */
public class MaxHitTest
{
	@Test
	public void meleeLevel99AggressiveNoGear()
	{
		// effective = 99 + 3 + 8 = 110; floor(0.5 + 110 * 64 / 640) = floor(11.5) = 11.
		assertEquals(110, MaxHit.effectiveLevel(99, 1.0, MaxHit.STANCE_AGGRESSIVE, 1.0));
		assertEquals(11, MaxHit.physical(99, 1.0, MaxHit.STANCE_AGGRESSIVE, 1.0, 0, 1.0));
	}

	@Test
	public void meleeWithPietyAndStrengthBonus()
	{
		// floor(99 * 1.23) = 121; effective = 121 + 3 + 8 = 132;
		// floor(0.5 + 132 * (189 + 64) / 640) = floor(0.5 + 52.1812) = 52.
		assertEquals(132, MaxHit.effectiveLevel(99, 1.23, MaxHit.STANCE_AGGRESSIVE, 1.0));
		assertEquals(52, MaxHit.physical(99, 1.23, MaxHit.STANCE_AGGRESSIVE, 1.0, 189, 1.0));
	}

	@Test
	public void voidMultipliesTheEffectiveLevelAndRoundsDown()
	{
		// 110 * 1.1 = 121.0000000000000x, floored to 121 (the wiki's order); floor(0.5 + 121 * 64 / 640) = 12.
		assertEquals(121, MaxHit.effectiveLevel(99, 1.0, MaxHit.STANCE_AGGRESSIVE, 1.1));
		assertEquals(12, MaxHit.physical(99, 1.0, MaxHit.STANCE_AGGRESSIVE, 1.1, 0, 1.0));
	}

	@Test
	public void otherMultiplierIsAppliedLastAndRoundsDown()
	{
		// Base 52 (above) * 7/6 (Slayer helm on task, as 1.1667) = 60.67, floored to 60.
		assertEquals(60, MaxHit.physical(99, 1.23, MaxHit.STANCE_AGGRESSIVE, 1.0, 189, 7.0 / 6.0));
	}

	@Test
	public void magicIsTheSpellBaseTimesDamageBonus()
	{
		// Ice Barrage 30 with +20%: 36. Trident of the seas 20 with +15%: 23.
		assertEquals(36, MaxHit.magic(30, 20, 1.0));
		assertEquals(23, MaxHit.magic(20, 15, 1.0));
		assertEquals(30, MaxHit.magic(30, 0, 1.0));
	}

	@Test
	public void chanceToHitWhenAttackBeatsDefence()
	{
		// Attack: 110 * 64 = 7040. Defence of a level 100 NPC with no bonus: 109 * 64 = 6976.
		// 7040 > 6976, so 1 - (6976 + 2) / (2 * (7040 + 1)) = 1 - 6978 / 14082 = 0.504474.
		final long attack = MaxHit.attackRoll(99, 1.0, MaxHit.STANCE_ACCURATE, 1.0, 0);
		assertEquals(7040, attack);
		assertEquals(6976, MaxHit.defenceRoll(100, 0));
		assertEquals(0.504474, MaxHit.hitChance(attack, MaxHit.defenceRoll(100, 0)), 1e-5);
	}

	@Test
	public void chanceToHitWhenDefenceBeatsAttack()
	{
		// 3000 against 6000: 3000 / (2 * 6001) = 0.249958.
		assertEquals(0.249958, MaxHit.hitChance(3000, 6000), 1e-5);
	}

	@Test
	public void expectedHitAndDps()
	{
		// 50% to hit, max 40: 10 per attack. A 4 tick weapon attacks every 2.4 s: 4.1667 per second.
		assertEquals(10.0, MaxHit.expectedHit(40, 0.5), 1e-9);
		assertEquals(4.1667, MaxHit.dps(10.0, 4), 1e-4);
		assertEquals(0, MaxHit.dps(10.0, 0), 0);
	}
}
