package com.vinnyvangogh.combatvitals;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Takes the game's own overhead chat line off an actor while the overlay
 * draws its copy above the readout, so only one line is on screen.
 *
 * The game's line is hidden by swapping the actor's text for a blank, the way
 * the base client's Chat Filter blanks a filtered message. The countdown is
 * left alone, so the message lasts exactly as long as the game would have
 * shown it, and the stacking of the bar and head icons does not shift.
 * Releasing writes the text back and the game carries on drawing it for
 * whatever time is left.
 *
 * Never drops a message: a hold is let go the moment it is no longer wanted,
 * and if the game writes a new message over the blank, the new one wins and
 * the old hold is forgotten rather than restored over it. Must only be used on
 * the client thread.
 *
 * @param <A> the actor type; {@code Actor} in the client, anything in tests
 */
final class OverheadChatHolder<A>
{
	/** What the game's line is swapped for. Draws nothing. */
	static final String BLANK = " ";

	/** The three actor calls this needs, so it can be tested without a client. */
	interface Access<A>
	{
		String text(A actor);

		int cyclesRemaining(A actor);

		void setText(A actor, String text);
	}

	private final Access<A> access;

	/** The real text of every actor whose line is hidden. Identity: actors have no value equality. */
	private final Map<A, String> held = new IdentityHashMap<>();

	OverheadChatHolder(Access<A> access)
	{
		this.access = access;
	}

	/**
	 * The actor's chat line as the overlay should see it: the held text while
	 * the game's copy is hidden, otherwise whatever the game is showing.
	 *
	 * @return the text, or null when no chat is up
	 */
	String chat(A actor)
	{
		final String mine = held.get(actor);
		if (mine != null && stillHeld(actor))
		{
			return mine;
		}

		final String text = access.text(actor);
		return OverheadChat.showing(text, access.cyclesRemaining(actor)) ? text : null;
	}

	/**
	 * Brings the holds in line with the latest frame: hides the game's line
	 * for every covered actor that is talking, and gives it back to every
	 * other actor held so far.
	 *
	 * @param covered actors whose readout covered their chat line last frame
	 */
	void update(Collection<A> covered)
	{
		for (Iterator<Map.Entry<A, String>> it = held.entrySet().iterator(); it.hasNext(); )
		{
			final Map.Entry<A, String> entry = it.next();
			final A actor = entry.getKey();
			if (!stillHeld(actor))
			{
				// Timed out, or the game wrote a newer message over the blank.
				it.remove();
			}
			else if (!covered.contains(actor))
			{
				access.setText(actor, entry.getValue());
				it.remove();
			}
		}

		for (A actor : covered)
		{
			if (held.containsKey(actor))
			{
				continue;
			}

			final String text = access.text(actor);
			if (OverheadChat.showing(text, access.cyclesRemaining(actor)))
			{
				held.put(actor, text);
				access.setText(actor, BLANK);
			}
		}
	}

	/** Gives every held line back to the game. */
	void releaseAll()
	{
		for (Map.Entry<A, String> entry : held.entrySet())
		{
			if (stillHeld(entry.getKey()))
			{
				access.setText(entry.getKey(), entry.getValue());
			}
		}
		held.clear();
	}

	boolean isHolding(A actor)
	{
		return held.containsKey(actor) && stillHeld(actor);
	}

	/**
	 * Our blank is still in place and the game's countdown has not run out. A
	 * different text means the game has posted a new message since.
	 */
	private boolean stillHeld(A actor)
	{
		return held.containsKey(actor)
			&& BLANK.equals(access.text(actor))
			&& access.cyclesRemaining(actor) > 0;
	}
}
