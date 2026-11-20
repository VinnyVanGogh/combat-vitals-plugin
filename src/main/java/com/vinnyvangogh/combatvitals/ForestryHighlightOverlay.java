package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines the parts of a Forestry event in the game view, so the base
 * RuneLite Woodcutting plugin's highlights are covered here. Observation
 * only: it draws, it never clicks.
 */
class ForestryHighlightOverlay extends Overlay
{
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;

	@Inject
	ForestryHighlightOverlay(CombatVitalsPlugin plugin, CombatVitalsConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.forestryHighlights())
		{
			return null;
		}
		final Color colour = config.forestryHighlightColour();
		for (Object thing : plugin.getForestryMarks().wanted(kind -> wants(config, kind)))
		{
			final Shape hull = hull(thing);
			if (hull != null)
			{
				OverlayUtil.renderPolygon(graphics, hull, colour);
			}
		}
		return null;
	}

	/** Whether the player has this kind of Forestry event's highlight on. */
	static boolean wants(CombatVitalsConfig config, SkillingEvents.Forestry kind)
	{
		switch (kind)
		{
			case RISING_ROOTS:
				return config.highlightRisingRoots();
			case STRUGGLING_SAPLING:
				return config.highlightStrugglingSapling();
			case FLOWERING_BUSH:
				return config.highlightFloweringBush();
			case LEPRECHAUN:
				return config.highlightLeprechaun();
			case POACHERS:
				return config.highlightPoachers();
			case PHEASANT_CONTROL:
				return config.highlightPheasantControl();
			case BEE_HIVE:
				return config.highlightBeeHive();
			case ENCHANTMENT_RITUAL:
				return config.highlightEnchantmentRitual();
			case FRIENDLY_ENT:
				return config.highlightFriendlyEnt();
			default:
				return false;
		}
	}

	private static Shape hull(Object thing)
	{
		if (thing instanceof GameObject)
		{
			return ((GameObject) thing).getConvexHull();
		}
		if (thing instanceof NPC)
		{
			return ((NPC) thing).getConvexHull();
		}
		return null;
	}
}
