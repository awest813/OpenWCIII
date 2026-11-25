package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;

public final class HumanExpansionOpeningPlaythroughProbe {

	private static final boolean ENABLED = Boolean.getBoolean("warsmash.humanExpansionOpeningAudit");
	private static final boolean BOATS = Boolean.getBoolean("warsmash.humanExpansionOpeningBoats");
	private static final boolean LANDING = Boolean.getBoolean("warsmash.humanExpansionOpeningLanding");
	private static final boolean CACHE = Boolean.getBoolean("warsmash.humanExpansionOpeningCache");
	private static final boolean SECOND_OBSERVATORY = Boolean.getBoolean("warsmash.humanExpansionOpeningSecondObservatory");
	private static final boolean ALL_OBSERVATORIES = Boolean.getBoolean("warsmash.humanExpansionOpeningAllObservatories");
	private static final War3ID PEASANT = War3ID.fromString("hpea");
	private static final War3ID FLAME_STRIKE = War3ID.fromString("AHfs");
	private static final War3ID TINY_CASTLE = War3ID.fromString("tgrh");
	private static final War3ID TINY_BARRACKS = War3ID.fromString("tbar");
	private static final War3ID CACHE_GATE = War3ID.fromString("DTg7");
	private static final War3ID TOWN_HALL = War3ID.fromString("htow");
	private static final War3ID BARRACKS = War3ID.fromString("hbar");
	private static final War3ID FARM = War3ID.fromString("hhou");
	private static final War3ID LUMBER_MILL = War3ID.fromString("hlum");
	private static final War3ID SCOUT_TOWER = War3ID.fromString("hwtw");
	private static final War3ID GUARD_TOWER = War3ID.fromString("hgtw");
	private static final float[][] CACHE_ROUTE = {
			{ 7216, 2864 }, { 7536, 2480 }, { 8112, 2256 }, { 8624, 1776 }, { 8368, 1296 }, { 8048, 784 },
			{ 7472, 592 }, { 6896, 528 }, { 6608, 656 }, { 6448, 944 }, { 6160, 1232 }, { 5872, 1360 }, { 5584, 1648 }
	};
	private static final float[][] SHORE_ROUTE = {
			{ 7216, 2864 }, { 7440, 2576 }, { 7728, 2384 }, { 8016, 2256 }, { 8304, 2160 }, { 8592, 1872 },
			{ 8624, 1584 }, { 8432, 1360 }, { 8144, 848 }, { 7856, 592 }, { 7280, 528 }, { 6992, 240 },
			{ 6704, -48 }, { 6608, -336 }, { 6736, -624 }, { 7312, -1008 }, { 7888, -1584 },
			{ 8368, -2160 }, { 8432, -2736 }, { 8336, -3024 }, { 8048, -3120 }, { 7472, -3120 },
			{ 6896, -3120 }, { 6320, -3120 }, { 6032, -3152 }, { 5744, -3440 }, { 5168, -3888 },
			{ 4592, -4080 }, { 4016, -3856 }, { 3440, -3408 }, { 3344, -2832 }, { 3312, -2544 }
	};
	private static final float[][] SUPPLY_CACHE_APPROACH = { { 6032, -3216 }, { 6128, -3504 }, { 6128, -3792 },
			{ 6128, -4080 }, { 5968, -4368 }, { 5968, -4656 }, { 6160, -4944 }, { 6400, -5184 } };
	private static final float[][] LAKE_ROUTE = {
			{ 3440, -2384 }, { 3152, -2320 }, { 2864, -2128 }, { 2576, -2000 }, { 2000, -1808 },
			{ 1424, -1712 }, { 1136, -1424 }, { 848, -1136 }, { 560, -848 }, { 432, -560 }, { 304, -272 },
			{ 304, 16 }, { 176, 304 }, { 16, 592 }, { -272, 880 }, { -560, 1168 }, { -720, 1456 },
			{ -720, 1744 }, { -784, 2032 }, { -784, 2320 }, { -656, 2608 }, { -368, 2800 }, { 208, 2800 }, { 496, 2800 }
	};
	private static final float LANDING_X = 656;
	private static final float LANDING_Y = 2736;
	private static final float[][] MINE_APPROACH = { { 688, 2736 }, { 976, 2736 }, { 1264, 2736 }, { 1552, 2512 },
			{ 1840, 2224 }, { 2128, 1936 }, { 2416, 1712 }, { 2704, 1424 }, { 2992, 1136 } };
	private static final float[][] TOWN_HALL_SITES = { { 3072, 704 }, { 3072, 768 }, { 3008, 832 }, { 2944, 960 } };
	private static final float[][] BARRACKS_SITES = { { 3072, 1056 }, { 2944, 1088 }, { 2944, 448 }, { 3200, 512 } };
	private static final float[][] FARM_SITES = { { 2816, 960 }, { 2816, 1184 }, { 3200, 1120 }, { 3008, 1216 } };
	private static final float[][] MILL_SITES = { { 3304, 1176 }, { 3360, 928 }, { 2944, 1264 } };
	private static final float[][] TOWER_SITES = { { 3008, 1152 }, { 3200, 1120 }, { 2880, 1056 }, { 3264, 960 } };
	private static final float[] MINE_REFUGE = { 3392, 640 };
	private static final float[][] RETURN_TO_FERRY = { { 2992, 1136 }, { 2704, 1424 }, { 2416, 1712 },
			{ 2128, 1936 }, { 1840, 2224 }, { 1552, 2512 }, { 1264, 2736 }, { 976, 2736 }, { 688, 2736 } };
	private static final float[][] THIRD_FERRY_ROUTE = { { 432, 2864 }, { 208, 2864 }, { -48, 2864 }, { -304, 2864 },
			{ -560, 2864 }, { -816, 2864 }, { -976, 2960 }, { -976, 3216 }, { -976, 3472 },
			{ -976, 3728 }, { -976, 3984 }, { -976, 4240 } };
	private static final float[][] THIRD_GROUND_ROUTE = { { -1040, 4336 }, { -1040, 4592 }, { -1040, 4848 },
			{ -1072, 5072 }, { -1072, 5328 }, { -1328, 5328 }, { -1584, 5328 }, { -1776, 5392 },
			{ -1968, 5456 }, { -2192, 5488 }, { -2448, 5488 }, { -2704, 5488 }, { -2960, 5488 },
			{ -3216, 5488 }, { -3472, 5488 }, { -3728, 5488 } };
	private static final Map<Integer, Integer> shoreWaypoints = new HashMap<>();
	private static final Map<Integer, Integer> mineWaypoints = new HashMap<>();
	private static final Map<Integer, Integer> thirdWaypoints = new HashMap<>();
	private static boolean repairIssued;
	private static boolean openingObserved;
	private static boolean firstRepaired;
	private static boolean castleCollected;
	private static boolean supplyCacheCollected;
	private static boolean approachingSupplyCache;
	private static boolean boatsReceived;
	private static boolean expeditionAboard;
	private static boolean expeditionLanded;
	private static boolean harvestingReported;
	private static boolean kaelRecovering;
	private static boolean secondRepairIssued;
	private static boolean repairWorkerGatheringLumber;
	private static boolean secondObservatoryVerified;
	private static boolean thirdExpeditionReady;
	private static boolean thirdExpeditionAboard;
	private static boolean thirdExpeditionLanded;
	private static int landingLumber;
	private static CUnit expeditionWorker;
	private static final List<CUnit> expeditionEscorts = new ArrayList<>();
	private static List<CUnit> repairEscorts = List.of();
	private static List<CUnit> thirdEscorts = List.of();
	private static boolean finished;
	private static int nextOrder;
	private static int nextReport;
	private static int nextCinematicReport;
	private static int nextThirdSupplyOrder;

	private HumanExpansionOpeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final GlobalScope globals = game.getGlobalScope();
		final CQuest survey = object(globals, "udg_QuestObservatory");
		final CUnit kael = object(globals, "udg_Kael");
		final CUnit observatory = object(globals, "udg_Observatory01");
		final int tick = game.getGameTurnTick();
		final boolean thirdRepaired = ALL_OBSERVATORIES
				&& globals.getGlobal("udg_Observatory03Repaired").visit(BooleanJassValueVisitor.getInstance());
		if (tick >= (ALL_OBSERVATORIES ? 120_000 : SECOND_OBSERVATORY ? 80_000 : LANDING ? 48_000 : BOATS ? 28_000 : 18_000)
				|| !thirdRepaired && globals.getGlobal("udg_GameOver").visit(BooleanJassValueVisitor.getInstance())
				|| "Defeat".equals(ui.getScoreDialogTitle())) {
			final long structures = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == 0 && unit.isBuilding() && !unit.isDead()).count();
			check(false, "Misconceptions ended at tick " + tick + " with Kael life " + (kael == null ? 0 : kael.getLife())
					+ ", repair worker life " + (expeditionWorker == null ? 0 : expeditionWorker.getLife()) + ", and " + structures + " structures");
		}
		if (thirdRepaired) {
			final CUnit third = object(globals, "udg_Observatory03");
			check(thirdExpeditionLanded && third.getLife() >= 2499 && third.isInvulnerable(), "the third Observatory did not finish its native repair");
			if (survey != null && survey.isCompleted() && "Victory".equals(ui.getScoreDialogTitle())) {
				System.out.println("[HumanExpansionOpeningProbe] repaired all three original Observatories and reached the native victory");
				finish();
			}
			return;
		}
		if (firstRepaired && kael != null && !ui.isUserControlEnabled() && tick >= nextCinematicReport) {
			nextCinematicReport = tick + 800;
			final CUnit firstBoat = object(globals, "udg_NagaBoat01");
			final CUnit secondBoat = object(globals, "udg_NagaBoat02");
			System.out.printf("[HumanExpansionOpeningProbe] shore cinematic at tick=%d Kael=%.0f,%.0f transports=%s,%s%n",
					tick, kael.getX(), kael.getY(), firstBoat != null, secondBoat != null);
		}
		if (survey == null || kael == null || observatory == null || !ui.isUserControlEnabled() || game.isGamePaused()
				|| !boatsReceived && kael.isPaused() || tick < nextOrder) return;
		nextOrder = tick + 20;
		check(!kael.isDead() && !observatory.isDead(), "Kael or the first Observatory died");
		if (!openingObserved && survey.isDiscovered()) {
			openingObserved = true;
			System.out.println("[HumanExpansionOpeningProbe] watched Kael's original introduction and received the Observatory quest");
		}
		if (!firstRepaired && globals.getGlobal("udg_Observatory01Repaired").visit(BooleanJassValueVisitor.getInstance())) {
			if (!openingObserved) return;
			check(repairIssued && observatory.getLife() >= 2499 && observatory.isInvulnerable(),
					"the native first Observatory repair milestone did not complete");
			System.out.println("[HumanExpansionOpeningProbe] repaired the first original Observatory and triggered its reveal");
			firstRepaired = true;
			if (!BOATS) finish();
			return;
		}
		if (firstRepaired) {
			if (thirdExpeditionReady) continueThirdExpedition(viewer, ui, globals, kael);
			else if (expeditionLanded) repairSecondObservatory(viewer, ui, globals, kael);
			else advanceToShore(viewer, ui, globals, kael);
			return;
		}
		final int player = kael.getPlayerIndex();
		final CUnit peasant = game.getUnits().stream().filter(unit -> player == unit.getPlayerIndex() && !unit.isDead()
				&& !unit.isHidden() && PEASANT.equals(unit.getTypeId()))
				.min(Comparator.comparingDouble(observatory::distanceSquaredNoCollision)).orElse(null);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (peasant != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, peasant, OrderIds.repair, observatory)) {
			repairIssued = true;
		}
		if (tick >= nextReport) {
			nextReport = tick + 800;
			final CAbilityHumanRepair repair = peasant == null ? null : peasant.getFirstAbilityOfType(CAbilityHumanRepair.class);
			final var receiver = ExternStringMsgTargetCheckReceiver.<CWidget>getInstance().reset();
			if (repair != null) repair.checkCanTarget(game, peasant, OrderIds.repair, observatory, receiver);
			System.out.printf("[HumanExpansionOpeningProbe] Observatory life=%.0f lumber=%d worker=%s repairOrder=%s%n",
					observatory.getLife(), game.getPlayer(player).getLumber(),
					peasant == null ? "none" : Math.round(peasant.getX()) + "," + Math.round(peasant.getY()),
					repair == null ? "unavailable" : receiver.getTarget() == null ? receiver.getExternStringKey() : "accepted");
		}
	}

	private static void advanceToShore(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit kael) {
		final var game = viewer.simulation;
		final int player = kael.getPlayerIndex();
		final CUnit firstBoat = object(globals, "udg_NagaBoat01");
		final CUnit secondBoat = object(globals, "udg_NagaBoat02");
		if (firstBoat != null && secondBoat != null && firstBoat.getPlayerIndex() == player && secondBoat.getPlayerIndex() == player
				&& !firstBoat.isPaused() && !secondBoat.isPaused() && !firstBoat.isHidden() && !secondBoat.isHidden()) {
			check(firstBoat.getCargoData() != null && secondBoat.getCargoData() != null && !firstBoat.isDead() && !secondBoat.isDead(),
					"the original cinematic did not leave two usable transports");
			if (!boatsReceived) {
				boatsReceived = true;
				System.out.println("[HumanExpansionOpeningProbe] reached the original shore, watched Vashj's cinematic and received both transports");
			}
			if (LANDING) sailToSecondObservatory(viewer, ui, kael, secondBoat, player);
			else finish();
			return;
		}
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final var owner = game.getPlayer(player);
		final CItem castle = CACHE && !castleCollected ? game.getItems().stream()
				.filter(item -> TINY_CASTLE.equals(item.getTypeId()) && !item.isDead())
				.findFirst().orElse(null) : null;
		if (castle != null && castle.getContainedUnit() == kael) {
			castleCollected = true;
			shoreWaypoints.clear();
			System.out.println("[HumanExpansionOpeningProbe] collected the original Tiny Castle from the hidden cache");
		}
		final boolean seekingCastle = CACHE && !castleCollected;
		final boolean seekingSupplies = SECOND_OBSERVATORY && castleCollected && !supplyCacheCollected;
		final CDestructable supplyCrate = seekingSupplies ? object(globals, "gg_dest_LTbx_4106") : null;
		final CDestructable supplyApproachTree = approachingSupplyCache && !supplyCacheCollected
				? game.getDestructables().stream().filter(tree -> !tree.isDead() && tree.getX() == 6208 && tree.getY() == -5056
						&& tree.getDestType().getTargetedAs().contains(CTargetType.TREE)).findFirst().orElse(null) : null;
		if (seekingSupplies && !approachingSupplyCache && kael.canReach(6032, -3152, 270)) {
			approachingSupplyCache = true;
			shoreWaypoints.clear();
		}
		final CItem barracksItem = seekingSupplies ? game.getItems().stream()
				.filter(item -> !item.isDead() && TINY_BARRACKS.equals(item.getTypeId())).findFirst().orElse(null) : null;
		final CItem cacheGold = approachingSupplyCache && !supplyCacheCollected ? game.getItems().stream()
				.filter(item -> !item.isDead() && "gold".equals(item.getTypeId().toString())
						&& item.distanceSquaredNoCollision(6400, -5280) < 720 * 720).findFirst().orElse(null) : null;
		if (seekingSupplies && barracksItem != null && barracksItem.getContainedUnit() == kael && cacheGold == null) {
			supplyCacheCollected = true;
			shoreWaypoints.clear();
			System.out.println("[HumanExpansionOpeningProbe] collected the original Tiny Barracks and gold from the second cache");
		}
		final CDestructable cacheCrate = seekingCastle ? object(globals, "gg_dest_LTcr_4111") : null;
		final CDestructable cacheGate = seekingCastle ? game.getDestructables().stream()
				.filter(gate -> CACHE_GATE.equals(gate.getTypeId()) && !gate.isDead() && !gate.isInvulnerable()
						&& gate.getX() == 5888 && gate.getY() == 1600 && kael.canReach(gate, 560))
				.findFirst().orElse(null) : null;
		if (kael.getHeroData().getSkillPoints() > 0) {
			HumanCampaignPlaythroughProbe.immediate(viewer, orders, kael, FLAME_STRIKE.getValue());
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player) && kael.canReach(unit, 530)).toList();
		final CUnit threat = enemies.stream().min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		final var army = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.isBuilding() && !PEASANT.equals(unit.getTypeId())
				&& !unit.getCurrentAttacks().isEmpty()).toList();
		for (final CUnit unit : army) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit == kael && threat != null && CampaignPlaythroughProbe.supportOrder(viewer, orders, kael, threat)) continue;
			if (unit == kael && threat != null && enemies.size() >= 2 && kael.canReach(threat, 620)
					&& army.stream().noneMatch(ally -> ally.canReach(threat, 155))
					&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, kael, OrderIds.flamestrike,
							threat.getX(), threat.getY())) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, threat)) continue;
			if (cacheGate != null && unit.canReach(cacheGate, 510)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, cacheGate)) continue;
			if (seekingCastle && unit == kael) {
				if (castle != null && kael.canReach(castle, 500)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.smart, castle)) continue;
				if (cacheCrate != null && !cacheCrate.isDead() && kael.canReach(cacheCrate, 490)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.attack, cacheCrate)) continue;
			}
			if (approachingSupplyCache && !supplyCacheCollected) {
				if (unit == kael) {
					if (supplyApproachTree != null && kael.canReach(supplyApproachTree, 450)) {
						CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, kael, OrderIds.flamestrike, 6256, -5024);
						continue;
					}
					if (cacheGold != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.smart, cacheGold)) continue;
					if (barracksItem != null && barracksItem.getContainedUnit() != kael
							&& CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.smart, barracksItem)) continue;
					if (supplyCrate != null && !supplyCrate.isDead() && kael.canReach(supplyCrate, 670)
							&& (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, kael, OrderIds.flamestrike, 6384, -5168)
									|| CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.attack, supplyCrate))) continue;
				}
				if (supplyCrate != null && !supplyCrate.isDead() && unit != kael && unit.canReach(supplyCrate, 600)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, supplyCrate)) continue;
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, SUPPLY_CACHE_APPROACH,
						SUPPLY_CACHE_APPROACH.length - 1, 125, shoreWaypoints);
			}
			else {
				final float[][] route = seekingCastle ? CACHE_ROUTE : SHORE_ROUTE;
				final int end = seekingSupplies && !supplyCacheCollected ? 24 : route.length - 1;
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 135, shoreWaypoints);
			}
		}
		final int tick = game.getGameTurnTick();
		if (tick >= nextReport) {
			nextReport = tick + 400;
			System.out.printf("[HumanExpansionOpeningProbe] shore %.0f,%.0f life=%.0f escorts=%d step=%d%n",
					kael.getX(), kael.getY(), kael.getLife(), army.size(), shoreWaypoints.getOrDefault(kael.getHandleId(), 0));
			if (approachingSupplyCache && !supplyCacheCollected) {
				System.out.printf("[HumanExpansionOpeningProbe] second cache mana=%.0f Flame Strike=%d crate=%.0f barracks=%s gold=%s%n",
						kael.getMana(), kael.getAbilityLevel(FLAME_STRIKE), supplyCrate == null ? -1 : supplyCrate.getLife(),
						barracksItem != null, cacheGold != null);
			}
		}
		ui.getCameraManager().setTarget(kael.getX(), kael.getY());
	}

	private static void sailToSecondObservatory(final War3MapViewer viewer, final MeleeUI ui, final CUnit kael,
			final CUnit boat, final int player) {
		final var game = viewer.simulation;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (expeditionWorker == null || expeditionWorker.isDead()) {
			check(!expeditionAboard, "the original repair worker died after sailing");
			expeditionWorker = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
					&& !unit.isHidden() && PEASANT.equals(unit.getTypeId()))
					.min(Comparator.comparingDouble(boat::distanceSquaredNoCollision)).orElse(null);
			if (expeditionWorker == null) return;
		}
		if (SECOND_OBSERVATORY && expeditionEscorts.isEmpty() && !expeditionAboard) {
			game.getUnits().stream().filter(unit -> unit != kael && unit.getPlayerIndex() == player
					&& !unit.isDead() && !unit.isHidden() && !unit.isPaused() && !unit.isBuilding()
					&& !PEASANT.equals(unit.getTypeId()) && !unit.getCurrentAttacks().isEmpty()
					&& unit.getUnitType().getCargoCapacity() == 1)
					.sorted(Comparator.comparingDouble(boat::distanceSquaredNoCollision)).limit(4)
					.forEach(expeditionEscorts::add);
		}
		final var passengers = new ArrayList<>(List.of(kael, expeditionWorker));
		expeditionEscorts.stream().filter(unit -> !unit.isDead()).forEach(passengers::add);
		if (!expeditionAboard && passengers.stream().allMatch(unit -> unit.getTransport() == boat)) {
			expeditionAboard = true;
			System.out.printf("[HumanExpansionOpeningProbe] boarded Kael and the original repair worker in a Naga transport with %d escorts%n",
					passengers.size() - 2);
		}
		if (expeditionAboard && passengers.stream().noneMatch(CUnit::isHidden)
				&& kael.distanceSquaredNoCollision(LANDING_X, LANDING_Y) < 320 * 320
				&& expeditionWorker.distanceSquaredNoCollision(LANDING_X, LANDING_Y) < 320 * 320) {
			check(boat.getCargoData().isEmpty(), "the expedition did not unload on the Observatory shore");
			System.out.println("[HumanExpansionOpeningProbe] landed Kael and the repair worker below the second Observatory");
			if (SECOND_OBSERVATORY) {
				expeditionLanded = true;
				landingLumber = game.getPlayer(player).getLumber();
			}
			else finish();
			return;
		}
		if (expeditionAboard) {
			if (boat.distanceSquaredNoCollision(LAKE_ROUTE[LAKE_ROUTE.length - 1][0], LAKE_ROUTE[LAKE_ROUTE.length - 1][1]) < 155 * 155) {
				CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, boat, OrderIds.unloadall, LANDING_X, LANDING_Y);
			}
			else CampaignPlaythroughProbe.followRoute(viewer, orders, boat, LAKE_ROUTE, LAKE_ROUTE.length - 1, 118, shoreWaypoints);
			ui.getCameraManager().setTarget(boat.getX(), boat.getY());
		}
		else {
			for (final CUnit passenger : passengers) {
				if (passenger.getTransport() == boat) continue;
				if (passenger.canReach(boat, 440)) {
					CampaignPlaythroughProbe.targetOrder(viewer, orders, boat, OrderIds.load, passenger);
					CampaignPlaythroughProbe.targetOrder(viewer, orders, passenger, OrderIds.smart, boat);
					break;
				}
				CampaignPlaythroughProbe.followRoute(viewer, orders, passenger, SHORE_ROUTE, SHORE_ROUTE.length - 1, 130, shoreWaypoints);
			}
			ui.getCameraManager().setTarget(expeditionWorker.getX(), expeditionWorker.getY());
		}
		final int tick = game.getGameTurnTick();
		if (tick >= nextReport) {
			nextReport = tick + 350;
			System.out.printf("[HumanExpansionOpeningProbe] lake transport %.0f,%.0f life=%.0f cargo=%d worker=%.0f,%.0f lumber=%d%n",
					boat.getX(), boat.getY(), boat.getLife(), boat.getCargoData().getCargoCount(), expeditionWorker.getX(),
					expeditionWorker.getY(), game.getPlayer(player).getLumber());
		}
	}

	private static void repairSecondObservatory(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit kael) {
		final var game = viewer.simulation;
		final int player = kael.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final CUnit observatory = object(globals, "udg_Observatory02");
		final CUnit mine = object(globals, "gg_unit_ngol_0026");
		check(observatory != null && !observatory.isDead(), "the second Observatory was lost");
		final CUnit townHall = game.getUnitsIncludingPending().stream().filter(unit -> unit.getPlayerIndex() == player
				&& TOWN_HALL.equals(unit.getTypeId()) && !unit.isDead()).findFirst().orElse(null);
		final CUnit barracks = game.getUnitsIncludingPending().stream().filter(unit -> unit.getPlayerIndex() == player
				&& BARRACKS.equals(unit.getTypeId()) && !unit.isDead()).findFirst().orElse(null);
		final CUnit rally = townHall == null ? kael : townHall;
		final var localWorkers = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player
				&& PEASANT.equals(unit.getTypeId()) && !unit.isDead() && !unit.isHidden() && rally.canReach(unit, 1700)).toList();
		if (expeditionWorker.isDead()) {
			expeditionWorker = localWorkers.stream().max(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			check(expeditionWorker != null, "the second Observatory expedition lost its repair workers");
			repairWorkerGatheringLumber = false;
			System.out.println("[HumanExpansionOpeningProbe] assigned a surviving Peasant to finish the second Observatory repair");
		}
		if (!secondObservatoryVerified && globals.getGlobal("udg_Observatory02Repaired").visit(BooleanJassValueVisitor.getInstance())) {
			check(harvestingReported && secondRepairIssued && observatory.getLife() >= 2499 && observatory.isInvulnerable(),
					"the native second Observatory repair did not complete after harvesting");
			System.out.println("[HumanExpansionOpeningProbe] repaired the second Observatory with harvested lumber");
			secondObservatoryVerified = true;
			repairEscorts = List.of();
			if (!ALL_OBSERVATORIES) finish();
			return;
		}
		final CUnit threat = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& (expeditionWorker.canReach(unit, 660) || kael.canReach(unit, 710)))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		final float health = kael.getLife() / kael.getMaximumLife();
		kaelRecovering = health < (kaelRecovering ? 0.92f : 0.66f);
		for (final CUnit escort : expeditionEscorts) {
			final boolean escortingRepair = repairEscorts.contains(escort);
			if (escort.isDead() || escort.isHidden() || escort.isPaused() || (!escortingRepair && !escort.canReach(rally, 1400))
					|| escort.getCurrentBehavior() != null && escort.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (escortingRepair) {
				if (threat != null && expeditionWorker.canReach(threat, 700)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, escort, OrderIds.attack, threat)) continue;
				if (!escort.canReach(expeditionWorker, 210)) {
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, escort, expeditionWorker.getX(), expeditionWorker.getY());
				}
				continue;
			}
			if (kael.getLife() < kael.getMaximumLife() * 0.85f && escort.canReach(kael, 620)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, escort, OrderIds.heal, kael)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, escort, OrderIds.attack, threat)) continue;
			if (townHall == null && !kaelRecovering) {
				final int advance = Math.min(MINE_APPROACH.length - 1, mineWaypoints.getOrDefault(kael.getHandleId(), 0) + 1);
				CampaignPlaythroughProbe.followRoute(viewer, orders, escort, MINE_APPROACH, advance, 145, mineWaypoints);
			}
			else if (!escort.canReach(rally, 360)) CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, escort, rally.getX() + 240, rally.getY());
		}
		if (secondObservatoryVerified && townHall != null && !kael.canReach(MINE_REFUGE[0], MINE_REFUGE[1], 120)) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, kael, MINE_REFUGE[0], MINE_REFUGE[1]);
		}
		else if (kaelRecovering) {
			if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, kael, "phea")) {
				if (!secondObservatoryVerified || townHall == null) CampaignPlaythroughProbe.pointOrder(viewer, orders, kael, 944, 2736);
			}
		}
		else if (threat != null && (kael.getCurrentBehavior() == null || kael.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.SPELL)) {
			if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, kael, "phea")
					&& !CampaignPlaythroughProbe.supportOrder(viewer, orders, kael, threat)
					&& !CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, kael, OrderIds.flamestrike, threat.getX(), threat.getY())) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.attack, threat);
			}
		}
		if (townHall == null) {
			if (threat == null && !kaelRecovering) {
				if (kael.canReach(2992, 1136, 110)) placeTinyBuilding(viewer, orders, kael, TINY_CASTLE, TOWN_HALL_SITES);
				else CampaignPlaythroughProbe.followRoute(viewer, orders, kael, MINE_APPROACH, MINE_APPROACH.length - 1, 136, mineWaypoints);
				final int workerStep = Math.max(0, mineWaypoints.getOrDefault(kael.getHandleId(), 0) - 1);
				CampaignPlaythroughProbe.followRoute(viewer, orders, expeditionWorker, MINE_APPROACH, workerStep, 140, mineWaypoints);
			}
		}
		else {
			if (barracks == null && threat == null && !kaelRecovering) {
				placeTinyBuilding(viewer, orders, kael, TINY_BARRACKS, BARRACKS_SITES);
			}
			final long nearbyGuards = expeditionEscorts.stream().filter(unit -> !unit.isDead() && !unit.isHidden()
					&& townHall.canReach(unit, 1200)).count();
			if (barracks != null && !barracks.isConstructing() && barracks.getBuildQueue()[0] == null && localWorkers.size() >= 3
					&& owner.getGold() >= (secondObservatoryVerified ? 270 : 190) && nearbyGuards < 8) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, "hfoo");
			}
			game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isHidden() && !unit.isDead()
					&& "hfoo".equals(unit.getTypeId().toString()) && townHall.canReach(unit, 850)
					&& !expeditionEscorts.contains(unit)).forEach(expeditionEscorts::add);
			if (!townHall.isConstructing()) {
				if (!secondRepairIssued && !townHall.canReach(expeditionWorker, 1200)) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, expeditionWorker, MINE_APPROACH,
							MINE_APPROACH.length - 1, 140, mineWaypoints);
				}
				if (!harvestingReported && owner.getLumber() > landingLumber) {
					harvestingReported = true;
					System.out.println("[HumanExpansionOpeningProbe] built the Tiny Castle and deposited harvested lumber");
				}
				final CAbilityHumanRepair repair = expeditionWorker.getFirstAbilityOfType(CAbilityHumanRepair.class);
				final float missing = 1f - observatory.getLife() / observatory.getMaximumLife();
				final int lumberNeeded = (int) Math.ceil(observatory.getUnitType().getLumberRepairCost() * repair.getRepairCostRatio() * missing);
				if (secondObservatoryVerified) {
					final CUnit third = object(globals, "udg_Observatory03");
					final int routeLumber = (int) Math.ceil(third.getUnitType().getLumberRepairCost() * repair.getRepairCostRatio()
							* (1f - third.getLife() / third.getMaximumLife()));
					final long guardTowers = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player
							&& GUARD_TOWER.equals(unit.getTypeId()) && !unit.isDead() && !unit.isConstructing()
							&& townHall.canReach(unit, 1000)).count();
					if (owner.getLumber() >= routeLumber + 20 && kael.getLife() >= kael.getMaximumLife() * 0.90f
							&& guardTowers >= 2 && nearbyGuards >= 5 && threat == null) {
						thirdExpeditionReady = true;
						thirdEscorts = expeditionEscorts.stream().filter(escort -> !escort.isDead() && !escort.isHidden()
								&& townHall.canReach(escort, 1200) && escort.getUnitType().getCargoCapacity() == 1).limit(3).toList();
						System.out.printf("[HumanExpansionOpeningProbe] stocked lumber and left %d defenders with %d Guard Towers%n",
								nearbyGuards - thirdEscorts.size(), guardTowers);
						return;
					}
					gatherThirdExpeditionSupplies(viewer, orders, townHall, mine, localWorkers);
					return;
				}
				if (!secondRepairIssued && owner.getFoodCap() - owner.getFoodUsed() < 3 && owner.getTechtreeInProgress(FARM) == 0) {
					for (final float[] site : FARM_SITES) {
						if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, expeditionWorker, FARM.getValue(), site[0], site[1])) return;
					}
				}
				if (!secondRepairIssued && localWorkers.size() < 6 && townHall.getBuildQueue()[0] == null) {
					HumanCampaignPlaythroughProbe.train(viewer, orders, townHall, PEASANT.toString());
				}
				final boolean wasRepairing = secondRepairIssued;
				if (!secondObservatoryVerified && (wasRepairing || owner.getLumber() >= Math.min(lumberNeeded, 110) && localWorkers.size() >= 3)) {
					secondRepairIssued |= CampaignPlaythroughProbe.targetOrder(viewer, orders, expeditionWorker, OrderIds.repair, observatory);
					if (secondRepairIssued && !wasRepairing) {
						final CUnit worker = expeditionWorker;
						repairEscorts = expeditionEscorts.stream().filter(escort -> !escort.isDead() && !escort.isHidden())
								.sorted(Comparator.comparingDouble(worker::distanceSquaredNoCollision)).limit(2).toList();
						System.out.println("[HumanExpansionOpeningProbe] sent the repair crew to the second Observatory");
					}
				}
				final CDestructable tree = game.getDestructables().stream().filter(target -> !target.isDead()
						&& target.getDestType().getTargetedAs().contains(CTargetType.TREE) && townHall.canReach(target, 1500))
						.min(Comparator.comparingDouble(target -> target.distanceSquaredNoCollision(
								townHall.getX() - 380, townHall.getY() + 480))).orElse(null);
				if (!secondRepairIssued && !repairWorkerGatheringLumber && owner.getGold() >= 200) {
					repairWorkerGatheringLumber = CampaignPlaythroughProbe.targetOrder(viewer, orders, expeditionWorker, OrderIds.harvest, tree);
				}
				final CUnit repairWorker = expeditionWorker;
				final var goldWorkers = localWorkers.stream()
						.filter(worker -> worker != repairWorker || !repairWorkerGatheringLumber).limit(2).toList();
				for (final CUnit worker : localWorkers) {
					if (secondRepairIssued && worker == repairWorker) continue;
					if (threat != null && worker.getLife() < worker.getMaximumLife() * 0.73f && worker.canReach(threat, 630)) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, worker, townHall.getX() - 320, townHall.getY() + 100);
					}
					else if (worker.getCurrentOrder() == null || worker.getCurrentOrder().getOrderId() == OrderIds.move
							|| worker.getCurrentBehavior() != null
							&& worker.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.IDLE) {
						CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, goldWorkers.contains(worker) ? mine : tree);
					}
				}
			}
		}
		ui.getCameraManager().setTarget(expeditionWorker.getX(), expeditionWorker.getY());
		final int tick = game.getGameTurnTick();
		if (tick >= nextReport) {
			nextReport = tick + 700;
			System.out.printf("[HumanExpansionOpeningProbe] second Observatory life=%.0f gold=%d lumber=%d food=%d/%d workers=%d hall=%s worker=%.0f,%.0f Kael=%.0f,%.0f life=%.0f castle=%s threat=%s%n",
					observatory.getLife(), owner.getGold(), owner.getLumber(), owner.getFoodUsed(), owner.getFoodCap(), localWorkers.size(),
					townHall == null ? "waiting" : townHall.isConstructing() ? "building" : "ready",
					expeditionWorker.getX(), expeditionWorker.getY(), kael.getX(), kael.getY(), kael.getLife(),
					CampaignPlaythroughProbe.hasItem(kael, "tgrh"), threat == null ? "none" : threat.getTypeId());
		}
	}

	private static void gatherThirdExpeditionSupplies(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit townHall, final CUnit mine, final List<CUnit> workers) {
		final var game = viewer.simulation;
		if (game.getGameTurnTick() < nextThirdSupplyOrder) return;
		nextThirdSupplyOrder = game.getGameTurnTick() + 300;
		final var owner = game.getPlayer(townHall.getPlayerIndex());
		final var localWorkers = workers.stream().filter(worker -> worker != expeditionWorker).toList();
		final var trees = game.getDestructables().stream().filter(tree -> !tree.isDead()
				&& tree.getDestType().getTargetedAs().contains(CTargetType.TREE) && townHall.canReach(tree, 1800))
				.sorted(Comparator.comparingDouble(tree -> tree.distanceSquaredNoCollision(3360, 1376))).limit(4).toList();
		if (localWorkers.size() < 6 && townHall.getBuildQueue()[0] == null && owner.getGold() >= 75) {
			HumanCampaignPlaythroughProbe.train(viewer, orders, townHall, PEASANT.toString());
		}
		if (localWorkers.size() >= 4) {
			final CUnit builder = localWorkers.get(localWorkers.size() - 1);
			if (owner.getFoodCap() - owner.getFoodUsed() < 3 && owner.getTechtreeInProgress(FARM) == 0) {
				for (final float[] site : FARM_SITES) {
					if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, builder, FARM.getValue(), site[0], site[1])) return;
				}
			}
			if (owner.getGold() >= 150 && owner.getLumber() >= 45 && orderMineDefense(viewer, orders, builder, townHall)) return;
		}
		final int miningWorkers = owner.getLumber() < 130 ? 2 : 3;
		for (int i = 0; i < localWorkers.size(); i++) {
			final CUnit worker = localWorkers.get(i);
			final CAbilityHarvest harvest = worker.getFirstAbilityOfType(CAbilityHarvest.class);
			if (harvest == null || harvest.getCarriedResourceAmount() > 0) continue;
			if (worker.getCurrentOrder() != null && (worker.getCurrentOrder().getOrderId() == LUMBER_MILL.getValue()
					|| worker.getCurrentOrder().getOrderId() == SCOUT_TOWER.getValue()
					|| worker.getCurrentOrder().getOrderId() == FARM.getValue())) continue;
			final CWidget target = i < Math.min(miningWorkers, localWorkers.size() - 1) ? mine
					: trees.isEmpty() ? null : trees.get(i % trees.size());
			CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, target);
		}
		System.out.printf("[HumanExpansionOpeningProbe] third expedition supplies gold=%d lumber=%d workers=%d trees=%d guard towers=%d%n",
				owner.getGold(), owner.getLumber(), localWorkers.size(), trees.size(), owner.getTechtreeUnlocked(GUARD_TOWER));
	}

	private static boolean orderMineDefense(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit worker, final CUnit townHall) {
		final var game = viewer.simulation;
		final var owner = game.getPlayer(townHall.getPlayerIndex());
		final var buildings = game.getUnitsIncludingPending().stream().filter(unit -> unit.getPlayerIndex() == owner.getId()
				&& !unit.isDead() && unit.isBuilding() && townHall.canReach(unit, 1000)).toList();
		final CUnit scout = buildings.stream().filter(unit -> SCOUT_TOWER.equals(unit.getTypeId())
				&& !unit.isConstructing() && unit.getBuildQueue()[0] == null).findFirst().orElse(null);
		if (scout != null && HumanCampaignPlaythroughProbe.immediate(viewer, orders, scout, GUARD_TOWER.getValue())) return true;
		final long towers = buildings.stream().filter(unit -> SCOUT_TOWER.equals(unit.getTypeId()) || GUARD_TOWER.equals(unit.getTypeId())).count();
		final boolean needsMill = buildings.stream().noneMatch(unit -> LUMBER_MILL.equals(unit.getTypeId()));
		final War3ID building = needsMill ? LUMBER_MILL : SCOUT_TOWER;
		if (owner.getTechtreeInProgress(building) > 0 || !needsMill && towers >= 2) return false;
		for (final float[] site : needsMill ? MILL_SITES : TOWER_SITES) {
			if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, worker, building.getValue(), site[0], site[1])) return true;
		}
		return false;
	}

	private static void continueThirdExpedition(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit kael) {
		final var game = viewer.simulation;
		final int player = kael.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		defendThirdExpeditionBase(viewer, orders, player);
		final CUnit boat = object(globals, "udg_NagaBoat02");
		final CUnit third = object(globals, "udg_Observatory03");
		check(!expeditionWorker.isDead() && third != null && !third.isDead(), "the third Observatory expedition lost its worker or target");
		if (!thirdExpeditionLanded) {
			check(boat != null && !boat.isDead(), "the third Observatory expedition lost its original transport");
			final var passengers = new ArrayList<>(List.of(kael, expeditionWorker));
			thirdEscorts.stream().filter(unit -> !unit.isDead()).forEach(passengers::add);
			if (!thirdExpeditionAboard && passengers.stream().allMatch(unit -> unit.getTransport() == boat)) {
				thirdExpeditionAboard = true;
				thirdWaypoints.clear();
				System.out.printf("[HumanExpansionOpeningProbe] boarded the third Observatory ferry with %d escorts%n", passengers.size() - 2);
			}
			final float[] shore = THIRD_GROUND_ROUTE[0];
			if (thirdExpeditionAboard && boat.getCargoData().isEmpty() && passengers.stream().noneMatch(CUnit::isHidden)
					&& kael.canReach(shore[0], shore[1], 270) && expeditionWorker.canReach(shore[0], shore[1], 270)) {
				thirdExpeditionLanded = true;
				thirdWaypoints.clear();
				System.out.println("[HumanExpansionOpeningProbe] landed the repair party at the third Observatory's original shore");
				return;
			}
			if (thirdExpeditionAboard) {
				final float[] berth = THIRD_FERRY_ROUTE[THIRD_FERRY_ROUTE.length - 1];
				if (boat.distanceSquaredNoCollision(berth[0], berth[1]) < 90 * 90) {
					CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, boat, OrderIds.unloadall, shore[0], shore[1]);
				}
				else CampaignPlaythroughProbe.followRoute(viewer, orders, boat, THIRD_FERRY_ROUTE,
						THIRD_FERRY_ROUTE.length - 1, 105, thirdWaypoints);
			}
			else {
				for (final CUnit passenger : passengers) {
					if (passenger.getTransport() == boat) continue;
					if (passenger.canReach(boat, 380)) {
						CampaignPlaythroughProbe.targetOrder(viewer, orders, boat, OrderIds.load, passenger);
						CampaignPlaythroughProbe.targetOrder(viewer, orders, passenger, OrderIds.smart, boat);
						break;
					}
					CampaignPlaythroughProbe.followRoute(viewer, orders, passenger, RETURN_TO_FERRY,
							RETURN_TO_FERRY.length - 1, 135, thirdWaypoints);
				}
			}
		}
		else {
			final int step = thirdWaypoints.getOrDefault(kael.getHandleId(), 0);
			final int last = THIRD_GROUND_ROUTE.length - 1;
			// Clear the unload area before chasing enemies across the lake.
			final float engagementRange = step < 4 ? 290 : 590;
			final CUnit enemy = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
					&& kael.distanceSquaredNoCollision(unit) < engagementRange * engagementRange
					&& (step >= 4 || unit.getY() >= kael.getY() - 64))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			for (final CUnit escort : thirdEscorts) {
				if (escort.isDead() || escort.isHidden() || escort.getCurrentBehavior() != null
						&& escort.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, escort, OrderIds.attack, enemy)) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, escort, THIRD_GROUND_ROUTE, Math.min(last, step + 1),
							120, thirdWaypoints);
				}
			}
			if (enemy == null) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, kael, THIRD_GROUND_ROUTE, last, 130, thirdWaypoints);
				if (step >= last - 1) CampaignPlaythroughProbe.targetOrder(viewer, orders, expeditionWorker, OrderIds.repair, third);
				else CampaignPlaythroughProbe.followRoute(viewer, orders, expeditionWorker, THIRD_GROUND_ROUTE,
						Math.max(0, step - 1), 140, thirdWaypoints);
			}
			else if (kael.getCurrentBehavior() == null || kael.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.SPELL) {
				if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, kael, "phea")
						&& !CampaignPlaythroughProbe.supportOrder(viewer, orders, kael, enemy)
						&& !CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, kael, OrderIds.flamestrike, enemy.getX(), enemy.getY())) {
					CampaignPlaythroughProbe.targetOrder(viewer, orders, kael, OrderIds.attack, enemy);
				}
			}
		}
		final int tick = game.getGameTurnTick();
		if (tick >= nextReport) {
			nextReport = tick + 720;
			System.out.printf("[HumanExpansionOpeningProbe] third Observatory life=%.0f lumber=%d Kael=%.0f,%.0f life=%.0f step=%d worker=%.0f,%.0f life=%.0f step=%d aboard=%s landed=%s%n",
					third.getLife(), owner.getLumber(), kael.getX(), kael.getY(), kael.getLife(), thirdWaypoints.getOrDefault(kael.getHandleId(), 0),
					expeditionWorker.getX(), expeditionWorker.getY(), expeditionWorker.getLife(), thirdWaypoints.getOrDefault(expeditionWorker.getHandleId(), 0),
					thirdExpeditionAboard, thirdExpeditionLanded);
		}
		final CUnit cameraTarget = thirdExpeditionAboard && !thirdExpeditionLanded ? boat : kael;
		ui.getCameraManager().setTarget(cameraTarget.getX(), cameraTarget.getY());
	}

	private static void defendThirdExpeditionBase(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final int player) {
		final var game = viewer.simulation;
		final CUnit townHall = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
				&& TOWN_HALL.equals(unit.getTypeId())).findFirst().orElse(null);
		if (townHall == null) return;
		final var owner = game.getPlayer(player);
		final CUnit raider = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& townHall.canReach(unit, 920)).min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		for (final CUnit defender : expeditionEscorts) {
			if (defender.isDead() || defender.isHidden() || thirdEscorts.contains(defender) || !townHall.canReach(defender, 1100)
					|| defender.getCurrentBehavior() != null && defender.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (raider != null) CampaignPlaythroughProbe.targetOrder(viewer, orders, defender, OrderIds.attack, raider);
		}
		final var workers = game.getUnits().stream().filter(unit -> unit != expeditionWorker && unit.getPlayerIndex() == player
				&& PEASANT.equals(unit.getTypeId()) && !unit.isDead() && !unit.isHidden() && townHall.canReach(unit, 1300)).toList();
		final CUnit mine = game.getUnits().stream().filter(unit -> "ngol".equals(unit.getTypeId().toString()) && !unit.isDead()
				&& townHall.canReach(unit, 900)).min(Comparator.comparingDouble(townHall::distanceSquaredNoCollision)).orElse(null);
		gatherThirdExpeditionSupplies(viewer, orders, townHall, mine, workers);
	}

	private static void placeTinyBuilding(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit kael,
			final War3ID itemId, final float[][] sites) {
		final var inventory = kael.getInventoryData();
		for (int slot = 0; slot < inventory.getItemCapacity(); slot++) {
			final CItem item = inventory.getItemInSlot(slot);
			if (item == null || !itemId.equals(item.getTypeId())) continue;
			for (final float[] site : sites) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, kael, OrderIds.itemuse00 + slot, site[0], site[1])) return;
			}
		}
	}

	private static void finish() {
		finished = true;
		Gdx.app.exit();
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String detail) {
		if (!condition) throw new IllegalStateException("Human expansion opening audit: " + detail);
	}
}
