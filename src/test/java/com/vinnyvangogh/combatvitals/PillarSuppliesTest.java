package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The item-name knowledge behind the Food, Doses, Ammo and Charges sources. */
public class PillarSuppliesTest
{
	@Test
	public void foodIsWhatHasAnEatAction()
	{
		assertTrue(PillarSupplies.isFood(new String[] {"Eat", null, null, "Use", "Drop"}));
		assertFalse(PillarSupplies.isFood(new String[] {"Drink", null, null, "Use", "Drop"}));
		assertFalse(PillarSupplies.isFood(new String[0]));
		assertFalse(PillarSupplies.isFood(null));
	}

	@Test
	public void healingCommonFoodsAndSayingWhenUnknown()
	{
		assertEquals(20, PillarSupplies.heal("Shark"));
		assertEquals(22, PillarSupplies.heal("Manta ray"));
		assertEquals(18, PillarSupplies.heal("Cooked karambwan"));
		assertEquals(12, PillarSupplies.heal("Lobster"));
		// The longer, more specific name wins: not "tuna" for a tuna potato.
		assertEquals(22, PillarSupplies.heal("Tuna potato"));
		// Part-eaten, raw and burnt food, and anything not in the table, is unknown rather than a guess.
		assertEquals(-1, PillarSupplies.heal("1/2 apple pie"));
		assertEquals(-1, PillarSupplies.heal("Raw shark"));
		assertEquals(-1, PillarSupplies.heal("Burnt lobster"));
		assertEquals(-1, PillarSupplies.heal("Mystery stew"));
		assertEquals(-1, PillarSupplies.heal(null));
	}

	@Test
	public void potionDosesAreTheNumberInTheName()
	{
		assertEquals(4, PillarSupplies.prayerDoses("Prayer potion(4)"));
		assertEquals(1, PillarSupplies.prayerDoses("Super restore(1)"));
		assertEquals(3, PillarSupplies.prayerDoses("Sanfew serum(3)"));
		// A plain restore potion does not restore prayer; a stamina potion is not a prayer potion.
		assertEquals(0, PillarSupplies.prayerDoses("Restore potion(4)"));
		assertEquals(0, PillarSupplies.prayerDoses("Stamina potion(4)"));
		assertEquals(0, PillarSupplies.prayerDoses("Vial"));
		assertEquals(2, PillarSupplies.staminaDoses("Stamina potion(2)"));
		assertEquals(0, PillarSupplies.staminaDoses("Prayer potion(2)"));
		assertEquals(0, PillarSupplies.staminaDoses(null));
	}

	@Test
	public void throwingWeaponsAreTheirOwnAmmo()
	{
		assertTrue(PillarSupplies.thrown("Rune dart"));
		assertTrue(PillarSupplies.thrown("Red chinchompa"));
		assertTrue(PillarSupplies.thrown("Rune javelin"));
		assertTrue(PillarSupplies.thrown("Rune throwing axe"));
		assertTrue(PillarSupplies.thrown("Rune knife"));
		assertFalse(PillarSupplies.thrown("Toxic blowpipe"));
		assertFalse(PillarSupplies.thrown("Rune crossbow"));
		assertFalse(PillarSupplies.thrown(null));
		assertTrue(PillarSupplies.blowpipe("Toxic blowpipe"));
		assertFalse(PillarSupplies.blowpipe("Rune dart"));
	}

	@Test
	public void aChargesMessageGivesTheItemAndItsCount()
	{
		final PillarSupplies.Charge swamp = PillarSupplies.charge("Your trident of the swamp has 2,317 charges remaining.");
		assertNotNull(swamp);
		assertEquals("trident of the swamp", swamp.item);
		assertEquals(2317, swamp.charges);
		assertEquals(2500, swamp.max);

		final PillarSupplies.Charge sang = PillarSupplies.charge("Your Sanguinesti staff has 19 charges.");
		assertNotNull(sang);
		assertEquals(19, sang.charges);
		assertEquals(20_000, sang.max);

		assertNull(PillarSupplies.charge("You catch a shark."));
		assertNull(PillarSupplies.charge("Your abyssal whip has 3 charges."));
		assertNull(PillarSupplies.charge(null));
	}

	@Test
	public void aBlowpipeCheckGivesDartsAndScales()
	{
		assertArrayEquals(new int[] {1200, 3500}, PillarSupplies.blowpipeCheck("Darts: 1,200. Scales: 3,500."));
		assertArrayEquals(new int[] {1200, 3500}, PillarSupplies.blowpipeCheck("Scales: 3,500. Darts: 1,200."));
		assertNull(PillarSupplies.blowpipeCheck("You catch a shark."));
		assertNull(PillarSupplies.blowpipeCheck(null));
	}

	@Test
	public void aWeaponsMaximumChargesComeFromItsName()
	{
		assertEquals(10_000, PillarSupplies.chargeMax("Trident of the seas (e)"));
		assertEquals(20_000, PillarSupplies.chargeMax("Tumeken's shadow"));
		assertEquals(-1, PillarSupplies.chargeMax("Abyssal whip"));
		assertEquals(-1, PillarSupplies.chargeMax(null));
	}
}
