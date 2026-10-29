package com.vinnyvangogh.combatvitals;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import net.runelite.api.gameval.SpriteID;

/**
 * Hides the game's own overhead health bar by overriding its sprites with
 * transparent ones of the same size.
 *
 * Global, not per actor: the client looks sprites up by id alone, so an
 * override applies to every standard health bar on screen, yours included.
 * See docs/native-hp-bar.md for why there is no per-actor route.
 *
 * Interface Styles' "HD health bars" overrides these same ids. Rather than
 * fight it, this remembers whatever override it displaced and puts it back on
 * restore, and {@link #apply()} is called every client tick so that when
 * Interface Styles rewrites or clears the map ours is reasserted within a
 * frame. Must only be called on the client thread.
 *
 * @param <S> the sprite type; {@code SpritePixels} in the client, anything in
 *            tests, since only identity is compared
 */
final class NativeHealthBarHider<S>
{
	/** Front and back sprites of every standard-width health bar, 30px to 160px. */
	static final int[] SPRITE_IDS = {
		SpriteID.StandardHealth30.FRONT, SpriteID.StandardHealth30.BACK,
		SpriteID.StandardHealth40.FRONT, SpriteID.StandardHealth40.BACK,
		SpriteID.StandardHealth50.FRONT, SpriteID.StandardHealth50.BACK,
		SpriteID.StandardHealth60.FRONT, SpriteID.StandardHealth60.BACK,
		SpriteID.StandardHealth70.FRONT, SpriteID.StandardHealth70.BACK,
		SpriteID.StandardHealth80.FRONT, SpriteID.StandardHealth80.BACK,
		SpriteID.StandardHealth90.FRONT, SpriteID.StandardHealth90.BACK,
		SpriteID.StandardHealth100.FRONT, SpriteID.StandardHealth100.BACK,
		SpriteID.StandardHealth120.FRONT, SpriteID.StandardHealth120.BACK,
		SpriteID.StandardHealth140.FRONT, SpriteID.StandardHealth140.BACK,
		SpriteID.StandardHealth160.FRONT, SpriteID.StandardHealth160.BACK,
	};

	/** The client's sprite override map, keyed by sprite id. */
	private final Map<Integer, S> overrides;

	/** Builds the transparent stand-in for one sprite id. */
	private final IntFunction<S> blankFor;

	/** Our transparent sprites, by id. Empty means nothing is hidden. */
	private final Map<Integer, S> ours = new HashMap<>();

	/** Another plugin's override we replaced, to hand back on restore. */
	private final Map<Integer, S> displaced = new HashMap<>();

	NativeHealthBarHider(Map<Integer, S> overrides, IntFunction<S> blankFor)
	{
		this.overrides = overrides;
		this.blankFor = blankFor;
	}

	/**
	 * Installs the transparent sprites, or reasserts them if something
	 * replaced or removed them.
	 *
	 * @return true when the map changed, meaning the client's health bar
	 *         sprite cache has to be reset for the change to show
	 */
	boolean apply()
	{
		boolean changed = false;
		for (int id : SPRITE_IDS)
		{
			S mine = ours.get(id);
			if (mine == null)
			{
				mine = blankFor.apply(id);
				if (mine == null)
				{
					continue;
				}
				ours.put(id, mine);
			}

			final S current = overrides.get(id);
			if (current == mine)
			{
				continue;
			}

			// Whatever sits there now is the latest word from everyone else:
			// remember it, or forget the old one if they cleared it.
			if (current == null)
			{
				displaced.remove(id);
			}
			else
			{
				displaced.put(id, current);
			}

			overrides.put(id, mine);
			changed = true;
		}
		return changed;
	}

	/**
	 * Takes our sprites out and puts back anything they displaced. An id
	 * another plugin has overwritten since is left with that plugin.
	 *
	 * @return true when the map changed
	 */
	boolean restore()
	{
		if (ours.isEmpty())
		{
			return false;
		}

		boolean changed = false;
		for (Map.Entry<Integer, S> entry : ours.entrySet())
		{
			final int id = entry.getKey();
			if (overrides.get(id) != entry.getValue())
			{
				continue;
			}

			final S previous = displaced.get(id);
			if (previous != null)
			{
				overrides.put(id, previous);
			}
			else
			{
				overrides.remove(id);
			}
			changed = true;
		}

		ours.clear();
		displaced.clear();
		return changed;
	}

	boolean isActive()
	{
		return !ours.isEmpty();
	}
}
