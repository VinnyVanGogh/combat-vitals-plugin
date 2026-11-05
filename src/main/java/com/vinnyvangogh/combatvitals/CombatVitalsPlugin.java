package com.vinnyvangogh.combatvitals;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SpritePixels;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.FishingSpot;
import net.runelite.client.game.NPCManager;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
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

	/** RuneLite's Status Bars plugin, whose pillars ours sit beside, and its config group and keys. */
	private static final String STATUS_BARS_NAME = "Status Bars";
	private static final String STATUS_BARS_GROUP = "statusbars";
	private static final String STATUS_BARS_DISABLED = "DISABLED";

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
	private SkillingBadgeOverlay skillingBadgeOverlay;

	@Inject
	private PillarOverlay pillarOverlay;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Notifier notifier;

	@Inject
	private EventBus eventBus;

	@Inject
	private NextSpotOverlay nextSpotOverlay;

	/** Finds the next fishing spot and paths to or highlights it. Client thread only; null until started. */
	private SpotPathing spotPathing;

	/** The bundled fishing spot locations, read once at start-up. */
	private List<NextFishingSpot.Spot> knownSpots = Collections.emptyList();

	/** The next fishing spot to outline, when it could not go to Shortest Path. */
	@Getter
	private volatile NPC highlightedSpot;

	/** Where the overlay drew the skilling badge in the latest frame, or null. */
	private volatile Rectangle badgeBounds;

	/** Replaced wholesale each tick, never mutated, so the overlay thread sees a consistent view. */
	@Getter
	private volatile Readout readout;

	/** Your own health. Exact, because the client knows it — never recovered. */
	@Getter
	private volatile Readout selfReadout;

	/**
	 * The skilling badge, while a fishing spot, tree, rock or other skilling
	 * NPC or object is being worked. Drawn over the local player, never over
	 * the target.
	 */
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

	/** The tree, rock or stall being worked, when the badge is for an object. */
	private final ObjectSkilling objectSkilling = new ObjectSkilling();

	/**
	 * The badge is up for an object rather than an NPC. Outlives the object
	 * itself: clicking the next rock keeps the badge lingering until the new
	 * swing starts, instead of flickering off for the walk.
	 */
	private boolean objectBadge;

	/**
	 * The NPC the last menu click was on, and the skill its option trains,
	 * until the interaction it starts takes them. This is what tells a
	 * pickpocket from an attack on the same Man.
	 */
	private NPC clickedNpc;
	private Skill clickedNpcSkill;

	/** The tree, rock or stall clicked last, for drawing the badge over it, and its larger side in tiles. */
	private WorldPoint objectSpot;
	private int objectSpotSize;

	/** Keeps the badge up as "Not fishing" once you stop. */
	private final IdleBadge idleBadge = new IdleBadge();

	/** How long "Needs: ..." stays on the badge after the click: about as long as the game's dialog. */
	private static final long GEAR_WARNING_MILLIS = 8_000;

	/** "Needs: Fishing rod + bait" from the last fishing click, and when; null when nothing was missing. */
	private String gearWarning;
	private long gearWarningAt;

	/** This login's counts and rates; outlives a hop, not a logout. */
	private final SkillingSession session = new SkillingSession();

	/** Status Bars as last read, refreshed each tick for the pillar overlay. */
	private volatile SidePillars.StatusBars statusBars = SidePillars.StatusBars.OFF;

	private final InventoryCatches inventoryCatches = new InventoryCatches();
	private final BoostTracker boosts = new BoostTracker();
	private final BoostTimer boostTimer = new BoostTimer();

	/** Boost levels are seeded from the client once, for a plugin enabled mid-session. */
	private boolean boostsSeeded;

	/** Ticks until the next boost drop, computed once per tick for the snapshot. */
	private int ticksUntilDrop = -1;

	private int lastSpecEnergy = -1;

	/** Every alert is pure state here; the plugin only feeds it and sends what it returns. */
	private final ActivityAlerts activityAlerts = new ActivityAlerts();

	/** The coin pouch item's name, shared by every pouch variant. */
	private static final String COIN_POUCH = "Coin pouch";

	/** Inventory slot counts from the last change; -1 until the inventory is seen. */
	private int inventoryFilling = -1;
	private int inventoryEmpty = -1;
	private int coinPouches;

	/** Slot class per item id: definitions never change within a session. */
	private final Map<Integer, InventoryFill.Slot> slotClasses = new HashMap<>();

	/** Your tile last tick, so walking away reads as stopping on purpose. */
	private WorldPoint lastPosition;

	private HiscoreEndpoint hiscoreEndpoint = HiscoreEndpoint.NORMAL;
	private Plugin stockPlugin;
	private Plugin statusBarsPlugin;

	/** Client thread only. Null until the plugin starts. */
	private volatile NativeHealthBarHider<SpritePixels> healthBarHider;

	/** Client thread only, the overlay included. Null until the plugin starts. */
	private volatile OverheadChatHolder<Actor> chatHolder;

	/** Actors the overlay drew a readout over in the latest frame, chat or not, published by the overlay. */
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
		overlayManager.add(skillingBadgeOverlay);
		overlayManager.add(pillarOverlay);
		overlayManager.add(nextSpotOverlay);

		knownSpots = KnownFishingSpots.load();
		final ShortestPathBridge bridge = new ShortestPathBridge(
			() -> pluginManager.getPlugins(),
			p -> pluginManager.isPluginEnabled((Plugin) p),
			ShortestPathBridge.clientThreadEventPoster(clientThread::invoke, eventBus::post));
		spotPathing = new SpotPathing(config::fishingSpotPath, router(bridge));

		stockPlugin = findPlugin(STOCK_PLUGIN_NAME);
		statusBarsPlugin = findPlugin(STATUS_BARS_NAME);

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
		overlayManager.remove(skillingBadgeOverlay);
		overlayManager.remove(pillarOverlay);
		overlayManager.remove(nextSpotOverlay);

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

		// Clears a path this sent, on the thread the pather lives on.
		final SpotPathing pathing = spotPathing;
		if (pathing != null)
		{
			clientThread.invoke(pathing::reset);
		}

		readout = null;
		selfReadout = null;
		opponent = null;
		combat.clear();
		clearSkilling();
		session.clear();
		stockPlugin = null;
		badgeBounds = null;
		statusBarsPlugin = null;
		statusBars = SidePillars.StatusBars.OFF;
	}

	private Plugin findPlugin(String name)
	{
		return pluginManager.getPlugins().stream()
			.filter(p -> name.equals(p.getName()))
			.findFirst()
			.orElse(null);
	}

	/** Status Bars as last read: whether it is on, which pillars it draws, and its width. */
	SidePillars.StatusBars statusBars()
	{
		return statusBars;
	}

	/**
	 * Reads Status Bars' own settings through the config manager, the
	 * sanctioned way to see another plugin's settings: on or off, its two bar
	 * modes (a bar set to Disabled leaves that pillar free), and its width.
	 * Unset keys are its defaults: Hitpoints, Prayer, 20.
	 */
	private void readStatusBars()
	{
		if (config.pillarMode() == PillarMode.OFF || statusBarsPlugin == null
			|| !pluginManager.isPluginEnabled(statusBarsPlugin))
		{
			statusBars = SidePillars.StatusBars.OFF;
			return;
		}

		final String left = configManager.getConfiguration(STATUS_BARS_GROUP, "leftBarMode");
		final String right = configManager.getConfiguration(STATUS_BARS_GROUP, "rightBarMode");
		final Integer width = configManager.getConfiguration(STATUS_BARS_GROUP, "barWidth", Integer.class);
		statusBars = new SidePillars.StatusBars(true, !STATUS_BARS_DISABLED.equals(left),
			!STATUS_BARS_DISABLED.equals(right), width == null ? SidePillars.WIDTH : width);
	}

	/** "Reset session" is a tick box that unticks itself once the counts are zeroed. */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CombatVitalsConfig.GROUP.equals(event.getGroup()) && config.skillingSessionReset())
		{
			session.reset();
			configManager.setConfiguration(CombatVitalsConfig.GROUP, "skillingSessionReset", false);
		}
	}

	/** Shortest Path as the pather sees it. */
	private static SpotPathing.Router router(ShortestPathBridge bridge)
	{
		return new SpotPathing.Router()
		{
			@Override
			public boolean available()
			{
				return bridge.availability() == ShortestPathBridge.Availability.AVAILABLE;
			}

			@Override
			public boolean route(NextFishingSpot.Spot spot)
			{
				final WorldPoint target = new WorldPoint(spot.getX(), spot.getY(), spot.getPlane());
				return bridge.route(target) == ShortestPathBridge.RouteOutcome.SENT;
			}

			@Override
			public void clear()
			{
				bridge.clear();
			}
		};
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
				// Logging out ends the session's counts; a hop does not.
				session.clear();
				// fall through
			case HOPPING:
				opponent = null;
				combat.clear();
				readout = null;
				selfReadout = null;
				clearSkilling();
				if (spotPathing != null)
				{
					spotPathing.reset();
				}
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
		objectSkilling.forget();
		objectBadge = false;
		objectSpot = null;
		idleBadge.clear();
		clickedNpc = null;
		clickedNpcSkill = null;
		gearWarning = null;
		inventoryCatches.clear();
		boosts.clear();
		boostTimer.clear();
		boostsSeeded = false;
		ticksUntilDrop = -1;
		lastSpecEnergy = -1;
		activityAlerts.clear();
		inventoryFilling = -1;
		inventoryEmpty = -1;
		coinPouches = 0;
		lastPosition = null;
		highlightedSpot = null;
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

			// The option clicked on this NPC decides first: Pickpocket on a Man
			// is thieving even though a Man can be fought. A hit the player
			// lands on it still proves a fight (CombatState#hit).
			final Skill clicked = target == clickedNpc ? clickedNpcSkill : null;
			clickedNpc = null;
			clickedNpcSkill = null;

			final boolean fight = clicked == null && attackable(target);
			combat.engage(fight);

			// Not a fight: perhaps a fishing spot. The badge takes the NPC only
			// when its menu names a skill; anything else drops the badge at once.
			// Either way any tree or rock is over.
			objectSkilling.forget();
			objectBadge = false;
			final Skill trained = clicked != null ? clicked : fight ? null : skillingSkill(target);
			if (trained != null)
			{
				skillingTarget = (NPC) target;
				skilling.engage(trained);
				startedFishing(trained);
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
			// An object badge follows the animation, not the interaction: the
			// end of an earlier NPC interaction must not start its linger.
			if (skillingTarget != null)
			{
				skilling.disengage(now);
			}
		}
	}

	/**
	 * Fishing a spot again ends the walk to it: the path this sent is
	 * cleared, and the spot's type and method are what the next search looks
	 * for.
	 */
	private void startedFishing(Skill trained)
	{
		if (trained == Skill.FISHING && spotPathing != null)
		{
			spotPathing.fishingOn(FishingSpot.findSpot(skillingTarget.getId()), skilling.method());
			highlightedSpot = null;
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

		activityAlerts.xp(skill, event.getXp());
		session.xp(skill, event.getXp(), System.currentTimeMillis());

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

	/**
	 * Drink or Eat on an item: remembered so a boost that follows can be
	 * named. An option on an NPC (Net, Bait, Pickpocket...): remembered so the
	 * badge knows which method the spot is worked with, and whether a Man was
	 * pickpocketed or attacked. An option on an object (Chop down, Mine...):
	 * holds the object until the swing starts. Walk here ends an object
	 * badge. The event is read, never changed.
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		final String option = event.getMenuOption();

		if (event.isItemOp())
		{
			if ("Drink".equalsIgnoreCase(option) || "Eat".equalsIgnoreCase(option))
			{
				skilling.consumed(client.getItemDefinition(event.getItemId()).getName(), client.getTickCount());
			}
			return;
		}

		clickedNpc = null;
		clickedNpcSkill = null;

		if (isNpcOption(event.getMenuAction()))
		{
			skilling.clicked(option);
			final NPC npc = event.getMenuEntry().getNpc();
			if (npc != null)
			{
				clickedNpc = npc;
				clickedNpcSkill = SkillingActions.forOption(option, Text.removeTags(event.getMenuTarget()));
				checkFishingGear(npc, option, clickedNpcSkill);
			}
			return;
		}

		if (isObjectOption(event.getMenuAction()))
		{
			clickedObject(event, option);
			return;
		}

		if (event.getMenuAction() == MenuAction.WALK)
		{
			// Walking off a tree clears its badge at once, no linger.
			endObjectBadge();
		}
	}

	/**
	 * A fishing option clicked without the tool, bait or level it needs is
	 * said on the badge at once, rather than after the game's dialog. Only
	 * read: the click goes through unchanged. Any NPC click replaces the last
	 * warning, so fetching the rod and clicking again clears it.
	 */
	private void checkFishingGear(NPC npc, String option, Skill skill)
	{
		gearWarning = null;
		if (skill != Skill.FISHING || !config.skillingGearWarning())
		{
			return;
		}

		final List<FishingCatches.Fish> fish = FishingCatches.forOption(FishingSpot.findSpot(npc.getId()), option);
		// BRUT_FISHING_S is Barbarian Training's barehanded stage. What each
		// value means is not published, so any progress counts as trained:
		// that can only hide a warning, never show a wrong one.
		final FishingGear.Kit kit = new FishingGear.Kit(carriedItemNames(),
			client.getVarbitValue(VarbitID.BRUT_FISHING_S) > 0, client.getBoostedSkillLevel(Skill.FISHING));
		gearWarning = FishingGear.text(FishingGear.missing(option, fish, kit));
		gearWarningAt = System.currentTimeMillis();
	}

	/** The names of everything in the inventory and worn, read on the click. */
	private List<String> carriedItemNames()
	{
		final List<String> names = new ArrayList<>();
		for (int containerId : new int[] {InventoryID.INV, InventoryID.WORN})
		{
			final ItemContainer container = client.getItemContainer(containerId);
			if (container == null)
			{
				continue;
			}
			for (Item item : container.getItems())
			{
				if (item.getId() > 0 && item.getQuantity() > 0)
				{
					names.add(client.getItemDefinition(item.getId()).getName());
				}
			}
		}
		return names;
	}

	/** The gear warning while it is fresh, or null. */
	private String gearWarning(long now)
	{
		return gearWarning != null && now - gearWarningAt < GEAR_WARNING_MILLIS ? gearWarning : null;
	}

	/** Drops an object badge at once, and any object waiting for its swing. */
	private void endObjectBadge()
	{
		if (objectBadge)
		{
			skilling.release();
			objectBadge = false;
		}
		objectSkilling.forget();
	}

	/**
	 * An option on a tree, rock or stall. A skilling option holds the object
	 * until the swing starts; any other option (Bank, Open) ends an object
	 * badge, the way clicking a banker ends a fishing one.
	 */
	private void clickedObject(MenuOptionClicked event, String option)
	{
		final WorldView view = client.getTopLevelWorldView();
		ObjectComposition composition = client.getObjectDefinition(event.getId());
		if (composition != null && composition.getImpostorIds() != null)
		{
			final ObjectComposition impostor = composition.getImpostor();
			if (impostor != null)
			{
				composition = impostor;
			}
		}

		final Skill skill = composition == null || view == null
			? null
			: SkillingActions.forOption(option, composition.getName());
		if (skill == null)
		{
			endObjectBadge();
			return;
		}

		// On to the next rock: the badge lingers through the walk, and the
		// new swing cancels the linger.
		if (objectBadge)
		{
			skilling.disengage(Instant.now());
		}

		// The menu gives the object's scene tile; its footprint may be rotated,
		// so take the larger side both ways and let REACH absorb the rest.
		final WorldPoint at = WorldPoint.fromScene(view, event.getParam0(), event.getParam1(), view.getPlane());
		final int size = Math.max(composition.getSizeX(), composition.getSizeY());
		objectSpot = at;
		objectSpotSize = size;
		objectSkilling.clicked(skill, at.getX(), at.getY(), at.getPlane(), size, size, client.getTickCount());
	}

	private static boolean isObjectOption(MenuAction action)
	{
		if (action == null)
		{
			return false;
		}

		switch (action)
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	private static boolean isNpcOption(MenuAction action)
	{
		if (action == null)
		{
			return false;
		}

		switch (action)
		{
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	/** "You catch some shrimps.": the fish the badge highlights. */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		final String message = Text.removeTags(event.getMessage());
		if (ActivityAlerts.isStunMessage(message))
		{
			activityAlerts.stunned();
		}
		else if (ActivityAlerts.isNecklaceBreakMessage(message))
		{
			activityAlerts.necklaceBroke();
		}

		final FishingCatches.Fish fish = FishingCatches.fromCatchMessage(message);
		if (fish != null)
		{
			skilling.caught(fish.getItemId());
			// A catch is the proof the gear was there after all.
			gearWarning = null;
		}
	}

	/**
	 * A fish landing in the inventory, for when the catch message is filtered
	 * out of the chat or the fish stacks. The inventory is read once per
	 * change, never per tick.
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			// The bank's contents arrive when it opens: you stopped on purpose.
			activityAlerts.banked();
			return;
		}

		if (event.getContainerId() != InventoryID.INV || event.getItemContainer() == null)
		{
			return;
		}

		countInventory(event.getItemContainer());

		final Item[] items = event.getItemContainer().getItems();
		final int[] ids = new int[items.length];
		final int[] quantities = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			ids[i] = items[i].getId();
			quantities[i] = items[i].getQuantity();
		}

		final int caught = inventoryCatches.observe(ids, quantities);
		if (caught != -1)
		{
			skilling.caught(caught);
		}
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

	/**
	 * Fishing spots move by despawning; the badge has nothing left to sit
	 * over. While you were still on it (interacting, or fishing without having
	 * walked off) that is the spot moving, not you leaving, and the alert says
	 * so. Logging out and hopping unload every NPC, so only while logged in.
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		// The spot being walked to moving too: the next search finds the latest one.
		if (spotPathing != null && client.getGameState() == GameState.LOGGED_IN)
		{
			spotPathing.despawned(event.getNpc());
		}

		if (event.getNpc() == skillingTarget)
		{
			if (fishingSpotLeft())
			{
				activityAlerts.spotMoved(Skill.FISHING);
				spotPathing.spotMoved();
			}

			skillingTarget = null;
			skilling.release();
		}
	}

	/**
	 * Some spots change id in place rather than despawning. Turning into
	 * anything but the same kind of spot is the spot moving.
	 */
	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		final NPC npc = event.getNpc();
		if (npc != skillingTarget || event.getOld() == null
			|| FishingSpot.findSpot(npc.getId()) == FishingSpot.findSpot(event.getOld().getId()))
		{
			return;
		}

		if (fishingSpotLeft())
		{
			spotPathing.spotMoved();
		}
	}

	/**
	 * The fishing spot under the player went while they were still on it
	 * (interacting, or fishing without having walked off): the spot moved,
	 * not the player leaving. Logging out and hopping unload every NPC, so
	 * only while logged in.
	 */
	private boolean fishingSpotLeft()
	{
		final Player self = client.getLocalPlayer();
		final boolean onIt = self != null && self.getInteracting() == skillingTarget;
		return spotPathing != null && client.getGameState() == GameState.LOGGED_IN
			&& skilling.skill() == Skill.FISHING && (onIt || activityAlerts.active() == Skill.FISHING);
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
	 * Hides the game's chat line for each actor the overlay drew a readout
	 * over last frame, and gives it back to every other. Every client tick, so
	 * turning the option off, or the readout going away, restores within a
	 * frame.
	 *
	 * The set only ever holds actors the overlay drew over: the opponent, whose
	 * readout only exists for a real fight ({@link CombatState#showTarget()}),
	 * and you, under your own bar or the skilling badge. A skilling NPC is
	 * never drawn over, so its chat is never moved or hidden.
	 */
	private void updateChatHolds()
	{
		final OverheadChatHolder<Actor> holder = chatHolder;
		if (holder == null)
		{
			return;
		}

		if (hidingChat())
		{
			holder.update(chatCovered);
		}
		else
		{
			holder.releaseAll();
		}
	}

	/**
	 * A new chat line over an actor the readout is drawn over is taken off the
	 * game here, inside the text change and before any frame is drawn, so the
	 * game's copy never flashes under the readout (Board, 2026-11-04). The
	 * per-tick {@link #updateChatHolds} still covers a message that was
	 * already up when the readout appeared.
	 */
	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		final OverheadChatHolder<Actor> holder = chatHolder;
		if (holder != null && hidingChat() && chatCovered.contains(event.getActor()))
		{
			holder.hold(event.getActor(), event.getOverheadText());
		}
	}

	private boolean hidingChat()
	{
		return config.hideGameOverheadChat() && config.showOverhead();
	}

	/**
	 * The chat line the overlay must draw over this actor because the plugin
	 * has hidden the game's. Client thread only.
	 *
	 * @return the text, or null when the game is drawing its own line
	 */
	String heldChat(Actor actor)
	{
		final OverheadChatHolder<Actor> holder = chatHolder;
		return holder == null ? null : holder.heldText(actor);
	}

	/** Called by the overlay once per frame with the actors it drew a readout over. */
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
		stepSkillingObject();
		expireSkillingTarget();
		seedBoosts();
		tickBoostTimer();
		tickAlerts();
		readStatusBars();
		tickSpotPathing();
		readout = buildReadout();
		selfReadout = buildSelfReadout();
		skillingReadout = buildSkillingReadout();
	}

	/** Runs a due next-spot search, at most once a tick, and publishes what to outline. */
	private void tickSpotPathing()
	{
		if (spotPathing == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		spotPathing.tick(this::pickSpot);
		highlightedSpot = (NPC) spotPathing.highlighted();
	}

	/**
	 * The next spot of this type: the nearest loaded one the player can catch
	 * something at, never the one being fished, else the nearest bundled
	 * location their world and level allow.
	 */
	private NextFishingSpot.Spot pickSpot(FishingSpot type, String method)
	{
		final Player self = client.getLocalPlayer();
		final WorldView view = client.getTopLevelWorldView();
		// In an instance (Tempoross, Fishing Trawler) tiles are template
		// coordinates, so a path built from them would lead somewhere else.
		if (self == null || view == null || view.isInstance())
		{
			return null;
		}

		final List<NextFishingSpot.Spot> loaded = new ArrayList<>();
		for (NPC npc : view.npcs())
		{
			final FishingSpot spot = FishingSpot.findSpot(npc.getId());
			if (spot != null)
			{
				final WorldPoint tile = npc.getWorldLocation();
				loaded.add(NextFishingSpot.Spot.loaded(spot, tile.getX(), tile.getY(), tile.getPlane(), npc));
			}
		}

		final NextFishingSpot.Need need = new NextFishingSpot.Need(type, method,
			client.getBoostedSkillLevel(Skill.FISHING), client.getWorldType().contains(WorldType.MEMBERS),
			skillingTarget);
		final WorldPoint at = self.getWorldLocation();
		return NextFishingSpot.choose(loaded, knownSpots, at.getX(), at.getY(), at.getPlane(), need);
	}

	/**
	 * Right-clicking the fishing badge offers "Path to next spot". The entry
	 * is the plugin's own, added only over the badge; no game entry is
	 * changed or removed.
	 */
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		final SkillingReadout badge = skillingReadout;
		// Fixed on screen the badge is SkillingBadgeOverlay's; otherwise the
		// overhead overlay reports where it drew it this frame.
		final Rectangle bounds = config.skillingPosition() == BadgePosition.FIXED && skillingBadgeOverlay != null
			? skillingBadgeOverlay.getBounds()
			: badgeBounds;
		if (spotPathing == null || config.fishingSpotPath() == FishingSpotPath.OFF || badge == null
			|| badge.getSkill() != Skill.FISHING || bounds == null)
		{
			return;
		}

		final net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || !bounds.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setOption("Path to next spot")
			.setTarget(ColorUtil.wrapWithColorTag("Fishing", SkillColours.of(Skill.FISHING)))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> spotPathing.requested());
	}

	/** Called by the overlay once per frame with where it drew the badge, or null. */
	void setBadgeBounds(Rectangle bounds)
	{
		badgeBounds = bounds;
	}

	/**
	 * A clicked tree or rock: the badge comes up when the player is animating
	 * beside it, lingers when the animation stops, and goes when they walk
	 * off. The player's animation and tile are read once per tick.
	 */
	private void stepSkillingObject()
	{
		final Player self = client.getLocalPlayer();
		if (self == null || (!objectSkilling.held() && !objectBadge))
		{
			return;
		}

		final WorldPoint at = self.getWorldLocation();
		// -1 is the idle animation.
		final boolean animating = self.getAnimation() != -1;
		final Instant now = Instant.now();
		switch (objectSkilling.tick(client.getTickCount(), at.getX(), at.getY(), at.getPlane(), animating))
		{
			case ENGAGE:
				skillingTarget = null;
				skilling.engage(objectSkilling.skill());
				objectBadge = true;
				break;
			case DISENGAGE:
				skilling.disengage(now);
				break;
			case RELEASE:
				skilling.release();
				objectBadge = false;
				break;
			default:
				break;
		}

		if (objectBadge && skilling.expire(now, timeout(), objectSkilling.working()))
		{
			objectBadge = false;
			objectSkilling.forget();
		}
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
			// The alerts' and session's experience baseline; a repeat of a
			// known value is no gain.
			activityAlerts.xp(skill, client.getSkillExperience(skill));
			session.xp(skill, client.getSkillExperience(skill), System.currentTimeMillis());
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
	 * takes over. The NPC decides whether the badge is up and what it can
	 * catch, and is where the badge goes only with "Over the spot".
	 *
	 * Once the work stops the badge stays as "Not fishing" ({@link IdleBadge})
	 * for as long as the idle setting says, until another activity or a fight.
	 */
	private SkillingReadout buildSkillingReadout()
	{
		final long now = System.currentTimeMillis();
		final boolean fighting = combat.inCombat(Instant.now(), timeout());
		final Skill active = skillingTarget != null || objectBadge ? skilling.skill() : null;
		idleBadge.tick(active, fighting, now);

		if (badgeHidden(fighting))
		{
			return null;
		}

		if (active != null)
		{
			return skillShown(active) ? activeReadout(active, now) : null;
		}

		final Skill idle = idleBadge.idleSkill(config.skillingIdleMode(), config.skillingIdleSeconds(), now);
		if (idle != null && skillShown(idle))
		{
			return baseReadout(idle, now).idle(true).build();
		}
		return gearOnlyReadout(now);
	}

	/**
	 * Clicking Bait without a rod usually never starts fishing at all, so
	 * with no badge up the warning brings one up of its own.
	 */
	private SkillingReadout gearOnlyReadout(long now)
	{
		return gearWarning(now) != null && skillShown(Skill.FISHING) ? baseReadout(Skill.FISHING, now).build() : null;
	}

	/** Off, a fight with a target (its health bar takes over), or any combat with Hide during combat. */
	private boolean badgeHidden(boolean fighting)
	{
		return !config.skillingEnabled() || combat.showTarget() || (fighting && config.skillingHideInCombat());
	}

	/** The badge while working: the shared rows plus the boost, spec, warning, catches and spot. */
	private SkillingReadout activeReadout(Skill skill, long now)
	{
		final int boosted = client.getBoostedSkillLevel(skill);

		int specPercent = -1;
		if (config.skillingSpecEnergy() && SkillingTools.specSkill(weaponName()) == skill)
		{
			// SA_ENERGY is in tenths of a percent.
			specPercent = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
		}

		final NPC target = skillingTarget;
		return baseReadout(skill, now)
			.ticksUntilDrop(config.skillingBoostTimer() ? ticksUntilDrop : -1)
			.cycleLength(boostTimer.cycleLength())
			.timerConfirmed(boostTimer.isConfirmed())
			.specPercent(specPercent)
			.source(config.skillingBoostSource() ? skilling.source() : null)
			.warning(skilling.warning(client.getTickCount()))
			.catches(catches(skill, target, boosted))
			.spotNpc(target)
			.spotTile(objectBadge ? objectSpot : null)
			.spotSize(objectSpotSize)
			.build();
	}

	/** What the badge shows working or idle: level, colour, and the XP, session and inventory rows. */
	private SkillingReadout.SkillingReadoutBuilder baseReadout(Skill skill, long now)
	{
		final XpProgress xp = XpProgress.of(client.getSkillExperience(skill));
		final int free = inventoryFree();
		return SkillingReadout.builder()
			.skill(skill)
			.real(client.getRealSkillLevel(skill))
			.boosted(client.getBoostedSkillLevel(skill))
			.colour(skillColour(skill))
			.gearText(skill == Skill.FISHING ? gearWarning(now) : null)
			.header(config.skillingShowHeader())
			.xp(xp)
			.xpText(config.skillingShowXp() ? xp.text(config.skillingXpDisplay()) : null)
			.sessionText(SkillingText.session(skill, session.actions(skill), session.xpPerHour(skill, now),
				session.actionsPerHour(skill, now), config.skillingSessionCount(), config.skillingRates()))
			.tickMillis(now)
			.inventoryFill(free < 0 ? -1 : InventoryFill.fraction(inventoryFilling, inventoryEmpty))
			.inventoryLabel(InventoryFill.label(free, config.skillingInventoryCount()));
	}

	/** The per-skill switches in Skilling badge. A skill without one is always shown. */
	private boolean skillShown(Skill skill)
	{
		switch (skill)
		{
			case FISHING:
				return config.skillingFishing();
			case WOODCUTTING:
				return config.skillingWoodcutting();
			case MINING:
				return config.skillingMining();
			case THIEVING:
				return config.skillingThieving();
			case RUNECRAFT:
				return config.skillingRunecraft();
			case HUNTER:
				return config.skillingHunter();
			default:
				return true;
		}
	}

	/** The user's colour for the skill, which defaults to {@link SkillColours}. */
	private Color skillColour(Skill skill)
	{
		switch (skill)
		{
			case FISHING:
				return config.skillingFishingColour();
			case WOODCUTTING:
				return config.skillingWoodcuttingColour();
			case MINING:
				return config.skillingMiningColour();
			case THIEVING:
				return config.skillingThievingColour();
			case RUNECRAFT:
				return config.skillingRunecraftColour();
			case HUNTER:
				return config.skillingHunterColour();
			default:
				return SkillColours.of(skill);
		}
	}

	/** Free slots for the badge's inventory line, or -1 when the line is off or the inventory unread. */
	private int inventoryFree()
	{
		return config.skillingInventoryBar() ? inventoryEmpty : -1;
	}

	/**
	 * Counts the inventory's slots for the fill bar and the alerts: filling,
	 * empty, and coin pouches held. Item definitions are read on the client
	 * thread, where container events arrive, and cached per id.
	 */
	private void countInventory(ItemContainer inventory)
	{
		final Item[] items = inventory.getItems();
		int filling = 0;
		int empty = 0;
		int pouches = 0;
		for (int i = 0; i < InventoryFill.SLOTS; i++)
		{
			final Item item = i < items.length ? items[i] : null;
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				empty++;
				continue;
			}

			final ItemComposition definition = client.getItemDefinition(item.getId());
			if (COIN_POUCH.equalsIgnoreCase(definition.getName()))
			{
				pouches += item.getQuantity();
			}

			final InventoryFill.Slot slot = slotClasses.computeIfAbsent(item.getId(), id ->
				InventoryFill.classify(definition.isStackable() || definition.getNote() != -1,
					definition.getName(), definition.getInventoryActions()));
			if (slot == InventoryFill.Slot.FILLING)
			{
				filling++;
			}
		}
		inventoryFilling = filling;
		inventoryEmpty = empty;
		coinPouches = pouches;
	}

	/**
	 * Feeds the tick to the activity alerts and sends what they decide. Every
	 * alert goes through {@link Notifier} with its own toggle, so the user's
	 * RuneLite notification settings (tray, sound, focus) decide how it looks.
	 */
	private void tickAlerts()
	{
		final Player self = client.getLocalPlayer();
		if (self == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (inventoryEmpty < 0)
		{
			final ItemContainer inventory = client.getItemContainer(InventoryID.INV);
			if (inventory != null)
			{
				countInventory(inventory);
			}
		}

		final WorldPoint position = self.getWorldLocation();
		final ActivityAlerts.Tick now = new ActivityAlerts.Tick();
		now.tick = client.getTickCount();
		now.animating = self.getAnimation() != -1;
		now.moved = lastPosition != null && position != null && !position.equals(lastPosition);
		now.filling = inventoryFilling;
		now.empty = inventoryEmpty;
		now.coinPouches = inventoryEmpty < 0 ? -1 : coinPouches;
		// Hundredths of a percent, as the base client's Idle Notifier reads it.
		now.runEnergy = client.getEnergy() / 100;
		lastPosition = position;

		final int pouchCap = config.alertCoinPouchCap();
		for (ActivityAlerts.Alert alert : activityAlerts.tick(now, ActivityAlerts.ticks(config.alertStopSeconds()),
			pouchCap, config.alertRunEnergyThreshold()))
		{
			notifier.notify(notification(alert.type), ActivityAlerts.message(alert, pouchCap));
		}
	}

	private Notification notification(ActivityAlerts.Type type)
	{
		switch (type)
		{
			case INVENTORY_FULL:
				return config.alertInventoryFull();
			case GATHERING_STOPPED:
				return config.alertGatheringStopped();
			case THIEVING_STOPPED:
				return config.alertThievingStopped();
			case PRODUCTION_FINISHED:
				return config.alertMakeFinished();
			case SPOT_MOVED:
				return config.alertSpotMoved();
			case STUNNED:
				return config.alertStunned();
			case COIN_POUCH_CAP:
				return config.alertCoinPouch();
			case NECKLACE_BROKE:
				return config.alertNecklaceBroke();
			case LOW_RUN_ENERGY:
			default:
				return config.alertLowRunEnergy();
		}
	}

	/**
	 * Every fish the clicked method can give, the ones the boosted level
	 * lands first, the last catch leading. The spot type comes from the base
	 * client's {@link FishingSpot} table of spot NPC ids; an unknown spot, an
	 * unknown method, or any skill but fishing gives nothing, and the badge
	 * shows no catch icons.
	 */
	private List<FishingCatches.Catch> catches(Skill skill, NPC target, int boosted)
	{
		if (!config.skillingCatches() || skill != Skill.FISHING || target == null)
		{
			return Collections.emptyList();
		}

		final FishingSpot spot = FishingSpot.findSpot(target.getId());
		final List<FishingCatches.Fish> all = FishingCatches.forOption(spot, skilling.method());
		final List<FishingCatches.Catch> options = FishingCatches.options(all, boosted, skilling.lastCatch());
		if (config.skillingLockedFish() == LockedFish.HIDE)
		{
			options.removeIf(c -> !c.isCatchable());
		}
		return options;
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
