package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.function.IntUnaryOperator;
import org.junit.Test;

/**
 * Board's screenshots, 2026-11-06: the catch icons ran into the word "free"
 * in "Inv 12/28 free", "Inv 14/28 free" and "Inv 0/28 free" but not at
 * other counts. The label's space is now measured at the font's widest digit
 * and a gap kept before the icons, which are drawn from the right edge.
 */
public class InventoryLineTest
{
	/** A font where "1" is narrow and "8" wide, as in most proportional fonts. */
	private static final IntUnaryOperator UNEVEN = c -> c == '1' ? 3 : Character.isDigit(c) ? 6 : 5;

	@Test
	public void everyDigitIsGivenTheWidestDigitsRoom()
	{
		// "Inv 12/28 free": 4 digits at 6, 10 other characters at 5.
		assertEquals(4 * 6 + 10 * 5, InventoryLine.reservedWidth("Inv 12/28 free", UNEVEN, 0));
	}

	@Test
	public void neverLessThanTheMeasuredWidth()
	{
		assertEquals(500, InventoryLine.reservedWidth("Inv 12/28 free", UNEVEN, 500));
	}

	@Test
	public void theSameNumberOfDigitsReservesTheSameWidthAtEveryCount()
	{
		final int reserved = InventoryLine.reservedWidth("Inv 11/28 free", UNEVEN, 0);
		for (int free = 10; free <= 28; free++)
		{
			assertEquals(free + "/28", reserved,
				InventoryLine.reservedWidth("Inv " + free + "/28 free", UNEVEN, 0));
		}
	}

	/** With a real font: at every count the label, the gap and the icons fit the reserved width. */
	@Test
	public void withARealFontTheLabelNeverReachesTheIcons()
	{
		final FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
			.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		for (int free = 0; free <= 28; free++)
		{
			final String label = InventoryFill.label(free);
			final int reserved = InventoryLine.reservedWidth(label, metrics::charWidth, metrics.stringWidth(label));
			assertTrue(label, reserved >= metrics.stringWidth(label));
		}
		assertTrue(InventoryLine.LABEL_GAP >= SkillingBadgeRenderer.PAD_X);
	}
}
