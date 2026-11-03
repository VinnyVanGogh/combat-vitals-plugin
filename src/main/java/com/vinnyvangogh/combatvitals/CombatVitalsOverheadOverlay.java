package com.vinnyvangogh.combatvitals;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
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

	/** Space between the skill icon and the badge text. */
	private static final int ICON_GAP = 3;

	/** Fish icons drawn on the badge before the count takes over. */
	static final int MAX_FISH_ICONS = 3;

	/** The inventory fill bar under the skilling badge, and the space above it. */
	private static final int FILL_BAR_HEIGHT = 4;
	private static final int FILL_BAR_GAP = 1;

	/** Fish that are not the last catch are faded, so the current one reads as current. */
	private static final float OTHER_FISH_ALPHA = 0.55f;

	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;
	private final SkillIconManager skillIconManager;
	private final ItemManager itemManager;

	@Inject
	CombatVitalsOverheadOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config,
		SkillIconManager skillIconManager, ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.skillIconManager = skillIconManager;
		this.itemManager = itemManager;

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
				liftChat(graphics, opponent.getActor(), draw(graphics, opponent), covered);
			}

			// Both sides in one visual language. Getting your own health from some
			// other plugin would render it in a different style beside your
			// opponent's, which is the main reason this belongs here.
			final Rectangle selfBar = draw(graphics, plugin.getSelfReadout());

			// The badge shares the anchor with your own bar and stacks above it
			// when both are up (hit while fishing).
			final Rectangle badge = config.skillingEnabled()
				? drawSkilling(graphics, plugin.getSkillingReadout(), selfBar)
				: null;

			// Your own overhead chat goes above whichever of the two is topmost,
			// once: the bar and the badge are one stack over one actor, so one
			// copy of the line, lifted clear of both, is all that is wanted.
			liftChat(graphics, client.getLocalPlayer(), OverheadStack.union(selfBar, badge), covered);
			return null;
		}
		finally
		{
			plugin.setChatCovered(covered);
		}
	}

	/**
	 * Redraws the actor's chat above the box drawn over it, and records the
	 * actor as covered so the plugin can take the game's line off it.
	 *
	 * @param box what this overlay drew over the actor this frame, or null
	 *            when nothing was
	 */
	private void liftChat(Graphics2D graphics, Actor actor, Rectangle box, Set<Actor> covered)
	{
		if (actor != null && box != null && drawChatAbove(graphics, actor, box))
		{
			covered.add(actor);
		}
	}

	/**
	 * The skilling badge, over your own head at the same anchor as your
	 * health bar: the health bar's style, with the skill's icon in place of a
	 * health number, and on a fishing spot the fish the clicked method can
	 * land at your level. While boosted the bar drains with the boost timer,
	 * so the colour ramp means "time left" rather than "health left". A drop
	 * or expiry paints the whole bar in the empty colour for a moment.
	 *
	 * The badge covers your own chat line the way the health bar does; the
	 * caller lifts the line above the whole stack ({@link #drawChatAbove}),
	 * so the badge and the chat never overlap.
	 *
	 * @param below your own health bar's box when it is drawn this frame, so
	 *              the badge can sit above it; null when it is not
	 * @return the badge's box, or null when nothing was drawn
	 */
	private Rectangle drawSkilling(Graphics2D graphics, SkillingReadout readout, Rectangle below)
	{
		if (readout == null)
		{
			return null;
		}

		// Always the local player. The spot being worked is not where the
		// badge goes: it moves, it is small, and it is not whose level this is.
		final Player self = client.getLocalPlayer();
		if (self == null)
		{
			return null;
		}

		final int seconds = SkillingText.seconds(readout.getTicksUntilDrop(),
			System.currentTimeMillis() - readout.getTickMillis());
		final String text = SkillingText.text(readout.getSkill().getName(), readout.getReal(),
			readout.getBoosted(), seconds, !readout.isTimerConfirmed(), readout.getSpecPercent(),
			readout.getSource(), readout.getWarning());

		final Point centred = self.getCanvasTextLocation(graphics, text, zOffset(self));
		if (centred == null)
		{
			return null;
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(text);
		final int textHeight = metrics.getAscent() + metrics.getDescent();

		final BufferedImage icon = skillIconManager.getSkillImage(readout.getSkill(), true);
		final int iconWidth = icon == null ? 0 : icon.getWidth() + ICON_GAP;
		final int iconHeight = icon == null ? 0 : icon.getHeight();

		// Fish icons are scaled to the text height so they read as part of
		// the line rather than sitting on it.
		final List<FishingCatches.Fish> fish = readout.getFish();
		final int shown = Math.min(fish.size(), MAX_FISH_ICONS);
		final int fishHeight = textHeight;
		final BufferedImage[] fishImages = new BufferedImage[shown];
		final int[] fishWidths = new int[shown];
		int fishWidth = 0;
		for (int i = 0; i < shown; i++)
		{
			fishImages[i] = itemManager.getImage(fish.get(i).getItemId());
			fishWidths[i] = fishImages[i].getHeight() <= 0
				? fishHeight
				: fishImages[i].getWidth() * fishHeight / fishImages[i].getHeight();
			fishWidth += ICON_GAP + fishWidths[i];
		}
		final String overflow = SkillingText.overflow(fish.size(), MAX_FISH_ICONS);
		final int overflowWidth = overflow == null ? 0 : ICON_GAP + metrics.stringWidth(overflow);

		// The game centres the text alone; centre the whole icon-text-fish
		// block on the same point, and lift it clear of the health bar below.
		final int contentWidth = iconWidth + textWidth + fishWidth + overflowWidth;
		final int left = centred.getX() + textWidth / 2 - contentWidth / 2;

		// The inventory bar hangs under the badge, so the badge rises by its
		// height to keep the pair clear of the health bar below.
		final boolean hasFill = readout.getInventoryFill() >= 0;
		final int fillLift = hasFill ?FILL_BAR_HEIGHT + FILL_BAR_GAP : 0;
		final int baseline = centred.getY() - OverheadStack.liftAbove(below) - fillLift;
		final Point location = new Point(left + iconWidth, baseline);

		final boolean warning = readout.getWarning() != SkillingState.Warning.NONE;
		final double fraction = readout.isBoosted() && readout.getTicksUntilDrop() >= 0
			&& readout.getCycleLength() > 0
			? (double) readout.getTicksUntilDrop() / readout.getCycleLength()
			: 1.0;
		final Color colour = warning
			? config.healthLowColour()
			: config.colourGradient() ? gradient(fraction) : config.healthFullColour();

		final int height = Math.max(textHeight, iconHeight) + PAD_Y * 2;
		final int top = baseline - metrics.getAscent() - PAD_Y - (height - textHeight - PAD_Y * 2) / 2;
		final Rectangle bounds = new Rectangle(left - PAD_X, top, contentWidth + PAD_X * 2, height);

		final boolean bar = config.overheadStyle() == OverheadStyle.BAR;
		if (bar)
		{
			fillBar(graphics, bounds.x, bounds.y, bounds.width, bounds.height, warning ? 1.0 : fraction, colour);
		}

		if (icon != null)
		{
			graphics.drawImage(icon, left, bounds.y + (height - iconHeight) / 2, null);
		}

		final Color textColour = bar || !(config.colourGradient() || warning) ? Color.WHITE : colour;
		OverlayUtil.renderTextLocation(graphics, location, text, textColour);

		int x = left + iconWidth + textWidth;
		final int fishTop = baseline - metrics.getAscent();
		final Composite composite = graphics.getComposite();
		for (int i = 0; i < shown; i++)
		{
			x += ICON_GAP;
			final boolean faded = readout.isLastCatchShown() && i > 0;
			if (faded)
			{
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, OTHER_FISH_ALPHA));
			}
			graphics.drawImage(fishImages[i], x, fishTop, fishWidths[i], fishHeight, null);
			if (faded)
			{
				graphics.setComposite(composite);
			}
			x += fishWidths[i];
		}
		if (overflow != null)
		{
			OverlayUtil.renderTextLocation(graphics, new Point(x + ICON_GAP, baseline), overflow, textColour);
		}

		if (!hasFill)
		{
			return bounds;
		}

		// Drawn in every style: it is the only place the fill shows. The
		// health colours run backwards, green while empty to red when full.
		final double fill = readout.getInventoryFill();
		final Rectangle fillBounds = new Rectangle(bounds.x, bounds.y + bounds.height + FILL_BAR_GAP,
			bounds.width, FILL_BAR_HEIGHT);
		fillBar(graphics, fillBounds.x, fillBounds.y, fillBounds.width, fillBounds.height, fill,
			InventoryFill.colour(fill, config.healthFullColour(), config.healthMidColour(),
				config.healthLowColour()));
		return OverheadStack.union(bounds, fillBounds);
	}

	/**
	 * @return the readout's box on screen (the bar's padding included for the
	 *         Bar style), or null when nothing was drawn
	 */
	private Rectangle draw(Graphics2D graphics, CombatVitalsPlugin.Readout readout)
	{
		if (readout == null)
		{
			return null;
		}

		final String text = HealthReadout.text(readout.getRange(), readout.getMaxHealth(),
			readout.getRatio(), readout.getScale(), config.displayMode(), config.percentageDecimals(),
			readout.getExactHealth());
		if (text == null)
		{
			return null;
		}

		final Actor actor = readout.getActor();
		final Point location = actor.getCanvasTextLocation(graphics, text, zOffset(actor));
		if (location == null)
		{
			return null;
		}

		final double fraction = HealthReadout.fraction(readout.getRatio(), readout.getScale(),
			readout.getExactHealth(), readout.getMaxHealth());

		if (config.overheadStyle() == OverheadStyle.BAR)
		{
			final Rectangle bounds = drawBar(graphics, location, text, fraction);

			// White on a filled bar. Colouring the text as well as the bar makes
			// the two fight each other — red text on a green bar is the worst
			// case and the reason this style exists.
			OverlayUtil.renderTextLocation(graphics, location, text, Color.WHITE);
			return bounds;
		}

		final Color colour = config.colourGradient() ? gradient(fraction) : Color.WHITE;
		OverlayUtil.renderTextLocation(graphics, location, text, colour);

		final FontMetrics metrics = graphics.getFontMetrics();
		return new Rectangle(location.getX(), location.getY() - metrics.getAscent(),
			metrics.stringWidth(text), metrics.getAscent() + metrics.getDescent());
	}

	/**
	 * A bar exactly as wide as the text it sits behind. The game's own bar is a
	 * fixed handful of pixels and the readout is always wider, so sizing to the
	 * text is what makes the two legible together instead of overlapping.
	 *
	 * @return the bar's box
	 */
	private Rectangle drawBar(Graphics2D graphics, Point location, String text, double fraction)
	{
		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(text);

		final int x = location.getX() - PAD_X;
		final int y = location.getY() - metrics.getAscent() - PAD_Y;
		final int width = textWidth + PAD_X * 2;
		final int height = metrics.getAscent() + metrics.getDescent() + PAD_Y * 2;

		fillBar(graphics, x, y, width, height, fraction,
			config.colourGradient() ? gradient(fraction) : config.healthFullColour());
		return new Rectangle(x, y, width, height);
	}

	/** Background, a fill proportional to {@code fraction} in {@code colour}, and a border. */
	private void fillBar(Graphics2D graphics, int x, int y, int width, int height, double fraction, Color colour)
	{
		// Opaque by default: a translucent background lets the game's own health
		// bar show through and the two read as one smeared bar.
		graphics.setColor(config.barBackground());
		graphics.fillRect(x, y, width, height);

		final int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
		if (filled > 0)
		{
			graphics.setColor(colour);
			graphics.fillRect(x, y, filled, height);
		}

		graphics.setColor(BAR_BORDER);
		graphics.drawRect(x, y, width, height);
	}

	/**
	 * Redraws the actor's overhead chat just above the box this overlay drew
	 * over it (the readout, or your own bar-and-badge stack) while that box
	 * covers the game's own line. The game draws chat at the actor's logical
	 * height, right where the readout sits, and this overlay paints over it;
	 * the game's text cannot be moved, so the readout keeps its place and the
	 * chat gets a readable copy on top.
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
	 * @param readout the box this overlay drew over the actor
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
