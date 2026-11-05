package com.vinnyvangogh.combatvitals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
 * hitpoints and prayer: your target's health while fighting; XP to the next
 * level and inventory fill while skilling. Each says what it is with an
 * icon and a number at its top, and in full on hover. Placement is
 * {@link SidePillars}, painting {@link PillarPainter}, wording
 * {@link PillarText}; this finds the side panel and builds each pillar.
 */
class PillarOverlay extends Overlay
{
	/** XP marks every quarter. */
	private static final int XP_DIVISIONS = 4;

	private final Client client;
	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;
	private final SkillIconManager skillIconManager;
	private final TooltipManager tooltipManager;

	private final PillarAnimation combatFill = new PillarAnimation();
	private final PillarAnimation xpFill = new PillarAnimation();
	private final PillarAnimation inventoryFill = new PillarAnimation();
	private Skill lastSkill;

	@Inject
	PillarOverlay(Client client, CombatVitalsPlugin plugin, CombatVitalsConfig config,
		SkillIconManager skillIconManager, TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.skillIconManager = skillIconManager;
		this.tooltipManager = tooltipManager;

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
		final SidePillars.Layout pillars = target == null && skilling == null ? null : layout();
		if (pillars == null)
		{
			resetFills();
			return null;
		}

		final long now = System.currentTimeMillis();
		if (target != null)
		{
			xpFill.reset();
			inventoryFill.reset();
			draw(graphics, pillars.get(config.pillarCombatSide()), targetPillar(target), combatFill, now);
		}
		else
		{
			combatFill.reset();
			drawSkilling(graphics, pillars, skilling, now);
		}
		return null;
	}

	/** XP on its side, inventory on the other. */
	private void drawSkilling(Graphics2D graphics, SidePillars.Layout pillars, SkillingReadout skilling, long now)
	{
		if (skilling.getSkill() != lastSkill)
		{
			// Another skill's XP is not a change to glide through.
			lastSkill = skilling.getSkill();
			xpFill.reset();
		}
		final PillarSide xpSide = config.pillarXpSide();
		draw(graphics, pillars.get(xpSide), xpPillar(skilling), xpFill, now);
		final Pillar inventory = inventoryPillar(skilling);
		if (inventory != null)
		{
			draw(graphics, pillars.get(xpSide.other()), inventory, inventoryFill, now);
		}
	}

	private void resetFills()
	{
		combatFill.reset();
		xpFill.reset();
		inventoryFill.reset();
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

	/** Green to red as it fills, one mark per slot; null with the inventory line off. */
	private Pillar inventoryPillar(SkillingReadout skilling)
	{
		final double fill = skilling.getInventoryFill();
		if (fill < 0)
		{
			return null;
		}
		final InventoryCount count = config.skillingInventoryCount();
		return Pillar.builder()
			.fraction(fill)
			.colour(InventoryFill.colour(fill, config.healthFullColour(), config.healthMidColour(),
				config.healthLowColour()))
			.icon(PillarIcons.backpack())
			.number(PillarText.inventoryNumber(skilling.getInventoryFree(), count))
			.divisions(InventoryFill.SLOTS)
			.idle(skilling.isIdle())
			.tooltip(PillarText.inventoryTooltip(skilling.getInventoryFree(), count))
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
