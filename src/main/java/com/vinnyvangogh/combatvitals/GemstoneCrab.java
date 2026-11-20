package com.vinnyvangogh.combatvitals;

/**
 * Gemstone Crab mode, the maths and the state, with no client in it.
 *
 * <p>From the OSRS wiki (Gemstone Crab): the crab's health bar is not
 * affected by player damage; it depletes over about ten minutes and the crab
 * burrows when it reaches 0%. It leaves a shell that stays for 90 seconds,
 * and each mine's cave (object 57631, "Crawl-through") always leads to its
 * current location. So "time until it burrows" is the bar's fraction of the
 * ten minutes, refined by the rate the bar is actually seen to fall.
 *
 * <p>The tracker fires each alert once per cycle: a cycle starts when a crab
 * is seen and ends when its shell goes, or the crab is gone for good.
 */
final class GemstoneCrab
{
	static final int CRAB_ID = 14779;
	static final int SHELL_ID = 14780;
	static final int CAVE_OBJECT_ID = 57631;

	/** The fight length the wiki gives: about ten minutes from full bar to burrow. */
	static final long FIGHT_MS = 10 * 60_000L;
	/** How long the shell stays before it sinks into the ground. */
	static final long SHELL_MS = 90_000L;

	/** A crab that vanishes with no shell counts as burrowed only if its timer was this close to zero. */
	static final long BURROW_GRACE_MS = 10_000L;

	/** Samples younger than this, or with less drop than {@link #MIN_DROP}, are too noisy for a rate. */
	private static final long MIN_SPAN_MS = 30_000L;
	private static final double MIN_DROP = 0.05;

	/** Farther than this from where the crab or its shell was last seen, the player has left the crab area. */
	static final int AREA_TILES = 32;

	/** True when the player is on another plane or more than {@link #AREA_TILES} tiles from the last sighting. */
	static boolean left(int fromX, int fromY, int fromPlane, int x, int y, int plane)
	{
		return plane != fromPlane || Math.max(Math.abs(x - fromX), Math.abs(y - fromY)) > AREA_TILES;
	}

	/** Where the remaining time came from. */
	enum Source
	{
		BAR, OBSERVED, CLOCK
	}

	enum Phase
	{
		NONE, CRAB, SHELL
	}

	/** One tick's read-out. */
	static final class Reading
	{
		final Phase phase;
		/** Health as 0..1, or -1 when the bar was not readable. */
		final double fraction;
		final long remainingMs;
		final Source source;
		/** Remaining shell time while {@link Phase#SHELL}, else -1. */
		final long shellMs;

		Reading(Phase phase, double fraction, long remainingMs, Source source, long shellMs)
		{
			this.phase = phase;
			this.fraction = fraction;
			this.remainingMs = remainingMs;
			this.source = source;
			this.shellMs = shellMs;
		}

		int percent()
		{
			return fraction < 0 ? -1 : (int) Math.round(fraction * 100);
		}
	}

	/** Alerts due this tick; each is true at most once per cycle. */
	static final class Alerts
	{
		boolean spawned;
		boolean aboutToBurrow;
		boolean goNow;
	}

	private final long warnMs;
	private Phase phase = Phase.NONE;
	private long cycleStart;
	private long shellStart;
	private long firstSampleAt = -1;
	private double firstFraction;
	private boolean sentSpawned;
	private boolean sentWarn;
	private boolean sentGo;
	private long lastLeft = Long.MAX_VALUE;
	private boolean lost;
	private long lostAt;

	GemstoneCrab(int warnSeconds)
	{
		this.warnMs = Math.max(0, warnSeconds) * 1000L;
	}

	void reset()
	{
		phase = Phase.NONE;
		firstSampleAt = -1;
		sentSpawned = false;
		sentWarn = false;
		sentGo = false;
		lost = false;
		lastLeft = Long.MAX_VALUE;
	}

	/** True while a crab or its shell is being tracked. */
	boolean active()
	{
		return phase != Phase.NONE;
	}

	boolean burrowed()
	{
		return phase == Phase.SHELL;
	}

	/**
	 * Remaining ms for a bar fraction: the wiki's ten minutes scaled by the
	 * fraction, or, once the bar has fallen at least 5% over 30 seconds, the
	 * time at the rate it is falling.
	 */
	static long remaining(double fraction, long spanMs, double dropped)
	{
		final double f = Math.max(0, Math.min(1, fraction));
		if (spanMs >= MIN_SPAN_MS && dropped >= MIN_DROP)
		{
			return Math.round(f / dropped * spanMs);
		}
		return Math.round(f * FIGHT_MS);
	}

	/**
	 * One tick.
	 *
	 * @param crabSeen   the crab NPC is in the scene
	 * @param shellSeen  the shell NPC is in the scene
	 * @param fraction   the crab's health bar as 0..1, or -1 when not readable
	 * @param now        epoch ms
	 * @param out        receives the alerts due
	 */
	Reading step(boolean crabSeen, boolean shellSeen, double fraction, long now, Alerts out)
	{
		if (crabSeen)
		{
			return crab(fraction, now, out);
		}
		if (shellSeen)
		{
			return shell(now, out);
		}
		if (phase == Phase.CRAB)
		{
			// The crab vanished with no shell. Burrowed only if the timer had run
			// out (or it was seen about to); otherwise it just left the scene.
			if (lastLeft <= BURROW_GRACE_MS)
			{
				return shell(now, out);
			}
			phase = Phase.NONE;
			firstSampleAt = -1;
			lost = true;
			lostAt = now;
			return new Reading(Phase.NONE, -1, -1, Source.CLOCK, -1);
		}
		if (phase == Phase.SHELL && now - shellStart < SHELL_MS)
		{
			return shell(now, out);
		}
		if (lost && now - lostAt < FIGHT_MS)
		{
			return new Reading(Phase.NONE, -1, -1, Source.CLOCK, -1);
		}
		reset();
		return new Reading(Phase.NONE, -1, -1, Source.CLOCK, -1);
	}

	private Reading crab(double fraction, long now, Alerts out)
	{
		if (phase != Phase.CRAB && lost)
		{
			// The same crab back in view: same cycle, no second spawn alert.
			phase = Phase.CRAB;
			lost = false;
		}
		else if (phase != Phase.CRAB)
		{
			phase = Phase.CRAB;
			cycleStart = now;
			// A crab after a shell is a new cycle: it spawns, and it burrows, again.
			sentSpawned = false;
			firstSampleAt = -1;
			sentWarn = false;
			sentGo = false;
		}
		if (!sentSpawned)
		{
			sentSpawned = true;
			out.spawned = true;
		}
		long left;
		Source source;
		if (fraction >= 0)
		{
			if (firstSampleAt < 0)
			{
				firstSampleAt = now;
				firstFraction = fraction;
			}
			final long span = now - firstSampleAt;
			final double dropped = firstFraction - fraction;
			left = remaining(fraction, span, dropped);
			source = span >= MIN_SPAN_MS && dropped >= MIN_DROP ? Source.OBSERVED : Source.BAR;
		}
		else
		{
			left = Math.max(0, FIGHT_MS - (now - cycleStart));
			source = Source.CLOCK;
		}
		if (warnMs > 0 && left <= warnMs && !sentWarn)
		{
			sentWarn = true;
			out.aboutToBurrow = true;
		}
		lastLeft = left;
		return new Reading(Phase.CRAB, fraction, left, source, -1);
	}

	private Reading shell(long now, Alerts out)
	{
		if (phase != Phase.SHELL)
		{
			phase = Phase.SHELL;
			shellStart = now;
		}
		if (!sentGo)
		{
			sentGo = true;
			out.goNow = true;
		}
		final long left = Math.max(0, SHELL_MS - (now - shellStart));
		return new Reading(Phase.SHELL, 0, 0, Source.CLOCK, left);
	}

	static String clock(long ms)
	{
		final long s = Math.max(0, (ms + 999) / 1000);
		return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
	}
}
