package com.vinnyvangogh.combatvitals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.FishingSpot;

/**
 * The bundled list of fishing spot locations, for when no spot of the right
 * type is loaded in the scene. Read once from {@code fishing_spots.tsv}
 * beside this class; the file cites its sources and says what was left out.
 *
 * One row per line, tab separated: the {@link FishingSpot} constant, x, y,
 * plane, {@code members} or {@code free}, the Fishing level the location
 * itself needs (0 for none), and the location's name. A row that does not
 * parse is skipped and logged rather than failing the whole list.
 */
@Slf4j
final class KnownFishingSpots
{
	static final String RESOURCE = "fishing_spots.tsv";

	private static final int COLUMNS = 7;

	private KnownFishingSpots()
	{
	}

	/** The bundled list, or an empty one when it cannot be read. */
	static List<NextFishingSpot.Spot> load()
	{
		// getResourceAsStream, not getResource: the plugin ships inside a jar.
		try (InputStream in = KnownFishingSpots.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("{} is missing from the plugin jar", RESOURCE);
				return Collections.emptyList();
			}
			return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
		}
		catch (IOException e)
		{
			log.warn("Could not read {}", RESOURCE, e);
			return Collections.emptyList();
		}
	}

	static List<NextFishingSpot.Spot> parse(BufferedReader reader) throws IOException
	{
		final List<NextFishingSpot.Spot> spots = new ArrayList<>();
		String line;
		while ((line = reader.readLine()) != null)
		{
			final String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#"))
			{
				continue;
			}
			final NextFishingSpot.Spot spot = row(trimmed.split("\t"));
			if (spot == null)
			{
				log.warn("Skipping bad {} row: {}", RESOURCE, trimmed);
				continue;
			}
			spots.add(spot);
		}
		return Collections.unmodifiableList(spots);
	}

	private static NextFishingSpot.Spot row(String[] cells)
	{
		if (cells.length != COLUMNS)
		{
			return null;
		}
		try
		{
			final FishingSpot type = FishingSpot.valueOf(cells[0].trim());
			return NextFishingSpot.Spot.known(type,
				Integer.parseInt(cells[1].trim()),
				Integer.parseInt(cells[2].trim()),
				Integer.parseInt(cells[3].trim()),
				"members".equals(cells[4].trim()),
				Integer.parseInt(cells[5].trim()),
				cells[6].trim());
		}
		catch (IllegalArgumentException e)
		{
			// NumberFormatException is an IllegalArgumentException, as is an unknown enum name.
			return null;
		}
	}
}
