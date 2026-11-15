package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;

/**
 * The sidebar's Live section, headless, fed fake readouts: what it shows in
 * a fight, while skilling and with nothing on; that an update redraws only
 * what changed and builds no components; that folding it is remembered; and
 * that no text runs past the panel's right edge. Shots go to build/screenshots.
 */
public class LiveSectionTest
{
	private static final int WIDTH = VitalsSidePanelShots.WIDTH;

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
			ui.layout(WIDTH);
		});
	}

	private void live(LiveVitals v)
	{
		SwingDriver.onEdt(() ->
		{
			panel.live(v);
			ui.layout(WIDTH);
		});
	}

	private LiveSection section()
	{
		return SwingDriver.onEdt(() -> ui.find(LiveSection.class, s -> true, "the Live section"));
	}

	static LiveVitals.Combat fighting()
	{
		return new LiveVitals.Combat(
			LiveVitals.level("Hitpoints", 72, 99, LiveVitals.HITPOINTS),
			LiveVitals.level("Prayer", 43, 70, LiveVitals.PRAYER),
			LiveVitals.energy(6_400, 1_000),
			"Vorkath",
			new LiveVitals.Bar(0.62, "466/750", LiveVitals.TARGET),
			new LiveVitals.Bar(0.45, "Ranged: Lv 92 -> 93: 45% (31,240 xp)", Color.GREEN),
			"152k xp/h | lvl 93 in 12m");
	}

	static LiveVitals.Combat idleCombat()
	{
		return new LiveVitals.Combat(
			LiveVitals.level("Hitpoints", 99, 99, LiveVitals.HITPOINTS),
			LiveVitals.level("Prayer", 70, 70, LiveVitals.PRAYER),
			LiveVitals.energy(10_000, 1_000), null, null, null, null);
	}

	static LiveVitals.Skilling chopping()
	{
		return new LiveVitals.Skilling("Woodcutting 72 (+3) 41s", Color.GREEN,
			new LiveVitals.Bar(0.64, "Lv 72 -> 73: 64% (34,120 xp)", Color.GREEN),
			Arrays.asList("142 chopped in 1h 05m | 60.1k xp/h | 131 logs/h | lvl 73 in 34m", "Inv 20/28 free",
				"Log basket 16/28 free"));
	}

	static final List<String> SESSION = Arrays.asList("Ranged: +412k xp, +2 lvls in 2h 10m",
		"Woodcutting: +86.2k xp in 1h 05m");

	@Test
	public void showsAFight()
	{
		live(new LiveVitals(true, fighting(), LiveVitals.skilling(null), SESSION));
		ui.assertShows("Hitpoints 72/99");
		ui.assertShows("Prayer 43/70");
		ui.assertShows("Run 64% | Spec 100%");
		ui.assertShows("Vorkath");
		ui.assertShows("466/750");
		ui.assertShows("Ranged: Lv 92 -> 93");
		ui.assertShows("152k xp/h | lvl 93 in 12m");
		ui.assertShows(LiveSection.NOT_SKILLING);
		ui.assertHides(LiveSection.NOT_IN_COMBAT);
		ui.assertShows("Ranged: +412k xp, +2 lvls in 2h 10m");
		ui.assertHides(LiveSection.NOTHING_YET);
		shot("panel-live-combat.png");
	}

	@Test
	public void showsSkilling()
	{
		live(new LiveVitals(true, idleCombat(), chopping(), SESSION.subList(1, 2)));
		ui.assertShows(LiveSection.NOT_IN_COMBAT);
		ui.assertShows("Woodcutting 72 (+3) 41s");
		ui.assertShows("Lv 72 -> 73: 64%");
		ui.assertShows("131 logs/h");
		ui.assertShows("Log basket 16/28 free");
		ui.assertHides(LiveSection.NOT_SKILLING);
		shot("panel-live-skilling.png");
	}

	@Test
	public void emptyStatesSayWhy()
	{
		live(new LiveVitals(true, idleCombat(), LiveVitals.skilling(null), Collections.emptyList()));
		ui.assertShows(LiveSection.NOT_IN_COMBAT);
		ui.assertShows(LiveSection.NOT_SKILLING);
		ui.assertShows(LiveSection.NOTHING_YET);
		shot("panel-live-empty.png");

		live(new LiveVitals(true, null, null, Collections.emptyList()));
		ui.assertShows(LiveSection.COMBAT_OFF);
		ui.assertShows(LiveSection.SKILLING_OFF);
		ui.assertHides("Hitpoints 99/99");

		live(LiveVitals.LOGGED_OUT);
		ui.assertShows(LiveSection.LOGGED_OUT);
		ui.assertHides(LiveSection.NOT_IN_COMBAT);
	}

	@Test
	public void theFightEndingSwapsTheTargetForNotInCombat()
	{
		live(new LiveVitals(true, fighting(), LiveVitals.skilling(null), SESSION));
		live(new LiveVitals(true, idleCombat(), LiveVitals.skilling(null), SESSION));
		ui.assertHides("Vorkath");
		ui.assertHides("466/750");
		ui.assertShows(LiveSection.NOT_IN_COMBAT);
	}

	@Test
	public void anUpdateBuildsNoComponentsAndRedrawsOnlyWhatChanged()
	{
		final LiveVitals first = new LiveVitals(true, fighting(), chopping(), SESSION);
		live(first);
		final List<Component> before = SwingDriver.onEdt(() -> tree(panel));
		final LiveSection section = section();

		final int changes = SwingDriver.onEdt(() -> section.changes);
		// The next tick, same values, freshly built as the plugin builds them.
		live(new LiveVitals(true, fighting(), chopping(), new ArrayList<>(SESSION)));
		assertEquals("an unchanged tick redraws nothing", changes, (int) SwingDriver.onEdt(() -> section.changes));

		// A tick where the numbers move: same components, new words.
		final LiveVitals.Combat hurt = new LiveVitals.Combat(LiveVitals.level("Hitpoints", 51, 99,
			LiveVitals.HITPOINTS), first.combat.prayer, first.combat.energy, "Vorkath",
			new LiveVitals.Bar(0.5, "375/750", LiveVitals.TARGET), first.combat.training, first.combat.trainingRate);
		for (int tick = 0; tick < 50; tick++)
		{
			live(new LiveVitals(true, tick % 2 == 0 ? hurt : first.combat, chopping(), SESSION));
		}
		final List<Component> after = SwingDriver.onEdt(() -> tree(panel));
		assertEquals(before.size(), after.size());
		for (int i = 0; i < before.size(); i++)
		{
			assertSame("component " + i + " was replaced", before.get(i), after.get(i));
		}
	}

	@Test
	public void foldingIsStoredAndComesBackFolded()
	{
		live(new LiveVitals(true, fighting(), chopping(), SESSION));
		SwingDriver.onEdt(panel::onActivate);
		assertTrue(SwingDriver.onEdt(panel::wantsLive));
		SwingDriver.onEdt(() ->
		{
			ui.click("- " + LiveSection.TITLE);
			ui.layout(WIDTH);
		});
		assertEquals(Boolean.FALSE, store.values.get(LiveSection.OPEN_KEY));
		ui.assertHides("Hitpoints 72/99");
		assertFalse("folded: the plugin builds nothing", SwingDriver.onEdt(panel::wantsLive));

		// A restart: a new panel over the stored settings opens folded.
		final VitalsSidePanel again = SwingDriver.onEdt(() -> new VitalsSidePanel(store, PanelFixture.descriptors(),
			null, t ->
			{
			}));
		final SwingDriver ui2 = SwingDriver.onEdt(() ->
		{
			final SwingDriver d = new SwingDriver(again);
			d.layout(WIDTH);
			return d;
		});
		ui2.assertShows("+ " + LiveSection.TITLE);
		ui2.assertHides(LiveSection.LOGGED_OUT);

		// Opened from the list (or another panel): the refresh that follows opens it.
		store.values.put(LiveSection.OPEN_KEY, true);
		SwingDriver.onEdt(() ->
		{
			again.refresh();
			ui2.layout(WIDTH);
		});
		ui2.assertShows(LiveSection.LOGGED_OUT);
	}

	@Test
	public void theLiveSectionSitsAboveThePresets()
	{
		final int liveY = SwingDriver.onEdt(() -> ui.yOf(ui.button("- " + LiveSection.TITLE)));
		final int presetsY = SwingDriver.onEdt(() -> ui.yOf(ui.label("Presets")));
		assertTrue(liveY < presetsY);
	}

	@Test
	public void noTextRunsPastThePanelsRightEdge()
	{
		live(new LiveVitals(true, fighting(), chopping(), SESSION));
		SwingDriver.onEdt(() ->
		{
			ui.click("+ Guide: presets");
			for (PanelLayout.Tab tab : PanelLayout.Tab.values())
			{
				panel.show(tab);
				ui.layout(WIDTH);
				ui.click("+ Guide: " + tab.label);
				ui.layout(WIDTH);
				final int edge = WIDTH - panel.getInsets().right;
				for (JLabel l : ui.all(JLabel.class))
				{
					if (!(l instanceof WrapLabel))
					{
						continue;
					}
					final int left = SwingUtilities.convertPoint(l.getParent(), l.getLocation(), panel).x;
					final int right = left + l.getPreferredSize().width;
					assertTrue(tab + ": '" + ((WrapLabel) l).plain() + "' ends at " + right + " past " + edge,
						right <= edge);
					assertTrue(tab + ": '" + ((WrapLabel) l).plain() + "' is cut short",
						l.getHeight() >= l.getPreferredSize().height);
				}
			}
		});
	}

	private static List<Component> tree(Component c)
	{
		final List<Component> out = new ArrayList<>();
		out.add(c);
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				out.addAll(tree(child));
			}
		}
		return out;
	}

	private void shot(String name)
	{
		final BufferedImage full = SwingDriver.onEdt(() -> ui.render(WIDTH, 0, 2.0));
		final int presets = SwingDriver.onEdt(() -> ui.yOf(ui.label("Presets")));
		final BufferedImage image = full.getSubimage(0, 0, full.getWidth(), presets * 2);
		assertTrue(name + " is empty", image.getHeight() > 100);
		SwingDriver.writePng(image, new File(System.getProperty("screenshots.dir", "build/screenshots"), name));
	}
}
