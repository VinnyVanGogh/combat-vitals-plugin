package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * A micro-benchmark of the work that runs every frame or every client tick
 * around a fishing spot, without a client: the health bar sprite retries,
 * the spot pather's per-tick tile check and the session row's layout. It
 * prints its numbers (run with {@code ./gradlew test --info} to see them)
 * and fails only on a count regression or a gross slowdown, so it does not
 * flake on a busy machine.
 */
public class FramePathBenchmarkTest
{
	/** About 20 seconds of client ticks at 50 a second. */
	private static final int FRAMES = 1_000;

	@Test
	public void healthBarSpritesNotReadyCostOneCacheReadASecondNotOneAFrame()
	{
		final int[] builds = {0};
		final NativeHealthBarHider<Object> hider = new NativeHealthBarHider<>(new java.util.HashMap<>(), id ->
		{
			builds[0]++;
			return null;
		});

		final long start = System.nanoTime();
		for (int i = 0; i < FRAMES; i++)
		{
			hider.apply();
		}
		final long nanos = System.nanoTime() - start;

		final int before = FRAMES * NativeHealthBarHider.SPRITE_IDS.length;
		System.out.printf("hider, sprites not ready: %d cache reads per %d frames (was %d), %.2f us/frame%n",
			builds[0], FRAMES, before, nanos / 1_000.0 / FRAMES);
		assertEquals(FRAMES / NativeHealthBarHider.RETRY_CALLS * NativeHealthBarHider.SPRITE_IDS.length, builds[0]);
	}

	@Test
	public void spotTileCheckIsCheapEnoughForEveryTick()
	{
		final Object spot = new Object();
		final SpotPathing pathing = new SpotPathing(() -> FishingSpotPath.ON_SPOT_MOVE, new SpotPathing.Router()
		{
			@Override
			public boolean available()
			{
				return false;
			}

			@Override
			public boolean route(NextFishingSpot.Spot s)
			{
				return false;
			}

			@Override
			public void clear()
			{
			}
		});
		pathing.fishingOn(net.runelite.client.game.FishingSpot.SHRIMP, "Net", spot);
		final long tile = SpotPathing.tile(3086, 3228, 0);
		final int[] picks = {0};

		final long start = System.nanoTime();
		for (int i = 0; i < FRAMES * 100; i++)
		{
			pathing.tick((type, method) ->
			{
				picks[0]++;
				return null;
			}, npc -> tile);
		}
		final long nanos = System.nanoTime() - start;

		System.out.printf("spot pather tick, spot standing still: %.1f ns/tick, %d searches%n",
			(double) nanos / (FRAMES * 100), picks[0]);
		assertEquals("a spot that does not move is never searched for", 0, picks[0]);
		assertTrue("under 50 microseconds a tick", nanos / (FRAMES * 100) < 50_000);
	}

	@Test
	public void sessionRowLayoutPerFrame()
	{
		final XpProgress xp = XpProgress.of(Experience.getXpForLevel(17) + 100);
		final List<SessionRow.Cell> cells = SessionRow.cells(Skill.FISHING, 24, 180_000, 10_597, 459, xp,
			true, true, true);
		final int[] widths = {96, 58, 60, 70};
		assertEquals(widths.length, cells.size());

		int sink = 0;
		final long start = System.nanoTime();
		for (int i = 0; i < FRAMES * 100; i++)
		{
			final int[][] lines = SessionColumns.lines(widths.length, true, true);
			for (int[] line : lines)
			{
				sink += SessionColumns.fit(widths, line, SessionColumns.width(widths, lines))[0];
			}
		}
		final long nanos = System.nanoTime() - start;

		System.out.printf("session row layout: %.1f ns/frame (sink %d)%n", (double) nanos / (FRAMES * 100), sink);
		assertTrue("under 50 microseconds a frame", nanos / (FRAMES * 100) < 50_000);
	}
}
