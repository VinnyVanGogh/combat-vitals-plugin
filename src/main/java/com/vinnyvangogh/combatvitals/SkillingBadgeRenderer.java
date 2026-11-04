package com.vinnyvangogh.combatvitals;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * Draws the skilling badge, for whichever overlay places it: over your
 * head, over the spot, or fixed on screen. The badge is drawn with its
 * bottom centre at the origin of the current transform, so the caller
 * translates to the anchor and scales for the size setting first.
 *
 * Rows, bottom up, each one of the same width and each with its own switch:
 * <ol>
 * <li>the inventory line: free or used slots on the fill bar, and on a
 * fishing spot every fish the method can give;</li>
 * <li>the session line: count this session and the hourly rates;</li>
 * <li>the XP line: progress to the next level;</li>
 * <li>the header, in the skill's colour: icon, level, boost, timer; or
 * "Not fishing" in the idle colour once you stop.</li>
 * </ol>
 */
class SkillingBadgeRenderer
{
	/** Space between the text and the edge of the bar behind it. */
	static final int PAD_X = 4;
	static final int PAD_Y = 2;

	/** Space between the skill icon and the header text. */
	private static final int ICON_GAP = 3;

	/** Space between the badge's rows. */
	private static final int ROW_GAP = 1;

	/** Fish the player's level does not land yet are dimmed, with the level they need over them. */
	private static final float LOCKED_FISH_ALPHA = 0.35f;

	/** Height of a catch icon in the inventory line; item sprites are 32 high, so about half. */
	private static final int FISH_ICON = 18;

	private final Client client;
	private final CombatVitalsConfig config;
	private final SkillIconManager skillIconManager;
	private final ItemManager itemManager;
	private final TooltipManager tooltipManager;

	@Inject
	SkillingBadgeRenderer(Client client, CombatVitalsConfig config, SkillIconManager skillIconManager,
		ItemManager itemManager, TooltipManager tooltipManager)
	{
		this.client = client;
		this.config = config;
		this.skillIconManager = skillIconManager;
		this.itemManager = itemManager;
		this.tooltipManager = tooltipManager;
	}

	/**
	 * Draws the badge, scaled by the size setting, with its bottom centre at
	 * ({@code x}, {@code y}).
	 *
	 * @return the badge's box on screen, or null when every row is off
	 */
	Rectangle drawAt(Graphics2D graphics, SkillingReadout readout, int x, int y)
	{
		final Layout layout = layout(graphics, readout);
		if (layout == null)
		{
			return null;
		}

		final double scale = scale();
		paintScaled(graphics, readout, layout, x, y, scale);
		final Rectangle box = layout.box;
		return new Rectangle(x + (int) Math.floor(box.x * scale), y + (int) Math.floor(box.y * scale),
			(int) Math.ceil(box.width * scale), (int) Math.ceil(box.height * scale));
	}

	/**
	 * Draws the badge, scaled, with its top left corner at the origin, for an
	 * overlay that the overlay manager places.
	 *
	 * @return the badge's size on screen, or null when every row is off
	 */
	Dimension drawFromCorner(Graphics2D graphics, SkillingReadout readout)
	{
		final Layout layout = layout(graphics, readout);
		if (layout == null)
		{
			return null;
		}

		final double scale = scale();
		final Rectangle box = layout.box;
		paintScaled(graphics, readout, layout, (int) Math.ceil(-box.x * scale), (int) Math.ceil(-box.y * scale), scale);
		return new Dimension((int) Math.ceil(box.width * scale), (int) Math.ceil(box.height * scale));
	}

	private double scale()
	{
		return Math.max(50, Math.min(200, config.skillingScale())) / 100.0;
	}

	private void paintScaled(Graphics2D graphics, SkillingReadout readout, Layout layout, int x, int y, double scale)
	{
		final AffineTransform saved = graphics.getTransform();
		graphics.translate(x, y);
		graphics.scale(scale, scale);
		try
		{
			paint(graphics, readout, layout);
		}
		finally
		{
			graphics.setTransform(saved);
		}
	}

	/** Every row's box, relative to the badge's bottom centre; null when every row is off. */
	private Layout layout(Graphics2D graphics, SkillingReadout readout)
	{
		final String text = headerText(readout);
		final FontMetrics metrics = graphics.getFontMetrics();
		final BufferedImage icon = skillIconManager.getSkillImage(readout.getSkill(), true);
		final int iconWidth = icon == null ? 0 : icon.getWidth() + ICON_GAP;
		final int headHeight = Math.max(metrics.getAscent() + metrics.getDescent(),
			icon == null ? 0 : icon.getHeight()) + PAD_Y * 2;

		// The lower rows use the small font, so each grows the badge by a short row.
		final FontMetrics small = graphics.getFontMetrics(FontManager.getRunescapeSmallFont());
		final int lineHeight = small.getAscent() + small.getDescent() + PAD_Y * 2;
		final String invLabel = readout.getInventoryLabel();
		final int invWidth = (invLabel == null ? 0 : small.stringWidth(invLabel))
			+ catchesWidth(readout.getCatches(), small);

		final int width = PAD_X * 2 + max(
			readout.isHeader() ? iconWidth + metrics.stringWidth(text) : 0,
			width(small, readout.getXpText()),
			width(small, readout.getSessionText()),
			invWidth);

		final Rows rows = new Rows(-width / 2, width);
		final int invHeight = readout.getCatches().isEmpty() ? lineHeight : Math.max(lineHeight, FISH_ICON + 2);
		final Layout layout = new Layout(text, icon);
		layout.inv = rows.add(readout.hasInventoryLine(), invHeight);
		layout.session = rows.add(readout.getSessionText() != null, lineHeight);
		layout.xp = rows.add(readout.getXpText() != null, lineHeight);
		layout.head = rows.add(readout.isHeader(), headHeight);
		layout.box = rows.box();
		return rows.empty() ? null : layout;
	}

	private void paint(Graphics2D graphics, SkillingReadout readout, Layout layout)
	{
		if (layout.head != null)
		{
			drawHeader(graphics, layout.head, readout, layout.text, layout.icon);
		}

		final Font previous = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeSmallFont());
		try
		{
			drawSmallRows(graphics, readout, layout.xp, layout.session, layout.inv);
		}
		finally
		{
			graphics.setFont(previous);
		}
	}

	private void drawSmallRows(Graphics2D graphics, SkillingReadout readout, Rectangle xp, Rectangle session,
		Rectangle inv)
	{
		if (xp != null)
		{
			Bars.across(graphics, xp, readout.getXp().getFraction(), readout.getColour(), config.barBackground());
			centredText(graphics, xp, readout.getXpText());
		}
		if (session != null)
		{
			Bars.across(graphics, session, 0, readout.getColour(), config.barBackground());
			centredText(graphics, session, readout.getSessionText());
		}
		if (inv != null)
		{
			drawInventoryLine(graphics, inv, readout);
		}
	}

	/** The header's words: the level and boost while working, "Not fishing" once stopped. */
	private static String headerText(SkillingReadout readout)
	{
		if (readout.isIdle())
		{
			return SkillingText.idleHeader(readout.getSkill());
		}

		final int seconds = SkillingText.seconds(readout.getTicksUntilDrop(),
			System.currentTimeMillis() - readout.getTickMillis());
		return SkillingText.text(readout.getSkill().getName(), readout.getReal(), readout.getBoosted(), seconds,
			!readout.isTimerConfirmed(), readout.getSpecPercent(), readout.getSource(), readout.getWarning());
	}

	/**
	 * The top row: filled behind white text in the Bar style, the text itself
	 * in the colour otherwise. The colour is the skill's; the low colour for a
	 * moment after a boost drop; the idle colour once you stop. The fill is the
	 * boost time left while boosted, full otherwise.
	 */
	private void drawHeader(Graphics2D graphics, Rectangle head, SkillingReadout readout, String text,
		BufferedImage icon)
	{
		final boolean warning = readout.getWarning() != SkillingState.Warning.NONE;
		final Color colour = readout.isIdle() ? config.skillingIdleColour()
			: warning ? config.healthLowColour() : readout.getColour();
		final boolean bar = config.overheadStyle() == OverheadStyle.BAR;
		if (bar)
		{
			Bars.across(graphics, head, warning ? 1.0 : boostFraction(readout), colour, config.barBackground());
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int iconWidth = icon == null ? 0 : icon.getWidth() + ICON_GAP;
		final int x = head.x + (head.width - iconWidth - metrics.stringWidth(text)) / 2;
		if (icon != null)
		{
			graphics.drawImage(icon, x, head.y + (head.height - icon.getHeight()) / 2, null);
		}

		OverlayUtil.renderTextLocation(graphics, new Point(x + iconWidth, textBaseline(head, metrics)), text,
			bar ? Color.WHITE : colour);
	}

	private static double boostFraction(SkillingReadout readout)
	{
		if (readout.isIdle() || !readout.isBoosted() || readout.getTicksUntilDrop() < 0 || readout.getCycleLength() <= 0)
		{
			return 1.0;
		}
		return (double) readout.getTicksUntilDrop() / readout.getCycleLength();
	}

	/**
	 * The inventory label on a bar filled as the inventory fills, green while
	 * empty to red when full: the health colours run backwards. Then every
	 * fish the spot's method can give. Either half can be off.
	 */
	private void drawInventoryLine(Graphics2D graphics, Rectangle row, SkillingReadout readout)
	{
		final String label = readout.getInventoryLabel();
		final double fill = label == null ? 0 : readout.getInventoryFill();
		Bars.across(graphics, row, fill,
			InventoryFill.colour(fill, config.healthFullColour(), config.healthMidColour(), config.healthLowColour()),
			config.barBackground());

		final FontMetrics metrics = graphics.getFontMetrics();
		final int baseline = textBaseline(row, metrics);
		int x = row.x + PAD_X;
		if (label != null)
		{
			OverlayUtil.renderTextLocation(graphics, new Point(x, baseline), label, Color.WHITE);
			x += metrics.stringWidth(label);
		}

		final int iconTop = row.y + (row.height - FISH_ICON) / 2;
		for (FishingCatches.Catch c : readout.getCatches())
		{
			x += ICON_GAP;
			x += drawCatch(graphics, c, readout, x, iconTop, baseline, metrics);
		}
	}

	/**
	 * A fish you can catch at full strength; one you cannot yet dimmed, with
	 * "Lv 15" over it in red and "Needs Fishing 15" on hover.
	 *
	 * @return the width the catch took
	 */
	private int drawCatch(Graphics2D graphics, FishingCatches.Catch c, SkillingReadout readout, int x, int top,
		int baseline, FontMetrics metrics)
	{
		final BufferedImage image = itemManager.getImage(c.getFish().getItemId());
		final int width = catchWidth(c, image, metrics);
		final int iconWidth = scaledWidth(image);
		final int iconX = x + (width - iconWidth) / 2;
		if (c.isCatchable())
		{
			graphics.drawImage(image, iconX, top, iconWidth, FISH_ICON, null);
			return width;
		}

		final Composite composite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LOCKED_FISH_ALPHA));
		graphics.drawImage(image, iconX, top, iconWidth, FISH_ICON, null);
		graphics.setComposite(composite);

		final int level = c.getFish().getLevel();
		final String label = SkillingText.lockedLabel(level);
		OverlayUtil.renderTextLocation(graphics,
			new Point(x + (width - metrics.stringWidth(label)) / 2, baseline), label, SkillingText.LOCKED_LEVEL);

		if (hovered(graphics, new Rectangle(x, top, width, FISH_ICON)))
		{
			tooltipManager.add(new Tooltip(SkillingText.lockedTooltip(readout.getSkill().getName(), level)));
		}
		return width;
	}

	/** The mouse is over this box, drawn under the current translate and scale. */
	private boolean hovered(Graphics2D graphics, Rectangle box)
	{
		final Point mouse = client.getMouseCanvasPosition();
		return mouse != null && graphics.getTransform().createTransformedShape(box).contains(mouse.getX(), mouse.getY());
	}

	/** Every catch icon with the gap before it, as {@link #drawInventoryLine} lays them out. */
	private int catchesWidth(List<FishingCatches.Catch> catches, FontMetrics metrics)
	{
		int width = 0;
		for (FishingCatches.Catch c : catches)
		{
			width += ICON_GAP + catchWidth(c, itemManager.getImage(c.getFish().getItemId()), metrics);
		}
		return width;
	}

	/** The icon, or the level written over a locked one if that is wider. */
	private static int catchWidth(FishingCatches.Catch c, BufferedImage image, FontMetrics metrics)
	{
		final int icon = scaledWidth(image);
		return c.isCatchable() ? icon
			: Math.max(icon, metrics.stringWidth(SkillingText.lockedLabel(c.getFish().getLevel())));
	}

	/** An item icon's width at {@link #FISH_ICON} high, keeping its shape. */
	private static int scaledWidth(BufferedImage image)
	{
		return image == null || image.getHeight() <= 0 ? FISH_ICON : image.getWidth() * FISH_ICON / image.getHeight();
	}

	private static void centredText(Graphics2D graphics, Rectangle row, String text)
	{
		final FontMetrics metrics = graphics.getFontMetrics();
		OverlayUtil.renderTextLocation(graphics,
			new Point(row.x + (row.width - metrics.stringWidth(text)) / 2, textBaseline(row, metrics)),
			text, Color.WHITE);
	}

	/** Text vertically centred in a row. */
	private static int textBaseline(Rectangle row, FontMetrics metrics)
	{
		return row.y + (row.height - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
	}

	private static int width(FontMetrics metrics, String text)
	{
		return text == null ? 0 : metrics.stringWidth(text);
	}

	private static int max(int a, int b, int c, int d)
	{
		return Math.max(Math.max(a, b), Math.max(c, d));
	}

	/** One frame's badge: the header's words and icon, and each row's box or null when it is off. */
	private static final class Layout
	{
		private final String text;
		private final BufferedImage icon;
		private Rectangle head;
		private Rectangle xp;
		private Rectangle session;
		private Rectangle inv;
		private Rectangle box;

		Layout(String text, BufferedImage icon)
		{
			this.text = text;
			this.icon = icon;
		}
	}

	/** Rows stacked upward from the origin, each the badge's full width, with a gap between. */
	private static final class Rows
	{
		private final int left;
		private final int width;
		private int top;
		private boolean any;

		Rows(int left, int width)
		{
			this.left = left;
			this.width = width;
		}

		/** @return the row's box, or null when it is off */
		Rectangle add(boolean on, int height)
		{
			if (!on)
			{
				return null;
			}
			final int bottom = any ? top - ROW_GAP : 0;
			final Rectangle row = new Rectangle(left, bottom - height, width, height);
			top = row.y;
			any = true;
			return row;
		}

		boolean empty()
		{
			return !any;
		}

		Rectangle box()
		{
			return new Rectangle(left, top, width, -top);
		}
	}
}
