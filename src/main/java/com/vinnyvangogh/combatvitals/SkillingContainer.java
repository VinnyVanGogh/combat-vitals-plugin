package com.vinnyvangogh.combatvitals;

import java.util.Locale;
import lombok.Getter;
import net.runelite.api.Skill;

/**
 * A container that stores what a skill gathers, shown under the badge's
 * inventory line while it is carried (Board, 2026-11-14).
 *
 * Capacities from the OSRS wiki ("Log basket", "Fish barrel", "Coal bag",
 * read 2026-11-14); essence pouches vary per pouch, see {@link EssencePouch}.
 * The gem bag (60 per gem type), herb sack, seed box and plank sack are left
 * out on purpose: BRIEF.md, "Skilling containers, compliance".
 */
@Getter
enum SkillingContainer
{
	/** Log basket and Forestry basket: 28 logs of any kind. */
	LOG_BASKET("Log basket", "basket", Skill.WOODCUTTING, true,
		"log basket", "forestry basket"),
	/** Fish barrel and Fish sack barrel: 28 raw fish of any kind. */
	FISH_BARREL("Fish barrel", "barrel", Skill.FISHING, true,
		"fish barrel", "fish sack barrel"),
	/**
	 * 27 coal, 36 with a Smithing cape. Takes coal only, so it is not free
	 * space at any other rock and never counts as inventory.
	 */
	COAL_BAG("Coal bag", "coal bag", Skill.MINING, false,
		"coal bag"),
	/** Every essence pouch carried, as one line. */
	ESSENCE_POUCHES("Essence pouches", "pouches", Skill.RUNECRAFT, true);

	static final int BASKET_CAPACITY = 28;
	static final int BARREL_CAPACITY = 28;
	private static final int COAL_BAG_CAPACITY = 27;
	private static final int COAL_BAG_CAPE_CAPACITY = 36;
	private static final String OPEN = "open ";

	/** "Log basket": the line's name. */
	private final String label;
	/** "basket": in "(+16 basket)" and "check the basket once". */
	private final String noun;
	/** The skill whose badge shows the line. */
	private final Skill skill;
	/** Whatever the skill gathers can go in, so its free space is free inventory. */
	private final boolean countsAsInventory;
	private final String[] names;

	SkillingContainer(String label, String noun, Skill skill, boolean countsAsInventory, String... names)
	{
		this.label = label;
		this.noun = noun;
		this.skill = skill;
		this.countsAsInventory = countsAsInventory;
		this.names = names;
	}

	/** The container an item is, by its name, open or closed; null for anything else. */
	static SkillingContainer fromItemName(String name)
	{
		if (name == null)
		{
			return null;
		}
		String lower = name.toLowerCase(Locale.ROOT).trim();
		if (lower.startsWith(OPEN))
		{
			lower = lower.substring(OPEN.length());
		}
		for (SkillingContainer container : values())
		{
			for (String n : container.names)
			{
				if (n.equals(lower))
				{
					return container;
				}
			}
		}
		return EssencePouch.fromItemName(name) != null ? ESSENCE_POUCHES : null;
	}

	/** "Open log basket": gathered items go straight in while it is open. */
	static boolean isOpen(String name)
	{
		return name != null && name.toLowerCase(Locale.ROOT).startsWith(OPEN);
	}

	static int coalBagCapacity(boolean smithingCape)
	{
		return smithingCape ? COAL_BAG_CAPE_CAPACITY : COAL_BAG_CAPACITY;
	}
}
