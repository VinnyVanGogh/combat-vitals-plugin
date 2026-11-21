package com.vinnyvangogh.combatvitals;

import java.util.Locale;

/**
 * Splashing mode's two clocks, like the pump's: how long until the game logs
 * you out for no input, and how long this splashing session has run.
 *
 * <p>The logout clock is the game's own: it counts from your last mouse or
 * keyboard input and any input resets it, so it reads "Active" while you are
 * using the client and counts down once you have been idle
 * {@link TrainingSession#ACTIVE_MS}. The session clock starts at the first
 * tick you cast at an NPC and resets after {@link SplashTracker#FORGET_TICKS}
 * ticks without a cast. No client types. Client thread only.
 */
final class SplashTimer
{
	private long startMillis = -1;
	private int idleTicks;

	/** A tick spent casting at an NPC. */
	void cast(long nowMillis)
	{
		idleTicks = 0;
		if (startMillis < 0)
		{
			startMillis = nowMillis;
		}
	}

	/** A tick with no cast: after {@link SplashTracker#FORGET_TICKS} the session is over. */
	void idle()
	{
		if (++idleTicks >= SplashTracker.FORGET_TICKS)
		{
			startMillis = -1;
		}
	}

	void reset()
	{
		startMillis = -1;
		idleTicks = 0;
		soonArmed = true;
		stoppedArmed = true;
	}

	/** Milliseconds this session has run, or -1 when none is. */
	long elapsed(long nowMillis)
	{
		return startMillis < 0 ? -1 : Math.max(0, nowMillis - startMillis);
	}

	/** Milliseconds until the idle logout, or -1 when the game's timeout is unknown. */
	static long logoutLeft(long idleMillis, long logoutMillis)
	{
		if (logoutMillis <= 0 || idleMillis < 0)
		{
			return -1;
		}
		return Math.max(0, logoutMillis - idleMillis);
	}

	/**
	 * How long without input before splashing stops by itself: "The player will
	 * stop attacking after 20 minutes of inactivity"
	 * (https://oldschool.runescape.wiki/w/Splashing).
	 */
	static final long STOP_MS = 20 * 60_000L;

	static final int NONE = 0;
	static final int STOPS_SOON = 1;
	static final int STOPPED = 2;

	private boolean soonArmed = true;
	private boolean stoppedArmed = true;

	/**
	 * The alert due for this much idle time: {@link #STOPS_SOON} once when
	 * {@code warnMillis} or less is left, {@link #STOPPED} once when none is,
	 * each again after any input brings the idle time back under
	 * {@link TrainingSession#ACTIVE_MS}.
	 */
	int afk(long idleMillis, long warnMillis)
	{
		if (idleMillis < TrainingSession.ACTIVE_MS)
		{
			soonArmed = true;
			stoppedArmed = true;
			return NONE;
		}
		final long left = STOP_MS - idleMillis;
		if (left <= 0)
		{
			soonArmed = false;
			if (stoppedArmed)
			{
				stoppedArmed = false;
				return STOPPED;
			}
			return NONE;
		}
		if (left <= warnMillis && soonArmed)
		{
			soonArmed = false;
			return STOPS_SOON;
		}
		return NONE;
	}

	/** Whether the card is still shown {@code sinceStopMillis} after the last cast. */
	static boolean lingers(long sinceStopMillis, int seconds, boolean forever)
	{
		return forever || sinceStopMillis < seconds * 1000L;
	}

	/** "Stopped 12s ago", or "Stopped 2m 05s ago" from a minute. */
	static String stoppedAgo(long sinceStopMillis)
	{
		return "Stopped " + ago(sinceStopMillis);
	}

	/** "16s ago" or "2m 05s ago". */
	static String ago(long sinceMillis)
	{
		final long s = Math.max(0, sinceMillis) / 1000;
		if (s >= 60)
		{
			return String.format(Locale.ROOT, "%dm %02ds ago", s / 60, s % 60);
		}
		return s + "s ago";
	}

	/** "Active" while you are using the client, else "Splashing stops in 14:32"; null when unknown. */
	static String logoutText(long idleMillis, long logoutMillis)
	{
		final long left = logoutLeft(idleMillis, logoutMillis);
		if (left < 0)
		{
			return null;
		}
		return idleMillis < TrainingSession.ACTIVE_MS ? "Active" : "Splashing stops in " + clock(left);
	}

	/** "3:07", or "1:02:09" from an hour. */
	static String clock(long millis)
	{
		final long s = Math.max(0, millis) / 1000;
		if (s >= 3600)
		{
			return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
		}
		return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
	}
}
