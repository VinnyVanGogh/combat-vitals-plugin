package com.vinnyvangogh.combatvitals;

/**
 * Charge counter with a low-charge alert, for the Runecrafting "pneck
 * reminder" feature. Information and alert only.
 *
 * Which item the Hub "Pneck reminder" plugin tracks, and its charge count,
 * are UNVERIFIED (its page was not read); the caller supplies both, so this
 * class does not hard-code an item.
 *
 * The alert fires once each time charges drop to the threshold, and re-arms
 * only after charges rise back above it (recharged or swapped).
 *
 * Pure: nothing is read from the client.
 */
final class ChargeReminder
{
	private final int threshold;
	private int charges = -1;
	private boolean armed = true;

	ChargeReminder(int threshold)
	{
		this.threshold = Math.max(0, threshold);
	}

	/** Set the charges read from the item (tooltip or chat); true if the alert should fire now. */
	boolean update(int newCharges)
	{
		charges = Math.max(0, newCharges);
		if (charges > threshold)
		{
			armed = true;
			return false;
		}
		if (armed)
		{
			armed = false;
			return true;
		}
		return false;
	}

	/** One charge used; true if the alert should fire now. Ignored while charges are unknown. */
	boolean used()
	{
		return charges >= 0 && update(charges - 1);
	}

	/** Charges left, or -1 when not yet known. */
	int charges()
	{
		return charges;
	}

	boolean low()
	{
		return charges >= 0 && charges <= threshold;
	}

	void reset()
	{
		charges = -1;
		armed = true;
	}
}
