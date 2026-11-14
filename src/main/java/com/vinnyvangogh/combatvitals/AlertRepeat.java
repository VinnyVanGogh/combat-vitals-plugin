package com.vinnyvangogh.combatvitals;

/**
 * "Repeat until I'm back": after an alert whose repeat is on, says it again
 * every interval until the player comes back. Coming back is a mouse press or
 * key press in the client, the activity resuming, a logout or hop, or the
 * maximum time running out. One alert repeats at a time; a newer one takes
 * its place.
 *
 * It only decides when; the plugin plays the sound. It never sends input:
 * the plugin's listeners only report that the player pressed something.
 *
 * Times are milliseconds from any clock. Synchronized, because the input
 * listeners call {@link #playerBack} from the AWT thread while game ticks
 * call the rest from the client thread.
 */
final class AlertRepeat
{
	private ActivityAlerts.Alert alert;
	private long startedAt;
	private long nextAt;
	private long intervalMs;
	private long maxMs;

	/**
	 * An alert just fired. Starts repeating it only when its repeat option is
	 * on; with it off, nothing changes (an earlier repeat carries on).
	 */
	synchronized void fired(ActivityAlerts.Alert fired, boolean repeatOn, long now, int everySeconds, int maxMinutes)
	{
		if (!repeatOn || fired == null)
		{
			return;
		}
		alert = fired;
		startedAt = now;
		intervalMs = Math.max(1, everySeconds) * 1000L;
		maxMs = Math.max(1, maxMinutes) * 60_000L;
		nextAt = now + intervalMs;
	}

	/**
	 * The alert to sound again now, or null. Stops for good once the maximum
	 * time has passed.
	 */
	synchronized ActivityAlerts.Alert due(long now)
	{
		if (alert == null)
		{
			return null;
		}
		if (now - startedAt >= maxMs)
		{
			stop();
			return null;
		}
		if (now < nextAt)
		{
			return null;
		}
		nextAt = now + intervalMs;
		return alert;
	}

	/** A mouse press or key press in the client: the player is back. */
	synchronized void playerBack()
	{
		stop();
	}

	/** The stopped activity started again. */
	synchronized void activityResumed()
	{
		stop();
	}

	synchronized void stop()
	{
		alert = null;
	}

	synchronized boolean active()
	{
		return alert != null;
	}
}
