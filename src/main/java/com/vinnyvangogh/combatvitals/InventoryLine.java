package com.vinnyvangogh.combatvitals;

import java.util.function.IntUnaryOperator;

/**
 * Sizes the badge's inventory line so the label and the catch icons never
 * meet: the label is measured with every digit at the font's widest digit,
 * and a gap is kept before the icons.
 *
 * Pure: a label and a font's character widths in, pixels out.
 */
final class InventoryLine
{
	/** Pixels always kept between the label and the first catch icon. */
	static final int LABEL_GAP = 6;

	private InventoryLine()
	{
	}

	/**
	 * The label's width with each digit as wide as the font's widest, never
	 * less than its measured width.
	 *
	 * @param charWidth a character's advance in the font, {@code FontMetrics#charWidth(int)}
	 * @param measured  the label's own width, {@code FontMetrics#stringWidth}
	 */
	static int reservedWidth(String label, IntUnaryOperator charWidth, int measured)
	{
		int widestDigit = 0;
		for (char digit = '0'; digit <= '9'; digit++)
		{
			widestDigit = Math.max(widestDigit, charWidth.applyAsInt(digit));
		}

		int width = 0;
		for (int i = 0; i < label.length(); i++)
		{
			final char c = label.charAt(i);
			width += Character.isDigit(c) ? widestDigit : charWidth.applyAsInt(c);
		}
		return Math.max(width, measured);
	}
}
