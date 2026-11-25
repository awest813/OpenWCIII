package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.neutral.CAbilityWayGate;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;

/** Exercises the first native Tree of Life bonus and the researched Pillage economy in Orc04. */
public final class OrcLumberPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcLumberAudit");
	private static final boolean PLAYTHROUGH = Boolean.getBoolean("warsmash.orcLumberPlaythrough");
	private static final boolean RESUME_INTERLUDE = Boolean.getBoolean("warsmash.orcLumberResumeInterlude");
	private static final boolean HUNTER_OPENING = Boolean.getBoolean("warsmash.orcHunterOpening");
	private static final boolean HUNTER_FOUNTAIN = Boolean.getBoolean("warsmash.orcHunterFountain");
	private static final boolean HUNTER_VICTORY = Boolean.getBoolean("warsmash.orcHunterVictory");
	private static final War3ID PILLAGE = War3ID.fromString("Ropg");
	private static final War3ID PEON = War3ID.fromString("opeo");
	private static final War3ID STRONGHOLD = War3ID.fromString("ostr");
	private static final War3ID GREAT_HALL = War3ID.fromString("ogre");
	private static final War3ID GOLD_MINE = War3ID.fromString("ngol");
	private static final War3ID WITCH_DOCTOR = War3ID.fromString("odoc");
	private static final War3ID SENTRY_WARD = War3ID.fromString("oeye");
	private static final War3ID CHAOS_PEON = War3ID.fromString("ncpn");
	private static final War3ID CHAOS_GRUNT = War3ID.fromString("nchg");
	private static final float[][] HUNTER_GATE_ROUTE = { { 5968, -4944 }, { 6032, -4368 }, { 6192, -3792 },
			{ 6576, -3216 }, { 6576, -2640 }, { 6576, -2064 }, { 6576, -1488 }, { 6416, -912 }, { 6416, -336 },
			{ 6416, 240 }, { 6224, 816 }, { 6224, 1392 }, { 6224, 1808 }, { 6384, 1808 }, { 6384, 2128 },
			{ 6512, 2256 }, { 6608, 2320 }, { 6640, 2576 },
			{ 6512, 3056 }, { 6512, 3568 }, { 6640, 3696 }, { 6640, 4272 },
			{ 6320, 4848 }, { 5744, 5296 },
			{ 5168, 5296 }, { 4592, 5296 }, { 4016, 5296 }, { 3440, 5296 }, { 2864, 5072 }, { 2528, 4816 } };
	private static final int HUNTER_ESCORT_HOLD = 11;
	private static final int HUNTER_TREE_LINE = 15;
	private static final int HUNTER_PASS_EXIT = 18;
	private static final int HUNTER_LANE_EXIT = 20;
	private static final float[][] HUNTER_PASS_TREES = { { 6720, 2304 }, { 6592, 2368 }, { 6656, 2496 }, { 6720, 2624 } };
	private static final float[][] HUNTER_FOUNTAIN_ROUTE = { { -400, 3344 }, { -976, 2896 }, { -1552, 2768 },
			{ -1584, 3312 }, { -1872, 3888 }, { -2032, 4464 }, { -2032, 5040 }, { -2032, 5616 },
			{ -2032, 6192 }, { -2032, 6768 }, { -2032, 7344 }, { -2096, 7920 }, { -2032, 8496 },
			{ -1456, 8496 }, { -880, 8336 }, { -304, 8336 }, { 272, 7952 }, { 848, 7824 }, { 1424, 7824 },
			{ 2000, 8080 }, { 2352, 8416 }, { 2384, 9040 }, { 2480, 9648 } };
	private static final int HUNTER_FOUNTAIN_RALLY = 17;
	private static final float[][] CENARIUS_ROUTE = { { 5616, -5616 }, { 5232, -5680 }, { 4848, -5936 },
			{ 4464, -5936 }, { 4080, -5936 }, { 3696, -5936 }, { 3152, -5936 }, { 2768, -5936 },
			{ 2384, -5936 }, { 2000, -5808 }, { 1680, -5424 }, { 1424, -5040 }, { 1040, -4656 },
			{ 656, -4304 }, { 272, -4304 }, { -112, -4208 }, { -496, -3952 }, { -880, -3920 },
			{ -1264, -3920 }, { -1648, -3792 } };
	private static final int CENARIUS_RALLY = 5;
	private static final float[][] WESTERN_GROVE_ROUTE = { { 5552, -7824 }, { 5232, -7824 }, { 4912, -7824 },
			{ 4592, -7824 }, { 4272, -7824 }, { 3952, -8080 }, { 3632, -8400 }, { 3312, -8720 } };
	private static final String[] GROVES = { "01", "05", "04", "03", "02" };
	private static final float[][][] GROVE_ROUTES = { WESTERN_GROVE_ROUTE,
			{ { 2896, -8624 }, { 2576, -8304 }, { 2256, -8048 }, { 1936, -8016 }, { 1616, -7728 },
					{ 976, -7696 }, { 336, -7504 }, { -304, -7376 }, { -944, -7376 }, { -1584, -7760 }, { -2224, -7984 } },
			{ { -2256, -7600 }, { -2352, -6960 }, { -2352, -6320 }, { -2352, -5680 }, { -2128, -5040 },
					{ -2064, -4400 }, { -1744, -3760 }, { -1488, -3120 }, { -848, -2480 }, { -784, -1840 },
					{ -784, -1200 }, { -784, -560 }, { -1264, 80 }, { -1264, 720 }, { -1264, 1360 },
					{ -1456, 2000 }, { -1520, 2640 }, { -1712, 3280 }, { -2192, 3920 } },
			{ { -1616, 3984 }, { -976, 3984 }, { -336, 3984 }, { 304, 4016 }, { 944, 4144 }, { 1584, 4144 },
					{ 1904, 4144 }, { 2544, 4688 }, { 3184, 4912 }, { 3824, 4912 }, { 4464, 4816 }, { 5104, 4592 },
					{ 5424, 4272 } },
			{ { 5616, 3792 }, { 5616, 3152 }, { 5616, 2512 }, { 5648, 1872 }, { 6160, 1232 }, { 6384, 592 },
					{ 6672, 272 }, { 6896, -368 }, { 7216, -688 } } };
	private static final float[][] EXPANSION_ROUTE = Stream.concat(Arrays.stream(GROVE_ROUTES[0]), Arrays.stream(GROVE_ROUTES[1]))
			.toArray(float[][]::new);
	private static final float[][] EXPANSION_SITES = { { -2688, -8000 }, { -2816, -7744 }, { -3008, -7616 },
			{ -3072, -8320 }, { -2624, -7680 }, { -2816, -8256 }, { -2496, -7872 } };
	private static final float[][] REINFORCEMENT_ROUTE = Arrays.stream(GROVE_ROUTES)
			.flatMap(Arrays::stream).toArray(float[][]::new);
	private static final Map<Integer, Integer> escortSteps = new HashMap<>();
	private static int waypoint;
	private static int groveIndex;
	private static int lastReportedGrove = -1;
	private static int orderTick;
	private static int reportTick;
	private static boolean introHandled;
	private static boolean researchObserved;
	private static boolean finished;
	private static CUnit expansionBuilder;
	private static int expansionSite;
	private static int expansionOrderTick;
	private static boolean expansionReported;
	private static boolean observedInterlude;
	private static int gromFallbackGrove = -1;
	private static int gromFallbackStep;
	private static int hunterStartingGold = -1;
	private static boolean wardOrdered;
	private static boolean hunterSettlementReady;
	private static boolean hunterChaos;
	private static boolean cenariusExpeditionReady;
	private static int chaosRefugeStep = HUNTER_FOUNTAIN_ROUTE.length - 2;
	private static boolean gromCrossedHunterGate;
	private static CUnit hunterLumberPeon;
	private static CUnit hunterEscortLeader;
	private static Set<Integer> assembledHunterEscort = Set.of();
	private static int hunterRetreatStep = -1;
	private static final Set<Integer> hunterGateCrossings = new HashSet<>();
	private static final Map<Integer, Integer> fountainSteps = new HashMap<>();
	private static boolean fountainEscortReady;
	private static int cenariusMarchStep = CENARIUS_RALLY + 1;
	private static final Map<Integer, Integer> cenariusSteps = new HashMap<>();
	private static Properties carriedGrom;

	private OrcLumberPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || !initialized || finished) return;
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (HUNTER_VICTORY && map.endsWith("/orc06.w3m")) {
			check(carriedGrom != null, "Orc06 loaded without the native Cenarius victory and saved Grom");
			System.out.println("[OrcLumberProbe] entered the original Orc06 after defeating Cenarius");
			finish();
			return;
		}
		final CUnit grom = object(globals, "udg_Grom");
		if (HUNTER_OPENING) {
			check(map.endsWith("/orc05.w3m"), "unexpected Hunter of Shadows map " + map);
			verifyHunterOpening(viewer, ui, globals, grom);
			return;
		}
		if (PLAYTHROUGH && map.endsWith("/orc04interlude.w3m")) {
			check(RESUME_INTERLUDE || carriedGrom != null, "interlude loaded without completing the lumber objective");
			if (!observedInterlude) {
				observedInterlude = true;
				System.out.println("[OrcLumberProbe] entered the original Orc04 story interlude");
			}
			check(game.getGameTurnTick() < 15_000, "the Orc04 story interlude did not finish");
			return;
		}
		if (PLAYTHROUGH && map.endsWith("/orc05.w3m")) {
			if (grom == null) return;
			if (RESUME_INTERLUDE && carriedGrom == null) {
				final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
				final var saved = cache == null ? null : cache.getStoredUnit("Orc05", "Grom");
				check(saved != null, "earned Grom was missing from the Orc04 cache");
				carriedGrom = CampaignPlaythroughProbe.heroProperties(saved);
			}
			check(observedInterlude, "Orc05 was reached without playing the Orc04 story interlude");
			check(carriedGrom != null && carriedGrom.equals(snapshot(grom)), "Grom carryover mismatch");
			System.out.println("[OrcLumberProbe] entered Orc05 and verified Grom carryover");
			finish();
			return;
		}
		check(map.endsWith("/orc04.w3m"), "unexpected map " + map);
		if (grom == null) return;
		final int tick = game.getGameTurnTick();
		check(tick < (PLAYTHROUGH ? 100_000 : 28_000), "the lumber objective stalled at grove " + groveIndex + " waypoint " + waypoint);
		check(!grom.isDead() && !"Defeat".equals(ui.getScoreDialogTitle()), "native defeat in the lumber expedition");
		final CQuest quest = object(globals, "udg_QuestLumber");
		if (PLAYTHROUGH && quest != null && quest.isCompleted()) {
			if (carriedGrom == null) {
				carriedGrom = snapshot(grom);
				System.out.println("[OrcLumberProbe] completed the original 15000 lumber objective");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		final Trigger intro = object(globals, "gg_trg_IntroCancel");
		if (!introHandled && intro != null && intro.isEnabled() && !ui.isUserControlEnabled()) {
			introHandled = ui.keyDown(Input.Keys.ESCAPE);
		}
		if (!ui.isUserControlEnabled() || game.isGamePaused() || grom.isPaused() || tick < orderTick
				|| flag(globals, "udg_InCinematic")) return;
		orderTick = tick + 20;

		final var player = game.getPlayer(grom.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
				&& !unit.isDead() && !unit.isHidden()).toList();
		final CUnit hall = owned.stream().filter(unit -> STRONGHOLD.equals(unit.getTypeId())).findFirst().orElse(null);
		check(hall != null, "the original Orc base was destroyed");
		if (flag(globals, "udg_PillageResearched")) {
			if (!researchObserved) {
				researchObserved = true;
				System.out.println("[OrcLumberProbe] the original Pillage research event fired");
			}
		}
		else if (hall.getBuildQueue()[0] == null) HumanCampaignPlaythroughProbe.immediate(viewer, orders, hall, PILLAGE.getValue());
		CUnit objectiveTree = object(globals, "udg_TreeOfLife" + GROVES[groveIndex]);
		if (researchObserved && groveIndex > lastReportedGrove && objectiveTree != null && objectiveTree.isDead()
				&& flag(globals, "udg_TreeOfLife" + GROVES[groveIndex] + "Dead") && player.getLumber() >= 3000) {
			lastReportedGrove = groveIndex;
			System.out.println("[OrcLumberProbe] grove " + GROVES[groveIndex] + " fell through ordinary attacks");
			if (groveIndex == 0) System.out.println("[OrcLumberProbe] earned the original 3000 lumber bonus through ordinary attacks");
			if (!PLAYTHROUGH) {
				finish();
				return;
			}
			if (groveIndex < GROVES.length - 1) {
				groveIndex++;
				waypoint = 0;
				escortSteps.clear();
				objectiveTree = object(globals, "udg_TreeOfLife" + GROVES[groveIndex]);
			}
		}

		final var army = owned.stream().filter(unit -> !unit.isBuilding() && !PEON.equals(unit.getTypeId())
				&& !unit.getCurrentAttacks().isEmpty()).toList();
		final CUnit expansion = PLAYTHROUGH ? owned.stream().filter(unit -> GREAT_HALL.equals(unit.getTypeId())
				&& unit.distanceSquaredNoCollision(-3200, -8000) < 950 * 950).findFirst().orElse(null) : null;
		final int expansionReserve = PLAYTHROUGH && expansion == null ? game.getUnitData().getUnitType(GREAT_HALL).getGoldCost() : 0;
		if (researchObserved && army.size() < 12) {
			for (final CUnit barracks : owned) {
				if (!"obar".equals(barracks.getTypeId().toString()) || barracks.getBuildQueue()[0] != null) continue;
				final long ranged = army.stream().filter(unit -> "ohun".equals(unit.getTypeId().toString())).count();
				final String recruit = ranged < 3 ? "ohun" : "ocat";
				if (player.getGold() < expansionReserve + game.getUnitData().getUnitType(War3ID.fromString(recruit)).getGoldCost()) break;
				if (!HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, recruit)) {
					HumanCampaignPlaythroughProbe.train(viewer, orders, barracks, "ogru");
				}
			}
		}
		final float[][] route = GROVE_ROUTES[groveIndex];
		if (PLAYTHROUGH && groveIndex >= 2 && expansion == null) {
			if (expansionBuilder == null || expansionBuilder.isDead()) {
				expansionBuilder = owned.stream().filter(unit -> PEON.equals(unit.getTypeId()))
						.min(Comparator.comparingDouble(grom::distanceSquaredNoCollision)).orElse(null);
			}
			if (expansionBuilder != null) establishExpansion(viewer, orders, expansionBuilder);
		}
		final CUnit westMine = expansion == null || expansion.isConstructing() ? null : game.getUnits().stream()
				.filter(unit -> GOLD_MINE.equals(unit.getTypeId()) && unit.distanceSquaredNoCollision(-3200, -8000) < 100 * 100)
				.findFirst().orElse(null);
		if (westMine != null && !expansionReported) {
			expansionReported = true;
			System.out.println("[OrcLumberProbe] completed the Great Hall beside the original western mine");
		}
		if (westMine != null && expansion.getBuildQueue()[0] == null && owned.stream().filter(unit -> PEON.equals(unit.getTypeId())
				&& expansion.distanceSquaredNoCollision(unit) < 1100 * 1100).count() < 5) {
			HumanCampaignPlaythroughProbe.train(viewer, orders, expansion, "opeo");
		}
		final float[] next = route[waypoint];
		final boolean securingExpansion = PLAYTHROUGH && groveIndex == 2 && westMine == null;
		if (!securingExpansion && waypoint < route.length - 1 && grom.distanceSquaredNoCollision(next[0], next[1]) < 175 * 175) waypoint++;
		final float[] destination = securingExpansion ? EXPANSION_ROUTE[EXPANSION_ROUTE.length - 1] : route[waypoint];
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player.getId())).toList();
		if (grom.getLife() >= grom.getMaximumLife() * 0.90f) gromFallbackGrove = -1;
		if (gromFallbackGrove < 0 && grom.getLife() < grom.getMaximumLife() * 0.71f) {
			gromFallbackGrove = groveIndex;
			gromFallbackStep = Math.max(0, waypoint - 2);
		}
		final int reinforcementStep = Arrays.stream(GROVE_ROUTES).limit(groveIndex).mapToInt(segment -> segment.length).sum() + waypoint;
		for (final CUnit peon : owned) {
			if (!PEON.equals(peon.getTypeId()) || peon == expansionBuilder && expansion == null
					|| peon.getCurrentBehavior() != null && peon.getCurrentBehavior().getHighlightOrderId() == OrderIds.returnresources) continue;
			if (westMine != null && expansion.distanceSquaredNoCollision(peon) < 1100 * 1100) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, peon, OrderIds.harvest, westMine);
				continue;
			}
			final boolean nearFront = peon.distanceSquaredNoCollision(destination[0], destination[1]) < 1350 * 1350;
			final CDestructable tree = game.getDestructables().stream().filter(target -> !target.isDead()
					&& target.getDestType().getTargetedAs().contains(CTargetType.TREE)
					&& (groveIndex == 0 || nearFront ? target.distanceSquaredNoCollision(destination[0], destination[1]) < 900 * 900
							: hall.distanceSquaredNoCollision(target) < 1800 * 1800))
					.min(Comparator.comparingDouble(target -> peon.distanceSquaredNoCollision(target)
							+ (groveIndex == 0 || nearFront ? target.distanceSquaredNoCollision(destination[0], destination[1]) : 0)))
					.orElse(null);
			CampaignPlaythroughProbe.targetOrder(viewer, orders, peon, OrderIds.harvest, tree);
		}
		for (final CUnit unit : army) {
			if (unit.isPaused() || unit.getCurrentBehavior() != null
					&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final var foe = enemies.stream().filter(enemy -> unit.canReach(enemy, 580))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (unit == grom && gromFallbackGrove >= 0) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, grom, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, grom, "pghe")
						|| foe != null && !grom.isInvisibleTo(game, foe.getPlayerIndex())
								&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, grom, OrderIds.windwalk)) continue;
				final float[] fallback = GROVE_ROUTES[gromFallbackGrove][gromFallbackStep];
				if (foe != null && grom.distanceSquaredNoCollision(fallback[0], fallback[1]) < 220 * 220) {
					if (gromFallbackStep > 0) gromFallbackStep--;
					else if (gromFallbackGrove > 0) {
						gromFallbackGrove--;
						gromFallbackStep = GROVE_ROUTES[gromFallbackGrove].length - 1;
					}
				}
				final var refuge = GROVE_ROUTES[gromFallbackGrove][gromFallbackStep];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, grom, refuge[0], refuge[1]);
				continue;
			}
			if (unit == grom && foe != null && enemies.stream().filter(enemy -> grom.canReach(enemy, 500)).limit(2).count() >= 2
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, grom, OrderIds.mirrorimage)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, foe)) continue;
			if (securingExpansion) {
				CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, destination[0], destination[1]);
				continue;
			}
			if (waypoint == route.length - 1 && objectiveTree != null && !objectiveTree.isDead()
					&& objectiveTree.isVisible(game, player.getId())
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, objectiveTree)) continue;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, REINFORCEMENT_ROUTE, reinforcementStep, 160, escortSteps);
		}
		if (tick >= reportTick) {
			reportTick = tick + 400;
			System.out.printf("[OrcLumberProbe] tick=%d grove=%s waypoint=%d Grom=%.0f,%.0f life=%.0f army=%d lumber=%d gold=%d%n",
					tick, GROVES[groveIndex], waypoint, grom.getX(), grom.getY(), grom.getLife(), army.size(), player.getLumber(), player.getGold());
			if (securingExpansion && expansionBuilder != null) {
				System.out.printf("[OrcLumberProbe] western builder=%.0f,%.0f route=%d site=%d order=%s%n",
						expansionBuilder.getX(), expansionBuilder.getY(),
						escortSteps.getOrDefault(expansionBuilder.getHandleId(), -1), expansionSite, expansionBuilder.getCurrentOrder());
			}
		}
		ui.getCameraManager().setTarget(grom.getX(), grom.getY());
	}

	private static void verifyHunterOpening(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit grom) {
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (HUNTER_VICTORY ? 105_000 : HUNTER_FOUNTAIN ? 62_000 : 8000), "the Hunter of Shadows stalled");
		if (HUNTER_VICTORY && hunterChaos && grom != null) {
			final CQuest quest = object(globals, "udg_QuestCenarius");
			if (quest != null && quest.isCompleted()) {
				verifyCenariusVictory(ui, globals, grom);
				return;
			}
		}
		if (grom == null || grom.isPaused() || !ui.isUserControlEnabled() || tick < orderTick) return;
		check(!grom.isDead(), "Grom died during Hunter of Shadows");
		orderTick = tick + 16;
		final int owner = grom.getPlayerIndex();
		final var player = game.getPlayer(owner);
		if (hunterStartingGold < 0) {
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var stored = cache == null ? null : cache.getStoredUnit("Orc05", "Grom");
			check(stored != null && snapshot(grom).equals(CampaignPlaythroughProbe.heroProperties(stored)),
					"Hunter of Shadows did not restore Grom from the earned cache");
			hunterStartingGold = player.getGold();
		}
		final var orders = game.getDefaultPlayerUnitOrderExecutor(owner);
		final CUnit mine = object(globals, "gg_unit_ngol_0004");
		for (final CUnit unit : game.getUnits()) {
			if (unit.getPlayerIndex() != owner || unit.isDead() || unit.isHidden()) continue;
			if (mine != null && unit != hunterLumberPeon
					&& (PEON.equals(unit.getTypeId()) || CHAOS_PEON.equals(unit.getTypeId())) && unit.canReach(mine, 1300)
					&& unit.getCurrentOrder() == null) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.harvest, mine);
			}
			if (!wardOrdered && WITCH_DOCTOR.equals(unit.getTypeId())) {
				wardOrdered = CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, unit, OrderIds.evileye,
						unit.getX() - 160, unit.getY() + 240);
			}
		}
		if (!hunterSettlementReady && player.getGold() > hunterStartingGold && wardOrdered && game.getUnits().stream()
				.anyMatch(unit -> SENTRY_WARD.equals(unit.getTypeId()) && unit.getPlayerIndex() == owner && !unit.isDead())) {
			hunterSettlementReady = true;
			System.out.println("[OrcLumberProbe] restored earned Grom and placed an original Sentry Ward in Orc05");
			if (!HUNTER_FOUNTAIN) finish();
		}
		if (hunterChaos) attackCenarius(viewer, ui, globals, grom, orders);
		else if (HUNTER_FOUNTAIN && hunterSettlementReady) advanceToFountain(viewer, ui, globals, grom, orders);
	}

	private static void advanceToFountain(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit grom, final CPlayerUnitOrderExecutor orders) {
		if (flag(globals, "udg_GromIsChaos") && "Opgh".equals(grom.getTypeId().toString())) {
			check(gromCrossedHunterGate, "Grom reached the Fountain of Blood without the original waygate");
			System.out.println("[OrcLumberProbe] the original Fountain of Blood cinematic transformed Grom");
			hunterChaos = true;
			if (!HUNTER_VICTORY) finish();
			return;
		}
		final var game = viewer.simulation;
		final var player = game.getPlayer(grom.getPlayerIndex());
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
				&& !unit.isDead() && !unit.isHidden()).toList();
		final var army = owned.stream().filter(unit -> !unit.isBuilding() && !PEON.equals(unit.getTypeId())
				&& !unit.getCurrentAttacks().isEmpty()).toList();
		final CDestructable passTree = Arrays.stream(HUNTER_PASS_TREES).map(point -> game.getDestructables().stream()
				.filter(tree -> !tree.isDead() && tree.getDestType().getTargetedAs().contains(CTargetType.TREE)
						&& tree.distanceSquaredNoCollision(point[0], point[1]) < 16 * 16).findFirst().orElse(null))
				.filter(java.util.Objects::nonNull).findFirst().orElse(null);
		if (!fountainEscortReady && gromCrossedHunterGate) {
			final float[] rally = HUNTER_FOUNTAIN_ROUTE[HUNTER_FOUNTAIN_RALLY];
			fountainEscortReady = army.stream().filter(unit -> unit != grom && hunterGateCrossings.contains(unit.getHandleId())
					&& unit.canReach(rally[0], rally[1], 550)).limit(5).count() == 5;
			if (fountainEscortReady) System.out.println("[OrcLumberProbe] gathered the ranged escort before the final Satyr camps");
		}
		final CUnit catapult = army.stream().filter(unit -> "ocat".equals(unit.getTypeId().toString())).findFirst().orElse(null);
		for (final CUnit building : owned) {
			if (!building.isBuilding() || building.getBuildQueue()[0] != null) continue;
			final String type = building.getTypeId().toString();
			if (("ogre".equals(type) || "ostr".equals(type)) && owned.stream()
					.filter(unit -> PEON.equals(unit.getTypeId())).limit(7).count() < 7) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, "opeo");
			}
			if (army.size() < 17 && "obar".equals(type)) {
				if (catapult == null && passTree != null && army.size() >= 8
						&& HumanCampaignPlaythroughProbe.train(viewer, orders, building, "ocat")) continue;
				final long ranged = army.stream().filter(unit -> "ohun".equals(unit.getTypeId().toString())).limit(7).count();
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, ranged < 7 ? "ohun" : "ogru");
			}
		}
		if (army.size() < 8 && escortSteps.isEmpty()) return;
		if (passTree != null && catapult == null) {
			if (hunterLumberPeon == null || hunterLumberPeon.isDead()) {
				hunterLumberPeon = owned.stream().filter(unit -> PEON.equals(unit.getTypeId()))
						.min(Comparator.comparingDouble(passTree::distanceSquaredNoCollision)).orElse(null);
			}
			if (hunterLumberPeon != null && (!hunterLumberPeon.canReach(passTree, 420)
					|| !CampaignPlaythroughProbe.targetOrder(viewer, orders, hunterLumberPeon, OrderIds.harvest, passTree))) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, hunterLumberPeon, HUNTER_GATE_ROUTE, HUNTER_TREE_LINE, 160, escortSteps);
			}
		}
		else if (hunterLumberPeon != null) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, hunterLumberPeon, OrderIds.harvest, object(globals, "gg_unit_ngol_0004"));
			hunterLumberPeon = null;
		}
		final CUnit gate = object(globals, "gg_unit_nwgt_0099");
		final var waygate = gate == null ? null : gate.getFirstAbilityOfType(CAbilityWayGate.class);
		final boolean gateOpen = waygate != null && waygate.isGateEnabled();
		final var nearbyEscort = army.stream().filter(unit -> unit != grom && unit.getY() > 2900 && unit.getX() >= 6300).toList();
		if (assembledHunterEscort.isEmpty() && nearbyEscort.size() >= 4) {
			assembledHunterEscort = nearbyEscort.stream().map(CUnit::getHandleId).collect(Collectors.toSet());
		}
		final boolean gatheringAtPassExit = !gateOpen && passTree == null && assembledHunterEscort.isEmpty();
		if (gatheringAtPassExit && (hunterEscortLeader == null || hunterEscortLeader.isDead() || hunterEscortLeader.getY() >= 2900)) {
			hunterEscortLeader = army.stream().filter(unit -> unit != grom && unit.getY() < 2900
					&& unit.getUnitType().getCollisionSize() <= 32)
					.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(6608, 2512))).orElse(null);
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		for (final CUnit unit : army) {
			if (unit.isPaused() || unit.getCurrentBehavior() != null
					&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (gateOpen && unit.distanceSquaredNoCollision(waygate.getDestination().x, waygate.getDestination().y) < 420 * 420
					&& hunterGateCrossings.add(unit.getHandleId()) && unit == grom) {
				gromCrossedHunterGate = true;
				System.out.println("[OrcLumberProbe] Grom crossed the original Satyr waygate");
			}
			final boolean crossed = hunterGateCrossings.contains(unit.getHandleId());
			final float[][] route = crossed ? HUNTER_FOUNTAIN_ROUTE : HUNTER_GATE_ROUTE;
			final var steps = crossed ? fountainSteps : escortSteps;
			final int currentStep = steps.getOrDefault(unit.getHandleId(), 0);
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, 610))
					.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (crossed && fountainEscortReady && unit == grom && currentStep < HUNTER_FOUNTAIN_ROUTE.length - 2) {
				if (unit.getMovementType() == MovementType.FOOT_NO_COLLISION) {
					hunterRetreatStep = -1;
					CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, route.length - 1, 160, steps);
					continue;
				}
				if (enemy != null && HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.windwalk)) continue;
			}
			if (unit == grom && (unit.getLife() < unit.getMaximumLife() * 0.78f
					|| hunterRetreatStep >= 0 && unit.getLife() < unit.getMaximumLife() * 0.92f)) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")) continue;
				if (hunterRetreatStep < 0) hunterRetreatStep = Math.max(0, currentStep - 2);
				final float[] lastRefuge = route[hunterRetreatStep];
				if (enemy != null && hunterRetreatStep > 0 && unit.distanceSquaredNoCollision(lastRefuge[0], lastRefuge[1]) < 150 * 150) {
					hunterRetreatStep--;
				}
				final float[] refuge = route[hunterRetreatStep];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, refuge[0], refuge[1]);
				continue;
			}
			if (unit == grom) hunterRetreatStep = -1;
			if (unit == grom && enemy != null
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.mirrorimage)) continue;
			final boolean supported = unit != grom || army.stream().filter(ally -> ally != grom && ally.canReach(grom, 450)).limit(3).count() == 3;
			if (supported && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (unit == catapult && passTree != null && unit.canReach(passTree, 850)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, passTree)) continue;
			if (!crossed && gateOpen && unit.distanceSquaredNoCollision(gate) < 800 * 800
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.smart, gate)) continue;
			final int arrivalRadius = !crossed && currentStep > HUNTER_ESCORT_HOLD && currentStep <= HUNTER_LANE_EXIT ? 36 : 180;
			int end = route.length - 1;
			if (crossed && !fountainEscortReady) end = HUNTER_FOUNTAIN_RALLY;
			else if (!crossed && passTree != null) end = HUNTER_TREE_LINE;
			else if (gatheringAtPassExit) end = unit == hunterEscortLeader || unit != grom && unit.getY() > 2900
					? HUNTER_PASS_EXIT : HUNTER_ESCORT_HOLD;
			else if (!crossed && !gateOpen && grom.getY() < 4000 && unit != grom && !assembledHunterEscort.isEmpty()
					&& !assembledHunterEscort.contains(unit.getHandleId())) end = HUNTER_ESCORT_HOLD;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, arrivalRadius, steps);
		}
		ui.getCameraManager().setTarget(grom.getX(), grom.getY());
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 400;
			final var order = grom.getCurrentOrder();
			final var orderTarget = order == null ? null : order.getTarget(game);
			System.out.printf("[OrcLumberProbe] Grom %.0f,%.0f life=%.0f army=%d gold=%d forest=%s gate=%s crossed=%d step=%d order=%s/%s%n",
					grom.getX(), grom.getY(), grom.getLife(), army.size(), player.getGold(), passTree != null, gateOpen, hunterGateCrossings.size(),
					(hunterGateCrossings.contains(grom.getHandleId()) ? fountainSteps : escortSteps).getOrDefault(grom.getHandleId(), -1),
					order == null ? "none" : order.getOrderId(), orderTarget == null ? "none" : Math.round(orderTarget.getX()) + "," + Math.round(orderTarget.getY()));
			if (gatheringAtPassExit && hunterEscortLeader != null) {
				System.out.printf("[OrcLumberProbe] pass escort %s %.0f,%.0f step=%d assembled=%d%n",
						hunterEscortLeader.getTypeId(), hunterEscortLeader.getX(), hunterEscortLeader.getY(),
						escortSteps.getOrDefault(hunterEscortLeader.getHandleId(), -1), army.stream().filter(unit -> unit != grom && unit.getY() > 2900).count());
			}
		}
	}

	private static void attackCenarius(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit grom, final CPlayerUnitOrderExecutor orders) {
		final CUnit cenarius = object(globals, "udg_Cenarius");
		check(cenarius != null, "the original Cenarius is missing");
		if (cenarius.isDead()) return;
		final var game = viewer.simulation;
		final var player = game.getPlayer(grom.getPlayerIndex());
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId()
				&& !unit.isDead() && !unit.isHidden()).toList();
		final var army = owned.stream().filter(unit -> unit != grom && !unit.isBuilding() && unit.getY() < 2100
				&& !PEON.equals(unit.getTypeId()) && !CHAOS_PEON.equals(unit.getTypeId())
				&& !unit.getCurrentAttacks().isEmpty()).toList();
		final long warlocks = army.stream().filter(unit -> "nchw".equals(unit.getTypeId().toString())).count();
		final long kodos = army.stream().filter(unit -> "nckb".equals(unit.getTypeId().toString())).count();
		final long catapults = army.stream().filter(unit -> "ocat".equals(unit.getTypeId().toString())).count();
		if (player.getLumber() < 250) {
			if (hunterLumberPeon == null || hunterLumberPeon.isDead()) {
				hunterLumberPeon = owned.stream().filter(unit -> CHAOS_PEON.equals(unit.getTypeId()) || PEON.equals(unit.getTypeId()))
						.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(6144, -5632))).orElse(null);
			}
			if (hunterLumberPeon != null && (hunterLumberPeon.getCurrentOrder() == null
					|| hunterLumberPeon.getCurrentOrder().getTarget(game) instanceof CUnit)) {
				final CDestructable tree = game.getDestructables().stream().filter(target -> !target.isDead()
						&& target.getDestType().getTargetedAs().contains(CTargetType.TREE)
						&& hunterLumberPeon.canReach(target, 1050))
						.min(Comparator.comparingDouble(hunterLumberPeon::distanceSquaredNoCollision)).orElse(null);
				CampaignPlaythroughProbe.targetOrder(viewer, orders, hunterLumberPeon, OrderIds.harvest, tree);
			}
		}
		if (army.size() < 6 && cenarius.getLife() > cenarius.getMaximumLife() * 0.75f) {
			cenariusExpeditionReady = false;
			cenariusMarchStep = CENARIUS_RALLY + 1;
			cenariusSteps.clear();
		}
		for (final CUnit building : owned) {
			if (building.getBuildQueue()[0] != null) continue;
			final String type = building.getTypeId().toString();
			if (("ogre".equals(type) || "ostr".equals(type) || "ofrt".equals(type)) && owned.stream()
					.filter(unit -> PEON.equals(unit.getTypeId()) || CHAOS_PEON.equals(unit.getTypeId())).limit(7).count() < 7) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, CHAOS_PEON.toString());
			}
			if (army.size() >= 25) continue;
			if ("osld".equals(type) && warlocks < 8 && (army.size() >= 6 || warlocks < 4)) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, "nchw");
			}
			if ("obea".equals(type) && army.size() >= 9 && kodos < 2 && (kodos == 0 || warlocks >= 5)) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, "nckb");
			}
			if ("obar".equals(type) && (warlocks >= 8 || player.getGold() >= 420)) {
				final String recruit = army.size() >= 7 && catapults < 2 ? "ocat" : CHAOS_GRUNT.toString();
				HumanCampaignPlaythroughProbe.train(viewer, orders, building, recruit);
			}
		}
		final float[] rally = CENARIUS_ROUTE[CENARIUS_RALLY];
		if (!cenariusExpeditionReady) {
			final long assembled = army.stream().filter(unit -> unit.canReach(rally[0], rally[1], 780)).limit(18).count();
			cenariusExpeditionReady = assembled >= 18 && warlocks >= 6 && army.stream()
					.anyMatch(unit -> CHAOS_GRUNT.equals(unit.getTypeId()) && unit.canReach(rally[0], rally[1], 640));
			if (cenariusExpeditionReady) System.out.println("[OrcLumberProbe] assembled Chaos reinforcements for the original Cenarius fight");
		}
		if (cenariusExpeditionReady && cenariusMarchStep < CENARIUS_ROUTE.length - 1) {
			final float[] destination = CENARIUS_ROUTE[cenariusMarchStep];
			if (army.stream().filter(unit -> unit.canReach(destination[0], destination[1], 310)).count() >= (army.size() + 1) / 2) {
				cenariusMarchStep++;
			}
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		for (final CUnit unit : army) {
			if (unit.isPaused() || unit.getCurrentBehavior() != null
					&& unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final String type = unit.getTypeId().toString();
			if (cenariusExpeditionReady && unit.canReach(cenarius, 870)) {
				if (CHAOS_GRUNT.equals(unit.getTypeId()) && unit.getLife() > unit.getMaximumLife() * 0.8f
						&& unit.canReach(cenarius, 220)) HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.berserk);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, cenarius)) continue;
			}
			final int range = "ocat".equals(type) ? 1120 : "nchw".equals(type) ? 660 : 550;
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, range))
					.min(Comparator.comparing((CUnit target) -> !"ocat".equals(type) || !target.isBuilding())
							.thenComparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (cenariusExpeditionReady && cenariusSteps.getOrDefault(unit.getHandleId(), 0) == CENARIUS_ROUTE.length - 1
					&& CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, cenarius.getX(), cenarius.getY())) continue;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, CENARIUS_ROUTE,
					cenariusExpeditionReady ? cenariusMarchStep : CENARIUS_RALLY, 120, cenariusSteps);
		}
		final boolean dangerNearGrom = enemies.stream().anyMatch(enemy -> grom.canReach(enemy, 730));
		if (grom.getY() > 3000 && (dangerNearGrom || grom.getLife() < grom.getMaximumLife() * 0.65f)) {
			if (grom.getMovementType() == MovementType.FOOT_NO_COLLISION
					|| !HumanCampaignPlaythroughProbe.immediate(viewer, orders, grom, OrderIds.windwalk)) {
				final float[] refuge = HUNTER_FOUNTAIN_ROUTE[chaosRefugeStep];
				if (dangerNearGrom && chaosRefugeStep > 0 && grom.distanceSquaredNoCollision(refuge[0], refuge[1]) < 180 * 180) {
					chaosRefugeStep--;
				}
				final float[] next = HUNTER_FOUNTAIN_ROUTE[chaosRefugeStep];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, grom, next[0], next[1]);
			}
		}
		army.stream().min(Comparator.comparingDouble(cenarius::distanceSquaredNoCollision))
				.ifPresent(unit -> ui.getCameraManager().setTarget(unit.getX(), unit.getY()));
		if (game.getGameTurnTick() >= reportTick) {
			reportTick = game.getGameTurnTick() + 500;
			final long chaos = army.stream().filter(unit -> CHAOS_GRUNT.equals(unit.getTypeId())).count();
			System.out.printf("[OrcLumberProbe] Cenarius %.0f,%.0f life=%.0f army=%d grunts=%d warlocks=%d gold=%d food=%d/%d assembled=%s step=%d Grom=%.0f,%.0f life=%.0f%n",
					cenarius.getX(), cenarius.getY(), cenarius.getLife(), army.size(), chaos, warlocks, player.getGold(),
					player.getFoodUsed(), player.getFoodCap(),
					cenariusExpeditionReady, cenariusMarchStep, grom.getX(), grom.getY(), grom.getLife());
		}
	}

	private static void verifyCenariusVictory(final MeleeUI ui, final GlobalScope globals, final CUnit grom) {
		final CUnit cenarius = object(globals, "udg_Cenarius");
		check(cenarius != null && cenarius.isDead() && "Opgh".equals(grom.getTypeId().toString()),
				"the original quest completed without Cenarius dying to Chaos Grom's army");
		if (carriedGrom == null) {
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var stored = cache == null ? null : cache.getStoredUnit("Orc08", "Grom");
			if (stored != null) {
				carriedGrom = CampaignPlaythroughProbe.heroProperties(stored);
				check(carriedGrom.equals(snapshot(grom)), "the native Hunter of Shadows victory did not save earned Chaos Grom");
				System.out.println("[OrcLumberProbe] defeated the original Cenarius and verified stored Chaos Grom");
			}
		}
		HumanCampaignPlaythroughProbe.continueVictory(ui);
	}

	private static void establishExpansion(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit builder) {
		final int tick = viewer.simulation.getGameTurnTick();
		if (builder.getCurrentOrder() != null && builder.getCurrentOrder().getOrderId() == GREAT_HALL.getValue()) {
			if (tick - expansionOrderTick < 650) return;
			expansionSite = (expansionSite + 1) % EXPANSION_SITES.length;
			CampaignPlaythroughProbe.pointOrder(viewer, orders, builder, -2256, -7600);
			return;
		}
		if (builder.distanceSquaredNoCollision(-2576, -8000) > 500 * 500) {
			CampaignPlaythroughProbe.followRoute(viewer, orders, builder, EXPANSION_ROUTE, EXPANSION_ROUTE.length - 1,
					180, escortSteps);
			return;
		}
		for (int offset = 0; offset < EXPANSION_SITES.length; offset++) {
			final int candidate = (expansionSite + offset) % EXPANSION_SITES.length;
			final float[] site = EXPANSION_SITES[candidate];
			if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, builder, GREAT_HALL.getValue(), site[0], site[1])) {
				expansionSite = candidate;
				expansionOrderTick = tick;
				return;
			}
		}
	}

	private static boolean flag(final GlobalScope globals, final String name) {
		return Boolean.TRUE.equals(globals.getGlobal(name).visit(BooleanJassValueVisitor.getInstance()));
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static Properties snapshot(final CUnit unit) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit));
	}

	private static void finish() {
		finished = true;
		Gdx.app.exit();
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc lumber audit: " + message);
	}
}
