package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

/**
 * The plugin's sidebar panel: presets at the top, then five tabs (Combat,
 * Targets, Skilling, Alerts, Look), each opening with its own short guide.
 * Every input is a setting from RuneLite's list, same key and same value
 * ({@link SettingControls}); where each sits is {@link PanelLayout}'s.
 *
 * <p>Patterns from GIM Utilities' panel (VinnyVanGogh/group-ironman-plugin):
 * tabs swapped with a {@link CardLayout} under a row of toggle buttons
 * ({@code GroupIronmanPanel}), headings and wrapping text in RuneScape fonts
 * ({@code GroupIronmanPanel.heading/text}), a left-aligning {@link Column}
 * ({@code GroupIronmanPanel.Column}), and sections that fold from their
 * header ({@code GroupSettingsTab.collapsible}).
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

	static final String TITLE = "Combat & Skilling Vitals";
	static final String ADVANCED_NOTE = "Timeouts and update notes are in RuneLite's settings list for this "
		+ "plugin, under Advanced.";
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR.darker();

	private final ConfigStore store;
	private final SettingControls controls;
	private final Actions actions;
	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);
	private final Map<PanelLayout.Tab, JToggleButton> tabButtons = new EnumMap<>(PanelLayout.Tab.class);
	/** Shown or hidden from the settings on every refresh, e.g. per-alert sounds while one is shared. */
	private final List<Runnable> visibility = new ArrayList<>();
	private final Column presetBody = new Column();
	private Presets.Preset previewing;
	private Presets.Undo undo;
	private String note;

	VitalsSidePanel(ConfigStore store, Map<String, ConfigItemDescriptor> items, ColorPickerManager pickers,
		Actions actions)
	{
		this.store = store;
		this.controls = new SettingControls(store, items, pickers);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final Column root = new Column();
		root.setOpaque(false);
		root.add(title());
		root.add(presets());
		root.add(tabBar());
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
		root.add(cardPanel);
		add(root, BorderLayout.NORTH);
		show(PanelLayout.Tab.COMBAT);
		refresh();
	}

	/** Redraws every input and what depends on them from the stored settings. */
	void refresh()
	{
		controls.loadAll();
		for (Runnable r : visibility)
		{
			r.run();
		}
		drawPresets();
		revalidate();
		repaint();
	}

	void show(PanelLayout.Tab tab)
	{
		cards.show(cardPanel, tab.name());
		tabButtons.get(tab).setSelected(true);
		revalidate();
		repaint();
	}

	// ---- Title and presets ----

	private static JComponent title()
	{
		final JLabel title = new JLabel(TITLE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 0, 4, 0));
		return title;
	}

	private JComponent presets()
	{
		final Column c = new Column();
		c.add(heading("Presets"));
		guide(c, "Guide: presets", PanelGuide.PRESETS);
		final JPanel buttons = new JPanel(new GridLayout(0, 2, 2, 2));
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
		c.add(buttons);
		c.add(presetBody);
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
			presetBody.add(text(note, ColorScheme.BRAND_ORANGE));
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
		presetBody.add(text(p.summary, MUTED));
		if (changes.isEmpty())
		{
			presetBody.add(text("Already set: nothing would change.", Color.WHITE));
		}
		for (Map.Entry<String, String[]> e : changes.entrySet())
		{
			presetBody.add(text(controls.name(e.getKey()) + ": " + shownValue(e.getKey(), e.getValue()[0])
				+ " -> " + shownValue(e.getKey(), e.getValue()[1]), Color.WHITE));
		}
		final JPanel row = new JPanel(new GridLayout(1, 2, 2, 0));
		row.setOpaque(false);
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

	private JComponent tabBar()
	{
		final JPanel bar = new JPanel(new GridLayout(0, 3, 2, 2));
		bar.setOpaque(false);
		bar.setBorder(new EmptyBorder(8, 0, 4, 0));
		final ButtonGroup group = new ButtonGroup();
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			final JToggleButton b = new JToggleButton(tab.label);
			b.setFont(FontManager.getRunescapeSmallFont());
			b.setFocusable(false);
			b.addActionListener(e -> show(tab));
			group.add(b);
			tabButtons.put(tab, b);
			bar.add(b);
		}
		return bar;
	}

	private JComponent tab(PanelLayout.Tab tab)
	{
		final Column c = new Column();
		c.setName(tab.name());
		guide(c, "Guide: " + tab.label, PanelGuide.of(tab));
		if (tab.master != null)
		{
			addControl(c, tab.master);
		}
		if (tab == PanelLayout.Tab.LOOK)
		{
			final LookPreview preview = new LookPreview(store);
			controls.track(preview);
			c.add(preview);
		}
		if (tab == PanelLayout.Tab.ALERTS)
		{
			alerts(c);
		}
		for (PanelLayout.Group g : PanelLayout.groups(tab))
		{
			c.add(heading(g.title));
			for (String key : g.keys)
			{
				addControl(c, key);
			}
		}
		if (tab == PanelLayout.Tab.COMBAT)
		{
			c.add(text(ADVANCED_NOTE, MUTED));
		}
		return c;
	}

	/** The shared sound, the repeat timing, then a row per alert; per-alert sounds only while not shared. */
	private void alerts(Column c)
	{
		c.add(heading(PanelLayout.SHARED_SOUND.title));
		final List<String> sharedKeys = PanelLayout.SHARED_SOUND.keys;
		addControl(c, sharedKeys.get(0));
		final Column shared = new Column();
		for (String key : sharedKeys.subList(1, sharedKeys.size()))
		{
			addControl(shared, key);
		}
		c.add(shared);
		c.add(heading(PanelLayout.REPEATS.title));
		for (String key : PanelLayout.REPEATS.keys)
		{
			addControl(c, key);
		}
		c.add(heading("Each alert"));
		final List<JComponent> own = new ArrayList<>();
		for (PanelLayout.AlertRow r : PanelLayout.ALERTS)
		{
			c.add(alertRow(r, own));
		}
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

	private JComponent alertRow(PanelLayout.AlertRow r, List<JComponent> own)
	{
		final Column box = new Column();
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(3, 0, 3, 0)));
		final JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		final JComponent toggle = controls.control(r.toggle);
		if (toggle != null)
		{
			top.add(toggle, BorderLayout.CENTER);
		}
		final JButton test = button("Test", "Play the " + r.label.toLowerCase() + " alert as it is set up now.",
			() -> actions.testAlert(r.type));
		test.setName("test-" + r.type.name());
		top.add(test, BorderLayout.EAST);
		box.add(top);
		if (r.extra != null)
		{
			addControl(box, r.extra);
		}
		final Column mine = new Column();
		for (String key : new String[]{r.sound, r.volume, r.repeat})
		{
			addControl(mine, key);
		}
		own.add(mine);
		box.add(mine);
		return box;
	}

	private void addControl(JPanel parent, String key)
	{
		final JComponent control = controls.control(key);
		if (control != null)
		{
			parent.add(control);
		}
	}

	/** A guide page that folds open from its header, closed until asked for. */
	private static void guide(Column parent, String title, PanelGuide.Page page)
	{
		final Column body = new Column();
		body.setBorder(new EmptyBorder(0, 6, 4, 0));
		body.add(text("What it does: " + page.what, Color.WHITE));
		body.add(text("How to use it: " + page.how, Color.WHITE));
		for (int i = 0; i < page.walkthrough.size(); i++)
		{
			body.add(text((i + 1) + ". " + page.walkthrough.get(i), MUTED));
		}
		body.setVisible(false);
		final JButton header = new JButton("+ " + title);
		header.setFont(FontManager.getRunescapeSmallFont());
		header.setForeground(ColorScheme.BRAND_ORANGE);
		header.setFocusable(false);
		header.setContentAreaFilled(false);
		header.setBorderPainted(false);
		header.setHorizontalAlignment(AbstractButton.LEFT);
		header.setBorder(new EmptyBorder(2, 0, 2, 0));
		header.addActionListener(e ->
		{
			body.setVisible(!body.isVisible());
			header.setText((body.isVisible() ? "- " : "+ ") + title);
			parent.revalidate();
			parent.repaint();
		});
		parent.add(header);
		parent.add(body);
	}

	// ---- Building blocks (after GroupIronmanPanel's) ----

	private static JButton button(String label, String tooltip, Runnable action)
	{
		final JButton b = new JButton(label);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setFocusable(false);
		b.setToolTipText(tooltip);
		b.addActionListener(e -> action.run());
		return b;
	}

	static JLabel heading(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setBorder(new EmptyBorder(6, 0, 2, 0));
		return label;
	}

	/** Wraps at the panel's width; the text is the plugin's own, escaped all the same. */
	static JLabel text(String text, Color color)
	{
		final JLabel label = new JLabel("<html><body style='width:170px'>" + escape(text) + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setBorder(new EmptyBorder(1, 0, 1, 0));
		return label;
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
	static final class Column extends JPanel
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
