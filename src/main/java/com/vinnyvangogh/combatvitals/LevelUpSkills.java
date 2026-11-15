package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Which skills' level-ups the level-up alert is about. */
@AllArgsConstructor
public enum LevelUpSkills
{
	ALL("All skills"),
	/** Everything but the seven skills that make up combat level. */
	SKILLING("Non-combat only");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
