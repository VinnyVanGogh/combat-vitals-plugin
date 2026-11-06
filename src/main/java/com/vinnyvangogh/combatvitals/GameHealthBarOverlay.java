package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * While "Hide game health bar" is on, draws a bar in the game's place for
 * every player and NPC that shows one ({@link GameHealthBars}), so hiding
 * the game's sprites takes no bar away. Your target and you keep the
 * plugin's readout instead, which is what the option is for.
 */
class GameHealthBarOverlay extends Overlay
{
	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;

	@Inject
	GameHealthBarOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final WorldView view = client.getTopLevelWorldView();
		if (!config.hideGameHealthBar() || view == null)
		{
			return null;
		}

		// As the overhead overlay decides what it draws over: a player target
		// only with "Show over players" on.
		final CombatVitalsPlugin.Readout target = plugin.getReadout();
		final Actor opponent = target != null && (target.isNpc() || config.overheadForPlayers())
			? readoutActor(target) : null;
		final Actor self = readoutActor(plugin.getSelfReadout());
		final GameHealthBars.Access<Actor> access = new ClientAccess(view.getPlane());
		draw(graphics, GameHealthBars.plan(view.npcs(), a -> a == opponent, access));
		draw(graphics, GameHealthBars.plan(view.players(), a -> a == opponent || a == self, access));
		return null;
	}

	/** The actor a readout is drawn over, when the overhead readout is on; else null. */
	private Actor readoutActor(CombatVitalsPlugin.Readout readout)
	{
		return readout == null || !config.showOverhead() ? null : readout.getActor();
	}

	private static void draw(Graphics2D graphics, Iterable<GameHealthBars.Bar> bars)
	{
		for (GameHealthBars.Bar bar : bars)
		{
			final Rectangle box = bar.getBox();
			graphics.setColor(GameHealthBars.EMPTY);
			graphics.fillRect(box.x, box.y, box.width, box.height);
			if (bar.getFilled() > 0)
			{
				graphics.setColor(GameHealthBars.FILL);
				graphics.fillRect(box.x, box.y, bar.getFilled(), box.height);
			}
		}
	}

	/** The client's view of an actor's bar, read during the frame. */
	private final class ClientAccess implements GameHealthBars.Access<Actor>
	{
		private final int plane;

		ClientAccess(int plane)
		{
			this.plane = plane;
		}

		@Override
		public int ratio(Actor actor)
		{
			return actor.getHealthRatio();
		}

		@Override
		public int scale(Actor actor)
		{
			return actor.getHealthScale();
		}

		@Override
		public boolean talking(Actor actor)
		{
			final String text = actor.getOverheadText();
			return text != null && !text.isEmpty() && actor.getOverheadCycle() > 0;
		}

		@Override
		public int[] anchor(Actor actor)
		{
			// An actor being removed (a kill) can throw from its scene lookups
			// mid-frame; that frame skips it.
			final Point at = CombatVitalsOverheadOverlay.onScreen(() ->
			{
				final LocalPoint local = actor.getLocalLocation();
				return local == null ? null : Perspective.localToCanvas(client, local, plane,
					Math.max(0, actor.getLogicalHeight()) + GameHealthBars.ABOVE_HEIGHT);
			});
			return at == null ? null : new int[]{at.getX(), at.getY()};
		}
	}
}
