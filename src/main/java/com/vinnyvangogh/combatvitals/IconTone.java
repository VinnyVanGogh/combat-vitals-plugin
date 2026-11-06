package com.vinnyvangogh.combatvitals;

import java.awt.image.BufferedImage;

/**
 * Whether a skill icon is too dark to see on the badge's dark rows. The
 * Thieving icon is a near-black mask: on the session row's dark background
 * it vanished, and the row read as if it had no icon (Board's screenshots,
 * 2026-11-06), so a dark icon gets a light backing.
 *
 * Each pixel is measured by its brightest channel (HSV value), not by Rec.
 * 709 luma. Luma weights blue at 7%, so the blue Fishing fish read as dark
 * (Board's test run, 2026-11-06) although it shows plainly on a near-black
 * row. Every pixel's brightest channel is at least its luma, so this can only
 * clear an icon luma called dark, never flag one it did not.
 *
 * Pure: an image in, a decision out.
 */
final class IconTone
{
	/** Mean brightness, 0 to 1, of the visible pixels below which an icon counts as dark. */
	static final double DARK = 0.25;

	/** Pixels at least this opaque are the ones that are seen. */
	private static final int VISIBLE_ALPHA = 128;

	private IconTone()
	{
	}

	/** Whether the visible pixels' mean brightness is under {@link #DARK}; an empty image is not dark. */
	static boolean isDark(BufferedImage image)
	{
		final double brightness = meanBrightness(image);
		return brightness >= 0 && brightness < DARK;
	}

	/** The visible pixels' mean {@link #brightness}, 0 to 1, or -1 when no pixel is visible. */
	static double meanBrightness(BufferedImage image)
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
				total += brightness((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
				counted++;
			}
		}
		return counted == 0 ? -1 : total / counted;
	}

	/** One pixel's brightest channel (HSV value), 0 to 1. */
	static double brightness(int red, int green, int blue)
	{
		return Math.max(red, Math.max(green, blue)) / 255.0;
	}
}
