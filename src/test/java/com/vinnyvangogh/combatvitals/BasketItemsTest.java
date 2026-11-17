package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/** What the log basket holds, item by item, and the forestry kit's leaves. */
public class BasketItemsTest
{
	private ContainerContents contents;

	@Before
	public void setUp()
	{
		contents = new ContainerContents();
		final Map<SkillingContainer, Integer> capacities = new EnumMap<>(SkillingContainer.class);
		capacities.put(SkillingContainer.LOG_BASKET, 28);
		contents.carried(Collections.singleton(SkillingContainer.LOG_BASKET), capacities);
	}

	@Test
	public void unknownUntilChecked()
	{
		assertNull(contents.itemsLabel(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void aCheckGivesTheKinds()
	{
		contents.message("The basket contains: 9 x Yew logs, 3 x Oak logs", 10);
		assertEquals(12, contents.contents(SkillingContainer.LOG_BASKET));
		assertEquals("9 Yew logs, 3 Oak logs", contents.itemsLabel(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void logsGatheredWhileOpenAreCountedByKind()
	{
		contents.message("The basket contains: 9 x Yew logs", 10);
		contents.message("You get some yew logs.", 12);
		contents.message("You get some maple logs.", 14);
		assertEquals(11, contents.contents(SkillingContainer.LOG_BASKET));
		assertEquals("10 Yew logs, 1 Maple logs", contents.itemsLabel(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void emptyingClearsTheKinds()
	{
		contents.message("The basket contains: 9 x Yew logs", 10);
		contents.message("You empty your basket into the bank.", 20);
		assertNull(contents.itemsLabel(SkillingContainer.LOG_BASKET));
		contents.message("You get some oak logs.", 22);
		assertEquals("1 Oak logs", contents.itemsLabel(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void aFillMakesTheKindsUnknownAgain()
	{
		contents.message("The basket contains: 9 x Yew logs", 10);
		contents.clicked(SkillingContainer.LOG_BASKET, "Fill", 20);
		contents.inventoryChanged(10, 6, 21);
		assertEquals(13, contents.contents(SkillingContainer.LOG_BASKET));
		assertNull(contents.itemsLabel(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void forestryKitLeavesAreCountedAfterTheBasket()
	{
		contents.message("The basket contains: 2 x Yew logs", 10);
		contents.message("You collect some maple leaves.", 11);
		contents.message("You collect some maple leaves.", 13);
		assertEquals("2 Yew logs; kit 2 Maple leaves", contents.itemsLabel(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void rowsSpellOutTheContents()
	{
		final ContainerRow row = new ContainerRow(SkillingContainer.LOG_BASKET, 12, 28, true)
			.withDetail("9 Yew logs, 3 Oak logs");
		assertEquals("Log basket 16/28 free: 9 Yew logs, 3 Oak logs", row.label(InventoryCount.FREE));
		assertEquals("Log basket 16/28 free", row.withDetail("").label(InventoryCount.FREE));
	}
}
