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
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the readout over the target itself, which is the thing the stock
 * plugin cannot do, and the skilling badge over your head or the spot.
 */
class CombatVitalsOverheadOverlay extends Overlay
{
	/** Space between the text and the edge of the bar behind it. */
	private static final int PAD_X = SkillingBadgeRenderer.PAD_X;
	private static final int PAD_Y = SkillingBadgeRenderer.PAD_Y;

	/** The game's default overhead chat colour. */
	private static final Color CHAT_COLOUR = Color.YELLOW;

	/**
	 * Height cleared when lifting the text above the game's own health bar.
	 * The bar's position is not exposed by the API, so this is a fixed clearance
	 * that puts the text above it; the offset setting fine-tunes from there.
	 */
	private static final int BAR_CLEARANCE = 34;

	/** Height above a tree, rock or stall's base the badge sits at with "Over the spot". */
	private static final int OBJECT_HEIGHT = 200;

	/** Local units from a tile's centre to the next tile's: an object's centre is half its extra size on. */
	private static final int HALF_TILE = Perspective.LOCAL_TILE_SIZE / 2;

	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;
	private final SkillingBadgeRenderer badge;

	@Inject
	CombatVitalsOverheadOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config,
		SkillingBadgeRenderer badge)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.badge = badge;

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

			// Over your head the badge shares the anchor with your own bar and
			// stacks above it when both are up (hit while fishing). Over the
			// spot it covers nothing of yours.
			final Rectangle overHead = drawSkilling(graphics, plugin.getSkillingReadout(), selfBar);

			// Your own overhead chat goes above whichever of the two is topmost,
			// once: the bar and the badge are one stack over one actor, so one
			// copy of the line, lifted clear of both, is all that is wanted.
			liftChat(graphics, client.getLocalPlayer(), OverheadStack.union(selfBar, overHead), covered);
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
	 * The skilling badge ({@link SkillingBadgeRenderer}) over the spot when
	 * that is asked for and the spot is on screen, otherwise over your head
	 * at the same anchor as your health bar, growing upward so its bottom
	 * stays clear of the bar. Fixed on screen is {@link SkillingBadgeOverlay}'s.
	 *
	 * @param below your own health bar's box when it is drawn this frame
	 * @return the badge's box when it was drawn over your head, else null
	 */
	private Rectangle drawSkilling(Graphics2D graphics, SkillingReadout readout, Rectangle below)
	{
		if (readout == null || !config.skillingEnabled() || config.skillingPosition() == BadgePosition.FIXED)
		{
			return null;
		}

		final Point spot = config.skillingPosition() == BadgePosition.OVER_SPOT ? spotAnchor(graphics, readout) : null;
		if (spot != null)
		{
			badge.drawAt(graphics, readout, spot.getX(), spot.getY() - config.skillingOffset());
			return null;
		}

		final Player self = client.getLocalPlayer();
		final Point head = self == null ? null : self.getCanvasTextLocation(graphics, "", zOffset(self));
		if (head == null)
		{
			return null;
		}

		// The bottom sits where the badge's own bottom always sat, clear of
		// the health bar below; the rows stack upward from there.
		final int bottom = head.getY() + graphics.getFontMetrics().getDescent() + PAD_Y
			- OverheadStack.liftAbove(below) - config.skillingOffset();
		return badge.drawAt(graphics, readout, head.getX(), bottom);
	}

	/** Above the fishing spot, or the middle of the tree or rock; null when neither is known or on screen. */
	private Point spotAnchor(Graphics2D graphics, SkillingReadout readout)
	{
		final NPC npc = readout.getSpotNpc();
		if (npc != null)
		{
			return npc.getCanvasTextLocation(graphics, "", zOffset(npc));
		}

		final WorldPoint tile = readout.getSpotTile();
		final WorldView view = client.getTopLevelWorldView();
		final LocalPoint corner = tile == null || view == null ? null : LocalPoint.fromWorld(view, tile);
		if (corner == null)
		{
			return null;
		}

		final int extra = (Math.max(1, readout.getSpotSize()) - 1) * HALF_TILE;
		return Perspective.getCanvasTextLocation(client, graphics, corner.plus(extra, extra), "", OBJECT_HEIGHT);
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
		final Rectangle box = new Rectangle(location.getX() - PAD_X, location.getY() - metrics.getAscent() - PAD_Y,
			metrics.stringWidth(text) + PAD_X * 2, metrics.getAscent() + metrics.getDescent() + PAD_Y * 2);

		// Opaque background by default: a translucent one lets the game's own
		// health bar show through and the two read as one smeared bar.
		Bars.across(graphics, box, fraction,
			config.colourGradient() ? gradient(fraction) : config.healthFullColour(), config.barBackground());
		return box;
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
