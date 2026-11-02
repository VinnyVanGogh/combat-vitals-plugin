package com.vinnyvangogh.combatvitals;

import java.time.Duration;
import java.time.Instant;
import net.runelite.api.Hitsplat;

/**
 * Decides whether the player is actually fighting.
 *
 * Interacting with something is not proof of combat. Fishing spots, rocks,
 * bankers, shopkeepers and pets are all NPCs, and clicking any of them fires
 * the same {@code InteractingChanged} as attacking a goblin. Two things are
 * proof: the target can be attacked, or a hitsplat has landed on the player or
 * been dealt by them recently.
 *
 * The opponent readout needs the first. Your own bar needs either, so that
 * being attacked while fishing still shows it without promoting the fishing
 * spot to an opponent.
 *
 * Pure: instants in, booleans out, so it is testable without a client. Not
 * thread safe; the plugin touches it from the client thread only.
 */
final class CombatState
{
	private static final String ATTACK = "Attack";

	/** A target is held, live or lingering after the interaction ended. */
	private boolean engaged;

	private boolean targetAttackable;

	/** When the interaction ended, or null while it is live. */
	private Instant interactionEnded;

	/** The last hitsplat on the player or dealt by them. */
	private Instant lastHit;

	/**
	 * @param actions     the NPC's menu options, {@code NPCComposition#getActions()}
	 * @param combatLevel the NPC's combat level, -1 when it has none
	 * @return whether the NPC can be fought
	 */
	static boolean attackable(String[] actions, int combatLevel)
	{
		if (combatLevel > 0)
		{
			return true;
		}

		if (actions == null)
		{
			return false;
		}

		for (String action : actions)
		{
			if (ATTACK.equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether a hitsplat says the player is fighting.
	 *
	 * The player's own hits land as the "me" types wherever they land. Hits on
	 * the player count whichever way the client typed them, but status
	 * hitsplats on the player (poison, venom, disease, healing, prayer drain)
	 * are not combat and must not pop the bar while skilling.
	 *
	 * @param onMe     the hitsplat landed on the local player
	 * @param hitsplat the hitsplat
	 */
	static boolean evidence(boolean onMe, Hitsplat hitsplat)
	{
		return hitsplat.isMine() || (onMe && hitsplat.isOthers());
	}

	/** The player started interacting with something. */
	void engage(boolean attackable)
	{
		engaged = true;
		targetAttackable = attackable;
		interactionEnded = null;
	}

	/** The player stopped interacting. The target lingers until {@link #expire}. */
	void disengage(Instant now)
	{
		if (engaged && interactionEnded == null)
		{
			interactionEnded = now;
		}
	}

	/**
	 * A hitsplat that {@link #evidence} accepted.
	 *
	 * @param onTarget the player's own hit landed on the held target, which
	 *                 proves it attackable whatever its composition said
	 */
	void hit(boolean onTarget, Instant now)
	{
		lastHit = now;
		if (onTarget && engaged)
		{
			targetAttackable = true;
		}
	}

	/**
	 * Retires the target once the interaction has been over for the timeout.
	 *
	 * @param stillInteractingWithTarget the game still has the player on this
	 *                                   target. Any other interaction, a
	 *                                   fishing spot included, does not count.
	 * @return true when the caller should drop its reference to the target
	 */
	boolean expire(Instant now, Duration timeout, boolean stillInteractingWithTarget)
	{
		if (!engaged || interactionEnded == null || stillInteractingWithTarget)
		{
			return false;
		}

		if (Duration.between(interactionEnded, now).compareTo(timeout) > 0)
		{
			engaged = false;
			targetAttackable = false;
			interactionEnded = null;
			return true;
		}
		return false;
	}

	/** The held target is something that can be fought, so show its readout. */
	boolean showTarget()
	{
		return engaged && targetAttackable;
	}

	/** Fighting an attackable target, or hit within the timeout. Gates your own bar. */
	boolean inCombat(Instant now, Duration timeout)
	{
		if (showTarget())
		{
			return true;
		}
		return lastHit != null && Duration.between(lastHit, now).compareTo(timeout) <= 0;
	}

	void clear()
	{
		engaged = false;
		targetAttackable = false;
		interactionEnded = null;
		lastHit = null;
	}
}
