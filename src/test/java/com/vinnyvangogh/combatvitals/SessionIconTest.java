package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * Board's screenshots and test run, 2026-11-06: the Thieving icon vanished on
 * the session row, and no brightness threshold could pick it out from Mining
 * or Fishing. So every skill's icon gets the same light backing.
 */
public class SessionIconTest
{
	/** The session row's darkest look: a near-black bar. */
	private static final Color ROW = new Color(20, 20, 20);

	private static final int X = 5;
	private static final int TOP = 4;
	private static final int WIDTH = 11;

	/** The row with one session icon drawn on it. */
	private static BufferedImage drawn(BufferedImage icon)
	{
		final BufferedImage row = new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = row.createGraphics();
		g.setColor(ROW);
		g.fillRect(0, 0, row.getWidth(), row.getHeight());
		SkillingBadgeRenderer.drawSessionIcon(g, icon, X, TOP, WIDTH);
		g.dispose();
		return row;
	}

	/** The backing's left edge, beside the icon, halfway down. */
	private static int backing(BufferedImage row)
	{
		return row.getRGB(X - 1, TOP + 5);
	}

	@Test
	public void theBackingIsLighterThanTheRow()
	{
		final BufferedImage row = drawn(new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB));
		final Color behind = new Color(backing(row));
		assertTrue("backing " + behind + " lighter than the row", behind.getRed() > ROW.getRed() + 60);
		assertEquals("nothing drawn outside the backing", ROW.getRGB(), row.getRGB(X - 3, TOP + 5));
	}

	/** Every skill the badge shows, Thieving's near-black mask included, gets the one same backing. */
	@Test
	public void everyBadgeSkillIconGetsTheSameBacking() throws Exception
	{
		final int expected = backing(drawn(icon(Skill.THIEVING)));
		for (Skill skill : new Skill[]{
			Skill.FISHING, Skill.WOODCUTTING, Skill.MINING, Skill.THIEVING, Skill.RUNECRAFT, Skill.HUNTER,
		})
		{
			assertEquals(skill.getName(), expected, backing(drawn(icon(skill))));
		}
	}

	/** The icon SkillIconManager.getSkillImage(skill, true) loads: the client jar's /skill_icons_small/ resource. */
	private static BufferedImage icon(Skill skill) throws Exception
	{
		try (InputStream in = net.runelite.client.game.SkillIconManager.class
			.getResourceAsStream("/skill_icons_small/" + skill.getName().toLowerCase() + ".png"))
		{
			assertNotNull("no bundled icon for " + skill, in);
			return ImageIO.read(in);
		}
	}
}
