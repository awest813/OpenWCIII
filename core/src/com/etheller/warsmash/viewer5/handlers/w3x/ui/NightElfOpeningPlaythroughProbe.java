package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.root.CAbilityRoot;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;

/** Plays the Night Elf base-building tutorial and its original confrontation with the Paladin. */
public final class NightElfOpeningPlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.nightElfOpeningAudit");
	static final boolean CHAIN = Boolean.getBoolean("warsmash.nightElfCampaignChain");
	private static final boolean FURBOLG_AUDIT = Boolean.getBoolean("warsmash.nightElfFurbolgAudit");
	private static final War3ID GOLD_MINE = War3ID.fromString("ngol");
	private static final War3ID ENTANGLED_MINE = War3ID.fromString("egol");
	private static final War3ID WISP = War3ID.fromString("ewsp");
	private static final War3ID ANCIENT_OF_WAR = War3ID.fromString("eaom");
	private static final War3ID MOON_WELL = War3ID.fromString("emow");
	private static final War3ID ARCHER = War3ID.fromString("earc");
	private static final float[][] APPROACH = {
			{3456, 672}, {2096, -304}, {1840, -1024}, {1408, -1744}, {-432, -1760},
			{-1008, -1232}, {-2992, -1232}, {-4336, -1936}, {-4336, -2544}, {-5424, -2608},
			{-5312, -3008}, {-5536, -3376}
	};
	private static final String[] FURBOLG_REGIONS = {
			"gg_rct_03_Furbolg_Wander_05", "gg_rct_Furbolg_Discovered_05B", "gg_rct_Furbolg_Discovered_05A",
			"gg_rct_Furbolg_Discovered_04", "gg_rct_Furbolg_Discovered_03", "gg_rct_Furbolg_Discovered_01",
			"gg_rct_Furbolg_Discovered_02", "gg_rct_Furbolg_Discovered_06"
	};
	private static int waypoint;
	private static int furbolgWaypoint;
	private static int orderTick;
	private static int reportTick;
	private static boolean baseVerified;
	private static boolean handoffVerified;
	private static boolean furbolgsVerified;
	private static boolean tyrandeRecovering;
	private static Properties carryover;

	private NightElfOpeningPlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || handoffVerified || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final CUnit tyrande = object(globals, "udg_Tyrande");
		final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase(Locale.ROOT);
		if (map.endsWith("/nightelf02.w3m")) {
			if (tyrande == null) return;
			check(carryover != null && carryover.equals(snapshot(tyrande)), "Tyrande carryover mismatch");
			System.out.println("[NightElfOpeningProbe] entered NightElf02; Tyrande carryover verified");
			handoffVerified = true;
			if (!CHAIN) Gdx.app.exit();
			return;
		}
		check(map.endsWith("/nightelf01.w3m"), "unexpected map " + map);
		check(!"Defeat".equals(ui.getScoreDialogTitle()), "native defeat");
		final CQuest base = object(globals, "udg_NightElfBaseQuest");
		final CUnit paladin = object(globals, "udg_Paladin");
		if (base == null || tyrande == null || !base.isDiscovered()) return;
		check(!tyrande.isDead(), "Tyrande died");
		if (base.isCompleted() && !baseVerified) {
			final CQuestItem entangle = object(globals, "udg_EntangleReq");
			check(entangle != null && entangle.isCompleted(), "base quest completed without entangling the mine");
			baseVerified = true;
			System.out.println("[NightElfOpeningProbe] native mine, Ancient of War, Moon Well and Archer objectives completed");
		}
		if (baseVerified && paladin != null && paladin.isDead()) {
			check(!FURBOLG_AUDIT || furbolgsVerified, "the Paladin died before the Furbolg quest was completed");
			final CGameCache cache = object(globals, "bj_lastCreatedGameCache");
			final var stored = cache == null ? null : cache.getStoredUnit("NightElf02", "Tyrande");
			if (carryover == null && stored != null) {
				carryover = CampaignPlaythroughProbe.heroProperties(stored);
				System.out.println("[NightElfOpeningProbe] defeated the original Paladin and captured the original Tyrande cache");
			}
			if ("Victory".equals(ui.getScoreDialogTitle()) || ui.getVisibleScriptDialog() != null) {
				check(carryover != null, "the native victory did not save Tyrande for NightElf02");
				HumanCampaignPlaythroughProbe.continueVictory(ui);
			}
			return;
		}
		final int tick = game.getGameTurnTick();
		if (!ui.isUserControlEnabled() || tyrande.isHidden() || tyrande.isPaused() || game.isGamePaused()
				|| tick < orderTick) return;
		orderTick = tick + 20;
		check(tick < (FURBOLG_AUDIT ? 70_000 : 34_000), "the Night Elf objectives stalled");
		final int player = tyrande.getPlayerIndex();
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		final var units = game.getUnitsIncludingPending();
		final var owned = units.stream().filter(unit -> !unit.isDead() && unit.getPlayerIndex() == player).toList();
		final var wisps = owned.stream().filter(unit -> WISP.equals(unit.getTypeId())).toList();
		final CUnit tree = object(globals, "gg_unit_etol_0009");
		final CUnit mine = units.stream().filter(unit -> GOLD_MINE.equals(unit.getTypeId())
				&& unit.distanceSquaredNoCollision(5504, 896) < 160 * 160).findFirst().orElse(null);
		final CUnit entangled = owned.stream().filter(unit -> ENTANGLED_MINE.equals(unit.getTypeId())).findFirst().orElse(null);
		final CUnit ancient = owned.stream().filter(unit -> ANCIENT_OF_WAR.equals(unit.getTypeId())).findFirst().orElse(null);
		final long wells = owned.stream().filter(unit -> MOON_WELL.equals(unit.getTypeId())).count();
		final long archers = owned.stream().filter(unit -> ARCHER.equals(unit.getTypeId())).count();
		final long queuedArchers = ancient == null ? 0 : Arrays.stream(ancient.getBuildQueue()).filter(ARCHER::equals).count();
		if (!baseVerified || archers < 15) {
			final CAbilityRoot root = tree == null ? null : tree.getFirstAbilityOfType(CAbilityRoot.class);
			if (tree != null && root != null && !root.isRooted()) {
				CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, tree, OrderIds.root, 5120, 896);
			}
			else if (tree != null && entangled == null) {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, tree, OrderIds.entangle, mine);
			}
			if (tree != null && wisps.size() < 6 && Arrays.stream(tree.getBuildQueue()).noneMatch(WISP::equals)) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, tree, WISP.toString());
			}
			if (ancient != null && !ancient.isConstructing() && archers + queuedArchers < 16 && queuedArchers < 2) {
				HumanCampaignPlaythroughProbe.train(viewer, orders, ancient, ARCHER.toString());
			}
			for (int i = 0; i < wisps.size(); i++) {
				final CUnit wisp = wisps.get(i);
				if (wisp.isHidden()) continue;
				if (i >= 2 && ancient == null && buildNear(viewer, wisp, ANCIENT_OF_WAR, 4608, 448)) break;
				if (i >= 2 && (wells == 0 || wells < 3 && archers + queuedArchers >= 7)
						&& buildNear(viewer, wisp, MOON_WELL, 4672 + wells * 288, 192)) break;
				if (entangled != null && !entangled.isConstructing() && i < 2) {
					CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.smart, entangled);
				}
				else {
					final var treeToHarvest = game.getDestructables().stream().filter(destructable -> !destructable.isDead()
							&& destructable.getDestType().getTargetedAs().contains(CTargetType.TREE))
							.min(Comparator.comparingDouble(wisp::distanceSquaredNoCollision)).orElse(null);
					CampaignPlaythroughProbe.targetOrder(viewer, orders, wisp, OrderIds.wispharvest, treeToHarvest);
				}
			}
		}
		if (baseVerified && archers >= 8) {
			final float[] furbolg = FURBOLG_AUDIT ? furbolgDestination(globals, tyrande) : null;
			if (furbolg == null && waypoint < APPROACH.length - 1
					&& tyrande.distanceSquaredNoCollision(APPROACH[waypoint][0], APPROACH[waypoint][1]) < 245 * 245) waypoint++;
			final float[] target = furbolg == null ? APPROACH[waypoint] : furbolg;
			final var enemies = units.stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& unit.getPlayerIndex() != player && !game.getPlayer(player).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
					&& (furbolg == null || unit != paladin && tyrande.distanceSquaredNoCollision(unit) < 840 * 840)
					&& unit.isVisible(game, player)).toList();
			final var force = owned.stream().filter(unit -> !unit.isHidden() && !unit.isBuilding()
					&& !unit.getCurrentAttacks().isEmpty() && !WISP.equals(unit.getTypeId())).toList();
			final boolean paladinFight = furbolg == null && waypoint >= 8 && enemies.contains(paladin);
			final float health = tyrande.getLife() / tyrande.getMaximumLife();
			tyrandeRecovering = furbolg == null && waypoint >= 8
					&& health < (tyrandeRecovering ? 0.90f : 0.68f);
			for (int formation = 0; formation < force.size(); formation++) {
				final CUnit unit = force.get(formation);
				if (unit == tyrande && tyrandeRecovering) {
					if (!CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")
							&& !CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea")) {
						CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, -4768, -2528);
					}
					continue;
				}
				final CUnit enemy = paladinFight && unit != tyrande && unit.canReach(paladin, 950) ? paladin
						: enemies.stream().filter(candidate -> unit.distanceSquaredNoCollision(candidate) < 750 * 750)
								.min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
				if (unit == tyrande) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, target[0], target[1]);
				}
				else if (furbolg != null) {
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, target[0] + 88 * (formation % 4 - 1),
							target[1] + 88 * (formation / 4));
				}
				else {
					CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, target[0] + 96 * (formation % 4 - 1),
							target[1] + 96 * (formation / 4));
				}
			}
		}
		if (tick >= reportTick) {
			reportTick = tick + 400;
			final var owner = game.getPlayer(player);
			System.out.printf("[NightElfOpeningProbe] tick=%d mine=%s wisps=%d war=%s wells=%d archers=%d gold=%d wood=%d waypoint=%d Tyrande=%.0f,%.0f life=%.0f Paladin=%s%n",
					tick, entangled != null, wisps.size(), ancient != null, wells, archers, owner.getGold(), owner.getLumber(),
					waypoint, tyrande.getX(), tyrande.getY(), tyrande.getLife(),
					paladin == null ? "absent" : String.format(Locale.ROOT, "%.0f,%.0f life=%.0f",
							paladin.getX(), paladin.getY(), paladin.getLife()));
		}
		ui.getCameraManager().setTarget(tyrande.getX(), tyrande.getY());
	}

	static boolean buildNear(final War3MapViewer viewer, final CUnit wisp, final War3ID building,
			final float x, final float y) {
		final var orders = viewer.simulation.getDefaultPlayerUnitOrderExecutor(wisp.getPlayerIndex());
		final int radius = ANCIENT_OF_WAR.equals(building) ? 384 : 128;
		for (int dx = -radius; dx <= radius; dx += 128) {
			for (int dy = -radius; dy <= radius; dy += 128) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, wisp, building.getValue(), x + dx, y + dy)) {
					return true;
				}
			}
		}
		return false;
	}

	private static float[] furbolgDestination(final GlobalScope globals, final CUnit tyrande) {
		final CQuest quest = object(globals, "udg_Furbolg");
		check(quest != null && !quest.isFailed(), "the Furbolg quest failed");
		if (quest.isCompleted()) {
			if (!furbolgsVerified) {
				final CUnit reward = object(globals, "gg_unit_nfrg_0076");
				check(reward != null && reward.getPlayerIndex() == tyrande.getPlayerIndex(),
						"the Furbolg reinforcements did not transfer to Tyrande");
				System.out.println("[NightElfOpeningProbe] completed the original Furbolg quest and received reinforcements");
				furbolgsVerified = true;
			}
			return null;
		}
		final int rescued = globals.getGlobal("udg_FurbolgFound").visit(IntegerJassValueVisitor.getInstance());
		if (quest.isDiscovered() && furbolgWaypoint == 0) furbolgWaypoint = 1;
		if (furbolgWaypoint == 0) {
			final CUnit elder = object(globals, "gg_unit_nfre_0074");
			check(elder != null && !elder.isDead(), "the Furbolg elder died before the quest");
			return new float[] { Math.round(elder.getX() / 128) * 128, Math.round(elder.getY() / 128) * 128 };
		}
		if (rescued >= 10) {
			final Rectangle home = object(globals, "gg_rct_Furbolg_Return_Home_01");
			return new float[] {home.x + home.width / 2, home.y + home.height / 2};
		}
		if ("gg_rct_Furbolg_Discovered_02".equals(FURBOLG_REGIONS[furbolgWaypoint])
				&& object(globals, "udg_Furbolg03") == null) {
			// The original camp requires defeating its Raiders and approaching the captives.
			return new float[] {-1632, 2672};
		}
		Rectangle region = object(globals, FURBOLG_REGIONS[furbolgWaypoint]);
		if (furbolgWaypoint > 0 && region.contains(tyrande.getX(), tyrande.getY())) {
			furbolgWaypoint++;
			if (furbolgWaypoint == FURBOLG_REGIONS.length) furbolgWaypoint = 1;
			region = object(globals, FURBOLG_REGIONS[furbolgWaypoint]);
		}
		if (orderTick >= reportTick) {
			System.out.printf("[NightElfOpeningProbe] Furbolgs rescued=%d region=%s%n",
					rescued, FURBOLG_REGIONS[furbolgWaypoint]);
		}
		return new float[] {region.x + region.width / 2, region.y + region.height / 2};
	}

	private static Properties snapshot(final CUnit hero) {
		return CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(hero));
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobalId(name) < 0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Night Elf opening audit: " + message);
	}
}
