package com.vinnyvangogh.combatvitals;

import java.awt.Rectangle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

/**
 * Where the two vertical bars beside the side panel (the "pillars") go.
 *
 * The placement follows RuneLite's own Status Bars plugin (BSD-2-Clause,
 * runelite-client {@code net.runelite.client.plugins.statusbars}:
 * {@code StatusBarsOverlay#render} and {@code Viewport}), so the bars sit
 * exactly where a RuneLite user expects them: the first visible of four
 * side-panel widgets decides the layout, and each has fixed offsets from
 * that widget's canvas location. The numbers are the same; the code is
 * this plugin's own.
 *
 * Alongside Status Bars: while it is off, or has both its bars set to
 * Disabled, ours take the two pillars. While it draws either bar, both of
 * ours stack to the left of its leftmost bar, beside it, out over the game
 * view where there is always room; stacking rather than taking a free
 * pillar keeps our two bars together and in order. They never go over the
 * inventory and never overlap its bars.
 *
 * Pure: a widget's bounds and Status Bars' settings in, two rectangles out.
 */
final class SidePillars
{
	/** Status Bars' {@code BarRenderer.DEFAULT_WIDTH}, and the width of ours. */
	static final int WIDTH = 20;

	/** Status Bars' bar heights: the classic layouts, and resizable with the panel along the bottom. */
	static final int HEIGHT = 252;
	static final int RESIZED_BOTTOM_HEIGHT = 272;

	/** Space between our bar and a Status Bars bar beside it. */
	static final int GAP = 2;

	/** Status Bars' {@code RESIZED_BOTTOM_OFFSET_X} and {@code _Y}. */
	private static final int RESIZED_BOTTOM_OFFSET_X = 10;
	private static final int RESIZED_BOTTOM_OFFSET_Y = 12;

	/** The four side-panel layouts, in the order Status Bars checks them, with its offsets. */
	@Getter
	@AllArgsConstructor
	enum Viewport
	{
		RESIZED_BOX(InterfaceID.ToplevelOsrsStretch.SIDE_CONTAINER, 20, -4, 0, -4),
		RESIZED_BOTTOM(InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND, 61, -12, 35, -12),
		FIXED(InterfaceID.Toplevel.SIDE_PANELS, 20, -4, 0, -4),
		FIXED_BANK(InterfaceID.Bankside.ITEMS, 20, -4, 0, -4);

		private final int widget;
		private final int leftX;
		private final int leftY;
		private final int rightX;
		private final int rightY;
	}

	/** Status Bars as this plugin sees it: on or off, which pillars it uses, and its width setting. */
	@AllArgsConstructor
	static final class StatusBars
	{
		static final StatusBars OFF = new StatusBars(false, false, false, WIDTH);

		final boolean enabled;
		final boolean left;
		final boolean right;
		final int width;

		boolean usesAny()
		{
			return enabled && (left || right);
		}
	}

	/** Our two bars. */
	@Getter
	@AllArgsConstructor
	static final class Layout
	{
		private final Rectangle left;
		private final Rectangle right;

		Rectangle get(PillarSide side)
		{
			return side == PillarSide.LEFT ? left : right;
		}
	}

	private SidePillars()
	{
	}

	/**
	 * @param widgetX     the side-panel widget's canvas x
	 * @param widgetY     its canvas y
	 * @param widgetWidth its width
	 */
	static Layout layout(Viewport viewport, int widgetX, int widgetY, int widgetWidth, StatusBars statusBars)
	{
		if (!statusBars.usesAny())
		{
			// Nothing of Status Bars is drawn, so its width setting is moot:
			// the pillars as they are at the default width.
			final Rectangle[] free = statusBarSlots(viewport, widgetX, widgetY, widgetWidth, StatusBars.OFF);
			return new Layout(free[0], free[1]);
		}

		final Rectangle[] slots = statusBarSlots(viewport, widgetX, widgetY, widgetWidth, statusBars);
		final Rectangle left = slots[0];
		final Rectangle right = slots[1];

		// Status Bars is drawing: ours go left of its leftmost bar. When only
		// its right pillar is used, its left slot is free, and in the classic
		// layouts that slot is the edge to stack from.
		final boolean bottom = viewport == Viewport.RESIZED_BOTTOM;
		final int edge = statusBars.left ? left.x : bottom ? right.x : left.x + WIDTH + GAP;
		final int ourRight = edge - WIDTH - GAP;
		return new Layout(ours(ourRight - WIDTH - GAP, left), ours(ourRight, left));
	}

	/**
	 * Where Status Bars draws its left and right bars. Its width setting only
	 * applies with the panel along the bottom; the classic layouts always use
	 * the default.
	 */
	static Rectangle[] statusBarSlots(Viewport viewport, int widgetX, int widgetY, int widgetWidth,
		StatusBars statusBars)
	{
		if (viewport == Viewport.RESIZED_BOTTOM)
		{
			final int width = statusBars.enabled ? Math.max(3, Math.min(40, statusBars.width)) : WIDTH;
			final int extra = width - WIDTH;
			final int x = widgetX + RESIZED_BOTTOM_OFFSET_X;
			final int y = widgetY - RESIZED_BOTTOM_OFFSET_Y;
			return new Rectangle[] {
				new Rectangle(x - viewport.leftX - 2 * extra, y - viewport.leftY, width, RESIZED_BOTTOM_HEIGHT),
				new Rectangle(x - viewport.rightX - extra, y - viewport.rightY, width, RESIZED_BOTTOM_HEIGHT),
			};
		}

		return new Rectangle[] {
			new Rectangle(widgetX - viewport.leftX, widgetY - viewport.leftY, WIDTH, HEIGHT),
			new Rectangle(widgetX - viewport.rightX + widgetWidth, widgetY - viewport.rightY, WIDTH, HEIGHT),
		};
	}

	/** One of our bars at {@code x}, at the slot's height. */
	private static Rectangle ours(int x, Rectangle slot)
	{
		return new Rectangle(x, slot.y, WIDTH, slot.height);
	}
}
