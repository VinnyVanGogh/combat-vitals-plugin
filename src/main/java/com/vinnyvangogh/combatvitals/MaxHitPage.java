package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * The Max Hit page of the sidebar: max hit, chance to hit and average damage
 * against an NPC, from {@link MaxHit}. Every box can be typed over, so "what
 * if I swap this weapon" is a new strength bonus, and "Use what I wear" fills
 * them from the game. NPC targets only. Swing thread only.
 */
final class MaxHitPage extends VitalsSidePanel.Column
{
	/** What the game says now, read on the client thread; -1 where a value is not known. */
	static final class Snapshot
	{
		int attack = 1;
		int strength = 1;
		int ranged = 1;
		int magic = 1;
		int attackBonus;
		int strengthBonus;
		int rangedAttack;
		int rangedStrength;
		int magicAttack;
		double magicDamagePercent;
		String target = "";
		int targetMagicLevel = -1;
		int targetMagicDefence = -1;
	}

	/** Reads the snapshot off the Swing thread and hands it back on it. */
	interface Source
	{
		void request(Consumer<Snapshot> done);
	}

	private static final String[] STYLES = {"Melee", "Ranged", "Magic"};

	/** name, attack multiplier, strength multiplier. */
	private static final Object[][][] PRAYERS = {
		{{"None", 1.0, 1.0}, {"Burst of Strength / Clarity of Thought", 1.05, 1.05}, {"Superhuman / Improved Reflexes", 1.10, 1.10},
			{"Ultimate Strength / Incredible Reflexes", 1.15, 1.15}, {"Chivalry", 1.15, 1.18}, {"Piety", 1.20, 1.23}},
		{{"None", 1.0, 1.0}, {"Sharp Eye", 1.05, 1.05}, {"Hawk Eye", 1.10, 1.10}, {"Eagle Eye", 1.15, 1.15},
			{"Rigour", 1.20, 1.23}},
		{{"None", 1.0, 1.0}, {"Mystic Will", 1.05, 1.0}, {"Mystic Lore", 1.10, 1.0}, {"Mystic Might", 1.15, 1.0},
			{"Augury", 1.25, 1.0}}};

	/** name, attack stance bonus, strength stance bonus. */
	private static final Object[][][] STANCES = {
		{{"Accurate", 3, 0}, {"Aggressive", 0, 3}, {"Controlled", 1, 1}, {"Defensive", 0, 0}},
		{{"Accurate", 3, 3}, {"Rapid", 0, 0}, {"Longrange", 0, 0}},
		{{"Spell", 0, 0}}};

	/** name, attack multiplier, strength multiplier, magic damage percent; per style. */
	private static final Object[][][] VOIDS = {
		{{"None", 1.0, 1.0, 0.0}, {"Void knight", 1.1, 1.1, 0.0}},
		{{"None", 1.0, 1.0, 0.0}, {"Void knight", 1.1, 1.1, 0.0}, {"Elite void", 1.1, 1.125, 0.0}},
		{{"None", 1.0, 1.0, 0.0}, {"Void knight", 1.45, 1.0, 0.0}, {"Elite void", 1.45, 1.0, 2.5}}};

	private final Source source;
	private final JComboBox<String> style = new JComboBox<>(STYLES);
	private final JComboBox<String> prayer = new JComboBox<>();
	private final JComboBox<String> stance = new JComboBox<>();
	private final JComboBox<String> voidSet = new JComboBox<>();
	private final JSpinner attackLevel = spinner(99, 1, 126);
	private final JSpinner strengthLevel = spinner(99, 1, 126);
	private final JSpinner attackBonus = spinner(0, -200, 400);
	private final JSpinner strengthBonus = spinner(0, -200, 400);
	private final JSpinner spellBase = spinner(30, 0, 100);
	private final JSpinner damagePercent = spinner(0, 0, 200);
	private final JSpinner otherPercent = spinner(100, 0, 500);
	private final JSpinner targetLevel = spinner(100, 1, 1500);
	private final JSpinner targetDefence = spinner(0, -200, 600);
	private final JSpinner speed = spinner(4, 1, 10);
	private final JLabel target = new JLabel(" ");
	private final JLabel result = new JLabel();
	private boolean loading;

	MaxHitPage(Source source)
	{
		this.source = source;
		add(VitalsSidePanel.text("Max hit and chance to hit against an NPC. Type over any box for a what-if "
			+ "(a different weapon is a different strength bonus). Special attacks and set effects are not "
			+ "modelled; use Other % for Slayer helm, Salve or similar.", PanelStyle.MUTED));

		final Card inputs = Card.of(PanelStyle.ACCENT);
		inputs.add(VitalsSidePanel.heading("Your setup"));
		final JButton use = new JButton("Use what I wear and my levels");
		use.setFont(PanelStyle.body());
		use.setFocusable(false);
		use.addActionListener(e -> source.request(this::fill));
		inputs.add(use);
		inputs.add(labelled("Style", style));
		inputs.add(labelled("Attack level", attackLevel));
		inputs.add(labelled("Strength / damage level", strengthLevel));
		inputs.add(labelled("Prayer", prayer));
		inputs.add(labelled("Stance", stance));
		inputs.add(labelled("Void", voidSet));
		inputs.add(labelled("Attack bonus", attackBonus));
		inputs.add(labelled("Strength bonus", strengthBonus));
		inputs.add(labelled("Spell max hit", spellBase));
		inputs.add(labelled("Magic damage %", damagePercent));
		inputs.add(labelled("Other multiplier %", otherPercent));
		inputs.add(labelled("Attack speed (ticks)", speed));
		add(inputs);

		final Card targetCard = Card.of(null);
		targetCard.add(VitalsSidePanel.heading("Target NPC"));
		target.setFont(PanelStyle.body());
		target.setForeground(PanelStyle.SOFT);
		targetCard.add(target);
		targetCard.add(labelled("Defence level", targetLevel));
		targetCard.add(labelled("Defence bonus", targetDefence));
		add(targetCard);

		final Card out = Card.of(PanelStyle.ACCENT);
		out.add(VitalsSidePanel.heading("Result"));
		result.setFont(PanelStyle.body());
		result.setForeground(PanelStyle.TEXT);
		out.add(result);
		add(out);

		style.addActionListener(e -> styleChanged());
		for (JComboBox<String> c : java.util.Arrays.asList(prayer, stance, voidSet))
		{
			c.addActionListener(e -> recompute());
		}
		for (JSpinner s : java.util.Arrays.asList(attackLevel, strengthLevel, attackBonus, strengthBonus,
			spellBase, damagePercent, otherPercent, targetLevel, targetDefence, speed))
		{
			s.addChangeListener(e -> recompute());
		}
		styleChanged();
	}

	private static JSpinner spinner(int value, int min, int max)
	{
		final JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
		s.setFont(PanelStyle.body());
		return s;
	}

	private static JComponent labelled(String text, JComponent control)
	{
		final JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		final JLabel label = new JLabel(text);
		label.setFont(PanelStyle.body());
		label.setForeground(PanelStyle.SOFT);
		label.setPreferredSize(new Dimension(110, 22));
		control.setFont(PanelStyle.body());
		row.add(label, BorderLayout.WEST);
		row.add(control, BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		return row;
	}

	private int styleIndex()
	{
		return Math.max(0, style.getSelectedIndex());
	}

	private void styleChanged()
	{
		final int s = styleIndex();
		loading = true;
		fillCombo(prayer, PRAYERS[s]);
		fillCombo(stance, STANCES[s]);
		fillCombo(voidSet, VOIDS[s]);
		loading = false;
		recompute();
	}

	private static void fillCombo(JComboBox<String> box, Object[][] rows)
	{
		box.removeAllItems();
		for (Object[] r : rows)
		{
			box.addItem((String) r[0]);
		}
	}

	/** Fills the boxes from what the game reports. */
	private void fill(Snapshot s)
	{
		loading = true;
		final int st = styleIndex();
		if (st == 0)
		{
			attackLevel.setValue(s.attack);
			strengthLevel.setValue(s.strength);
			attackBonus.setValue(s.attackBonus);
			strengthBonus.setValue(s.strengthBonus);
		}
		else if (st == 1)
		{
			attackLevel.setValue(s.ranged);
			strengthLevel.setValue(s.ranged);
			attackBonus.setValue(s.rangedAttack);
			strengthBonus.setValue(s.rangedStrength);
		}
		else
		{
			attackLevel.setValue(s.magic);
			strengthLevel.setValue(s.magic);
			attackBonus.setValue(s.magicAttack);
			damagePercent.setValue((int) Math.round(s.magicDamagePercent));
		}
		if (s.targetMagicLevel >= 0 && st == 2)
		{
			targetLevel.setValue(s.targetMagicLevel);
			targetDefence.setValue(s.targetMagicDefence);
		}
		target.setText(s.target.isEmpty() ? "No NPC targeted. Type its defence below."
			: s.target + (st == 2 && s.targetMagicLevel < 0 ? " (not in the table: type its Magic defence)" : ""));
		loading = false;
		recompute();
	}

	private void recompute()
	{
		if (loading)
		{
			return;
		}
		final int st = styleIndex();
		final int p = Math.max(0, prayer.getSelectedIndex());
		final int stn = Math.max(0, stance.getSelectedIndex());
		final int v = Math.max(0, voidSet.getSelectedIndex());
		final double prayerAttack = (Double) PRAYERS[st][p][1];
		final double prayerStrength = (Double) PRAYERS[st][p][2];
		final int stanceAttack = (Integer) STANCES[st][stn][1];
		final int stanceStrength = (Integer) STANCES[st][stn][2];
		final double voidAttack = (Double) VOIDS[st][v][1];
		final double voidStrength = (Double) VOIDS[st][v][2];
		final double voidDamage = (Double) VOIDS[st][v][3];
		final double other = value(otherPercent) / 100.0;

		final int max;
		if (st == 2)
		{
			max = MaxHit.magic(value(spellBase), value(damagePercent) + voidDamage, other);
		}
		else
		{
			max = MaxHit.physical(value(strengthLevel), prayerStrength, stanceStrength, voidStrength,
				value(strengthBonus), other);
		}
		final long attack = MaxHit.attackRoll(value(attackLevel), prayerAttack, stanceAttack, voidAttack,
			value(attackBonus));
		final long defence = MaxHit.defenceRoll(value(targetLevel), value(targetDefence));
		final double chance = MaxHit.hitChance(attack, defence);
		final double avg = MaxHit.expectedHit(max, chance);
		result.setText(String.format(Locale.ROOT,
			"<html>Max hit: <b>%d</b><br>Chance to hit: <b>%.1f%%</b><br>Average per attack: <b>%.2f</b>"
				+ "<br>Damage per second: <b>%.2f</b></html>",
			max, chance * 100, avg, MaxHit.dps(avg, value(speed))));
	}

	private static int value(JSpinner s)
	{
		return ((Number) s.getValue()).intValue();
	}
}
