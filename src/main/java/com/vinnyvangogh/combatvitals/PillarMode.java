package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Which vitals the side panel's pillars carry. */
@AllArgsConstructor
public enum PillarMode
{
	OFF("Off"),
	COMBAT("Combat"),
	SKILLING("Skilling"),
	BOTH("Both");

	@Getter
	private final String name;

	boolean combat()
	{
		return this == COMBAT || this == BOTH;
	}

	boolean skilling()
	{
		return this == SKILLING || this == BOTH;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
