package com.vinnyvangogh.combatvitals;

/**
 * Glides a pillar's fill to its new value over a few frames instead of
 * jumping a whole tick's worth at once. A big fall (a level gained, the
 * inventory banked) and anything after a long pause are shown at once:
 * sliding down past them would show something that never happened.
 *
 * Pure: the target and the frame's time in, the fraction to draw out.
 */
final class PillarAnimation
{
	/** About how long a change takes to settle. */
	static final long DURATION_MILLIS = 300;

	/** Falls bigger than this snap. */
	private static final double SNAP_FALL = 0.5;

	/** A frame gap longer than this, the overlay was not drawn: snap. */
	private static final long SNAP_PAUSE_MILLIS = 1_000;

	private static final double SETTLED = 0.002;

	private double shown = -1;
	private long lastMillis;

	double next(double target, long nowMillis)
	{
		final long elapsed = nowMillis - lastMillis;
		lastMillis = nowMillis;
		if (shown < 0 || shown - target > SNAP_FALL || elapsed > SNAP_PAUSE_MILLIS || elapsed < 0)
		{
			shown = target;
			return shown;
		}

		// Exponential approach: a quarter of the duration is one time constant,
		// so a change is within 2% of its target by the end of it.
		shown += (target - shown) * (1 - Math.exp(-elapsed / (DURATION_MILLIS / 4.0)));
		if (Math.abs(target - shown) < SETTLED)
		{
			shown = target;
		}
		return shown;
	}

	void reset()
	{
		shown = -1;
	}
}
