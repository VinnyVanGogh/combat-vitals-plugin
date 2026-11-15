package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * The inventory pillar with an open log basket or fish barrel carried: one
 * bar out of the container's capacity plus 28, the container filling first
 * in its own colour and the inventory on top (Board, 2026-11-14). Anything
 * unknown, closed or switched off is today's inventory-only bar exactly.
 */
public class InventoryPillarTest
{
	private static final Color VIOLET = new Color(1, 2, 3);

	private static final CombatVitalsConfig ON = new CombatVitalsConfig()
	{
		@Override
		public Color pillarContainerColour()
		{
			return VIOLET;
		}
	};

	private static final CombatVitalsConfig OFF = new CombatVitalsConfig()
	{
		@Override
		public boolean pillarContainers()
		{
			return false;
		}
	};

	private static ContainerRow basket(int contents, boolean open)
	{
		return new ContainerRow(SkillingContainer.LOG_BASKET, contents, SkillingContainer.BASKET_CAPACITY, open);
	}

	/** {@code empty} free slots, nothing fixed, so the inventory-only fill is (28 - empty) / 28. */
	private static SkillingReadout readout(int empty, List<ContainerRow> rows)
	{
		return SkillingReadout.builder()
			.inventoryFill(InventoryFill.fraction(InventoryFill.SLOTS - empty, empty))
			.inventoryFree(empty)
			.containers(rows)
			.containerCount(InventoryCount.FREE)
			.build();
	}

	private static Pillar pillar(int empty, List<ContainerRow> rows, CombatVitalsConfig config)
	{
		return InventoryPillar.of(readout(empty, rows), config, null);
	}

	@Test
	public void combinedFillIsOutOfCapacityPlus28()
	{
		final int[] baskets = {0, 14, 28};
		final int[] frees = {1, 27, 28};
		for (int inBasket : baskets)
		{
			for (int empty : frees)
			{
				final Pillar p = pillar(empty, Collections.singletonList(basket(inBasket, true)), ON);
				final double total = SkillingContainer.BASKET_CAPACITY + InventoryFill.SLOTS;
				final String at = "basket " + inBasket + ", " + empty + " free";
				assertEquals(at, inBasket / total, p.getBaseFraction(), 1e-9);
				assertEquals(at, (inBasket + InventoryFill.SLOTS - empty) / total, p.getFraction(), 1e-9);
				assertEquals(at, 56, p.getDivisions());
				assertEquals(at, Integer.toString(SkillingContainer.BASKET_CAPACITY - inBasket + empty), p.getNumber());
			}
		}
	}

	@Test
	public void theBoardsCaseReads55Free()
	{
		final Pillar p = pillar(27, Collections.singletonList(basket(0, true)), ON);
		assertEquals("55", p.getNumber());
		assertEquals(0, p.getBaseFraction(), 0);
		assertEquals(1 / 56.0, p.getFraction(), 1e-9);
	}

	@Test
	public void usedCountsTheContainerAndTheInventory()
	{
		final SkillingReadout used = readout(27, Collections.singletonList(basket(16, true)));
		final CombatVitalsConfig usedConfig = new CombatVitalsConfig()
		{
			@Override
			public InventoryCount skillingInventoryCount()
			{
				return InventoryCount.USED;
			}
		};
		assertEquals("17", InventoryPillar.of(used, usedConfig, null).getNumber());
	}

	@Test
	public void theContainerSitsBelowInItsOwnColourAndTheInventoryOnTop()
	{
		final Pillar p = pillar(20, Collections.singletonList(basket(14, true)), ON);
		assertEquals(VIOLET, p.getBaseColour());
		assertTrue("the inventory fills above the container", p.getFraction() > p.getBaseFraction());
		assertEquals(InventoryFill.colour(p.getFraction(), ON.healthFullColour(), ON.healthMidColour(),
			ON.healthLowColour()), p.getColour());
	}

	@Test
	public void unknownContentsFallBackToTodaysBarExactly()
	{
		final List<ContainerRow> rows = Collections.singletonList(basket(-1, true));
		assertTodaysBar(pillar(27, rows, ON), 27, rows);
	}

	@Test
	public void aClosedBasketIsIgnored()
	{
		final List<ContainerRow> rows = Collections.singletonList(basket(14, false));
		assertTodaysBar(pillar(27, rows, ON), 27, rows);
	}

	@Test
	public void settingOffIsTodaysBar()
	{
		final List<ContainerRow> rows = Collections.singletonList(basket(14, true));
		assertTodaysBar(pillar(27, rows, OFF), 27, rows);
	}

	@Test
	public void aCoalBagNeverMerges()
	{
		// Takes coal only, so its space is not free inventory at any other rock.
		final List<ContainerRow> rows = Collections.singletonList(
			new ContainerRow(SkillingContainer.COAL_BAG, 3, 27, true));
		assertTodaysBar(pillar(10, rows, ON), 10, rows);
	}

	@Test
	public void noInventoryLineIsNoPillar()
	{
		assertNull(InventoryPillar.of(SkillingReadout.builder().build(), ON, null));
	}

	@Test
	public void tooltipListsTheOpenContainerFirst()
	{
		final ContainerRow open = basket(0, true);
		final ContainerRow coal = new ContainerRow(SkillingContainer.COAL_BAG, 3, 27, false);
		final Pillar p = pillar(27, Arrays.asList(coal, open), ON);
		assertEquals("Log basket 28/28 free</br>Inventory: 27 of 28 free</br>Coal bag 24/27 free", p.getTooltip());
	}

	@Test
	public void tooltipKeepsTodaysOrderWhenNotMerged()
	{
		final Pillar p = pillar(27, Collections.singletonList(basket(0, false)), ON);
		assertEquals("Inventory: 27 of 28 free</br>Log basket 28/28 free", p.getTooltip());
	}

	@Test
	public void theDefaultContainerColourIsNotOnTheInventoryRamp()
	{
		final CombatVitalsConfig defaults = new CombatVitalsConfig()
		{
		};
		final Color c = defaults.pillarContainerColour();
		for (Color ramp : new Color[] {defaults.healthFullColour(), defaults.healthMidColour(),
			defaults.healthLowColour()})
		{
			final int distance = Math.abs(c.getRed() - ramp.getRed()) + Math.abs(c.getGreen() - ramp.getGreen())
				+ Math.abs(c.getBlue() - ramp.getBlue());
			assertTrue(c + " too close to " + ramp, distance > 150);
		}
		assertTrue(defaults.pillarContainers());
	}

	/** Exactly what the pillar was before containers joined it. */
	private static void assertTodaysBar(Pillar p, int empty, List<ContainerRow> rows)
	{
		final double fill = InventoryFill.fraction(InventoryFill.SLOTS - empty, empty);
		assertEquals(fill, p.getFraction(), 0);
		assertEquals(0, p.getBaseFraction(), 0);
		assertNull(p.getBaseColour());
		assertEquals(InventoryFill.SLOTS, p.getDivisions());
		assertEquals(Integer.toString(empty), p.getNumber());
		assertEquals(InventoryFill.colour(fill, ON.healthFullColour(), ON.healthMidColour(), ON.healthLowColour()),
			p.getColour());
		assertEquals(PillarText.inventoryTooltip(empty, InventoryCount.FREE, rows), p.getTooltip());
	}
}
