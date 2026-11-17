package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import org.junit.Test;

public class ContainerItemsTest
{
	@Test
	public void parsesEachItemOfABasketCheck()
	{
		final Map<String, Integer> items = ContainerItems.parse(" 14 x Yew logs, 3 x Oak logs");
		assertEquals(14, (int) items.get("Yew logs"));
		assertEquals(3, (int) items.get("Oak logs"));
		assertEquals(17, ContainerItems.total(items));
	}

	@Test
	public void parsesForestryKitLeaves()
	{
		final Map<String, Integer> items = ContainerItems.parse("120 x Maple leaves, 8 x Oak leaves");
		assertEquals(120, (int) items.get("Maple leaves"));
		assertEquals(128, ContainerItems.total(items));
	}

	@Test
	public void emptyOrGarbageIsEmpty()
	{
		assertTrue(ContainerItems.parse("").isEmpty());
		assertTrue(ContainerItems.parse(null).isEmpty());
		assertTrue(ContainerItems.parse("nothing here").isEmpty());
	}

	@Test
	public void addingOneKeepsTheKind()
	{
		final Map<String, Integer> items = ContainerItems.parse("1 x Yew logs");
		ContainerItems.add(items, "Yew logs");
		ContainerItems.add(items, "Maple logs");
		assertEquals(2, (int) items.get("Yew logs"));
		assertEquals(1, (int) items.get("Maple logs"));
	}

	@Test
	public void labelListsBiggestFirstAndCapsTheList()
	{
		final Map<String, Integer> items = ContainerItems.parse("3 x Oak logs, 14 x Yew logs, 1 x Logs, 2 x Willow logs");
		assertEquals("14 Yew logs, 3 Oak logs, 2 Willow logs, +1 more", ContainerItems.label(items, 3));
		assertEquals("", ContainerItems.label(ContainerItems.parse(""), 3));
	}

	@Test
	public void gatheredItemNameFromAChatLine()
	{
		assertEquals("Yew logs", ContainerItems.gathered("You get some yew logs."));
		assertEquals("Logs", ContainerItems.gathered("You get some logs."));
		assertEquals(null, ContainerItems.gathered("You get some coal."));
	}

	@Test
	public void forestryKitLeafLine()
	{
		assertEquals("Maple leaves", ContainerItems.leaf("You collect some maple leaves."));
		assertEquals(null, ContainerItems.leaf("You get some yew logs."));
	}
}
