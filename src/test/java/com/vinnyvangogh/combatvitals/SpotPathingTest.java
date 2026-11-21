package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import net.runelite.client.game.FishingSpot;
import org.junit.Before;
import org.junit.Test;

public class SpotPathingTest
{
	/** Hands out the queued spots in order, one per pick, and counts the picks. */
	private static final class FakePicker implements SpotPathing.Picker
	{
		final Deque<NextFishingSpot.Spot> next = new ArrayDeque<>();
		int picks;
		FishingSpot lastType;
		String lastMethod;

		@Override
		public NextFishingSpot.Spot pick(FishingSpot type, String method)
		{
			picks++;
			lastType = type;
			lastMethod = method;
			return next.poll();
		}
	}

	/** Where each spot NPC stands; one with no entry is not loaded. */
	private static final class FakeLocator implements SpotPathing.Locator
	{
		final Map<Object, Long> tiles = new IdentityHashMap<>();

		void put(Object npc, int x, int y, int plane)
		{
			tiles.put(npc, SpotPathing.tile(x, y, plane));
		}

		@Override
		public long tile(Object npc)
		{
			final Long tile = tiles.get(npc);
			return tile == null ? SpotPathing.UNKNOWN : tile;
		}
	}

	/** The spot NPC {@link #setUp} starts fishing; its despawning is the spot moving. */
	private static final Object FISHED = new Object();
	private static final Object NPC_1 = new Object();
	private static final Object NPC_2 = new Object();
	private static final NextFishingSpot.Spot SPOT_1 = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3001, 3000, 0, NPC_1);
	private static final NextFishingSpot.Spot SPOT_2 = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3005, 3000, 0, NPC_2);

	private boolean enabled;
	private FakeHintArrow game;
	private NextSpotArrow arrow;
	private FakePicker picker;
	private FakeLocator tiles;
	private SpotPathing pathing;

	@Before
	public void setUp()
	{
		enabled = true;
		game = new FakeHintArrow();
		arrow = new NextSpotArrow(game, () -> enabled, () -> 15);
		picker = new FakePicker();
		tiles = new FakeLocator();
		pathing = new SpotPathing(() -> enabled, arrow);
		pathing.fishingOn(FishingSpot.LOBSTER, "Cage", FISHED);
	}

	/** One game tick: the pather reads where the spots stand, then runs any due search. */
	private void tick()
	{
		pathing.tick(picker, tiles);
	}

	@Test
	public void fishedSpotRelocatingAsTheSameNpcSearches()
	{
		// How the game moves a spot (RuneLite's FishingPlugin keys spots by NPC
		// index and resets on a world location change): same NPC, new tile.
		picker.next.add(SPOT_1);
		tiles.put(FISHED, 3000, 3000, 0);
		tick();
		assertEquals(0, picker.picks);

		tiles.put(FISHED, 3010, 3004, 0);
		tick();

		assertEquals(1, picker.picks);
		assertTrue("the arrow follows the spot NPC", arrow.targets(NPC_1));
	}

	@Test
	public void fishedSpotRelocatingWithinTheGraceAfterTheInteractionEndsSearches()
	{
		picker.next.add(SPOT_1);
		tiles.put(FISHED, 3000, 3000, 0);
		tick();

		// The game ends the interaction, then the spot shows on its new tile.
		pathing.interactionEnded();
		tick();
		tiles.put(FISHED, 3010, 3004, 0);
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void fishedSpotRelocatingLongAfterTheInteractionEndedIsNotTheSpotMoving()
	{
		picker.next.add(SPOT_1);
		tiles.put(FISHED, 3000, 3000, 0);
		tick();

		pathing.interactionEnded();
		for (int i = 0; i < 10; i++)
		{
			tick();
		}
		tiles.put(FISHED, 3010, 3004, 0);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void walkingAwayThenTheSpotRelocatingDoesNotSearch()
	{
		picker.next.add(SPOT_1);
		tiles.put(FISHED, 3000, 3000, 0);
		tick();

		// Walk here: no NPC.
		pathing.turnedTo(null);
		tiles.put(FISHED, 3010, 3004, 0);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void standingStillOnTheSameTileNeverSearches()
	{
		tiles.put(FISHED, 3000, 3000, 0);
		for (int i = 0; i < 20; i++)
		{
			tick();
		}

		assertEquals(0, picker.picks);
	}

	@Test
	public void fishedSpotNotLoadedForATickIsNotAMove()
	{
		tiles.put(FISHED, 3000, 3000, 0);
		tick();
		tiles.tiles.remove(FISHED);
		tick();
		tiles.put(FISHED, 3000, 3000, 0);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void relocateThenDespawnOfTheSameSpotSearchesOnce()
	{
		picker.next.add(SPOT_1);
		tiles.put(FISHED, 3000, 3000, 0);
		tick();
		tiles.put(FISHED, 3010, 3004, 0);
		tick();
		pathing.despawned(FISHED);
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void targetRelocatingBeforeItIsReachedSearchesAgain()
	{
		picker.next.add(SPOT_1);
		picker.next.add(SPOT_2);
		pathing.despawned(FISHED);
		tiles.put(NPC_1, SPOT_1.getX(), SPOT_1.getY(), SPOT_1.getPlane());
		tick();
		tick();
		assertEquals("the target standing where it was picked is not a move", 1, picker.picks);

		tiles.put(NPC_1, 3020, 3020, 0);
		tick();

		assertEquals(2, picker.picks);
		assertSame(SPOT_2, pathing.target());
		assertTrue(arrow.targets(NPC_2));
	}

	@Test
	public void startingToFishResetsTheRememberedTile()
	{
		picker.next.add(SPOT_1);
		tiles.put(FISHED, 3000, 3000, 0);
		tick();

		// A new spot NPC, somewhere else: its first tile is the baseline, not a move.
		final Object other = new Object();
		pathing.fishingOn(FishingSpot.LOBSTER, "Cage", other);
		tiles.put(other, 3050, 3050, 0);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void spotMovePointsTheArrowAtTheNextSpotOfTheSameTypeAndMethod()
	{
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		tick();

		assertEquals(1, game.shows);
		assertSame(NPC_1, game.shown.npc());
		assertSame(FishingSpot.LOBSTER, picker.lastType);
		assertEquals("Cage", picker.lastMethod);
	}

	@Test
	public void theArrowStaysUntilFishingStartsAgain()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		tick();
		assertSame(SPOT_1, pathing.target());

		for (int i = 0; i < 20; i++)
		{
			tick();
		}
		assertSame("still up while walking up to it", SPOT_1, pathing.target());
		assertSame(NPC_1, game.shown.npc());

		pathing.fishingOn(FishingSpot.LOBSTER, "Cage", NPC_1);
		assertNull("cleared once XP starts on it", pathing.target());
		assertNull(game.shown);
		assertEquals(1, game.hides);
	}

	@Test
	public void clickingTheArrowsSpotTakesTheArrowDown()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		tick();

		pathing.turnedTo(NPC_1);

		assertNull(game.shown);
		assertNull(pathing.target());
	}

	@Test
	public void clickingSomethingElseKeepsTheArrow()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		tick();

		pathing.turnedTo(NPC_2);

		assertSame(SPOT_1, pathing.target());
		assertSame(NPC_1, game.shown.npc());
	}

	@Test
	public void aKnownLocationIsPointedAtAsATile()
	{
		picker.next.add(NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 2844, 3429, 0, true, 0, "Catherby"));

		pathing.despawned(FISHED);
		tick();

		assertNull(game.shown.npc());
		assertTrue(arrow.targetsTile(2844, 3429, 0));
	}

	@Test
	public void offDoesNothingEvenOnRightClick()
	{
		enabled = false;
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.requested();
		tick();

		assertEquals(0, picker.picks);
		assertEquals(0, game.shows);
	}

	@Test
	public void theBadgeRightClickAsksForTheNextSpot()
	{
		picker.next.add(SPOT_1);

		pathing.requested();
		tick();

		assertEquals(1, game.shows);
		assertSame(SPOT_1, pathing.target());
	}

	@Test
	public void neverOverridesAForeignArrowOnADespawn()
	{
		game.foreign = true;
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		tick();

		assertEquals(1, picker.picks);
		assertEquals(0, game.shows);
		assertNull(pathing.target());
		assertTrue("the quest's arrow is untouched", game.foreign);
	}

	@Test
	public void spotMovingTwiceInOneTickPointsOnce()
	{
		picker.next.add(SPOT_2);

		pathing.despawned(FISHED);
		pathing.despawned(FISHED);
		tick();
		tick();

		assertEquals(1, picker.picks);
		assertEquals(1, game.shows);
		assertSame(NPC_2, game.shown.npc());
	}

	@Test
	public void targetMovingAgainPointsAtTheLatestSpot()
	{
		picker.next.add(SPOT_1);
		picker.next.add(SPOT_2);

		pathing.despawned(FISHED);
		tick();
		// The spot being walked to moves before the player gets there.
		pathing.despawned(NPC_1);
		tick();

		assertEquals(2, game.shows);
		assertSame(NPC_2, game.shown.npc());
		assertSame(SPOT_2, pathing.target());
	}

	@Test
	public void someOtherNpcDespawningIsIgnored()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		tick();

		pathing.despawned(new Object());
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void noNextSpotClearsAnOldArrow()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		tick();

		pathing.despawned(NPC_1);
		tick();

		assertNull(game.shown);
		assertNull(pathing.target());
	}

	@Test
	public void anArrowThePluginNeverSetIsNeverCleared()
	{
		game.foreign = true;
		pathing.fishingOn(FishingSpot.SHRIMP, "Net", new Object());
		pathing.reset();

		assertEquals(0, game.hides);
		assertTrue(game.foreign);
	}

	@Test
	public void nothingFishedYetMeansNothingToFind()
	{
		final SpotPathing fresh = new SpotPathing(() -> true, arrow);
		fresh.despawned(FISHED);
		fresh.requested();
		fresh.tick(picker, tiles);

		assertEquals(0, picker.picks);
	}

	@Test
	public void fishedSpotDespawningWhileOnItSearches()
	{
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		tick();

		assertEquals(1, picker.picks);
		assertSame(NPC_1, game.shown.npc());
	}

	@Test
	public void interactionEndingBeforeTheDespawnStillSearchesOnce()
	{
		picker.next.add(SPOT_1);

		// The game clears the interaction first, then the spot despawns.
		pathing.interactionEnded();
		pathing.despawned(FISHED);
		assertOneSearchOverTwoTicks();
	}

	@Test
	public void despawnBeforeTheInteractionEndsSearchesOnce()
	{
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.interactionEnded();
		assertOneSearchOverTwoTicks();
	}

	private void assertOneSearchOverTwoTicks()
	{
		tick();
		tick();

		assertEquals(1, picker.picks);
		assertEquals(1, game.shows);
	}

	@Test
	public void despawnOnTheTickAfterTheInteractionEndsStillSearches()
	{
		picker.next.add(SPOT_1);

		pathing.interactionEnded();
		tick();
		pathing.despawned(FISHED);
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void despawnLongAfterTheInteractionEndedIsNotTheSpotMoving()
	{
		picker.next.add(SPOT_1);

		// Stopped fishing (full inventory) and stood there; the spot moves much later.
		pathing.interactionEnded();
		for (int i = 0; i < 10; i++)
		{
			tick();
		}
		pathing.despawned(FISHED);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void walkingAwayBeforeTheDespawnDoesNotSearch()
	{
		picker.next.add(SPOT_1);

		// Walk here, or a tree or rock: no NPC.
		pathing.turnedTo(null);
		pathing.despawned(FISHED);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void clickingTheSameSpotAgainKeepsIt()
	{
		picker.next.add(SPOT_1);

		pathing.turnedTo(FISHED);
		pathing.despawned(FISHED);
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void clickingElsewhereThenTheInteractionEndingDoesNotSearch()
	{
		picker.next.add(SPOT_1);

		// Clicked a banker: a different NPC.
		pathing.turnedTo(NPC_2);
		pathing.interactionEnded();
		pathing.despawned(FISHED);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void fishedSpotTurningIntoAnotherSpotTypeSearches()
	{
		picker.next.add(SPOT_1);

		pathing.changed(FISHED);
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void changeThenDespawnOfTheSameSpotSearchesOnce()
	{
		picker.next.add(SPOT_1);

		pathing.changed(FISHED);
		tick();
		pathing.despawned(FISHED);
		tick();

		assertEquals(1, picker.picks);
	}

	@Test
	public void someOtherNpcChangingIsIgnored()
	{
		pathing.changed(new Object());
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void resetForgetsTheFishedSpot()
	{
		pathing.reset();

		pathing.despawned(FISHED);
		tick();

		assertEquals(0, picker.picks);
	}

	@Test
	public void logoutClearsOurArrowAndForgetsTheSpot()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		tick();
		assertSame(NPC_1, game.shown.npc());

		pathing.reset();
		pathing.despawned(FISHED);
		tick();

		assertNull(game.shown);
		assertEquals(1, game.hides);
		assertEquals(1, picker.picks);
	}

	@Test
	public void cancelTakesDownTheArrowWhateverItPointsAt()
	{
		// A tree's arrow, set through the shared arrow rather than by a fishing search.
		arrow.point(NextSpotArrow.Target.tile(3200, 3200, 0));

		pathing.cancel();

		assertNull(game.shown);
		assertFalse(arrow.active());
	}
}
