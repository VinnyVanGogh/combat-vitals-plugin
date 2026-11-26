package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.Notification;
import org.junit.Before;
import org.junit.Test;

/**
 * The sidebar panel clicked through in JUnit, headless: nothing leaves the
 * component tree (SwingDriver). The panel and RuneLite's list show the same
 * values: a change stored by the list shows in the panel on refresh (what the
 * plugin's ConfigChanged handler runs), and a change made in the panel is
 * stored under the list's own key.
 */
public class VitalsSidePanelTest
{
	private PanelFixture.Store store;
	private final List<ActivityAlerts.Type> tested = new ArrayList<>();
	private VitalsSidePanel panel;
	private SwingDriver ui;

	@Before
	public void setUp()
	{
		store = PanelFixture.store();
		SwingDriver.onEdt(() ->
		{
			RuneLiteLook.install();
			panel = new VitalsSidePanel(store, PanelFixture.descriptors(), null, tested::add);
			panel.show(PanelLayout.Tab.COMBAT);
			ui = new SwingDriver(panel);
			ui.layout(VitalsSidePanelShots.WIDTH);
		});
	}

	private void onEdt(Runnable r)
	{
		SwingDriver.onEdt(() ->
		{
			r.run();
			ui.layout(VitalsSidePanelShots.WIDTH);
		});
	}

	private JCheckBox checkbox(String name)
	{
		return ui.find(JCheckBox.class, b -> name.equals(SwingDriver.plain(b.getText())), "checkbox " + name);
	}

	/** Each alert is its own card, folded until opened: opens {@code type}'s. Call on the EDT. */
	private void openAlert(String type)
	{
		ui.find(FoldCard.class, c -> ("alert-" + type).equals(c.getName()), "alert card " + type).setOpen(true);
	}

	@Test
	public void buildingAndRedrawingWritesNothing()
	{
		onEdt(() ->
		{
			panel.refresh();
			for (PanelLayout.Tab tab : PanelLayout.Tab.values())
			{
				panel.show(tab);
			}
			panel.refresh();
		});
		assertTrue("writes on load: " + store.writes, store.writes.isEmpty());
	}

	@Test
	public void everyPlacedSettingHasItsListNameInThePanel()
	{
		final Map<String, ConfigItemDescriptor> items = PanelFixture.descriptors();
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			for (String key : PanelLayout.listOrder(tab))
			{
				assertNotNull(key, items.get(key));
			}
		}
		onEdt(() -> panel.show(PanelLayout.Tab.COMBAT));
		ui.assertShows(items.get("showPanel").name());
		ui.assertShows("Combat vitals");
	}

	@Test
	public void aChangeInTheListShowsInThePanel()
	{
		assertTrue(SwingDriver.onEdt(() -> checkbox("Show panel").isSelected()));
		store.values.put("showPanel", false);
		onEdt(panel::refresh);
		assertFalse(SwingDriver.onEdt(() -> checkbox("Show panel").isSelected()));
	}

	@Test
	public void aChangeInThePanelIsStoredUnderTheListsKey()
	{
		onEdt(() -> ui.click(checkbox("Show panel")));
		assertEquals(Boolean.FALSE, store.values.get("showPanel"));
		assertEquals("[showPanel]", store.writes.toString());
	}

	@Test
	public void choicesAndNumbersWriteTheirOwnType()
	{
		onEdt(() -> panel.show(PanelLayout.Tab.LOOK));
		SwingDriver.onEdt(() ->
		{
			final JComboBox<?> position = ui.find(JComboBox.class,
				b -> b.getItemCount() > 0 && b.getItemAt(0) == BadgePosition.OVER_HEAD, "badge position");
			ui.select(position, BadgePosition.FIXED);
			final JSpinner scale = ui.find(JSpinner.class,
				s -> s.getToolTipText() != null && s.getToolTipText().contains("badge"), "a badge spinner");
			scale.setValue(((Integer) scale.getValue()) + 1);
		});
		assertEquals(BadgePosition.FIXED, store.values.get("skillingPosition"));
		assertTrue(store.writes.contains("skillingPosition"));
	}

	@Test
	public void anAlertsSwitchKeepsTheRestOfItsNotification()
	{
		onEdt(() -> panel.show(PanelLayout.Tab.ALERTS));
		onEdt(() -> openAlert("INVENTORY_FULL"));
		onEdt(() -> ui.click(checkbox("Inventory full")));
		final Notification n = (Notification) store.values.get("alertInventoryFull");
		assertFalse(n.isEnabled());
	}

	@Test
	public void sameForEveryAlertHidesEachAlertsOwnSound()
	{
		onEdt(() -> panel.show(PanelLayout.Tab.ALERTS));
		ui.assertHides("Full inv sound");
		onEdt(() -> openAlert("INVENTORY_FULL"));
		ui.assertShows("Full inv sound");
		ui.assertHides("Sound for all");

		store.values.put(PanelLayout.SOUND_SHARED, true);
		onEdt(panel::refresh);
		ui.assertShows("Sound for all");
		ui.assertShows("Volume for all");
		ui.assertHides("Full inv sound");
		// Switching an alert on or off and testing it stay per alert.
		ui.assertShows("Inventory full");
	}

	@Test
	public void oneOffEventCardsHaveNoRepeatControlButStopAlertsDo()
	{
		onEdt(() -> panel.show(PanelLayout.Tab.ALERTS));
		onEdt(() -> openAlert("INVENTORY_FULL"));
		ui.assertShows("Repeat full inv");
		for (String type : new String[]{"BIRD_NEST", "CLUE_DROP", "FORESTRY_EVENT", "PET", "LEVEL_UP",
			"RANDOM_EVENT", "SKILLING_EVENT"})
		{
			onEdt(() -> openAlert(type));
		}
		// The sound row of an opened event card is there; its Repeat row is not.
		ui.assertShows("Level up sound");
		for (String name : new String[]{"Repeat bird nest", "Repeat clue drop", "Repeat Forestry", "Repeat pet",
			"Repeat level up", "Repeat random", "Repeat gem/bottle"})
		{
			ui.assertHides(name);
		}
	}

	@Test
	public void testPlaysThatAlert()
	{
		onEdt(() -> panel.show(PanelLayout.Tab.ALERTS));
		onEdt(() -> openAlert("STUNNED"));
		onEdt(() -> ui.click(ui.find(AbstractButton.class, b -> "test-STUNNED".equals(b.getName()), "Test")));
		assertEquals("[STUNNED]", tested.toString());
		assertTrue("testing writes nothing", store.writes.isEmpty());
	}

	@Test
	public void aPresetShowsWhatChangesThenAppliesAndUndoes()
	{
		onEdt(() ->
		{
			panel.display(VitalsSidePanel.Main.PRESETS);
			ui.layout(VitalsSidePanelShots.WIDTH);
			ui.click("Minimal");
		});
		ui.assertShows("Minimal: what changes");
		ui.assertShows("Show panel: on -> off");
		assertTrue("previewing wrote " + store.writes, store.writes.isEmpty());

		onEdt(() -> ui.click("Apply"));
		assertEquals(Boolean.FALSE, store.values.get("showPanel"));
		assertEquals(PillarMode.OFF, store.values.get("pillarMode"));
		onEdt(() ->
		{
			panel.refresh();
			panel.show(PanelLayout.Tab.COMBAT);
			ui.layout(VitalsSidePanelShots.WIDTH);
		});
		assertFalse(SwingDriver.onEdt(() -> checkbox("Show panel").isSelected()));

		onEdt(() ->
		{
			panel.display(VitalsSidePanel.Main.PRESETS);
			ui.layout(VitalsSidePanelShots.WIDTH);
			ui.click("Undo Minimal");
		});
		assertEquals(Boolean.TRUE, store.values.get("showPanel"));
		ui.assertHides("Undo Minimal");
	}

	@Test
	public void cancelLeavesEverything()
	{
		onEdt(() ->
		{
			panel.display(VitalsSidePanel.Main.PRESETS);
			ui.layout(VitalsSidePanelShots.WIDTH);
			ui.click("PvM");
		});
		onEdt(() -> ui.click("Cancel"));
		ui.assertHides("PvM: what changes");
		assertTrue(store.writes.isEmpty());
	}

	@Test
	public void everyGuideStartsOpenWithItsNumberedSteps()
	{
		onEdt(() -> panel.display(VitalsSidePanel.Main.PRESETS));
		ui.assertShows(PanelGuide.PRESETS.how);
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			onEdt(() -> panel.show(tab));
			final PanelGuide.Page page = PanelGuide.of(tab);
			ui.assertShows(page.what);
			ui.assertShows(page.how);
			for (String step : page.walkthrough)
			{
				ui.assertShows(step);
			}
		}
		assertTrue("opening writes nothing: " + store.writes, store.writes.isEmpty());
	}

	@Test
	public void aFoldedGuideStaysFoldedForThatTabOnly()
	{
		onEdt(() -> ui.click("Guide: Combat"));
		ui.assertHides(PanelGuide.of(PanelLayout.Tab.COMBAT).how);
		assertEquals("COMBAT", store.values.get(VitalsSidePanel.GUIDES_CLOSED_KEY));
		onEdt(() -> panel.show(PanelLayout.Tab.TARGETS));
		ui.assertShows(PanelGuide.of(PanelLayout.Tab.TARGETS).how);

		// A restart, or the refresh after any change: Combat's guide comes back folded, the rest open.
		onEdt(panel::refresh);
		onEdt(() -> panel.show(PanelLayout.Tab.COMBAT));
		ui.assertHides(PanelGuide.of(PanelLayout.Tab.COMBAT).how);
		final VitalsSidePanel again = SwingDriver.onEdt(() -> new VitalsSidePanel(store, PanelFixture.descriptors(),
			null, t ->
			{
			}));
		final SwingDriver ui2 = new SwingDriver(again);
		SwingDriver.onEdt(() ->
		{
			again.show(PanelLayout.Tab.COMBAT);
			ui2.layout(VitalsSidePanelShots.WIDTH);
		});
		ui2.assertHides(PanelGuide.of(PanelLayout.Tab.COMBAT).how);
		SwingDriver.onEdt(() ->
		{
			again.display(VitalsSidePanel.Main.PRESETS);
			ui2.layout(VitalsSidePanelShots.WIDTH);
		});
		ui2.assertShows(PanelGuide.PRESETS.how);

		onEdt(() -> ui.click("Guide: Combat"));
		ui.assertShows(PanelGuide.of(PanelLayout.Tab.COMBAT).how);
		assertEquals("", store.values.get(VitalsSidePanel.GUIDES_CLOSED_KEY));
	}

	@Test
	public void hideGuidesHidesEveryGuideAndRemembers()
	{
		onEdt(() -> ui.click(VitalsSidePanel.HIDE_GUIDES));
		assertEquals(Boolean.TRUE, store.values.get(VitalsSidePanel.GUIDES_HIDDEN_KEY));
		ui.assertHides(PanelGuide.PRESETS.how);
		ui.assertHides("Guide: Combat");
		onEdt(panel::refresh);
		ui.assertHides("Guide: Combat");
		ui.assertShows(VitalsSidePanel.SHOW_GUIDES);

		onEdt(() -> ui.click(VitalsSidePanel.SHOW_GUIDES));
		assertEquals(Boolean.FALSE, store.values.get(VitalsSidePanel.GUIDES_HIDDEN_KEY));
		ui.assertShows(PanelGuide.of(PanelLayout.Tab.COMBAT).how);
	}

	@Test
	public void seldomUsedSettingsWaitBehindMore()
	{
		final Map<String, ConfigItemDescriptor> items = PanelFixture.descriptors();
		final String spotPath = items.get("fishingSpotPath").name();
		onEdt(() -> panel.show(PanelLayout.Tab.SKILLING));
		ui.assertShows(items.get("skillingEnabled").name());
		ui.assertHides(spotPath);
		ui.assertHides("Next fishing spot");
		onEdt(() -> ui.click(VitalsSidePanel.MORE));
		onEdt(() -> ui.click("Next fishing spot"));
		ui.assertShows(spotPath);
	}

	@Test
	public void theLookPreviewFollowsTheSettings()
	{
		store.values.put("skillingPosition", BadgePosition.FIXED);
		store.values.put("skillingScale", 200);
		final LookPreview.Look big = LookPreview.read(store);
		final Rectangle fixed = LookPreview.badge(big);
		store.values.put("skillingPosition", BadgePosition.OVER_HEAD);
		store.values.put("skillingOffset", 100);
		store.values.put("skillingScale", 100);
		final Rectangle raised = LookPreview.badge(LookPreview.read(store));
		store.values.put("skillingOffset", 0);
		final Rectangle level = LookPreview.badge(LookPreview.read(store));

		assertEquals(6, fixed.x);
		assertTrue("200% is wider", fixed.width > level.width);
		assertTrue("a positive offset raises it", raised.y < level.y);
	}

	@Test
	public void theLookPreviewFadesFullToEmpty()
	{
		final LookPreview.Look l = new LookPreview.Look();
		l.full = Color.GREEN;
		l.mid = Color.YELLOW;
		l.low = Color.RED;
		assertEquals(Color.GREEN, LookPreview.colourAt(l, 1.0));
		assertEquals(Color.YELLOW, LookPreview.colourAt(l, 0.5));
		assertEquals(Color.RED, LookPreview.colourAt(l, 0.0));
		l.gradient = false;
		assertEquals(Color.GREEN, LookPreview.colourAt(l, 0.1));
	}
}
