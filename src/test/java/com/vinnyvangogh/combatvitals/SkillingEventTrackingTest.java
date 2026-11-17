package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

/** Forestry events tracked start to end, and the mining and fishing event lines. */
public class SkillingEventTrackingTest
{
	private static final int XP = 1_000_000;

	private SkillingEvents events;
	private final Object part = new Object();

	@Before
	public void setUp()
	{
		events = new SkillingEvents();
		events.stat(Skill.WOODCUTTING, XP, 80, 1);
		events.stat(Skill.WOODCUTTING, XP + 100, 80, 5);
	}

	@Test
	public void anEventSumsTheExperienceEarnedWhileItRanAndEndsWhenItsPartsAreGone()
	{
		final SkillingEvents.Forestry kind = SkillingEvents.Forestry.PHEASANT_CONTROL;
		events.forestrySpawned(kind, part, true, 6);
		events.tick(6);
		events.stat(Skill.WOODCUTTING, XP + 150, 80, 7);
		events.stat(Skill.WOODCUTTING, XP + 220, 80, 9);
		events.inventoryGained("Anima-infused bark", true);
		events.inventoryGained("Yew logs", true);
		events.forestryDespawned(kind, part, 10);
		events.tick(10);
		assertTrue("not over yet", events.takeSummaries().isEmpty());
		events.tick(10 + SkillingEvents.EVENT_END_TICKS);
		assertEquals(java.util.Collections.singletonList(kind.title + ": 120 xp, 1 x Anima-infused bark"),
			events.takeSummaries());
		assertTrue("taken once", events.takeSummaries().isEmpty());
	}

	@Test
	public void experienceBeforeTheEventIsNotItsContribution()
	{
		final SkillingEvents.Forestry kind = SkillingEvents.Forestry.RISING_ROOTS;
		events.forestrySpawned(kind, part, true, 6);
		events.forestryDespawned(kind, part, 7);
		events.tick(7 + SkillingEvents.EVENT_END_TICKS);
		assertEquals(java.util.Collections.singletonList(kind.title + ": no contribution"), events.takeSummaries());
	}

	@Test
	public void aNewEventClosesTheOldOnesSummary()
	{
		events.forestrySpawned(SkillingEvents.Forestry.RISING_ROOTS, part, true, 6);
		events.stat(Skill.WOODCUTTING, XP + 160, 80, 8);
		events.forestrySpawned(SkillingEvents.Forestry.BEE_HIVE, new Object(), true, 9);
		final List<String> done = events.takeSummaries();
		assertEquals(1, done.size());
		assertTrue(done.get(0), done.get(0).startsWith(SkillingEvents.Forestry.RISING_ROOTS.title + ": 60 xp"));
	}

	@Test
	public void aGemWhileMiningIsAMiningEvent()
	{
		events.chat("You just found a Sapphire!");
		final List<ActivityAlerts.Alert> fired = events.tick(20);
		assertEquals(1, fired.size());
		assertSame(ActivityAlerts.Type.SKILLING_EVENT, fired.get(0).type);
		assertSame(Skill.MINING, fired.get(0).skill);
		assertEquals("Gem found: Sapphire", SkillingEvents.message(fired.get(0)));
	}

	@Test
	public void aClueBottleWhileFishingIsAFishingEvent()
	{
		events.chat("You catch a clue bottle.");
		final List<ActivityAlerts.Alert> fired = events.tick(20);
		assertEquals(1, fired.size());
		assertSame(Skill.FISHING, fired.get(0).skill);
		assertEquals("Clue bottle caught", SkillingEvents.message(fired.get(0)));
	}

	@Test
	public void ordinaryLinesAreNotEvents()
	{
		events.chat("You catch a trout.");
		events.chat("You manage to mine some iron.");
		events.chat("You just found nothing");
		assertTrue(events.tick(20).isEmpty());
	}

	@Test
	public void theMiningAndFishingAlertIsOffByDefaultAndIsASkillingEvent()
	{
		assertTrue(SkillingEvents.isEvent(ActivityAlerts.Type.SKILLING_EVENT));
	}
}
