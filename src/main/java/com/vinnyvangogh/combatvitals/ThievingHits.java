package com.vinnyvangogh.combatvitals;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Whether a hit on you while thieving is a fight (Board's in-game test,
 * 2026-11-06). A failed pickpocket stuns you and hits you; that is the price
 * of thieving, and must neither hide the skilling badge ("Hide during
 * combat") nor end it. Anything else on you is a fight as usual: a stall's
 * guard, an aggressive NPC, another player.
 *
 * Pure: the actors around you and how to read them in, a decision out.
 */
final class ThievingHits
{
	private ThievingHits()
	{
	}

	/**
	 * @param onMe          the hitsplat landed on you
	 * @param thieving      you are pickpocketing or at a stall, working or lingering
	 * @param otherAttacker something other than the thieving target is on you ({@link #otherAttacker})
	 * @return whether the hit counts as combat
	 */
	static boolean isFight(boolean onMe, boolean thieving, boolean otherAttacker)
	{
		return !onMe || !thieving || otherAttacker;
	}

	/**
	 * Whether anything but you and the thieving target, that can fight, is
	 * interacting with you. A pet or a banker cannot fight, so it is not
	 * counted; the NPC being pickpocketed turning to you after a stun is not
	 * either.
	 *
	 * @param actors      every NPC and player in the scene
	 * @param target      the NPC being pickpocketed, or null at a stall
	 * @param canFight    whether an actor can fight
	 * @param interacting who an actor is interacting with
	 */
	static <A> boolean otherAttacker(Iterable<? extends A> actors, A self, A target, Predicate<? super A> canFight,
		Function<? super A, ?> interacting)
	{
		for (A actor : actors)
		{
			if (actor != self && actor != target && interacting.apply(actor) == self && canFight.test(actor))
			{
				return true;
			}
		}
		return false;
	}
}
