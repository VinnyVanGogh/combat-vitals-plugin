package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.runelite.client.game.FishingSpot;
import org.junit.Before;
import org.junit.Test;

public class SpotPathingTest
{
	/** Records what would have gone to Shortest Path. */
	private static final class FakeRouter implements SpotPathing.Router
	{
		boolean installed = true;
		final List<NextFishingSpot.Spot> routes = new ArrayList<>();
		int clears;

		@Override
		public boolean available()
		{
			return installed;
		}

		@Override
		public boolean route(NextFishingSpot.Spot spot)
		{
			routes.add(spot);
			return true;
		}

		@Override
		public void clear()
		{
			clears++;
		}
	}

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

	/** The spot NPC {@link #setUp} starts fishing; its despawning is the spot moving. */
	private static final Object FISHED = new Object();
	private static final Object NPC_1 = new Object();
	private static final Object NPC_2 = new Object();
	private static final NextFishingSpot.Spot SPOT_1 = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3001, 3000, 0, NPC_1);
	private static final NextFishingSpot.Spot SPOT_2 = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3005, 3000, 0, NPC_2);

	private FishingSpotPath mode;
	private FakeRouter router;
	private FakePicker picker;
	private SpotPathing pathing;

	@Before
	public void setUp()
	{
		mode = FishingSpotPath.ON_SPOT_MOVE;
		router = new FakeRouter();
		picker = new FakePicker();
		pathing = new SpotPathing(() -> mode, router);
		pathing.fishingOn(FishingSpot.LOBSTER, "Cage", FISHED);
	}

	@Test
	public void spotMoveRoutesToTheNextSpotOfTheSameTypeAndMethod()
	{
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(1, router.routes.size());
		assertSame(SPOT_1, router.routes.get(0));
		assertSame(FishingSpot.LOBSTER, picker.lastType);
		assertEquals("Cage", picker.lastMethod);
		assertNull("routed, not highlighted", pathing.highlighted());
	}

	@Test
	public void offDoesNothingEvenOnRightClick()
	{
		mode = FishingSpotPath.OFF;
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.requested();
		pathing.tick(picker);

		assertEquals(0, picker.picks);
		assertEquals(0, router.routes.size());
	}

	@Test
	public void rightClickOnlyIgnoresTheSpotMovingButAnswersTheMenu()
	{
		mode = FishingSpotPath.RIGHT_CLICK;
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.tick(picker);
		assertEquals(0, router.routes.size());

		pathing.requested();
		pathing.tick(picker);
		assertEquals(1, router.routes.size());
	}

	@Test
	public void spotMovingTwiceInOneTickSendsOnePath()
	{
		picker.next.add(SPOT_2);

		pathing.despawned(FISHED);
		pathing.despawned(FISHED);
		pathing.tick(picker);
		pathing.tick(picker);

		assertEquals(1, picker.picks);
		assertEquals(1, router.routes.size());
		assertSame(SPOT_2, router.routes.get(0));
	}

	@Test
	public void targetMovingAgainReroutesToTheLatestSpot()
	{
		picker.next.add(SPOT_1);
		picker.next.add(SPOT_2);

		pathing.despawned(FISHED);
		pathing.tick(picker);
		// The spot being walked to moves before the player gets there.
		pathing.despawned(NPC_1);
		pathing.tick(picker);

		assertEquals(2, router.routes.size());
		assertSame(SPOT_2, router.routes.get(1));
		assertSame(SPOT_2, pathing.target());
	}

	@Test
	public void someOtherNpcDespawningIsIgnored()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		pathing.despawned(new Object());
		pathing.tick(picker);

		assertEquals(1, picker.picks);
	}

	@Test
	public void withoutShortestPathTheSpotIsHighlightedInstead()
	{
		router.installed = false;
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(0, router.routes.size());
		assertSame(NPC_1, pathing.highlighted());
	}

	@Test
	public void aBundledSpotWithoutShortestPathHasNothingToHighlight()
	{
		router.installed = false;
		picker.next.add(NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 2844, 3429, 0, true, 0, "Catherby"));

		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertNull(pathing.highlighted());
	}

	@Test
	public void startingToFishAgainClearsThePath()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		pathing.fishingOn(FishingSpot.LOBSTER, "Cage", new Object());

		assertEquals(1, router.clears);
		assertNull(pathing.target());
	}

	@Test
	public void startingToFishAgainClearsTheHighlight()
	{
		router.installed = false;
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		pathing.fishingOn(FishingSpot.LOBSTER, "Cage", new Object());

		assertNull(pathing.highlighted());
		assertEquals("nothing was sent, so nothing to clear", 0, router.clears);
	}

	@Test
	public void aPathThePluginNeverSentIsNeverCleared()
	{
		pathing.fishingOn(FishingSpot.SHRIMP, "Net", new Object());
		pathing.reset();

		assertEquals(0, router.clears);
	}

	@Test
	public void noNextSpotClearsAnOldPath()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		pathing.despawned(NPC_1);
		pathing.tick(picker);

		assertEquals(1, router.clears);
		assertNull(pathing.target());
	}

	@Test
	public void nothingFishedYetMeansNothingToFind()
	{
		final SpotPathing fresh = new SpotPathing(() -> FishingSpotPath.ON_SPOT_MOVE, router);
		fresh.despawned(FISHED);
		fresh.requested();
		fresh.tick(picker);

		assertEquals(0, picker.picks);
	}

	@Test
	public void fishedSpotDespawningWhileOnItSearches()
	{
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(1, picker.picks);
		assertSame(SPOT_1, router.routes.get(0));
	}

	@Test
	public void interactionEndingBeforeTheDespawnStillSearchesOnce()
	{
		picker.next.add(SPOT_1);

		// The game clears the interaction first, then the spot despawns.
		pathing.interactionEnded();
		pathing.despawned(FISHED);
		pathing.interactionEnded();
		pathing.tick(picker);
		pathing.tick(picker);

		assertEquals(1, picker.picks);
		assertEquals(1, router.routes.size());
	}

	@Test
	public void despawnBeforeTheInteractionEndsSearchesOnce()
	{
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.interactionEnded();
		pathing.tick(picker);
		pathing.tick(picker);

		assertEquals(1, picker.picks);
		assertEquals(1, router.routes.size());
	}

	@Test
	public void despawnOnTheTickAfterTheInteractionEndsStillSearches()
	{
		picker.next.add(SPOT_1);

		pathing.interactionEnded();
		pathing.tick(picker);
		pathing.despawned(FISHED);
		pathing.tick(picker);

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
			pathing.tick(picker);
		}
		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(0, picker.picks);
	}

	@Test
	public void walkingAwayBeforeTheDespawnDoesNotSearch()
	{
		picker.next.add(SPOT_1);

		// Walk here, or a tree or rock: no NPC.
		pathing.turnedTo(null);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(0, picker.picks);
	}

	@Test
	public void clickingTheSameSpotAgainKeepsIt()
	{
		picker.next.add(SPOT_1);

		pathing.turnedTo(FISHED);
		pathing.despawned(FISHED);
		pathing.tick(picker);

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
		pathing.tick(picker);

		assertEquals(0, picker.picks);
	}

	@Test
	public void fishedSpotTurningIntoAnotherSpotTypeSearches()
	{
		picker.next.add(SPOT_1);

		pathing.changed(FISHED);
		pathing.tick(picker);

		assertEquals(1, picker.picks);
	}

	@Test
	public void changeThenDespawnOfTheSameSpotSearchesOnce()
	{
		picker.next.add(SPOT_1);

		pathing.changed(FISHED);
		pathing.tick(picker);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(1, picker.picks);
	}

	@Test
	public void someOtherNpcChangingIsIgnored()
	{

		pathing.changed(new Object());
		pathing.tick(picker);

		assertEquals(0, picker.picks);
	}

	@Test
	public void rightClickModeIgnoresTheFishedSpotMovingButAnswersTheMenu()
	{
		mode = FishingSpotPath.RIGHT_CLICK;
		picker.next.add(SPOT_1);

		pathing.despawned(FISHED);
		pathing.tick(picker);
		assertEquals(0, picker.picks);

		pathing.requested();
		pathing.tick(picker);
		assertEquals(1, picker.picks);
		assertEquals(1, router.routes.size());
	}

	@Test
	public void resetForgetsTheFishedSpot()
	{
		pathing.reset();

		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(0, picker.picks);
	}

	@Test
	public void resetClearsASentPathAndForgetsTheSpot()
	{
		picker.next.add(SPOT_1);
		pathing.despawned(FISHED);
		pathing.tick(picker);

		pathing.reset();
		pathing.despawned(FISHED);
		pathing.tick(picker);

		assertEquals(1, router.clears);
		assertEquals(1, picker.picks);
	}
}
