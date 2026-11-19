package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.util.Text;

/**
 * Splashing mode and the casts-left read-out, the client side: reads the
 * player's own Magic attack bonus, Magic level and prayer, the runes they
 * carry, and the NPC the player is casting at, once a tick, and hands them to
 * {@link SplashTracker} and {@link SpellCasts}. Information only: it reads
 * state and shows text, and never casts, clicks or changes a menu.
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

	private static final int[] POUCH_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4};
	private static final int[] POUCH_QUANTITIES = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4};

	/** The item shown for each rune in a spell's cost, by {@link SpellCasts.Rune} ordinal. */
	private static final int[] RUNE_ITEMS = {ItemID.AIRRUNE, ItemID.WATERRUNE, ItemID.EARTHRUNE, ItemID.FIRERUNE,
		ItemID.MINDRUNE, ItemID.CHAOSRUNE, ItemID.DEATHRUNE, ItemID.BLOODRUNE, ItemID.SOULRUNE, ItemID.WRATHRUNE};

	private final Client client;
	private final ItemManager itemManager;
	private final CombatVitalsConfig config;
	private final SplashTracker tracker = new SplashTracker(MonsterMagic.load());
	private final SplashTimer timer = new SplashTimer();
	private final Map<Integer, String> names = new HashMap<>();

	private volatile SplashReadout readout;
	private int manualNpcIndex = -1;
	private int manualSpell;
	private String manualSpellName;
	private int manualTicks;
	private String spellName;
	private boolean lowArmed = true;
	private String lowAlert;

	@Inject
	SplashWatcher(Client client, ItemManager itemManager, CombatVitalsConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.config = config;
	}

	/** The latest read-out for the overlay, or null when nothing is being cast at an NPC. */
	SplashReadout readout()
	{
		return on() ? readout : null;
	}

	private boolean on()
	{
		return config.splashMode() || config.castsLeft();
	}

	void reset()
	{
		readout = null;
		manualNpcIndex = -1;
		lowArmed = true;
		lowAlert = null;
		tracker.reset();
		timer.reset();
	}

	/** The low-casts alert text due this tick, once, or null. */
	String takeLowCastsAlert()
	{
		final String text = lowAlert;
		lowAlert = null;
		return text;
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!on() || event.getMenuAction() != MenuAction.WIDGET_TARGET_ON_NPC)
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
		manualSpellName = spell == null || spell.getName() == null ? null : Text.removeTags(spell.getName());
		manualTicks = MANUAL_TICKS;
	}

	/**
	 * Reads this tick. Returns the setup-changed alert text, or null; the
	 * plugin sends it, so the sidebar mute and "Mute alert sounds" apply.
	 */
	String tick()
	{
		if (!on())
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
			readout = null;
			tracker.idle();
			timer.idle();
			return null;
		}
		final long now = System.currentTimeMillis();
		timer.cast(now);
		final SplashTracker.Result splash = tracker.step(in, config.splashMaxChance());
		final boolean splashing = config.splashMode();
		readout = build(splashing ? splash : null, now);
		return splashing ? splash.changed : null;
	}

	private SplashReadout build(SplashTracker.Result splash, long now)
	{
		String castsText = null;
		int casts = -2;
		boolean low = false;
		int[] items = new int[0];
		int[] costs = new int[0];
		final int[] cost = SpellCasts.cost(spellName);
		if (config.castsLeft() && cost != null)
		{
			final SpellCasts.Result r = SpellCasts.casts(cost, supply());
			casts = r.casts;
			castsText = SplashReadout.castsText(r);
			final int warnAt = config.castsWarnAt();
			low = warnAt > 0 && casts != SpellCasts.INFINITE && casts <= warnAt;
			pace(low, casts);
			items = new int[cost.length];
			costs = new int[cost.length];
			int n = 0;
			for (int i = 0; i < cost.length; i++)
			{
				if (cost[i] > 0)
				{
					items[n] = RUNE_ITEMS[i];
					costs[n++] = cost[i];
				}
			}
			items = Arrays.copyOf(items, n);
			costs = Arrays.copyOf(costs, n);
		}
		final long idle = Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks())
			* (long) Constants.CLIENT_TICK_LENGTH;
		final long logout = client.getIdleTimeout() * (long) Constants.CLIENT_TICK_LENGTH;
		return new SplashReadout(splash, spellName, castsText, casts, low, items, costs, idle, logout,
			timer.elapsed(now), now);
	}

	/** Fires the low-casts alert once as the count drops to the warn level, and again after a refill. */
	private void pace(boolean low, int casts)
	{
		if (!low)
		{
			lowArmed = true;
			return;
		}
		if (lowArmed)
		{
			lowArmed = false;
			lowAlert = "Low on casts: " + spellName + " has " + casts + (casts == 1 ? " cast" : " casts")
				+ " left.";
		}
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
		final int autoSpell = client.getVarbitValue(VarbitID.AUTOCAST_SPELL);
		final int spell = manual && !auto ? 100_000 + manualSpell : autoSpell;
		spellName = manual && !auto ? manualSpellName : SpellCasts.autocastName(autoSpell);
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

	/** Runes in the inventory and rune pouch, and the staff and tome worn. */
	private SpellCasts.Supply supply()
	{
		final SpellCasts.Supply supply = new SpellCasts.Supply();
		boolean pouch = false;
		final ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() <= 0)
				{
					continue;
				}
				if (!addRune(supply, item.getId(), item.getQuantity()))
				{
					pouch |= name(item.getId()).contains("rune pouch");
				}
			}
		}
		if (pouch)
		{
			addPouch(supply);
		}
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			for (Item item : worn.getItems())
			{
				if (item.getId() > 0)
				{
					supply.item(name(item.getId()));
				}
			}
		}
		return supply;
	}

	private void addPouch(SpellCasts.Supply supply)
	{
		final EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runes == null)
		{
			return;
		}
		for (int slot = 0; slot < POUCH_TYPES.length; slot++)
		{
			final int type = client.getVarbitValue(POUCH_TYPES[slot]);
			final int quantity = client.getVarbitValue(POUCH_QUANTITIES[slot]);
			if (type > 0 && quantity > 0)
			{
				addRune(supply, runes.getIntValue(type), quantity);
			}
		}
	}

	/** Adds a rune item to the supply; false when the item is not one the spells use. */
	private static boolean addRune(SpellCasts.Supply supply, int itemId, long count)
	{
		switch (itemId)
		{
			case ItemID.AIRRUNE:
				supply.rune(SpellCasts.Rune.AIR, count);
				return true;
			case ItemID.WATERRUNE:
				supply.rune(SpellCasts.Rune.WATER, count);
				return true;
			case ItemID.EARTHRUNE:
				supply.rune(SpellCasts.Rune.EARTH, count);
				return true;
			case ItemID.FIRERUNE:
			case ItemID.SUNFIRERUNE:
				supply.rune(SpellCasts.Rune.FIRE, count);
				return true;
			case ItemID.MINDRUNE:
				supply.rune(SpellCasts.Rune.MIND, count);
				return true;
			case ItemID.CHAOSRUNE:
				supply.rune(SpellCasts.Rune.CHAOS, count);
				return true;
			case ItemID.DEATHRUNE:
				supply.rune(SpellCasts.Rune.DEATH, count);
				return true;
			case ItemID.BLOODRUNE:
				supply.rune(SpellCasts.Rune.BLOOD, count);
				return true;
			case ItemID.SOULRUNE:
				supply.rune(SpellCasts.Rune.SOUL, count);
				return true;
			case ItemID.WRATHRUNE:
				supply.rune(SpellCasts.Rune.WRATH, count);
				return true;
			case ItemID.DUSTRUNE:
				supply.combo(SpellCasts.Combo.DUST, count);
				return true;
			case ItemID.MISTRUNE:
				supply.combo(SpellCasts.Combo.MIST, count);
				return true;
			case ItemID.MUDRUNE:
				supply.combo(SpellCasts.Combo.MUD, count);
				return true;
			case ItemID.SMOKERUNE:
				supply.combo(SpellCasts.Combo.SMOKE, count);
				return true;
			case ItemID.STEAMRUNE:
				supply.combo(SpellCasts.Combo.STEAM, count);
				return true;
			case ItemID.LAVARUNE:
				supply.combo(SpellCasts.Combo.LAVA, count);
				return true;
			default:
				return false;
		}
	}

	private String name(int itemId)
	{
		return names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName().toLowerCase(Locale.ROOT));
	}
}
