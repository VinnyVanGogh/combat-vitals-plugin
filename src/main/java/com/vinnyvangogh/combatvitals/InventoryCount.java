package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Whether the badge's inventory row counts the free slots or the used ones. */
@AllArgsConstructor
public enum InventoryCount
{
	FREE("Free slots"),
	USED("Used slots");

	@Getter
	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
