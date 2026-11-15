package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The failure cases first: an alert that fires twice for one event, fires for
 * someone else's random event, or fires on logging in is the risk. Then each
 * chat line and spawn fires its alert once.
 */
public class SkillingEventsTest
{
	private static final String NEST = "A bird's nest falls out of the tree.";
	private static final int WC_XP = 1_000_000;

	private final SkillingEvents events = new SkillingEvents();
	private final Object root1 = new Object();
	private final Object root2 = new Object();

	// --- failure cases ---

	@Test
	public void loggingInIsNotALevelUp()
	{
		events.stat(Skill.WOODCUTTING, 13_034_431, 99, 1);
		events.stat(Skill.ATTACK, 0, 1, 1);
		assertTrue(events.tick(1).isEmpty());
	}

	@Test
	public void aRandomEventForSomeoneElseIsNeverPassedIn()
	{
		// The plugin's gate is the static check plus "targets the local
		// player"; an NPC that is not a random event never counts.
		assertFalse(SkillingEvents.isRandomEventNpc(3010));
		assertTrue(SkillingEvents.isRandomEventNpc(326));
		assertTrue(SkillingEvents.isRandomEventNpc(12552));
	}

	@Test
	public void aRandomEventFiresOnceThenWaitsOutTheCooldown()
	{
		events.randomEvent("Genie", 10);
		assertEquals(1, of(events.tick(10), ActivityAlerts.Type.RANDOM_EVENT));
		// It keeps talking to you: one alert, as the base plugin.
		events.randomEvent("Genie", 12);
		events.randomEvent("Genie", 100);
		assertTrue(events.tick(100).isEmpty());
		events.randomEvent("Genie", 10 + SkillingEvents.RANDOM_EVENT_COOLDOWN + 1);
		assertEquals(1, of(events.tick(161), ActivityAlerts.Type.RANDOM_EVENT));
	}

	@Test
	public void oneForestryEventIsOneAlertHoweverManyParts()
	{
		chopping(5);
		events.forestrySpawned(SkillingEvents.Forestry.RISING_ROOTS, root1, true, 6);
		events.forestrySpawned(SkillingEvents.Forestry.RISING_ROOTS, root2, true, 6);
		assertEquals(1, of(events.tick(6), ActivityAlerts.Type.FORESTRY_EVENT));
		// A root chopped and another rising elsewhere is the same event.
		events.forestryDespawned(SkillingEvents.Forestry.RISING_ROOTS, root1, 8);
		events.forestryDespawned(SkillingEvents.Forestry.RISING_ROOTS, root2, 8);
		events.tick(8);
		events.forestrySpawned(SkillingEvents.Forestry.RISING_ROOTS, new Object(), true, 10);
		assertTrue(events.tick(10).isEmpty());
	}

	@Test
	public void aForestryEventCanFireAgainOnceTheLastOneIsLongGone()
	{
		chopping(5);
		events.forestrySpawned(SkillingEvents.Forestry.BEE_HIVE, root1, true, 6);
		events.tick(6);
		events.forestryDespawned(SkillingEvents.Forestry.BEE_HIVE, root1, 7);
		final int later = 7 + SkillingEvents.FORESTRY_REARM_TICKS + 1;
		chopping(later);
		events.forestrySpawned(SkillingEvents.Forestry.BEE_HIVE, root2, true, later);
		assertEquals(1, of(events.tick(later), ActivityAlerts.Type.FORESTRY_EVENT));
	}

	@Test
	public void aForestryEventFarAwayOrWhileNotChoppingIsQuiet()
	{
		events.forestrySpawned(SkillingEvents.Forestry.FRIENDLY_ENT, root1, true, 6);
		assertTrue("not chopping", events.tick(6).isEmpty());

		final SkillingEvents far = new SkillingEvents();
		far.stat(Skill.WOODCUTTING, WC_XP, 80, 1);
		far.stat(Skill.WOODCUTTING, WC_XP + 100, 80, 5);
		far.forestrySpawned(SkillingEvents.Forestry.FRIENDLY_ENT, root1, false, 6);
		assertTrue("far away", far.tick(6).isEmpty());

		final SkillingEvents stale = new SkillingEvents();
		stale.stat(Skill.WOODCUTTING, WC_XP, 80, 1);
		stale.stat(Skill.WOODCUTTING, WC_XP + 100, 80, 5);
		final int late = 5 + SkillingEvents.WOODCUTTING_RECENT_TICKS + 1;
		stale.forestrySpawned(SkillingEvents.Forestry.FRIENDLY_ENT, root1, true, late);
		assertTrue("stopped chopping a while ago", stale.tick(late).isEmpty());
	}

	@Test
	public void reloadingTheSceneIsNotANewForestryEvent()
	{
		chopping(5);
		events.forestrySpawned(SkillingEvents.Forestry.RISING_ROOTS, root1, true, 6);
		events.tick(6);
		// Crossing a region border reloads every object in view.
		// Long enough after that a genuinely new event would fire.
		final int later = 6 + SkillingEvents.FORESTRY_REARM_TICKS + 2;
		chopping(later);
		events.sceneLoading();
		events.forestrySpawned(SkillingEvents.Forestry.RISING_ROOTS, root2, true, later);
		assertTrue(events.tick(later).isEmpty());
	}

	@Test
	public void aClueScrollChangingStepIsNotAClueDrop()
	{
		// Solving a step swaps one clue scroll for another; only geodes,
		// bottles, nests and scroll boxes are drops.
		assertNull(SkillingEvents.clueTier("Clue scroll (hard)"));
		events.inventoryGained("Clue scroll (hard)", true);
		assertTrue(events.tick(3).isEmpty());
	}

	@Test
	public void aClueFromTheBankIsNotADrop()
	{
		events.inventoryGained("Clue geode (easy)", false);
		assertTrue(events.tick(3).isEmpty());
	}

	@Test
	public void pickingUpAClueNestIsNotASecondAlert()
	{
		// The nest already alerted when it fell; picking it up is quiet.
		events.inventoryGained("Clue nest (elite)", true);
		assertTrue(events.tick(3).isEmpty());
	}

	@Test
	public void clearMakesEverythingABaselineAgain()
	{
		events.stat(Skill.MINING, 0, 1, 1);
		events.chat(NEST);
		events.randomEvent("Genie", 2);
		events.clear();
		events.stat(Skill.MINING, 500, 5, 3);
		assertTrue(events.tick(3).isEmpty());
	}

	// --- each fires once ---

	@Test
	public void aBirdNestFiresOnce()
	{
		events.chat(NEST);
		final List<ActivityAlerts.Alert> fired = events.tick(4);
		assertEquals(1, fired.size());
		assertSame(ActivityAlerts.Type.BIRD_NEST, fired.get(0).type);
		assertNull(fired.get(0).detail);
		assertTrue(events.tick(5).isEmpty());
	}

	@Test
	public void aClueNestSaysItsTier()
	{
		// The ground item spawns before the chat line (base Woodcutting plugin).
		events.groundItem("Bird nest", 4);
		events.groundItem("Clue nest (hard)", 4);
		events.chat(NEST);
		final List<ActivityAlerts.Alert> fired = events.tick(4);
		assertEquals(1, fired.size());
		assertSame(ActivityAlerts.Type.CLUE_DROP, fired.get(0).type);
		assertEquals("Clue nest (hard)", fired.get(0).detail);
		assertEquals("CLUE_DROP|hard", SkillingEvents.voiceKey(fired.get(0)));
	}

	@Test
	public void anOldClueNestOnTheGroundIsNotThisNest()
	{
		events.groundItem("Clue nest (hard)", 1);
		events.tick(1);
		events.chat(NEST);
		final List<ActivityAlerts.Alert> fired = events.tick(9);
		assertSame(ActivityAlerts.Type.BIRD_NEST, fired.get(0).type);
	}

	@Test
	public void aClueNestGoesToBirdNestWhenClueDropIsOff()
	{
		final ActivityAlerts.Alert clue =
			new ActivityAlerts.Alert(ActivityAlerts.Type.CLUE_DROP, null, "Clue nest (easy)");
		assertSame(clue, SkillingEvents.route(clue, true));
		final ActivityAlerts.Alert nest = SkillingEvents.route(clue, false);
		assertSame(ActivityAlerts.Type.BIRD_NEST, nest.type);
		assertEquals("Clue nest (easy)", nest.detail);
		assertEquals("BIRD_NEST|clue", SkillingEvents.voiceKey(nest));

		// A geode has no nest to fall back to.
		final ActivityAlerts.Alert geode =
			new ActivityAlerts.Alert(ActivityAlerts.Type.CLUE_DROP, Skill.MINING, "Clue geode (easy)");
		assertSame(geode, SkillingEvents.route(geode, false));
	}

	@Test
	public void eachSkillingClueFiresOnce()
	{
		for (String item : new String[]{"Clue geode (beginner)", "Clue bottle (medium)", "Scroll box (elite)"})
		{
			final SkillingEvents fresh = new SkillingEvents();
			fresh.inventoryGained(item, true);
			final List<ActivityAlerts.Alert> fired = fresh.tick(3);
			assertEquals(item, 1, fired.size());
			assertSame(ActivityAlerts.Type.CLUE_DROP, fired.get(0).type);
			assertEquals(item, fired.get(0).detail);
			assertTrue(fresh.tick(4).isEmpty());
		}
		assertEquals("beginner", SkillingEvents.clueTier("Clue geode (beginner)"));
		assertEquals("master", SkillingEvents.clueTier("Scroll box (master)"));
	}

	@Test
	public void eachPetLineFiresOnce()
	{
		events.chat("You have a funny feeling like you're being followed.");
		ActivityAlerts.Alert pet = only(events.tick(2));
		assertSame(ActivityAlerts.Type.PET, pet.type);
		assertNull(SkillingEvents.voiceKey(pet));

		events.chat("You feel something weird sneaking into your backpack.");
		assertSame(ActivityAlerts.Type.PET, only(events.tick(3)).type);

		events.chat("You have a funny feeling like you would have been followed...");
		pet = only(events.tick(4));
		assertSame(ActivityAlerts.Type.PET, pet.type);
		assertEquals("PET|duplicate", SkillingEvents.voiceKey(pet));
	}

	@Test
	public void aLineInTwoChatTypesIsOneAlert()
	{
		events.chat(NEST);
		events.chat(NEST);
		events.chat("You have a funny feeling like you're being followed.");
		events.chat("You have a funny feeling like you're being followed.");
		final List<ActivityAlerts.Alert> fired = events.tick(2);
		assertEquals(fired.toString(), 2, fired.size());
	}

	@Test
	public void otherChatIsIgnored()
	{
		events.chat("You get some yew logs.");
		events.chat(null);
		assertTrue(events.tick(2).isEmpty());
	}

	@Test
	public void aLevelUpFiresOncePerLevel()
	{
		events.stat(Skill.WOODCUTTING, WC_XP, 80, 1);
		events.stat(Skill.WOODCUTTING, WC_XP + 10_000, 81, 2);
		final ActivityAlerts.Alert up = only(events.tick(2));
		assertSame(ActivityAlerts.Type.LEVEL_UP, up.type);
		assertSame(Skill.WOODCUTTING, up.skill);
		assertEquals("81", up.detail);
		events.stat(Skill.WOODCUTTING, WC_XP + 10_100, 81, 3);
		assertTrue(events.tick(3).isEmpty());
	}

	@Test
	public void theLevelUpFilterKeepsTheSkillingSkills()
	{
		assertTrue(SkillingEvents.levelWanted(Skill.ATTACK, LevelUpSkills.ALL));
		assertFalse(SkillingEvents.levelWanted(Skill.ATTACK, LevelUpSkills.SKILLING));
		assertFalse(SkillingEvents.levelWanted(Skill.HITPOINTS, LevelUpSkills.SKILLING));
		assertFalse(SkillingEvents.levelWanted(Skill.PRAYER, LevelUpSkills.SKILLING));
		assertTrue(SkillingEvents.levelWanted(Skill.WOODCUTTING, LevelUpSkills.SKILLING));
		assertTrue(SkillingEvents.levelWanted(Skill.THIEVING, LevelUpSkills.SKILLING));
	}

	@Test
	public void eachForestryEventFiresOnce()
	{
		chopping(5);
		int tick = 6;
		for (SkillingEvents.Forestry kind : SkillingEvents.Forestry.values())
		{
			events.forestrySpawned(kind, new Object(), true, tick);
			final ActivityAlerts.Alert alert = only(events.tick(tick));
			assertSame(ActivityAlerts.Type.FORESTRY_EVENT, alert.type);
			assertEquals(kind.title, alert.detail);
			assertEquals("FORESTRY_EVENT|" + kind.name(), SkillingEvents.voiceKey(alert));
			tick++;
		}
	}

	@Test
	public void forestryIdsAreTheBaseWoodcuttingPlugins()
	{
		assertSame(SkillingEvents.Forestry.RISING_ROOTS, SkillingEvents.forestryObject(47482));
		assertSame(SkillingEvents.Forestry.STRUGGLING_SAPLING, SkillingEvents.forestryObject(47491));
		assertNull(SkillingEvents.forestryObject(1276));
		assertSame(SkillingEvents.Forestry.FLOWERING_BUSH, SkillingEvents.forestryNpc(12151));
		assertSame(SkillingEvents.Forestry.LEPRECHAUN, SkillingEvents.forestryNpc(12163));
		assertSame(SkillingEvents.Forestry.POACHERS, SkillingEvents.forestryNpc(12560));
		assertSame(SkillingEvents.Forestry.PHEASANT_CONTROL, SkillingEvents.forestryNpc(12536));
		assertSame(SkillingEvents.Forestry.BEE_HIVE, SkillingEvents.forestryNpc(12518));
		assertSame(SkillingEvents.Forestry.ENCHANTMENT_RITUAL, SkillingEvents.forestryNpc(12519));
		assertSame(SkillingEvents.Forestry.FRIENDLY_ENT, SkillingEvents.forestryNpc(12543));
		assertNull(SkillingEvents.forestryNpc(12544));
	}

	@Test
	public void messagesSayWhatHappened()
	{
		assertEquals("A bird's nest fell", text(ActivityAlerts.Type.BIRD_NEST, null, null));
		assertEquals("A clue nest (easy) fell", text(ActivityAlerts.Type.BIRD_NEST, null, "Clue nest (easy)"));
		assertEquals("Clue drop: Clue geode (hard)", text(ActivityAlerts.Type.CLUE_DROP, null, "Clue geode (hard)"));
		assertEquals("A Forestry event started: Rising Roots",
			text(ActivityAlerts.Type.FORESTRY_EVENT, null, "Rising Roots"));
		assertEquals("You have a funny feeling like you're being followed", text(ActivityAlerts.Type.PET, null, null));
		assertEquals("You would have got a pet, but you already have it",
			text(ActivityAlerts.Type.PET, null, SkillingEvents.DUPLICATE));
		assertEquals("Woodcutting level 81", text(ActivityAlerts.Type.LEVEL_UP, Skill.WOODCUTTING, "81"));
		assertEquals("A random event is here for you: Genie", text(ActivityAlerts.Type.RANDOM_EVENT, null, "Genie"));
		assertNull(SkillingEvents.message(new ActivityAlerts.Alert(ActivityAlerts.Type.INVENTORY_FULL, null)));
	}

	@Test
	public void onlyTheStopAlertsEndWhenYouAreBackAtIt()
	{
		assertTrue(SkillingEvents.isEvent(ActivityAlerts.Type.BIRD_NEST));
		assertTrue(SkillingEvents.isEvent(ActivityAlerts.Type.RANDOM_EVENT));
		assertFalse(SkillingEvents.isEvent(ActivityAlerts.Type.GATHERING_STOPPED));
	}

	private void chopping(int tick)
	{
		events.stat(Skill.WOODCUTTING, WC_XP, 80, tick - 1);
		events.stat(Skill.WOODCUTTING, WC_XP + tick, 80, tick);
	}

	private static String text(ActivityAlerts.Type type, Skill skill, String detail)
	{
		return SkillingEvents.message(new ActivityAlerts.Alert(type, skill, detail));
	}

	private static ActivityAlerts.Alert only(List<ActivityAlerts.Alert> fired)
	{
		assertEquals(fired.toString(), 1, fired.size());
		return fired.get(0);
	}

	private static int of(List<ActivityAlerts.Alert> fired, ActivityAlerts.Type type)
	{
		int n = 0;
		for (ActivityAlerts.Alert alert : fired)
		{
			if (alert.type == type)
			{
				n++;
			}
		}
		return n;
	}
}
