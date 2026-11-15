package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

/**
 * Skilling containers (Board, 2026-11-14). The failure cases first: an
 * unchecked container is never guessed, the option off leaves the inventory
 * line exactly as it was, and a container never shows for a skill it does
 * not take from.
 */
public class SkillingContainersTest
{
	private ContainerContents contents;

	@Before
	public void setUp()
	{
		contents = new ContainerContents();
	}

	// --- Never a guess -----------------------------------------------------

	@Test
	public void uncheckedBasketShowsAQuestionMarkAndAHint()
	{
		final ContainerRow row = row(SkillingContainer.LOG_BASKET, contents.contents(SkillingContainer.LOG_BASKET), 28);
		assertEquals("Log basket ?/28 (check the basket once)", row.label(InventoryCount.FREE));
		assertEquals("Log basket ?/28 (check the basket once)", row.label(InventoryCount.USED));
		assertEquals(-1, row.free());
		assertEquals(-1, row.fill(), 0);
	}

	@Test
	public void catchesIntoAnUncheckedBasketKeepItUnknown()
	{
		contents.carried(open(SkillingContainer.LOG_BASKET), capacities());
		assertFalse(contents.message("You get some logs."));
		assertEquals(-1, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void fillingAnUncheckedBasketKeepsItUnknown()
	{
		contents.clicked(SkillingContainer.LOG_BASKET, "Fill", 10);
		contents.inventoryChanged(20, 4, 10);
		assertEquals(-1, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void unknownContainerAddsNothingToTheInventoryLine()
	{
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.LOG_BASKET, -1, 28));
		assertEquals("Inv 27/28 free", ContainerRow.inventoryLabel(27, InventoryCount.FREE, rows, true));
		assertEquals(27, ContainerRow.combinedEmpty(27, rows, true));
	}

	@Test
	public void loggingOutForgetsEverything()
	{
		contents.message("The basket contains: 5 x Logs");
		contents.clear();
		assertEquals(-1, contents.contents(SkillingContainer.LOG_BASKET));
	}

	// --- Check dialog, full by landing, kept across logins ------------------

	/** The Board's screenshot 2026-11-14: Empty into the inventory says only "You empty your basket." */
	@Test
	public void emptyingIntoTheInventoryZeroesTheBasket()
	{
		contents.message("The basket contains: 21 x Oak logs", 100);
		assertTrue(contents.message("You empty your basket.", 101));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
		contents.message("The basket contains: 5 x Logs", 102);
		assertTrue(contents.message("You empty your basket into the bank.", 103));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
	}

	/** The Board's screenshot 2026-11-14: Check answers in the item dialog, "<br>" between the lines. */
	@Test
	public void checkDialogTextSetsTheCount()
	{
		assertTrue(contents.message("The basket contains: 14 x Yew logs", 100));
		assertEquals(14, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void aLogLandingInTheInventoryWhileTheBasketIsOpenMeansFull()
	{
		contents.carried(open(SkillingContainer.LOG_BASKET), capacities());
		contents.message("You get some yew logs.", 200);
		assertTrue(contents.inventoryChanged(1, 2, 200));
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void fullByLandingWorksWhicheverEventComesFirst()
	{
		contents.carried(open(SkillingContainer.LOG_BASKET), capacities());
		contents.inventoryChanged(1, 2, 300);
		contents.message("You get some yew logs.", 300);
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void aLogIntoTheOpenBasketIsNotFull()
	{
		contents.carried(open(SkillingContainer.LOG_BASKET), capacities());
		contents.message("The basket contains: 14 x Yew logs", 400);
		contents.message("You get some yew logs.", 401);
		contents.inventoryChanged(1, 2, 405);
		assertEquals(15, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void aClosedBasketIsNeverMarkedFull()
	{
		contents.message("You get some yew logs.", 500);
		contents.inventoryChanged(1, 2, 500);
		assertEquals(-1, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void aCountKeptFromTheLastLoginComesBack()
	{
		contents.restore(SkillingContainer.LOG_BASKET, 14);
		assertEquals(14, contents.contents(SkillingContainer.LOG_BASKET));
		contents.message("The basket contains: 3 x Logs", 600);
		contents.restore(SkillingContainer.LOG_BASKET, 14);
		assertEquals("this session's count wins", 3, contents.contents(SkillingContainer.LOG_BASKET));
		assertEquals(Integer.valueOf(3), contents.known().get(SkillingContainer.LOG_BASKET));
	}

	// --- Option off: the inventory line is exactly as it was ---------------

	@Test
	public void optionOffKeepsTheInventoryLineExact()
	{
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.LOG_BASKET, 12, 28));
		assertEquals("Inv 27/28 free", ContainerRow.inventoryLabel(27, InventoryCount.FREE, rows, false));
		assertEquals("Inv 1/28 used", ContainerRow.inventoryLabel(27, InventoryCount.USED, rows, false));
		assertEquals(InventoryFill.label(27, InventoryCount.FREE),
			ContainerRow.inventoryLabel(27, InventoryCount.FREE, rows, false));
		assertNull(ContainerRow.inventoryLabel(-1, InventoryCount.FREE, rows, false));
	}

	@Test
	public void optionOffAlertFollowsTheInventoryAlone()
	{
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.FISH_BARREL, 3, 28));
		assertEquals(0, ContainerRow.combinedEmpty(0, rows, false));
		assertEquals(-1, ContainerRow.combinedEmpty(-1, rows, true));
	}

	// --- Container lines ---------------------------------------------------

	@Test
	public void basketLinesAtZeroTwelveAndTwentyEight()
	{
		assertEquals("Log basket 28/28 free", row(SkillingContainer.LOG_BASKET, 0, 28).label(InventoryCount.FREE));
		assertEquals("Log basket 12/28 free", row(SkillingContainer.LOG_BASKET, 16, 28).label(InventoryCount.FREE));
		assertEquals("Log basket 0/28 free", row(SkillingContainer.LOG_BASKET, 28, 28).label(InventoryCount.FREE));
		assertEquals("Log basket 16/28 used", row(SkillingContainer.LOG_BASKET, 16, 28).label(InventoryCount.USED));
		assertEquals(0.0, row(SkillingContainer.LOG_BASKET, 0, 28).fill(), 0);
		assertEquals(1.0, row(SkillingContainer.LOG_BASKET, 28, 28).fill(), 0);
	}

	@Test
	public void otherContainersNameThemselves()
	{
		assertEquals("Fish barrel 25/28 free", row(SkillingContainer.FISH_BARREL, 3, 28).label(InventoryCount.FREE));
		assertEquals("Coal bag 0/27 free", row(SkillingContainer.COAL_BAG, 27, 27).label(InventoryCount.FREE));
		assertEquals("Coal bag ?/36 (check the coal bag once)",
			row(SkillingContainer.COAL_BAG, -1, 36).label(InventoryCount.FREE));
		assertEquals("Essence pouches 4/12 free",
			row(SkillingContainer.ESSENCE_POUCHES, 8, 12).label(InventoryCount.FREE));
	}

	@Test
	public void containerNotShownForUnrelatedSkills()
	{
		final Map<SkillingContainer, Integer> carried = capacities();
		assertTrue(ContainerRow.rows(Skill.THIEVING, carried, contents::contents).isEmpty());
		assertTrue(ContainerRow.rows(Skill.HUNTER, carried, contents::contents).isEmpty());
		assertEquals(Collections.singletonList(SkillingContainer.LOG_BASKET),
			kinds(ContainerRow.rows(Skill.WOODCUTTING, carried, contents::contents)));
		assertEquals(Collections.singletonList(SkillingContainer.FISH_BARREL),
			kinds(ContainerRow.rows(Skill.FISHING, carried, contents::contents)));
		assertEquals(Collections.singletonList(SkillingContainer.COAL_BAG),
			kinds(ContainerRow.rows(Skill.MINING, carried, contents::contents)));
		assertEquals(Collections.singletonList(SkillingContainer.ESSENCE_POUCHES),
			kinds(ContainerRow.rows(Skill.RUNECRAFT, carried, contents::contents)));
		assertTrue(ContainerRow.rows(null, carried, contents::contents).isEmpty());
	}

	@Test
	public void containerNotCarriedIsNotShown()
	{
		assertTrue(ContainerRow.rows(Skill.WOODCUTTING, new EnumMap<>(SkillingContainer.class),
			contents::contents).isEmpty());
	}

	// --- Option on: container space counts as inventory --------------------

	@Test
	public void optionOnAddsContainerSpace()
	{
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.LOG_BASKET, 12, 28));
		assertEquals("Inv 27/28 free (+16 basket)", ContainerRow.inventoryLabel(27, InventoryCount.FREE, rows, true));
		assertEquals("Inv 1/28 used (+16 basket free)",
			ContainerRow.inventoryLabel(27, InventoryCount.USED, rows, true));
		assertEquals(43, ContainerRow.combinedEmpty(27, rows, true));
	}

	@Test
	public void aFullContainerAddsNothing()
	{
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.FISH_BARREL, 28, 28));
		assertEquals("Inv 5/28 free", ContainerRow.inventoryLabel(5, InventoryCount.FREE, rows, true));
	}

	@Test
	public void coalBagIsShownButNotCountedAsInventory()
	{
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.COAL_BAG, 0, 27));
		assertEquals("Inv 10/28 free", ContainerRow.inventoryLabel(10, InventoryCount.FREE, rows, true));
		assertEquals(10, ContainerRow.combinedEmpty(10, rows, true));
	}

	@Test
	public void optionOnDrivesTheInventoryFullAlert()
	{
		final ActivityAlerts alerts = new ActivityAlerts();
		alerts.xp(Skill.FISHING, 1_000);
		int tick = 100;
		alerts.tick(frame(tick++, 20, 8), 8, 28, 10);

		// The inventory fills, but the barrel still has room: not full.
		List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.FISH_BARREL, 20, 28));
		alerts.xp(Skill.FISHING, 1_010);
		List<ActivityAlerts.Alert> out = alerts.tick(
			frame(tick++, 28, ContainerRow.combinedEmpty(0, rows, true)), 8, 28, 10);
		assertTrue(out.isEmpty());

		// The barrel fills too: now it is.
		rows = Collections.singletonList(row(SkillingContainer.FISH_BARREL, 28, 28));
		alerts.xp(Skill.FISHING, 1_020);
		out = alerts.tick(frame(tick, 28, ContainerRow.combinedEmpty(0, rows, true)), 8, 28, 10);
		assertEquals(1, out.size());
		assertEquals(ActivityAlerts.Type.INVENTORY_FULL, out.get(0).type);
	}

	@Test
	public void optionOffAlertsWhenTheInventoryFillsWhateverTheBarrelHolds()
	{
		final ActivityAlerts alerts = new ActivityAlerts();
		alerts.xp(Skill.FISHING, 1_000);
		alerts.tick(frame(100, 20, 8), 8, 28, 10);
		final List<ContainerRow> rows = Collections.singletonList(row(SkillingContainer.FISH_BARREL, 3, 28));
		alerts.xp(Skill.FISHING, 1_010);
		final List<ActivityAlerts.Alert> out = alerts.tick(
			frame(101, 28, ContainerRow.combinedEmpty(0, rows, false)), 8, 28, 10);
		assertEquals(1, out.size());
		assertEquals(ActivityAlerts.Type.INVENTORY_FULL, out.get(0).type);
	}

	// --- Chat messages -----------------------------------------------------

	@Test
	public void checkMessageSetsTheCount()
	{
		assertTrue(contents.message("The basket contains: 5 x Logs, 3 x Oak logs, 1 x Willow logs."));
		assertEquals(9, contents.contents(SkillingContainer.LOG_BASKET));
		assertTrue(contents.message("The barrel contains: 12 x Raw trout, 4 x Raw salmon"));
		assertEquals(16, contents.contents(SkillingContainer.FISH_BARREL));
		assertTrue(contents.message("The coal bag contains 12 coal."));
		assertEquals(12, contents.contents(SkillingContainer.COAL_BAG));
		assertTrue(contents.message("The coal bag still contains one coal."));
		assertEquals(1, contents.contents(SkillingContainer.COAL_BAG));
	}

	@Test
	public void emptyMessagesSetZero()
	{
		contents.message("The basket contains: 5 x Logs");
		assertTrue(contents.message("The basket is empty."));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
		contents.message("The basket contains: 5 x Logs");
		assertTrue(contents.message("You empty your basket into the bank."));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
		assertTrue(contents.message("Your barrel is empty."));
		assertEquals(0, contents.contents(SkillingContainer.FISH_BARREL));
		assertTrue(contents.message("The coal bag is now empty."));
		assertEquals(0, contents.contents(SkillingContainer.COAL_BAG));
	}

	@Test
	public void barrelFullMessageFillsIt()
	{
		contents.carried(open(SkillingContainer.FISH_BARREL), capacities());
		assertTrue(contents.message("The barrel is full. It may be emptied at a bank."));
		assertEquals(28, contents.contents(SkillingContainer.FISH_BARREL));
	}

	@Test
	public void catchesLandInAnOpenContainerUpToItsCapacity()
	{
		contents.carried(open(SkillingContainer.LOG_BASKET, SkillingContainer.COAL_BAG), capacities());
		contents.message("The basket contains: 27 x Logs");
		assertTrue(contents.message("You get some oak logs."));
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));
		// Full: the next log goes to the inventory.
		assertFalse(contents.message("You get some logs."));
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));

		contents.message("The coal bag is empty.");
		assertTrue(contents.message("You manage to mine some coal."));
		assertEquals(1, contents.contents(SkillingContainer.COAL_BAG));
	}

	@Test
	public void natureOfferingsAddTheExtraLog()
	{
		contents.carried(open(SkillingContainer.LOG_BASKET), capacities());
		contents.message("The basket is empty.");
		contents.message("You get some logs.");
		contents.message("The nature offerings enabled you to chop an extra log.");
		assertEquals(2, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void catchesIntoAClosedContainerGoToTheInventory()
	{
		contents.carried(closed(SkillingContainer.LOG_BASKET), capacities());
		contents.message("The basket is empty.");
		assertFalse(contents.message("You get some logs."));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void fishCaughtOnlyCountsIntoAnOpenBarrel()
	{
		contents.carried(open(SkillingContainer.FISH_BARREL), capacities());
		contents.message("The barrel is empty.");
		assertTrue(contents.fishCaught());
		assertTrue(contents.message("Rada's blessing enabled you to catch an extra fish."));
		assertEquals(2, contents.contents(SkillingContainer.FISH_BARREL));

		contents.carried(closed(SkillingContainer.FISH_BARREL), capacities());
		assertFalse(contents.fishCaught());
		assertEquals(2, contents.contents(SkillingContainer.FISH_BARREL));
	}

	@Test
	public void unrelatedMessagesChangeNothing()
	{
		assertFalse(contents.message("You catch some shrimps."));
		assertFalse(contents.message("Welcome to Old School RuneScape."));
		assertFalse(contents.message(null));
	}

	// --- Fill and Empty clicks ----------------------------------------------

	@Test
	public void fillMovesWhatLeftTheInventory()
	{
		contents.carried(closed(SkillingContainer.LOG_BASKET), capacities());
		contents.message("The basket contains: 2 x Logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Fill", 50);
		assertTrue(contents.inventoryChanged(25, 15, 51));
		assertEquals(12, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void emptyMovesWhatArrivedInTheInventory()
	{
		contents.message("The basket contains: 12 x Logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Empty", 50);
		assertTrue(contents.inventoryChanged(4, 14, 50));
		assertEquals(2, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void staleClickIsIgnored()
	{
		contents.message("The basket contains: 2 x Logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Fill", 50);
		assertFalse(contents.inventoryChanged(25, 15, 60));
		assertEquals(2, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void emptyToBankSetsZero()
	{
		contents.message("The barrel contains: 12 x Raw trout");
		contents.clicked(SkillingContainer.FISH_BARREL, "Empty to bank", 50);
		assertEquals(0, contents.contents(SkillingContainer.FISH_BARREL));
	}

	// --- Empty in the bank (Board, 2026-11-14) -------------------------------

	@Test
	public void emptyInTheBankIsZeroOnceTheBankChangesWithoutTheChatLine()
	{
		contents.message("The basket contains: 28 x Yew logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Empty", 50, true);
		// Not yet: the click alone could have failed (a full bank).
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));
		assertTrue(contents.bankChanged(51));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void emptyInTheBankOfAnUncheckedBasketIsStillZero()
	{
		contents.clicked(SkillingContainer.LOG_BASKET, "Empty", 50, true);
		assertTrue(contents.bankChanged(50));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void emptyInTheBankIsZeroFromTheChatLineToo()
	{
		contents.message("The basket contains: 28 x Yew logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Empty", 50, true);
		assertTrue(contents.message("You empty your basket into the bank.", 51));
		assertEquals(0, contents.contents(SkillingContainer.LOG_BASKET));
		// Already settled: the bank change after it moves nothing.
		assertFalse(contents.bankChanged(51));
	}

	@Test
	public void fishBarrelEmptyInTheBankIsZero()
	{
		contents.message("The barrel contains: 12 x Raw trout");
		contents.clicked(SkillingContainer.FISH_BARREL, "Empty", 50, true);
		assertTrue(contents.bankChanged(52));
		assertEquals(0, contents.contents(SkillingContainer.FISH_BARREL));
	}

	@Test
	public void emptyInTheBankWithNoBankChangeLeavesTheCount()
	{
		contents.message("The basket contains: 28 x Yew logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Empty", 50, true);
		assertFalse(contents.bankChanged(60));
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void bankChangeWithoutAnEmptyClickMovesNothing()
	{
		contents.message("The basket contains: 28 x Yew logs");
		assertFalse(contents.bankChanged(50));
		contents.clicked(SkillingContainer.LOG_BASKET, "Fill", 50, true);
		assertFalse(contents.bankChanged(50));
		assertEquals(28, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void emptyOutsideTheBankIsUnchanged()
	{
		contents.message("The basket contains: 12 x Logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Empty", 50, false);
		// A bank change (none should come, but) never zeroes an Empty made outside the bank.
		assertFalse(contents.bankChanged(50));
		assertEquals(12, contents.contents(SkillingContainer.LOG_BASKET));
		assertTrue(contents.inventoryChanged(4, 14, 50));
		assertEquals(2, contents.contents(SkillingContainer.LOG_BASKET));
	}

	@Test
	public void checkOrOtherOptionsDoNotMoveAnything()
	{
		contents.message("The basket contains: 2 x Logs");
		contents.clicked(SkillingContainer.LOG_BASKET, "Check", 50);
		assertFalse(contents.inventoryChanged(25, 15, 50));
		assertEquals(2, contents.contents(SkillingContainer.LOG_BASKET));
	}

	// --- Item names ---------------------------------------------------------

	@Test
	public void containersAreFoundByName()
	{
		assertEquals(SkillingContainer.LOG_BASKET, SkillingContainer.fromItemName("Log basket"));
		assertEquals(SkillingContainer.LOG_BASKET, SkillingContainer.fromItemName("Open forestry basket"));
		assertEquals(SkillingContainer.FISH_BARREL, SkillingContainer.fromItemName("Open fish sack barrel"));
		assertEquals(SkillingContainer.COAL_BAG, SkillingContainer.fromItemName("Open coal bag"));
		assertEquals(SkillingContainer.ESSENCE_POUCHES, SkillingContainer.fromItemName("Colossal pouch"));
		assertNull(SkillingContainer.fromItemName("Logs"));
		assertNull(SkillingContainer.fromItemName("Gem bag"));
		assertNull(SkillingContainer.fromItemName(null));
		assertTrue(SkillingContainer.isOpen("Open log basket"));
		assertFalse(SkillingContainer.isOpen("Log basket"));
	}

	@Test
	public void aCarriedContainerHoldsItsSlotRatherThanFillingIt()
	{
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Log basket", new String[]{"Fill", "Check"}));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Open fish barrel", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Coal bag", null));
		assertEquals(InventoryFill.Slot.FIXED, InventoryFill.classify(false, "Giant pouch", null));
	}

	@Test
	public void coalBagHoldsMoreWithASmithingCape()
	{
		assertEquals(27, SkillingContainer.coalBagCapacity(false));
		assertEquals(36, SkillingContainer.coalBagCapacity(true));
	}

	// --- Essence pouches (base client EssencePouchOverlay's table) ----------

	@Test
	public void essencePouchCapacityFollowsDegradation()
	{
		assertEquals(3, EssencePouch.SMALL.capacity(0, 99));
		assertEquals(6, EssencePouch.MEDIUM.capacity(0, 99));
		assertEquals(3, EssencePouch.MEDIUM.capacity(400, 99));
		assertEquals(0, EssencePouch.MEDIUM.capacity(800, 99));
		assertEquals(9, EssencePouch.LARGE.capacity(0, 99));
		assertEquals(5, EssencePouch.LARGE.capacity(600, 99));
		assertEquals(12, EssencePouch.GIANT.capacity(0, 99));
		assertEquals(9, EssencePouch.GIANT.capacity(200, 99));
	}

	@Test
	public void colossalPouchScalesWithRunecraftLevel()
	{
		assertEquals(40, EssencePouch.COLOSSAL.capacity(0, 85));
		assertEquals(27, EssencePouch.COLOSSAL.capacity(0, 75));
		assertEquals(16, EssencePouch.COLOSSAL.capacity(0, 50));
		assertEquals(8, EssencePouch.COLOSSAL.capacity(0, 49));
		// Degraded to 35 of 40, scaled for level 75: 35 * 27 / 40.
		assertEquals(23, EssencePouch.COLOSSAL.capacity(320, 75));
		assertEquals(0, EssencePouch.COLOSSAL.capacity(1020, 99));
	}

	@Test
	public void pouchesAreFoundByName()
	{
		assertEquals(EssencePouch.GIANT, EssencePouch.fromItemName("Giant pouch"));
		assertNull(EssencePouch.fromItemName("Rune pouch"));
		assertNull(EssencePouch.fromItemName("Coin pouch"));
	}

	// --- helpers ----------------------------------------------------------------

	private static ContainerRow row(SkillingContainer kind, int contents, int capacity)
	{
		return new ContainerRow(kind, contents, capacity);
	}

	private static List<SkillingContainer> kinds(List<ContainerRow> rows)
	{
		final SkillingContainer[] out = new SkillingContainer[rows.size()];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = rows.get(i).getKind();
		}
		return Arrays.asList(out);
	}

	private static Map<SkillingContainer, Integer> capacities()
	{
		final Map<SkillingContainer, Integer> carried = new EnumMap<>(SkillingContainer.class);
		carried.put(SkillingContainer.LOG_BASKET, 28);
		carried.put(SkillingContainer.FISH_BARREL, 28);
		carried.put(SkillingContainer.COAL_BAG, 27);
		carried.put(SkillingContainer.ESSENCE_POUCHES, 12);
		return carried;
	}

	private static java.util.Set<SkillingContainer> open(SkillingContainer... kinds)
	{
		return java.util.EnumSet.copyOf(Arrays.asList(kinds));
	}

	private static java.util.Set<SkillingContainer> closed(SkillingContainer... kinds)
	{
		return java.util.EnumSet.noneOf(SkillingContainer.class);
	}

	private static ActivityAlerts.Tick frame(int tick, int filling, int empty)
	{
		final ActivityAlerts.Tick t = new ActivityAlerts.Tick();
		t.tick = tick;
		t.animating = true;
		t.filling = filling;
		t.empty = empty;
		t.runEnergy = 100;
		return t;
	}
}
