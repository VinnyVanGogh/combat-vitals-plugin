package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.FishingSpot;
import org.junit.Test;

/**
 * The Board's example, from the screenshot that started this: at Fishing 10
 * on a Net/Bait spot, Net shows Shrimps only (Anchovies need 15) and Bait
 * shows Sardine and Herring.
 */
public class FishingCatchesTest
{
	private static List<String> names(List<FishingCatches.Fish> fish)
	{
		return fish.stream().map(FishingCatches.Fish::getName).collect(Collectors.toList());
	}

	@Test
	public void netAtLevelTenIsShrimpsOnly()
	{
		final List<FishingCatches.Fish> net = FishingCatches.forOption(FishingSpot.SHRIMP, "Net");
		assertEquals(Arrays.asList("Shrimps", "Anchovies"), names(net));
		assertEquals(Arrays.asList("Shrimps"), names(FishingCatches.catchable(net, 10)));
	}

	@Test
	public void baitAtLevelTenIsSardineAndHerring()
	{
		final List<FishingCatches.Fish> bait = FishingCatches.forOption(FishingSpot.SHRIMP, "Bait");
		assertEquals(Arrays.asList("Sardine", "Herring"), names(FishingCatches.catchable(bait, 10)));
	}

	@Test
	public void boostedLevelUnlocksTheNextFish()
	{
		final List<FishingCatches.Fish> net = FishingCatches.forOption(FishingSpot.SHRIMP, "Net");
		assertEquals(Arrays.asList("Shrimps", "Anchovies"), names(FishingCatches.catchable(net, 15)));
		assertEquals(Arrays.asList("Shrimps"), names(FishingCatches.catchable(net, 14)));
	}

	@Test
	public void belowTheFirstLevelNothingIsCatchable()
	{
		final List<FishingCatches.Fish> bait = FishingCatches.forOption(FishingSpot.SHRIMP, "Bait");
		assertTrue(FishingCatches.catchable(bait, 4).isEmpty());
	}

	@Test
	public void harpoonDependsOnTheSpot()
	{
		assertEquals(Arrays.asList("Tuna", "Swordfish"),
			names(FishingCatches.forOption(FishingSpot.LOBSTER, "Harpoon")));
		assertEquals(Arrays.asList("Shark"),
			names(FishingCatches.forOption(FishingSpot.SHARK, "Harpoon")));
		assertEquals(Arrays.asList("Tuna", "Swordfish"),
			names(FishingCatches.forOption(FishingSpot.MONKFISH, "Harpoon")));
	}

	@Test
	public void netDependsOnTheSpot()
	{
		assertEquals(Arrays.asList("Monkfish"), names(FishingCatches.forOption(FishingSpot.MONKFISH, "Net")));
		assertEquals(Arrays.asList("Shrimps", "Karambwanji"),
			names(FishingCatches.forOption(FishingSpot.KARAMBWANJI, "Net")));
		assertEquals(Arrays.asList("Frog spawn"), names(FishingCatches.forOption(FishingSpot.CAVE_EEL, "Net")));
	}

	@Test
	public void bigNetAndSmallNetAreDifferentMethods()
	{
		assertEquals(Arrays.asList("Mackerel", "Cod", "Bass"),
			names(FishingCatches.forOption(FishingSpot.SHARK, "Big Net")));
		assertEquals(Arrays.asList("Shrimps", "Anchovies"),
			names(FishingCatches.forOption(FishingSpot.SHRIMP, "Small Net")));
		assertEquals(Arrays.asList("Guppy", "Cavefish", "Tetra"),
			names(FishingCatches.forOption(FishingSpot.CAMDOZAAL_TETRA, "Small Net")));
		assertEquals(Arrays.asList("Tetra", "Catfish"),
			names(FishingCatches.forOption(FishingSpot.CAMDOZAAL_TETRA, "Big Net")));
	}

	@Test
	public void lureAndRodSpots()
	{
		assertEquals(Arrays.asList("Trout", "Salmon", "Rainbow fish"),
			names(FishingCatches.forOption(FishingSpot.SALMON, "Lure")));
		assertEquals(Arrays.asList("Pike"), names(FishingCatches.forOption(FishingSpot.SALMON, "Bait")));
		assertEquals(Arrays.asList("Leaping trout", "Leaping salmon", "Leaping sturgeon"),
			names(FishingCatches.forOption(FishingSpot.BARB_FISH, "Use-rod")));
	}

	@Test
	public void singleFishSpots()
	{
		assertEquals(Arrays.asList("Anglerfish"), names(FishingCatches.forOption(FishingSpot.ANGLERFISH, "Bait")));
		assertEquals(Arrays.asList("Karambwan"), names(FishingCatches.forOption(FishingSpot.KARAMBWAN, "Fish")));
		assertEquals(Arrays.asList("Minnow"), names(FishingCatches.forOption(FishingSpot.MINNOW, "Small Net")));
		assertEquals(Arrays.asList("Lobster", "Dark crab"),
			names(FishingCatches.forOption(FishingSpot.DARK_CRAB, "Cage")));
		assertEquals(Arrays.asList("Infernal eel"), names(FishingCatches.forOption(FishingSpot.INFERNAL_EEL, "Bait")));
		assertEquals(Arrays.asList("Sacred eel"), names(FishingCatches.forOption(FishingSpot.SACRED_EEL, "Bait")));
		assertEquals(Arrays.asList("Lava eel"), names(FishingCatches.forOption(FishingSpot.LAVA_EEL, "Bait")));
		assertEquals(Arrays.asList("Slimy eel", "Cave eel"), names(FishingCatches.forOption(FishingSpot.CAVE_EEL, "Bait")));
		assertEquals(Arrays.asList("Harpoonfish"), names(FishingCatches.forOption(FishingSpot.HARPOONFISH, "Harpoon")));
	}

	@Test
	public void unknownSpotFallsBackToNothing()
	{
		// Null is what FishingSpot.findSpot returns for an NPC id it does not
		// know, and a known spot type this table has no row for is the same
		// case: the badge then shows the skill and level only.
		assertTrue(FishingCatches.forOption(null, "Net").isEmpty());
		assertTrue(FishingCatches.forOption(FishingSpot.COMMON_TENCH, "Catch").isEmpty());
		assertTrue(FishingCatches.forOption(FishingSpot.QUEST_RUM_DEAL, "Fish").isEmpty());
	}

	@Test
	public void unknownOrMissingOptionFallsBackToNothing()
	{
		assertTrue(FishingCatches.forOption(FishingSpot.SHRIMP, null).isEmpty());
		assertTrue(FishingCatches.forOption(FishingSpot.SHRIMP, "Harpoon").isEmpty());
		assertTrue(FishingCatches.forOption(FishingSpot.SHRIMP, "Talk-to").isEmpty());
	}

	@Test
	public void optionMatchingIgnoresCase()
	{
		assertEquals(Arrays.asList("Shrimps", "Anchovies"), names(FishingCatches.forOption(FishingSpot.SHRIMP, "NET")));
		assertEquals(Arrays.asList("Mackerel", "Cod", "Bass"), names(FishingCatches.forOption(FishingSpot.SHARK, "big net")));
	}

	@Test
	public void everyFishHasAnItemForItsIcon()
	{
		for (FishingSpot spot : FishingSpot.values())
		{
			for (String option : new String[] {"Net", "Small Net", "Big Net", "Bait", "Lure", "Cage", "Harpoon", "Use-rod", "Fish"})
			{
				for (FishingCatches.Fish fish : FishingCatches.forOption(spot, option))
				{
					assertTrue(fish.getName() + " has no item", fish.getItemId() > 0);
					assertTrue(fish.getName() + " has no level", fish.getLevel() >= 1);
					assertSame(fish.getName() + " round-trips by item", fish, FishingCatches.byItem(fish.getItemId()));
				}
			}
		}
	}

	@Test
	public void levelsAreAscendingWithinAMethod()
	{
		for (FishingSpot spot : FishingSpot.values())
		{
			for (String option : new String[] {"Net", "Big Net", "Bait", "Lure", "Cage", "Harpoon", "Use-rod", "Fish"})
			{
				int last = 0;
				for (FishingCatches.Fish fish : FishingCatches.forOption(spot, option))
				{
					assertTrue(spot + " " + option + " out of order at " + fish.getName(), fish.getLevel() >= last);
					last = fish.getLevel();
				}
			}
		}
	}

	@Test
	public void catchMessagesNameTheFish()
	{
		assertEquals(ItemID.RAW_SHRIMP, FishingCatches.fromCatchMessage("You catch some shrimps.").getItemId());
		assertEquals(ItemID.RAW_ANCHOVIES, FishingCatches.fromCatchMessage("You catch some anchovies.").getItemId());
		assertEquals(ItemID.RAW_SARDINE, FishingCatches.fromCatchMessage("You catch a sardine.").getItemId());
		assertEquals(ItemID.RAW_ANGLERFISH, FishingCatches.fromCatchMessage("You catch an anglerfish.").getItemId());
		assertEquals(ItemID.RAW_SHARK, FishingCatches.fromCatchMessage("You catch a shark!").getItemId());
		assertEquals(ItemID.TBWT_RAW_KARAMBWANJI, FishingCatches.fromCatchMessage("You catch some karambwanji.").getItemId());
		assertEquals(ItemID.BRUT_SPAWNING_TROUT, FishingCatches.fromCatchMessage("You catch a leaping trout.").getItemId());
		assertEquals(ItemID.MINNOW, FishingCatches.fromCatchMessage("You catch some minnows.").getItemId());
	}

	@Test
	public void otherMessagesAreNotCatches()
	{
		assertNull(FishingCatches.fromCatchMessage("You fail to catch anything."));
		assertNull(FishingCatches.fromCatchMessage("You catch a casket."));
		assertNull(FishingCatches.fromCatchMessage("You need a fishing rod to bait these fish."));
		assertNull(FishingCatches.fromCatchMessage(null));
	}

	@Test
	public void byItemKnowsOnlyFish()
	{
		assertEquals("Lobster", FishingCatches.byItem(ItemID.RAW_LOBSTER).getName());
		assertNull(FishingCatches.byItem(ItemID.CASKET));
		assertNull(FishingCatches.byItem(-1));
	}

	@Test
	public void lastCaughtFishComesFirst()
	{
		final List<FishingCatches.Fish> bait = FishingCatches.catchable(
			FishingCatches.forOption(FishingSpot.SHRIMP, "Bait"), 10);

		assertEquals(Arrays.asList("Herring", "Sardine"),
			names(FishingCatches.highlightFirst(bait, ItemID.RAW_HERRING)));
		assertEquals("unknown catch keeps the level order", Arrays.asList("Sardine", "Herring"),
			names(FishingCatches.highlightFirst(bait, -1)));
		assertEquals("a catch from another method keeps the level order", Arrays.asList("Sardine", "Herring"),
			names(FishingCatches.highlightFirst(bait, ItemID.RAW_SHRIMP)));
	}

	/** "Shrimps" for one the player can catch, "Anchovies 15" for one locked behind its level. */
	private static List<String> shown(List<FishingCatches.Catch> catches)
	{
		return catches.stream()
			.map(c -> c.isCatchable() ? c.getFish().getName() : c.getFish().getName() + " " + c.getFish().getLevel())
			.collect(Collectors.toList());
	}

	@Test
	public void netSpotShowsEveryFishWithTheLockedOnesMarked()
	{
		final List<FishingCatches.Fish> net = FishingCatches.forOption(FishingSpot.SHRIMP, "Net");
		// Below Anchovies: Shrimps now, Anchovies at 15.
		assertEquals(Arrays.asList("Shrimps", "Anchovies 15"), shown(FishingCatches.options(net, 14, -1)));
		// On the level: both.
		assertEquals(Arrays.asList("Shrimps", "Anchovies"), shown(FishingCatches.options(net, 15, -1)));
	}

	@Test
	public void baitSpotBelowBetweenAndAboveItsLevels()
	{
		final List<FishingCatches.Fish> bait = FishingCatches.forOption(FishingSpot.SHRIMP, "Bait");
		assertEquals("below both", Arrays.asList("Sardine 5", "Herring 10"), shown(FishingCatches.options(bait, 4, -1)));
		assertEquals("between", Arrays.asList("Sardine", "Herring 10"), shown(FishingCatches.options(bait, 7, -1)));
		assertEquals("above both", Arrays.asList("Sardine", "Herring"), shown(FishingCatches.options(bait, 99, -1)));
	}

	@Test
	public void lastCatchLeadsTheCatchableAndLockedStayLast()
	{
		final List<FishingCatches.Fish> bait = FishingCatches.forOption(FishingSpot.SHRIMP, "Bait");
		assertEquals(Arrays.asList("Herring", "Sardine"),
			shown(FishingCatches.options(bait, 10, ItemID.RAW_HERRING)));
		assertEquals(Arrays.asList("Sardine", "Herring 10"),
			shown(FishingCatches.options(bait, 9, ItemID.RAW_SARDINE)));
	}

	@Test
	public void unknownSpotHasNoOptions()
	{
		assertTrue(FishingCatches.options(FishingCatches.forOption(null, "Net"), 50, -1).isEmpty());
	}

	@Test
	public void lastCaughtAboveTheShownLevelStillLeadsWhenItIsInTheList()
	{
		// Net at 15: Shrimps and Anchovies are both catchable; the last catch
		// was anchovies, so anchovies lead.
		final List<FishingCatches.Fish> net = FishingCatches.catchable(
			FishingCatches.forOption(FishingSpot.SHRIMP, "Net"), 15);
		assertEquals(Arrays.asList("Anchovies", "Shrimps"),
			names(FishingCatches.highlightFirst(net, ItemID.RAW_ANCHOVIES)));
	}
}
