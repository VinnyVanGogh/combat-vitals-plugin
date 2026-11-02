package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.runelite.api.Skill;
import org.junit.Test;

/** The spec-tool table, checked against the OSRS wiki's "Special attack" list. */
public class SkillingToolsTest
{
	@Test
	public void harpoonsAreFishing()
	{
		assertEquals(Skill.FISHING, SkillingTools.specSkill("Dragon harpoon"));
		assertEquals(Skill.FISHING, SkillingTools.specSkill("Infernal harpoon"));
		assertEquals(Skill.FISHING, SkillingTools.specSkill("Crystal harpoon"));
	}

	@Test
	public void axesAreWoodcutting()
	{
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("Dragon axe"));
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("Dragon felling axe"));
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("Infernal axe"));
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("3rd age axe"));
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("3rd age felling axe"));
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("Crystal axe"));
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("Crystal felling axe"));
	}

	@Test
	public void pickaxesAreMining()
	{
		assertEquals(Skill.MINING, SkillingTools.specSkill("Dragon pickaxe"));
		assertEquals(Skill.MINING, SkillingTools.specSkill("Dragon pickaxe (or)"));
		assertEquals(Skill.MINING, SkillingTools.specSkill("Dragon pickaxe (upgraded)"));
		assertEquals(Skill.MINING, SkillingTools.specSkill("Infernal pickaxe"));
		assertEquals(Skill.MINING, SkillingTools.specSkill("3rd age pickaxe"));
		assertEquals(Skill.MINING, SkillingTools.specSkill("Crystal pickaxe"));
	}

	@Test
	public void eventVariantsCount()
	{
		assertEquals(Skill.WOODCUTTING, SkillingTools.specSkill("Trailblazer axe"));
		assertEquals(Skill.MINING, SkillingTools.specSkill("Echo pickaxe"));
	}

	@Test
	public void lesserToolsHaveNoSpecial()
	{
		assertNull(SkillingTools.specSkill("Rune axe"));
		assertNull(SkillingTools.specSkill("Rune pickaxe"));
		assertNull(SkillingTools.specSkill("Harpoon"));
		assertNull(SkillingTools.specSkill("Barb-tail harpoon"));
		assertNull(SkillingTools.specSkill("Dragon scimitar"));
		assertNull(SkillingTools.specSkill("Dragon dagger"));
	}

	@Test
	public void unchargedCrystalToolsHaveNoSpecial()
	{
		assertNull(SkillingTools.specSkill("Crystal axe (inactive)"));
		assertNull(SkillingTools.specSkill("Crystal harpoon (inactive)"));
	}

	@Test
	public void nothingWieldedIsNothing()
	{
		assertNull(SkillingTools.specSkill(null));
		assertNull(SkillingTools.specSkill(""));
	}
}
