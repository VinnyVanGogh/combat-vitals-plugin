package com.vinnyvangogh.combatvitals;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The inventory pillar's icon: a small brown backpack, drawn here rather
 * than taken from a game sprite so it needs no sprite id and no client.
 * Sized like the small skill icons the other pillars carry.
 */
final class PillarIcons
{
	static final int SIZE = 16;

	private static final Color LEATHER = new Color(150, 100, 50);
	private static final Color FLAP = new Color(115, 72, 34);
	private static final Color BUCKLE = new Color(230, 190, 80);
	private static final Color OUTLINE = new Color(20, 12, 4);

	private static BufferedImage backpack;

	private PillarIcons()
	{
	}

	static synchronized BufferedImage backpack()
	{
		if (backpack == null)
		{
			backpack = drawBackpack();
		}
		return backpack;
	}

	private static BufferedImage drawBackpack()
	{
		final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setStroke(new BasicStroke(1f));

			// Carry handle.
			g.setColor(OUTLINE);
			g.drawRoundRect(5, 0, 5, 4, 3, 3);

			// Body.
			g.setColor(LEATHER);
			g.fillRoundRect(2, 3, 12, 12, 5, 5);
			g.setColor(OUTLINE);
			g.drawRoundRect(2, 3, 11, 12, 5, 5);

			// Flap over the top half, and its buckle.
			g.setColor(FLAP);
			g.fillRoundRect(3, 4, 10, 5, 3, 3);
			g.setColor(OUTLINE);
			g.drawLine(3, 9, 12, 9);
			g.setColor(BUCKLE);
			g.fillRect(7, 8, 2, 3);

			// Front pocket.
			g.setColor(OUTLINE);
			g.drawRoundRect(4, 11, 7, 3, 2, 2);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}
}
