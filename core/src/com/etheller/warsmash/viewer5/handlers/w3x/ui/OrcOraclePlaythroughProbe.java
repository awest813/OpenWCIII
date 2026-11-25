package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.parsers.jass.triggers.UnitGroup;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;

/** Directs Thrall's original Oracle party to the Blade Key and King's Door. */
public final class OrcOraclePlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcOracleAudit");
	private static final boolean DRAGON = Boolean.getBoolean("warsmash.orcOracleDragon");
	private static final boolean HEART = Boolean.getBoolean("warsmash.orcOracleHeart");
	private static final boolean RETURN_HEART = Boolean.getBoolean("warsmash.orcOracleReturnHeart");
	private static final boolean GEMSTONE = Boolean.getBoolean("warsmash.orcOracleGemstone");
	private static final boolean VICTORY = Boolean.getBoolean("warsmash.orcOracleVictory");
	private static final float[][] BLADE_KEY_ROUTE = {
			{-2544, 3888}, {-2672, 3856}, {-3120, 3728}, {-3408, 3600}, {-3440, 3312},
			{-3440, 3024}, {-3216, 2832},
			{-2608, 2832}, {-2000, 2832}, {-1488, 2800}, {-1200, 2512}, {-1040, 2224}, {-752, 1936},
			{-464, 1648}, {-176, 1488}, {112, 1360}, {400, 1360}, {688, 1424}, {976, 1712},
			{1072, 2000}, {1072, 2576}, {1072, 2864}, {1264, 3152}, {1552, 3440}, {1840, 3600},
			{2128, 3632}, {2704, 3632}, {3280, 3632}, {3856, 3632}, {4144, 3632},
			{4432, 3344}, {4528, 3056}, {4624, 2768}, {4912, 2480}, {5200, 2384}, {5488, 2320}, {5776, 2128}
	};
	private static final float[][] KINGS_DOOR_ROUTE = {
			{5744, 2128}, {5456, 2128}, {5168, 2224}, {4944, 2512}, {4816, 2800}, {4752, 3088},
			{4688, 3376}, {4688, 3664}, {4688, 3952}, {4720, 4272}
	};
	private static final float[][] WAYGATE_ROUTE = {
			{4688, 4144}, {4752, 3920}, {5040, 3920}, {5328, 3920}, {5616, 3920}, {5904, 3920},
			{6128, 3888}, {6256, 3824}, {6416, 3792}, {6704, 3792}, {6768, 3664}
	};
	private static final float[][] DRAGON_ROUTE = {
			{1968, -1136}, {2256, -1136}, {2544, -1136}, {2800, -976}, {2864, -688}, {3152, -400},
			{3440, -272}, {3728, -208}, {4304, -208}, {4880, -208}, {5456, -208}, {6032, -144},
			{6608, -144}, {7184, -176}, {7760, -240}, {8336, -432}, {8624, -432}, {8752, -144},
			{8752, 432}, {8656, 720}, {8656, 1008}, {8656, 1296}, {8464, 1584}
	};
	private static final float[][] HEART_RETURN_ROUTE = {
			{ 8464, 1584 }, { 8656, 1296 }, { 8656, 720 }, { 8752, 144 }, { 8752, -144 }, { 8624, -432 },
			{ 8336, -432 }, { 7760, -240 }, { 7184, -176 }, { 6608, -144 }, { 6032, -144 }, { 5456, -208 },
			{ 4880, -208 }, { 4304, -208 }, { 3728, -208 }, { 3440, -272 }, { 3152, -400 }, { 2864, -688 },
			{ 2800, -976 }, { 2640, -1136 }, { 2768, -1392 }, { 2864, -1680 }, { 2864, -1968 }, { 2672, -2256 },
			{ 2528, -2368 }
	};
	private static final War3ID STATUE_APPROACH_GATE_TYPE = War3ID.fromString("LTe1");
	private static final float[][] CAIRNE_GATE_SWITCH_ROUTE = {
			{ -7632, -1872 }, { -7344, -1872 }, { -7056, -1680 }, { -6768, -1680 }, { -6480, -1744 },
			{ -6256, -2032 }, { -6096, -2320 }, { -6096, -2608 }, { -5840, -2896 }, { -5552, -3184 },
			{ -5264, -3376 }, { -4688, -3376 }, { -4080, -3280 }, { -3792, -2992 }, { -3504, -2704 },
			{ -3216, -2416 }, { -3152, -2128 }, { -3024, -1840 }, { -3024, -1552 }, { -3024, -1264 },
			{ -3088, -976 }, { -3248, -688 }, { -3248, -400 }, { -3248, -112 }, { -3088, 256 }
	};
	private static final float[][] CAIRNE_TRAP_ROUTE = {
			{ -3088, 48 }, { -3088, -240 }, { -3248, -528 }, { -3248, -816 }, { -2960, -1072 },
			{ -2672, -1328 }, { -2384, -1584 }, { -2096, -1648 }, { -1808, -1648 }, { -1520, -1648 },
			{ -1232, -1424 }, { -1152, -1408 }
	};

	private static final float[][] CAIRNE_GEMSTONE_ROUTE = {
			{ -1360, -1392 }, { -1648, -1552 }, { -1872, -1840 }, { -1968, -2128 }, { -2096, -2416 },
			{ -2160, -2704 }, { -2416, -2992 }, { -2384, -3280 }, { -2096, -3504 }, { -1808, -3760 },
			{ -1520, -3760 }, { -1360, -3568 }, { -1360, -3280 }, { -1360, -2992 }, { -1360, -2704 }
	};
	private static final float[][] CAIRNE_GHOST_ROUTE = {
			{ -1296, -2768 }, { -1008, -3056 }, { -720, -3216 }, { -432, -3248 }, { 144, -3248 },
			{ 432, -3280 }, { 720, -3568 }, { 944, -3856 }, { 944, -4432 }, { 1232, -4720 },
			{ 1520, -4784 }, { 2096, -4784 }, { 2384, -4752 }, { 2672, -4464 }
	};
	private static final float[][] CAIRNE_RETURN_ROUTE = {
			{ 2672, -4464 }, { 2800, -4176 }, { 2800, -3888 }, { 2800, -3600 }, { 2800, -3312 },
			{ 2864, -3024 }, { 2864, -2736 }, { 2928, -2448 }
	};
	private static final Map<Integer, Integer> waypoints = new HashMap<>();
	private static List<CUnit> party;
	private static boolean bladeKeyFound;
	private static boolean kingsDoorOpened;
	private static boolean waygateUsed;
	private static int nextOrder;
	private static int nextReport;
	private static boolean finished;
	private static boolean passageReported;
	private static boolean sentWaygateScouts;
	private static float waygateProgressX;
	private static float waygateProgressY;
	private static int waygateProgressTick;
	private static boolean waygatePassageReported;
	private static boolean dragonEncounterReached;
	private static boolean dragonRecovery;
	private static boolean heartCollected;
	private static boolean heartReturned;
	private static boolean cairneGateOpened;
	private static int quillboarTrapReadyTick = -1;
	private static boolean gemstoneCollected;
	private static boolean bridgeRaised;
	private static Properties carriedThrall;
	private static Properties carriedCairne;
	private static boolean ghostKeyCollected;
	private static int cairneRefuge = -1;
	private static int thrallRefuge = -1;

	private OrcOraclePlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final CUnit thrall = object(globals, "udg_Thrall");
		final CUnit cairne = object(globals, "udg_Cairne");
		if (VICTORY && map.endsWith("/orc08.w3m")) {
			if (thrall == null || cairne == null) return;
			check(carriedThrall != null && carriedCairne != null
					&& carriedThrall.equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(thrall)))
					&& carriedCairne.equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(cairne))),
					"the original Orc08 did not restore both heroes");
			System.out.println("[OrcOracleProbe] entered the original Orc08 with Thrall and Cairne restored from the earned cache");
			finished = true;
			Gdx.app.exit();
			return;
		}
		check(map.endsWith("/orc07.w3m"), "unexpected map " + map);
		final CQuest oracle = object(globals, "udg_QuestOracle");
		final int tick = game.getGameTurnTick();
		check(tick < (VICTORY ? 96_000 : 76_000) && !"Defeat".equals(ui.getScoreDialogTitle()),
				"the Oracle expedition stalled or ended in defeat at tick " + tick);
		final Trigger ending = object(globals, "gg_trg_Ending01");
		if (VICTORY && bridgeRaised && ending != null && !ending.isEnabled()) {
			final var dialog = ui.getVisibleScriptDialog();
			if (dialog != null && !dialog.getButtons().isEmpty()) {
				check(oracle != null && oracle.isCompleted(), "the original Oracle quest did not complete");
				final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
				final var storedThrall = cache == null ? null : cache.getStoredUnit("Orc08", "Thrall");
				final var storedCairne = cache == null ? null : cache.getStoredUnit("Orc08", "Cairne");
				check(storedThrall != null && storedCairne != null, "the original victory did not store both heroes");
				if (carriedThrall == null) {
					carriedThrall = CampaignPlaythroughProbe.heroProperties(storedThrall);
					carriedCairne = CampaignPlaythroughProbe.heroProperties(storedCairne);
					System.out.println("[OrcOracleProbe] crossed the bridge, watched the Oracle ending, and reached the original victory");
				}
				HumanCampaignPlaythroughProbe.continueVictory(ui);
			}
			return;
		}
		if (thrall == null || cairne == null || oracle == null || !oracle.isDiscovered() || game.isGamePaused()
				|| !ui.isUserControlEnabled() || tick < nextOrder) return;
		check(!globals.getGlobal("udg_GAMEOVER").visit(BooleanJassValueVisitor.getInstance()),
				"the original script recorded a hero death in the Oracle's dungeon");
		if (thrall.isDead() || cairne.isDead()) return;
		nextOrder = tick + 14;

		final int player = thrall.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (bridgeRaised) {
			ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
			CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, 4656, -2336);
			return;
		}
		if (party == null) {
			if (cairne.getPlayerIndex() == player || thrall.distanceSquaredNoCollision(cairne) <= 3_000 * 3_000) return;
			party = game.getUnitsIncludingPending().stream().filter(unit -> unit.getPlayerIndex() == player
					&& !unit.isDead() && !unit.isHidden() && !unit.getCurrentAttacks().isEmpty()).toList();
			System.out.println("[OrcOracleProbe] watched the original cave-in separate Thrall from Cairne");
		}
		final CItem bladeKey = object(globals, "gg_item_kybl_0069");
		final CDestructable kingsDoor = object(globals, "gg_dest_DTg5_0053");
		check(bladeKey != null && kingsDoor != null, "the original Blade Key or King's Door was missing");
		if (!bladeKeyFound && bladeKey.getContainedUnit() == thrall) {
			bladeKeyFound = true;
			waypoints.clear();
			thrallRefuge = -1;
			System.out.println("[OrcOracleProbe] recovered the original Blade Key");
		}
		if (bladeKeyFound && kingsDoor.isDead() && !kingsDoorOpened) {
			check(bladeKey.isDead(), "the original King's Door did not consume the Blade Key");
			System.out.println("[OrcOracleProbe] opened the original King's Door with the Blade Key");
			kingsDoorOpened = true;
			waypoints.clear();
			thrallRefuge = -1;
			if (!DRAGON) {
				finished = true;
				Gdx.app.exit();
				return;
			}
			thrall.addDamageTakenListener((simulation, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
				if (attacker != null && actual > 0 && (actual >= 90 || target.getLife() < 200)) {
					System.out.printf("[OrcOracleProbe] tick=%d %s at %.0f,%.0f dealt %.0f to Thrall life=%.0f%n",
							simulation.getGameTurnTick(), attacker.getTypeId(), attacker.getX(), attacker.getY(), actual, target.getLife());
				}
			});
		}
		if (kingsDoorOpened && !waygateUsed && thrall.distanceSquaredNoCollision(1856, -1152) < 280 * 280) {
			waygateUsed = true;
			waypoints.clear();
			thrallRefuge = -1;
			System.out.println("[OrcOracleProbe] used the original waygate to the lower halls");
		}
		if (waygateUsed) {
			final Trigger dragonReset = object(globals, "gg_trg_DamageControl");
			if (!dragonEncounterReached && dragonReset != null && !dragonReset.isEnabled()) {
				dragonEncounterReached = true;
				System.out.println("[OrcOracleProbe] reached the Dragon chamber and triggered the original encounter");
				if (!HEART) {
					finished = true;
					Gdx.app.exit();
					return;
				}
			}
		}
		if (dragonEncounterReached) {
			if (heartReturned) guideCairne(viewer, ui, globals, cairne, player, orders);
			else if (heartCollected) returnHeart(viewer, ui, globals, thrall, orders);
			else fightDragon(viewer, ui, globals, thrall, orders);
			return;
		}
		final var hostiles = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
				&& !unit.isInvulnerable() && !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player)).toList();
		final var breakableGates = game.getDestructables().stream().filter(gate -> gate != kingsDoor
				&& !gate.isDead() && !gate.isInvulnerable() && (gate.getTypeId().toString().startsWith("DTg")
						|| waygateUsed && "LTe1".equals(gate.getTypeId().toString()))).toList();
		final float[][] route = waygateUsed ? DRAGON_ROUTE
				: kingsDoorOpened ? WAYGATE_ROUTE : bladeKeyFound ? KINGS_DOOR_ROUTE : BLADE_KEY_ROUTE;
		final var activeParty = new java.util.ArrayList<>(party);
		if (kingsDoorOpened) {
			game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && unit.isSummonedOrWard()
					&& !unit.isDead() && !unit.getCurrentAttacks().isEmpty() && !activeParty.contains(unit)).forEach(activeParty::add);
		}
		if (kingsDoorOpened && !waygateUsed && thrall.canReach(4900, 3920, 240) && !sentWaygateScouts
				&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, thrall, OrderIds.spiritwolf)) {
			sentWaygateScouts = true;
			return;
		}
		if (kingsDoorOpened && !waygateUsed) {
			ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
			if (tick >= nextReport) {
				nextReport = tick + 160;
				System.out.printf("[OrcOracleProbe] waygate approach %.0f,%.0f life=%.0f step=%d wolves=%d order=%s%n",
						thrall.getX(), thrall.getY(), thrall.getLife(), waypoints.getOrDefault(thrall.getHandleId(), 0),
						activeParty.stream().filter(CUnit::isSummonedOrWard).count(),
						thrall.getCurrentOrder() == null ? "none" : thrall.getCurrentOrder().getOrderId());
			}
			if (waygateProgressTick == 0 || thrall.distanceSquaredNoCollision(waygateProgressX, waygateProgressY) > 70 * 70) {
				waygateProgressX = thrall.getX();
				waygateProgressY = thrall.getY();
				waygateProgressTick = tick;
			}
			for (final CUnit unit : activeParty) {
				if (unit != thrall && !unit.isDead() && !unit.isHidden()) {
					final CUnit blocker = hostiles.stream().filter(enemy -> unit.canReach(enemy, 630))
							.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
					if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, blocker)) {
						final int lead = Math.min(route.length - 1, waypoints.getOrDefault(thrall.getHandleId(), 0) + 2);
						CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, route[lead][0], route[lead][1]);
					}
				}
			}
			if (thrall.getCurrentBehavior() != null && thrall.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
			if (thrall.getLife() < thrall.getMaximumLife() * 0.55f
					&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "pghe")
							|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "phea"))) return;
			if (tick - waygateProgressTick >= 85) {
				final CUnit blocker = hostiles.stream().filter(unit -> thrall.canReach(unit, 430))
						.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
				if (blocker != null && activeParty.size() <= 1
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, thrall, OrderIds.spiritwolf)
						|| CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.chainlightning, blocker)
						|| CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, blocker)) return;
				if (thrall.getX() > 5900 && !waygatePassageReported) {
					waygatePassageReported = true;
					System.out.printf("[OrcOracleProbe] east passage destructibles %s%n", game.getDestructables().stream()
							.filter(gate -> !gate.isDead() && thrall.canReach(gate, 715))
							.map(gate -> String.format(Locale.ROOT, "%s at %.0f,%.0f invulnerable=%s", gate.getTypeId(),
									gate.getX(), gate.getY(), gate.isInvulnerable())).toList());
					System.out.printf("[OrcOracleProbe] east passage units %s%n", game.getUnits().stream()
							.filter(unit -> unit != thrall && !unit.isHidden() && !unit.isDead() && thrall.canReach(unit, 715))
							.map(unit -> String.format(Locale.ROOT, "%s at %.0f,%.0f invulnerable=%s", unit.getTypeId(),
									unit.getX(), unit.getY(), unit.isInvulnerable())).toList());
				}
				final CDestructable blockade = game.getDestructables().stream().filter(gate -> !gate.isDead() && !gate.isInvulnerable()
						&& (gate.getTypeId().toString().startsWith("DTg") || "DTrc".equals(gate.getTypeId().toString()))
						&& gate.getX() > thrall.getX() && thrall.canReach(gate, 480))
						.min(Comparator.comparingDouble(thrall::distanceSquaredNoCollision)).orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, blockade)) return;
			}
			if (thrall.canReach(6848, 3648, 740)) {
				final CUnit gate = game.getUnits().stream().filter(unit -> "nwgt".equals(unit.getTypeId().toString())
						&& unit.distanceSquaredNoCollision(6848, 3648) < 110 * 110).findFirst().orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.smart, gate)) return;
			}
			CampaignPlaythroughProbe.followRoute(viewer, orders, thrall, route, route.length - 1, 135, waypoints);
			return;
		}
		advanceParty(viewer, orders, activeParty, thrall, hostiles, route, bladeKeyFound ? null : bladeKey, breakableGates);
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (tick >= nextReport) {
			nextReport = tick + 360;
			System.out.printf("[OrcOracleProbe] Thrall=%.0f,%.0f life=%.0f step=%d companions=%d Blade Key=%s%n",
					thrall.getX(), thrall.getY(), thrall.getLife(), waypoints.getOrDefault(thrall.getHandleId(), 0),
					party.stream().filter(unit -> unit != thrall && !unit.isDead()).count(), bladeKeyFound);
			if (!passageReported && !bladeKeyFound && thrall.getY() > 3_700 && thrall.getX() > -3_000 && thrall.getX() < -2_550) {
				passageReported = true;
				System.out.printf("[OrcOracleProbe] passage units %s%n", game.getUnits().stream()
						.filter(unit -> unit != thrall && !unit.isHidden() && !unit.isDead() && thrall.canReach(unit, 275))
						.map(unit -> String.format(Locale.ROOT, "%s at %.0f,%.0f", unit.getTypeId(), unit.getX(), unit.getY())).toList());
				System.out.printf("[OrcOracleProbe] passage destructibles %s%n", game.getDestructables().stream()
						.filter(destructable -> !destructable.isDead() && thrall.canReach(destructable, 460))
						.map(destructable -> String.format(Locale.ROOT, "%s at %.0f,%.0f", destructable.getTypeId(),
								destructable.getX(), destructable.getY())).toList());
				final float collision = thrall.getUnitType().getCollisionSize();
				for (int y = 4_080; y >= 3_696; y -= 32) {
					final StringBuilder cells = new StringBuilder();
					for (int x = -2_960; x <= -2_384; x += 32) {
						cells.append(game.getPathingGrid().isPathable(x, y, thrall.getMovementType(), collision) ? '.' : '#');
					}
					System.out.printf("[OrcOracleProbe] passage row %d %s%n", y, cells);
				}
			}
		}
	}

	private static void fightDragon(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit thrall, final CPlayerUnitOrderExecutor orders) {
		final CUnit dragon = object(globals, "gg_unit_nrwm_0013");
		final CItem heart = object(globals, "udg_HeartOfAszune");
		check(dragon != null, "the original Dragon was missing");
		if (heart != null && heart.getContainedUnit() == thrall) {
			check(dragon.isDead(), "the Heart of Aszune was collected before the original Dragon died");
			System.out.println("[OrcOracleProbe] defeated the original Dragon and picked up the Heart of Aszune");
			heartCollected = true;
			waypoints.clear();
			if (!RETURN_HEART) {
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		final int tick = viewer.simulation.getGameTurnTick();
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (tick >= nextReport) {
			nextReport = tick + 180;
			System.out.printf("[OrcOracleProbe] Dragon life=%.0f Thrall %.0f,%.0f life=%.0f mana=%.0f%n",
					dragon.getLife(), thrall.getX(), thrall.getY(), thrall.getLife(), thrall.getMana());
		}
		if (thrall.getCurrentBehavior() != null && thrall.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return;
		final float hp = thrall.getLife() / thrall.getMaximumLife();
		dragonRecovery = hp < (dragonRecovery ? 0.85f : 0.66f);
		if (dragonRecovery) {
			if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "phea")
					|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "pghe")
					|| !thrall.canReach(dragon, 660) && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, thrall, "shea")) return;
			CampaignPlaythroughProbe.pointOrder(viewer, orders, thrall, 8336, -432);
			return;
		}
		if (dragon.isDead()) {
			CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.smart, heart);
			return;
		}
		if (thrall.canReach(dragon, 720) && (CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.chainlightning, dragon)
				|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, thrall, OrderIds.spiritwolf))) return;
		CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, dragon);
	}

	private static void returnHeart(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit thrall, final CPlayerUnitOrderExecutor orders) {
		final CItem heart = object(globals, "udg_HeartOfAszune");
		final CQuest quest = object(globals, "udg_QuestHeartOfAszune");
		final CDestructable barrier = object(globals, "gg_dest_Dofw_1237");
		if (quest != null && quest.isCompleted()) {
			check(heart != null && heart.isDead() && barrier != null && barrier.isDead(),
					"the native statue quest did not consume the Heart and open the next passage");
			System.out.println("[OrcOracleProbe] returned the Heart of Aszune and opened the original statue passage");
			heartReturned = true;
			waypoints.clear();
			if (GEMSTONE) return;
			finished = true;
			Gdx.app.exit();
			return;
		}
		if (heart != null && heart.getContainedUnit() == thrall) {
			final CDestructable gate = viewer.simulation.getDestructables().stream()
					.filter(candidate -> STATUE_APPROACH_GATE_TYPE.equals(candidate.getTypeId()) && !candidate.isDead()
								&& !candidate.isInvulnerable() && candidate.getX() == 3008 && candidate.getY() == -1664
								&& thrall.canReach(candidate, 360))
					.findFirst().orElse(null);
			if (gate != null) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, OrderIds.attack, gate);
			} else {
				CampaignPlaythroughProbe.followRoute(viewer, orders, thrall, HEART_RETURN_ROUTE,
						HEART_RETURN_ROUTE.length - 1, 132, waypoints);
			}
		}
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
	}

	private static void guideCairne(final War3MapViewer viewer, final MeleeUI ui, final GlobalScope globals,
			final CUnit cairne, final int player, final CPlayerUnitOrderExecutor orders) {
		final CQuest quest = object(globals, "udg_QuestEnchantedGemstone");
		if (quest == null || !quest.isDiscovered() || cairne.getPlayerIndex() != player || cairne.isHidden()) return;
		final CItem gemstone = object(globals, "udg_EnchantedGemstone");
		final CItem key = object(globals, "udg_GhostKey");
		final var game = viewer.simulation;
		final CDestructable quillboarGate = object(globals, "gg_dest_DTg2_1299");
		if (!cairneGateOpened && quillboarGate != null && quillboarGate.isDead()) {
			cairneGateOpened = true;
			waypoints.clear();
			cairneRefuge = -1;
			System.out.println("[OrcOracleProbe] Cairne activated the original switch and opened the Quilboar gate");
		}
		final Trigger trap = object(globals, "gg_trg_CairneActivatesTrap");
		if (cairneGateOpened && quillboarTrapReadyTick < 0 && trap != null && !trap.isEnabled()) {
			quillboarTrapReadyTick = game.getGameTurnTick() + 100;
			waypoints.clear();
			cairneRefuge = -1;
			System.out.println("[OrcOracleProbe] Cairne activated the original trap outside the Quilboar hall");
		}
		if (quillboarTrapReadyTick >= 0 && game.getGameTurnTick() < quillboarTrapReadyTick) return;
		if (quest.isCompleted()) {
			final CDestructable bridge = object(globals, "gg_dest_DTsb_0099");
			check(gemstoneCollected && ghostKeyCollected && gemstone.isDead() && bridge != null && !bridge.isDead(),
					"the original Gemstone quest did not consume the item and raise the bridge");
			System.out.println("[OrcOracleProbe] returned the original Enchanted Gemstone and raised the bridge");
			bridgeRaised = true;
			if (VICTORY) return;
			finished = true;
			Gdx.app.exit();
			return;
		}
		if (!gemstoneCollected && gemstone.getContainedUnit() == cairne) {
			gemstoneCollected = true;
			waypoints.clear();
			cairneRefuge = -1;
			System.out.println("[OrcOracleProbe] Cairne picked up the original Enchanted Gemstone");
			cairne.addDamageTakenListener((simulation, attacker, target, attack, ranged, type, damage, bonus, actual) -> {
				if (attacker != null && actual > 0 && (actual >= 80 || target.getLife() < 240)) {
					System.out.printf("[OrcOracleProbe] %s hit Cairne at %.0f,%.0f damage=%.0f life=%.0f%n",
							attacker.getTypeId(), cairne.getX(), cairne.getY(), actual, cairne.getLife());
				}
			});
		}
		if (!ghostKeyCollected && key != null && key.getContainedUnit() == cairne) {
			ghostKeyCollected = true;
			waypoints.clear();
			cairneRefuge = -1;
			System.out.println("[OrcOracleProbe] Cairne recovered the Ghost Key from the animated statue");
		}
		final boolean approachingTrap = cairneGateOpened && quillboarTrapReadyTick < 0;
		final float[][] route = ghostKeyCollected ? CAIRNE_RETURN_ROUTE : gemstoneCollected ? CAIRNE_GHOST_ROUTE
				: approachingTrap ? CAIRNE_TRAP_ROUTE : cairneGateOpened ? CAIRNE_GEMSTONE_ROUTE : CAIRNE_GATE_SWITCH_ROUTE;
		final CItem pickup = ghostKeyCollected ? null : gemstoneCollected ? key : gemstone;
		final var owner = game.getPlayer(player);
		final UnitGroup group = object(globals, "udg_CairneGroup");
		final var expedition = new java.util.ArrayList<CUnit>();
		expedition.add(cairne);
		if (group != null) group.stream().filter(unit -> unit != cairne && unit.getPlayerIndex() == player && !unit.isDead()
				&& !unit.isHidden() && !unit.getCurrentAttacks().isEmpty()).forEach(expedition::add);
		final CUnit enemy = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
				&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && unit.isVisible(game, player)
				&& cairne.canReach(unit, 530)).min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
		final CDestructable gate = game.getDestructables().stream().filter(candidate -> !candidate.isDead()
				&& !candidate.isInvulnerable() && (candidate.getTypeId().toString().startsWith("DTg")
						|| STATUE_APPROACH_GATE_TYPE.equals(candidate.getTypeId())) && cairne.canReach(candidate, 300))
				.min(Comparator.comparingDouble(cairne::distanceSquaredNoCollision)).orElse(null);
		final int step = waypoints.getOrDefault(cairne.getHandleId(), 0);
		final float retreatAt = gemstoneCollected ? 0.70f : 0.48f;
		if (cairne.getLife() < cairne.getMaximumLife() * retreatAt && cairneRefuge < 0) cairneRefuge = Math.max(0, step - 4);
		if (cairne.getLife() >= cairne.getMaximumLife() * 0.85f) cairneRefuge = -1;
		for (final CUnit unit : expedition) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit == cairne && cairneRefuge >= 0) {
				if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, cairne, "pghe")
						&& !CampaignPlaythroughProbe.useInventoryItem(viewer, orders, cairne, "phea")) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, cairne, route[cairneRefuge][0], route[cairneRefuge][1]);
				}
				continue;
			}
			if (gemstoneCollected && cairneRefuge >= 0) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, route[cairneRefuge][0], route[cairneRefuge][1]);
				continue;
			}
			if (approachingTrap && unit == cairne) {
				final float tolerance = step >= route.length - 2 ? 64 : 132;
				CampaignPlaythroughProbe.followRoute(viewer, orders, cairne, route, route.length - 1, tolerance, waypoints);
				continue;
			}
			if (unit == cairne && enemy != null && cairne.canReach(enemy, 165)
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, cairne, OrderIds.stomp)) continue;
			if (unit == cairne && enemy != null && cairne.canReach(enemy, 690)
					&& CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, cairne, OrderIds.shockwave,
							enemy.getX(), enemy.getY())) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)
					|| CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) continue;
			if (unit == cairne && pickup != null && cairne.canReach(pickup, 400)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, cairne, OrderIds.smart, pickup)) continue;
			final int end = unit == cairne ? route.length - 1
					: approachingTrap ? route.length - 4 : Math.min(step + 1, route.length - 1);
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 132, waypoints);
		}
		ui.getCameraManager().setTarget(cairne.getX(), cairne.getY());
		if (game.getGameTurnTick() >= nextReport) {
			nextReport = game.getGameTurnTick() + 450;
			System.out.printf("[OrcOracleProbe] Cairne %.0f,%.0f life=%.0f step=%d escorts=%d gemstone=%s key=%s%n",
					cairne.getX(), cairne.getY(), cairne.getLife(), step, expedition.size() - 1, gemstoneCollected, ghostKeyCollected);
		}
	}

	private static void advanceParty(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final List<CUnit> party, final CUnit hero, final List<CUnit> hostiles, final float[][] route,
			final CItem goalItem, final List<CDestructable> breakableGates) {
		final int heroStep = waypoints.getOrDefault(hero.getHandleId(), 0);
		final boolean enemyNearHero = hostiles.stream().anyMatch(enemy -> hero.canReach(enemy, 560));
		if (hero.getLife() < hero.getMaximumLife() * (kingsDoorOpened ? 0.78f : 0.70f) && thrallRefuge < 0) {
			thrallRefuge = Math.max(0, heroStep - 2);
		}
		if (hero.getLife() >= hero.getMaximumLife() * 0.88f) thrallRefuge = -1;
		final CDestructable gate = breakableGates.stream().filter(candidate -> hero.canReach(candidate, 390))
				.min(Comparator.comparingDouble(hero::distanceSquaredNoCollision)).orElse(null);
		for (final CUnit unit : party) {
			if (unit.isDead() || unit.isPaused() || unit.isHidden()
					|| unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			final CUnit enemy = hostiles.stream().filter(target -> unit.canReach(target, kingsDoorOpened ? 760 : 500))
					.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (unit == hero) {
				if (hero.getLife() < hero.getMaximumLife() * 0.68f
						&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, hero, "pghe")
								|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, hero, "phea")
								|| !enemyNearHero && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, hero, "shea"))) continue;
				if (thrallRefuge >= 0) {
					final float[] refuge = route[thrallRefuge];
					if (kingsDoorOpened && enemyNearHero && (HumanCampaignPlaythroughProbe.immediate(viewer, orders, hero, OrderIds.spiritwolf)
							|| CampaignPlaythroughProbe.targetOrder(viewer, orders, hero, OrderIds.chainlightning, enemy))) continue;
					if (enemyNearHero && thrallRefuge > 0 && unit.distanceSquaredNoCollision(refuge[0], refuge[1]) < 140 * 140) thrallRefuge--;
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, route[thrallRefuge][0], route[thrallRefuge][1]);
					continue;
				}
				if (kingsDoorOpened && enemy != null
						&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, hero, OrderIds.spiritwolf)) continue;
				if (enemy != null && (CampaignPlaythroughProbe.targetOrder(viewer, orders, hero, OrderIds.chainlightning, enemy)
						|| hero.canReach(enemy, 200) && HumanCampaignPlaythroughProbe.immediate(viewer, orders, hero, OrderIds.stomp)
						|| HumanCampaignPlaythroughProbe.immediate(viewer, orders, hero, OrderIds.spiritwolf))) continue;
				if (goalItem != null && unit.canReach(goalItem, 340)
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, hero, OrderIds.smart, goalItem)) continue;
				final var inventory = hero.getInventoryData();
				if (!enemyNearHero && inventory != null && java.util.stream.IntStream.range(0, inventory.getItemCapacity())
						.anyMatch(slot -> inventory.getItemInSlot(slot) == null)) {
					final CItem supply = viewer.simulation.getItems().stream().filter(item -> !item.isDead()
							&& !item.isHidden() && item.getContainedInventory() == null && hero.canReach(item, 190)
							&& List.of("phea", "pghe", "shea", "rlif").contains(item.getTypeId().toString()))
							.min(Comparator.comparingDouble(hero::distanceSquaredNoCollision)).orElse(null);
					if (CampaignPlaythroughProbe.targetOrder(viewer, orders, hero, OrderIds.smart, supply)) continue;
				}
			}
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, gate)) continue;
			if (unit != hero && heroStep < 5 && !bladeKeyFound) {
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, -2224, 3650 + 52 * party.indexOf(unit));
				continue;
			}
			final int end = unit == hero ? route.length - 1 : Math.min(route.length - 1, heroStep + 1);
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 110, waypoints);
		}
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc Oracle audit: " + message);
	}
}
