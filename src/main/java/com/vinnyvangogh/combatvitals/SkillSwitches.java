package com.vinnyvangogh.combatvitals;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import net.runelite.api.Skill;

/**
 * The per-skill switches in Skilling badge: which skills the badge shows
 * for. A skill without a switch is always shown.
 */
final class SkillSwitches
{
	private static final Map<Skill, Predicate<CombatVitalsConfig>> SWITCHES = new EnumMap<>(Skill.class);

	static
	{
		SWITCHES.put(Skill.FISHING, CombatVitalsConfig::skillingFishing);
		SWITCHES.put(Skill.WOODCUTTING, CombatVitalsConfig::skillingWoodcutting);
		SWITCHES.put(Skill.MINING, CombatVitalsConfig::skillingMining);
		SWITCHES.put(Skill.THIEVING, CombatVitalsConfig::skillingThieving);
		SWITCHES.put(Skill.RUNECRAFT, CombatVitalsConfig::skillingRunecraft);
		SWITCHES.put(Skill.HUNTER, CombatVitalsConfig::skillingHunter);
		SWITCHES.put(Skill.HERBLORE, CombatVitalsConfig::skillingHerblore);
		SWITCHES.put(Skill.COOKING, CombatVitalsConfig::skillingCooking);
		SWITCHES.put(Skill.FLETCHING, CombatVitalsConfig::skillingFletching);
		SWITCHES.put(Skill.CRAFTING, CombatVitalsConfig::skillingCrafting);
		SWITCHES.put(Skill.SMITHING, CombatVitalsConfig::skillingSmithing);
		SWITCHES.put(Skill.FIREMAKING, CombatVitalsConfig::skillingFiremaking);
		SWITCHES.put(Skill.PRAYER, CombatVitalsConfig::skillingPrayer);
		SWITCHES.put(Skill.MAGIC, CombatVitalsConfig::skillingMagic);
		SWITCHES.put(Skill.AGILITY, CombatVitalsConfig::skillingAgility);
		SWITCHES.put(Skill.CONSTRUCTION, CombatVitalsConfig::skillingConstruction);
		SWITCHES.put(Skill.SAILING, CombatVitalsConfig::skillingSailing);
	}

	private SkillSwitches()
	{
	}

	static boolean shown(CombatVitalsConfig config, Skill skill)
	{
		final Predicate<CombatVitalsConfig> on = SWITCHES.get(skill);
		return on == null || on.test(config);
	}

	/** The skill has its own switch. */
	static boolean switchable(Skill skill)
	{
		return SWITCHES.containsKey(skill);
	}
}
