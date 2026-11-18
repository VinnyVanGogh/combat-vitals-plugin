package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.util.Text;

/**
 * Splashing mode, the client side: reads the player's own Magic attack bonus,
 * Magic level and prayer, and the NPC the player is casting at, once a tick,
 * and hands them to {@link SplashTracker}. Information only: it reads state
 * and shows text, and never casts, clicks or changes a menu.
 *
 * "Casting" is autocast on (varbit AUTOCAST_SET), or a spell the player
 * clicked on that NPC in the last {@link #MANUAL_TICKS} ticks, seen by
 * reading, never consuming, the {@code MenuOptionClicked} event.
 */
@Singleton
class SplashWatcher
{
	/** How long a hand-cast spell counts as casting at that NPC. */
	static final int MANUAL_TICKS = 8;

	private static final Prayer[] MAGIC_PRAYERS = {
		Prayer.AUGURY, Prayer.MYSTIC_VIGOUR, Prayer.MYSTIC_MIGHT, Prayer.MYSTIC_LORE, Prayer.MYSTIC_WILL};
	/** The wiki's prayer multipliers, in {@link #MAGIC_PRAYERS} order. */
	private static final double[] PRAYER_BONUS = {1.25, 1.18, 1.15, 1.10, 1.05};

	private final Client client;
	private final ItemManager itemManager;
	private final CombatVitalsConfig config;
	private final SplashTracker tracker = new SplashTracker(MonsterMagic.load());

	private volatile SplashTracker.Result result;
	private int manualNpcIndex = -1;
	private int manualSpell;
	private int manualTicks;

	@Inject
	SplashWatcher(Client client, ItemManager itemManager, CombatVitalsConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.config = config;
	}

	/** The latest read-out for the overlay, or null when nothing is being cast at an NPC. */
	SplashTracker.Result result()
	{
		return config.splashMode() ? result : null;
	}

	void reset()
	{
		result = null;
		manualNpcIndex = -1;
		tracker.reset();
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!config.splashMode() || event.getMenuAction() != MenuAction.WIDGET_TARGET_ON_NPC)
		{
			return;
		}
		final NPC npc = event.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}
		final Widget spell = client.getSelectedWidget();
		manualNpcIndex = npc.getIndex();
		manualSpell = spell == null ? 0 : spell.getId();
		manualTicks = MANUAL_TICKS;
	}

	/**
	 * Reads this tick. Returns the setup-changed alert text, or null; the
	 * plugin sends it, so the sidebar mute and "Mute alert sounds" apply.
	 */
	String tick()
	{
		if (!config.splashMode())
		{
			reset();
			return null;
		}
		if (manualTicks > 0)
		{
			manualTicks--;
		}
		final SplashTracker.Input in = read();
		if (in == null)
		{
			result = null;
			tracker.idle();
			return null;
		}
		final SplashTracker.Result now = tracker.step(in, config.splashMaxChance());
		result = now;
		return now.changed;
	}

	private SplashTracker.Input read()
	{
		if (client.getLocalPlayer() == null)
		{
			return null;
		}
		final Actor target = client.getLocalPlayer().getInteracting();
		if (!(target instanceof NPC))
		{
			return null;
		}
		final NPC npc = (NPC) target;
		final boolean auto = client.getVarbitValue(VarbitID.AUTOCAST_SET) == 1;
		final boolean manual = manualTicks > 0 && manualNpcIndex == npc.getIndex();
		if (!auto && !manual)
		{
			return null;
		}
		final int spell = manual && !auto ? 100_000 + manualSpell : client.getVarbitValue(VarbitID.AUTOCAST_SPELL);
		final String name = npc.getName() == null ? "this NPC" : Text.removeTags(npc.getName());
		final int[] gear = worn();
		return new SplashTracker.Input(spell, Arrays.hashCode(gear), npc.getIndex(), npc.getId(), name,
			magicBonus(gear), client.getBoostedSkillLevel(Skill.MAGIC), prayerBonus());
	}

	/** The item ids worn, in slot order, -1 for an empty slot. */
	private int[] worn()
	{
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return new int[0];
		}
		final Item[] items = worn.getItems();
		final int[] ids = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			ids[i] = items[i].getId();
		}
		return ids;
	}

	/** The Equipment stats window's Magic attack: the sum over everything worn. */
	private int magicBonus(int[] gear)
	{
		int total = 0;
		for (int id : gear)
		{
			if (id <= 0)
			{
				continue;
			}
			final ItemStats stats = itemManager.getItemStats(id);
			final ItemEquipmentStats equipment = stats == null ? null : stats.getEquipment();
			if (equipment != null)
			{
				total += equipment.getAmagic();
			}
		}
		return total;
	}

	private double prayerBonus()
	{
		for (int i = 0; i < MAGIC_PRAYERS.length; i++)
		{
			if (client.isPrayerActive(MAGIC_PRAYERS[i]))
			{
				return PRAYER_BONUS[i];
			}
		}
		return 1.0;
	}
}
