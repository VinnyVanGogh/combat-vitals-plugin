package com.vinnyvangogh.combatvitals;

/**
 * Auto splash detection: Splashing mode follows the worn Magic attack bonus,
 * on at or under the threshold and off above it. A hand-made switch wins until
 * the gear next crosses the threshold, so flipping the mode never gets undone
 * a tick later. Pure state, no client calls.
 */
final class SplashAuto
{
	private boolean wantSplash;
	private boolean seen;
	private boolean overridden;

	/**
	 * Called each tick with the worn Magic attack bonus.
	 *
	 * @return the mode to switch to, or null to leave it as it is
	 */
	Boolean step(int magicBonus, int threshold, boolean currentlySplashing)
	{
		final boolean want = magicBonus <= threshold;
		if (!seen)
		{
			seen = true;
			wantSplash = want;
		}
		else if (want != wantSplash)
		{
			wantSplash = want;
			overridden = false;
		}
		if (overridden || want == currentlySplashing)
		{
			return null;
		}
		return want;
	}

	/** The player switched the mode by hand: leave it until the gear crosses the threshold. */
	void manualSwitch()
	{
		overridden = true;
	}

	void reset()
	{
		seen = false;
		overridden = false;
	}
}
