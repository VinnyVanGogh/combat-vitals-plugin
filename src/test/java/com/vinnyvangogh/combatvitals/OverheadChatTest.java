package com.vinnyvangogh.combatvitals;

import java.awt.Rectangle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OverheadChatTest
{
	/** The readout's box: x 100..160, y 200..216. */
	private static final Rectangle READOUT = new Rectangle(100, 200, 60, 16);
	private static final int DESCENT = 3;

	@Test
	public void chatWithTimeLeftIsShowing()
	{
		assertTrue(OverheadChat.showing("Hello", 150));
		assertTrue(OverheadChat.showing("Hello", 1));
	}

	@Test
	public void expiredChatIsNotShowing()
	{
		// The client keeps the old text after the countdown runs out.
		assertFalse(OverheadChat.showing("Hello", 0));
		assertFalse(OverheadChat.showing("Hello", -5));
	}

	@Test
	public void emptyOrNullTextIsIgnored()
	{
		assertFalse(OverheadChat.showing(null, 150));
		assertFalse(OverheadChat.showing("", 150));
		assertFalse(OverheadChat.showing("   ", 150));
	}

	@Test
	public void coveredChatIsDrawnJustAboveTheReadout()
	{
		// The game's line sits right on the readout.
		final Rectangle chat = new Rectangle(90, 205, 80, 15);

		final Integer baseline = OverheadChat.baselineAbove(READOUT, chat, DESCENT);

		// The copy's bottom edge (baseline + descent) is GAP above the readout.
		assertEquals(READOUT.y - OverheadChat.GAP - DESCENT, (int) baseline);
	}

	@Test
	public void partlyCoveredChatIsStillRedrawn()
	{
		// One pixel row of overlap is enough to mangle the line.
		final Rectangle chat = new Rectangle(90, 186, 80, 15);

		assertEquals(READOUT.y - OverheadChat.GAP - DESCENT, (int) OverheadChat.baselineAbove(READOUT, chat, DESCENT));
	}

	@Test
	public void chatClearOfTheReadoutIsLeftToTheGame()
	{
		// Above, below, or off to the side: the game's own line is readable,
		// and a copy would show the same message twice.
		assertNull(OverheadChat.baselineAbove(READOUT, new Rectangle(90, 150, 80, 15), DESCENT));
		assertNull(OverheadChat.baselineAbove(READOUT, new Rectangle(90, 230, 80, 15), DESCENT));
		assertNull(OverheadChat.baselineAbove(READOUT, new Rectangle(300, 205, 80, 15), DESCENT));
	}

	@Test
	public void readoutPositionIsNeverChanged()
	{
		final Rectangle readout = new Rectangle(READOUT);

		OverheadChat.baselineAbove(readout, new Rectangle(90, 205, 80, 15), DESCENT);

		assertEquals(READOUT, readout);
	}
}
