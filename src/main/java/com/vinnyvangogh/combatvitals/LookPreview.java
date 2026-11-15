package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The Look tab's preview: the health colours as a bar fading from full to
 * empty, and a small scene with you, a fishing spot and the skilling badge
 * where its position, offset and size put it. Cheap by design: a few
 * rectangles, redrawn only when a setting changes. A sketch of where things
 * go, not the game's own rendering.
 */
final class LookPreview extends JComponent implements SettingControls.Bound
{
	static final int PREVIEW_W = 200;
	static final int PREVIEW_H = 128;
	private static final int BAR_Y = 16;
	private static final int BAR_H = 10;
	private static final int SCENE_Y = 34;
	private static final int SCENE_H = 90;
	/** The preview draws the game's pixels at this fraction, so offsets and sizes fit. */
	private static final double SCALE = 0.4;

	/** What the preview draws, read from the settings on each load. */
	static final class Look
	{
		Color full = Color.GREEN;
		Color mid = Color.YELLOW;
		Color low = Color.RED;
		Color background = Color.BLACK;
		boolean gradient = true;
		BadgePosition position = BadgePosition.OVER_HEAD;
		int offset;
		int scale = 100;
		Color badgeColour = Color.CYAN;
	}

	private final ConfigStore store;
	private Look look = new Look();

	LookPreview(ConfigStore store)
	{
		this.store = store;
		setPreferredSize(new Dimension(PREVIEW_W, PREVIEW_H));
		setToolTipText("A sketch of the health colours and where the skilling badge sits.");
	}

	@Override
	public JComponent view()
	{
		return this;
	}

	@Override
	public void load()
	{
		look = read(store);
		repaint();
	}

	static Look read(ConfigStore store)
	{
		final Look l = new Look();
		l.full = or(store.get("healthFullColour", Color.class), l.full);
		l.mid = or(store.get("healthMidColour", Color.class), l.mid);
		l.low = or(store.get("healthLowColour", Color.class), l.low);
		l.background = or(store.get("barBackground", Color.class), l.background);
		l.gradient = or(store.get("colourGradient", boolean.class), l.gradient);
		l.position = or(store.get("skillingPosition", BadgePosition.class), l.position);
		l.offset = or(store.get("skillingOffset", int.class), l.offset);
		l.scale = or(store.get("skillingScale", int.class), l.scale);
		l.badgeColour = or(store.get("skillingFishingColour", Color.class), l.badgeColour);
		return l;
	}

	private static <T> T or(T value, T fallback)
	{
		return value == null ? fallback : value;
	}

	/** The colour at {@code fraction} of full health: full to half to empty, or full only without the fade. */
	static Color colourAt(Look l, double fraction)
	{
		if (!l.gradient)
		{
			return l.full;
		}
		return fraction >= 0.5 ? mix(l.mid, l.full, (fraction - 0.5) * 2) : mix(l.low, l.mid, fraction * 2);
	}

	private static Color mix(Color from, Color to, double t)
	{
		return new Color(
			(int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
			(int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
			(int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
	}

	/** Where the badge goes in the scene, in the preview's pixels. */
	static Rectangle badge(Look l)
	{
		final int w = (int) Math.round(110 * SCALE * l.scale / 100.0);
		final int h = (int) Math.round(48 * SCALE * l.scale / 100.0);
		final int lift = (int) Math.round(l.offset * SCALE);
		switch (l.position)
		{
			case FIXED:
				return new Rectangle(6, SCENE_Y + 6, w, h);
			case OVER_SPOT:
				return new Rectangle(spotX() - w / 2, SCENE_Y + 44 - h - lift, w, h);
			case OVER_HEAD:
			default:
				return new Rectangle(playerX() - w / 2, SCENE_Y + 40 - h - lift, w, h);
		}
	}

	private static int playerX()
	{
		return PREVIEW_W / 2 - 30;
	}

	private static int spotX()
	{
		return PREVIEW_W / 2 + 50;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setFont(FontManager.getRunescapeSmallFont());
			paintBar(g);
			paintScene(g);
		}
		finally
		{
			g.dispose();
		}
	}

	private void paintBar(Graphics2D g)
	{
		g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g.drawString("Full to empty", 0, BAR_Y - 3);
		g.setColor(look.background);
		g.fillRect(0, BAR_Y, PREVIEW_W, BAR_H);
		for (int x = 0; x < PREVIEW_W; x++)
		{
			g.setColor(colourAt(look, 1.0 - (double) x / (PREVIEW_W - 1)));
			g.drawLine(x, BAR_Y + 1, x, BAR_Y + BAR_H - 2);
		}
	}

	private void paintScene(Graphics2D g)
	{
		g.setColor(ColorScheme.DARKER_GRAY_COLOR.darker());
		g.fillRect(0, SCENE_Y, PREVIEW_W, SCENE_H);
		// You: a head and a body.
		g.setColor(new Color(214, 190, 150));
		g.fillOval(playerX() - 5, SCENE_Y + 46, 10, 10);
		g.setColor(new Color(90, 110, 160));
		g.fillRect(playerX() - 6, SCENE_Y + 56, 12, 22);
		// The fishing spot: ripples.
		g.setColor(new Color(120, 170, 220));
		g.drawOval(spotX() - 14, SCENE_Y + 64, 28, 10);
		g.drawOval(spotX() - 8, SCENE_Y + 66, 16, 6);
		paintBadge(g, badge(look));
	}

	private void paintBadge(Graphics2D g, Rectangle r)
	{
		g.setColor(new Color(20, 20, 20, 220));
		g.fillRect(r.x, r.y, r.width, r.height);
		g.setColor(look.badgeColour);
		g.fillRect(r.x, r.y, r.width, Math.max(3, r.height / 4));
		g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
		g.setColor(Color.WHITE);
		g.fillRect(r.x + 3, r.y + r.height / 2, Math.max(2, r.width * 2 / 3), 2);
	}
}
