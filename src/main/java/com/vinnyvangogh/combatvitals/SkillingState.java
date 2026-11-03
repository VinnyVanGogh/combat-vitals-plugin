package com.vinnyvangogh.combatvitals;

import java.time.Duration;
import java.time.Instant;
import net.runelite.api.Skill;

/**
 * Decides whether the skilling badge is up, and what it can honestly say
 * about where a boost came from and what just happened to it.
 *
 * Engagement mirrors {@link CombatState}: working a skilling NPC or object
 * raises the badge, and it lingers for the timeout after the work stops
 * unless the game still has the player on it.
 *
 * The boost source is attributed from what was seen. A rise that follows a
 * Drink or Eat within a few ticks is likely that item, and labelled
 * "likely"; one that follows a special attack from a recognised tool is
 * that tool; anything else has no source.
 * Attribution is kept per skill, because the usual order is drink, then
 * click the spot, and the badge is not up yet when the boost lands.
 *
 * Pure: ticks and instants in, decisions out, no client. Not thread safe;
 * the plugin touches it from the client thread only.
 */
final class SkillingState
{
	/** How long after a Drink, Eat or special attack a rise is credited to it. */
	static final int ATTRIBUTION_TICKS = 3;

	/** How long the on-badge cue stays after a drop (3 s) and after expiry (about 5 s). */
	static final int DROP_CUE_TICKS = 5;
	static final int EXPIRY_CUE_TICKS = 8;

	enum Warning
	{
		NONE,
		DROPPED,
		EXPIRED
	}

	private boolean engaged;
	private Skill skill;
	private Instant interactionEnded;

	/** The menu option of the last NPC click, until an engagement takes it. */
	private String pendingClick;

	/** The option the current engagement was started with: Net, Bait, Harpoon... */
	private String method;

	/** Item id of the last fish landed, or -1. */
	private int lastCatch = -1;

	private final String[] sourceBySkill = new String[Skill.values().length];

	private String consumedItem;
	private int consumedTick = -1;

	private String specTool;
	private Skill specSkill;
	private int specTick = -1;

	private Skill warningSkill;
	private Warning lastWarning = Warning.NONE;
	private int warningTick = -1;

	/**
	 * The player clicked a menu option on an NPC. Remembered until the
	 * interaction it starts arrives, a tick later, so the badge knows which
	 * method the spot is being worked with.
	 */
	void clicked(String option)
	{
		pendingClick = option;
	}

	/**
	 * The player started interacting with an NPC that trains {@code skill}.
	 * A click just before this names the method; without one (the spot moved
	 * and the game re-engaged it) the method from the last click stands.
	 */
	void engage(Skill skill)
	{
		engaged = true;
		this.skill = skill;
		interactionEnded = null;
		if (pendingClick != null)
		{
			method = pendingClick;
			pendingClick = null;
		}
	}

	/** The player moved on to something that is not a skilling NPC: no linger. */
	void release()
	{
		engaged = false;
		skill = null;
		interactionEnded = null;
		pendingClick = null;
		method = null;
		lastCatch = -1;
	}

	/** The option the current engagement was clicked with, or null when none was seen. */
	String method()
	{
		return method;
	}

	/** A fish landed: from the catch message or the inventory. */
	void caught(int itemId)
	{
		lastCatch = itemId;
	}

	/** Item id of the last fish landed this engagement, or -1. */
	int lastCatch()
	{
		return lastCatch;
	}

	/** The player stopped interacting. The badge lingers until {@link #expire}. */
	void disengage(Instant now)
	{
		if (engaged && interactionEnded == null)
		{
			interactionEnded = now;
		}
	}

	/**
	 * Retires the badge once the interaction has been over for the timeout.
	 *
	 * @return true when the caller should drop its reference to the NPC
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
			skill = null;
			interactionEnded = null;
			method = null;
			lastCatch = -1;
			return true;
		}
		return false;
	}

	/** The badge should be up, and for this skill. Null when it should not. */
	Skill skill()
	{
		return engaged ? skill : null;
	}

	/** The player chose Drink or Eat on an item. */
	void consumed(String itemName, int tick)
	{
		consumedItem = itemName == null ? null : stripDoses(itemName);
		consumedTick = tick;
	}

	/**
	 * Special attack energy fell while a recognised skilling tool was wielded.
	 *
	 * @param toolName  the tool
	 * @param toolSkill the skill its special boosts; a rise in any other skill
	 *                  is not credited to it
	 */
	void specUsed(String toolName, Skill toolSkill, int tick)
	{
		specTool = toolName;
		specSkill = toolSkill;
		specTick = tick;
	}

	/**
	 * A boost event for any skill, from {@link BoostTracker}.
	 *
	 * @return the warning to raise, if the badge is up for this skill
	 */
	Warning boostChanged(Skill changed, BoostTracker.Change change, int tick)
	{
		final int idx = changed.ordinal();
		switch (change)
		{
			case RISE:
				sourceBySkill[idx] = attribute(changed, tick);
				return Warning.NONE;
			case DROP:
				return warn(changed, Warning.DROPPED, tick);
			case EXPIRED:
				sourceBySkill[idx] = null;
				return warn(changed, Warning.EXPIRED, tick);
			case NONE:
			default:
				return Warning.NONE;
		}
	}

	/** Where the current boost on the engaged skill came from, or null. */
	String source()
	{
		return skill == null ? null : sourceBySkill[skill.ordinal()];
	}

	/** The cue the badge should show now, if any. */
	Warning warning(int tick)
	{
		if (warningSkill == null || warningSkill != skill || lastWarning == Warning.NONE)
		{
			return Warning.NONE;
		}

		final int window = lastWarning == Warning.DROPPED ? DROP_CUE_TICKS : EXPIRY_CUE_TICKS;
		return tick - warningTick < window ? lastWarning : Warning.NONE;
	}

	void clear()
	{
		engaged = false;
		skill = null;
		interactionEnded = null;
		pendingClick = null;
		method = null;
		lastCatch = -1;
		java.util.Arrays.fill(sourceBySkill, null);
		consumedItem = null;
		consumedTick = -1;
		specTool = null;
		specSkill = null;
		specTick = -1;
		warningSkill = null;
		lastWarning = Warning.NONE;
		warningTick = -1;
	}

	private Warning warn(Skill changed, Warning warning, int tick)
	{
		warningSkill = changed;
		lastWarning = warning;
		warningTick = tick;
		return changed == skill() ? warning : Warning.NONE;
	}

	/** The most recent of a Drink/Eat or a special attack inside the window, else nothing. */
	private String attribute(Skill changed, int tick)
	{
		final boolean itemRecent = consumedItem != null && consumedTick != -1
			&& tick - consumedTick >= 0 && tick - consumedTick <= ATTRIBUTION_TICKS;
		final boolean specRecent = specTool != null && specSkill == changed && specTick != -1
			&& tick - specTick >= 0 && tick - specTick <= ATTRIBUTION_TICKS;

		if (itemRecent && (!specRecent || consumedTick >= specTick))
		{
			// Any Drink or Eat counts, so a rise after a stew is only a
			// correlation; the label says as much.
			return "likely " + consumedItem;
		}
		if (specRecent)
		{
			return specTool + " special";
		}
		return null;
	}

	/** "Fishing potion(3)" names the dose, which is not the source. */
	static String stripDoses(String itemName)
	{
		return itemName.replaceAll("\\s*\\(\\d+\\)$", "");
	}
}
