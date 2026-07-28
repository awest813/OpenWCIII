package com.etheller.warsmash.parsers.jass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.HandleIdAllocator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerSleepAction;
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

	private JassAIEnvironment(final JassProgram jassProgramVisitor, final DataSource dataSource,
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
		jassProgramVisitor.getJassNativeManager().createNative("Player", (arguments, globalScope, triggerScope) -> {
			final int index = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			final CPlayer player = simulation.getPlayer(index);
			return new HandleJassValue(JassAIEnvironment.this.playerType, player);
		});
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
					final CPlayer player = simulation.getPlayer(JassAIEnvironment.this.aiPlayerIndex);
					return IntegerJassValue.of(player != null ? player.getGold() : 0);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetWood",
				(arguments, globalScope, triggerScope) -> {
					final CPlayer player = simulation.getPlayer(JassAIEnvironment.this.aiPlayerIndex);
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
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("TownCountDone",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("TownCountEx",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("TownWithGreatestNeed",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("GetNextExpansion",
				(arguments, globalScope, triggerScope) -> IntegerJassValue.of(-1));
		jassProgramVisitor.getJassNativeManager().createNative("GetExpansionX",
				(arguments, globalScope, triggerScope) -> RealJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("GetExpansionY",
				(arguments, globalScope, triggerScope) -> RealJassValue.ZERO);
		jassProgramVisitor.getJassNativeManager().createNative("SetBuildUnit",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("SetBuildUnitEx",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("SetBuildUpgr",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("SetBuildUpgrEx",
				(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
		jassProgramVisitor.getJassNativeManager().createNative("SetBuildDone",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("ClearBuildQueue",
				(arguments, globalScope, triggerScope) -> null);
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
		jassProgramVisitor.getJassNativeManager().createNative("SuicideUnit",
				(arguments, globalScope, triggerScope) -> null);
		jassProgramVisitor.getJassNativeManager().createNative("SuicideUnitEx",
				(arguments, globalScope, triggerScope) -> null);
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
		final String[] files = new String[] { "Scripts\\common.ai", scriptPath, "Scripts\\" + scriptPath };
		boolean loadedAny = false;
		for (final String file : files) {
			String path = file;
			if (!dataSource.has(path)) {
				// try basename only
				final int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
				if (slash >= 0) {
					path = path.substring(slash + 1);
				}
			}
			if (!dataSource.has(path)) {
				continue;
			}
			try {
				Jass2.readJassFile(dataSource, jassProgramVisitor, path);
				loadedAny = true;
			}
			catch (final Exception e) {
				System.err.println("StartCampaignAI: failed reading " + path + ": " + e.getMessage());
			}
		}
		if (!loadedAny) {
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
