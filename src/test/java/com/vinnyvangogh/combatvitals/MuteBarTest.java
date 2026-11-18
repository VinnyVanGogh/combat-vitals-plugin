package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.awt.Component;
import java.awt.Container;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.Before;
import org.junit.Test;

/** The panel's mute buttons clicked headless, and the mute surviving a panel built again. */
public class MuteBarTest
{
	private static final long T0 = 1_700_000_000_000L;

	private final AtomicLong now = new AtomicLong(T0);
	private final AlertMute mute = new AlertMute(now::get, ZoneOffset.UTC);
	private VitalsSidePanel panel;
	private SwingDriver ui;

	@Before
	public void setUp()
	{
		build();
	}

	private void build()
	{
		SwingDriver.onEdt(() ->
		{
			RuneLiteLook.install();
			panel = new VitalsSidePanel(PanelFixture.store(), PanelFixture.descriptors(), null, t ->
			{
			}, mute);
			ui = new SwingDriver(panel);
			ui.layout(VitalsSidePanelShots.WIDTH);
		});
	}

	private JButton button(String name)
	{
		return ui.find(JButton.class, b -> name.equals(b.getName()), "button " + name);
	}

	private JLabel status()
	{
		// The finder only sees visible components, and this label is hidden while not muted.
		final JLabel found = findNamed(ui.root(), "mute-status");
		if (found == null)
		{
			throw new AssertionError("No mute status label in the panel");
		}
		return found;
	}

	private static JLabel findNamed(Container c, String name)
	{
		for (Component child : c.getComponents())
		{
			if (child instanceof JLabel && name.equals(child.getName()))
			{
				return (JLabel) child;
			}
			if (child instanceof Container)
			{
				final JLabel inner = findNamed((Container) child, name);
				if (inner != null)
				{
					return inner;
				}
			}
		}
		return null;
	}

	private void click(String name)
	{
		SwingDriver.onEdt(() -> button(name).doClick());
	}

	@Test
	public void showsNoStatusWhileNotMuted()
	{
		SwingDriver.onEdt(() ->
		{
			assertEquals(MuteBar.MUTE, button("mute-toggle").getText());
			assertFalse(status().isVisible());
		});
	}

	@Test
	public void theToggleMutesAndUnmutes()
	{
		click("mute-toggle");
		assertTrue(mute.muted());
		assertTrue(mute.untilUnmuted());
		SwingDriver.onEdt(() ->
		{
			assertEquals(MuteBar.UNMUTE, button("mute-toggle").getText());
			assertTrue(status().isVisible());
		});
		click("mute-toggle");
		assertFalse(mute.muted());
		SwingDriver.onEdt(() -> assertFalse(status().isVisible()));
	}

	@Test
	public void aTimedButtonMutesAndTheLineShowsWhenItEnds()
	{
		click("mute-30");
		assertTrue(mute.muted());
		SwingDriver.onEdt(() -> assertEquals("Muted until 22:43", status().getText()));
	}

	@Test
	public void theLineClearsOnRefreshOnceTheTimeIsUp()
	{
		click("mute-15");
		now.set(T0 + 15 * 60_000L);
		SwingDriver.onEdt(() ->
		{
			panel.refresh();
			assertFalse(status().isVisible());
			assertEquals(MuteBar.MUTE, button("mute-toggle").getText());
		});
	}

	@Test
	public void aPanelBuiltAgainShowsTheSameMute()
	{
		click("mute-60");
		build();
		SwingDriver.onEdt(() ->
		{
			assertEquals(MuteBar.UNMUTE, button("mute-toggle").getText());
			assertEquals("Muted until 23:13", status().getText());
		});
		assertTrue(mute.muted());
	}
}
