package com.vinnyvangogh.combatvitals;

/**
 * Motherlode Mine sack numbers: capacity, what is waiting, deposits this
 * session and the level the player is on.
 *
 * Capacity is 81 pay-dirt, 162 once the sack upgrade is bought (OSRS wiki,
 * Motherlode Mine). The sack count comes from the game's own varbit; a drop
 * in it with no deposit is a collect, a rise is a deposit.
 *
 * Pure. Not thread safe; the plugin touches it from the client thread only.
 */
final class MotherlodeSack
{
	static final int BASE_CAPACITY = 81;
	static final int UPGRADED_CAPACITY = 162;

	private int sack = -1;
	private int deposits;
	private int paydirtDeposited;

	static int capacity(boolean upgraded)
	{
		return upgraded ? UPGRADED_CAPACITY : BASE_CAPACITY;
	}

	/** The sack count changed. The first reading only sets the baseline. */
	void sackChanged(int count)
	{
		if (sack >= 0 && count > sack)
		{
			deposits++;
			paydirtDeposited += count - sack;
		}
		sack = count;
	}

	int sack()
	{
		return Math.max(0, sack);
	}

	/** Deposits made this session. */
	int deposits()
	{
		return deposits;
	}

	/** Pay-dirt put into the sack this session. */
	int paydirtDeposited()
	{
		return paydirtDeposited;
	}

	/** True when a deposit of {@code inventory} pay-dirt would overflow the sack. */
	static boolean wouldOverflow(int sack, int inventory, boolean upgraded)
	{
		return sack + inventory > capacity(upgraded);
	}

	/** Percent full, 0 to 100. */
	static int percentFull(int sack, boolean upgraded)
	{
		return Math.min(100, Math.max(0, sack) * 100 / capacity(upgraded));
	}

	/** "Sack 45/81 | carrying 12 pay-dirt | 3 deposits | 2 nuggets", with a warning when a deposit would overflow. */
	String label(int inventoryPaydirt, boolean upgraded, int nuggets)
	{
		final StringBuilder sb = new StringBuilder("Sack ").append(sack()).append('/').append(capacity(upgraded));
		if (wouldOverflow(sack(), inventoryPaydirt, upgraded))
		{
			sb.append(" (full)");
		}
		sb.append(" | carrying ").append(inventoryPaydirt).append(" pay-dirt");
		sb.append(" | ").append(deposits).append(deposits == 1 ? " deposit" : " deposits");
		sb.append(" | ").append(nuggets).append(nuggets == 1 ? " nugget" : " nuggets");
		return sb.toString();
	}

	void reset()
	{
		sack = -1;
		deposits = 0;
		paydirtDeposited = 0;
	}
}
