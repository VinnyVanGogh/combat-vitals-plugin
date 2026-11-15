package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** One of the Live section's bars: filled to its fraction in its colour, the words centred over it. */
final class MeterBar extends JLabel
{
	static final int HEIGHT = 16;
	private static final Color TRACK = ColorScheme.DARKER_GRAY_COLOR;

	private LiveVitals.Bar bar;

	MeterBar()
	{
		setFont(FontManager.getRunescapeSmallFont());
		setForeground(Color.WHITE);
		setHorizontalAlignment(SwingConstants.CENTER);
		setOpaque(false);
	}

	/** Shows {@code next}; false when it is the bar already showing, so nothing is redrawn. */
	boolean set(LiveVitals.Bar next)
	{
		if (Objects.equals(next, bar))
		{
			return false;
		}
		bar = next;
		setText(next == null ? "" : next.text);
		repaint();
		return true;
	}

	LiveVitals.Bar bar()
	{
		return bar;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(10, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final int w = getWidth();
		final int h = getHeight() - 2;
		g.setColor(TRACK);
		g.fillRect(0, 1, w, h);
		if (bar != null && bar.fraction > 0)
		{
			g.setColor((bar.colour == null ? SkillColours.DEFAULT : bar.colour).darker());
			g.fillRect(0, 1, (int) Math.round(w * Math.min(1.0, bar.fraction)), h);
		}
		super.paintComponent(g);
	}
}
