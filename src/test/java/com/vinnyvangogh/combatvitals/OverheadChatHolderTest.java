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
		assertEquals("Hello", holder.chat(opponent));
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
		assertNull(holder.chat(expired));
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

		assertNull(holder.chat(opponent));
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

		// The copy shows the new message, not the stale one.
		assertEquals("Second", holder.chat(opponent));

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
		assertEquals("Second", holder.chat(opponent));

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
	public void chatWithoutAHoldIsWhatTheGameShows()
	{
		assertEquals("Hello", holder.chat(opponent));
		opponent.tick(150);
		assertNull(holder.chat(opponent));
	}
}
