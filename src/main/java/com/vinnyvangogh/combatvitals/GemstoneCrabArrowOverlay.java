package com.vinnyvangogh.combatvitals;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A down arrow above the cave to crawl through, and the cave's outline, from
 * the burrow warning until the shell is gone. Drawing only: nothing is
 * clicked for you.
 */
class GemstoneCrabArrowOverlay extends Overlay
{
	private static final Color ARROW = new Color(0, 200, 83);
	private static final Color EDGE = new Color(0, 0, 0, 200);
	private static final Color FILL = new Color(0, 200, 83, 70);
	private static final int HEIGHT = 30;
	private static final int HALF_WIDTH = 17;
	private static final int LIFT = 80;

	private final GemstoneCrabWatcher watcher;

	@Inject
	GemstoneCrabArrowOverlay(CombatVitalsPlugin plugin, GemstoneCrabWatcher watcher)
	{
		super(plugin);
		this.watcher = watcher;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final GameObject cave = watcher.arrowTarget();
		if (cave == null)
		{
			return null;
		}
		final Shape hull = cave.getConvexHull();
		if (hull != null)
		{
			graphics.setColor(FILL);
			graphics.fill(hull);
			final Stroke before = graphics.getStroke();
			graphics.setStroke(new BasicStroke(3f));
			graphics.setColor(ARROW);
			graphics.draw(hull);
			graphics.setStroke(before);
		}
		final Point top = cave.getCanvasLocation(LIFT);
		if (top != null)
		{
			final int x = top.getX();
			final int y = top.getY();
			final Polygon arrow = new Polygon(
				new int[]{x - HALF_WIDTH, x + HALF_WIDTH, x},
				new int[]{y - HEIGHT, y - HEIGHT, y}, 3);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(ARROW);
			graphics.fillPolygon(arrow);
			graphics.setColor(EDGE);
			graphics.drawPolygon(arrow);
		}
		return null;
	}
}
