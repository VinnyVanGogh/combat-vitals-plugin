package com.vinnyvangogh.combatvitals;

import java.time.Duration;
import java.time.Instant;

/**
 * Lap tracker for any runecrafting altar: laps, runes, runes per hour, xp per
 * hour and lap time. Information only; it reads the player's own crafts and
 * never acts for them.
 *
 * A lap is one craft at an altar. Its time runs from the previous craft (or
 * from {@link #start} for the first). A gap longer than {@link #IDLE_GAP}
 * (bank trip, AFK) is not counted as lap time: the lap clock restarts
 * and the rates only use time spent lapping.
 *
 * Pure: time is passed in, nothing is read from the client.
 */
final class RunecraftLaps
{
	/** A gap longer than this between crafts is a break, not a slow lap. */
	static final Duration IDLE_GAP = Duration.ofMinutes(5);

	private int laps;
	private int timed;
	private long runes;
	private long xp;
	private Duration active = Duration.ZERO;
	private Duration last = Duration.ZERO;
	private Duration best = Duration.ZERO;
	private Instant lapStart;

	/** Begin timing the next lap, e.g. when essence is first taken from the bank. */
	void start(Instant now)
	{
		lapStart = now;
	}

	/** A craft at an altar: {@code runesMade} runes and {@code xpGained} xp. */
	void crafted(Instant now, int runesMade, int xpGained)
	{
		if (runesMade <= 0)
		{
			return;
		}
		laps++;
		runes += runesMade;
		xp += Math.max(0, xpGained);
		if (lapStart != null)
		{
			final Duration lap = Duration.between(lapStart, now);
			if (!lap.isNegative() && lap.compareTo(IDLE_GAP) <= 0)
			{
				last = lap;
				timed++;
				active = active.plus(lap);
				if (best.isZero() || lap.compareTo(best) < 0)
				{
					best = lap;
				}
			}
		}
		lapStart = now;
	}

	void reset()
	{
		laps = 0;
		runes = 0;
		xp = 0;
		active = Duration.ZERO;
		last = Duration.ZERO;
		best = Duration.ZERO;
		timed = 0;
		lapStart = null;
	}

	int laps()
	{
		return laps;
	}

	long runes()
	{
		return runes;
	}

	long xp()
	{
		return xp;
	}

	/** The most recent timed lap, zero before one has been timed. */
	Duration lastLap()
	{
		return last;
	}

	Duration bestLap()
	{
		return best;
	}

	/** Mean timed lap, zero before one has been timed. */
	Duration averageLap()
	{
		return timed == 0 ? Duration.ZERO : active.dividedBy(timed);
	}

	long runesPerHour()
	{
		return perHour(runes);
	}

	long xpPerHour()
	{
		return perHour(xp);
	}

	private long perHour(long amount)
	{
		final long millis = active.toMillis();
		return millis <= 0 ? 0 : amount * 3_600_000L / millis;
	}
}
