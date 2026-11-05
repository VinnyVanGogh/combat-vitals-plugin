package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import lombok.Builder;
import lombok.Getter;

/** One side pillar for one frame, as {@link PillarPainter} draws it. */
@Getter
@Builder(toBuilder = true)
final class Pillar
{
	/** How full, 0 to 1. */
	private final double fraction;

	private final Color colour;

	/** What the pillar is: the skill, a backpack, a heart. Null draws none. */
	private final BufferedImage icon;

	/** Under the icon: "82%", "18", "12". Null draws none. */
	private final String number;

	/** At the foot: the XP pillar's level. Null draws none. */
	private final String bottom;

	/** Equal parts marked on the track: 4 for quarters, 28 for slots, 1 for none. */
	@Builder.Default
	private final int divisions = 1;

	/** The activity has stopped: the pillar is dimmed. */
	private final boolean idle;

	/** The hover text. */
	private final String tooltip;
}
