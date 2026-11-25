package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityCyclone;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

/** Replays the original NightElf05 entrance from earned NightElf04 heroes. */
final class NightElfDungeonOpeningProbe {
	private static final boolean CROSS_LAVA_BRIDGE = Boolean.getBoolean("warsmash.nightElfDungeonBridge");
	private static final boolean WATCH_BEAR_ENCOUNTER = Boolean.getBoolean("warsmash.nightElfDungeonBears");
	private static final boolean FINISH_FURION_BRANCH = Boolean.getBoolean("warsmash.nightElfDungeonFurion");
	private static final boolean OPEN_TYRANDE_GATE = Boolean.getBoolean("warsmash.nightElfDungeonPlates");
	private static final boolean CROSS_TYRANDE_WAYGATE = Boolean.getBoolean("warsmash.nightElfDungeonWaygate");
	static final boolean PLAYTHROUGH = Boolean.getBoolean("warsmash.nightElfDungeonPlaythrough");
	private static final float[][] ENTRANCE_ROUTE = { { 16, 7344 }, { 272, 6864 }, { 304, 6384 }, { 304, 5904 },
			{ -48, 5424 }, { -176, 4944 }, { -240, 4464 }, { -240, 3984 }, { -16, 3504 }, { 176, 3024 },
			{ 176, 2544 }, { 240, 2064 }, { 240, 1584 }, { 592, 1104 }, { 1072, 624 }, { 1488, 144 },
			{ 1968, -336 }, { 2096, -816 }, { 2416, -1296 }, { 2608, -1776 }, { 2576, -2256 }, { 2096, -2544 },
			{ 1616, -2544 }, { 1136, -2512 }, { 656, -2320 }, { 304, -2480 }, { 144, -2960 }, { 80, -3440 },
			{ 80, -3920 }, { 80, -4400 }, { 80, -4880 }, { -240, -5360 }, { -240, -5424 } };
	private static final float[][] HEALING_SCROLL_ROUTE = {
			{ -16, 3504 }, { 272, 3504 }, { 560, 3504 }, { 848, 3504 }, { 1136, 3504 }, { 1424, 3536 } };
	private static final float[][] LAVA_BRIDGE_ROUTE = { { -1424, -6224 }, { -944, -6224 }, { -464, -6416 },
			{ 16, -6896 }, { 496, -7088 }, { 976, -7280 }, { 1456, -7280 }, { 1936, -6928 }, { 2416, -6608 },
			{ 2896, -6544 }, { 3376, -6544 }, { 3856, -6544 }, { 4336, -6544 }, { 4816, -6544 },
			{ 5296, -6544 }, { 5624, -6720 }, { 5776, -6640 }, { 6256, -6640 }, { 6736, -6640 },
			{ 7376, -6640 }, { 7728, -6832 }, { 8080, -6800 }, { 8176, -6448 }, { 8176, -6096 }, { 8176, -5744 },
			{ 8112, -5392 }, { 7952, -5040 }, { 7888, -4688 }, { 7760, -4336 }, { 7632, -3984 },
			{ 7632, -3632 }, { 7632, -3280 }, { 7632, -2928 }, { 7632, -2576 }, { 7376, -2224 }, { 7376, -2160 } };
	private static final int BRIDGE_FAR_SIDE = 18;
	private static final float[][] DRUID_DENS_ROUTE = { { 7376, -1904 }, { 7024, -1488 }, { 6608, -1072 },
			{ 6672, -720 }, { 7088, -304 }, { 7280, 112 }, { 7344, 528 }, { 7344, 944 }, { 7344, 1360 },
			{ 7088, 1776 }, { 6672, 2192 }, { 6480, 2608 }, { 6480, 3024 }, { 6480, 3440 }, { 6480, 3760 },
			{ 6608, 4272 }, { 7024, 4432 }, { 7440, 4784 }, { 7856, 4784 }, { 8272, 4432 }, { 8496, 4624 },
			{ 8496, 5040 }, { 8496, 5456 }, { 8624, 5872 }, { 8752, 6288 }, { 8688, 6704 }, { 8272, 6704 },
			{ 7856, 6704 }, { 7440, 6640 }, { 7248, 6320 }, { 7024, 6480 }, { 6608, 6352 }, { 6192, 6256 },
			{ 5776, 5840 }, { 5696, 5840 } };
	private static final int FIRST_DEN_GATE_APPROACH = 14;
	private static final int SECOND_DEN_GATE_APPROACH = 29;
	private static final float[][] FIRST_PRESSURE_PLATE = { { -2736, -6128 }, { -2960, -5840 }, { -2992, -5552 },
			{ -2992, -5264 }, { -2992, -4976 }, { -3216, -4688 }, { -3504, -4560 }, { -3792, -4560 },
			{ -4080, -4304 }, { -4368, -4304 }, { -4656, -4304 }, { -4944, -4464 }, { -5232, -4464 },
			{ -5456, -4720 }, { -5744, -5008 }, { -5808, -5296 }, { -5648, -5552 } };
	private static final float[][] SECOND_PRESSURE_PLATE = { { -5712, -5520 }, { -5808, -5232 }, { -5744, -4944 },
			{ -5456, -4656 }, { -5168, -4624 }, { -4880, -4624 }, { -4592, -4496 }, { -4304, -4304 },
			{ -4016, -4304 }, { -3728, -4304 }, { -3440, -4464 }, { -3280, -4752 }, { -3280, -5040 },
			{ -3280, -5328 }, { -3280, -5616 }, { -3280, -5904 }, { -3280, -6192 }, { -3280, -6480 },
			{ -3280, -6768 }, { -3280, -7056 }, { -3280, -7344 }, { -3440, -7632 }, { -3728, -7792 },
			{ -4016, -7856 }, { -4304, -7856 }, { -4592, -7856 }, { -4880, -7856 }, { -5168, -7664 },
			{ -5456, -7568 }, { -5744, -7280 }, { -5808, -6992 }, { -5648, -6736 } };
	private static final float[][] PRISON_WARD_CRATE_ROUTE = { { -5648, -5552 }, { -5648, -5264 }, { -5648, -4976 },
			{ -5648, -4688 }, { -5648, -4400 }, { -5584, -4112 } };
	private static final float[][] PRISON_WAYGATE_ROUTE = { { -4720, -4464 }, { -4432, -4336 }, { -4144, -4304 },
			{ -3856, -4304 }, { -3568, -4336 }, { -3344, -4624 }, { -3280, -4912 }, { -3280, -5200 },
			{ -3280, -5488 }, { -3280, -5776 }, { -3408, -5936 }, { -3696, -5936 }, { -3984, -5936 },
			{ -4272, -5936 }, { -4560, -5936 }, { -4848, -5936 }, { -5136, -5936 }, { -5424, -5936 },
			{ -5712, -6128 }, { -6064, -6160 }, { -6416, -6320 }, { -6448, -6608 }, { -6448, -6896 },
			{ -6448, -7184 }, { -6608, -7472 }, { -6896, -7728 }, { -7184, -7728 }, { -7472, -7728 },
			{ -7760, -7728 }, { -8048, -7728 }, { -8112, -7504 }, { -8112, -7216 }, { -8112, -6928 },
			{ -8112, -6640 }, { -8112, -6352 }, { -8112, -6064 }, { -8112, -5776 }, { -8112, -5488 }, { -8112, -5200 },
			{ -8112, -4912 }, { -8112, -4624 }, { -8112, -4336 }, { -8112, -4048 }, { -8112, -3760 },
			{ -8112, -3472 }, { -8112, -3184 }, { -8176, -2896 } };
	private static final float[][] ILLIDAN_PRISON_ROUTE = { { -4848, 528 }, { -4848, 240 }, { -4848, -48 },
			{ -4848, -336 }, { -4848, -624 }, { -4976, -912 }, { -5264, -1072 }, { -5552, -1136 },
			{ -5840, -1200 }, { -6128, -1200 }, { -6416, -1200 }, { -6704, -1200 }, { -6992, -1200 },
			{ -7280, -1072 }, { -7344, -784 }, { -7344, -496 }, { -7344, -208 }, { -7184, -80 },
			{ -6896, -80 }, { -6608, 144 }, { -6512, 432 }, { -6480, 720 }, { -6480, 1008 }, { -6768, 1296 },
			{ -7056, 1584 }, { -7280, 1872 }, { -7280, 2160 }, { -7280, 2448 }, { -7440, 2736 }, { -7472, 3024 },
			{ -7600, 3312 }, { -7600, 3400 }, { -7600, 3700 }, { -7600, 3990 }, { -7600, 4270 },
			{ -7600, 4550 }, { -7628, 4900 } };
	private static final int ILLIDAN_GUARD_TRIPWIRE = 31;
	private static final War3ID STARFALL = War3ID.fromString("AEsf");
	private static final List<String> PRISON_SUPPLIES = List.of("phea", "pghe", "shea", "rhe1", "rhe2", "rlif", "rde1", "rde2");
	private static final float[][] PRISON_FOUNTAIN_ROUTE = { { -4848, 496 }, { -4848, 208 }, { -4848, -80 },
			{ -4848, -368 }, { -4848, -656 }, { -4848, -944 }, { -4752, -1232 } };
	private static final float[][] PRISON_GAUNTLET_ROUTE = { { -4752, -1296 }, { -4752, -1584 }, { -4496, -1648 },
			{ -4208, -1712 }, { -4208, -1968 } };
	private static final Map<Integer, Integer> routeSteps = new HashMap<>();
	private static boolean carryoverVerified;
	private static boolean gateSplit;
	private static boolean lavaBridgeWasDown;
	private static boolean crossedLavaBridge;
	private static boolean bearWasLifted;
	private static boolean playedBearEncounter;
	private static boolean returnedToTyrande;
	private static boolean headingToSecondPlate;
	private static boolean pressureGateOpened;
	private static boolean prisonGateCrossed;
	private static boolean prisonWaygateEntered;
	private static boolean collectedPrisonWards;
	private static int prisonWardDeadline;
	private static final Map<Integer, Integer> prisonWardSteps = new HashMap<>();
	private static int healingScrollDeadline;
	private static final Map<Integer, Integer> healingScrollSteps = new HashMap<>();
	private static boolean healedAtPrisonFountain;
	private static boolean collectedPrisonGauntlets;
	private static CItem prisonGauntlets;
	private static boolean observingPrisonDamage;
	private static boolean victoryVerified;
	private static List<CDestructable> prisonPassageCrates = List.of();
	private static final Map<Integer, Integer> prisonSteps = new HashMap<>();
	private static final Map<Integer, Integer> fountainSteps = new HashMap<>();
	private static final Map<Integer, Integer> gauntletSteps = new HashMap<>();
	private static final Map<Integer, Integer> prisonRetreatSteps = new HashMap<>();
	private static CDestructable firstDenGate;
	private static CDestructable secondDenGate;
	private static int bearReportTick;
	private static boolean finished;
	private static CQuest openingDruids;
	private static List<CDestructable> entranceEggs = List.of();
	private static int orderTick;
	private static int reportTick;

	private NightElfDungeonOpeningProbe() {
	}

	static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit tyrande, final CUnit furion) {
		if (finished || tyrande == null || furion == null) return;
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (PLAYTHROUGH ? 90_000 : CROSS_TYRANDE_WAYGATE ? 70_000 : OPEN_TYRANDE_GATE ? 62_000
				: CROSS_LAVA_BRIDGE ? 50_000 : 34_000),
				"the original dungeon expedition stalled");
		if (PLAYTHROUGH && prisonWaygateEntered) {
			final CQuest illidan = globals.getGlobal("udg_QuestIllidan").visit(ObjectJassValueVisitor.getInstance());
			if (illidan != null && illidan.isCompleted()) {
				verifyPrisonVictory(ui, globals, tyrande, furion);
				return;
			}
		}
		final boolean bearCinematicPlaying = WATCH_BEAR_ENCOUNTER && crossedLavaBridge && Boolean.TRUE.equals(
				globals.getGlobal("udg_BearCinematicPlaying").visit(BooleanJassValueVisitor.getInstance()));
		if (WATCH_BEAR_ENCOUNTER && crossedLavaBridge && !bearWasLifted) {
			final CUnit bear = globals.getGlobal("udg_BearCinAssailant1").visit(ObjectJassValueVisitor.getInstance());
			bearWasLifted = bear != null && bear.isInvulnerable() && bear.getFlyHeight() > 0
					&& CAbilityCyclone.hasDispellableLift(bear);
			if (bear != null && tick >= bearReportTick && bearCinematicPlaying) {
				bearReportTick = tick + 24;
				final CUnit caster = globals.getGlobal("udg_DruidofTalon01").visit(ObjectJassValueVisitor.getInstance());
				if (caster != null) {
					final var cyclone = caster.getFirstAbilityOfType(CAbilityCyclone.class);
					final var activation = ExternStringMsgAbilityActivationReceiver.INSTANCE.reset();
					final var target = ExternStringMsgTargetCheckReceiver.<CWidget>getInstance().reset();
					if (cyclone != null) {
						cyclone.checkCanUse(game, caster, OrderIds.cyclone, activation);
						cyclone.checkCanTarget(game, caster, OrderIds.cyclone, bear, target);
					}
					System.out.printf("[NightElfDungeonProbe] Cyclone tick=%d caster %.0f,%.0f mana=%.0f order=%s spell=%s target=%s paused=%s bear %.0f,%.0f visible=%s life=%.0f invulnerable=%s%n",
							tick, caster.getX(), caster.getY(), caster.getMana(), caster.getCurrentOrder() == null ? 0 : caster.getCurrentOrder().getOrderId(),
							cyclone == null ? "missing" : activation.isUseOk() ? "ready" : activation.getExternStringKey(),
							target.getTarget() == null ? target.getExternStringKey() : "valid", caster.isPaused(), bear.getX(), bear.getY(),
							bear.isVisible(game, caster.getPlayerIndex()), bear.getLife(), bear.isInvulnerable());
				}
			}
		}
		if (bearCinematicPlaying || !ui.isUserControlEnabled() || game.isGamePaused() || tick < orderTick) return;
		check(!tyrande.isDead() && !furion.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "native defeat in the entrance");
		orderTick = tick + 16;
		final CQuest druids = globals.getGlobal("udg_QuestDruids").visit(ObjectJassValueVisitor.getInstance());
		if (FINISH_FURION_BRANCH && playedBearEncounter && druids != null && druids.isCompleted()) {
			if (!tyrande.isHidden() && tyrande.getPlayerIndex() == viewer.getLocalPlayerIndex()) {
				if (!returnedToTyrande) {
					returnedToTyrande = true;
					routeSteps.clear();
					System.out.println("[NightElfDungeonProbe] completed Furion's original quest and regained control of Tyrande");
				}
				if (prisonWaygateEntered) advanceToIllidan(viewer, ui, globals, tyrande);
				else if (OPEN_TYRANDE_GATE) openTyrandeGate(viewer, ui, globals, tyrande);
				else {
					finished = true;
					Gdx.app.exit();
				}
			}
			return;
		}
		if (!carryoverVerified) {
			final CGameCache cache = globals.getGlobal("bj_lastCreatedGameCache").visit(ObjectJassValueVisitor.getInstance());
			check(matchesEarnedHero(cache, "Tyrande", tyrande) && matchesEarnedHero(cache, "Furion", furion),
					"NightElf05 did not restore the earned heroes");
			carryoverVerified = true;
			openingDruids = druids;
			final Rectangle eggNest = globals.getGlobal("gg_rct_EggSatchels_01A").visit(ObjectJassValueVisitor.getInstance());
			entranceEggs = game.getDestructables().stream().filter(egg -> !egg.isDead()
					&& eggNest.contains(egg.getX(), egg.getY())).toList();
			System.out.println("[NightElfDungeonProbe] restored earned Tyrande and Furion after the original introduction");
		}
		if (!gateSplit && Boolean.TRUE.equals(globals.getGlobal("udg_GateClosedBoolean").visit(BooleanJassValueVisitor.getInstance()))) {
			if (druids != null && druids != openingDruids && tyrande.getX() < furion.getX() - 350 && furion.getY() < -6000) {
				check(entranceEggs.stream().anyMatch(CDestructable::isDead), "the native spider egg sequence did not break an egg");
				System.out.println("[NightElfDungeonProbe] the original dungeon gate split the heroes and replaced their quests");
				gateSplit = true;
				routeSteps.clear();
				if (!CROSS_LAVA_BRIDGE) {
					finished = true;
					Gdx.app.exit();
				}
			}
			return;
		}
		final CDestructable bridge = gateSplit
				? globals.getGlobal("udg_LavaBridge").visit(ObjectJassValueVisitor.getInstance()) : null;
		if (gateSplit) {
			check(bridge != null, "the original lava bridge is missing");
			lavaBridgeWasDown |= bridge.isDead();
			if (!crossedLavaBridge && lavaBridgeWasDown && !bridge.isDead() && furion.getX() > 6480 && furion.getY() < -6300) {
				crossedLavaBridge = true;
				System.out.println("[NightElfDungeonProbe] Furion raised and crossed the original lava bridge");
				if (!WATCH_BEAR_ENCOUNTER) {
					finished = true;
					Gdx.app.exit();
					return;
				}
			}
			if (crossedLavaBridge && !playedBearEncounter
					&& Boolean.TRUE.equals(globals.getGlobal("udg_BearCinPlayed").visit(BooleanJassValueVisitor.getInstance()))) {
				check(bearWasLifted, "the authored Cyclone order did not lift a Druid of the Claw");
				System.out.println("[NightElfDungeonProbe] played the original Druid of the Claw encounter");
				playedBearEncounter = true;
				routeSteps.clear();
				if (FINISH_FURION_BRANCH) {
					firstDenGate = findDenGate(viewer, 6464, 3904);
					secondDenGate = findDenGate(viewer, 7168, 6272);
					check(firstDenGate != null && secondDenGate != null, "the authored Druid den gates are missing");
				}
				else {
					finished = true;
					Gdx.app.exit();
				}
				return;
			}
		}
		final boolean headingToDens = FINISH_FURION_BRANCH && playedBearEncounter;
		final float[][] route = headingToDens ? DRUID_DENS_ROUTE : gateSplit ? LAVA_BRIDGE_ROUTE : ENTRANCE_ROUTE;
		final CDestructable denGate = headingToDens && !firstDenGate.isDead() ? firstDenGate
				: headingToDens && !secondDenGate.isDead() ? secondDenGate : null;
		final int end = denGate == firstDenGate && denGate != null ? FIRST_DEN_GATE_APPROACH
				: denGate == secondDenGate && denGate != null ? SECOND_DEN_GATE_APPROACH
				: bridge != null && bridge.isDead() ? 15
				: gateSplit && !WATCH_BEAR_ENCOUNTER ? BRIDGE_FAR_SIDE : route.length - 1;
		final var player = game.getPlayer(furion.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final var army = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId() && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		final CPlayer druidsPlayer = headingToDens ? globals.getGlobal("udg_P_Druids").visit(ObjectJassValueVisitor.getInstance()) : null;
		final var bears = druidsPlayer != null ? game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& unit.getPlayerIndex() == druidsPlayer.getId() && "edcm".equals(unit.getTypeId().toString()) && unit.isVisible(game, player.getId())).toList()
				: List.<CUnit>of();
		if (!gateSplit) NightElfEscapePlaythroughProbe.prepareSearingArrows(viewer, orders, tyrande);
		if (PLAYTHROUGH && !gateSplit && healingScrollDeadline == 0
				&& tyrande.distanceSquaredNoCollision(-16, 3504) < 260 * 260) healingScrollDeadline = tick + 1000;
		if (PLAYTHROUGH && !gateSplit && healingScrollDeadline > 0 && CampaignPlaythroughProbe.hasItem(tyrande, "shea")) {
			healingScrollDeadline = -1;
			System.out.println("[NightElfDungeonProbe] collected the original spider egg's Scroll of Healing");
		}
		final boolean collectsScroll = !gateSplit && healingScrollDeadline > tick;
		final CDestructable scrollEgg = collectsScroll
				? globals.getGlobal("gg_dest_DTes_0102").visit(ObjectJassValueVisitor.getInstance()) : null;
		for (final CUnit unit : army) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, 540))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			final CUnit bear = bears.stream().filter(target -> unit.canReach(target, 610))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			final boolean hero = unit == tyrande || unit == furion;
			if (hero && unit.getLife() < unit.getMaximumLife() * (headingToDens ? 0.8f : 0.67f)) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")) continue;
				final int step = routeSteps.getOrDefault(unit.getHandleId(), 0);
				final float[] refuge = route[Math.max(0, step - 1)];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			if (bear != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.cyclone, bear)) continue;
			if (unit == furion && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.entanglingroots,
					enemy == null ? bear : enemy)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (collectsScroll && scrollEgg != null) {
				if (!scrollEgg.isDead() && unit.canReach(scrollEgg, 430)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, scrollEgg)) continue;
				if (unit == tyrande && scrollEgg.isDead()) {
					final var scroll = game.getItems().stream().filter(item -> "shea".equals(item.getTypeId().toString())
							&& !item.isDead() && !item.isHidden() && item.getContainedInventory() == null
							&& item.distanceSquaredNoCollision(scrollEgg) < 96 * 96).findFirst().orElse(null);
					if (CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.smart, scroll)) continue;
				}
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, HEALING_SCROLL_ROUTE,
						HEALING_SCROLL_ROUTE.length - 1, 105, healingScrollSteps);
				continue;
			}
			if (denGate != null && unit.canReach(denGate, 480)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, denGate)) continue;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 150, routeSteps);
		}
		ui.getCameraManager().setTarget(furion.getX(), furion.getY());
		if (tick >= reportTick) {
			reportTick = tick + 400;
			System.out.printf("[NightElfDungeonProbe] Furion %.0f,%.0f life=%.0f waypoint=%d army=%d%n", furion.getX(),
					furion.getY(), furion.getLife(), routeSteps.getOrDefault(furion.getHandleId(), 0), army.size());
		}
	}

	private static void openTyrandeGate(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit tyrande) {
		final var game = viewer.simulation;
		final boolean first = Boolean.TRUE.equals(
				globals.getGlobal("udg_PressurePlate01Activated").visit(BooleanJassValueVisitor.getInstance()));
		final boolean second = Boolean.TRUE.equals(
				globals.getGlobal("udg_PressurePlate02Activated").visit(BooleanJassValueVisitor.getInstance()));
		final CDestructable gate = globals.getGlobal("gg_dest_LTe3_0181").visit(ObjectJassValueVisitor.getInstance());
		check(gate != null, "Tyrande's original pressure plate gate is missing");
		if (!pressureGateOpened && first && second && gate.isDead()) {
			pressureGateOpened = true;
			routeSteps.clear();
			prisonPassageCrates = game.getDestructables().stream().filter(crate -> "LTcr".equals(crate.getTypeId().toString())
					&& crate.getX() < -7900 && crate.getX() > -8400 && crate.getY() > -6600 && crate.getY() < -6300).toList();
			System.out.println("[NightElfDungeonProbe] activated both original pressure plates and opened Tyrande's gate");
			if (!CROSS_TYRANDE_WAYGATE) {
				finished = true;
				Gdx.app.exit();
				return;
			}
		}
		if (first && !headingToSecondPlate) {
			final CDestructable passage = globals.getGlobal("gg_dest_DTg3_1295").visit(ObjectJassValueVisitor.getInstance());
			check(passage != null && passage.isDead(), "the original southern passage did not open");
			headingToSecondPlate = true;
			routeSteps.clear();
			System.out.println("[NightElfDungeonProbe] activated Tyrande's northern pressure plate");
		}
		if (pressureGateOpened && tyrande.getX() < -6080 && Math.abs(tyrande.getY() + 6160) < 280 && !prisonGateCrossed) {
			prisonGateCrossed = true;
			System.out.println("[NightElfDungeonProbe] Tyrande crossed the original pressure plate gate");
		}
		if (pressureGateOpened && tyrande.distanceSquaredNoCollision(-4864, 736) < 380 * 380) {
			check(prisonGateCrossed, "Tyrande reached the prison without crossing the pressure plate gate");
			check(prisonPassageCrates.stream().anyMatch(CDestructable::isDead), "the expedition did not clear the authored crate passage");
			System.out.println("[NightElfDungeonProbe] Tyrande travelled through the original prison waygate");
			prisonWaygateEntered = true;
			if (!PLAYTHROUGH) {
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		final float[][] route = pressureGateOpened ? PRISON_WAYGATE_ROUTE
				: headingToSecondPlate ? SECOND_PRESSURE_PLATE : FIRST_PRESSURE_PLATE;
		final var player = game.getPlayer(tyrande.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final var army = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId() && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.getCurrentAttacks().isEmpty()).toList();
		NightElfEscapePlaythroughProbe.prepareSearingArrows(viewer, orders, tyrande);
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player.getId())).toList();
		final CUnit prisonWaygate = pressureGateOpened ? findPrisonWaygate(viewer) : null;
		final CDestructable wardCrate = globals.getGlobal("gg_dest_LTcr_0339").visit(ObjectJassValueVisitor.getInstance());
		if (PLAYTHROUGH && !collectedPrisonWards && CampaignPlaythroughProbe.hasItem(tyrande, "whwd")) {
			collectedPrisonWards = true;
			System.out.println("[NightElfDungeonProbe] collected the original dungeon crate's Healing Wards");
		}
		final boolean wardDetour = PLAYTHROUGH && headingToSecondPlate && !pressureGateOpened && !collectedPrisonWards;
		if (wardDetour && prisonWardDeadline == 0 && tyrande.getLife() >= tyrande.getMaximumLife() * 0.62f) {
			prisonWardDeadline = game.getGameTurnTick() + 1050;
		}
		final boolean goingToWardCrate = wardDetour && game.getGameTurnTick() < prisonWardDeadline;
		for (final CUnit unit : army) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, 500))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit == tyrande && unit.getLife() < unit.getMaximumLife() * 0.52f) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "shea")) continue;
				if (enemy == null) continue;
				final float[] refuge = route[Math.max(0, routeSteps.getOrDefault(unit.getHandleId(), 0) - 2)];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) {
				if (wardDetour && prisonWardDeadline == 0) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, -4944, -4464);
					continue;
				}
				if (goingToWardCrate && wardCrate != null && unit.canReach(wardCrate, 660)) {
					if (!wardCrate.isDead() && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, wardCrate)) continue;
					final var item = game.getItems().stream().filter(supply -> "whwd".equals(supply.getTypeId().toString())
							&& !supply.isDead() && !supply.isHidden() && supply.getContainedInventory() == null
							&& supply.distanceSquaredNoCollision(wardCrate) < 96 * 96).findFirst().orElse(null);
					if (unit == tyrande && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, item)) continue;
				}
				if (goingToWardCrate) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, PRISON_WARD_CRATE_ROUTE,
							PRISON_WARD_CRATE_ROUTE.length - 1, 110, prisonWardSteps);
					continue;
				}
				if (pressureGateOpened && unit.getX() < -7856 && unit.getY() < -6200) {
					final CDestructable crate = prisonPassageCrates.stream()
							.filter(target -> !target.isDead() && unit.canReach(target, 550))
							.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
					if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, crate)) continue;
				}
				if (prisonWaygate != null && unit.canReach(prisonWaygate, 440)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, prisonWaygate)) continue;
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, route.length - 1, 105, routeSteps);
			}
		}
		ui.getCameraManager().setTarget(tyrande.getX(), tyrande.getY());
		final int tick = game.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 400;
			System.out.printf("[NightElfDungeonProbe] Tyrande %.0f,%.0f life=%.0f waypoint=%d plates=%s/%s escort=%d%n",
					tyrande.getX(), tyrande.getY(), tyrande.getLife(), routeSteps.getOrDefault(tyrande.getHandleId(), 0),
					first, second, army.size());
		}
	}

	private static CUnit findPrisonWaygate(final War3MapViewer viewer) {
		return viewer.simulation.getUnits().stream().filter(unit -> "nwgt".equals(unit.getTypeId().toString())
				&& unit.distanceSquaredNoCollision(-8192, -2816) < 32 * 32).findFirst().orElse(null);
	}

	private static void advanceToIllidan(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit tyrande) {
		final var game = viewer.simulation;
		final var player = game.getPlayer(tyrande.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final CQuestItem locate = globals.getGlobal("udg_QuestIllidanReqLocate").visit(ObjectJassValueVisitor.getInstance());
		final boolean guardsDiscovered = locate != null && locate.isCompleted();
		final int end = guardsDiscovered ? ILLIDAN_PRISON_ROUTE.length - 1 : ILLIDAN_GUARD_TRIPWIRE;
		final var army = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId() && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		final CUnit waygate = findPrisonWaygate(viewer);
		final List<CUnit> guards = globals.getGlobal("udg_IllidanGuards").visit(ObjectJassValueVisitor.getInstance());
		if (!observingPrisonDamage) {
			observingPrisonDamage = true;
			tyrande.addDamageTakenListener((sim, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
				if (actual > 0 && target.getLife() < target.getMaximumLife() * 0.35f) {
					System.out.printf("[NightElfDungeonProbe] Tyrande hurt near %.0f,%.0f by %s life=%.0f%n",
							target.getX(), target.getY(), attacker == null ? type : attacker.getTypeId(), target.getLife());
				}
			});
		}
		final var canLearn = new BooleanAbilityActivationReceiver();
		NightElfEscapePlaythroughProbe.prepareSearingArrows(viewer, orders, tyrande);
		tyrande.getHeroData().checkCanUse(game, tyrande, STARFALL.getValue(), canLearn);
		if (canLearn.isOk()) orders.issueImmediateOrder(tyrande.getHandleId(), tyrande.getHeroData().getHandleId(), STARFALL.getValue(), false);
		if (!healedAtPrisonFountain && tyrande.getLife() >= tyrande.getMaximumLife() * 0.96f
				&& tyrande.distanceSquaredNoCollision(-4752, -1232) < 150 * 150
				&& army.stream().filter(unit -> unit != tyrande && unit.canReach(tyrande, 450)).limit(2).count() >= Math.min(2, army.size() - 1)) {
			healedAtPrisonFountain = true;
			System.out.println("[NightElfDungeonProbe] recovered Tyrande and her escort at the original prison fountain");
		}
		if (healedAtPrisonFountain && !collectedPrisonGauntlets && collectPrisonGauntlets(viewer, orders, tyrande)) {
			ui.getCameraManager().setTarget(tyrande.getX(), tyrande.getY());
			return;
		}
		final CUnit priority = enemies.stream().filter(target -> tyrande.canReach(target, 730))
				.min(Comparator.comparing((CUnit target) -> !guardsDiscovered || guards == null || !guards.contains(target))
						.thenComparingDouble(CUnit::getLife)).orElse(null);
		for (final CUnit unit : army) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit.getY() < -2100) {
				if (waygate == null || !unit.canReach(waygate, 440)
						|| !CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, waygate)) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, PRISON_WAYGATE_ROUTE,
							PRISON_WAYGATE_ROUTE.length - 1, 100, routeSteps);
				}
				continue;
			}
			if (!healedAtPrisonFountain) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, PRISON_FOUNTAIN_ROUTE,
						PRISON_FOUNTAIN_ROUTE.length - 1, 93, fountainSteps);
				continue;
			}
			final CUnit enemy = priority != null && unit.canReach(priority, 820) ? priority : enemies.stream()
					.filter(target -> unit.canReach(target, 520)).min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			final int handle = unit.getHandleId();
			final float health = unit.getLife() / unit.getMaximumLife();
			final boolean retreating = prisonRetreatSteps.containsKey(handle);
			final boolean atFountain = unit.distanceSquaredNoCollision(-4752, -1232) < 235 * 235;
			if (unit == tyrande && enemy != null && health < (atFountain ? 0.92f : 0.78f)
					&& game.getUnits().stream().noneMatch(ward -> "ohwd".equals(ward.getTypeId().toString())
							&& !ward.isDead() && ward.getPlayerIndex() == player.getId() && unit.canReach(ward, 460))) {
				final var inventory = unit.getInventoryData();
				for (int slot = 0; slot < inventory.getItemCapacity(); slot++) {
					final var item = inventory.getItemInSlot(slot);
					if (item != null && "whwd".equals(item.getTypeId().toString())) {
						final double away = enemy.angleTo(unit);
						final float wardX = unit.getX() + (float) (Math.cos(away) * 112);
						final float wardY = unit.getY() + (float) (Math.sin(away) * 112);
						if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.itemuse00 + slot, wardX, wardY)) {
							return;
						}
					}
				}
			}
			if (atFountain && enemy != null && unit.canReach(enemy, 380) && health > 0.34f
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (health < (retreating ? 0.89f : unit == tyrande ? 0.69f : 0.68f)) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "shea")) continue;
				if (atFountain
						&& (unit.getCurrentOrder() != null && unit.getCurrentOrder().getOrderId() == OrderIds.ambush
							|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.ambush))) continue;
				int retreatStep = prisonRetreatSteps.getOrDefault(handle, Math.max(5, prisonSteps.getOrDefault(handle, 5) - 1));
				float[] refuge = ILLIDAN_PRISON_ROUTE[retreatStep];
				if (retreatStep > 5 && unit.distanceSquaredNoCollision(refuge[0], refuge[1]) < 150 * 150) retreatStep--;
				prisonRetreatSteps.put(handle, retreatStep);
				refuge = retreatStep == 5 ? PRISON_FOUNTAIN_ROUTE[PRISON_FOUNTAIN_ROUTE.length - 1]
						: ILLIDAN_PRISON_ROUTE[retreatStep];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			prisonRetreatSteps.remove(handle);
			if (unit == tyrande) {
				if (health > 0.80f && enemies.stream().filter(target -> tyrande.canReach(target, 600)).limit(2).count() == 2
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.starfall)) continue;
				if (enemy == null) {
					final var supply = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
							&& item.getContainedInventory() == null && PRISON_SUPPLIES.contains(item.getTypeId().toString())
							&& unit.distanceSquaredNoCollision(item) < 224 * 224)
							.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
					if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, supply)) continue;
				}
			}
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			final CUnit guard = guardsDiscovered && guards != null ? guards.stream()
					.filter(target -> !target.isDead() && !target.isHidden() && unit.canReach(target, 620))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null) : null;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, guard)) continue;
			final int escortEnd = unit == tyrande ? end : Math.min(end,
					Math.max(5, prisonSteps.getOrDefault(tyrande.getHandleId(), 5) - 1));
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, ILLIDAN_PRISON_ROUTE, escortEnd, 100, prisonSteps);
		}
		ui.getCameraManager().setTarget(tyrande.getX(), tyrande.getY());
		final int tick = game.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 420;
			System.out.printf("[NightElfDungeonProbe] Illidan's prison %.0f,%.0f life=%.0f waypoint=%d discovered=%s escort=%d%n",
					tyrande.getX(), tyrande.getY(), tyrande.getLife(), prisonSteps.getOrDefault(tyrande.getHandleId(), 0),
					guardsDiscovered, army.size());
			if (guardsDiscovered && guards != null) {
				for (final CUnit guard : guards) {
					if (!guard.isDead()) System.out.printf("[NightElfDungeonProbe] prison guard %s %.0f,%.0f life=%.0f%n",
							guard.getTypeId(), guard.getX(), guard.getY(), guard.getLife());
				}
			}
		}
	}

	private static boolean collectPrisonGauntlets(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande) {
		if (prisonGauntlets == null) {
			prisonGauntlets = viewer.simulation.getItems().stream()
					.filter(item -> "rst1".equals(item.getTypeId().toString())
							&& item.distanceSquaredNoCollision(-4274, -1983) < 160 * 160)
					.findFirst().orElse(null);
		}
		final CItem gauntlets = prisonGauntlets;
		if (gauntlets == null) return false;
		if (gauntlets.getContainedUnit() == tyrande) {
			collectedPrisonGauntlets = true;
			System.out.println("[NightElfDungeonProbe] equipped Tyrande with the original prison Gauntlets of Ogre Strength");
			return false;
		}
		final var inventory = tyrande.getInventoryData();
		if (gauntlets.getContainedInventory() != null || gauntlets.isDead() || gauntlets.isHidden()
				|| inventory == null || java.util.stream.IntStream.range(0, inventory.getItemCapacity())
						.noneMatch(slot -> inventory.getItemInSlot(slot) == null)) return false;
		if (!tyrande.canReach(gauntlets, 130)
				|| !CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.smart, gauntlets)) {
			CampaignPlaythroughProbe.followRoute(viewer, orders, tyrande, PRISON_GAUNTLET_ROUTE,
					PRISON_GAUNTLET_ROUTE.length - 1, 100, gauntletSteps);
		}
		return true;
	}

	private static void verifyPrisonVictory(final MeleeUI ui, final GlobalScope globals, final CUnit tyrande, final CUnit furion) {
		if (!victoryVerified) {
			final CGameCache cache = globals.getGlobal("bj_lastCreatedGameCache").visit(ObjectJassValueVisitor.getInstance());
			check(matchesEarnedHero(cache, "NightElf07", "Tyrande", tyrande)
					&& matchesEarnedHero(cache, "NightElf07", "Furion", furion),
					"the original chapter did not store both earned heroes for NightElf07");
			victoryVerified = true;
			System.out.println("[NightElfDungeonProbe] defeated the original prison guards, freed Illidan, and verified both stored heroes");
		}
		HumanCampaignPlaythroughProbe.continueVictory(ui);
	}

	static void verifyNextChapter() {
		if (finished) return;
		check(victoryVerified, "NightElf06 loaded without native prison victory");
		System.out.println("[NightElfDungeonProbe] entered the original NightElf06 after freeing Illidan");
		finished = true;
		if (!NightElfOpeningPlaythroughProbe.CHAIN) Gdx.app.exit();
	}

	private static CDestructable findDenGate(final War3MapViewer viewer, final float x, final float y) {
		return viewer.simulation.getDestructables().stream()
				.filter(gate -> gate.getTypeId().toString().startsWith("DTg") && gate.distanceSquaredNoCollision(x, y) < 32 * 32)
				.findFirst().orElse(null);
	}

	private static boolean matchesEarnedHero(final CGameCache cache, final String name, final CUnit hero) {
		return matchesEarnedHero(cache, "NightElf05", name, hero);
	}

	private static boolean matchesEarnedHero(final CGameCache cache, final String chapter, final String name, final CUnit hero) {
		final var stored = cache == null ? null : cache.getStoredUnit(chapter, name);
		return stored != null && CampaignPlaythroughProbe.heroProperties(stored)
				.equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero)));
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf dungeon opening audit: " + message);
	}
}
