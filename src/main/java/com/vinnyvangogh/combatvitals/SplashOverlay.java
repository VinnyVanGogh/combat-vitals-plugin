package com.vinnyvangogh.combatvitals;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

/**
 * The splashing and casts-left card, styled like the skilling badge: one
 * compact rounded card with a coloured accent bar, the spell and the runes it
 * costs, the casts left, the Magic bonus and hit chance (Splashing mode), and
 * the pump-style clocks: time until the idle logout and time spent casting.
 * Text and icons only.
 */
class SplashOverlay extends Overlay
{
	private static final Color SAFE = new Color(0, 200, 83);
	private static final Color LOW = new Color(255, 193, 7);
	private static final Color WARN = new Color(220, 60, 60);
	private static final Color PLAIN = new Color(235, 235, 235);
	private static final Color MUTED = new Color(160, 160, 160);
	private static final Color BACK = new Color(ColorScheme.DARKER_GRAY_COLOR.getRed(),
		ColorScheme.DARKER_GRAY_COLOR.getGreen(), ColorScheme.DARKER_GRAY_COLOR.getBlue(), 225);
	private static final Color TRACK = new Color(255, 255, 255, 40);

	private static final int PAD = 5;
	private static final int ACCENT = 3;
	private static final int ICON = 14;
	private static final int ROW_GAP = 2;
	private static final int BAR_HEIGHT = 3;
	private static final int MIN_WIDTH = 130;

	private final SplashWatcher watcher;
	private final ItemManager itemManager;

	@Inject
	SplashOverlay(CombatVitalsPlugin plugin, SplashWatcher watcher, ItemManager itemManager)
	{
		super(plugin);
		this.watcher = watcher;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, PluginName.NAME + " splashing overlay");
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, modeOption(false), PluginName.NAME));
	}

	/** The right-click option's text; clicking it flips Splashing mode. */
	static final String MODE_PREFIX = "Splashing mode: ";

	static String modeOption(boolean on)
	{
		return MODE_PREFIX + (on ? "on" : "off");
	}

	/** Shows the current state in the right-click option; client thread. */
	void showMode(boolean on)
	{
		final List<OverlayMenuEntry> entries = getMenuEntries();
		for (int i = 0; i < entries.size(); i++)
		{
			final OverlayMenuEntry e = entries.get(i);
			if (e.getOption() != null && e.getOption().startsWith(MODE_PREFIX))
			{
				entries.set(i, new OverlayMenuEntry(e.getMenuAction(), modeOption(on), e.getTarget()));
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final SplashReadout readout = watcher.readout();
		if (readout == null)
		{
			return null;
		}
		final long now = System.currentTimeMillis();
		final Color accent = readout.isStopped() ? MUTED : accent(readout);
		final List<Row> rows = rows(readout, now, accent);
		final Composite before = graphics.getComposite();
		if (readout.isStopped())
		{
			graphics.setComposite(AlphaComposite.SrcOver.derive(0.55f));
		}

		final Font body = FontManager.getRunescapeSmallFont();
		final Font bold = FontManager.getRunescapeBoldFont();
		final FontMetrics bodyMetrics = graphics.getFontMetrics(body);
		final FontMetrics boldMetrics = graphics.getFontMetrics(bold);
		final int lineHeight = Math.max(bodyMetrics.getHeight(), boldMetrics.getHeight());

		int width = MIN_WIDTH;
		int height = PAD * 2;
		for (Row row : rows)
		{
			width = Math.max(width, ACCENT + PAD * 2 + row.width(bodyMetrics, boldMetrics));
			height += row.height(lineHeight) + ROW_GAP;
		}
		height -= ROW_GAP;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(BACK);
		graphics.fillRoundRect(0, 0, width, height, 8, 8);
		graphics.setColor(accent);
		graphics.fillRoundRect(0, 0, ACCENT + 4, height, 8, 8);
		graphics.setColor(BACK);
		graphics.fillRect(ACCENT, 0, 5, height);

		int y = PAD;
		final int x = ACCENT + PAD;
		for (Row row : rows)
		{
			row.draw(graphics, x, y, width - x - PAD, lineHeight, body, bold, bodyMetrics, boldMetrics);
			y += row.height(lineHeight) + ROW_GAP;
		}
		graphics.setComposite(before);
		return new Dimension(width, height);
	}

	private List<Row> rows(SplashReadout r, long now, Color accent)
	{
		final List<Row> rows = new ArrayList<>();
		rows.add(Row.text(title(r), accent, true));
		if (r.runeItems.length > 0)
		{
			final BufferedImage[] icons = new BufferedImage[r.runeItems.length];
			for (int i = 0; i < icons.length; i++)
			{
				icons[i] = itemManager.getImage(r.runeItems[i]);
			}
			rows.add(Row.runes(icons, r.runeCosts));
		}
		if (r.castsText != null)
		{
			rows.add(Row.text(r.castsText, r.castsLow ? WARN : PLAIN, false));
		}
		if (r.splash != null)
		{
			final Color colour = colour(r.splash.status);
			rows.add(Row.text(r.splash.bonusLine, colour, false));
			if (r.splash.chanceLine != null)
			{
				rows.add(Row.text(r.splash.chanceLine, colour, false));
			}
		}
		if (r.isStopped())
		{
			rows.add(Row.text(SplashTimer.stoppedAgo(now - r.stoppedAt), MUTED, false));
			return rows;
		}
		final String logout = r.logoutText(now);
		final long session = r.session(now);
		if (logout != null || session >= 0)
		{
			final String timers = (logout == null ? "" : logout)
				+ (logout != null && session >= 0 ? "  |  " : "")
				+ (session >= 0 ? "Casting " + SplashTimer.clock(session) : "");
			rows.add(Row.text(timers, MUTED, false));
			final double fraction = r.logoutFraction(now);
			if (fraction >= 0)
			{
				rows.add(Row.bar(fraction, fraction < 0.2 ? WARN : accent));
			}
		}
		return rows;
	}

	private static String title(SplashReadout r)
	{
		if (r.splash != null)
		{
			return title(r.splash.status);
		}
		return r.spell == null ? "Casting" : r.spell;
	}

	/** The card's accent: the splash status with Splashing mode on, else the casts' own warning. */
	private static Color accent(SplashReadout r)
	{
		if (r.castsLow)
		{
			return WARN;
		}
		return r.splash == null ? SAFE : colour(r.splash.status);
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

	/** One line of the card: text, the rune icons, or the logout bar. */
	private static final class Row
	{
		private final String text;
		private final Color colour;
		private final boolean bold;
		private final BufferedImage[] icons;
		private final int[] counts;
		private final double fraction;

		private Row(String text, Color colour, boolean bold, BufferedImage[] icons, int[] counts, double fraction)
		{
			this.text = text;
			this.colour = colour;
			this.bold = bold;
			this.icons = icons;
			this.counts = counts;
			this.fraction = fraction;
		}

		static Row text(String text, Color colour, boolean bold)
		{
			return new Row(text, colour, bold, null, null, -1);
		}

		static Row runes(BufferedImage[] icons, int[] counts)
		{
			return new Row(null, PLAIN, false, icons, counts, -1);
		}

		static Row bar(double fraction, Color colour)
		{
			return new Row(null, colour, false, null, null, fraction);
		}

		int height(int lineHeight)
		{
			if (icons != null)
			{
				return Math.max(ICON, lineHeight);
			}
			return text == null ? BAR_HEIGHT : lineHeight;
		}

		int width(FontMetrics body, FontMetrics boldMetrics)
		{
			if (text != null)
			{
				return (bold ? boldMetrics : body).stringWidth(text);
			}
			if (icons != null)
			{
				int w = 0;
				for (int i = 0; i < icons.length; i++)
				{
					w += ICON + 2 + body.stringWidth("x" + counts[i]) + 6;
				}
				return w;
			}
			return 0;
		}

		void draw(Graphics2D g, int x, int y, int width, int lineHeight, Font body, Font boldFont,
			FontMetrics bodyMetrics, FontMetrics boldMetrics)
		{
			if (text != null)
			{
				final FontMetrics m = bold ? boldMetrics : bodyMetrics;
				g.setFont(bold ? boldFont : body);
				final int baseline = y + (lineHeight + m.getAscent() - m.getDescent()) / 2;
				g.setColor(Color.BLACK);
				g.drawString(text, x + 1, baseline + 1);
				g.setColor(colour);
				g.drawString(text, x, baseline);
			}
			else if (icons != null)
			{
				g.setFont(body);
				int cx = x;
				final int rowHeight = height(lineHeight);
				for (int i = 0; i < icons.length; i++)
				{
					if (icons[i] != null)
					{
						g.drawImage(icons[i], cx, y + (rowHeight - ICON) / 2, ICON, ICON, null);
					}
					cx += ICON + 2;
					final String count = "x" + counts[i];
					final int baseline = y + (rowHeight + bodyMetrics.getAscent() - bodyMetrics.getDescent()) / 2;
					g.setColor(Color.BLACK);
					g.drawString(count, cx + 1, baseline + 1);
					g.setColor(PLAIN);
					g.drawString(count, cx, baseline);
					cx += bodyMetrics.stringWidth(count) + 6;
				}
			}
			else
			{
				g.setColor(TRACK);
				g.fillRoundRect(x, y, width, BAR_HEIGHT, 2, 2);
				g.setColor(colour);
				g.fillRoundRect(x, y, (int) Math.round(width * fraction), BAR_HEIGHT, 2, 2);
			}
		}
	}
}
