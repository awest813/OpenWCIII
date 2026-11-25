package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;

/** Plays the original NightElf03 expedition to Moonglade and the Horn of Cenarius. */
public final class NightElfAwakeningPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.nightElfAwakeningAudit");
	private static boolean handoffVerified;
	private static final War3ID WISP = War3ID.fromString("ewsp");
	private static final War3ID MINE = War3ID.fromString("egol");
	private static final War3ID ARCHER = War3ID.fromString("earc");
	private static final War3ID MOON_WELL = War3ID.fromString("emow");
	private static final Map<Integer, Integer> reinforcementWaypoints = new HashMap<>();
	private static final float[][] ROUTE = {
			{ -960, -6224 }, { 208, -6224 }, { 1168, -6096 }, { 1456, -5456 }, { 1456, -4816 },
			{ 2000, -4176 }, { 2640, -4048 }, { 3280, -4048 }, { 3760, -3664 }, { 3760, -3024 },
			{ 3760, -2384 }, { 3760, -1744 }, { 3760, -1104 }, { 3504, -464 }, { 3248, 496 },
			{ 3088, 784 }, { 2960, 880 }, { 2864, 1008 }, { 2800, 1328 },
			{ 2800, 1776 }, { 2800, 2416 }, { 2512, 2768 }, { 2000, 3216 },
			{ 1616, 3792 }, { 1616, 4432 }, { 1616, 4752 }, { 1888, 5088 }, { 1520, 5232 },
			{ 880, 5296 }, { 560, 5264 }, { -80, 4624 }, { -416, 4016 }, { -976, 3952 },
			{ -1616, 4400 }, { -2256, 5008 }, { -2576, 5328 }, { -2160, 5360 }, { -1744, 5376 }
	};
	private static int orderTick;
	private static int reportTick;
	private static int waypoint;
	private static boolean expeditionStarted;
	private static boolean reachedMoonglade;
	private static boolean keepersDefeated;
	private static boolean withdrawing;
	private static int retreatWaypoint;
	private static Properties carriedTyrande;

	private NightElfAwakeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || handoffVerified || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final String path = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (NightElfOpeningPlaythroughProbe.CHAIN && !path.endsWith("/nightelf03.w3m") && !path.endsWith("/nightelf04.w3m")) return;
		final CUnit tyrande = object(globals, "udg_Tyrande");
		if (path.endsWith("/nightelf04.w3m")) {
			if (tyrande == null) return;
			check(carriedTyrande != null && carriedTyrande.equals(snapshot(tyrande)), "Tyrande carryover mismatch");
			System.out.println("[NightElfAwakeningProbe] entered NightElf04 and verified Tyrande carryover");
			handoffVerified = true;
			if (!NightElfOpeningPlaythroughProbe.CHAIN) Gdx.app.exit();
			return;
		}
		check(path.endsWith("/nightelf03.w3m"), "unexpected map " + path);
		if (tyrande == null) return;
		check(!tyrande.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		final CUnit furion = object(globals, "udg_Furion");
		if (furion != null && reachedMoonglade && keepersDefeated) {
			if (carriedTyrande == null) {
				carriedTyrande = snapshot(tyrande);
				System.out.println("[NightElfAwakeningProbe] the original Horn of Cenarius awakened Furion");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final int tick = game.getGameTurnTick();
		check(tick < 44_000, "the expedition stalled");
		if (!ui.isUserControlEnabled() || tyrande.isHidden() || tyrande.isPaused() || game.isGamePaused()
				|| tick < orderTick) return;
		orderTick = tick + 16;
		final int player = tyrande.getPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final List<CUnit> units = game.getUnitsIncludingPending();
		final List<CUnit> owned = units.stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()).toList();
		final List<CUnit> army = owned.stream().filter(unit -> unit != tyrande && !unit.isBuilding() && !unit.isHidden()
				&& !unit.isPaused() && !WISP.equals(unit.getTypeId()) && !unit.getCurrentAttacks().isEmpty()).toList();
		final List<CUnit> keepers = object(globals, "udg_KeeperGroup");
		final CQuestItem isle = object(globals, "udg_HornOfCenariusReq04");
		final CQuestItem guardians = object(globals, "udg_HornOfCenariusReq01");
		if (!reachedMoonglade && isle != null && isle.isCompleted()) {
			reachedMoonglade = true;
			System.out.println("[NightElfAwakeningProbe] reached the original Moonglade Isle objective");
		}
		if (!keepersDefeated && guardians != null && guardians.isCompleted() && keepers != null
				&& keepers.stream().noneMatch(unit -> !unit.isDead() && game.containsUnit(unit))) {
			keepersDefeated = true;
			System.out.println("[NightElfAwakeningProbe] defeated the original Keepers of the Grove");
		}
		prepareEconomy(viewer, orders, owned, army.size());
		NightElfEscapePlaythroughProbe.prepareSearingArrows(viewer, orders, tyrande);
		if (army.size() >= 12) expeditionStarted = true;
		final float[] destination = ROUTE[waypoint];
		final boolean keeperApproach = reachedMoonglade && !keepersDefeated;
		final long vanguard = army.stream().filter(unit -> unit.distanceSquaredNoCollision(destination[0],
				destination[1]) < 270 * 270).count();
		if (expeditionStarted && waypoint < ROUTE.length - 1 && (keeperApproach && vanguard >= 4
				|| tyrande.distanceSquaredNoCollision(destination[0], destination[1]) < 200 * 200)) waypoint++;
		final float[] next = ROUTE[waypoint];
		final var playerData = game.getPlayer(player);
		final var enemies = units.stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& unit.getPlayerIndex() != player && !playerData.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player)).toList();
		if (expeditionStarted) {
			final boolean wasWithdrawing = withdrawing;
			final float health = tyrande.getLife() / tyrande.getMaximumLife();
			withdrawing = health < (withdrawing ? 0.93f : keeperApproach ? 0.80f : 0.65f);
			if (withdrawing && !wasWithdrawing) retreatWaypoint = Math.max(0, waypoint - 3);
			final boolean healed = health < 0.72f && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "pghe")
					|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "phea"));
			final CUnit close = enemies.stream().filter(enemy -> tyrande.canReach(enemy, 570))
					.min(Comparator.comparingDouble(tyrande::distanceSquaredNoCollision)).orElse(null);
			if (withdrawing && !healed) {
				final float[] refuge = ROUTE[retreatWaypoint];
				if (close != null && retreatWaypoint > 0
						&& tyrande.distanceSquaredNoCollision(refuge[0], refuge[1]) < 240 * 240) retreatWaypoint--;
				final float[] shelter = ROUTE[retreatWaypoint];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, shelter[0], shelter[1]);
			}
			else if (!healed && keeperApproach) {
				final float[] support = ROUTE[Math.max(0, waypoint - 2)];
				if (close != null && tyrande.getCurrentAttacks().stream()
						.anyMatch(attack -> tyrande.canReach(close, attack.getRange() - 60))) {
					CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, close);
				}
				else CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, support[0], support[1]);
			}
			else if (!healed && !CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, close)) {
				final long escort = army.stream().filter(unit -> tyrande.distanceSquaredNoCollision(unit) < 850 * 850).count();
				if (escort >= Math.min(army.size(), 5)) CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, next[0], next[1]);
				else HumanCampaignPlaythroughProbe.immediate(viewer, orders, tyrande, OrderIds.holdposition);
			}
			for (int index = 0; index < army.size(); index++) {
				final CUnit unit = army.get(index);
				final int range = unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0);
				final CUnit enemy = enemies.stream().filter(candidate -> unit.canReach(candidate, range + 160))
						.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
				final boolean needsWaypoints = keeperApproach ? unit.distanceSquaredNoCollision(next[0], next[1]) > 400 * 400
						: unit.distanceSquaredNoCollision(tyrande) > 950 * 950;
				if (needsWaypoints) {
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, ROUTE,
							keeperApproach ? waypoint : Math.max(0, waypoint - 1), 180,
							reinforcementWaypoints);
					continue;
				}
				final float x = next[0] + (index % 4 - 1) * 82;
				final float y = next[1] + index / 4 * 66;
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, x, y);
			}
		}
		if (tick >= reportTick) {
			reportTick = tick + 400;
			final int trees = globals.getGlobal("udg_TreesAlive").visit(IntegerJassValueVisitor.getInstance());
			final long escort = army.stream().filter(unit -> tyrande.distanceSquaredNoCollision(unit) < 850 * 850).count();
			System.out.printf("[NightElfAwakeningProbe] tick=%d route=%d Tyrande=%.0f,%.0f life=%.0f army=%d escort=%d trees=%d gold=%d wood=%d%n",
					tick, waypoint, tyrande.getX(), tyrande.getY(), tyrande.getLife(), army.size(), escort, trees,
					playerData.getGold(), playerData.getLumber());
		}
		ui.getCameraManager().setTarget(tyrande.getX(), tyrande.getY());
	}

	private static void prepareEconomy(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> owned, final int armySize) {
		final var game = viewer.simulation;
		final CUnit town = owned.stream().filter(unit -> "etol".equals(unit.getTypeId().toString())).findFirst().orElse(null);
		if (town == null) return;
		final var player = game.getPlayer(town.getPlayerIndex());
		final List<CUnit> wisps = owned.stream().filter(unit -> WISP.equals(unit.getTypeId())).toList();
		final CUnit mine = owned.stream().filter(unit -> MINE.equals(unit.getTypeId()) && !unit.isConstructing()).findFirst().orElse(null);
		if (wisps.size() < 7 && town.getBuildQueue()[0] == null) HumanCampaignPlaythroughProbe.train(viewer, orders, town, "ewsp");
		for (final CUnit building : owned) {
			if (!building.isConstructing() && "eaom".equals(building.getTypeId().toString())
					&& armySize + Arrays.stream(building.getBuildQueue()).filter(ARCHER::equals).count() < 20
					&& Arrays.stream(building.getBuildQueue()).filter(ARCHER::equals).count() <= 1) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, "earc");
			}
		}
		final long wells = owned.stream().filter(unit -> MOON_WELL.equals(unit.getTypeId())).count();
		for (int index = 0; index < wisps.size(); index++) {
			final CUnit wisp = wisps.get(index);
			if (wisp.isHidden() || wisp.getCurrentOrder() != null && wisp.getCurrentOrder().getOrderId() == MOON_WELL.getValue()) continue;
			if (index < 4 && mine != null) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.smart, mine);
			}
			else {
				if (wells < 7 && player.getFoodCap() - player.getFoodUsed() < 6
						&& NightElfOpeningPlaythroughProbe.buildNear(viewer, wisp, MOON_WELL, -1800 + wells * 150, -5536)) continue;
				final var tree = game.getDestructables().stream().filter(item -> !item.isDead()
						&& item.getDestType().getTargetedAs().contains(CTargetType.TREE)
						&& town.distanceSquaredNoCollision(item) < 1600 * 1600)
						.min(Comparator.comparingDouble(wisp::distanceSquaredNoCollision)).orElse(null);
				CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.wispharvest, tree);
			}
		}
	}

	private static Properties snapshot(final CUnit unit) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit));
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf awakening audit: " + message);
	}
}
