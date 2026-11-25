package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** How the skilling badge is drawn: label/value rows on a card, or the original bars. */
@AllArgsConstructor
public enum CardStyle
{
	CLEAN("Clean"),
	RETRO("Retro");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
