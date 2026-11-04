package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import java.awt.Color;
import org.junit.Test;

public class InventoryFillTest
{
	private static final Color GREEN = new Color(0, 146, 54, 230);
	private static final Color YELLOW = new Color(255, 193, 7, 230);
	private static final Color RED = new Color(199, 26, 26, 230);

	private static final String[] FISH_ACTIONS = {"Eat", null, null, null, "Drop"};
	private static final String[] WIELDABLE = {null, "Wield", null, null, "Drop"};

	@Test
	public void emptyIsGreenHalfIsYellowFullIsRed()
	{
		assertEquals(GREEN, InventoryFill.colour(0.0, GREEN, YELLOW, RED));
		assertEquals(YELLOW, InventoryFill.colour(0.5, GREEN, YELLOW, RED));
		assertEquals(RED, InventoryFill.colour(1.0, GREEN, YELLOW, RED));
	}

	@Test
	public void colourSlidesRatherThanSnapping()
	{
		final Color quarter = InventoryFill.colour(0.25, GREEN, YELLOW, RED);
		// Halfway between green and yellow on every channel.
		assertEquals(128, quarter.getRed());
		assertEquals(170, quarter.getGreen());
		assertEquals(31, quarter.getBlue());
	}

	@Test
	public void colourClampsOutOfRange()
	{
		assertEquals(GREEN, InventoryFill.colour(-1, GREEN, YELLOW, RED));
		assertEquals(RED, InventoryFill.colour(2, GREEN, YELLOW, RED));
	}

	@Test
	public void twentyEightFilledIsFull()
	{
		assertEquals(1.0, InventoryFill.fraction(28, 0), 0);
		assertEquals(0.0, InventoryFill.fraction(0, 28), 0);
		assertEquals(0.5, InventoryFill.fraction(14, 14), 1e-9);
	}

	@Test
	public void labelCountsFreeSlotsOutOfTwentyEight()
	{
		assertEquals("Inv 28/28 free", InventoryFill.label(28));
		assertEquals("Inv 26/28 free", InventoryFill.label(26));
		assertEquals("Inv 0/28 free", InventoryFill.label(0));
	}

	@Test
	public void stackablesHoldASlotSoTheyAreNotFree()
	{
		// Rod, a feather stack and 20 trout: 6 slots free, and the bar reads
		// 20 of the 26 slots the trip can fill.
		assertEquals("Inv 6/28 free", InventoryFill.label(6));
		assertEquals(20.0 / 26, InventoryFill.fraction(20, 6), 1e-9);
		// Rod, feathers and 26 trout: nothing free, full.
		assertEquals("Inv 0/28 free", InventoryFill.label(0));
		assertEquals(1.0, InventoryFill.fraction(26, 0), 0);
	}

	@Test
	public void unknownInventoryHasNoLabel()
	{
		assertEquals(null, InventoryFill.label(-1));
	}

	@Test
	public void fixedSlotsShrinkTheCapacityInsteadOfFilling()
	{
		// Harpoon and feathers held: 26 free slots, so 13 fish is half.
		assertEquals(0.5, InventoryFill.fraction(13, 13), 1e-9);
		// Harpoon and 27 fish: no slot left, full red.
		assertEquals(1.0, InventoryFill.fraction(27, 0), 0);
		// An inventory of nothing but tools has no room either.
		assertEquals(1.0, InventoryFill.fraction(0, 0), 0);
	}

	@Test
	public void stackablesToolsAndEquipmentDoNotFill()
	{
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(true, "Feather", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(true, "Coin pouch", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(true, "Raw shark", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Small fishing net", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Lobster pot", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Fly fishing rod", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Rune pickaxe", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Bronze axe", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Hammer", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Knife", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Dragon harpoon", WIELDABLE));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Rune scimitar", WIELDABLE));
	}

	@Test
	public void whatYouGatherFills()
	{
		assertEquals(InventoryFill.Slot.FILLING, InventoryFill.classify(false, "Raw shark", FISH_ACTIONS));
		assertEquals(InventoryFill.Slot.FILLING, InventoryFill.classify(false, "Yew logs", null));
		assertEquals(InventoryFill.Slot.FILLING, InventoryFill.classify(false, "Iron ore", null));
		assertEquals(InventoryFill.Slot.FILLING, InventoryFill.classify(false, "Jug of wine", null));
		// "axe" inside a word is not a tool.
		assertEquals(InventoryFill.Slot.FILLING, InventoryFill.classify(false, "Flax", null));
	}
}
