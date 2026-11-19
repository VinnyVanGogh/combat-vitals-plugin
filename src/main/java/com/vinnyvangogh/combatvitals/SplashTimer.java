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

	/** "Active" while you are using the client, else "Logout in 14:32"; null when unknown. */
	static String logoutText(long idleMillis, long logoutMillis)
	{
		final long left = logoutLeft(idleMillis, logoutMillis);
		if (left < 0)
		{
			return null;
		}
		return idleMillis < TrainingSession.ACTIVE_MS ? "Active" : "Logout in " + clock(left);
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
