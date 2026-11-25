package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellUnits;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;

/** Leads the original Orc06 army to the Harpy nest through ordinary campaign orders. */
public final class OrcWyvernPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcWyvernAudit");
	private static final boolean PLAYTHROUGH = Boolean.getBoolean("warsmash.orcWyvernVictory");
	private static final War3ID PEON = War3ID.fromString("opeo");
	private static final War3ID HEADHUNTER = War3ID.fromString("ohun");
	private static final War3ID BARRACKS = War3ID.fromString("obar");
	private static final War3ID GREAT_HALL = War3ID.fromString("ogre");
	private static final Set<War3ID> TOWN_HALLS = Set.of(GREAT_HALL, War3ID.fromString("ostr"), War3ID.fromString("ofrt"));
	private static final War3ID WYVERN = War3ID.fromString("owyv");
	private static final War3ID ZEPPELIN = War3ID.fromString("nzep");
	private static final War3ID CATAPULT = War3ID.fromString("ocat");
	private static final War3ID BURROW = War3ID.fromString("otrb");
	private static final War3ID WATCH_TOWER = War3ID.fromString("owtw");
	private static final War3ID GRUNT = War3ID.fromString("ogru");
	private static final Set<String> REQUIRED_BUILDINGS = Set.of("htow", "hkee", "hcas", "halt", "hbar", "harm", "hgra");
	private static final Set<String> SUMMIT_AIR_DEFENDERS = Set.of("hgry", "hgyr", "hrif", "hgtw", "hatw");
	private static final List<String> NEST_HARPIES = List.of("gg_unit_nhrq_0071", "gg_unit_nhrw_0024", "gg_unit_nhrw_0033",
			"gg_unit_nhrh_0123", "gg_unit_nhrh_0022", "gg_unit_nhar_0121", "gg_unit_nhar_0119", "gg_unit_nhar_0120");
	private static final float[] SUMMIT_LANDING = { 3056, 6128 };
	private static final float[] SUMMIT_AIR_REFUGE = { 3136, 6544 };
	private static final float[][] SUMMIT_SCOUT_ROUTE = { { 3056, 5968 }, { 2928, 5680 }, { 2896, 5392 },
			{ 2660, 5184 }, { 2368, 4936 }, { 2152, 4720 }, { 1936, 4592 }, { 1600, 4672 },
			{ 1408, 5024 }, { 1184, 5312 }, { 920, 5568 }, { 704, 5776 } };
	private static final float[][] LABORATORY_ROUTE = { { -2928, 1520 }, { -2928, 944 }, { -2928, 656 }, { -3024, 80 },
			{ -2864, -208 }, { -2288, -400 }, { -2000, -592 }, { -1744, -880 }, { -1680, -1456 }, { -1936, -1552 },
			{ -2512, -1616 }, { -2800, -1904 }, { -2896, -2192 }, { -2960, -2480 }, { -2960, -3344 },
			{ -2960, -4208 }, { -2960, -5072 }, { -2736, -5328 }, { -2160, -5328 }, { -1584, -5296 },
			{ -1296, -5008 }, { -1008, -4720 }, { -432, -4144 }, { -144, -3856 }, { 144, -3568 },
			{ 400, -2992 }, { 688, -2704 }, { 1264, -2128 }, { 1840, -1552 }, { 2128, -1264 } };
	private static final float[][] SUMMIT_AIR_ROUTE = { { 1088, 2304 }, { 2528, 2304 }, { 3760, 2272 },
			{ 3760, 4000 }, { 3760, 5728 }, SUMMIT_AIR_REFUGE, SUMMIT_LANDING };
	private static final float[][] CATAPULT_ROUTE = { { 1616, -5872 }, { 1616, -5584 }, { 1360, -5296 },
			{ 1072, -5008 }, { 784, -4720 }, { 688, -4432 }, { 688, -3856 }, { 688, -3280 }, { 688, -2704 },
			{ 976, -2416 }, { 1264, -2128 }, { 1552, -1840 }, { 1840, -1552 }, { 2128, -1264 } };
	private static final float[][] HARPY_NEST = { { 1616, -5904 }, { 1424, -5776 }, { 1168, -5584 },
			{ 592, -5008 }, { 304, -4720 }, { 16, -4432 }, { -656, -4336 }, { -1232, -4368 },
			{ -1520, -4656 }, { -1808, -4944 }, { -2096, -5168 }, { -2384, -5328 }, { -2672, -5328 },
			{ -2896, -4848 }, { -2960, -4272 }, { -2960, -3696 }, { -2960, -3120 }, { -2960, -2544 },
			{ -2960, -2256 }, { -2864, -2000 }, { -2288, -2000 }, { -2000, -1808 }, { -1744, -1520 },
			{ -1680, -1232 }, { -1680, -944 }, { -1936, -784 }, { -2512, -464 }, { -2800, -272 },
			{ -2992, 16 }, { -3024, 592 }, { -3024, 1168 }, { -2960, 1456 }, { -2944, 1712 }, { -2944, 2072 } };
	private static final Map<Integer, Integer> waypoints = new HashMap<>();
	private static final Map<Integer, Integer> laboratorySteps = new HashMap<>();
	private static final Set<Integer> assignedPeons = new HashSet<>();
	private static final Set<Integer> recoveringSummitHeroes = new HashSet<>();
	private static CUnit summitHeroAttacker;
	private static boolean openingVerified;
	private static boolean wyvernsFreed;
	private static boolean cairneRecovering;
	private static boolean finished;
	private static int summitStep;
	private static boolean summitArrived;
	private static boolean siegeBoarded;
	private static boolean repairingSummitTransport;
	private static boolean siegeTrainedBeforeRescue;
	private static List<CUnit> expedition = List.of();
	private static CUnit summitTransport;
	private static CUnit siegeWorker;
	private static CUnit summitTarget;
	private static int summitScoutStep;
	private static Properties carriedThrall;
	private static Properties carriedCairne;
	private static int orderTick;
	private static int reportTick;
	private static int nextBaseDefenseOrder;
	private static CUnit baseThreat;

	private OrcWyvernPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final CUnit thrall = object(globals, "udg_Thrall");
		final CUnit cairne = object(globals, "udg_Cairne");
		if (PLAYTHROUGH && map.endsWith("/orc07.w3m")) {
			if (thrall == null || cairne == null) return;
			check(carriedThrall != null && carriedCairne != null && carriedThrall.equals(snapshot(thrall))
					&& carriedCairne.equals(snapshot(cairne)), "the original Orc07 did not restore both heroes");
			System.out.println("[OrcWyvernProbe] entered the original Orc07 and verified Thrall and Cairne carryover");
			finish();
			return;
		}
		check(map.endsWith("/orc06.w3m"), "unexpected map " + map);
		final int tick = game.getGameTurnTick();
		check(tick < (PLAYTHROUGH ? 84_000 : 48_000) && !"Defeat".equals(ui.getScoreDialogTitle()),
				"the original Wyvern mission stalled or ended in defeat");
		final CQuest summit = object(globals, "udg_QuestSummit");
		check(!globals.getGlobal("udg_GameOver").visit(BooleanJassValueVisitor.getInstance())
				|| globals.getGlobal("udg_Victory").visit(BooleanJassValueVisitor.getInstance()),
				"the original script reported defeat");
		if (PLAYTHROUGH && summit != null && summit.isCompleted()) {
			verifyVictory(ui, globals);
			return;
		}
		if (summit == null || thrall == null || cairne == null || !ui.isUserControlEnabled() || game.isGamePaused()
				|| thrall.isPaused() && !wyvernsFreed || tick < orderTick) return;
		orderTick = tick + 14;
		check(!thrall.isDead() && !cairne.isDead(), "a hero died during " + (summitArrived ? "the summit assault" : "the rescue"));
		final int owner = thrall.getPlayerIndex();
		final var player = game.getPlayer(owner);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(owner);
		if (!openingVerified) {
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var stored = cache == null ? null : cache.getStoredUnit("Orc06", "Thrall");
			check(stored != null, "the campaign cache is missing earned Thrall");
			final var earned = CampaignPlaythroughProbe.heroProperties(stored);
			final var restored = CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(thrall));
			check(earned.stringPropertyNames().stream().filter(key -> key.startsWith("item.") || key.startsWith("ability.")
					|| key.equals("type") || key.equals("name"))
					.allMatch(key -> earned.getProperty(key).equals(restored.getProperty(key)))
					&& thrall.getHeroData().getXp() == stored.xp + 600,
					"the introduction did not restore earned Thrall with the authored experience bonus");
			check(summit.isDiscovered() && cairne.getPlayerIndex() == owner, "the original introduction did not recruit Cairne");
			openingVerified = true;
			System.out.println("[OrcWyvernProbe] original introduction restored earned Thrall and recruited Cairne");
		}
		final CQuest wyvernQuest = object(globals, "udg_QuestWyvern");
		final var allies = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == owner && !unit.isDead() && !unit.isHidden()).toList();
		if (PLAYTHROUGH) defendBase(viewer, orders, allies, owner);
		if (!wyvernsFreed && wyvernQuest != null && wyvernQuest.isCompleted()) {
			final long rescued = allies.stream().filter(unit -> WYVERN.equals(unit.getTypeId())).count();
			if (rescued < 6) return;
			System.out.println("[OrcWyvernProbe] defeated the original Harpies and freed all six Wyverns");
			wyvernsFreed = true;
			if (!PLAYTHROUGH) finish();
			return;
		}
		if (wyvernsFreed) {
			if (summitArrived || landSiegeExpedition(viewer, ui, orders, allies, thrall, cairne)) {
				assaultSummit(viewer, ui, orders, allies, thrall, cairne);
			}
			return;
		}
		final CUnit mine = object(globals, "gg_unit_ngol_0003");
		final var headhunters = allies.stream().filter(unit -> HEADHUNTER.equals(unit.getTypeId())).toList();
		if (PLAYTHROUGH) fundSiege(viewer, orders, allies, owner, mine, 0);
		for (final CUnit unit : allies) {
			if (unit != siegeWorker && PEON.equals(unit.getTypeId()) && assignedPeons.size() < 5 && assignedPeons.add(unit.getHandleId())) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, mine);
			}
			if (BARRACKS.equals(unit.getTypeId()) && headhunters.size() < 5 && unit.getBuildQueue()[0] == null
					&& player.getGold() >= 145 && player.getLumber() >= 20 && player.getFoodCap() - player.getFoodUsed() >= 2) {
				HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, HEADHUNTER.getValue());
			}
		}
		if (waypoints.isEmpty() && headhunters.size() < 5 && tick < 2400) return;
		if (PLAYTHROUGH && baseThreat != null && thrall.canReach(baseThreat, 4_000)) {
			for (final CUnit defender : List.of(thrall, cairne)) {
				if (defender.getCurrentBehavior() != null
						&& defender.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
				if (defender == thrall && CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.chainlightning, baseThreat)
						|| defender == cairne && cairne.canReach(baseThreat, 180)
								&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, cairne, OrderIds.stomp)) continue;
				CampaignPlaythroughProbe.targetOrder(viewer, orders, defender, OrderIds.attack, baseThreat);
			}
			return;
		}
		if (PLAYTHROUGH && waypoints.isEmpty() && tick < 3400
				&& allies.stream().filter(unit -> WATCH_TOWER.equals(unit.getTypeId()) && !unit.isConstructing()).count() < 2) return;
		if (PLAYTHROUGH && !siegeTrainedBeforeRescue && headhunters.size() >= 5 && player.getGold() >= 330) {
			final var catapultType = game.getUnitData().getUnitType(CATAPULT);
			final int rescueSupply = 6 * game.getUnitData().getUnitType(WYVERN).getFoodUsed();
			fundSiege(viewer, orders, allies, owner, mine, rescueSupply + catapultType.getFoodUsed());
			if (player.getFoodCap() - player.getFoodUsed() >= catapultType.getFoodUsed()
					&& player.getGold() >= catapultType.getGoldCost()) {
				final var barracks = allies.stream().filter(unit -> BARRACKS.equals(unit.getTypeId())
						&& unit.getBuildQueue()[0] == null).findFirst().orElse(null);
				siegeTrainedBeforeRescue = barracks != null && HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, "ocat");
			}
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, owner)).toList();
		final List<CUnit> nestDefenders = NEST_HARPIES.stream().map(name -> (CUnit) object(globals, name))
				.filter(unit -> unit != null && !unit.isDead()).toList();
		cairneRecovering = cairne.getLife() < cairne.getMaximumLife() * (cairneRecovering ? 0.88f : 0.66f);
		final CUnit wounded = allies.stream().filter(unit -> unit.getMaximumLife() - unit.getLife() > 140 && thrall.canReach(unit, 580))
				.max(Comparator.comparingDouble(unit -> unit.getMaximumLife() - unit.getLife())).orElse(null);
		for (final CUnit unit : allies) {
			if (unit.isBuilding() || unit.isPaused() || PEON.equals(unit.getTypeId()) || CATAPULT.equals(unit.getTypeId()) || unit.getCurrentAttacks().isEmpty()
					|| unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, 630))
					.min(Comparator.comparingDouble(CUnit::getLife).thenComparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit.isHero() && unit.getLife() < unit.getMaximumLife() * 0.65f
					&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
							|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe"))) continue;
			if (unit == thrall && cairneRecovering && unit.canReach(cairne, 580)
					&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "shea")) continue;
			if (unit == cairne && cairneRecovering) {
				final int step = waypoints.getOrDefault(cairne.getHandleId(), 0);
				final float[] refuge = HARPY_NEST[Math.max(0, step - 4)];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, cairne, refuge[0], refuge[1]);
				continue;
			}
			if (unit == thrall && (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.healingwave, wounded)
					|| enemy != null && (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.chainlightning, enemy)
							|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.spiritwolf)))) continue;
			if (unit == cairne && enemy != null && (unit.canReach(enemy, 180)
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.stomp)
					|| CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.shockwave, enemy.getX(), enemy.getY()))) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (waypoints.getOrDefault(unit.getHandleId(), 0) == HARPY_NEST.length - 1) {
				final CUnit defender = nestDefenders.stream().filter(target -> !target.isHidden())
						.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				if (defender != null && CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, defender.getX(), defender.getY())) continue;
			}
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, HARPY_NEST, HARPY_NEST.length - 1, 145, waypoints);
		}
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (tick >= reportTick) {
			reportTick = tick + 360;
			System.out.printf("[OrcWyvernProbe] Thrall %.0f,%.0f life=%.0f step=%d, Cairne life=%.0f, Headhunters=%d, gold=%d nest=%s%n",
					thrall.getX(), thrall.getY(), thrall.getLife(), waypoints.getOrDefault(thrall.getHandleId(), 0),
					cairne.getLife(), headhunters.size(), player.getGold(),
					nestDefenders.stream().map(unit -> String.format(Locale.ROOT, "%s %.0f,%.0f%s",
							unit.getTypeId(), unit.getX(), unit.getY(), unit.isHidden() ? " hidden" : "")).toList());
		}
	}

	private static boolean landSiegeExpedition(final War3MapViewer viewer, final MeleeUI ui,
			final CPlayerUnitOrderExecutor orders, final List<CUnit> allies, final CUnit thrall, final CUnit cairne) {
		final var game = viewer.simulation;
		final int player = thrall.getPlayerIndex();
		final var wyverns = allies.stream().filter(unit -> WYVERN.equals(unit.getTypeId())).toList();
		if (!siegeBoarded) {
			final CUnit mine = object(game.getGlobalScope(), "gg_unit_ngol_0003");
			final var lab = game.getUnits().stream().filter(unit -> "ngad".equals(unit.getTypeId().toString()) && !unit.isDead())
					.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(2368, -1024))).orElseThrow();
			if (summitTransport == null || summitTransport.isDead()) {
				summitTransport = allies.stream().filter(unit -> ZEPPELIN.equals(unit.getTypeId())).findFirst().orElse(null);
			}
			if (summitTransport != null) {
				final float recoveryThreshold = repairingSummitTransport ? 0.95f : 0.67f;
				repairingSummitTransport = summitTransport.getLife() < summitTransport.getMaximumLife() * recoveryThreshold;
				if (repairingSummitTransport) {
					final CUnit worker = allies.stream().filter(unit -> unit != siegeWorker && PEON.equals(unit.getTypeId())
							&& !unit.isPaused() && unit.canReach(2592, -6480, 900))
							.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(2592, -6480))).orElse(null);
					if (worker != null && game.getPlayer(player).getGold() >= 40) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, summitTransport, 2592, -6480);
						if (summitTransport.canReach(2592, -6480, 145)
								&& !CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.repair, summitTransport)) {
							repairingSummitTransport = false;
						}
						if (repairingSummitTransport) return false;
					}
				}
			}
			final CUnit catapult = (expedition.isEmpty() ? allies : expedition).stream()
					.filter(unit -> CATAPULT.equals(unit.getTypeId()) && !unit.isDead()).findFirst().orElse(null);
			if (catapult == null) expedition = List.of();
			if (expedition.isEmpty()) {
				final var ranged = allies.stream().filter(unit -> HEADHUNTER.equals(unit.getTypeId())).toList();
				final var workers = allies.stream().filter(unit -> PEON.equals(unit.getTypeId())).toList();
				final int foodReserve = (catapult == null ? game.getUnitData().getUnitType(CATAPULT).getFoodUsed() : 0)
						+ Math.max(0, 4 - ranged.size()) * game.getUnitData().getUnitType(HEADHUNTER).getFoodUsed();
				fundSiege(viewer, orders, allies, player, mine, foodReserve);
				if (workers.size() < 4) {
					allies.stream().filter(unit -> TOWN_HALLS.contains(unit.getTypeId()) && unit.getBuildQueue()[0] == null)
							.findFirst().ifPresent(unit -> HumanCampaignPlaythroughProbe.train(viewer, orders, unit, "opeo"));
				}
				if (catapult == null || ranged.size() < 4) {
					allies.stream().filter(unit -> BARRACKS.equals(unit.getTypeId()) && unit.getBuildQueue()[0] == null)
							.findFirst().ifPresent(unit -> HumanCampaignPlaythroughProbe.train(viewer, orders, unit,
									catapult == null ? "ocat" : "ohun"));
					if (game.getGameTurnTick() >= reportTick) {
						reportTick = game.getGameTurnTick() + 360;
						final var mineAbility = mine.getFirstAbilityOfType(CAbilityGoldMine.class);
						System.out.printf("[OrcWyvernProbe] preparing summit siege=%s ranged=%d workers=%d gold=%d lumber=%d mine=%d raid=%s%n",
								catapult != null, ranged.size(), workers.size(), game.getPlayer(player).getGold(),
								game.getPlayer(player).getLumber(), mineAbility == null ? 0 : mineAbility.getGold(),
								baseThreat == null ? "none" : baseThreat.getTypeId());
					}
					return false;
				}
				final var force = new java.util.ArrayList<>(List.of(thrall, cairne, catapult));
				force.addAll(ranged.subList(0, 4));
				expedition = List.copyOf(force);
			}
			final boolean readyToMarch = !expedition.isEmpty()
					&& (!laboratorySteps.isEmpty() || cairne.getLife() >= cairne.getMaximumLife() * 0.8f);
			final var group = readyToMarch ? expedition : List.<CUnit>of();
			if (summitTransport == null) {
				for (final CUnit unit : group) {
					advanceToLaboratory(viewer, orders, unit, catapult, thrall, cairne);
				}
			}
			else {
				final var ship = summitTransport;
				final var cargo = ship.getCargoData();
				for (int index = cargo.getCargoCount() - 1; index >= 0 && !group.isEmpty(); index--) {
					final CUnit passenger = cargo.getUnit(index);
					if (!expedition.contains(passenger)) {
						CampaignPlaythroughProbe.targetOrder(viewer, orders, ship, OrderIds.unload, passenger);
					}
				}
				final CUnit nextPassenger = group.stream().filter(unit -> !unit.isDead() && unit.getTransport() != ship)
						.min(Comparator.comparingDouble(ship::distanceSquaredNoCollision)).orElse(null);
				if (nextPassenger != null) {
					if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, ship, OrderIds.load, nextPassenger)
							&& !ship.canReach(nextPassenger, 88)) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, ship, nextPassenger.getX(), nextPassenger.getY());
					}
					for (final CUnit unit : group) {
						if (!unit.isDead() && unit.getTransport() != ship && !unit.isHidden() && unit.canReach(ship, 480)) {
							CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, ship);
						}
					}
				}
			}
			if (summitTransport == null && lab.getNeutralBuildingData() != null
					&& lab.getNeutralBuildingData().getSelectedPlayerUnit(player) != null) {
				final var seller = lab.getFirstAbilityOfType(CAbilitySellUnits.class);
				check(seller != null && seller.getUnitsSold().contains(ZEPPELIN), "the original Orc06 laboratory has no Zeppelin sale");
				final int orderId = player | ((seller.getUnitsSold().indexOf(ZEPPELIN) + 1) << 8);
				HumanCampaignPlaythroughProbe.immediate(viewer, orders, lab, orderId);
			}
			if (summitTransport != null && !expedition.isEmpty() && thrall.getTransport() == summitTransport
					&& cairne.getTransport() == summitTransport && expedition.stream().filter(unit -> !unit.isDead())
							.allMatch(unit -> unit.getTransport() == summitTransport)) {
				check(catapult != null && !catapult.isDead(), "the summit Catapult was destroyed before boarding");
				siegeBoarded = true;
				summitTransport.addDamageTakenListener((simulation, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
					if (attacker != null && actual > 0) {
						System.out.printf("[OrcWyvernProbe] tick=%d %s at %.0f,%.0f dealt %.0f to Zeppelin life=%.0f%n",
								simulation.getGameTurnTick(), attacker.getTypeId(), attacker.getX(), attacker.getY(), actual, target.getLife());
					}
				});
				System.out.println("[OrcWyvernProbe] boarded both heroes and siege support in an original Zeppelin");
			}
			ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
			if (game.getGameTurnTick() >= reportTick) {
				reportTick = game.getGameTurnTick() + 360;
				final var owner = game.getPlayer(player);
				System.out.printf("[OrcWyvernProbe] laboratory Thrall %.0f,%.0f Cairne life=%.0f siege=%s Zeppelin=%s cargo=%d Wyverns=%d gold=%d lumber=%d food=%d/%d%n",
						thrall.getX(), thrall.getY(), cairne.getLife(), catapult != null,
						summitTransport == null ? "none" : Math.round(summitTransport.getX()) + "," + Math.round(summitTransport.getY()),
						summitTransport == null ? 0 : summitTransport.getCargoData().getCargoCount(), wyverns.size(),
						owner.getGold(), owner.getLumber(), owner.getFoodUsed(), owner.getFoodCap());
				if (summitTransport != null && !expedition.isEmpty()) {
					final CUnit ship = summitTransport;
					System.out.printf("[OrcWyvernProbe] awaiting passengers %s%n", expedition.stream()
							.filter(unit -> unit.getTransport() != ship)
							.map(unit -> String.format(Locale.ROOT, "%s at %.0f,%.0f life=%.0f hidden=%s",
									unit.getTypeId(), unit.getX(), unit.getY(), unit.getLife(), unit.isHidden())).toList());
				}
			}
			return false;
		}
		check(!summitTransport.isDead(), "the summit Zeppelin was shot down before landing");
		if (summitStep == 0 && wyverns.stream().filter(unit -> unit.canReach(summitTransport, 650)).count() < Math.min(4, wyverns.size())) {
			for (final CUnit unit : wyverns) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, summitTransport.getX(), summitTransport.getY());
			}
			return false;
		}
		final float[] approach = SUMMIT_AIR_ROUTE[summitStep];
		if (summitStep < SUMMIT_AIR_ROUTE.length - 1
				&& summitTransport.distanceSquaredNoCollision(approach[0], approach[1]) < 170 * 170) summitStep++;
		final float[] destination = SUMMIT_AIR_ROUTE[summitStep];
		final var owner = game.getPlayer(player);
		final CUnit interceptor = game.getUnits().stream().filter(enemy -> !enemy.isDead() && !enemy.isHidden()
				&& !enemy.isInvulnerable() && !owner.hasAlliance(enemy.getPlayerIndex(), CAllianceType.PASSIVE)
				&& enemy.getCurrentAttacks().stream().anyMatch(attack -> attack.getTargetsAllowed().contains(CTargetType.AIR))
				&& summitTransport.canReach(enemy, 950) && enemy.isVisible(game, player)
				&& wyverns.stream().anyMatch(escort -> escort.canReach(enemy, 780)))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		for (final CUnit unit : wyverns) {
			if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, interceptor)) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, destination[0], destination[1]);
			}
		}
		if (summitStep == SUMMIT_AIR_ROUTE.length - 1
				&& summitTransport.distanceSquaredNoCollision(destination[0], destination[1]) < 200 * 200) {
			if (summitTransport.getCargoData().isEmpty()) {
				check(!thrall.isHidden() && !cairne.isHidden(), "the heroes did not disembark on the summit");
				summitArrived = true;
				for (final CUnit hero : List.of(thrall, cairne)) {
					hero.addDamageTakenListener((simulation, attacker, victim, attack, ranged, type, damage, bonus, actual) -> {
						if (attacker != null && actual > 0 && victim.getLife() < victim.getMaximumLife() * 0.79f) {
							summitHeroAttacker = attacker;
						}
					});
				}
				System.out.println("[OrcWyvernProbe] disembarked the ground assault on the original summit");
				return true;
			}
			final var cargo = summitTransport.getCargoData();
			final int previouslyAboard = cargo.getCargoCount();
			for (int index = previouslyAboard - 1; index >= 0; index--) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, summitTransport, OrderIds.unload, cargo.getUnit(index));
			}
			if (cargo.getCargoCount() == previouslyAboard) {
				CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, summitTransport, OrderIds.unloadall,
						SUMMIT_LANDING[0], SUMMIT_LANDING[1]);
			}
		}
		else if (!wyverns.isEmpty() && interceptor != null && summitTransport.canReach(interceptor, 820)) {
			if (summitStep == 0) holdSummitTransport(game);
			else {
				final float[] refuge = SUMMIT_AIR_ROUTE[summitStep - 1];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, summitTransport, refuge[0], refuge[1]);
			}
		}
		else if (interceptor == null) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, summitTransport, destination[0], destination[1]);
		}
		else {
			holdSummitTransport(game);
		}
		ui.getCameraManager().setTarget(summitTransport.getX(), summitTransport.getY());
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 300;
			System.out.printf("[OrcWyvernProbe] summit flight %.0f,%.0f life=%.0f step=%d escort=%d%n",
					summitTransport.getX(), summitTransport.getY(), summitTransport.getLife(), summitStep, wyverns.size());
		}
		return false;
	}

	private static void holdSummitTransport(final CSimulation game) {
		if (summitTransport.getCurrentOrder() == null || summitTransport.getCurrentOrder().getOrderId() != OrderIds.stop) {
			summitTransport.order(game, OrderIds.stop, null);
		}
	}

	private static void advanceToLaboratory(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit unit, final CUnit catapult, final CUnit thrall, final CUnit cairne) {
		if (unit.isDead() || unit.isHidden()) return;
		final var game = viewer.simulation;
		final int player = thrall.getPlayerIndex();
		final var enemy = game.getUnits().stream().filter(target -> !target.isDead() && !target.isHidden()
				&& !target.isInvulnerable() && !game.getPlayer(player).hasAlliance(target.getPlayerIndex(), CAllianceType.PASSIVE)
				&& target.isVisible(game, player) && unit.canReach(target, 560))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		if (unit.isHero() && unit.getLife() < unit.getMaximumLife() * 0.65f
				&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea"))) return;
		if (unit == thrall && (cairne.getLife() < cairne.getMaximumLife() * 0.78f
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.healingwave, cairne)
				|| enemy != null && (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.chainlightning, enemy)
						|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, thrall, OrderIds.spiritwolf)))) return;
		if (unit == cairne && enemy != null && unit.canReach(enemy, 180)
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.stomp)) return;
		if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) return;
		final float[][] route = unit == catapult ? CATAPULT_ROUTE : LABORATORY_ROUTE;
		CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, route.length - 1, 146, laboratorySteps);
	}

	private static void fundSiege(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> allies, final int player, final CUnit mine, final int foodReserve) {
		if (siegeWorker == null || siegeWorker.isDead()) {
			siegeWorker = allies.stream().filter(unit -> PEON.equals(unit.getTypeId()))
					.max(Comparator.comparingDouble(mine::distanceSquaredNoCollision)).orElse(null);
		}
		for (final CUnit peon : allies) {
			if (peon != siegeWorker && PEON.equals(peon.getTypeId()) && !peon.isPaused()
					&& (peon.getCurrentBehavior() == null || peon.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.IDLE)) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, peon, OrderIds.harvest, mine);
			}
		}
		if (siegeWorker == null || siegeWorker.isHidden()) return;
		final var game = viewer.simulation;
		final var owner = game.getPlayer(player);
		final var catapultType = game.getUnitData().getUnitType(CATAPULT);
		if (owner.getFoodCap() - owner.getFoodUsed() < foodReserve
				&& allies.stream().noneMatch(unit -> BURROW.equals(unit.getTypeId()) && unit.isConstructingOrUpgrading())) {
			for (int y = -6240; y >= -6720; y -= 160) {
				for (int x = 1184; x <= 2144; x += 160) {
					if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, siegeWorker, BURROW.getValue(), x, y)) return;
				}
			}
		}
		if (siegeWorker.getCurrentOrder() != null && siegeWorker.getCurrentOrder().getOrderId() == BURROW.getValue()) return;
		if (owner.getLumber() < catapultType.getLumberCost() + 90) {
			game.getDestructables().stream().filter(tree -> !tree.isDead())
					.sorted(Comparator.comparingDouble(siegeWorker::distanceSquaredNoCollision))
					.anyMatch(tree -> CampaignPlaythroughProbe.targetOrder(viewer, orders, siegeWorker, OrderIds.harvest, tree));
		}
	}

	private static void defendBase(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> allies, final int player) {
		final var game = viewer.simulation;
		final CUnit hall = allies.stream().filter(unit -> TOWN_HALLS.contains(unit.getTypeId())).findFirst().orElse(null);
		baseThreat = null;
		if (hall == null) return;
		final CUnit threat = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == 1 && !unit.isDead()
				&& !unit.isHidden() && !unit.isInvulnerable() && hall.canReach(unit, 1_550)
				&& unit.isVisible(game, player)).min(Comparator.comparingDouble(hall::distanceSquaredNoCollision)).orElse(null);
		baseThreat = threat;
		if (game.getGameTurnTick() >= nextBaseDefenseOrder) {
			nextBaseDefenseOrder = game.getGameTurnTick() + 150;
			final var burrows = allies.stream().filter(unit -> BURROW.equals(unit.getTypeId()) && !unit.isConstructing()).toList();
			if (threat != null && burrows.stream().anyMatch(unit -> unit.canReach(threat, 720))) {
				burrows.stream().filter(unit -> unit.getCargoData() != null && unit.getCargoData().hasCapacity(1)
							&& unit.canReach(threat, 720))
						.min(Comparator.comparingDouble(threat::distanceSquaredNoCollision))
						.ifPresent(unit -> HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.battlestations));
			}
			else {
				burrows.stream().filter(unit -> unit.getCargoData() != null && !unit.getCargoData().isEmpty())
						.forEach(unit -> HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.standdown));
			}
		}
		final var guards = allies.stream().filter(unit -> GRUNT.equals(unit.getTypeId())
				&& hall.canReach(unit, 1_250) && !expedition.contains(unit)).toList();
		for (final CUnit guard : guards) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, guard, OrderIds.attack, threat);
		}
		final var owner = game.getPlayer(player);
		if (owner.getGold() >= (siegeBoarded ? 190 : 480) && guards.size() < 5) {
			allies.stream().filter(unit -> BARRACKS.equals(unit.getTypeId()) && unit.getBuildQueue()[0] == null)
					.findFirst().ifPresent(unit -> HumanCampaignPlaythroughProbe.train(viewer, orders, unit, "ogru"));
		}
		final CUnit worker = allies.stream().filter(unit -> unit != siegeWorker && PEON.equals(unit.getTypeId()) && !unit.isPaused()
				&& hall.canReach(unit, 620) && (unit.getCurrentOrder() == null
						|| unit.getCurrentOrder().getOrderId() != WATCH_TOWER.getValue()
								&& unit.getCurrentOrder().getOrderId() != BURROW.getValue())).findFirst().orElse(null);
		if (worker == null || allies.stream().anyMatch(unit -> WATCH_TOWER.equals(unit.getTypeId()) && unit.isConstructing())) return;
		final CUnit damaged = allies.stream().filter(unit -> unit.isBuilding() && !unit.isConstructing()
				&& unit.getLife() < unit.getMaximumLife() * 0.65f && worker.canReach(unit, 400))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		if (owner.getGold() >= 100 && owner.getLumber() >= 20
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.repair, damaged)) return;
		final long towers = allies.stream().filter(unit -> WATCH_TOWER.equals(unit.getTypeId())).count();
		if (towers >= 5 || owner.getGold() < (siegeBoarded || towers < 2 ? 155 : 375) || owner.getLumber() < 85) return;
		for (int y = -5600; y >= -6368; y -= 192) {
			for (int x = 1568; x <= 2528; x += 192) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, worker, WATCH_TOWER.getValue(), x, y)) return;
			}
		}
	}

	private static void assaultSummit(final War3MapViewer viewer, final MeleeUI ui, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> allies, final CUnit thrall, final CUnit cairne) {
		final var game = viewer.simulation;
		final var wyverns = allies.stream().filter(unit -> WYVERN.equals(unit.getTypeId())).toList();
		final var leader = wyverns.stream().filter(unit -> unit.getLife() >= unit.getMaximumLife() * 0.18f)
				.findFirst().orElse(thrall.isHidden() ? cairne : thrall);
		if (summitArrived) {
			final var owner = game.getPlayer(thrall.getPlayerIndex());
			final var visible = game.getUnits().stream().filter(unit -> !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
					&& !unit.isDead()
					&& !unit.isHidden() && !unit.isInvulnerable() && unit.isVisible(game, thrall.getPlayerIndex())).toList();
			final var nearbyHero = visible.stream().filter(CUnit::isHero)
					.filter(enemy -> thrall.canReach(enemy, 800) || cairne.canReach(enemy, 800) || leader.canReach(enemy, 800))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			final var antiAir = visible.stream().filter(enemy -> leader.canReach(enemy, 700)
					&& SUMMIT_AIR_DEFENDERS.contains(enemy.getTypeId().toString()))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (nearbyHero != null) summitTarget = nearbyHero;
			else if (summitHeroAttacker != null && visible.contains(summitHeroAttacker) && !summitHeroAttacker.isBuilding()
					&& (thrall.canReach(summitHeroAttacker, 820) || cairne.canReach(summitHeroAttacker, 820))) {
				summitTarget = summitHeroAttacker;
			}
			else if (antiAir != null) summitTarget = antiAir;
			else if (!visible.contains(summitTarget)) {
				summitTarget = visible.stream().filter(enemy -> enemy.getPlayerIndex() == 1
						&& REQUIRED_BUILDINGS.contains(enemy.getTypeId().toString()))
						.min(Comparator.comparingDouble(leader::distanceSquaredNoCollision)).orElse(null);
			}
		}
		rescueSummitHeroes(viewer, orders, thrall, cairne);
		if (summitTarget == null && summitScoutStep < SUMMIT_SCOUT_ROUTE.length - 1
				&& leader.distanceSquaredNoCollision(SUMMIT_SCOUT_ROUTE[summitScoutStep][0],
						SUMMIT_SCOUT_ROUTE[summitScoutStep][1]) < 230 * 230) summitScoutStep++;
		final float[] scoutPoint = SUMMIT_SCOUT_ROUTE[summitScoutStep];
		final CUnit wounded = expedition.stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& unit.getMaximumLife() - unit.getLife() >= 120 && thrall.canReach(unit, 640))
				.min(Comparator.comparingDouble(unit -> unit.getLife() / unit.getMaximumLife())).orElse(null);
		for (final CUnit unit : wyverns) {
			if (unit.getLife() < unit.getMaximumLife() * 0.18f) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, SUMMIT_AIR_REFUGE[0], SUMMIT_AIR_REFUGE[1]);
			}
			else if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, summitTarget)) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, scoutPoint[0], scoutPoint[1]);
			}
		}
		for (final CUnit unit : expedition) {
			if (unit.isDead() || unit.isHidden()) continue;
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if ((unit == thrall || unit == cairne) && recoverSummitHero(viewer, orders, unit)) continue;
			if (unit == thrall && CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.healingwave, wounded)) continue;
			if (CATAPULT.equals(unit.getTypeId()) && summitTarget != null && !summitTarget.isBuilding()) {
				final int rear = Math.max(0, summitScoutStep - 2);
				final float[] position = rear == 0 ? SUMMIT_LANDING : SUMMIT_SCOUT_ROUTE[rear];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, position[0], position[1]);
				continue;
			}
			if (unit == thrall && summitTarget != null && !summitTarget.isBuilding()) {
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.chainlightning, summitTarget)
						|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.spiritwolf)) continue;
			}
			if (unit == cairne && summitTarget != null && !summitTarget.isBuilding() && unit.canReach(summitTarget, 180)
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.stomp)) continue;
			if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, summitTarget)) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, scoutPoint[0], scoutPoint[1]);
			}
		}
		ui.getCameraManager().setTarget(leader.getX(), leader.getY());
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 240;
			System.out.printf("[OrcWyvernProbe] summit scout=%.0f,%.0f route=%d Thrall=%.0f,%.0f life=%.0f Cairne=%.0f,%.0f life=%.0f Wyverns=%d ground=%s target=%s%n",
					leader.getX(), leader.getY(), summitScoutStep, thrall.getX(), thrall.getY(), thrall.getLife(),
					cairne.getX(), cairne.getY(), cairne.getLife(), wyverns.size(),
					expedition.stream().filter(unit -> !unit.isDead())
							.map(unit -> unit.getTypeId() + (unit.isHidden() ? " aboard" : "")).toList(),
					summitTarget == null ? "none" : String.format(Locale.ROOT, "%s at %.0f,%.0f life=%.0f",
							summitTarget.getTypeId(), summitTarget.getX(), summitTarget.getY(), summitTarget.getLife()));
		}
	}

	private static boolean recoverSummitHero(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit hero) {
		final int handle = hero.getHandleId();
		final float health = hero.getLife() / hero.getMaximumLife();
		if (health < 0.73f && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, hero, "pghe")
				|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, hero, "phea"))) return true;
		if (health < 0.44f) recoveringSummitHeroes.add(handle);
		if (health >= 0.68f) recoveringSummitHeroes.remove(handle);
		if (!recoveringSummitHeroes.contains(handle)) return false;
		if (summitTransport != null && !summitTransport.isDead() && summitTransport.getLife() > 175
				&& hero.canReach(summitTransport, 430)) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, hero, OrderIds.smart, summitTransport);
		}
		else CampaignPlaythroughProbe.pointOrder(viewer, orders, hero, SUMMIT_LANDING[0], SUMMIT_LANDING[1]);
		return true;
	}

	private static void rescueSummitHeroes(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit thrall, final CUnit cairne) {
		if (summitTransport == null || summitTransport.isDead()) return;
		final CUnit passenger = List.of(thrall, cairne).stream()
				.filter(hero -> !hero.isHidden() && recoveringSummitHeroes.contains(hero.getHandleId()))
				.filter(hero -> hero.distanceSquaredNoCollision(SUMMIT_LANDING[0], SUMMIT_LANDING[1]) < 860 * 860)
				.min(Comparator.comparingDouble(hero -> hero.getLife() / hero.getMaximumLife())).orElse(null);
		if (passenger != null && !summitTransport.canReach(passenger, 112)) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, summitTransport, passenger.getX(), passenger.getY());
		}
		else if (passenger == null && !summitTransport.getCargoData().isEmpty()) {
			final boolean closeEnemy = summitTarget != null && summitTransport.canReach(summitTarget, 670);
			final boolean healed = summitTransport.getCargoData().getUnit(0).getLife()
					>= summitTransport.getCargoData().getUnit(0).getMaximumLife() * 0.68f;
			if (closeEnemy && !healed) CampaignPlaythroughProbe.pointOrder(viewer, orders, summitTransport, SUMMIT_AIR_REFUGE[0], SUMMIT_AIR_REFUGE[1]);
			else if (summitTransport.distanceSquaredNoCollision(SUMMIT_LANDING[0], SUMMIT_LANDING[1]) < 200 * 200) {
				CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, summitTransport, OrderIds.unloadall,
						SUMMIT_LANDING[0], SUMMIT_LANDING[1]);
			}
			else CampaignPlaythroughProbe.pointOrder(viewer, orders, summitTransport, SUMMIT_LANDING[0], SUMMIT_LANDING[1]);
		}
	}

	private static void verifyVictory(final MeleeUI ui, final GlobalScope globals) {
		check(wyvernsFreed && summitArrived, "native victory fired before the summit assault");
		final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
		final var thrall = cache == null ? null : cache.getStoredUnit("Orc07", "Thrall");
		final var cairne = cache == null ? null : cache.getStoredUnit("Orc07", "Cairne");
		if (carriedThrall == null && thrall != null && cairne != null) {
			carriedThrall = CampaignPlaythroughProbe.heroProperties(thrall);
			carriedCairne = CampaignPlaythroughProbe.heroProperties(cairne);
			System.out.println("[OrcWyvernProbe] captured the original summit and verified both stored heroes");
		}
		HumanCampaignPlaythroughProbe.continueVictory(ui);
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static void finish() {
		finished = true;
		Gdx.app.exit();
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc Wyvern audit: " + message);
	}
}
