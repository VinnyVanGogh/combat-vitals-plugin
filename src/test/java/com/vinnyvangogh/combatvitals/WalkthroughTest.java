package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;

/** The first-run tour: shown once, Skip and Done persist, and Show guides and the "?" links replay it. */
public class WalkthroughTest
{
	private PanelFixture.Store store;
	private VitalsSidePanel panel;
	private SwingDriver ui;

	@Before
	public void setUp()
	{
		store = PanelFixture.store();
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

	private boolean tourOpen()
	{
		return SwingDriver.onEdt(() -> ui.find(WalkthroughCard.class, c -> true, "tour").isOpen());
	}

	private void click(String name)
	{
		SwingDriver.onEdt(() -> ui.click(ui.find(javax.swing.JButton.class, b -> name.equals(b.getName()), name)));
	}

	@Test
	public void showsOnFirstRun()
	{
		build();
		assertTrue(tourOpen());
		ui.assertShows("Step 1 of " + Walkthrough.STEPS.size());
		assertTrue("building writes nothing: " + store.writes, store.writes.isEmpty());
	}

	@Test
	public void skipPersistsAndItDoesNotShowAgain()
	{
		build();
		click("tour-skip");
		assertFalse(tourOpen());
		assertEquals(Integer.valueOf(Walkthrough.REVISION), store.get(Walkthrough.SEEN_KEY, Integer.class));
		build();
		assertFalse("not shown again", tourOpen());
	}

	@Test
	public void walkingToTheEndPersistsToo()
	{
		build();
		for (int i = 1; i < Walkthrough.STEPS.size(); i++)
		{
			click("tour-next");
		}
		ui.assertShows("Step " + Walkthrough.STEPS.size() + " of " + Walkthrough.STEPS.size());
		click("tour-next");
		assertFalse(tourOpen());
		assertEquals(Integer.valueOf(Walkthrough.REVISION), store.get(Walkthrough.SEEN_KEY, Integer.class));
	}

	@Test
	public void stepsOpenTheirTabsWithoutRememberingThem()
	{
		build();
		click("tour-next");
		click("tour-next");
		assertEquals(VitalsSidePanel.Main.OVERLAYS, SwingDriver.onEdt(() -> panel.main()));
		assertFalse(store.writes.contains(VitalsSidePanel.TAB_KEY));
	}

	@Test
	public void raisedRevisionShowsAgain()
	{
		store.values.put(Walkthrough.SEEN_KEY, Walkthrough.REVISION - 1);
		store.types.put(Walkthrough.SEEN_KEY, Integer.class);
		build();
		assertTrue(tourOpen());
	}

	@Test
	public void showGuidesAndTheQuestionLinksReplayIt()
	{
		store.values.put(Walkthrough.SEEN_KEY, Walkthrough.REVISION);
		store.types.put(Walkthrough.SEEN_KEY, Integer.class);
		build();
		assertFalse(tourOpen());
		click("guide-link-OVERLAYS");
		assertTrue(tourOpen());
		ui.assertShows("Step " + (Walkthrough.stepFor(VitalsSidePanel.Main.OVERLAYS) + 1));
		click("tour-skip");
		click("hide-guides");
		assertFalse("Hide guides alone does not play it", tourOpen());
		click("hide-guides");
		assertTrue("Show guides replays it", tourOpen());
	}

	@Test
	public void everyTabHasAStepAndTheStepsAreShort()
	{
		final ArrayList<VitalsSidePanel.Main> seen = new ArrayList<>();
		for (Walkthrough.Step s : Walkthrough.STEPS)
		{
			assertTrue(s.text.length() < 280);
			if (s.tab != null)
			{
				seen.add(s.tab);
			}
		}
		assertEquals(VitalsSidePanel.Main.values().length, seen.size());
		assertTrue(Walkthrough.STEPS.size() >= 5 && Walkthrough.STEPS.size() <= 7);
	}
}
