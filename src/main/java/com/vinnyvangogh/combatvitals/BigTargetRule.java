package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** What makes an NPC a big target. */
@AllArgsConstructor
public enum BigTargetRule
{
	BOSSES("Bosses only"),
	BOSSES_AND_HP("Bosses + max HP"),
	HP_ONLY("Max HP only");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
