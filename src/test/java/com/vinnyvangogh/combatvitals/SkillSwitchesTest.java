package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

public class SkillSwitchesTest
{
	private static final CombatVitalsConfig DEFAULTS = new CombatVitalsConfig()
	{
	};

	@Test
	public void everyXpDropSkillHasASwitchThatDefaultsOn()
	{
		for (Skill skill : Skill.values())
		{
			if (XpSkilling.tracked(skill))
			{
				assertTrue(skill + " has a switch", SkillSwitches.switchable(skill));
				assertTrue(skill + " is on by default", SkillSwitches.shown(DEFAULTS, skill));
			}
		}
	}

	@Test
	public void switchingHerbloreOffHidesOnlyHerblore()
	{
		final CombatVitalsConfig noHerblore = new CombatVitalsConfig()
		{
			@Override
			public boolean skillingHerblore()
			{
				return false;
			}
		};
		assertFalse(SkillSwitches.shown(noHerblore, Skill.HERBLORE));
		assertTrue(SkillSwitches.shown(noHerblore, Skill.COOKING));
		assertTrue(SkillSwitches.shown(noHerblore, Skill.FISHING));
	}

	@Test
	public void eachNewSwitchHidesItsSkill()
	{
		final CombatVitalsConfig allOff = new CombatVitalsConfig()
		{
			@Override
			public boolean skillingHerblore()
			{
				return false;
			}

			@Override
			public boolean skillingCooking()
			{
				return false;
			}

			@Override
			public boolean skillingFletching()
			{
				return false;
			}

			@Override
			public boolean skillingCrafting()
			{
				return false;
			}

			@Override
			public boolean skillingSmithing()
			{
				return false;
			}

			@Override
			public boolean skillingFiremaking()
			{
				return false;
			}

			@Override
			public boolean skillingPrayer()
			{
				return false;
			}

			@Override
			public boolean skillingMagic()
			{
				return false;
			}

			@Override
			public boolean skillingAgility()
			{
				return false;
			}

			@Override
			public boolean skillingConstruction()
			{
				return false;
			}

			@Override
			public boolean skillingSailing()
			{
				return false;
			}
		};
		for (Skill skill : Skill.values())
		{
			if (XpSkilling.tracked(skill))
			{
				assertFalse(skill + " hidden when off", SkillSwitches.shown(allOff, skill));
			}
		}
		assertTrue("a skill without a switch is always shown", SkillSwitches.shown(allOff, Skill.SLAYER));
	}
}
