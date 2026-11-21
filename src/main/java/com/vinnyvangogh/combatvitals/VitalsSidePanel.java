package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.util.ImageUtil;

/**
 * The plugin's sidebar panel: a title with Hide guides, the live vitals as
 * cards ({@link LiveSection}), then presets, then five tabs (Combat, Targets,
 * Skilling, Alerts, Look) under a segmented control, each opening with its
 * guide as a boxed card ({@link GuideCard}) and its settings in cards that
 * fold, the seldom-changed ones behind "More settings".
 * Every input is a setting from RuneLite's list, same key and same value
 * ({@link SettingControls}); where each sits is {@link PanelLayout}'s. How it
 * looks is {@link PanelStyle}'s.
 *
 * <p>Patterns from GIM Utilities' panel (VinnyVanGogh/group-ironman-plugin):
 * tabs swapped with a {@link CardLayout} under a row of toggle buttons
 * ({@code GroupIronmanPanel.tabBar}), cards on DARKER_GRAY with 6 px padding
 * ({@code GroupIronmanPanel.section}), sections that fold from their header
 * ({@code GroupSettingsTab.collapsible}), a left-aligning {@link Column}
 * ({@code GroupIronmanPanel.Column}) and small toggle chips
 * ({@code GroupIronmanPanel.chip}).
 *
 * <p>Swing thread only. Config writes leave it through the {@link ConfigStore};
 * {@link #refresh} redraws from the stored values after any change, made
 * here or in RuneLite's list.
 */
final class VitalsSidePanel extends PluginPanel
{
	/** What the panel's buttons ask of the plugin. */
	interface Actions
	{
		/** Plays one alert as it is set up now, off the Swing thread. */
		void testAlert(ActivityAlerts.Type type);
	}

	static final String OVERLAYS = "Overlays";
	static final String SHOW_ALL = "Show every overlay";
	static final String HIDE_ALL = "Hide every overlay";
	static final String SESSION = "Session";
	static final String RESET_SESSION = "Reset session";
	static final String TITLE = "Combat & Skilling Vitals";
	static final String ADVANCED_NOTE = "Timeouts and update notes are in RuneLite's settings list for this "
		+ "plugin, under Advanced.";
	/** Hidden setting: the guides folded by the user, "PRESETS,COMBAT"; every other guide is open. */
	static final String GUIDES_CLOSED_KEY = "sidebarGuidesClosed";
	/** Hidden setting: every guide hidden from the header's Hide guides. */
	static final String GUIDES_HIDDEN_KEY = "sidebarGuidesHidden";
	static final String HIDE_GUIDES = "Hide guides";
	static final String SHOW_GUIDES = "Show guides";
	static final String MORE = "More settings";
	static final String PRESETS_GUIDE = "PRESETS";
	/** How many of a tab's setting cards start open; the rest start folded. */
	static final int OPEN_GROUPS = 3;
	/** Every setting row is at least this tall, so rows line up from card to card. */
	static final int ROW_HEIGHT = 24;

	private final ConfigStore store;
	private final SettingControls controls;
	private final Actions actions;
	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);
	private final Map<PanelLayout.Tab, JToggleButton> tabButtons = new EnumMap<>(PanelLayout.Tab.class);
	/** Shown or hidden from the settings on every refresh, e.g. per-alert sounds while one is shared. */
	private final List<Runnable> visibility = new ArrayList<>();
	private final Column presetBody = new Column();
	private final List<GuideCard> guides = new ArrayList<>();
	private final Set<String> closedGuides = new LinkedHashSet<>();
	private final JButton hideGuides = linkButton(HIDE_GUIDES, "Hide or show the guide at the top of each tab.");
	private final LiveSection live;
	private final WalkthroughCard tour;
	private final MuteBar muteBar;
	private final GoalsPage goalsPage;
	private final ButtonGroup tabGroup = new ButtonGroup();
	/** Hidden setting: the last open main tab, a {@link Main} name. */
	static final String TAB_KEY = "sidebarTab";
	static final String SOON = "Coming soon.";
	private final CardLayout mainCards = new CardLayout();
	private final JPanel mainPanel = new JPanel(mainCards);
	private final Map<Main, Segment> mainButtons = new EnumMap<>(Main.class);
	private final ButtonGroup mainGroup = new ButtonGroup();
	private Main main = Main.LIVE;

	/** The sidebar's tabs, in the order of the tab bar; each has an icon bundled under /icons. */
	enum Main
	{
		LIVE("Live", "tab_live.png"),
		GOALS("Goals", "tab_goals.png"),
		MAX_HIT("Max Hit", "tab_maxhit.png"),
		OVERLAYS("Overlays", "tab_overlays.png"),
		PRESETS("Presets", "tab_presets.png"),
		SETTINGS("Settings", "tab_settings.png");

		final String label;
		final String icon;

		Main(String label, String icon)
		{
			this.label = label;
			this.icon = icon;
		}

		static Main of(String name)
		{
			for (Main m : values())
			{
				if (m.name().equals(name))
				{
					return m;
				}
			}
			return LIVE;
		}
	}
	/** The sidebar has this panel open; set on the Swing thread, read on the client thread. */
	private volatile boolean shown;
	private Presets.Preset previewing;
	private Presets.Undo undo;
	private String note;

	VitalsSidePanel(ConfigStore store, Map<String, ConfigItemDescriptor> items, ColorPickerManager pickers,
		Actions actions)
	{
		this(store, items, pickers, actions, new AlertMute());
	}

	VitalsSidePanel(ConfigStore store, Map<String, ConfigItemDescriptor> items, ColorPickerManager pickers,
		Actions actions, AlertMute mute)
	{
		this(store, items, pickers, actions, mute, new GoalBook(new GoalBook.Storage()
		{
			private String kept;

			@Override
			public String load()
			{
				return kept;
			}

			@Override
			public void save(String text)
			{
				kept = text;
			}

			@Override
			public void unset()
			{
				kept = null;
			}
		}), done -> done.accept(new MaxHitPage.Snapshot()));
	}

	/**
	 * {@code mute} is the plugin's, so a panel built again shows the mute as it stands. {@code goals} and
	 * {@code gear} are what the Goals and Max Hit pages read.
	 */
	VitalsSidePanel(ConfigStore store, Map<String, ConfigItemDescriptor> items, ColorPickerManager pickers,
		Actions actions, AlertMute mute, GoalBook goals, MaxHitPage.Source gear)
	{
		this(store, items, pickers, actions, mute, goals, gear, null);
	}

	/** {@code icons} gives the Goals page's skill dropdown each skill's icon. */
	VitalsSidePanel(ConfigStore store, Map<String, ConfigItemDescriptor> items, ColorPickerManager pickers,
		Actions actions, AlertMute mute, GoalBook goals, MaxHitPage.Source gear,
		java.util.function.Function<net.runelite.api.Skill, java.awt.image.BufferedImage> icons)
	{
		this.goalsPage = new GoalsPage(goals, icons);
		this.muteBar = new MuteBar(mute);
		this.store = store;
		this.controls = new SettingControls(store, items, pickers);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final Column root = new Column();
		root.add(header());
		tour = new WalkthroughCard(step -> display(step.tab), () -> store.set(Walkthrough.SEEN_KEY, Walkthrough.REVISION));
		root.add(tour);
		live = new LiveSection(store);
		root.add(mainBar());
		mainPanel.setOpaque(false);
		addMain(Main.LIVE, withHelp(Main.LIVE, live));
		root.add(mainPanel);
		final Column settings = new Column();
		settings.add(tabBar());
		cardPanel.setOpaque(false);
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			// A card is as tall as the tallest tab; pinned to the top, a shorter tab keeps its rows' own
			// heights instead of BoxLayout stretching every dropdown and spinner row to fill it.
			final JPanel card = new JPanel(new BorderLayout());
			card.setOpaque(false);
			card.add(tab(tab), BorderLayout.NORTH);
			cardPanel.add(card, tab.name());
		}
		settings.add(cardPanel);
		addMain(Main.GOALS, goalsPage);
		addMain(Main.MAX_HIT, new MaxHitPage(gear, icons));
		final Column overlayTab = new Column();
		overlayTab.add(overlays());
		overlayTab.add(text("The look of each overlay, the pillars and the casts overlay are under Settings.",
			PanelStyle.MUTED));
		addMain(Main.OVERLAYS, withHelp(Main.OVERLAYS, overlayTab));
		addMain(Main.PRESETS, presets());
		addMain(Main.SETTINGS, settings);
		add(root, BorderLayout.NORTH);
		show(PanelLayout.Tab.COMBAT);
		display(Main.of(store.get(TAB_KEY, String.class)));
		refresh();
		if (Walkthrough.due(store.get(Walkthrough.SEEN_KEY, Integer.class)))
		{
			tour.start(0);
		}
	}

	/** A page under a short "? Guide" link that plays the tour from that page's step. */
	private JComponent withHelp(Main m, JComponent page)
	{
		final JButton help = linkButton("? Guide", "Play the tour of this panel, from this page.");
		help.setName("guide-link-" + m.name());
		help.addActionListener(e -> tour.start(Walkthrough.stepFor(m)));
		final JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.add(help, BorderLayout.WEST);
		final Column c = new Column();
		c.add(row);
		c.add(page);
		return c;
	}

	/** Redraws every input and what depends on them from the stored settings. */
	void refresh()
	{
		live.refresh();
		muteBar.refresh();
		controls.loadAll();
		for (Runnable r : visibility)
		{
			r.run();
		}
		drawGuides();
		drawPresets();
		revalidate();
		repaint();
	}

	/** Shows a tick's vitals in the Live section. Swing thread. */
	void live(LiveVitals vitals)
	{
		live.update(vitals);
	}

	/**
	 * Whether a tick's vitals would be seen: the panel is open in the sidebar
	 * and its Live section unfolded. The plugin builds none otherwise. Any thread.
	 */
	boolean wantsLive()
	{
		return shown && live.isOpen();
	}

	@Override
	public void onActivate()
	{
		shown = true;
		muteBar.start();
	}

	@Override
	public void onDeactivate()
	{
		shown = false;
		muteBar.stop();
	}

	/** Opens a settings tab, under the Settings main tab. */
	void show(PanelLayout.Tab tab)
	{
		cards.show(cardPanel, tab.name());
		tabButtons.get(tab).setSelected(true);
		select(Main.SETTINGS, false);
	}

	/** Opens a main tab and remembers it. */
	void showMain(Main m)
	{
		select(m, true);
	}

	/** Opens a main tab without remembering it, as the panel does while it is built. */
	void display(Main m)
	{
		select(m, false);
	}

	Main main()
	{
		return main;
	}

	private void select(Main m, boolean remember)
	{
		main = m;
		mainCards.show(mainPanel, m.name());
		mainButtons.get(m).setSelected(true);
		if (m == Main.GOALS)
		{
			goalsPage.refresh();
		}
		if (remember)
		{
			store.set(TAB_KEY, m.name());
		}
		revalidate();
		repaint();
	}

	private void addMain(Main m, JComponent page)
	{
		final JPanel card = new JPanel(new BorderLayout());
		card.setOpaque(false);
		card.add(page, BorderLayout.NORTH);
		mainPanel.add(card, m.name());
	}

	/** The main tabs as the same segmented strip as the settings tabs, three to a row, each with its icon. */
	private JComponent mainBar()
	{
		final JPanel bar = new JPanel(new GridLayout(0, 3, 0, PanelStyle.XS))
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				final Graphics2D g2 = PanelStyle.smooth(g);
				try
				{
					g2.setColor(PanelStyle.CARD);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), PanelStyle.ARC, PanelStyle.ARC);
				}
				finally
				{
					g2.dispose();
				}
			}
		};
		bar.setOpaque(false);
		bar.setBorder(new EmptyBorder(PanelStyle.XS, PanelStyle.XS, PanelStyle.XS, PanelStyle.XS));
		for (Main m : Main.values())
		{
			final Segment b = new Segment(m.label);
			b.setName("main-" + m.name());
			final BufferedImage icon = ImageUtil.loadImageResource(VitalsSidePanel.class, "/com/vinnyvangogh/combatvitals/icons/"
				+ m.icon);
			b.setIcon(new ImageIcon(icon));
			b.setIconTextGap(PanelStyle.XS);
			b.setBorder(new EmptyBorder(PanelStyle.S, PanelStyle.XS, PanelStyle.S, PanelStyle.XS));
			b.addActionListener(e -> showMain(m));
			mainGroup.add(b);
			mainButtons.put(m, b);
			bar.add(b);
		}
		final Column c = new Column();
		c.setBorder(new EmptyBorder(0, 0, PanelStyle.M, 0));
		c.add(bar);
		return c;
	}

	/** Redraws the Goals page when it is the one showing; Swing thread. */
	void goalsChanged()
	{
		if (shown && main == Main.GOALS)
		{
			goalsPage.refresh();
		}
	}

	// ---- Header and guides ----

	private JComponent header()
	{
		final Column c = new Column();
		c.setBorder(new EmptyBorder(0, 0, PanelStyle.M, 0));
		final JLabel title = new JLabel(TITLE);
		title.setFont(PanelStyle.title());
		title.setForeground(PanelStyle.TEXT);
		c.add(title);
		final JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		hideGuides.setName("hide-guides");
		hideGuides.addActionListener(e ->
		{
			// From what is showing, not the store: the write lands later, off the Swing thread.
			final boolean hide = HIDE_GUIDES.equals(hideGuides.getText());
			store.set(GUIDES_HIDDEN_KEY, hide);
			showGuides(hide);
			if (!hide)
			{
				tour.start(0);
			}
		});
		row.add(hideGuides, BorderLayout.EAST);
		c.add(row);
		c.add(muteBar);
		return c;
	}

	private GuideCard guide(String id, String title, PanelGuide.Page page)
	{
		final GuideCard g = new GuideCard(id, title, page);
		g.onToggle(open ->
		{
			if (open)
			{
				closedGuides.remove(id);
			}
			else
			{
				closedGuides.add(id);
			}
			store.set(GUIDES_CLOSED_KEY, String.join(",", closedGuides));
		});
		guides.add(g);
		return g;
	}

	private boolean guidesHidden()
	{
		return Boolean.TRUE.equals(store.get(GUIDES_HIDDEN_KEY, Boolean.class));
	}

	private void drawGuides()
	{
		guides(guidesHidden(), store.get(GUIDES_CLOSED_KEY, String.class));
	}

	/**
	 * Each guide open unless the user folded it, all hidden while Hide guides
	 * is on. The plugin calls this alone after a guide setting changes.
	 *
	 * @param closed the folded guides' ids, "PRESETS,COMBAT"; null or empty for none
	 */
	void guides(boolean hidden, String closed)
	{
		closedGuides.clear();
		if (closed != null)
		{
			for (String id : closed.split(","))
			{
				if (!id.isEmpty())
				{
					closedGuides.add(id);
				}
			}
		}
		showGuides(hidden);
	}

	private void showGuides(boolean hidden)
	{
		hideGuides.setText(hidden ? SHOW_GUIDES : HIDE_GUIDES);
		for (GuideCard g : guides)
		{
			g.setVisible(!hidden);
			g.setOpen(!closedGuides.contains(g.id));
		}
		revalidate();
		repaint();
	}

	// ---- Presets ----

	/** One switch per overlay, and for all of them; each is the setting of the same name, so both views agree. */
	private JComponent overlays()
	{
		final FoldCard card = new FoldCard(OVERLAYS, null, null, true);
		for (OverlayToggles.Toggle t : OverlayToggles.TOGGLES)
		{
			final JCheckBox box = new JCheckBox(t.label);
			box.setName("overlay-" + t.key);
			box.setFont(PanelStyle.body());
			box.setForeground(Color.WHITE);
			box.setOpaque(false);
			box.setFocusable(false);
			box.addActionListener(e -> OverlayToggles.set(store, t, box.isSelected()));
			controls.track(new SettingControls.Bound()
			{
				@Override
				public JComponent view()
				{
					return box;
				}

				@Override
				public void load()
				{
					box.setSelected(OverlayToggles.isOn(store, t));
				}
			});
			card.body.add(settingRow(box));
		}
		final JPanel all = new JPanel(new GridLayout(1, 2, PanelStyle.S, PanelStyle.S));
		all.setOpaque(false);
		all.add(button(SHOW_ALL, "Show every overlay.", () -> OverlayToggles.setAll(store, true)));
		all.add(button(HIDE_ALL, "Hide every overlay.", () -> OverlayToggles.setAll(store, false)));
		card.body.add(all);
		return card;
	}

	private JComponent presets()
	{
		final Column c = new Column();
		c.add(guide(PRESETS_GUIDE, "Guide: presets", PanelGuide.PRESETS));
		final Card card = Card.of(null);
		card.add(heading("Presets"));
		final JPanel buttons = new JPanel(new GridLayout(0, 2, PanelStyle.S, PanelStyle.S));
		buttons.setOpaque(false);
		for (Presets.Preset p : Presets.Preset.values())
		{
			buttons.add(button(p.label, p.summary, () ->
			{
				previewing = p;
				note = null;
				drawPresets();
			}));
		}
		card.add(buttons);
		card.add(presetBody);
		c.add(card);
		return c;
	}

	/** The preview of the chosen preset, with Apply, and Undo once one was applied. */
	private void drawPresets()
	{
		presetBody.removeAll();
		if (previewing != null)
		{
			drawPreview(previewing);
		}
		if (note != null)
		{
			presetBody.add(text(note, PanelStyle.ACCENT));
		}
		if (undo != null)
		{
			presetBody.add(button("Undo " + undo.preset.label, "Put back the settings " + undo.preset.label
				+ " changed, as they were before.", this::undo));
		}
		presetBody.revalidate();
		presetBody.repaint();
	}

	private void drawPreview(Presets.Preset p)
	{
		final Map<String, String[]> changes = Presets.changes(p, store);
		presetBody.add(heading(p.label + ": what changes"));
		presetBody.add(text(p.summary, PanelStyle.MUTED));
		if (changes.isEmpty())
		{
			presetBody.add(text("Already set: nothing would change.", PanelStyle.TEXT));
		}
		for (Map.Entry<String, String[]> e : changes.entrySet())
		{
			presetBody.add(text(controls.name(e.getKey()) + ": " + shownValue(e.getKey(), e.getValue()[0])
				+ " -> " + shownValue(e.getKey(), e.getValue()[1]), PanelStyle.TEXT));
		}
		final JPanel row = new JPanel(new GridLayout(1, 2, PanelStyle.S, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(PanelStyle.S, 0, 0, 0));
		final JButton apply = button("Apply", "Change the settings listed above.", () -> apply(p));
		apply.setEnabled(!changes.isEmpty());
		row.add(apply);
		row.add(button("Cancel", null, () ->
		{
			previewing = null;
			drawPresets();
		}));
		presetBody.add(row);
	}

	private void apply(Presets.Preset p)
	{
		undo = Presets.apply(p, store);
		previewing = null;
		note = "Applied " + p.label + ".";
		drawPresets();
	}

	private void undo()
	{
		if (undo == null)
		{
			return;
		}
		undo.restore(store);
		note = "Undid " + undo.preset.label + ".";
		undo = null;
		drawPresets();
	}

	/** A stored value as the panel words it: on, off, an enum's own name, or the text. */
	String shownValue(String key, String raw)
	{
		if (raw == null)
		{
			return "default";
		}
		if ("true".equals(raw) || "false".equals(raw))
		{
			return "true".equals(raw) ? "on" : "off";
		}
		final Class<?> type = controls.enumType(key);
		if (type != null)
		{
			for (Object constant : type.getEnumConstants())
			{
				if (((Enum<?>) constant).name().equals(raw))
				{
					return constant.toString();
				}
			}
		}
		return raw;
	}

	// ---- Tabs ----

	/** The tabs as one segmented control: a rounded strip, the active tab raised and underlined in orange. */
	private JComponent tabBar()
	{
		final JPanel bar = new JPanel(new GridLayout(1, 0, 0, 0))
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				final Graphics2D g2 = PanelStyle.smooth(g);
				try
				{
					g2.setColor(PanelStyle.CARD);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), PanelStyle.ARC, PanelStyle.ARC);
				}
				finally
				{
					g2.dispose();
				}
			}
		};
		bar.setOpaque(false);
		bar.setBorder(new EmptyBorder(PanelStyle.XS, PanelStyle.XS, PanelStyle.XS, PanelStyle.XS));
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			final Segment b = new Segment(tab.label);
			b.addActionListener(e -> show(tab));
			tabGroup.add(b);
			tabButtons.put(tab, b);
			bar.add(b);
		}
		final Column c = new Column();
		c.setBorder(new EmptyBorder(PanelStyle.L, 0, PanelStyle.M, 0));
		c.add(bar);
		return c;
	}

	private JComponent tab(PanelLayout.Tab tab)
	{
		final Column c = new Column();
		c.setName(tab.name());
		c.add(guide(tab.name(), "Guide: " + tab.label, PanelGuide.of(tab)));
		if (tab.master != null)
		{
			final Card master = Card.of(PanelStyle.ACCENT);
			addControl(master, tab.master);
			c.add(master);
		}
		if (tab == PanelLayout.Tab.LOOK)
		{
			final LookPreview preview = new LookPreview(store);
			controls.track(preview);
			final Card box = Card.of(null);
			box.add(preview);
			c.add(box);
		}
		if (tab == PanelLayout.Tab.ALERTS)
		{
			alerts(c);
		}
		groups(c, tab);
		if (tab == PanelLayout.Tab.COMBAT)
		{
			c.add(text(ADVANCED_NOTE, PanelStyle.MUTED));
		}
		return c;
	}

	/** A card per heading, the first {@link #OPEN_GROUPS} open; seldom-changed ones under "More settings". */
	private void groups(Column c, PanelLayout.Tab tab)
	{
		final Column more = new Column();
		int open = 0;
		for (PanelLayout.Group g : PanelLayout.groups(tab))
		{
			final FoldCard card = new FoldCard(g.title, null, null, !g.more && open++ < OPEN_GROUPS);
			for (String key : g.keys)
			{
				addControl(card.body, key);
			}
			if (tab == PanelLayout.Tab.SKILLING && SESSION.equals(g.title))
			{
				final JButton reset = button(RESET_SESSION, "Zero the session count and rates now.",
					() -> store.set("skillingSessionReset", true));
				reset.setName("reset-session");
				card.body.add(reset);
			}
			(g.more ? more : c).add(card);
		}
		if (more.getComponentCount() > 0)
		{
			c.add(moreSettings(more));
		}
	}

	/** "More settings", folded: the tab's seldom-changed cards under it. */
	private static JComponent moreSettings(Column more)
	{
		final Column c = new Column();
		final FoldCard.Header header = new FoldCard.Header(MORE, null);
		header.setForeground(PanelStyle.SOFT);
		header.setBorder(new EmptyBorder(PanelStyle.S, 0, PanelStyle.S, 0));
		more.setVisible(false);
		header.addActionListener(e ->
		{
			more.setVisible(!more.isVisible());
			header.showOpen(more.isVisible());
			c.revalidate();
			c.repaint();
		});
		c.add(header);
		c.add(more);
		return c;
	}

	/** The shared sound, the repeat timing, then a box per alert; per-alert sounds only while not shared. */
	private void alerts(Column c)
	{
		final FoldCard muting = new FoldCard(PanelLayout.MUTING.title, null, null, true);
		for (String key : PanelLayout.MUTING.keys)
		{
			addControl(muting.body, key);
		}
		c.add(muting);
		final FoldCard sound = new FoldCard(PanelLayout.SHARED_SOUND.title, null, null, true);
		final List<String> sharedKeys = PanelLayout.SHARED_SOUND.keys;
		addControl(sound.body, sharedKeys.get(0));
		final Column shared = new Column();
		for (String key : sharedKeys.subList(1, sharedKeys.size()))
		{
			addControl(shared, key);
		}
		sound.body.add(shared);
		c.add(sound);
		final FoldCard repeats = new FoldCard(PanelLayout.REPEATS.title, null, null, false);
		for (String key : PanelLayout.REPEATS.keys)
		{
			addControl(repeats.body, key);
		}
		c.add(repeats);
		final FoldCard each = new FoldCard("Each alert", null, null, true);
		final List<JComponent> own = new ArrayList<>();
		for (PanelLayout.AlertRow r : PanelLayout.ALERTS)
		{
			each.body.add(alertRow(r, own));
		}
		c.add(each);
		visibility.add(() ->
		{
			final boolean one = Boolean.TRUE.equals(store.get(PanelLayout.SOUND_SHARED, boolean.class));
			shared.setVisible(one);
			for (JComponent o : own)
			{
				o.setVisible(!one);
			}
		});
	}

	/**
	 * One alert as its own folded card (Board 2026-11-15: 22 alerts at four rows each made the tab a scroll, and
	 * the Test button beside the switch cut its name short). The title is the alert's full name; inside are its
	 * switch, its own setting, sound, volume, repeat and Test.
	 */
	private JComponent alertRow(PanelLayout.AlertRow r, List<JComponent> own)
	{
		final FoldCard box = new FoldCard(r.label, null, null, false);
		box.setName("alert-" + r.type.name());
		addControl(box.body, r.toggle);
		if (r.extra != null)
		{
			addControl(box.body, r.extra);
		}
		final Column mine = new Column();
		for (String key : new String[]{r.sound, r.volume, r.repeat})
		{
			addControl(mine, key);
		}
		own.add(mine);
		box.body.add(mine);
		final JButton test = button("Test", "Play the " + r.label.toLowerCase() + " alert as it is set up now.",
			() -> actions.testAlert(r.type));
		test.setName("test-" + r.type.name());
		box.body.add(settingRow(test));
		return box;
	}

	private void addControl(JPanel parent, String key)
	{
		final JComponent control = controls.control(key);
		if (control != null)
		{
			parent.add(settingRow(control));
		}
	}

	/** One setting's row: at least {@link #ROW_HEIGHT} tall and never stretched taller. */
	static JComponent settingRow(JComponent control)
	{
		final JPanel row = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getPreferredSize()
			{
				final Dimension d = super.getPreferredSize();
				return new Dimension(d.width, Math.max(ROW_HEIGHT, d.height));
			}

			@Override
			public Dimension getMaximumSize()
			{
				return PanelStyle.fullWidth(this);
			}
		};
		row.setOpaque(false);
		row.add(control, BorderLayout.CENTER);
		return row;
	}

	// ---- Building blocks ----

	/** A tab of the segmented control: soft text, orange and raised while active. */
	static final class Segment extends JToggleButton
	{
		Segment(String label)
		{
			super(label);
			setFont(PanelStyle.body());
			setForeground(PanelStyle.SOFT);
			setFocusable(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setOpaque(false);
			setBorder(new EmptyBorder(PanelStyle.S, 0, PanelStyle.S, 0));
			addItemListener(e -> setForeground(isSelected() ? PanelStyle.ACCENT : PanelStyle.SOFT));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			if (isSelected())
			{
				final Graphics2D g2 = PanelStyle.smooth(g);
				try
				{
					g2.setColor(PanelStyle.INSET);
					g2.fillRoundRect(0, 0, getWidth(), getHeight(), PanelStyle.ARC, PanelStyle.ARC);
					g2.setColor(PanelStyle.ACCENT);
					g2.fillRect(PanelStyle.M, getHeight() - 2, getWidth() - 2 * PanelStyle.M, 2);
				}
				finally
				{
					g2.dispose();
				}
			}
			super.paintComponent(g);
		}
	}

	/** A flat text button, as a link: Hide guides, Show all. */
	static JButton linkButton(String text, String tooltip)
	{
		final JButton b = new JButton(text);
		b.setFont(PanelStyle.body());
		b.setForeground(PanelStyle.SOFT);
		b.setFocusable(false);
		b.setContentAreaFilled(false);
		b.setBorderPainted(false);
		b.setOpaque(false);
		b.setHorizontalAlignment(AbstractButton.LEFT);
		b.setBorder(new EmptyBorder(PanelStyle.XS, 0, PanelStyle.XS, 0));
		b.setToolTipText(tooltip);
		return b;
	}

	private static JButton button(String label, String tooltip, Runnable action)
	{
		final JButton b = new JButton(label);
		b.setFont(PanelStyle.body());
		b.setFocusable(false);
		b.setToolTipText(tooltip);
		b.addActionListener(e -> action.run());
		return b;
	}

	static JLabel heading(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(PanelStyle.title());
		label.setForeground(PanelStyle.TEXT);
		label.setBorder(new EmptyBorder(0, 0, PanelStyle.S, 0));
		return label;
	}

	/** Wraps at the panel's width ({@link WrapLabel}); the text is the plugin's own, escaped all the same. */
	static JLabel text(String text, Color color)
	{
		return new WrapLabel(text, color);
	}

	static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** The sidebar icon: a small health bar over a skilling bar, drawn rather than shipped. */
	static BufferedImage icon()
	{
		final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setColor(new Color(30, 30, 30));
			g.fillRect(1, 3, 14, 4);
			g.fillRect(1, 9, 14, 4);
			g.setColor(new Color(60, 200, 60));
			g.fillRect(2, 4, 9, 2);
			g.setColor(new Color(200, 50, 50));
			g.fillRect(11, 4, 3, 2);
			g.setColor(new Color(80, 160, 230));
			g.fillRect(2, 10, 6, 2);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/**
	 * A vertical BoxLayout panel that left-aligns everything added to it
	 * (GIM Utilities' {@code GroupIronmanPanel.Column}): BoxLayout lines
	 * children up on their alignmentX, JLabel's is left and JPanel's centre.
	 */
	static class Column extends JPanel
	{
		Column()
		{
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(false);
		}

		@Override
		protected void addImpl(Component comp, Object constraints, int index)
		{
			if (comp instanceof JComponent)
			{
				((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
			}
			super.addImpl(comp, constraints, index);
		}
	}
}
