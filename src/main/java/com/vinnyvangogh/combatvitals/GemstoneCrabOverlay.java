package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * The Gemstone Crab card: its health, how long until it burrows and where
 * that figure came from, then, once it has burrowed, how long its shell
 * stays. The idle-logout clock is added only when the casting card is not
 * already showing one, so no clock is drawn twice.
 */
class GemstoneCrabOverlay extends OverlayPanel
{
	private static final Color SAFE = new Color(0, 200, 83);
	private static final Color LOW = new Color(255, 193, 7);
	private static final Color WARN = new Color(220, 60, 60);
	private static final Color MUTED = new Color(160, 160, 160);

	private final Client client;
	private final CombatVitalsConfig config;
	private final GemstoneCrabWatcher watcher;
	private final SplashWatcher splash;

	@Inject
	GemstoneCrabOverlay(CombatVitalsPlugin plugin, Client client, CombatVitalsConfig config,
		GemstoneCrabWatcher watcher, SplashWatcher splash)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.watcher = watcher;
		this.splash = splash;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final GemstoneCrab.Reading r = watcher.reading();
		if (r == null)
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder().text("Gemstone Crab").build());
		if (r.phase == GemstoneCrab.Phase.SHELL)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Burrowed: crawl through the cave").leftColor(SAFE).build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Shell gone in").right(GemstoneCrab.clock(r.shellMs)).build());
		}
		else
		{
			final Color colour = watcher.due(r) ? WARN : r.remainingMs < 2 * 60_000L ? LOW : SAFE;
			if (r.percent() >= 0)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Health").right(r.percent() + "%").rightColor(colour).build());
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Burrows in").right(GemstoneCrab.clock(r.remainingMs)).rightColor(colour).build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Source").leftColor(MUTED).right(r.source.label).rightColor(MUTED).build());
		}
		// The casting card already carries the logout clock while it is up.
		if (splash.readout() == null)
		{
			final long idle = Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks())
				* (long) Constants.CLIENT_TICK_LENGTH;
			final long logout = client.getIdleTimeout() * (long) Constants.CLIENT_TICK_LENGTH;
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Logout in").leftColor(MUTED).right(GemstoneCrab.clock(logout - idle)).rightColor(MUTED).build());
		}
		return super.render(graphics);
	}
}
