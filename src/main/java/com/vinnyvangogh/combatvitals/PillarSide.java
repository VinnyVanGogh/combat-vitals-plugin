package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One of the two pillars beside the side panel. */
@AllArgsConstructor
public enum PillarSide
{
	LEFT("Left"),
	RIGHT("Right");

	@Getter
	private final String name;

	PillarSide other()
	{
		return this == LEFT ? RIGHT : LEFT;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
