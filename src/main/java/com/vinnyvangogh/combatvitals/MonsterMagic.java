package com.vinnyvangogh.combatvitals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.google.common.base.Charsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Each NPC's Magic level and Magic defence bonus, from the OSRS Wiki's
 * Monster infobox, bundled as {@code monster_magic.tsv} and built offline by
 * {@code tools/splash/fetch_monsters.py}. Nothing is fetched at runtime.
 */
@Slf4j
final class MonsterMagic
{
	private static final String RESOURCE = "monster_magic.tsv";

	final int level;
	final int defence;
	final String name;

	MonsterMagic(int level, int defence, String name)
	{
		this.level = level;
		this.defence = defence;
		this.name = name;
	}

	/** An NPC id to its Magic stats; empty (not null) if the table cannot be read. */
	static Map<Integer, MonsterMagic> load()
	{
		try (InputStream in = MonsterMagic.class.getResourceAsStream(RESOURCE))
		{
			return in == null ? new HashMap<>() : parse(in);
		}
		catch (IOException e)
		{
			log.warn("Could not read {}", RESOURCE, e);
			return new HashMap<>();
		}
	}

	static Map<Integer, MonsterMagic> parse(InputStream in) throws IOException
	{
		final Map<Integer, MonsterMagic> table = new HashMap<>();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null)
		{
			if (line.isEmpty() || line.charAt(0) == '#')
			{
				continue;
			}
			final String[] parts = line.split("\t", 4);
			if (parts.length < 4)
			{
				continue;
			}
			try
			{
				table.put(Integer.parseInt(parts[0]),
					new MonsterMagic(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]));
			}
			catch (NumberFormatException e)
			{
				// A bad row is skipped, never fatal.
			}
		}
		return table;
	}
}
