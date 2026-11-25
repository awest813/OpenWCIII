package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilitySearingArrows;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.CAbilitySentinel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

/** Uses ordinary movement and Hide orders to drive the installed Night Elf escape. */
public final class NightElfEscapePlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.nightElfEscapeAudit");
	private static final War3ID WAYGATE = War3ID.fromString("nwgt");
	private static final War3ID SPIRIT_TOWER = War3ID.fromString("uzg1");
	private static final War3ID MEAT_WAGON = War3ID.fromString("umtw");
	private static final War3ID BALLISTA = War3ID.fromString("ebal");
	private static final War3ID SEARING_ARROWS = War3ID.fromString("AHfa");
	private static final War3ID SOUTH_GATE = War3ID.fromString("LTe1");
	private static final String[] HEALING_CRATES = { "gg_dest_LTcr_1709", "gg_dest_LTcr_2399", "gg_dest_LTcr_1389" };
	private static final int BARRICADE_APPROACH = 20;
	private static final int WAYGATE_APPROACH = 28;
	private static final int DEMONIC_GATE_APPROACH = 31;
	private static final int REINFORCEMENTS_APPROACH = 36;
	private static final int FOUNTAIN_APPROACH = 43;
	private static final int UNDEAD_BASE_APPROACH = 45;
	private static final int SOUTH_GATE_APPROACH = 52;
	private static final float[][] ROUTE = {
			{-6000, 1360}, {-7216, 2576}, {-7504, 2800}, {-7504, 3120}, {-8240, 3120},
			{-8624, 2736}, {-8624, 2032}, {-7616, 1024}, {-8080, 560}, {-8624, 560},
			{-8656, -416}, {-8560, -528}, {-7568, -528}, {-6800, -752}, {-6384, -336},
			{-5968, -336}, {-5904, -1008}, {-7408, -2512}, {-7600, -2512}, {-8336, -3248},
			{-8336, -4144}, {-8736, -4416}, {-8496, -4656}, {-7440, -4656}, {-6832, -4048},
			{-6064, -3856}, {-5488, -3408}, {-3344, -3408}, {-2260, -4496},
			{-1040, -3776}, {-760, -3460}, {-976, -3024}, {-976, -1936}, {-2400, -1376},
			{-3136, -256}, {-3696, 656}, {-3792, 2304}, {-3760, 3032}, {-3500, 3380},
			{368, 3408}, {1168, 3872}, {1552, 3280}, {2864, 3024}, {3800, 3890},
			{3120, 3024}, {2384, 2288}, {2384, 1808}, {2384, 1328}, {2384, 848},
			{2384, 272}, {2472, 100}, {2400, -304}, {2384, -592},
			{2368, -768}, {2368, -1008}, {2368, -1488}, {2368, -1600}
	};
	private static int waypoint;
	private static int orderTick;
	private static int reportTick;
	private static int openingDeadline;
	private static int hideUntil;
	private static int hideAgainAt;
	private static boolean nativeHideVerified;
	private static boolean patrolHideVerified;
	private static boolean trackingSouthernDamage;
	private static boolean waygateVerified;
	private static boolean sentinelVerified;
	private static int siegeStarted;
	private static final Set<Integer> recoveringSiegeEscort = new HashSet<>();
	private static int fountainRecoveryStarted;
	private static boolean retreatingFromSiege;
	private static boolean returningToFountain;
	private static int fountainReturnProgress;
	private static int fountainRetreatAvailableAt;
	private static int fountainRetreatWaypoint;
	private static boolean southGateReported;
	private static boolean finished;
	private static Properties carryover;
	private static float bestApproach = Float.MAX_VALUE;
	private static int lastApproachTick;

	private NightElfEscapePlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) {
			return;
		}
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (NightElfOpeningPlaythroughProbe.CHAIN && !map.endsWith("/nightelf02.w3m") && !map.endsWith("/nightelf03.w3m")) return;
		final CUnit tyrande = object(globals, "udg_Tyrande");
		if (map.endsWith("/nightelf03.w3m")) {
			if (tyrande == null) return;
			check(carryover != null && carryover.equals(snapshot(tyrande)), "Tyrande carryover mismatch");
			System.out.println("[NightElfEscapeProbe] entered NightElf03; Tyrande carryover verified");
			finished = true;
			if (!NightElfOpeningPlaythroughProbe.CHAIN) Gdx.app.exit();
			return;
		}
		check(map.endsWith("/nightelf02.w3m"), "unexpected map " + map);
		if (!sentinelVerified) {
			final CUnit huntress = object(globals, "udg_HuntressShadeSpotter01");
			final CUnit shade = object(globals, "gg_unit_ushd_0026");
			final CAbilitySentinel sentinel = huntress == null ? null : huntress.getFirstAbilityOfType(CAbilitySentinel.class);
			if (sentinel != null && sentinel.getUsesRemaining() == 0 && shade != null
					&& (shade.isDead() || !shade.isInvisibleTo(game, 10))) {
				sentinelVerified = true;
				System.out.println("[NightElfEscapeProbe] the original Huntress used Sentinel to reveal the Shade");
			}
		}
		check(!"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		if ("Victory".equals(ui.getScoreDialogTitle())) {
			check(nativeHideVerified && patrolHideVerified && waygateVerified && sentinelVerified
					&& tyrande != null && !tyrande.isDead(), "victory without verified stealth, Sentinel, or waygate use");
			carryover = snapshot(tyrande);
			ui.keyDown(Input.Keys.C);
			return;
		}
		if (tyrande != null && !tyrande.isDead() && ui.getVisibleScriptDialog() != null
				&& globals.getGlobal("udg_GameOver").visit(BooleanJassValueVisitor.getInstance())) {
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final CQuest quest = object(globals, "udg_QuestEscape");
		final int tick = game.getGameTurnTick();
		if (tyrande == null || quest == null || !quest.isDiscovered() || tyrande.isHidden() || tyrande.isPaused()
				|| !ui.isUserControlEnabled() || game.isGamePaused() || tick < orderTick) {
			return;
		}
		check(!tyrande.isDead(), "Tyrande died at waypoint " + waypoint + " near "
				+ Math.round(tyrande.getX()) + ", " + Math.round(tyrande.getY()));
		if (!trackingSouthernDamage && waypoint >= UNDEAD_BASE_APPROACH) {
			trackingSouthernDamage = true;
			tyrande.addDamageTakenListener((simulation, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
				if (attacker != null && actual > 0 && (actual >= 75 || target.getLife() <= 165)) {
					System.out.printf("[NightElfEscapeProbe] tick=%d %s at %.0f,%.0f dealt %.0f to Tyrande at %.0f,%.0f life=%.0f%n",
							simulation.getGameTurnTick(), attacker.getTypeId(), attacker.getX(), attacker.getY(), actual,
							target.getX(), target.getY(), target.getLife());
				}
			});
		}
		orderTick = tick + 12;
		if (!nativeHideVerified) {
			if (openingDeadline == 0) openingDeadline = tick + 180;
			final var behavior = tyrande.getCurrentBehavior();
			check(tick < openingDeadline, "the original opening did not make Tyrande hide");
			if (behavior == null || behavior.getHighlightOrderId() != OrderIds.ambush || !tyrande.isInvisibleTo(game, 3)) return;
			nativeHideVerified = true;
			System.out.println("[NightElfEscapeProbe] original opening Hide order concealed Tyrande from the patrol player");
		}
		final boolean nearPatrol = game.getUnits().stream()
				.anyMatch(unit -> unit.getPlayerIndex() == 3 && !unit.isDead() && !unit.isHidden()
						&& !unit.getCurrentAttacks().isEmpty() && tyrande.distanceSquaredNoCollision(unit) < 600 * 600);
		if (hideUntil > tick && !game.isDay()) {
			if (!patrolHideVerified && nearPatrol && tyrande.isInvisibleTo(game, 3)) {
				patrolHideVerified = true;
				System.out.println("[NightElfEscapeProbe] ordinary Hide order concealed Tyrande near an active patrol");
			}
			return;
		}
		final boolean evadeUndead = waypoint > UNDEAD_BASE_APPROACH && !game.isDay()
				&& tyrande.getLife() < tyrande.getMaximumLife() * 0.60f && !tyrande.isInvisibleTo(game, 7);
		if (nearPatrol && tick >= hideAgainAt && tyrande.order(game, OrderIds.ambush, null)) {
			hideUntil = tick + 150;
			hideAgainAt = tick + 280;
			return;
		}
		final var orders = game.getDefaultPlayerUnitOrderExecutor(viewer.getLocalPlayerIndex());
		if (waypoint >= REINFORCEMENTS_APPROACH) prepareSearingArrows(viewer, orders, tyrande);
		final float missingLife = tyrande.getMaximumLife() - tyrande.getLife();
		final boolean needsHealing = tyrande.getLife() < tyrande.getMaximumLife() * 0.40f
				|| waypoint >= UNDEAD_BASE_APPROACH && missingLife >= 255;
		if (needsHealing && ((missingLife >= 350 && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "pghe"))
				|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "phea")
				|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "shea")
				|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, tyrande, "pghe"))) {
			System.out.printf("[NightElfEscapeProbe] tick=%d used a healing potion at %.0f,%.0f%n",
					tick, tyrande.getX(), tyrande.getY());
			return;
		}
		if (evadeUndead && tick >= hideAgainAt && tyrande.order(game, OrderIds.ambush, null)) {
			hideUntil = tick + 48;
			hideAgainAt = tick + 280;
			return;
		}
		if (waypoint == UNDEAD_BASE_APPROACH && clearUndeadApproach(viewer, orders, tyrande, tick)) {
			lastApproachTick = tick;
			return;
		}
		if (waypoint > UNDEAD_BASE_APPROACH && withdrawFromUndeadBase(viewer, orders, tyrande)) {
			lastApproachTick = tick;
			return;
		}
		if (waypoint > UNDEAD_BASE_APPROACH && waypoint < SOUTH_GATE_APPROACH
				&& fightAlongTheSouthernRoad(viewer, orders, tyrande)) {
			lastApproachTick = tick;
			return;
		}
		if (waypoint >= SOUTH_GATE_APPROACH - 2 && waypoint <= SOUTH_GATE_APPROACH
				&& attackSouthGate(viewer, orders, tyrande)) {
			lastApproachTick = tick;
			return;
		}
		final float[] destination = ROUTE[waypoint];
		final float distance = (float) tyrande.distanceSquaredNoCollision(destination[0], destination[1]);
		final int arrivalRadius = waypoint == UNDEAD_BASE_APPROACH - 1 ? 240 : waypoint == REINFORCEMENTS_APPROACH + 2 ? 160
						: waypoint > UNDEAD_BASE_APPROACH ? 112 : 88;
		if (waypoint == FOUNTAIN_APPROACH && distance < 224 * 224) {
			if (fountainRecoveryStarted == 0) fountainRecoveryStarted = tick;
			if (tick - fountainRecoveryStarted < 1600 && recoverAtFountain(viewer, orders, tyrande)) {
				lastApproachTick = tick;
				return;
			}
		}
		if (waypoint < ROUTE.length - 1 && distance < arrivalRadius * arrivalRadius) {
			waypoint++;
			bestApproach = Float.MAX_VALUE;
		}
		else if (distance < bestApproach - 16 * 16) {
			bestApproach = distance;
			lastApproachTick = tick;
		}
		else if (tick - lastApproachTick >= 1800) {
			final var nearby = game.getUnits().stream().filter(unit -> unit != tyrande && !unit.isDead()
					&& !unit.isHidden() && tyrande.distanceSquaredNoCollision(unit) < 240 * 240)
					.map(unit -> unit.getTypeId() + " at " + unit.getX() + ", " + unit.getY()).toList();
			throw new IllegalStateException("Night Elf escape audit: could not reach waypoint " + waypoint + " from "
					+ tyrande.getX() + ", " + tyrande.getY() + " with nearby units " + nearby);
		}
		if (waypoint < WAYGATE_APPROACH && collectHealingSupplies(viewer, orders, tyrande, globals)) {
			lastApproachTick = tick;
			return;
		}
		final String obstacleName = waypoint == BARRICADE_APPROACH ? "gg_dest_LTba_0282"
				: waypoint == DEMONIC_GATE_APPROACH ? "gg_dest_ATg1_0879" : null;
		final CDestructable obstacle = obstacleName == null ? null : object(globals, obstacleName);
		if (obstacle != null && !obstacle.isDead() && tyrande.canReach(obstacle, 440)
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, obstacle)) {
			return;
		}
		if (!waygateVerified && (waypoint == WAYGATE_APPROACH || waypoint == WAYGATE_APPROACH + 1)) {
			if (tyrande.distanceSquaredNoCollision(-1056, -3792) < 220 * 220) {
				waygateVerified = true;
				waypoint = WAYGATE_APPROACH + 2;
				System.out.println("[NightElfEscapeProbe] used the original waygate through a normal right-click");
			}
			else {
				final CUnit gate = game.getUnits().stream().filter(unit -> WAYGATE.equals(unit.getTypeId())
						&& unit.distanceSquaredNoCollision(-2304, -4480) < 100 * 100).findFirst().orElse(null);
				if (gate != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.smart, gate)) {
					return;
				}
				waypoint = WAYGATE_APPROACH;
			}
		}
		final float[] next = ROUTE[waypoint];
		CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, next[0], next[1]);
		if (waypoint >= REINFORCEMENTS_APPROACH) {
			int position = 0;
			for (final CUnit ally : game.getUnits()) {
				if (ally != tyrande && ally.getPlayerIndex() == tyrande.getPlayerIndex()
						&& !ally.isDead() && !ally.isHidden() && !ally.getCurrentAttacks().isEmpty()) {
					final var order = ally.getCurrentOrder();
					if (order != null && order.getOrderId() == OrderIds.attack
							&& order.getTarget(game) instanceof CDestructable gate && SOUTH_GATE.equals(gate.getTypeId())
							&& !gate.isDead()) continue;
					if (waypoint == FOUNTAIN_APPROACH + 1) {
						// Keep the fountain exit clear until Tyrande has reached the next bend.
						CampaignPlaythroughProbe.pointOrder(viewer, orders, ally,
								3584 + position % 4 * 96, 3680 + position / 4 * 90);
					}
					else {
						final int xOffset = (position % 3 - 1) * 88;
						final int yOffset = (position / 3) * 80;
						CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, ally, next[0] + xOffset, next[1] + yOffset);
					}
					position++;
				}
			}
		}
		ui.getCameraManager().setTarget(tyrande.getX(), tyrande.getY());
		if (tick >= reportTick) {
			reportTick = tick + 320;
			System.out.printf("[NightElfEscapeProbe] tick=%d waypoint=%d hero=%.0f,%.0f life=%.0f patrol=%s%n",
					tick, waypoint, tyrande.getX(), tyrande.getY(), tyrande.getLife(), nearPatrol);
		}
	}

	private static boolean fightAlongTheSouthernRoad(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande) {
		final var game = viewer.simulation;
		final var owner = game.getPlayer(tyrande.getPlayerIndex());
		final var escorts = game.getUnits().stream().filter(unit -> unit != tyrande && !unit.isDead() && !unit.isHidden()
				&& unit.getPlayerIndex() == owner.getId() && !unit.getCurrentAttacks().isEmpty() && tyrande.canReach(unit, 960)).toList();
		if (escorts.size() < 2) return false;
		final CUnit danger = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, owner.getId())
				&& !unit.getCurrentAttacks().isEmpty() && tyrande.canReach(unit, unit.isBuilding() ? 880 : 640))
				.min(Comparator.comparing((CUnit unit) -> unit.isBuilding()).thenComparingDouble(CUnit::getLife)).orElse(null);
		if (danger == null) return false;
		boolean engaging = false;
		for (final CUnit escort : escorts) {
			engaging |= CampaignPlaythroughProbe.targetOrder(viewer, orders, escort, OrderIds.attack, danger);
		}
		if (!engaging) return false;
		if (tyrande.getLife() >= tyrande.getMaximumLife() * 0.68f && !danger.isBuilding() && tyrande.canReach(danger, 470)) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, danger);
		}
		else if (tyrande.canReach(danger, danger.isBuilding() ? 680 : 440)) {
			final float[] refuge = ROUTE[Math.max(UNDEAD_BASE_APPROACH, waypoint - 2)];
			CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, refuge[0], refuge[1]);
		}
		else if (tyrande.getCurrentBehavior() == null
				|| tyrande.getCurrentBehavior().getHighlightOrderId() != OrderIds.holdposition) {
			HumanCampaignPlaythroughProbe.immediate(viewer, orders, tyrande, OrderIds.holdposition);
		}
		return true;
	}

	private static boolean withdrawFromUndeadBase(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande) {
		final float health = tyrande.getLife() / tyrande.getMaximumLife();
		final int tick = viewer.simulation.getGameTurnTick();
		if (returningToFountain && health >= 0.94f) {
			returningToFountain = false;
			waypoint = Math.min(waypoint, fountainRetreatWaypoint + 1);
			bestApproach = Float.MAX_VALUE;
			fountainRetreatAvailableAt = tick + 240;
		}
		final long escorts = viewer.simulation.getUnits().stream().filter(unit -> unit != tyrande
				&& unit.getPlayerIndex() == tyrande.getPlayerIndex() && !unit.isDead() && !unit.isHidden()
				&& !unit.getCurrentAttacks().isEmpty()).count();
		final boolean reachedNewGround = waypoint > fountainReturnProgress;
		final float threshold = reachedNewGround && escorts > 1 ? 0.80f : 0.42f;
		if (!returningToFountain && health < threshold && tick >= fountainRetreatAvailableAt) {
			returningToFountain = true;
			fountainReturnProgress = waypoint;
			fountainRetreatWaypoint = Math.max(FOUNTAIN_APPROACH,
					Math.min(waypoint - 2, SOUTH_GATE_APPROACH - 3));
			System.out.println("[NightElfEscapeProbe] withdrew Tyrande and the escort to the northern fountain");
		}
		if (!returningToFountain) return false;
		final float[] retreat = ROUTE[fountainRetreatWaypoint];
		if (tyrande.distanceSquaredNoCollision(retreat[0], retreat[1]) < 250 * 250) {
			if (fountainRetreatWaypoint == FOUNTAIN_APPROACH) {
				recoverAtFountain(viewer, orders, tyrande);
				return true;
			}
			fountainRetreatWaypoint--;
		}
		final float[] destination = ROUTE[fountainRetreatWaypoint];
		CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, destination[0], destination[1]);
		final var game = viewer.simulation;
		final int player = tyrande.getPlayerIndex();
		final CUnit pursuer = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && unit.getPlayerIndex() != player && !unit.getCurrentAttacks().isEmpty()
				&& !game.getPlayer(player).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& tyrande.distanceSquaredNoCollision(unit) < 480 * 480)
				.min(Comparator.comparingDouble(tyrande::distanceSquaredNoCollision)).orElse(null);
		int position = 0;
		for (final CUnit ally : game.getUnits()) {
			if (ally == tyrande || ally.getPlayerIndex() != tyrande.getPlayerIndex()
					|| ally.isDead() || ally.isHidden() || ally.getCurrentAttacks().isEmpty()) continue;
			if (pursuer != null && ally.distanceSquaredNoCollision(pursuer) < 640 * 640
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, ally, OrderIds.attack, pursuer)) continue;
			CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, ally, destination[0] + (position % 4 - 1) * 74,
					destination[1] + position / 4 * 70);
			position++;
		}
		return true;
	}

	private static boolean recoverAtFountain(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande) {
		final var game = viewer.simulation;
		final var escort = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& unit.getPlayerIndex() == tyrande.getPlayerIndex() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var owner = game.getPlayer(tyrande.getPlayerIndex());
		final CUnit pursuer = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, owner.getId()) && !unit.getCurrentAttacks().isEmpty()
				&& tyrande.canReach(unit, 620)).min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		if (pursuer == null && escort.stream().allMatch(unit -> unit.getLife() >= unit.getMaximumLife() * 0.90f)) return false;
		for (int position = 0; position < escort.size(); position++) {
			final CUnit unit = escort.get(position);
			if (pursuer != null && unit.canReach(pursuer, unit == tyrande ? 510 : 720)
					&& (unit != tyrande || unit.getLife() >= unit.getMaximumLife() * 0.63f
							&& !unit.isInvisibleTo(game, pursuer.getPlayerIndex()))
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, pursuer)) continue;
			final float x = unit == tyrande ? 3800 : 3600 + position % 4 * 94;
			final float y = unit == tyrande ? 3890 : 3584 + position / 4 * 94;
			if (unit != tyrande && pursuer != null) CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, x, y);
			else CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, x, y);
		}
		return true;
	}

	static void prepareSearingArrows(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande) {
		final var arrows = tyrande.getFirstAbilityOfType(CAbilitySearingArrows.class);
		if (arrows != null) {
			if (!arrows.isAutoCastOn()) {
				orders.issueImmediateOrder(tyrande.getHandleId(), arrows.getHandleId(), OrderIds.flamingarrows, false);
			}
			return;
		}
		final var canLearn = new BooleanAbilityActivationReceiver();
		final var hero = tyrande.getHeroData();
		hero.checkCanUse(viewer.simulation, tyrande, SEARING_ARROWS.getValue(), canLearn);
		if (canLearn.isOk()) {
			orders.issueImmediateOrder(tyrande.getHandleId(), hero.getHandleId(), SEARING_ARROWS.getValue(), false);
		}
	}

	private static boolean attackSouthGate(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande) {
		final var game = viewer.simulation;
		final CDestructable gate = game.getDestructables().stream().filter(destructable ->
				SOUTH_GATE.equals(destructable.getTypeId())
						&& destructable.distanceSquaredNoCollision(2368, -768) < 80 * 80).findFirst().orElse(null);
		if (gate == null) return false;
		if (gate.isDead()) {
			if (!southGateReported) {
				southGateReported = true;
				System.out.println("[NightElfEscapeProbe] opened the southern gate");
			}
			return false;
		}
		final var escort = game.getUnits().stream().filter(ally -> ally != tyrande && !ally.isDead()
				&& !ally.isHidden() && ally.getPlayerIndex() == tyrande.getPlayerIndex()
				&& !ally.getCurrentAttacks().isEmpty()).toList();
		final boolean artillery = escort.stream().anyMatch(ally -> BALLISTA.equals(ally.getTypeId()));
		if (!artillery && tyrande.canReach(gate, 600)) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, gate);
		}
		else {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, 2400, artillery ? 360 : -304);
		}
		int formation = 0;
		for (final CUnit ally : escort) {
			if (BALLISTA.equals(ally.getTypeId())) {
				if (ally.canReach(gate, 1_100)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, ally, OrderIds.attack, gate)) continue;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, ally, 2368, 112);
				continue;
			}
			CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, ally, 2304 + formation % 3 * 88,
					(artillery ? -160 : -544) + formation / 3 * 80);
			formation++;
		}
		return true;
	}

	private static boolean collectHealingSupplies(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande, final GlobalScope globals) {
		for (final String name : HEALING_CRATES) {
			final CDestructable crate = object(globals, name);
			if (crate != null && !crate.isDead() && tyrande.distanceSquaredNoCollision(crate) < 470 * 470
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, crate)) return true;
		}
		final var healing = viewer.simulation.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
				&& item.getContainedInventory() == null && tyrande.distanceSquaredNoCollision(item) < 460 * 460)
				.filter(item -> "phea".equals(item.getTypeId().toString()) || "pghe".equals(item.getTypeId().toString())
						|| "shea".equals(item.getTypeId().toString()))
				.findFirst().orElse(null);
		return CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.smart, healing);
	}

	private static boolean clearUndeadApproach(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit tyrande, final int tick) {
		final var game = viewer.simulation;
		final var owner = game.getPlayer(tyrande.getPlayerIndex());
		final var escort = game.getUnits().stream().filter(ally -> ally != tyrande && !ally.isDead()
				&& !ally.isHidden() && ally.getPlayerIndex() == owner.getId()
				&& !ally.getCurrentAttacks().isEmpty()).toList();
		final var hostiles = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !unit.getCurrentAttacks().isEmpty()
				&& unit.getPlayerIndex() != owner.getId()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.getX() > 900 && unit.getX() < 3500 && unit.getY() > -1088 && unit.getY() < 2600)
				.toList();
		final CUnit threat = hostiles.stream()
				.filter(unit -> Math.abs(unit.getX() - 2384) < 820
						&& (unit.isBuilding() || SPIRIT_TOWER.equals(unit.getTypeId()) || MEAT_WAGON.equals(unit.getTypeId())))
				.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(2384, 2288))).orElse(null);
		if (threat == null || escort.stream().noneMatch(ally -> BALLISTA.equals(ally.getTypeId()))) return false;
		if (siegeStarted == 0) siegeStarted = tick;
		check(tick - siegeStarted < 9600, "the army could not clear the Undead towers");
		final float stagingX = Math.max(1920, Math.min(2688, Math.round(threat.getX() / 128) * 128));
		final float stagingY = Math.min(2384, Math.round(threat.getY() / 128) * 128 + 800);
		final CUnit closeEnemy = hostiles.stream().filter(unit -> !unit.isBuilding()
				&& unit.isVisible(game, owner.getId()) && unit.distanceSquaredNoCollision(stagingX, stagingY) < 720 * 720)
				.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(stagingX, stagingY))).orElse(null);
		retreatingFromSiege = tyrande.getLife() < tyrande.getMaximumLife() * (retreatingFromSiege ? 0.85f : 0.64f);
		if (retreatingFromSiege) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, 2472, 2560);
		}
		else if (closeEnemy == null || !tyrande.canReach(closeEnemy, 640)
				|| !CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.flamingarrowstarg, closeEnemy)
						&& !CampaignPlaythroughProbe.targetOrder(viewer, orders, tyrande, OrderIds.attack, closeEnemy)) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, tyrande, stagingX, stagingY + 220);
		}
		for (int position = 0; position < escort.size(); position++) {
			final CUnit ally = escort.get(position);
			final boolean siegeWeapon = BALLISTA.equals(ally.getTypeId());
			final float life = ally.getLife() / ally.getMaximumLife();
			if (life < (siegeWeapon ? 0.25f : 0.38f)) recoveringSiegeEscort.add(ally.getHandleId());
			else if (life >= 0.9f) recoveringSiegeEscort.remove(ally.getHandleId());
			if (recoveringSiegeEscort.contains(ally.getHandleId())) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, ally, 3728 + position % 3 * 62,
						3712 + position % 4 * 48);
				continue;
			}
			if (siegeWeapon && threat.isVisible(game, owner.getId())
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, ally, OrderIds.attack, threat)) continue;
			if (siegeWeapon && threat.isBuilding() && CampaignPlaythroughProbe.attackPointOrder(viewer, orders, ally,
					OrderIds.attackground, threat.getX(), threat.getY())) continue;
			if (!siegeWeapon && closeEnemy != null && ally.canReach(closeEnemy, 830)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, ally, OrderIds.attack, closeEnemy)) continue;
			final int row = position / 4;
			final int column = position % 4;
			final float x = stagingX + (column - 1) * 90;
			final float y = siegeWeapon ? Math.min(stagingY + 32 * row, threat.getY() + 680) : stagingY + 36 + row * 56;
			if (ally.distanceSquaredNoCollision(x, y) >= 72 * 72) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, ally, x, y);
			}
			else if (ally.getCurrentBehavior() != null
					&& ally.getCurrentBehavior().getHighlightOrderId() != OrderIds.holdposition) {
				HumanCampaignPlaythroughProbe.immediate(viewer, orders, ally, OrderIds.holdposition);
			}
		}
		if (tick >= reportTick) {
			reportTick = tick + 320;
			System.out.printf("[NightElfEscapeProbe] tick=%d siege target=%s at %.0f,%.0f life=%.0f visible=%s escort=%d ballistas=%s%n",
					tick, threat.getTypeId(), threat.getX(), threat.getY(), threat.getLife(), threat.isVisible(game, owner.getId()),
					escort.size(), escort.stream().filter(ally -> BALLISTA.equals(ally.getTypeId()))
							.map(ally -> String.format(Locale.ROOT, "%.0f,%.0f life=%.0f", ally.getX(), ally.getY(), ally.getLife())).toList());
		}
		return true;
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf escape audit: " + message);
	}
}
