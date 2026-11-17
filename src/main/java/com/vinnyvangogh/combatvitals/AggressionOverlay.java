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

/**
 * The NPC aggression countdown as a small corner panel: "Aggressive for
 * 9:54", then "NPCs are no longer aggressive here". Text from
 * {@link AggressionTimer#text}; nothing is drawn while the timer is idle or
 * the setting is off.
 */
class AggressionOverlay extends OverlayPanel
{
	private static final Color CALM = new Color(0, 200, 83);
	private static final Color RUNNING = new Color(255, 170, 40);

	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;

	@Inject
	AggressionOverlay(CombatVitalsPlugin plugin, CombatVitalsConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.aggressionTimer() || !VitalsHalves.combat(config))
		{
			return null;
		}
		final String text = plugin.aggressionText();
		if (text == null)
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder().text("NPC aggression").build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(text)
			.leftColor(plugin.aggressionCalm() ? CALM : RUNNING)
			.build());
		return super.render(graphics);
	}
}
