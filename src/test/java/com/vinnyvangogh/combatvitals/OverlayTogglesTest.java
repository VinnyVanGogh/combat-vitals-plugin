package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class OverlayTogglesTest
{
	private final Map<String, Object> values = new HashMap<>();
	private ConfigStore store;

	@Before
	public void setUp()
	{
		store = new ConfigStore()
		{
			@Override
			public String raw(String key)
			{
				return values.containsKey(key) ? String.valueOf(values.get(key)) : null;
			}

			@Override
			public void setRaw(String key, String value)
			{
				values.put(key, value);
			}

			@Override
			public <T> T get(String key, Class<T> type)
			{
				return type.cast(values.get(key));
			}

			@Override
			public void set(String key, Object value)
			{
				values.put(key, value);
			}
		};
	}

	@Test
	public void everyToggleKeyIsARealSetting()
	{
		for (OverlayToggles.Toggle t : OverlayToggles.TOGGLES)
		{
			assertTrue(t.key, PanelFixture.methods().containsKey(t.key));
		}
	}

	@Test
	public void allOffThenAllOnWritesEveryOverlayKey()
	{
		OverlayToggles.setAll(store, false);
		for (OverlayToggles.Toggle t : OverlayToggles.TOGGLES)
		{
			if (!t.extra)
			{
				assertFalse(t.key, OverlayToggles.isOn(store, t));
			}
		}
		assertEquals(PillarMode.OFF, values.get("pillarMode"));
		OverlayToggles.setAll(store, true);
		for (OverlayToggles.Toggle t : OverlayToggles.TOGGLES)
		{
			if (!t.extra)
			{
				assertTrue(t.key, OverlayToggles.isOn(store, t));
			}
		}
		assertEquals(PillarMode.BOTH, values.get("pillarMode"));
	}

	@Test
	public void anUnsetKeyReadsAsItsDefault()
	{
		assertTrue(OverlayToggles.isOn(store, OverlayToggles.find("showPanel")));
		assertFalse(OverlayToggles.isOn(store, OverlayToggles.find("pillarMode")));
		assertFalse(OverlayToggles.isOn(store, OverlayToggles.find("aggressionAreaLines")));
	}

	@Test
	public void pillarsCountAsOnForAnythingButOff()
	{
		final OverlayToggles.Toggle pillars = OverlayToggles.find("pillarMode");
		values.put("pillarMode", PillarMode.COMBAT);
		assertTrue(OverlayToggles.isOn(store, pillars));
		OverlayToggles.set(store, pillars, false);
		assertEquals(PillarMode.OFF, values.get("pillarMode"));
	}

	@Test
	public void areaLinesStayOutOfAllOn()
	{
		OverlayToggles.setAll(store, true);
		assertFalse(values.containsKey("aggressionAreaLines"));
	}

	@Test
	public void allOnIsTrueOnlyWhenEveryOverlayIsOn()
	{
		OverlayToggles.setAll(store, true);
		assertTrue(OverlayToggles.allOn(store));
		values.put("showPanel", false);
		assertFalse(OverlayToggles.allOn(store));
	}
}
