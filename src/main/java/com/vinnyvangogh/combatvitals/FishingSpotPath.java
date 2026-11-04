package com.vinnyvangogh.combatvitals;

/** When to find the next fishing spot and send it to Shortest Path. */
enum FishingSpotPath
{
	OFF("Off"),
	ON_SPOT_MOVE("On spot move"),
	RIGHT_CLICK("Right-click only");

	private final String label;

	FishingSpotPath(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
