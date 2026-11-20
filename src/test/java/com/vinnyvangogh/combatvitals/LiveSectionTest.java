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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
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

	/** Clicks the Live tab's Combat / Skilling / Overall toggle, as a user does. */
	private void view(LiveSection.View v)
	{
		SwingDriver.onEdt(() ->
		{
			ui.find(javax.swing.JToggleButton.class, b -> ("live-view-" + v.name()).equals(b.getName()),
				"view " + v).doClick();
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
		final Map<SessionRow.Kind, String> stats = new EnumMap<>(SessionRow.Kind.class);
		stats.put(SessionRow.Kind.COUNT, "142 chopped in 1h 05m");
		stats.put(SessionRow.Kind.XP_RATE, "60.1k xp/h");
		stats.put(SessionRow.Kind.ACTION_RATE, "131 logs/h");
		stats.put(SessionRow.Kind.TIME_TO_LEVEL, "lvl 73 in 34m");
		return new LiveVitals.Skilling("Woodcutting 72 (+3) 41s", Color.GREEN, null,
			new LiveVitals.Bar(0.64, "Lv 72 -> 73: 64% (34,120 xp)", Color.GREEN), stats, Collections.emptyList(),
			Arrays.asList("Inv 20/28 free", "Log basket 16/28 free"));
	}

	/** The Blast Furnace pump: a training method's timers as chips, no inventory line. */
	static LiveVitals.Skilling pumping()
	{
		final Map<SessionRow.Kind, String> stats = new EnumMap<>(SessionRow.Kind.class);
		stats.put(SessionRow.Kind.XP_RATE, "84.0k xp/h");
		stats.put(SessionRow.Kind.TIME_TO_LEVEL, "lvl 86 in 1h 12m");
		return new LiveVitals.Skilling("Pumping (Blast Furnace) | Strength 85", new Color(0xC8, 0x50, 0x3C), null,
			new LiveVitals.Bar(0.31, "Lv 85 -> 86: 31% (61,004 xp)", new Color(0xC8, 0x50, 0x3C)), stats,
			Arrays.asList("Active", "Pump stops in 97:30"), Collections.emptyList());
	}

	static LiveVitals.Gain gain(String text, int xp)
	{
		return new LiveVitals.Gain(text, xp, Color.GREEN);
	}

	static final List<LiveVitals.Gain> SESSION = Arrays.asList(gain("Ranged: +412k xp, +2 lvls in 2h 10m", 412_000),
		gain("Woodcutting: +86.2k xp in 1h 05m", 86_200));

	static final List<LiveVitals.Gain> LONG_SESSION = Arrays.asList(SESSION.get(0), SESSION.get(1),
		gain("Hitpoints: +137k xp in 2h 10m", 137_000), gain("Strength: +20.1k xp in 31m", 20_100),
		gain("Agility: +4.2k xp in 12m", 4_200));

	@Test
	public void showsAFight()
	{
		live(new LiveVitals(true, fighting(), LiveVitals.skilling(null), SESSION, SESSION));
		ui.assertShows("Hitpoints 72/99");
		ui.assertShows("Prayer 43/70");
		ui.assertShows("Run 64%");
		ui.assertShows("Spec 100%");
		ui.assertShows("Vorkath");
		ui.assertShows("466/750");
		ui.assertShows("Ranged: Lv 92 -> 93");
		ui.assertShows("152k xp/h | lvl 93 in 12m");
		ui.assertHides(LiveSection.NOT_IN_COMBAT);
		shot("panel-live-combat.png");
		view(LiveSection.View.SKILLING);
		ui.assertShows(LiveSection.NOT_SKILLING);
		view(LiveSection.View.OVERALL);
		ui.assertShows("Ranged: +412k xp, +2 lvls in 2h 10m");
		ui.assertHides(LiveSection.NOTHING_YET);
	}

	@Test
	public void theToggleShowsOneCardAtATimeAndRemembersIt()
	{
		live(new LiveVitals(true, fighting(), chopping(), SESSION));
		assertEquals(LiveSection.View.COMBAT, SwingDriver.onEdt(() -> section().view()));
		ui.assertShows("Hitpoints 72/99");
		ui.assertHides("Woodcutting 72 (+3) 41s");
		view(LiveSection.View.SKILLING);
		ui.assertShows("Woodcutting 72 (+3) 41s");
		ui.assertHides("Hitpoints 72/99");
		assertEquals("SKILLING", store.values.get(LiveSection.VIEW_KEY));
		view(LiveSection.View.OVERALL);
		ui.assertShows("Ranged: +412k xp, +2 lvls in 2h 10m");
		ui.assertHides("Woodcutting 72 (+3) 41s");

		final VitalsSidePanel again = SwingDriver.onEdt(() -> new VitalsSidePanel(store, PanelFixture.descriptors(),
			null, t ->
			{
			}));
		assertEquals(LiveSection.View.OVERALL, SwingDriver.onEdt(() -> ui2(again).view()));
	}

	private static LiveSection ui2(VitalsSidePanel p)
	{
		return new SwingDriver(p).find(LiveSection.class, s -> true, "the Live section");
	}

	@Test
	public void showsSkilling()
	{
		live(new LiveVitals(true, idleCombat(), chopping(), SESSION.subList(1, 2)));
		ui.assertShows(LiveSection.NOT_IN_COMBAT);
		view(LiveSection.View.SKILLING);
		ui.assertShows("Woodcutting 72 (+3) 41s");
		ui.assertShows("Lv 72 -> 73: 64%");
		ui.assertShows("XP / hour");
		ui.assertShows("60.1k xp/h");
		ui.assertShows("131 logs/h");
		ui.assertShows("lvl 73 in 34m");
		ui.assertShows("142 chopped in 1h 05m");
		ui.assertShows("Log basket 16/28 free");
		ui.assertHides(LiveSection.NOT_SKILLING);
		shot("panel-live-skilling.png");
	}

	@Test
	public void showsAMethodsTimersAsChips()
	{
		live(new LiveVitals(true, idleCombat(), pumping(), SESSION.subList(1, 2)));
		view(LiveSection.View.SKILLING);
		ui.assertShows("Pumping (Blast Furnace) | Strength 85");
		ui.assertShows("Active");
		ui.assertShows("Pump stops in 97:30");
		ui.assertShows("84.0k xp/h");
		ui.assertHides("Actions / hour");
		final Chip chip = SwingDriver.onEdt(() -> ui.find(Chip.class, c -> "Active".equals(c.getText()), "Active chip"));
		assertTrue(chip.isVisible());
		shot("panel-live-pump.png");
	}

	@Test
	public void idleShowsNotInCombatAndNotSkilling()
	{
		live(new LiveVitals(true, idleCombat(), LiveVitals.skilling(null), SESSION));
		ui.assertShows(LiveSection.NOT_IN_COMBAT);
		ui.assertShows("Hitpoints 99/99");
		view(LiveSection.View.SKILLING);
		ui.assertShows(LiveSection.NOT_SKILLING);
		ui.assertHides("XP / hour");
		shot("panel-live-idle.png");
	}

	@Test
	public void thisSessionShowsTheTopThreeThenAll()
	{
		final List<LiveVitals.Gain> ranked = new ArrayList<>(LONG_SESSION);
		ranked.sort((a, b) -> Integer.compare(b.xp, a.xp));
		live(new LiveVitals(true, idleCombat(), LiveVitals.skilling(null), ranked));
		view(LiveSection.View.OVERALL);
		final LiveSection section = section();
		assertEquals(Arrays.asList("Ranged: +412k xp, +2 lvls in 2h 10m", "Hitpoints: +137k xp in 2h 10m",
			"Woodcutting: +86.2k xp in 1h 05m"), SwingDriver.onEdt(section::sessionShown));
		ui.assertHides("Agility: +4.2k xp");
		shot("panel-live-session.png");

		SwingDriver.onEdt(() ->
		{
			ui.click("Show all 5");
			ui.layout(WIDTH);
		});
		assertEquals(5, (int) SwingDriver.onEdt(() -> section.sessionShown().size()));
		ui.assertShows("Agility: +4.2k xp in 12m");
		SwingDriver.onEdt(() ->
		{
			ui.click(LiveSection.SHOW_TOP);
			ui.layout(WIDTH);
		});
		ui.assertHides("Agility: +4.2k xp");
	}

	@Test
	public void emptyStatesSayWhy()
	{
		live(new LiveVitals(true, idleCombat(), LiveVitals.skilling(null), Collections.emptyList()));
		ui.assertShows(LiveSection.NOT_IN_COMBAT);
		view(LiveSection.View.SKILLING);
		ui.assertShows(LiveSection.NOT_SKILLING);
		view(LiveSection.View.OVERALL);
		ui.assertShows(LiveSection.NOTHING_YET);
		shot("panel-live-empty.png");

		live(new LiveVitals(true, null, null, Collections.emptyList()));
		view(LiveSection.View.SKILLING);
		ui.assertShows(LiveSection.SKILLING_OFF);
		view(LiveSection.View.COMBAT);
		ui.assertShows(LiveSection.COMBAT_OFF);
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
			ui.click(liveToggle(ui));
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
		assertFalse(SwingDriver.onEdt(() -> ((FoldCard.Header) liveToggle(ui2)).isOpen()));
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
	public void theLiveTabOpensFirstAndTheMainTabsSitBelowTheMuteBar()
	{
		assertEquals(VitalsSidePanel.Main.LIVE, SwingDriver.onEdt(panel::main));
		final int muteY = SwingDriver.onEdt(() -> ui.yOf(ui.button(MuteBar.MUTE)));
		final int liveY = SwingDriver.onEdt(() -> ui.yOf(ui.button(LiveSection.TITLE)));
		assertTrue(muteY < liveY);
	}

	@Test
	public void noTextRunsPastThePanelsRightEdge()
	{
		live(new LiveVitals(true, fighting(), chopping(), LONG_SESSION));
		view(LiveSection.View.OVERALL);
		SwingDriver.onEdt(() ->
		{
			ui.click("Show all 5");
			for (PanelLayout.Tab tab : PanelLayout.Tab.values())
			{
				panel.show(tab);
				ui.layout(WIDTH);
				openEverything();
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

	/** Opens every folded card on screen, More settings first, so every label is laid out. */
	private void openEverything()
	{
		for (int pass = 0; pass < 3; pass++)
		{
			for (FoldCard.Header h : ui.all(FoldCard.Header.class))
			{
				if (!h.isOpen())
				{
					ui.click(h);
				}
			}
			ui.layout(WIDTH);
		}
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

	private static JButton liveToggle(SwingDriver d)
	{
		return d.find(JButton.class, b -> "live-toggle".equals(b.getName()), "the Live fold header");
	}

	private void shot(String name)
	{
		final BufferedImage full = SwingDriver.onEdt(() -> ui.render(WIDTH, 0, 2.0));
		final int bottom = SwingDriver.onEdt(() ->
		{
			final Component section = ui.find(LiveSection.class, s -> true, "the live section");
			return SwingUtilities.convertPoint(section.getParent(), section.getLocation(), panel).y
				+ section.getHeight();
		});
		final BufferedImage image = full.getSubimage(0, 0, full.getWidth(),
			Math.min(full.getHeight(), bottom * 2));
		assertTrue(name + " is empty", image.getHeight() > 100);
		SwingDriver.writePng(image, new File(System.getProperty("screenshots.dir", "build/screenshots"), name));
	}
}
