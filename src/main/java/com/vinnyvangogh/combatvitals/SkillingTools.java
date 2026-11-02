package com.vinnyvangogh.combatvitals;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Skill;

/**
 * Recognises the skilling tools whose special attack boosts a skill.
 *
 * Lumber Up (+3 Woodcutting), Rock Knocker (+3 Mining) and Fishstabber
 * (+3 Fishing) live on the dragon, infernal, crystal, 3rd age and event
 * variants of the axe, pickaxe and harpoon, all for 100% special attack
 * energy. Matching on the item name rather than a list of ids keeps the
 * ornamented and felling variants covered without maintaining every id.
 *
 * Source: OSRS wiki, "Special attack", skilling tools table.
 */
final class SkillingTools
{
	private static final Pattern SPEC_TOOL = Pattern.compile(
		"^(?:dragon|infernal|crystal|3rd age|trailblazer|echo) (?:felling )?(axe|pickaxe|harpoon)\\b");

	private SkillingTools()
	{
	}

	/**
	 * @param weaponName the equipped weapon's name, or null when nothing is wielded
	 * @return the skill the weapon's special attack boosts, or null when it has none
	 */
	static Skill specSkill(String weaponName)
	{
		if (weaponName == null)
		{
			return null;
		}

		final String name = weaponName.toLowerCase(Locale.ROOT);

		// An uncharged crystal tool cannot be used at all, let alone spec'd.
		if (name.contains("(inactive)") || name.contains("(uncharged)"))
		{
			return null;
		}

		final Matcher matcher = SPEC_TOOL.matcher(name);
		if (!matcher.find())
		{
			return null;
		}

		switch (matcher.group(1))
		{
			case "axe":
				return Skill.WOODCUTTING;
			case "pickaxe":
				return Skill.MINING;
			case "harpoon":
				return Skill.FISHING;
			default:
				return null;
		}
	}
}
