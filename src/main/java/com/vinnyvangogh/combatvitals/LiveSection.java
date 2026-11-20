package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The sidebar's Live section, above the presets, as cards (Board, 2026-11-14):
 * a Combat card (thick Hitpoints and Prayer bars, Run and Spec chips, the
 * target in its own box), a Skilling card (the skill's icon and level, its XP
 * bar, a 2x2 grid of the session's stats, a training method's timers as
 * chips) and This session (the three skills with the most XP gained, the rest
 * behind Show all). Redrawn from each tick's {@link LiveVitals}. The section
 * folds from its title and stays folded or open across restarts
 * ({@link #OPEN_KEY}); each card folds on its own.
 *
 * <p>Every component is built once. An update only changes text, bars and
 * visibility, and only where they differ from what is showing; a session line,
 * chip or skilling line beyond any shown before is the one thing ever added.
 * Swing thread only.
 */
final class LiveSection extends VitalsSidePanel.Column
{
	/** Hidden setting: the section is open. */
	static final String OPEN_KEY = "sidebarLiveOpen";
	static final String TITLE = "Live";
	static final String LOGGED_OUT = "Log in to see your vitals here.";
	static final String COMBAT_OFF = "Combat vitals is off (Combat tab).";
	static final String SKILLING_OFF = "Skilling vitals is off (Skilling tab).";
	static final String NOT_IN_COMBAT = "Not in combat";
	static final String NOT_SKILLING = "Not skilling";
	static final String NOTHING_YET = "No XP gained yet this session.";
	/** How many of the session's skills show before Show all. */
	static final int TOP = 3;
	static final String SHOW_TOP = "Show top " + TOP;
	/** Hidden setting: which of the three cards the Live tab shows, a {@link View} name. */
	static final String VIEW_KEY = "sidebarLiveView";
	/** Hidden setting the right-click Clear ticks; the plugin zeroes the session, lets the Skilling card go, and unticks it. */
	static final String CLEAR_KEY = "liveCardClear";
	/** The same setting as the Skilling tab's "Reset session" tick box. */
	static final String RESET_KEY = "skillingSessionReset";
	static final String PAUSED = " · Paused";

	/** The secondary toggle under the Live tab: one card at a time, as in Group Ironman's live view. */
	enum View
	{
		COMBAT("Combat"), SKILLING("Skilling"), OVERALL("Overall");

		final String label;

		View(String label)
		{
			this.label = label;
		}

		static View of(String name)
		{
			for (View v : values())
			{
				if (v.name().equals(name))
				{
					return v;
				}
			}
			return COMBAT;
		}
	}

	/** The 2x2 grid's tiles, in reading order, and their captions. */
	private static final SessionRow.Kind[] STATS = {SessionRow.Kind.XP_RATE, SessionRow.Kind.ACTION_RATE,
		SessionRow.Kind.TIME_TO_LEVEL, SessionRow.Kind.COUNT};
	private static final String[] CAPTIONS = {"XP / hour", "Actions / hour", "To level", "Done"};

	/** The Splashing mode setting, switched here beside the vitals as well as on the Targets tab. */
	static final String SPLASH_KEY = "splashMode";

	private final javax.swing.JCheckBox splashSwitch = new javax.swing.JCheckBox("Splashing mode (AFK Magic)");
	private final ConfigStore store;
	private final FoldCard.Header header = new FoldCard.Header(TITLE, null);
	private final VitalsSidePanel.Column body = new VitalsSidePanel.Column();

	private final Card loggedOutCard = Card.of(null);
	private final VitalsSidePanel.Column live = new VitalsSidePanel.Column();

	// Combat
	private final FoldCard combatCard = new FoldCard("Combat", null, LiveVitals.HITPOINTS, true);
	private final WrapLabel combatOff = new WrapLabel(COMBAT_OFF, PanelStyle.MUTED);
	private final VitalsSidePanel.Column combat = new VitalsSidePanel.Column();
	private final MeterBar hitpoints = new MeterBar();
	private final MeterBar prayer = new MeterBar();
	private final JPanel energyRow = new JPanel(new GridLayout(1, 2, PanelStyle.S, 0));
	private final Chip run = new Chip();
	private final Chip spec = new Chip();
	private final Card targetBox = Card.inset();
	private final WrapLabel targetName = new WrapLabel("", PanelStyle.ACCENT);
	private final MeterBar target = new MeterBar();
	private final WrapLabel notInCombat = new WrapLabel(NOT_IN_COMBAT, PanelStyle.MUTED);
	private final MeterBar training = new MeterBar();
	private final WrapLabel trainingRate = new WrapLabel("", PanelStyle.MUTED);
	/** Goals in the skills a fight trains, the same lines as the Skilling card's. */
	private final VitalsSidePanel.Column combatGoals = new VitalsSidePanel.Column();
	private final List<GoalRow> combatGoalPool = new ArrayList<>();
	/** This session and Total, side by side in time: the session resets on an activity change, the total does not. */
	private final VitalsSidePanel.Column combatPeriods = new VitalsSidePanel.Column();
	private final List<PeriodRow> combatPeriodPool = new ArrayList<>();
	private final VitalsSidePanel.Column skillPeriods = new VitalsSidePanel.Column();
	private final List<PeriodRow> skillPeriodPool = new ArrayList<>();
	/** "Active · running 14m", "Pauses in 0:42", "Session resets in 12:50", under the card's title. */
	private final VitalsSidePanel.Column combatTimers = new VitalsSidePanel.Column();
	private final List<WrapLabel> combatTimerPool = new ArrayList<>();
	private final VitalsSidePanel.Column skillTimers = new VitalsSidePanel.Column();
	private final List<WrapLabel> skillTimerPool = new ArrayList<>();
	/** The Overall card's All time row. */
	private final VitalsSidePanel.Column overallPeriods = new VitalsSidePanel.Column();
	private final List<PeriodRow> overallPeriodPool = new ArrayList<>();
	/** Right-click on any Live card: Reset session, Clear. */
	private final javax.swing.JPopupMenu cardMenu = new javax.swing.JPopupMenu();
	private final java.awt.event.MouseAdapter cardMenuTrigger = new java.awt.event.MouseAdapter()
	{
		@Override
		public void mousePressed(java.awt.event.MouseEvent e)
		{
			popup(e);
		}

		@Override
		public void mouseReleased(java.awt.event.MouseEvent e)
		{
			popup(e);
		}

		private void popup(java.awt.event.MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				cardMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	};

	// Total (Overall view): every skill gained since login
	private final FoldCard totalCard = new FoldCard("Total since login", null, null, true);
	private final WrapLabel totalEmpty = new WrapLabel(NOTHING_YET, PanelStyle.MUTED);
	private final VitalsSidePanel.Column totalLines = new VitalsSidePanel.Column();
	private final List<GainRow> totalPool = new ArrayList<>();

	// Skilling
	private final FoldCard skillingCard = new FoldCard("Skilling", null, SkillColours.DEFAULT, true);
	private final WrapLabel skillingOff = new WrapLabel(SKILLING_OFF, PanelStyle.MUTED);
	private final VitalsSidePanel.Column skilling = new VitalsSidePanel.Column();
	private final SkillIcon skillIcon = new SkillIcon();
	private final WrapLabel skillHeader = new WrapLabel("", PanelStyle.TEXT);
	private final MeterBar skillXp = new MeterBar();
	private final JPanel statGrid = new JPanel(new GridLayout(2, 2, PanelStyle.S, PanelStyle.S));
	private final Map<SessionRow.Kind, Card> statTiles = new EnumMap<>(SessionRow.Kind.class);
	private final Map<SessionRow.Kind, WrapLabel> statValues = new EnumMap<>(SessionRow.Kind.class);
	private final VitalsSidePanel.Column skillChips = new VitalsSidePanel.Column();
	private final List<Chip> chipPool = new ArrayList<>();
	private final VitalsSidePanel.Column skillLines = new VitalsSidePanel.Column();
	private final List<WrapLabel> skillPool = new ArrayList<>();
	/** The active skill's goals, each with what is left and the time to go. */
	private final VitalsSidePanel.Column skillGoals = new VitalsSidePanel.Column();
	private final List<GoalRow> goalPool = new ArrayList<>();

	// This session
	private final FoldCard sessionCard = new FoldCard("This session", null, null, true);
	private final WrapLabel sessionEmpty = new WrapLabel(NOTHING_YET, PanelStyle.MUTED);
	private final VitalsSidePanel.Column sessionLines = new VitalsSidePanel.Column();
	private final List<GainRow> sessionPool = new ArrayList<>();
	private final JButton showAll = VitalsSidePanel.linkButton("", null);
	private boolean allGains;

	private final Map<View, VitalsSidePanel.Segment> viewButtons = new EnumMap<>(View.class);
	private View view = View.COMBAT;
	private LiveVitals last = LiveVitals.LOGGED_OUT;
	private volatile boolean open = true;
	/** Text, bar and visibility changes made, for the tests that check an update redraws only what changed. */
	int changes;

	LiveSection(ConfigStore store)
	{
		this.store = store;
		header.setName("live-toggle");
		header.setToolTipText("Fold or open the live readout.");
		header.setBorder(new EmptyBorder(0, 0, PanelStyle.S, 0));
		header.addActionListener(e -> setOpen(!open, true));
		add(header);

		loggedOutCard.add(new WrapLabel(LOGGED_OUT, PanelStyle.MUTED));
		body.add(loggedOutCard);
		body.add(live);
		add(body);

		buildCombat();
		buildSkilling();
		buildSession();
		live.add(viewBar());
		live.add(combatCard);
		live.add(skillingCard);
		live.add(sessionCard);
		totalCard.body.add(overallPeriods);
		totalCard.body.add(totalEmpty);
		totalCard.body.add(totalLines);
		live.add(totalCard);
		buildCardMenu();
		applyView(View.of(store.get(VIEW_KEY, String.class)));

		refresh();
		show(LiveVitals.LOGGED_OUT);
	}

	/**
	 * Right-click on a Live card: Reset session zeroes This session's counts (the card stays); Clear also lets the
	 * Skilling card go, so it is empty until you train again. Total and All time are never touched.
	 */
	private void buildCardMenu()
	{
		final javax.swing.JMenuItem reset = new javax.swing.JMenuItem(VitalsSidePanel.RESET_SESSION);
		reset.setName("live-menu-reset");
		reset.setToolTipText("Zero This session's counts. Total and All time stay.");
		reset.addActionListener(e -> store.set(RESET_KEY, true));
		final javax.swing.JMenuItem clear = new javax.swing.JMenuItem("Clear");
		clear.setName("live-menu-clear");
		clear.setToolTipText("Zero This session and empty the Skilling card until you train again. Total and All time stay.");
		clear.addActionListener(e -> store.set(CLEAR_KEY, true));
		cardMenu.add(reset);
		cardMenu.add(clear);
		popupOn(combatCard);
		popupOn(skillingCard);
		popupOn(sessionCard);
		popupOn(totalCard);
	}

	/** Makes the right-click menu come up over a component and everything inside it; once per component. */
	private void popupOn(Component c)
	{
		if (c instanceof JComponent && ((JComponent) c).getClientProperty(cardMenuTrigger) == null)
		{
			((JComponent) c).putClientProperty(cardMenuTrigger, Boolean.TRUE);
			c.addMouseListener(cardMenuTrigger);
		}
		if (c instanceof java.awt.Container)
		{
			for (Component child : ((java.awt.Container) c).getComponents())
			{
				popupOn(child);
			}
		}
	}

	/** For tests: the right-click menu's items, to click. */
	javax.swing.JPopupMenu cardMenu()
	{
		return cardMenu;
	}

	/** Combat / Skilling / Overall as a small segmented row; the choice is remembered. */
	private JComponent viewBar()
	{
		final JPanel bar = new JPanel(new GridLayout(1, 0, 0, 0));
		bar.setOpaque(false);
		bar.setBorder(new EmptyBorder(0, 0, PanelStyle.S, 0));
		final ButtonGroup group = new ButtonGroup();
		for (View v : View.values())
		{
			final VitalsSidePanel.Segment b = new VitalsSidePanel.Segment(v.label);
			b.setName("live-view-" + v.name());
			b.addActionListener(e ->
			{
				applyView(v);
				store.set(VIEW_KEY, v.name());
			});
			group.add(b);
			viewButtons.put(v, b);
			bar.add(b);
		}
		return bar;
	}

	/** Shows only the chosen view's card. */
	void applyView(View v)
	{
		view = v;
		combatCard.setVisible(v == View.COMBAT);
		skillingCard.setVisible(v == View.SKILLING);
		sessionCard.setVisible(v == View.OVERALL);
		totalCard.setVisible(v == View.OVERALL);
		viewButtons.get(v).setSelected(true);
		revalidate();
		repaint();
	}

	View view()
	{
		return view;
	}

	private void buildCombat()
	{
		energyRow.setOpaque(false);
		energyRow.add(run);
		energyRow.add(spec);
		targetName.setFont(PanelStyle.title());
		targetBox.add(targetName);
		targetBox.add(notInCombat);
		targetBox.add(target);
		// The same setting as the Targets tab and the card's right-click, so all three agree.
		splashSwitch.setName("live-splash-mode");
		splashSwitch.setFont(PanelStyle.body());
		splashSwitch.setForeground(PanelStyle.TEXT);
		splashSwitch.setOpaque(false);
		splashSwitch.setFocusable(false);
		splashSwitch.setToolTipText("Splashing mode: show your Magic bonus and hit chance on the combat card. "
			+ "Same as the right-click option on the card.");
		splashSwitch.addActionListener(e -> store.set(SPLASH_KEY, splashSwitch.isSelected()));
		for (Component c : new Component[]{splashSwitch, hitpoints, gap(), prayer, gap(), energyRow, targetBox, gap(),
			training, trainingRate, combatTimers, combatPeriods, combatGoals})
		{
			combat.add(c);
		}
		combatCard.body.add(combatOff);
		combatCard.body.add(combat);
	}

	private void buildSkilling()
	{
		final JPanel title = new JPanel(new BorderLayout(PanelStyle.S, 0));
		title.setOpaque(false);
		title.add(skillIcon, BorderLayout.WEST);
		final VitalsSidePanel.Column name = new VitalsSidePanel.Column();
		skillHeader.setFont(PanelStyle.title());
		name.add(skillHeader);
		title.add(name, BorderLayout.CENTER);

		statGrid.setOpaque(false);
		for (int i = 0; i < STATS.length; i++)
		{
			final Card tile = Card.inset();
			final JLabel caption = new JLabel(CAPTIONS[i]);
			caption.setFont(PanelStyle.body());
			caption.setForeground(PanelStyle.MUTED);
			final WrapLabel value = new WrapLabel("", PanelStyle.TEXT);
			tile.add(caption);
			tile.add(value);
			statTiles.put(STATS[i], tile);
			statValues.put(STATS[i], value);
			statGrid.add(tile);
		}
		for (Component c : new Component[]{title, skillTimers, gap(), skillXp, gap(), statGrid, gap(), skillChips, skillLines,
			skillPeriods, skillGoals})
		{
			skilling.add(c);
		}
		skillingCard.body.add(skillingOff);
		skillingCard.body.add(skilling);
	}

	private void buildSession()
	{
		showAll.setName("session-show-all");
		showAll.addActionListener(e ->
		{
			allGains = !allGains;
			show(last);
		});
		sessionCard.body.add(sessionEmpty);
		sessionCard.body.add(sessionLines);
		sessionCard.body.add(showAll);
	}

	private static Component gap()
	{
		return Box.createVerticalStrut(PanelStyle.S);
	}

	/** Open, as last set on the Swing thread; read from the client thread. */
	boolean isOpen()
	{
		return open;
	}

	/** Folds or opens to match the stored setting, after a change made anywhere. */
	void refresh()
	{
		setOpen(!Boolean.FALSE.equals(store.get(OPEN_KEY, Boolean.class)), false);
		splashSwitch.setSelected(Boolean.TRUE.equals(store.get(SPLASH_KEY, Boolean.class)));
	}

	private void setOpen(boolean open, boolean remember)
	{
		this.open = open;
		header.showOpen(open);
		if (body.isVisible() != open)
		{
			body.setVisible(open);
			if (open)
			{
				show(last);
			}
			revalidate();
			repaint();
		}
		if (remember)
		{
			store.set(OPEN_KEY, open);
		}
	}

	/** Shows a tick's vitals; while folded it is kept and drawn on opening. */
	void update(LiveVitals v)
	{
		last = v;
		if (open)
		{
			show(v);
		}
	}

	private void show(LiveVitals v)
	{
		final int before = changes;
		visible(loggedOutCard, !v.loggedIn);
		visible(live, v.loggedIn);
		if (v.loggedIn)
		{
			showCombat(v.combat);
			showSkilling(v.skilling);
			showSession(v.session);
			periods(overallPeriods, overallPeriodPool, v.overall);
			visible(overallPeriods, !v.overall.isEmpty());
			showTotals(v.totals);
		}
		if (changes != before)
		{
			// Rows added by this update are right-clickable too.
			popupOn(combatCard);
			popupOn(skillingCard);
			popupOn(sessionCard);
			popupOn(totalCard);
			revalidate();
			repaint();
		}
	}

	private void showCombat(LiveVitals.Combat c)
	{
		visible(combatOff, c == null);
		visible(combat, c != null);
		if (c == null)
		{
			return;
		}
		bar(hitpoints, c.hitpoints);
		bar(prayer, c.prayer);
		final String[] energy = c.energy == null ? new String[0] : c.energy.split(" \\| ");
		chip(run, energy.length > 0 ? energy[0] : null);
		chip(spec, energy.length > 1 ? energy[1] : null);
		visible(energyRow, energy.length > 0);
		final boolean fighting = c.targetName != null;
		text(targetName, c.targetName);
		bar(target, fighting ? c.target : null);
		visible(notInCombat, !fighting);
		bar(training, c.training);
		text(trainingRate, c.trainingRate);
		lines(combatTimers, combatTimerPool, c.timers);
		visible(combatTimers, !c.timers.isEmpty());
		periods(combatPeriods, combatPeriodPool, c.periods);
		visible(combatPeriods, !c.periods.isEmpty());
		goals(combatGoals, combatGoalPool, c.goals);
		visible(combatGoals, !c.goals.isEmpty());
	}

	/** Every skill gained since login, most XP first, with no fold: the session list's twin for the Overall view. */
	private void showTotals(List<LiveVitals.Gain> gains)
	{
		visible(totalEmpty, gains.isEmpty());
		while (totalPool.size() < gains.size())
		{
			final GainRow row = new GainRow();
			row.setVisible(false);
			totalPool.add(row);
			totalLines.add(row);
			changes++;
		}
		final int top = gains.isEmpty() ? 1 : Math.max(1, gains.get(0).xp);
		for (int i = 0; i < totalPool.size(); i++)
		{
			gain(totalPool.get(i), i < gains.size() ? gains.get(i) : null, top);
		}
	}

	private void showSkilling(LiveVitals.Skilling s)
	{
		visible(skillingOff, s == null);
		visible(skilling, s != null);
		if (s == null)
		{
			return;
		}
		if (skillingCard.accent(s.colour))
		{
			changes++;
		}
		if (skillIcon.set(s.header == null ? null : s.icon, s.colour))
		{
			changes++;
		}
		visible(skillIcon, s.header != null);
		text(skillHeader, s.header == null ? NOT_SKILLING
			: s.paused && s.title != null ? s.title + PAUSED : s.header);
		lines(skillTimers, skillTimerPool, s.timers);
		visible(skillTimers, !s.timers.isEmpty());
		bar(skillXp, s.xp);
		for (SessionRow.Kind kind : STATS)
		{
			final String value = s.stats.get(kind);
			text(statValues.get(kind), value);
			visible(statTiles.get(kind), value != null);
		}
		visible(statGrid, !s.stats.isEmpty());
		chips(s.chips);
		lines(skillLines, skillPool, s.lines);
		periods(skillPeriods, skillPeriodPool, s.header == null ? java.util.Collections.emptyList() : s.periods);
		visible(skillPeriods, s.header != null && !s.periods.isEmpty());
		goals(skillGoals, goalPool, s.header == null ? java.util.Collections.emptyList() : s.goals);
		visible(skillGoals, s.header != null && !s.goals.isEmpty());
	}

	/** The three skills with the most XP gained; the rest only after Show all. */
	private void showSession(List<LiveVitals.Gain> gains)
	{
		visible(sessionEmpty, gains.isEmpty());
		while (sessionPool.size() < gains.size())
		{
			final GainRow row = new GainRow();
			row.setVisible(false);
			sessionPool.add(row);
			sessionLines.add(row);
			changes++;
		}
		final int top = gains.isEmpty() ? 1 : Math.max(1, gains.get(0).xp);
		final int shown = allGains ? gains.size() : Math.min(TOP, gains.size());
		for (int i = 0; i < sessionPool.size(); i++)
		{
			gain(sessionPool.get(i), i < shown ? gains.get(i) : null, top);
		}
		text(showAll, gains.size() <= TOP ? null : allGains ? SHOW_TOP : "Show all " + gains.size());
	}

	/** One Overall row: the gain's line inside its bar against the top skill's XP, details in the tooltip; hidden for null. */
	private void gain(GainRow row, LiveVitals.Gain g, int top)
	{
		bar(row.bar, g == null ? null : new LiveVitals.Bar((double) g.xp / top, g.text, g.colour));
		final String tip = g == null ? null : g.tip;
		if (g != null && !g.tip.equals(row.bar.getToolTipText()))
		{
			row.bar.setToolTipText(tip);
			changes++;
		}
		visible(row, g != null);
	}

	/** The first {@code texts.size()} chips of the pool show the timers; the rest hide. */
	private void chips(List<String> texts)
	{
		while (chipPool.size() < texts.size())
		{
			final Chip chip = new Chip();
			chip.setVisible(false);
			chipPool.add(chip);
			skillChips.add(chip);
			changes++;
		}
		for (int i = 0; i < chipPool.size(); i++)
		{
			chip(chipPool.get(i), i < texts.size() ? texts.get(i) : null);
		}
	}

	/** The first rows of the pool show the goals (one bar each, its words inside, the detail in a tooltip); the rest hide. */
	private void goals(VitalsSidePanel.Column parent, List<GoalRow> pool, List<LiveVitals.GoalLine> goals)
	{
		while (pool.size() < goals.size())
		{
			final GoalRow row = new GoalRow();
			row.setVisible(false);
			pool.add(row);
			parent.add(row);
			changes++;
		}
		for (int i = 0; i < pool.size(); i++)
		{
			final GoalRow row = pool.get(i);
			final LiveVitals.GoalLine g = i < goals.size() ? goals.get(i) : null;
			if (g != null && row.set(g))
			{
				changes++;
			}
			visible(row, g != null);
		}
	}

	/** The first rows of the pool show the periods (label left, value right); the rest hide. */
	private void periods(VitalsSidePanel.Column parent, List<PeriodRow> pool, List<LiveVitals.Period> periods)
	{
		while (pool.size() < periods.size())
		{
			final PeriodRow row = new PeriodRow();
			row.setVisible(false);
			pool.add(row);
			parent.add(row);
			changes++;
		}
		for (int i = 0; i < pool.size(); i++)
		{
			final PeriodRow row = pool.get(i);
			final LiveVitals.Period p = i < periods.size() ? periods.get(i) : null;
			if (p != null && row.set(p))
			{
				changes++;
			}
			visible(row, p != null);
		}
	}

	/** One goal: a single bar like the level bar, its words inside, and every detail in the tooltip. */
	private static final class GoalRow extends VitalsSidePanel.Column
	{
		private final MeterBar bar = new MeterBar();
		private LiveVitals.GoalLine shown;

		GoalRow()
		{
			setBorder(new EmptyBorder(PanelStyle.S, 0, 0, 0));
			add(bar);
		}

		/** False when already showing this goal. */
		boolean set(LiveVitals.GoalLine g)
		{
			if (shown != null && shown.tooltip.equals(g.tooltip) && Objects.equals(shown.bar, g.bar))
			{
				return false;
			}
			shown = g;
			bar.set(g.bar);
			bar.setToolTipText(g.tooltip);
			return true;
		}

		MeterBar bar()
		{
			return bar;
		}
	}

	/**
	 * "This session   +12.3k xp   23m   45.2k/h": a muted label and three right-aligned columns sized to the widest
	 * text in the card, so every row's columns line up. A cell too long for the panel is cut with an ellipsis and the
	 * row's tooltip has the whole line.
	 */
	private static final class PeriodRow extends JPanel
	{
		/** Label, xp, time, xp/h. */
		private static final int COLUMNS = 4;
		/** Space between columns, so one cell's text never runs into the next. */
		private static final int COLUMN_GAP = 4;
		private static final String XP_UNIT = " xp";
		private final JLabel[] cells = new JLabel[COLUMNS];
		private LiveVitals.Period shown;

		PeriodRow()
		{
			super(null);
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
			setBorder(new EmptyBorder(1, 0, 1, 0));
			for (int i = 0; i < cells.length; i++)
			{
				cells[i] = new JLabel();
				cells[i].setFont(PanelStyle.body());
				cells[i].setForeground(i == 0 ? PanelStyle.MUTED : PanelStyle.TEXT);
				cells[i].setHorizontalAlignment(i == 0 ? javax.swing.SwingConstants.LEFT : javax.swing.SwingConstants.RIGHT);
				add(cells[i]);
			}
		}

		@Override
		public Dimension getPreferredSize()
		{
			final java.awt.Insets in = getInsets();
			return new Dimension(10, cells[0].getPreferredSize().height + in.top + in.bottom);
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize()
		{
			return PanelStyle.fullWidth(this);
		}

		/**
		 * Columns are as wide as the widest text in any visible row of the same card, so every row's columns line up
		 * and nothing is cut while it fits. When the rows are wider than the panel, the label gives way first.
		 */
		@Override
		public void doLayout()
		{
			final java.awt.Insets in = getInsets();
			final int width = getWidth() - in.left - in.right;
			final int height = getHeight() - in.top - in.bottom;
			final int[] widths = columnWidths(width);
			int x = in.left;
			for (int i = 0; i < cells.length; i++)
			{
				final int gap = i == 0 ? 0 : COLUMN_GAP;
				final int w = i == cells.length - 1 ? in.left + width - x - gap : widths[i];
				cells[i].setBounds(x + gap, in.top, Math.max(0, w), height);
				x += widths[i] + gap;
			}
		}

		/** Label, xp, time, xp/h widths for a row {@code width} wide; the last column is flush right. */
		private int[] columnWidths(int width)
		{
			final int[] want = new int[cells.length];
			final java.awt.Container parent = getParent();
			final java.awt.Component[] rows = parent == null ? new java.awt.Component[] {this} : parent.getComponents();
			for (java.awt.Component c : rows)
			{
				if (c instanceof PeriodRow && c.isVisible())
				{
					for (int i = 0; i < want.length; i++)
					{
						want[i] = Math.max(want[i], ((PeriodRow) c).cells[i].getPreferredSize().width);
					}
				}
			}
			final int gaps = COLUMN_GAP * (want.length - 1);
			int over = 0;
			for (int w : want)
			{
				over += w;
			}
			over += gaps - width;
			final int[] out = new int[want.length];
			System.arraycopy(want, 0, out, 0, want.length);
			if (over > 0)
			{
				// Too wide: the label gives way first, down to a third of the row, then the xp column.
				final int cutLabel = Math.min(over, Math.max(0, want[0] - width / 3));
				out[0] -= cutLabel;
				over -= cutLabel;
				out[1] -= Math.min(over, Math.max(0, want[1] / 2));
			}
			else
			{
				// Spare room goes between the label and the numbers, which stay together at the right edge.
				out[0] += -over;
			}
			return out;
		}

		boolean set(LiveVitals.Period p)
		{
			if (shown != null && shown.label.equals(p.label) && shown.xp.equals(p.xp) && shown.time.equals(p.time)
				&& shown.rate.equals(p.rate) && shown.tip.equals(p.tip))
			{
				return false;
			}
			shown = p;
			cells[0].setText(p.label);
			// The unit goes: the rate beside it is xp/h, and the tooltip says "xp" in full. It is the 12px that keep
			// "+412.3k  2h 10m  190.2k/h" from being cut at the default sidebar width.
			cells[1].setText(p.xp.endsWith(XP_UNIT) ? p.xp.substring(0, p.xp.length() - XP_UNIT.length()) : p.xp);
			cells[2].setText(p.time);
			cells[3].setText(p.rate);
			setToolTipText(p.tip);
			for (JLabel cell : cells)
			{
				cell.setToolTipText(p.tip);
			}
			return true;
		}
	}

	/** The first {@code texts.size()} labels of the pool show the lines; the rest hide. */
	private void lines(VitalsSidePanel.Column parent, List<WrapLabel> pool, List<String> texts)
	{
		while (pool.size() < texts.size())
		{
			final WrapLabel l = new WrapLabel("", PanelStyle.SOFT);
			l.setVisible(false);
			pool.add(l);
			parent.add(l);
			changes++;
		}
		for (int i = 0; i < pool.size(); i++)
		{
			text(pool.get(i), i < texts.size() ? texts.get(i) : null);
		}
	}

	/** Shows the text, or hides the label for null. */
	private void text(WrapLabel label, String text)
	{
		if (text != null && label.setPlain(text))
		{
			changes++;
		}
		visible(label, text != null);
	}

	private void text(AbstractButton button, String text)
	{
		if (text != null && !text.equals(button.getText()))
		{
			button.setText(text);
			changes++;
		}
		visible(button, text != null);
	}

	private void chip(Chip chip, String text)
	{
		if (text != null && chip.show(text))
		{
			changes++;
		}
		visible(chip, text != null);
	}

	private void bar(MeterBar meter, LiveVitals.Bar bar)
	{
		if (meter.set(bar))
		{
			changes++;
		}
		visible(meter, bar != null);
	}

	private void visible(Component c, boolean visible)
	{
		if (c.isVisible() != visible)
		{
			c.setVisible(visible);
			changes++;
		}
	}

	/** One of the Overall card's skills: a single bar of its XP against the top skill's, its line inside it. */
	private static final class GainRow extends VitalsSidePanel.Column
	{
		final MeterBar bar = new MeterBar();

		GainRow()
		{
			setBorder(new EmptyBorder(0, 0, PanelStyle.S, 0));
			add(bar);
		}
	}

	/** The skill's small icon, or a dot in its colour when there is none. */
	static final class SkillIcon extends JComponent
	{
		private static final int SIZE = 18;
		private BufferedImage image;
		private Color colour;

		/** False when already showing these, so nothing is redrawn. */
		boolean set(BufferedImage image, Color colour)
		{
			if (image == this.image && Objects.equals(colour, this.colour))
			{
				return false;
			}
			this.image = image;
			this.colour = colour;
			repaint();
			return true;
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(SIZE, SIZE);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			if (image != null)
			{
				g.drawImage(image, (SIZE - image.getWidth()) / 2, (SIZE - image.getHeight()) / 2, null);
				return;
			}
			final Graphics2D g2 = PanelStyle.smooth(g);
			try
			{
				g2.setColor(colour == null ? SkillColours.DEFAULT : colour);
				g2.fillOval(3, 3, SIZE - 6, SIZE - 6);
			}
			finally
			{
				g2.dispose();
			}
		}
	}

	/** For tests: the session lines showing, top first. */
	List<String> sessionShown()
	{
		final List<String> out = new ArrayList<>();
		for (GainRow r : sessionPool)
		{
			if (r.isVisible())
			{
				out.add(r.bar.bar().text);
			}
		}
		return Collections.unmodifiableList(out);
	}
}
