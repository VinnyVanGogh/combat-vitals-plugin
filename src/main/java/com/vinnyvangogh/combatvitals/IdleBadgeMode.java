package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** What the skilling badge does once you stop: go, stay a while, or stay until something else starts. */
@AllArgsConstructor
public enum IdleBadgeMode
{
	OFF("Off"),
	TIMED("Show for a while"),
	UNTIL_NEXT("Until another activity");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
