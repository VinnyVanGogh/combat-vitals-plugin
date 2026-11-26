package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.Test;

/** Each pillar source's value out of a mocked snapshot: the fill, the number, the hover text, the nothing-to-show cases. */
public class PillarValuesTest
{
	private static final Color LOW = new Color(255, 0, 0);

	private static final PillarValues.Settings SETTINGS = new PillarValues.Settings()
	{
		@Override
		public Color colour(PillarSource source)
		{
			return source.getDefaultColour();
		}

		@Override
		public Color targetColour(double fraction)
		{
			return Color.GREEN;
		}

		@Override
		public Color lowColour()
		{
			return LOW;
		}

		@Override
		public BufferedImage icon(PillarSource source)
		{
			return null;
		}

		@Override
		public int ammoFull()
		{
			return 200;
		}

		@Override
		public int ammoLow()
		{
			return 25;
		}

		@Override
		public int runeFull()
		{
			return 1000;
		}

		@Override
		public PillarRune rune()
		{
			return PillarRune.DEATH;
		}

		@Override
		public int castsLow()
		{
			return 10;
		}
	};

	private static Pillar of(PillarSource source, PillarSnapshot snapshot)
	{
		return PillarValues.of(source, snapshot, SETTINGS);
	}

	@Test
	public void theDefaultAndBlankSourcesDrawNothingOfTheirOwn()
	{
		final PillarSnapshot s = PillarSnapshot.builder().hp(50).maxHp(99).build();
		assertNull(of(PillarSource.DEFAULT, s));
		assertNull(of(PillarSource.NONE, s));
		assertNull(of(null, s));
	}

	@Test
	public void anUnreadSnapshotShowsNothingForEverySource()
	{
		final PillarSnapshot unread = PillarSnapshot.builder().build();
		for (PillarSource source : PillarSource.values())
		{
			assertNull(source.name(), of(source, unread));
		}
	}

	@Test
	public void hitpointsAndPrayerAreTheirShareOfTheLevel()
	{
		final PillarSnapshot s = PillarSnapshot.builder().hp(50).maxHp(99).prayer(20).maxPrayer(80).build();
		final Pillar hp = of(PillarSource.HITPOINTS, s);
		assertEquals(50 / 99.0, hp.getFraction(), 1e-9);
		assertEquals("50", hp.getNumber());
		assertEquals("Hitpoints: 50/99", hp.getTooltip());
		assertEquals(PillarSource.HITPOINTS.getDefaultColour(), hp.getColour());
		assertEquals(0.25, of(PillarSource.PRAYER, s).getFraction(), 1e-9);
		assertEquals("Prayer: 20/80", of(PillarSource.PRAYER, s).getTooltip());
	}

	@Test
	public void runAndSpecReadAsPercents()
	{
		final PillarSnapshot s = PillarSnapshot.builder().runEnergy(64).specPercent(50).build();
		assertEquals(0.64, of(PillarSource.RUN_ENERGY, s).getFraction(), 1e-9);
		assertEquals("64%", of(PillarSource.RUN_ENERGY, s).getNumber());
		assertEquals("Special attack 50%", of(PillarSource.SPEC, s).getTooltip());
	}

	@Test
	public void ammoFillsAgainstTheSettingAndWarnsWhenLow()
	{
		final Pillar plenty = of(PillarSource.AMMO,
			PillarSnapshot.builder().ammoCount(100).ammoName("Rune arrow").build());
		assertEquals(0.5, plenty.getFraction(), 1e-9);
		assertEquals("100", plenty.getNumber());
		assertEquals("Rune arrow: 100", plenty.getTooltip());
		assertEquals(PillarSource.AMMO.getDefaultColour(), plenty.getColour());

		final Pillar low = of(PillarSource.AMMO, PillarSnapshot.builder().ammoCount(25).ammoName("Rune arrow").build());
		assertEquals(LOW, low.getColour());
		assertTrue(low.getTooltip().endsWith("(low)"));

		// Past the bar's full mark it is full, never over.
		assertEquals(1.0, of(PillarSource.AMMO, PillarSnapshot.builder().ammoCount(5000).build()).getFraction(), 0);
	}

	@Test
	public void aBlowpipeShowsItsDartsAndSaysItsScales()
	{
		final Pillar p = of(PillarSource.AMMO,
			PillarSnapshot.builder().ammoCount(150).ammoExtra(3000).ammoName("Toxic blowpipe darts").build());
		assertEquals("150", p.getNumber());
		assertEquals("Toxic blowpipe darts: 150, scales 3,000", p.getTooltip());
	}

	@Test
	public void runesCountTheChosenRuneOnly()
	{
		final long[] runes = new long[PillarRune.values().length];
		runes[PillarRune.DEATH.ordinal()] = 250;
		runes[PillarRune.BLOOD.ordinal()] = 40;
		final PillarSnapshot s = PillarSnapshot.builder().runeCounts(runes).build();
		final Pillar p = of(PillarSource.RUNES, s);
		assertEquals(0.25, p.getFraction(), 1e-9);
		assertEquals("250", p.getNumber());
		assertEquals("Death runes: 250", p.getTooltip());
	}

	@Test
	public void castsShowTheCountUnlimitedAndTheLowWarning()
	{
		final Pillar p = of(PillarSource.CASTS,
			PillarSnapshot.builder().castsLeft(50).castsSpell("Ice Barrage").build());
		assertEquals(0.5, p.getFraction(), 1e-9);
		assertEquals("Ice Barrage: 50 casts left", p.getTooltip());

		final Pillar low = of(PillarSource.CASTS,
			PillarSnapshot.builder().castsLeft(1).castsSpell("Ice Barrage").build());
		assertEquals(LOW, low.getColour());
		assertEquals("Ice Barrage: 1 cast left (low)", low.getTooltip());

		final Pillar unlimited = of(PillarSource.CASTS,
			PillarSnapshot.builder().castsLeft(PillarSnapshot.INFINITE).castsSpell("Fire Strike").build());
		assertEquals(1.0, unlimited.getFraction(), 0);
		assertEquals("Fire Strike: unlimited casts", unlimited.getTooltip());

		assertNull(of(PillarSource.CASTS, PillarSnapshot.builder().build()));
	}

	@Test
	public void chargesAreAgainstTheWeaponsMaximum()
	{
		final Pillar p = of(PillarSource.CHARGES, PillarSnapshot.builder().charges(2500).chargesMax(10000)
			.chargesName("Trident of the seas").build());
		assertEquals(0.25, p.getFraction(), 1e-9);
		assertEquals("2.5k", p.getNumber());
		assertEquals("Trident of the seas: 2,500 charges", p.getTooltip());
	}

	@Test
	public void foodCountsAndSumsItsHealingAndAdmitsWhatItDoesNotKnow()
	{
		final Pillar p = of(PillarSource.FOOD,
			PillarSnapshot.builder().foodCount(14).foodHeal(280).foodUnknown(2).build());
		assertEquals(0.5, p.getFraction(), 1e-9);
		assertEquals("14", p.getNumber());
		assertEquals("Food: 14, heals 280 (2 with unknown healing)", p.getTooltip());
	}

	@Test
	public void dosesAndBoostAndCannonballs()
	{
		assertEquals("Prayer and restore doses: 16",
			of(PillarSource.PRAYER_DOSES, PillarSnapshot.builder().prayerDoses(16).build()).getTooltip());
		assertEquals(0.25, of(PillarSource.STAMINA, PillarSnapshot.builder().staminaDoses(3).build()).getFraction(),
			1e-9);
		final Pillar boost = of(PillarSource.BOOST,
			PillarSnapshot.builder().boostLeft(6).boostPeak(12).boostSkill("Strength").build());
		assertEquals(0.5, boost.getFraction(), 1e-9);
		assertEquals("Strength boost: +6 of +12", boost.getTooltip());
		assertEquals("Cannonballs: 30",
			of(PillarSource.CANNONBALLS, PillarSnapshot.builder().cannonballs(30).build()).getTooltip());
	}

	/**
	 * Plugin Hub: a player target is HP only (precedent: the base Opponent Information plugin). The
	 * snapshot has no field for anything else, and the pillar says only the name and the health.
	 */
	@Test
	public void aTargetIsItsNameAndHealthAndNothingElse()
	{
		final PillarSnapshot s = PillarSnapshot.builder().targetName("Placeholder Player").targetFraction(0.4)
			.targetHp(40).targetMax(100).build();
		final Pillar p = of(PillarSource.TARGET_HP, s);
		assertNotNull(p);
		assertEquals(0.4, p.getFraction(), 1e-9);
		assertEquals("40", p.getNumber());
		assertEquals("Target: Placeholder Player 40/100", p.getTooltip());
		assertEquals(Color.GREEN, p.getColour());
		assertFalse(p.getTooltip().toLowerCase().contains("combat"));
		assertFalse(p.getTooltip().toLowerCase().contains("level"));
		assertFalse(p.getTooltip().toLowerCase().contains("prayer"));
		assertFalse(p.getTooltip().toLowerCase().contains("gear"));
		assertNull(of(PillarSource.TARGET_HP, PillarSnapshot.builder().build()));
	}

	@Test
	public void aTargetKnownOnlyByItsBarShowsAPercent()
	{
		final PillarSnapshot s = PillarSnapshot.builder().targetName("Placeholder NPC").targetFraction(0.82).build();
		final Pillar p = of(PillarSource.TARGET_HP, s);
		assertEquals("82%", p.getNumber());
		assertEquals("Target: Placeholder NPC 82%", p.getTooltip());
	}

	@Test
	public void everyShownSourceFootsItsTag()
	{
		final long[] runes = new long[PillarRune.values().length];
		runes[PillarRune.DEATH.ordinal()] = 1;
		final PillarSnapshot s = PillarSnapshot.builder().hp(1).maxHp(1).prayer(1).maxPrayer(1).runEnergy(1)
			.specPercent(1).ammoCount(1).runeCounts(runes).castsLeft(1).castsSpell("Wind Strike").charges(1)
			.cannonballs(1).foodCount(1).prayerDoses(1).staminaDoses(1).boostLeft(1).boostPeak(1)
			.targetName("Placeholder NPC").targetFraction(0.5).build();
		for (PillarSource source : PillarSource.values())
		{
			if (source.custom())
			{
				final Pillar p = of(source, s);
				assertNotNull(source.name(), p);
				assertEquals(source.name(), source.getTag(), p.getBottom());
				assertNotNull(source.name(), p.getTooltip());
			}
		}
	}
}
