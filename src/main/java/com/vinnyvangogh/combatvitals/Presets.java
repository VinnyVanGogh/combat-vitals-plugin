package com.vinnyvangogh.combatvitals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One-click setups for the sidebar panel. A preset only writes existing
 * config keys, the ones it lists and no others, as the text
 * {@code ConfigManager} stores them; everything else stays as the player set
 * it. Applying returns an {@link Undo} holding what those keys were, unset
 * ones included, so Undo puts back exactly that.
 */
final class Presets
{
	private Presets()
	{
	}

	enum Preset
	{
		SKILLER("Skiller", "The skilling badge, skilling side pillars, and every alert as a voice line that repeats "
			+ "until you are back."),
		PVM("PvM", "Your target's and your own health, over the target and in the panel, the stat comparison, "
			+ "and the combat side pillar."),
		MINIMAL("Minimal", "Just the health bar over your target. No panel, no badge, no pillars."),
		EVERYTHING("Everything", "Both halves on: readout, panel, your own health, the skilling badge, and both "
			+ "side pillars.");

		final String label;
		final String summary;

		Preset(String label, String summary)
		{
			this.label = label;
			this.summary = summary;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private static final String ON = "true";
	private static final String OFF = "false";

	/** The keys a preset writes and the text it writes, in the order the preview lists them. */
	static Map<String, String> values(Preset preset)
	{
		final Map<String, String> v = new LinkedHashMap<>();
		switch (preset)
		{
			case SKILLER:
				v.put("skillingVitals", ON);
				v.put("skillingEnabled", ON);
				v.put("pillarMode", PillarMode.SKILLING.name());
				v.put(PanelLayout.SOUND_SHARED, ON);
				v.put("alertSoundAll", AlertSound.VOICE.name());
				v.put("alertRepeatAll", ON);
				break;
			case PVM:
				v.put("combatVitals", ON);
				v.put("showOverhead", ON);
				v.put("showPanel", ON);
				v.put("showOwnHealth", ON);
				v.put("showForNpcs", ON);
				v.put("showStatComparison", ON);
				v.put("pillarMode", PillarMode.COMBAT.name());
				break;
			case MINIMAL:
				v.put("combatVitals", ON);
				v.put("showOverhead", ON);
				v.put("showPanel", OFF);
				v.put("showOwnHealth", OFF);
				v.put("showStatComparison", OFF);
				v.put("skillingVitals", OFF);
				v.put("pillarMode", PillarMode.OFF.name());
				break;
			case EVERYTHING:
			default:
				v.put("combatVitals", ON);
				v.put("skillingVitals", ON);
				v.put("showOverhead", ON);
				v.put("showPanel", ON);
				v.put("showOwnHealth", ON);
				v.put("showStatComparison", ON);
				v.put("skillingEnabled", ON);
				v.put("pillarMode", PillarMode.BOTH.name());
				break;
		}
		return Collections.unmodifiableMap(v);
	}

	/**
	 * What applying would change: each key whose stored text differs, with
	 * the text now (null: unset) and the text it would get. RuneLite stores
	 * every default when the plugin loads ({@code setDefaultConfiguration}),
	 * so an unset key is rare.
	 */
	static Map<String, String[]> changes(Preset preset, ConfigStore store)
	{
		final Map<String, String[]> out = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : values(preset).entrySet())
		{
			final String now = store.raw(e.getKey());
			if (!Objects.equals(now, e.getValue()))
			{
				out.put(e.getKey(), new String[]{now, e.getValue()});
			}
		}
		return out;
	}

	/** Writes the preset's keys and returns how to put them back. */
	static Undo apply(Preset preset, ConfigStore store)
	{
		final Map<String, String> before = new LinkedHashMap<>();
		final Map<String, String> values = values(preset);
		for (String key : values.keySet())
		{
			before.put(key, store.raw(key));
		}
		for (Map.Entry<String, String> e : values.entrySet())
		{
			store.setRaw(e.getKey(), e.getValue());
		}
		return new Undo(preset, before);
	}

	/** What a preset's keys held before it was applied; null values were unset. */
	static final class Undo
	{
		final Preset preset;
		private final Map<String, String> before;

		private Undo(Preset preset, Map<String, String> before)
		{
			this.preset = preset;
			this.before = Collections.unmodifiableMap(before);
		}

		Map<String, String> before()
		{
			return before;
		}

		void restore(ConfigStore store)
		{
			for (Map.Entry<String, String> e : before.entrySet())
			{
				store.setRaw(e.getKey(), e.getValue());
			}
		}
	}
}
