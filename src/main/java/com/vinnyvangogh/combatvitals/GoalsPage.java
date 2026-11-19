package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;

/**
 * The Goals page of the sidebar: our own goals, not the game's XP-drop goals.
 * A form to add one, then a card per goal with a bar and, for a skill, this
 * session's rate and the time to go at it. Swing thread only; the plugin
 * calls {@link #refresh()} when experience or a count moves.
 */
final class GoalsPage extends VitalsSidePanel.Column
{
	private static final String[] KINDS = {"Skill level", "Skill XP", "Item", "Boss kills", "Daily", "Weekly"};
	private static final Goal.Type[] TYPES = {Goal.Type.SKILL_LEVEL, Goal.Type.SKILL_XP, Goal.Type.ITEM,
		Goal.Type.BOSS_KC, Goal.Type.DAILY, Goal.Type.WEEKLY};

	private final GoalBook book;
	private final JComboBox<String> kind = new JComboBox<>(KINDS);
	private final JComboBox<Skill> skill = new JComboBox<>(skills());
	private final JTextField name = new JTextField();
	private final JSpinner target = new JSpinner(new SpinnerNumberModel(99, 1, 2_000_000_000, 1));
	private final VitalsSidePanel.Column list = new VitalsSidePanel.Column();

	GoalsPage(GoalBook book)
	{
		this.book = book;
		add(VitalsSidePanel.text("Your own goals, kept per account. Skill goals follow your experience and show "
			+ "the time to go at this session's rate.", PanelStyle.MUTED));
		add(form());
		add(list);
		kind.addActionListener(e -> syncForm());
		syncForm();
		refresh();
	}

	private static Skill[] skills()
	{
		final java.util.List<Skill> out = new java.util.ArrayList<>();
		for (Skill s : Skill.values())
		{
			if (s != Skill.OVERALL)
			{
				out.add(s);
			}
		}
		return out.toArray(new Skill[0]);
	}

	private Card form()
	{
		final Card card = Card.of(PanelStyle.ACCENT);
		card.add(VitalsSidePanel.heading("Add a goal"));
		for (JComponent c : new JComponent[] {kind, skill, name, target})
		{
			c.setFont(PanelStyle.body());
			c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
			card.add(c);
		}
		name.setToolTipText("The item, boss or task. For a boss, use the name the game's kill count line uses.");
		target.setToolTipText("A level, experience, a count or a number of kills.");
		final JButton add = new JButton("Add goal");
		add.setFont(PanelStyle.body());
		add.setFocusable(false);
		add.addActionListener(e -> add());
		card.add(add);
		return card;
	}

	private void syncForm()
	{
		final boolean isSkill = kind.getSelectedIndex() < 2;
		skill.setVisible(isSkill);
		name.setVisible(!isSkill);
		revalidate();
	}

	private void add()
	{
		final Goal.Type type = TYPES[kind.getSelectedIndex()];
		final long goal = ((Number) target.getValue()).longValue();
		final long now = System.currentTimeMillis() / 1000;
		if (type == Goal.Type.SKILL_LEVEL || type == Goal.Type.SKILL_XP)
		{
			final Skill s = (Skill) skill.getSelectedItem();
			if (s == null)
			{
				return;
			}
			book.addSkill(s.getName(), type == Goal.Type.SKILL_LEVEL, goal, now);
		}
		else
		{
			final String label = name.getText().trim();
			if (label.isEmpty())
			{
				return;
			}
			book.addCounted(type, label, goal, now);
			name.setText("");
		}
		refresh();
	}

	/** Redraws the goal cards from the book. */
	void refresh()
	{
		final long now = System.currentTimeMillis() / 1000;
		list.removeAll();
		final List<Goal> goals = book.goals();
		if (goals.isEmpty())
		{
			list.add(VitalsSidePanel.text("No goals yet.", PanelStyle.SOFT));
		}
		for (Goal g : goals)
		{
			list.add(card(g, now));
		}
		list.revalidate();
		list.repaint();
	}

	private Card card(Goal g, long now)
	{
		final long value = book.value(g);
		final long goalValue = g.goalValue();
		final Card card = Card.of(g.reached ? PanelStyle.ACCENT : null);

		final JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		final JLabel title = new JLabel(title(g));
		title.setFont(PanelStyle.title());
		title.setForeground(PanelStyle.TEXT);
		head.add(title, BorderLayout.CENTER);
		final JButton remove = new JButton("x");
		remove.setFont(PanelStyle.body());
		remove.setFocusable(false);
		remove.setToolTipText("Delete this goal");
		remove.addActionListener(e ->
		{
			book.remove(g.id);
			refresh();
		});
		head.add(remove, BorderLayout.EAST);
		head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		card.add(head);

		card.add(new Bar(GoalMath.fraction(g.baseline, value, goalValue)));
		card.add(VitalsSidePanel.text(detail(g, value, goalValue, now), PanelStyle.SOFT));

		if (!g.skill() && g.type != Goal.Type.BOSS_KC)
		{
			final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			row.setOpaque(false);
			row.setBorder(new EmptyBorder(PanelStyle.S, 0, 0, 0));
			for (int delta : new int[] {-1, 1})
			{
				final JButton b = new JButton(delta > 0 ? "+1" : "-1");
				b.setFont(PanelStyle.body());
				b.setFocusable(false);
				b.addActionListener(e ->
				{
					book.bump(g.id, delta);
					refresh();
				});
				row.add(b);
			}
			card.add(row);
		}
		return card;
	}

	private static String title(Goal g)
	{
		switch (g.type)
		{
			case SKILL_LEVEL:
				return g.label + " level " + g.target;
			case SKILL_XP:
				return g.label + " " + number(g.target) + " xp";
			case BOSS_KC:
				return g.label + " x" + number(g.target);
			case ITEM:
				return g.label + " x" + number(g.target);
			case DAILY:
				return "Daily: " + g.label;
			default:
				return "Weekly: " + g.label;
		}
	}

	private String detail(Goal g, long value, long goalValue, long now)
	{
		if (g.reached)
		{
			return "Reached.";
		}
		final long left = GoalMath.remaining(value, goalValue);
		final String percent = Math.round(100 * GoalMath.fraction(g.baseline, value, goalValue)) + "%";
		if (!g.skill())
		{
			return number(value) + " / " + number(goalValue) + " (" + percent + ")";
		}
		final double rate = book.xpPerHour(g.label, now);
		return number(left) + " xp to go (" + percent + "), " + number(Math.round(rate)) + " xp/hr, "
			+ GoalMath.etaText(GoalMath.etaSeconds(left, rate));
	}

	private static String number(long n)
	{
		return NumberFormat.getIntegerInstance(Locale.US).format(n);
	}

	/** A thin progress bar in the panel's track colour and accent. */
	private static final class Bar extends JComponent
	{
		private final double fraction;

		Bar(double fraction)
		{
			this.fraction = fraction;
			setPreferredSize(new Dimension(10, 10));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			final Graphics2D g2 = PanelStyle.smooth(g);
			try
			{
				g2.setColor(PanelStyle.TRACK);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
				g2.setColor(PanelStyle.ACCENT);
				g2.fillRoundRect(0, 0, (int) Math.round(getWidth() * fraction), getHeight(), 6, 6);
			}
			finally
			{
				g2.dispose();
			}
		}
	}
}
