package com.vinnyvangogh.combatvitals;

/**
 * Tells when the player has clearly stopped working a skilling NPC, so the
 * badge can switch to "Not fishing" within a tick or two instead of after the
 * whole linger (Board's in-game test, 2026-11-06: walking away took the
 * timeout, 5 s by default, about 8 ticks, before the badge changed).
 *
 * Read once per game tick, while a skilling NPC is held:
 * <ul>
 * <li>Still interacting with it, or a click on an NPC waiting for its
 * interaction to arrive: working. Nothing changes.</li>
 * <li>The interaction has ended and the player's tile changed: they walked
 * off. Stopped at once. Nobody moves while fishing, pickpocketing or
 * catching; walking up to the next spot starts with a click on it, which
 * counts as working until its interaction arrives.</li>
 * <li>Fishing only: the interaction has ended and the player has stood still
 * without animating for {@link #STILL_TICKS} ticks: the spot moved or the
 * inventory filled. Stopped. While fishing the game keeps the player on the
 * spot through every gap in the animation, so only a real end of the
 * interaction starts this count. Thieving and hunting are a click per
 * attempt, with the interaction ending between them, so they keep the
 * ordinary linger instead.</li>
 * </ul>
 *
 * Pure: booleans and packed tiles in, a decision out.
 */
final class SkillingStop
{
	/** Ticks standing still, not animating and not on the spot, before fishing counts as stopped. */
	static final int STILL_TICKS = 2;

	/** A tile not read yet. */
	static final long UNKNOWN = SpotPathing.UNKNOWN;

	private long lastTile = UNKNOWN;
	private int stillTicks;

	/**
	 * @param working    the player is still interacting with the NPC, or has
	 *                   just clicked an NPC whose interaction has not arrived
	 * @param tile       the player's tile, {@link SpotPathing#tile}, or {@link #UNKNOWN}
	 * @param animating  the player is animating
	 * @param fishing    the NPC is a fishing spot
	 * @return true when the work has clearly stopped
	 */
	boolean tick(boolean working, long tile, boolean animating, boolean fishing)
	{
		final boolean moved = lastTile != UNKNOWN && tile != UNKNOWN && tile != lastTile;
		lastTile = tile;
		if (working)
		{
			stillTicks = 0;
			return false;
		}
		if (moved)
		{
			return true;
		}
		if (!fishing || animating)
		{
			stillTicks = 0;
			return false;
		}
		return ++stillTicks >= STILL_TICKS;
	}

	/** A new engagement, or none: forget the last tile and the count. */
	void reset()
	{
		lastTile = UNKNOWN;
		stillTicks = 0;
	}
}
