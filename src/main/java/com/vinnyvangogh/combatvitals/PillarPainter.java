package com.vinnyvangogh.combatvitals;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;

/**
 * Paints one side pillar so it sits natively beside RuneLite's Status Bars:
 * a dark frame with the fill drawn inside it, the icon at the top and the
 * number under it.
 *
 * The look follows Status Bars' {@code BarRenderer} (BSD-2-Clause,
 * Copyright (c) 2019, Jos and Rheon, runelite-client
 * {@code net.runelite.client.plugins.statusbars}): its translucent black
 * background, a fill inset by one pixel, the icon 4 px from the top, the
 * number's baseline 35 px down under an icon or 18 px without, and no
 * labels on a bar narrower than 16 px. The numbers are the same; the code
 * is this plugin's own, and adds a shine, tick marks, a level at the foot
 * and an idle dim.
 *
 * Needs no client: a graphics context, a box and a {@link Pillar} in.
 */
final class PillarPainter
{
	/** Status Bars' {@code BACKGROUND}. */
	static final Color BACKGROUND = new Color(0, 0, 0, 150);

	/** How opaque a pillar is once the activity stops. */
	static final float IDLE_ALPHA = 0.45f;

	/** Status Bars' icon offset and the number's baselines with and without an icon. */
	private static final int ICON_TOP = 4;
	private static final int NUMBER_UNDER_ICON = 35;
	private static final int NUMBER_ALONE = 18;
	private static final int BOTTOM_BASELINE = 4;

	/** Status Bars' {@code MIN_ICON_AND_COUNTER_WIDTH}. */
	private static final int MIN_LABEL_WIDTH = 16;

	/** Above this many divisions the marks are slots across the track, below it short quarter marks. */
	private static final int SLOT_TICKS_FROM = 8;
	private static final int QUARTER_TICK = 4;

	private static final Color FRAME = new Color(0, 0, 0, 220);
	private static final Color SHINE = new Color(255, 255, 255, 60);
	private static final Color EDGE = new Color(255, 255, 255, 110);
	private static final Color SLOT_TICK = new Color(0, 0, 0, 110);
	private static final Color QUARTER = new Color(255, 255, 255, 90);
	private static final Color SHADOW = Color.BLACK;

	private PillarPainter()
	{
	}

	/**
	 * @param flatBackground the bar background setting, for the flat style
	 */
	static void paint(Graphics2D graphics, Rectangle box, Pillar pillar, PillarStyle style, PillarLabels labels,
		Color flatBackground)
	{
		final Composite composite = graphics.getComposite();
		final Paint paint = graphics.getPaint();
		final Font font = graphics.getFont();
		final Shape clip = graphics.getClip();
		if (pillar.isIdle())
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, IDLE_ALPHA));
		}
		try
		{
			if (style == PillarStyle.FLAT)
			{
				Bars.up(graphics, box, pillar.getFraction(), pillar.getColour(), flatBackground);
			}
			else
			{
				framed(graphics, box, pillar);
			}
			if (box.width >= MIN_LABEL_WIDTH)
			{
				// Kept inside the bar: a shadow pixel past its edge would sit on Status Bars' bar.
				graphics.clip(box);
				labels(graphics, box, pillar, labels);
			}
		}
		finally
		{
			graphics.setClip(clip);
			graphics.setComposite(composite);
			graphics.setPaint(paint);
			graphics.setFont(font);
		}
	}

	/** The Status Bars style: background, inset fill with its shine, ticks, frame. */
	private static void framed(Graphics2D graphics, Rectangle box, Pillar pillar)
	{
		graphics.setColor(BACKGROUND);
		graphics.fillRect(box.x, box.y, box.width, box.height);

		final Rectangle inner = new Rectangle(box.x + 1, box.y + 1, box.width - 2, box.height - 2);
		final int filled = (int) Math.round(inner.height * Math.max(0, Math.min(1, pillar.getFraction())));
		if (filled > 0)
		{
			fill(graphics, inner, filled, pillar.getColour());
		}

		ticks(graphics, inner, pillar.getDivisions());

		graphics.setColor(FRAME);
		graphics.drawRect(box.x, box.y, box.width - 1, box.height - 1);
	}

	/** Lit from the left, darker to the right, a soft shine down the left and a bright top edge. */
	private static void fill(Graphics2D graphics, Rectangle inner, int filled, Color colour)
	{
		final int top = inner.y + inner.height - filled;
		graphics.setPaint(new GradientPaint(inner.x, 0, colour.brighter(), inner.x + inner.width, 0,
			colour.darker()));
		graphics.fillRect(inner.x, top, inner.width, filled);

		graphics.setColor(SHINE);
		graphics.fillRect(inner.x + 1, top, 2, filled);
		graphics.setColor(EDGE);
		graphics.drawLine(inner.x, top, inner.x + inner.width - 1, top);
	}

	/** One dark line per slot boundary, or short light marks at each quarter. */
	private static void ticks(Graphics2D graphics, Rectangle inner, int divisions)
	{
		final boolean slots = divisions > SLOT_TICKS_FROM;
		graphics.setColor(slots ? SLOT_TICK : QUARTER);
		final int right = inner.x + inner.width - 1;
		for (int y : PillarText.ticks(inner.y, inner.height, divisions))
		{
			if (slots)
			{
				graphics.drawLine(inner.x, y, right, y);
			}
			else
			{
				graphics.drawLine(inner.x, y, inner.x + QUARTER_TICK - 1, y);
				graphics.drawLine(right - QUARTER_TICK + 1, y, right, y);
			}
		}
	}

	/** The icon at the top, the number under it and the level at the foot, as the setting allows. */
	private static void labels(Graphics2D graphics, Rectangle box, Pillar pillar, PillarLabels labels)
	{
		final BufferedImage icon = labels.icon() ? pillar.getIcon() : null;
		if (icon != null)
		{
			icon(graphics, box, icon);
		}
		if (!labels.number())
		{
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		centred(graphics, box, pillar.getNumber(), box.y + (icon != null ? NUMBER_UNDER_ICON : NUMBER_ALONE));
		centred(graphics, box, pillar.getBottom(), box.y + box.height - BOTTOM_BASELINE);
	}

	/** Centred, 4 px from the top, scaled down only if wider than the bar. */
	private static void icon(Graphics2D graphics, Rectangle box, BufferedImage icon)
	{
		final int room = box.width - 2;
		final int width = Math.min(icon.getWidth(), room);
		final int height = icon.getHeight() * width / Math.max(1, icon.getWidth());
		graphics.drawImage(icon, box.x + (box.width - width) / 2, box.y + ICON_TOP, width, height, null);
	}

	/**
	 * White with a drop shadow, as Status Bars' counter. "82%" is wider than
	 * a 20 px bar in the small font, so a percentage that does not fit drops
	 * its sign; the hover text still says it in full.
	 */
	private static void centred(Graphics2D graphics, Rectangle box, String number, int baseline)
	{
		if (number == null)
		{
			return;
		}
		final FontMetrics metrics = graphics.getFontMetrics();
		final String text = metrics.stringWidth(number) > box.width - 2 && number.endsWith("%")
			? number.substring(0, number.length() - 1) : number;
		final int x = box.x + (box.width - metrics.stringWidth(text)) / 2;
		graphics.setColor(SHADOW);
		graphics.drawString(text, x + 1, baseline + 1);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x, baseline);
	}
}
