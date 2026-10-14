package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OverheadChatTest
{
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
	public void readoutOverlappingChatMovesBelowIt()
	{
		// Chat bottom at 100, readout top at 90: down by 10 plus the gap.
		assertEquals(10 + OverheadChat.GAP, OverheadChat.shiftBelow(90, 100));
	}

	@Test
	public void readoutAboveChatMovesBelowIt()
	{
		// Fully above the chat still puts the chat underneath the readout,
		// where it reads as part of the readout. It belongs on top.
		assertEquals(60 + OverheadChat.GAP, OverheadChat.shiftBelow(40, 100));
	}

	@Test
	public void readoutAlreadyBelowChatStaysPut()
	{
		assertEquals(0, OverheadChat.shiftBelow(100 + OverheadChat.GAP, 100));
		assertEquals(0, OverheadChat.shiftBelow(150, 100));
	}
}
