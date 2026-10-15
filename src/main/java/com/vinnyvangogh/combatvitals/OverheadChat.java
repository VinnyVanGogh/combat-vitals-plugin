package com.vinnyvangogh.combatvitals;

import java.awt.Rectangle;

/**
 * Keeps the actor's overhead chat readable over the overhead readout.
 *
 * The game draws an actor's chat line at the same height the readout uses,
 * and the overlay paints over it. The game's text cannot be moved, so while
 * the readout covers it, the overlay draws its own copy of the line just
 * above the readout and the readout stays where it is.
 *
 * Pure: screen rectangles in, a baseline out, so it is testable without a
 * running game.
 */
final class OverheadChat
{
	/** Pixels left between the bottom of the chat copy and the readout. */
	static final int GAP = 2;

	private OverheadChat()
	{
	}

	/**
	 * @param text            the actor's overhead text
	 * @param cyclesRemaining client ticks before the game stops drawing it
	 * @return whether the game is drawing a chat line over the actor right now
	 */
	static boolean showing(String text, int cyclesRemaining)
	{
		// The text is left behind after it times out, so the countdown is what
		// says whether it is still on screen.
		return text != null && !text.trim().isEmpty() && cyclesRemaining > 0;
	}

	/**
	 * @param readout the readout's box on screen, bar included; not modified
	 * @param chat    the game's chat line's box on screen
	 * @param descent the chat font's descent
	 * @return the baseline to draw the chat copy at, or null when the readout
	 *         does not cover the game's line and no copy should be drawn
	 */
	static Integer baselineAbove(Rectangle readout, Rectangle chat, int descent)
	{
		// Only redraw when the line is actually covered: an uncovered line is
		// already readable, and a copy beside it would show the message twice.
		if (!readout.intersects(chat))
		{
			return null;
		}

		return readout.y - GAP - descent;
	}
}
