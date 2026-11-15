package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The sidebar's Live section, above the presets: your vitals and target, the
 * skill being trained, and this session's gains, redrawn from each tick's
 * {@link LiveVitals}. It folds from its header, and stays folded or open
 * across restarts ({@link #OPEN_KEY}).
 *
 * <p>Every component is built once. An update only changes text, bars and
 * visibility, and only where they differ from what is showing; a session or
 * skilling line beyond any shown before is the one thing ever added. Swing
 * thread only.
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

	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR.darker();

	private final ConfigStore store;
	private final JButton header = new JButton();
	private final VitalsSidePanel.Column body = new VitalsSidePanel.Column();

	private final WrapLabel loggedOut = new WrapLabel(LOGGED_OUT, MUTED);
	private final VitalsSidePanel.Column live = new VitalsSidePanel.Column();

	private final WrapLabel combatOff = new WrapLabel(COMBAT_OFF, MUTED);
	private final VitalsSidePanel.Column combat = new VitalsSidePanel.Column();
	private final MeterBar hitpoints = new MeterBar();
	private final MeterBar prayer = new MeterBar();
	private final WrapLabel energy = new WrapLabel("", Color.WHITE);
	private final WrapLabel targetName = new WrapLabel("", ColorScheme.BRAND_ORANGE);
	private final MeterBar target = new MeterBar();
	private final MeterBar training = new MeterBar();
	private final WrapLabel trainingRate = new WrapLabel("", MUTED);

	private final WrapLabel skillingOff = new WrapLabel(SKILLING_OFF, MUTED);
	private final VitalsSidePanel.Column skilling = new VitalsSidePanel.Column();
	private final WrapLabel skillHeader = new WrapLabel("", Color.WHITE);
	private final MeterBar skillXp = new MeterBar();
	private final VitalsSidePanel.Column skillLines = new VitalsSidePanel.Column();
	private final List<WrapLabel> skillPool = new ArrayList<>();

	private final WrapLabel sessionEmpty = new WrapLabel(NOTHING_YET, MUTED);
	private final VitalsSidePanel.Column sessionLines = new VitalsSidePanel.Column();
	private final List<WrapLabel> sessionPool = new ArrayList<>();

	private LiveVitals last = LiveVitals.LOGGED_OUT;
	private volatile boolean open = true;
	/** Text, bar and visibility changes made, for the tests that check an update redraws only what changed. */
	int changes;

	LiveSection(ConfigStore store)
	{
		this.store = store;
		header.setName("live-toggle");
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setForeground(ColorScheme.BRAND_ORANGE);
		header.setFocusable(false);
		header.setContentAreaFilled(false);
		header.setBorderPainted(false);
		header.setHorizontalAlignment(AbstractButton.LEFT);
		header.setBorder(new EmptyBorder(2, 0, 2, 0));
		header.setToolTipText("Fold or open the live readout.");
		header.addActionListener(e -> setOpen(!open, true));
		add(header);

		body.setBorder(new EmptyBorder(0, 0, 4, 0));
		body.add(loggedOut);
		body.add(live);
		add(body);

		live.add(VitalsSidePanel.heading("Combat"));
		live.add(combatOff);
		live.add(combat);
		for (Component c : new Component[]{hitpoints, prayer, energy, targetName, target, training, trainingRate})
		{
			combat.add(c);
		}

		live.add(VitalsSidePanel.heading("Skilling"));
		live.add(skillingOff);
		live.add(skilling);
		skilling.add(skillHeader);
		skilling.add(skillXp);
		skilling.add(skillLines);

		live.add(VitalsSidePanel.heading("This session"));
		live.add(sessionEmpty);
		live.add(sessionLines);

		refresh();
		show(LiveVitals.LOGGED_OUT);
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
	}

	private void setOpen(boolean open, boolean remember)
	{
		this.open = open;
		header.setText((open ? "- " : "+ ") + TITLE);
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
		visible(loggedOut, !v.loggedIn);
		visible(live, v.loggedIn);
		if (v.loggedIn)
		{
			showCombat(v.combat);
			showSkilling(v.skilling);
			visible(sessionEmpty, v.session.isEmpty());
			lines(sessionLines, sessionPool, v.session, Color.WHITE);
		}
		if (changes != before)
		{
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
		text(energy, c.energy);
		text(targetName, c.targetName == null ? NOT_IN_COMBAT : c.targetName);
		bar(target, c.targetName == null ? null : c.target);
		bar(training, c.training);
		text(trainingRate, c.trainingRate);
	}

	private void showSkilling(LiveVitals.Skilling s)
	{
		visible(skillingOff, s == null);
		visible(skilling, s != null);
		if (s == null)
		{
			return;
		}
		text(skillHeader, s.header == null ? NOT_SKILLING : s.header);
		bar(skillXp, s.xp);
		lines(skillLines, skillPool, s.lines, MUTED);
	}

	/** The first {@code texts.size()} labels of the pool show the lines; the rest hide. */
	private void lines(VitalsSidePanel.Column parent, List<WrapLabel> pool, List<String> texts, Color colour)
	{
		while (pool.size() < texts.size())
		{
			final WrapLabel l = new WrapLabel("", colour);
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
}
