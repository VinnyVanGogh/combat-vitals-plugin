package com.vinnyvangogh.combatvitals;

/**
 * The experience sync that comes with logging in, hopping or reconnecting is
 * not anybody's experience drop. RuneLite's own XP Tracker treats every
 * {@code StatChanged} from {@code LOGGING_IN} or {@code HOPPING} until its
 * second game tick as "the XP sync on login" and ignores it, since "xp is not
 * available until after login is finished" (XpTrackerPlugin,
 * runelite/runelite master, read 2026-11-14). This keeps the same window: the
 * plugin feeds no experience to its trackers while {@link #syncing}, and seeds
 * every tracker's baseline from the client once the window closes.
 *
 * Pure: no client. Client thread only.
 */
final class LoginSync
{
	/** Game ticks after the login, hop or reconnect before experience counts, as XP Tracker waits. */
	static final int SYNC_TICKS = 2;

	private int ticksLeft;

	/** Logging in, hopping, or the connection lost: the next experience values are a sync. */
	void loggingIn()
	{
		ticksLeft = SYNC_TICKS;
	}

	/** Once per game tick, before anything reads experience. */
	void tick()
	{
		if (ticksLeft > 0)
		{
			ticksLeft--;
		}
	}

	/** Experience reported now is the sync, not a gain. */
	boolean syncing()
	{
		return ticksLeft > 0;
	}
}
