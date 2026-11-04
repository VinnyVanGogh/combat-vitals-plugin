package com.vinnyvangogh.combatvitals;

import net.runelite.api.Skill;

/**
 * Keeps the skilling badge up after you stop, as "Not fishing", instead of
 * letting it vanish (Board, 2026-11-04).
 *
 * Idle starts the tick the badge would otherwise have gone: the activity's
 * own linger ({@link SkillingState#expire}) has run out, or it was released
 * by clicking something else. It ends when another activity starts, when a
 * fight starts, or after the configured seconds.
 *
 * Pure: skills and wall-clock milliseconds in, the idle skill out.
 */
final class IdleBadge
{
	private Skill lastSkill;

	/** When the badge went idle, or -1 while working or with nothing to show. */
	private long idleSince = -1;

	/**
	 * Once per game tick.
	 *
	 * @param active the skill the badge is up for now, or null
	 * @param combat a fight or a hitsplat is in progress
	 */
	void tick(Skill active, boolean combat, long nowMillis)
	{
		if (active != null)
		{
			lastSkill = active;
			idleSince = -1;
			return;
		}

		if (combat)
		{
			clear();
			return;
		}

		if (lastSkill != null && idleSince < 0)
		{
			idleSince = nowMillis;
		}
	}

	/** The skill to show as "Not ..." now, or null when the idle badge should not be up. */
	Skill idleSkill(IdleBadgeMode mode, int seconds, long nowMillis)
	{
		if (lastSkill == null || idleSince < 0 || mode == IdleBadgeMode.OFF)
		{
			return null;
		}
		if (mode == IdleBadgeMode.TIMED && nowMillis - idleSince >= seconds * 1000L)
		{
			return null;
		}
		return lastSkill;
	}

	void clear()
	{
		lastSkill = null;
		idleSince = -1;
	}
}
