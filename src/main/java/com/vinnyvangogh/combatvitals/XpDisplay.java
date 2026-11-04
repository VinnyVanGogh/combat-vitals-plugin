package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** How the badge's XP row states the progress to the next level. */
@AllArgsConstructor
public enum XpDisplay
{
	PERCENT("Percent"),
	XP_LEFT("XP left"),
	BOTH("Both");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
