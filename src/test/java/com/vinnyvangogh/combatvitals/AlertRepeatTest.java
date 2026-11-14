package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The failure cases first: a repeat that never stops, or starts when it is
 * off, is the whole risk. Then it repeats on time.
 */
public class AlertRepeatTest
{
	private static final int EVERY = 15;
	private static final int MAX_MIN = 10;
	private static final long T0 = 1_000_000L;

	private final ActivityAlerts.Alert stopped =
		new ActivityAlerts.Alert(ActivityAlerts.Type.GATHERING_STOPPED, Skill.WOODCUTTING);

	@Test
	public void neverStartsWhenTheOptionIsOff()
	{
		final AlertRepeat repeat = new AlertRepeat();
		repeat.fired(stopped, false, T0, EVERY, MAX_MIN);
		assertFalse(repeat.active());
		for (long t = T0; t < T0 + 60 * 60_000L; t += 600)
		{
			assertNull(repeat.due(t));
		}
	}

	@Test
	public void anOffAlertLeavesARunningRepeatAlone()
	{
		final AlertRepeat repeat = started();
		repeat.fired(new ActivityAlerts.Alert(ActivityAlerts.Type.LOW_RUN_ENERGY, null), false, T0 + 1000, EVERY, MAX_MIN);
		assertSame(stopped, repeat.due(T0 + EVERY * 1000L));
	}

	@Test
	public void stopsOnAClickOrKeyPress()
	{
		final AlertRepeat repeat = started();
		repeat.playerBack();
		assertFalse(repeat.active());
		assertNull(repeat.due(T0 + EVERY * 1000L));
		assertNull(repeat.due(T0 + 5 * EVERY * 1000L));
	}

	@Test
	public void stopsWhenTheActivityResumes()
	{
		final AlertRepeat repeat = started();
		assertSame(stopped, repeat.due(T0 + EVERY * 1000L));
		repeat.activityResumed();
		assertNull(repeat.due(T0 + 2 * EVERY * 1000L));
	}

	@Test
	public void stopsAfterTheMaximumTime()
	{
		final AlertRepeat repeat = started();
		int repeats = 0;
		for (long t = T0; t < T0 + 2 * MAX_MIN * 60_000L; t += 600)
		{
			if (repeat.due(t) != null)
			{
				repeats++;
			}
		}
		assertFalse(repeat.active());
		// Ten minutes at 15 s: 39 repeats after the first alert, none after.
		assertTrue("repeats " + repeats, repeats >= 39 && repeats <= 40);
	}

	@Test
	public void repeatsOnlyOnTheInterval()
	{
		final AlertRepeat repeat = started();
		assertNull("not straight away: the first alert just played", repeat.due(T0));
		assertNull(repeat.due(T0 + EVERY * 1000L - 1));
		assertSame(stopped, repeat.due(T0 + EVERY * 1000L));
		assertNull("once per interval", repeat.due(T0 + EVERY * 1000L + 600));
		assertSame(stopped, repeat.due(T0 + 2 * EVERY * 1000L));
	}

	@Test
	public void aNewerAlertTakesOver()
	{
		final AlertRepeat repeat = started();
		final ActivityAlerts.Alert full = new ActivityAlerts.Alert(ActivityAlerts.Type.INVENTORY_FULL, Skill.FISHING);
		repeat.fired(full, true, T0 + 5000, EVERY, MAX_MIN);
		assertSame(full, repeat.due(T0 + 5000 + EVERY * 1000L));
	}

	private AlertRepeat started()
	{
		final AlertRepeat repeat = new AlertRepeat();
		repeat.fired(stopped, true, T0, EVERY, MAX_MIN);
		assertTrue(repeat.active());
		return repeat;
	}
}
