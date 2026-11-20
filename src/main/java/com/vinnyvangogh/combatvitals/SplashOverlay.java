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
import java.util.function.IntFunction;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Constants;
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
 * The combat card: one compact rounded card with a coloured accent bar for
 * everything cast-related. A header (title, and the mode it is in), then up to
 * two groups: the Gemstone Crab (health, burrow clock) while at the crab, and
 * the casts (spell, runes, casts left, the Splashing mode Magic bonus and hit
 * chance, the clocks). Leaving the crab drops its group and leaves the casts
 * alone; there is never a second box. Text and icons only.
 */
class SplashOverlay extends Overlay
{
	static final Color SAFE = new Color(0, 200, 83);
	private static final Color LOW = new Color(255, 193, 7);
	static final Color WARN = new Color(220, 60, 60);
	static final Color PLAIN = new Color(235, 235, 235);
	private static final Color MUTED = new Color(160, 160, 160);
	private static final Color BACK = new Color(ColorScheme.DARKER_GRAY_COLOR.getRed(),
		ColorScheme.DARKER_GRAY_COLOR.getGreen(), ColorScheme.DARKER_GRAY_COLOR.getBlue(), 225);
	private static final Color TRACK = new Color(255, 255, 255, 40);
	private static final Color DIVIDER = new Color(255, 255, 255, 30);

	private static final int PAD = 6;
	private static final int ACCENT = 3;
	private static final int ICON = 14;
	private static final int ROW_GAP = 2;
	private static final int GROUP_GAP = 4;
	private static final int BAR_HEIGHT = 3;
	private static final int MIN_WIDTH = 140;
	private static final int TAG_GAP = 10;

	private final SplashWatcher watcher;
	private final GemstoneCrabWatcher crabWatcher;
	private final ItemManager itemManager;
	private final Client client;
	private final CombatVitalsConfig config;

	@Inject
	SplashOverlay(CombatVitalsPlugin plugin, SplashWatcher watcher, GemstoneCrabWatcher crabWatcher,
		ItemManager itemManager, Client client, CombatVitalsConfig config)
	{
		super(plugin);
		this.watcher = watcher;
		this.crabWatcher = crabWatcher;
		this.itemManager = itemManager;
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, PluginName.NAME + " combat card");
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
		final GemstoneCrab.Reading crab = crabWatcher.reading();
		if (readout == null && crab == null)
		{
			return null;
		}
		final long now = System.currentTimeMillis();
		final boolean dim = crab == null && readout != null && readout.isStopped();
		final Color accent = accent(readout, crab, crab != null && crabWatcher.due(crab));
		final List<Row> rows = rows(readout, crab, crab != null && crabWatcher.due(crab), config.splashMode(), now,
			accent, idleLogoutMillis(), id -> itemManager.getImage(id));
		final Composite before = graphics.getComposite();
		if (dim)
		{
			graphics.setComposite(AlphaComposite.SrcOver.derive(0.55f));
		}
		try
		{
			return card(graphics, rows, accent);
		}
		finally
		{
			graphics.setComposite(before);
		}
	}

	/** Paints the rounded card with its accent bar around the rows, and returns its size. */
	static Dimension card(Graphics2D graphics, List<Row> rows, Color accent)
	{
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
			height += row.height(lineHeight) + row.gapAfter();
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
			y += row.height(lineHeight) + row.gapAfter();
		}
		return new Dimension(width, height);
	}

	/** Milliseconds left before the idle logout, from the client's own idle count. */
	private long idleLogoutMillis()
	{
		final long idle = Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks())
			* (long) Constants.CLIENT_TICK_LENGTH;
		return client.getIdleTimeout() * (long) Constants.CLIENT_TICK_LENGTH - idle;
	}

	/**
	 * The card's rows, top to bottom. Pure: everything it needs is passed in, so
	 * the layout is tested without a client.
	 *
	 * @param r        the casts read-out, or null when nothing is being cast
	 * @param crab     the Gemstone Crab reading, or null when away from the crab
	 * @param crabDue  the burrow warning level has been reached
	 * @param splashing Splashing mode is on
	 * @param logoutMs idle logout left, shown on a crab-only card
	 */
	static List<Row> rows(SplashReadout r, GemstoneCrab.Reading crab, boolean crabDue, boolean splashing, long now,
		Color accent, long logoutMs, IntFunction<BufferedImage> icons)
	{
		final List<Row> rows = new ArrayList<>();
		final String tag = r == null ? null : splashing ? "Splashing mode" : "Casting";
		rows.add(Row.header(crab != null ? "Gemstone Crab" : title(r), tag, accent));
		if (crab != null)
		{
			crabRows(rows, crab, crabDue);
			if (r != null)
			{
				rows.add(Row.divider());
			}
		}
		if (r != null)
		{
			castRows(rows, r, crab != null, now, accent, icons);
		}
		else
		{
			rows.add(Row.text("Logout in " + GemstoneCrab.clock(logoutMs), MUTED, false));
		}
		return rows;
	}

	private static void crabRows(List<Row> rows, GemstoneCrab.Reading crab, boolean due)
	{
		if (crab.phase == GemstoneCrab.Phase.SHELL)
		{
			rows.add(Row.text("Burrowed: crawl through the cave", SAFE, true));
			rows.add(Row.text("Shell gone in " + GemstoneCrab.clock(crab.shellMs), MUTED, false));
			return;
		}
		final Color colour = due ? WARN : crab.remainingMs < 2 * 60_000L ? LOW : SAFE;
		rows.add(Row.text(crab.percent() >= 0 ? "Health " + crab.percent() + "%" : "Health unknown", colour, true));
		rows.add(Row.text("Burrows in " + GemstoneCrab.clock(crab.remainingMs), colour, false));
	}

	private static void castRows(List<Row> rows, SplashReadout r, boolean underCrab, long now, Color accent,
		IntFunction<BufferedImage> icons)
	{
		if (underCrab && r.spell != null)
		{
			rows.add(Row.text(r.spell, accent, true));
		}
		if (r.runeItems.length > 0)
		{
			final BufferedImage[] images = new BufferedImage[r.runeItems.length];
			for (int i = 0; i < images.length; i++)
			{
				images[i] = icons.apply(r.runeItems[i]);
			}
			rows.add(Row.runes(images, r.runeCosts));
		}
		if (r.castsText != null)
		{
			rows.add(Row.text(r.castsText, r.castsLow ? WARN : PLAIN, true));
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
			return;
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
	}

	private static String title(SplashReadout r)
	{
		if (r.splash != null)
		{
			return title(r.splash.status);
		}
		return r.spell == null ? "Casting" : r.spell;
	}

	/** The card's accent: the crab's burrow state at the crab, the splash status, else the casts' own warning. */
	private static Color accent(SplashReadout r, GemstoneCrab.Reading crab, boolean crabDue)
	{
		if (r != null && r.isStopped() && crab == null)
		{
			return MUTED;
		}
		if (crab != null)
		{
			return crabDue ? WARN : crab.phase != GemstoneCrab.Phase.SHELL && crab.remainingMs < 2 * 60_000L ? LOW : SAFE;
		}
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

	/** One line of the card: header, text, the rune icons, a divider, or the logout bar. */
	static final class Row
	{
		final String text;
		/** The mode label at the right of a header, or null. */
		final String tag;
		private final Color colour;
		private final boolean bold;
		private final BufferedImage[] icons;
		private final int[] counts;
		private final double fraction;
		private final boolean divider;

		private Row(String text, String tag, Color colour, boolean bold, BufferedImage[] icons, int[] counts,
			double fraction, boolean divider)
		{
			this.text = text;
			this.tag = tag;
			this.colour = colour;
			this.bold = bold;
			this.icons = icons;
			this.counts = counts;
			this.fraction = fraction;
			this.divider = divider;
		}

		static Row header(String text, String tag, Color colour)
		{
			return new Row(text, tag, colour, true, null, null, -1, false);
		}

		static Row text(String text, Color colour, boolean bold)
		{
			return new Row(text, null, colour, bold, null, null, -1, false);
		}

		static Row runes(BufferedImage[] icons, int[] counts)
		{
			return new Row(null, null, PLAIN, false, icons, counts, -1, false);
		}

		static Row bar(double fraction, Color colour)
		{
			return new Row(null, null, colour, false, null, null, fraction, false);
		}

		static Row divider()
		{
			return new Row(null, null, DIVIDER, false, null, null, -1, true);
		}

		int gapAfter()
		{
			return divider ? GROUP_GAP : ROW_GAP;
		}

		int height(int lineHeight)
		{
			if (divider)
			{
				return 1;
			}
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
				final int w = (bold ? boldMetrics : body).stringWidth(text);
				return tag == null ? w : w + TAG_GAP + body.stringWidth(tag);
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
			if (divider)
			{
				g.setColor(DIVIDER);
				g.fillRect(x, y, width, 1);
			}
			else if (text != null)
			{
				final FontMetrics m = bold ? boldMetrics : bodyMetrics;
				g.setFont(bold ? boldFont : body);
				final int baseline = y + (lineHeight + m.getAscent() - m.getDescent()) / 2;
				shadowed(g, text, x, baseline, colour);
				if (tag != null)
				{
					g.setFont(body);
					shadowed(g, tag, x + width - bodyMetrics.stringWidth(tag), baseline, MUTED);
				}
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
					shadowed(g, count, cx, baseline, PLAIN);
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

		private static void shadowed(Graphics2D g, String s, int x, int baseline, Color colour)
		{
			g.setColor(Color.BLACK);
			g.drawString(s, x + 1, baseline + 1);
			g.setColor(colour);
			g.drawString(s, x, baseline);
		}
	}
}
