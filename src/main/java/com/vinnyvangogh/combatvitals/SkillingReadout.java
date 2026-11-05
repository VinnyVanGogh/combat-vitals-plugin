package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable snapshot of everything the skilling badge needs for one tick.
 *
 * Every row's text that can be built once per tick is built here, by the
 * plugin, so the overlays only lay it out. The header is the exception: its
 * boost timer counts down between ticks.
 *
 * The badge is drawn over the local player unless the position setting
 * asks for the spot; the spot fields say where that is.
 */
@Getter
@Builder
final class SkillingReadout
{
	private final Skill skill;
	private final int real;
	private final int boosted;

	/** The skill's colour, the user's override or the palette ({@link SkillColours}). */
	private final Color colour;

	/** The activity has stopped and the header says "Not fishing" ({@link IdleBadge}). */
	private final boolean idle;

	/** The top row is on. */
	private final boolean header;

	/** Ticks until the boost drops by one, or -1 while the cycle phase is unknown. */
	@Builder.Default
	private final int ticksUntilDrop = -1;

	/** Length of the drain cycle in force, for drawing the bar. */
	private final int cycleLength;

	/** The timer is anchored on an observed drop rather than on login. */
	private final boolean timerConfirmed;

	/** Remaining special attack energy, or -1 when no spec tool is wielded. */
	@Builder.Default
	private final int specPercent = -1;

	private final String source;

	@Builder.Default
	private final SkillingState.Warning warning = SkillingState.Warning.NONE;

	/**
	 * "Needs: Fishing rod + bait" after clicking a fishing option without
	 * what it takes ({@link FishingGear}), or null. Turns the header red.
	 */
	private final String gearText;

	/** Progress through the current level, from the skill's XP. */
	private final XpProgress xp;

	/** The XP row's text, or null with the row off. */
	private final String xpText;

	/** The session row's columns ({@link SessionRow#cells}), empty with it off. */
	@Builder.Default
	private final List<SessionRow.Cell> sessionCells = Collections.emptyList();

	/**
	 * Every fish the clicked method can give, the catchable ones first
	 * ({@link FishingCatches#options}). Empty for an unknown spot or method,
	 * with the catch icons off, and for every skill but fishing.
	 */
	@Builder.Default
	private final List<FishingCatches.Catch> catches = Collections.emptyList();

	/** Wall clock of the tick this was built on, for counting down between ticks. */
	private final long tickMillis;

	/** How full the inventory is, 0 to 1 ({@link InventoryFill}), or -1 when the line is off or unknown. */
	@Builder.Default
	private final double inventoryFill = -1;

	/** "Inv 26/28 free", or null when the line is off or the inventory unread. */
	private final String inventoryLabel;

	/** Empty slots, for the inventory pillar's number, or -1 when the line is off or unread. */
	@Builder.Default
	private final int inventoryFree = -1;

	/** The fishing spot or other NPC being worked, or null. */
	private final NPC spotNpc;

	/** The south-west tile of the tree, rock or stall being worked, or null. */
	private final WorldPoint spotTile;

	/** The object's larger side in tiles, for centring over it. */
	private final int spotSize;

	boolean isBoosted()
	{
		return boosted > real;
	}

	/** The inventory line has something to show: the fill, the catch icons, or both. */
	boolean hasInventoryLine()
	{
		return inventoryLabel != null || !catches.isEmpty();
	}
}
