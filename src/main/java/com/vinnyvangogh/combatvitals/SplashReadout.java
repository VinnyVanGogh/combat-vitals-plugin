package com.vinnyvangogh.combatvitals;

/**
 * Everything the splashing card draws for one tick: the Magic read-out (null
 * with Splashing mode off), the casts left for the spell being cast (null
 * with that off or the spell unknown), and the two clocks as of this tick.
 * The clocks count on between ticks by the wall clock ({@link #tickMillis}).
 */
final class SplashReadout
{
	/** Hit chance and Magic bonus, or null with Splashing mode off. */
	final SplashTracker.Result splash;
	/** The spell being cast, or null when it is not one the casts table knows. */
	final String spell;
	/** "142 casts left", "Unlimited casts", "Out of Chaos runes", or null when not shown. */
	final String castsText;
	/** Casts left, {@link SpellCasts#INFINITE}, or -2 when not known. */
	final int casts;
	/** The casts are at or under the user's warn level. */
	final boolean castsLow;
	/** The rune item ids the spell needs, for the card's icons, with how many of each per cast. */
	final int[] runeItems;
	final int[] runeCosts;
	final long idleMillis;
	final long logoutMillis;
	final long sessionMillis;
	final long tickMillis;
	/** When the casting stopped (wall clock), or -1 while it goes on: the card is then a dimmed afterimage. */
	final long stoppedAt;

	SplashReadout(SplashTracker.Result splash, String spell, String castsText, int casts, boolean castsLow,
		int[] runeItems, int[] runeCosts, long idleMillis, long logoutMillis, long sessionMillis, long tickMillis)
	{
		this(splash, spell, castsText, casts, castsLow, runeItems, runeCosts, idleMillis, logoutMillis,
			sessionMillis, tickMillis, -1);
	}

	private SplashReadout(SplashTracker.Result splash, String spell, String castsText, int casts, boolean castsLow,
		int[] runeItems, int[] runeCosts, long idleMillis, long logoutMillis, long sessionMillis, long tickMillis,
		long stoppedAt)
	{
		this.stoppedAt = stoppedAt;
		this.splash = splash;
		this.spell = spell;
		this.castsText = castsText;
		this.casts = casts;
		this.castsLow = castsLow;
		this.runeItems = runeItems;
		this.runeCosts = runeCosts;
		this.idleMillis = idleMillis;
		this.logoutMillis = logoutMillis;
		this.sessionMillis = sessionMillis;
		this.tickMillis = tickMillis;
	}

	/** This card as an afterimage of casting that stopped at {@code at}. */
	SplashReadout stopped(long at)
	{
		return new SplashReadout(splash, spell, castsText, casts, castsLow, runeItems, runeCosts, idleMillis,
			logoutMillis, sessionMillis, tickMillis, at);
	}

	boolean isStopped()
	{
		return stoppedAt >= 0;
	}

	/** The idle logout line as of {@code now}, or null. */
	String logoutText(long now)
	{
		final long since = Math.max(0, now - tickMillis);
		// Input is not seen between ticks, so the idle time can only be said to have grown.
		return SplashTimer.logoutText(idleMillis < 0 ? -1 : idleMillis + since, logoutMillis);
	}

	/** Time spent splashing as of {@code now}, or -1. */
	long session(long now)
	{
		return sessionMillis < 0 ? -1 : sessionMillis + Math.max(0, now - tickMillis);
	}

	/** Share of the idle logout left, 0 to 1, for the bar, or -1 when unknown. */
	double logoutFraction(long now)
	{
		final long left = SplashTimer.logoutLeft(idleMillis < 0 ? -1 : idleMillis + Math.max(0, now - tickMillis),
			logoutMillis);
		return left < 0 ? -1 : Math.min(1.0, (double) left / logoutMillis);
	}

	/** The casts line for a result. */
	static String castsText(SpellCasts.Result r)
	{
		if (r.casts == SpellCasts.INFINITE)
		{
			return "Unlimited casts";
		}
		if (r.casts == 0)
		{
			return "Out of " + (r.limiting == null ? "runes" : r.limiting.label + " runes");
		}
		return r.casts + (r.casts == 1 ? " cast left" : " casts left");
	}
}
