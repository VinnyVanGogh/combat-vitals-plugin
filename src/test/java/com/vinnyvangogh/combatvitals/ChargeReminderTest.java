package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChargeReminderTest
{
	@Test
	public void unknownChargesNeverAlert()
	{
		final ChargeReminder r = new ChargeReminder(2);
		assertFalse(r.used());
		assertEquals(-1, r.charges());
		assertFalse(r.low());
	}

	@Test
	public void alertsOnceWhenReachingThreshold()
	{
		final ChargeReminder r = new ChargeReminder(2);
		assertFalse(r.update(4));
		assertFalse(r.used());
		assertTrue(r.used());
		assertFalse(r.used());
		assertEquals(1, r.charges());
		assertTrue(r.low());
	}

	@Test
	public void rearmsAfterRecharge()
	{
		final ChargeReminder r = new ChargeReminder(1);
		assertTrue(r.update(1));
		assertFalse(r.update(0));
		assertFalse(r.update(10));
		assertTrue(r.update(1));
	}

	@Test
	public void chargesNeverGoNegativeAndResetClears()
	{
		final ChargeReminder r = new ChargeReminder(0);
		r.update(0);
		r.used();
		assertEquals(0, r.charges());
		r.reset();
		assertEquals(-1, r.charges());
		assertTrue(r.update(0));
	}
}
