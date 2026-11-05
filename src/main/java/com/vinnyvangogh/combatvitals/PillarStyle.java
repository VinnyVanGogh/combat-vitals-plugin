package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * How the side pillars are drawn: like RuneLite's Status Bars, framed with
 * a shine and tick marks, or flat in one colour on the bar background.
 * Public: RuneLite reads settings through a proxy outside this package.
 */
@AllArgsConstructor
public enum PillarStyle
{
	STATUS_BARS("Status Bars"),
	FLAT("Flat");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
