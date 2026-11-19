package com.vinnyvangogh.combatvitals;

/**
 * What a fight does to the skilling badge (Board, 2026-11-14).
 *
 * Over your head the badge shares the space with the health bars, so a
 * target's bar replaces it, Hide during combat hides it, and a fight ends its
 * "Not fishing". Moved off your head (fixed or over the spot) it shares no
 * space with them: a fight never hides it nor ends its idle, it only says so,
 * "in combat" once stopped or "under attack" while still at it, and the idle
 * runs out on its own timer.
 *
 * Pure: flags in, decisions out.
 */
final class BadgeCombat
{
	private BadgeCombat()
	{
	}

	/**
	 * @param fighting     a fight or a hitsplat is in progress
	 * @param overHead     the badge is drawn over your head
	 * @param hideInCombat the Hide during combat setting
	 * @param targetShown  a target's health bar is up
	 * @return whether the badge is hidden because of combat
	 */
	static boolean hides(boolean fighting, boolean overHead, boolean hideInCombat, boolean targetShown)
	{
		return overHead && (targetShown || (fighting && hideInCombat));
	}

	/** Whether the fight ends the badge's "Not fishing": anywhere, so it never reads "Not hunting | in combat". */
	static boolean endsIdle(boolean fighting, boolean overHead)
	{
		return fighting;
	}

	/** "in combat" on a stopped badge, "under attack" on a working one, or null out of combat. */
	static String tag(boolean fighting, boolean idle)
	{
		if (!fighting)
		{
			return null;
		}
		return idle ? "in combat" : "under attack";
	}
}
