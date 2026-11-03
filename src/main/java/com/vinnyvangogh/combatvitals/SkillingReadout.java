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

	/**
	 * What the clicked method can land at the boosted level, the last catch
	 * first. Empty for an unknown spot or method, and for every skill but
	 * fishing: the badge then shows the skill and level only.
	 */
	private final List<FishingCatches.Fish> fish;

	/** Item id of the last fish landed, or -1; when it leads {@link #fish} it is drawn highlighted. */
	private final int lastCatch;

	/** Wall clock of the tick this was built on, for counting down between ticks. */
	private final long tickMillis;

	SkillingReadout(Skill skill, int real, int boosted, int ticksUntilDrop,
		int cycleLength, boolean timerConfirmed, int specPercent, String source,
		SkillingState.Warning warning, List<FishingCatches.Fish> fish, int lastCatch, long tickMillis)
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
		this.fish = fish;
		this.lastCatch = lastCatch;
		this.tickMillis = tickMillis;
	}

	boolean isBoosted()
	{
		return boosted > real;
	}

	/** The last catch is in the list, so the first icon is the one to highlight. */
	boolean isLastCatchShown()
	{
		return !fish.isEmpty() && fish.get(0).getItemId() == lastCatch;
	}
}
