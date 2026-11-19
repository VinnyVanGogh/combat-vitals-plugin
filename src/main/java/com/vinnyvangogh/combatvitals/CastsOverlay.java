package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

/**
 * The standalone "Casts left" card: the spell, the runes it costs per cast
 * and the casts left, in the same card style as the splashing card. Separate
 * from Splashing mode; {@link CastsVisibility} decides when it draws. Text
 * and icons only.
 */
class CastsOverlay extends Overlay
{
	private final SplashWatcher watcher;
	private final ItemManager itemManager;

	@Inject
	CastsOverlay(CombatVitalsPlugin plugin, SplashWatcher watcher, ItemManager itemManager)
	{
		super(plugin);
		this.watcher = watcher;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, PluginName.NAME + " casts left overlay");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final CastsReadout readout = watcher.castsReadout();
		if (readout == null)
		{
			return null;
		}
		final Color accent = readout.low ? SplashOverlay.WARN : SplashOverlay.SAFE;
		final List<SplashOverlay.Row> rows = new ArrayList<>();
		rows.add(SplashOverlay.Row.text(readout.spell, accent, true));
		if (readout.runeItems.length > 0)
		{
			final BufferedImage[] icons = new BufferedImage[readout.runeItems.length];
			for (int i = 0; i < icons.length; i++)
			{
				icons[i] = itemManager.getImage(readout.runeItems[i]);
			}
			rows.add(SplashOverlay.Row.runes(icons, readout.runeCosts));
		}
		rows.add(SplashOverlay.Row.text(readout.castsText, readout.low ? SplashOverlay.WARN : SplashOverlay.PLAIN,
			false));
		return SplashOverlay.card(graphics, rows, accent);
	}
}
