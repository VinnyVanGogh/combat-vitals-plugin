package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * How full the inventory is, for the bar under the skilling badge.
 *
 * Only slots that gathering fills count as filled. A stackable (feathers,
 * bait, coins, coin pouches, noted items), a tool (net, rod, pot, axe,
 * pickaxe, hammer...) or a piece of equipment sits in its slot for the whole
 * trip, so it is taken out of the capacity rather than counted as fill: a
 * harpoon and 27 fish is a full bar, and so is any inventory with no free
 * slot left.
 *
 * Pure: slot classes in, a fraction and a colour out.
 */
final class InventoryFill
{
	static final int SLOTS = 28;

	enum Slot
	{
		EMPTY,
		/** Something the trip is filling the inventory with. */
		FILLING,
		/** A stackable, tool or piece of equipment: holds its slot, fills nothing. */
		FIXED
	}

	/**
	 * Tools that live in the inventory while gathering or making. Names, not
	 * ids, so every metal and variant is covered. Source: OSRS wiki,
	 * "Fishing", "Woodcutting", "Mining", "Crafting", "Smithing".
	 */
	private static final Pattern TOOL = Pattern.compile(
		"^(?:small fishing net|big fishing net|fishing rod|fly fishing rod|barbarian rod|oily fishing rod"
			+ "|pearl (?:fishing|fly fishing|barbarian) rod|lobster pot|karambwan vessel|harpoon"
			+ "|.*\\b(?:axe|pickaxe|harpoon)(?: \\(.*\\))?"
			+ "|tinderbox|hammer|imcando hammer|chisel|knife|needle|glassblowing pipe|saw|crystal saw"
			+ "|pestle and mortar|bucket|ammo mould|.*\\bmould|noose wand|butterfly net|.*\\bbutterfly net"
			+ "|rope|bird snare|box trap|rabbit snare|teasing stick)$");

	private InventoryFill()
	{
	}

	/**
	 * @param stackable the item stacks, noted items included
	 * @param name      the item's name
	 * @param actions   the item's inventory options, {@code ItemComposition#getInventoryActions()}
	 */
	static Slot classify(boolean stackable, String name, String[] actions)
	{
		if (stackable)
		{
			return Slot.FIXED;
		}
		if (name != null && TOOL.matcher(name.toLowerCase(Locale.ROOT)).matches())
		{
			return Slot.FIXED;
		}
		if (actions != null)
		{
			for (String action : actions)
			{
				if ("Wield".equalsIgnoreCase(action) || "Wear".equalsIgnoreCase(action)
					|| "Equip".equalsIgnoreCase(action))
				{
					return Slot.FIXED;
				}
			}
		}
		return Slot.FILLING;
	}

	/**
	 * @return 0 for nothing gathered yet, 1 for no free slot left; the share of
	 *         the free capacity (filled plus empty slots) that is filled
	 */
	static double fraction(int filling, int empty)
	{
		if (empty <= 0)
		{
			return 1.0;
		}
		final int capacity = Math.max(0, filling) + empty;
		return (double) Math.max(0, filling) / capacity;
	}

	/**
	 * The inventory line's label: free slots out of the whole inventory. A
	 * stack or a tool holds a slot, so it is not free.
	 *
	 * @param empty empty slots, or -1 when the inventory has not been read
	 * @return e.g. "Inv 26/28 free", or null when unknown
	 */
	static String label(int empty)
	{
		return label(empty, InventoryCount.FREE);
	}

	/** "Inv 26/28 free", or the same inventory counted the other way, "Inv 2/28 used". */
	static String label(int empty, InventoryCount count)
	{
		if (empty < 0)
		{
			return null;
		}
		return count == InventoryCount.USED
			? "Inv " + (SLOTS - empty) + "/" + SLOTS + " used"
			: "Inv " + empty + "/" + SLOTS + " free";
	}

	/**
	 * Green while empty, through yellow at half, to red when full: the health
	 * ramp run the other way, since a full inventory is the thing to act on.
	 */
	static Color colour(double fill, Color empty, Color half, Color full)
	{
		return HealthReadout.colour(1.0 - fill, empty, half, full);
	}
}
