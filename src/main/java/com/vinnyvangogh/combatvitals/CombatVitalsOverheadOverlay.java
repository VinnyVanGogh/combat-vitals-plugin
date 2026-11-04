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

	/** Space between the badge's rows: the head, the XP line and the inventory line. */
	private static final int ROW_GAP = 1;

	/** Fish the player's level does not land yet are dimmed, with the level they need over them. */
	private static final float LOCKED_FISH_ALPHA = 0.35f;

	/** Height of a catch icon in the inventory line; item sprites are 32 high, so about half. */
	private static final int FISH_ICON = 18;

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
	 * Records the actor as covered, chat or not, so a message posted next is
	 * taken off the game the moment it arrives, and draws the line the plugin
	 * is holding for it above the box.
	 *
	 * @param box what this overlay drew over the actor this frame, or null
	 *            when nothing was
	 */
	private void liftChat(Graphics2D graphics, Actor actor, Rectangle box, Set<Actor> covered)
	{
		if (actor != null && box != null)
		{
			covered.add(actor);
			drawHeldChat(graphics, actor, box);
		}
	}

	/**
	 * The skilling badge, over your own head at the same anchor as your
	 * health bar, in three rows of one width:
	 * <ol>
	 * <li>the head, in the skill's colour ({@link SkillColours}): the skill's
	 * icon, level and boost. While boosted the bar drains with the boost
	 * timer; a drop or expiry paints it in the low colour for a moment;</li>
	 * <li>the XP line: progress to the next level, as a bar and as text;</li>
	 * <li>the inventory line ({@link #drawInventoryLine}): free slots on the
	 * fill bar, and on a fishing spot every fish the method can give.</li>
	 * </ol>
	 * The rows grow upward from where the badge always sat, so its bottom
	 * stays clear of your health bar.
	 *
	 * The badge covers your own chat line the way the health bar does; the
	 * caller lifts the line above the whole stack ({@link #drawHeldChat}),
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
		final BufferedImage icon = skillIconManager.getSkillImage(readout.getSkill(), true);
		final int iconWidth = icon == null ? 0 : icon.getWidth() + ICON_GAP;
		final int headHeight = Math.max(metrics.getAscent() + metrics.getDescent(),
			icon == null ? 0 : icon.getHeight()) + PAD_Y * 2;

		// The XP and inventory lines use the small font, so the badge grows
		// by two short rows rather than two full ones.
		final Font smallFont = FontManager.getRunescapeSmallFont();
		final FontMetrics small = graphics.getFontMetrics(smallFont);
		final int lineHeight = small.getAscent() + small.getDescent() + PAD_Y * 2;
		final String invLabel = InventoryFill.label(readout.getInventoryFree());
		final int invWidth = (invLabel == null ? 0 : small.stringWidth(invLabel))
			+ catchesWidth(readout.getCatches(), small);

		// One width for all three rows, centred where the game centres the
		// head's text, which is over the player.
		final int width = PAD_X * 2 + Math.max(iconWidth + textWidth,
			Math.max(small.stringWidth(readout.getXp().text()), invWidth));
		final int left = centred.getX() + textWidth / 2 - width / 2;

		// The bottom stays where the badge's own bottom always sat, clear of
		// the health bar below; the rows stack upward from there.
		final int bottom = centred.getY() + metrics.getDescent() + PAD_Y - OverheadStack.liftAbove(below);
		final Rectangle inv = inventoryRow(readout, left, bottom, width, lineHeight);
		final Rectangle xp = new Rectangle(left, inv.y - ROW_GAP - lineHeight, width, lineHeight);
		final Rectangle head = new Rectangle(left, xp.y - ROW_GAP - headHeight, width, headHeight);

		drawBadgeHead(graphics, head, readout, text, icon);

		final Font previous = graphics.getFont();
		graphics.setFont(smallFont);
		try
		{
			drawXpLine(graphics, xp, readout);
			if (inv.height > 0)
			{
				drawInventoryLine(graphics, inv, readout, invLabel);
			}
		}
		finally
		{
			graphics.setFont(previous);
		}

		return new Rectangle(left, head.y, width, bottom - head.y);
	}

	/**
	 * The inventory line's box at the bottom of the badge, tall enough for
	 * the catch icons when there are any. With nothing to show it has no
	 * height and sits just below the bottom, so the XP line takes its place.
	 */
	private static Rectangle inventoryRow(SkillingReadout readout, int left, int bottom, int width, int lineHeight)
	{
		if (!readout.hasInventoryLine())
		{
			return new Rectangle(left, bottom + ROW_GAP, width, 0);
		}
		final int height = readout.getCatches().isEmpty() ? lineHeight : Math.max(lineHeight, FISH_ICON + 2);
		return new Rectangle(left, bottom - height, width, height);
	}

	/**
	 * The badge's top row in the skill's colour: filled behind white text in
	 * the Bar style, the text itself in the colour otherwise. The fill is the
	 * boost time left while boosted, and full in the low colour for a moment
	 * after a drop or expiry.
	 */
	private void drawBadgeHead(Graphics2D graphics, Rectangle head, SkillingReadout readout, String text,
		BufferedImage icon)
	{
		final boolean warning = readout.getWarning() != SkillingState.Warning.NONE;
		final Color colour = warning ? config.healthLowColour() : SkillColours.of(readout.getSkill());
		final boolean bar = config.overheadStyle() == OverheadStyle.BAR;
		if (bar)
		{
			final double fraction = readout.isBoosted() && readout.getTicksUntilDrop() >= 0
				&& readout.getCycleLength() > 0
				? (double) readout.getTicksUntilDrop() / readout.getCycleLength()
				: 1.0;
			fillBar(graphics, head.x, head.y, head.width, head.height, warning ? 1.0 : fraction, colour);
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int iconWidth = icon == null ? 0 : icon.getWidth() + ICON_GAP;
		final int x = head.x + (head.width - iconWidth - metrics.stringWidth(text)) / 2;
		if (icon != null)
		{
			graphics.drawImage(icon, x, head.y + (head.height - icon.getHeight()) / 2, null);
		}

		final int baseline = head.y + (head.height - metrics.getAscent() - metrics.getDescent()) / 2
			+ metrics.getAscent();
		OverlayUtil.renderTextLocation(graphics, new Point(x + iconWidth, baseline), text,
			bar ? Color.WHITE : colour);
	}

	/** Progress to the next level, filled in the skill's colour, in every style. */
	private void drawXpLine(Graphics2D graphics, Rectangle row, SkillingReadout readout)
	{
		final XpProgress xp = readout.getXp();
		fillBar(graphics, row.x, row.y, row.width, row.height, xp.getFraction(), SkillColours.of(readout.getSkill()));

		final String text = xp.text();
		final FontMetrics metrics = graphics.getFontMetrics();
		OverlayUtil.renderTextLocation(graphics,
			new Point(row.x + (row.width - metrics.stringWidth(text)) / 2, textBaseline(row, metrics)),
			text, Color.WHITE);
	}

	/**
	 * Free slots ("Inv 26/28 free") on a bar filled as the inventory fills,
	 * green while empty to red when full: the health colours run backwards.
	 * Then every fish the spot's method can give: the ones your level lands
	 * at full strength, the rest dimmed with the level they need. Either half
	 * can be off; the line shows whichever is on.
	 *
	 * @param label the free-slot label, or null when the fill is off
	 */
	private void drawInventoryLine(Graphics2D graphics, Rectangle row, SkillingReadout readout, String label)
	{
		final double fill = label == null ? 0 : readout.getInventoryFill();
		fillBar(graphics, row.x, row.y, row.width, row.height, fill,
			InventoryFill.colour(fill, config.healthFullColour(), config.healthMidColour(), config.healthLowColour()));

		final FontMetrics metrics = graphics.getFontMetrics();
		final int baseline = textBaseline(row, metrics);
		int x = row.x + PAD_X;
		if (label != null)
		{
			OverlayUtil.renderTextLocation(graphics, new Point(x, baseline), label, Color.WHITE);
			x += metrics.stringWidth(label);
		}

		final int iconTop = row.y + (row.height - FISH_ICON) / 2;
		for (FishingCatches.Catch c : readout.getCatches())
		{
			x += ICON_GAP;
			x += drawCatch(graphics, c, x, iconTop, baseline, metrics);
		}
	}

	/** @return the width the catch took */
	private int drawCatch(Graphics2D graphics, FishingCatches.Catch c, int x, int top, int baseline,
		FontMetrics metrics)
	{
		final BufferedImage image = itemManager.getImage(c.getFish().getItemId());
		final int width = catchWidth(c, image, metrics);
		final int iconWidth = scaledWidth(image);
		final int iconX = x + (width - iconWidth) / 2;
		if (c.isCatchable())
		{
			graphics.drawImage(image, iconX, top, iconWidth, FISH_ICON, null);
			return width;
		}

		final Composite composite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LOCKED_FISH_ALPHA));
		graphics.drawImage(image, iconX, top, iconWidth, FISH_ICON, null);
		graphics.setComposite(composite);

		final String level = String.valueOf(c.getFish().getLevel());
		OverlayUtil.renderTextLocation(graphics,
			new Point(x + (width - metrics.stringWidth(level)) / 2, baseline), level, Color.WHITE);
		return width;
	}

	/** Every catch icon with the gap before it, as {@link #drawInventoryLine} lays them out. */
	private int catchesWidth(List<FishingCatches.Catch> catches, FontMetrics metrics)
	{
		int width = 0;
		for (FishingCatches.Catch c : catches)
		{
			width += ICON_GAP + catchWidth(c, itemManager.getImage(c.getFish().getItemId()), metrics);
		}
		return width;
	}

	/** The icon, or the level written over a locked one if that is wider. */
	private static int catchWidth(FishingCatches.Catch c, BufferedImage image, FontMetrics metrics)
	{
		final int icon = scaledWidth(image);
		return c.isCatchable() ? icon : Math.max(icon, metrics.stringWidth(String.valueOf(c.getFish().getLevel())));
	}

	/** An item icon's width at {@link #FISH_ICON} high, keeping its shape. */
	private static int scaledWidth(BufferedImage image)
	{
		return image == null || image.getHeight() <= 0 ? FISH_ICON : image.getWidth() * FISH_ICON / image.getHeight();
	}

	/** Text vertically centred in a row. */
	private static int textBaseline(Rectangle row, FontMetrics metrics)
	{
		return row.y + (row.height - metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getAscent();
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
	 * Draws the chat line the plugin has taken off the game for this actor
	 * ("Hide game chat under readout"): just above the box this overlay drew
	 * over it (the readout, or your own bar-and-badge stack) when that box
	 * covers where the game would draw it, otherwise in the game's own place.
	 * The game draws chat at the actor's logical height, right where the
	 * readout sits; its text cannot be moved, so the readout keeps its place
	 * and the chat is drawn here instead.
	 *
	 * Only a held line is drawn. With the option off nothing is held, the
	 * game draws its own line and no copy is added, so a message never shows
	 * twice (Board, 2026-11-04).
	 *
	 * Measured in screen space, so it holds at any zoom, position and model
	 * height.
	 *
	 * @param readout the box this overlay drew over the actor
	 */
	private void drawHeldChat(Graphics2D graphics, Actor actor, Rectangle readout)
	{
		final String chat = plugin.heldChat(actor);
		if (chat == null)
		{
			return;
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
				return;
			}

			final FontMetrics metrics = graphics.getFontMetrics();
			final Rectangle line = new Rectangle(chatAt.getX(), chatAt.getY() - metrics.getAscent(),
				metrics.stringWidth(chat), metrics.getAscent() + metrics.getDescent());
			final int baseline = OverheadChat.baseline(readout, line, metrics.getDescent());

			// Centred on the actor like the game's line, in its default yellow
			// with the black shadow renderTextLocation adds. The API does not
			// expose the chat's colour or effect, so those are not reproduced.
			OverlayUtil.renderTextLocation(graphics, new Point(chatAt.getX(), baseline), chat, CHAT_COLOUR);
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
