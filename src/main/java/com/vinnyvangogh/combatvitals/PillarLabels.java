package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What each side pillar is labelled with at its top: an icon saying what it
 * is and the number under it, the icon alone, or nothing. Public: RuneLite
 * reads settings through a proxy outside this package (ConfigKeysTest).
 */
@AllArgsConstructor
public enum PillarLabels
{
	ICON_AND_NUMBER("Icon and number"),
	ICON_ONLY("Icon only"),
	NONE("None");

	@Getter
	private final String name;

	boolean icon()
	{
		return this != NONE;
	}

	boolean number()
	{
		return this == ICON_AND_NUMBER;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
