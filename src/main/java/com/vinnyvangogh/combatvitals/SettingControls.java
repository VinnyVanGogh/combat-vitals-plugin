package com.vinnyvangogh.combatvitals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ColorUtil;

/**
 * The sidebar panel's inputs, one per setting, built from RuneLite's own
 * description of it ({@link ConfigItemDescriptor}: name, tooltip, type,
 * range), so a setting reads the same here as in RuneLite's list. Each input
 * writes its key through the {@link ConfigStore} and redraws from it in
 * {@link #loadAll}; a redraw never writes back.
 *
 * <p>Swing thread only.
 */
final class SettingControls
{
	/** An input bound to one key. */
	interface Bound
	{
		JComponent view();

		/** Shows the stored value. */
		void load();
	}

	private static final Color TEXT = Color.WHITE;

	private final ConfigStore store;
	private final Map<String, ConfigItemDescriptor> items;
	/** Null in tests: the colour button then does nothing. */
	private final ColorPickerManager pickers;
	private final List<Bound> bound = new ArrayList<>();
	private boolean loading;

	SettingControls(ConfigStore store, Map<String, ConfigItemDescriptor> items, ColorPickerManager pickers)
	{
		this.store = store;
		this.items = items;
		this.pickers = pickers;
	}

	/** The setting's name in RuneLite's list, or the key when there is none. */
	String name(String key)
	{
		final ConfigItemDescriptor d = items.get(key);
		return d == null ? key : d.name();
	}

	/** The setting's enum type, or null when it is not an enum or the config has no such key. */
	Class<?> enumType(String key)
	{
		final ConfigItemDescriptor d = items.get(key);
		return d == null ? null : asEnum(d.getType());
	}

	/** The type as an enum class, or null when it is not one. */
	private static Class<?> asEnum(Object type)
	{
		return type instanceof Class && ((Class<?>) type).isEnum() ? (Class<?>) type : null;
	}

	/** Every bound input shows its stored value; no listener writes meanwhile. */
	void loadAll()
	{
		load(bound);
	}

	private void load(List<Bound> which)
	{
		loading = true;
		try
		{
			for (Bound b : which)
			{
				b.load();
			}
		}
		finally
		{
			loading = false;
		}
	}

	/** Adds something that redraws with the inputs, like a preview. */
	void track(Bound b)
	{
		bound.add(b);
		load(Collections.singletonList(b));
	}

	/** The input for {@code key}, or null when the config has no such key. */
	JComponent control(String key)
	{
		final ConfigItemDescriptor d = items.get(key);
		if (d == null)
		{
			return null;
		}
		final Bound b = bind(d);
		track(b);
		return b.view();
	}

	private Bound bind(ConfigItemDescriptor d)
	{
		final Object type = d.getType();
		if (type == boolean.class || type == Boolean.class)
		{
			return checkbox(d);
		}
		if (type == Notification.class)
		{
			return notificationBox(d);
		}
		if (type == int.class || type == Integer.class)
		{
			return spinner(d);
		}
		if (type == Color.class)
		{
			return colour(d);
		}
		final Class<?> enumType = asEnum(type);
		if (enumType != null)
		{
			return choice(d, enumType);
		}
		if (type == String.class)
		{
			return text(d);
		}
		final JLabel unsupported = label(d.name());
		return bound(unsupported, () ->
		{
		});
	}

	private Bound checkbox(ConfigItemDescriptor d)
	{
		final String key = d.key();
		final JCheckBox box = new JCheckBox(d.name());
		small(box, d);
		box.addActionListener(e -> write(key, box.isSelected()));
		return bound(box, () -> box.setSelected(Boolean.TRUE.equals(store.get(key, boolean.class))));
	}

	/** An alert's RuneLite notification: ticking it on or off keeps the rest of the notification as it was. */
	private Bound notificationBox(ConfigItemDescriptor d)
	{
		final String key = d.key();
		final JCheckBox box = new JCheckBox(d.name());
		small(box, d);
		box.addActionListener(e ->
		{
			final Notification now = store.get(key, Notification.class);
			write(key, (now == null ? Notification.ON : now).withEnabled(box.isSelected()));
		});
		return bound(box, () ->
		{
			final Notification n = store.get(key, Notification.class);
			box.setSelected(n != null && n.isEnabled());
		});
	}

	private Bound spinner(ConfigItemDescriptor d)
	{
		final String key = d.key();
		final Range range = d.getRange();
		final int min = range == null ? 0 : range.min();
		final int max = range == null ? Integer.MAX_VALUE : range.max();
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
		spinner.setPreferredSize(new Dimension(64, spinner.getPreferredSize().height));
		spinner.setToolTipText(d.getItem().description());
		spinner.addChangeListener(e -> write(key, ((Number) spinner.getValue()).intValue()));
		return bound(row(d, spinner), () ->
		{
			final Integer v = store.get(key, int.class);
			spinner.setValue(v == null ? min : Math.max(min, Math.min(max, v)));
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Bound choice(ConfigItemDescriptor d, Class<?> type)
	{
		final String key = d.key();
		final JComboBox box = new JComboBox(type.getEnumConstants());
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setToolTipText(d.getItem().description());
		box.addActionListener(e -> write(key, box.getSelectedItem()));
		return bound(row(d, box), () ->
		{
			final Object v = store.get(key, type);
			if (v != null)
			{
				box.setSelectedItem(v);
			}
		});
	}

	/** A line of text, written on Enter and when the field loses focus. */
	private Bound text(ConfigItemDescriptor d)
	{
		final String key = d.key();
		final JTextField field = new JTextField(8);
		field.setToolTipText(d.getItem().description());
		field.addActionListener(e -> write(key, field.getText()));
		field.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				write(key, field.getText());
			}
		});
		return bound(row(d, field), () ->
		{
			final String v = store.get(key, String.class);
			field.setText(v == null ? "" : v);
		});
	}

	private Bound colour(ConfigItemDescriptor d)
	{
		final String key = d.key();
		final JButton swatch = new JButton();
		swatch.setFont(FontManager.getRunescapeSmallFont());
		swatch.setFocusable(false);
		swatch.setToolTipText(d.getItem().description());
		swatch.addActionListener(e -> pick(d, swatch));
		return bound(row(d, swatch), () -> paintSwatch(swatch, store.get(key, Color.class)));
	}

	/** RuneLite's own colour picker, as its settings list opens it. */
	private void pick(ConfigItemDescriptor d, JButton swatch)
	{
		if (pickers == null)
		{
			return;
		}
		final Color now = store.get(d.key(), Color.class);
		final RuneliteColorPicker picker = pickers.create(SwingUtilities.windowForComponent(swatch),
			now == null ? Color.WHITE : now, d.name(), d.getAlpha() == null);
		picker.setLocationRelativeTo(swatch);
		picker.setOnColorChange(c -> paintSwatch(swatch, c));
		picker.setOnClose(c -> write(d.key(), c));
		picker.setVisible(true);
	}

	private static void paintSwatch(JButton swatch, Color c)
	{
		final Color shown = c == null ? Color.GRAY : c;
		swatch.setBackground(shown);
		swatch.setForeground(luminance(shown) > 140 ? Color.BLACK : Color.WHITE);
		swatch.setText(ColorUtil.toHexColor(shown).toUpperCase());
	}

	private static int luminance(Color c)
	{
		return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
	}

	private void write(String key, Object value)
	{
		if (!loading && value != null)
		{
			store.set(key, value);
		}
	}

	private static Bound bound(JComponent view, Runnable load)
	{
		return new Bound()
		{
			@Override
			public JComponent view()
			{
				return view;
			}

			@Override
			public void load()
			{
				load.run();
			}
		};
	}

	/** The name on the left, the input on the right, as RuneLite's list lays a setting out. */
	private static JPanel row(ConfigItemDescriptor d, JComponent input)
	{
		final JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		final JLabel name = label(d.name());
		name.setToolTipText(d.getItem().description());
		row.add(name, BorderLayout.CENTER);
		row.add(input, BorderLayout.EAST);
		return row;
	}

	private static JLabel label(String text)
	{
		final JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(TEXT);
		return l;
	}

	private static void small(JCheckBox box, ConfigItemDescriptor d)
	{
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setForeground(TEXT);
		box.setOpaque(false);
		box.setToolTipText(d.getItem().description());
	}
}
