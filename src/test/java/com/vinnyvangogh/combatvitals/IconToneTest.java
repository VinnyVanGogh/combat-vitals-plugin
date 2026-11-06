package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
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

	/** RuneLite's own small skill icons: Thieving is dark, Fishing and Mining are not. */
	@Test
	public void runelitesThievingIconIsDarkAndFishingIsNot() throws Exception
	{
		assertTrue("thieving", IconTone.isDark(icon("thieving")));
		assertFalse("fishing", IconTone.isDark(icon("fishing")));
		assertFalse("mining", IconTone.isDark(icon("mining")));
	}

	/** The icon SkillIconManager loads: the client jar's /skill_icons_small/ resource. */
	private static BufferedImage icon(String skill) throws Exception
	{
		try (InputStream in = net.runelite.client.game.SkillIconManager.class
			.getResourceAsStream("/skill_icons_small/" + skill + ".png"))
		{
			if (in == null)
			{
				throw new AssertionError("no bundled icon for " + skill);
			}
			return ImageIO.read(in);
		}
	}
}
