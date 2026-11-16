package com.vinnyvangogh.combatvitals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

/**
 * Picks the voice clips for an alert from the bundled word list
 * ({@code voice/lines.tsv}): "You've stopped" + the activity + what you were
 * gathering, so a few dozen short clips cover every resource. Anything the
 * list does not know falls back, first to "You've stopped" + the activity,
 * then to "You've stopped gathering.".
 *
 * Pure: the word list and whether a clip exists are handed in, no client.
 */
@Slf4j
final class VoiceLines
{
	/** The word list, a class resource beside this class. */
	static final String LIST = "voice/lines.tsv";
	/** Every clip's resource path starts with this. */
	static final String FOLDER = "voice/";

	static final String STOPPED = "stopped";
	static final String STOPPED_GATHERING = "stopped-gathering";
	static final String FINISHED = "finished";

	private final Map<String, String> phrases = new HashMap<>();
	private final Map<String, String> alerts = new HashMap<>();
	private final Map<String, String> verbs = new HashMap<>();
	private final Map<String, String> resources = new HashMap<>();
	/** Whole lines more particular than the alert's own, by {@link SkillingEvents#voiceKey}. */
	private final Map<String, String> details = new HashMap<>();
	/** Every clip id to the words it says, for the generator's check. */
	private final Map<String, String> spoken = new HashMap<>();

	/** The bundled list, or an empty one (every alert then falls back) if it cannot be read. */
	static VoiceLines load()
	{
		// getResourceAsStream, not getResource: the plugin ships inside a jar.
		try (InputStream in = VoiceLines.class.getResourceAsStream(LIST))
		{
			if (in == null)
			{
				log.warn("Voice word list {} is missing", LIST);
				return new VoiceLines();
			}
			return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
		}
		catch (IOException e)
		{
			log.warn("Could not read voice word list", e);
			return new VoiceLines();
		}
	}

	static VoiceLines parse(BufferedReader reader) throws IOException
	{
		final VoiceLines lines = new VoiceLines();
		String line;
		while ((line = reader.readLine()) != null)
		{
			if (line.trim().isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			final String[] cells = line.split("\t");
			if (cells.length == 4)
			{
				lines.add(cells[0].trim(), cells[1].trim(), cells[2].trim(), cells[3].trim());
			}
		}
		return lines;
	}

	private void add(String kind, String key, String clip, String words)
	{
		final Map<String, String> table = table(kind);
		if (table == null)
		{
			return;
		}
		table.put(key.toLowerCase(Locale.ROOT), clip);
		spoken.put(clip, words);
	}

	private Map<String, String> table(String kind)
	{
		switch (kind)
		{
			case "phrase":
				return phrases;
			case "alert":
				return alerts;
			case "verb":
				return verbs;
			case "resource":
				return resources;
			case "detail":
				return details;
			default:
				return null;
		}
	}

	/** Every clip id the list names, each once. */
	Map<String, String> clips()
	{
		return Collections.unmodifiableMap(spoken);
	}

	/** The class resource path of a clip id. */
	static String path(String clip)
	{
		return FOLDER + clip + ".wav";
	}

	/**
	 * The clips to play, in order, as resource paths; empty when nothing
	 * fits, and the caller then plays RuneLite's sound instead.
	 *
	 * @param item   the name of the item that last landed in the inventory, or null
	 * @param exists whether a clip's resource path is bundled
	 */
	List<String> compose(ActivityAlerts.Alert alert, String item, Predicate<String> exists)
	{
		final List<String> ids = ids(alert, item);
		if (allExist(ids, exists))
		{
			return paths(ids);
		}
		final String fallback = isStop(alert.type) ? phrase(STOPPED_GATHERING) : alerts.get(key(alert.type.name()));
		if (fallback != null && allExist(Collections.singletonList(fallback), exists))
		{
			return paths(Collections.singletonList(fallback));
		}
		return Collections.emptyList();
	}

	/** The clip ids for an alert, before checking any is bundled. */
	List<String> ids(ActivityAlerts.Alert alert, String item)
	{
		final List<String> ids = new ArrayList<>();
		final String verb = alert.skill == null ? null : verbs.get(key(alert.skill.name()));
		if (isStop(alert.type))
		{
			if (verb == null)
			{
				ids.add(phrase(STOPPED_GATHERING));
				return ids;
			}
			ids.add(phrase(STOPPED));
			ids.add(verb);
			final String resource = resource(alert.skill, item);
			if (resource != null)
			{
				ids.add(resource);
			}
			return ids;
		}
		if (alert.type == ActivityAlerts.Type.PRODUCTION_FINISHED && verb != null)
		{
			ids.add(phrase(FINISHED));
			ids.add(verb);
			return ids;
		}
		final String whole = wholeLine(alert);
		if (whole != null)
		{
			ids.add(whole);
		}
		return ids;
	}

	/**
	 * The most particular line first: a skilling event's detail ("You got a
	 * hard clue!"), then a training method's own ("You've stopped pumping."),
	 * then the type's, or null.
	 */
	private String wholeLine(ActivityAlerts.Alert alert)
	{
		final String detailKey = SkillingEvents.voiceKey(alert);
		final String detail = detailKey == null ? null : details.get(key(detailKey));
		if (detail != null)
		{
			return detail;
		}
		final String own = alert.method == null ? null
			: alerts.get(key(alert.type.name() + "|" + alert.method.name()));
		return own != null ? own : alerts.get(key(alert.type.name()));
	}

	/** The resource clip for an item gathered with a skill, or null. */
	String resource(Skill skill, String item)
	{
		if (skill == null || item == null)
		{
			return null;
		}
		return resources.get(key(skill.name() + "|" + item));
	}

	private String phrase(String name)
	{
		final String clip = phrases.get(name);
		return clip == null ? name : clip;
	}

	private static boolean isStop(ActivityAlerts.Type type)
	{
		return type == ActivityAlerts.Type.GATHERING_STOPPED || type == ActivityAlerts.Type.THIEVING_STOPPED;
	}

	private static boolean allExist(List<String> ids, Predicate<String> exists)
	{
		if (ids.isEmpty())
		{
			return false;
		}
		for (String id : ids)
		{
			if (!exists.test(path(id)))
			{
				return false;
			}
		}
		return true;
	}

	private static List<String> paths(List<String> ids)
	{
		final List<String> out = new ArrayList<>(ids.size());
		for (String id : ids)
		{
			out.add(path(id));
		}
		return out;
	}

	private static String key(String raw)
	{
		return raw.toLowerCase(Locale.ROOT);
	}
}
