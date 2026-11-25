package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.root.CAbilityRoot;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;

/** Verifies the original uprooted base, construction event, and first income in NightElf04. */
public final class NightElfBarrowOpeningProbe {
	private static final boolean DUNGEON_OPENING = Boolean.getBoolean("warsmash.nightElfDungeonOpening");
	private static final boolean SKULL_QUEST = Boolean.getBoolean("warsmash.nightElfSkullQuest");
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.nightElfBarrowOpeningAudit");
	private static final boolean PLAYTHROUGH = Boolean.getBoolean("warsmash.nightElfBarrowPlaythrough");
	private static final War3ID WISP = War3ID.fromString("ewsp");
	private static final War3ID GOLD_MINE = War3ID.fromString("ngol");
	private static final War3ID ARCHER = War3ID.fromString("earc");
	private static final War3ID TREE_OF_LIFE = War3ID.fromString("etol");
	private static final War3ID ANCIENT_OF_WAR = War3ID.fromString("eaom");
	private static final War3ID MOON_WELL = War3ID.fromString("emow");
	private static final float[][] MINE_ROUTE = { { -6768, -3056 }, { -6448, -2736 }, { -6192, -2416 },
			{ -5872, -2096 }, { -5744, -1776 }, { -5616, -1456 }, { -5328, -1136 }, { -5008, -816 },
			{ -4688, -496 } };
	private static final float[][] GOLD_MINE_ROOT_SITES = { { -4672, -448 }, { -4800, -512 }, { -4736, -640 } };
	private static final float[][] EXPANSION_ROUTE = { { -4336, -496 }, { -3696, -464 }, { -3056, -464 },
			{ -2416, -464 }, { -1776, -464 }, { -1136, -464 } };
	private static final float[][] BARROW_ROUTE = { { -4336, -496 }, { -3696, -464 }, { -3056, -464 }, { -2416, -464 },
			{ -1776, -464 }, { -1136, 112 }, { -496, 432 }, { 144, 432 }, { 720, 208 }, { 1360, 368 }, { 2000, 432 },
			{ 2448, 720 }, { 2640, 400 }, { 2736, -240 }, { 2736, -880 }, { 2416, -1008 }, { 2096, -1200 },
			{ 1936, -1744 }, { 1936, -2384 }, { 1936, -3024 }, { 2020, -3720 }, { 2512, -3824 }, { 3152, -3952 },
			{ 3792, -4112 }, { 4432, -3472 }, { 5072, -3280 }, { 5712, -2704 }, { 6320, -2064 }, { 6640, -1712 },
			{ 7056, -1648 }, { 7696, -1104 }, { 7344, -464 }, { 6704, 48 }, { 6064, 432 }, { 5808, 752 },
			{ 5808, 1200 }, { 5808, 1840 }, { 5872, 2480 }, { 5840, 3216 } };
	private static final int FRONT_GATE = 11;
	private static final int DRYAD_GATE = 17;
	private static final int SATYR_RALLY = 28;
	private static final int SATYR_CAMP = 34;
	private static final int FORWARD_MINE_SUPPORT = 14;
	private static final int FORWARD_MINE_FORK = 27;
	private static final float[] FORWARD_MINE = { 8832, -3136 };
	private static final float[][] FORWARD_MINE_ROUTE = {
			{ 6416, -2064 }, { 6736, -2064 }, { 7056, -2064 }, { 7376, -1904 },
			{ 7696, -1904 }, { 7984, -2224 }, { 8304, -2544 }, { 8624, -2864 }
	};
	private static final Map<Integer, Integer> routeSteps = new HashMap<>();
	private static final Map<Integer, Integer> forwardMineSteps = new HashMap<>();
	private static int orderTick;
	private static int reportTick;
	private static int goldBeforeMining = -1;
	private static boolean introHandled;
	private static boolean sawUprootedTree;
	private static boolean mining;
	private static boolean relocating;
	private static boolean reachedExpansion;
	private static boolean expansionReported;
	private static boolean dungeonHandoffVerified;
	private static int barrowWaypoint;
	private static int nextMarchReport;
	private static boolean expeditionReady;
	private static boolean satyrsDefeated;
	private static boolean satyrEscortReady;
	private static CUnit forwardBuilder;
	private static CUnit forwardMine;
	private static boolean forwardBuilderReachedFork;
	private static Properties tyrandeCarryover;
	private static Properties furionCarryover;
	private static boolean finished;

	private NightElfBarrowOpeningProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || !initialized || finished) return;
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final String path = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (NightElfFinaleOpeningProbe.VICTORY && path.endsWith("/bonuscredits.w3m")) {
			NightElfFinaleOpeningProbe.verifyCredits();
			return;
		}
		if (NightElfFinaleOpeningProbe.ENABLED && path.endsWith("/nightelf07.w3m")) {
			NightElfFinaleOpeningProbe.afterMapRender(viewer, ui, globals);
			return;
		}
		if (SKULL_QUEST && NightElfSkullPlaythroughProbe.DEFEAT_TICHONDRIUS && path.endsWith("/nightelf06interlude.w3m")) {
			NightElfSkullPlaythroughProbe.verifyInterlude(ui);
			return;
		}
		if (SKULL_QUEST && path.endsWith("/nightelf06.w3m")) {
			if (NightElfOpeningPlaythroughProbe.CHAIN) NightElfDungeonOpeningProbe.verifyNextChapter();
			NightElfSkullPlaythroughProbe.afterMapRender(viewer, ui, globals);
			return;
		}
		if (DUNGEON_OPENING && NightElfDungeonOpeningProbe.PLAYTHROUGH && path.endsWith("/nightelf06.w3m")) {
			NightElfDungeonOpeningProbe.verifyNextChapter();
			return;
		}
		final boolean inDungeon = path.endsWith("/nightelf05.w3m");
		if (NightElfOpeningPlaythroughProbe.CHAIN && !inDungeon && !path.endsWith("/nightelf04.w3m")) return;
		final CUnit furion = object(globals, "udg_Furion");
		final CUnit tyrande = object(globals, "udg_Tyrande");
		if (inDungeon) {
			if (PLAYTHROUGH && !dungeonHandoffVerified) {
				if (furion == null || tyrande == null) return;
				check(satyrsDefeated && tyrandeCarryover.equals(snapshot(tyrande)) && furionCarryover.equals(snapshot(furion)),
						"hero carryover mismatch after the original victory");
				dungeonHandoffVerified = true;
				System.out.println("[NightElfBarrowOpeningProbe] entered NightElf05 and verified Tyrande and Furion carryover");
				if (!NightElfOpeningPlaythroughProbe.CHAIN) {
					finish();
					return;
				}
			}
			if (DUNGEON_OPENING) NightElfDungeonOpeningProbe.afterMapRender(viewer, ui, globals, tyrande, furion);
			return;
		}
		check(!DUNGEON_OPENING || NightElfOpeningPlaythroughProbe.CHAIN, "unexpected dungeon map " + path);
		check(path.endsWith("/nightelf04.w3m"), "unexpected map " + path);
		final CUnit town = object(globals, "udg_IntroFro");
		if (furion == null || tyrande == null || town == null) return;
		final int tick = game.getGameTurnTick();
		check(tick < (PLAYTHROUGH ? 65_000 : 14_000), "the expedition stalled at waypoint " + barrowWaypoint);
		check(!furion.isDead() && !tyrande.isDead() && !town.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()),
				"native defeat with Furion at " + Math.round(furion.getX()) + "," + Math.round(furion.getY())
						+ " life " + Math.round(furion.getLife()) + " and Tyrande life " + Math.round(tyrande.getLife()));
		final CQuest barrow = object(globals, "udg_QuestBarrow");
		if (PLAYTHROUGH && barrow != null && barrow.isCompleted()) {
			if (tyrandeCarryover == null) {
				tyrandeCarryover = snapshot(tyrande);
				furionCarryover = snapshot(furion);
				System.out.println("[NightElfBarrowOpeningProbe] Furion woke the Druids through the original quest");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final Trigger intro = object(globals, "gg_trg_Intro_Skip");
		if (!introHandled && intro != null && intro.isEnabled() && !ui.isUserControlEnabled()) {
			introHandled = ui.keyDown(Input.Keys.ESCAPE);
		}
		final var root = town.getFirstAbilityOfType(CAbilityRoot.class);
		if (root != null && !root.isRooted()) sawUprootedTree = true;
		if (root == null || !ui.isUserControlEnabled() || game.isGamePaused() || town.isPaused() || tick < orderTick
				|| Boolean.TRUE.equals(globals.getGlobal("udg_CinematicPlaying").visit(BooleanJassValueVisitor.getInstance()))) return;
		orderTick = tick + 20;

		final var orders = game.getDefaultPlayerUnitOrderExecutor(furion.getPlayerIndex());
		final var player = game.getPlayer(furion.getPlayerIndex());
		final CUnit mine = object(globals, "gg_unit_ngol_0005");
		final CUnit entangled = object(globals, "udg_EntangledGoldmine");
		if (PLAYTHROUGH && mining) {
			final var wisps = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
					&& WISP.equals(unit.getTypeId()) && !unit.isDead()).sorted(Comparator.comparingInt(CUnit::getHandleId)).toList();
			final CUnit workingMine = keepMining(viewer, orders, town, root, entangled);
			final CUnit forwardTown = barrowWaypoint >= FORWARD_MINE_SUPPORT ? developForwardMine(viewer, orders, town, wisps) : null;
			wisps.stream().filter(unit -> unit != forwardBuilder && (forwardTown == null
					|| forwardTown.distanceSquaredNoCollision(unit) > 1300 * 1300)).limit(4).filter(unit -> !unit.isHidden())
					.forEach(unit -> CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, workingMine));
			prepareArmy(viewer, orders, town, forwardTown, wisps, globals);
			march(viewer, orders, tyrande, furion, town, globals);
			ui.getCameraManager().setTarget(furion.getX(), furion.getY());
			return;
		}
		if (root.isRooted() && entangled != null && !entangled.isConstructing()) {
			if (goldBeforeMining < 0) {
				goldBeforeMining = player.getGold();
				System.out.println("[NightElfBarrowOpeningProbe] original construction event registered the Entangled Gold Mine");
			}
			final var wisps = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
					&& WISP.equals(unit.getTypeId()) && !unit.isDead()).sorted(Comparator.comparingInt(CUnit::getHandleId)).toList();
			wisps.stream().limit(4).filter(unit -> !unit.isHidden())
					.forEach(unit -> CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, entangled));
			if (!mining && sawUprootedTree && player.getGold() > goldBeforeMining && barrow != null && barrow.isDiscovered()) {
				mining = true;
				System.out.println("[NightElfBarrowOpeningProbe] uprooted Tree of Ages took root and delivered the first gold");
				if (!PLAYTHROUGH) finish();
			}
		}
		else if (root.isRooted()) CampaignPlaythroughProbe.targetOrder(viewer, orders, town, OrderIds.entangle, mine);
		else if (town.distanceSquaredNoCollision(MINE_ROUTE[MINE_ROUTE.length - 1][0],
				MINE_ROUTE[MINE_ROUTE.length - 1][1]) < 185 * 185) {
			for (final float[] site : GOLD_MINE_ROOT_SITES) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, town, OrderIds.root, site[0], site[1])) break;
			}
		}
		else CampaignPlaythroughProbe.followRoute(viewer, orders, town, MINE_ROUTE, MINE_ROUTE.length - 1, 135, routeSteps);

		for (final CUnit unit : game.getUnits()) {
			if (unit.isDead() || unit.isHidden() || unit.getPlayerIndex() != player.getId()) continue;
			if (unit != tyrande && unit != furion && !ARCHER.equals(unit.getTypeId())) continue;
			if (unit.distanceSquaredNoCollision(town) > 380 * 380) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, town.getX() - 180, town.getY() + 150);
			}
			if (unit != furion || unit.getLife() < unit.getMaximumLife() * 0.65f) continue;
			final var foe = game.getUnits().stream().filter(enemy -> !enemy.isDead() && !enemy.isHidden()
					&& !player.hasAlliance(enemy.getPlayerIndex(), CAllianceType.PASSIVE)
					&& !enemy.isBuilding() && furion.canReach(enemy, 520) && enemy.isVisible(game, player.getId()))
					.min(Comparator.comparingDouble(furion::distanceSquaredNoCollision)).orElse(null);
			CampaignPlaythroughProbe.targetOrder(viewer, orders, furion, OrderIds.entanglingroots, foe);
		}
		if (tick >= reportTick) {
			reportTick = tick + 400;
			System.out.printf("[NightElfBarrowOpeningProbe] tick=%d tree=%.0f,%.0f rooted=%b gold=%d firstMine=%b%n",
					tick, town.getX(), town.getY(), root.isRooted(), player.getGold(), entangled != null);
		}
		ui.getCameraManager().setTarget(town.getX(), town.getY());
	}

	private static CUnit keepMining(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit town,
			final CAbilityRoot root, final CUnit firstMine) {
		final var game = viewer.simulation;
		final var original = firstMine == null ? null : firstMine.getFirstAbilityOfType(CAbilityEntangledMine.class);
		if (!relocating && original != null && original.getGold() > 0) return firstMine;
		if (!relocating) {
			relocating = true;
			routeSteps.remove(town.getHandleId());
		}
		final var workingMine = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == town.getPlayerIndex()
				&& !unit.isDead() && !unit.isConstructing() && unit != firstMine
				&& unit.distanceSquaredNoCollision(-768, -640) < 150 * 150
				&& unit.getFirstAbilityOfType(CAbilityEntangledMine.class) != null).findFirst().orElse(null);
		if (workingMine != null) {
			if (!expansionReported) {
				expansionReported = true;
				System.out.println("[NightElfBarrowOpeningProbe] the Tree of Ages reopened gold income at the second mine");
			}
			return workingMine;
		}
		if (!reachedExpansion) {
			if (root.isRooted()) HumanCampaignPlaythroughProbe.immediate(viewer, orders, town, OrderIds.unroot);
			else if (town.distanceSquaredNoCollision(-1136, -464) < 205 * 205) reachedExpansion = true;
			else CampaignPlaythroughProbe.followRoute(viewer, orders, town, EXPANSION_ROUTE, EXPANSION_ROUTE.length - 1,
					135, routeSteps);
		}
		else if (!root.isRooted()) {
			for (final float[] site : new float[][] { { -1152, -448 }, { -1216, -512 }, { -1280, -576 } }) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, town, OrderIds.root, site[0], site[1])) break;
			}
		}
		else {
			final CUnit mine = game.getUnits().stream().filter(unit -> GOLD_MINE.equals(unit.getTypeId())
					&& unit.distanceSquaredNoCollision(-768, -640) < 120 * 120).findFirst().orElse(null);
			CampaignPlaythroughProbe.targetOrder(viewer, orders, town, OrderIds.entangle, mine);
		}
		return null;
	}

	private static CUnit developForwardMine(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit town,
			final List<CUnit> wisps) {
		final var game = viewer.simulation;
		final int owner = town.getPlayerIndex();
		final CUnit forwardTown = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == owner
				&& TREE_OF_LIFE.equals(unit.getTypeId()) && !unit.isDead()
				&& unit.distanceSquaredNoCollision(FORWARD_MINE[0], FORWARD_MINE[1]) < 1050 * 1050).findFirst().orElse(null);
		if (forwardTown == null) {
			if (forwardBuilder == null || forwardBuilder.isDead()) {
				forwardBuilderReachedFork = false;
				forwardBuilder = wisps.stream().filter(unit -> !unit.isHidden())
						.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(6320, -2064))).orElse(null);
			}
			if (forwardBuilder == null || forwardBuilder.getCurrentOrder() != null
					&& forwardBuilder.getCurrentOrder().getOrderId() == TREE_OF_LIFE.getValue()) return null;
			forwardBuilderReachedFork |= forwardBuilder.distanceSquaredNoCollision(6320, -2064) < 650 * 650
					|| forwardBuilder.distanceSquaredNoCollision(FORWARD_MINE[0], FORWARD_MINE[1]) < 2800 * 2800;
			if (forwardBuilderReachedFork) {
				final int nextStep = forwardMineSteps.getOrDefault(forwardBuilder.getHandleId(), 0);
				final float[] advance = FORWARD_MINE_ROUTE[Math.min(nextStep + 1, FORWARD_MINE_ROUTE.length - 1)];
				final long nearbyGuards = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == owner
						&& !unit.isDead() && !unit.isHidden() && !unit.isBuilding() && !unit.getCurrentAttacks().isEmpty()
						&& unit.distanceSquaredNoCollision(advance[0], advance[1]) < 550 * 550).limit(3).count();
				if (nearbyGuards < 3) {
					HumanCampaignPlaythroughProbe.immediate(viewer, orders, forwardBuilder, OrderIds.stop);
					return null;
				}
				if (forwardBuilder.distanceSquaredNoCollision(FORWARD_MINE[0], FORWARD_MINE[1]) < 900 * 900
						&& (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, forwardBuilder, TREE_OF_LIFE.getValue(), 8384, -2688)
								|| NightElfOpeningPlaythroughProbe.buildNear(viewer, forwardBuilder, TREE_OF_LIFE, 8320, -2560))) return null;
				CampaignPlaythroughProbe.followRoute(viewer, orders, forwardBuilder, FORWARD_MINE_ROUTE,
						FORWARD_MINE_ROUTE.length - 1, 165, forwardMineSteps);
			}
			else CampaignPlaythroughProbe.followRoute(viewer, orders, forwardBuilder, BARROW_ROUTE,
					Math.min(barrowWaypoint, FORWARD_MINE_FORK), 165, routeSteps);
			return null;
		}
		forwardBuilder = null;
		if (forwardTown.isConstructing()) return forwardTown;
		final CUnit mine = game.getUnits().stream().filter(unit -> !unit.isDead()
				&& unit.distanceSquaredNoCollision(FORWARD_MINE[0], FORWARD_MINE[1]) < 120 * 120
				&& (GOLD_MINE.equals(unit.getTypeId()) || unit.getFirstAbilityOfType(CAbilityEntangledMine.class) != null))
				.filter(unit -> !unit.isHidden()).findFirst().orElse(null);
		if (mine == null) return forwardTown;
		if (GOLD_MINE.equals(mine.getTypeId())) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, forwardTown, OrderIds.entangle, mine);
		}
		else {
			if (!mine.isConstructing() && forwardMine == null) {
				forwardMine = mine;
				System.out.println("[NightElfBarrowOpeningProbe] entangled the original gold mine behind the Satyr approach");
			}
			final var local = wisps.stream().filter(unit -> forwardTown.distanceSquaredNoCollision(unit) < 1300 * 1300).toList();
			if (local.size() < 6 && forwardTown.getBuildQueue()[0] == null) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, forwardTown, "ewsp");
			}
			local.stream().limit(4).filter(unit -> !unit.isHidden())
					.forEach(unit -> CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, forwardMine));
		}
		return forwardTown;
	}

	private static void prepareArmy(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit town,
			final CUnit forwardTown, final List<CUnit> wisps, final GlobalScope globals) {
		final var game = viewer.simulation;
		final var player = game.getPlayer(town.getPlayerIndex());
		final int treeReserve = barrowWaypoint >= 1 && forwardTown == null
				? game.getUnitData().getUnitType(TREE_OF_LIFE).getGoldCost() : 0;
		final int wispCost = game.getUnitData().getUnitType(WISP).getGoldCost();
		if (wisps.size() < 8 && town.getBuildQueue()[0] == null
				&& (wisps.size() < 4 || player.getGold() >= treeReserve + wispCost)) {
			HumanCampaignPlaythroughProbe.train(viewer, orders, town, "ewsp");
		}
		final CUnit ancient = object(globals, "udg_IntroWar");
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId() && !unit.isDead()).toList();
		final CUnit trainingAncient = owned.stream().filter(unit -> ANCIENT_OF_WAR.equals(unit.getTypeId())
				&& !unit.isConstructing() && unit.isBuilding())
				.min(Comparator.comparingDouble(unit -> forwardTown == null ? town.distanceSquaredNoCollision(unit)
						: forwardTown.distanceSquaredNoCollision(unit))).orElse(null);
		final boolean forwardTraining = forwardTown != null && owned.stream().anyMatch(unit -> ANCIENT_OF_WAR.equals(unit.getTypeId())
				&& forwardTown.distanceSquaredNoCollision(unit) < 1350 * 1350);
		final List<CUnit> localWisps = forwardTown == null ? List.of() : wisps.stream()
				.filter(unit -> forwardTown.distanceSquaredNoCollision(unit) < 1300 * 1300).toList();
		final long archers = owned.stream().filter(unit -> ARCHER.equals(unit.getTypeId())).count();
		final long wells = owned.stream().filter(unit -> MOON_WELL.equals(unit.getTypeId())).count();
		final int reserve = treeReserve + (forwardMine != null && !forwardTraining
				? game.getUnitData().getUnitType(ANCIENT_OF_WAR).getGoldCost() : 0);
		if (trainingAncient != null && archers < 22 && trainingAncient.getBuildQueue()[0] == null
				&& player.getGold() >= reserve + game.getUnitData().getUnitType(ARCHER).getGoldCost()) {
			HumanCampaignPlaythroughProbe.train(viewer, orders, trainingAncient, "earc");
		}
		if (trainingAncient == null && ancient != null && !ancient.isDead()) {
			if (ancient.distanceSquaredNoCollision(-5616, -1456) > 260 * 260) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, ancient, MINE_ROUTE, 5, 140, routeSteps);
			}
			else {
				for (final float[] site : new float[][] { { -5632, -1472 }, { -5504, -1344 }, { -5440, -1152 } }) {
					if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, ancient, OrderIds.root, site[0], site[1])) break;
				}
			}
		}
		for (final CUnit wisp : wisps.stream().skip(4).filter(unit -> !unit.isHidden()).toList()) {
			if (wisp == forwardBuilder || localWisps.indexOf(wisp) >= 0 && localWisps.indexOf(wisp) < 4) continue;
			if (wisp.getCurrentOrder() != null && (wisp.getCurrentOrder().getOrderId() == MOON_WELL.getValue()
					|| wisp.getCurrentOrder().getOrderId() == ANCIENT_OF_WAR.getValue())) continue;
			if (forwardMine != null && !forwardTraining && localWisps.contains(wisp)
					&& NightElfOpeningPlaythroughProbe.buildNear(viewer, wisp, ANCIENT_OF_WAR, 8160, -2496)) break;
			if (trainingAncient == null && (ancient == null || ancient.isDead() || game.getGameTurnTick() > 4400)
					&& owned.stream().noneMatch(unit -> ANCIENT_OF_WAR.equals(unit.getTypeId()) && unit.isConstructing())
					&& NightElfOpeningPlaythroughProbe.buildNear(viewer, wisp, ANCIENT_OF_WAR, -5200, -912)) break;
			if (wells < 6 && player.getFoodCap() - player.getFoodUsed() < 5
					&& NightElfOpeningPlaythroughProbe.buildNear(viewer, wisp, MOON_WELL, -5472 + wells * 128, -1088)) break;
			final var tree = game.getDestructables().stream().filter(item -> !item.isDead()
					&& item.getDestType().getTargetedAs().contains(CTargetType.TREE))
					.min(Comparator.comparingDouble(wisp::distanceSquaredNoCollision)).orElse(null);
			CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.wispharvest, tree);
		}
	}

	private static void march(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit tyrande,
			final CUnit furion, final CUnit town, final GlobalScope globals) {
		final var game = viewer.simulation;
		final var player = game.getPlayer(town.getPlayerIndex());
		final int tick = game.getGameTurnTick();
		final var troops = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId() && !unit.isDead()
				&& !unit.isHidden() && !unit.isBuilding() && unit.getFirstAbilityOfType(CAbilityRoot.class) == null
				&& !unit.getCurrentAttacks().isEmpty()).toList();
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		final CQuestItem camp = object(globals, "udg_QuestBarrowReqKillBase");
		if (!satyrsDefeated && camp != null && camp.isCompleted()) {
			satyrsDefeated = true;
			System.out.println("[NightElfBarrowOpeningProbe] destroyed the original Satyr base");
		}
		if (troops.size() >= 14) expeditionReady = true;
		final var next = BARROW_ROUTE[barrowWaypoint];
		final float[] rally = BARROW_ROUTE[SATYR_RALLY];
		if (!satyrEscortReady && barrowWaypoint >= SATYR_RALLY) {
			final long ralliedTroops = troops.stream().filter(unit -> unit != tyrande && unit != furion
					&& unit.distanceSquaredNoCollision(rally[0], rally[1]) < 700 * 700).count();
			satyrEscortReady = ralliedTroops >= (forwardMine == null ? 10 : 6);
			if (satyrEscortReady) System.out.println("[NightElfBarrowOpeningProbe] rallied the escort before attacking the Satyrs");
		}
		final boolean assemblingSatyrEscort = barrowWaypoint >= SATYR_RALLY && !satyrEscortReady;
		final CUnit protector = satyrsDefeated ? enemies.stream()
				.filter(unit -> unit.getPlayerIndex() == 6 && "ncap".equals(unit.getTypeId().toString())
						&& unit.distanceSquaredNoCollision(6304, 768) < 1000 * 1000)
				.min(Comparator.comparingDouble(furion::distanceSquaredNoCollision)).orElse(null) : null;
		if (expeditionReady && !assemblingSatyrEscort && barrowWaypoint < BARROW_ROUTE.length - 1
				&& (barrowWaypoint != SATYR_CAMP || satyrsDefeated && protector == null)
				&& furion.distanceSquaredNoCollision(next[0], next[1]) < 210 * 210
				&& (satyrsDefeated || troops.stream().filter(unit -> unit.distanceSquaredNoCollision(next[0], next[1]) < 500 * 500)
						.count() >= Math.min(4, Math.max(2, troops.size() - 3)))) {
			barrowWaypoint++;
		}
		final CDestructable gate = barrowWaypoint >= FRONT_GATE && barrowWaypoint < DRYAD_GATE
				? object(globals, "gg_dest_LTg2_0277")
				: barrowWaypoint >= DRYAD_GATE && barrowWaypoint <= DRYAD_GATE + 1 ? object(globals, "gg_dest_LTg4_2100") : null;
		final boolean gateInWay = gate != null && !gate.isDead() && furion.canReach(gate, 1050);
		final List<CUnit> builderEscort = forwardBuilder == null || forwardBuilder.isDead() ? List.of() : troops.stream()
				.filter(unit -> unit != tyrande && unit != furion)
				.sorted(Comparator.comparingDouble(forwardBuilder::distanceSquaredNoCollision))
				.limit(forwardBuilderReachedFork ? 5 : 3).toList();
		NightElfEscapePlaythroughProbe.prepareSearingArrows(viewer, orders, tyrande);
		int sentries = 0;
		for (final CUnit unit : troops) {
			if (unit.isPaused() || unit.getCurrentBehavior() != null
					&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (builderEscort.contains(unit)) {
				final CUnit threat = enemies.stream().filter(enemy -> unit.canReach(enemy, 500)
						|| forwardBuilder.canReach(enemy, 480))
						.min(Comparator.comparingDouble(forwardBuilder::distanceSquaredNoCollision)).orElse(null);
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, threat)) {
					final var builderRoute = forwardBuilderReachedFork ? FORWARD_MINE_ROUTE : BARROW_ROUTE;
					final var builderSteps = forwardBuilderReachedFork ? forwardMineSteps : routeSteps;
					final int step = Math.min(builderRoute.length - 1,
							builderSteps.getOrDefault(forwardBuilder.getHandleId(), 0) + (forwardBuilderReachedFork ? 1 : 0));
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, builderRoute, step, 180, builderSteps);
				}
				continue;
			}
			if (!expeditionReady || barrowWaypoint < SATYR_RALLY && troops.size() > 10
					&& ARCHER.equals(unit.getTypeId()) && sentries++ < 3) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, town.getX() + 144, town.getY() + 96);
				continue;
			}
			if (assemblingSatyrEscort && unit != tyrande && unit != furion) {
				final CUnit pursuer = enemies.stream().filter(target -> unit.canReach(target, 400))
						.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, pursuer)) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, BARROW_ROUTE, SATYR_RALLY, 180, routeSteps);
				}
				continue;
			}
			if (unit == tyrande && barrowWaypoint >= SATYR_RALLY + 1) {
				final float[] rear = BARROW_ROUTE[FORWARD_MINE_FORK - 1];
				if (tyrande.getLife() < tyrande.getMaximumLife() * 0.62f
						&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "phea")
								|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "pghe"))) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, rear[0], rear[1]);
				continue;
			}
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, 520))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit == furion && enemy != null
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, furion, OrderIds.entanglingroots, enemy)) continue;
			if (unit == furion && enemy != null && furion.getMana() >= 125) {
				final var trees = game.getDestructables().stream().filter(tree -> !tree.isDead()
						&& tree.getDestType().getTargetedAs().contains(CTargetType.TREE) && furion.canReach(tree, 550))
						.min(Comparator.comparingDouble(enemy::distanceSquaredNoCollision)).orElse(null);
				if (trees != null && CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, furion, OrderIds.forceofnature,
						trees.getX(), trees.getY())) continue;
			}
			final boolean hero = unit == furion || unit == tyrande;
			if (hero && unit.getLife() < unit.getMaximumLife() * (barrowWaypoint >= SATYR_CAMP - 2 ? 0.79f : 0.68f)) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")) continue;
				final float[] rear = BARROW_ROUTE[Math.max(0, barrowWaypoint - (barrowWaypoint >= SATYR_CAMP - 2 ? 4 : 2))];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, rear[0], rear[1]);
				continue;
			}
			if (assemblingSatyrEscort) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, BARROW_ROUTE, SATYR_RALLY, 180, routeSteps);
				continue;
			}
			if (gateInWay && !hero && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) continue;
			if (!hero && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, protector)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			final CUnit satyrBuilding = barrowWaypoint == SATYR_CAMP && !satyrsDefeated ? enemies.stream()
					.filter(target -> target.getPlayerIndex() == 6 && target.isBuilding())
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null) : null;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, satyrBuilding)) continue;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, BARROW_ROUTE, barrowWaypoint, 190, routeSteps);
		}
		if (tick >= nextMarchReport) {
			nextMarchReport = tick + 400;
			System.out.printf("[NightElfBarrowOpeningProbe] tick=%d waypoint=%d Furion=%.0f,%.0f life=%.0f army=%d gold=%d wood=%d food=%d/%d%n",
					tick, barrowWaypoint, furion.getX(), furion.getY(), furion.getLife(), troops.size(), player.getGold(),
					player.getLumber(), player.getFoodUsed(), player.getFoodCap());
			if (forwardBuilder != null && forwardMine == null) {
				final var order = forwardBuilder.getCurrentOrder();
				final var goal = order == null ? null : order.getTarget(game);
				System.out.printf("[NightElfBarrowOpeningProbe] outpost wisp %.0f,%.0f route=%d fork=%s destination=%s%n",
						forwardBuilder.getX(), forwardBuilder.getY(),
						routeSteps.getOrDefault(forwardBuilder.getHandleId(), -1), forwardBuilderReachedFork,
						goal == null ? "none" : Math.round(goal.getX()) + "," + Math.round(goal.getY()));
			}
		}
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static void finish() {
		finished = true;
		Gdx.app.exit();
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf barrow opening audit: " + message);
	}
}
