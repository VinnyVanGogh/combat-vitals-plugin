package com.vinnyvangogh.combatvitals;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
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

	/** The eight one-pixel offsets the label's outline is drawn at. */
	private static final int[][] OUTLINE = {
		{-1, -1}, {0, -1}, {1, -1},
		{-1, 0}, {1, 0},
		{-1, 1}, {0, 1}, {1, 1},
	};
	private static final int[][] FACE = {{0, 0}};

	/** A percent's sign beside full-size digits, raised to their top like a superscript. */
	static final float SIGN_SIZE = 0.7f;

	/**
	 * How far a percent may reach past each side of the bar before it is
	 * condensed: no further than the gap to the next bar, so it never
	 * touches Status Bars' bar or the side panel.
	 */
	static final int OVERHANG = SidePillars.GAP;

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
				// Kept inside the bar and its gap: a shadow pixel past that would sit on Status Bars' bar.
				graphics.clip(labelBounds(box));
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
	 * White inside a one-pixel black outline, so it reads on any fill: a
	 * drop shadow alone was lost on the thin tan Mining fill (Board's
	 * screenshots, 2026-11-06). A label that does not fit is condensed
	 * sideways rather than cut; a percent is drawn by {@link #percent}.
	 */
	private static void centred(Graphics2D graphics, Rectangle box, String number, int baseline)
	{
		if (number == null)
		{
			return;
		}
		if (isPercent(number))
		{
			percent(graphics, box, number, baseline);
			return;
		}
		final Font font = graphics.getFont();
		final int width = graphics.getFontMetrics().stringWidth(number);
		final int room = box.width - 2;
		final double scale = labelScale(width, room);
		final int x = box.x + (box.width - Math.min(width, room)) / 2;
		outlined(graphics, new Part(number, condensed(font, scale), x, baseline));
		graphics.setFont(font);
	}

	/**
	 * "63%" in the small font is wider than a 20 px bar, and condensing it
	 * whole squished the digits (Board's Woodcutting screenshot,
	 * 2026-11-14). Every skill keeps its sign (Board, 2026-11-09), so the
	 * digits stay full size, the sign is drawn smaller at their top, the
	 * label may reach {@link #OVERHANG} into the gap each side, and only a
	 * label still too wide ("100%") is condensed.
	 */
	private static void percent(Graphics2D graphics, Rectangle box, String number, int baseline)
	{
		final Font font = graphics.getFont();
		final Font sign = signFont(font);
		final String digits = number.substring(0, number.length() - 1);
		final int digitsWidth = graphics.getFontMetrics(font).stringWidth(digits);
		final int width = percentWidth(graphics, number);
		final double scale = labelScale(width, percentRoom(box.width));
		final int x = box.x + (box.width - (int) Math.ceil(width * scale)) / 2;
		final int raise = raise(graphics, font, digits, sign);
		outlined(graphics,
			new Part(digits, condensed(font, scale), x, baseline),
			new Part("%", condensed(sign, scale), x + (int) Math.round(digitsWidth * scale), baseline - raise));
		graphics.setFont(font);
	}

	/** One piece of a label: its text, font and where its baseline starts. */
	private static final class Part
	{
		private final String text;
		private final Font font;
		private final int x;
		private final int y;

		Part(String text, Font font, int x, int y)
		{
			this.text = text;
			this.font = font;
			this.x = x;
			this.y = y;
		}
	}

	/** Every part's outline first, then every face, so no outline covers a neighbouring face. */
	private static void outlined(Graphics2D graphics, Part... parts)
	{
		graphics.setColor(SHADOW);
		draw(graphics, parts, OUTLINE);
		graphics.setColor(Color.WHITE);
		draw(graphics, parts, FACE);
	}

	private static void draw(Graphics2D graphics, Part[] parts, int[][] offsets)
	{
		for (Part part : parts)
		{
			graphics.setFont(part.font);
			for (int[] offset : offsets)
			{
				graphics.drawString(part.text, part.x + offset[0], part.y + offset[1]);
			}
		}
	}

	private static Font condensed(Font font, double scale)
	{
		return scale < 1 ? font.deriveFont(AffineTransform.getScaleInstance(scale, 1)) : font;
	}

	/** How much to narrow a label {@code width} px wide to fit {@code room}: 1 when it already fits. */
	static double labelScale(int width, int room)
	{
		return width > room ? (double) room / width : 1;
	}

	/** "63%", not "%" or "63". */
	static boolean isPercent(String number)
	{
		return number.length() > 1 && number.endsWith("%");
	}

	static Font signFont(Font font)
	{
		return font.deriveFont(font.getSize2D() * SIGN_SIZE);
	}

	/** Full-size digits plus the small sign, in the graphics' current font, before any condensing. */
	static int percentWidth(Graphics2D graphics, String number)
	{
		final Font font = graphics.getFont();
		return graphics.getFontMetrics(font).stringWidth(number.substring(0, number.length() - 1))
			+ graphics.getFontMetrics(signFont(font)).stringWidth("%");
	}

	/** The room a percent's glyphs get: the bar less its outline, plus the overhang each side. */
	static int percentRoom(int boxWidth)
	{
		return boxWidth - 2 + 2 * OVERHANG;
	}

	/** Where a label may paint: the bar, widened by the overhang each side. */
	static Rectangle labelBounds(Rectangle box)
	{
		return new Rectangle(box.x - OVERHANG, box.y, box.width + 2 * OVERHANG, box.height);
	}

	/** How far up the small sign moves so its top lines up with the digits' top. */
	private static int raise(Graphics2D graphics, Font font, String digits, Font sign)
	{
		final FontRenderContext context = graphics.getFontRenderContext();
		final double digitsTop = font.createGlyphVector(context, digits).getVisualBounds().getMinY();
		final double signTop = sign.createGlyphVector(context, "%").getVisualBounds().getMinY();
		return (int) Math.round(signTop - digitsTop);
	}
}
