package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.game.FishingSpot;
import org.junit.Test;

/**
 * The "Needs:" row on a fishing click: each option with and without its
 * tool, worn or carried, the harpoon variants, barehanded harpooning, bait
 * and level (Board, 2026-11-04).
 */
public class FishingGearTest
{
	private static final List<FishingCatches.Fish> SHRIMP_NET = FishingCatches.forOption(FishingSpot.SHRIMP, "Net");
	private static final List<FishingCatches.Fish> SHRIMP_BAIT = FishingCatches.forOption(FishingSpot.SHRIMP, "Bait");
	private static final List<FishingCatches.Fish> TUNA_HARPOON = FishingCatches.forOption(FishingSpot.LOBSTER, "Harpoon");
	private static final List<FishingCatches.Fish> SHARK_HARPOON = FishingCatches.forOption(FishingSpot.SHARK, "Harpoon");

	private static FishingGear.Kit kit(int level, String... items)
	{
		return new FishingGear.Kit(Arrays.asList(items), false, level);
	}

	private static FishingGear.Kit barbarian(int level, String... items)
	{
		return new FishingGear.Kit(Arrays.asList(items), true, level);
	}

	private static List<String> missing(String option, List<FishingCatches.Fish> fish, FishingGear.Kit kit)
	{
		return FishingGear.missing(option, fish, kit);
	}

	@Test
	public void baitWithNothingNeedsRodAndBait()
	{
		final List<String> missing = missing("Bait", SHRIMP_BAIT, kit(99));
		assertEquals(Arrays.asList("Fishing rod", "bait"), missing);
		assertEquals("Needs: Fishing rod + bait", FishingGear.text(missing));
	}

	@Test
	public void baitWithRodButNoBaitNeedsBait()
	{
		assertEquals(Collections.singletonList("bait"), missing("Bait", SHRIMP_BAIT, kit(99, "Fishing rod")));
	}

	@Test
	public void baitWithBaitButNoRodNeedsRod()
	{
		assertEquals(Collections.singletonList("Fishing rod"), missing("Bait", SHRIMP_BAIT, kit(99, "Fishing bait")));
	}

	@Test
	public void baitWithEverythingNeedsNothing()
	{
		assertTrue(missing("Bait", SHRIMP_BAIT, kit(99, "Fishing rod", "Fishing bait")).isEmpty());
		assertTrue(missing("Bait", SHRIMP_BAIT, kit(99, "Pearl fishing rod", "Fishing bait")).isEmpty());
		assertTrue(missing("Bait", SHRIMP_BAIT, kit(99, "Oily fishing rod", "Fishing bait")).isEmpty());
		assertNull(FishingGear.text(missing("Bait", SHRIMP_BAIT, kit(99, "Fishing rod", "Fishing bait"))));
	}

	@Test
	public void aFlyRodIsNotABaitRod()
	{
		assertEquals(Collections.singletonList("Fishing rod"),
			missing("Bait", SHRIMP_BAIT, kit(99, "Fly fishing rod", "Fishing bait")));
	}

	@Test
	public void lureNeedsFlyRodAndFeathers()
	{
		assertEquals(Arrays.asList("Fly fishing rod", "feathers"), missing("Lure", Collections.emptyList(), kit(99)));
		assertEquals(Collections.singletonList("feathers"),
			missing("Lure", Collections.emptyList(), kit(99, "Fly fishing rod")));
		assertTrue(missing("Lure", Collections.emptyList(), kit(99, "Fly fishing rod", "Feather")).isEmpty());
		assertTrue(missing("Lure", Collections.emptyList(), kit(99, "Pearl fly fishing rod", "Stripy feather")).isEmpty());
	}

	@Test
	public void netsAndCage()
	{
		assertEquals(Collections.singletonList("Small fishing net"), missing("Net", SHRIMP_NET, kit(99)));
		assertTrue(missing("Small Net", SHRIMP_NET, kit(99, "Small fishing net")).isEmpty());
		assertEquals(Collections.singletonList("Big fishing net"), missing("Big Net", Collections.emptyList(), kit(99)));
		assertTrue(missing("Big Net", Collections.emptyList(), kit(99, "Big fishing net")).isEmpty());
		assertEquals(Collections.singletonList("Lobster pot"), missing("Cage", Collections.emptyList(), kit(99)));
		assertTrue(missing("Cage", Collections.emptyList(), kit(99, "Lobster pot")).isEmpty());
	}

	@Test
	public void wornCountsTheSameAsCarried()
	{
		// The kit is every item carried or worn; a wielded harpoon is one of them.
		assertTrue(missing("Harpoon", TUNA_HARPOON, kit(99, "Dragon harpoon")).isEmpty());
	}

	@Test
	public void everyHarpoonVariantCounts()
	{
		for (String harpoon : new String[] {
			"Harpoon", "Barb-tail harpoon", "Dragon harpoon", "Infernal harpoon", "Crystal harpoon",
			"Infernal harpoon (uncharged)", "Trailblazer harpoon"})
		{
			assertTrue(harpoon, missing("Harpoon", TUNA_HARPOON, kit(99, harpoon)).isEmpty());
		}
	}

	@Test
	public void harpoonWithoutOneNeedsOne()
	{
		assertEquals(Collections.singletonList("Harpoon"), missing("Harpoon", TUNA_HARPOON, kit(99)));
	}

	@Test
	public void barbarianTrainedHarpoonsBarehandedAtTwentyOver()
	{
		// Tuna is 35, so barehanded from 55; shark is 76, so from 96.
		assertTrue(missing("Harpoon", TUNA_HARPOON, barbarian(55)).isEmpty());
		assertEquals(Collections.singletonList("Harpoon"), missing("Harpoon", TUNA_HARPOON, barbarian(54)));
		assertTrue(missing("Harpoon", SHARK_HARPOON, barbarian(96)).isEmpty());
		assertEquals(Collections.singletonList("Harpoon"), missing("Harpoon", SHARK_HARPOON, barbarian(90)));
	}

	@Test
	public void barehandedOnAnUnknownSpotNeedsTunasLevel()
	{
		assertTrue(missing("Harpoon", Collections.emptyList(), barbarian(55)).isEmpty());
		assertEquals(Collections.singletonList("Harpoon"), missing("Harpoon", Collections.emptyList(), barbarian(40)));
	}

	@Test
	public void barbarianTrainingDoesNotFreeTheCage()
	{
		assertEquals(Collections.singletonList("Lobster pot"), missing("Cage", Collections.emptyList(), barbarian(99)));
	}

	@Test
	public void barbarianRodNeedsRodAndBait()
	{
		assertEquals(Arrays.asList("Barbarian rod", "bait"), missing("Use-rod", Collections.emptyList(), kit(99)));
		assertTrue(missing("Use-rod", Collections.emptyList(), kit(99, "Barbarian rod", "Feather")).isEmpty());
		assertTrue(missing("Use-rod", Collections.emptyList(), kit(99, "Barbarian rod", "Fish offcuts")).isEmpty());
	}

	@Test
	public void zeroBaitIsNoBait()
	{
		// Items with a quantity of 0 never reach the kit: the plugin skips empty slots.
		assertEquals(Collections.singletonList("bait"), missing("Bait", SHRIMP_BAIT, kit(99, "Fishing rod")));
	}

	@Test
	public void levelTooLowIsNamed()
	{
		// Sardine, the lowest Bait fish on a shrimp spot, is level 5.
		final List<String> missing = missing("Bait", SHRIMP_BAIT, kit(3, "Fishing rod", "Fishing bait"));
		assertEquals(Collections.singletonList("Fishing 5"), missing);
		assertEquals("Needs: Fishing 5", FishingGear.text(missing));
		assertEquals(Arrays.asList("Fishing rod", "bait", "Fishing 5"), missing("Bait", SHRIMP_BAIT, kit(3)));
	}

	@Test
	public void unknownOptionsSayNothing()
	{
		assertTrue(missing("Talk-to", Collections.emptyList(), kit(1)).isEmpty());
		assertTrue(missing("Cast", Collections.emptyList(), kit(1)).isEmpty());
		assertTrue(missing(null, Collections.emptyList(), kit(1)).isEmpty());
	}

	@Test
	public void optionCaseIsIgnored()
	{
		assertEquals(Collections.singletonList("Small fishing net"), missing("NET", SHRIMP_NET, kit(99)));
	}
}
