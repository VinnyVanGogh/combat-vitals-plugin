package com.vinnyvangogh.combatvitals;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Marks the next fishing spot until the player fishes again: its tile
 * outlined, a line from the player to it, and an arrow at the edge of the
 * game view while it is off screen.
 *
 * Drawn whether or not the spot also went to Shortest Path. Shortest Path
 * ends its own path once the player is within its "Finish distance" of the
 * target (config key {@code finishDistance}, default 5 tiles, read straight
 * from its config in {@code ShortestPathPlugin#onGameTick}; a
 * {@code PluginMessage} cannot change it), and the next spot is usually
 * closer than that, so its line showed for a moment and went (Board's
 * in-game test, 2026-11-06). This mark stays.
 *
 * A known location outside the loaded scene has no screen position to mark;
 * Shortest Path's path covers that case.
 */
class NextSpotOverlay extends Overlay
{
	/** Fishing's skill colour, so the mark reads as the badge's. */
	private static final Color OUTLINE = SkillColours.of(Skill.FISHING);
	private static final Color LINE = new Color(OUTLINE.getRed(), OUTLINE.getGreen(), OUTLINE.getBlue(), 170);
	private static final Stroke LINE_STROKE = new BasicStroke(2);

	private final Client client;
	private final CombatVitalsPlugin plugin;

	@Inject
	NextSpotOverlay(Client client, CombatVitalsPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final NextFishingSpot.Spot spot = plugin.getNextSpot();
		final WorldView view = client.getTopLevelWorldView();
		final Player self = client.getLocalPlayer();
		if (spot == null || view == null || self == null || spot.getPlane() != view.getPlane())
		{
			return null;
		}

		final LocalPoint target = targetLocation(view, spot);
		final LocalPoint from = self.getLocalLocation();
		if (target != null && from != null)
		{
			mark(graphics, from, target, view.getPlane());
		}
		return null;
	}

	/** The tile outline, the line from you, and the edge arrow while the spot is off screen. */
	private void mark(Graphics2D graphics, LocalPoint from, LocalPoint target, int plane)
	{
		final Polygon tile = Perspective.getCanvasTilePoly(client, target);
		if (tile != null)
		{
			OverlayUtil.renderPolygon(graphics, tile, OUTLINE);
		}

		final Point to = Perspective.localToCanvas(client, target, plane);
		final Point at = Perspective.localToCanvas(client, from, plane);
		if (to != null && at != null)
		{
			drawLine(graphics, at, to);
		}

		final Rectangle game = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
			client.getViewportWidth(), client.getViewportHeight());
		if (to == null || !EdgeArrow.inside(game, to.getX(), to.getY(), EdgeArrow.MARGIN))
		{
			drawEdgeArrow(graphics, game, direction(from, target, at, to));
		}
	}

	private static void drawLine(Graphics2D graphics, Point at, Point to)
	{
		final Stroke stroke = graphics.getStroke();
		graphics.setStroke(LINE_STROKE);
		graphics.setColor(LINE);
		graphics.drawLine(at.getX(), at.getY(), to.getX(), to.getY());
		graphics.setStroke(stroke);
	}

	/** Where the spot is now: a loaded spot NPC's own tile, else the spot's. Null outside the scene. */
	private static LocalPoint targetLocation(WorldView view, NextFishingSpot.Spot spot)
	{
		if (spot.getNpc() instanceof NPC)
		{
			final LocalPoint npc = ((NPC) spot.getNpc()).getLocalLocation();
			if (npc != null)
			{
				return npc;
			}
		}
		return LocalPoint.fromWorld(view, new WorldPoint(spot.getX(), spot.getY(), spot.getPlane()));
	}

	/**
	 * Which way the spot lies on screen: from you to it when both project,
	 * else from the minimap, which turns with the camera so that up on it is
	 * into the screen. Null when neither is known.
	 */
	private double[] direction(LocalPoint from, LocalPoint target, Point at, Point to)
	{
		if (to != null && at != null)
		{
			return new double[]{to.getX() - at.getX(), to.getY() - at.getY()};
		}
		final Point mapFrom = Perspective.localToMinimap(client, from);
		final Point mapTo = Perspective.localToMinimap(client, target);
		if (mapFrom == null || mapTo == null)
		{
			return null;
		}
		return new double[]{mapTo.getX() - mapFrom.getX(), mapTo.getY() - mapFrom.getY()};
	}

	private static void drawEdgeArrow(Graphics2D graphics, Rectangle game, double[] direction)
	{
		final int[] tip = direction == null ? null : EdgeArrow.tip(game, direction[0], direction[1], EdgeArrow.MARGIN);
		if (tip == null)
		{
			return;
		}
		final Polygon head = EdgeArrow.head(tip[0], tip[1], direction[0], direction[1]);
		graphics.setColor(OUTLINE);
		graphics.fillPolygon(head);
		graphics.setColor(Color.BLACK);
		graphics.drawPolygon(head);
	}
}
