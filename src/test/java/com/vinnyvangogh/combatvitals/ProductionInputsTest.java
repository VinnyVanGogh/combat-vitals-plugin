package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

public class ProductionInputsTest
{
	private ProductionInputs inputs;

	@Before
	public void setUp()
	{
		inputs = new ProductionInputs();
	}

	private static Map<Integer, Integer> inv(int... idAndQuantity)
	{
		final Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < idAndQuantity.length; i += 2)
		{
			map.put(idAndQuantity[i], idAndQuantity[i + 1]);
		}
		return map;
	}

	@Test
	public void cleaningHerbsCountsTheGrimyOnesLeft()
	{
		inputs.action(Skill.HERBLORE, inv(ItemID.UNIDENTIFIED_RANARR, 28),
			inv(ItemID.UNIDENTIFIED_RANARR, 27, ItemID.RANARR_WEED, 1), 10);
		assertEquals(27, inputs.left(Skill.HERBLORE, inv(ItemID.UNIDENTIFIED_RANARR, 27, ItemID.RANARR_WEED, 1)));
	}

	@Test
	public void anUnfinishedPotionIsLimitedByTheScarcerInput()
	{
		final Map<Integer, Integer> before = inv(ItemID.RANARR_WEED, 14, ItemID.VIAL_WATER, 10);
		final Map<Integer, Integer> after = inv(ItemID.RANARR_WEED, 13, ItemID.VIAL_WATER, 9,
			ItemID.RANARRVIAL, 1);
		inputs.action(Skill.HERBLORE, before, after, 10);
		assertEquals(9, inputs.left(Skill.HERBLORE, after));
	}

	@Test
	public void smithingTakesSeveralBarsAnAction()
	{
		// A rune platebody is five bars.
		inputs.action(Skill.SMITHING, inv(ItemID.RUNITE_BAR, 25), inv(ItemID.RUNITE_BAR, 20), 10);
		assertEquals(4, inputs.left(Skill.SMITHING, inv(ItemID.RUNITE_BAR, 20)));
		assertEquals(0, inputs.left(Skill.SMITHING, inv(ItemID.RUNITE_BAR, 4)));
	}

	@Test
	public void alchingStacksCountDownToo()
	{
		inputs.action(Skill.MAGIC, inv(ItemID.NATURERUNE, 100, ItemID.YEW_LONGBOW + 1, 40),
			inv(ItemID.NATURERUNE, 99, ItemID.YEW_LONGBOW + 1, 39, ItemID.COINS, 768), 10);
		assertEquals(39, inputs.left(Skill.MAGIC, inv(ItemID.NATURERUNE, 99, ItemID.YEW_LONGBOW + 1, 39)));
	}

	@Test
	public void nothingKnownBeforeTheFirstUse()
	{
		assertEquals(-1, inputs.left(Skill.HERBLORE, inv(ItemID.RANARR_WEED, 14)));
		assertNull(inputs.label(Skill.HERBLORE, inv(ItemID.RANARR_WEED, 14)));
	}

	@Test
	public void anotherSkillsInputsAreNotThisOnes()
	{
		inputs.action(Skill.FLETCHING, inv(ItemID.YEW_LOGS, 27), inv(ItemID.YEW_LOGS, 26), 10);
		assertEquals(-1, inputs.left(Skill.COOKING, inv(ItemID.YEW_LOGS, 26)));
	}

	@Test
	public void anInventoryThatLagsATickIsCaughtOnTheNextAction()
	{
		// The experience lands before the inventory: the first action sees no change.
		inputs.action(Skill.COOKING, inv(ItemID.RAW_SHARK, 28), inv(ItemID.RAW_SHARK, 28), 10);
		assertEquals(-1, inputs.left(Skill.COOKING, inv(ItemID.RAW_SHARK, 28)));
		inputs.action(Skill.COOKING, inv(ItemID.RAW_SHARK, 27, ItemID.SHARK, 1),
			inv(ItemID.RAW_SHARK, 27, ItemID.SHARK, 1), 14);
		assertEquals(27, inputs.left(Skill.COOKING, inv(ItemID.RAW_SHARK, 27, ItemID.SHARK, 1)));
	}

	@Test
	public void aBankTripIsNotAnInput()
	{
		inputs.action(Skill.HERBLORE, inv(ItemID.RANARR_WEED, 2, ItemID.VIAL_WATER, 2),
			inv(ItemID.RANARR_WEED, 1, ItemID.VIAL_WATER, 1, ItemID.RANARRVIAL, 1), 10);
		// Deposit the potions, withdraw more: the bank opening rebases.
		inputs.rebase();
		final Map<Integer, Integer> withdrawn = inv(ItemID.RANARR_WEED, 14, ItemID.VIAL_WATER, 14);
		inputs.action(Skill.HERBLORE, withdrawn,
			inv(ItemID.RANARR_WEED, 13, ItemID.VIAL_WATER, 13, ItemID.RANARRVIAL, 1), 40);
		assertEquals(13, inputs.left(Skill.HERBLORE,
			inv(ItemID.RANARR_WEED, 13, ItemID.VIAL_WATER, 13, ItemID.RANARRVIAL, 1)));
	}

	@Test
	public void theLabelSaysWhatIsLeftAndWhenItIsDone()
	{
		final Map<Integer, Integer> logs = inv(ItemID.MAPLE_LOGS, 27);
		inputs.action(Skill.FIREMAKING, inv(ItemID.MAPLE_LOGS, 28), logs, 10);
		assertEquals("27 left", inputs.label(Skill.FIREMAKING, logs));
		inputs.action(Skill.FIREMAKING, logs, inv(ItemID.MAPLE_LOGS, 26), 14);
		// 26 left at 4 ticks each: 26 * 4 * 0.6 s = 62 s.
		assertEquals("26 left, done in ~1m", inputs.label(Skill.FIREMAKING, inv(ItemID.MAPLE_LOGS, 26)));
		assertEquals("13 left, done in ~31s", inputs.label(Skill.FIREMAKING, inv(ItemID.MAPLE_LOGS, 13)));
		assertEquals("None left", inputs.label(Skill.FIREMAKING, inv()));
	}

	@Test
	public void aLongPauseIsNotTheActionsPace()
	{
		final Map<Integer, Integer> logs = inv(ItemID.MAPLE_LOGS, 27);
		inputs.action(Skill.FIREMAKING, inv(ItemID.MAPLE_LOGS, 28), logs, 10);
		inputs.action(Skill.FIREMAKING, logs, inv(ItemID.MAPLE_LOGS, 26), 500);
		assertEquals("26 left", inputs.label(Skill.FIREMAKING, inv(ItemID.MAPLE_LOGS, 26)));
	}

	@Test
	public void forgetDropsWhatWasLearned()
	{
		inputs.action(Skill.FLETCHING, inv(ItemID.YEW_LOGS, 27), inv(ItemID.YEW_LOGS, 26), 10);
		inputs.forget();
		assertEquals(-1, inputs.left(Skill.FLETCHING, inv(ItemID.YEW_LOGS, 26)));
	}
}
