package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

/**
 * The failure cases first: walking, banking, logging out, stuns and a full
 * inventory must never read as "you stopped". Then each trigger fires once,
 * and only on the edge it is about.
 */
public class ActivityAlertsTest
{
	private static final int STOP = 8;
	private static final int CAP = 28;
	private static final int LOW = 10;

	private ActivityAlerts alerts;
	private int tick;
	private int fishingXp;

	@Before
	public void setUp()
	{
		alerts = new ActivityAlerts();
		tick = 100;
		fishingXp = 1_000_000;
		// Login reports every skill: the baseline, never a drop.
		alerts.xp(Skill.FISHING, fishingXp);
		alerts.xp(Skill.THIEVING, 500_000);
		alerts.xp(Skill.COOKING, 200_000);
		assertTrue(step(null).isEmpty());
	}

	// --- Stop detector: things that must not fire -------------------------

	@Test
	public void loginBaselineIsNotActivity()
	{
		assertNull(alerts.active());
		assertTrue(idle(STOP * 3).isEmpty());
	}

	@Test
	public void walkingToTheNextSpotDoesNotFire()
	{
		catchFish();
		final ActivityAlerts.Tick walk = frame();
		walk.moved = true;
		assertTrue(step(walk).isEmpty());
		assertNull(alerts.active());
		assertTrue(idle(STOP * 2).isEmpty());
	}

	@Test
	public void bankingDoesNotFire()
	{
		catchFish();
		alerts.banked();
		assertTrue(step(null).isEmpty());
		assertTrue(idle(STOP * 2).isEmpty());
	}

	@Test
	public void depositingOrDroppingDoesNotFire()
	{
		ActivityAlerts.Tick t = frame();
		t.filling = 20;
		t.empty = 8;
		alerts.xp(Skill.FISHING, ++fishingXp);
		step(t);

		t = frame();
		t.filling = 0;
		t.empty = 28;
		assertTrue(step(t).isEmpty());
		assertTrue(idle(STOP * 2).isEmpty());
	}

	@Test
	public void logoutClearsEverything()
	{
		catchFish();
		alerts.clear();
		assertTrue(idle(STOP * 2).isEmpty());

		// And the first experience after logging back in is a baseline again.
		alerts.xp(Skill.FISHING, fishingXp + 500);
		step(null);
		assertNull(alerts.active());
	}

	@Test
	public void animationKeepsItAlive()
	{
		catchFish();
		for (int i = 0; i < STOP * 3; i++)
		{
			final ActivityAlerts.Tick t = frame();
			t.animating = true;
			assertTrue(step(t).isEmpty());
		}
		assertEquals(Skill.FISHING, alerts.active());
	}

	@Test
	public void stunIsNotStopping()
	{
		alerts.xp(Skill.THIEVING, 500_100);
		step(null);
		alerts.stunned();
		assertEquals(single(ActivityAlerts.Type.STUNNED), types(step(null)));

		// The stun plus the threshold passes before "stopped" may fire.
		assertTrue(idle(ActivityAlerts.STUN_TICKS + STOP - 2).isEmpty());
		assertEquals(single(ActivityAlerts.Type.THIEVING_STOPPED), types(idle(2)));
	}

	@Test
	public void movingWhileGainingXpIsPickpocketingAWanderer()
	{
		alerts.xp(Skill.THIEVING, 500_100);
		final ActivityAlerts.Tick t = frame();
		t.moved = true;
		step(t);
		assertEquals(Skill.THIEVING, alerts.active());
	}

	// --- Stop detector: fires once ----------------------------------------

	@Test
	public void standingIdleAfterACatchFiresOnceAtTheThreshold()
	{
		catchFish();
		assertTrue(idle(STOP - 1).isEmpty());

		final List<ActivityAlerts.Alert> fired = idle(1);
		assertEquals(single(ActivityAlerts.Type.GATHERING_STOPPED), types(fired));
		assertEquals(Skill.FISHING, fired.get(0).skill);

		assertTrue("once, not every tick after", idle(STOP * 3).isEmpty());
	}

	@Test
	public void makeXRunningOutIsFinished()
	{
		alerts.xp(Skill.COOKING, 200_100);
		step(null);
		final List<ActivityAlerts.Alert> fired = idle(STOP);
		assertEquals(single(ActivityAlerts.Type.PRODUCTION_FINISHED), types(fired));
		assertEquals(Skill.COOKING, fired.get(0).skill);
	}

	@Test
	public void untrackedSkillsNeverArm()
	{
		alerts.xp(Skill.ATTACK, 100);
		alerts.xp(Skill.ATTACK, 200);
		step(null);
		assertNull(alerts.active());
	}

	@Test
	public void secondsBecomeTicks()
	{
		assertEquals(8, ActivityAlerts.ticks(5));
		assertEquals(2, ActivityAlerts.ticks(1));
		assertEquals(1, ActivityAlerts.ticks(0));
	}

	// --- Inventory full ---------------------------------------------------

	@Test
	public void inventoryFillingWhileFishingFiresAndSuppressesStopped()
	{
		ActivityAlerts.Tick t = frame();
		t.filling = 26;
		t.empty = 1;
		alerts.xp(Skill.FISHING, ++fishingXp);
		assertTrue(step(t).isEmpty());

		t = frame();
		t.filling = 27;
		t.empty = 0;
		alerts.xp(Skill.FISHING, ++fishingXp);
		assertEquals(single(ActivityAlerts.Type.INVENTORY_FULL), types(step(t)));

		assertTrue("full already said it", idleWith(STOP * 2, 27, 0).isEmpty());
	}

	@Test
	public void firstInventorySeenIsABaseline()
	{
		catchFish();
		final ActivityAlerts.Tick t = frame();
		t.filling = 28;
		t.empty = 0;
		assertTrue(step(t).isEmpty());
	}

	@Test
	public void inventoryFillingWhileNotGatheringIsQuiet()
	{
		ActivityAlerts.Tick t = frame();
		t.filling = 20;
		t.empty = 8;
		step(t);
		// Withdrawing from the bank, say.
		t = frame();
		t.filling = 28;
		t.empty = 0;
		assertTrue(step(t).isEmpty());
	}

	@Test
	public void inventoryFillingDuringMakeXIsQuiet()
	{
		ActivityAlerts.Tick t = frame();
		t.filling = 27;
		t.empty = 1;
		step(t);
		alerts.xp(Skill.COOKING, 200_100);
		t = frame();
		t.filling = 28;
		t.empty = 0;
		assertTrue(step(t).isEmpty());
	}

	// --- Spot moved ---------------------------------------------------------

	@Test
	public void spotMovingFiresAndSuppressesStopped()
	{
		catchFish();
		alerts.spotMoved(Skill.FISHING);
		final List<ActivityAlerts.Alert> fired = step(null);
		assertEquals(single(ActivityAlerts.Type.SPOT_MOVED), types(fired));
		assertTrue(idle(STOP * 2).isEmpty());
	}

	// --- Thresholds -----------------------------------------------------------

	@Test
	public void coinPouchesFireOnReachingTheCapOnce()
	{
		assertTrue(pouches(26).isEmpty());
		assertTrue(pouches(27).isEmpty());
		assertEquals(single(ActivityAlerts.Type.COIN_POUCH_CAP), types(pouches(28)));
		assertTrue(pouches(28).isEmpty());
		// Opened them: re-armed.
		assertTrue(pouches(0).isEmpty());
		assertTrue(pouches(27).isEmpty());
		assertEquals(single(ActivityAlerts.Type.COIN_POUCH_CAP), types(pouches(28)));
	}

	@Test
	public void loggingInAtTheCapIsQuiet()
	{
		assertTrue(pouches(28).isEmpty());
		assertTrue(pouches(28).isEmpty());
	}

	@Test
	public void runEnergyFiresOnCrossingDown()
	{
		assertTrue(energy(50).isEmpty());
		assertTrue(energy(10).isEmpty());
		assertEquals(single(ActivityAlerts.Type.LOW_RUN_ENERGY), types(energy(9)));
		assertTrue(energy(5).isEmpty());
		assertTrue(energy(30).isEmpty());
		assertEquals(single(ActivityAlerts.Type.LOW_RUN_ENERGY), types(energy(8)));
	}

	@Test
	public void necklaceBreakFiresOnce()
	{
		alerts.necklaceBroke();
		assertEquals(single(ActivityAlerts.Type.NECKLACE_BROKE), types(step(null)));
		assertTrue(step(null).isEmpty());
	}

	// --- Messages ---------------------------------------------------------

	@Test
	public void messagesAreRecognised()
	{
		assertTrue(ActivityAlerts.isStunMessage("You've been stunned!"));
		assertFalse(ActivityAlerts.isStunMessage("You fail to pick the man's pocket."));
		assertTrue(ActivityAlerts.isNecklaceBreakMessage(
			"Your dodgy necklace protects you. It then crumbles to dust."));
		assertFalse(ActivityAlerts.isNecklaceBreakMessage(
			"Your dodgy necklace protects you. It has 3 charges left."));
		assertFalse(ActivityAlerts.isStunMessage(null));
		assertFalse(ActivityAlerts.isNecklaceBreakMessage(null));
	}

	@Test
	public void messagesReadAsSentences()
	{
		assertEquals("You have stopped fishing", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.GATHERING_STOPPED, Skill.FISHING), CAP));
		assertEquals("You have stopped hunting", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.GATHERING_STOPPED, Skill.HUNTER), CAP));
		assertEquals("You have finished cooking", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.PRODUCTION_FINISHED, Skill.COOKING), CAP));
		assertEquals("Your inventory is full while woodcutting", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.INVENTORY_FULL, Skill.WOODCUTTING), CAP));
		assertEquals("You have 28 coin pouches: open them", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.COIN_POUCH_CAP, Skill.THIEVING), CAP));
	}

	// --- Helpers ------------------------------------------------------------

	private void catchFish()
	{
		alerts.xp(Skill.FISHING, ++fishingXp);
		assertTrue(step(null).isEmpty());
		assertEquals(Skill.FISHING, alerts.active());
	}

	private ActivityAlerts.Tick frame()
	{
		final ActivityAlerts.Tick t = new ActivityAlerts.Tick();
		t.tick = ++tick;
		return t;
	}

	private List<ActivityAlerts.Alert> step(ActivityAlerts.Tick t)
	{
		return alerts.tick(t == null ? frame() : t, STOP, CAP, LOW);
	}

	private List<ActivityAlerts.Alert> idle(int ticks)
	{
		return idleWith(ticks, -1, -1);
	}

	private List<ActivityAlerts.Alert> idleWith(int ticks, int filling, int empty)
	{
		final List<ActivityAlerts.Alert> all = new java.util.ArrayList<>();
		for (int i = 0; i < ticks; i++)
		{
			final ActivityAlerts.Tick t = frame();
			t.filling = filling;
			t.empty = empty;
			all.addAll(step(t));
		}
		return all;
	}

	private List<ActivityAlerts.Alert> pouches(int count)
	{
		final ActivityAlerts.Tick t = frame();
		t.coinPouches = count;
		return step(t);
	}

	private List<ActivityAlerts.Alert> energy(int percent)
	{
		final ActivityAlerts.Tick t = frame();
		t.runEnergy = percent;
		return step(t);
	}

	private static List<ActivityAlerts.Type> single(ActivityAlerts.Type type)
	{
		return java.util.Collections.singletonList(type);
	}

	private static List<ActivityAlerts.Type> types(List<ActivityAlerts.Alert> alerts)
	{
		final List<ActivityAlerts.Type> out = new java.util.ArrayList<>();
		for (ActivityAlerts.Alert alert : alerts)
		{
			out.add(alert.type);
		}
		return out;
	}
}
