package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * A fish appearing in the inventory, or its stack growing, is a catch; a
 * fish leaving (dropped, banked, cooked) is not.
 */
public class InventoryCatchesTest
{
	@Test
	public void aNewFishIsACatch()
	{
		InventoryCatches inventory = new InventoryCatches();
		assertEquals(-1, inventory.observe(new int[] {ItemID.NET}, new int[] {1}));
		assertEquals(ItemID.RAW_SHRIMP,
			inventory.observe(new int[] {ItemID.NET, ItemID.RAW_SHRIMP}, new int[] {1, 1}));
	}

	@Test
	public void anotherOfTheSameFishIsACatch()
	{
		InventoryCatches inventory = new InventoryCatches();
		inventory.observe(new int[] {ItemID.RAW_SHRIMP}, new int[] {1});
		assertEquals(ItemID.RAW_SHRIMP,
			inventory.observe(new int[] {ItemID.RAW_SHRIMP, ItemID.RAW_SHRIMP}, new int[] {1, 1}));
	}

	@Test
	public void aGrowingStackIsACatch()
	{
		InventoryCatches inventory = new InventoryCatches();
		inventory.observe(new int[] {ItemID.MINNOW}, new int[] {40});
		assertEquals(ItemID.MINNOW, inventory.observe(new int[] {ItemID.MINNOW}, new int[] {55}));
	}

	@Test
	public void losingAFishIsNotACatch()
	{
		InventoryCatches inventory = new InventoryCatches();
		inventory.observe(new int[] {ItemID.RAW_SHRIMP, ItemID.RAW_SARDINE}, new int[] {1, 1});
		assertEquals(-1, inventory.observe(new int[] {ItemID.RAW_SARDINE}, new int[] {1}));
	}

	@Test
	public void nonFishAreIgnored()
	{
		InventoryCatches inventory = new InventoryCatches();
		inventory.observe(new int[] {}, new int[] {});
		assertEquals(-1, inventory.observe(new int[] {ItemID.CASKET, ItemID.COINS}, new int[] {1, 500}));
	}

	@Test
	public void theFirstSnapshotIsNeverACatch()
	{
		// Enabling the plugin with a full inventory of fish must not claim
		// the last one was just caught.
		InventoryCatches inventory = new InventoryCatches();
		assertEquals(-1, inventory.observe(new int[] {ItemID.RAW_SHARK, ItemID.RAW_SHARK}, new int[] {1, 1}));
	}

	@Test
	public void twoFishAtOnceReportsTheRarerHigherLevelOne()
	{
		// Karambwanji nets can land shrimps and karambwanji in one tick; the
		// higher level fish is the one worth showing.
		InventoryCatches inventory = new InventoryCatches();
		inventory.observe(new int[] {}, new int[] {});
		assertEquals(ItemID.TBWT_RAW_KARAMBWANJI,
			inventory.observe(new int[] {ItemID.RAW_SHRIMP, ItemID.TBWT_RAW_KARAMBWANJI}, new int[] {1, 1}));
	}

	@Test
	public void clearForgetsTheSnapshot()
	{
		InventoryCatches inventory = new InventoryCatches();
		inventory.observe(new int[] {}, new int[] {});
		inventory.clear();
		assertEquals(-1, inventory.observe(new int[] {ItemID.RAW_SHRIMP}, new int[] {1}));
	}
}
