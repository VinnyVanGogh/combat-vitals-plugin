package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import org.junit.Before;
import org.junit.Test;

/** The sidebar's overlay switches and Reset session button, clicked through headless. */
public class SidebarOverlaysTest
{
	private PanelFixture.Store store;
	private VitalsSidePanel panel;
	private SwingDriver ui;

	@Before
	public void setUp()
	{
		store = PanelFixture.store();
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

	private JCheckBox overlay(String key)
	{
		return ui.find(JCheckBox.class, b -> ("overlay-" + key).equals(b.getName()), "overlay " + key);
	}

	@Test
	public void switchesMatchTheSettingsOnOpen()
	{
		SwingDriver.onEdt(() ->
		{
			assertTrue(overlay("showPanel").isSelected());
			assertFalse(overlay("pillarMode").isSelected());
			assertFalse(overlay("aggressionAreaLines").isSelected());
		});
	}

	@Test
	public void tickingASwitchWritesTheSettingsOwnKey()
	{
		SwingDriver.onEdt(() -> overlay("showPanel").doClick());
		assertEquals(Boolean.FALSE, store.get("showPanel", Boolean.class));
	}

	@Test
	public void aChangeInTheListShowsInThePanelOnRefresh()
	{
		store.set("showOverhead", false);
		SwingDriver.onEdt(() ->
		{
			panel.refresh();
			assertFalse(overlay("showOverhead").isSelected());
		});
	}

	@Test
	public void hideAllThenShowAll()
	{
		SwingDriver.onEdt(() -> ui.find(AbstractButton.class, b -> VitalsSidePanel.HIDE_ALL.equals(b.getText()),
			"hide all").doClick());
		assertEquals(Boolean.FALSE, store.get("showPanel", Boolean.class));
		assertEquals(PillarMode.OFF, store.get("pillarMode", PillarMode.class));
		SwingDriver.onEdt(() -> ui.find(AbstractButton.class, b -> VitalsSidePanel.SHOW_ALL.equals(b.getText()),
			"show all").doClick());
		assertEquals(Boolean.TRUE, store.get("skillingEnabled", Boolean.class));
		assertEquals(PillarMode.BOTH, store.get("pillarMode", PillarMode.class));
		assertEquals(Boolean.FALSE, store.get("aggressionAreaLines", Boolean.class));
	}

	@Test
	public void resetSessionButtonTicksTheResetKey()
	{
		SwingDriver.onEdt(() -> ui.find(AbstractButton.class, b -> "Skilling".equals(b.getText()),
			"skilling tab").doClick());
		// The Session card is folded shut until its header is clicked.
		SwingDriver.onEdt(() -> ui.find(AbstractButton.class, b -> "Session".equals(b.getText()),
			"session card header").doClick());
		SwingDriver.onEdt(() -> ui.find(AbstractButton.class, b -> "reset-session".equals(b.getName()),
			"reset session").doClick());
		assertEquals(Boolean.TRUE, store.get("skillingSessionReset", Boolean.class));
	}
}
