package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * One carried container as the badge shows it: "Log basket 12/28 free", or
 * "Log basket ?/28 (check the basket once)" until its contents are known.
 * Also the inventory line with container space counted in,
 * "Inv 27/28 free (+16 basket)".
 *
 * Pure: counts in, text out.
 */
@Value
class ContainerRow
{
	SkillingContainer kind;
	/** Items inside, or -1 while unknown. */
	int contents;
	int capacity;

	/** Free space, or -1 while the contents are unknown. */
	int free()
	{
		return contents < 0 ? -1 : Math.max(0, capacity - contents);
	}

	/** 0 to 1, as the inventory bar fills; -1 while unknown. */
	double fill()
	{
		if (contents < 0)
		{
			return -1;
		}
		return capacity <= 0 ? 1.0 : Math.min(1.0, (double) contents / capacity);
	}

	/** "Log basket 12/28 free", "Log basket 16/28 used", "Log basket ?/28 (check the basket once)". */
	String label(InventoryCount count)
	{
		final String name = kind.getLabel() + " ";
		if (contents < 0)
		{
			return name + "?/" + capacity + " (check the " + kind.getNoun() + " once)";
		}
		return count == InventoryCount.USED
			? name + Math.min(contents, capacity) + "/" + capacity + " used"
			: name + free() + "/" + capacity + " free";
	}

	/**
	 * The containers carried that take what the skill gathers, in a fixed
	 * order; none for any other skill.
	 *
	 * @param capacities each container carried and its capacity
	 * @param contents   items inside each, -1 while unknown
	 */
	static List<ContainerRow> rows(Skill skill, Map<SkillingContainer, Integer> capacities,
		ToIntFunction<SkillingContainer> contents)
	{
		if (skill == null || capacities.isEmpty())
		{
			return Collections.emptyList();
		}
		final List<ContainerRow> rows = new ArrayList<>();
		for (SkillingContainer kind : SkillingContainer.values())
		{
			final Integer capacity = capacities.get(kind);
			if (capacity != null && kind.getSkill() == skill)
			{
				rows.add(new ContainerRow(kind, contents.applyAsInt(kind), capacity));
			}
		}
		return rows;
	}

	/**
	 * The inventory line. With the option off, exactly {@link InventoryFill#label};
	 * on, each container's known free space that counts as inventory after
	 * it: "Inv 27/28 free (+16 basket)", "Inv 1/28 used (+16 basket free)".
	 */
	static String inventoryLabel(int empty, InventoryCount count, List<ContainerRow> rows, boolean countContainers)
	{
		final String base = InventoryFill.label(empty, count);
		if (base == null || !countContainers)
		{
			return base;
		}
		final StringBuilder extra = new StringBuilder();
		for (ContainerRow row : rows)
		{
			if (row.kind.isCountsAsInventory() && row.free() > 0)
			{
				extra.append(extra.length() == 0 ? " (" : ", ")
					.append('+').append(row.free()).append(' ').append(row.kind.getNoun());
			}
		}
		if (extra.length() == 0)
		{
			return base;
		}
		return base + extra + (count == InventoryCount.USED ? " free)" : ")");
	}

	/**
	 * Empty slots for the inventory-full alert: the inventory's alone with the
	 * option off; with it on, plus every known free space that counts. An
	 * unknown container adds nothing, so it never hides a full inventory.
	 */
	static int combinedEmpty(int empty, List<ContainerRow> rows, boolean countContainers)
	{
		if (empty < 0 || !countContainers)
		{
			return empty;
		}
		int total = empty;
		for (ContainerRow row : rows)
		{
			if (row.kind.isCountsAsInventory() && row.free() > 0)
			{
				total += row.free();
			}
		}
		return total;
	}
}
