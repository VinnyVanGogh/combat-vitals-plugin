package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import net.runelite.api.Skill;

/**
 * Which skill last gained experience, and on which tick. An object badge
 * (the pump) counts a gain in its skill as working, the same as the
 * animation: spam-clicking the pump keeps cutting the animation short while
 * the Strength drops keep coming, and the badge read that as "Not pumping"
 * (Board, 2026-11-15).
 *
 * The first value per skill is a baseline, as everywhere else: the login's
 * burst of experience is never a gain. Pure; client thread only.
 */
final class RecentGain
{
	private final int[] lastXp = new int[Skill.values().length];
	private Skill skill;
	private int tick = -1;

	RecentGain()
	{
		clear();
	}

	void xp(Skill skill, int xp, int tick)
	{
		final int i = skill.ordinal();
		final int before = lastXp[i];
		lastXp[i] = xp;
		if (before >= 0 && xp > before)
		{
			this.skill = skill;
			this.tick = tick;
		}
	}

	/** {@code skill} gained on this tick or the one before (drops land between ticks). */
	boolean gained(Skill skill, int now)
	{
		return skill != null && skill == this.skill && tick >= 0 && now - tick <= 1;
	}

	/** Logged out or hopped: every skill is a baseline again. */
	void clear()
	{
		Arrays.fill(lastXp, -1);
		skill = null;
		tick = -1;
	}
}
