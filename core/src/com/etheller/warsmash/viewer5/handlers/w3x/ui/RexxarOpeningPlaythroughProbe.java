package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellItems;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapFlag;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionSaveStore;

/** Delivers Mogrin's report through the original Founding of Durotar opening. */
public final class RexxarOpeningPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.rexxarOpeningAudit");
	private static final boolean ACCEPT_QUESTS = Boolean.getBoolean("warsmash.rexxarAcceptOpeningQuests");
	private static final boolean ENTER_TUNNELS = Boolean.getBoolean("warsmash.rexxarEnterGoblinTunnels");
	private static final boolean RETURN_FROM_TUNNELS = Boolean.getBoolean("warsmash.rexxarReturnFromGoblinTunnels");
	private static final boolean BACK_UP_ZONES = Boolean.getBoolean("warsmash.rexxarBackUpZoneSaves");
	private static final boolean COLLAPSE_FIRST_TUNNEL = Boolean.getBoolean("warsmash.rexxarCollapseFirstTunnel");
	private static final boolean COLLAPSE_ALL_TUNNELS = Boolean.getBoolean("warsmash.rexxarCollapseAllTunnels");
	private static final boolean COLLECT_THUNDER_HERBS = Boolean.getBoolean("warsmash.rexxarCollectThunderHerbs");
	private static final boolean RETURN_THUNDER_HERBS = Boolean.getBoolean("warsmash.rexxarReturnThunderHerbs");
	private static final boolean HARPY_THREAT = Boolean.getBoolean("warsmash.rexxarHarpyThreat");
	private static final War3ID SHIMMERWEED = War3ID.fromString("shwd");
	private static final int ROKHAN_HEALING_WAVE = War3ID.fromString("ANhw").getValue();
	private static final int ROKHAN_HEX = War3ID.fromString("ANhx").getValue();
	private static final int ROKHAN_SERPENT_WARD = War3ID.fromString("Arsw").getValue();
	private static final int REXXAR_QUILBEAST = War3ID.fromString("Arsq").getValue();
	private static final float[][] ORGRIMMAR_ROAD = { { -3088, -6928 }, { -3376, -7056 }, { -3952, -7056 },
			{ -4240, -7056 }, { -4496, -6832 }, { -4496, -6544 }, { -4496, -5968 }, { -4496, -5680 },
			{ -4240, -5392 }, { -3952, -5232 }, { -3728, -4944 }, { -3504, -4656 }, { -3216, -4368 },
			{ -2928, -4080 }, { -2640, -3792 }, { -2352, -3504 }, { -2128, -3216 }, { -2032, -2928 },
			{ -1872, -2640 }, { -1744, -2352 }, { -1808, -2128 }, { -1872, -2096 } };
	private static final float[][] GAZLOWE = { { -1808, -2960 }, { -1520, -2672 }, { -1232, -2512 }, { -656, -2512 },
			{ -80, -2512 }, { 208, -2800 }, { 304, -3088 }, { 368, -3376 }, { 384, -3456 } };
	private static final float[][] DREK_THAR = { { 400, -3280 }, { 400, -2992 }, { 112, -2768 }, { -176, -2576 },
			{ -464, -2576 }, { -752, -2416 }, { -1040, -2128 }, { -1040, -1840 }, { -1296, -1552 },
			{ -1584, -1264 }, { -1744, -976 }, { -1744, -400 }, { -1744, 176 }, { -1776, 464 }, { -1792, 544 } };
	private static final float[][] NAZGREL = { { -1616, 432 }, { -1328, 176 }, { -1040, 176 }, { -464, 176 },
			{ -176, 176 }, { 112, -16 }, { 400, -208 }, { 624, -144 }, { 640, -64 } };
	private static final float[][][] QUEST_ROUTES = { GAZLOWE, DREK_THAR, NAZGREL };
	private static final float[][] THUNDER_RIDGE = { { 560, -176 }, { 144, -112 }, { -176, 304 }, { -432, 720 },
			{ -432, 1136 }, { -432, 1552 }, { -432, 1968 }, { -432, 2384 }, { -848, 2800 }, { -1008, 3216 },
			{ -1008, 3632 }, { -1008, 4048 }, { -1008, 4464 }, { -1008, 4880 }, { -1008, 5296 }, { -1008, 5712 },
			{ -1008, 6128 }, { -1072, 6544 }, { -1072, 6960 }, { -1072, 7376 }, { -1104, 7792 }, { -1520, 8208 },
			{ -1776, 8624 }, { -1776, 9040 }, { -2192, 9456 }, { -2416, 9872 }, { -2800, 10288 }, { -2864, 10704 },
			{ -3216, 11120 }, { -3600, 11536 }, { -4016, 11952 }, { -4432, 12368 }, { -4448, 12448 } };
	private static final float[][] THUNDER_RIDGE_EXIT = { { -2112, -3264 }, { -2560, -3648 }, { -3168, -4320 },
			{ -3520, -4720 }, { -3968, -4688 }, { -4400, -4352 } };
	private static final float[][] THUNDER_RIDGE_TURN_IN = thunderRidgeTurnInRoute();
	private static final float[][] HARPY_ROAD = { { 656, -272 }, { 656, -688 }, { 880, -1104 }, { 1296, -1520 },
			{ 1712, -1616 }, { 2128, -1616 }, { 2544, -1552 }, { 2960, -1392 }, { 3376, -976 }, { 3792, -784 },
			{ 4208, -400 }, { 4624, 16 }, { 4976, 432 }, { 5360, 848 }, { 5424, 1264 }, { 5488, 1680 },
			{ 5488, 2096 }, { 5456, 2512 }, { 5232, 2928 }, { 5232, 3344 }, { 5360, 3760 }, { 5360, 4176 },
			{ 5360, 4592 }, { 5360, 5008 }, { 5360, 5424 }, { 5584, 5840 }, { 5680, 6256 }, { 5456, 6640 },
			{ 5040, 6768 }, { 4624, 6768 }, { 4208, 6576 }, { 3792, 6576 }, { 3376, 6576 }, { 2960, 6480 }, { 2544, 6064 } };
	private static final float[][] HARPY_RETURN = reversedRoute(HARPY_ROAD, HARPY_ROAD.length - 1);
	private static final float[][] ARMORY_ROAD = { { 656, -80 }, { 656, -496 }, { 656, -912 }, { 656, -1328 },
			{ 560, -1744 }, { 144, -1968 }, { -272, -2384 }, { -528, -2800 }, { -944, -3216 } };
	private static final float[][] GOBLIN_TUNNELS = { { 656, -464 }, { 656, -752 }, { 816, -1040 }, { 1392, -1616 },
			{ 1680, -1616 }, { 1968, -1680 }, { 2544, -2256 }, { 2832, -2544 }, { 3056, -2832 }, { 3056, -3120 },
			{ 3600, -3696 }, { 3760, -3984 }, { 4048, -4272 }, { 4144, -4560 }, { 4240, -4848 },
			{ 4528, -5136 }, { 4816, -5328 }, { 5104, -5328 }, { 5392, -5456 }, { 5680, -5456 },
			{ 5776, -5168 }, { 5808, -4880 }, { 5904, -4592 }, { 5936, -4304 } };
	private static final float[][] SAPPER_RENDEZVOUS = { { -2736, -1136 }, { -2448, -1136 }, { -2100, -1136 } };
	private static final float[][] FIRST_SUPPORT_COLUMN = { { -1296, -1008 }, { -1296, -720 }, { -1328, -432 },
			{ -1328, -144 }, { -1328, 144 }, { -1488, 432 }, { -1520, 720 }, { -1744, 1008 }, { -2032, 1296 },
			{ -2160, 1584 }, { -2160, 1872 }, { -2384, 2160 }, { -2672, 2448 }, { -2672, 2544 } };
	private static final float[][] SECOND_SUPPORT_COLUMN = { { -1296, -912 }, { -1296, -624 }, { -1328, -336 },
			{ -1328, -48 }, { -1328, 240 }, { -1520, 528 }, { -1552, 816 }, { -1840, 1104 }, { -2032, 1392 },
			{ -2160, 1680 }, { -1968, 1968 }, { -1680, 1968 }, { -1392, 1968 }, { -1104, 1968 }, { -816, 1968 },
			{ -528, 1968 }, { -240, 1968 }, { 48, 1712 }, { 336, 1552 }, { 624, 1424 }, { 912, 1424 },
			{ 1200, 1424 }, { 1488, 1424 }, { 1776, 1648 }, { 2064, 1936 }, { 2352, 2224 }, { 2640, 2512 },
			{ 2672, 2512 } };
	private static final float[][] THIRD_SUPPORT_COLUMN = { { -1296, -1136 }, { -1296, -848 }, { -1296, -560 },
			{ -1328, -272 }, { -1296, 16 }, { -1008, 112 }, { -720, 176 }, { -432, 176 }, { -144, 176 },
			{ 144, -48 }, { 432, -336 }, { 720, -432 }, { 1008, -720 }, { 1296, -944 }, { 1584, -1232 },
			{ 1872, -1392 }, { 2160, -1552 }, { 2448, -1840 }, { 2736, -2128 }, { 2832, -2128 } };
	private static final float[][][] SUPPORT_ROUTES = { FIRST_SUPPORT_COLUMN, THIRD_SUPPORT_COLUMN, SECOND_SUPPORT_COLUMN };
	private static final String[][] SUPPORT_COLUMNS = { { "gg_dest_BTsc_0462", "gg_dest_BTsc_0453" },
			{ "gg_dest_BTsc_0463", "gg_dest_BTsc_0461" }, { "gg_dest_BTsc_0460", "gg_dest_BTsc_0073" } };
	private static final String[] OPENING_QUESTS = { "udg_QuestSealTunnel", "udg_QuestThunderHerb", "udg_QuestHarpyThreat" };
	private static final String[] QUEST_GIVERS = { "Gazlowe", "Drek'Thar", "Nazgrel" };
	private static final Map<Integer, Integer> waypoints = new HashMap<>();
	private static CItem originalReport;
	private static boolean mishaObserved;
	private static boolean reportDelivered;
	private static boolean tunnelsEntered;
	private static boolean thunderRidgeEntered;
	private static boolean thunderHerbsGathered;
	private static boolean returnedFromThunderRidge;
	private static boolean bloodfeatherDefeated;
	private static boolean nazgrelTurnIn;
	private static boolean harpyPartyReviving;
	private static final Set<CItem> obstructedHerbs = new HashSet<>();
	private static CItem watchedHerb;
	private static double closestHerbDistance;
	private static int herbProgressTick;
	private static boolean thunderPartyReviving;
	private static boolean sappersRescued;
	private static boolean tunnelPartyReady;
	private static boolean rokhanRecovering;
	private static boolean rexxarRecovering;
	private static int verifiedTunnels;
	private static CUnit sapper;
	private static boolean finished;
	private static int nextQuest;
	private static int orderTick;
	private static int reportTick;
	private static int nextReturnReport;
	private static int tunnelOrderTick;
	private static int nextTunnelReport;
	private static int rexxarRefuge = -1;
	private static int rokhanRefuge = -1;
	private static int tunnelPartyRefuge = -1;
	private static int resurrectionDeadline;
	private static boolean rexxarAwaitingRevival;
	private static boolean rokhanAwaitingRevival;

	private RexxarOpeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		if (ENTER_TUNNELS && map.endsWith("/orcx01_02.w3x")) {
			verifyTunnelArrival(viewer, ui, globals);
			return;
		}
		if (COLLECT_THUNDER_HERBS && map.endsWith("/orcx01_05.w3x")) {
			collectThunderHerbs(viewer, ui, globals);
			return;
		}
		check(map.endsWith("/orcx01.w3x"), "unexpected opening map " + map);
		if (RETURN_THUNDER_HERBS && thunderHerbsGathered) {
			returnThunderHerbs(viewer, ui, globals);
			return;
		}
		if (tunnelsEntered) {
			verifyDurotarReturn(viewer, ui, globals);
			return;
		}
		final int tick = game.getGameTurnTick();
		check(tick < (HARPY_THREAT ? 125_000 : COLLECT_THUNDER_HERBS ? 95_000 : ENTER_TUNNELS ? 65_000 : ACCEPT_QUESTS ? 52_000 : 36_000),
				"the original introduction or journey stalled");
		if (!ui.isUserControlEnabled() || game.isGamePaused() || tick < orderTick) return;
		orderTick = tick + 14;
		final CUnit rexxar = object(globals, "udg_Rexxar");
		final CQuest dyingWish = object(globals, "udg_QuestDyingWish");
		if (rexxar == null || rexxar.isPaused() || dyingWish == null || !dyingWish.isDiscovered()) return;
		check((!rexxar.isDead() || (COLLECT_THUNDER_HERBS || HARPY_THREAT) && reportDelivered && nextQuest == OPENING_QUESTS.length)
				&& !"Defeat".equals(ui.getScoreDialogTitle()), "Rexxar was defeated on the way to Orgrimmar");
		if (originalReport == null) {
			originalReport = object(globals, "udg_MogrinsReport");
			check(originalReport != null && originalReport.getContainedUnit() == rexxar
					&& "mort".equals(originalReport.getTypeId().toString()), "the native introduction did not grant Mogrin's Report");
			System.out.println("[RexxarOpeningProbe] watched the original introduction and accepted Mogrin's Report");
		}
		if (dyingWish.isCompleted() && !reportDelivered) {
			final CQuest earnKeep = object(globals, "udg_QuestEarnKeep");
			final CUnit rokhan = object(globals, "udg_Rokhan");
			if (earnKeep == null || !earnKeep.isDiscovered() || rokhan == null || rokhan.isHidden()) return;
			check(rokhan.getPlayerIndex() == rexxar.getPlayerIndex() && !rokhan.isDead(), "Rokhan did not join the original party");
			check(originalReport.getContainedUnit() == null, "the original audience did not turn in Mogrin's Report");
			check(mishaObserved, "the original opening did not summon Misha");
			System.out.println("[RexxarOpeningProbe] delivered Mogrin's Report to Thrall and recruited Rokhan");
			reportDelivered = true;
			waypoints.clear();
			if (!ACCEPT_QUESTS) {
				finish();
				return;
			}
		}
		final var player = game.getPlayer(rexxar.getPlayerIndex());
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player.getId());
		if (reportDelivered) {
			if (nextQuest < OPENING_QUESTS.length) {
				acceptQuests(viewer, ui, globals, rexxar, orders);
			}
			else if (COLLECT_THUNDER_HERBS) {
				travelToThunderRidge(viewer, ui, globals, rexxar, orders);
			}
			else if (HARPY_THREAT) {
				completeHarpyThreat(viewer, ui, globals, rexxar, orders);
			}
			else {
				advance(viewer, rexxar, rexxar, orders, GOBLIN_TUNNELS);
				final CUnit rokhan = object(globals, "udg_Rokhan");
				if (rokhan != null && !rokhan.isDead() && !rokhan.isHidden()) advance(viewer, rokhan, rexxar, orders, GOBLIN_TUNNELS);
				ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
			}
			return;
		}
		final CUnit misha = object(globals, "udg_Misha");
		if (misha != null && game.containsUnit(misha) && !misha.isDead() && !misha.isHidden()
				&& misha.getPlayerIndex() == player.getId()) {
			if (!mishaObserved) System.out.println("[RexxarOpeningProbe] Misha joined the original party");
			mishaObserved = true;
			advance(viewer, misha, rexxar, orders, ORGRIMMAR_ROAD);
		}
		else if (HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.spiritwolf)) {
			return;
		}
		advance(viewer, rexxar, rexxar, orders, ORGRIMMAR_ROAD);
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
		if (tick >= reportTick) {
			reportTick = tick + 400;
			System.out.printf("[RexxarOpeningProbe] Orgrimmar road %.0f,%.0f life=%.0f step=%d Misha=%s%n",
					rexxar.getX(), rexxar.getY(), rexxar.getLife(), waypoints.getOrDefault(rexxar.getHandleId(), 0),
					misha == null ? "absent" : Math.round(misha.getX()) + "," + Math.round(misha.getY()));
		}
	}

	private static void acceptQuests(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit rexxar, final CPlayerUnitOrderExecutor orders) {
		final CQuest quest = object(globals, OPENING_QUESTS[nextQuest]);
		if (quest != null && quest.isDiscovered()) {
			System.out.println("[RexxarOpeningProbe] accepted the original assignment from " + QUEST_GIVERS[nextQuest]);
			nextQuest++;
			waypoints.clear();
			if (nextQuest == OPENING_QUESTS.length) {
				if (!ENTER_TUNNELS && !COLLECT_THUNDER_HERBS && !HARPY_THREAT) finish();
				return;
			}
		}
		final float[][] route = QUEST_ROUTES[nextQuest];
		CampaignPlaythroughProbe.followRoute(viewer, orders, rexxar, route, route.length - 1, 145, waypoints);
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
	}

	private static void travelToThunderRidge(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit rexxar, final CPlayerUnitOrderExecutor orders) {
		final CUnit rokhan = object(globals, "udg_Rokhan");
		if (rokhan == null || rokhan.isHidden()) return;
		if (rexxar.isDead() || rokhan.isDead()) {
			thunderPartyReviving = true;
			return;
		}
		if (thunderPartyReviving) {
			thunderPartyReviving = false;
			waypoints.clear();
			System.out.println("[RexxarOpeningProbe] resumed the Thunder Ridge journey after native resurrection");
		}
		final int lastStep = THUNDER_RIDGE.length - 1;
		final int step = waypoints.getOrDefault(rexxar.getHandleId(), 0);
		final int supportStep = waypoints.getOrDefault(rokhan.getHandleId(), 0);
		final int advanceStep = Math.min(lastStep, supportStep + 2);
		if (rexxar.getHeroData().getSkillPoints() > 0 && HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, REXXAR_QUILBEAST)) {
			return;
		}
		final CUnit misha = object(globals, "udg_Misha");
		if (misha == null || misha.isDead() || misha.isHidden()) {
			HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.spiritwolf);
		}
		else advance(viewer, misha, rexxar, orders, THUNDER_RIDGE, Math.min(lastStep, advanceStep + 1));
		if (rokhan.getHeroData().getSkillPoints() > 0 && HumanCampaignPlaythroughProbe.immediate(viewer, orders, rokhan, ROKHAN_HEALING_WAVE)) return;
		final CUnit enemy = nearestTunnelThreat(viewer, rexxar, 580);
		if (enemy != null && rexxar.getMana() >= 100
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.summonquillbeast)) return;
		if (rexxar.getLife() < rexxar.getMaximumLife() * 0.62f && enemy != null) {
			final float[] refuge = THUNDER_RIDGE[Math.max(0, step - 2)];
			CampaignPlaythroughProbe.pointOrder(viewer, orders, rexxar, refuge[0], refuge[1]);
		}
		else advance(viewer, rexxar, rexxar, orders, THUNDER_RIDGE, advanceStep);
		protectRokhan(viewer, orders, rexxar, rokhan, THUNDER_RIDGE, Math.min(lastStep, step + 1));
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
		final int tick = viewer.simulation.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 450;
			System.out.printf("[RexxarOpeningProbe] Thunder Ridge approach Rexxar step=%d at %s life=%.0f Rokhan step=%d at %s%n",
					step, heroPosition(rexxar), rexxar.getLife(), supportStep, heroPosition(rokhan));
		}
	}

	private static void collectThunderHerbs(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals) {
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < 95_000 && !"Defeat".equals(ui.getScoreDialogTitle()), "the original Thunder Ridge quest stalled or ended in defeat");
		if (!thunderRidgeEntered) {
			orderTick = 0;
			reportTick = 0;
		}
		final CUnit rexxar = object(globals, "udg_Rexxar");
		final CUnit rokhan = object(globals, "udg_Rokhan");
		if (rexxar == null || rokhan == null || game.isGamePaused() || !ui.isUserControlEnabled()
				|| rexxar.isPaused() || tick < orderTick) return;
		orderTick = tick + 16;
		if (!thunderRidgeEntered) {
			verifyRestoredParty(globals, rexxar, rokhan);
			check(validArchive("OrcX01"), "the original Thunder Ridge transition did not archive Durotar");
			thunderRidgeEntered = true;
			System.out.println("[RexxarOpeningProbe] entered original Thunder Ridge and restored both heroes");
		}
		if (rexxar.isDead() || rokhan.isDead()) return;
		final CQuestItem herbs = object(globals, "udg_QuestThunderHerbReq1");
		final int gathered = globals.getGlobal("udg_ShimmerweedObtained").visit(IntegerJassValueVisitor.getInstance());
		if (gathered >= 6) {
			if (herbs != null && herbs.isCompleted()) {
				if (!thunderHerbsGathered) {
					System.out.println("[RexxarOpeningProbe] gathered six original Shimmerweed and completed the native collection requirement");
					thunderHerbsGathered = true;
					waypoints.clear();
				}
				if (RETURN_THUNDER_HERBS) {
					final var orders = game.getDefaultPlayerUnitOrderExecutor(rexxar.getPlayerIndex());
					advance(viewer, rexxar, rexxar, orders, THUNDER_RIDGE_EXIT);
					advance(viewer, rokhan, rexxar, orders, THUNDER_RIDGE_EXIT);
					ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
				}
				else finish();
			}
			return;
		}
		final int player = rexxar.getPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final CUnit collector = hasInventorySpace(rexxar) ? rexxar : rokhan;
		final CItem nextHerb = game.getItems().stream().filter(item -> SHIMMERWEED.equals(item.getTypeId())
				&& !item.isDead() && !item.isHidden() && item.getContainedUnit() == null && !obstructedHerbs.contains(item))
				.min(Comparator.comparingDouble(rexxar::distanceSquaredNoCollision)).orElse(null);
		check(nextHerb != null, "the native Shimmerweed supply was exhausted before the quest counted six");
		final CUnit misha = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& unit.getPlayerIndex() == player && "ngzc".equals(unit.getTypeId().toString())).findFirst().orElse(null);
		final CUnit enemy = nearestTunnelThreat(viewer, rexxar, 560);
		final double herbDistance = Math.sqrt(collector.distanceSquaredNoCollision(nextHerb));
		if (watchedHerb != nextHerb || enemy != null || herbDistance < closestHerbDistance - 32) {
			watchedHerb = nextHerb;
			closestHerbDistance = herbDistance;
			herbProgressTick = tick;
		}
		else if (tick - herbProgressTick >= 900) {
			final var behavior = collector.getCurrentBehavior();
			System.out.printf("[RexxarOpeningProbe] trying another Shimmerweed after the path to %.0f,%.0f stalled at %s (%s, speed=%d)%n",
					nextHerb.getX(), nextHerb.getY(), heroPosition(collector), behavior == null ? "idle" : behavior.getClass().getSimpleName(),
					collector.getMovementSpeed(game));
			obstructedHerbs.add(nextHerb);
			return;
		}
		if (misha == null && HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.spiritwolf)) return;
		if (misha != null && !CampaignPlaythroughProbe.targetOrder(viewer, orders, misha, OrderIds.attack, enemy)) {
			leaveHerbApproachClear(viewer, orders, misha, collector, nextHerb, -1);
		}
		if (enemy == null) CampaignPlaythroughProbe.targetOrder(viewer, orders, collector, OrderIds.smart, nextHerb);
		else CampaignPlaythroughProbe.targetOrder(viewer, orders, rexxar, OrderIds.attack, enemy);
		final CUnit hurt = rexxar.getLife() / rexxar.getMaximumLife() < rokhan.getLife() / rokhan.getMaximumLife() ? rexxar : rokhan;
		if (hurt.getLife() < hurt.getMaximumLife() * 0.80f
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.healingwave, hurt)) return;
		if (collector != rokhan || enemy != null) {
			if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.attack, enemy)) {
				leaveHerbApproachClear(viewer, orders, rokhan, collector, nextHerb, 1);
			}
		}
		else leaveHerbApproachClear(viewer, orders, rexxar, collector, nextHerb, 1);
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
		if (tick >= reportTick) {
			reportTick = tick + 420;
			System.out.printf("[RexxarOpeningProbe] Shimmerweed=%d Rexxar=%s Rokhan=%s Misha=%s next=%.0f,%.0f threat=%s%n",
					gathered, heroPosition(rexxar), heroPosition(rokhan), heroPosition(misha), nextHerb.getX(), nextHerb.getY(),
					enemy == null ? "none" : enemy.getTypeId());
		}
	}

	private static void returnThunderHerbs(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals) {
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		final CUnit rexxar = object(globals, "udg_Rexxar");
		final CUnit rokhan = object(globals, "udg_Rokhan");
		final CQuest quest = object(globals, "udg_QuestThunderHerb");
		check(tick < 110_000 && !"Defeat".equals(ui.getScoreDialogTitle()), "the original Thunder Ridge turn-in stalled");
		if (rexxar == null || rokhan == null || quest == null || rexxar.isHidden() || game.isGamePaused()
				|| !ui.isUserControlEnabled()) return;
		if (!returnedFromThunderRidge) {
			check(game.getMissionCheckpoint().isLoading() && viewer.getMapConfig().isMapFlagSet(CMapFlag.MAP_RELOADED)
					&& validArchive("OrcX01_05"), "returning from Thunder Ridge lost a native zone archive");
			final CQuestItem collected = object(globals, "udg_QuestThunderHerbReq1");
			check(collected != null && collected.isCompleted(), "the native zone transition lost the six collected herbs");
			verifyRestoredParty(globals, rexxar, rokhan);
			returnedFromThunderRidge = true;
			orderTick = 0;
			waypoints.clear();
			System.out.println("[RexxarOpeningProbe] returned to Durotar with Thunder Ridge archived and the collection preserved");
		}
		if (quest.isCompleted()) {
			check(game.getItems().stream().anyMatch(item -> "envl".equals(item.getTypeId().toString())
					&& !item.isDead() && item.distanceSquaredNoCollision(-1824, 512) < 128 * 128),
					"Drek'Thar did not grant the native Enchanted Vial reward");
			System.out.println("[RexxarOpeningProbe] turned in the original Shimmerweed quest to Drek'Thar and received its reward");
			finish();
			return;
		}
		if (tick < orderTick || rexxar.isDead() || rokhan.isDead()) return;
		orderTick = tick + 16;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(rexxar.getPlayerIndex());
		advance(viewer, rexxar, rexxar, orders, THUNDER_RIDGE_TURN_IN);
		advance(viewer, rokhan, rexxar, orders, THUNDER_RIDGE_TURN_IN);
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
	}

	private static float[][] thunderRidgeTurnInRoute() {
		final int portal = THUNDER_RIDGE.length - 3;
		final float[][] route = new float[portal + 3][];
		System.arraycopy(reversedRoute(THUNDER_RIDGE, portal), 0, route, 0, portal + 1);
		route[portal + 1] = new float[] { -1120, 192 };
		route[portal + 2] = new float[] { -1792, 544 };
		return route;
	}

	private static float[][] reversedRoute(final float[][] route, final int last) {
		final float[][] reverse = new float[last + 1][];
		for (int index = 0; index <= last; index++) reverse[index] = route[last - index];
		return reverse;
	}

	private static void completeHarpyThreat(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit rexxar, final CPlayerUnitOrderExecutor orders) {
		final var game = viewer.simulation;
		final CUnit rokhan = object(globals, "udg_Rokhan");
		final CUnit bloodfeather = object(globals, "udg_Bloodfeather");
		final CQuest quest = object(globals, "udg_QuestHarpyThreat");
		if (rokhan == null || bloodfeather == null || quest == null) return;
		if (rexxar.isDead() || rokhan.isDead()) {
			harpyPartyReviving = true;
			return;
		}
		if (harpyPartyReviving) {
			harpyPartyReviving = false;
			waypoints.clear();
			System.out.printf("[RexxarOpeningProbe] resumed the Harpy Threat after native revival at %s/%s%n",
					heroPosition(rexxar), heroPosition(rokhan));
		}
		final int progress = globals.getGlobal("udg_QuestHarpyThreatProgress").visit(IntegerJassValueVisitor.getInstance());
		if (progress >= 3 && !bloodfeatherDefeated) {
			check(bloodfeather.isDead() && game.getItems().stream().anyMatch(item -> "bfhr".equals(item.getTypeId().toString())),
					"Bloodfeather's original death event did not award the feather");
			bloodfeatherDefeated = true;
			waypoints.clear();
			System.out.println("[RexxarOpeningProbe] defeated the original Bloodfeather and verified her native drop");
		}
		if (quest.isCompleted()) {
			check(nazgrelTurnIn && (CampaignPlaythroughProbe.hasItem(rexxar, "frhg") || CampaignPlaythroughProbe.hasItem(rokhan, "frhg")),
					"the original armory sale did not grant the selected Firehand Gauntlets");
			System.out.println("[RexxarOpeningProbe] completed the original Harpy Threat by claiming the armory reward");
			finish();
			return;
		}
		if (progress >= 4) {
			final CUnit armory = object(globals, "udg_Armory");
			check(armory != null && armory.getNeutralBuildingData() != null, "the original armory is unavailable");
			final var stock = armory.getFirstAbilityOfType(CAbilitySellItems.class);
			check(stock != null, "the original armory has no item stock");
			final int reward = stock.getItemsSold().indexOf(War3ID.fromString("I000"));
			if (!nazgrelTurnIn) {
				check(reward >= 0 && stock.getItemStockCurrent(War3ID.fromString("I000")) == 1, "Nazgrel did not stock the armory reward");
				nazgrelTurnIn = true;
				waypoints.clear();
				System.out.println("[RexxarOpeningProbe] returned to Nazgrel and verified the original armory choices");
			}
			final CUnit buyer = armory.getNeutralBuildingData().getSelectedPlayerUnit(rexxar.getPlayerIndex());
			if (reward >= 0 && buyer != null && hasInventorySpace(buyer)
					&& stock.getItemStockCurrent(War3ID.fromString("I000")) > 0) {
				orders.issueImmediateOrder(armory.getHandleId(), stock.getHandleId(), ((reward + 1) << 8) | rexxar.getPlayerIndex(), false);
			}
			else advance(viewer, rexxar, rexxar, orders, ARMORY_ROAD);
		}
		else if (bloodfeatherDefeated) {
			advance(viewer, rexxar, rexxar, orders, HARPY_RETURN);
			advance(viewer, rokhan, rexxar, orders, HARPY_RETURN);
		}
		else {
			if (rexxar.getHeroData().getSkillPoints() > 0
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, REXXAR_QUILBEAST)) return;
			if (rokhan.getHeroData().getSkillPoints() > 0
					&& (HumanCampaignPlaythroughProbe.immediate(viewer, orders, rokhan, ROKHAN_SERPENT_WARD)
							|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, rokhan, ROKHAN_HEALING_WAVE))) return;
			final int rokhanStep = waypoints.getOrDefault(rokhan.getHandleId(), 0);
			final int last = HARPY_ROAD.length - 1;
			final CUnit hurt = rexxar.getLife() / rexxar.getMaximumLife() < rokhan.getLife() / rokhan.getMaximumLife() ? rexxar : rokhan;
			final boolean healing = hurt.getLife() < hurt.getMaximumLife() * 0.79f
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.healingwave, hurt);
			if (rexxar.canReach(bloodfeather, 720)) {
				HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.summonquillbeast);
				if (!healing && rokhan.getMana() >= 120 && rokhan.canReach(bloodfeather, 600)) {
					CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, rokhan, OrderIds.ward, rokhan.getX(), rokhan.getY());
				}
			}
			advance(viewer, rexxar, rexxar, orders, HARPY_ROAD, Math.min(last, rokhanStep + 2));
			if (!healing) advance(viewer, rokhan, rexxar, orders, HARPY_ROAD);
		}
		final int tick = game.getGameTurnTick();
		if (tick >= reportTick) {
			reportTick = tick + 450;
			System.out.printf("[RexxarOpeningProbe] Harpy Threat progress=%d Bloodfeather life=%.0f Rexxar=%s life=%.0f Rokhan=%s life=%.0f mana=%.0f%n",
					progress, bloodfeather.getLife(), heroPosition(rexxar), rexxar.getLife(), heroPosition(rokhan), rokhan.getLife(), rokhan.getMana());
		}
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
	}

	private static void leaveHerbApproachClear(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit companion, final CUnit collector, final CItem herb, final int side) {
		if (companion.distanceSquaredNoCollision(herb) > 240 * 240
				&& companion.distanceSquaredNoCollision(collector) < 420 * 420) return;
		final float dx = collector.getX() - herb.getX();
		final float dy = collector.getY() - herb.getY();
		final float length = Math.max(1, (float) Math.hypot(dx, dy));
		CampaignPlaythroughProbe.pointOrder(viewer, orders, companion,
				collector.getX() + (dx * 220 - dy * 90 * side) / length,
				collector.getY() + (dy * 220 + dx * 90 * side) / length);
	}

	private static boolean hasInventorySpace(final CUnit hero) {
		final var inventory = hero.getInventoryData();
		if (inventory == null) return false;
		for (int slot = 0; slot < inventory.getCapacity(); slot++) {
			if (inventory.getItemInSlot(slot) == null) return true;
		}
		return false;
	}

	private static void verifyTunnelArrival(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals) {
		final var game = viewer.simulation;
		final int tick = game.getGameTurnTick();
		check(tick < (COLLAPSE_ALL_TUNNELS ? 90_000 : COLLAPSE_FIRST_TUNNEL ? 30_000 : 8000),
				"the original Goblin Tunnels objective stalled after " + verifiedTunnels + " collapses");
		final CUnit rexxar = object(globals, "udg_Rexxar");
		final CUnit rokhan = object(globals, "udg_Rokhan");
		final CQuest tunnels = object(globals, "udg_QuestSealTunnel");
		if (!ui.isUserControlEnabled() || rexxar == null || rokhan == null || tunnels == null || !tunnels.isDiscovered()) return;
		if (!tunnelsEntered) {
			verifyRestoredParty(globals, rexxar, rokhan);
			check(validArchive("OrcX01"), "the original zone transition did not archive Durotar");
			System.out.println("[RexxarOpeningProbe] entered the original Goblin Tunnels with both heroes and archived Durotar");
			tunnelsEntered = true;
			if (BACK_UP_ZONES) {
				ui.quickSaveGame();
				final File directory = MissionSaveStore.currentDirectory();
				check(CGameSave.tryLoad(new File(directory, "QuickSave.w3s")) != null
						&& CGameSave.tryLoad(new File(directory, "Blizzard/QuickSave/OrcX01.w3z.w3s")) != null,
						"the original save trigger did not back up the Durotar zone before Quick Save completed");
				System.out.println("[RexxarOpeningProbe] Quick Save backed up the Durotar archive through the original save trigger");
			}
			if (!RETURN_FROM_TUNNELS) {
				finish();
				return;
			}
		}
		if (game.isGamePaused() || tick < tunnelOrderTick || rexxar.isPaused() && !rexxar.isDead()) return;
		tunnelOrderTick = tick + 14;
		final var orders = game.getDefaultPlayerUnitOrderExecutor(rexxar.getPlayerIndex());
		if (COLLAPSE_FIRST_TUNNEL && verifiedTunnels < (COLLAPSE_ALL_TUNNELS ? SUPPORT_COLUMNS.length : 1)) {
			collapseTunnel(viewer, ui, globals, rexxar, rokhan, orders);
			return;
		}
		CampaignPlaythroughProbe.pointOrder(viewer, orders, rexxar, -3264, -1152);
	}

	private static void collapseTunnel(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit rexxar, final CUnit rokhan, final CPlayerUnitOrderExecutor orders) {
		final int tick = viewer.simulation.getGameTurnTick();
		if (rexxar.isDead() || rokhan.isDead()) {
			if (resurrectionDeadline == 0) {
				resurrectionDeadline = tick + 560;
				System.out.println("[RexxarOpeningProbe] waiting for the original Resurrection Stone");
			}
			rexxarAwaitingRevival |= rexxar.isDead();
			rokhanAwaitingRevival |= rokhan.isDead();
			check(tick < resurrectionDeadline, "the original stone did not revive a fallen hero in the Goblin Tunnels");
			return;
		}
		if (resurrectionDeadline != 0) {
			check((!rexxarAwaitingRevival || rexxar.canReach(-2400, -736, 225))
					&& (!rokhanAwaitingRevival || rokhan.canReach(-2400, -736, 225)),
					"the original stone revived a hero outside its authored return area");
			System.out.println("[RexxarOpeningProbe] the original Resurrection Stone revived the fallen heroes");
			waypoints.remove(rexxar.getHandleId());
			waypoints.remove(rokhan.getHandleId());
			rexxarRefuge = -1;
			rokhanRefuge = -1;
			tunnelPartyRefuge = -1;
			rexxarAwaitingRevival = false;
			rokhanAwaitingRevival = false;
			resurrectionDeadline = 0;
		}
		final CDestructable column = object(globals, SUPPORT_COLUMNS[verifiedTunnels][0]);
		final float[][] route = SUPPORT_ROUTES[verifiedTunnels];
		final int collapsedColumns = globals.getGlobal("udg_LBColumnsDestroyed").visit(IntegerJassValueVisitor.getInstance());
		if (collapsedColumns > verifiedTunnels) {
			final CDestructable pairedColumn = object(globals, SUPPORT_COLUMNS[verifiedTunnels][1]);
			check(sappersRescued && column.isDead() && pairedColumn.isDead(), "an original pair of support columns survived");
			verifiedTunnels++;
			waypoints.clear();
			rexxarRefuge = -1;
			rokhanRefuge = -1;
			tunnelPartyRefuge = -1;
			if (verifiedTunnels == 1) {
				System.out.println("[RexxarOpeningProbe] rescued a Goblin Sapper and destroyed the first pair of original support columns");
			}
			else if (verifiedTunnels == SUPPORT_COLUMNS.length) {
				final CQuestItem seals = object(globals, "udg_QuestSealTunnelReq2");
				check(seals != null && seals.isCompleted(), "the original quest did not count all three collapsed tunnels");
				System.out.println("[RexxarOpeningProbe] collapsed all three original Goblin Tunnels and completed the support objective");
			}
			return;
		}
		ui.getCameraManager().setTarget(rexxar.getX(), rexxar.getY());
		if (tick >= nextTunnelReport) {
			nextTunnelReport = tick + 360;
			System.out.printf("[RexxarOpeningProbe] tunnels=%d Rexxar=%s life=%.0f Rokhan=%s life=%.0f mana=%.0f Sapper=%s%n",
					verifiedTunnels, heroPosition(rexxar), rexxar.getLife(), heroPosition(rokhan), rokhan.getLife(), rokhan.getMana(), heroPosition(sapper));
		}
		if (rokhan.getHeroData().getSkillPoints() > 0
				&& (HumanCampaignPlaythroughProbe.immediate(viewer, orders, rokhan, ROKHAN_HEALING_WAVE)
						|| verifiedTunnels > 0 && HumanCampaignPlaythroughProbe.immediate(viewer, orders, rokhan, ROKHAN_SERPENT_WARD)
						|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, rokhan, ROKHAN_HEX))) return;
		if (verifiedTunnels > 0 && rexxar.getHeroData().getSkillPoints() > 0
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, REXXAR_QUILBEAST)) return;
		if (!sappersRescued && rexxar.getLife() < rexxar.getMaximumLife() * 0.77f
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.healingwave, rexxar)) return;
		if (!tunnelPartyReady) {
			if (rexxar.getLife() < rexxar.getMaximumLife() * 0.77f || rokhan.getMana() < 185) return;
			tunnelPartyReady = true;
		}
		if (sapper == null || sapper.isDead()) {
			sapper = viewer.simulation.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
					&& unit.getPlayerIndex() == rexxar.getPlayerIndex() && "ngsp".equals(unit.getTypeId().toString()))
					.findFirst().orElse(null);
			if (sapper == null) {
				if (!sappersRescued) advance(viewer, rexxar, rexxar, orders, SAPPER_RENDEZVOUS);
				return;
			}
			if (!sappersRescued) {
				sappersRescued = true;
				waypoints.clear();
				System.out.println("[RexxarOpeningProbe] watched the original rendezvous and gained control of the Goblin Sappers");
			}
		}
		final CUnit misha = viewer.simulation.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& unit.getPlayerIndex() == rexxar.getPlayerIndex() && "ngzc".equals(unit.getTypeId().toString()))
				.findFirst().orElse(null);
		if (misha == null && HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.spiritwolf)) return;
		if (verifiedTunnels > 0 && rexxar.getMana() >= 100 && nearestTunnelThreat(viewer, rexxar, 680) != null
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, rexxar, OrderIds.summonquillbeast)) return;
		if (verifiedTunnels > 0 && recoverTunnelParty(viewer, orders, rexxar, rokhan, misha, route)) return;
		final float health = rexxar.getLife() / rexxar.getMaximumLife();
		rexxarRecovering = verifiedTunnels == 0 && health < (rexxarRecovering ? 0.8f : 0.59f);
		final CUnit pursuer = rexxarRecovering ? nearestTunnelThreat(viewer, rexxar, 620) : null;
		if (misha != null) {
			if (rexxarRecovering) {
				if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, misha, OrderIds.attack, pursuer)) {
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, misha, rexxar.getX() + 96, rexxar.getY() + 72);
				}
			}
			else if (verifiedTunnels == 0) advance(viewer, misha, rexxar, orders, route);
			else advance(viewer, misha, rexxar, orders, route,
					Math.min(route.length - 2, waypoints.getOrDefault(rexxar.getHandleId(), 0) + 1));
		}
		final int scoutStep = waypoints.getOrDefault(rexxar.getHandleId(), 0);
		final int supportFront = verifiedTunnels == 0 ? route.length - 1 : route.length - 2;
		if (rexxarRecovering && !CampaignPlaythroughProbe.useInventoryItem(viewer, orders, rexxar, "phea")
				&& !CampaignPlaythroughProbe.useInventoryItem(viewer, orders, rexxar, "pghe")) {
			final int step = waypoints.getOrDefault(rexxar.getHandleId(), 0);
			if (rexxarRefuge < 0) rexxarRefuge = Math.max(0, step - 4);
			if (pursuer != null && rexxarRefuge > 0 && rexxar.distanceSquaredNoCollision(
					route[rexxarRefuge][0], route[rexxarRefuge][1]) < 180 * 180) rexxarRefuge--;
			final float[] refuge = route[rexxarRefuge];
			CampaignPlaythroughProbe.pointOrder(viewer, orders, rexxar, refuge[0], refuge[1]);
		}
		else if (!rexxarRecovering) {
			rexxarRefuge = -1;
			advance(viewer, rexxar, rexxar, orders, route, supportFront);
		}
		protectRokhan(viewer, orders, rexxar, rokhan, route, verifiedTunnels == 0 ? 3 : Math.max(0, scoutStep - 1));
		if (column.isDead()) return;
		if (sapper.canReach(column, 260)
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, sapper, OrderIds.selfdestruct, column)) return;
		if (sapper.distanceSquaredNoCollision(column) < 210 * 210
				&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, sapper, OrderIds.selfdestruct,
						column.getX() - (verifiedTunnels == 1 ? 88 : 0), column.getY() - (verifiedTunnels == 1 ? 0 : 88))) return;
		if (verifiedTunnels == 0 || rexxar.canReach(column, 730) && nearestTunnelThreat(viewer, rexxar, 480) == null) {
			CampaignPlaythroughProbe.followRoute(viewer, orders, sapper, route, route.length - 1, 135, waypoints);
		}
		else if (!rexxarRecovering && nearestTunnelThreat(viewer, sapper, 380) == null) {
			CampaignPlaythroughProbe.followRoute(viewer, orders, sapper, route, Math.max(0, scoutStep - 1), 135, waypoints);
		}
	}

	private static boolean recoverTunnelParty(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit rexxar, final CUnit rokhan, final CUnit misha, final float[][] route) {
		final float rexxarHealth = rexxar.getLife() / rexxar.getMaximumLife();
		final float rokhanHealth = rokhan.getLife() / rokhan.getMaximumLife();
		if (tunnelPartyRefuge < 0) {
			if (rexxarHealth > 0.57f && rokhanHealth > 0.60f) return false;
			tunnelPartyRefuge = Math.max(0, Math.min(waypoints.getOrDefault(rexxar.getHandleId(), 0),
					waypoints.getOrDefault(rokhan.getHandleId(), 0)) - 1);
		}
		final CUnit pursuer = nearestTunnelThreat(viewer, rokhan, 500);
		if (rexxarHealth >= 0.78f && rokhanHealth >= 0.78f && rokhan.getMana() >= 75 && pursuer == null
				&& rexxar.canReach(rokhan, 650)) {
			waypoints.put(rexxar.getHandleId(), tunnelPartyRefuge);
			waypoints.put(rokhan.getHandleId(), tunnelPartyRefuge);
			if (misha != null) waypoints.put(misha.getHandleId(), tunnelPartyRefuge);
			tunnelPartyRefuge = -1;
			return false;
		}
		final float[] currentRefuge = route[tunnelPartyRefuge];
		if (pursuer != null && tunnelPartyRefuge > 0 && rokhan.canReach(currentRefuge[0], currentRefuge[1], 180)
				&& rexxar.canReach(rokhan, 450)) tunnelPartyRefuge--;
		final float[] refuge = route[tunnelPartyRefuge];
		final CUnit healingTarget = rokhanHealth < rexxarHealth ? rokhan : rexxar;
		final boolean healing = rokhan.getCurrentBehavior() != null
				&& rokhan.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL;
		if (!healing && !(healingTarget.getLife() < healingTarget.getMaximumLife() * 0.87f && rokhan.canReach(healingTarget, 650)
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.healingwave, healingTarget))) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, rokhan, refuge[0], refuge[1]);
		}
		if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, rexxar, "phea")
				&& !CampaignPlaythroughProbe.useInventoryItem(viewer, orders, rexxar, "pghe")) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, rexxar, refuge[0] + 64, refuge[1]);
		}
		if (misha != null && !CampaignPlaythroughProbe.targetOrder(viewer, orders, misha, OrderIds.attack, pursuer)) {
			CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, misha, refuge[0], refuge[1]);
		}
		if (nearestTunnelThreat(viewer, sapper, 440) != null) {
			CampaignPlaythroughProbe.pointOrder(viewer, orders, sapper, refuge[0], refuge[1] - 72);
		}
		return true;
	}

	private static CUnit nearestTunnelThreat(final War3MapViewer viewer, final CUnit unit, final float radius) {
		final var game = viewer.simulation;
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return game.getUnits().stream().filter(enemy -> !enemy.isDead() && !enemy.isHidden() && !enemy.isInvulnerable()
				&& !owner.hasAlliance(enemy.getPlayerIndex(), CAllianceType.PASSIVE) && enemy.isVisible(game, owner.getId())
				&& !enemy.getCurrentAttacks().isEmpty() && unit.canReach(enemy, radius))
				.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
	}

	private static void protectRokhan(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit rexxar, final CUnit rokhan, final float[][] route, final int end) {
		if (rokhan.getCurrentBehavior() != null && rokhan.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
		final float health = rokhan.getLife() / rokhan.getMaximumLife();
		final CUnit hurtHero = health < 0.82f && health < rexxar.getLife() / rexxar.getMaximumLife() ? rokhan : rexxar;
		if (hurtHero.getLife() < hurtHero.getMaximumLife() * 0.82f && rokhan.canReach(hurtHero, 700)
				&& CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.healingwave, hurtHero)) return;
		rokhanRecovering = health < (rokhanRecovering ? 0.77f : 0.6f);
		if (rokhanRecovering) {
			if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, rokhan, "phea")) return;
			if (rokhanRefuge < 0) rokhanRefuge = Math.max(0, waypoints.getOrDefault(rokhan.getHandleId(), 0) - 3);
			final float[] previous = route[rokhanRefuge];
			if (rokhanRefuge > 0 && rokhan.distanceSquaredNoCollision(previous[0], previous[1]) < 160 * 160) rokhanRefuge--;
			final float[] refuge = route[rokhanRefuge];
			CampaignPlaythroughProbe.pointOrder(viewer, orders, rokhan, refuge[0], refuge[1]);
			return;
		}
		rokhanRefuge = -1;
		final var game = viewer.simulation;
		final var owner = game.getPlayer(rokhan.getPlayerIndex());
		final CUnit threat = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, owner.getId())
				&& rokhan.canReach(unit, 520)).max(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		if (verifiedTunnels > 0 && threat != null && rokhan.getMana() >= 125
				&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, rokhan, OrderIds.ward,
						(rokhan.getX() + threat.getX()) * 0.5f, (rokhan.getY() + threat.getY()) * 0.5f)) return;
		if (verifiedTunnels == 0 || rokhan.getMana() >= 160) {
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.hex, threat)) return;
		}
		if (verifiedTunnels > 0 && CampaignPlaythroughProbe.targetOrder(viewer, orders, rokhan, OrderIds.attack, threat)) return;
		CampaignPlaythroughProbe.followRoute(viewer, orders, rokhan, route, end, 140, waypoints);
	}

	private static void verifyDurotarReturn(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals) {
		final CUnit rexxar = object(globals, "udg_Rexxar");
		final CUnit rokhan = object(globals, "udg_Rokhan");
		final CQuest wish = object(globals, "udg_QuestDyingWish");
		final int tick = viewer.simulation.getGameTurnTick();
		if (tick >= nextReturnReport) {
			nextReturnReport = tick + 160;
			System.out.printf("[RexxarOpeningProbe] Durotar return tick=%d scriptReload=%s control=%s party=%s/%s%n",
					tick, viewer.getMapConfig().isMapFlagSet(CMapFlag.MAP_RELOADED), ui.isUserControlEnabled(),
					heroPosition(rexxar), heroPosition(rokhan));
		}
		if (!ui.isUserControlEnabled() || rexxar == null || rokhan == null || wish == null || rexxar.isHidden()) return;
		check(viewer.simulation.getMissionCheckpoint().isLoading() && viewer.getMapConfig().isMapFlagSet(CMapFlag.MAP_RELOADED),
				"returning from the Goblin Tunnels did not reload the Durotar archive");
		check(wish.isCompleted() && validArchive("OrcX01_02"), "the Goblin Tunnels archive or Durotar quest state was lost");
		verifyRestoredParty(globals, rexxar, rokhan);
		System.out.println("[RexxarOpeningProbe] returned from the original Goblin Tunnels and restored the Durotar archive");
		finish();
	}

	private static String heroPosition(final CUnit hero) {
		return hero == null ? "absent" : Math.round(hero.getX()) + "," + Math.round(hero.getY()) + (hero.isHidden() ? " hidden" : "");
	}

	private static boolean validArchive(final String map) {
		final File file = new File(MissionSaveStore.currentDirectory(), "Blizzard/Zones/" + map + ".w3z.w3s");
		final CGameSave save = CGameSave.tryLoad(file);
		return save != null && save.checkpoint != null;
	}

	private static void verifyRestoredParty(final GlobalScope globals, final CUnit rexxar, final CUnit rokhan) {
		final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
		check(cache != null && restoredHeroMatches(cache, "TransitionRexxar", rexxar)
				&& restoredHeroMatches(cache, "TransitionRokhan", rokhan), "the zone did not restore the original party");
	}

	private static boolean restoredHeroMatches(final CGameCache cache, final String key, final CUnit hero) {
		final var stored = cache.getStoredUnit("OrcX01Transitions", key);
		return stored != null && CampaignPlaythroughProbe.heroProperties(stored)
				.equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero)));
	}

	private static void finish() {
		finished = true;
		Gdx.app.exit();
	}

	private static void advance(final War3MapViewer viewer, final CUnit unit, final CUnit rexxar,
			final CPlayerUnitOrderExecutor orders, final float[][] route) {
		advance(viewer, unit, rexxar, orders, route, route.length - 1);
	}

	private static void advance(final War3MapViewer viewer, final CUnit unit, final CUnit rexxar,
			final CPlayerUnitOrderExecutor orders, final float[][] route, final int end) {
		if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
		final var game = viewer.simulation;
		final var player = game.getPlayer(unit.getPlayerIndex());
		final CUnit enemy = game.getUnits().stream().filter(candidate -> !candidate.isDead() && !candidate.isHidden()
				&& !candidate.isInvulnerable() && !player.hasAlliance(candidate.getPlayerIndex(), CAllianceType.PASSIVE)
				&& candidate.isVisible(game, player.getId()) && rexxar.canReach(candidate, 530))
				.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		if (unit == rexxar && unit.getLife() < unit.getMaximumLife() * 0.62f
				&& CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")) return;
		if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) return;
		CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 145, waypoints);
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Rexxar opening audit: " + message);
	}
}
