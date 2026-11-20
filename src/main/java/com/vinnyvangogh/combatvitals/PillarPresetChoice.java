package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The "Apply preset" dropdown: picking one sets the pillars to that built-in, then the setting
 * goes back to {@link #KEEP}. Your own saved presets are loaded by name instead.
 */
@AllArgsConstructor
public enum PillarPresetChoice
{
	KEEP("Keep current", null),
	MELEE("Melee", PillarPresets.Builtin.MELEE),
	RANGED("Ranged", PillarPresets.Builtin.RANGED),
	MAGIC("Magic", PillarPresets.Builtin.MAGIC),
	BOSSING("Bossing", PillarPresets.Builtin.BOSSING),
	SKILLING("Skilling", PillarPresets.Builtin.SKILLING),
	MINIMAL("Minimal", PillarPresets.Builtin.MINIMAL);

	@Getter
	private final String name;

	/** The built-in this applies; null for {@link #KEEP}. */
	final PillarPresets.Builtin builtin;

	@Override
	public String toString()
	{
		return name;
	}
}
