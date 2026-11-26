package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What one pillar, or one half of a split pillar, shows. {@link #DEFAULT} is
 * the pillar as it was before sources existed (your target's health while
 * fighting, XP and inventory fill while skilling) so nobody's pillars change
 * until they pick something else. Names are short: a settings row has room
 * for the longest of them beside its label.
 */
@AllArgsConstructor
public enum PillarSource
{
	DEFAULT("Default", null, ""),
	NONE("Nothing", null, ""),
	HITPOINTS("Hitpoints", new Color(225, 35, 0), "HP"),
	PRAYER("Prayer", new Color(50, 200, 200), "PRY"),
	RUN_ENERGY("Run energy", new Color(235, 185, 20), "RUN"),
	SPEC("Special attack", new Color(60, 160, 60), "SPEC"),
	AMMO("Ammo", new Color(150, 110, 60), "AMMO"),
	RUNES("Runes", new Color(110, 90, 220), "RUNE"),
	CASTS("Casts left", new Color(90, 130, 230), "CAST"),
	CHARGES("Charges", new Color(230, 110, 40), "CHG"),
	CANNONBALLS("Cannonballs", new Color(120, 120, 130), "CB"),
	FOOD("Food", new Color(235, 140, 140), "FOOD"),
	PRAYER_DOSES("Prayer doses", new Color(130, 220, 230), "DOSE"),
	STAMINA("Stamina doses", new Color(210, 150, 40), "STAM"),
	BOOST("Stat boost", new Color(190, 90, 200), "BST"),
	TARGET_HP("Target health", new Color(200, 40, 40), "TGT");

	@Getter
	private final String name;

	/** The fill colour when the player has not picked one; null for the two that have none. */
	@Getter
	private final Color defaultColour;

	/** At the foot of a split half: what the pillar is, in a few letters. */
	@Getter
	private final String tag;

	/** True for a source that draws something of its own, not the default pillar and not blank. */
	boolean custom()
	{
		return this != DEFAULT && this != NONE;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
