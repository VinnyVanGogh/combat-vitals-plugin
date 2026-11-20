package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatter;
import net.runelite.api.Experience;
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

	/** Levels above the current one a new skill goal starts at. */
	static final int DEFAULT_LEVELS_AHEAD = 5;

	private final GoalBook book;
	private final java.util.function.Function<Skill, java.awt.image.BufferedImage> icons;
	private final JComboBox<String> kind = new JComboBox<>(KINDS);
	private final JComboBox<Skill> skill = new JComboBox<>(skills());
	private final JTextField name = new JTextField();
	private final JSpinner target = new JSpinner(new SpinnerNumberModel(6, 1, 2_000_000_000, 1));
	private final JLabel problem = new JLabel(" ");
	private final VitalsSidePanel.Column list = new VitalsSidePanel.Column();

	GoalsPage(GoalBook book)
	{
		this(book, null);
	}

	/** {@code icons} gives a skill's small icon for the dropdown; null or a null image shows the name alone. */
	GoalsPage(GoalBook book, java.util.function.Function<Skill, java.awt.image.BufferedImage> icons)
	{
		this.book = book;
		this.icons = icons;
		add(VitalsSidePanel.text("Your own goals, kept per account. Skill goals follow your experience and show "
			+ "what is left, the time to go at this session's rate and at the last hour's, and the actions left.",
			PanelStyle.MUTED));
		add(form());
		add(list);
		skill.setRenderer(new SkillRenderer());
		commitAsYouType();
		kind.addActionListener(e ->
		{
			syncForm();
			defaultTarget();
		});
		skill.addActionListener(e -> defaultTarget());
		syncForm();
		defaultTarget();
		refresh();
	}

	/** Makes the number box take each valid keystroke, so what is typed is what Add goal reads. */
	private void commitAsYouType()
	{
		final JFormattedTextField field = ((JSpinner.DefaultEditor) target.getEditor()).getTextField();
		if (field.getFormatter() instanceof DefaultFormatter)
		{
			((DefaultFormatter) field.getFormatter()).setCommitsOnValidEdit(true);
		}
	}

	/**
	 * A new skill goal starts about five levels above the current one, capped at 99; for an experience goal
	 * it is the experience that level takes. Other kinds keep whatever was typed.
	 */
	void defaultTarget()
	{
		final Skill s = (Skill) skill.getSelectedItem();
		if (kind.getSelectedIndex() > 1 || s == null)
		{
			return;
		}
		final int level = Experience.getLevelForXp((int) Math.min(Integer.MAX_VALUE, book.xp(s.getName())));
		final int goal = GoalMath.defaultLevel(level, DEFAULT_LEVELS_AHEAD);
		target.setValue(kind.getSelectedIndex() == 0 ? goal : Experience.getXpForLevel(goal));
		problem.setText(" ");
	}

	/** The skill's name as the game spells it ("Attack"), with its icon. */
	private final class SkillRenderer extends DefaultListCellRenderer
	{
		@Override
		public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
			boolean selected, boolean focus)
		{
			final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
			if (value instanceof Skill)
			{
				final Skill s = (Skill) value;
				label.setText(s.getName());
				final java.awt.image.BufferedImage image = icons == null ? null : icons.apply(s);
				label.setIcon(image == null ? null : new ImageIcon(image));
			}
			return label;
		}
	}

	/** Test hook: the form's boxes. */
	JComboBox<Skill> skillBox()
	{
		return skill;
	}

	JSpinner targetBox()
	{
		return target;
	}

	JComboBox<String> kindBox()
	{
		return kind;
	}

	String problemText()
	{
		return problem.getText();
	}

	void clickAdd()
	{
		add();
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
		problem.setFont(PanelStyle.body());
		problem.setForeground(PanelStyle.SOFT);
		card.add(problem);
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
		// Read what is typed, even if the box has not committed it yet; a number that does not parse adds nothing.
		try
		{
			target.commitEdit();
		}
		catch (java.text.ParseException e)
		{
			problem.setText("Type a whole number for the target.");
			return;
		}
		final long goal = ((Number) target.getValue()).longValue();
		problem.setText(" ");
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
		final GoalBook.Progress progress = g.reached ? null : book.progress(g, now);
		if (progress == null)
		{
			card.add(VitalsSidePanel.text(detail(g, value, goalValue, now), PanelStyle.SOFT));
		}
		else
		{
			card.add(progressRows(progress));
		}

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

	/** The progress section of a skill goal: one labelled row per figure. */
	private static JPanel progressRows(GoalBook.Progress p)
	{
		final VitalsSidePanel.Column rows = new VitalsSidePanel.Column();
		rows.add(row("XP to go", number(p.left)));
		rows.add(row("Done", p.percent + "%"));
		rows.add(row("At session rate", eta(p.left, p.sessionRate)));
		rows.add(row("At last hour's rate", eta(p.left, p.hourRate)));
		rows.add(row("Actions left", p.actionsLeft < 0 ? "no action yet"
			: number(p.actionsLeft) + " (" + number(Math.round(p.actionXp)) + " xp each)"));
		return rows;
	}

	private static String eta(long left, double perHour)
	{
		return GoalMath.etaText(GoalMath.etaSeconds(left, perHour))
			+ (perHour > 0 ? " (" + number(Math.round(perHour)) + " xp/hr)" : "");
	}

	private static JComponent row(String label, String value)
	{
		final JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		final JLabel l = new JLabel(label);
		l.setFont(PanelStyle.body());
		l.setForeground(PanelStyle.MUTED);
		final JLabel v = new JLabel(value);
		v.setFont(PanelStyle.body());
		v.setForeground(PanelStyle.TEXT);
		row.add(l, BorderLayout.WEST);
		row.add(v, BorderLayout.EAST);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		return row;
	}

	private static String title(Goal g)
	{
		return GoalMath.title(g);
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
