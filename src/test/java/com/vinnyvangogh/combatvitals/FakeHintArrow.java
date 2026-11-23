package com.vinnyvangogh.combatvitals;

/** A stand-in for the game's single hint arrow slot: one arrow at a time, ours or a quest's. */
final class FakeHintArrow implements NextSpotArrow.Pointer
{
	/** The target of the arrow this plugin set, or null. */
	NextSpotArrow.Target shown;

	/** A quest or clue's arrow is up. */
	boolean foreign;

	int shows;
	int hides;

	@Override
	public boolean hasArrow()
	{
		return shown != null || foreign;
	}

	@Override
	public boolean showing(NextSpotArrow.Target target)
	{
		return shown == target;
	}

	@Override
	public void show(NextSpotArrow.Target target)
	{
		shown = target;
		foreign = false;
		shows++;
	}

	@Override
	public void hide()
	{
		shown = null;
		foreign = false;
		hides++;
	}

	/** The game (a quest) takes the slot over from us. */
	void takenOverByQuest()
	{
		shown = null;
		foreign = true;
	}
}
