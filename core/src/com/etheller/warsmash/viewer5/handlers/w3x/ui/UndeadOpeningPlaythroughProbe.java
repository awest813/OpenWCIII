package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

/** Drives the installed Undead opening through its ordinary rescue, combat, and Continue actions. */
public final class UndeadOpeningPlaythroughProbe {
	private static final String[] SUPPLY_CRATES = {"gg_dest_LTcr_0012", "gg_dest_LTcr_1433", "gg_dest_LTcr_0788", "gg_dest_LTcr_1063"};
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.undeadOpeningAudit");
	private static final boolean SKIP_INTRO = Boolean.getBoolean("warsmash.undeadOpeningSkipIntro");
	private static final War3ID HIDING_ACOLYTE = War3ID.fromString("nC00");
	private static final War3ID GHOUL = War3ID.fromString("ugho");
	private static final War3ID DEATH_COIL = War3ID.fromString("AUdc");
	private static final float[][] WEST_RESCUES = {
			{-1968, 2416}, {-2048, 2720}, {-2749, 3221}, {-3778, 3398},
			{-3258, 1480}, {-4064, 1824}, {-4464, 1680}, {-4064, 1816}, {-3504, 1968},
			{-3024, 1968}, {-2704, 2192}, {-2064, 2224}, {-1744, 2416}, {-1584, 2384},
			{-1520, 2064}, {-1168, 1584}, {-1136, 1264}, {-1360, 944}, {-1840, 688},
			{-2160, 368}, {-2480, 144}, {-3440, 144}, {-3760, 400}, {-4080, 720},
			{-3888, 656}, {-3568, 656}, {-3248, 464}, {-2928, 336}, {-2608, 16}, {-2128, -464},
			{-2928, -432}, {-3088, -432}, {-3280, -1008}, {-3920, -1008}, {-4336, -1552},
			{-4464, -1968}, {-3760, -2096}, {-3440, -2224}, {-3120, -2352}, {-2640, -2768},
			{-2192, -2896}, {-2128, -2416}, {-2128, -2256}, {-2128, -2736}, {-2064, -2928},
			{-1584, -2928}, {-1360, -2640}, {-1232, -2320}, {-784, -1840}, {-720, -1520}, {-656, -1360},
			{-656, -1936}, {-688, -2256}, {-752, -2576}, {-1072, -2896}, {-1392, -3216}, {-1584, -3856},
			{-1904, -4176}, {-2224, -4400}, {-2544, -4400}, {-2864, -4400}, {-3312, -4272},
			{-3632, -4272}, {-3952, -4272}, {-4272, -4592}
	};
	private static int westRescueWaypoint = -1;
	private static int westRescueDeadline;
	private static final War3ID UNHOLY_AURA = War3ID.fromString("AUau");
	private static final War3ID DEATH_PACT = War3ID.fromString("AUdp");
	private static final War3ID CANNIBALIZE = War3ID.fromString("Acan");
	private static final float[][] GRAVEYARD_ROUTE = {
			{-688, -2064}, {-1552, -2928}, {-1552, -3856}, {-1264, -4072},
			{-2928, -2928}, {-3760, -2096}, {-4336, -2096},
			{-4464, -1968}, {-4464, -1744}, {-4336, -1552}, {-4336, -1424}, {-3920, -1008},
			{-3808, 400}, {-2896, 600}, {-1712, 304}, {-1424, 304}, {-1296, 176}, {-1296, -48}, {-1456, 0}
	};
	private static final float[][] GRAVEYARD_REFUGES = {
			{-2544, 1104}, {-3258, 1480}, {-1968, 1968}, {-1776, 2416}, {-2048, 2720}, {-2000, 3200}
	};
	private static final float[][] EARLY_BRIDGE_ESCAPE = {
			{-592, -2064}, {-368, -2064}, {-144, -2064}, {80, -2064}, {304, -2064},
			{528, -2064}, {752, -2064}, {976, -2192}
	};
	private static final float[][] NORTH_AND_EAST_RESCUES = {
			{-784, 2416}, {-272, 2928}, {-118, 2913}, {112, 2672}, {112, 1840},
			{112, 2672}, {80, 3280}, {944, 3280}, {1232, 3248}, {1424, 2960},
			{2992, 2960}, {3246, 3223}, {3248, 2544}, {3824, 1968}, {4240, 1616}, {4480, 1600},
			{3850, 1265}, {4093, 240}, {2553, 294}, {2480, 80}, {1808, 566}, {1568, 784},
			{1100, 1200}, {112, 2209}, {-486, 2455}, {-1776, 2416}
	};
	private static final float[][] NORTHWEST_FALLBACK = {
			{-784, 2416}, {-1776, 2416}, {-2048, 2720}, {-2000, 3200}
	};
	private static final float EASTERN_RALLY_X = 2992;
	private static final float EASTERN_RALLY_Y = 2960;
	private static final float[][] WESTERN_FALLBACK = {
			{-1968, 2416}, {-2048, 2720}, {-2416, 2736}, {-2416, 2512},
			{-2704, 2192}, {-3504, 1968}, {-4064, 1816}, {-4272, 976}
	};
	private static final Map<Integer, Integer> retryTicks = new HashMap<>();
	private static final Set<Integer> blockedSupplies = new HashSet<>();
	private static int attemptedSupply;
	private static int supplyOrderTick;
	private static CUnit acolyte;
	private static double nearestAcolyteDistance;
	private static int progressTick;
	private static int orderTick;
	private static int reportTick;
	private static int graveyardWaypoint;
	private static float safeX = 3888;
	private static float safeY = -2992;
	private static float rearRefugeX;
	private static float rearRefugeY;
	private static float forwardRefugeX;
	private static float forwardRefugeY;
	private static boolean recovering;
	private static float retreatProgressX;
	private static float retreatProgressY;
	private static int retreatProgressTick;
	private static CUnit blockingGuard;
	private static boolean securedGraveyard;
	private static int graveyardRefuge;
	private static int earlyBridgeEscapeStep;
	private static int northAndEastWaypoint;
	private static int retreatWaypoint;
	private static int waypointDeadline;
	private static boolean initialSuppliesCollected;
	private static boolean shadeScouted;
	private static boolean introSkipped;
	private static boolean monitoringDamage;
	private static boolean finished;
	private static Properties carriedHero;

	private UndeadOpeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) {
			return;
		}
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		final CUnit arthas = object(globals, "udg_Arthas");
		if (map.endsWith("/undead02.w3m")) {
			if (arthas == null) {
				return;
			}
			check(carriedHero != null && carriedHero.equals(snapshot(arthas)), "Arthas carryover mismatch");
			System.out.println("[UndeadOpeningProbe] entered Undead02; Arthas carryover verified");
			finished = true;
			Gdx.app.exit();
			return;
		}
		check(map.endsWith("/undead01.w3m"), "unexpected map " + map);
		if (SKIP_INTRO && !introSkipped && !ui.isUserControlEnabled()) {
			final Trigger intro = object(globals, "gg_trg_Intro_Cancel");
			if (intro != null && intro.isEnabled()) introSkipped = ui.keyDown(Input.Keys.ESCAPE);
		}
		if ("Defeat".equals(ui.getScoreDialogTitle())) {
			throw new IllegalStateException("Undead opening audit: native defeat");
		}
		if (completed(globals, "udg_QuestRescue")) {
			if (carriedHero == null) {
				check(completed(globals, "udg_QuestGraveyard"), "optional graveyard objective incomplete");
				carriedHero = snapshot(arthas);
				System.out.println("[UndeadOpeningProbe] native acolyte rescue and optional graveyard completed");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final int tick = game.getGameTurnTick();
		if (arthas == null || arthas.isHidden() || arthas.isPaused() || !ui.isUserControlEnabled()
				|| game.isGamePaused() || tick < orderTick) {
			return;
		}
		check(!arthas.isDead(), "Arthas died before the rescue");
		if (!monitoringDamage) {
			monitoringDamage = true;
			arthas.addDamageTakenListener((sim, attacker, victim, attack, ranged, type, damage, bonus, actual) -> {
				if (actual > 0 && victim.getLife() < victim.getMaximumLife() * 0.32f) {
					System.out.printf("[UndeadOpeningProbe] Arthas hurt at %.0f,%.0f by %s for %.0f life=%.0f speed=%d%n",
							victim.getX(), victim.getY(), attacker == null ? type : attacker.getTypeId(), actual,
							victim.getLife(), victim.getSpeed());
				}
			});
		}
		orderTick = tick + 20;
		final int rescued = globals.getGlobal("udg_RescuedAcolytesCount").visit(IntegerJassValueVisitor.getInstance());
		final boolean graveyardComplete = completed(globals, "udg_QuestGraveyard");
		final boolean visitGraveyard = rescued >= 1 && !graveyardComplete && initialSuppliesCollected;
		final List<CUnit> available = object(globals, "udg_RescuableAcolytes");
		if (reportTick == 0) {
			final long placed = game.getUnitsIncludingPending().stream()
					.filter(unit -> HIDING_ACOLYTE.equals(unit.getTypeId()) && unit.getPlayerIndex() == 0).count();
			final int groupSize = available == null ? 0 : available.size();
			check(groupSize >= 19, "initial rescue group has " + groupSize + " candidates among " + placed + " placed acolytes");
		}
		if (visitGraveyard && graveyardWaypoint < GRAVEYARD_ROUTE.length - 1) {
			final float[] waypoint = GRAVEYARD_ROUTE[graveyardWaypoint];
			if (arthas.distanceSquaredNoCollision(waypoint[0], waypoint[1]) < 128 * 128) {
				graveyardWaypoint++;
			}
		}
		if (securedGraveyard && northAndEastWaypoint == NORTH_AND_EAST_RESCUES.length && westRescueWaypoint < 0) {
			westRescueWaypoint = 0;
			westRescueDeadline = tick + 1800;
		}
		if (followingWestRoute()) {
			final float[] west = WEST_RESCUES[westRescueWaypoint];
			if (recovering) {
				westRescueDeadline = tick + 1800;
			}
			else if (arthas.distanceSquaredNoCollision(west[0], west[1]) < 185 * 185 || tick >= westRescueDeadline) {
				westRescueWaypoint++;
				westRescueDeadline = tick + 1800;
			}
		}
		final boolean westRescues = followingWestRoute();
		final boolean northAndEast = securedGraveyard && !westRescues
				&& northAndEastWaypoint < NORTH_AND_EAST_RESCUES.length;
		final boolean remainingRescues = securedGraveyard && !westRescues && !northAndEast;
		if (westRescues || remainingRescues) orderTick = tick + 8;
		if (rescued == 0) {
			acolyte = object(globals, "udg_BridgeAcolyte");
		}
		else if (!visitGraveyard && !westRescues && !northAndEast) {
			selectAcolyte(arthas, tick, available, remainingRescues ? game.getUnitsIncludingPending() : List.of(), viewer);
		}
		final float[] waypoint = visitGraveyard ? GRAVEYARD_ROUTE[graveyardWaypoint]
				: westRescues ? WEST_RESCUES[westRescueWaypoint]
				: northAndEast ? NORTH_AND_EAST_RESCUES[northAndEastWaypoint] : null;
		if (northAndEast && !recovering) {
			if (waypointDeadline == 0) waypointDeadline = tick + 1100;
			if (arthas.distanceSquaredNoCollision(waypoint[0], waypoint[1]) < 70 * 70 || tick >= waypointDeadline) {
				northAndEastWaypoint++;
				waypointDeadline = 0;
				if (northAndEastWaypoint == NORTH_AND_EAST_RESCUES.length) {
					acolyte = null;
					westRescueDeadline = tick + 1800;
				}
			}
		}
		else {
			waypointDeadline = 0;
		}
		final float x = waypoint != null ? waypoint[0] : acolyte == null ? arthas.getX() : acolyte.getX();
		final float y = waypoint != null ? waypoint[1] : acolyte == null ? arthas.getY() : acolyte.getY();
		final int player = viewer.getLocalPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final CUnit shade = object(globals, "udg_Shade01");
		if (shade != null && shade.getPlayerIndex() == player && !shade.isHidden() && !shade.isDead()) {
			if (!shadeScouted) {
				check(shade.isInvisibleTo(game, 0), "the rescued Shade is visible to the Human guards");
				shadeScouted = true;
				System.out.println("[UndeadOpeningProbe] recruited the original Shade and verified its stealth");
			}
			CampaignPlaythroughProbe.pointOrder(viewer, orders, shade, x, y);
		}
		final var nearbyEnemies = game.getUnitsIncludingPending().stream()
				.filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
						&& !unit.getCurrentAttacks().isEmpty() && unit.getPlayerIndex() != player
						&& !game.getPlayer(player).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
						&& arthas.distanceSquaredNoCollision(unit) < 900 * 900).toList();
		final var target = nearbyEnemies.stream().filter(unit -> arthas.distanceSquaredNoCollision(unit) < 500 * 500)
				.min(Comparator.comparing((CUnit unit) -> !"hkni".equals(unit.getTypeId().toString()))
						.thenComparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
		final var skills = arthas.getAbilityLevel(DEATH_COIL) == 0 ? List.of(DEATH_COIL)
				: arthas.getAbilityLevel(UNHOLY_AURA) == 0 ? List.of(UNHOLY_AURA, DEATH_PACT)
						: arthas.getAbilityLevel(DEATH_PACT) == 0 ? List.of(DEATH_PACT, DEATH_COIL)
								: List.of(UNHOLY_AURA, DEATH_COIL, DEATH_PACT);
		for (final War3ID skill : skills) {
			final var canLearn = new BooleanAbilityActivationReceiver();
			arthas.getHeroData().checkCanUse(game, arthas, skill.getValue(), canLearn);
			if (canLearn.isOk()) {
				orders.issueImmediateOrder(arthas.getHandleId(), arthas.getHeroData().getHandleId(), skill.getValue(), false);
				break;
			}
		}
		final float health = arthas.getLife() / arthas.getMaximumLife();
		if (health < 0.54f && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")) return;
		if (initialSuppliesCollected && health < 0.62f
				&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea")) return;
		if (securedGraveyard && health < 0.59f
				&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "shea")) return;
		if (arthas.getMana() < arthas.getMaximumMana() * 0.3f
				&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pgma")) return;
		if (graveyardComplete && !securedGraveyard) {
			final float[] refuge = GRAVEYARD_REFUGES[graveyardRefuge];
			final boolean reachedRefuge = arthas.distanceSquaredNoCollision(refuge[0], refuge[1]) < 270 * 270;
			if (reachedRefuge && target != null && health < 0.86f && graveyardRefuge < GRAVEYARD_REFUGES.length - 1) {
				graveyardRefuge++;
			}
			safeX = GRAVEYARD_REFUGES[graveyardRefuge][0];
			safeY = GRAVEYARD_REFUGES[graveyardRefuge][1];
			if (!securedGraveyard && reachedRefuge && target == null && health >= 0.85f) {
				securedGraveyard = true;
				rearRefugeX = safeX;
				rearRefugeY = safeY;
				forwardRefugeX = safeX;
				forwardRefugeY = safeY;
				acolyte = null;
			}
		}
		else if (securedGraveyard && !recovering && nearbyEnemies.isEmpty() && health > 0.9f) {
			if (arthas.distanceSquaredNoCollision(forwardRefugeX, forwardRefugeY) > 420 * 420) {
				rearRefugeX = forwardRefugeX;
				rearRefugeY = forwardRefugeY;
				forwardRefugeX = arthas.getX();
				forwardRefugeY = arthas.getY();
			}
			safeX = arthas.getX();
			safeY = arthas.getY();
		}
		else if (!recovering && target == null && health > 0.9f
				&& graveyardWaypoint >= 2 && graveyardWaypoint < 12) {
			safeX = GRAVEYARD_ROUTE[graveyardWaypoint - 2][0];
			safeY = GRAVEYARD_ROUTE[graveyardWaypoint - 2][1];
		}
		final boolean wasRecovering = recovering;
		final boolean crossingEasternVillages = northAndEast && northAndEastWaypoint >= 11 && northAndEastWaypoint <= 20;
		final boolean takingWesternExit = northAndEast && northAndEastWaypoint >= 20 && northAndEastWaypoint < 25;
		// The village route loops back north through the patrols. Continue west once past the last village.
		recovering = !takingWesternExit
				&& health < (recovering ? 0.92f : crossingEasternVillages ? 0.52f : northAndEast ? 0.82f : 0.75f);
		if (recovering && visitGraveyard && graveyardWaypoint < 6) {
			if (!wasRecovering) earlyBridgeEscapeStep = 0;
			final float[] refuge = EARLY_BRIDGE_ESCAPE[earlyBridgeEscapeStep];
			if (target != null && earlyBridgeEscapeStep < EARLY_BRIDGE_ESCAPE.length - 1
					&& arthas.distanceSquaredNoCollision(refuge[0], refuge[1]) < 166 * 166) earlyBridgeEscapeStep++;
			safeX = EARLY_BRIDGE_ESCAPE[earlyBridgeEscapeStep][0];
			safeY = EARLY_BRIDGE_ESCAPE[earlyBridgeEscapeStep][1];
		}
		if (!recovering || !wasRecovering || arthas.distanceSquaredNoCollision(retreatProgressX, retreatProgressY) > 64 * 64
				|| blockingGuard != null && blockingGuard.isDead()) {
			retreatProgressX = arthas.getX();
			retreatProgressY = arthas.getY();
			retreatProgressTick = tick;
			blockingGuard = null;
		}
		if (recovering && tick - retreatProgressTick >= 100 && blockingGuard == null) {
			blockingGuard = nearbyEnemies.stream().filter(enemy -> arthas.canReach(enemy, 125))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		}
		if (securedGraveyard && recovering && (northAndEast || westRescues)) {
			final float[][] route = northAndEast ? NORTH_AND_EAST_RESCUES : WEST_RESCUES;
			if (!wasRecovering) {
				retreatWaypoint = Math.max(0, (northAndEast ? northAndEastWaypoint : westRescueWaypoint) - 2);
			}
			if (target != null && retreatWaypoint > 0 && arthas.distanceSquaredNoCollision(
					route[retreatWaypoint][0], route[retreatWaypoint][1]) < 250 * 250) retreatWaypoint--;
			// The eastern patrol's return crosses the northern road. Stay above its lower spur.
			if (northAndEast && retreatWaypoint == 4) retreatWaypoint = 3;
			final float[][] fallback = northAndEast ? NORTHWEST_FALLBACK : WESTERN_FALLBACK;
			if (retreatWaypoint <= 0 && retreatWaypoint > 1 - fallback.length && target != null
					&& arthas.distanceSquaredNoCollision(fallback[-retreatWaypoint][0],
							fallback[-retreatWaypoint][1]) < 230 * 230) retreatWaypoint--;
			final float[] refuge = retreatWaypoint < 0 ? fallback[-retreatWaypoint] : route[retreatWaypoint];
			safeX = refuge[0];
			safeY = refuge[1];
		}
		final boolean regroup = recovering || graveyardComplete && !securedGraveyard;
		final CDestructable northGate = northAndEast ? object(globals, "udg_Gate02") : null;
		final CDestructable westGate = westRescues && !regroup && target == null
				? game.getDestructables().stream().filter(gate -> !gate.isDead() && !gate.isInvulnerable()
						&& gate.getTypeId().toString().startsWith("LTg") && arthas.canReach(gate, 300)
						&& gate.distanceSquaredNoCollision(x, y) < 390 * 390)
						.min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null) : null;
		if (CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.attack, westGate)) return;
		final boolean breachNorthGate = !regroup && target == null && northGate != null && !northGate.isDead()
				&& arthas.distanceSquaredNoCollision(northGate) < 600 * 600;
		final boolean crossingNorthGate = northGate != null && northGate.isDead() && northAndEastWaypoint < 4
				&& arthas.getX() < -660 && arthas.distanceSquaredNoCollision(northGate) < 600 * 600;
		final var supply = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
				&& item.getContainedInventory() == null && !blockedSupplies.contains(item.getHandleId())
				&& arthas.distanceSquaredNoCollision(item) < 500 * 500
						&& List.of("phea", "pghe", "pgma", "shea", "rde1").contains(item.getTypeId().toString()))
				.min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
		final boolean atSparePotion = visitGraveyard && graveyardWaypoint >= 3 && graveyardWaypoint <= 4;
		if (supply != null && (target == null || !initialSuppliesCollected || atSparePotion
				|| health < 0.70f && "pghe".equals(supply.getTypeId().toString())
						&& arthas.distanceSquaredNoCollision(supply) < 260 * 260)) {
			if (attemptedSupply != supply.getHandleId()) {
				attemptedSupply = supply.getHandleId();
				supplyOrderTick = tick;
			}
			if (tick - supplyOrderTick >= 200) {
				blockedSupplies.add(attemptedSupply);
			}
			else if (CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.smart, supply)) return;
		}
		if (!regroup && rescued >= 1 && !initialSuppliesCollected) {
			final CDestructable crate = object(globals, SUPPLY_CRATES[0]);
			if (crate != null && !crate.isDead()) {
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.attack, crate)) {
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, arthas, crate.getX(), crate.getY());
				}
				return;
			}
			check(CampaignPlaythroughProbe.hasItem(arthas, "phea"), "the authored crate did not grant its healing potion");
			initialSuppliesCollected = true;
			System.out.println("[UndeadOpeningProbe] collected healing potion from an authored destructable death event");
		}
		if (!regroup && (target == null || atSparePotion)) {
			for (final String name : SUPPLY_CRATES) {
				final CDestructable crate = object(globals, name);
				if (crate != null && !crate.isDead() && arthas.distanceSquaredNoCollision(crate) < 420 * 420
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.attack, crate)) return;
			}
		}
		final var woundedGhoul = game.getUnitsIncludingPending().stream()
				.filter(unit -> unit.getPlayerIndex() == player && !unit.isDead() && !unit.isHidden()
						&& GHOUL.equals(unit.getTypeId()) && unit.getMaximumLife() - unit.getLife() >= 110
						&& arthas.distanceSquaredNoCollision(unit) < 600 * 600)
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		final var escort = game.getUnits().stream().filter(unit -> unit != arthas && !unit.isDead() && !unit.isHidden()
				&& unit.getPlayerIndex() == player && !unit.isBuilding() && !unit.getCurrentAttacks().isEmpty()).toList();
		final boolean easternExpedition = northAndEast && northAndEastWaypoint >= 10 && northAndEastWaypoint <= 23;
		final long easternEscort = easternExpedition ? escort.stream()
				.filter(unit -> unit.distanceSquaredNoCollision(EASTERN_RALLY_X, EASTERN_RALLY_Y) < 520 * 520).count() : 0;
		if (easternEscort >= 2 && recovering && arthas.getX() > 2550 && arthas.getY() > 1000) {
			safeX = EASTERN_RALLY_X;
			safeY = EASTERN_RALLY_Y;
		}
		final boolean holdAgainstPatrol = securedGraveyard && target != null && health >= 0.70f
				&& nearbyEnemies.size() <= 2 && escort.stream().filter(unit -> unit.canReach(arthas, 430)).limit(3).count() == 3;
		if (target != null && health < 0.49f && arthas.getAbilityLevel(DEATH_PACT) > 0) {
			final CUnit sacrifice = escort.stream().filter(unit -> GHOUL.equals(unit.getTypeId())
					&& arthas.canReach(unit, 240)).max(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.deathpact, sacrifice)) return;
		}
		if (remainingRescues && recovering && target != null && health < 0.65f
				&& arthas.distanceSquaredNoCollision(safeX, safeY) < 250 * 250
				&& arthas.distanceSquaredNoCollision(rearRefugeX, rearRefugeY) > 300 * 300) {
			safeX = rearRefugeX;
			safeY = rearRefugeY;
		}
		final boolean escortWaitTime = northAndEast ? tick < waypointDeadline - 300
				: westRescues ? tick < westRescueDeadline - 1240 : tick - progressTick < 480;
		final boolean waitForEscort = securedGraveyard && !regroup && (target == null || !arthas.canReach(target, 280)) && !escort.isEmpty()
				&& escort.stream().filter(unit -> arthas.distanceSquaredNoCollision(unit) < 460 * 460).count()
						< Math.min(2, escort.size()) && escortWaitTime && (!easternExpedition || northAndEastWaypoint == 10);
		for (final CUnit unit : game.getUnitsIncludingPending()) {
			if (unit.getPlayerIndex() != player || unit.isDead() || unit.isHidden() || unit.isPaused()
					|| unit.getCurrentAttacks().isEmpty()) {
				continue;
			}
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (breachNorthGate && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, northGate)) {
				continue;
			}
			if (crossingNorthGate && unit != arthas && target == null) {
				final int formationPosition = escort.indexOf(unit);
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, -1264 - 104 * (formationPosition / 2),
						2384 + 128 * (formationPosition % 2));
				continue;
			}
			if (unit == arthas) {
				if (blockingGuard != null && arthas.canReach(blockingGuard, 150)
						&& (CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.deathcoil, blockingGuard)
								|| CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.attack, blockingGuard))) continue;
				final boolean defendRefuge = graveyardComplete && target != null
						&& arthas.distanceSquaredNoCollision(safeX, safeY) < 250 * 250
						&& (health > 0.58f || northAndEast && retreatWaypoint == 1 - NORTHWEST_FALLBACK.length);
				final var coilTarget = holdAgainstPatrol ? target : woundedGhoul != null && health > 0.52f ? woundedGhoul
						: !regroup || defendRefuge || target != null && "hkni".equals(target.getTypeId().toString())
								&& arthas.canReach(target, 220) ? target : null;
				final int coilRange = remainingRescues ? 600 : 450;
				if (coilTarget != null && arthas.distanceSquaredNoCollision(coilTarget) < coilRange * coilRange
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.deathcoil, coilTarget)) {
					continue;
				}
				if (waitForEscort) {
					unit.order(game, OrderIds.holdposition, null);
					continue;
				}
				if (regroup && !defendRefuge && !holdAgainstPatrol) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, safeX, safeY);
					continue;
				}
				if (!defendRefuge && !holdAgainstPatrol && (visitGraveyard || (northAndEast || westRescues) && !regroup)) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, x, y);
					continue;
				}
			}
			if (easternExpedition && unit != arthas && (!recovering || arthas.getX() > 2550)) {
				if (GHOUL.equals(unit.getTypeId()) && unit.getLife() < unit.getMaximumLife() * 0.57f) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, EASTERN_RALLY_X - 320, EASTERN_RALLY_Y + 144);
					continue;
				}
				final CUnit approachingGuard = nearbyEnemies.stream().filter(enemy -> unit.canReach(enemy, 420))
						.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, approachingGuard)) {
					final int index = Math.max(0, escort.indexOf(unit));
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, EASTERN_RALLY_X - (index / 3) * 104,
							EASTERN_RALLY_Y + (index % 3 - 1) * 106);
				}
				continue;
			}
			if (waitForEscort) {
				rallyBehindHero(viewer, orders, arthas, unit, escort.indexOf(unit), x, y);
				continue;
			}
			final boolean ghoulInDanger = GHOUL.equals(unit.getTypeId()) && nearbyEnemies.stream()
					.anyMatch(enemy -> unit.distanceSquaredNoCollision(enemy) < 310 * 310);
			if (!ghoulInDanger && GHOUL.equals(unit.getTypeId())
					&& unit.getMaximumLife() - unit.getLife() > 80) {
				if (unit.getCurrentOrder() != null && unit.getCurrentOrder().getOrderId() == OrderIds.cannibalize) continue;
				final var cannibalize = unit.getAbilities().stream()
						.filter(ability -> CANNIBALIZE.equals(ability.getAlias())).findFirst().orElse(null);
				if (cannibalize != null) {
					final var canEat = new BooleanAbilityActivationReceiver();
					cannibalize.checkCanUse(game, unit, OrderIds.cannibalize, canEat);
					if (canEat.isOk()) {
						orders.issueImmediateOrder(unit.getHandleId(), cannibalize.getHandleId(), OrderIds.cannibalize, false);
						continue;
					}
				}
			}
			if (ghoulInDanger && unit.getLife() < unit.getMaximumLife() * 0.54f) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, safeX, safeY);
				continue;
			}
			if ((northAndEast || westRescues) && !regroup && !holdAgainstPatrol
					&& unit != arthas && (target == null || !unit.canReach(target, 210))) {
				rallyBehindHero(viewer, orders, arthas, unit, escort.indexOf(unit), x, y);
				continue;
			}
			if (target != null) {
				if (regroup && !holdAgainstPatrol && unit.distanceSquaredNoCollision(arthas) > 260 * 260
						&& CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit,
								safeX + 104 * (escort.indexOf(unit) - 1), safeY + 116)) continue;
				if (unit.getCurrentBehavior() != null
						&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.ATTACK) {
					continue;
				}
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, target)) {
					continue;
				}
			}
			if (westRescues && !regroup && unit != arthas) {
				final int position = escort.indexOf(unit);
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, x + 112 * (position - 1), y + 192);
			}
			else if (!regroup || !CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, safeX, safeY)) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, regroup ? safeX : x, regroup ? safeY : y);
			}
		}
		ui.getCameraManager().setTarget(arthas.getX(), arthas.getY());
		if (tick >= reportTick) {
			reportTick = tick + 400;
			final long nearbyEscort = game.getUnits().stream()
					.filter(unit -> unit != arthas && unit.getPlayerIndex() == player && !unit.isDead()
							&& !unit.isHidden() && !unit.getCurrentAttacks().isEmpty()
							&& arthas.distanceSquaredNoCollision(unit) < 650 * 650)
					.count();
			final var order = arthas.getCurrentOrder();
			final var orderTarget = order == null ? null : order.getTarget(game);
			System.out.printf("[UndeadOpeningProbe] tick=%d rescued=%d available=%d hero=%.0f,%.0f life=%.0f"
					+ " next=%.0f,%.0f retreat=%.0f,%.0f regroup=%s escort=%d/%d eastRally=%d west=%d enemy=%s order=%s/%s behavior=%s gate=%s%n",
					tick, rescued, available == null ? 0 : available.size(), arthas.getX(), arthas.getY(), arthas.getLife(),
					x, y, safeX, safeY, regroup, nearbyEscort, escort.size(), easternEscort, westRescueWaypoint, target == null ? "none" : target.getTypeId(),
					order == null ? "none" : order.getOrderId(), orderTarget == null ? "none" : Math.round(orderTarget.getX()) + "," + Math.round(orderTarget.getY()),
					arthas.getCurrentBehavior() == null ? "none" : arthas.getCurrentBehavior().getBehaviorCategory(),
					northGate == null ? "none" : northGate.getLife());
		}
	}

	private static void rallyBehindHero(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit arthas, final CUnit unit, final int position, final float x, final float y) {
		final var game = viewer.simulation;
		final float heading = (float) Math.atan2(y - arthas.getY(), x - arthas.getX());
		final float forwardX = (float) Math.cos(heading);
		final float forwardY = (float) Math.sin(heading);
		final int index = Math.max(0, position);
		final float back = 178 + 64 * (index / 3);
		final float side = 102 * (index % 3 - 1);
		final float rallyX = arthas.getX() - back * forwardX - side * forwardY;
		final float rallyY = arthas.getY() - back * forwardY + side * forwardX;
		float orderX = arthas.getX();
		float orderY = arthas.getY();
		if (game.getPathingGrid().isPathable(rallyX, rallyY, unit.getMovementType(), unit.getUnitType().getCollisionSize())) {
			orderX = rallyX;
			orderY = rallyY;
		}
		final var order = unit.getCurrentOrder();
		if (order instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint
				&& order.getOrderId() == OrderIds.attack) {
			final var target = order.getTarget(game);
			final float deltaX = target.getX() - orderX;
			final float deltaY = target.getY() - orderY;
			if (deltaX * deltaX + deltaY * deltaY < 172 * 172) return;
		}
		CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, orderX, orderY);
	}

	private static boolean followingWestRoute() {
		return westRescueWaypoint >= 0 && westRescueWaypoint < WEST_RESCUES.length;
	}

	private static void selectAcolyte(final CUnit arthas, final int tick,
			final List<CUnit> available, final List<CUnit> units, final War3MapViewer viewer) {
		if (acolyte != null && !acolyte.isDead() && !acolyte.isHidden()
				&& available != null && available.contains(acolyte)) {
			final double distance = arthas.distanceSquaredNoCollision(acolyte);
			if (distance < nearestAcolyteDistance - 32 * 32) {
				progressTick = tick;
				nearestAcolyteDistance = distance;
			}
			if (tick - progressTick < 1200) {
				return;
			}
			retryTicks.put(acolyte.getHandleId(), tick + 2400);
		}
		final var game = viewer.simulation;
		final var player = game.getPlayer(viewer.getLocalPlayerIndex());
		final var guards = units.stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& unit.getPlayerIndex() != player.getId() && !unit.getCurrentAttacks().isEmpty()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)).toList();
		acolyte = available == null ? null : available.stream()
				.filter(unit -> !unit.isDead() && !unit.isHidden() && HIDING_ACOLYTE.equals(unit.getTypeId())
						&& retryTicks.getOrDefault(unit.getHandleId(), 0) <= tick)
				.min(Comparator.comparingDouble(unit -> arthas.distanceSquaredNoCollision(unit)
						+ guards.stream().filter(guard -> unit.distanceSquaredNoCollision(guard) < 720 * 720).count()
								* 1_440_000.0)).orElse(null);
		progressTick = tick;
		nearestAcolyteDistance = acolyte == null ? 0 : arthas.distanceSquaredNoCollision(acolyte);
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static boolean completed(final GlobalScope globals, final String name) {
		final CQuest quest = object(globals, name);
		return quest != null && quest.isCompleted();
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new IllegalStateException("Undead opening audit: " + message);
		}
	}
}
