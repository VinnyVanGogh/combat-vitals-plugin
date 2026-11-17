package com.vinnyvangogh.combatvitals;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The 21x21 area's edge on the ground, while the aggression timer runs, when
 * "Aggression area lines" is on (off by default: the base NPC Aggression
 * Timer plugin draws the same lines). Tile outlines along the edge only.
 */
class AggressionAreaOverlay extends Overlay
{
	private static final Color LINE = new Color(255, 170, 40, 160);

	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;

	@Inject
	AggressionAreaOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final AggressionTimer timer = plugin.aggressionArea();
		if (timer == null || !config.aggressionTimer() || !config.aggressionAreaLines()
			|| !VitalsHalves.combat(config))
		{
			return null;
		}
		graphics.setColor(LINE);
		graphics.setStroke(new BasicStroke(1));
		for (int x = timer.minX(); x <= timer.maxX(); x++)
		{
			for (int y = timer.minY(); y <= timer.maxY(); y++)
			{
				final boolean edge = x == timer.minX() || x == timer.maxX() || y == timer.minY() || y == timer.maxY();
				if (edge)
				{
					outline(graphics, x, y, timer.plane());
				}
			}
		}
		return null;
	}

	private void outline(Graphics2D graphics, int x, int y, int plane)
	{
		final LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(),
			new net.runelite.api.coords.WorldPoint(x, y, plane));
		if (local == null)
		{
			return;
		}
		final Polygon poly = Perspective.getCanvasTilePoly(client, local);
		if (poly != null)
		{
			graphics.draw(poly);
		}
	}
}
