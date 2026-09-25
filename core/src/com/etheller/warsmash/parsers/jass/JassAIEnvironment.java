package com.etheller.warsmash.parsers.jass;

import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.definition.JassDefinitionBlock;
import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.function.JassParameter;
import com.etheller.interpreter.ast.function.NativeJassFunction;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.JassValueVisitor;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.Scene;
import net.warsmash.parsers.jass.SmashJassParser;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUpgradeType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.HandleIdAllocator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMinable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityQueue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.upgrade.CAbilityUpgrade;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.harvest.CBehaviorReturnResources;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerSleepAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.BuildOnBuildingIntersector;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.PointAbilityTargetCheckReceiver;
import com.etheller.warsmash.util.War3ID;

/**
 * Separate JASS VM used for campaign/melee AI scripts ({@code common.ai} +
 * race {@code *.ai} files). Cooperative {@code StartThread}/{@code Sleep}
 * mirror the map script VM so AI scripts can run without blocking simulation.
 */
public class JassAIEnvironment {
	private final GameUI gameUI;
	private Element skin;
	private final JassProgram jassProgramVisitor;
	private final CSimulation simulation;
	private final int aiPlayerIndex;
	private final HandleJassType playerType;
	private final HandleJassType unitType;
	private float captainHomeX;
	private float captainHomeY;
	private float captainX;
	private float captainY;
	private boolean captainAtHome = true;
	private final List<CUnit> assaultGroup = new ArrayList<>();
	private float nextExpansionX = 0f;
	private float nextExpansionY = 0f;

	private static final class GuardPost {
		final War3ID unitId;
		final float x;
		final float y;
		CUnit assignedUnit;

		GuardPost(final War3ID unitId, final float x, final float y) {
			this.unitId = unitId;
			this.x = x;
			this.y = y;
		}
	}

	private final List<GuardPost> guardPosts = new ArrayList<>();

	JassAIEnvironment(final JassProgram jassProgramVisitor, final DataSource dataSource,
			final Viewport uiViewport, final Scene uiScene, final GameUI gameUI, final War3MapConfig mapConfig,
			final CSimulation simulation, final int aiPlayerIndex) {
		this.jassProgramVisitor = jassProgramVisitor;
		this.gameUI = gameUI;
		this.simulation = simulation;
		this.aiPlayerIndex = aiPlayerIndex;
		final Rectangle tempRect = new Rectangle();
		final GlobalScope globals = jassProgramVisitor.getGlobals();
		globals.registerHandleType("agent");
		globals.registerHandleType("event");
		this.playerType = globals.registerHandleType("player");
		globals.registerHandleType("widget");
		this.unitType = globals.registerHandleType("unit");
		globals.registerHandleType("destructable");
		globals.registerHandleType("item");
		globals.registerHandleType("ability");
		globals.registerHandleType("buff");
		globals.registerHandleType("force");
		globals.registerHandleType("group");
		globals.registerHandleType("trigger");
		globals.registerHandleType("triggercondition");
		globals.registerHandleType("triggeraction");
		globals.registerHandleType("timer");
		final HandleJassType locationType = globals.registerHandleType("location");
		globals.registerHandleType("region");
		globals.registerHandleType("rect");
		globals.registerHandleType("boolexpr");
		globals.registerHandleType("sound");
		globals.registerHandleType("conditionfunc");
		globals.registerHandleType("filterfunc");
		globals.registerHandleType("unitpool");
		globals.registerHandleType("itempool");
		final HandleJassType raceType = globals.registerHandleType("race");
		final HandleJassType alliancetypeType = globals.registerHandleType("alliancetype");
		final HandleJassType racepreferenceType = globals.registerHandleType("racepreference");
		globals.registerHandleType("gamestate");
		final HandleJassType igamestateType = globals.registerHandleType("igamestate");
		final HandleJassType fgamestateType = globals.registerHandleType("fgamestate");
		final HandleJassType playerstateType = globals.registerHandleType("playerstate");
		final HandleJassType playerscoreType = globals.registerHandleType("playerscore");
		final HandleJassType playergameresultType = globals.registerHandleType("playergameresult");
		final HandleJassType unitstateType = globals.registerHandleType("unitstate");
		final HandleJassType aidifficultyType = globals.registerHandleType("aidifficulty");
		globals.registerHandleType("eventid");
		final HandleJassType gameeventType = globals.registerHandleType("gameevent");
		final HandleJassType playereventType = globals.registerHandleType("playerevent");
		final HandleJassType playeruniteventType = globals.registerHandleType("playerunitevent");
		final HandleJassType uniteventType = globals.registerHandleType("unitevent");
		final HandleJassType limitopType = globals.registerHandleType("limitop");
		final HandleJassType widgeteventType = globals.registerHandleType("widgetevent");
		final HandleJassType dialogeventType = globals.registerHandleType("dialogevent");
		final HandleJassType unittypeType = globals.registerHandleType("unittype");
		final HandleJassType gamespeedType = globals.registerHandleType("gamespeed");
		final HandleJassType gamedifficultyType = globals.registerHandleType("gamedifficulty");
		final HandleJassType gametypeType = globals.registerHandleType("gametype");
		final HandleJassType mapflagType = globals.registerHandleType("mapflag");
		final HandleJassType mapvisibilityType = globals.registerHandleType("mapvisibility");
		final HandleJassType mapsettingType = globals.registerHandleType("mapsetting");
		final HandleJassType mapdensityType = globals.registerHandleType("mapdensity");
		final HandleJassType mapcontrolType = globals.registerHandleType("mapcontrol");
		final HandleJassType playerslotstateType = globals.registerHandleType("playerslotstate");
		final HandleJassType volumegroupType = globals.registerHandleType("volumegroup");
		final HandleJassType camerafieldType = globals.registerHandleType("camerafield");
		globals.registerHandleType("camerasetup");
		final HandleJassType playercolorType = globals.registerHandleType("playercolor");
		final HandleJassType placementType = globals.registerHandleType("placement");
		final HandleJassType startlocprioType = globals.registerHandleType("startlocprio");
		final HandleJassType raritycontrolType = globals.registerHandleType("raritycontrol");
		final HandleJassType blendmodeType = globals.registerHandleType("blendmode");
		final HandleJassType texmapflagsType = globals.registerHandleType("texmapflags");
		globals.registerHandleType("effect");
		final HandleJassType effecttypeType = globals.registerHandleType("effecttype");
		globals.registerHandleType("weathereffect");
		globals.registerHandleType("terraindeformation");
		final HandleJassType fogstateType = globals.registerHandleType("fogstate");
		globals.registerHandleType("fogmodifier");
		globals.registerHandleType("dialog");
		globals.registerHandleType("button");
		globals.registerHandleType("quest");
		globals.registerHandleType("questitem");
		globals.registerHandleType("defeatcondition");
		globals.registerHandleType("timerdialog");
		globals.registerHandleType("leaderboard");
		globals.registerHandleType("multiboard");
		globals.registerHandleType("multiboarditem");
		globals.registerHandleType("trackable");
		globals.registerHandleType("gamecache");
		final HandleJassType versionType = globals.registerHandleType("version");
		final HandleJassType itemtypeType = globals.registerHandleType("itemtype");
		globals.registerHandleType("texttag");
		final HandleJassType attacktypeType = globals.registerHandleType("attacktype");
		final HandleJassType damagetypeType = globals.registerHandleType("damagetype");
		final HandleJassType weapontypeType = globals.registerHandleType("weapontype");
		final HandleJassType soundtypeType = globals.registerHandleType("soundtype");
		globals.registerHandleType("lightning");
		final HandleJassType pathingtypeType = globals.registerHandleType("pathingtype");
		globals.registerHandleType("image");
		globals.registerHandleType("ubersplat");
		globals.registerHandleType("hashtable");
		globals.registerHandleType("framehandle");
		globals.registerHandleType("abilitytype");
		globals.registerHandleType("ordercommandcard");
		globals.registerHandleType("ordercommandcardtype");
		globals.registerHandleType("abilitybehavior");
		globals.registerHandleType("behaviorexpr");
		globals.registerHandleType("iconui");

		Jass2.registerTypingNatives(jassProgramVisitor, raceType, alliancetypeType, racepreferenceType, igamestateType,
				fgamestateType, playerstateType, playerscoreType, playergameresultType, unitstateType, aidifficultyType,
				gameeventType, playereventType, playeruniteventType, uniteventType, limitopType, widgeteventType,
				dialogeventType, unittypeType, gamespeedType, gamedifficultyType, gametypeType, mapflagType,
				mapvisibilityType, mapsettingType, mapdensityType, mapcontrolType, playerslotstateType, volumegroupType,
				camerafieldType, playercolorType, placementType, startlocprioType, raritycontrolType, blendmodeType,
				texmapflagsType, effecttypeType, fogstateType, versionType, itemtypeType, attacktypeType,
				damagetypeType, weapontypeType, soundtypeType, pathingtypeType);
		Jass2.registerConversionAndStringNatives(jassProgramVisitor, gameUI);
		Jass2.registerConfigNatives(jassProgramVisitor, mapConfig, startlocprioType, gametypeType, placementType,
				gamespeedType, gamedifficultyType, mapdensityType, locationType, this.playerType, playercolorType,
				mapcontrolType, playerslotstateType, mapConfig, new HandleIdAllocator());
		Jass2.registerRandomNatives(jassProgramVisitor, simulation);

		globals.createGlobal("PLAYER_STATE_RESOURCE_GOLD", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.RESOURCE_GOLD));
		globals.createGlobal("PLAYER_STATE_RESOURCE_LUMBER", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.RESOURCE_LUMBER));
		globals.createGlobal("PLAYER_STATE_RESOURCE_FOOD_CAP", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.RESOURCE_FOOD_CAP));
		globals.createGlobal("PLAYER_STATE_RESOURCE_FOOD_USED", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.RESOURCE_FOOD_USED));
		globals.createGlobal("PLAYER_STATE_FOOD_CAP_CEILING", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.FOOD_CAP_CEILING));
		globals.createGlobal("PLAYER_STATE_GIVES_BOUNTY", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.GIVES_BOUNTY));
		globals.createGlobal("PLAYER_STATE_ALLIED_VICTORY", playerstateType,
				new HandleJassValue(playerstateType, CPlayerState.ALLIED_VICTORY));

		globals.createGlobal("ALLIANCE_PASSIVE", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.PASSIVE));
		globals.createGlobal("ALLIANCE_HELP_REQUEST", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.HELP_REQUEST));
		globals.createGlobal("ALLIANCE_HELP_RESPONSE", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.HELP_RESPONSE));
		globals.createGlobal("ALLIANCE_SHARED_XP", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.SHARED_XP));
		globals.createGlobal("ALLIANCE_SHARED_SPELLS", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.SHARED_SPELLS));
		globals.createGlobal("ALLIANCE_SHARED_VISION", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.SHARED_VISION));
		globals.createGlobal("ALLIANCE_SHARED_CONTROL", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.SHARED_CONTROL));
		globals.createGlobal("ALLIANCE_SHARED_ADVANCED_CONTROL", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.SHARED_ADVANCED_CONTROL));
		globals.createGlobal("ALLIANCE_RESCUABLE", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.RESCUABLE));
		globals.createGlobal("ALLIANCE_SHARED_VISION_FORCED", alliancetypeType,
				new HandleJassValue(alliancetypeType, CAllianceType.SHARED_VISION_FORCED));

		globals.createGlobal("MAP_DIFFICULTY_EASY", gamedifficultyType,
				new HandleJassValue(gamedifficultyType, CMapDifficulty.EASY));
		globals.createGlobal("MAP_DIFFICULTY_NORMAL", gamedifficultyType,
				new HandleJassValue(gamedifficultyType, CMapDifficulty.NORMAL));
		globals.createGlobal("MAP_DIFFICULTY_HARD", gamedifficultyType,
				new HandleJassValue(gamedifficultyType, CMapDifficulty.HARD));
		globals.createGlobal("MAP_DIFFICULTY_INSANE", gamedifficultyType,
				new HandleJassValue(gamedifficultyType, CMapDifficulty.INSANE));

		globals.createGlobal("VERSION_REIGN_OF_CHAOS", versionType,
				new HandleJassValue(versionType, 0));
		globals.createGlobal("VERSION_FROZEN_THRONE", versionType,
				new HandleJassValue(versionType, 1));

		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerState",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer p = arguments.size() > 0 && arguments.get(0) != null
							? arguments.get(0).visit(ObjectJassValueVisitor.getInstance()) : null;
					final CPlayerState state = arguments.size() > 1 && arguments.get(1) != null
							? arguments.get(1).visit(ObjectJassValueVisitor.getInstance()) : null;
					if (p != null && state != null && simulation != null) {
						return IntegerJassValue.of(p.getPlayerState(simulation, state));
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerAlliance",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer p1 = arguments.size() > 0 && arguments.get(0) != null
							? arguments.get(0).visit(ObjectJassValueVisitor.getInstance()) : null;
					final CPlayer p2 = arguments.size() > 1 && arguments.get(1) != null
							? arguments.get(1).visit(ObjectJassValueVisitor.getInstance()) : null;
					final CAllianceType setting = arguments.size() > 2 && arguments.get(2) != null
							? arguments.get(2).visit(ObjectJassValueVisitor.getInstance()) : null;
					if (p1 != null && p2 != null && setting != null) {
						return BooleanJassValue.of(p1.hasAlliance(p2.getId(), setting));
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerStructureCount",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer p = arguments.size() > 0 && arguments.get(0) != null
							? arguments.get(0).visit(ObjectJassValueVisitor.getInstance()) : null;
					final boolean includeUnfinished = arguments.size() > 1 && arguments.get(1) != null
							&& arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					if (p != null && simulation != null) {
						int count = 0;
						for (final CUnit unit : simulation.getUnits()) {
							if (unit != null && !unit.isDead() && unit.getPlayerIndex() == p.getId() && unit.isBuilding()) {
								if (includeUnfinished || (!unit.isConstructing() && !unit.isUpgrading())) {
									count++;
								}
							}
						}
						return IntegerJassValue.of(count);
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetFloatGameState",
				(arguments, globalScope, triggerScope) -> RealJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("GetGameDifficulty",
				(arguments, globalScope, triggerScope) -> gamedifficultyType.getNullValue());
		jassProgramVisitor.getJassNativeManager().createNative("GetFoodMade",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation != null) {
						final CUnitType unitType = simulation.getUnitData().getUnitType(new War3ID(unitId));
						return IntegerJassValue.of(unitType != null ? unitType.getFoodMade() : 0);
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetFoodUsed",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation != null) {
						final CUnitType unitType = simulation.getUnitData().getUnitType(new War3ID(unitId));
						return IntegerJassValue.of(unitType != null ? unitType.getFoodUsed() : 0);
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("VersionCompatible",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.TRUE);
		jassProgramVisitor.getJassNativeManager().createNative("SuicideSleep",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("Cheat",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("Max",
				(arguments, globalScope, triggerScope) -> {
					final int a = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int b = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue.of(Math.max(a, b));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerStartLocation",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer p = arguments.size() > 0 && arguments.get(0) != null
							? arguments.get(0).visit(ObjectJassValueVisitor.getInstance()) : null;
					return IntegerJassValue.of(p != null ? p.getId() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsUnitDetected",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = arguments.size() > 0 && arguments.get(0) != null
							? arguments.get(0).visit(ObjectJassValueVisitor.getInstance()) : null;
					return BooleanJassValue.of(unit != null && !unit.isDead());
				});

		jassProgramVisitor.getJassNativeManager().createNative("StartThread",
				(arguments, globalScope, triggerScope) -> {
					final CodeJassValue threadFunc = arguments.get(0).visit(CodeJassValueVisitor.getInstance());
					if (threadFunc != null) {
						globalScope.queueThread(globalScope.createThread(threadFunc));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("Sleep", (arguments, globalScope, triggerScope) -> {
			float seconds;
			if (arguments.size() >= 2) {
				final float lowBound = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
				final float highBound = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
				if (highBound > lowBound) {
					seconds = lowBound + (simulation.getSeededRandom().nextFloat() * (highBound - lowBound));
				}
				else {
					seconds = lowBound;
				}
			}
			else {
				seconds = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
			}
			final JassThread currentThread = globalScope.getCurrentThread();
			if (currentThread != null) {
				currentThread.setSleeping(true);
				final CTimerSleepAction timer = new CTimerSleepAction(currentThread);
				timer.setRepeats(false);
				timer.setTimeoutTime(Math.max(0f, seconds));
				timer.start(simulation);
			}
			return null;
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetAiPlayer",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(JassAIEnvironment.this.aiPlayerIndex);
				});
		final JassFunction playerFunc = (arguments, globalScope, triggerScope) -> {
			final int index = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			final CPlayer player = simulation != null ? simulation.getPlayer(index) : null;
			return new HandleJassValue(JassAIEnvironment.this.playerType, player);
		};
		globals.defineFunction(0, "native", "Player",
				new NativeJassFunction(Collections.singletonList(new JassParameter(JassType.INTEGER, "number")),
						this.playerType, "Player", playerFunc));
		jassProgramVisitor.getJassNativeManager().createNative("Player", playerFunc);
		jassProgramVisitor.getJassNativeManager().createNative("UnitAlive",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of((unit != null) && !unit.isDead());
				});
		jassProgramVisitor.getJassNativeManager().createNative("DoNothing",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DisplayText",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DisplayTextI",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DisplayTextII",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DisplayTextIII",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("Trace",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("TraceI",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("TraceII",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("TraceIII",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DebugS",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DebugFI",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DebugUnitID",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetMeleeDifficulty",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetTargetHeroes",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetPeonsRepair",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetHeroesFlee",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetUnitsFlee",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetGroupsFlee",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetSlowChopping",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetDefendPlayer",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetHeroesTakeItems",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetUnitsTakeItems",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetIgnoreInjured",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetCaptainChanges",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetSmartArtillery",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetWatchMegaTargets",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("InitAssault",
				(arguments, globalScope, triggerScope) -> {
					JassAIEnvironment.this.assaultGroup.clear();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("InitDefense",
				(arguments, globalScope, triggerScope) -> {
					JassAIEnvironment.this.assaultGroup.clear();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("WaitForSignal",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("GetGold",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer player = simulation != null ? simulation.getPlayer(JassAIEnvironment.this.aiPlayerIndex) : null;
					return IntegerJassValue.of(player != null ? player.getGold() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetWood",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer player = simulation != null ? simulation.getPlayer(JassAIEnvironment.this.aiPlayerIndex) : null;
					return IntegerJassValue.of(player != null ? player.getLumber() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUnitCount",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue.of(countUnitsOfType(JassAIEnvironment.this.aiPlayerIndex, unitId, false));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerUnitTypeCount",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int playerIndex = whichPlayer != null ? whichPlayer.getId()
							: JassAIEnvironment.this.aiPlayerIndex;
					return IntegerJassValue.of(countUnitsOfType(playerIndex, unitId, false));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetEnemyUnitCount",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue.of(countEnemyUnitsOfType(unitId));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetEnemyStrength",
				(arguments, globalScope, triggerScope) -> IntegerJassValue
						.of(countEnemyUnitsOfType(0)));
		jassProgramVisitor.getJassNativeManager().createNative("TownCount",
				(arguments, globalScope, triggerScope) -> {
					if (arguments.isEmpty()) {
						return IntegerJassValue.of(townHasHall(0) ? 1 : 0);
					}
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue.of(unitId == 0 ? (townHasHall(0) ? 1 : 0)
							: countUnitsOfType(this.aiPlayerIndex, unitId, false));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TownCountDone",
				(arguments, globalScope, triggerScope) -> {
					if (arguments.isEmpty()) {
						return IntegerJassValue.of(townHasHall(0) ? 1 : 0);
					}
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue.of(unitId == 0 ? (townHasHall(0) ? 1 : 0) : countUnitsOfTypeDone(unitId));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TownCountEx",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final boolean onlyDone = (arguments.size() > 1)
							&& arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					return IntegerJassValue.of(onlyDone ? countUnitsOfTypeDone(unitId)
							: countUnitsOfType(this.aiPlayerIndex, unitId, false));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TownWithGreatestNeed",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("GetNextExpansion",
				(arguments, globalScope, triggerScope) -> {
					final CUnit mine = findNextExpansionMine();
					if (mine != null) {
						this.nextExpansionX = mine.getX();
						this.nextExpansionY = mine.getY();
						return IntegerJassValue.of(1);
					}
					return IntegerJassValue.of(-1);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetExpansionX",
				(arguments, globalScope, triggerScope) -> {
					if ((this.nextExpansionX != 0f) || (this.nextExpansionY != 0f)) {
						return IntegerJassValue.of((int) this.nextExpansionX);
					}
					final CUnit mine = findNextExpansionMine();
					if (mine != null) {
						this.nextExpansionX = mine.getX();
						this.nextExpansionY = mine.getY();
						return IntegerJassValue.of((int) this.nextExpansionX);
					}
					return IntegerJassValue.of((int) getTownCenterX());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetExpansionY",
				(arguments, globalScope, triggerScope) -> {
					if ((this.nextExpansionX != 0f) || (this.nextExpansionY != 0f)) {
						return IntegerJassValue.of((int) this.nextExpansionY);
					}
					final CUnit mine = findNextExpansionMine();
					if (mine != null) {
						this.nextExpansionX = mine.getX();
						this.nextExpansionY = mine.getY();
						return IntegerJassValue.of((int) this.nextExpansionY);
					}
					return IntegerJassValue.of((int) getTownCenterY());
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetProduce",
				(arguments, globalScope, triggerScope) -> {
					final int qty = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int town = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(setProduce(qty, unitId, town));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetExpansion",
				(arguments, globalScope, triggerScope) -> {
					final CUnit peon = nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance());
					final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(setExpansion(peon, unitId));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetUpgrade",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(setProduce(1, unitId, -1));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUnitGoldCost",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CUnitType ut = simulation.getUnitData().getUnitType(new War3ID(unitId));
					return IntegerJassValue.of(ut != null ? ut.getGoldCost() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUnitWoodCost",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CUnitType ut = simulation.getUnitData().getUnitType(new War3ID(unitId));
					return IntegerJassValue.of(ut != null ? ut.getLumberCost() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUpgradeGoldCost",
				(arguments, globalScope, triggerScope) -> {
					final int upgId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CUpgradeType ut = simulation.getUpgradeData().getType(new War3ID(upgId));
					final CPlayer player = simulation.getPlayer(aiPlayerIndex);
					final int currentLevel = player != null ? player.getTechtreeUnlocked(new War3ID(upgId)) : 0;
					return IntegerJassValue.of(ut != null ? ut.getGoldCost(currentLevel) : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUpgradeWoodCost",
				(arguments, globalScope, triggerScope) -> {
					final int upgId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CUpgradeType ut = simulation.getUpgradeData().getType(new War3ID(upgId));
					final CPlayer player = simulation.getPlayer(aiPlayerIndex);
					final int currentLevel = player != null ? player.getTechtreeUnlocked(new War3ID(upgId)) : 0;
					return IntegerJassValue.of(ut != null ? ut.getLumberCost(currentLevel) : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUpgradeLevel",
				(arguments, globalScope, triggerScope) -> {
					final int upgId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CPlayer player = simulation.getPlayer(aiPlayerIndex);
					return IntegerJassValue.of(player != null ? player.getTechtreeUnlocked(new War3ID(upgId)) : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTownUnitCount",
				(arguments, globalScope, triggerScope) -> {
					final int id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int town = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final boolean doneOnly = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					return IntegerJassValue.of(getTownUnitCount(id, town, doneOnly));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUnitCountDone",
				(arguments, globalScope, triggerScope) -> {
					final int id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue.of(countUnitsOfTypeDone(id));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetMinesOwned",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.of(townHasMine(0) ? 1 : 0));
		jassProgramVisitor.getJassNativeManager().createNative("GetGoldOwned",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer player = simulation != null ? simulation.getPlayer(aiPlayerIndex) : null;
					return IntegerJassValue.of(player != null ? player.getGold() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("TownWithMine",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.of(townHasMine(0) ? 0 : -1));
		jassProgramVisitor.getJassNativeManager().createNative("TownHasMine",
				(arguments, globalScope, triggerScope) -> {
					final int town = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(townHasMine(town));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TownHasHall",
				(arguments, globalScope, triggerScope) -> {
					final int town = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(townHasHall(town));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TownThreated",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.of(isTownThreatened()));
		jassProgramVisitor.getJassNativeManager().createNative("HarvestGold",
				(arguments, globalScope, triggerScope) -> {
					final int town = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int peons = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderHarvestGold(peons);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("HarvestWood",
				(arguments, globalScope, triggerScope) -> {
					final int town = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int peons = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderHarvestWood(peons);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ClearHarvestAI",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("StopGathering",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("GetBuilding",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer p = nullable(arguments, 0, ObjectJassValueVisitor.<CPlayer>getInstance());
					return new HandleJassValue(unitType, getBuilding(p));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetEnemyBase",
				(arguments, globalScope, triggerScope) -> new HandleJassValue(unitType, getEnemyBase()));
		jassProgramVisitor.getJassNativeManager().createNative("GetExpansionFoe",
				(arguments, globalScope, triggerScope) -> new HandleJassValue(unitType, getEnemyBase()));
		jassProgramVisitor.getJassNativeManager().createNative("GetEnemyExpansion",
				(arguments, globalScope, triggerScope) -> new HandleJassValue(unitType, getEnemyBase()));
		jassProgramVisitor.getJassNativeManager().createNative("GetExpansionPeon",
				(arguments, globalScope, triggerScope) -> new HandleJassValue(unitType, getExpansionPeon()));
		jassProgramVisitor.getJassNativeManager().createNative("ShiftTownSpot",
				(arguments, globalScope, triggerScope) -> {
					final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					JassAIEnvironment.this.captainHomeX = x;
					JassAIEnvironment.this.captainHomeY = y;
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AttackMoveXY",
				(arguments, globalScope, triggerScope) -> {
					final int x = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int y = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					issueAssaultPointOrder(x, y, OrderIds.attack);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AttackMoveKill",
				(arguments, globalScope, triggerScope) -> {
					final CUnit target = nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance());
					if (target != null) {
						issueAssaultPointOrder(target.getX(), target.getY(), OrderIds.attack);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetCampaignAI",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetMeleeAI",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetHeroLevels",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetNewHeroes",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("GetHeroId",
				(arguments, globalScope, triggerScope) -> {
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CPlayer p = simulation.getPlayer(aiPlayerIndex);
					if ((p != null) && !p.getHeroes().isEmpty()) {
						return IntegerJassValue.of(p.getHeroes().get(0).getTypeId().getValue());
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetHeroLevelAI",
				(arguments, globalScope, triggerScope) -> {
					if (simulation == null) {
						return IntegerJassValue.ZERO;
					}
					final CPlayer p = simulation.getPlayer(aiPlayerIndex);
					if ((p != null) && !p.getHeroes().isEmpty()) {
						final CUnit hero = p.getHeroes().get(0);
						if (hero.getHeroData() != null) {
							return IntegerJassValue.of(hero.getHeroData().getHeroLevel());
						}
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("Unsummon",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("MergeUnits",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("PurchaseZeppelin",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetReplacementCount",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("RemoveInjuries",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("RemoveSiege",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("GetCreepCamp",
				(arguments, globalScope, triggerScope) -> {
					final int minLevel = !arguments.isEmpty()
							? arguments.get(0).visit(IntegerJassValueVisitor.getInstance()) : 0;
					final int maxLevel = (arguments.size() > 1)
							? arguments.get(1).visit(IntegerJassValueVisitor.getInstance()) : 100;
					final CUnit creep = findCreepTarget(minLevel, maxLevel);
					if (creep != null) {
						return new HandleJassValue(this.unitType, creep);
					}
					return this.unitType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("StartGetEnemyBase",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("WaitGetEnemyBase",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.TRUE);
		jassProgramVisitor.getJassNativeManager().createNative("SetStagePoint",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("AddGuardPost",
				(arguments, globalScope, triggerScope) -> {
					final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					this.guardPosts.add(new GuardPost(unitId != 0 ? new War3ID(unitId) : null, x, y));
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("FillGuardPosts",
				(arguments, globalScope, triggerScope) -> {
					fillGuardPosts();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ReturnGuardPosts",
				(arguments, globalScope, triggerScope) -> {
					returnGuardPosts();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateCaptains",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("TeleportCaptain",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("IsTowered",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("IgnoredUnits",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("CreepsOnMap",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.of(creepsOnMap()));
		jassProgramVisitor.getJassNativeManager().createNative("GetMegaTarget",
				(arguments, globalScope, triggerScope) -> unitType.getNullValue());
		jassProgramVisitor.getJassNativeManager().createNative("GetEnemyPower",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("SetAllianceTarget",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("GetAllianceTarget",
				(arguments, globalScope, triggerScope) -> unitType.getNullValue());
		jassProgramVisitor.getJassNativeManager().createNative("DoAiScriptDebug",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainReadinessMa",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("AddAssault",
				(arguments, globalScope, triggerScope) -> {
					final int count = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(addUnitsToAssault(count, unitId));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddDefenders",
				(arguments, globalScope, triggerScope) -> {
					final int count = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return BooleanJassValue.of(addUnitsToAssault(count, unitId));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetCaptainX",
				(arguments, globalScope, triggerScope) -> RealJassValue.of(JassAIEnvironment.this.captainX));
		jassProgramVisitor.getJassNativeManager().createNative("GetCaptainY",
				(arguments, globalScope, triggerScope) -> RealJassValue.of(JassAIEnvironment.this.captainY));
		jassProgramVisitor.getJassNativeManager().createNative("GetCaptainLoc",
				(arguments, globalScope, triggerScope) -> locationType.getNullValue());
		jassProgramVisitor.getJassNativeManager().createNative("CaptainInCombat",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainIsHome",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.of(JassAIEnvironment.this.captainAtHome));
		jassProgramVisitor.getJassNativeManager().createNative("CaptainIsEmpty",
				(arguments, globalScope, triggerScope) -> {
					pruneAssaultGroup();
					return BooleanJassValue.of(JassAIEnvironment.this.assaultGroup.isEmpty());
				});
		jassProgramVisitor.getJassNativeManager().createNative("CaptainIsFull",
				(arguments, globalScope, triggerScope) -> {
					pruneAssaultGroup();
					return BooleanJassValue.of(!JassAIEnvironment.this.assaultGroup.isEmpty());
				});
		jassProgramVisitor.getJassNativeManager().createNative("CaptainRetreating",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainAtGoal",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.of(JassAIEnvironment.this.captainAtHome));
		jassProgramVisitor.getJassNativeManager().createNative("CaptainGoHome",
				(arguments, globalScope, triggerScope) -> {
					JassAIEnvironment.this.captainX = JassAIEnvironment.this.captainHomeX;
					JassAIEnvironment.this.captainY = JassAIEnvironment.this.captainHomeY;
					JassAIEnvironment.this.captainAtHome = true;
					issueAssaultPointOrder(JassAIEnvironment.this.captainX, JassAIEnvironment.this.captainY,
							OrderIds.move);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ClearCaptainTargets",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainAttack",
				(arguments, globalScope, triggerScope) -> {
					final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					JassAIEnvironment.this.captainX = x;
					JassAIEnvironment.this.captainY = y;
					JassAIEnvironment.this.captainAtHome = false;
					issueAssaultPointOrder(x, y, OrderIds.attack);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("CaptainVsUnits",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainVsPlayer",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainReadiness",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainReadinessHP",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("CaptainGroupSize",
				(arguments, globalScope, triggerScope) -> {
					pruneAssaultGroup();
					return IntegerJassValue.of(JassAIEnvironment.this.assaultGroup.size());
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetCaptainHome",
				(arguments, globalScope, triggerScope) -> {
					// common.ai: SetCaptainHome(which, x, y) — which often ignored for single captain
					final float x;
					final float y;
					if (arguments.size() >= 3) {
						x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					}
					else {
						x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					}
					JassAIEnvironment.this.captainHomeX = x;
					JassAIEnvironment.this.captainHomeY = y;
					if (JassAIEnvironment.this.captainAtHome) {
						JassAIEnvironment.this.captainX = x;
						JassAIEnvironment.this.captainY = y;
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SuicidePlayer",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer target = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (target == null) {
						return BooleanJassValue.FALSE;
					}
					float tx = 0f;
					float ty = 0f;
					int count = 0;
					for (final CUnit unit : JassAIEnvironment.this.simulation.getUnits()) {
						if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == target.getId())) {
							tx += unit.getX();
							ty += unit.getY();
							count++;
						}
					}
					if (count == 0) {
						return BooleanJassValue.FALSE;
					}
					tx /= count;
					ty /= count;
					issueAllCombatUnitsAttack(tx, ty);
					return BooleanJassValue.TRUE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SuicidePlayerUnits",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer target = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (target == null) {
						return BooleanJassValue.FALSE;
					}
					float tx = 0f;
					float ty = 0f;
					int count = 0;
					for (final CUnit unit : JassAIEnvironment.this.simulation.getUnits()) {
						if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == target.getId())) {
							tx += unit.getX();
							ty += unit.getY();
							count++;
						}
					}
					if (count == 0) {
						return BooleanJassValue.FALSE;
					}
					tx /= count;
					ty /= count;
					issueAllCombatUnitsAttack(tx, ty);
					return BooleanJassValue.TRUE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("UnitAliveCheck",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("GroupTimedLife",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("DisablePathing",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SetAmphibious",
				(arguments, globalScope, triggerScope) -> null);
		// Ignore unused locals that some AI scripts declare against BooleanJassValueVisitor
		jassProgramVisitor.getJassNativeManager().createNative("CommandsWaiting",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("PopLastCommand",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("GetLastCommand",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("GetLastData",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
	}

	public GlobalScope getGlobalScope() {
		return this.jassProgramVisitor.getGlobals();
	}

	public int getAiPlayerIndex() {
		return this.aiPlayerIndex;
	}

	private int countUnitsOfType(final int playerIndex, final int unitTypeId, final boolean enemiesOnly) {
		if (this.simulation == null) {
			return 0;
		}
		int count = 0;
		final War3ID typeId = unitTypeId == 0 ? null : new War3ID(unitTypeId);
		final CPlayer self = this.simulation.getPlayer(this.aiPlayerIndex);
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead()) {
				continue;
			}
			if (enemiesOnly) {
				if (self != null && self.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
					continue;
				}
				if (unit.getPlayerIndex() == this.aiPlayerIndex) {
					continue;
				}
			}
			else if (unit.getPlayerIndex() != playerIndex) {
				continue;
			}
			if ((typeId != null) && (unit.getTypeId().getValue() != typeId.getValue())) {
				continue;
			}
			count++;
		}
		return count;
	}

	private int countEnemyUnitsOfType(final int unitTypeId) {
		return countUnitsOfType(-1, unitTypeId, true);
	}

	private void pruneAssaultGroup() {
		final Iterator<CUnit> it = this.assaultGroup.iterator();
		while (it.hasNext()) {
			final CUnit unit = it.next();
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				it.remove();
			}
		}
	}

	private boolean addUnitsToAssault(final int count, final int unitTypeId) {
		if (count <= 0) {
			return false;
		}
		final War3ID typeId = unitTypeId == 0 ? null : new War3ID(unitTypeId);
		int added = 0;
		for (final CUnit unit : this.simulation.getUnits()) {
			if (added >= count) {
				break;
			}
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			if ((typeId != null) && (unit.getTypeId().getValue() != typeId.getValue())) {
				continue;
			}
			if (this.assaultGroup.contains(unit)) {
				continue;
			}
			this.assaultGroup.add(unit);
			added++;
		}
		return added > 0;
	}

	private void issueAssaultPointOrder(final float x, final float y, final int orderId) {
		pruneAssaultGroup();
		for (final CUnit unit : this.assaultGroup) {
			issuePointOrder(unit, x, y, orderId);
		}
	}

	private void issueAllCombatUnitsAttack(final float x, final float y) {
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			if (unit.isBuilding()) {
				continue;
			}
			issuePointOrder(unit, x, y, OrderIds.attack);
		}
	}

	private void issuePointOrder(final CUnit whichUnit, final float x, final float y, final int preferredOrderId) {
		final CPlayerUnitOrderExecutor executor = this.simulation
				.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
		if (executor == null) {
			return;
		}
		final int[] tryOrders = new int[] { preferredOrderId, OrderIds.attack, OrderIds.smart, OrderIds.move };
		for (final int orderId : tryOrders) {
			final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
			int abilityHandleId = 0;
			AbilityPointTarget targetAsPoint = new AbilityPointTarget(x, y);
			for (final CAbility ability : whichUnit.getAbilities()) {
				ability.checkCanUse(this.simulation, whichUnit, orderId, activationReceiver);
				if (activationReceiver.isOk()) {
					final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
					ability.checkCanTarget(this.simulation, whichUnit, orderId, targetAsPoint, targetReceiver.reset());
					if (targetReceiver.getTarget() != null) {
						targetAsPoint = targetReceiver.getTarget();
						abilityHandleId = ability.getHandleId();
					}
				}
			}
			if (abilityHandleId != 0) {
				executor.issuePointOrder(whichUnit.getHandleId(), abilityHandleId, orderId, targetAsPoint.x,
						targetAsPoint.y, false);
				return;
			}
		}
	}

	private static <T> T nullable(final List<JassValue> arguments, final int index,
			final JassValueVisitor<T> visitor) {
		if (index < arguments.size()) {
			final JassValue value = arguments.get(index);
			if (value != null) {
				return value.visit(visitor);
			}
		}
		return null;
	}

	private float getTownCenterX() {
		if ((this.captainHomeX != 0f) || (this.captainHomeY != 0f)) {
			return this.captainHomeX;
		}
		if (this.simulation != null) {
			for (final CUnit unit : this.simulation.getUnits()) {
				if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == this.aiPlayerIndex)) {
					return unit.getX();
				}
			}
		}
		return 0f;
	}

	private float getTownCenterY() {
		if ((this.captainHomeX != 0f) || (this.captainHomeY != 0f)) {
			return this.captainHomeY;
		}
		if (this.simulation != null) {
			for (final CUnit unit : this.simulation.getUnits()) {
				if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == this.aiPlayerIndex)) {
					return unit.getY();
				}
			}
		}
		return 0f;
	}

	private AbilityPointTarget findBuildLocation(final CUnitType unitType, final float centerX, final float centerY) {
		final BufferedImage buildingPathingPixelMap = unitType.getBuildingPathingPixelMap();
		final boolean canBeBuiltOnThem = unitType.isCanBeBuiltOnThem();
		for (float radius = 256f; radius <= 1536f; radius += 128f) {
			for (int angle = 0; angle < 360; angle += 45) {
				final double rad = Math.toRadians(angle);
				final float testX = (float) (centerX + (radius * Math.cos(rad)));
				final float testY = (float) (centerY + (radius * Math.sin(rad)));
				final AbilityPointTarget point = new AbilityPointTarget(testX, testY);
				AbstractCAbilityBuild.roundTargetPoint(point, unitType);
				final boolean obstructed = AbstractCAbilityBuild.isBuildLocationObstructed(this.simulation, unitType,
						buildingPathingPixelMap, canBeBuiltOnThem, point.getX(), point.getY(), null,
						BuildOnBuildingIntersector.INSTANCE.reset(point.getX(), point.getY()));
				if (!obstructed) {
					return point;
				}
			}
		}
		return null;
	}

	private boolean setProduce(final int qty, final int unitIdInt, final int town) {
		if (this.simulation == null) {
			return false;
		}
		final War3ID unitId = new War3ID(unitIdInt);
		final CPlayer player = this.simulation.getPlayer(this.aiPlayerIndex);
		if (player == null) {
			return false;
		}
		final CPlayerUnitOrderExecutor executor = this.simulation
				.getDefaultPlayerUnitOrderExecutor(this.aiPlayerIndex);
		if (executor == null) {
			return false;
		}

		final CUnitType unitType = this.simulation.getUnitData().getUnitType(unitId);
		if (unitType != null) {
			final int currentCount = countUnitsOfType(this.aiPlayerIndex, unitIdInt, false);
			if (currentCount >= qty) {
				return true;
			}
			if ((player.getGold() < unitType.getGoldCost()) || (player.getLumber() < unitType.getLumberCost())) {
				return false;
			}
			if (!unitType.isBuilding() && (unitType.getFoodUsed() > 0)
					&& ((player.getFoodUsed() + unitType.getFoodUsed()) > player.getFoodCap())) {
				return false;
			}

			// Structure building
			if (unitType.isBuilding()) {
				for (final CUnit bldg : this.simulation.getUnits()) {
					if ((bldg != null) && !bldg.isDead() && (bldg.getPlayerIndex() == this.aiPlayerIndex)) {
						for (final CAbility ability : bldg.getAbilities()) {
							if (ability instanceof CAbilityUpgrade) {
								final CAbilityUpgrade upg = (CAbilityUpgrade) ability;
								if (upg.getUpgradesTo().contains(unitId)) {
									executor.issueImmediateOrder(bldg.getHandleId(), upg.getHandleId(), unitIdInt,
											false);
									return true;
								}
							}
						}
					}
				}
				for (final CUnit worker : this.simulation.getUnits()) {
					if ((worker != null) && !worker.isDead() && (worker.getPlayerIndex() == this.aiPlayerIndex)) {
						for (final CAbility ability : worker.getAbilities()) {
							if (ability instanceof AbstractCAbilityBuild) {
								final AbstractCAbilityBuild buildAbil = (AbstractCAbilityBuild) ability;
								if (buildAbil.getStructuresBuilt().contains(unitId)) {
									final AbilityPointTarget target = findBuildLocation(unitType, getTownCenterX(),
											getTownCenterY());
									if (target != null) {
										executor.issuePointOrder(worker.getHandleId(), buildAbil.getHandleId(),
												unitIdInt, target.getX(), target.getY(), false);
										return true;
									}
								}
							}
						}
					}
				}
				return false;
			}

			// Unit training
			for (final CUnit bldg : this.simulation.getUnits()) {
				if ((bldg != null) && !bldg.isDead() && (bldg.getPlayerIndex() == this.aiPlayerIndex)) {
					for (final CAbility ability : bldg.getAbilities()) {
						if (ability instanceof CAbilityQueue) {
							final CAbilityQueue queue = (CAbilityQueue) ability;
							if (queue.getUnitsTrained().contains(unitId)) {
								executor.issueImmediateOrder(bldg.getHandleId(), queue.getHandleId(), unitIdInt,
										false);
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		final CUpgradeType upgType = this.simulation.getUpgradeData().getType(unitId);
		if (upgType != null) {
			final int currentLevel = player.getTechtreeUnlocked(unitId);
			if (currentLevel >= qty) {
				return true;
			}
			if ((player.getGold() < upgType.getGoldCost(currentLevel))
					|| (player.getLumber() < upgType.getLumberCost(currentLevel))) {
				return false;
			}
			for (final CUnit bldg : this.simulation.getUnits()) {
				if ((bldg != null) && !bldg.isDead() && (bldg.getPlayerIndex() == this.aiPlayerIndex)) {
					for (final CAbility ability : bldg.getAbilities()) {
						if (ability instanceof CAbilityQueue) {
							final CAbilityQueue queue = (CAbilityQueue) ability;
							if (queue.getResearchesAvailable().contains(unitId)) {
								executor.issueImmediateOrder(bldg.getHandleId(), queue.getHandleId(), unitIdInt,
										false);
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private int countUnitsOfTypeDone(final int unitTypeId) {
		if (this.simulation == null) {
			return 0;
		}
		int count = 0;
		final War3ID typeId = unitTypeId == 0 ? null : new War3ID(unitTypeId);
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			if (unit.isConstructing()) {
				continue;
			}
			if ((typeId != null) && (unit.getTypeId().getValue() != typeId.getValue())) {
				continue;
			}
			count++;
		}
		return count;
	}

	private int getTownUnitCount(final int unitTypeId, final int townIndex, final boolean doneOnly) {
		if (this.simulation == null) {
			return 0;
		}
		int count = 0;
		final War3ID typeId = unitTypeId == 0 ? null : new War3ID(unitTypeId);
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			if (doneOnly && unit.isConstructing()) {
				continue;
			}
			if ((typeId != null) && (unit.getTypeId().getValue() != typeId.getValue())) {
				continue;
			}
			count++;
		}
		return count;
	}

	private void orderHarvestGold(final int peonCount) {
		if (this.simulation == null) {
			return;
		}
		final CPlayerUnitOrderExecutor executor = this.simulation
				.getDefaultPlayerUnitOrderExecutor(this.aiPlayerIndex);
		if (executor == null) {
			return;
		}
		int ordered = 0;
		for (final CUnit unit : this.simulation.getUnits()) {
			if (ordered >= peonCount) {
				break;
			}
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			CAbilityHarvest harvest = null;
			for (final CAbility a : unit.getAbilities()) {
				if (a instanceof CAbilityHarvest) {
					harvest = (CAbilityHarvest) a;
					break;
				}
			}
			if (harvest != null) {
				final CUnit mine = CBehaviorReturnResources.findNearestMine(unit, this.simulation);
				if (mine != null) {
					executor.issueTargetOrder(unit.getHandleId(), harvest.getHandleId(), OrderIds.smart,
							mine.getHandleId(), false);
					ordered++;
				}
			}
		}
	}

	private void orderHarvestWood(final int peonCount) {
		if (this.simulation == null) {
			return;
		}
		final CPlayerUnitOrderExecutor executor = this.simulation
				.getDefaultPlayerUnitOrderExecutor(this.aiPlayerIndex);
		if (executor == null) {
			return;
		}
		int ordered = 0;
		for (final CUnit unit : this.simulation.getUnits()) {
			if (ordered >= peonCount) {
				break;
			}
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			CAbilityHarvest harvest = null;
			for (final CAbility a : unit.getAbilities()) {
				if (a instanceof CAbilityHarvest) {
					harvest = (CAbilityHarvest) a;
					break;
				}
			}
			if (harvest != null) {
				final CDestructable tree = CBehaviorReturnResources.findNearestTree(unit, harvest, this.simulation,
						unit);
				if (tree != null) {
					executor.issueTargetOrder(unit.getHandleId(), harvest.getHandleId(), OrderIds.smart,
							tree.getHandleId(), false);
					ordered++;
				}
			}
		}
	}

	private void suicideUnit(final int count, final int unitTypeId, final int targetPlayerIndex) {
		if (this.simulation == null) {
			return;
		}
		float tx = 0f;
		float ty = 0f;
		int enemyCount = 0;
		final CPlayer self = this.simulation.getPlayer(this.aiPlayerIndex);
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead()) {
				continue;
			}
			if (targetPlayerIndex >= 0) {
				if (unit.getPlayerIndex() == targetPlayerIndex) {
					tx += unit.getX();
					ty += unit.getY();
					enemyCount++;
				}
			}
			else if (unit.getPlayerIndex() != this.aiPlayerIndex) {
				if ((self == null) || !self.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
					tx += unit.getX();
					ty += unit.getY();
					enemyCount++;
				}
			}
		}
		if (enemyCount == 0) {
			return;
		}
		tx /= enemyCount;
		ty /= enemyCount;

		final War3ID typeId = unitTypeId == 0 ? null : new War3ID(unitTypeId);
		int ordered = 0;
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((count > 0) && (ordered >= count)) {
				break;
			}
			if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				continue;
			}
			if (unit.isBuilding()) {
				continue;
			}
			if ((typeId != null) && (unit.getTypeId().getValue() != typeId.getValue())) {
				continue;
			}
			issuePointOrder(unit, tx, ty, OrderIds.attack);
			ordered++;
		}
	}

	private CUnit getBuilding(final CPlayer p) {
		if ((p == null) || (this.simulation == null)) {
			return null;
		}
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == p.getId()) && unit.isBuilding()) {
				return unit;
			}
		}
		return null;
	}

	private CUnit getEnemyBase() {
		if (this.simulation == null) {
			return null;
		}
		final CPlayer self = this.simulation.getPlayer(this.aiPlayerIndex);
		CUnit fallbackEnemyBldg = null;
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				if ((self != null) && !self.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
					if (unit.isBuilding()) {
						if (unit.getClassifications().contains(CUnitClassification.TOWNHALL)) {
							return unit;
						}
						if (fallbackEnemyBldg == null) {
							fallbackEnemyBldg = unit;
						}
					}
				}
			}
		}
		return fallbackEnemyBldg;
	}

	private CUnit getExpansionPeon() {
		if (this.simulation == null) {
			return null;
		}
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == this.aiPlayerIndex)) {
				for (final CAbility a : unit.getAbilities()) {
					if (a instanceof AbstractCAbilityBuild) {
						return unit;
					}
				}
			}
		}
		return null;
	}

	private boolean townHasHall(final int townId) {
		if (this.simulation == null) {
			return false;
		}
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() == this.aiPlayerIndex)) {
				if (unit.getClassifications().contains(CUnitClassification.TOWNHALL)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean townHasMine(final int townId) {
		if (this.simulation == null) {
			return false;
		}
		final float cx = getTownCenterX();
		final float cy = getTownCenterY();
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit != null) && !unit.isDead()) {
				for (final CAbility a : unit.getAbilities()) {
					if ((a instanceof CAbilityGoldMine) || (a instanceof CAbilityGoldMinable)) {
						final float dx = unit.getX() - cx;
						final float dy = unit.getY() - cy;
						if (((dx * dx) + (dy * dy)) < (2500f * 2500f)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isTownThreatened() {
		if (this.simulation == null) {
			return false;
		}
		final float cx = getTownCenterX();
		final float cy = getTownCenterY();
		final CPlayer self = this.simulation.getPlayer(this.aiPlayerIndex);
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit != null) && !unit.isDead() && (unit.getPlayerIndex() != this.aiPlayerIndex)) {
				if ((self == null) || !self.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
					final float dx = unit.getX() - cx;
					final float dy = unit.getY() - cy;
					if (((dx * dx) + (dy * dy)) < (1500f * 1500f)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private CUnit findNextExpansionMine() {
		if (this.simulation == null) {
			return null;
		}
		final float cx = getTownCenterX();
		final float cy = getTownCenterY();
		CUnit closestMine = null;
		float closestDistSq = Float.MAX_VALUE;

		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead()) {
				continue;
			}
			boolean isGoldMine = false;
			int gold = 0;
			for (final CAbility a : unit.getAbilities()) {
				if (a instanceof CAbilityGoldMine) {
					isGoldMine = true;
					gold = ((CAbilityGoldMine) a).getGold();
					break;
				}
				else if (a instanceof CAbilityGoldMinable) {
					isGoldMine = true;
					gold = 10000;
					break;
				}
			}
			if (!isGoldMine || (gold <= 0)) {
				continue;
			}
			final float dx = unit.getX() - cx;
			final float dy = unit.getY() - cy;
			final float distSq = (dx * dx) + (dy * dy);
			if (distSq < (1500f * 1500f)) {
				continue;
			}
			boolean claimed = false;
			for (final CUnit other : this.simulation.getUnits()) {
				if ((other != null) && !other.isDead()
						&& other.getClassifications().contains(CUnitClassification.TOWNHALL)) {
					final float ox = other.getX() - unit.getX();
					final float oy = other.getY() - unit.getY();
					if (((ox * ox) + (oy * oy)) < (1200f * 1200f)) {
						claimed = true;
						break;
					}
				}
			}
			if (claimed) {
				continue;
			}
			if (distSq < closestDistSq) {
				closestDistSq = distSq;
				closestMine = unit;
			}
		}
		return closestMine;
	}

	private boolean setExpansion(CUnit peon, final int unitIdInt) {
		if (this.simulation == null) {
			return false;
		}
		if (peon == null) {
			peon = getExpansionPeon();
		}
		if (peon == null) {
			return false;
		}
		final War3ID unitId = new War3ID(unitIdInt);
		final CPlayer player = this.simulation.getPlayer(this.aiPlayerIndex);
		if (player == null) {
			return false;
		}
		final CUnitType unitType = this.simulation.getUnitData().getUnitType(unitId);
		if (unitType == null) {
			return false;
		}
		if ((player.getGold() < unitType.getGoldCost()) || (player.getLumber() < unitType.getLumberCost())) {
			return false;
		}
		float targetX = this.nextExpansionX;
		float targetY = this.nextExpansionY;
		if ((targetX == 0f) && (targetY == 0f)) {
			final CUnit mine = findNextExpansionMine();
			if (mine != null) {
				targetX = mine.getX();
				targetY = mine.getY();
			}
			else {
				return false;
			}
		}
		final AbilityPointTarget target = findBuildLocation(unitType, targetX, targetY);
		if (target == null) {
			return false;
		}
		for (final CAbility ability : peon.getAbilities()) {
			if (ability instanceof AbstractCAbilityBuild) {
				final AbstractCAbilityBuild buildAbil = (AbstractCAbilityBuild) ability;
				if (buildAbil.getStructuresBuilt().contains(unitId)) {
					final CPlayerUnitOrderExecutor executor = this.simulation
							.getDefaultPlayerUnitOrderExecutor(this.aiPlayerIndex);
					if (executor != null) {
						executor.issuePointOrder(peon.getHandleId(), buildAbil.getHandleId(), unitIdInt,
								target.getX(), target.getY(), false);
						return true;
					}
				}
			}
		}
		return false;
	}

	private void fillGuardPosts() {
		if (this.simulation == null) {
			return;
		}
		for (final GuardPost post : this.guardPosts) {
			if ((post.assignedUnit != null) && !post.assignedUnit.isDead()) {
				continue;
			}
			post.assignedUnit = null;
			for (final CUnit unit : this.simulation.getUnits()) {
				if ((unit == null) || unit.isDead() || (unit.getPlayerIndex() != this.aiPlayerIndex)) {
					continue;
				}
				if (unit.isBuilding() || this.assaultGroup.contains(unit)) {
					continue;
				}
				boolean alreadyAssigned = false;
				for (final GuardPost other : this.guardPosts) {
					if (other.assignedUnit == unit) {
						alreadyAssigned = true;
						break;
					}
				}
				if (alreadyAssigned) {
					continue;
				}
				if ((post.unitId != null) && (unit.getTypeId().getValue() != post.unitId.getValue())) {
					continue;
				}
				post.assignedUnit = unit;
				issuePointOrder(unit, post.x, post.y, OrderIds.move);
				break;
			}
		}
	}

	private void returnGuardPosts() {
		if (this.simulation == null) {
			return;
		}
		for (final GuardPost post : this.guardPosts) {
			if ((post.assignedUnit != null) && !post.assignedUnit.isDead()) {
				final float dx = post.assignedUnit.getX() - post.x;
				final float dy = post.assignedUnit.getY() - post.y;
				if (((dx * dx) + (dy * dy)) > (600f * 600f)) {
					issuePointOrder(post.assignedUnit, post.x, post.y, OrderIds.move);
				}
			}
		}
	}

	private boolean creepsOnMap() {
		if (this.simulation == null) {
			return false;
		}
		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead() || unit.isBuilding()) {
				continue;
			}
			if (unit.getPlayerIndex() >= 12) {
				return true;
			}
		}
		return false;
	}

	private CUnit findCreepTarget(final int minLevel, final int maxLevel) {
		if (this.simulation == null) {
			return null;
		}
		final float cx = getTownCenterX();
		final float cy = getTownCenterY();
		CUnit bestCreep = null;
		float bestDistSq = Float.MAX_VALUE;

		for (final CUnit unit : this.simulation.getUnits()) {
			if ((unit == null) || unit.isDead() || unit.isBuilding() || (unit.getPlayerIndex() < 12)) {
				continue;
			}
			final int level = (unit.getUnitType() != null) ? unit.getUnitType().getLevel() : 1;
			if (((minLevel > 0) && (level < minLevel)) || ((maxLevel > 0) && (level > maxLevel))) {
				continue;
			}
			final float dx = unit.getX() - cx;
			final float dy = unit.getY() - cy;
			final float distSq = (dx * dx) + (dy * dy);
			if (distSq < bestDistSq) {
				bestDistSq = distSq;
				bestCreep = unit;
			}
		}
		if (bestCreep == null) {
			for (final CUnit unit : this.simulation.getUnits()) {
				if ((unit == null) || unit.isDead() || unit.isBuilding() || (unit.getPlayerIndex() < 12)) {
					continue;
				}
				final float dx = unit.getX() - cx;
				final float dy = unit.getY() - cy;
				final float distSq = (dx * dx) + (dy * dy);
				if (distSq < bestDistSq) {
					bestDistSq = distSq;
					bestCreep = unit;
				}
			}
		}
		return bestCreep;
	}

	public void main() {
		try {
			final JassThread mainThread = this.jassProgramVisitor.getGlobals().createThread("main",
					Collections.emptyList(), TriggerExecutionScope.EMPTY);
			this.jassProgramVisitor.getGlobals().queueThread(mainThread);
		}
		catch (final Exception exc) {
			throw new JassException(this.jassProgramVisitor.getGlobals(),
					"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc);
		}
	}

	private static final String COMMON_AI_EXTRA_NATIVES =
			"native Player takes integer number returns player\n"
			+ "constant native GetPlayerState takes player whichPlayer, playerstate whichPlayerState returns integer\n"
			+ "constant native GetPlayerAlliance takes player whichPlayer, player otherPlayer, alliancetype whichAllianceSetting returns boolean\n"
			+ "constant native GetPlayerStructureCount takes player whichPlayer, boolean includeUnfinished returns integer\n"
			+ "constant native GetRandomInt takes integer lowBound, integer highBound returns integer\n"
			+ "constant native GetFloatGameState takes fgamestate whichFloatGameState returns real\n"
			+ "constant native GetGameDifficulty takes nothing returns gamedifficulty\n"
			+ "constant native GetFoodMade takes integer unitid returns integer\n"
			+ "constant native GetFoodUsed takes integer unitid returns integer\n"
			+ "constant native VersionCompatible takes version whichVersion returns boolean\n"
			+ "constant native SuicideSleep takes integer seconds returns nothing\n"
			+ "native Cheat takes string cheatStr returns nothing\n"
			+ "constant native Deg2Rad takes real degrees returns real\n"
			+ "constant native Rad2Deg takes real radians returns real\n"
			+ "constant native Sin takes real radians returns real\n"
			+ "constant native Cos takes real radians returns real\n"
			+ "constant native Tan takes real radians returns real\n"
			+ "constant native Asin takes real y returns real\n"
			+ "constant native Acos takes real x returns real\n"
			+ "constant native Atan takes real x returns real\n"
			+ "constant native Atan2 takes real y, real x returns real\n"
			+ "constant native SquareRoot takes real x returns real\n"
			+ "constant native Pow takes real x, real power returns real\n"
			+ "constant native I2R takes integer i returns real\n"
			+ "constant native R2I takes real r returns integer\n"
			+ "constant native I2S takes integer i returns string\n"
			+ "constant native R2S takes real r returns string\n"
			+ "constant native R2SW takes real r, integer width, integer precision returns string\n"
			+ "constant native S2I takes string s returns integer\n"
			+ "constant native S2R takes string s returns real\n"
			+ "constant native StringHash takes string s returns integer\n"
			+ "constant native StringLength takes string s returns integer\n"
			+ "constant native SubString takes string source, integer start, integer end returns string\n"
			+ "constant native StringCase takes string source, boolean upper returns string\n"
			+ "constant native Max takes integer a, integer b returns integer\n"
			+ "constant native GetPlayers takes nothing returns integer\n"
			+ "constant native GetStartLocationX takes integer whichStartLocation returns real\n"
			+ "constant native GetStartLocationY takes integer whichStartLocation returns real\n"
			+ "constant native GetPlayerStartLocation takes player whichPlayer returns integer\n"
			+ "constant native IsUnitDetected takes unit whichUnit, player whichPlayer returns boolean\n";

	private static volatile List<JassDefinitionBlock> cachedCommonAiBlocks = null;
	private static final Object COMMON_AI_LOCK = new Object();

	public static void preloadCommonAi(final DataSource dataSource) {
		if (cachedCommonAiBlocks == null) {
			synchronized (COMMON_AI_LOCK) {
				if (cachedCommonAiBlocks == null) {
					loadCommonAiBlocks(dataSource);
				}
			}
		}
	}

	public static void resetCachedCommonAi() {
		synchronized (COMMON_AI_LOCK) {
			cachedCommonAiBlocks = null;
		}
	}

	private static List<JassDefinitionBlock> loadCommonAiBlocks(final DataSource dataSource) {
		final List<JassDefinitionBlock> collected = new ArrayList<>();
		final JassProgram collector = new JassProgram() {
			@Override
			public void addAll(final List<JassDefinitionBlock> blocks) {
				super.addAll(blocks);
				collected.addAll(blocks);
			}
		};
		String commonPath = "Scripts\\common.ai";
		if (!dataSource.has(commonPath) && dataSource.has("common.ai")) {
			commonPath = "common.ai";
		}
		if (dataSource.has(commonPath)) {
			try {
				final SmashJassParser extraParser = new SmashJassParser(new StringReader(COMMON_AI_EXTRA_NATIVES));
				extraParser.scanAndParse("COMMON_AI_EXTRA_NATIVES", collector);
				Jass2.readJassFile(dataSource, collector, commonPath);
				cachedCommonAiBlocks = Collections.unmodifiableList(collected);
			}
			catch (final Exception e) {
				System.err.println("Failed to cache common.ai: " + e.getMessage());
			}
		}
		return cachedCommonAiBlocks;
	}

	/**
	 * Loads {@code Scripts\common.ai} plus the race/campaign script and returns a
	 * ready AI environment. Missing scripts are tolerated (returns null) so maps
	 * without AI assets do not crash.
	 */
	public static JassAIEnvironment loadAI(final DataSource dataSource, final Viewport uiViewport, final Scene uiScene,
			final GameUI gameUI, final War3MapConfig mapConfig, final CSimulation simulation, final int aiPlayerIndex,
			final String scriptPath) {
		if ((scriptPath == null) || scriptPath.isEmpty()) {
			System.err.println("StartCampaignAI: empty script path for player " + aiPlayerIndex);
			return null;
		}
		final JassProgram jassProgramVisitor = new JassProgram();
		final JassAIEnvironment environment = new JassAIEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				gameUI, mapConfig, simulation, aiPlayerIndex);

		preloadCommonAi(dataSource);
		if (cachedCommonAiBlocks != null) {
			jassProgramVisitor.addAll(cachedCommonAiBlocks);
		}
		else {
			final String[] preambleFiles = new String[] { "Scripts\\common.j", "Scripts\\common.ai" };
			for (final String file : preambleFiles) {
				String path = file;
				if (!dataSource.has(path)) {
					final int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
					if (slash >= 0) {
						path = path.substring(slash + 1);
					}
				}
				if (dataSource.has(path)) {
					try {
						Jass2.readJassFile(dataSource, jassProgramVisitor, path);
					}
					catch (final Exception e) {
						System.err.println("StartCampaignAI: failed reading " + path + ": " + e.getMessage());
						return null;
					}
				}
			}
		}
		boolean loadedScript = false;
		final String[] scriptCandidates = new String[] { scriptPath, "Scripts\\" + scriptPath };
		for (final String file : scriptCandidates) {
			String path = file;
			if (!dataSource.has(path)) {
				final int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
				if (slash >= 0) {
					path = path.substring(slash + 1);
				}
			}
			if (dataSource.has(path)) {
				try {
					Jass2.readJassFile(dataSource, jassProgramVisitor, path);
					loadedScript = true;
					break;
				}
				catch (final Exception e) {
					System.err.println("StartCampaignAI: failed reading " + path + ": " + e.getMessage());
					return null;
				}
			}
		}
		if (!loadedScript) {
			System.err.println("StartCampaignAI: no AI script found for \"" + scriptPath + "\" (player "
					+ aiPlayerIndex + ")");
			return null;
		}
		try {
			jassProgramVisitor.initialize();
		}
		catch (final Exception e) {
			System.err.println("StartCampaignAI: initialize failed for " + scriptPath + ": " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}
}
