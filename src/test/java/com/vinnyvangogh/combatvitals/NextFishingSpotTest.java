package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.game.FishingSpot;
import org.junit.Test;

public class NextFishingSpotTest
{
	private static final Object NPC_A = new Object();
	private static final Object NPC_B = new Object();
	private static final Object NPC_C = new Object();

	private static NextFishingSpot.Need need(FishingSpot type, String method, int level, boolean members, Object exclude)
	{
		return new NextFishingSpot.Need(type, method, level, members, exclude);
	}

	private static NextFishingSpot.Need lobsters()
	{
		return need(FishingSpot.LOBSTER, "Cage", 99, true, null);
	}

	@Test
	public void picksTheNearestLoadedSpot()
	{
		final NextFishingSpot.Spot far = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3010, 3000, 0, NPC_A);
		final NextFishingSpot.Spot near = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3003, 3001, 0, NPC_B);

		assertSame(near, NextFishingSpot.nearest(Arrays.asList(far, near), 3000, 3000, 0, lobsters()));
	}

	@Test
	public void tieGoesToTheLowerXThenLowerYWhateverTheSceneOrder()
	{
		// All three are 4 tiles away (Chebyshev).
		final NextFishingSpot.Spot east = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3004, 3000, 0, NPC_A);
		final NextFishingSpot.Spot westHigh = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 2996, 3002, 0, NPC_B);
		final NextFishingSpot.Spot westLow = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 2996, 2997, 0, NPC_C);

		assertSame(westLow, NextFishingSpot.nearest(Arrays.asList(east, westHigh, westLow), 3000, 3000, 0, lobsters()));
		assertSame(westLow, NextFishingSpot.nearest(Arrays.asList(westLow, westHigh, east), 3000, 3000, 0, lobsters()));
	}

	@Test
	public void onlyTheSameSpotType()
	{
		final NextFishingSpot.Spot nets = NextFishingSpot.Spot.loaded(FishingSpot.SHRIMP, 3001, 3000, 0, NPC_A);
		final NextFishingSpot.Spot sharks = NextFishingSpot.Spot.loaded(FishingSpot.SHARK, 3001, 3001, 0, NPC_B);
		final NextFishingSpot.Spot cages = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3020, 3020, 0, NPC_C);

		assertSame(cages, NextFishingSpot.nearest(Arrays.asList(nets, sharks, cages), 3000, 3000, 0, lobsters()));
		assertNull(NextFishingSpot.nearest(Arrays.asList(nets, sharks), 3000, 3000, 0, lobsters()));
	}

	@Test
	public void levelFilterDropsSpotsTheMethodCannotCatchAtThisLevel()
	{
		final NextFishingSpot.Spot spot = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3001, 3000, 0, NPC_A);

		// Harpoon on a cage/harpoon spot starts at tuna, level 35.
		assertNull(NextFishingSpot.nearest(Collections.singletonList(spot), 3000, 3000, 0,
			need(FishingSpot.LOBSTER, "Harpoon", 34, true, null)));
		assertSame(spot, NextFishingSpot.nearest(Collections.singletonList(spot), 3000, 3000, 0,
			need(FishingSpot.LOBSTER, "Harpoon", 35, true, null)));
	}

	@Test
	public void levelFilterHonoursALocationsOwnRequirement()
	{
		final NextFishingSpot.Spot guild = NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 2599, 3419, 0, true, 68, "Fishing Guild");
		final NextFishingSpot.Spot catherby = NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 2844, 3429, 0, true, 0, "Catherby");
		final List<NextFishingSpot.Spot> known = Arrays.asList(guild, catherby);

		assertSame(catherby, NextFishingSpot.nearest(known, 2600, 3420, 0, need(FishingSpot.LOBSTER, "Cage", 67, true, null)));
		assertSame(guild, NextFishingSpot.nearest(known, 2600, 3420, 0, need(FishingSpot.LOBSTER, "Cage", 68, true, null)));
	}

	@Test
	public void freeWorldsOnlyGetFreeLocations()
	{
		final NextFishingSpot.Spot members = NextFishingSpot.Spot.known(FishingSpot.SHRIMP, 3001, 3001, 0, true, 0, "Members");
		final NextFishingSpot.Spot free = NextFishingSpot.Spot.known(FishingSpot.SHRIMP, 3050, 3050, 0, false, 0, "Free");
		final List<NextFishingSpot.Spot> known = Arrays.asList(members, free);

		assertSame(free, NextFishingSpot.nearest(known, 3000, 3000, 0, need(FishingSpot.SHRIMP, "Net", 1, false, null)));
		assertSame(members, NextFishingSpot.nearest(known, 3000, 3000, 0, need(FishingSpot.SHRIMP, "Net", 1, true, null)));
	}

	@Test
	public void theSpotBeingFishedIsNotTheNextOne()
	{
		final NextFishingSpot.Spot current = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3001, 3000, 0, NPC_A);
		final NextFishingSpot.Spot other = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3009, 3000, 0, NPC_B);

		assertSame(other, NextFishingSpot.nearest(Arrays.asList(current, other), 3000, 3000, 0,
			need(FishingSpot.LOBSTER, "Cage", 99, true, NPC_A)));
	}

	@Test
	public void anotherPlaneLosesToAnyOnThisPlane()
	{
		final NextFishingSpot.Spot upstairs = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3000, 3000, 1, NPC_A);
		final NextFishingSpot.Spot across = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3060, 3000, 0, NPC_B);

		assertSame(across, NextFishingSpot.nearest(Arrays.asList(upstairs, across), 3000, 3000, 0, lobsters()));
	}

	@Test
	public void loadedSpotsWinOverTheBundledList()
	{
		final NextFishingSpot.Spot loaded = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3040, 3000, 0, NPC_A);
		final NextFishingSpot.Spot known = NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 3001, 3000, 0, false, 0, "Close");

		assertSame(loaded, NextFishingSpot.choose(Collections.singletonList(loaded), Collections.singletonList(known),
			3000, 3000, 0, lobsters()));
	}

	@Test
	public void noLoadedSpotFallsBackToTheNearestKnownOne()
	{
		final NextFishingSpot.Spot wrongType = NextFishingSpot.Spot.loaded(FishingSpot.SHRIMP, 3001, 3000, 0, NPC_A);
		final NextFishingSpot.Spot near = NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 3100, 3000, 0, false, 0, "Near");
		final NextFishingSpot.Spot far = NextFishingSpot.Spot.known(FishingSpot.LOBSTER, 3300, 3000, 0, false, 0, "Far");

		assertSame(near, NextFishingSpot.choose(Collections.singletonList(wrongType), Arrays.asList(far, near),
			3000, 3000, 0, lobsters()));
	}

	@Test
	public void unknownSpotTypeFindsNothing()
	{
		final NextFishingSpot.Spot spot = NextFishingSpot.Spot.loaded(FishingSpot.LOBSTER, 3001, 3000, 0, NPC_A);

		assertNull(NextFishingSpot.choose(Collections.singletonList(spot), Collections.emptyList(), 3000, 3000, 0,
			need(null, "Cage", 99, true, null)));
	}
}
