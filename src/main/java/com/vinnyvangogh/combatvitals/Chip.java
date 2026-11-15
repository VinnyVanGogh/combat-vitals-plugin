package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/**
 * A small rounded pill of text, "Run 64%" or "Pump stops in 97:30": a value
 * that sits beside the bars rather than under them as another line.
 */
final class Chip extends JLabel
{
	Chip()
	{
		setFont(PanelStyle.body());
		setForeground(PanelStyle.SOFT);
		setHorizontalAlignment(SwingConstants.CENTER);
		// The bottom inset is the gap to whatever is under the chip; the pill is drawn above it.
		setBorder(new EmptyBorder(1, PanelStyle.M, 1 + PanelStyle.S, PanelStyle.M));
		setOpaque(false);
	}

	/** Shows the text; false when it is already showing, so nothing is redrawn. */
	boolean show(String text)
	{
		if (text.equals(getText()))
		{
			return false;
		}
		setText(text);
		return true;
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = PanelStyle.smooth(g);
		try
		{
			final int h = getHeight() - 1 - PanelStyle.S;
			g2.setColor(PanelStyle.INSET);
			g2.fillRoundRect(0, 0, getWidth() - 1, h, h, h);
			g2.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
			g2.drawRoundRect(0, 0, getWidth() - 1, h, h, h);
		}
		finally
		{
			g2.dispose();
		}
		super.paintComponent(g);
	}
}
