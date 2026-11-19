package com.vinnyvangogh.combatvitals;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
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
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.FishingSpot;
import net.runelite.client.game.NPCManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.Text;

/**
 * Combat & Skilling Vitals — a standalone replacement for the stock Opponent Information
 * plugin, with overhead rendering.
 *
 * The readout is rebuilt once per game tick and published as an immutable
 * snapshot. The overlays render whatever the latest snapshot holds and compute
 * nothing themselves, because they run every frame and the underlying health
 * bar only changes on a tick.
 */
@Slf4j
@PluginDescriptor(
	// Equal to displayName in runelite-plugin.properties (PluginNameTest).
	// RuneLite keys the on/off state by the class name (PluginManager
	// isPluginEnabled), settings by the config group and the Hub install by
	// the manifest's internalName, so renaming this keeps all three. The one
	// thing keyed by this name is the plugin list's pin (PluginListPanel
	// pinnedPlugins), so anyone who pinned "Combat Vitals" pins it again.
	name = PluginName.NAME,
	// The Plugin Hub name: getPluginDirectory() is .runelite/plugin-data/combat-vitals (the Filepath API).
	internalName = "combat-vitals",
	// RuneLite moves the old .runelite/combat-vitals folder (My sounds) there on first use.
	legacyDataDirectory = "combat-vitals",
	description = "Combat: health for your target and for you. Skilling: a badge, alerts and side pillars while you skill",
	tags = {"combat", "vitals", "health", "hitpoints", "hp", "overlay", "overhead", "opponent", "pvp", "pvm",
		"skilling", "woodcutting", "fishing", "mining", "thieving", "runecraft", "hunter", "inventory", "idle", "xp",
		"herblore", "cooking", "fletching", "crafting", "smithing", "firemaking", "prayer", "magic", "agility",
		"construction", "sailing"}
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
	private SkillIconManager skillIconManager;

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
	private ChatMessageManager chatMessageManager;

	/** RuneLite's own player, the one {@link Notifier} plays through. */
	@Inject
	private AudioPlayer audioPlayer;

	/** RuneLite's notification settings, copied into a notification whose sound the plugin plays itself. */
	@Inject
	private RuneLiteConfig runeLiteConfig;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	/** Decodes and plays alert sounds off the client thread. */
	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ClientToolbar clientToolbar;

	/** RuneLite's colour picker, for the sidebar panel's colour settings, as its settings list uses it. */
	@Inject
	private ColorPickerManager colorPickerManager;

	/** The sidebar panel and its button; null while the plugin is off. Swing thread. */
	private VitalsSidePanel sidePanel;
	private NavigationButton navButton;

	/** Which alert, if any, is repeating until the player is back. */
	private final AlertRepeat alertRepeat = new AlertRepeat();

	/** The experience sync after a login, hop or reconnect: not experience drops. */
	private final LoginSync loginSync = new LoginSync();

	/** Alert sound choices and the one-time "file missing" hints. */
	private final AlertSounds alertSounds = new AlertSounds();

	/** The sidebar's mute: every alert, sound and on-screen, off. Kept here so a rebuilt panel shows it. */
	private final AlertMute alertMute = new AlertMute();

	/** The item that last landed in the inventory, for voice lines ("yew logs"). */
	private final GainedItems gainedItems = new GainedItems();
	private String lastGainedItem;

	/** The bundled voice word list; loaded at start. */
	private VoiceLines voiceLines;

	/**
	 * A mouse press in the client means the player is back. Only listens: the
	 * event is handed back unchanged and nothing is ever sent to the game.
	 */
	private final MouseAdapter backOnClick = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			alertRepeat.playerBack();
			return event;
		}
	};

	/** A key press in the client means the player is back. Only listens, never consumes. */
	private final KeyListener backOnKey = new KeyListener()
	{
		@Override
		public void keyTyped(KeyEvent event)
		{
		}

		@Override
		public void keyPressed(KeyEvent event)
		{
			alertRepeat.playerBack();
		}

		@Override
		public void keyReleased(KeyEvent event)
		{
		}
	};

	/** Whether this start found the player logged in: with no stored version, a fresh install (UpdateNotes). */
	private boolean startedLoggedIn;

	@Inject
	private NextSpotOverlay nextSpotOverlay;

	@Inject
	private AggressionOverlay aggressionOverlay;

	@Inject
	private AggressionAreaOverlay aggressionAreaOverlay;

	/** When nearby NPCs stop being aggressive: this plugin's own timer, not the base plugin's. */
	private final AggressionTimer aggression = new AggressionTimer();
	/** The gathering activity of the session, for "Auto-reset on a new activity". */
	private final SessionActivity sessionActivity = new SessionActivity();

	/** Draws back every bar "Hide game health bar" takes off the game. */
	@Inject
	private GameHealthBarOverlay gameHealthBarOverlay;

	/** Splashing mode: the Magic bonus and hit chance read-out, and its setup-changed alert. */
	@Inject
	private SplashOverlay splashOverlay;

	@Inject
	private SplashWatcher splashWatcher;

	/** Finds the next fishing spot and paths to or highlights it. Client thread only; null until started. */
	private SpotPathing spotPathing;

	/** The bundled fishing spot locations, read once at start-up. */
	private List<NextFishingSpot.Spot> knownSpots = Collections.emptyList();

	/**
	 * The next fishing spot to mark, until the player fishes again. Marked
	 * whether or not it also went to Shortest Path, which drops its own path
	 * once the player is within its finish distance (5 tiles by default).
	 */
	@Getter
	private volatile NextFishingSpot.Spot nextSpot;

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

	/** Whether the player has clearly stopped working {@link #skillingTarget}: walked off, or a fishing spot left. */
	private final SkillingStop skillingStop = new SkillingStop();

	/** The tree, rock or stall being worked, when the badge is for an object. */
	private final ObjectSkilling objectSkilling = new ObjectSkilling();

	/**
	 * The badge is up for an object rather than an NPC. Outlives the object
	 * itself: clicking the next rock keeps the badge lingering until the new
	 * swing starts, instead of flickering off for the walk.
	 */
	private boolean objectBadge;
	/** A gain in the clicked object's skill counts as working, like its animation. */
	private final RecentGain recentGain = new RecentGain();

	/** The training method the clicked object starts (the Blast Furnace pump), or null for a tree or rock. */
	private TrainingMethod objectMethod;

	/** The tick {@link #objectMethod}'s object was clicked: the game's own stop counts from it. */
	private int objectClickTick = -1;

	/** The pump's stopped, idle logout and auto stop alerts and countdowns. */
	private final TrainingSession trainingSession = new TrainingSession();

	/** The pump's world, stove and coke checks. */
	private final PumpChecks pumpChecks = new PumpChecks();

	/** The Blast Furnace stove in the scene, for how much coke it shows, or null. */
	private GameObject stove;

	/** Herblore, the Make-X skills, Firemaking, Prayer, Magic, Agility, Construction, Sailing: by XP drop. */
	private final XpSkilling xpSkilling = new XpSkilling();

	/** The badge is up for an {@link XpSkilling} skill, with no NPC or object to hold. */
	private boolean xpBadge;

	/** What one action uses up, for "14 left, done in ~25s". */
	private final ProductionInputs productionInputs = new ProductionInputs();

	private final AgilityLaps agilityLaps = new AgilityLaps();

	/** The inventory as item id to quantity: now, and as it stood at the end of the last tick. */
	private Map<Integer, Integer> inventoryNow = Collections.emptyMap();
	private Map<Integer, Integer> inventoryLastTick = Collections.emptyMap();

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
	/** Nests, Forestry, clues, pets, level-ups and randoms: news while skilling. */
	private final SkillingEvents skillingEvents = new SkillingEvents();

	/** The coin pouch item's name, shared by every pouch variant. */
	private static final String COIN_POUCH = "Coin pouch";

	/** Inventory slot counts from the last change; -1 until the inventory is seen. */
	private int inventoryFilling = -1;
	private int inventoryEmpty = -1;
	private int coinPouches;

	/** Slot class per item id: definitions never change within a session. */
	private final Map<Integer, InventoryFill.Slot> slotClasses = new HashMap<>();

	/**
	 * Log basket, fish barrel and coal bag contents, from the game's chat lines and Check dialogs;
	 * kept per account (RuneScape profile config) so a login starts from the last known count.
	 */
	private final ContainerContents containerContents = new ContainerContents();
	/** Counts as last written to the profile config; null until restored after a login. */
	private Map<SkillingContainer, Integer> containersSaved;
	/** The Check dialog text already read, so one dialog is read once. */
	private String containerDialog;

	/** Each skilling container carried or worn, with its capacity; essence pouches are filled in per read. */
	private final Map<SkillingContainer, Integer> containersCarried = new EnumMap<>(SkillingContainer.class);

	/** Every essence pouch carried. */
	private final Set<EssencePouch> pouchesCarried = EnumSet.noneOf(EssencePouch.class);

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
		overlayManager.add(aggressionOverlay);
		overlayManager.add(aggressionAreaOverlay);
		overlayManager.add(gameHealthBarOverlay);
		overlayManager.add(splashOverlay);

		knownSpots = KnownFishingSpots.load();
		voiceLines = VoiceLines.load();
		mouseManager.registerMouseListener(backOnClick);
		keyManager.registerKeyListener(backOnKey);
		final ShortestPathBridge bridge = new ShortestPathBridge(
			() -> pluginManager.getPlugins(),
			p -> pluginManager.isPluginEnabled((Plugin) p),
			ShortestPathBridge.clientThreadEventPoster(clientThread::invoke, eventBus::post));
		spotPathing = new SpotPathing(() -> VitalsHalves.fishingSpotPath(config),router(bridge, config::fishingSpotShortestPath));

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

		startSidePanel();

		// Installed or turned on mid-session: no LOGGED_IN is coming, so say it now.
		startedLoggedIn = client.getGameState() == GameState.LOGGED_IN;
		if (startedLoggedIn)
		{
			clientThread.invoke(this::postUpdateNotes);
		}
	}

	/**
	 * The sidebar panel, built from RuneLite's own description of this
	 * plugin's settings. RuneLite starts plugins on the Swing thread, where the
	 * panel is built; its config writes go to the executor.
	 */
	private void startSidePanel()
	{
		final Map<String, ConfigItemDescriptor> items = new HashMap<>();
		for (ConfigItemDescriptor d : configManager.getConfigDescriptor(config).getItems())
		{
			items.put(d.key(), d);
		}
		sidePanel = new VitalsSidePanel(ConfigStore.of(configManager, executor), items, colorPickerManager,
			type -> clientThread.invoke(() -> testAlert(type)), alertMute);
		navButton = NavigationButton.builder()
			.tooltip(VitalsSidePanel.TITLE)
			.icon(VitalsSidePanel.icon())
			.priority(7)
			.panel(sidePanel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	/**
	 * The panel's Test button: the alert as it is set up now, sound and all,
	 * even while it is switched off. Never repeats. Client thread.
	 */
	private void testAlert(ActivityAlerts.Type type)
	{
		final AlertSettings settings = AlertSettings.of(config, type);
		final ActivityAlerts.Alert alert = new ActivityAlerts.Alert(type, null);
		if (alertMute.muted())
		{
			return;
		}
		final AlertSounds.Plan plan = soundPlan(settings, alert);
		if (plan.runelite)
		{
			notifier.notify(muted(settings.notification.withEnabled(true)),
				"Test: " + ActivityAlerts.message(alert, config.alertCoinPouchCap()));
		}
		play(plan, settings.volume);
	}

	/**
	 * Posts the "what's new" lines once per version (UpdateNotes), and stores the
	 * version as seen even with the setting off, so turning it on later does not
	 * bring back notes for an update long past. Client thread.
	 */
	private void postUpdateNotes()
	{
		final String lastSeen = config.lastSeenVersion();
		if (!UpdateNotes.needsRecord(lastSeen))
		{
			return;
		}
		configManager.setConfiguration(CombatVitalsConfig.GROUP, CombatVitalsConfig.LAST_SEEN_VERSION, UpdateNotes.VERSION);
		final List<String> lines = UpdateNotes.toShow(lastSeen, config.showUpdateNotes(), startedLoggedIn);
		for (int i = 0; i < lines.size(); i++)
		{
			consoleLine(i == 0 ? ChatColorType.HIGHLIGHT : ChatColorType.NORMAL, lines.get(i));
		}
	}

	/** One line in the game chat's console, local only: nothing is typed or sent. */
	private void consoleLine(ChatColorType colour, String text)
	{
		final String message = new ChatMessageBuilder()
			.append(colour)
			.append(text)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(panelOverlay);
		overlayManager.remove(overheadOverlay);
		overlayManager.remove(skillingBadgeOverlay);
		overlayManager.remove(pillarOverlay);
		overlayManager.remove(nextSpotOverlay);
		overlayManager.remove(aggressionOverlay);
		overlayManager.remove(aggressionAreaOverlay);
		overlayManager.remove(gameHealthBarOverlay);
		overlayManager.remove(splashOverlay);
		splashWatcher.reset();
		mouseManager.unregisterMouseListener(backOnClick);
		keyManager.unregisterKeyListener(backOnKey);
		alertRepeat.stop();
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		sidePanel = null;

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
		if (VitalsHalves.pillarMode(config) == PillarMode.OFF || statusBarsPlugin == null
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

	/**
	 * "Reset session" is a tick box that unticks itself once the counts are
	 * zeroed. Any change, made in RuneLite's list or the sidebar panel,
	 * redraws the panel on the Swing thread, so the two always agree.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CombatVitalsConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (config.skillingSessionReset())
		{
			session.reset();
			agilityLaps.clear();
			configManager.setConfiguration(CombatVitalsConfig.GROUP, "skillingSessionReset", false);
		}
		final VitalsSidePanel panel = sidePanel;
		if (panel == null)
		{
			return;
		}
		if (VitalsSidePanel.GUIDES_CLOSED_KEY.equals(event.getKey())
			|| VitalsSidePanel.GUIDES_HIDDEN_KEY.equals(event.getKey()))
		{
			// Folding a guide redraws the guides only, not every input in the panel.
			final boolean hidden = config.sidebarGuidesHidden();
			final String closed = config.sidebarGuidesClosed();
			SwingUtilities.invokeLater(() -> panel.guides(hidden, closed));
			return;
		}
		SwingUtilities.invokeLater(panel::refresh);
	}

	/** Shortest Path as the pather sees it: unavailable while the user has it switched off here. */
	private static SpotPathing.Router router(ShortestPathBridge bridge, BooleanSupplier wanted)
	{
		return new SpotPathing.Router()
		{
			@Override
			public boolean available()
			{
				return wanted.getAsBoolean() && bridge.availability() == ShortestPathBridge.Availability.AVAILABLE;
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
				postUpdateNotes();
				break;
			case LOGGING_IN:
				loginSync.loggingIn();
				break;
			case CONNECTION_LOST:
				reconnecting();
				break;
			case LOGIN_SCREEN:
				// Logging out ends the session's counts; a hop does not.
				session.clear();
				sessionActivity.clear();
				// No more ticks come to redraw the Live section, so it says so now.
				publishLive();
				// Another account may log in next: its own counts are restored from its profile.
				containerContents.clear();
				containersSaved = null;
				containerDialog = null;
				containersCarried.clear();
				pouchesCarried.clear();
				// fall through
			case HOPPING:
				loginSync.loggingIn();
				aggression.clear();
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
			case LOADING:
				// Every object in view despawns and spawns again: not new Forestry events.
				skillingEvents.sceneLoading();
				break;
			default:
				break;
		}
	}

	/**
	 * The connection dropped. The client either reconnects, which to the
	 * player is logging in again, or goes to the login screen, which clears
	 * everything. Either way nothing started before it may say "stopped"
	 * after it (Board, 2026-11-14): the badge stays, the alerts need the
	 * activity seen again, and the experience that comes back is a sync.
	 */
	private void reconnecting()
	{
		loginSync.loggingIn();
		boostsSeeded = false;
		trainingSession.reconnected();
		pumpChecks.reconnected();
		activityAlerts.reconnected();
		alertRepeat.stop();
	}

	private void clearSkilling()
	{
		skillingTarget = null;
		skillingReadout = null;
		skilling.clear();
		skillingStop.reset();
		objectSkilling.forget();
		objectBadge = false;
		objectSpot = null;
		objectMethod = null;
		objectClickTick = -1;
		trainingSession.clear();
		recentGain.clear();
		pumpChecks.clear();
		xpSkilling.clear();
		xpBadge = false;
		productionInputs.forget();
		agilityLaps.clear();
		inventoryNow = Collections.emptyMap();
		inventoryLastTick = Collections.emptyMap();
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
		skillingEvents.clear();
		alertRepeat.stop();
		gainedItems.clear();
		lastGainedItem = null;
		inventoryFilling = -1;
		inventoryEmpty = -1;
		coinPouches = 0;
		lastPosition = null;
		nextSpot = null;
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		randomEventTurnedTo(event.getSource(), event.getTarget());
		npcTurnedOnPlayer(event.getSource(), event.getTarget());

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
			// A fight or an NPC is not the pump stopping by itself: no alert.
			trainingSession.end();
			// Turning to anything else (a banker, an attacker) is leaving the spot.
			turnedTo(target);
			turnedToNpc(target, clicked != null ? clicked : fight ? null : skillingSkill(target), fight);
		}
		else
		{
			interactionEnded();
		}
	}

	/**
	 * A random event NPC turning to the local player, and not one the player
	 * is already talking to: the base Random Events plugin's own condition.
	 * Another player's random event never targets you, so never counts.
	 */
	private void randomEventTurnedTo(Actor source, Actor target)
	{
		final Player self = client.getLocalPlayer();
		if (self != null && target == self && source instanceof NPC && self.getInteracting() != source
			&& SkillingEvents.isRandomEventNpc(((NPC) source).getId()))
		{
			skillingEvents.randomEvent(source.getName(), client.getTickCount());
		}
	}

	/**
	 * An attackable NPC turned on the player who is not fighting it: it is
	 * aggressive, so the tolerance timer starts (unverified rule, see
	 * {@link AggressionTimer}). Reads only what the client reports; nothing is sent.
	 */
	private void npcTurnedOnPlayer(Actor source, Actor target)
	{
		final Player self = client.getLocalPlayer();
		if (self == null || target != self || !(source instanceof NPC) || self.getInteracting() == source
			|| ((NPC) source).getCombatLevel() <= 0 || SkillingEvents.isRandomEventNpc(((NPC) source).getId()))
		{
			return;
		}
		final WorldPoint at = self.getWorldLocation();
		if (at != null)
		{
			aggression.aggressed(at.getX(), at.getY(), at.getPlane(), client.getWorld(), client.getTickCount());
		}
	}

	private void tickAggression()
	{
		final Player self = client.getLocalPlayer();
		final WorldPoint at = self == null ? null : self.getWorldLocation();
		if (at != null)
		{
			aggression.tick(at.getX(), at.getY(), at.getPlane(), client.getWorld());
		}
	}

	/** The countdown text, or null while the timer is idle. */
	String aggressionText()
	{
		return aggression.text(client.getTickCount());
	}

	boolean aggressionCalm()
	{
		return aggression.state(client.getTickCount()) == AggressionTimer.State.CALM;
	}

	/** The timer while it runs (for the area lines), else null. */
	AggressionTimer aggressionArea()
	{
		return aggression.state(client.getTickCount()) == AggressionTimer.State.RUNNING ? aggression : null;
	}

	/** A finished skilling event's line in the chat, when Forestry event alerts are on. */
	private void tickSkillingEventSummaries()
	{
		for (String summary : skillingEvents.takeSummaries())
		{
			if (AlertSettings.of(config, ActivityAlerts.Type.FORESTRY_EVENT).enabled())
			{
				consoleLine(ChatColorType.NORMAL, "Forestry event over - " + summary);
			}
		}
	}

	/**
	 * A gathering chat line: when it is a different activity from the session's
	 * and "Auto-reset on a new activity" is on, the session starts over and the
	 * finished one's summary goes to the chat.
	 */
	private void observeActivity(String message)
	{
		final Skill skill = SessionActivity.skillOf(message);
		if (skill == null || !VitalsHalves.skilling(config))
		{
			return;
		}
		final String before = sessionSummary(sessionActivity.skill(), sessionActivity.resource());
		if (sessionActivity.observe(skill, SessionActivity.resourceOf(message), config.autoResetSession()))
		{
			sessionActivity.keepSummary(before, System.currentTimeMillis());
			session.reset();
			agilityLaps.clear();
			if (before != null)
			{
				consoleLine(ChatColorType.NORMAL, "New activity, session reset. Before: " + before);
			}
		}
	}

	/** "yew logs: 120 gathered, 40000 xp" for the session so far, or null with no gains. */
	private String sessionSummary(Skill skill, String resource)
	{
		if (skill == null || session.actions(skill) == 0)
		{
			return null;
		}
		return (resource == null ? skill.getName() : resource) + ": " + session.actions(skill) + " gathered, "
			+ session.xpGained(skill) + " xp";
	}

	/**
	 * The badge takes an NPC only when its menu names a skill; anything else
	 * drops it at once, except that talking to the butler or a banker mid-build
	 * is not stopping: an XP badge runs out on its own gap.
	 */
	private void turnedToNpc(Actor target, Skill trained, boolean fight)
	{
		if (trained != null)
		{
			endXpBadge();
			skillingTarget = (NPC) target;
			skilling.engage(trained);
			skillingStop.reset();
			startedFishing(trained);
		}
		else if (!xpBadge || fight)
		{
			endXpBadge();
			skillingTarget = null;
			skilling.release();
		}
	}

	/** The player stopped interacting with anything. */
	private void interactionEnded()
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
		if (spotPathing != null)
		{
			spotPathing.interactionEnded();
		}
	}

	/** The player clicked or turned to this NPC, or to a tile or object when null. */
	private void turnedTo(Object npc)
	{
		if (spotPathing != null)
		{
			spotPathing.turnedTo(npc);
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
			spotPathing.fishingOn(FishingSpot.findSpot(skillingTarget.getId()), skilling.method(), skillingTarget);
			nextSpot = null;
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

		// The login sync is nobody's experience drop: seedBoosts takes the
		// baselines from the client once it is over.
		if (!loginSync.syncing())
		{
			feedXp(skill, event.getXp(), real, tick);
		}

		final BoostTracker.Change change = boosts.observe(skill, real, boosted);
		if ((change == BoostTracker.Change.DROP || change == BoostTracker.Change.EXPIRED)
			&& BoostTimer.onSkillingTimer(skill))
		{
			boostTimer.anchorDrop(tick);
		}

		final SkillingState.Warning warning = skilling.boostChanged(skill, change, tick);
		if (warning != SkillingState.Warning.NONE && VitalsHalves.skilling(config) && !alertMute.muted())
		{
			final String message = warning == SkillingState.Warning.EXPIRED
				? skill.getName() + " boost has worn off"
				: skill.getName() + " boost dropped to +" + (boosted - real);
			notifier.notify(config.skillingBoostNotify(), message);
		}
	}

	/** One skill's experience and real level to every tracker that reads them. The first value is a baseline. */
	private void feedXp(Skill skill, int xp, int level, int tick)
	{
		activityAlerts.xp(skill, xp);
		skillingEvents.stat(skill, xp, level, tick);
		session.xp(skill, xp, System.currentTimeMillis());
		xpSkilling.xp(skill, xp);
		trainingSession.xp(skill, xp);
		recentGain.xp(skill, xp, tick);
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
		splashWatcher.onMenuOptionClicked(event);
		final String option = event.getMenuOption();

		// Fill, Empty or Empty to bank on a basket, barrel or coal bag, in the
		// inventory or the bank; read only, the click goes through untouched.
		final SkillingContainer clickedContainer =
			SkillingContainer.fromItemName(Text.removeTags(event.getMenuTarget()));
		if (clickedContainer != null)
		{
			containerContents.clicked(clickedContainer, option, client.getTickCount(), bankOpen());
		}

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
			turnedTo(npc);
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
			turnedTo(null);
			clickedObject(event, option);
			return;
		}

		if (event.getMenuAction() == MenuAction.WALK)
		{
			// Walking off a tree clears its badge at once, no linger; walking
			// off a fishing spot means it going later is not it moving.
			turnedTo(null);
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
		trainingSession.end();
	}

	/**
	 * An option on a tree, rock or stall. A skilling option holds the object
	 * until the swing starts; any other option (Bank, Open) ends an object
	 * badge, the way clicking a banker ends a fishing one.
	 */
	private void clickedObject(MenuOptionClicked event, String option)
	{
		final WorldView view = client.getTopLevelWorldView();
		final ObjectComposition composition = objectDefinition(event.getId());
		if (view == null || composition == null)
		{
			endObjectBadge();
			return;
		}

		// A training method (the Blast Furnace pump) by the clicked id; else a skilling option by name.
		final TrainingMethod method = TrainingMethod.forObject(event.getId(), option);
		final Skill skill = method != null ? method.skill() : SkillingActions.forOption(option, composition.getName());
		if (skill == null)
		{
			endObjectBadge();
			return;
		}
		clickedMethod(method);

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

	/** An object's definition, its impostor when it has one in force, or null. */
	private ObjectComposition objectDefinition(int id)
	{
		final ObjectComposition composition = client.getObjectDefinition(id);
		if (composition == null || composition.getImpostorIds() == null)
		{
			return composition;
		}
		final ObjectComposition impostor = composition.getImpostor();
		return impostor != null ? impostor : composition;
	}

	/**
	 * A skilling object clicked: remembered for the training session. The
	 * pump clicked again keeps its session, and the game's stop restarts
	 * from this click; anything else ends it.
	 */
	private void clickedMethod(TrainingMethod method)
	{
		if (method == null || trainingSession.method() != method)
		{
			trainingSession.end();
		}
		objectMethod = method;
		objectClickTick = client.getTickCount();
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
		if (event.getType() == ChatMessageType.MESBOX)
		{
			// The base Woodcutting plugin reads the nest line from MESBOX too.
			skillingEvents.chat(Text.removeTags(event.getMessage()));
			return;
		}
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
		skillingEvents.chat(message);
		observeActivity(message);

		containerContents.message(message, client.getTickCount());
		agilityLaps.message(message, System.currentTimeMillis());

		final FishingCatches.Fish fish = FishingCatches.fromCatchMessage(message);
		if (fish != null)
		{
			skilling.caught(fish.getItemId());
			// A catch is the proof the gear was there after all.
			gearWarning = null;
			// Minnows and karambwanji stack in the inventory; the barrel takes neither.
			if (!client.getItemDefinition(fish.getItemId()).isStackable())
			{
				containerContents.fishCaught(client.getTickCount());
			}
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
			productionInputs.rebase();
			// And change when a basket or barrel is emptied into it, with no chat line needed.
			containerContents.bankChanged(client.getTickCount());
			return;
		}

		if (event.getContainerId() == InventoryID.WORN)
		{
			// A worn forestry basket or fish sack barrel, or a Smithing cape for the coal bag.
			scanContainers();
			return;
		}

		if (event.getContainerId() != InventoryID.INV || event.getItemContainer() == null)
		{
			return;
		}

		final int fillingBefore = inventoryFilling;
		countInventory(event.getItemContainer());
		scanContainers();
		containerContents.inventoryChanged(fillingBefore, inventoryFilling, client.getTickCount());

		final Item[] items = event.getItemContainer().getItems();
		final int[] ids = new int[items.length];
		final int[] quantities = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			ids[i] = items[i].getId();
			quantities[i] = items[i].getQuantity();
		}
		inventoryNow = byId(ids, quantities);
		agilityLaps.marksHeld(inventoryNow.getOrDefault(ItemID.GRACE, 0));

		final int caught = inventoryCatches.observe(ids, quantities);
		if (caught != -1)
		{
			skilling.caught(caught);
		}

		noteGainedItems(ids, quantities);
	}

	/** The item that last landed, for voice lines, and any clue among everything that did. */
	private void noteGainedItems(int[] ids, int[] quantities)
	{
		final int gained = gainedItems.observe(ids, quantities);
		if (gained == -1)
		{
			return;
		}
		lastGainedItem = client.getItemDefinition(gained).getName();
		// A clue geode can land on the same tick as the ore: every item gained is checked.
		final boolean skillingNow = (activityAlerts.active() != null || skilling.skill() != null) && !bankOpen();
		for (int id : gainedItems.gained())
		{
			skillingEvents.inventoryGained(client.getItemDefinition(id).getName(), skillingNow);
		}
	}

	/** A clue nest on the ground, for the bird nest line that follows it. */
	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		skillingEvents.groundItem(client.getItemDefinition(event.getItem().getId()).getName(), client.getTickCount());
	}

	/**
	 * Rising Roots and Struggling Sapling are objects; the other Forestry
	 * events are NPCs. The Blast Furnace stove is kept for how much coke it
	 * shows ({@link PumpChecks}).
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (PumpChecks.isStove(event.getGameObject().getId()))
		{
			stove = event.getGameObject();
		}
		final SkillingEvents.Forestry kind = SkillingEvents.forestryObject(event.getGameObject().getId());
		if (kind != null)
		{
			skillingEvents.forestrySpawned(kind, event.getGameObject(), near(event.getGameObject().getWorldLocation()),
				client.getTickCount());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (event.getGameObject() == stove)
		{
			stove = null;
		}
		final SkillingEvents.Forestry kind = SkillingEvents.forestryObject(event.getGameObject().getId());
		if (kind != null)
		{
			skillingEvents.forestryDespawned(kind, event.getGameObject(), client.getTickCount());
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final SkillingEvents.Forestry kind = SkillingEvents.forestryNpc(event.getNpc().getId());
		if (kind != null)
		{
			skillingEvents.forestrySpawned(kind, event.getNpc(), near(event.getNpc().getWorldLocation()),
				client.getTickCount());
		}
	}

	/** Within {@link SkillingEvents#FORESTRY_RANGE} tiles of the local player. */
	private boolean near(WorldPoint point)
	{
		final Player self = client.getLocalPlayer();
		if (self == null || point == null)
		{
			return false;
		}
		final int distance = self.getWorldLocation().distanceTo(point);
		return distance <= SkillingEvents.FORESTRY_RANGE;
	}

	/** The inventory as item id to total quantity, empty slots left out. */
	static Map<Integer, Integer> byId(int[] ids, int[] quantities)
	{
		final Map<Integer, Integer> byId = new HashMap<>();
		for (int i = 0; i < ids.length; i++)
		{
			if (ids[i] >= 0 && quantities[i] > 0)
			{
				byId.merge(ids[i], quantities[i], Integer::sum);
			}
		}
		return byId;
	}

	/** The bank is open: its inventory side panel, where a basket's Empty is clicked, is up. */
	private boolean bankOpen()
	{
		final Widget side = client.getWidget(InterfaceID.Bankside.ITEMS);
		return side != null && !side.isHidden();
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
	 * A fishing spot that despawns leaves the badge nothing to sit over (most
	 * spots relocate as the same NPC instead, which {@link SpotPathing} reads
	 * from the tile each tick). While you were still on it (interacting, or fishing without having
	 * walked off) that is the spot moving, not you leaving, and the alert says
	 * so. Logging out and hopping unload every NPC, so only while logged in.
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		// The pather decides from its own state whether this was the fished
		// spot moving, or the spot being walked to moving: the player's
		// interaction is cleared around this tick and cannot say.
		if (spotPathing != null && client.getGameState() == GameState.LOGGED_IN)
		{
			spotPathing.despawned(event.getNpc());
		}

		final SkillingEvents.Forestry kind = SkillingEvents.forestryNpc(event.getNpc().getId());
		if (kind != null)
		{
			skillingEvents.forestryDespawned(kind, event.getNpc(), client.getTickCount());
		}

		// A kill: the NPC is leaving the scene, and frames drawn before the
		// next tick must not ask the client where it is on screen (the Board's
		// in-game test, 2026-11-04: OverlayRenderer error from
		// getCanvasTextLocation while the dead NPC was being removed).
		if (event.getNpc() == opponent)
		{
			opponent = null;
			readout = null;
			// The fight is over too: without this it stayed engaged with no opponent
			// to time out against, and the skilling badge never came back (Board,
			// 2026-11-14: still hidden 20 s into woodcutting after a kill).
			combat.disengage(Instant.now());
		}

		if (event.getNpc() == skillingTarget)
		{
			if (fishingSpotLeft())
			{
				activityAlerts.spotMoved(Skill.FISHING);
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
		if (spotPathing == null || event.getOld() == null
			|| FishingSpot.findSpot(npc.getId()) == FishingSpot.findSpot(event.getOld().getId()))
		{
			return;
		}

		spotPathing.changed(npc);
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

		// A failed pickpocket stuns and hits you; that is the price of thieving,
		// not a fight, and must not hide the badge (Board's in-game test,
		// 2026-11-06). Anyone else on you is a fight: a stall's guard, a player.
		// The scene is only searched for a hit on you while thieving.
		final boolean thieving = onMe && thieving();
		if (!ThievingHits.isFight(onMe, thieving, thieving && attackedByAnotherThan(self, skillingTarget)))
		{
			return;
		}

		final boolean onTarget = !onMe && actor == opponent && event.getHitsplat().isMine();
		combat.hit(onTarget, Instant.now());
	}

	/** Pickpocketing an NPC or stealing from a stall, working or lingering. */
	private boolean thieving()
	{
		return skilling.skill() == Skill.THIEVING && (skillingTarget != null || objectBadge);
	}

	/**
	 * Pickpocketing or knocking out an NPC, which can stun and hit you, so your
	 * health shows over the badge for as long as you are at it. A stall cannot
	 * hit you; its guards can, and they are a fight.
	 */
	private boolean thievingFromNpc()
	{
		return skillingTarget != null && skilling.skill() == Skill.THIEVING;
	}

	/**
	 * Whether anything but {@code except} that can fight is interacting with
	 * you: an attackable NPC, or another player ({@link ThievingHits#otherAttacker}).
	 */
	private boolean attackedByAnotherThan(Player self, NPC except)
	{
		final WorldView view = client.getTopLevelWorldView();
		if (view == null)
		{
			// Unknown: count it as a fight, as before.
			return true;
		}
		return ThievingHits.<Actor>otherAttacker(view.npcs(), self, except, CombatVitalsPlugin::attackable,
			Actor::getInteracting)
			|| ThievingHits.<Actor>otherAttacker(view.players(), self, except, CombatVitalsPlugin::attackable,
			Actor::getInteracting);
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

		if (VitalsHalves.hideGameHealthBar(config))
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
	 * read yet; {@link NativeHealthBarHider} retries about once a second.
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
		tickAggression();
		tickSkillingEventSummaries();
		loginSync.tick();
		expireOpponent();
		stepSkillingObject();
		expireSkillingTarget();
		stepXpSkilling();
		seedBoosts();
		tickBoostTimer();
		tickAlerts();
		final String splashBroke = splashWatcher.tick();
		if (splashBroke != null && !alertMute.muted())
		{
			notifier.notify(muted(config.splashAlertBroke()), splashBroke);
		}
		readStatusBars();
		tickSpotPathing();
		readContainerDialog();
		keepContainerCounts();
		// A half switched off builds nothing, so none of its overlays or pillars draws.
		readout = VitalsHalves.combat(config) ? buildReadout() : null;
		selfReadout = VitalsHalves.combat(config) ? buildSelfReadout() : null;
		skillingReadout = VitalsHalves.skilling(config) ? buildSkillingReadout() : null;
		publishLive();
	}

	/**
	 * The sidebar's Live section, from the readouts just built: only while the
	 * panel is open in the sidebar and the section unfolded, so a closed panel
	 * costs nothing. Built here on the client thread, drawn on the Swing thread.
	 */
	private void publishLive()
	{
		final VitalsSidePanel panel = sidePanel;
		if (panel == null || !config.sidebarLiveOpen() || !panel.wantsLive())
		{
			return;
		}
		final LiveVitals vitals = buildLive();
		SwingUtilities.invokeLater(() -> panel.live(vitals));
	}

	private LiveVitals buildLive()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return LiveVitals.LOGGED_OUT;
		}
		final long now = System.currentTimeMillis();
		return new LiveVitals(true,
			VitalsHalves.combat(config) ? liveCombat(now) : null,
			VitalsHalves.skilling(config) ? liveSkilling() : null,
			LiveVitals.session(session, now, this::skillColour));
	}

	/** The badge's readout for the Skilling card, with the skill's small icon for its title. */
	private LiveVitals.Skilling liveSkilling()
	{
		final SkillingReadout r = skillingReadout;
		return LiveVitals.skilling(r, r == null ? null : skillIconManager.getSkillImage(r.getSkill(), true));
	}

	/** Your vitals, the target as the overhead readout has it, and the combat skill gained in last. */
	private LiveVitals.Combat liveCombat(long now)
	{
		final Readout target = readout;
		final Skill skill = session.latest(LiveVitals.COMBAT_SKILLS);
		final XpProgress xp = skill == null ? null : XpProgress.of(client.getSkillExperience(skill));
		return new LiveVitals.Combat(
			LiveVitals.level("Hitpoints", client.getBoostedSkillLevel(Skill.HITPOINTS),
				client.getRealSkillLevel(Skill.HITPOINTS), LiveVitals.HITPOINTS),
			LiveVitals.level("Prayer", client.getBoostedSkillLevel(Skill.PRAYER),
				client.getRealSkillLevel(Skill.PRAYER), LiveVitals.PRAYER),
			LiveVitals.energy(client.getEnergy(), client.getVarpValue(VarPlayerID.SA_ENERGY)),
			target == null ? null : target.getName(),
			LiveVitals.target(target, config.percentageDecimals()),
			skill == null ? null : LiveVitals.training(skill, xp, skillColour(skill)),
			skill == null ? null : LiveVitals.trainingRate(xp, session.xpPerHour(skill, now)));
	}

	/**
	 * A container's Check answers in the item dialog (Objectbox), not in chat: "The basket contains:<br>14 x Yew
	 * logs". Read once per dialog, with line breaks as spaces.
	 */
	private void readContainerDialog()
	{
		final Widget text = client.getWidget(InterfaceID.Objectbox.TEXT);
		if (text == null || text.isHidden() || text.getText() == null)
		{
			containerDialog = null;
			return;
		}
		final String message = Text.removeTags(text.getText().replace("<br>", " ")).trim();
		if (!message.equals(containerDialog))
		{
			containerDialog = message;
			containerContents.message(message, client.getTickCount());
		}
	}

	/**
	 * Restores this account's container counts once its profile is known after a login, then writes any count
	 * that changed, so the next login starts from it instead of "?".
	 */
	private void keepContainerCounts()
	{
		if (client.getGameState() != GameState.LOGGED_IN || configManager.getRSProfileKey() == null)
		{
			return;
		}
		if (containersSaved == null)
		{
			containersSaved = new EnumMap<>(SkillingContainer.class);
			for (SkillingContainer container : SkillingContainer.values())
			{
				final Integer count = configManager.getRSProfileConfiguration(CombatVitalsConfig.GROUP,
					containerKey(container), Integer.class);
				if (count != null)
				{
					containerContents.restore(container, count);
					containersSaved.put(container, count);
				}
			}
		}
		for (Map.Entry<SkillingContainer, Integer> entry : containerContents.known().entrySet())
		{
			if (!entry.getValue().equals(containersSaved.get(entry.getKey())))
			{
				configManager.setRSProfileConfiguration(CombatVitalsConfig.GROUP, containerKey(entry.getKey()),
					entry.getValue());
				containersSaved.put(entry.getKey(), entry.getValue());
			}
		}
	}

	/** The profile config key a container's count is kept under. */
	static String containerKey(SkillingContainer container)
	{
		return "containerCount." + container.name();
	}

	/** Runs a due next-spot search, at most once a tick, and publishes what to mark. */
	private void tickSpotPathing()
	{
		if (spotPathing == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		spotPathing.tick(this::pickSpot, CombatVitalsPlugin::spotTile);
		nextSpot = VitalsHalves.fishingSpotPath(config) == FishingSpotPath.OFF ? null : spotPathing.target();
	}

	/** Where a spot NPC stands: spots move by relocating, the same NPC on a new tile. */
	private static long spotTile(Object npc)
	{
		final WorldPoint tile = ((NPC) npc).getWorldLocation();
		return tile == null ? SpotPathing.UNKNOWN : SpotPathing.tile(tile.getX(), tile.getY(), tile.getPlane());
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
			log.debug("No next fishing spot: {}", self == null || view == null ? "no player or world view" : "in an instance");
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
		if (spotPathing == null || VitalsHalves.fishingSpotPath(config) == FishingSpotPath.OFF || badge == null
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
		// -1 is the idle animation. A drop in the object's skill counts too: spam-clicking the pump cuts the
		// animation short while Strength keeps coming in.
		final boolean animating = self.getAnimation() != -1
			|| recentGain.gained(objectSkilling.skill(), client.getTickCount());
		final Instant now = Instant.now();
		switch (objectSkilling.tick(client.getTickCount(), at.getX(), at.getY(), at.getPlane(), animating))
		{
			case ENGAGE:
				endXpBadge();
				skillingTarget = null;
				skilling.engage(objectSkilling.skill());
				objectBadge = true;
				startTraining();
				break;
			case DISENGAGE:
				skilling.disengage(now);
				break;
			case RELEASE:
				skilling.release();
				objectBadge = false;
				trainingSession.end();
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

	/** The object badge came up: for a training method its session starts, for a tree or rock any session ends. */
	private void startTraining()
	{
		if (objectMethod == null)
		{
			trainingSession.end();
		}
		else
		{
			trainingSession.start(objectMethod, objectClickTick, client.getTickCount());
		}
	}

	/**
	 * The badge for a skill with no NPC or object ({@link XpSkilling}): up on
	 * an experience drop, held by further drops or any animation, lingering
	 * like the others once the skill's gap passes. Never over another badge
	 * or during a fight. Each drop is one action for the inputs-left count.
	 */
	private void stepXpSkilling()
	{
		final Player self = client.getLocalPlayer();
		final boolean animating = self != null && self.getAnimation() != -1;
		final Instant now = Instant.now();
		final boolean blocked = skillingTarget != null || objectBadge || combat.inCombat(now, timeout());
		final int tick = client.getTickCount();
		applyXpStep(xpSkilling.tick(tick, animating, blocked), now);

		if (xpSkilling.acted())
		{
			productionInputs.action(xpSkilling.skill(), inventoryLastTick, inventoryNow, tick);
		}
		if (xpBadge && skilling.expire(now, timeout(), xpSkilling.working()))
		{
			xpBadge = false;
			xpSkilling.forget();
		}
		inventoryLastTick = inventoryNow;
	}

	private void applyXpStep(XpSkilling.Step step, Instant now)
	{
		switch (step)
		{
			case ENGAGE:
				skilling.engage(xpSkilling.skill());
				xpBadge = true;
				break;
			case DISENGAGE:
				skilling.disengage(now);
				break;
			case RELEASE:
				if (xpBadge)
				{
					skilling.release();
					xpBadge = false;
				}
				break;
			default:
				break;
		}
	}

	/** A skilling NPC, object or XP-drop skill holds the badge up. */
	private boolean badgeHeld()
	{
		return skillingTarget != null || objectBadge || xpBadge;
	}

	/** An NPC or object badge takes over from the XP badge. */
	private void endXpBadge()
	{
		xpBadge = false;
		xpSkilling.forget();
	}

	/**
	 * Ends the NPC badge: at once when the player has clearly stopped
	 * ({@link SkillingStop}: walked off, or a fishing spot left for two
	 * ticks), so "Not fishing" shows within a tick or two; otherwise after the
	 * linger.
	 */
	private void expireSkillingTarget()
	{
		if (skillingTarget == null)
		{
			return;
		}

		final Player self = client.getLocalPlayer();
		final boolean stillOnTarget = self != null && self.getInteracting() == skillingTarget;

		if (self != null && skillingStop.tick(stillOnTarget || clickedNpc != null, playerTile(self),
			self.getAnimation() != -1, skilling.skill() == Skill.FISHING))
		{
			log.debug("Stopped {}: walked off or left the spot", skilling.skill());
			skillingTarget = null;
			skilling.release();
			return;
		}

		if (skilling.expire(Instant.now(), timeout(), stillOnTarget))
		{
			skillingTarget = null;
		}
	}

	/** The player's tile as {@link SpotPathing#tile}, or unknown. */
	private static long playerTile(Player self)
	{
		final WorldPoint at = self.getWorldLocation();
		return at == null ? SkillingStop.UNKNOWN : SpotPathing.tile(at.getX(), at.getY(), at.getPlane());
	}

	/**
	 * A plugin enabled mid-session has seen no stat updates, and a login, hop
	 * or reconnect feeds none until its sync is over ({@link LoginSync}).
	 * Seed from the client so the first real change is classified rather
	 * than swallowed.
	 */
	private void seedBoosts()
	{
		if (boostsSeeded || loginSync.syncing() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final int tick = client.getTickCount();
		for (Skill skill : Skill.values())
		{
			if (!boosts.isSeeded(skill))
			{
				boosts.seed(skill, client.getBoostedSkillLevel(skill));
			}
			// Every tracker's experience and level baseline once the login
			// sync is over; a repeat of a known value is no gain.
			feedXp(skill, client.getSkillExperience(skill), client.getRealSkillLevel(skill), tick);
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
	 * for as long as the idle setting says, until another activity, or a
	 * fight over your head. Moved off your head a fight never hides it; the
	 * header just says "in combat" or "under attack" ({@link BadgeCombat}).
	 */
	private SkillingReadout buildSkillingReadout()
	{
		final long now = System.currentTimeMillis();
		final boolean fighting = combat.inCombat(Instant.now(), timeout());
		final Skill active = badgeHeld() ? skilling.skill() : null;
		final boolean overHead = config.skillingPosition() == BadgePosition.OVER_HEAD;
		idleBadge.tick(active, BadgeCombat.endsIdle(fighting, overHead), now);

		// With Combat vitals off no target readout takes the badge's place, so a fight keeps the badge.
		if (!VitalsHalves.skillingBadge(config)
			|| BadgeCombat.hides(fighting, overHead, config.skillingHideInCombat(),
				VitalsHalves.combat(config) && combat.showTarget()))
		{
			return null;
		}

		if (active != null)
		{
			return skillShown(active) ? activeReadout(active, now, BadgeCombat.tag(fighting, false)) : null;
		}

		final Skill idle = idleBadge.idleSkill(config.skillingIdleMode(), config.skillingIdleSeconds(), now);
		if (idle != null && skillShown(idle))
		{
			return baseReadout(idle, now).idle(true).combatTag(BadgeCombat.tag(fighting, true)).build();
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

	/** The badge while working: the shared rows plus the boost, spec, warning, catches and spot. */
	private SkillingReadout activeReadout(Skill skill, long now, String combatTag)
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
			.combatTag(combatTag)
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
			.healthAbove(thievingFromNpc())
			.build();
	}

	/** What the badge shows working or idle: level, colour, and the XP, session and inventory rows. */
	private SkillingReadout.SkillingReadoutBuilder baseReadout(Skill skill, long now)
	{
		final XpProgress xp = XpProgress.of(client.getSkillExperience(skill));
		final SkillingReadout.SkillingReadoutBuilder builder = SkillingReadout.builder()
			.skill(skill)
			.real(client.getRealSkillLevel(skill))
			.boosted(client.getBoostedSkillLevel(skill))
			.colour(skillColour(skill))
			.header(config.skillingShowHeader())
			.xp(xp)
			.xpText(config.skillingShowXp() ? xp.text(config.skillingXpDisplay()) : null)
			.sessionCells(sessionCells(skill, xp, now))
			.tickMillis(now)
			.containerCount(config.skillingInventoryCount());

		final TrainingMethod method = trainingMethod(skill);
		if (method != null)
		{
			return methodRows(builder, method);
		}
		final int free = inventoryFree();
		final List<ContainerRow> containers = free < 0 || !config.skillingContainers()
			? Collections.emptyList() : containerRows(skill);
		return builder
			.gearText(skill == Skill.FISHING ? gearWarning(now) : null)
			.inventoryFill(free < 0 ? -1 : InventoryFill.fraction(inventoryFilling, inventoryEmpty))
			.inventoryLabel(inventoryLabel(skill, free, containers))
			.inventoryFree(free)
			.containers(containers);
	}

	/** The training method the badge for this skill is about, working or "Not pumping", or null. */
	private TrainingMethod trainingMethod(Skill skill)
	{
		final TrainingMethod method = trainingSession.method();
		return method != null && method.skill() == skill ? method : null;
	}

	/**
	 * A training method's badge: the same header, XP and session rows, and
	 * in place of the inventory line "Re-click in 4:12 | Stops in 97:30" on a
	 * bar filling towards the game's own stop. No line once stopped.
	 */
	private SkillingReadout.SkillingReadoutBuilder methodRows(SkillingReadout.SkillingReadoutBuilder builder,
		TrainingMethod method)
	{
		final int stopTicks = trainingSession.stopTicksLeft();
		final int autoStop = method.autoStopTicks();
		final boolean pump = method == TrainingMethod.BLAST_FURNACE_PUMP;
		return builder
			.training(method)
			.gearText(pump ? pumpChecks.warning() : null)
			.inventoryFill(stopTicks < 0 || autoStop <= 0 ? 0 : 1 - (double) stopTicks / autoStop)
			.inventoryLabel(SkillingText.methodTimers(trainingSession.reclickMillis(), trainingSession.active(),
				method.stopNoun(), stopTicks, pump ? pumpChecks.cokeText() : null));
	}

	/**
	 * The inventory line: for Agility its laps and marks, for a skill that
	 * uses things up what is left ("14 left, done in ~25s"), otherwise the
	 * free slots. Null with the line off or the inventory unread.
	 */
	private String inventoryLabel(Skill skill, int free, List<ContainerRow> containers)
	{
		if (free < 0)
		{
			return null;
		}
		final String own = skill == Skill.AGILITY ? agilityLaps.label() : productionInputs.label(skill, inventoryNow);
		return own != null ? own : ContainerRow.inventoryLabel(free, config.skillingInventoryCount(), containers,
			config.skillingContainersAsInventory());
	}

	/**
	 * The containers carried that take what the skill gathers, each with what
	 * is known of its contents: chat-tracked ones from {@link ContainerContents},
	 * essence pouches summed from their varbits.
	 */
	private List<ContainerRow> containerRows(Skill skill)
	{
		if (containersCarried.isEmpty())
		{
			return Collections.emptyList();
		}
		final Map<SkillingContainer, Integer> capacities = new EnumMap<>(containersCarried);
		int pouchHeld = 0;
		if (capacities.containsKey(SkillingContainer.ESSENCE_POUCHES))
		{
			final int runecraft = client.getRealSkillLevel(Skill.RUNECRAFT);
			int pouchCapacity = 0;
			for (EssencePouch pouch : pouchesCarried)
			{
				pouchHeld += client.getVarbitValue(pouch.getAmountVarbit());
				pouchCapacity += pouch.capacity(pouchDegradation(pouch), runecraft);
			}
			capacities.put(SkillingContainer.ESSENCE_POUCHES, pouchCapacity);
		}
		final int pouches = pouchHeld;
		final List<ContainerRow> rows = new ArrayList<>();
		for (ContainerRow row : ContainerRow.rows(skill, capacities, kind ->
			kind == SkillingContainer.ESSENCE_POUCHES ? pouches : containerContents.contents(kind),
			containerContents::isOpen))
		{
			rows.add(row.withDetail(containerContents.itemsLabel(row.getKind())));
		}
		return rows;
	}

	private int pouchDegradation(EssencePouch pouch)
	{
		if (pouch.getDegradationVar() < 0)
		{
			return 0;
		}
		return pouch.isDegradationIsVarbit() ? client.getVarbitValue(pouch.getDegradationVar())
			: client.getVarpValue(pouch.getDegradationVar());
	}

	/**
	 * Which skilling containers are carried or worn, open or closed, with
	 * their capacities. Run on inventory and equipment changes, never per tick.
	 */
	private void scanContainers()
	{
		containersCarried.clear();
		pouchesCarried.clear();
		final Set<SkillingContainer> open = EnumSet.noneOf(SkillingContainer.class);
		scanContainers(client.getItemContainer(InventoryID.INV), open);
		final boolean smithingCape = scanContainers(client.getItemContainer(InventoryID.WORN), open);
		containersCarried.replaceAll((kind, ignored) -> fixedCapacity(kind, smithingCape));
		containerContents.carried(open, containersCarried);
	}

	/** @return whether a Smithing cape is among the items, for the coal bag's 36 */
	private boolean scanContainers(ItemContainer items, Set<SkillingContainer> open)
	{
		if (items == null)
		{
			return false;
		}
		boolean smithingCape = false;
		for (Item item : items.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			final String name = client.getItemDefinition(item.getId()).getName();
			smithingCape |= name.startsWith("Smithing cape");
			final SkillingContainer kind = SkillingContainer.fromItemName(name);
			if (kind != null)
			{
				containersCarried.put(kind, 0);
				if (SkillingContainer.isOpen(name))
				{
					open.add(kind);
				}
				final EssencePouch pouch = EssencePouch.fromItemName(name);
				if (pouch != null)
				{
					pouchesCarried.add(pouch);
				}
			}
		}
		return smithingCape;
	}

	/** Capacity of a basket, barrel or coal bag; essence pouches are read with their varbits. */
	private static int fixedCapacity(SkillingContainer kind, boolean smithingCape)
	{
		switch (kind)
		{
			case LOG_BASKET:
				return SkillingContainer.BASKET_CAPACITY;
			case FISH_BARREL:
				return SkillingContainer.BARREL_CAPACITY;
			case COAL_BAG:
				return SkillingContainer.coalBagCapacity(smithingCape);
			default:
				return 0;
		}
	}

	/** The session row's cells in the chosen badge layout. */
	private List<SessionRow.Cell> sessionCells(Skill skill, XpProgress xp, long now)
	{
		if (config.skillingBadgeLayout() == BadgeLayout.COMPACT)
		{
			return SessionRow.compactCells(skill, session.actions(skill), session.xpPerHour(skill, now),
				config.skillingSessionCount(), config.skillingRates());
		}
		return SessionRow.cells(skill, session.actions(skill), session.elapsed(skill, now),
			session.xpPerHour(skill, now), session.actionsPerHour(skill, now), xp,
			config.skillingSessionCount(), config.skillingRates(), config.skillingTimeToLevel());
	}

	/** The per-skill switches in Skilling badge. A skill without one is always shown. */
	private boolean skillShown(Skill skill)
	{
		return SkillSwitches.shown(config, skill);
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
				scanContainers();
			}
		}

		final WorldPoint position = self.getWorldLocation();
		final ActivityAlerts.Tick now = new ActivityAlerts.Tick();
		now.tick = client.getTickCount();
		now.animating = self.getAnimation() != -1;
		now.moved = lastPosition != null && position != null && !position.equals(lastPosition);
		now.filling = inventoryFilling;
		now.empty = alertEmpty();
		now.coinPouches = inventoryEmpty < 0 ? -1 : coinPouches;
		// Hundredths of a percent, as the base client's Idle Notifier reads it.
		now.runEnergy = client.getEnergy() / 100;
		lastPosition = position;

		final int pouchCap = config.alertCoinPouchCap();
		final int stopTicks = ActivityAlerts.ticks(config.alertStopSeconds());
		final List<ActivityAlerts.Alert> fired = new ArrayList<>(activityAlerts.tick(now, stopTicks, pouchCap,
			config.alertRunEnergyThreshold()));
		fired.addAll(tickTraining(now.tick, now.animating, stopTicks));
		fired.addAll(tickPump(now.tick, now.animating));
		addSkillingEvents(fired, now.tick);
		sendAlerts(fired, pouchCap);
	}

	/**
	 * This tick's skilling events, a clue nest routed to the bird nest alert
	 * when Clue drop is off, and level-ups the filter does not want left out.
	 */
	private void addSkillingEvents(List<ActivityAlerts.Alert> fired, int tick)
	{
		final boolean clueOn = AlertSettings.of(config, ActivityAlerts.Type.CLUE_DROP).enabled();
		for (ActivityAlerts.Alert alert : skillingEvents.tick(tick))
		{
			if (alert.type != ActivityAlerts.Type.LEVEL_UP
				|| SkillingEvents.levelWanted(alert.skill, config.alertLevelUpSkills()))
			{
				fired.add(SkillingEvents.route(alert, clueOn));
			}
		}
	}

	/**
	 * The training method's tick: its countdowns for the badge and its
	 * alerts. The idle logout is read the way RuneLite's Idle Notifier reads
	 * it, in client ticks of 20 ms.
	 */
	private List<ActivityAlerts.Alert> tickTraining(int tick, boolean animating, int stopTicks)
	{
		final long idle = Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks())
			* (long) Constants.CLIENT_TICK_LENGTH;
		final long logout = client.getIdleTimeout() * (long) Constants.CLIENT_TICK_LENGTH;
		return trainingSession.tick(tick, animating, idle, logout, stopTicks,
			config.alertMethodWarnSeconds() * 1000L);
	}

	/** The pump's world, stove and coke checks, after the training session's tick. */
	private List<ActivityAlerts.Alert> tickPump(int tick, boolean animating)
	{
		pumpChecks.stove(stoveLevel(), tick);
		final boolean pumping = trainingSession.running()
			&& trainingSession.method() == TrainingMethod.BLAST_FURNACE_PUMP;
		return pumpChecks.tick(tick, pumping, animating, trainingSession.gainedLastTick(), client.getWorld(),
			PumpChecks.worlds(config.pumpWorlds()), config.alertMethodWarnSeconds() * 1000L);
	}

	/**
	 * How much coke the stove shows, 0 to 2, or -1 when it is not in the
	 * scene. Through its impostor in case the look is chosen by a varbit
	 * rather than by swapping the object.
	 */
	private int stoveLevel()
	{
		if (stove == null)
		{
			return -1;
		}
		ObjectComposition def = client.getObjectDefinition(stove.getId());
		if (def != null && def.getImpostorIds() != null)
		{
			def = def.getImpostor();
		}
		return def == null ? -1 : PumpChecks.stoveLevel(def.getId());
	}

	/** This tick's alerts, then any repeat that is due. */
	private void sendAlerts(List<ActivityAlerts.Alert> fired, int pouchCap)
	{
		// Skilling vitals off: no alert, and one already repeating stops.
		if (!VitalsHalves.skilling(config))
		{
			alertRepeat.stop();
			return;
		}

		// Back at it: experience again stops a repeat. Checked before this
		// tick's alerts, so "inventory full" on the tick of the last catch
		// still starts its own.
		if (activityAlerts.experienced() || trainingSession.resumed())
		{
			alertRepeat.activityResumed();
		}

		final long nowMs = System.currentTimeMillis();
		for (ActivityAlerts.Alert alert : fired)
		{
			sendAlert(alert, pouchCap, nowMs);
		}

		final ActivityAlerts.Alert again = alertRepeat.due(nowMs);
		if (again != null)
		{
			repeatAlert(again, pouchCap);
		}
	}

	/**
	 * Empty slots as Inventory full sees them: the inventory's alone, or with
	 * Count containers as free on, plus the known free space of the
	 * containers that take what the skill being worked gathers.
	 */
	private int alertEmpty()
	{
		if (!config.skillingContainersAsInventory() || inventoryEmpty < 0)
		{
			return inventoryEmpty;
		}
		final Skill active = activityAlerts.active() != null ? activityAlerts.active() : skilling.skill();
		return ContainerRow.combinedEmpty(inventoryEmpty, containerRows(active), true);
	}

	/**
	 * One alert: RuneLite's notification as the user set it up, with its
	 * sound swapped for the plugin's when the alert has its own, and a repeat
	 * when that is on. An alert switched off does nothing at all.
	 */
	private void sendAlert(ActivityAlerts.Alert alert, int pouchCap, long nowMs)
	{
		final AlertSettings settings = AlertSettings.of(config, alert.type);
		if (!settings.enabled() || alertMute.muted())
		{
			return;
		}

		final AlertSounds.Plan plan = soundPlan(settings, alert);
		notifier.notify(plan.runelite ? muted(settings.notification)
				: AlertSettings.quiet(settings.notification, runeLiteConfig),
			ActivityAlerts.message(alert, pouchCap));
		play(plan, settings.volume);
		if (alertSounds.shouldHint(plan, alert.type))
		{
			consoleLine(ChatColorType.HIGHLIGHT, PluginName.NAME + ": no sound file at .runelite/plugin-data/"
				+ "combat-vitals/" + AlertSounds.USER_FOLDER + "/" + plan.file
				+ ", so RuneLite's sound played instead.");
		}
		alertRepeat.fired(alert, settings.repeat, nowMs, config.repeatEverySeconds(), config.repeatMaxMinutes());
	}

	/**
	 * A repeat: only the sound again. RuneLite's own sound can only be played
	 * by notifying again; the plugin's sounds play without another tray
	 * notification. Stops if the alert was switched off meanwhile.
	 */
	private void repeatAlert(ActivityAlerts.Alert alert, int pouchCap)
	{
		final AlertSettings settings = AlertSettings.of(config, alert.type);
		if (!settings.enabled() || !settings.repeat)
		{
			alertRepeat.stop();
			return;
		}

		final AlertSounds.Plan plan = soundPlan(settings, alert);
		if (config.alertMuteSounds() || alertMute.muted())
		{
			return;
		}
		if (plan.runelite)
		{
			notifier.notify(settings.notification, ActivityAlerts.message(alert, pouchCap));
		}
		else
		{
			play(plan, settings.volume);
		}
	}

	/** The notification with RuneLite's own sound off while the plugin's alerts are muted. */
	private Notification muted(Notification notification)
	{
		return config.alertMuteSounds() ? AlertSettings.quiet(notification, runeLiteConfig) : notification;
	}

	private AlertSounds.Plan soundPlan(AlertSettings settings, ActivityAlerts.Alert alert)
	{
		return AlertSounds.plan(settings.sound, alert, lastGainedItem, voiceLines,
			BUNDLED, name -> userSound(name) != null);
	}

	/** {@code name} in the user's sounds folder through the Filepath API, or null when it is not a file there. */
	private Filepath userSound(String name)
	{
		try
		{
			final Filepath file = getPluginDirectory().joinSegment(AlertSounds.USER_FOLDER).joinSegment(name);
			return file.isFile() ? file : null;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("No user sound {}", name, e);
			return null;
		}
	}

	/**
	 * A sound bundled in the jar, opened the jar-safe way and closed again;
	 * a voice clip must also be in the one format the joiner takes.
	 */
	private static final java.util.function.Predicate<String> BUNDLED =
		AlertSounds.playable(AlertSounds.class::getResourceAsStream);

	/** Plays the plugin's own sound off the client thread; RuneLite's own is the Notifier's to play. */
	private void play(AlertSounds.Plan plan, int volume)
	{
		if (plan.runelite || config.alertMuteSounds() || alertMute.muted())
		{
			return;
		}
		// Read here, on the client thread: preferences are the game's own.
		final int effective = AlertSounds.effectiveVolume(volume, config.alertFollowGameVolume(),
			client.getPreferences().getSoundEffectVolume());
		if (effective <= 0)
		{
			return;
		}
		final float gain = AlertSounds.gain(effective);
		executor.execute(() ->
		{
			try
			{
				if (plan.file != null)
				{
					final Filepath file = userSound(plan.file);
					if (file == null)
					{
						return;
					}
					// Read whole: the audio player needs a stream it can mark and reset.
					try (InputStream in = file.openInputStream())
					{
						audioPlayer.play(new ByteArrayInputStream(in.readAllBytes()), gain);
					}
				}
				else if (plan.resources.size() == 1)
				{
					audioPlayer.play(AlertSounds.class, plan.resources.get(0), gain);
				}
				else
				{
					final byte[] joined = AlertSounds.join(plan.resources, AlertSounds.class::getResourceAsStream);
					audioPlayer.play(new ByteArrayInputStream(joined), gain);
				}
			}
			// Exception, not the player's own exception types: those are Java
			// sound API classes, which the Plugin Hub refuses in a plugin's source.
			catch (Exception e)
			{
				log.warn("Could not play the alert sound", e);
			}
		});
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
		// Runs with no opponent too: a fight whose target despawned still has to
		// time out, or it holds the skilling badge off for good.
		// Only interacting with the opponent itself holds it; interacting with
		// anything else, a fishing spot included, lets the timeout run.
		final Player self = client.getLocalPlayer();
		final boolean stillOnTarget = opponent != null && self != null && self.getInteracting() == opponent;

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

		final boolean onHud = hasHpHud(actor);
		if (config.hideWhenHpHud() && onHud)
		{
			return null;
		}

		String name = Text.removeTags(actor.getName());
		int maxHealth = -1;
		HiscoreResult hiscore = null;
		DisplayMode mode = config.displayMode();

		if (npc)
		{
			final NPC target = (NPC) actor;
			final boolean boss = BigTargets.isBoss(onHud, name);
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
			mode = npcDisplay(boss, maxHealth);
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

		return new Readout(actor, name, ratio, scale, maxHealth, range, npc, -1, hiscore, mode);
	}

	/**
	 * Bosses and high-HP NPCs can show a different display from everything
	 * else. Max HP is NPCManager's, the same number the readout prints, so an
	 * NPC it has no entry for is never over the threshold.
	 */
	private DisplayMode npcDisplay(boolean boss, int maxHealth)
	{
		final boolean big = BigTargets.isBig(config.bigTargetRule(), boss, maxHealth, config.bigTargetMaxHp());
		return BigTargets.mode(big, config.bigTargetDisplay(), config.otherTargetDisplay(), config.displayMode());
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
		// Pickpocketing can stun and hit you at any attempt, so your health is
		// up for the whole time, over the badge (Board, 2026-11-06).
		if (!config.showOwnHealth() || !(combat.inCombat(Instant.now(), timeout()) || thievingFromNpc()))
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

		return new Readout(self, Text.removeTags(self.getName()), -1, -1, max, null, false, current, null,
			config.displayMode());
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

		/** Display, Big targets or Other targets, whichever applies to this actor. */
		private final DisplayMode mode;

		Readout(Actor actor, String name, int ratio, int scale, int maxHealth,
			HealthRecovery.Range range, boolean npc, int exactHealth, HiscoreResult hiscore, DisplayMode mode)
		{
			this.mode = mode;
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

		/** The text both overlays draw, in this actor's display, or null for nothing. */
		String text(boolean decimals)
		{
			return HealthReadout.text(range, maxHealth, ratio, scale, mode, decimals, exactHealth);
		}
	}
}
