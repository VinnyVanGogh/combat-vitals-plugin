package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

/**
 * A rounded box that groups what is in it, with an optional coloured bar down
 * its left edge, and a gap under it before the next card. A column inside, so
 * it stacks its rows left-aligned.
 *
 * <p>After GIM Utilities' {@code GroupIronmanPanel.section()}: a DARKER_GRAY
 * column with 6 px of padding on the panel's DARK_GRAY, here with rounded
 * corners and the accent bar.
 */
class Card extends VitalsSidePanel.Column
{
	static final int ACCENT_WIDTH = 3;

	private final Color fill;
	private Color accent;
	private final int gapBelow;

	/**
	 * @param fill     the box's colour ({@link PanelStyle#CARD}, or {@link PanelStyle#INSET} inside a card)
	 * @param accent   the left bar's colour, or null for none
	 * @param pad      padding inside the box
	 * @param gapBelow space under the box, outside it
	 */
	Card(Color fill, Color accent, int pad, int gapBelow)
	{
		this.fill = fill;
		this.accent = accent;
		this.gapBelow = gapBelow;
		setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 0, gapBelow, 0),
			new EmptyBorder(pad, pad + (accent == null ? 0 : ACCENT_WIDTH), pad, pad)));
	}

	/** A card on the panel's background, accented or not, with the usual padding and gap. */
	static Card of(Color accent)
	{
		return new Card(PanelStyle.CARD, accent, PanelStyle.M, PanelStyle.M);
	}

	/** A box inside a card. */
	static Card inset()
	{
		return new Card(PanelStyle.INSET, null, PanelStyle.S, 0);
	}

	/**
	 * Recolours the bar, e.g. to the skill being trained; false when unchanged.
	 * Only a card made with a bar has room for one.
	 */
	boolean accent(Color colour)
	{
		if (accent == null || colour == null || colour.equals(accent))
		{
			return false;
		}
		accent = colour;
		repaint();
		return true;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final int h = getHeight() - gapBelow;
		if (h <= 0 || getWidth() <= 0)
		{
			return;
		}
		final Graphics2D g2 = PanelStyle.smooth(g);
		try
		{
			final Shape box = new RoundRectangle2D.Float(0, 0, getWidth(), h, PanelStyle.ARC, PanelStyle.ARC);
			g2.setColor(fill);
			g2.fill(box);
			if (accent != null)
			{
				g2.clip(box);
				g2.setColor(accent);
				g2.fillRect(0, 0, ACCENT_WIDTH, h);
			}
		}
		finally
		{
			g2.dispose();
		}
	}
}
