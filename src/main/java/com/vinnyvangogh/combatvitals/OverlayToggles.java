package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The sidebar's show/hide switches for each overlay, and for all of them.
 * Each is a plain setting (same key in RuneLite's list), so the sidebar and
 * the list always agree; this class only knows which keys are overlays and
 * how the side pillars' three-way setting maps to on and off.
 *
 * Pure: reads and writes through a {@link ConfigStore}.
 */
final class OverlayToggles
{
	static final class Toggle
	{
		final String key;
		final String label;
		/** What the setting is when it was never set. */
		final boolean defaultOn;
		/** Left out of "all on" and "all off": an extra drawn on top of an overlay, not an overlay. */
		final boolean extra;

		Toggle(String key, String label, boolean defaultOn, boolean extra)
		{
			this.key = key;
			this.label = label;
			this.defaultOn = defaultOn;
			this.extra = extra;
		}
	}

	static final List<Toggle> TOGGLES = Collections.unmodifiableList(Arrays.asList(
		new Toggle("showPanel", "Corner panel", true, false),
		new Toggle("showOverhead", "Readout over target", true, false),
		new Toggle("showOwnHealth", "Readout over you", true, false),
		new Toggle("skillingEnabled", "Badge (skilling)", true, false),
		new Toggle("pillarMode", "Side pillars", false, false),
		new Toggle("aggressionTimer", "Aggression countdown", true, false),
		new Toggle("aggressionAreaLines", "Aggression 21x21 lines", false, true),
		new Toggle("splashMode", "Splashing mode (AFK Magic)", false, true)));

	private OverlayToggles()
	{
	}

	static Toggle find(String key)
	{
		for (Toggle t : TOGGLES)
		{
			if (t.key.equals(key))
			{
				return t;
			}
		}
		throw new IllegalArgumentException(key);
	}

	static boolean isOn(ConfigStore store, Toggle toggle)
	{
		if ("pillarMode".equals(toggle.key))
		{
			final PillarMode mode = store.get(toggle.key, PillarMode.class);
			return mode == null ? toggle.defaultOn : mode != PillarMode.OFF;
		}
		final Boolean on = store.get(toggle.key, Boolean.class);
		return on == null ? toggle.defaultOn : on;
	}

	static void set(ConfigStore store, Toggle toggle, boolean on)
	{
		if ("pillarMode".equals(toggle.key))
		{
			store.set(toggle.key, on ? PillarMode.BOTH : PillarMode.OFF);
		}
		else
		{
			store.set(toggle.key, on);
		}
	}

	/** Every overlay on or off; the extras (area lines) are left as they are. */
	static void setAll(ConfigStore store, boolean on)
	{
		for (Toggle t : TOGGLES)
		{
			if (!t.extra)
			{
				set(store, t, on);
			}
		}
	}

	static boolean allOn(ConfigStore store)
	{
		for (Toggle t : TOGGLES)
		{
			if (!t.extra && !isOn(store, t))
			{
				return false;
			}
		}
		return true;
	}
}
