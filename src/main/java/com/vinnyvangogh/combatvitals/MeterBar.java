package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * One of the Live section's bars: a rounded track filled to its fraction in
 * its colour, the words centred inside it. {@link #THICK} for Hitpoints,
 * Prayer, the target and XP; {@link #THIN} under a session line, with no words.
 */
final class MeterBar extends JLabel
{
	static final int THICK = 20;
	static final int THIN = 5;

	private final int height;
	private LiveVitals.Bar bar;

	MeterBar()
	{
		this(THICK);
	}

	MeterBar(int height)
	{
		this.height = height;
		setFont(PanelStyle.body());
		setForeground(PanelStyle.TEXT);
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
		setText(next == null || height < THICK ? "" : next.text);
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
		return new Dimension(10, height + (height < THICK ? 0 : 2));
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
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
			final int w = getWidth();
			final Shape track = new RoundRectangle2D.Float(0, 1, w, height, height, height);
			g2.setColor(PanelStyle.TRACK);
			g2.fill(track);
			if (bar != null && bar.fraction > 0)
			{
				g2.clip(track);
				g2.setColor((bar.colour == null ? SkillColours.DEFAULT : bar.colour).darker());
				g2.fillRect(0, 1, (int) Math.round(w * Math.min(1.0, bar.fraction)), height);
			}
		}
		finally
		{
			g2.dispose();
		}
		super.paintComponent(g);
	}
}
