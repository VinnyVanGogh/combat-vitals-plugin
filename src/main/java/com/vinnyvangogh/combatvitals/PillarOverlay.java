package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Vitals on the side panel's pillars, where RuneLite's Status Bars draws
 * hitpoints and prayer: your target's health while fighting; XP to the next
 * level and inventory fill while skilling. Placement is {@link SidePillars};
 * this only finds the side panel and paints.
 */
class PillarOverlay extends Overlay
{
	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;

	@Inject
	PillarOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		// As Status Bars: drawn over the interface, positioned by hand.
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final PillarMode mode = config.pillarMode();
		final CombatVitalsPlugin.Readout target = mode.combat() ? plugin.getReadout() : null;
		final SkillingReadout skilling = mode.skilling() && config.skillingEnabled() ? plugin.getSkillingReadout() : null;
		if (target == null && skilling == null)
		{
			return null;
		}

		final SidePillars.Layout pillars = layout();
		if (pillars == null)
		{
			return null;
		}

		if (target != null)
		{
			final double fraction = HealthReadout.fraction(target.getRatio(), target.getScale(),
				target.getExactHealth(), target.getMaxHealth());
			Bars.up(graphics, pillars.get(config.pillarCombatSide()), fraction, HealthReadout.colour(fraction,
				config.healthFullColour(), config.healthMidColour(), config.healthLowColour()), config.barBackground());
		}
		else
		{
			final PillarSide xpSide = config.pillarXpSide();
			Bars.up(graphics, pillars.get(xpSide), skilling.getXp().getFraction(), skilling.getColour(),
				config.barBackground());
			drawInventory(graphics, pillars.get(xpSide.other()), skilling);
		}
		return null;
	}

	private void drawInventory(Graphics2D graphics, Rectangle pillar, SkillingReadout skilling)
	{
		final double fill = skilling.getInventoryFill();
		if (fill < 0)
		{
			return;
		}
		Bars.up(graphics, pillar, fill,
			InventoryFill.colour(fill, config.healthFullColour(), config.healthMidColour(), config.healthLowColour()),
			config.barBackground());
	}

	/** The first visible side-panel widget, in Status Bars' order; null when none is. */
	private SidePillars.Layout layout()
	{
		for (SidePillars.Viewport viewport : SidePillars.Viewport.values())
		{
			final Widget widget = client.getWidget(viewport.getWidget());
			if (widget != null && !widget.isHidden())
			{
				final Point at = widget.getCanvasLocation();
				return SidePillars.layout(viewport, at.getX(), at.getY(), widget.getWidth(), plugin.statusBars());
			}
		}
		return null;
	}
}
