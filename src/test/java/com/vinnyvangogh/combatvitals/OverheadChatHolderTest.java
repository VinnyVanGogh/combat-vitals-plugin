package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class OverheadChatHolderTest
{
	/** Stands in for an actor: the two fields the client keeps for overhead chat. */
	private static final class FakeActor
	{
		String text;
		int cycles;
		int writes;

		/** Called on every write, as the client posts {@code OverheadTextChanged}; null for none. */
		java.util.function.Consumer<String> onWrite;

		FakeActor(String text, int cycles)
		{
			this.text = text;
			this.cycles = cycles;
		}

		/** The game posting a new message. */
		void say(String message)
		{
			text = message;
			cycles = 150;
		}

		/** The game's countdown running. */
		void tick(int n)
		{
			cycles -= n;
		}

		/** What the game draws over this actor right now, if anything. */
		String gameDraws()
		{
			return OverheadChat.showing(text, cycles) ? text : null;
		}
	}

	private static final OverheadChatHolder.Access<FakeActor> ACCESS = new OverheadChatHolder.Access<FakeActor>()
	{
		@Override
		public String text(FakeActor actor)
		{
			return actor.text;
		}

		@Override
		public int cyclesRemaining(FakeActor actor)
		{
			return actor.cycles;
		}

		@Override
		public void setText(FakeActor actor, String text)
		{
			actor.text = text;
			actor.writes++;
			if (actor.onWrite != null)
			{
				actor.onWrite.accept(text);
			}
		}
	};

	private OverheadChatHolder<FakeActor> holder;
	private FakeActor opponent;

	@Before
	public void setUp()
	{
		holder = new OverheadChatHolder<>(ACCESS);
		opponent = new FakeActor("Hello", 150);
	}

	private static Set<FakeActor> covered(FakeActor actor)
	{
		return Collections.singleton(actor);
	}

	@Test
	public void coveredChatIsTakenOffTheGameButKeptForTheCopy()
	{
		holder.update(covered(opponent));

		assertNull(opponent.gameDraws());
		assertEquals("Hello", holder.heldText(opponent));
		assertTrue(holder.isHolding(opponent));
	}

	@Test
	public void countdownIsLeftToTheGame()
	{
		holder.update(covered(opponent));

		// The message lasts as long as the game would have shown it.
		assertEquals(150, opponent.cycles);
	}

	@Test
	public void uncoveredChatIsNeverTouched()
	{
		holder.update(Collections.emptySet());

		assertEquals("Hello", opponent.gameDraws());
		assertEquals(0, opponent.writes);
	}

	@Test
	public void actorWithNoChatIsNeverTouched()
	{
		final FakeActor silent = new FakeActor(null, 0);
		final FakeActor expired = new FakeActor("Old", 0);

		holder.update(covered(silent));
		holder.update(covered(expired));

		assertEquals(0, silent.writes);
		assertEquals(0, expired.writes);
		assertNull(holder.heldText(expired));
	}

	@Test
	public void readoutMovingClearGivesTheLineBackWithItsTimeLeft()
	{
		holder.update(covered(opponent));
		opponent.tick(40);

		holder.update(Collections.emptySet());

		assertEquals("Hello", opponent.gameDraws());
		assertEquals(110, opponent.cycles);
		assertFalse(holder.isHolding(opponent));
	}

	@Test
	public void releaseAllGivesEveryLineBack()
	{
		final FakeActor self = new FakeActor("Hi back", 150);
		holder.update(covered(opponent));
		holder.update(new HashSet<>(Arrays.asList(opponent, self)));

		holder.releaseAll();

		assertEquals("Hello", opponent.gameDraws());
		assertEquals("Hi back", self.gameDraws());
		assertFalse(holder.isHolding(opponent));
		assertFalse(holder.isHolding(self));
	}

	@Test
	public void expiredHoldIsDroppedNotRestored()
	{
		holder.update(covered(opponent));
		opponent.tick(150);

		assertNull(holder.heldText(opponent));
		holder.update(Collections.emptySet());
		holder.releaseAll();

		// Nothing written back: the game would not be showing it any more.
		assertEquals(OverheadChatHolder.BLANK, opponent.text);
		assertNull(opponent.gameDraws());
	}

	@Test
	public void newMessageWinsOverAHeldOne()
	{
		holder.update(covered(opponent));
		opponent.say("Second");

		// No stale copy: the old hold is forgotten, never drawn over the new one.
		assertNull(holder.heldText(opponent));
		assertEquals("Second", opponent.gameDraws());

		// Releasing must not write the old message over the new one.
		holder.releaseAll();
		assertEquals("Second", opponent.gameDraws());
	}

	@Test
	public void newMessageIsHeldInTurnWhileStillCovered()
	{
		holder.update(covered(opponent));
		opponent.say("Second");

		holder.update(covered(opponent));

		assertNull(opponent.gameDraws());
		assertEquals("Second", holder.heldText(opponent));

		holder.update(Collections.emptySet());
		assertEquals("Second", opponent.gameDraws());
	}

	@Test
	public void steadyCoverWritesOnlyOnce()
	{
		holder.update(covered(opponent));
		holder.update(covered(opponent));
		holder.update(covered(opponent));

		assertEquals(1, opponent.writes);
	}

	@Test
	public void releaseAllWithNothingHeldWritesNothing()
	{
		holder.releaseAll();

		assertEquals(0, opponent.writes);
		assertEquals("Hello", opponent.gameDraws());
	}

	@Test
	public void noCopyWithoutAHold()
	{
		// With the option off nothing is held, so the overlay draws no copy
		// and the game's own line is the only one (Board, 2026-11-04: it
		// showed twice).
		assertNull(holder.heldText(opponent));
		assertEquals("Hello", opponent.gameDraws());
	}

	// --- Held at the source: the game's line is blanked inside the text
	// change itself, before any frame can draw it. -------------------------

	/** An actor whose text setter posts the change back, as the client's does. */
	private final class PostingActor
	{
		final FakeActor actor = new FakeActor(null, 0);
		int posts;

		PostingActor()
		{
			actor.onWrite = this::posted;
		}

		/** The game posting a message: text first, then the countdown. */
		void say(String message)
		{
			actor.text = message;
			posted(message);
			actor.cycles = 150;
		}

		void posted(String text)
		{
			posts++;
			holder.hold(actor, text);
		}
	}

	@Test
	public void messageUnderTheReadoutIsNeverDrawnByTheGame()
	{
		final PostingActor me = new PostingActor();

		me.say("Sup");

		// Straight after the change, before any frame: the game has a blank.
		assertNull(me.actor.gameDraws());
		assertEquals("Sup", holder.heldText(me.actor));
		assertTrue(holder.isHolding(me.actor));
		// The blank posted back once and was ignored, not held in turn.
		assertEquals(2, me.posts);
		assertEquals(1, me.actor.writes);
	}

	@Test
	public void heldAtTheSourceSurvivesTheNextFrameUpdate()
	{
		final PostingActor me = new PostingActor();

		me.say("Sup");
		holder.update(covered(me.actor));

		assertNull(me.actor.gameDraws());
		assertEquals("Sup", holder.heldText(me.actor));
		assertEquals("countdown untouched", 150, me.actor.cycles);
	}

	@Test
	public void secondMessageReplacesTheFirstWithoutAFrameOfEither()
	{
		final PostingActor me = new PostingActor();

		me.say("Sup");
		me.say("Bye");

		assertNull(me.actor.gameDraws());
		assertEquals("Bye", holder.heldText(me.actor));

		// Given back, it is the newest message that returns.
		holder.update(Collections.emptySet());
		assertEquals("Bye", me.actor.gameDraws());
	}

	@Test
	public void heldMessageExpiresWithTheGamesCountdown()
	{
		holder.hold(opponent, "Hello");
		opponent.tick(150);

		assertNull(holder.heldText(opponent));
		holder.update(covered(opponent));
		assertFalse(holder.isHolding(opponent));
	}

	@Test
	public void blankOrEmptyTextIsNeverHeld()
	{
		final FakeActor quiet = new FakeActor(null, 0);
		holder.hold(quiet, null);
		holder.hold(quiet, "");
		holder.hold(quiet, OverheadChatHolder.BLANK);

		assertFalse(holder.isHolding(quiet));
		assertEquals(0, quiet.writes);
	}
}
