package com.vinnyvangogh.combatvitals;

import java.awt.Rectangle;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** How one side pillar is divided: whole, two thin lanes, or an upper and a lower half. */
@AllArgsConstructor
public enum PillarSplit
{
	OFF("Off (one pillar)"),
	SIDE_BY_SIDE("Side by side"),
	TOP_BOTTOM("Top and bottom");

	/** The space between the two parts. */
	static final int GAP = 1;

	@Getter
	private final String name;

	/** How many things the pillar shows: 1 or 2. */
	int segments()
	{
		return this == OFF ? 1 : 2;
	}

	/**
	 * The rectangle of each part of {@code box}, first part first: left lane then right, or upper
	 * half then lower. Together with the gap they cover the box exactly.
	 */
	Rectangle[] split(Rectangle box)
	{
		switch (this)
		{
			case SIDE_BY_SIDE:
			{
				final int first = (box.width - GAP) / 2;
				return new Rectangle[] {
					new Rectangle(box.x, box.y, first, box.height),
					new Rectangle(box.x + first + GAP, box.y, box.width - first - GAP, box.height),
				};
			}
			case TOP_BOTTOM:
			{
				final int first = (box.height - GAP) / 2;
				return new Rectangle[] {
					new Rectangle(box.x, box.y, box.width, first),
					new Rectangle(box.x, box.y + first + GAP, box.width, box.height - first - GAP),
				};
			}
			case OFF:
			default:
				return new Rectangle[] {new Rectangle(box)};
		}
	}

	@Override
	public String toString()
	{
		return name;
	}
}
