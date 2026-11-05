package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Fish above your level in the catch list: dimmed with the level they need, or left out. */
@AllArgsConstructor
public enum LockedFish
{
	DIM("Dim"),
	HIDE("Hide");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
