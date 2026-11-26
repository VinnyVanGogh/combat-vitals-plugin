package com.vinnyvangogh.combatvitals;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/** One side pillar's setup: how it is split and what each part shows. */
@Getter
@EqualsAndHashCode
final class PillarSetup
{
	/** The pillar as it has always been. */
	static final PillarSetup DEFAULT = new PillarSetup(PillarSplit.OFF, PillarSource.DEFAULT, PillarSource.NONE);

	private final PillarSplit split;
	private final PillarSource first;
	private final PillarSource second;

	PillarSetup(PillarSplit split, PillarSource first, PillarSource second)
	{
		this.split = split == null ? PillarSplit.OFF : split;
		this.first = first == null ? PillarSource.DEFAULT : first;
		// The default pillar is one per side: a second part cannot be it as well.
		this.second = second == null || second == PillarSource.DEFAULT ? PillarSource.NONE : second;
	}

	/** What part {@code index} (0 or 1) shows; a whole pillar has only part 0. */
	PillarSource source(int index)
	{
		return index == 0 ? first : second;
	}

	/** Whether anything here draws without the default pillar's mode being on. */
	boolean anyCustom()
	{
		return first.custom() || (split != PillarSplit.OFF && second.custom());
	}
}
