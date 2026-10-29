package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the readout over the target itself, which is the thing the stock
 * plugin cannot do.
 */
class CombatVitalsOverheadOverlay extends Overlay
{
	/** Space between the text and the edge of the bar behind it. */
	private static final int PAD_X = 4;
	private static final int PAD_Y = 2;

	private static final Color BAR_BORDER = new Color(0, 0, 0, 200);

	/** The game's default overhead chat colour. */
	private static final Color CHAT_COLOUR = Color.YELLOW;

	/**
	 * Height cleared when lifting the text above the game's own health bar.
	 * The bar's position is not exposed by the API, so this is a fixed clearance
	 * that puts the text above it; the offset setting fine-tunes from there.
	 */
	private static final int BAR_CLEARANCE = 34;

	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;

	@Inject
	CombatVitalsOverheadOverlay(CombatVitalsPlugin plugin, CombatVitalsConfig config)
	{
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Rebuilt every frame and handed over whole: an actor missing from it
		// gets its chat line back from the plugin on the next client tick.
		final Set<Actor> covered = new HashSet<>();
		try
		{
			if (!config.showOverhead())
			{
				return null;
			}

			final CombatVitalsPlugin.Readout opponent = plugin.getReadout();
			if (opponent != null && (opponent.isNpc() || config.overheadForPlayers()))
			{
				draw(graphics, opponent, covered);
			}

			// Both sides in one visual language. Getting your own health from some
			// other plugin would render it in a different style beside your
			// opponent's, which is the main reason this belongs here.
			draw(graphics, plugin.getSelfReadout(), covered);
			return null;
		}
		finally
		{
			plugin.setChatCovered(covered);
		}
	}

	private void draw(Graphics2D graphics, CombatVitalsPlugin.Readout readout, Set<Actor> covered)
	{
		if (readout == null)
		{
			return;
		}

		final String text = HealthReadout.text(readout.getRange(), readout.getMaxHealth(),
			readout.getRatio(), readout.getScale(), config.displayMode(), config.percentageDecimals(),
			readout.getExactHealth());
		if (text == null)
		{
			return;
		}

		final Actor actor = readout.getActor();
		final Point location = actor.getCanvasTextLocation(graphics, text, zOffset(actor));
		if (location == null)
		{
			return;
		}

		final double fraction = HealthReadout.fraction(readout.getRatio(), readout.getScale(),
			readout.getExactHealth(), readout.getMaxHealth());

		if (config.overheadStyle() == OverheadStyle.BAR)
		{
			drawBar(graphics, location, text, fraction);

			// White on a filled bar. Colouring the text as well as the bar makes
			// the two fight each other — red text on a green bar is the worst
			// case and the reason this style exists.
			OverlayUtil.renderTextLocation(graphics, location, text, Color.WHITE);
		}
		else
		{
			final Color colour = config.colourGradient() ? gradient(fraction) : Color.WHITE;
			OverlayUtil.renderTextLocation(graphics, location, text, colour);
		}

		if (drawChatAbove(graphics, actor, readoutBounds(graphics, location, text)))
		{
			covered.add(actor);
		}
	}

	/** The readout's box on screen, the bar's padding included for the Bar style. */
	private Rectangle readoutBounds(Graphics2D graphics, Point location, String text)
	{
		final FontMetrics metrics = graphics.getFontMetrics();
		final int padX = config.overheadStyle() == OverheadStyle.BAR ? PAD_X : 0;
		final int padY = config.overheadStyle() == OverheadStyle.BAR ? PAD_Y : 0;

		return new Rectangle(
			location.getX() - padX,
			location.getY() - metrics.getAscent() - padY,
			metrics.stringWidth(text) + padX * 2,
			metrics.getAscent() + metrics.getDescent() + padY * 2);
	}

	/**
	 * A bar exactly as wide as the text it sits behind. The game's own bar is a
	 * fixed handful of pixels and the readout is always wider, so sizing to the
	 * text is what makes the two legible together instead of overlapping.
	 */
	private void drawBar(Graphics2D graphics, Point location, String text, double fraction)
	{
		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(text);

		final int x = location.getX() - PAD_X;
		final int y = location.getY() - metrics.getAscent() - PAD_Y;
		final int width = textWidth + PAD_X * 2;
		final int height = metrics.getAscent() + metrics.getDescent() + PAD_Y * 2;

		// Opaque by default: a translucent background lets the game's own health
		// bar show through and the two read as one smeared bar.
		graphics.setColor(config.barBackground());
		graphics.fillRect(x, y, width, height);

		final int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
		if (filled > 0)
		{
			graphics.setColor(config.colourGradient() ? gradient(fraction) : config.healthFullColour());
			graphics.fillRect(x, y, filled, height);
		}

		graphics.setColor(BAR_BORDER);
		graphics.drawRect(x, y, width, height);
	}

	/**
	 * Redraws the actor's overhead chat just above the readout while the
	 * readout covers the game's own line. The game draws chat at the actor's
	 * logical height, right where the readout sits, and this overlay paints
	 * over it; the game's text cannot be moved, so the readout keeps its place
	 * and the chat gets a readable copy on top.
	 *
	 * Measured in screen space, so it holds at any zoom, position and model
	 * height. When the readout does not cover the game's line (another
	 * position, or the offset setting moves it clear) no copy is drawn, so the
	 * message never shows twice.
	 *
	 * With "Hide game chat under readout" on, the plugin takes the game's line
	 * off the actor while this copy is drawn; the text then comes from the
	 * plugin, which still has it.
	 *
	 * @return true when the copy was drawn, meaning the game's line is covered
	 */
	private boolean drawChatAbove(Graphics2D graphics, Actor actor, Rectangle readout)
	{
		final String chat = plugin.overheadChat(actor);
		if (chat == null)
		{
			return false;
		}

		// The game's overhead chat font. Set before measuring: the canvas text
		// location centres on the width of the current font.
		final Font previous = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeBoldFont());
		try
		{
			final Point chatAt = actor.getCanvasTextLocation(graphics, chat, Math.max(0, actor.getLogicalHeight()));
			if (chatAt == null)
			{
				return false;
			}

			final FontMetrics metrics = graphics.getFontMetrics();
			final Rectangle line = new Rectangle(chatAt.getX(), chatAt.getY() - metrics.getAscent(),
				metrics.stringWidth(chat), metrics.getAscent() + metrics.getDescent());

			final Integer baseline = OverheadChat.baselineAbove(readout, line, metrics.getDescent());
			if (baseline == null)
			{
				return false;
			}

			// Centred on the actor like the game's line, in its default yellow
			// with the black shadow renderTextLocation adds. The API does not
			// expose the chat's colour or effect, so those are not reproduced.
			OverlayUtil.renderTextLocation(graphics, new Point(chatAt.getX(), baseline), chat, CHAT_COLOUR);
			return true;
		}
		finally
		{
			graphics.setFont(previous);
		}
	}

	private Color gradient(double fraction)
	{
		return HealthReadout.colour(fraction,
			config.healthFullColour(), config.healthMidColour(), config.healthLowColour());
	}

	/**
	 * Height above the actor's base. A large NPC's own name plate and the
	 * top-screen boss bar both crowd the top, which is why this is adjustable
	 * rather than fixed.
	 */
	private int zOffset(Actor actor)
	{
		final int logicalHeight = Math.max(0, actor.getLogicalHeight());

		final int base;
		switch (config.overheadPosition())
		{
			case MIDDLE:
				base = logicalHeight / 2;
				break;
			case BOTTOM:
				base = 0;
				break;
			case TOP:
			default:
				base = logicalHeight;
				break;
		}

		// ABOVE_BAR has to clear the game's bar; the other styles either cover it
		// or deliberately sit where it falls.
		final int clearance = config.overheadStyle() == OverheadStyle.ABOVE_BAR ? BAR_CLEARANCE : 0;

		return base + clearance + config.overheadOffset();
	}
}
