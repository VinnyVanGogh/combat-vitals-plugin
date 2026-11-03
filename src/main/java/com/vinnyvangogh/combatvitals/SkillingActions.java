package com.vinnyvangogh.combatvitals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Works out which skill an NPC or object trains, from its menu options.
 *
 * A fishing spot's menu says "Net" or "Harpoon"; a pickpocket target's says
 * "Pickpocket". The option is the only thing the client exposes that ties the
 * NPC to a skill, and it is enough: an unknown option means no badge rather
 * than a guess, which is the same rule the health readout follows.
 *
 * Pure and stateless, so the table is testable without a client.
 */
final class SkillingActions
{
	/** Menu option, lower case, to the skill it trains. */
	private static final Map<String, Skill> BY_ACTION = new HashMap<>();

	static
	{
		// Fishing spots: every style the OSRS wiki lists for an NPC spot.
		for (String action : new String[] {
			"net", "small net", "big net", "bait", "lure", "cage", "harpoon", "use-rod", "fish", "cast"})
		{
			BY_ACTION.put(action, Skill.FISHING);
		}

		BY_ACTION.put("pickpocket", Skill.THIEVING);
		BY_ACTION.put("knock-out", Skill.THIEVING);

		// Implings and butterflies. Aerial fishing shoals also say "Catch" and
		// are told apart by name below.
		BY_ACTION.put("catch", Skill.HUNTER);

		// Trees, rocks, stalls and altars are objects, matched by the clicked
		// option alone; rare as NPCs, but harmless there too.
		BY_ACTION.put("mine", Skill.MINING);
		BY_ACTION.put("chop down", Skill.WOODCUTTING);
		BY_ACTION.put("chop", Skill.WOODCUTTING);
		BY_ACTION.put("steal-from", Skill.THIEVING);
		BY_ACTION.put("craft-rune", Skill.RUNECRAFT);
	}

	private SkillingActions()
	{
	}

	/**
	 * @param actions the NPC's menu options, {@code NPCComposition#getActions()}
	 * @param name    the NPC's name, used only to split "Catch" between fishing
	 *                and hunter
	 * @return the skill the NPC trains, or null when none is recognised
	 */
	static Skill skill(String[] actions, String name)
	{
		if (actions == null)
		{
			return null;
		}

		for (String action : actions)
		{
			final Skill skill = forOption(action, name);
			if (skill != null)
			{
				return skill;
			}
		}
		return null;
	}

	/**
	 * The skill one clicked menu option trains. This is what decides an NPC
	 * that can be both fought and skilled, a Man that can be pickpocketed,
	 * and every object: a tree's "Chop down", a rock's "Mine".
	 *
	 * @param option the menu option, e.g. "Pickpocket"
	 * @param name   the target's name, used only to split "Catch"
	 * @return the skill, or null when the option is not a skilling one
	 */
	static Skill forOption(String option, String name)
	{
		if (option == null)
		{
			return null;
		}

		final Skill skill = BY_ACTION.get(option.toLowerCase(Locale.ROOT));
		if (skill == Skill.HUNTER && name != null
			&& name.toLowerCase(Locale.ROOT).contains("shoal"))
		{
			return Skill.FISHING;
		}
		return skill;
	}
}
