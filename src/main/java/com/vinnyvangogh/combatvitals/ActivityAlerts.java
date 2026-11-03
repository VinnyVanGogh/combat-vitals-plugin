package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.runelite.api.Skill;

/**
 * Decides when an activity alert fires. The plugin feeds it what it saw on
 * each tick; it answers with the alerts to send, and the plugin maps each to
 * its config toggle and RuneLite's {@code Notifier}.
 *
 * <h3>Stopped</h3>
 * Experience in a tracked skill arms the stop detector for that skill. Any
 * animation or further experience keeps it alive; after the threshold with
 * neither, the alert fires once and the detector disarms until the next
 * experience drop. Anything that means you stopped on purpose disarms it
 * without firing: walking (to the next spot, or the bank), the bank opening,
 * items leaving the inventory (deposited or dropped), the inventory filling
 * (that has its own alert), the spot moving (likewise), and logging out or
 * hopping ({@link #clear}).
 *
 * <h3>Inventory full</h3>
 * Fires on the tick the last free slot is taken while a gathering or
 * thieving detector is armed. The first inventory seen is a baseline.
 *
 * <h3>Thresholds</h3>
 * Coin pouches fire on crossing up to the cap, run energy on crossing down
 * through the threshold; each re-arms once back on the other side. Neither
 * fires on the first value seen, so logging in with 28 pouches is quiet.
 *
 * Pure: ticks and counts in, alerts out, no client. Not thread safe; the
 * plugin touches it from the client thread only.
 */
final class ActivityAlerts
{
	enum Kind
	{
		/** Fishing, woodcutting, mining, hunter. */
		GATHERING,
		THIEVING,
		/** Make-X: cooking, fletching, crafting, smithing. */
		PRODUCTION
	}

	enum Type
	{
		INVENTORY_FULL,
		GATHERING_STOPPED,
		THIEVING_STOPPED,
		PRODUCTION_FINISHED,
		SPOT_MOVED,
		STUNNED,
		COIN_POUCH_CAP,
		NECKLACE_BROKE,
		LOW_RUN_ENERGY
	}

	/** One alert to send, with the skill it is about when there is one. */
	static final class Alert
	{
		final Type type;
		final Skill skill;

		Alert(Type type, Skill skill)
		{
			this.type = type;
			this.skill = skill;
		}

		@Override
		public String toString()
		{
			return skill == null ? type.name() : type + "(" + skill + ")";
		}
	}

	/** What the plugin saw on one tick, beyond the events recorded as they arrive. */
	static final class Tick
	{
		int tick;
		/** The local player has an animation playing. */
		boolean animating;
		/** The local player's tile changed since the last tick. */
		boolean moved;
		/** Inventory slots by {@link InventoryFill.Slot}; -1 until the inventory is seen. */
		int filling = -1;
		int empty = -1;
		/** Coin pouches held, -1 until seen. */
		int coinPouches = -1;
		/** Run energy in whole percent, -1 until seen. */
		int runEnergy = -1;
	}

	/** A failed pickpocket stuns for nine ticks, about five seconds. */
	static final int STUN_TICKS = 9;

	private static final Pattern NECKLACE_BREAK =
		Pattern.compile("Your dodgy necklace protects you\\..*It then crumbles to dust\\.");

	private static final Map<Skill, Kind> KINDS = new EnumMap<>(Skill.class);

	static
	{
		KINDS.put(Skill.FISHING, Kind.GATHERING);
		KINDS.put(Skill.WOODCUTTING, Kind.GATHERING);
		KINDS.put(Skill.MINING, Kind.GATHERING);
		KINDS.put(Skill.HUNTER, Kind.GATHERING);
		KINDS.put(Skill.THIEVING, Kind.THIEVING);
		KINDS.put(Skill.COOKING, Kind.PRODUCTION);
		KINDS.put(Skill.FLETCHING, Kind.PRODUCTION);
		KINDS.put(Skill.CRAFTING, Kind.PRODUCTION);
		KINDS.put(Skill.SMITHING, Kind.PRODUCTION);
	}

	private final int[] xpBySkill = new int[Skill.values().length];

	/** The skill the stop detector is armed for, or null when disarmed. */
	private Skill active;
	private int lastActiveTick = -1;

	/** Set by events between ticks, consumed by {@link #tick}. */
	private Skill xpThisTick;
	private boolean banked;
	private boolean stunned;
	private boolean necklaceBroke;
	private Skill spotMoved;

	private int lastFilling = -1;
	private int lastEmpty = -1;
	private int lastPouches = -1;
	private int lastEnergy = -1;

	ActivityAlerts()
	{
		clear();
	}

	/** The kind of activity a skill is, or null when it is not tracked. */
	static Kind kind(Skill skill)
	{
		return skill == null ? null : KINDS.get(skill);
	}

	/** The game message for a failed pickpocket's stun, tags already removed. */
	static boolean isStunMessage(String message)
	{
		return message != null && message.startsWith("You've been stunned");
	}

	/**
	 * The dodgy necklace's last charge, tags already removed. The same pattern
	 * as the base client's Item Charges plugin ({@code DODGY_BREAK_PATTERN}).
	 */
	static boolean isNecklaceBreakMessage(String message)
	{
		return message != null && NECKLACE_BREAK.matcher(message).find();
	}

	/** Seconds to game ticks of 0.6 s, at least one. */
	static int ticks(int seconds)
	{
		return Math.max(1, (int) Math.round(seconds / 0.6));
	}

	/**
	 * A skill's experience, from {@code StatChanged}. The first value per skill
	 * is a baseline: logging in reports every skill and is not a drop.
	 */
	void xp(Skill skill, int xp)
	{
		final int idx = skill.ordinal();
		final int before = xpBySkill[idx];
		xpBySkill[idx] = xp;
		if (before >= 0 && xp > before && KINDS.containsKey(skill))
		{
			xpThisTick = skill;
		}
	}

	/** The bank opened. */
	void banked()
	{
		banked = true;
	}

	/** "You've been stunned!" */
	void stunned()
	{
		stunned = true;
	}

	/** "Your dodgy necklace protects you... It then crumbles to dust." */
	void necklaceBroke()
	{
		necklaceBroke = true;
	}

	/**
	 * The spot being worked despawned while the player was still on it,
	 * which is how a fishing spot moves.
	 */
	void spotMoved(Skill skill)
	{
		spotMoved = skill;
	}

	/** The skill the stop detector is armed for, or null. */
	Skill active()
	{
		return active;
	}

	/**
	 * @param stopTicks ticks without animation or experience before "stopped"
	 * @param pouchCap  coin pouches at which the cap alert fires
	 * @param energyLow run energy percent below which the low energy alert fires
	 * @return the alerts to send, in a stable order; empty most ticks
	 */
	List<Alert> tick(Tick now, int stopTicks, int pouchCap, int energyLow)
	{
		List<Alert> out = null;

		if (xpThisTick != null)
		{
			active = xpThisTick;
			lastActiveTick = now.tick;
		}
		else if (active != null && now.animating)
		{
			lastActiveTick = now.tick;
		}

		// A stun holds you still with no experience for its whole length; that
		// is not you stopping, so the clock starts again once it wears off.
		if (stunned && active != null)
		{
			lastActiveTick = Math.max(lastActiveTick, now.tick + STUN_TICKS);
		}

		final boolean inventorySeen = now.filling >= 0 && now.empty >= 0;
		final boolean firstInventory = lastFilling < 0;
		final boolean itemsLeft = inventorySeen && !firstInventory && now.filling < lastFilling;
		final boolean filled = inventorySeen && !firstInventory && now.empty == 0 && lastEmpty > 0;

		if (filled && active != null && KINDS.get(active) != Kind.PRODUCTION)
		{
			out = add(out, new Alert(Type.INVENTORY_FULL, active));
			active = null;
		}

		if (spotMoved != null)
		{
			out = add(out, new Alert(Type.SPOT_MOVED, spotMoved));
			active = null;
		}

		// Stopping on purpose is not "stopped": no alert, just disarm. Moving
		// counts only on a tick with no experience, since pickpocketing a
		// wandering NPC walks you after it between successes.
		if ((now.moved && xpThisTick == null) || banked || itemsLeft)
		{
			active = null;
		}

		if (active != null && now.tick - lastActiveTick >= stopTicks)
		{
			out = add(out, new Alert(stopType(KINDS.get(active)), active));
			active = null;
		}

		if (stunned)
		{
			out = add(out, new Alert(Type.STUNNED, Skill.THIEVING));
		}
		if (necklaceBroke)
		{
			out = add(out, new Alert(Type.NECKLACE_BROKE, null));
		}

		if (now.coinPouches >= 0)
		{
			if (lastPouches >= 0 && lastPouches < pouchCap && now.coinPouches >= pouchCap)
			{
				out = add(out, new Alert(Type.COIN_POUCH_CAP, Skill.THIEVING));
			}
			lastPouches = now.coinPouches;
		}

		if (now.runEnergy >= 0)
		{
			if (lastEnergy >= 0 && lastEnergy >= energyLow && now.runEnergy < energyLow)
			{
				out = add(out, new Alert(Type.LOW_RUN_ENERGY, null));
			}
			lastEnergy = now.runEnergy;
		}

		if (inventorySeen)
		{
			lastFilling = now.filling;
			lastEmpty = now.empty;
		}

		xpThisTick = null;
		banked = false;
		stunned = false;
		necklaceBroke = false;
		spotMoved = null;
		return out == null ? Collections.emptyList() : out;
	}

	/** Logged out or hopped: everything is a baseline again. */
	void clear()
	{
		java.util.Arrays.fill(xpBySkill, -1);
		active = null;
		lastActiveTick = -1;
		xpThisTick = null;
		banked = false;
		stunned = false;
		necklaceBroke = false;
		spotMoved = null;
		lastFilling = -1;
		lastEmpty = -1;
		lastPouches = -1;
		lastEnergy = -1;
	}

	/** The notification text for an alert. */
	static String message(Alert alert, int pouchCap)
	{
		switch (alert.type)
		{
			case INVENTORY_FULL:
				return "Your inventory is full while " + verb(alert.skill);
			case GATHERING_STOPPED:
			case THIEVING_STOPPED:
				return "You have stopped " + verb(alert.skill);
			case PRODUCTION_FINISHED:
				return "You have finished " + verb(alert.skill);
			case SPOT_MOVED:
				return "Your fishing spot has moved";
			case STUNNED:
				return "You have been stunned";
			case COIN_POUCH_CAP:
				return "You have " + pouchCap + " coin pouches: open them";
			case NECKLACE_BROKE:
				return "Your dodgy necklace has crumbled";
			case LOW_RUN_ENERGY:
			default:
				return "You have low run energy";
		}
	}

	/** "fishing", "hunting", "smithing": the activity as a verb. */
	static String verb(Skill skill)
	{
		if (skill == null)
		{
			return "skilling";
		}
		switch (skill)
		{
			case HUNTER:
				return "hunting";
			default:
				return skill.getName().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private static Type stopType(Kind kind)
	{
		switch (kind)
		{
			case THIEVING:
				return Type.THIEVING_STOPPED;
			case PRODUCTION:
				return Type.PRODUCTION_FINISHED;
			case GATHERING:
			default:
				return Type.GATHERING_STOPPED;
		}
	}

	private static List<Alert> add(List<Alert> out, Alert alert)
	{
		final List<Alert> list = out == null ? new ArrayList<>() : out;
		list.add(alert);
		return list;
	}
}
