package com.vinnyvangogh.combatvitals;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;

/**
 * Named setups for the side pillars: which source each pillar part shows and how each is split.
 * A preset is just config keys and the text stored for them, so applying one is a handful of
 * {@link ConfigStore} writes, and saved presets live in one hidden RuneLite config string (no
 * files). Built-ins are code; yours are saved, renamed and deleted by name.
 */
final class PillarPresets
{
	static final String MODE = "pillarMode";
	static final String LEFT_SPLIT = "pillarLeftSplit";
	static final String LEFT_FIRST = "pillarLeftSource";
	static final String LEFT_SECOND = "pillarLeftSource2";
	static final String RIGHT_SPLIT = "pillarRightSplit";
	static final String RIGHT_FIRST = "pillarRightSource";
	static final String RIGHT_SECOND = "pillarRightSource2";

	/** Every key a preset may hold. */
	static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(MODE, LEFT_SPLIT, LEFT_FIRST,
		LEFT_SECOND, RIGHT_SPLIT, RIGHT_FIRST, RIGHT_SECOND));

	/** A name longer than this does not fit the settings list. */
	static final int MAX_NAME = 24;

	/** The stored JSON's shape: Gson fills it, no type tokens or reflection of ours. */
	private static final class Saved
	{
		private LinkedHashMap<String, LinkedHashMap<String, String>> presets;
	}

	private PillarPresets()
	{
	}

	/** The built-in presets. */
	enum Builtin
	{
		MELEE("Melee", PillarSplit.TOP_BOTTOM, PillarSource.HITPOINTS, PillarSource.PRAYER,
			PillarSplit.TOP_BOTTOM, PillarSource.SPEC, PillarSource.TARGET_HP, null),
		RANGED("Ranged", PillarSplit.TOP_BOTTOM, PillarSource.HITPOINTS, PillarSource.PRAYER,
			PillarSplit.TOP_BOTTOM, PillarSource.AMMO, PillarSource.TARGET_HP, null),
		MAGIC("Magic", PillarSplit.TOP_BOTTOM, PillarSource.HITPOINTS, PillarSource.PRAYER,
			PillarSplit.TOP_BOTTOM, PillarSource.CASTS, PillarSource.TARGET_HP, null),
		BOSSING("Bossing", PillarSplit.TOP_BOTTOM, PillarSource.HITPOINTS, PillarSource.PRAYER,
			PillarSplit.SIDE_BY_SIDE, PillarSource.FOOD, PillarSource.PRAYER_DOSES, null),
		SKILLING("Skilling", PillarSplit.OFF, PillarSource.DEFAULT, PillarSource.NONE,
			PillarSplit.OFF, PillarSource.DEFAULT, PillarSource.NONE, PillarMode.SKILLING),
		MINIMAL("Minimal", PillarSplit.OFF, PillarSource.HITPOINTS, PillarSource.NONE,
			PillarSplit.OFF, PillarSource.NONE, PillarSource.NONE, null);

		final String label;
		private final Map<String, String> values = new LinkedHashMap<>();

		Builtin(String label, PillarSplit leftSplit, PillarSource left1, PillarSource left2, PillarSplit rightSplit,
			PillarSource right1, PillarSource right2, PillarMode mode)
		{
			this.label = label;
			if (mode != null)
			{
				values.put(MODE, mode.name());
			}
			values.put(LEFT_SPLIT, leftSplit.name());
			values.put(LEFT_FIRST, left1.name());
			values.put(LEFT_SECOND, left2.name());
			values.put(RIGHT_SPLIT, rightSplit.name());
			values.put(RIGHT_FIRST, right1.name());
			values.put(RIGHT_SECOND, right2.name());
		}

		/** The keys this preset writes, and the text for each. */
		Map<String, String> settings()
		{
			return Collections.unmodifiableMap(values);
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** A built-in by its label, ignoring case; null for none. */
	static Builtin builtin(String name)
	{
		if (name == null)
		{
			return null;
		}
		for (Builtin b : Builtin.values())
		{
			if (b.label.equalsIgnoreCase(name.trim()))
			{
				return b;
			}
		}
		return null;
	}

	/** The pillar keys as stored now, with the default for any left unset. */
	static Map<String, String> current(ConfigStore store)
	{
		final Map<String, String> out = new LinkedHashMap<>();
		for (String key : KEYS)
		{
			final String raw = store.raw(key);
			if (raw != null)
			{
				out.put(key, raw);
			}
		}
		return sanitize(out);
	}

	/** Writes a preset's keys; a key it does not hold is left as the player set it. */
	static void apply(Map<String, String> preset, ConfigStore store)
	{
		for (Map.Entry<String, String> e : sanitize(preset).entrySet())
		{
			store.setRaw(e.getKey(), e.getValue());
		}
	}

	/** Only known keys with a value the setting accepts: a hand-edited or stale preset cannot write junk. */
	static Map<String, String> sanitize(Map<String, String> preset)
	{
		final Map<String, String> out = new LinkedHashMap<>();
		if (preset == null)
		{
			return out;
		}
		for (String key : KEYS)
		{
			final String value = preset.get(key);
			if (value != null && valid(key, value))
			{
				out.put(key, value);
			}
		}
		return out;
	}

	private static boolean valid(String key, String value)
	{
		try
		{
			switch (key)
			{
				case MODE:
					PillarMode.valueOf(value);
					return true;
				case LEFT_SPLIT:
				case RIGHT_SPLIT:
					PillarSplit.valueOf(value);
					return true;
				default:
					PillarSource.valueOf(value);
					return true;
			}
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}

	/** The side's setup out of preset or stored values, the default pillar for what is missing. */
	static PillarSetup setup(Map<String, String> values, PillarSide side)
	{
		final boolean left = side == PillarSide.LEFT;
		return new PillarSetup(
			parse(PillarSplit.class, values.get(left ? LEFT_SPLIT : RIGHT_SPLIT)),
			parse(PillarSource.class, values.get(left ? LEFT_FIRST : RIGHT_FIRST)),
			parse(PillarSource.class, values.get(left ? LEFT_SECOND : RIGHT_SECOND)));
	}

	private static <E extends Enum<E>> E parse(Class<E> type, String value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, value);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	// ---- the player's own presets ----

	/** The saved presets in {@code json}, in saved order; empty for none or text that does not parse. */
	static Map<String, Map<String, String>> parse(Gson gson, String json)
	{
		final Map<String, Map<String, String>> out = new LinkedHashMap<>();
		if (json == null || json.trim().isEmpty())
		{
			return out;
		}
		try
		{
			final Saved read = gson.fromJson(json, Saved.class);
			if (read != null && read.presets != null)
			{
				for (Map.Entry<String, LinkedHashMap<String, String>> e : read.presets.entrySet())
				{
					final String name = validName(e.getKey());
					if (name != null && e.getValue() != null)
					{
						out.put(name, sanitize(e.getValue()));
					}
				}
			}
		}
		catch (JsonSyntaxException e)
		{
			out.clear();
		}
		return out;
	}

	static String toJson(Gson gson, Map<String, Map<String, String>> saved)
	{
		final Saved out = new Saved();
		out.presets = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, String>> e : saved.entrySet())
		{
			out.presets.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
		}
		return gson.toJson(out);
	}

	/** The name trimmed, or null when blank, too long or shadowing a built-in. */
	static String validName(String name)
	{
		if (name == null)
		{
			return null;
		}
		final String trimmed = name.trim();
		if (trimmed.isEmpty() || trimmed.length() > MAX_NAME || builtin(trimmed) != null)
		{
			return null;
		}
		return trimmed;
	}

	/** The key in {@code saved} equal to {@code name} ignoring case, or null. */
	static String key(Map<String, ?> saved, String name)
	{
		if (name == null)
		{
			return null;
		}
		for (String k : saved.keySet())
		{
			if (k.equalsIgnoreCase(name.trim()))
			{
				return k;
			}
		}
		return null;
	}

	/** A preset by name: a built-in, else one of yours; null for neither. */
	static Map<String, String> find(String name, Map<String, Map<String, String>> saved)
	{
		final Builtin builtin = builtin(name);
		if (builtin != null)
		{
			return builtin.settings();
		}
		final String key = key(saved, name);
		return key == null ? null : saved.get(key);
	}

	/** Saves {@code values} as {@code name}, replacing one of the same name; false for a name that is not valid. */
	static boolean save(Map<String, Map<String, String>> saved, String name, Map<String, String> values)
	{
		final String valid = validName(name);
		if (valid == null)
		{
			return false;
		}
		final String existing = key(saved, valid);
		if (existing != null)
		{
			saved.remove(existing);
		}
		saved.put(valid, sanitize(values));
		return true;
	}

	/** Renames {@code from} to {@code to}; false when {@code from} is missing, {@code to} is invalid or taken. */
	static boolean rename(Map<String, Map<String, String>> saved, String from, String to)
	{
		final String old = key(saved, from);
		final String valid = validName(to);
		if (old == null || valid == null)
		{
			return false;
		}
		final String clash = key(saved, valid);
		if (clash != null && !clash.equals(old))
		{
			return false;
		}
		final Map<String, String> values = saved.remove(old);
		saved.put(valid, values);
		return true;
	}

	static boolean delete(Map<String, Map<String, String>> saved, String name)
	{
		final String key = key(saved, name);
		return key != null && saved.remove(key) != null;
	}

	/** "Melee, Ranged, ...; yours: Raids, Zulrah" for messages. */
	static String names(Map<String, ?> saved)
	{
		final StringBuilder out = new StringBuilder();
		for (Builtin b : Builtin.values())
		{
			out.append(out.length() == 0 ? "" : ", ").append(b.label);
		}
		if (!saved.isEmpty())
		{
			out.append("; yours: ").append(String.join(", ", saved.keySet()));
		}
		return out.toString();
	}

	/**
	 * The preset the weapon and activity call for, or null for no change: skilling while a skilling
	 * session is running, else Ranged, Magic or Melee by the weapon's name. Off unless the player
	 * turns auto-switch on.
	 */
	static String autoChoice(String weaponName, boolean skilling)
	{
		if (skilling)
		{
			return Builtin.SKILLING.label;
		}
		final String name = weaponName == null ? "" : weaponName.toLowerCase(Locale.ROOT);
		for (String word : RANGED_WORDS)
		{
			if (name.contains(word))
			{
				return Builtin.RANGED.label;
			}
		}
		for (String word : MAGIC_WORDS)
		{
			if (name.contains(word))
			{
				return Builtin.MAGIC.label;
			}
		}
		return Builtin.MELEE.label;
	}

	private static final String[] RANGED_WORDS = {"bow", "blowpipe", "dart", "chinchompa", "ballista", "javelin",
		"throwing", "knife", "atlatl", "thrown"};
	private static final String[] MAGIC_WORDS = {"staff", "wand", "trident", "sanguinesti", "tumeken", "sceptre",
		"kodai", "eldritch", "volatile", "harmonised"};
}
