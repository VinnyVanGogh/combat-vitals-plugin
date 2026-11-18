package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

/**
 * Splashing mode's read-out: your Magic attack bonus and your chance to hit
 * the NPC you are casting at, coloured green when you are splashing, amber
 * for a small chance and red above the warn threshold. Text only.
 */
class SplashOverlay extends OverlayPanel
{
	private static final Color SAFE = new Color(0, 200, 83);
	private static final Color LOW = new Color(255, 193, 7);
	private static final Color WARN = new Color(220, 60, 60);

	private final SplashWatcher watcher;

	@Inject
	SplashOverlay(CombatVitalsPlugin plugin, SplashWatcher watcher)
	{
		super(plugin);
		this.watcher = watcher;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, PluginName.NAME + " splashing overlay");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final SplashTracker.Result result = watcher.result();
		if (result == null)
		{
			return null;
		}
		final Color colour = colour(result.status);
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(title(result.status))
			.color(colour)
			.build());
		panelComponent.getChildren().add(LineComponent.builder().left(result.bonusLine).leftColor(colour).build());
		panelComponent.getChildren().add(LineComponent.builder().left(result.chanceLine).leftColor(colour).build());
		return super.render(graphics);
	}

	static Color colour(SplashTracker.Status status)
	{
		switch (status)
		{
			case SAFE:
				return SAFE;
			case LOW:
			case UNKNOWN:
				return LOW;
			case WARN:
			default:
				return WARN;
		}
	}

	static String title(SplashTracker.Status status)
	{
		switch (status)
		{
			case SAFE:
				return "Splashing";
			case LOW:
				return "Small chance to hit";
			case UNKNOWN:
				return "Magic bonus too high";
			case WARN:
			default:
				return "Too likely to hit";
		}
	}
}
