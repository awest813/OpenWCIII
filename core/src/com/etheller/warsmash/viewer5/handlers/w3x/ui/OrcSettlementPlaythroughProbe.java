package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellUnits;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgAbilityActivationReceiver;

/** Exercises the original Orc03 settlement objective through construction and harvesting orders. */
public final class OrcSettlementPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcSettlementAudit");
	private static final boolean ZEPPELIN_OBJECTIVE = Boolean.getBoolean("warsmash.orcSettlementZeppelin");
	private static final War3ID PEON = War3ID.fromString("opeo");
	private static final War3ID GREAT_HALL = War3ID.fromString("ogre");
	private static final War3ID STRONGHOLD = War3ID.fromString("ostr");
	private static final War3ID GOLD_MINE = War3ID.fromString("ngol");
	private static final War3ID GRUNT = War3ID.fromString("ogru");
	private static final War3ID HEADHUNTER = War3ID.fromString("ohun");
	private static final War3ID CATAPULT = War3ID.fromString("ocat");
	private static final War3ID LAB = War3ID.fromString("nC00");
	private static final War3ID ZEPPELIN = War3ID.fromString("nzep");
	private static final float[][] HALL_SITES = { { 1920, -7040 }, { 1664, -7168 }, { 1792, -6784 } };
	private static final float[] FOREST_APPROACH = { 1904, -7088 };
	private static final int HARPY_RAVINE = 19;
	private static final Map<Integer, WoodApproach> woodApproaches = new HashMap<>();
	private static final Map<Integer, Integer> blockedTrees = new HashMap<>();
	private static final Map<Integer, Integer> reinforcementWaypoints = new HashMap<>();
	private static final float[][] LAB_ROUTE = { { 1104, -5488 }, { 1104, -3952 }, { 528, -3184 },
			{ -496, -2160 }, { -1008, -1744 }, { -2800, -1744 }, { -3504, -1008 }, { -3632, -240 },
			{ -3952, 272 }, { -4400, 1040 }, { -4528, 1552 }, { -4528, 2832 }, { -4176, 3536 },
			{ -4208, 4048 }, { -4400, 4304 }, { -4656, 4560 }, { -4848, 4816 }, { -4816, 5328 },
			{ -4304, 5808 }, { -3024, 5840 },
			{ -2768, 6096 }, { -2704, 6608 }, { -2576, 6864 } };
	private static int nextOrderTick;
	private static int goldBeforeHarvest;
	private static boolean harvestStarted;
	private static boolean goldDelivered;
	private static boolean expeditionStarted;
	private static boolean laboratoryOpened;
	private static CUnit laboratoryBuyer;
	private static int laboratoryWaypoint;
	private static boolean transportsBought;
	private static boolean zeppelinQuestCompleted;
	private static int waypoint;
	private static double closestWaypointDistance = Double.POSITIVE_INFINITY;
	private static int lastWaypointProgress;
	private static int reportTick;
	private static boolean rallying;
	private static int retreatWaypoint;
	private static boolean finished;
	private record WoodApproach(float x, float y, int tick) { }

	private OrcSettlementPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final GlobalScope globals = game.getGlobalScope();
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (map.endsWith("/orc04.w3m")) {
			check(zeppelinQuestCompleted, "Orc04 opened before the original Zeppelin victory");
			System.out.println("[OrcSettlementProbe] the original ending continued into Orc04");
			finish();
			return;
		}
		check(map.endsWith("/orc03.w3m"), "unexpected map " + map);
		final CUnit thrall = object(globals, "udg_Thrall");
		final CQuest townQuest = object(globals, "udg_QuestBuildTown");
		if (thrall == null || townQuest == null || !townQuest.isDiscovered()) return;
		check(!thrall.isDead(), "Thrall died before the Zeppelin objective");
		check(!"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		final int tick = game.getGameTurnTick();
		check(tick < (ZEPPELIN_OBJECTIVE ? 48_000 : 12_000), "the original objectives stalled");
		final CQuest zeppelinQuest = object(globals, "udg_QuestZeppelin");
		if (zeppelinQuest != null && zeppelinQuest.isCompleted()) {
			check(goldDelivered && laboratoryOpened && transportsBought, "victory without the original preparations");
			if (!zeppelinQuestCompleted) {
				zeppelinQuestCompleted = true;
				System.out.println("[OrcSettlementProbe] both Zeppelins returned and the original victory triggered");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		if (!ui.isUserControlEnabled() || game.isGamePaused() || tick < nextOrderTick) return;
		nextOrderTick = tick + 32;
		final int player = thrall.getPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()).toList();
		final var workers = owned.stream().filter(unit -> PEON.equals(unit.getTypeId()) && !unit.isHidden()).toList();
		final CUnit hall = owned.stream()
				.filter(unit -> GREAT_HALL.equals(unit.getTypeId()) || STRONGHOLD.equals(unit.getTypeId()))
				.findFirst().orElse(null);
		check(!workers.isEmpty(), "no surviving Peons");
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (hall == null) {
			final CUnit builder = workers.get(0);
			final var order = builder.getCurrentOrder();
			if (order != null && order.getOrderId() == GREAT_HALL.getValue()) return;
			for (final float[] site : HALL_SITES) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, builder, GREAT_HALL.getValue(), site[0], site[1])) {
					System.out.printf("[OrcSettlementProbe] ordered a Great Hall at %.0f, %.0f%n", site[0], site[1]);
					break;
				}
			}
			return;
		}
		if (hall.isConstructingOrUpgrading() || !townQuest.isCompleted()
				|| zeppelinQuest == null || !zeppelinQuest.isDiscovered()) return;
		final var owner = game.getPlayer(player);
		if (!harvestStarted) {
			final CUnit mine = game.getUnits().stream().filter(unit -> GOLD_MINE.equals(unit.getTypeId()) && !unit.isDead())
					.min(Comparator.comparingDouble(hall::distanceSquaredNoCollision)).orElseThrow();
			goldBeforeHarvest = owner.getGold();
			for (final CUnit worker : workers) {
				harvestStarted |= CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, mine);
			}
			check(harvestStarted, "Peons could not start mining at the original gold mine");
			System.out.println("[OrcSettlementProbe] the original town quest completed and Zeppelin quest opened");
		}
		if (!goldDelivered && owner.getGold() >= goldBeforeHarvest + 100) {
			goldDelivered = true;
			System.out.println("[OrcSettlementProbe] Peons delivered 100 gold to the completed Great Hall");
			if (!ZEPPELIN_OBJECTIVE) {
				finish();
				return;
			}
		}
		if (goldDelivered) pursueZeppelins(viewer, orders, thrall, hall, owned, globals, tick);
	}

	private static void pursueZeppelins(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit thrall, final CUnit hall, final List<CUnit> owned,
			final GlobalScope globals, final int tick) {
		final var game = viewer.simulation;
		final var owner = game.getPlayer(thrall.getPlayerIndex());
		final var zeppelin = game.getUnitData().getUnitType(ZEPPELIN);
		final int zeppelinReserve = zeppelin.getLumberCost() * 2;
		final CQuestItem harpies = object(globals, "udg_QuestReqKillHarpies");
		if (expeditionStarted) {
			if (owner.getLumber() < zeppelinReserve) {
				final int keepMining = harpies != null && harpies.isCompleted() && owner.getGold() >= zeppelin.getGoldCost() * 2
						? 0 : 3;
				owned.stream().filter(unit -> PEON.equals(unit.getTypeId()))
						.sorted(Comparator.comparingInt(CUnit::getHandleId)).skip(keepMining).filter(unit -> !unit.isHidden())
						.forEach(worker -> harvestWood(viewer, orders, worker, hall));
			}
		}
		final List<CUnit> aircraft = owned.stream().filter(unit -> ZEPPELIN.equals(unit.getTypeId())).toList();
		if (aircraft.size() >= 2 && !transportsBought) {
			transportsBought = true;
			System.out.println("[OrcSettlementProbe] bought two Zeppelins through the original Goblin Laboratory");
		}
		for (int index = 0; index < aircraft.size(); index++) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, aircraft.get(index), hall.getX() + 240 + index * 100, hall.getY());
		}
		if (aircraft.size() >= 2) return;
		final CUnit lab = game.getUnits().stream().filter(unit -> LAB.equals(unit.getTypeId()) && !unit.isDead())
				.filter(unit -> unit.distanceSquaredNoCollision(-2432, 7104) < 160 * 160).findFirst().orElse(null);
		if (lab != null && harpies != null && harpies.isCompleted()) {
			if (!laboratoryOpened) {
				laboratoryOpened = true;
				System.out.println("[OrcSettlementProbe] defeated the original Harpies and opened the laboratory");
			}
			final var shop = lab.getNeutralBuildingData();
			final boolean reportShop = tick >= reportTick;
			if (reportShop) {
				reportTick = tick + 320;
				System.out.printf("[OrcSettlementProbe] shop at %.0f,%.0f buyer=%s selected=%s waypoint=%d gold=%d lumber=%d food=%d/%d aircraft=%d%n",
						lab.getX(), lab.getY(), laboratoryBuyer == null ? "none" : laboratoryBuyer.getTypeId() + " at "
								+ Math.round(laboratoryBuyer.getX()) + "," + Math.round(laboratoryBuyer.getY()),
						shop == null || shop.getSelectedPlayerUnit(owner.getId()) == null ? "none"
								: shop.getSelectedPlayerUnit(owner.getId()).getTypeId(),
						laboratoryWaypoint, owner.getGold(), owner.getLumber(), owner.getFoodUsed(), owner.getFoodCap(), aircraft.size());
					System.out.println("[OrcSettlementProbe] remaining Peons=" + owned.stream()
							.filter(unit -> PEON.equals(unit.getTypeId()) && !unit.isHidden())
							.map(unit -> Math.round(unit.getX()) + "," + Math.round(unit.getY()) + " order "
									+ (unit.getCurrentOrder() == null ? "none" : unit.getCurrentOrder().getOrderId())).toList());
			}
			if (shop != null && shop.getSelectedPlayerUnit(owner.getId()) != null) {
				final var seller = lab.getFirstAbilityOfType(CAbilitySellUnits.class);
				check(seller != null && seller.getUnitsSold().contains(ZEPPELIN), "the laboratory has no Zeppelin sale");
				final int orderId = owner.getId() | ((seller.getUnitsSold().indexOf(ZEPPELIN) + 1) << 8);
				final var activation = new ExternStringMsgAbilityActivationReceiver().reset();
				seller.checkCanUse(game, lab, orderId, activation);
				if (activation.isUseOk()) orders.issueImmediateOrder(lab.getHandleId(), seller.getHandleId(), orderId, false);
				else if (reportShop) System.out.println("[OrcSettlementProbe] Zeppelin sale unavailable " + activation.getExternStringKey());
			}
			else {
				if (laboratoryBuyer == null || laboratoryBuyer.isDead() || laboratoryBuyer.isHidden()) {
					laboratoryBuyer = shop != null && shop.requiresInventory() ? thrall
							: owned.stream().filter(unit -> !unit.isBuilding() && !unit.isHidden() && !unit.isPaused())
									.min(Comparator.comparingDouble(lab::distanceSquaredNoCollision)).orElse(thrall);
					laboratoryWaypoint = nearestWaypoint(laboratoryBuyer);
				}
				while (laboratoryWaypoint < LAB_ROUTE.length) {
					final float[] approach = LAB_ROUTE[laboratoryWaypoint];
					if (laboratoryBuyer.distanceSquaredNoCollision(approach[0], approach[1]) > 150 * 150) break;
					laboratoryWaypoint++;
				}
				if (laboratoryWaypoint < LAB_ROUTE.length) {
					final float[] approach = LAB_ROUTE[laboratoryWaypoint];
					CampaignPlaythroughProbe.pointOrder(viewer, orders, laboratoryBuyer, approach[0], approach[1]);
				}
				else CampaignPlaythroughProbe.pointOrder(viewer, orders, laboratoryBuyer, -2400, 6880);
			}
			return;
		}
		final long ranged = owned.stream().filter(unit -> HEADHUNTER.equals(unit.getTypeId())).count();
		final long siege = owned.stream().filter(unit -> CATAPULT.equals(unit.getTypeId())).count();
		final long grunts = owned.stream().filter(unit -> GRUNT.equals(unit.getTypeId())).count();
		if (!expeditionStarted) {
			prepareArmy(viewer, orders, hall, owned, ranged, siege, grunts);
			if (tick >= reportTick) {
				reportTick = tick + 640;
				System.out.printf("[OrcSettlementProbe] recruiting at tick=%d hall=%s workers=%d ranged=%d siege=%d gold=%d lumber=%d food=%d/%d%n",
						tick, hall.getTypeId(), owned.stream().filter(unit -> PEON.equals(unit.getTypeId())).count(),
						ranged, siege, owner.getGold(), owner.getLumber(),
						owner.getFoodUsed(), owner.getFoodCap());
			}
			if (ranged < 6 || siege == 0 || grunts < 3 || owner.getLumber() < zeppelinReserve) return;
			expeditionStarted = true;
			System.out.println("[OrcSettlementProbe] trained Grunts, Headhunters, and a Catapult for the Harpy expedition");
		}
		OrcOpeningPlaythroughProbe.learnSpells(viewer, orders, thrall);
		if (thrall.getLife() < thrall.getMaximumLife() * 0.66f
				&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "shea"))) return;
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && unit.getPlayerIndex() != owner.getId()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, owner.getId()))
				.filter(unit -> !unit.getCurrentAttacks().isEmpty()).toList();
		final CUnit nearbyThreat = enemies.stream().filter(enemy -> thrall.distanceSquaredNoCollision(enemy) < 950 * 950)
				.min(Comparator.comparingDouble(thrall::distanceSquaredNoCollision)).orElse(null);
		final var army = owned.stream().filter(unit -> !unit.isBuilding() && !unit.isHidden() && !unit.isPaused()
				&& !PEON.equals(unit.getTypeId()) && !unit.getCurrentAttacks().isEmpty()).toList();
		prepareArmy(viewer, orders, hall, owned, ranged, siege, grunts);
		final boolean wasRallying = rallying;
		final boolean atHarpyRavine = waypoint >= HARPY_RAVINE;
		rallying = thrall.getLife() < thrall.getMaximumLife() * (rallying ? 0.94f : atHarpyRavine ? 0.83f : 0.72f);
		if (rallying) {
			if (!wasRallying) retreatWaypoint = Math.max(0, nearestWaypoint(thrall) - 1);
			if (nearbyThreat != null && retreatWaypoint > 0 && thrall.distanceSquaredNoCollision(nearbyThreat) < 460 * 460
					&& thrall.distanceSquaredNoCollision(LAB_ROUTE[retreatWaypoint][0], LAB_ROUTE[retreatWaypoint][1])
							< 280 * 280) retreatWaypoint--;
			lastWaypointProgress = tick;
		}
		else if (nearbyThreat == null && collectHealing(viewer, orders, thrall, globals)) {
			lastWaypointProgress = tick;
			return;
		}
		if (wasRallying && !rallying) reinforcementWaypoints.put(thrall.getHandleId(), nearestWaypoint(thrall));
		final float[] current = LAB_ROUTE[waypoint];
		final double distance = thrall.distanceSquaredNoCollision(current[0], current[1]);
		final boolean screenedApproach = waypoint >= 9;
		final boolean rangedInPosition = screenedApproach && army.stream().filter(unit -> HEADHUNTER.equals(unit.getTypeId())
				&& unit.distanceSquaredNoCollision(current[0], current[1]) < 200 * 200).limit(2).count() >= 2;
		if (waypoint < LAB_ROUTE.length - 1 && (rangedInPosition || distance <= 128 * 128)) {
			waypoint++;
			closestWaypointDistance = Double.POSITIVE_INFINITY;
			lastWaypointProgress = tick;
		}
		else if (distance < closestWaypointDistance - 32 * 32) {
			closestWaypointDistance = distance;
			lastWaypointProgress = tick;
		}
		else if (waypoint < LAB_ROUTE.length - 1) {
			check(tick - lastWaypointProgress < (screenedApproach ? 4200 : 2400), "the expedition stalled at waypoint " + waypoint
					+ " near " + Math.round(thrall.getX()) + ", " + Math.round(thrall.getY()));
		}
		final float[] destination = LAB_ROUTE[waypoint];
		final boolean approachingLaboratory = waypoint == LAB_ROUTE.length - 1;
		final long nearbyEscort = army.stream().filter(unit -> unit != thrall && HEADHUNTER.equals(unit.getTypeId())
				&& thrall.distanceSquaredNoCollision(unit) < 620 * 620).count();
		final List<CUnit> harpyGroup = object(globals, "udg_HarpyGroup");
		final long livingHarpies = harpyGroup == null ? 3
				: harpyGroup.stream().filter(unit -> game.containsUnit(unit) && !unit.isDead()).count();
		final boolean escortReady = atHarpyRavine ? nearbyEscort >= Math.max(2, Math.min(3, livingHarpies))
				: nearbyEscort >= Math.min(ranged, 3);
		int formation = 0;
		for (final CUnit unit : army) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit == thrall) {
				if (rallying) {
					if (nearbyThreat != null && !nearbyThreat.isUnitType(CUnitTypeJass.FLYING)
							&& thrall.canReach(nearbyThreat, 480)
							&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, thrall, OrderIds.spiritwolf)) continue;
					final float[] refuge = LAB_ROUTE[retreatWaypoint];
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				}
				else if (nearbyThreat != null && thrall.distanceSquaredNoCollision(nearbyThreat) < 560 * 560
						&& (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.chainlightning, nearbyThreat)
								|| (!atHarpyRavine || !nearbyThreat.isUnitType(CUnitTypeJass.FLYING) || escortReady)
								&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, thrall, OrderIds.spiritwolf))) continue;
				else if (screenedApproach || approachingLaboratory) {
					final int behind = Math.max(0, waypoint - (escortReady ? 1 : 2));
					final boolean ownRangedAttack = atHarpyRavine && escortReady;
					if (ownRangedAttack && nearbyThreat != null && thrall.getCurrentAttacks().stream()
							.anyMatch(attack -> thrall.canReach(nearbyThreat, attack.getRange()))
							&& CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, nearbyThreat)) continue;
					final int firingPosition = atHarpyRavine && !escortReady ? Math.min(behind, HARPY_RAVINE) : behind;
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, LAB_ROUTE, firingPosition, 150,
							reinforcementWaypoints);
				}
				else if (escortReady || nearbyThreat == null && thrall.getLife() >= thrall.getMaximumLife() * 0.90f
						&& tick - lastWaypointProgress >= 600) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, destination[0], destination[1]);
				}
				else if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.MOVEMENT) {
					orders.issueImmediateOrder(unit.getHandleId(), 0, OrderIds.stop, false);
				}
			}
			else {
				if (atHarpyRavine && HEADHUNTER.equals(unit.getTypeId())
						&& unit.getLife() < unit.getMaximumLife() * (rallying ? 0.75f : 0.52f)
						&& enemies.stream().anyMatch(enemy -> unit.distanceSquaredNoCollision(enemy) < 430 * 430)) {
					final float[] refuge = LAB_ROUTE[rallying ? retreatWaypoint : HARPY_RAVINE - 1];
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
					continue;
				}
				final CUnit threat = nearbyThreat != null && unit.distanceSquaredNoCollision(nearbyThreat) < 1000 * 1000
						? nearbyThreat : enemies.stream().filter(enemy -> unit.distanceSquaredNoCollision(enemy) < 640 * 640)
								.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				final CUnit target = CATAPULT.equals(unit.getTypeId()) ? enemies.stream().filter(CUnit::isBuilding)
						.filter(enemy -> unit.distanceSquaredNoCollision(enemy) < 900 * 900)
						.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(threat) : threat;
				final boolean rangedSupport = HEADHUNTER.equals(unit.getTypeId()) || CATAPULT.equals(unit.getTypeId());
				if ((!rangedSupport || target != null && unit.getCurrentAttacks().stream()
						.anyMatch(attack -> unit.canReach(target, attack.getRange())))
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, target)) continue;
				if (screenedApproach && rangedSupport) {
					final int firingLine = rallying ? Math.min(waypoint, retreatWaypoint + 1)
							: atHarpyRavine && !escortReady ? HARPY_RAVINE : waypoint;
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, LAB_ROUTE, firingLine, 180,
							reinforcementWaypoints);
					continue;
				}
				final boolean holdNearHero = rallying || !escortReady && !screenedApproach && !approachingLaboratory;
				final float x = holdNearHero ? Math.round(thrall.getX() / 128) * 128 : destination[0];
				final float y = holdNearHero ? Math.round(thrall.getY() / 128) * 128 : destination[1];
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, x + (formation % 4 - 1) * 56,
						y - formation / 4 * 64);
				formation++;
			}
		}
		if (tick >= reportTick) {
			reportTick = tick + 320;
			System.out.printf("[OrcSettlementProbe] tick=%d waypoint=%d Thrall=%.0f,%.0f life=%.0f mana=%.0f ranged=%d siege=%d gold=%d lumber=%d%n",
					tick, waypoint, thrall.getX(), thrall.getY(), thrall.getLife(), thrall.getMana(), ranged, siege,
					owner.getGold(), owner.getLumber());
			if (waypoint >= HARPY_RAVINE) {
				System.out.println("[OrcSettlementProbe] ravine enemies=" + enemies.stream()
						.filter(enemy -> thrall.distanceSquaredNoCollision(enemy) < 1250 * 1250)
						.map(enemy -> enemy.getTypeId() + "@" + Math.round(enemy.getX()) + "," + Math.round(enemy.getY())
								+ " life " + Math.round(enemy.getLife())).toList());
				System.out.println("[OrcSettlementProbe] ranged escort=" + army.stream()
						.filter(unit -> HEADHUNTER.equals(unit.getTypeId()))
						.map(unit -> Math.round(unit.getX()) + "," + Math.round(unit.getY()) + " life "
								+ Math.round(unit.getLife())).toList() + " original Harpies remaining=" + livingHarpies);
			}
		}
	}

	private static int nearestWaypoint(final CUnit unit) {
		int nearest = 0;
		for (int index = 1; index < LAB_ROUTE.length; index++) {
			final float[] candidate = LAB_ROUTE[index];
			final float[] previous = LAB_ROUTE[nearest];
			if (unit.distanceSquaredNoCollision(candidate[0], candidate[1])
					< unit.distanceSquaredNoCollision(previous[0], previous[1])) nearest = index;
		}
		return nearest;
	}

	private static boolean collectHealing(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit thrall, final GlobalScope globals) {
		final var crate = OrcSettlementPlaythroughProbe.<CDestructable>object(globals, "gg_dest_LTcr_0867");
		if (crate != null && !crate.isDead() && thrall.distanceSquaredNoCollision(crate) < 560 * 560
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, crate)) return true;
		final var item = viewer.simulation.getItems().stream().filter(value -> !value.isDead() && !value.isHidden()
				&& value.getContainedInventory() == null && thrall.distanceSquaredNoCollision(value) < 480 * 480
				&& ("phea".equals(value.getTypeId().toString()) || "shea".equals(value.getTypeId().toString())))
				.min(Comparator.comparingDouble(thrall::distanceSquaredNoCollision)).orElse(null);
		return item != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.smart, item);
	}

	private static void prepareArmy(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit hall,
			final List<CUnit> owned, final long ranged, final long siege, final long grunts) {
		final var game = viewer.simulation;
		final var owner = game.getPlayer(hall.getPlayerIndex());
		final CUnit mine = game.getUnits().stream().filter(unit -> GOLD_MINE.equals(unit.getTypeId()) && !unit.isDead())
				.min(Comparator.comparingDouble(hall::distanceSquaredNoCollision)).orElseThrow();
		final CUnit barracks = owned.stream().filter(unit -> "obar".equals(unit.getTypeId().toString())).findFirst().orElse(null);
		if (barracks != null && !barracks.isConstructingOrUpgrading() && barracks.getBuildQueue()[0] == null) {
			final int reserve = expeditionStarted && ranged >= 4 ? game.getUnitData().getUnitType(ZEPPELIN).getLumberCost() : 0;
			if (ranged < 6 && owner.getLumber() >= reserve + game.getUnitData().getUnitType(HEADHUNTER).getLumberCost()) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, "ohun");
			}
			else if (siege == 0 && STRONGHOLD.equals(hall.getTypeId()) && (!expeditionStarted
					|| owner.getLumber() >= reserve + game.getUnitData().getUnitType(CATAPULT).getLumberCost())) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, "ocat");
			}
			else if (siege > 0 && grunts < 3) HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, "ogru");
		}
		if (owned.stream().filter(unit -> PEON.equals(unit.getTypeId())).count() < 6 && hall.getBuildQueue()[0] == null) {
			HumanCampaignPlaythroughProbe.train(viewer, orders, hall, "opeo");
		}
		else if (ranged >= 6 && GREAT_HALL.equals(hall.getTypeId()) && hall.getBuildQueue()[0] == null
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, hall, STRONGHOLD.getValue())) {
			System.out.println("[OrcSettlementProbe] ordered the Stronghold upgrade for siege training");
			return;
		}
		final List<CUnit> workforce = owned.stream().filter(unit -> PEON.equals(unit.getTypeId()))
				.sorted(Comparator.comparingInt(CUnit::getHandleId)).toList();
		final CUnit builder = workforce.get(workforce.size() - 1);
		final var order = builder.getCurrentOrder();
		if (order != null && builder.getCurrentBehavior() != null
				&& builder.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.MOVEMENT
				&& List.of("obar", "ofor", "otrb").stream().map(War3ID::fromString)
						.anyMatch(type -> type.getValue() == order.getOrderId())) return;
		for (int index = 0; index < workforce.size(); index++) {
			final CUnit worker = workforce.get(index);
			if (worker == builder || worker.isHidden()) continue;
			if (index < 3) CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, mine);
			else harvestWood(viewer, orders, worker, hall);
		}
		if (builder.isHidden()) return;
		if (owned.stream().anyMatch(CUnit::isConstructingOrUpgrading)) return;
		String building = null;
		if (barracks == null) building = "obar";
		else if (owned.stream().noneMatch(unit -> "ofor".equals(unit.getTypeId().toString()))) building = "ofor";
		else if (owner.getFoodCap() - owner.getFoodUsed() <= 5) building = "otrb";
		if (building == null) {
			harvestWood(viewer, orders, builder, hall);
			return;
		}
		final int id = War3ID.fromString(building).getValue();
		for (int y = -480; y <= 320; y += 160) {
			for (int x = -480; x >= -1120; x -= 160) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, builder, id, hall.getX() + x, hall.getY() + y)) return;
			}
		}
		harvestWood(viewer, orders, builder, hall);
	}

	private static void harvestWood(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit worker,
			final CUnit hall) {
		final CAbilityHarvest harvest = worker.getFirstAbilityOfType(CAbilityHarvest.class);
		if (harvest == null) return;
		final var behavior = worker.getCurrentBehavior();
		if (behavior != null && behavior.getHighlightOrderId() == OrderIds.returnresources) return;
		final int tick = viewer.simulation.getGameTurnTick();
		if (behavior != null && behavior.getHighlightOrderId() == OrderIds.harvest
				&& harvest.getLastHarvestTarget() instanceof CDestructable tree && !tree.isDead()) {
			if (behavior.getBehaviorCategory() != CBehaviorCategory.MOVEMENT) {
				woodApproaches.remove(worker.getHandleId());
				return;
			}
			final WoodApproach last = woodApproaches.get(worker.getHandleId());
			if (last == null || worker.distanceSquaredNoCollision(last.x(), last.y()) >= 48 * 48) {
				woodApproaches.put(worker.getHandleId(), new WoodApproach(worker.getX(), worker.getY(), tick));
				return;
			}
			if (tick - last.tick() < 450) return;
			woodApproaches.remove(worker.getHandleId());
			blockedTrees.put(tree.getHandleId(), tick + 3200);
		}
		if (worker.distanceSquaredNoCollision(FOREST_APPROACH[0], FOREST_APPROACH[1]) > 200 * 200) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, worker, FOREST_APPROACH[0], FOREST_APPROACH[1]);
			return;
		}
		viewer.simulation.getDestructables().stream().filter(tree -> !tree.isDead()
				&& blockedTrees.getOrDefault(tree.getHandleId(), 0) <= tick
				&& hall.distanceSquaredNoCollision(tree) < 2950 * 2950)
				.sorted(Comparator.comparingDouble(worker::distanceSquaredNoCollision))
				.anyMatch(tree -> CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, tree));
	}

	private static void finish() {
		finished = true;
		Gdx.app.exit();
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc settlement audit: " + message);
	}
}
