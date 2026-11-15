package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ObjectID;
import org.junit.Before;
import org.junit.Test;

/**
 * The Blast Furnace pump, driven by fake events as the plugin would feed
 * them: the Operate click, the pumping animation, Strength experience every
 * tick, a fight, walking off, the idle logout and the game's 100 minute stop.
 */
public class TrainingSessionTest
{
	private static final TrainingMethod PUMP = TrainingMethod.BLAST_FURNACE_PUMP;
	private static final int STOP = 8;
	private static final long WARN = 60_000;
	/** The client's idle timeout at the game's default, 5 minutes. */
	private static final long LOGOUT = 5 * 60_000;

	private TrainingSession session;
	private int strengthXp;

	@Before
	public void setUp()
	{
		session = new TrainingSession();
		strengthXp = 1_000_000;
		session.xp(Skill.STRENGTH, strengthXp);
	}

	private void pumpXp()
	{
		strengthXp += 2;
		session.xp(Skill.STRENGTH, strengthXp);
	}

	private List<ActivityAlerts.Type> tick(int tick, boolean animating, long idle)
	{
		final List<ActivityAlerts.Type> types = new ArrayList<>();
		for (ActivityAlerts.Alert alert : session.tick(tick, animating, idle, LOGOUT, STOP, WARN))
		{
			assertSame(PUMP, alert.method);
			assertEquals(Skill.STRENGTH, alert.skill);
			types.add(alert.type);
		}
		return types;
	}

	@Test
	public void thePumpIsFoundByItsObjectAndOption()
	{
		assertSame(PUMP, TrainingMethod.forObject(ObjectID.BLAST_FURNACE_PUMP, "Operate"));
		assertSame(PUMP, TrainingMethod.forObject(9090, "operate"));
		assertNull("Examine is not pumping", TrainingMethod.forObject(ObjectID.BLAST_FURNACE_PUMP, "Examine"));
		assertNull(TrainingMethod.forObject(ObjectID.BLAST_FURNACE_PEDALS, "Operate"));
		assertNull(TrainingMethod.forObject(ObjectID.BLAST_FURNACE_PUMP, null));
		assertEquals(Skill.STRENGTH, PUMP.skill());
		assertEquals("Pumping (Blast Furnace)", PUMP.label());
		assertEquals("100 minutes of game ticks", 10_000, PUMP.autoStopTicks());
	}

	@Test
	public void pumpingWithExperienceEveryTickNeverAlerts()
	{
		session.start(PUMP, 0, 1);
		for (int t = 1; t < 200; t++)
		{
			pumpXp();
			assertTrue("tick " + t, tick(t, true, 0).isEmpty());
		}
		assertTrue(session.running());
	}

	@Test
	public void theAnimationStoppingAndNoExperienceAlertsOnceAfterTheDelay()
	{
		session.start(PUMP, 0, 1);
		pumpXp();
		tick(1, true, 0);
		for (int t = 2; t < 1 + STOP; t++)
		{
			assertTrue("tick " + t, tick(t, false, 0).isEmpty());
		}
		assertEquals(single(ActivityAlerts.Type.METHOD_STOPPED), tick(1 + STOP, false, 0));
		assertFalse(session.running());
		assertTrue("once only", tick(2 + STOP, false, 0).isEmpty());
		assertEquals("countdowns gone once stopped", -1, session.reclickMillis());
		assertEquals(-1, session.stopTicksLeft());
		assertSame("the idle badge still knows it was the pump", PUMP, session.method());
	}

	@Test
	public void experienceAloneKeepsItAliveWhileTheAnimationFlickers()
	{
		session.start(PUMP, 0, 1);
		for (int t = 1; t < 50; t++)
		{
			pumpXp();
			assertTrue(tick(t, t % 3 == 0, 0).isEmpty());
		}
	}

	@Test
	public void aFightEndsItWithoutTheStoppedAlert()
	{
		session.start(PUMP, 0, 1);
		pumpXp();
		tick(1, true, 0);
		// The plugin ends the session when an NPC is interacted with, then the fight's Strength XP arrives.
		session.end();
		for (int t = 2; t < 40; t++)
		{
			pumpXp();
			assertTrue("tick " + t, tick(t, false, 0).isEmpty());
		}
		assertFalse(session.running());
	}

	@Test
	public void combatExperienceBeforeAnyStartIsIgnored()
	{
		for (int t = 1; t < 40; t++)
		{
			pumpXp();
			assertTrue(tick(t, true, 0).isEmpty());
		}
		assertNull(session.method());
	}

	@Test
	public void walkingOffEndsItQuietly()
	{
		session.start(PUMP, 0, 1);
		tick(1, true, 0);
		session.end();
		for (int t = 2; t < 30; t++)
		{
			assertTrue(tick(t, false, 0).isEmpty());
		}
	}

	@Test
	public void theIdleLogoutCountsDownAndWarnsOnceUntilTheClientIsTouched()
	{
		session.start(PUMP, 0, 1);
		pumpXp();
		assertTrue(tick(1, true, 0).isEmpty());
		assertEquals(LOGOUT, session.reclickMillis());

		pumpXp();
		assertTrue("not yet", tick(2, true, LOGOUT - WARN - 1).isEmpty());
		assertEquals(WARN + 1, session.reclickMillis());

		pumpXp();
		assertEquals(single(ActivityAlerts.Type.RECLICK_SOON), tick(3, true, LOGOUT - WARN));
		pumpXp();
		assertTrue("once", tick(4, true, LOGOUT - 1000).isEmpty());

		// The mouse moved: idle back to zero, and the warning re-arms.
		pumpXp();
		assertTrue(tick(5, true, 0).isEmpty());
		pumpXp();
		assertEquals(single(ActivityAlerts.Type.RECLICK_SOON), tick(6, true, LOGOUT - 5000));
		pumpXp();
		tick(7, true, LOGOUT + 5000);
		assertEquals("never below zero", 0, session.reclickMillis());
	}

	@Test
	public void anUnknownIdleTimeoutShowsNoReclickCountdown()
	{
		session.start(PUMP, 0, 1);
		pumpXp();
		assertTrue(session.tick(1, true, 0, 0, STOP, WARN).isEmpty());
		assertEquals(-1, session.reclickMillis());
		pumpXp();
		assertTrue(session.tick(2, true, -1, LOGOUT, STOP, WARN).isEmpty());
		assertEquals(-1, session.reclickMillis());
	}

	@Test
	public void theGamesOwnStopIsCountedFromTheClickAndWarnedOnce()
	{
		session.start(PUMP, 100, 102);
		pumpXp();
		tick(102, true, 0);
		assertEquals(10_000 - 2, session.stopTicksLeft());

		final int warnAt = 100 + 10_000 - 100; // 60 s = 100 ticks
		pumpXp();
		assertTrue(tick(warnAt - 1, true, 0).isEmpty());
		pumpXp();
		assertEquals(single(ActivityAlerts.Type.AUTO_STOP_SOON), tick(warnAt, true, 0));
		pumpXp();
		assertTrue("once per click", tick(warnAt + 1, true, 0).isEmpty());

		// The animation resuming in place (same click) does not re-arm it.
		session.start(PUMP, 100, warnAt + 2);
		pumpXp();
		assertTrue(tick(warnAt + 2, true, 0).isEmpty());

		// Clicking the pump again restarts the 100 minutes and the warning.
		session.start(PUMP, warnAt + 3, warnAt + 4);
		pumpXp();
		assertTrue(tick(warnAt + 4, true, 0).isEmpty());
		assertEquals(10_000 - 1, session.stopTicksLeft());
	}

	@Test
	public void startingAgainIsResumingForOneTick()
	{
		session.start(PUMP, 0, 1);
		tick(1, true, 0);
		assertTrue(session.resumed());
		tick(2, true, 0);
		assertFalse(session.resumed());
	}

	@Test
	public void loggingOutForgetsTheMethodAndBaselines()
	{
		session.start(PUMP, 0, 1);
		session.clear();
		assertNull(session.method());
		assertFalse(session.running());
		// The first value after a clear is a baseline, not a gain.
		session.start(PUMP, 0, 1);
		session.xp(Skill.STRENGTH, 5);
		tick(1, false, 0);
		for (int t = 2; t < 1 + STOP; t++)
		{
			assertTrue(tick(t, false, 0).isEmpty());
		}
		assertEquals(single(ActivityAlerts.Type.METHOD_STOPPED), tick(1 + STOP, false, 0));
	}

	@Test
	public void alertTextNamesTheMethod()
	{
		assertEquals("You have stopped pumping", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.METHOD_STOPPED, Skill.STRENGTH, PUMP), 28));
		assertEquals("You will stop pumping soon: click again to keep going", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.AUTO_STOP_SOON, Skill.STRENGTH, PUMP), 28));
		assertEquals("You have stopped training", ActivityAlerts.message(
			new ActivityAlerts.Alert(ActivityAlerts.Type.METHOD_STOPPED, null), 28));
	}

	@Test
	public void thePumpHasItsOwnVoiceLinesAndOthersTheGenericOnes()
	{
		final VoiceLines lines = VoiceLines.load();
		assertEquals(single("pump-stopped"), lines.ids(
			new ActivityAlerts.Alert(ActivityAlerts.Type.METHOD_STOPPED, Skill.STRENGTH, PUMP), null));
		assertEquals(single("pump-stopping"), lines.ids(
			new ActivityAlerts.Alert(ActivityAlerts.Type.AUTO_STOP_SOON, Skill.STRENGTH, PUMP), null));
		assertEquals(single("reclick-soon"), lines.ids(
			new ActivityAlerts.Alert(ActivityAlerts.Type.RECLICK_SOON, Skill.STRENGTH, PUMP), null));
		assertEquals(single("method-stopped"), lines.ids(
			new ActivityAlerts.Alert(ActivityAlerts.Type.METHOD_STOPPED, null), null));
	}

	@Test
	public void badgeTextForThePump()
	{
		assertEquals("Pumping (Blast Furnace) | Strength", SkillingText.headerName(Skill.STRENGTH, PUMP));
		assertEquals("Strength", SkillingText.headerName(Skill.STRENGTH, null));
		assertEquals("Not pumping", SkillingText.idleHeader(Skill.STRENGTH, PUMP));
		assertEquals("Not fishing", SkillingText.idleHeader(Skill.FISHING, null));
		assertEquals("Re-click in 4:12 | Stops in 97:30",
			SkillingText.methodTimers(252_000, 9750));
		assertEquals("Stops in 100:00", SkillingText.methodTimers(-1, 10_000));
		assertEquals("Re-click in 0:01", SkillingText.methodTimers(1, -1));
		assertNull(SkillingText.methodTimers(-1, -1));
		assertEquals("0:00", SkillingText.clock(0));
	}

	private static <T> List<T> single(T value)
	{
		final List<T> out = new ArrayList<>();
		out.add(value);
		return out;
	}
}
