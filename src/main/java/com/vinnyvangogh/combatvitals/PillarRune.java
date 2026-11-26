package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Which rune the Runes pillar source counts; the same order as {@code SpellCasts.Rune}. */
@AllArgsConstructor
public enum PillarRune
{
	AIR("Air"), WATER("Water"), EARTH("Earth"), FIRE("Fire"), MIND("Mind"), CHAOS("Chaos"), DEATH("Death"),
	BLOOD("Blood"), SOUL("Soul"), WRATH("Wrath");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
