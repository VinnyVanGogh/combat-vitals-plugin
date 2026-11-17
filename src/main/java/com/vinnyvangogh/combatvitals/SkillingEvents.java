package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Skill;

/**
 * The skilling events worth coming back for: a bird or clue nest falling, a
 * Forestry event starting near you, a clue drop while skilling, a pet, a
 * level-up, and a random event that has come for you. The plugin reports
 * what it saw as it arrives; {@link #tick} answers with the alerts to send,
 * each at most once per event.
 *
 * Every chat line and spawn id here is the base client's own, read from the
 * plugins that already notify on them (BRIEF.md, "Skilling notifications"):
 * Woodcutting for nests and Forestry, Screenshot for pets, Random Events for
 * the event NPCs and its cooldown. Clue geodes, bottles and scroll boxes have
 * no chat line, so they are read from the player's own inventory.
 *
 * Pure: names, ids and ticks in, alerts out, no client. Client thread only.
 */
final class SkillingEvents
{
	/** The Forestry events, by the base Woodcutting plugin's spawn ids. */
	enum Forestry
	{
		RISING_ROOTS("Rising Roots"),
		STRUGGLING_SAPLING("Struggling Sapling"),
		FLOWERING_BUSH("Flowering Bush"),
		LEPRECHAUN("Woodcutting Leprechaun"),
		POACHERS("Poachers"),
		PHEASANT_CONTROL("Pheasant Control"),
		BEE_HIVE("Bee Hive"),
		ENCHANTMENT_RITUAL("Enchantment Ritual"),
		FRIENDLY_ENT("Friendly Ent");

		final String title;

		Forestry(String title)
		{
			this.title = title;
		}
	}

	/** The alerts this class sends; the rest are {@link ActivityAlerts}'. */
	static final Set<ActivityAlerts.Type> TYPES = Collections.unmodifiableSet(EnumSet.of(
		ActivityAlerts.Type.BIRD_NEST, ActivityAlerts.Type.CLUE_DROP, ActivityAlerts.Type.FORESTRY_EVENT,
		ActivityAlerts.Type.PET, ActivityAlerts.Type.LEVEL_UP, ActivityAlerts.Type.RANDOM_EVENT,
		ActivityAlerts.Type.SKILLING_EVENT));

	/** A Forestry event whose parts have all been gone this many ticks is over. */
	static final int EVENT_END_TICKS = 10;
	/** "You just found a Sapphire!": the mining gem line (unverified wording). */
	private static final Pattern GEM_LINE = Pattern.compile("^You just found (?:an? )?([\\w' -]+)!$");

	private final EventLog eventLog = new EventLog();
	private Forestry openEvent;
	private final List<String> summaries = new ArrayList<>();

	/** The base Random Events plugin's {@code RANDOM_EVENT_TIMEOUT}. */
	static final int RANDOM_EVENT_COOLDOWN = 150;
	/** Woodcutting experience this recently means you are chopping: a minute. */
	static final int WOODCUTTING_RECENT_TICKS = 100;
	/** A Forestry event's parts gone this long means the next spawn is a new event: 30 s. */
	static final int FORESTRY_REARM_TICKS = 50;
	/** Tiles from the player a Forestry spawn counts as near. */
	static final int FORESTRY_RANGE = 15;

	/** The pet line's detail when you already have that pet. */
	static final String DUPLICATE = "duplicate";

	private static final String NEST_LINE = "A bird's nest falls out of the tree";
	private static final String PET_FOLLOWED = "You have a funny feeling like you're being followed";
	private static final String PET_BACKPACK = "You feel something weird sneaking into your backpack";
	private static final String PET_DUPLICATE = "You have a funny feeling like you would have been followed";

	private static final Pattern CLUE_ITEM = Pattern.compile(
		"(clue nest|clue geode|clue bottle|scroll box) \\((beginner|easy|medium|hard|elite|master)\\)",
		Pattern.CASE_INSENSITIVE);

	private static final Set<Skill> COMBAT = EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
		Skill.HITPOINTS, Skill.RANGED, Skill.PRAYER, Skill.MAGIC);

	/** The base Random Events plugin's {@code EVENT_NPCS}. */
	private static final Set<Integer> RANDOM_EVENT_NPCS = new HashSet<>(Arrays.asList(
		307, 314, 322, 326, 327, 375, 376, 380, 390, 5426, 5429, 5436, 5437, 5438, 5439, 5440, 5441, 5510,
		6738, 6743, 6744, 6746, 6747, 6748, 6749, 6750, 6751, 6752, 6753, 6754, 6755, 12551, 12552));

	private static final Map<Integer, Forestry> FORESTRY_OBJECTS = new HashMap<>();
	private static final Map<Integer, Forestry> FORESTRY_NPCS = new HashMap<>();

	static
	{
		put(FORESTRY_OBJECTS, Forestry.RISING_ROOTS, 47482, 47483);
		put(FORESTRY_OBJECTS, Forestry.STRUGGLING_SAPLING, 47484, 47485, 47487, 47488, 47490, 47491);
		put(FORESTRY_NPCS, Forestry.FLOWERING_BUSH, 12120, 12134, 12141, 12143, 12145, 12147, 12149, 12151);
		put(FORESTRY_NPCS, Forestry.LEPRECHAUN, 12163);
		put(FORESTRY_NPCS, Forestry.POACHERS, 12559, 12560);
		put(FORESTRY_NPCS, Forestry.PHEASANT_CONTROL, 12536);
		put(FORESTRY_NPCS, Forestry.BEE_HIVE, 12518);
		put(FORESTRY_NPCS, Forestry.ENCHANTMENT_RITUAL, 12519);
		put(FORESTRY_NPCS, Forestry.FRIENDLY_ENT, 12543);
	}

	private final int[] levels = new int[Skill.values().length];
	private final int[] xps = new int[Skill.values().length];
	private int lastWoodcuttingTick = -1;

	/** Each Forestry event's parts on the scene, and the last tick any was. */
	private final Map<Forestry, Set<Object>> forestryLive = new EnumMap<>(Forestry.class);
	private final Map<Forestry, Integer> forestryLastLive = new EnumMap<>(Forestry.class);
	/** Objects reloading with the scene are not new events, until the next tick. */
	private boolean loading;

	private String clueNest;
	private int clueNestTick = -1;
	private int lastRandomTick;

	/** Set as events arrive, sent and emptied by {@link #tick}. */
	private final List<ActivityAlerts.Alert> pending = new ArrayList<>();
	private boolean nestFell;

	SkillingEvents()
	{
		clear();
	}

	private static void put(Map<Integer, Forestry> map, Forestry kind, int... ids)
	{
		for (int id : ids)
		{
			map.put(id, kind);
		}
	}

	static boolean isEvent(ActivityAlerts.Type type)
	{
		return TYPES.contains(type);
	}

	static Forestry forestryObject(int id)
	{
		return FORESTRY_OBJECTS.get(id);
	}

	static Forestry forestryNpc(int id)
	{
		return FORESTRY_NPCS.get(id);
	}

	static boolean isRandomEventNpc(int id)
	{
		return RANDOM_EVENT_NPCS.contains(id);
	}

	/** "easy" for "Clue geode (easy)"; null for anything but a skilling clue item. */
	static String clueTier(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}
		final Matcher m = CLUE_ITEM.matcher(itemName.trim());
		return m.matches() ? m.group(2).toLowerCase(Locale.ROOT) : null;
	}

	private static boolean isClueNest(String itemName)
	{
		return clueTier(itemName) != null && itemName.trim().toLowerCase(Locale.ROOT).startsWith("clue nest");
	}

	/** Whether a level-up in this skill is one the filter wants. */
	static boolean levelWanted(Skill skill, LevelUpSkills filter)
	{
		return filter != LevelUpSkills.SKILLING || !COMBAT.contains(skill);
	}

	/**
	 * A clue nest goes to the clue drop alert, or to the bird nest alert when
	 * that one is off (Board, 2026-11-14). Anything else is unchanged.
	 */
	static ActivityAlerts.Alert route(ActivityAlerts.Alert alert, boolean clueDropOn)
	{
		if (alert.type == ActivityAlerts.Type.CLUE_DROP && !clueDropOn && isClueNest(alert.detail))
		{
			return new ActivityAlerts.Alert(ActivityAlerts.Type.BIRD_NEST, alert.skill, alert.detail);
		}
		return alert;
	}

	/** The notification text for one of these alerts, or null for any other. */
	static String message(ActivityAlerts.Alert alert)
	{
		switch (alert.type)
		{
			case BIRD_NEST:
				return alert.detail == null ? "A bird's nest fell" : "A " + lower(alert.detail) + " fell";
			case CLUE_DROP:
				return "Clue drop: " + alert.detail;
			case FORESTRY_EVENT:
				return "A Forestry event started: " + alert.detail;
			case PET:
				return DUPLICATE.equals(alert.detail)
					? "You would have got a pet, but you already have it"
					: PET_FOLLOWED;
			case LEVEL_UP:
				return alert.skill.getName() + " level " + alert.detail;
			case RANDOM_EVENT:
				return "A random event is here for you: " + alert.detail;
			case SKILLING_EVENT:
				return alert.detail;
			default:
				return null;
		}
	}

	/**
	 * The voice word list's key for a line more particular than the alert's
	 * own ("CLUE_DROP|hard"), or null when the alert's line says it all.
	 */
	static String voiceKey(ActivityAlerts.Alert alert)
	{
		if (alert.detail == null)
		{
			return null;
		}
		switch (alert.type)
		{
			case CLUE_DROP:
				return alert.type.name() + "|" + clueTier(alert.detail);
			case BIRD_NEST:
				return isClueNest(alert.detail) ? alert.type.name() + "|clue" : null;
			case FORESTRY_EVENT:
				final Forestry kind = byTitle(alert.detail);
				return kind == null ? null : alert.type.name() + "|" + kind.name();
			case PET:
				return DUPLICATE.equals(alert.detail) ? alert.type.name() + "|" + DUPLICATE : null;
			default:
				return null;
		}
	}

	private static Forestry byTitle(String title)
	{
		for (Forestry kind : Forestry.values())
		{
			if (kind.title.equals(title))
			{
				return kind;
			}
		}
		return null;
	}

	private static String lower(String text)
	{
		return text.toLowerCase(Locale.ROOT);
	}

	/**
	 * A skill's experience and real level, from {@code StatChanged}. The first
	 * of each per skill is a baseline: logging in reports every skill.
	 */
	void stat(Skill skill, int xp, int level, int tick)
	{
		final int i = skill.ordinal();
		if (levels[i] >= 0 && level > levels[i])
		{
			pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.LEVEL_UP, skill, Integer.toString(level)));
		}
		if (skill == Skill.WOODCUTTING && xps[i] >= 0 && xp > xps[i])
		{
			lastWoodcuttingTick = tick;
			eventLog.xp(xp - xps[i]);
		}
		levels[i] = level;
		xps[i] = xp;
	}

	/** An item appeared on the ground; a clue nest's tier is read from it. */
	void groundItem(String name, int tick)
	{
		if (isClueNest(name))
		{
			clueNest = name.trim();
			clueNestTick = tick;
		}
	}

	/** A game message, tags already removed. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		if (message.contains(NEST_LINE))
		{
			nestFell = true;
		}
		else if (message.startsWith(PET_DUPLICATE))
		{
			petOnce(DUPLICATE);
		}
		else if (message.startsWith(PET_FOLLOWED) || message.startsWith(PET_BACKPACK))
		{
			petOnce(null);
		}
		else
		{
			gatheringEvent(message);
		}
	}

	/** One pet alert per tick, however many chat types carry the line. */
	private void petOnce(String detail)
	{
		for (ActivityAlerts.Alert alert : pending)
		{
			if (alert.type == ActivityAlerts.Type.PET)
			{
				return;
			}
		}
		pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.PET, null, detail));
	}

	/** Mining gems and fishing clue bottles, as one alert each (Board, 2026-11-16). */
	private void gatheringEvent(String message)
	{
		final Matcher gem = GEM_LINE.matcher(message);
		if (gem.matches())
		{
			pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.SKILLING_EVENT, Skill.MINING,
				"Gem found: " + gem.group(1)));
		}
		else if (message.startsWith("You catch") && message.toLowerCase(Locale.ROOT).contains("clue bottle"))
		{
			pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.SKILLING_EVENT, Skill.FISHING, "Clue bottle caught"));
		}
	}

	/** Finished event lines for the chat, taken once. */
	List<String> takeSummaries()
	{
		if (summaries.isEmpty())
		{
			return Collections.emptyList();
		}
		final List<String> out = new ArrayList<>(summaries);
		summaries.clear();
		return out;
	}

	/**
	 * An item landed in the player's own inventory.
	 *
	 * @param skilling a gathering or thieving activity is going and the bank is shut
	 */
	void inventoryGained(String name, boolean skilling)
	{
		eventLog.reward(name);
		// A clue nest alerted when it fell; picking it up is not news.
		if (skilling && clueTier(name) != null && !isClueNest(name))
		{
			pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.CLUE_DROP, null, name.trim()));
		}
	}

	/**
	 * A Forestry event's object or NPC spawned.
	 *
	 * @param key  the spawned thing, to match its despawn
	 * @param near within {@link #FORESTRY_RANGE} tiles of the player
	 */
	void forestrySpawned(Forestry kind, Object key, boolean near, int tick)
	{
		final Set<Object> live = forestryLive.computeIfAbsent(kind, k -> new HashSet<>());
		final boolean fresh = live.isEmpty();
		live.add(key);
		final Integer lastLive = forestryLastLive.get(kind);
		final boolean rearmed = lastLive == null || tick - lastLive > FORESTRY_REARM_TICKS;
		forestryLastLive.put(kind, tick);
		if (fresh && rearmed && near && !loading && choppedRecently(tick))
		{
			pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.FORESTRY_EVENT, Skill.WOODCUTTING, kind.title));
			final String previous = eventLog.started(kind.title, tick);
			if (previous != null)
			{
				summaries.add(previous);
			}
			openEvent = kind;
		}
	}

	void forestryDespawned(Forestry kind, Object key, int tick)
	{
		final Set<Object> live = forestryLive.get(kind);
		if (live != null && live.remove(key))
		{
			forestryLastLive.put(kind, tick);
		}
	}

	private boolean choppedRecently(int tick)
	{
		return lastWoodcuttingTick >= 0 && tick - lastWoodcuttingTick <= WOODCUTTING_RECENT_TICKS;
	}

	/** A random event NPC has turned to the local player (the plugin checks that). */
	void randomEvent(String name, int tick)
	{
		if (tick - lastRandomTick > RANDOM_EVENT_COOLDOWN)
		{
			lastRandomTick = tick;
			pending.add(new ActivityAlerts.Alert(ActivityAlerts.Type.RANDOM_EVENT, null, name));
		}
	}

	/**
	 * The scene is reloading: every object in view despawns and spawns again.
	 * Objects are forgotten, and none spawned before the next tick is news.
	 */
	void sceneLoading()
	{
		loading = true;
		forestryLive.remove(Forestry.RISING_ROOTS);
		forestryLive.remove(Forestry.STRUGGLING_SAPLING);
	}

	/** @return the alerts to send this tick, in arrival order; empty most ticks */
	List<ActivityAlerts.Alert> tick(int tick)
	{
		loading = false;
		for (Map.Entry<Forestry, Set<Object>> entry : forestryLive.entrySet())
		{
			if (!entry.getValue().isEmpty())
			{
				forestryLastLive.put(entry.getKey(), tick);
			}
		}
		endEventIfGone(tick);
		if (nestFell)
		{
			// The clue nest's ground item arrives just before the chat line.
			final boolean clue = clueNest != null && clueNestTick >= tick - 1;
			pending.add(0, clue
				? new ActivityAlerts.Alert(ActivityAlerts.Type.CLUE_DROP, Skill.WOODCUTTING, clueNest)
				: new ActivityAlerts.Alert(ActivityAlerts.Type.BIRD_NEST, Skill.WOODCUTTING));
			nestFell = false;
			clueNest = null;
		}
		if (pending.isEmpty())
		{
			return Collections.emptyList();
		}
		final List<ActivityAlerts.Alert> out = new ArrayList<>(pending);
		pending.clear();
		return out;
	}

	/** The open Forestry event is over once all its parts have been gone {@link #EVENT_END_TICKS} ticks. */
	private void endEventIfGone(int tick)
	{
		if (openEvent == null)
		{
			return;
		}
		final Set<Object> live = forestryLive.get(openEvent);
		final Integer last = forestryLastLive.get(openEvent);
		if ((live == null || live.isEmpty()) && last != null && tick - last >= EVENT_END_TICKS)
		{
			final String summary = eventLog.finish();
			if (summary != null)
			{
				summaries.add(summary);
			}
			openEvent = null;
		}
	}

	/** Logged out or hopped: everything is a baseline again. */
	void clear()
	{
		eventLog.clear();
		openEvent = null;
		summaries.clear();
		Arrays.fill(levels, -1);
		Arrays.fill(xps, -1);
		lastWoodcuttingTick = -1;
		forestryLive.clear();
		forestryLastLive.clear();
		loading = false;
		clueNest = null;
		clueNestTick = -1;
		lastRandomTick = Integer.MIN_VALUE / 2;
		pending.clear();
		nestFell = false;
	}
}
