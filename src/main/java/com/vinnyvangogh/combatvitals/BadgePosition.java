package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Where the skilling badge is drawn. */
@AllArgsConstructor
public enum BadgePosition
{
	OVER_HEAD("Over your head"),
	OVER_SPOT("Over the spot"),
	FIXED("Fixed on screen");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
