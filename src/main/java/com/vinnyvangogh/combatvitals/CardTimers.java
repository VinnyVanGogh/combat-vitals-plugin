package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.runelite.api.Skill;

/**
 * The timer lines a Live card shows under its title: how long the session has been running (active time), the time
 * until it auto-pauses while it is active or how long it has been paused, and the time until the session resets
 * itself after being idle. Whether the card is "Paused" comes from the same numbers.
 *
 * <p>Pure: the session, its skills and the clock in, text out.
 */
final class CardTimers
{
	static final CardTimers NONE = new CardTimers(false, Collections.emptyList());

	/** The session is not being trained right now: the clock stopped, or the work did. */
	final boolean paused;
	/** "Active · running 14m", "Pauses in 0:42", "Paused for 2:10", "Session resets in 12:50"; may be empty. */
	final List<String> lines;

	CardTimers(boolean paused, List<String> lines)
	{
		this.paused = paused;
		this.lines = lines;
	}

	/**
	 * @param working the card's activity is being done right now (the badge is up and not idle)
	 * @param resetMinutes the idle minutes after which the session resets itself; 0 or less for never
	 */
	static CardTimers of(SkillingSession session, Skill[] skills, long nowMillis, boolean working, int resetMinutes)
	{
		long since = -1;
		long elapsed = -1;
		for (Skill skill : skills)
		{
			final long s = session.sinceLastGain(skill, nowMillis);
			if (s < 0)
			{
				continue;
			}
			since = since < 0 ? s : Math.min(since, s);
			elapsed = Math.max(elapsed, session.elapsed(skill, nowMillis));
		}
		if (since < 0)
		{
			return working ? NONE : new CardTimers(true, Collections.singletonList("Paused · no gains yet"));
		}

		final long idle = session.idleMillis();
		final boolean gap = idle > 0 && since > idle;
		final boolean paused = gap || !working;
		final List<String> lines = new ArrayList<>(3);
		if (paused)
		{
			lines.add("Paused · ran " + SessionRow.duration(elapsed));
			lines.add((gap ? "Paused for " : "Idle for ") + clock(gap ? since - idle : since));
		}
		else
		{
			lines.add("Active · running " + SessionRow.duration(elapsed));
			if (idle > 0)
			{
				lines.add("Pauses in " + clock(idle - since));
			}
		}
		if (resetMinutes > 0)
		{
			lines.add("Session resets in " + clock(resetMinutes * 60_000L - since));
		}
		return new CardTimers(paused, lines);
	}

	/** True once the session has been idle for {@code resetMinutes}, and should reset itself. */
	static boolean resetDue(SkillingSession session, Skill[] skills, long nowMillis, int resetMinutes)
	{
		if (resetMinutes <= 0)
		{
			return false;
		}
		long since = -1;
		for (Skill skill : skills)
		{
			final long s = session.sinceLastGain(skill, nowMillis);
			if (s >= 0)
			{
				since = since < 0 ? s : Math.min(since, s);
			}
		}
		return since >= resetMinutes * 60_000L;
	}

	/** "0:42", "12:50", "1:02:03": a countdown or a stretch, minutes and seconds. */
	static String clock(long millis)
	{
		final long s = Math.max(0, millis) / 1000;
		if (s >= 3600)
		{
			return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60);
		}
		return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
	}
}
