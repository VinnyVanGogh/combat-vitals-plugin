package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * Board's in-game test, 2026-11-06: "Hide game health bar" hid every
 * standard bar but the plugin drew back only some. Every bar hidden must be
 * drawn back, for every player and NPC that shows one.
 */
public class GameHealthBarsTest
{
	/** A stand-in actor: its bar reading and where it projects. */
	private static final class Fake
	{
		final int ratio;
		final int scale;
		final boolean talking;
		final int[] anchor;

		Fake(int ratio, int scale, boolean talking, int[] anchor)
		{
			this.ratio = ratio;
			this.scale = scale;
			this.talking = talking;
			this.anchor = anchor;
		}
	}

	private static final GameHealthBars.Access<Fake> ACCESS = new GameHealthBars.Access<Fake>()
	{
		@Override
		public int ratio(Fake actor)
		{
			return actor.ratio;
		}

		@Override
		public int scale(Fake actor)
		{
			return actor.scale;
		}

		@Override
		public boolean talking(Fake actor)
		{
			return actor.talking;
		}

		@Override
		public int[] anchor(Fake actor)
		{
			return actor.anchor;
		}
	};

	@Test
	public void aBarIsShownOnlyWithARatioAndAScale()
	{
		assertTrue(GameHealthBars.shown(0, 30));
		assertTrue(GameHealthBars.shown(30, 30));
		assertFalse("no bar: the client reports -1", GameHealthBars.shown(-1, 30));
		assertFalse(GameHealthBars.shown(10, -1));
	}

	@Test
	public void theBarSitsWhereTheClientDrawsIt()
	{
		// Projected at (200, 100): the client's offset starts at -2 and grows by
		// the bar's 5 px, so the top is 3 px above the point; centred on x.
		assertEquals(new Rectangle(185, 97, 30, 5), GameHealthBars.box(200, 100, 30, false));
	}

	@Test
	public void chatLiftsTheBarTwelvePixels()
	{
		assertEquals(new Rectangle(185, 85, 30, 5), GameHealthBars.box(200, 100, 30, true));
	}

	@Test
	public void anOddWidthCentresAsTheClientDoes()
	{
		// x - (width >> 1)
		assertEquals(200 - 7, GameHealthBars.box(200, 100, 15, false).x);
	}

	@Test
	public void theFillIsTheRatiosShareOfTheWidth()
	{
		assertEquals(30, GameHealthBars.filled(30, 30, 30));
		assertEquals(15, GameHealthBars.filled(15, 30, 30));
		assertEquals(0, GameHealthBars.filled(0, 30, 30));
		assertEquals("any health left shows a pixel", 1, GameHealthBars.filled(1, 255, 30));
		assertEquals("a boss bar", 60, GameHealthBars.filled(60, 120, 120));
	}

	@Test
	public void theWidthIsTheScaleUpToTheWidestBar()
	{
		assertEquals(30, GameHealthBars.width(30));
		assertEquals(120, GameHealthBars.width(120));
		assertEquals(160, GameHealthBars.width(255));
	}

	/**
	 * Several players fighting nearby, NPCs at different health, some talking,
	 * some off screen, and your target: every on-screen actor with a bar gets
	 * exactly one, in its own place, and the target is left to the readout.
	 */
	@Test
	public void aCrowdGetsABarEachExceptTheTarget()
	{
		final List<Fake> crowd = new ArrayList<>();
		for (int i = 0; i < 12; i++)
		{
			crowd.add(new Fake(i * 2, 30, i % 3 == 0, new int[]{100 + i * 40, 200}));
		}
		final Fake noBar = new Fake(-1, 30, false, new int[]{50, 50});
		final Fake offScreen = new Fake(10, 30, false, null);
		final Fake target = new Fake(5, 30, false, new int[]{700, 200});
		crowd.add(noBar);
		crowd.add(offScreen);
		crowd.add(target);

		final List<GameHealthBars.Bar> bars = GameHealthBars.plan(crowd, a -> a == target, ACCESS);

		assertEquals("one per on-screen actor with a bar, the target excepted", 12, bars.size());
		final Set<Integer> centres = new HashSet<>();
		for (int i = 0; i < 12; i++)
		{
			final GameHealthBars.Bar bar = bars.get(i);
			final Rectangle box = bar.getBox();
			centres.add(box.x + 15);
			assertEquals("fill follows its own ratio", i * 2, bar.getFilled());
			assertEquals("talking actors' bars sit higher", i % 3 == 0 ? 85 + 100 : 97 + 100, box.y);
		}
		assertEquals("each in its own place", 12, centres.size());
	}
}
