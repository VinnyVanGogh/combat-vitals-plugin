package com.vinnyvangogh.combatvitals;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How much is in each chat-tracked container (log basket, fish barrel, coal
 * bag), from the game's own lines, never a guess: unknown until a Check or
 * an empty says, and forgotten on logout.
 *
 * The wording is the Plugin Hub's GP Per Hour plugin's (MosheBenZacharia/
 * GP-Per-Hour @ cce6706, {@code itemcharges/items/U_LogBasket.java},
 * {@code U_FishBarrel.java}, {@code U_CoalBag.java}), not captured in game
 * here. Fill and Empty move what the inventory change that follows them
 * shows, the way that plugin's {@code addDifference} does.
 *
 * Pure: messages, clicks and slot counts in, counts out.
 */
final class ContainerContents
{
	/** A Fill or Empty is matched to an inventory change at most this many ticks later. */
	static final int CLICK_TICKS = 2;

	private static final Pattern COUNT = Pattern.compile("(\\d+) x ");

	private static final Pattern BASKET_CHECK = Pattern.compile("^The basket contains:(.*)$");
	private static final Pattern BASKET_EMPTY = Pattern.compile(
		"^(?:(?:Your|The) basket is empty|You empty your basket into the bank)\\.?$");
	private static final Pattern BASKET_ADD = Pattern.compile("^You get some (?:[\\w' -]+ )?logs[.!]?$");
	private static final Pattern BASKET_EXTRA = Pattern.compile("^The nature offerings enabled you to chop an extra log\\.?$");

	private static final Pattern BARREL_CHECK = Pattern.compile("^The barrel contains:(.*)$");
	private static final Pattern BARREL_EMPTY = Pattern.compile("^(?:Your|The) barrel is empty\\.?$");
	private static final Pattern BARREL_FULL = Pattern.compile("^The barrel is full\\..*$");
	private static final Pattern BARREL_EXTRA = Pattern.compile("^.* enabled you to catch an extra fish\\.?$");

	private static final Pattern COAL_CHECK = Pattern.compile("^The coal bag (?:still )?contains (one|\\d+) ");
	private static final Pattern COAL_EMPTY = Pattern.compile("^(?:Your|The) coal bag is (?:now )?empty\\.?$");
	private static final Pattern COAL_ADD = Pattern.compile("^You manage to mine some coal\\.?$");

	private final Map<SkillingContainer, Integer> contents = new EnumMap<>(SkillingContainer.class);
	private Set<SkillingContainer> open = Collections.emptySet();
	private Map<SkillingContainer, Integer> capacities = Collections.emptyMap();

	private SkillingContainer clicked;
	private boolean clickedFill;
	private int clickedTick;

	/** Items inside, or -1 while unknown. */
	int contents(SkillingContainer container)
	{
		final Integer n = contents.get(container);
		return n == null ? -1 : n;
	}

	/** Which containers are carried open, and every carried one's capacity, from the last scan. */
	void carried(Set<SkillingContainer> open, Map<SkillingContainer, Integer> capacities)
	{
		this.open = open.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(open);
		this.capacities = capacities;
	}

	/** @return whether a count changed */
	boolean message(String message)
	{
		if (message == null)
		{
			return false;
		}
		return basket(message) || barrel(message) || coal(message);
	}

	/** A fish worth a barrel slot was caught; it lands in the barrel while open and not full. */
	boolean fishCaught()
	{
		return addOne(SkillingContainer.FISH_BARREL);
	}

	/** Fill, Empty or Empty to bank clicked on a container; anything else is ignored. */
	void clicked(SkillingContainer container, String option, int tick)
	{
		if ("Empty to bank".equalsIgnoreCase(option))
		{
			contents.put(container, 0);
			clicked = null;
		}
		else if ("Fill".equalsIgnoreCase(option) || "Empty".equalsIgnoreCase(option))
		{
			clicked = container;
			clickedFill = "Fill".equalsIgnoreCase(option);
			clickedTick = tick;
		}
	}

	/**
	 * The inventory's filling slots changed: after a Fill, what left went in;
	 * after an Empty, what arrived came out.
	 *
	 * @return whether a count changed
	 */
	boolean inventoryChanged(int fillingBefore, int fillingAfter, int tick)
	{
		final SkillingContainer container = clicked;
		if (container == null || tick - clickedTick > CLICK_TICKS || fillingBefore < 0 || fillingAfter < 0)
		{
			return false;
		}
		final int moved = clickedFill ? fillingBefore - fillingAfter : fillingAfter - fillingBefore;
		final int now = contents(container);
		if (moved <= 0 || now < 0)
		{
			return false;
		}
		clicked = null;
		contents.put(container, clickedFill ? Math.min(capacity(container), now + moved) : Math.max(0, now - moved));
		return true;
	}

	void clear()
	{
		contents.clear();
		open = Collections.emptySet();
		capacities = Collections.emptyMap();
		clicked = null;
	}

	private boolean basket(String message)
	{
		return check(SkillingContainer.LOG_BASKET, BASKET_CHECK, message)
			|| empty(SkillingContainer.LOG_BASKET, BASKET_EMPTY, message)
			|| ((BASKET_ADD.matcher(message).matches() || BASKET_EXTRA.matcher(message).matches())
				&& addOne(SkillingContainer.LOG_BASKET));
	}

	private boolean barrel(String message)
	{
		if (BARREL_FULL.matcher(message).matches())
		{
			contents.put(SkillingContainer.FISH_BARREL, capacity(SkillingContainer.FISH_BARREL));
			return true;
		}
		return check(SkillingContainer.FISH_BARREL, BARREL_CHECK, message)
			|| empty(SkillingContainer.FISH_BARREL, BARREL_EMPTY, message)
			|| (BARREL_EXTRA.matcher(message).matches() && addOne(SkillingContainer.FISH_BARREL));
	}

	private boolean coal(String message)
	{
		final Matcher check = COAL_CHECK.matcher(message);
		if (check.find())
		{
			contents.put(SkillingContainer.COAL_BAG, "one".equals(check.group(1)) ? 1 : Integer.parseInt(check.group(1)));
			return true;
		}
		return empty(SkillingContainer.COAL_BAG, COAL_EMPTY, message)
			|| (COAL_ADD.matcher(message).matches() && addOne(SkillingContainer.COAL_BAG));
	}

	/** "The basket contains: 5 x Logs, 3 x Oak logs": the sum of the counts. */
	private boolean check(SkillingContainer container, Pattern pattern, String message)
	{
		final Matcher matcher = pattern.matcher(message);
		if (!matcher.matches())
		{
			return false;
		}
		int total = 0;
		final Matcher count = COUNT.matcher(matcher.group(1));
		while (count.find())
		{
			total += Integer.parseInt(count.group(1));
		}
		contents.put(container, total);
		return true;
	}

	private boolean empty(SkillingContainer container, Pattern pattern, String message)
	{
		if (!pattern.matcher(message).matches())
		{
			return false;
		}
		contents.put(container, 0);
		return true;
	}

	/** One more into an open container that is known and not full; otherwise it went to the inventory. */
	private boolean addOne(SkillingContainer container)
	{
		final int now = contents(container);
		if (!open.contains(container) || now < 0 || now >= capacity(container))
		{
			return false;
		}
		contents.put(container, now + 1);
		return true;
	}

	private int capacity(SkillingContainer container)
	{
		final Integer capacity = capacities.get(container);
		if (capacity != null)
		{
			return capacity;
		}
		return container == SkillingContainer.COAL_BAG ? SkillingContainer.coalBagCapacity(false)
			: SkillingContainer.BASKET_CAPACITY;
	}
}
