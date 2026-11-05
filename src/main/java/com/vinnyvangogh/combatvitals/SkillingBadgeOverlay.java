package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The skilling badge with "Fixed on screen": an ordinary overlay the user
 * Alt-drags wherever they like, and RuneLite remembers where.
 */
class SkillingBadgeOverlay extends Overlay
{
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;
	private final SkillingBadgeRenderer badge;

	@Inject
	SkillingBadgeOverlay(CombatVitalsPlugin plugin, CombatVitalsConfig config, SkillingBadgeRenderer badge)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.badge = badge;

		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final SkillingReadout readout = plugin.getSkillingReadout();
		if (readout == null || !config.skillingEnabled() || config.skillingPosition() != BadgePosition.FIXED)
		{
			return null;
		}
		return badge.drawFromCorner(graphics, readout);
	}
}
