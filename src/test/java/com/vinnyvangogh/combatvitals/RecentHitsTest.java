package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecentHitsTest
{
	@Test
	public void neverHitIsNotRecent()
	{
		assertFalse(new RecentHits<Object>().recent(new Object(), 1000));
		assertFalse(new RecentHits<Object>().recent(null, 1000));
	}

	@Test
	public void recentInsideWindowOnly()
	{
		final RecentHits<Object> hits = new RecentHits<>();
		final Object actor = new Object();
		hits.hit(actor, 1000);
		assertTrue(hits.recent(actor, 1000));
		assertTrue(hits.recent(actor, 1000 + RecentHits.WINDOW_MILLIS - 1));
		assertFalse(hits.recent(actor, 1000 + RecentHits.WINDOW_MILLIS));
	}

	@Test
	public void otherActorUnaffected()
	{
		final RecentHits<Object> hits = new RecentHits<>();
		hits.hit(new Object(), 1000);
		assertFalse(hits.recent(new Object(), 1001));
	}

	@Test
	public void newHitRestartsWindow()
	{
		final RecentHits<Object> hits = new RecentHits<>();
		final Object actor = new Object();
		hits.hit(actor, 0);
		hits.hit(actor, 5000);
		assertTrue(hits.recent(actor, 10000));
	}
}
