package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * The two master switches, Combat vitals and Skilling vitals (Board,
 * 2026-11-14): both on for a new profile and for everyone who had the plugin
 * before them, and each one off takes away its own half (overlays, side
 * pillars, alerts) and nothing of the other's.
 */
public class VitalsHalvesTest
{
	private static final Path SOURCES = Paths.get("src/main/java/com/vinnyvangogh/combatvitals");

	/** Every sub-setting a switch gates turned on, so only the switch decides. */
	private static CombatVitalsConfig config(boolean combat, boolean skilling)
	{
		return new CombatVitalsConfig()
		{
			@Override
			public boolean combatVitals()
			{
				return combat;
			}

			@Override
			public boolean skillingVitals()
			{
				return skilling;
			}

			@Override
			public boolean hideGameHealthBar()
			{
				return true;
			}

			@Override
			public PillarMode pillarMode()
			{
				return PillarMode.BOTH;
			}

			@Override
			public FishingSpotPath fishingSpotPath()
			{
				return FishingSpotPath.ON_SPOT_MOVE;
			}
		};
	}

	@Test
	public void bothSwitchesAreOnForANewProfile()
	{
		final CombatVitalsConfig newProfile = new CombatVitalsConfig()
		{
		};
		assertTrue(newProfile.combatVitals());
		assertTrue(newProfile.skillingVitals());
		assertTrue(VitalsHalves.combat(newProfile));
		assertTrue(VitalsHalves.skilling(newProfile));
		// Nothing a user had set changes: the defaults pass straight through.
		assertEquals(newProfile.pillarMode(), VitalsHalves.pillarMode(newProfile));
		assertEquals(newProfile.skillingEnabled(), VitalsHalves.skillingBadge(newProfile));
		assertEquals(newProfile.hideGameHealthBar(), VitalsHalves.hideGameHealthBar(newProfile));
		assertEquals(newProfile.fishingSpotPath(), VitalsHalves.fishingSpotPath(newProfile));
	}

	@Test
	public void bothOnLeavesEverySettingAsSet()
	{
		final CombatVitalsConfig on = config(true, true);
		assertTrue(VitalsHalves.combat(on));
		assertTrue(VitalsHalves.skilling(on));
		assertTrue(VitalsHalves.skillingBadge(on));
		assertTrue(VitalsHalves.hideGameHealthBar(on));
		assertEquals(PillarMode.BOTH, VitalsHalves.pillarMode(on));
		assertEquals(on.fishingSpotPath(), VitalsHalves.fishingSpotPath(on));
	}

	@Test
	public void combatOffTakesOnlyTheCombatHalf()
	{
		final CombatVitalsConfig skillingOnly = config(false, true);
		// Combat: target and own-health readouts, the game health bar swap, combat pillars.
		assertFalse(VitalsHalves.combat(skillingOnly));
		assertFalse(VitalsHalves.hideGameHealthBar(skillingOnly));
		assertFalse(VitalsHalves.pillarMode(skillingOnly).combat());
		// Skilling untouched: badge, skilling pillars, next spot, alerts.
		assertTrue(VitalsHalves.skilling(skillingOnly));
		assertTrue(VitalsHalves.skillingBadge(skillingOnly));
		assertTrue(VitalsHalves.pillarMode(skillingOnly).skilling());
		assertEquals(skillingOnly.fishingSpotPath(), VitalsHalves.fishingSpotPath(skillingOnly));
	}

	@Test
	public void skillingOffTakesOnlyTheSkillingHalf()
	{
		final CombatVitalsConfig combatOnly = config(true, false);
		// Skilling: badge, skilling pillars, next spot path, activity and boost alerts.
		assertFalse(VitalsHalves.skilling(combatOnly));
		assertFalse(VitalsHalves.skillingBadge(combatOnly));
		assertFalse(VitalsHalves.pillarMode(combatOnly).skilling());
		assertEquals(FishingSpotPath.OFF, VitalsHalves.fishingSpotPath(combatOnly));
		// Combat untouched.
		assertTrue(VitalsHalves.combat(combatOnly));
		assertTrue(VitalsHalves.hideGameHealthBar(combatOnly));
		assertTrue(VitalsHalves.pillarMode(combatOnly).combat());
	}

	@Test
	public void bothOffDrawsNoPillars()
	{
		assertEquals(PillarMode.OFF, VitalsHalves.pillarMode(config(false, false)));
	}

	@Test
	public void pillarModeKeepsWhatTheUserChoseForTheHalfThatIsOn()
	{
		for (PillarMode mode : PillarMode.values())
		{
			final CombatVitalsConfig skillingOff = new CombatVitalsConfig()
			{
				@Override
				public boolean skillingVitals()
				{
					return false;
				}

				@Override
				public PillarMode pillarMode()
				{
					return mode;
				}
			};
			assertEquals(mode.toString(), mode.combat(), VitalsHalves.pillarMode(skillingOff).combat());
			assertFalse(mode.toString(), VitalsHalves.pillarMode(skillingOff).skilling());
		}
	}

	/**
	 * A setting a switch gates, read anywhere but through VitalsHalves, would
	 * keep its half running with the switch off. The plugin builds the
	 * readouts every overlay and pillar draws from, and sends every alert,
	 * only through the switches.
	 */
	@Test
	public void gatedSettingsAreOnlyReadThroughTheSwitches() throws IOException
	{
		final String[] gated = {
			"config.skillingEnabled()", "config.pillarMode()", "config.hideGameHealthBar()",
			"config.fishingSpotPath()", "config::fishingSpotPath",
		};
		try (Stream<Path> files = Files.list(SOURCES))
		{
			files.filter(p -> !p.getFileName().toString().equals("VitalsHalves.java"))
				.forEach(p ->
				{
					final String code = read(p);
					for (String read : gated)
					{
						assertFalse(p.getFileName() + " reads " + read + " around its master switch", code.contains(read));
					}
				});
		}
	}

	@Test
	public void thePluginGatesEachHalfAtItsSource()
	{
		final String plugin = read(SOURCES.resolve("CombatVitalsPlugin.java"));
		assertTrue(plugin.contains("readout = VitalsHalves.combat(config) ? buildReadout() : null;"));
		assertTrue(plugin.contains("selfReadout = VitalsHalves.combat(config) ? buildSelfReadout() : null;"));
		assertTrue(plugin.contains("skillingReadout = VitalsHalves.skilling(config) ? buildSkillingReadout() : null;"));
		assertTrue("activity alerts", plugin.contains("if (!VitalsHalves.skilling(config))\n\t\t{\n\t\t\talertRepeat.stop();"));
		assertTrue("with combat off no target readout takes the badge's place, so a fight keeps the badge",
			plugin.contains("(VitalsHalves.combat(config) && combat.showTarget())"));
		assertTrue("boost alerts",plugin.contains("warning != SkillingState.Warning.NONE && VitalsHalves.skilling(config)"));
	}

	private static String read(Path path)
	{
		try
		{
			return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
