package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * Vitals on the side panel's pillars, where RuneLite's Status Bars draws
 * hitpoints and prayer. By default your target's health while fighting; XP to
 * the next level and inventory fill while skilling. Each pillar can instead
 * show another {@link PillarSource}, or be split in two ({@link PillarSplit})
 * with a source for each part. Each says what it is with an icon and a
 * number at its top, and in full on hover. Placement is
 * {@link SidePillars}, painting {@link PillarPainter}, wording
 * {@link PillarText} and {@link PillarValues}; this finds the side panel and
 * builds each pillar.
 */
class PillarOverlay extends Overlay
{
	/** XP marks every quarter. */
	private static final int XP_DIVISIONS = 4;

	/** Two sides, two parts each. */
	private static final int SLOTS = 4;

	private static final String KIND_COMBAT = "combat";
	private static final String KIND_INVENTORY = "inventory";

	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;
	private final SkillIconManager skillIconManager;
	private final TooltipManager tooltipManager;
	private final PillarController controller;

	private final PillarAnimation[] fills = new PillarAnimation[SLOTS];
	private final String[] kinds = new String[SLOTS];
	private Skill lastSkill;

	@Inject
	PillarOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config,
		SkillIconManager skillIconManager, TooltipManager tooltipManager, PillarController controller)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.skillIconManager = skillIconManager;
		this.tooltipManager = tooltipManager;
		this.controller = controller;
		for (int i = 0; i < SLOTS; i++)
		{
			fills[i] = new PillarAnimation();
		}

		// As Status Bars: drawn over the interface, positioned by hand.
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final PillarMode mode = VitalsHalves.pillarMode(config);
		final CombatVitalsPlugin.Readout target = mode.combat() ? plugin.getReadout() : null;
		final SkillingReadout skilling = target == null && mode.skilling() && VitalsHalves.skillingBadge(config)
			? plugin.getSkillingReadout() : null;
		final PillarSetup left = controller.setup(PillarSide.LEFT);
		final PillarSetup right = controller.setup(PillarSide.RIGHT);
		final boolean defaults = target != null || skilling != null;
		final SidePillars.Layout pillars = defaults || left.anyCustom() || right.anyCustom() ? layout() : null;
		if (pillars == null)
		{
			resetFills();
			return null;
		}

		final long now = System.currentTimeMillis();
		final Map<PillarSide, Pillar> legacy = new EnumMap<>(PillarSide.class);
		final Map<PillarSide, String> legacyKind = new EnumMap<>(PillarSide.class);
		if (target != null)
		{
			legacy.put(config.pillarCombatSide(), targetPillar(target));
			legacyKind.put(config.pillarCombatSide(), KIND_COMBAT);
		}
		else if (skilling != null)
		{
			skillingPillars(skilling, legacy, legacyKind);
		}

		final PillarValues.Settings settings = controller.settings();
		final PillarSnapshot snapshot = controller.snapshot();
		drawSide(graphics, PillarSide.LEFT, left, pillars.get(PillarSide.LEFT), legacy, legacyKind, snapshot, settings,
			now);
		drawSide(graphics, PillarSide.RIGHT, right, pillars.get(PillarSide.RIGHT), legacy, legacyKind, snapshot,
			settings, now);
		return null;
	}

	/** One side: whole, or its two parts, each with its own source and its own gliding fill. */
	private void drawSide(Graphics2D graphics, PillarSide side, PillarSetup setup, Rectangle box,
		Map<PillarSide, Pillar> legacy, Map<PillarSide, String> legacyKind, PillarSnapshot snapshot,
		PillarValues.Settings settings, long now)
	{
		final Rectangle[] parts = setup.getSplit().split(box);
		for (int i = 0; i < parts.length; i++)
		{
			final int slot = side.ordinal() * 2 + i;
			final PillarSource source = setup.source(i);
			final Pillar pillar;
			final String kind;
			if (source == PillarSource.DEFAULT)
			{
				pillar = legacy.get(side);
				kind = legacyKind.get(side);
			}
			else if (source == PillarSource.NONE)
			{
				pillar = null;
				kind = null;
			}
			else
			{
				final Pillar built = PillarValues.of(source, snapshot, settings);
				pillar = built != null ? built : emptyTrack(source);
				kind = "source:" + source.name();
			}
			if (pillar == null)
			{
				fills[slot].reset();
				kinds[slot] = null;
				continue;
			}
			if (!kind.equals(kinds[slot]))
			{
				// A different thing is not a change to glide through.
				fills[slot].reset();
				kinds[slot] = kind;
			}
			draw(graphics, parts[i], pillar, fills[slot], now);
		}
	}

	/** A source with nothing to read yet: an empty track, so the pillar keeps its place. */
	private Pillar emptyTrack(PillarSource source)
	{
		return Pillar.builder().fraction(0).colour(source.getDefaultColour()).bottom(source.getTag())
			.tooltip(source.getName() + ": nothing to show yet").build();
	}

	/** XP on its side, inventory on the other. */
	private void skillingPillars(SkillingReadout skilling, Map<PillarSide, Pillar> legacy,
		Map<PillarSide, String> legacyKind)
	{
		if (skilling.getSkill() != lastSkill)
		{
			lastSkill = skilling.getSkill();
			for (PillarAnimation fill : fills)
			{
				fill.reset();
			}
		}
		final PillarSide xpSide = config.pillarXpSide();
		legacy.put(xpSide, xpPillar(skilling));
		legacyKind.put(xpSide, "xp");
		final Pillar inventory = InventoryPillar.of(skilling, config, PillarIcons.backpack());
		if (inventory != null)
		{
			legacy.put(xpSide.other(), inventory);
			legacyKind.put(xpSide.other(), KIND_INVENTORY);
		}
	}

	private void resetFills()
	{
		for (int i = 0; i < SLOTS; i++)
		{
			fills[i].reset();
			kinds[i] = null;
		}
	}

	/** Paints the pillar at its gliding fill, and its hover text while the mouse is over it. */
	private void draw(Graphics2D graphics, Rectangle box, Pillar pillar, PillarAnimation fill, long now)
	{
		final Pillar shown = pillar.toBuilder().fraction(fill.next(pillar.getFraction(), now)).build();
		PillarPainter.paint(graphics, box, shown, config.pillarStyle(), config.pillarLabels(), config.barBackground());

		final Point mouse = client.getMouseCanvasPosition();
		if (pillar.getTooltip() != null && mouse != null && box.contains(mouse.getX(), mouse.getY()))
		{
			tooltipManager.add(new Tooltip(pillar.getTooltip()));
		}
	}

	private Pillar targetPillar(CombatVitalsPlugin.Readout target)
	{
		final double fraction = HealthReadout.fraction(target.getRatio(), target.getScale(),
			target.getExactHealth(), target.getMaxHealth());
		final int hp = PillarText.targetHp(target.getExactHealth(), target.getRange(), target.getMaxHealth());
		return Pillar.builder()
			.fraction(fraction)
			.colour(HealthReadout.colour(fraction, config.healthFullColour(), config.healthMidColour(),
				config.healthLowColour()))
			.icon(skillIconManager.getSkillImage(Skill.HITPOINTS, true))
			.number(PillarText.targetNumber(hp, fraction))
			.tooltip(PillarText.targetTooltip(target.getName(), hp, target.getMaxHealth(), fraction))
			.build();
	}

	private Pillar xpPillar(SkillingReadout skilling)
	{
		final XpProgress xp = skilling.getXp();
		return Pillar.builder()
			.fraction(xp.getFraction())
			.colour(skilling.getColour())
			.icon(skillIconManager.getSkillImage(skilling.getSkill(), true))
			.number(PillarText.xpNumber(xp, config.skillingXpDisplay()))
			.bottom(PillarText.xpBottom(xp))
			.divisions(XP_DIVISIONS)
			.idle(skilling.isIdle())
			.tooltip(PillarText.xpTooltip(skilling.getSkill().getName(), xp))
			.build();
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
