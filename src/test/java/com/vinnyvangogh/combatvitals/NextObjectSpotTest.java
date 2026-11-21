package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class NextObjectSpotTest
{
	private static NextObjectSpot.Candidate c(int x, int y, int plane)
	{
		return new NextObjectSpot.Candidate(x, y, plane);
	}

	@Test
	public void picksTheNearestByTileDistance()
	{
		final NextObjectSpot.Candidate near = c(3203, 3202, 0);
		final NextObjectSpot.Candidate far = c(3220, 3200, 0);

		assertEquals(near, NextObjectSpot.nearest(Arrays.asList(far, near), 3200, 3200, 0, 0, 0, 0));
	}

	@Test
	public void neverTheExcludedTile()
	{
		final NextObjectSpot.Candidate worked = c(3201, 3200, 0);
		final NextObjectSpot.Candidate other = c(3210, 3200, 0);

		assertEquals(other, NextObjectSpot.nearest(Arrays.asList(worked, other), 3200, 3200, 0, 3201, 3200, 0));
	}

	@Test
	public void aTieGoesToTheLowerXThenLowerY()
	{
		final NextObjectSpot.Candidate low = c(3198, 3200, 0);
		final NextObjectSpot.Candidate high = c(3202, 3200, 0);

		assertEquals(low, NextObjectSpot.nearest(Arrays.asList(high, low), 3200, 3200, 0, 0, 0, 0));
		assertEquals(low, NextObjectSpot.nearest(Arrays.asList(low, high), 3200, 3200, 0, 0, 0, 0));
	}

	@Test
	public void thisPlaneBeatsACloserTileOnAnother()
	{
		final NextObjectSpot.Candidate upstairs = c(3200, 3201, 1);
		final NextObjectSpot.Candidate here = c(3230, 3200, 0);

		assertEquals(here, NextObjectSpot.nearest(Arrays.asList(upstairs, here), 3200, 3200, 0, 0, 0, 0));
	}

	@Test
	public void noneLeftIsNull()
	{
		assertNull(NextObjectSpot.nearest(Collections.<NextObjectSpot.Candidate>emptyList(), 0, 0, 0, 0, 0, 0));
		assertNull(NextObjectSpot.nearest(Collections.singletonList(c(5, 5, 0)), 0, 0, 0, 5, 5, 0));
	}
}
