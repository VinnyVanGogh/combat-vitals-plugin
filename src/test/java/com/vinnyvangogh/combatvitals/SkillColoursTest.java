package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

public class SkillColoursTest
{
	/** WCAG 2 minimum for large or bold text, which the badge's bold font is. */
	private static final double MIN_CONTRAST = 3.0;

	@Test
	public void eachBadgeSkillHasItsOwnColour()
	{
		final Skill[] skills = {Skill.FISHING, Skill.WOODCUTTING, Skill.MINING, Skill.THIEVING,
			Skill.HUNTER, Skill.RUNECRAFT};
		for (int i = 0; i < skills.length; i++)
		{
			assertNotEquals(skills[i] + " falls back to the default", SkillColours.DEFAULT, SkillColours.of(skills[i]));
			for (int j = i + 1; j < skills.length; j++)
			{
				assertNotEquals(skills[i] + " and " + skills[j], SkillColours.of(skills[i]), SkillColours.of(skills[j]));
			}
		}
	}

	@Test
	public void fishingIsBlueAndWoodcuttingGreen()
	{
		final Color fishing = SkillColours.of(Skill.FISHING);
		assertTrue(fishing.getBlue() > fishing.getRed() && fishing.getBlue() > fishing.getGreen());
		final Color woodcutting = SkillColours.of(Skill.WOODCUTTING);
		assertTrue(woodcutting.getGreen() > woodcutting.getRed() && woodcutting.getGreen() > woodcutting.getBlue());
	}

	@Test
	public void unknownSkillGetsTheDefault()
	{
		assertEquals(SkillColours.DEFAULT, SkillColours.of(Skill.ATTACK));
		assertEquals(SkillColours.DEFAULT, SkillColours.of(null));
	}

	@Test
	public void everyColourReadsOnLightAndDarkGround()
	{
		// Text-only styles draw the colour straight onto the game world, which
		// can be snow or a cave; the bar style puts white text on the colour.
		for (Skill skill : Skill.values())
		{
			final Color c = SkillColours.of(skill);
			assertTrue(skill + " on white: " + SkillColours.contrast(c, Color.WHITE),
				SkillColours.contrast(c, Color.WHITE) >= MIN_CONTRAST);
			assertTrue(skill + " on black: " + SkillColours.contrast(c, Color.BLACK),
				SkillColours.contrast(c, Color.BLACK) >= MIN_CONTRAST);
		}
	}

	@Test
	public void contrastMatchesTheWcagFormula()
	{
		assertEquals(21.0, SkillColours.contrast(Color.BLACK, Color.WHITE), 1e-9);
		assertEquals(1.0, SkillColours.contrast(Color.GRAY, Color.GRAY), 1e-9);
	}
}
