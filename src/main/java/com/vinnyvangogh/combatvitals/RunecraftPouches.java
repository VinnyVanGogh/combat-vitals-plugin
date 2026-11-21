package com.vinnyvangogh.combatvitals;

/**
 * Essence pouch usage: fill and empty counts, essence held, and uses left
 * before a pouch degrades. Information only; it reads what the player did.
 *
 * Capacities and use limits are plain numbers passed in by the caller so they
 * can be corrected without touching this class. The values in {@link Kind}
 * are the commonly documented ones and are UNVERIFIED against the live game.
 *
 * Pure: nothing is read from the client.
 */
final class RunecraftPouches
{
	enum Kind
	{
		SMALL(3, 0),
		MEDIUM(6, 45),
		LARGE(9, 31),
		GIANT(12, 10),
		COLOSSAL(40, 0);

		final int capacity;
		/** Fills before the pouch degrades; 0 means unknown or does not degrade. */
		final int usesBeforeDegrade;

		Kind(int capacity, int usesBeforeDegrade)
		{
			this.capacity = capacity;
			this.usesBeforeDegrade = usesBeforeDegrade;
		}
	}

	private final Kind kind;
	private int held;
	private int fills;
	private int empties;
	private int uses;

	RunecraftPouches(Kind kind)
	{
		this.kind = kind;
	}

	/** Essence offered by the player; returns how many actually fit. */
	int fill(int essence)
	{
		final int moved = Math.max(0, Math.min(essence, kind.capacity - held));
		if (moved > 0)
		{
			held += moved;
			fills++;
			uses++;
		}
		return moved;
	}

	/** Empty the pouch; returns the essence taken out. */
	int empty()
	{
		final int moved = held;
		if (moved > 0)
		{
			held = 0;
			empties++;
		}
		return moved;
	}

	/** The pouch was repaired (NPC Contact, Dark Mage): its use count restarts. */
	void repaired()
	{
		uses = 0;
	}

	void reset()
	{
		held = 0;
		fills = 0;
		empties = 0;
		uses = 0;
	}

	int held()
	{
		return held;
	}

	int capacity()
	{
		return kind.capacity;
	}

	int fills()
	{
		return fills;
	}

	int empties()
	{
		return empties;
	}

	/** Fills left before degrade, or -1 when the limit is unknown. */
	int usesLeft()
	{
		return kind.usesBeforeDegrade <= 0 ? -1 : Math.max(0, kind.usesBeforeDegrade - uses);
	}

	/** True when a known limit has been used up and a repair is due. */
	boolean needsRepair()
	{
		return usesLeft() == 0;
	}
}
