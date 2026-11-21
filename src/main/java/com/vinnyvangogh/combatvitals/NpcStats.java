package com.vinnyvangogh.combatvitals;

import com.google.common.base.Charsets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * One NPC's wiki stats for the target card: combat level, hitpoints, max
 * hit, attack style and speed, defence bonuses, weakness and poison
 * immunity. From the OSRS Wiki's Monster infobox (CC BY-NC-SA 3.0), bundled
 * as {@code npc_stats.tsv} and built offline by
 * {@code tools/npcstats/fetch_npc_stats.py}. Nothing is fetched at runtime.
 * A value the wiki leaves blank is null (text) or -1 (number).
 */
@Slf4j
final class NpcStats
{
	private static final String RESOURCE = "npc_stats.tsv";
	private static final int COLUMNS = 15;

	final int combatLevel;
	final int hitpoints;
	final String maxHit;
	final String style;
	/** Game ticks between attacks. */
	final int speed;
	final int stab;
	final int slash;
	final int crush;
	final int magic;
	final int ranged;
	final String weakness;
	final boolean poisonImmune;
	final boolean venomImmune;
	final String name;

	private NpcStats(String[] p)
	{
		combatLevel = number(p[1]);
		hitpoints = number(p[2]);
		maxHit = text(p[3]);
		style = text(p[4]);
		speed = number(p[5]);
		stab = number(p[6]);
		slash = number(p[7]);
		crush = number(p[8]);
		magic = number(p[9]);
		ranged = number(p[10]);
		weakness = text(p[11]);
		poisonImmune = "1".equals(p[12]);
		venomImmune = "1".equals(p[13]);
		name = p[14];
	}

	private static int number(String s)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	private static String text(String s)
	{
		return s.isEmpty() || "-".equals(s) ? null : s;
	}

	/** An NPC id to its stats; empty (not null) if the table cannot be read. */
	static Map<Integer, NpcStats> load()
	{
		try (InputStream in = NpcStats.class.getResourceAsStream(RESOURCE))
		{
			return in == null ? new HashMap<>() : parse(in);
		}
		catch (IOException e)
		{
			log.warn("Could not read {}", RESOURCE, e);
			return new HashMap<>();
		}
	}

	static Map<Integer, NpcStats> parse(InputStream in) throws IOException
	{
		final Map<Integer, NpcStats> table = new HashMap<>();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null)
		{
			if (line.isEmpty() || line.charAt(0) == '#')
			{
				continue;
			}
			final String[] parts = line.split("\t", COLUMNS);
			if (parts.length < COLUMNS)
			{
				continue;
			}
			try
			{
				table.put(Integer.parseInt(parts[0]), new NpcStats(parts));
			}
			catch (NumberFormatException e)
			{
				// A bad row is skipped, never fatal.
			}
		}
		return table;
	}

	/** The wiki's attack speed in ticks as seconds, "2.4s"; null when unknown. */
	String speedText()
	{
		return speed <= 0 ? null : String.format("%.1fs", speed * 0.6);
	}
}
