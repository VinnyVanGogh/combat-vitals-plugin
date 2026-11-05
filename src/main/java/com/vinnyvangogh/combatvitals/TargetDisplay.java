package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The display for one kind of NPC target. Same as Display follows the main
 * Display setting, which is why it is the default: a profile that never
 * touches these settings looks exactly as it did before they existed.
 */
@AllArgsConstructor
public enum TargetDisplay
{
	SAME("Same as Display", null),
	HITPOINTS("Hitpoints", DisplayMode.HITPOINTS),
	PERCENTAGE("Percentage", DisplayMode.PERCENTAGE),
	BOTH("Both", DisplayMode.BOTH);

	@Getter
	private final String name;

	private final DisplayMode mode;

	DisplayMode resolve(DisplayMode display)
	{
		return mode == null ? display : mode;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
