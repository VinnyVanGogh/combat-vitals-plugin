package com.vinnyvangogh.combatvitals;

import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NPCManager;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/**
 * Combat Vitals — a standalone replacement for the stock Opponent Information
 * plugin, with overhead rendering.
 *
 * The readout is rebuilt once per game tick and published as an immutable
 * snapshot. The overlays render whatever the latest snapshot holds and compute
 * nothing themselves, because they run every frame and the underlying health
 * bar only changes on a tick.
 */
@Slf4j
@PluginDescriptor(
	name = "Combat Vitals",
	description = "Health for your target and for you, drawn over the target or in a panel",
	tags = {"combat", "vitals", "health", "hitpoints", "hp", "overlay", "overhead", "opponent", "pvp", "pvm"}
)
public class CombatVitalsPlugin extends Plugin
{
	private static final String STOCK_PLUGIN_NAME = "Opponent Information";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CombatVitalsConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NPCManager npcManager;

	@Inject
	private HiscoreManager hiscoreManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private CombatVitalsPanelOverlay panelOverlay;

	@Inject
	private CombatVitalsOverheadOverlay overheadOverlay;

	@Inject
	private Notifier notifier;

	/** Replaced wholesale each tick, never mutated, so the overlay thread sees a consistent view. */
	@Getter
	private volatile Readout readout;

	/** Your own health. Exact, because the client knows it — never recovered. */
	@Getter
	private volatile Readout selfReadout;

	/** The skilling badge, for the fishing spot or other skilling NPC being worked. */
	@Getter
	private volatile SkillingReadout skillingReadout;

	private Actor opponent;

	/**
	 * Whether the interaction is a fight. Interacting with an NPC is not proof:
	 * fishing spots, rocks, bankers and pets are NPCs too.
	 */
	private final CombatState combat = new CombatState();

	/** The skilling NPC being worked, when the interaction is not a fight. */
	private NPC skillingTarget;

	private final SkillingState skilling = new SkillingState();
	private final BoostTracker boosts = new BoostTracker();
	private final BoostTimer boostTimer = new BoostTimer();

	/** Boost levels are seeded from the client once, for a plugin enabled mid-session. */
	private boolean boostsSeeded;

	/** Ticks until the next boost drop, computed once per tick for the snapshot. */
	private int ticksUntilDrop = -1;

	private int lastSpecEnergy = -1;

	private HiscoreEndpoint hiscoreEndpoint = HiscoreEndpoint.NORMAL;
	private Plugin stockPlugin;

	/** Client thread only. Null until the plugin starts. */
	private volatile NativeHealthBarHider<SpritePixels> healthBarHider;

	/** Client thread only, the overlay included. Null until the plugin starts. */
	private volatile OverheadChatHolder<Actor> chatHolder;

	/** Actors whose readout covered their chat line in the latest frame, published by the overlay. */
	private volatile Set<Actor> chatCovered = Collections.emptySet();

	@Provides
	CombatVitalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatVitalsConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(panelOverlay);
		overlayManager.add(overheadOverlay);

		stockPlugin = pluginManager.getPlugins().stream()
			.filter(p -> STOCK_PLUGIN_NAME.equals(p.getName()))
			.findFirst()
			.orElse(null);

		healthBarHider = new NativeHealthBarHider<>(client.getSpriteOverrides(), this::blankSprite);
		chatHolder = new OverheadChatHolder<>(new OverheadChatHolder.Access<Actor>()
		{
			@Override
			public String text(Actor actor)
			{
				return actor.getOverheadText();
			}

			@Override
			public int cyclesRemaining(Actor actor)
			{
				return actor.getOverheadCycle();
			}

			@Override
			public void setText(Actor actor, String text)
			{
				actor.setOverheadText(text);
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(panelOverlay);
		overlayManager.remove(overheadOverlay);

		// shutDown runs off the client thread; the sprite map belongs to it.
		final NativeHealthBarHider<SpritePixels> hider = healthBarHider;
		healthBarHider = null;
		if (hider != null)
		{
			clientThread.invoke(() -> restoreHealthBars(hider));
		}

		// Every hidden chat line goes back to the game, with the time it has left.
		final OverheadChatHolder<Actor> holder = chatHolder;
		chatHolder = null;
		chatCovered = Collections.emptySet();
		if (holder != null)
		{
			clientThread.invoke(holder::releaseAll);
		}

		readout = null;
		selfReadout = null;
		opponent = null;
		combat.clear();
		clearSkilling();
		stockPlugin = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				hiscoreEndpoint = HiscoreEndpoint.fromWorldTypes(client.getWorldType());
				// Logging in restarts the drain segment (OSRS wiki, "Temporary
				// skill boost"): the one anchor available before a drop is seen.
				boostTimer.anchorLogin(client.getTickCount());
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				opponent = null;
				combat.clear();
				readout = null;
				selfReadout = null;
				clearSkilling();
				chatCovered = Collections.emptySet();
				final OverheadChatHolder<Actor> holder = chatHolder;
				if (holder != null)
				{
					holder.releaseAll();
				}
				break;
			default:
				break;
		}
	}

	private void clearSkilling()
	{
		skillingTarget = null;
		skillingReadout = null;
		skilling.clear();
		boosts.clear();
		boostTimer.clear();
		boostsSeeded = false;
		ticksUntilDrop = -1;
		lastSpecEnergy = -1;
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		// The single gate the whole compliance argument rests on: this plugin
		// only ever tracks the target the user themselves engaged.
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		final Actor target = event.getTarget();
		if (target != null)
		{
			// Held even when it cannot be fought, so that a later hitsplat can
			// prove otherwise; the readouts below check attackability first.
			opponent = target;
			final boolean fight = attackable(target);
			combat.engage(fight);

			// Not a fight: perhaps a fishing spot. The badge takes the NPC only
			// when its menu names a skill; anything else drops the badge at once.
			final Skill trained = fight ? null : skillingSkill(target);
			if (trained != null)
			{
				skillingTarget = (NPC) target;
				skilling.engage(trained);
			}
			else
			{
				skillingTarget = null;
				skilling.release();
			}
		}
		else
		{
			// Keep the opponent: the health bar lingers after combat and the
			// readout should too. The timeout below retires it.
			final Instant now = Instant.now();
			combat.disengage(now);
			skilling.disengage(now);
		}
	}

	/** The skill a non-attackable NPC trains, from its menu, or null. */
	private static Skill skillingSkill(Actor target)
	{
		if (!(target instanceof NPC))
		{
			return null;
		}

		final NPC npc = (NPC) target;
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getComposition();
		}
		if (composition == null)
		{
			return null;
		}

		return SkillingActions.skill(composition.getActions(), composition.getName());
	}

	/**
	 * The badge's boost tracking. Every skill is watched, not only the one on
	 * the badge: the usual order is drink, then click the spot, and a drop in
	 * any non-combat skill fixes the phase of the shared drain timer.
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		final Skill skill = event.getSkill();
		final int tick = client.getTickCount();
		final int real = event.getLevel();
		final int boosted = event.getBoostedLevel();

		final BoostTracker.Change change = boosts.observe(skill, real, boosted);
		if ((change == BoostTracker.Change.DROP || change == BoostTracker.Change.EXPIRED)
			&& BoostTimer.onSkillingTimer(skill))
		{
			boostTimer.anchorDrop(tick);
		}

		final SkillingState.Warning warning = skilling.boostChanged(skill, change, tick);
		if (warning != SkillingState.Warning.NONE)
		{
			final String message = warning == SkillingState.Warning.EXPIRED
				? skill.getName() + " boost has worn off"
				: skill.getName() + " boost dropped to +" + (boosted - real);
			notifier.notify(config.skillingBoostNotify(), message);
		}
	}

	/** Drink or Eat on an item: remembered so a boost that follows can be named. */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!event.isItemOp())
		{
			return;
		}

		final String option = event.getMenuOption();
		if (!"Drink".equalsIgnoreCase(option) && !"Eat".equalsIgnoreCase(option))
		{
			return;
		}

		skilling.consumed(client.getItemDefinition(event.getItemId()).getName(), client.getTickCount());
	}

	/** Special attack energy falling while a skilling spec tool is wielded is that tool's special. */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != VarPlayerID.SA_ENERGY)
		{
			return;
		}

		final int energy = event.getValue();
		if (lastSpecEnergy != -1 && energy < lastSpecEnergy)
		{
			final String weapon = weaponName();
			final Skill toolSkill = SkillingTools.specSkill(weapon);
			if (toolSkill != null)
			{
				skilling.specUsed(weapon, toolSkill, client.getTickCount());
			}
		}
		lastSpecEnergy = energy;
	}

	/** Fishing spots move by despawning; the badge has nothing left to sit over. */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == skillingTarget)
		{
			skillingTarget = null;
			skilling.release();
		}
	}

	/**
	 * Hitsplats are the proof of combat that interaction alone is not. One on
	 * you, or one you dealt, keeps your own bar up for the timeout, and one you
	 * dealt to the held target proves it attackable whatever its menu said.
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		final Player self = client.getLocalPlayer();
		if (self == null)
		{
			return;
		}

		final Actor actor = event.getActor();
		final boolean onMe = actor == self;
		if (!CombatState.evidence(onMe, event.getHitsplat()))
		{
			return;
		}

		final boolean onTarget = !onMe && actor == opponent && event.getHitsplat().isMine();
		combat.hit(onTarget, Instant.now());
	}

	/**
	 * Every client tick rather than on config change, so that when Interface
	 * Styles rewrites or clears the same sprite ids ours is back within a
	 * frame, and so that turning the option off restores without a separate
	 * listener.
	 */
	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		updateChatHolds();

		final NativeHealthBarHider<SpritePixels> hider = healthBarHider;
		if (hider == null)
		{
			return;
		}

		if (config.hideGameHealthBar())
		{
			if (hider.apply())
			{
				client.resetHealthBarCaches();
			}
		}
		else
		{
			restoreHealthBars(hider);
		}
	}

	/**
	 * Hides the game's chat line for each actor the overlay redrew it for last
	 * frame, and gives it back to every other. Every client tick, so turning
	 * the option off, or the readout moving clear, restores within a frame.
	 *
	 * The set only ever holds actors with a readout, and a readout only exists
	 * for a real fight ({@link CombatState#showTarget()}), so a skilling NPC's
	 * chat is never moved or hidden: no readout means no chat move.
	 */
	private void updateChatHolds()
	{
		final OverheadChatHolder<Actor> holder = chatHolder;
		if (holder == null)
		{
			return;
		}

		if (config.hideGameOverheadChat() && config.showOverhead())
		{
			holder.update(chatCovered);
		}
		else
		{
			holder.releaseAll();
		}
	}

	/**
	 * The actor's overhead chat as the overlay should draw it, including a line
	 * this plugin has taken off the game. Client thread only.
	 *
	 * @return the text, or null when no chat is up
	 */
	String overheadChat(Actor actor)
	{
		final OverheadChatHolder<Actor> holder = chatHolder;
		if (holder != null)
		{
			return holder.chat(actor);
		}

		final String text = actor.getOverheadText();
		return OverheadChat.showing(text, actor.getOverheadCycle()) ? text : null;
	}

	/** Called by the overlay once per frame with the actors whose chat it redrew. */
	void setChatCovered(Set<Actor> covered)
	{
		chatCovered = covered;
	}

	private void restoreHealthBars(NativeHealthBarHider<SpritePixels> hider)
	{
		if (hider.restore())
		{
			client.resetHealthBarCaches();
		}
	}

	/**
	 * A fully transparent sprite the size of the one it replaces. Keeping the
	 * size keeps the client's overhead stacking unchanged, so head icons and
	 * the like stay exactly where they were. Null while the sprite cannot be
	 * read yet; {@link NativeHealthBarHider} retries next tick.
	 */
	private SpritePixels blankSprite(int spriteId)
	{
		final SpritePixels[] original = client.getSprites(client.getIndexSprites(), spriteId, 0);
		if (original == null || original.length == 0 || original[0] == null)
		{
			return null;
		}

		final int width = original[0].getWidth();
		final int height = original[0].getHeight();
		if (width <= 0 || height <= 0)
		{
			return null;
		}

		// Pixel value 0 is transparent to the client's sprite rasteriser.
		return client.createSpritePixels(new int[width * height], width, height);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		expireOpponent();
		expireSkillingTarget();
		seedBoosts();
		tickBoostTimer();
		readout = buildReadout();
		selfReadout = buildSelfReadout();
		skillingReadout = buildSkillingReadout();
	}

	private void expireSkillingTarget()
	{
		if (skillingTarget == null)
		{
			return;
		}

		final Player self = client.getLocalPlayer();
		final boolean stillOnTarget = self != null && self.getInteracting() == skillingTarget;

		if (skilling.expire(Instant.now(), timeout(), stillOnTarget))
		{
			skillingTarget = null;
		}
	}

	/**
	 * A plugin enabled mid-session has seen no stat updates. Seed from the
	 * client so the first real change is classified rather than swallowed.
	 */
	private void seedBoosts()
	{
		if (boostsSeeded || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (Skill skill : Skill.values())
		{
			if (!boosts.isSeeded(skill))
			{
				boosts.seed(skill, client.getBoostedSkillLevel(skill));
			}
		}
		lastSpecEnergy = client.getVarpValue(VarPlayerID.SA_ENERGY);
		boostsSeeded = true;
	}

	/**
	 * Advances the drain timer every tick, badge or no badge, so its phase
	 * survives long stretches with nothing boosted. Preserve is read here, on
	 * the client thread, the same way the Boosts plugin does.
	 */
	private void tickBoostTimer()
	{
		final int tick = client.getTickCount();
		ticksUntilDrop = boostTimer.ticksUntilDrop(tick, client.isPrayerActive(Prayer.PRESERVE));
		if (ticksUntilDrop <= 0 && boostTimer.isKnown())
		{
			boostTimer.roll(tick);
			ticksUntilDrop = boostTimer.ticksUntilDrop(tick, client.isPrayerActive(Prayer.PRESERVE));
		}
	}

	/**
	 * The skilling badge. Only while the interaction is not a fight: a hit
	 * landing on the held NPC promotes it to an opponent, and the health bar
	 * takes over.
	 */
	private SkillingReadout buildSkillingReadout()
	{
		final Skill skill = skilling.skill();
		final NPC target = skillingTarget;
		if (!config.skillingEnabled() || skill == null || target == null || combat.showTarget())
		{
			return null;
		}

		final int tick = client.getTickCount();
		final int real = client.getRealSkillLevel(skill);
		final int boosted = client.getBoostedSkillLevel(skill);

		int specPercent = -1;
		if (config.skillingSpecEnergy() && SkillingTools.specSkill(weaponName()) == skill)
		{
			// SA_ENERGY is in tenths of a percent.
			specPercent = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
		}

		return new SkillingReadout(
			target,
			skill,
			real,
			boosted,
			config.skillingBoostTimer() ? ticksUntilDrop : -1,
			boostTimer.cycleLength(),
			boostTimer.isConfirmed(),
			specPercent,
			config.skillingBoostSource() ? skilling.source() : null,
			skilling.warning(tick),
			System.currentTimeMillis());
	}

	/** The wielded weapon's name, or null with nothing in the slot. */
	private String weaponName()
	{
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return null;
		}

		final Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null)
		{
			return null;
		}

		return client.getItemDefinition(weapon.getId()).getName();
	}

	private void expireOpponent()
	{
		if (opponent == null)
		{
			return;
		}

		// Only interacting with the opponent itself holds it; interacting with
		// anything else, a fishing spot included, lets the timeout run.
		final Player self = client.getLocalPlayer();
		final boolean stillOnTarget = self != null && self.getInteracting() == opponent;

		if (combat.expire(Instant.now(), timeout(), stillOnTarget))
		{
			opponent = null;
		}
	}

	private Duration timeout()
	{
		return Duration.ofSeconds(Math.max(1, config.opponentTimeout()));
	}

	/** Players can always be fought; an NPC only if its menu or combat level says so. */
	private static boolean attackable(Actor target)
	{
		if (!(target instanceof NPC))
		{
			return true;
		}

		final NPC npc = (NPC) target;
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getComposition();
		}
		if (composition == null)
		{
			return false;
		}

		return CombatState.attackable(composition.getActions(), composition.getCombatLevel());
	}

	private Readout buildReadout()
	{
		final Actor actor = opponent;
		if (actor == null || actor.getName() == null || !combat.showTarget())
		{
			return null;
		}

		final int scale = actor.getHealthScale();
		final int ratio = actor.getHealthRatio();

		// A despawned actor keeps a stale scale after its ratio has gone.
		if (scale <= 0 || ratio < 0)
		{
			return null;
		}

		final boolean npc = actor instanceof NPC;
		if (npc ? !config.showForNpcs() : !config.showForPlayers())
		{
			return null;
		}

		if (config.hideWhenHpHud() && hasHpHud(actor))
		{
			return null;
		}

		String name = Text.removeTags(actor.getName());
		int maxHealth = -1;
		HiscoreResult hiscore = null;

		if (npc)
		{
			final NPC target = (NPC) actor;
			final NPCComposition composition = target.getTransformedComposition();
			if (composition != null)
			{
				final String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
				if (longName != null && !longName.isEmpty())
				{
					name = longName;
				}
			}

			final Integer health = npcManager.getHealth(target.getId());
			if (health != null)
			{
				maxHealth = health;
			}
		}
		else if (config.showPlayerHitpoints() || config.showStatComparison())
		{
			// One lookup per opponent, in direct response to the user engaging
			// them. HiscoreManager returns null until its cache is warm and
			// refreshes off the client thread, so this never blocks and never
			// repeats a request. The comparison reads this same result.
			hiscore = hiscoreManager.lookupAsync(name, hiscoreEndpoint);

			if (hiscore != null && config.showPlayerHitpoints())
			{
				final net.runelite.client.hiscore.Skill hitpoints =
					hiscore.getSkill(HiscoreSkill.HITPOINTS);

				// An unranked player comes back non-positive. Leaving maxHealth
				// unknown degrades to a percentage rather than a wrong number.
				if (hitpoints != null && hitpoints.getLevel() > 0)
				{
					maxHealth = hitpoints.getLevel();
				}
			}
		}

		final HealthRecovery.Range range = maxHealth > 0
			? HealthRecovery.recover(ratio, scale, maxHealth)
			: null;

		return new Readout(actor, name, ratio, scale, maxHealth, range, npc, -1, hiscore);
	}

	/**
	 * Your own health, taken from the hitpoints orb rather than inverted from a
	 * health bar.
	 *
	 * The client knows this number exactly. Running it through the recovery
	 * would print a guess in place of a fact, which would be absurd on its own
	 * terms and would undermine the one claim this plugin actually makes.
	 *
	 * Only while fighting: a permanent readout over your own head is clutter,
	 * and the orb already covers the out-of-combat case. Fighting means an
	 * attackable target or a recent hitsplat, so being attacked while skilling
	 * shows it and clicking a fishing spot does not.
	 */
	private Readout buildSelfReadout()
	{
		if (!config.showOwnHealth() || !combat.inCombat(Instant.now(), timeout()))
		{
			return null;
		}

		final Player self = client.getLocalPlayer();
		if (self == null || self.getName() == null)
		{
			return null;
		}

		final int current = client.getBoostedSkillLevel(Skill.HITPOINTS);
		final int max = client.getRealSkillLevel(Skill.HITPOINTS);
		if (max <= 0)
		{
			return null;
		}

		return new Readout(self, Text.removeTags(self.getName()), -1, -1, max, null, false, current, null);
	}

	/** Mirrors the base client: NPC only, and false for players by definition. */
	private boolean hasHpHud(Actor actor)
	{
		if (!(actor instanceof NPC) || client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS_DISABLED) != 0)
		{
			return false;
		}

		final NPC npc = (NPC) actor;
		final int hudNpcId = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		return hudNpcId != -1
			&& npc.getComposition() != null
			&& hudNpcId == npc.getComposition().getId();
	}

	/** True when the stock plugin is drawing its own panel in the same corner. */
	boolean stockPluginEnabled()
	{
		return stockPlugin != null && pluginManager.isPluginEnabled(stockPlugin);
	}

	/** Immutable snapshot of everything the overlays need for one tick. */
	@Getter
	static final class Readout
	{
		private final Actor actor;
		private final String name;
		private final int ratio;
		private final int scale;
		private final int maxHealth;
		private final HealthRecovery.Range range;
		private final boolean npc;

		/** Known exactly, or -1 when it can only be recovered from the bar. */
		private final int exactHealth;

		/** The one cached hiscores result, shared by the readout and comparison. */
		private final HiscoreResult hiscore;

		Readout(Actor actor, String name, int ratio, int scale, int maxHealth,
			HealthRecovery.Range range, boolean npc, int exactHealth, HiscoreResult hiscore)
		{
			this.actor = actor;
			this.name = name;
			this.ratio = ratio;
			this.scale = scale;
			this.maxHealth = maxHealth;
			this.range = range;
			this.npc = npc;
			this.exactHealth = exactHealth;
			this.hiscore = hiscore;
		}
	}
}
