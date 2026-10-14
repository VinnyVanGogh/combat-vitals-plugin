package com.vinnyvangogh.combatvitals;

/**
 * Keeps the overhead readout clear of the game's overhead chat.
 *
 * The game draws an actor's chat line at the same height the readout uses,
 * and the overlay paints over it. The game's text cannot be moved, so the
 * readout steps down below the chat line for as long as the chat is showing.
 *
 * Pure: screen coordinates in, pixel shift out, so it is testable without a
 * running game.
 */
final class OverheadChat
{
	/** Pixels left between the bottom of the chat line and the readout. */
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
	 * @param readoutTop  screen y of the top edge of the readout, bar included
	 * @param chatBottom  screen y of the bottom edge of the chat line
	 * @return how many pixels to move the readout down so the chat sits fully
	 *         above it, or 0 if it already does
	 */
	static int shiftBelow(int readoutTop, int chatBottom)
	{
		final int wanted = chatBottom + GAP;
		return readoutTop < wanted ? wanted - readoutTop : 0;
	}
}
