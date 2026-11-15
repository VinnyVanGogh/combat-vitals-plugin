package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The Board's rule (2026-11-14): a stopped alert never fires on login; it
 * fires only after that activity actually started in this login session.
 *
 * The bug: logging in at the Blast Furnace, not pumping, played "You've
 * stopped pumping." Only {@link TrainingSession} sends that line
 * (METHOD_STOPPED with the pump). Logging out and hopping clear it, but the
 * plugin had no case for {@code CONNECTION_LOST}: a reconnect, which to the
 * player is logging back in, kept the pump session from before, and with no
 * pumping after it the stop delay ran out and the alert fired.
 * {@link #reconnectMidPumpReproducesTheBug} replays that.
 */
public class LoginAlertsTest
{
	private static final TrainingMethod PUMP = TrainingMethod.BLAST_FURNACE_PUMP;
	private static final int STOP = 8;
	private static final long WARN = 60_000;
	private static final long LOGOUT = 5 * 60_000;
	private static final int STRENGTH_XP = 1_000_000;

	/** A fresh login: every skill reported once, the way the login sync reports them. */
	private static TrainingSession loggedIn()
	{
		final TrainingSession session = new TrainingSession();
		session.clear();
		for (Skill skill : Skill.values())
		{
			session.xp(skill, skill == Skill.STRENGTH ? STRENGTH_XP : 50_000);
		}
		return session;
	}

	private static List<ActivityAlerts.Type> tick(TrainingSession session, int tick, boolean animating)
	{
		final List<ActivityAlerts.Type> types = new ArrayList<>();
		for (ActivityAlerts.Alert alert : session.tick(tick, animating, 0, LOGOUT, STOP, WARN))
		{
			types.add(alert.type);
		}
		return types;
	}

	/** Every stopped alert over a run of ticks. */
	private static int stoppedAlerts(TrainingSession session, int from, int to, boolean animating)
	{
		int count = 0;
		for (int t = from; t < to; t++)
		{
			for (ActivityAlerts.Type type : tick(session, t, animating))
			{
				if (type == ActivityAlerts.Type.METHOD_STOPPED)
				{
					count++;
				}
			}
		}
		return count;
	}

	@Test
	public void loginAtThePumpWithNoClickIsSilent()
	{
		final TrainingSession session = loggedIn();
		// Standing still, then an emote: animation alone is not a pump session.
		assertEquals(0, stoppedAlerts(session, 1, 100, false));
		assertEquals(0, stoppedAlerts(session, 100, 120, true));
		assertEquals(0, stoppedAlerts(session, 120, 200, false));
		assertFalse("no session, so no badge countdowns", session.running());
		assertNull("and no \"Not pumping\" badge", session.method());
		assertEquals(-1, session.reclickMillis());
	}

	@Test
	public void loginThenPumpThenStopAlertsOnce()
	{
		final TrainingSession session = loggedIn();
		assertEquals(0, stoppedAlerts(session, 1, 20, false));
		session.start(PUMP, 20, 21);
		int xp = STRENGTH_XP;
		for (int t = 21; t < 60; t++)
		{
			xp += 2;
			session.xp(Skill.STRENGTH, xp);
			assertTrue(tick(session, t, true).isEmpty());
		}
		assertEquals("one alert for one stop", 1, stoppedAlerts(session, 60, 200, false));
	}

	@Test
	public void hopMidSessionLeavesNoStrayAlert()
	{
		final TrainingSession session = loggedIn();
		session.start(PUMP, 0, 1);
		session.xp(Skill.STRENGTH, STRENGTH_XP + 2);
		tick(session, 1, true);
		// HOPPING: the plugin clears, then the new world's sync reports every skill again.
		session.clear();
		session.xp(Skill.STRENGTH, STRENGTH_XP + 2);
		assertEquals(0, stoppedAlerts(session, 2, 200, false));
		assertNull(session.method());
	}

	/**
	 * The bug, replayed: pumping, the connection drops, the client reconnects
	 * (no LOGIN_SCREEN, no HOPPING, so nothing was cleared), and the player is
	 * not pumping after it. Before the fix the plugin did nothing on the
	 * reconnect and "You've stopped pumping" fired; now the reconnect is a new
	 * login session and the old pumping does not count.
	 */
	@Test
	public void reconnectMidPumpReproducesTheBug()
	{
		final TrainingSession before = loggedIn();
		before.start(PUMP, 0, 1);
		before.xp(Skill.STRENGTH, STRENGTH_XP + 2);
		tick(before, 1, true);
		assertEquals("what main did: no reconnect handling, so the stale session alerts", 1,
			stoppedAlerts(before, 2, 100, false));

		final TrainingSession after = loggedIn();
		after.start(PUMP, 0, 1);
		after.xp(Skill.STRENGTH, STRENGTH_XP + 2);
		tick(after, 1, true);
		after.reconnected();
		assertEquals("the fix: nothing started this login, so no alert", 0, stoppedAlerts(after, 2, 100, false));
		assertFalse("the stale session ended quietly", after.running());
		assertEquals(-1, after.reclickMillis());
	}

	@Test
	public void afterAReconnectPumpingAgainThenStoppingAlertsOnce()
	{
		final TrainingSession session = loggedIn();
		session.start(PUMP, 0, 1);
		tick(session, 1, true);
		session.reconnected();
		// The game's tick count can start again from zero after a reconnect.
		int xp = STRENGTH_XP;
		for (int t = 0; t < 20; t++)
		{
			xp += 2;
			session.xp(Skill.STRENGTH, xp);
			assertTrue("still pumping after the reconnect, tick " + t, tick(session, t, true).isEmpty());
		}
		assertEquals(1, stoppedAlerts(session, 20, 100, false));
	}

	@Test
	public void theLoginSyncIsNotADropForAnySkill()
	{
		final XpSkilling xp = new XpSkilling();
		final ActivityAlerts alerts = new ActivityAlerts();
		for (Skill skill : Skill.values())
		{
			xp.xp(skill, 123_456);
			alerts.xp(skill, 123_456);
		}
		assertEquals(XpSkilling.Step.NONE, xp.tick(1, false, false));
		assertNull("no XP badge on login", xp.skill());

		final ActivityAlerts.Tick now = new ActivityAlerts.Tick();
		for (int t = 1; t < 100; t++)
		{
			now.tick = t;
			assertTrue("tick " + t, alerts.tick(now, STOP, 28, 20).isEmpty());
		}
		assertNull("nothing armed", alerts.active());
		assertFalse(alerts.experienced());
	}

	@Test
	public void aReconnectDisarmsTheGatheringStopDetector()
	{
		final ActivityAlerts alerts = new ActivityAlerts();
		alerts.xp(Skill.WOODCUTTING, 1_000);
		final ActivityAlerts.Tick now = new ActivityAlerts.Tick();
		now.tick = 1;
		alerts.tick(now, STOP, 28, 20);
		alerts.xp(Skill.WOODCUTTING, 1_025);
		now.tick = 2;
		alerts.tick(now, STOP, 28, 20);
		assertEquals(Skill.WOODCUTTING, alerts.active());

		alerts.reconnected();
		for (int t = 3; t < 100; t++)
		{
			now.tick = t;
			assertTrue("tick " + t, alerts.tick(now, STOP, 28, 20).isEmpty());
		}

		// Chopping again after the reconnect arms it, and stopping alerts once.
		alerts.xp(Skill.WOODCUTTING, 1_050);
		now.tick = 100;
		alerts.tick(now, STOP, 28, 20);
		int stopped = 0;
		for (int t = 101; t < 200; t++)
		{
			now.tick = t;
			stopped += alerts.tick(now, STOP, 28, 20).size();
		}
		assertEquals(1, stopped);
	}

	@Test
	public void theSyncWindowCoversTwoTicksAfterEveryLogin()
	{
		final LoginSync sync = new LoginSync();
		assertFalse("started mid-session: experience counts at once", sync.syncing());
		sync.tick();
		assertFalse(sync.syncing());

		sync.loggingIn();
		assertTrue("between LOGGING_IN and the first tick", sync.syncing());
		sync.tick();
		assertTrue(sync.syncing());
		sync.tick();
		assertFalse("XP Tracker counts from its second tick too", sync.syncing());

		sync.loggingIn();
		sync.tick();
		sync.loggingIn();
		assertTrue("a hop during the window starts it again", sync.syncing());
		sync.tick();
		sync.tick();
		assertFalse(sync.syncing());
	}
}
