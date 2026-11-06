package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * Board's screenshots, 2026-11-06: the Thieving session row seemed to have no
 * icon. Its icon is a near-black mask that vanished on the dark row, so dark
 * icons get a light backing.
 */
public class IconToneTest
{
	private static BufferedImage filled(Color colour)
	{
		final BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(colour);
		g.fillOval(1, 1, 10, 10);
		g.dispose();
		return image;
	}

	@Test
	public void aNearBlackIconIsDark()
	{
		assertTrue(IconTone.isDark(filled(new Color(40, 30, 25))));
	}

	@Test
	public void aBrightIconIsNot()
	{
		assertFalse(IconTone.isDark(filled(new Color(200, 170, 90))));
	}

	@Test
	public void transparentPixelsDoNotCount()
	{
		// The icon's clear surround is black with zero alpha; only the visible pixels count.
		assertFalse(IconTone.isDark(filled(new Color(230, 230, 230))));
		assertFalse("an empty image is not dark", IconTone.isDark(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)));
	}

	/**
	 * Board's test run, 2026-11-06: under Rec. 709 luma the Fishing icon read
	 * as dark. Luma counts blue at 7%, so a blue icon that shows plainly on a
	 * near-black row (here luma 0.18, under the line) must not get a backing.
	 */
	@Test
	public void aBlueIconIsNotDark()
	{
		assertFalse(IconTone.isDark(filled(new Color(20, 40, 170))));
	}

	/**
	 * RuneLite's own small skill icons, every skill the badge shows: only
	 * Thieving's mask is dark. Each failure message carries every icon's
	 * measured brightness, so a miss shows where the line has to go.
	 */
	@Test
	public void everyBadgeSkillIconIsClassified() throws Exception
	{
		final Skill[] skills = {
			Skill.FISHING, Skill.WOODCUTTING, Skill.MINING, Skill.THIEVING, Skill.RUNECRAFT, Skill.HUNTER,
		};
		final StringBuilder measured = new StringBuilder("mean brightness, dark under ").append(IconTone.DARK);
		for (Skill skill : skills)
		{
			measured.append(String.format(" | %s %.3f", skill.getName(), IconTone.meanBrightness(icon(skill))));
		}
		for (Skill skill : skills)
		{
			assertEquals(skill.getName() + ": " + measured, skill == Skill.THIEVING, IconTone.isDark(icon(skill)));
		}
	}

	/** The icon SkillIconManager.getSkillImage(skill, true) loads: the client jar's /skill_icons_small/ resource. */
	private static BufferedImage icon(Skill skill) throws Exception
	{
		try (InputStream in = net.runelite.client.game.SkillIconManager.class
			.getResourceAsStream("/skill_icons_small/" + skill.getName().toLowerCase() + ".png"))
		{
			if (in == null)
			{
				throw new AssertionError("no bundled icon for " + skill);
			}
			return ImageIO.read(in);
		}
	}
}
