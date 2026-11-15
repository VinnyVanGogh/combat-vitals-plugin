package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

/**
 * The pump's three checks, driven by fake ticks as the plugin would feed
 * them: the world, Strength experience while the pumping animation plays,
 * and the stove's look (its object id) as it burns down and is refuelled.
 */
public class PumpChecksTest
{
	private static final Set<Integer> WORLDS = new HashSet<>(Arrays.asList(302, 303));
	private static final long WARN = 60_000;

	private PumpChecks checks;

	@Before
	public void setUp()
	{
		checks = new PumpChecks();
	}

	/** One tick pumping: running, animating, and whether Strength experience came. */
	private List<ActivityAlerts.Type> pump(int tick, int world, boolean xp)
	{
		return tick(tick, true, true, xp, world);
	}

	private List<ActivityAlerts.Type> tick(int tick, boolean running, boolean animating, boolean xp, int world)
	{
		final List<ActivityAlerts.Type> types = new ArrayList<>();
		for (ActivityAlerts.Alert alert : checks.tick(tick, running, animating, xp, world, WORLDS, WARN))
		{
			assertSame(TrainingMethod.BLAST_FURNACE_PUMP, alert.method);
			assertEquals(Skill.STRENGTH, alert.skill);
			types.add(alert.type);
		}
		return types;
	}

	private static List<ActivityAlerts.Type> single(ActivityAlerts.Type type)
	{
		return Collections.singletonList(type);
	}

	// --- world list ---

	@Test
	public void theWorldListReadsNumbersAndSkipsTheRest()
	{
		assertEquals(new HashSet<>(Arrays.asList(302, 303)), PumpChecks.worlds("302, 303"));
		assertEquals(new HashSet<>(Arrays.asList(302, 330)), PumpChecks.worlds(" 302;330  w99999 x"));
		assertTrue(PumpChecks.worlds("").isEmpty());
		assertTrue(PumpChecks.worlds(null).isEmpty());
		assertEquals(new HashSet<>(Arrays.asList(302, 303)), PumpChecks.worlds(PumpChecks.DEFAULT_WORLDS));
	}

	@Test
	public void onAListedWorldThereIsNoWarning()
	{
		assertTrue(pump(1, 302, true).isEmpty());
		assertFalse(checks.wrongWorld());
		assertNull(checks.warning());
	}

	@Test
	public void offTheListTheBadgeSaysSoAndTheAlertFiresOnce()
	{
		assertEquals(single(ActivityAlerts.Type.WRONG_WORLD), pump(1, 420, true));
		assertTrue(checks.wrongWorld());
		assertEquals("World 420: not a pumping world", checks.warning());
		for (int t = 2; t < 50; t++)
		{
			assertTrue(pump(t, 420, true).isEmpty());
		}
		assertTrue("still shown", checks.wrongWorld());
	}

	@Test
	public void anEmptyListTurnsTheWorldCheckOff()
	{
		final List<ActivityAlerts.Alert> out = checks.tick(1, true, true, true, 420, Collections.emptySet(), WARN);
		assertTrue(out.isEmpty());
		assertFalse(checks.wrongWorld());
	}

	@Test
	public void notPumpingNothingIsShownOrSent()
	{
		assertTrue(tick(1, false, false, false, 420).isEmpty());
		assertFalse(checks.wrongWorld());
		assertNull(checks.warning());
	}

	@Test
	public void hoppingToAnotherWrongWorldWarnsAgain()
	{
		assertEquals(single(ActivityAlerts.Type.WRONG_WORLD), pump(1, 420, true));
		checks.clear();
		assertEquals(single(ActivityAlerts.Type.WRONG_WORLD), pump(2, 421, true));
	}

	// --- stoking ---

	@Test
	public void experienceEveryTickMeansSomeoneIsStoking()
	{
		for (int t = 1; t < 100; t++)
		{
			assertTrue(pump(t, 302, true).isEmpty());
		}
		assertFalse(checks.noStoking());
	}

	@Test
	public void pumpingWithoutExperienceMeansAnEmptyStove()
	{
		pump(1, 302, true);
		for (int t = 2; t < 2 + PumpChecks.NO_XP_TICKS - 1; t++)
		{
			assertTrue("a short gap is not news", pump(t, 302, false).isEmpty());
			assertFalse(checks.noStoking());
		}
		assertEquals(single(ActivityAlerts.Type.NO_STOKING), pump(1 + PumpChecks.NO_XP_TICKS, 302, false));
		assertTrue(checks.noStoking());
		assertEquals("Stove: no one stoking", checks.warning());
		assertTrue("once", pump(2 + PumpChecks.NO_XP_TICKS, 302, false).isEmpty());
	}

	@Test
	public void experienceAgainClearsItAndArmsTheNextAlert()
	{
		int t = 1;
		for (; t <= PumpChecks.NO_XP_TICKS; t++)
		{
			pump(t, 302, false);
		}
		assertTrue(checks.noStoking());
		assertTrue(pump(t++, 302, true).isEmpty());
		assertFalse(checks.noStoking());
		final int from = t;
		for (; t < from + PumpChecks.NO_XP_TICKS - 1; t++)
		{
			assertTrue(pump(t, 302, false).isEmpty());
		}
		assertEquals(single(ActivityAlerts.Type.NO_STOKING), pump(t, 302, false));
	}

	@Test
	public void standingStillAtThePumpIsNotAnEmptyStove()
	{
		for (int t = 1; t < 30; t++)
		{
			assertTrue(tick(t, true, false, false, 302).isEmpty());
		}
		assertFalse("no animation, so no pumping to expect experience from", checks.noStoking());
	}

	@Test
	public void bothWarningsShareTheLine()
	{
		pump(1, 420, false);
		for (int t = 2; t <= PumpChecks.NO_XP_TICKS + 1; t++)
		{
			pump(t, 420, false);
		}
		assertEquals("World 420: not a pumping world | Stove: no one stoking", checks.warning());
	}

	@Test
	public void aReconnectStartsTheGapAgain()
	{
		for (int t = 1; t < PumpChecks.NO_XP_TICKS; t++)
		{
			pump(t, 302, false);
		}
		checks.reconnected();
		assertTrue("the gap before the reconnect does not count", pump(PumpChecks.NO_XP_TICKS, 302, false).isEmpty());
	}

	// --- the stove and its coke ---

	@Test
	public void theStoveIsReadByItsLook()
	{
		assertEquals(0, PumpChecks.stoveLevel(PumpChecks.STOVE_NOT_MUCH));
		assertEquals(1, PumpChecks.stoveLevel(PumpChecks.STOVE_FAIR));
		assertEquals(2, PumpChecks.stoveLevel(PumpChecks.STOVE_LOTS));
		assertEquals(-1, PumpChecks.stoveLevel(9090));
		assertTrue(PumpChecks.isStove(9086));
		assertFalse(PumpChecks.isStove(9090));
	}

	@Test
	public void noCountdownUntilOneStepHasBeenTimed()
	{
		checks.stove(2, 0);
		pump(1, 302, true);
		assertEquals(-1, checks.cokeTicksLeft());
		checks.stove(1, 100);
		pump(101, 302, true);
		assertEquals("the first drop only anchors", -1, checks.cokeTicksLeft());
		assertNull(checks.cokeText());
	}

	@Test
	public void theCountdownComesFromTheTimedStep()
	{
		checks.stove(2, 0);
		checks.stove(1, 100);
		checks.stove(0, 300);
		pump(300, 302, true);
		assertEquals("one step of 200 ticks left", 200, checks.cokeTicksLeft());
		pump(350, 302, true);
		assertEquals(150, checks.cokeTicksLeft());
		assertEquals("Coke runs out in 1:30", checks.cokeText());
	}

	@Test
	public void aRefuelBetweenTwoDropsLeavesTheStepUntimed()
	{
		checks.stove(0, 0);
		checks.stove(2, 10);
		checks.stove(1, 110);
		checks.stove(2, 120);
		checks.stove(1, 220);
		pump(220, 302, true);
		assertEquals("the refuel at 120 means 110 to 220 is not one step burnt", -1, checks.cokeTicksLeft());
		checks.stove(0, 320);
		pump(320, 302, true);
		assertEquals(100, checks.cokeTicksLeft());
	}

	@Test
	public void aRefuelKeepsTheRateButNeedsTheNextDropToPlaceIt()
	{
		checks.stove(2, 0);
		checks.stove(1, 100);
		checks.stove(0, 200);
		pump(200, 302, true);
		assertEquals(100, checks.cokeTicksLeft());
		checks.stove(2, 210);
		pump(210, 302, true);
		assertEquals("refuelled: where in the step is unknown", -1, checks.cokeTicksLeft());
		checks.stove(1, 260);
		pump(260, 302, true);
		assertEquals("two steps of the known 100", 200, checks.cokeTicksLeft());
	}

	@Test
	public void theCokeAlertFiresOnceBeforeItRunsOutAndAgainAfterARefuel()
	{
		checks.stove(2, 0);
		checks.stove(1, 200);
		checks.stove(0, 400);
		final int warnTicks = (int) (WARN / 600);
		assertTrue(pump(400, 302, true).isEmpty());
		assertTrue(pump(599 - warnTicks, 302, true).isEmpty());
		assertEquals(single(ActivityAlerts.Type.COKE_LOW), pump(600 - warnTicks, 302, true));
		assertTrue("once", pump(601 - warnTicks, 302, true).isEmpty());
		checks.stove(2, 610);
		checks.stove(1, 700);
		checks.stove(0, 900);
		assertEquals(single(ActivityAlerts.Type.COKE_LOW), pump(1100 - warnTicks, 302, true));
	}

	@Test
	public void theStoveSeenAtLoginNeverAlerts()
	{
		checks.stove(0, 0);
		for (int t = 1; t < 50; t++)
		{
			assertTrue(pump(t, 302, true).isEmpty());
		}
		assertEquals(-1, checks.cokeTicksLeft());
	}

	@Test
	public void noCountdownWhileNotPumping()
	{
		checks.stove(2, 0);
		checks.stove(1, 100);
		checks.stove(0, 200);
		assertTrue(tick(250, false, false, false, 302).isEmpty());
		assertEquals(-1, checks.cokeTicksLeft());
	}

	@Test
	public void theCokeCountdownIsTheLastChip()
	{
		assertEquals("Active | Pump stops in 1:00 | Coke runs out in 1:30",
			SkillingText.methodTimers(0, true, "Pump", 100, "Coke runs out in 1:30"));
		assertEquals("Coke runs out in 1:30", SkillingText.methodTimers(-1, false, "Pump", -1, "Coke runs out in 1:30"));
		assertEquals(SkillingText.methodTimers(0, true, "Pump", 100),
			SkillingText.methodTimers(0, true, "Pump", 100, null));
	}

	@Test
	public void theSessionReportsTheTicksExperience()
	{
		final TrainingSession session = new TrainingSession();
		session.xp(Skill.STRENGTH, 1000);
		session.start(TrainingMethod.BLAST_FURNACE_PUMP, 1, 1);
		session.xp(Skill.STRENGTH, 1002);
		session.tick(2, true, 0, 300_000, 8, WARN);
		assertTrue(session.gainedLastTick());
		session.tick(3, true, 0, 300_000, 8, WARN);
		assertFalse(session.gainedLastTick());
	}

	@Test
	public void loggingOutForgetsEverything()
	{
		checks.stove(2, 0);
		checks.stove(1, 100);
		checks.stove(0, 200);
		checks.clear();
		checks.stove(0, 300);
		pump(300, 302, true);
		assertEquals(-1, checks.cokeTicksLeft());
	}
}
