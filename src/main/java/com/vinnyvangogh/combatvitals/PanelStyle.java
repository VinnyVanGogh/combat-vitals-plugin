package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The sidebar's one visual system (Board, 2026-11-14: "make it nice and
 * pretty", "it's a lot"): a four-step spacing scale, two text sizes (the
 * RuneScape small font for body text and the bold one for titles, no new
 * fonts), RuneLite's {@link ColorScheme} greys with its orange as the one
 * accent, and the skill colours the Look tab already sets for the bars.
 * Every card, chip and bar in the panel draws from here.
 */
final class PanelStyle
{
	private PanelStyle()
	{
	}

	/** The spacing scale: between words, rows, inside a card, and between cards. */
	static final int XS = 2;
	static final int S = 4;
	static final int M = 6;
	static final int L = 8;

	/** Corner rounding of cards, chips and bars. */
	static final int ARC = 8;

	/** A card on the panel's DARK_GRAY background. */
	static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	/** A box inside a card: a target, a stat tile, a chip. */
	static final Color INSET = ColorScheme.DARK_GRAY_COLOR;
	/** The one accent: titles, the active tab, a guide's bar. */
	static final Color ACCENT = ColorScheme.BRAND_ORANGE;
	static final Color TEXT = Color.WHITE;
	static final Color SOFT = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR.darker();
	/** A bar's empty part. */
	static final Color TRACK = ColorScheme.MEDIUM_GRAY_COLOR.darker();

	static Font body()
	{
		return FontManager.getRunescapeSmallFont();
	}

	static Font title()
	{
		return FontManager.getRunescapeBoldFont();
	}

	/** A copy of {@code g} with anti-aliased shapes; dispose it after use. */
	static Graphics2D smooth(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return g2;
	}
}
