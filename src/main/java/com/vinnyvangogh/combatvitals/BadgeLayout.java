package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The session row: every stat, wrapping when too wide, or one short line of the count and XP rate. */
@AllArgsConstructor
public enum BadgeLayout
{
	FULL("Full"),
	COMPACT("Compact");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
