package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

/**
 * What the sidebar panel's tests stand on: RuneLite's description of every
 * setting, built the way {@code ConfigManager.getConfigDescriptor} builds it
 * (test code may use reflection; the plugin never does), and an in-memory
 * {@link ConfigStore} holding every setting's default, as RuneLite stores them
 * when the plugin loads.
 */
final class PanelFixture
{
	private PanelFixture()
	{
	}

	private static final CombatVitalsConfig DEFAULTS = new CombatVitalsConfig()
	{
	};

	/** Every @ConfigItem method of the config, by keyName. */
	static Map<String, Method> methods()
	{
		final Map<String, Method> out = new LinkedHashMap<>();
		for (Method m : CombatVitalsConfig.class.getMethods())
		{
			final ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				out.put(item.keyName(), m);
			}
		}
		return out;
	}

	static Map<String, ConfigItemDescriptor> descriptors()
	{
		final Map<String, ConfigItemDescriptor> out = new HashMap<>();
		for (Map.Entry<String, Method> e : methods().entrySet())
		{
			final Method m = e.getValue();
			out.put(e.getKey(), new ConfigItemDescriptor(m.getAnnotation(ConfigItem.class), m.getGenericReturnType(),
				m.getAnnotation(Range.class), m.getAnnotation(Alpha.class), m.getAnnotation(Units.class)));
		}
		return out;
	}

	/** A store with every default stored, as after RuneLite's setDefaultConfiguration. */
	static Store store()
	{
		final Store s = new Store();
		for (Map.Entry<String, Method> e : methods().entrySet())
		{
			try
			{
				s.values.put(e.getKey(), e.getValue().invoke(DEFAULTS));
				s.types.put(e.getKey(), e.getValue().getReturnType());
			}
			catch (ReflectiveOperationException ex)
			{
				throw new IllegalStateException(ex);
			}
		}
		return s;
	}

	/** ConfigManager in memory: typed values, raw text as ConfigManager stores it, and every write recorded. */
	static final class Store implements ConfigStore
	{
		final Map<String, Object> values = new HashMap<>();
		final Map<String, Class<?>> types = new HashMap<>();
		/** Keys in the order they were written, by {@link #set} or {@link #setRaw}. */
		final List<String> writes = new ArrayList<>();

		@Override
		public String raw(String key)
		{
			final Object v = values.get(key);
			if (v == null)
			{
				return null;
			}
			if (v instanceof Enum)
			{
				return ((Enum<?>) v).name();
			}
			if (v instanceof Color)
			{
				return String.valueOf(((Color) v).getRGB());
			}
			if (v instanceof Notification)
			{
				return "notification:" + ((Notification) v).isEnabled();
			}
			return String.valueOf(v);
		}

		@Override
		public void setRaw(String key, String value)
		{
			writes.add(key);
			if (value == null)
			{
				values.remove(key);
				return;
			}
			values.put(key, parse(types.get(key), value));
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		private static Object parse(Class<?> type, String value)
		{
			if (type == boolean.class)
			{
				return Boolean.parseBoolean(value);
			}
			if (type == int.class)
			{
				return Integer.parseInt(value);
			}
			if (type != null && type.isEnum())
			{
				return Enum.valueOf((Class) type, value);
			}
			if (type == Color.class)
			{
				return new Color(Integer.parseInt(value), true);
			}
			return value;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(String key, Class<T> type)
		{
			return (T) values.get(key);
		}

		@Override
		public void set(String key, Object value)
		{
			writes.add(key);
			values.put(key, value);
		}
	}
}
