package com.vinnyvangogh.combatvitals;

import java.util.List;
import lombok.Getter;
import net.runelite.api.Skill;

/**
 * Immutable snapshot of everything the skilling badge needs for one tick.
 *
 * Carries no actor on purpose: the badge is always drawn over the local
 * player, at the same anchor as your own health bar, never over the spot or
 * NPC being worked. The overlay takes the player from the client.
 */
@Getter
final class SkillingReadout
{
	private final Skill skill;
	private final int real;
	private final int boosted;

	/** Ticks until the boost drops by one, or -1 while the cycle phase is unknown. */
	private final int ticksUntilDrop;

	/** Length of the drain cycle in force, for drawing the bar. */
	private final int cycleLength;

	/** The timer is anchored on an observed drop rather than on login. */
	private final boolean timerConfirmed;

	/** Remaining special attack energy, or -1 when no spec tool is wielded. */
	private final int specPercent;

	private final String source;
	private final SkillingState.Warning warning;

	/** Progress through the current level, from the skill's XP. */
	private final XpProgress xp;

	/**
	 * Every fish the clicked method can give, the catchable ones first
	 * ({@link FishingCatches#options}). Empty for an unknown spot or method,
	 * with the catch icons off, and for every skill but fishing.
	 */
	private final List<FishingCatches.Catch> catches;

	/** Wall clock of the tick this was built on, for counting down between ticks. */
	private final long tickMillis;

	/** How full the inventory is, 0 to 1 ({@link InventoryFill}), or -1 when the line is off or unknown. */
	private final double inventoryFill;

	/** Free inventory slots, or -1 when the line is off or unknown. */
	private final int inventoryFree;

	SkillingReadout(Skill skill, int real, int boosted, int ticksUntilDrop,
		int cycleLength, boolean timerConfirmed, int specPercent, String source,
		SkillingState.Warning warning, XpProgress xp, List<FishingCatches.Catch> catches, long tickMillis,
		double inventoryFill, int inventoryFree)
	{
		this.skill = skill;
		this.real = real;
		this.boosted = boosted;
		this.ticksUntilDrop = ticksUntilDrop;
		this.cycleLength = cycleLength;
		this.timerConfirmed = timerConfirmed;
		this.specPercent = specPercent;
		this.source = source;
		this.warning = warning;
		this.xp = xp;
		this.catches = catches;
		this.tickMillis = tickMillis;
		this.inventoryFill = inventoryFill;
		this.inventoryFree = inventoryFree;
	}

	boolean isBoosted()
	{
		return boosted > real;
	}

	/** The inventory line has something to show: the fill, the catch icons, or both. */
	boolean hasInventoryLine()
	{
		return inventoryFree >= 0 || !catches.isEmpty();
	}
}
