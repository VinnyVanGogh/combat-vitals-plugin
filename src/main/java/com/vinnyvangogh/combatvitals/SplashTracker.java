package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Splashing mode's state: what to show for the NPC the player is casting at,
 * and when the setup changed under them. No client types, so the whole
 * decision is unit tested. Client thread only.
 */
final class SplashTracker
{
	/** Ticks without a cast before the last setup is forgotten, so a new session starts clean. */
	static final int FORGET_TICKS = 10;

	enum Status
	{
		/** The Magic bonus alone guarantees a splash. */
		SAFE,
		/** A chance to hit, at or below the warn threshold. */
		LOW,
		/** A chance to hit above the threshold. */
		WARN,
		/** The bonus is not enough and there is no wiki data to say what the chance is. */
		UNKNOWN
	}

	/** What the client saw this tick while the player was casting at an NPC. */
	static final class Input
	{
		final int spellKey;
		final int gearKey;
		final int npcIndex;
		final int npcId;
		final String npcName;
		final int magicBonus;
		final int magicLevel;
		final double prayerBonus;

		Input(int spellKey, int gearKey, int npcIndex, int npcId, String npcName, int magicBonus, int magicLevel,
			double prayerBonus)
		{
			this.spellKey = spellKey;
			this.gearKey = gearKey;
			this.npcIndex = npcIndex;
			this.npcId = npcId;
			this.npcName = npcName;
			this.magicBonus = magicBonus;
			this.magicLevel = magicLevel;
			this.prayerBonus = prayerBonus;
		}
	}

	/** What the overlay draws, and the alert text if the setup changed this tick (else null). */
	static final class Result
	{
		final Status status;
		final String bonusLine;
		final String chanceLine;
		final String changed;

		Result(Status status, String bonusLine, String chanceLine, String changed)
		{
			this.status = status;
			this.bonusLine = bonusLine;
			this.chanceLine = chanceLine;
			this.changed = changed;
		}
	}

	private final Map<Integer, MonsterMagic> monsters;
	private Input last;
	private int idleTicks;

	SplashTracker(Map<Integer, MonsterMagic> monsters)
	{
		this.monsters = monsters;
	}

	/** A tick with no cast at an NPC: the read-out goes, and after {@link #FORGET_TICKS} so does the baseline. */
	void idle()
	{
		if (++idleTicks >= FORGET_TICKS)
		{
			last = null;
		}
	}

	void reset()
	{
		last = null;
		idleTicks = 0;
	}

	Result step(Input in, int maxChancePercent)
	{
		idleTicks = 0;
		final String changed = last == null ? null : changes(last, in);
		last = in;

		if (SplashMath.splashes(in.magicBonus))
		{
			return new Result(Status.SAFE, bonus(in.magicBonus), "Splashing: 0% to hit anything", changed);
		}
		final MonsterMagic monster = monsters.get(in.npcId);
		if (monster == null)
		{
			return new Result(Status.UNKNOWN, bonus(in.magicBonus) + " (need " + SplashMath.SPLASH_BONUS
				+ " or lower)", null, changed);
		}
		final double chance = SplashMath.hitChance(in.magicLevel, in.prayerBonus, in.magicBonus, monster.level,
			monster.defence);
		final Status status = chance * 100 > maxChancePercent ? Status.WARN : Status.LOW;
		return new Result(status, bonus(in.magicBonus) + " (need " + SplashMath.SPLASH_BONUS + " or lower)",
			"Hit chance on " + in.npcName + ": " + percent(chance), changed);
	}

	private static String changes(Input before, Input now)
	{
		final List<String> what = new ArrayList<>();
		if (before.gearKey != now.gearKey)
		{
			what.add("gear changed");
		}
		if (before.spellKey != now.spellKey)
		{
			what.add("spell changed");
		}
		if (before.npcIndex != now.npcIndex)
		{
			what.add("target changed");
		}
		return what.isEmpty() ? null : "Splash setup changed: " + String.join(", ", what) + ".";
	}

	private static String bonus(int magicBonus)
	{
		return "Magic bonus: " + (magicBonus > 0 ? "+" : "") + magicBonus;
	}

	static String percent(double chance)
	{
		final double pct = chance * 100;
		if (pct > 0 && pct < 0.1)
		{
			return "<0.1%";
		}
		return String.format(Locale.ROOT, "%.1f%%", pct);
	}
}
