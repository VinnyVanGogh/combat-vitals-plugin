package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * The inventory side pillar: green to red as it fills, one mark per slot.
 *
 * With a log basket or fish barrel carried open and its contents known, the
 * pillar is one bar out of the container's capacity plus 28: the container
 * fills first, at the bottom in its own colour, then the inventory on top
 * (Board, 2026-11-14). The inventory part counts every used slot, a tool's
 * included, so the bar matches the "27 of 28 free" its hover text gives and
 * each mark is one slot. A closed or unknown container, a coal bag (coal
 * only) or the setting off leaves today's inventory-only bar exactly.
 *
 * Pure: a readout and the settings in, a {@link Pillar} out.
 */
final class InventoryPillar
{
	private InventoryPillar()
	{
	}

	/** @return null with the inventory line off or unread */
	static Pillar of(SkillingReadout skilling, CombatVitalsConfig config, BufferedImage icon)
	{
		final double inventoryFill = skilling.getInventoryFill();
		if (inventoryFill < 0)
		{
			return null;
		}
		final int empty = skilling.getInventoryFree();
		final InventoryCount count = config.skillingInventoryCount();
		final ContainerRow container = config.pillarContainers() ? joined(skilling) : null;
		final Pillar.PillarBuilder pillar = Pillar.builder()
			.icon(icon)
			.idle(skilling.isIdle())
			.tooltip(PillarText.inventoryTooltip(empty, count, skilling.getContainers(), container));
		if (container == null)
		{
			return pillar
				.fraction(inventoryFill)
				.colour(colour(inventoryFill, config))
				.number(PillarText.inventoryNumber(empty, count))
				.divisions(InventoryFill.SLOTS)
				.build();
		}

		final int slots = container.getCapacity() + InventoryFill.SLOTS;
		final int inside = Math.min(container.getContents(), container.getCapacity());
		final int used = inside + InventoryFill.SLOTS - Math.max(0, empty);
		final double fill = (double) used / slots;
		return pillar
			.fraction(fill)
			.colour(colour(fill, config))
			.baseFraction((double) inside / slots)
			.baseColour(config.pillarContainerColour())
			.number(Integer.toString(count == InventoryCount.USED ? used : slots - used))
			.divisions(slots)
			.build();
	}

	/** The first container that joins the inventory on the pillar; null for none. */
	private static ContainerRow joined(SkillingReadout skilling)
	{
		for (ContainerRow row : skilling.getContainers())
		{
			if (row.joinsInventory())
			{
				return row;
			}
		}
		return null;
	}

	private static Color colour(double fill, CombatVitalsConfig config)
	{
		return InventoryFill.colour(fill, config.healthFullColour(), config.healthMidColour(),
			config.healthLowColour());
	}
}
