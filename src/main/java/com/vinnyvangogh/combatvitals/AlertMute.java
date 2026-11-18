package com.vinnyvangogh.combatvitals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.LongSupplier;

/**
 * The sidebar's mute: every alert of the plugin, sound and on-screen, off
 * until the user turns it back on or a timed mute runs out. Held by the
 * plugin, not the panel, so a rebuilt panel shows the same state. It is not a
 * setting: a mute left on by accident across a restart would be a missed
 * alert, so it never outlives the client.
 *
 * <p>Any thread. The clock is injected so the timer can be tested.
 */
final class AlertMute
{
	/** The timed choices the panel offers, in minutes. */
	static final int[] MINUTES = {15, 30, 60};

	private static final long NOT_MUTED = -1;
	private static final long FOREVER = Long.MAX_VALUE;
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

	private final LongSupplier clock;
	private final ZoneId zone;
	/** Epoch millis the mute ends at; {@link #FOREVER} until unmuted; {@link #NOT_MUTED} when off. */
	private volatile long until = NOT_MUTED;

	AlertMute()
	{
		this(System::currentTimeMillis, ZoneId.systemDefault());
	}

	AlertMute(LongSupplier clock, ZoneId zone)
	{
		this.clock = clock;
		this.zone = zone;
	}

	/** Whether alerts are silenced right now. A timed mute that has run out is off again by itself. */
	boolean muted()
	{
		final long end = until;
		if (end == NOT_MUTED)
		{
			return false;
		}
		if (end != FOREVER && clock.getAsLong() >= end)
		{
			// Only clear the value that was read: a newer mute set meanwhile stands.
			synchronized (this)
			{
				if (until == end)
				{
					until = NOT_MUTED;
				}
			}
			return muted();
		}
		return true;
	}

	/** Muted with no end, until {@link #unmute}. */
	void muteUntilUnmuted()
	{
		until = FOREVER;
	}

	/** Muted for {@code minutes}, replacing any mute running; non-positive minutes unmutes. */
	void muteFor(int minutes)
	{
		until = minutes > 0 ? clock.getAsLong() + minutes * 60_000L : NOT_MUTED;
	}

	void unmute()
	{
		until = NOT_MUTED;
	}

	/** Whether the mute has no end. */
	boolean untilUnmuted()
	{
		return until == FOREVER;
	}

	/** The line the panel shows: "Muted until 14:30", "Muted until you turn it on", or null when not muted. */
	String status()
	{
		if (!muted())
		{
			return null;
		}
		final long end = until;
		if (end == FOREVER)
		{
			return "Muted until you turn alerts back on";
		}
		if (end == NOT_MUTED)
		{
			return null;
		}
		return "Muted until " + CLOCK.format(Instant.ofEpochMilli(end).atZone(zone));
	}
}
