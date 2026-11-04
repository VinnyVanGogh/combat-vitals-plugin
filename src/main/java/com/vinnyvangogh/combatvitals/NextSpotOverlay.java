package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines the tile of the next fishing spot when the path to it could not
 * go to Shortest Path (not installed, or switched off). Draws nothing
 * otherwise.
 */
class NextSpotOverlay extends Overlay
{
	/** Fishing's skill colour, so the outline reads as the badge's. */
	private static final Color OUTLINE = SkillColours.of(Skill.FISHING);

	private final CombatVitalsPlugin plugin;

	@Inject
	NextSpotOverlay(CombatVitalsPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final NPC spot = plugin.getHighlightedSpot();
		if (spot == null)
		{
			return null;
		}

		final Polygon tile = spot.getCanvasTilePoly();
		if (tile != null)
		{
			OverlayUtil.renderPolygon(graphics, tile, OUTLINE);
		}
		return null;
	}
}
