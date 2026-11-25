package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;

/** Leads Illidan's original army from the NightElf06 introduction to the Skull. */
final class NightElfSkullPlaythroughProbe {
	static final boolean DEFEAT_TICHONDRIUS = Boolean.getBoolean("warsmash.nightElfDefeatTichondrius");
	private static final War3ID ILLIDAN = War3ID.fromString("Eill");
	private static final War3ID DEMON_ILLIDAN = War3ID.fromString("Eidm");
	private static final int MANA_BURN = War3ID.fromString("AEmb").getValue();
	private static final int MOON_WELL = War3ID.fromString("emow").getValue();
	private static final float[][] SKULL_ROUTE = { { 1424, -7696 }, { 1616, -7120 }, { 1776, -6544 },
			{ 1776, -5968 }, { 1776, -5392 }, { 1776, -4816 }, { 1488, -4528 }, { 912, -3952 }, { 464, -3376 },
			{ -112, -2800 }, { -688, -2224 }, { -976, -1936 }, { -1264, -1648 }, { -1552, -1360 },
			{ -1584, -1072 }, { -1584, -496 }, { -1584, 80 }, { -1712, 368 }, { -2000, 656 }, { -2576, 816 },
			{ -3152, 816 }, { -3440, 848 }, { -3728, 1136 }, { -4016, 1424 }, { -4176, 1456 },
			{ -4464, 1712 }, { -4752, 2000 }, { -5040, 2288 }, { -5328, 2576 } };
	private static final float[][] TICHONDRIUS_ROUTE = { { -5104, 2576 }, { -4816, 2480 }, { -4528, 2192 },
			{ -4240, 1904 }, { -3952, 1680 }, { -3376, 1680 }, { -2800, 1616 }, { -2224, 1616 },
			{ -1936, 1872 }, { -1648, 2096 }, { -1072, 2096 }, { -496, 2096 }, { 80, 2096 }, { 656, 2096 },
			{ 944, 2160 }, { 1232, 2288 }, { 1520, 2416 }, { 2096, 2416 }, { 2672, 2352 }, { 2960, 2064 },
			{ 3248, 1776 }, { 3536, 1680 }, { 4112, 1680 }, { 4688, 1552 }, { 5264, 1488 }, { 5840, 1520 },
			{ 6128, 1808 }, { 6416, 2096 }, { 6704, 2384 } };
	private static final float[][] TICHONDRIUS_REINFORCEMENTS = { { 3568, -7600 }, { 2144, -6480 },
			{ 1776, -4816 }, { 2288, -4528 }, { 2576, -4240 }, { 2800, -3952 }, { 3184, -3376 },
			{ 3472, -3088 }, { 3568, -2800 }, { 3632, -2224 }, { 3632, -1648 }, { 3760, -1072 },
			{ 4336, -496 }, { 4624, -272 }, { 5200, 240 }, { 5424, 336 }, { 5488, 912 }, { 5584, 1200 },
			{ 5872, 1488 }, { 6160, 1776 }, { 6448, 2064 }, { 6736, 2352 } };
	private static final Map<Integer, Integer> routeSteps = new HashMap<>();
	private static final Map<Integer, Integer> reinforcementSteps = new HashMap<>();
	private static final Set<Integer> assignedWisps = new HashSet<>();
	private static boolean openingVerified;
	private static boolean finished;
	private static boolean expeditionReady;
	private static boolean recoveringAtBase;
	private static boolean skullAcquired;
	private static boolean victoryRecorded;
	private static boolean interludeVerified;
	private static int orderTick;
	private static int reportTick;
	private static int refugeStep = -1;

	private NightElfSkullPlaythroughProbe() {
	}

	static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals) {
		if (finished || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (DEFEAT_TICHONDRIUS ? 89_000 : 54_000) && !"Defeat".equals(ui.getScoreDialogTitle()),
				"the approach stalled or ended in defeat");
		final CUnit illidan = object(globals, "udg_Illidan");
		final CUnit gate = object(globals, "gg_unit_ndmg_0068");
		final CQuest skull = object(globals, "udg_QuestSkullofGuldan");
		final CQuest hunt = object(globals, "udg_QuestKillTicho");
		final CUnit tichondrius = object(globals, "udg_Tichondrius");
		if (DEFEAT_TICHONDRIUS && hunt != null && hunt.isCompleted()) {
			if (!victoryRecorded) {
				check(skullAcquired && tichondrius != null && tichondrius.isDead(), "Tichondrius survived native victory");
				victoryRecorded = true;
				System.out.println("[NightElfSkullProbe] defeated the original Tichondrius and completed NightElf06");
			}
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		if (illidan == null || gate == null || skull == null || !ui.isUserControlEnabled()
				|| illidan.isPaused() || !skull.isDiscovered() || tick < orderTick) return;
		orderTick = tick + 14;
		if (!skullAcquired && skull.isCompleted()) {
			final CQuestItem demonGate = object(globals, "udg_QuestSkullReqDemongate");
			if (hunt == null || !hunt.isDiscovered()) return;
			check(openingVerified && gate.isDead() && demonGate != null && demonGate.isCompleted(), "the Demon Gate objective was bypassed");
			check(DEMON_ILLIDAN.equals(illidan.getTypeId()) && !illidan.isDead(), "the Skull did not transform Illidan");
			System.out.println("[NightElfSkullProbe] destroyed the original Demon Gate and completed Illidan's Skull transformation");
			if (!DEFEAT_TICHONDRIUS) {
				finished = true;
				Gdx.app.exit();
				return;
			}
			skullAcquired = true;
			refugeStep = -1;
			routeSteps.clear();
		}
		if (!openingVerified) {
			check(ILLIDAN.equals(illidan.getTypeId()), "the original introduction did not place Illidan in control");
			openingVerified = true;
			System.out.println("[NightElfSkullProbe] watched the original introduction and discovered the Skull of Gul'dan quest");
		}
		check(!illidan.isDead(), skullAcquired ? "Illidan fell during the battle with Tichondrius" : "Illidan fell before reaching the Skull");
		final var player = game.getPlayer(illidan.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		final var owned = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player.getId() && !unit.isDead()).toList();
		final var force = owned.stream().filter(unit -> !unit.isHidden() && !unit.isBuilding() && !unit.getCurrentAttacks().isEmpty()).toList();
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player.getId())).toList();
		final long archers = force.stream().filter(unit -> "earc".equals(unit.getTypeId().toString())).count();
		final long dryads = force.stream().filter(unit -> "edry".equals(unit.getTypeId().toString())).count();
		prepareExpedition(viewer, orders, owned, archers, dryads, player.getFoodCap() - player.getFoodUsed());
		if (!expeditionReady && (force.size() < 12 || archers < 6 || dryads < 2) && tick < 12_000) {
			if (tick >= reportTick) {
				reportTick = tick + 420;
				System.out.printf("[NightElfSkullProbe] building the expedition troops=%d Archers=%d Dryads=%d gold=%d lumber=%d food=%d/%d%n",
						force.size(), archers, dryads, player.getGold(), player.getLumber(), player.getFoodUsed(), player.getFoodCap());
			}
			return;
		}
		expeditionReady = true;
		if (recoveringAtBase) {
			if (illidan.getCurrentBehavior() != null && illidan.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
			if (illidan.getLife() < illidan.getMaximumLife() * 0.91f) {
				final CUnit well = owned.stream().filter(unit -> "emow".equals(unit.getTypeId().toString()) && !unit.isConstructing())
						.min(Comparator.comparingDouble(illidan::distanceSquaredNoCollision)).orElse(null);
				if (well != null && !CampaignPlaythroughProbe.targetOrder(viewer, orders, well, OrderIds.replenish, illidan)) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, illidan, well.getX() - 160, well.getY());
				}
				return;
			}
			recoveringAtBase = false;
			refugeStep = -1;
			routeSteps.clear();
		}
		if (illidan.getHeroData().getSkillPoints() > 0) HumanCampaignPlaythroughProbe.immediate(viewer, orders, illidan, MANA_BURN);
		final float[][] mainRoute = skullAcquired ? TICHONDRIUS_ROUTE : SKULL_ROUTE;
		for (final CUnit unit : force) {
			if (unit.isPaused() || unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if ("edoc".equals(unit.getTypeId().toString()) && illidan.getLife() < illidan.getMaximumLife() * 0.78f
					&& unit.canReach(illidan, 450) && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.rejuvination, illidan)) continue;
			final boolean fightingTichondrius = skullAcquired && tichondrius != null && !tichondrius.isDead()
					&& unit.canReach(tichondrius, 650);
			if (unit == illidan && unit.getLife() < unit.getMaximumLife() * (refugeStep >= 0 ? 0.85f : 0.58f)) {
				if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")
						|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")) continue;
				if (unit.getLife() < unit.getMaximumLife() * 0.44f && !enemies.isEmpty()
						&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "stwp")) {
					recoveringAtBase = true;
					System.out.println("[NightElfSkullProbe] Illidan used his original Town Portal to recover at the base");
					return;
				}
				if (fightingTichondrius && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, tichondrius)) continue;
				if (refugeStep < 0) refugeStep = Math.max(0, routeSteps.getOrDefault(unit.getHandleId(), 0) - 3);
				final float[] refuge = mainRoute[refugeStep];
				if (refugeStep > 0 && unit.distanceSquaredNoCollision(refuge[0], refuge[1]) < 155 * 155) refugeStep--;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, mainRoute[refugeStep][0], mainRoute[refugeStep][1]);
				continue;
			}
			if (unit == illidan) refugeStep = -1;
			if (fightingTichondrius && unit != illidan) {
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, tichondrius)) continue;
			}
			final CUnit enemy = enemies.stream().filter(target -> unit.canReach(target, 570))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (unit == illidan && enemy != null && enemy.getMana() > 30
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.manaburn, enemy)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			final CUnit objective = skullAcquired ? tichondrius : gate;
			if (objective != null && !objective.isDead() && unit.canReach(objective, 1030)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, objective)) continue;
			final boolean reinforce = skullAcquired && unit != illidan
					&& (reinforcementSteps.containsKey(unit.getHandleId()) || unit.getX() > 700 && unit.getY() < -2000);
			final float[][] route = reinforce ? TICHONDRIUS_REINFORCEMENTS : mainRoute;
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, route.length - 1, 155,
					reinforce ? reinforcementSteps : routeSteps);
		}
		ui.getCameraManager().setTarget(illidan.getX(), illidan.getY());
		if (tick >= reportTick) {
			reportTick = tick + 420;
			System.out.printf("[NightElfSkullProbe] Illidan %.0f,%.0f life=%.0f step=%d troops=%d objective=%.0f%n",
					illidan.getX(), illidan.getY(), illidan.getLife(), routeSteps.getOrDefault(illidan.getHandleId(), 0),
					force.size(), skullAcquired && tichondrius != null ? tichondrius.getLife() : gate.getLife());
		}
	}

	static void verifyInterlude(final MeleeUI ui) {
		if (finished) return;
		if (!interludeVerified) {
			check(victoryRecorded, "the interlude loaded without Illidan's original victory");
			interludeVerified = true;
			System.out.println("[NightElfSkullProbe] entered the original NightElf06 interlude");
		}
		if (NightElfFinaleOpeningProbe.ENABLED) {
			HumanCampaignPlaythroughProbe.continueVictory(ui);
			return;
		}
		finished = true;
		Gdx.app.exit();
	}

	private static void prepareExpedition(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> owned, final long archers, final long dryads, final int freeFood) {
		final var wisps = owned.stream().filter(unit -> "ewsp".equals(unit.getTypeId().toString())).toList();
		final CUnit mine = owned.stream().filter(unit -> unit.getFirstAbilityOfType(CAbilityEntangledMine.class) != null)
				.findFirst().orElse(null);
		for (int index = 0; index < wisps.size() - 1; index++) {
			final CUnit wisp = wisps.get(index);
			if (wisp.isHidden()) continue;
			if (index < 5 && CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.smart, mine)) continue;
			if (!assignedWisps.add(wisp.getHandleId())) continue;
			viewer.simulation.getDestructables().stream().filter(tree -> !tree.isDead() && wisp.canReach(tree, 650))
					.sorted(Comparator.comparingDouble(wisp::distanceSquaredNoCollision))
					.anyMatch(tree -> CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.wispharvest, tree));
		}
		for (final CUnit unit : owned) {
			if (unit.getBuildQueue()[0] != null || unit.isConstructingOrUpgrading()) continue;
			if (archers < 8 && "eaom".equals(unit.getTypeId().toString())) HumanCampaignPlaythroughProbe.train(viewer, orders, unit, "earc");
			if (dryads < 5 && "eaoe".equals(unit.getTypeId().toString())) HumanCampaignPlaythroughProbe.train(viewer, orders, unit, "edry");
		}
		if (freeFood >= 4 || wisps.isEmpty() || owned.stream().anyMatch(unit -> unit.isConstructing() && unit.getTypeId().getValue() == MOON_WELL)) return;
		final CUnit builder = wisps.get(wisps.size() - 1);
		for (int x = 2240; x <= 3840; x += 160) {
			if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, builder, MOON_WELL, x, -7440)) return;
		}
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf Skull audit: " + message);
	}
}
