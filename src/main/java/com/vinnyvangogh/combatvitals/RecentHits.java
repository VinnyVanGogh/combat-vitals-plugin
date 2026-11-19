package com.vinnyvangogh.combatvitals;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * When each actor last took a hitsplat, so the overhead readout can appear
 * only for a few seconds after damage, as the game's own health bar does.
 * Weak keys: a despawned actor drops out on its own.
 */
final class RecentHits<T>
{
	/** How long the game keeps a health bar up after a hit. */
	static final long WINDOW_MILLIS = 6000;

	private final Map<T, Long> last = new WeakHashMap<>();

	synchronized void hit(T actor, long nowMillis)
	{
		if (actor != null)
		{
			last.put(actor, nowMillis);
		}
	}

	synchronized boolean recent(T actor, long nowMillis)
	{
		final Long at = actor == null ? null : last.get(actor);
		return at != null && nowMillis - at < WINDOW_MILLIS;
	}

	synchronized void clear()
	{
		last.clear();
	}
}
