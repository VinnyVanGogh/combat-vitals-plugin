package com.vinnyvangogh.combatvitals;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The sound one activity alert makes. {@link #RUNELITE} is RuneLite's own
 * notification sound, as before this setting existed; the packs are bundled
 * clips, {@link #VOICE} says what happened, and {@link #MY_SOUNDS} plays the
 * user's own file from {@code ~/.runelite/combat-vitals/sounds/}.
 */
@AllArgsConstructor
public enum AlertSound
{
	RUNELITE("RuneLite", null),
	SOFT_CHIME("Soft chime", "sounds/soft-chime.wav"),
	MUSIC_BOX("Music box", "sounds/music-box.wav"),
	RETRO("Retro blip", "sounds/retro-blip.wav"),
	VOICE("Voice line", null),
	MY_SOUNDS("My sounds", null);

	@Getter
	private final String name;

	/** The bundled clip's class resource path, for a pack sound; null otherwise. */
	@Getter
	private final String resource;

	@Override
	public String toString()
	{
		return name;
	}
}
