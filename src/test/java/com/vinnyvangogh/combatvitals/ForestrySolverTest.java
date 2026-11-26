package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/** Which part of each Forestry event is the right one, on fixtures with no client. */
public class ForestrySolverTest
{
	private final Map<Object, Integer> ids = new HashMap<>();
	private final Map<Object, WorldPoint> tiles = new HashMap<>();
	private final ForestryMarks marks = new ForestryMarks();

	private final ForestryMarks.View view = new ForestryMarks.View()
	{
		@Override
		public int idOf(Object thing)
		{
			return ids.get(thing);
		}

		@Override
		public WorldPoint tile(Object thing)
		{
			return tiles.get(thing);
		}
	};

	private Object object(int id)
	{
		final Object thing = new Object();
		ids.put(thing, id);
		marks.objectSpawned(thing, id);
		return thing;
	}

	private Object npc(int id)
	{
		final Object thing = new Object();
		ids.put(thing, id);
		marks.npcSpawned(thing, id);
		return thing;
	}

	private List<ForestryMarks.Target> targets(boolean all)
	{
		return marks.targets(kind -> true, all, view);
	}

	private static String labelOf(List<ForestryMarks.Target> targets, Object thing)
	{
		for (ForestryMarks.Target target : targets)
		{
			if (target.thing == thing)
			{
				return target.label;
			}
		}
		return "missing";
	}

	// ---- Enchantment Ritual ----

	@Test
	public void theCircleThatPairsWithNoOtherIsTheOneToStandOn()
	{
		// shape * 4 + colour from 12520: (0,0) (0,1) (1,0) (1,1) pair up, (3,3) is alone
		assertEquals(4, ForestryMarks.solveCircles(new int[] {12520, 12521, 12524, 12525, 12535}));
		assertEquals(0, ForestryMarks.solveCircles(new int[] {12535, 12521, 12524, 12525, 12520}));
		assertEquals(2, ForestryMarks.solveCircles(new int[] {12520, 12521, 12535, 12525, 12524}));
	}

	@Test
	public void noCircleIsSolvedUntilAllFiveAreOnTheScene()
	{
		assertEquals(-1, ForestryMarks.solveCircles(new int[] {12520, 12521, 12524, 12535}));
		assertEquals(-1, ForestryMarks.solveCircles(new int[0]));
	}

	@Test
	public void onlyTheSolvedCircleIsOutlinedUnlessAllIsOn()
	{
		npc(12519);
		npc(12520);
		npc(12521);
		npc(12524);
		npc(12525);
		final Object answer = npc(12535);

		final List<ForestryMarks.Target> only = targets(false);
		assertEquals(1, only.size());
		assertEquals(answer, only.get(0).thing);
		assertEquals("Step here", only.get(0).label);
		assertTrue(only.get(0).correct);
		assertEquals(6, targets(true).size());
	}

	@Test
	public void fourCirclesShowNoAnswerYet()
	{
		npc(12520);
		npc(12521);
		npc(12524);
		npc(12535);
		assertTrue(targets(false).isEmpty());
	}

	// ---- Rising Roots ----

	@Test
	public void onlyTheSpecialRootIsTheOneToChop()
	{
		object(47482);
		final Object special = object(47483);
		final List<ForestryMarks.Target> only = targets(false);
		assertEquals(1, only.size());
		assertEquals(special, only.get(0).thing);
		assertEquals(2, targets(true).size());
	}

	// ---- Flowering Bush ----

	@Test
	public void aBushPairShowsOnceBothAreSeenBeingTended()
	{
		final Object a = npc(12120);
		final Object b = npc(12134);
		npc(12141);
		assertTrue(targets(false).isEmpty());

		marks.flowerTended(a);
		marks.flowerTended(b);
		final List<ForestryMarks.Target> pair = targets(false);
		assertEquals(2, pair.size());
		assertEquals("Pair", labelOf(pair, a));
		assertEquals("Pair", labelOf(pair, b));
		assertEquals(3, targets(true).size());
	}

	@Test
	public void aThirdBushStartsANewPairAndNonBushesAreIgnored()
	{
		final Object a = npc(12120);
		final Object b = npc(12134);
		final Object c = npc(12141);
		marks.flowerTended(a);
		marks.flowerTended(b);
		marks.flowerTended(c);
		assertEquals(1, targets(false).size());
		assertEquals(c, targets(false).get(0).thing);

		marks.flowerTended(new Object());
		marks.flowerTended(null);
		assertEquals(1, targets(false).size());
	}

	@Test
	public void theNoFlowersLineForgetsThePairOnlyForTheBushYouClicked()
	{
		final Object a = npc(12120);
		final Object b = npc(12134);
		final Object other = npc(12141);
		marks.flowerTended(a);
		marks.flowerTended(b);

		marks.flowerClicked(other);
		marks.chat("There are no open, unpollinated flowers on this bush yet.", t -> null);
		assertEquals(2, targets(false).size());

		marks.flowerClicked(b);
		marks.chat("There are no open, unpollinated flowers on this bush yet.", t -> null);
		assertTrue(targets(false).isEmpty());
	}

	@Test
	public void aDespawnedBushEndsThePair()
	{
		final Object a = npc(12120);
		final Object b = npc(12134);
		marks.flowerTended(a);
		marks.flowerTended(b);
		marks.despawned(a);
		assertTrue(targets(false).isEmpty());
	}

	// ---- Struggling Sapling ----

	@Test
	public void aSaplingsIngredientIsLabelledWithItsPlaceOnceTheGameSaysSo()
	{
		object(47484);
		final Object leaves = object(47493);
		final Object mushrooms = object(47494);
		final Map<Object, String> names = new HashMap<>();
		names.put(leaves, "Green leaves");
		names.put(mushrooms, "Wild mushrooms");

		assertTrue(targets(false).isEmpty());
		marks.chat("The sapling seems to love green leaves as the second ingredient.", names::get);
		marks.chat("The sapling seems to love wild mushrooms as the first ingredient.", names::get);

		final List<ForestryMarks.Target> only = targets(false);
		assertEquals(2, only.size());
		assertEquals("2nd", labelOf(only, leaves));
		assertEquals("1st", labelOf(only, mushrooms));
		assertEquals(3, targets(true).size());
	}

	@Test
	public void aLineWithNoPlaceOrNoKnownIngredientChangesNothing()
	{
		object(47493);
		marks.chat("The sapling seems to love green leaves.", t -> "Green leaves");
		marks.chat("The sapling seems to love bark as the third ingredient.", t -> "Green leaves");
		marks.chat("Something else entirely.", t -> "Green leaves");
		marks.chat(null, t -> "Green leaves");
		assertTrue(targets(false).isEmpty());
	}

	@Test
	public void theOrderIsForgottenWhenAnIngredientDespawns()
	{
		final Object leaves = object(47493);
		marks.chat("The sapling seems to love green leaves as the first ingredient.", t -> "Green leaves");
		assertEquals(1, targets(false).size());
		marks.despawned(leaves);
		assertTrue(targets(false).isEmpty());
	}

	// ---- Pheasant Control ----

	@Test
	public void aNestWithAPheasantInItIsNotTheOneToRob()
	{
		final Object full = object(49936);
		final Object empty = object(49937);
		final Object bird = npc(12537);
		final Object forester = npc(12536);
		tiles.put(full, new WorldPoint(3000, 3000, 0));
		tiles.put(empty, new WorldPoint(3010, 3000, 0));
		tiles.put(bird, new WorldPoint(3001, 3000, 0));

		final List<ForestryMarks.Target> only = targets(false);
		assertEquals(2, only.size());
		assertEquals("Empty nest", labelOf(only, empty));
		assertEquals("Eggs here", labelOf(only, forester));
		assertEquals("missing", labelOf(only, full));
		assertEquals(4, targets(true).size());
	}

	@Test
	public void aPheasantOnAnotherPlaneDoesNotFillANest()
	{
		final Object nest = object(49936);
		final Object bird = npc(12537);
		tiles.put(nest, new WorldPoint(3000, 3000, 0));
		tiles.put(bird, new WorldPoint(3000, 3000, 1));
		assertEquals(1, targets(false).size());
	}

	// ---- Friendly Ent ----

	@Test
	public void anEntlingShowsThePruneItsRequestCallsFor()
	{
		final Object entling = npc(12543);
		final Object done = npc(12544);
		assertEquals("Prune", labelOf(targets(false), entling));

		marks.entlingSaid(entling, "Breezy at the back!");
		assertEquals("Prune back", labelOf(targets(false), entling));
		marks.entlingSaid(entling, "A leafy mullet!");
		assertEquals("Prune top + sides", labelOf(targets(false), entling));
		marks.entlingSaid(entling, "Thanks, friend");
		assertEquals("Prune top + sides", labelOf(targets(false), entling));
		assertEquals("missing", labelOf(targets(true), done));
	}

	@Test
	public void everyEntlingRequestOnTheWikiHasAPrune()
	{
		assertEquals("Prune back", ForestryMarks.entlingPrune("Breezy at the back!"));
		assertEquals("Prune top + sides", ForestryMarks.entlingPrune("A leafy mullet!"));
		assertEquals("Prune back + sides", ForestryMarks.entlingPrune("Short back and sides!"));
		assertEquals("Prune top", ForestryMarks.entlingPrune("Short on top!"));
		assertNull(ForestryMarks.entlingPrune("Hello"));
	}

	// ---- the rest ----

	@Test
	public void theRainbowTheTrapsAndTheBeeboxesAreTheTargets()
	{
		npc(12163);
		final Object rainbow = object(49934);
		final Object trap = npc(12541);
		npc(12559);
		final Object box = npc(12516);

		final List<ForestryMarks.Target> only = targets(false);
		assertEquals(3, only.size());
		assertEquals("Stand under", labelOf(only, rainbow));
		assertEquals("Disarm", labelOf(only, trap));
		assertEquals("Add logs", labelOf(only, box));
		assertEquals(5, targets(true).size());
	}

	@Test
	public void aSwitchedOffKindIsNotDrawnAndClearForgetsAll()
	{
		object(47483);
		assertTrue(marks.targets(kind -> false, true, view).isEmpty());
		marks.clear();
		assertTrue(targets(true).isEmpty());
	}

	@Test
	public void spawnIdsMapToTheirEvent()
	{
		assertEquals(SkillingEvents.Forestry.ENCHANTMENT_RITUAL, ForestryMarks.kindOfNpc(12528));
		assertEquals(SkillingEvents.Forestry.STRUGGLING_SAPLING, ForestryMarks.kindOfObject(47499));
		assertNull(ForestryMarks.kindOfNpc(1));
		assertNull(ForestryMarks.kindOfObject(1));
	}
}
