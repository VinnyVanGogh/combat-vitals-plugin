package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/** The mute's timer, on a clock the test moves. */
public class AlertMuteTest
{
	private static final long T0 = 1_700_000_000_000L;
	private static final long MINUTE = 60_000L;

	private final AtomicLong now = new AtomicLong(T0);
	private final AlertMute mute = new AlertMute(now::get, ZoneOffset.UTC);

	@Test
	public void startsUnmuted()
	{
		assertFalse(mute.muted());
		assertNull(mute.status());
	}

	@Test
	public void aTimedMuteEndsByItself()
	{
		mute.muteFor(15);
		assertTrue(mute.muted());
		now.set(T0 + 15 * MINUTE - 1);
		assertTrue(mute.muted());
		now.set(T0 + 15 * MINUTE);
		assertFalse(mute.muted());
		assertNull(mute.status());
	}

	@Test
	public void anotherTimedMuteReplacesTheRunningOne()
	{
		mute.muteFor(15);
		now.set(T0 + 10 * MINUTE);
		mute.muteFor(60);
		now.set(T0 + 30 * MINUTE);
		assertTrue(mute.muted());
		now.set(T0 + 70 * MINUTE);
		assertFalse(mute.muted());
	}

	@Test
	public void aPlainMuteNeverEndsUntilUnmuted()
	{
		mute.muteUntilUnmuted();
		now.set(T0 + 1000 * MINUTE);
		assertTrue(mute.muted());
		assertTrue(mute.untilUnmuted());
		mute.unmute();
		assertFalse(mute.muted());
	}

	@Test
	public void unmutingCancelsATimedMute()
	{
		mute.muteFor(30);
		mute.unmute();
		assertFalse(mute.muted());
	}

	@Test
	public void nonPositiveMinutesUnmute()
	{
		mute.muteUntilUnmuted();
		mute.muteFor(0);
		assertFalse(mute.muted());
	}

	@Test
	public void statusNamesTheClockTimeTheMuteEnds()
	{
		// 1_700_000_000 s is 2023-11-14 22:13:20 UTC.
		mute.muteFor(30);
		assertEquals("Muted until 22:43", mute.status());
		mute.muteUntilUnmuted();
		assertEquals("Muted until you turn alerts back on", mute.status());
	}
}
