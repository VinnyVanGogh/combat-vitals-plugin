package com.vinnyvangogh.combatvitals;

import java.util.function.Predicate;
import net.runelite.api.Skill;

/**
 * Keeps the sidebar's Skilling card on the skill you last trained, so it does not turn into "Not skilling" when
 * the work stops and the in-game badge goes away (Board, 2026-11-20). It lets go only when the user clears the card,
 * on logout, or when that skill is no longer one the badge is set to show.
 *
 * <p>Pure: skills in, the held skill out.
 */
final class LiveCardHold
{
	/** Written on the client thread, and by the user's Clear from wherever the setting was ticked. */
	private volatile Skill held;

	/** The skill the badge is up for this tick (working or idle): the card follows it. */
	void seen(Skill skill)
	{
		if (skill != null)
		{
			held = skill;
		}
	}

	/** The skill the card should show, or null for the empty "Not skilling" card. */
	Skill held(Predicate<Skill> shown)
	{
		return held != null && shown.test(held) ? held : null;
	}

	/** The user's Clear, and logout. */
	void clear()
	{
		held = null;
	}
}
