package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/** Presets: the built-ins, saving, renaming and deleting your own, the stored JSON, and auto-switch. */
public class PillarPresetsTest
{
	/** A config in a map, applying writes at once. */
	private static final class MapStore implements ConfigStore
	{
		final Map<String, String> values = new HashMap<>();

		@Override
		public String raw(String key)
		{
			return values.get(key);
		}

		@Override
		public void setRaw(String key, String value)
		{
			if (value == null)
			{
				values.remove(key);
			}
			else
			{
				values.put(key, value);
			}
		}

		@Override
		public <T> T get(String key, Class<T> type)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(String key, Object value)
		{
			values.put(key, String.valueOf(value));
		}
	}

	private static Map<String, String> preset(PillarSplit split, PillarSource first, PillarSource second)
	{
		final Map<String, String> p = new LinkedHashMap<>();
		p.put(PillarPresets.LEFT_SPLIT, split.name());
		p.put(PillarPresets.LEFT_FIRST, first.name());
		p.put(PillarPresets.LEFT_SECOND, second.name());
		return p;
	}

	@Test
	public void everyBuiltInWritesOnlyKnownKeysWithValuesTheSettingsAccept()
	{
		for (PillarPresets.Builtin builtin : PillarPresets.Builtin.values())
		{
			assertEquals(builtin.label, builtin.settings(), PillarPresets.sanitize(builtin.settings()));
			for (String key : builtin.settings().keySet())
			{
				assertTrue(key, PillarPresets.KEYS.contains(key));
			}
		}
	}

	@Test
	public void theSixBuiltInsAreThereByName()
	{
		for (String name : new String[] {"Melee", "Ranged", "Magic", "Bossing", "Skilling", "Minimal"})
		{
			assertNotNull(name, PillarPresets.builtin(name));
			assertNotNull(name, PillarPresets.builtin(name.toUpperCase()));
		}
		assertNull(PillarPresets.builtin("Raids"));
		assertEquals(6, PillarPresets.Builtin.values().length);
	}

	@Test
	public void theMeleeBuiltInSplitsBothPillars()
	{
		final Map<String, String> melee = PillarPresets.Builtin.MELEE.settings();
		final PillarSetup left = PillarPresets.setup(melee, PillarSide.LEFT);
		final PillarSetup right = PillarPresets.setup(melee, PillarSide.RIGHT);
		assertEquals(new PillarSetup(PillarSplit.TOP_BOTTOM, PillarSource.HITPOINTS, PillarSource.PRAYER), left);
		assertEquals(new PillarSetup(PillarSplit.TOP_BOTTOM, PillarSource.SPEC, PillarSource.TARGET_HP), right);
	}

	@Test
	public void theSkillingBuiltInGoesBackToTheDefaultPillarsAndTurnsSkillingOn()
	{
		final Map<String, String> skilling = PillarPresets.Builtin.SKILLING.settings();
		assertEquals(PillarMode.SKILLING.name(), skilling.get(PillarPresets.MODE));
		assertEquals(PillarSetup.DEFAULT, PillarPresets.setup(skilling, PillarSide.LEFT));
		assertEquals(PillarSetup.DEFAULT, PillarPresets.setup(skilling, PillarSide.RIGHT));
	}

	@Test
	public void applyingWritesTheKeysAndLeavesTheRestAlone()
	{
		final MapStore store = new MapStore();
		store.values.put("pillarStyle", "FLAT");
		store.values.put(PillarPresets.MODE, "BOTH");
		PillarPresets.apply(PillarPresets.Builtin.RANGED.settings(), store);
		assertEquals("TOP_BOTTOM", store.values.get(PillarPresets.LEFT_SPLIT));
		assertEquals("AMMO", store.values.get(PillarPresets.RIGHT_FIRST));
		assertEquals("FLAT", store.values.get("pillarStyle"));
		// The Ranged preset holds no mode, so the player's stays.
		assertEquals("BOTH", store.values.get(PillarPresets.MODE));
	}

	@Test
	public void aPresetWithJunkWritesNoJunk()
	{
		final Map<String, String> bad = new HashMap<>();
		bad.put(PillarPresets.LEFT_SPLIT, "DIAGONAL");
		bad.put(PillarPresets.LEFT_FIRST, "AMMO");
		bad.put("showPanel", "false");
		bad.put(PillarPresets.RIGHT_FIRST, "NOT_A_SOURCE");
		final MapStore store = new MapStore();
		PillarPresets.apply(bad, store);
		assertEquals(1, store.values.size());
		assertEquals("AMMO", store.values.get(PillarPresets.LEFT_FIRST));
	}

	@Test
	public void saveThenLoadReturnsWhatWasSaved()
	{
		final MapStore store = new MapStore();
		PillarPresets.apply(PillarPresets.Builtin.MAGIC.settings(), store);
		final Map<String, Map<String, String>> saved = new LinkedHashMap<>();
		assertTrue(PillarPresets.save(saved, "  Zulrah  ", PillarPresets.current(store)));
		assertTrue(saved.containsKey("Zulrah"));

		PillarPresets.apply(PillarPresets.Builtin.MINIMAL.settings(), store);
		assertEquals("OFF", store.values.get(PillarPresets.LEFT_SPLIT));

		PillarPresets.apply(PillarPresets.find("zulrah", saved), store);
		assertEquals("TOP_BOTTOM", store.values.get(PillarPresets.LEFT_SPLIT));
		assertEquals("CASTS", store.values.get(PillarPresets.RIGHT_FIRST));
	}

	@Test
	public void theSameNameReplacesAndABuiltInNameIsRefused()
	{
		final Map<String, Map<String, String>> saved = new LinkedHashMap<>();
		assertTrue(PillarPresets.save(saved, "Raids", preset(PillarSplit.OFF, PillarSource.FOOD, PillarSource.NONE)));
		assertTrue(PillarPresets.save(saved, "raids", preset(PillarSplit.OFF, PillarSource.AMMO, PillarSource.NONE)));
		assertEquals(1, saved.size());
		assertEquals("AMMO", saved.get("raids").get(PillarPresets.LEFT_FIRST));

		assertFalse(PillarPresets.save(saved, "Melee", preset(PillarSplit.OFF, PillarSource.FOOD, PillarSource.NONE)));
		assertFalse(PillarPresets.save(saved, "   ", preset(PillarSplit.OFF, PillarSource.FOOD, PillarSource.NONE)));
		assertFalse(PillarPresets.save(saved, "x1234567890123456789012345",
			preset(PillarSplit.OFF, PillarSource.FOOD, PillarSource.NONE)));
		assertEquals(1, saved.size());
	}

	@Test
	public void renameAndDelete()
	{
		final Map<String, Map<String, String>> saved = new LinkedHashMap<>();
		PillarPresets.save(saved, "Raids", preset(PillarSplit.OFF, PillarSource.FOOD, PillarSource.NONE));
		PillarPresets.save(saved, "Zulrah", preset(PillarSplit.OFF, PillarSource.AMMO, PillarSource.NONE));

		assertTrue(PillarPresets.rename(saved, "raids", "Tombs"));
		assertTrue(saved.containsKey("Tombs"));
		assertFalse(saved.containsKey("Raids"));
		// Not onto another of yours, a built-in's name, a blank, or from a name that is not there.
		assertFalse(PillarPresets.rename(saved, "Tombs", "zulrah"));
		assertFalse(PillarPresets.rename(saved, "Tombs", "Ranged"));
		assertFalse(PillarPresets.rename(saved, "Tombs", " "));
		assertFalse(PillarPresets.rename(saved, "Nope", "Other"));
		// A case-only change of its own name is fine.
		assertTrue(PillarPresets.rename(saved, "Tombs", "TOMBS"));

		assertTrue(PillarPresets.delete(saved, "zulrah"));
		assertFalse(PillarPresets.delete(saved, "zulrah"));
		assertFalse(PillarPresets.delete(saved, "Melee"));
		assertEquals(1, saved.size());
	}

	@Test
	public void savedPresetsRoundTripThroughTheStoredJson()
	{
		final Gson gson = new Gson();
		final Map<String, Map<String, String>> saved = new LinkedHashMap<>();
		PillarPresets.save(saved, "Raids", PillarPresets.Builtin.BOSSING.settings());
		PillarPresets.save(saved, "Zulrah", PillarPresets.Builtin.MAGIC.settings());
		final String json = PillarPresets.toJson(gson, saved);
		final Map<String, Map<String, String>> back = PillarPresets.parse(gson, json);
		assertEquals(saved, back);
		assertEquals("Raids", back.keySet().iterator().next());
	}

	@Test
	public void storedJsonThatIsEmptyBrokenOrStaleReadsAsFewerPresetsNeverAnError()
	{
		final Gson gson = new Gson();
		assertTrue(PillarPresets.parse(gson, null).isEmpty());
		assertTrue(PillarPresets.parse(gson, "").isEmpty());
		assertTrue(PillarPresets.parse(gson, "{not json").isEmpty());
		assertTrue(PillarPresets.parse(gson, "[]").isEmpty());
		final Map<String, Map<String, String>> stale = PillarPresets.parse(gson,
			"{\"presets\":{\"Melee\":{\"pillarLeftSource\":\"AMMO\"},\"Old\":{\"pillarLeftSource\":\"GONE\","
				+ "\"pillarRightSource\":\"FOOD\"}}}");
		// A saved preset cannot shadow a built-in, and a source that no longer exists is dropped.
		assertFalse(stale.containsKey("Melee"));
		assertEquals(1, stale.get("Old").size());
		assertEquals("FOOD", stale.get("Old").get(PillarPresets.RIGHT_FIRST));
	}

	@Test
	public void findPrefersTheBuiltInAndSaysNullForNeither()
	{
		final Map<String, Map<String, String>> saved = new LinkedHashMap<>();
		PillarPresets.save(saved, "Raids", PillarPresets.Builtin.BOSSING.settings());
		assertEquals(PillarPresets.Builtin.MELEE.settings(), PillarPresets.find("melee", saved));
		assertNotNull(PillarPresets.find("RAIDS", saved));
		assertNull(PillarPresets.find("Nope", saved));
		assertNull(PillarPresets.find(null, saved));
		assertEquals("Melee, Ranged, Magic, Bossing, Skilling, Minimal; yours: Raids", PillarPresets.names(saved));
	}

	@Test
	public void autoSwitchPicksByWeaponAndActivity()
	{
		assertEquals("Skilling", PillarPresets.autoChoice("Rune pickaxe", true));
		assertEquals("Skilling", PillarPresets.autoChoice(null, true));
		assertEquals("Ranged", PillarPresets.autoChoice("Twisted bow", false));
		assertEquals("Ranged", PillarPresets.autoChoice("Rune crossbow", false));
		assertEquals("Ranged", PillarPresets.autoChoice("Toxic blowpipe", false));
		assertEquals("Ranged", PillarPresets.autoChoice("Rune dart", false));
		assertEquals("Ranged", PillarPresets.autoChoice("Red chinchompa", false));
		assertEquals("Magic", PillarPresets.autoChoice("Staff of the dead", false));
		assertEquals("Magic", PillarPresets.autoChoice("Trident of the swamp", false));
		assertEquals("Magic", PillarPresets.autoChoice("Tumeken's shadow", false));
		assertEquals("Magic", PillarPresets.autoChoice("Sanguinesti staff", false));
		assertEquals("Melee", PillarPresets.autoChoice("Abyssal whip", false));
		assertEquals("Melee", PillarPresets.autoChoice(null, false));
	}

	private static final CombatVitalsConfig NEW_PROFILE = new CombatVitalsConfig()
	{
	};

	/** Nothing changes for a current user until they pick something: default sources, no split, auto-switch off. */
	@Test
	public void aNewProfileKeepsTheDefaultPillarsAndAutoSwitchIsOff()
	{
		assertEquals(PillarSplit.OFF, NEW_PROFILE.pillarLeftSplit());
		assertEquals(PillarSplit.OFF, NEW_PROFILE.pillarRightSplit());
		assertEquals(PillarSource.DEFAULT, NEW_PROFILE.pillarLeftSource());
		assertEquals(PillarSource.DEFAULT, NEW_PROFILE.pillarRightSource());
		assertEquals(PillarSource.NONE, NEW_PROFILE.pillarLeftSource2());
		assertEquals(PillarSource.NONE, NEW_PROFILE.pillarRightSource2());
		assertFalse(NEW_PROFILE.pillarAutoSwitch());
		assertEquals(PillarMode.OFF, NEW_PROFILE.pillarMode());
		assertEquals(PillarPresetChoice.KEEP, NEW_PROFILE.pillarPreset());
		assertFalse(PillarSetup.DEFAULT.anyCustom());
	}

	@Test
	public void theAutoSwitchPresetNamesAreTheBuiltIns()
	{
		assertNotNull(PillarPresets.builtin(NEW_PROFILE.pillarAutoMelee()));
		assertNotNull(PillarPresets.builtin(NEW_PROFILE.pillarAutoRanged()));
		assertNotNull(PillarPresets.builtin(NEW_PROFILE.pillarAutoMagic()));
		assertNotNull(PillarPresets.builtin(NEW_PROFILE.pillarAutoSkilling()));
	}

	@Test
	public void theApplyPresetDropdownHasEachBuiltInOnce()
	{
		int builtins = 0;
		for (PillarPresetChoice choice : PillarPresetChoice.values())
		{
			if (choice.builtin != null)
			{
				builtins++;
				assertEquals(choice.getName(), choice.builtin.label);
			}
		}
		assertEquals(PillarPresets.Builtin.values().length, builtins);
		assertNull(PillarPresetChoice.KEEP.builtin);
	}
}
