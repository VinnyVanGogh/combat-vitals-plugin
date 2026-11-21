package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

/**
 * The target card in the corner: the same rounded card with an accent bar as
 * the combat card, replacing the stock-style box. The target's name and
 * combat level, its health as text and a bar, then for an NPC its wiki stats
 * (when the bundled table has them) and for a player the stat comparison. It
 * reads the tick snapshot and does no work of its own beyond laying out rows.
 */
class CombatVitalsPanelOverlay extends Overlay
{
	/** The same seven the base client compares, in the same order. */
	private static final HiscoreSkill[] COMBAT_SKILLS = {
		HiscoreSkill.ATTACK,
		HiscoreSkill.STRENGTH,
		HiscoreSkill.DEFENCE,
		HiscoreSkill.HITPOINTS,
		HiscoreSkill.RANGED,
		HiscoreSkill.MAGIC,
		HiscoreSkill.PRAYER,
	};

	private static final Skill[] OWN_SKILLS = {
		Skill.ATTACK,
		Skill.STRENGTH,
		Skill.DEFENCE,
		Skill.HITPOINTS,
		Skill.RANGED,
		Skill.MAGIC,
		Skill.PRAYER,
	};

	private static final Color AHEAD = new Color(0, 200, 83);
	private static final Color BEHIND = new Color(220, 60, 60);

	private final CombatVitalsPlugin plugin;
	private final CombatVitalsConfig config;
	private final Client client;
	private Map<Integer, NpcStats> stats;

	@Inject
	CombatVitalsPanelOverlay(CombatVitalsPlugin plugin, CombatVitalsConfig config, Client client)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.client = client;

		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_HIGH);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, PluginName.NAME + " overlay");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showPanel())
		{
			return null;
		}

		// Two panels stacked in the same corner reads as a broken plugin, so
		// stand down rather than draw over the stock one.
		if (config.hideWhenStockEnabled() && plugin.stockPluginEnabled())
		{
			return null;
		}

		final CombatVitalsPlugin.Readout readout = plugin.getReadout();
		if (readout == null)
		{
			return null;
		}

		final String text = readout.text(config.percentageDecimals());
		if (text == null)
		{
			return null;
		}

		final double fraction = HealthReadout.fraction(readout.getRatio(), readout.getScale(),
			readout.getExactHealth(), readout.getMaxHealth());
		final Color colour = config.colourGradient()
			? HealthReadout.colour(fraction, config.healthFullColour(), config.healthMidColour(), config.healthLowColour())
			: SplashOverlay.SAFE;

		final List<SplashOverlay.Row> rows = new ArrayList<>();
		final NpcStats npc = config.showNpcStats() ? statsFor(readout.getActor()) : null;
		rows.addAll(rows(readout.getName(), npc != null && npc.combatLevel > 0 ? "Level " + npc.combatLevel : null,
			text, fraction, colour, npc));
		addStatComparison(rows, readout);

		return SplashOverlay.card(graphics, rows, colour);
	}

	/**
	 * The card's rows, top to bottom. Pure: everything it needs is passed in,
	 * so the layout is tested without a client.
	 *
	 * @param tag      the mode label at the header's right (the combat level), or null
	 * @param fraction health from 0 to 1, or negative when unknown (no bar)
	 * @param npc      the target's wiki stats, or null
	 */
	static List<SplashOverlay.Row> rows(String name, String tag, String text, double fraction, Color colour,
		NpcStats npc)
	{
		final List<SplashOverlay.Row> rows = new ArrayList<>();
		rows.add(SplashOverlay.Row.header(name, tag, colour));
		rows.add(SplashOverlay.Row.text(text, colour, true));
		if (fraction >= 0)
		{
			rows.add(SplashOverlay.Row.bar(Math.min(1, fraction), colour));
		}
		if (npc != null)
		{
			rows.add(SplashOverlay.Row.divider());
			addNpcRows(rows, npc);
		}
		return rows;
	}

	private static void addNpcRows(List<SplashOverlay.Row> rows, NpcStats s)
	{
		if (s.maxHit != null)
		{
			rows.add(SplashOverlay.Row.pair("Max hit", s.maxHit, SplashOverlay.PLAIN));
		}
		final String attack = s.style == null ? s.speedText()
			: s.speedText() == null ? s.style : s.style + ", " + s.speedText();
		if (attack != null)
		{
			rows.add(SplashOverlay.Row.pair("Attack", attack, SplashOverlay.PLAIN));
		}
		if (s.stab != -1 || s.slash != -1 || s.crush != -1)
		{
			rows.add(SplashOverlay.Row.pair("Defence", "Stab " + bonus(s.stab) + "  Slash " + bonus(s.slash)
				+ "  Crush " + bonus(s.crush), SplashOverlay.PLAIN));
		}
		if (s.magic != -1 || s.ranged != -1)
		{
			rows.add(SplashOverlay.Row.pair("", "Magic " + bonus(s.magic) + "  Ranged " + bonus(s.ranged),
				SplashOverlay.PLAIN));
		}
		if (s.weakness != null)
		{
			rows.add(SplashOverlay.Row.pair("Weakness", s.weakness, SplashOverlay.PLAIN));
		}
		final String immune = immunity(s);
		if (immune != null)
		{
			rows.add(SplashOverlay.Row.pair("Immune to", immune, SplashOverlay.WARN));
		}
	}

	private static String bonus(int value)
	{
		return value == -1 ? "?" : String.valueOf(value);
	}

	/** "Poison, venom", "Poison" or "Venom", or null when it is neither. */
	static String immunity(NpcStats s)
	{
		if (s.poisonImmune && s.venomImmune)
		{
			return "Poison, venom";
		}
		return s.poisonImmune ? "Poison" : s.venomImmune ? "Venom" : null;
	}

	private NpcStats statsFor(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return null;
		}
		if (stats == null)
		{
			stats = NpcStats.load();
		}
		return stats.get(((NPC) actor).getId());
	}

	/**
	 * Your combat stats against theirs, from the hiscores result already fetched
	 * for the hitpoints lookup. No second request: the same cache entry answers
	 * both, which is why this costs nothing beyond the panel space.
	 */
	private void addStatComparison(List<SplashOverlay.Row> rows, CombatVitalsPlugin.Readout readout)
	{
		if (!config.showStatComparison())
		{
			return;
		}

		final HiscoreResult hiscore = readout.getHiscore();
		if (hiscore == null)
		{
			return;
		}

		rows.add(SplashOverlay.Row.divider());
		rows.add(SplashOverlay.Row.pair("Skill", "You / Them", SplashOverlay.PLAIN));

		for (int i = 0; i < COMBAT_SKILLS.length; i++)
		{
			final net.runelite.client.hiscore.Skill theirs = hiscore.getSkill(COMBAT_SKILLS[i]);

			// Unranked in a skill comes back non-positive. Skipping the row is
			// honest; showing a zero would read as a level.
			if (theirs == null || theirs.getLevel() <= 0)
			{
				continue;
			}

			final int mine = client.getRealSkillLevel(OWN_SKILLS[i]);
			final int level = theirs.getLevel();

			rows.add(SplashOverlay.Row.pair(COMBAT_SKILLS[i].getName(), mine + " / " + level,
				mine == level ? SplashOverlay.PLAIN : (mine > level ? AHEAD : BEHIND)));
		}
	}
}
