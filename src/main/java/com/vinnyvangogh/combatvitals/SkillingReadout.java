package com.vinnyvangogh.combatvitals;

import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Skill;

/** Immutable snapshot of everything the skilling badge needs for one tick. */
@Getter
final class SkillingReadout
{
	private final Actor actor;
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

	/** Wall clock of the tick this was built on, for counting down between ticks. */
	private final long tickMillis;

	SkillingReadout(Actor actor, Skill skill, int real, int boosted, int ticksUntilDrop,
		int cycleLength, boolean timerConfirmed, int specPercent, String source,
		SkillingState.Warning warning, long tickMillis)
	{
		this.actor = actor;
		this.skill = skill;
		this.real = real;
		this.boosted = boosted;
		this.ticksUntilDrop = ticksUntilDrop;
		this.cycleLength = cycleLength;
		this.timerConfirmed = timerConfirmed;
		this.specPercent = specPercent;
		this.source = source;
		this.warning = warning;
		this.tickMillis = tickMillis;
	}

	boolean isBoosted()
	{
		return boosted > real;
	}
}
