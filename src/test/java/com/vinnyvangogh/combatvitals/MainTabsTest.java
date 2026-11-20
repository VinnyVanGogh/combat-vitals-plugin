package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import javax.swing.JToggleButton;
import net.runelite.client.util.ImageUtil;
import org.junit.Before;
import org.junit.Test;

/** The sidebar's main tabs: switching, the mute bar on each, the last tab kept, and the bundled icons. */
public class MainTabsTest
{
	private PanelFixture.Store store;
	private VitalsSidePanel panel;
	private SwingDriver ui;

	@Before
	public void setUp()
	{
		store = PanelFixture.store();
		build();
	}

	private void build()
	{
		SwingDriver.onEdt(() ->
		{
			RuneLiteLook.install();
			panel = new VitalsSidePanel(store, PanelFixture.descriptors(), null, t ->
			{
			});
			ui = new SwingDriver(panel);
			ui.layout(VitalsSidePanelShots.WIDTH);
		});
	}

	private void click(VitalsSidePanel.Main m)
	{
		SwingDriver.onEdt(() ->
		{
			ui.find(JToggleButton.class, b -> ("main-" + m.name()).equals(b.getName()), "tab " + m).doClick();
			ui.layout(VitalsSidePanelShots.WIDTH);
		});
	}

	@Test
	public void opensOnLiveAndEachTabShowsItsOwnContent()
	{
		assertEquals(VitalsSidePanel.Main.LIVE, SwingDriver.onEdt(panel::main));
		ui.assertShows(LiveSection.LOGGED_OUT);
		click(VitalsSidePanel.Main.GOALS);
		ui.assertHides(LiveSection.LOGGED_OUT);
		click(VitalsSidePanel.Main.OVERLAYS);
		ui.assertShows(VitalsSidePanel.SHOW_ALL);
		click(VitalsSidePanel.Main.PRESETS);
		ui.assertShows("Minimal");
		ui.assertHides(VitalsSidePanel.SHOW_ALL);
		click(VitalsSidePanel.Main.SETTINGS);
		ui.assertShows("Combat vitals");
		click(VitalsSidePanel.Main.LIVE);
		ui.assertShows(LiveSection.LOGGED_OUT);
	}

	@Test
	public void theMuteBarIsOnEveryTab()
	{
		for (VitalsSidePanel.Main m : VitalsSidePanel.Main.values())
		{
			click(m);
			ui.assertShows(MuteBar.MUTE);
		}
	}

	@Test
	public void theLastTabIsRememberedAndComesBack()
	{
		click(VitalsSidePanel.Main.OVERLAYS);
		assertEquals("OVERLAYS", store.values.get(VitalsSidePanel.TAB_KEY));
		build();
		assertEquals(VitalsSidePanel.Main.OVERLAYS, SwingDriver.onEdt(panel::main));
		ui.assertShows(VitalsSidePanel.SHOW_ALL);
		ui.assertHides(LiveSection.LOGGED_OUT);
	}

	@Test
	public void anUnknownStoredTabFallsBackToLive()
	{
		store.values.put(VitalsSidePanel.TAB_KEY, "GONE");
		build();
		assertEquals(VitalsSidePanel.Main.LIVE, SwingDriver.onEdt(panel::main));
	}

	@Test
	public void everyTabHasABundledIcon()
	{
		for (VitalsSidePanel.Main m : VitalsSidePanel.Main.values())
		{
			assertNotNull(m.icon, VitalsSidePanel.class.getResourceAsStream("/com/vinnyvangogh/combatvitals/icons/"
				+ m.icon));
			assertTrue(m.icon, ImageUtil.loadImageResource(VitalsSidePanel.class,
				"/com/vinnyvangogh/combatvitals/icons/" + m.icon).getWidth() > 0);
		}
	}
}
