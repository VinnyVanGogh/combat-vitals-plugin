package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;

/**
 * Raises the skilling badge for the skills that have no NPC or object to
 * hold: Herblore (Board, 2026-11-14), Cooking, Fletching, Crafting,
 * Smithing, Firemaking, Prayer, Magic, Agility, Construction and Sailing.
 *
 * The same experience-drop and animation pair {@link ActivityAlerts} uses:
 * a gain in one of these skills starts the badge for it; further gains or
 * any animation keep it up; a skill's gap with neither stops it, and the
 * plugin's linger and idle badge take it from there. Animation ids are not
 * listed per skill, since every one of these actions plays one and a
 * list would go stale with each new recipe.
 *
 * Not while another badge is up or a fight is on: an infernal harpoon cooks
 * while you fish, a bonecrusher gives Prayer and every combat spell Magic.
 * A tick that also brings Hitpoints experience is combat, whatever else it
 * brings. Superheat and Spin flax give Magic and a second skill; Magic is
 * what was cast.
 *
 * Pure: experience totals and ticks in, steps out. Not thread safe; the
 * plugin touches it from the client thread only.
 */
final class XpSkilling
{
	enum Step
	{
		NONE,
		/** A gain started the badge, or switched it to another skill. */
		ENGAGE,
		/** The skill's gap passed with no gain or animation: start the linger. */
		DISENGAGE,
		/** A fight or another badge took over: drop it at once. */
		RELEASE
	}

	/** Ticks with neither a gain nor an animation before most skills count as stopped (about 5 s). */
	static final int DEFAULT_IDLE_TICKS = 8;

	private static final Set<Skill> SKILLS = EnumSet.of(Skill.HERBLORE, Skill.COOKING, Skill.FLETCHING,
		Skill.CRAFTING, Skill.SMITHING, Skill.FIREMAKING, Skill.PRAYER, Skill.MAGIC, Skill.AGILITY,
		Skill.CONSTRUCTION, Skill.SAILING);

	/**
	 * Skills whose next drop can be further off: running between agility
	 * obstacles, fetching planks from the butler, a firemaking line's walk
	 * back, the Ectofuntus' grind, a long stretch at sea.
	 */
	private static final Map<Skill, Integer> IDLE_TICKS = new EnumMap<>(Skill.class);

	static
	{
		IDLE_TICKS.put(Skill.AGILITY, 20);
		IDLE_TICKS.put(Skill.CONSTRUCTION, 20);
		IDLE_TICKS.put(Skill.FIREMAKING, 10);
		IDLE_TICKS.put(Skill.MAGIC, 10);
		IDLE_TICKS.put(Skill.PRAYER, 15);
		IDLE_TICKS.put(Skill.SAILING, 30);
	}

	private final int[] lastXp = new int[Skill.values().length];
	private final Set<Skill> gained = EnumSet.noneOf(Skill.class);
	private boolean hitpointsGained;

	private Skill skill;
	private boolean stopped;
	private boolean acted;
	private int lastActiveTick = -1;

	XpSkilling()
	{
		clear();
	}

	/** One of the skills this detector raises the badge for. */
	static boolean tracked(Skill skill)
	{
		return skill != null && SKILLS.contains(skill);
	}

	static int idleTicks(Skill skill)
	{
		return IDLE_TICKS.getOrDefault(skill, DEFAULT_IDLE_TICKS);
	}

	/** A skill's experience, from {@code StatChanged}. The first value per skill is a baseline. */
	void xp(Skill skill, int xp)
	{
		final int i = skill.ordinal();
		final int before = lastXp[i];
		lastXp[i] = xp;
		if (before < 0 || xp <= before)
		{
			return;
		}
		if (skill == Skill.HITPOINTS)
		{
			hitpointsGained = true;
		}
		else if (SKILLS.contains(skill))
		{
			gained.add(skill);
		}
	}

	/**
	 * Once per game tick, after the tick's experience has been reported.
	 *
	 * @param animating the local player has an animation playing
	 * @param blocked   another badge is up or a fight is on
	 */
	Step tick(int tick, boolean animating, boolean blocked)
	{
		final Skill now = blocked || hitpointsGained ? null : chosen();
		gained.clear();
		hitpointsGained = false;
		acted = now != null;

		if (blocked)
		{
			final boolean wasUp = skill != null;
			forget();
			return wasUp ? Step.RELEASE : Step.NONE;
		}

		if (now != null)
		{
			lastActiveTick = tick;
			final boolean start = now != skill || stopped;
			skill = now;
			stopped = false;
			return start ? Step.ENGAGE : Step.NONE;
		}

		if (skill == null || stopped)
		{
			return Step.NONE;
		}
		if (animating)
		{
			lastActiveTick = tick;
			return Step.NONE;
		}
		if (tick - lastActiveTick >= idleTicks(skill))
		{
			stopped = true;
			return Step.DISENGAGE;
		}
		return Step.NONE;
	}

	/** Magic first, then the skill already up, then the first in skill order. */
	private Skill chosen()
	{
		if (gained.isEmpty())
		{
			return null;
		}
		if (gained.contains(Skill.MAGIC))
		{
			return Skill.MAGIC;
		}
		if (skill != null && gained.contains(skill))
		{
			return skill;
		}
		return gained.iterator().next();
	}

	/** The skill the badge is up for, working or lingering, or null. */
	Skill skill()
	{
		return skill;
	}

	/** Gains or animation are still coming in. */
	boolean working()
	{
		return skill != null && !stopped;
	}

	/** The last tick brought a gain in {@link #skill}: one action done. */
	boolean acted()
	{
		return acted;
	}

	/** The badge went to something else: keep the baselines, drop the rest. */
	void forget()
	{
		skill = null;
		stopped = false;
		acted = false;
		lastActiveTick = -1;
	}

	/** Logged out or hopped: everything is a baseline again. */
	void clear()
	{
		Arrays.fill(lastXp, -1);
		gained.clear();
		hitpointsGained = false;
		forget();
	}
}
