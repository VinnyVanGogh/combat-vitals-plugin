package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.Value;

/**
 * Redraws the game's own overhead health bar for every player and NPC that
 * shows one, while "Hide game health bar" has blanked the game's sprites.
 * Those sprites are global (docs/native-hp-bar.md): blanking them hides every
 * standard bar on screen, so every bar hidden has to be drawn back, not only
 * the target's (Board's in-game test, 2026-11-06).
 *
 * Geometry, from the client's {@code drawActor2d} as deobfuscated in
 * jbx5/devious-client {@code UserComparator10.java} (commit 36c5a58,
 * L107-L229):
 * <ul>
 * <li>The actor is projected at its height plus 15 (L145). RuneLite's
 * {@code Actor#getLogicalHeight} is that height; an NPC whose definition
 * sets its own bar height (cache opcode 124) is drawn a little off.</li>
 * <li>A running offset starts at -2 (L123) and grows by 12 when the actor is
 * talking (L137), then by the bar's 5 px height (L198/L216): the bar's top
 * is 3 px above the projected point, 15 px while chat is up.</li>
 * <li>Centred: {@code x - (width >> 1)} (L199/L217).</li>
 * <li>Width: the bar definition's width, which is what
 * {@code Actor#getHealthScale} returns (RuneLite's mixin reads the same
 * {@code width} field), 30 px for most actors. Fill: {@code ratio * width / scale}
 * of it in green, the rest red, as the client's no-sprite fallback draws
 * (L215-L223).</li>
 * </ul>
 *
 * Not reproduced: a second bar stacked on the first (the API reports one),
 * Interface Styles' HD look, and the special head bars (shields, blood,
 * heat, ice), which the hider leaves alone in the first place.
 *
 * Pure: numbers in, boxes out.
 */
final class GameHealthBars
{
	static final int HEIGHT = 5;

	/** Above the actor's logical height, where the client projects its bars. */
	static final int ABOVE_HEIGHT = 15;

	/** How far the bar moves up while the actor's overhead chat is up. */
	static final int CHAT_LIFT = 12;

	/** The client's own fallback colours, 0x00FF00 and 0xFF0000. */
	static final Color FILL = new Color(0x00FF00);
	static final Color EMPTY = new Color(0xFF0000);

	/** Widest bar the game draws (StandardHealth160); a larger scale is clamped to it. */
	private static final int MAX_WIDTH = 160;

	/** One actor's reading, as the overlay takes it from the client. */
	interface Access<A>
	{
		int ratio(A actor);

		int scale(A actor);

		/** Overhead chat is up, which lifts the bar. */
		boolean talking(A actor);

		/** The actor projected at its logical height plus {@link #ABOVE_HEIGHT}: {x, y}, or null off screen. */
		int[] anchor(A actor);
	}

	/** One bar to draw: its box, and how many pixels of it are green. */
	@Value
	static class Bar
	{
		Rectangle box;
		int filled;
	}

	private GameHealthBars()
	{
	}

	/** Whether the client is showing a bar: it has a ratio and a scale. */
	static boolean shown(int ratio, int scale)
	{
		return ratio >= 0 && scale > 0;
	}

	/** The bar's width in pixels for a scale: the scale itself, up to the widest standard bar. */
	static int width(int scale)
	{
		return Math.max(1, Math.min(MAX_WIDTH, scale));
	}

	/** Green pixels: the ratio's share of the width, never 0 while there is any health left. */
	static int filled(int ratio, int scale, int width)
	{
		if (ratio <= 0 || scale <= 0)
		{
			return 0;
		}
		return Math.max(1, Math.min(width, ratio * width / scale));
	}

	/** The bar's box, centred on the projected point. */
	static Rectangle box(int x, int y, int width, boolean talking)
	{
		final int top = y + 2 - HEIGHT - (talking ? CHAT_LIFT : 0);
		return new Rectangle(x - (width >> 1), top, width, HEIGHT);
	}

	/**
	 * Every bar to draw this frame.
	 *
	 * @param actors  every player and NPC in the scene
	 * @param covered actors the plugin already draws a health readout over,
	 *                which takes the bar's place
	 */
	static <A> List<Bar> plan(Iterable<? extends A> actors, Predicate<? super A> covered, Access<A> access)
	{
		final List<Bar> bars = new ArrayList<>();
		for (A actor : actors)
		{
			final int ratio = access.ratio(actor);
			final int scale = access.scale(actor);
			if (!shown(ratio, scale) || covered.test(actor))
			{
				continue;
			}
			final int[] at = access.anchor(actor);
			if (at == null)
			{
				continue;
			}
			final int width = width(scale);
			bars.add(new Bar(box(at[0], at[1], width, access.talking(actor)), filled(ratio, scale, width)));
		}
		return bars;
	}
}
