package com.vinnyvangogh.combatvitals;

import java.awt.image.BufferedImage;

/**
 * Whether a skill icon is too dark to see on the badge's dark rows. The
 * Thieving icon is a near-black mask: on the session row's dark background
 * it vanished, and the row read as if it had no icon (Board's screenshots,
 * 2026-11-06), so a dark icon gets a light backing.
 *
 * Pure: an image in, a decision out.
 */
final class IconTone
{
	/** Mean luminance, 0 to 1, of the visible pixels below which an icon counts as dark. */
	static final double DARK = 0.25;

	/** Pixels at least this opaque are the ones that are seen. */
	private static final int VISIBLE_ALPHA = 128;

	private IconTone()
	{
	}

	/** Whether the visible pixels' mean luminance is under {@link #DARK}; an empty image is not dark. */
	static boolean isDark(BufferedImage image)
	{
		double total = 0;
		int counted = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				final int argb = image.getRGB(x, y);
				if ((argb >>> 24) < VISIBLE_ALPHA)
				{
					continue;
				}
				total += luminance((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
				counted++;
			}
		}
		return counted > 0 && total / counted < DARK;
	}

	/** Rec. 709 luma of one pixel, 0 to 1. */
	static double luminance(int red, int green, int blue)
	{
		return (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0;
	}
}
