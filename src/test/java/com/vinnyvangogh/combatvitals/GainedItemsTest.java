package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** The first inventory is a baseline; after that, a rising count is the item gained. */
public class GainedItemsTest
{
	private static final int YEW_LOGS = 1515;
	private static final int BIRD_NEST = 5070;

	@Test
	public void theFirstInventoryGainsNothing()
	{
		final GainedItems gained = new GainedItems();
		assertEquals(-1, gained.observe(new int[]{YEW_LOGS, YEW_LOGS}, new int[]{1, 1}));
	}

	@Test
	public void depositingOrDroppingGainsNothing()
	{
		final GainedItems gained = new GainedItems();
		gained.observe(new int[]{YEW_LOGS, YEW_LOGS}, new int[]{1, 1});
		assertEquals(-1, gained.observe(new int[]{YEW_LOGS, -1}, new int[]{1, 0}));
		assertEquals(-1, gained.observe(new int[]{-1, -1}, new int[]{0, 0}));
	}

	@Test
	public void aNewLogIsGained()
	{
		final GainedItems gained = new GainedItems();
		gained.observe(new int[]{YEW_LOGS, -1, -1}, new int[]{1, 0, 0});
		assertEquals(YEW_LOGS, gained.observe(new int[]{YEW_LOGS, YEW_LOGS, -1}, new int[]{1, 1, 0}));
		assertEquals(BIRD_NEST, gained.observe(new int[]{YEW_LOGS, YEW_LOGS, BIRD_NEST}, new int[]{1, 1, 1}));
	}

	@Test
	public void clearMakesTheNextABaseline()
	{
		final GainedItems gained = new GainedItems();
		gained.observe(new int[]{-1}, new int[]{0});
		gained.clear();
		assertEquals(-1, gained.observe(new int[]{YEW_LOGS}, new int[]{1}));
	}
}
