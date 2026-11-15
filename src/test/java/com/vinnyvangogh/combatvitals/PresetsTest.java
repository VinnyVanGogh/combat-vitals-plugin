package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;

/**
 * A preset writes exactly the keys it lists, each an existing key with a
 * value its type can hold, and Undo puts every one of them back, an unset
 * key included.
 */
public class PresetsTest
{
	@Test
	public void applyWritesExactlyTheListedKeys()
	{
		for (Presets.Preset p : Presets.Preset.values())
		{
			final PanelFixture.Store store = PanelFixture.store();
			Presets.apply(p, store);
			assertEquals(p.name(), Presets.values(p).keySet(), new HashSet<>(store.writes));
			assertEquals(p.name() + " wrote a key twice", Presets.values(p).size(), store.writes.size());
			for (Map.Entry<String, String> e : Presets.values(p).entrySet())
			{
				assertEquals(p + " " + e.getKey(), e.getValue(), store.raw(e.getKey()));
			}
		}
	}

	@Test
	public void undoRestoresEveryKeyItWrote()
	{
		for (Presets.Preset p : Presets.Preset.values())
		{
			final PanelFixture.Store store = PanelFixture.store();
			// Not the defaults: one key flipped, one unset, so Undo has to bring back both.
			store.set("pillarMode", PillarMode.BOTH);
			store.setRaw("combatVitals", null);
			store.set("skillingVitals", false);
			final Map<String, String> before = new HashMap<>();
			for (String key : PanelFixture.methods().keySet())
			{
				before.put(key, store.raw(key));
			}

			final Presets.Undo undo = Presets.apply(p, store);
			store.writes.clear();
			undo.restore(store);

			assertEquals(p.name(), Presets.values(p).keySet(), new HashSet<>(store.writes));
			for (String key : PanelFixture.methods().keySet())
			{
				assertEquals(p + " " + key, before.get(key), store.raw(key));
			}
		}
	}

	@Test
	public void undoBringsBackAnUnsetKeyAsUnset()
	{
		final PanelFixture.Store store = PanelFixture.store();
		store.setRaw("showPanel", null);
		final Presets.Undo undo = Presets.apply(Presets.Preset.MINIMAL, store);
		assertEquals("false", store.raw("showPanel"));
		undo.restore(store);
		assertNull(store.raw("showPanel"));
	}

	@Test
	public void everyPresetKeyExistsAndItsValueFitsItsType()
	{
		final Map<String, Method> methods = PanelFixture.methods();
		for (Presets.Preset p : Presets.Preset.values())
		{
			for (Map.Entry<String, String> e : Presets.values(p).entrySet())
			{
				final Method m = methods.get(e.getKey());
				assertNotNull(p + " writes unknown key " + e.getKey(), m);
				assertTrue(p + " " + e.getKey() + "=" + e.getValue(), fits(m.getReturnType(), e.getValue()));
			}
		}
	}

	private static boolean fits(Class<?> type, String value)
	{
		if (type == boolean.class)
		{
			return value.equals("true") || value.equals("false");
		}
		if (type.isEnum())
		{
			for (Object c : type.getEnumConstants())
			{
				if (((Enum<?>) c).name().equals(value))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void theSkillerPresetIsTheBoardsSkillingSetup()
	{
		final Map<String, String> v = Presets.values(Presets.Preset.SKILLER);
		assertEquals("true", v.get("skillingEnabled"));
		assertEquals("SKILLING", v.get("pillarMode"));
		assertEquals("true", v.get("alertSoundShared"));
		assertEquals("VOICE", v.get("alertSoundAll"));
		assertEquals("true", v.get("alertRepeatAll"));
	}

	@Test
	public void changesListOnlyWhatDiffersAndNothingOnceApplied()
	{
		final PanelFixture.Store store = PanelFixture.store();
		final Map<String, String[]> changes = Presets.changes(Presets.Preset.MINIMAL, store);
		assertTrue(changes.containsKey("showPanel"));
		assertFalse("combatVitals is already on", changes.containsKey("combatVitals"));
		assertEquals("true", changes.get("showPanel")[0]);
		assertEquals("false", changes.get("showPanel")[1]);

		Presets.apply(Presets.Preset.MINIMAL, store);
		assertTrue(Presets.changes(Presets.Preset.MINIMAL, store).isEmpty());
	}

	@Test
	public void previewingWritesNothing()
	{
		final PanelFixture.Store store = PanelFixture.store();
		for (Presets.Preset p : Presets.Preset.values())
		{
			Presets.changes(p, store);
		}
		assertTrue(store.writes.isEmpty());
	}
}
