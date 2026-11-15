package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** The one way this plugin paints a bar: background, fill, border. */
final class Bars
{
	private static final Color BORDER = new Color(0, 0, 0, 200);

	private Bars()
	{
	}

	/** Filled from the left, {@code fraction} of the width, for the overhead readout and badge rows. */
	static void across(Graphics2D graphics, Rectangle box, double fraction, Color colour, Color background)
	{
		graphics.setColor(background);
		graphics.fillRect(box.x, box.y, box.width, box.height);

		final int filled = (int) Math.round(box.width * clamp(fraction));
		if (filled > 0)
		{
			graphics.setColor(colour);
			graphics.fillRect(box.x, box.y, filled, box.height);
		}

		graphics.setColor(BORDER);
		graphics.drawRect(box.x, box.y, box.width, box.height);
	}

	/** Filled from the bottom, {@code fraction} of the height, for the side pillars. */
	static void up(Graphics2D graphics, Rectangle box, double fraction, Color colour, Color background)
	{
		up(graphics, box, fraction, colour, 0, null, background);
	}

	/** As above, the bottom {@code baseFraction} of the height in {@code baseColour}. */
	static void up(Graphics2D graphics, Rectangle box, double fraction, Color colour, double baseFraction,
		Color baseColour, Color background)
	{
		graphics.setColor(background);
		graphics.fillRect(box.x, box.y, box.width, box.height);

		fromBottom(graphics, box, fraction, colour);
		if (baseColour != null)
		{
			fromBottom(graphics, box, baseFraction, baseColour);
		}

		graphics.setColor(BORDER);
		graphics.drawRect(box.x, box.y, box.width, box.height);
	}

	private static void fromBottom(Graphics2D graphics, Rectangle box, double fraction, Color colour)
	{
		final int filled = (int) Math.round(box.height * clamp(fraction));
		if (filled > 0)
		{
			graphics.setColor(colour);
			graphics.fillRect(box.x, box.y + box.height - filled, box.width, filled);
		}
	}

	private static double clamp(double fraction)
	{
		return Math.max(0, Math.min(1, fraction));
	}
}
