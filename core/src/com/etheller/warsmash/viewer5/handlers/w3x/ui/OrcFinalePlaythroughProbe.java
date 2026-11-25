package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilitySoulPossession;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;

/** Plays the original Orc finale using the authored Soul Gem and Circle of Power. */
public final class OrcFinalePlaythroughProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.orcFinaleAudit");
	private static final float[][] GROM_APPROACH = {
			{-1136, -7152}, {-1424, -6864}, {-1712, -6832}, {-1968, -6544}, {-2096, -6256},
			{-2096, -5680}, {-2096, -5104}, {-2064, -4528}, {-2064, -3952}, {-2064, -3376},
			{-2064, -3088}, {-2160, -2800}, {-2448, -2512}, {-3024, -1936}, {-3504, -1360},
			{-3632, -1072}, {-3920, -784}, {-4016, -208}, {-4048, 368}, {-4336, 656},
			{-4624, 944}, {-4848, 1232}, {-4848, 1808}, {-4912, 2096}, {-4912, 2384},
			{-4656, 2672}, {-4368, 2832}, {-4080, 3120}, {-3792, 3248}, {-3504, 3248},
			{-3216, 3280}, {-2928, 3568}, {-2640, 3632}, {-2064, 3632}, {-1488, 3632},
			{-912, 3632}, {-336, 3728}, {240, 3760}, {816, 3760}, {1392, 3856}, {1968, 4432},
			{2544, 5008}, {3120, 5584}, {3216, 5680}, {3504, 5968}, {3792, 6256}
	};
	private static final Map<Integer, Integer> waypoints = new HashMap<>();
	private static final War3ID PEON = War3ID.fromString("opeo");
	private static final War3ID BARRACKS = War3ID.fromString("obar");
	private static final War3ID GOLD_MINE = War3ID.fromString("ngol");
	private static final War3ID HEADHUNTER = War3ID.fromString("ohun");
	private static final War3ID CATAPULT = War3ID.fromString("ocat");
	private static final War3ID GRUNT = War3ID.fromString("ogru");
	private static final War3ID BURROW = War3ID.fromString("otrb");
	private static final War3ID FELHOUND = War3ID.fromString("nfel");
	private static CUnit capturedGrom;
	private static int nextOrder;
	private static int nextReport;
	private static boolean finished;
	private static boolean gromPurged;
	private static boolean thrallRecovering;
	private static int refugeStep = -1;
	private static CUnit vanguard;
	private static CUnit lumberPeon;
	private static CUnit secondLumberPeon;
	private static boolean expeditionReady;
	private static boolean escortRegrouping;
	private static int escortRallyStep;
	private static int nextDamageReport;
	private static int lastThrallDamage = -1;
	private static int nextGemReport;
	private static boolean initialGemVerified;
	private static CUnit thrallPursuer;
	private static int thrallProgressTick;
	private static float thrallProgressX;
	private static float thrallProgressY;

	private OrcFinalePlaythroughProbe() {
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED || !initialized || finished) return;
		final var game = viewer.simulation;
		final var globals = game.getGlobalScope();
		final int tick = game.getGameTurnTick();
		final CUnit thrall = object(globals, "udg_Thrall");
		final CUnit grom = object(globals, "udg_Grom");
		final CQuest quest = object(globals, "udg_SaveGromQuest");
		final CQuestItem capture = object(globals, "udg_ReqCaptureGrom");
		check(tick < 92_000 && !"Defeat".equals(ui.getScoreDialogTitle()), "the Orc finale stalled or ended in defeat");
		if (capturedGrom != null && quest != null && quest.isCompleted()) {
			if (!gromPurged && grom != null && game.containsUnit(grom) && !grom.isHidden()
					&& "Ogrh".equals(grom.getTypeId().toString()) && grom.getPlayerIndex() == thrall.getPlayerIndex()
					&& capturedGrom.getFirstAbilityOfType(CAbilitySoulPossession.class) == null
					&& !CampaignPlaythroughProbe.hasItem(thrall, "soul")) {
				gromPurged = true;
				System.out.println("[OrcFinaleProbe] purged Grom at the original Circle of Power and completed the quest");
			}
			if (gromPurged && "Victory".equals(ui.getScoreDialogTitle())) {
				final var progress = CampaignProgressStore.get().snapshot();
				check("true".equals(progress.get("campaign.4"))
						&& "true".equals(progress.get("ending." + (3L << 32)))
						&& "true".equals(progress.get("mission." + (4L << 32))),
						"the original Orc victory did not unlock the ending and the Night Elf campaign");
				System.out.println("[OrcFinaleProbe] final native Orc campaign victory and Night Elf campaign unlock");
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		if (thrall == null || grom == null || quest == null || capture == null || !quest.isDiscovered()
				|| game.isGamePaused() || !ui.isUserControlEnabled() || tick < nextOrder) return;
		nextOrder = tick + 16;
		check(!thrall.isDead(), "Thrall died during the Soul Gem expedition");
		final int player = thrall.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
		if (!initialGemVerified) {
			if (!CampaignPlaythroughProbe.hasItem(thrall, "gsou")) {
				check(tick < 12_000, "the original intro did not award the Soul Gem");
				return;
			}
			initialGemVerified = true;
			System.out.println("[OrcFinaleProbe] received the original Soul Gem from Jaina");
		}
		if (capturedGrom == null && capture.isCompleted()) {
			final CItem filled = object(globals, "udg_SoulItemVar");
			final var possession = grom.getFirstAbilityOfType(CAbilitySoulPossession.class);
			check(filled != null && filled.getContainedUnit() == thrall && possession != null && grom.isHidden(),
					"the original Soul Gem trigger did not capture Grom");
			capturedGrom = grom;
			waypoints.clear();
			thrallRecovering = false;
			System.out.println("[OrcFinaleProbe] captured Grom and obtained the original map's filled Soul Gem");
		}
		if (capturedGrom == null && !grom.isHidden() && !grom.isDead() && thrall.canReach(grom, 760)) {
			final var inventory = thrall.getInventoryData();
			final boolean canSeeGrom = grom.isVisible(game, player);
			boolean gemFound = false;
			for (int slot = 0; slot < inventory.getItemCapacity(); slot++) {
				final CItem item = inventory.getItemInSlot(slot);
				if (item == null || !"gsou".equals(item.getTypeId().toString())) continue;
				gemFound = true;
				final int gemOrder = OrderIds.itemuse00 + slot;
				if (canSeeGrom && !grom.isInvulnerable()
						&& CampaignPlaythroughProbe.targetOrder(viewer, orders, thrall, gemOrder, grom)) return;
				if (tick >= nextGemReport) {
					final var target = new ExternStringMsgTargetCheckReceiver<CWidget>();
					inventory.checkCanTarget(game, thrall, gemOrder, grom, target);
					System.out.printf("[OrcFinaleProbe] Soul Gem slot=%d charges=%d visible=%s invulnerable=%s targetError=%s%n",
							slot, item.getCharges(), canSeeGrom, grom.isInvulnerable(), target.getExternStringKey());
				}
				break;
			}
			if (tick >= nextGemReport && !gemFound) System.out.println("[OrcFinaleProbe] Thrall approached Grom without the original Soul Gem");
			if (tick >= nextGemReport) nextGemReport = tick + 120;
			if (!canSeeGrom && CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, thrall, OrderIds.farsight, grom.getX(), grom.getY())) return;
		}
		final var escort = game.getUnits().stream().filter(unit -> unit.getPlayerIndex() == player && !unit.isDead()
				&& !unit.isHidden() && !unit.isPaused() && !unit.isBuilding() && !unit.getCurrentAttacks().isEmpty()
				&& !PEON.equals(unit.getTypeId())).toList();
		if (!prepareExpedition(viewer, orders, thrall, escort, tick)) return;
		if (vanguard == null || vanguard.isDead()) {
			vanguard = escort.stream().filter(unit -> unit != thrall)
					.max(Comparator.comparingDouble(CUnit::getMaximumLife)).orElse(null);
		}
		final var enemies = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden() && unit != grom
				&& !unit.isInvulnerable() && !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
				&& unit.isVisible(game, player)).toList();
		final float[][] route = capturedGrom == null ? GROM_APPROACH : GROM_RETURN;
		final int currentStep = waypoints.getOrDefault(thrall.getHandleId(), 0);
		final boolean approachingGrom = capturedGrom == null && currentStep >= 37;
		final long nearbyEscort = escort.stream().filter(unit -> unit != thrall
				&& thrall.canReach(unit, approachingGrom ? 540 : 850)).count();
		final boolean nearbyThreat = enemies.stream().anyMatch(enemy -> thrall.canReach(enemy, 800));
		final boolean nearbyAttacker = enemies.stream().anyMatch(enemy -> !enemy.isBuilding() && !enemy.getCurrentAttacks().isEmpty()
				&& thrall.canReach(enemy, 430));
		if (thrall.distanceSquaredNoCollision(thrallProgressX, thrallProgressY) > 64 * 64) {
			thrallProgressX = thrall.getX();
			thrallProgressY = thrall.getY();
			thrallProgressTick = tick;
		}
		if (capturedGrom == null && !thrallRecovering && !escortRegrouping && !nearbyAttacker
				&& currentStep > 0 && currentStep < route.length - 2
				&& tick - thrallProgressTick >= 420 && (lastThrallDamage < 0 || tick - lastThrallDamage >= 420)
				&& thrall.canReach(route[currentStep][0], route[currentStep][1], 330)) {
			waypoints.put(thrall.getHandleId(), currentStep + 1);
			thrallProgressTick = tick;
			System.out.printf("[OrcFinaleProbe] advanced past the occupied approach waypoint %d%n", currentStep);
		}
		final int thrallStep = waypoints.getOrDefault(thrall.getHandleId(), 0);
		if (capturedGrom == null && thrallStep > 4 && (nearbyThreat || thrallStep >= 18)) {
			final int minimumEscort = Math.min(escort.size() - 1, thrallStep >= 18 ? 6 : 4);
			if (!escortRegrouping && nearbyEscort < minimumEscort) {
				final float[] approach = route[thrallStep];
				escortRallyStep = thrall.canReach(approach[0], approach[1], 380) ? thrallStep : thrallStep - 1;
			}
			escortRegrouping = nearbyEscort < Math.min(escort.size() - 1, minimumEscort + (escortRegrouping ? 1 : 0));
		}
		else escortRegrouping = false;
		if (thrall.getLife() < thrall.getMaximumLife() * (approachingGrom ? 0.78f : 0.65f) && !thrallRecovering) {
			thrallRecovering = true;
			refugeStep = Math.max(0, thrallStep - 2);
		}
		if (thrallRecovering && thrall.getLife() >= thrall.getMaximumLife() * 0.82f) {
			thrallRecovering = false;
			waypoints.put(thrall.getHandleId(), refugeStep);
		}
		final CUnit pursuer = thrallPursuer != null && enemies.contains(thrallPursuer) && thrall.canReach(thrallPursuer, 660)
				? thrallPursuer : null;
		final CUnit felhound = approachingGrom ? enemies.stream().filter(enemy -> FELHOUND.equals(enemy.getTypeId())
				&& thrall.canReach(enemy, 1000)).min(Comparator.comparingDouble(thrall::distanceSquaredNoCollision)).orElse(null) : null;
		for (final CUnit unit : escort) {
			if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
			if (unit.isHero() && unit.getLife() < unit.getMaximumLife() * 0.75f
					&& (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "pghe")
							|| CampaignPlaythroughProbe.useInventoryItem(viewer, orders, unit, "phea"))) continue;
			if (unit != thrall && felhound != null && unit.canReach(felhound, 1000)
					&& CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, felhound)) continue;
			if (CATAPULT.equals(unit.getTypeId())) {
				final CUnit tower = enemies.stream().filter(target -> target.isBuilding() && !target.getCurrentAttacks().isEmpty()
						&& unit.canReach(target, 1150)).min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
				if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, tower)) continue;
			}
			final CUnit enemy = pursuer != null && unit.canReach(pursuer, unit == thrall ? 440 : 1250) ? pursuer
							: enemies.stream().filter(target -> unit.canReach(target, unit == thrall ? 350 : 530))
									.min(Comparator.comparingDouble(CUnit::getLife)).orElse(null);
			if (unit == thrall && thrallRecovering) {
				if (felhound != null && unit.canReach(felhound, 385) && nearbyEscort >= 2
						&& thrall.getLife() > thrall.getMaximumLife() * 0.35f
						&& (HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.spiritwolf)
								|| CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.chainlightning, felhound))) continue;
				final float[] refuge = route[refugeStep];
				if ((enemy != null || nearbyAttacker) && refugeStep > 0 && unit.canReach(refuge[0], refuge[1], 160)) refugeStep--;
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, route[refugeStep][0], route[refugeStep][1]);
				continue;
			}
			if (unit == thrall && capturedGrom == null && enemy != null && !enemy.isBuilding()
					&& (HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.spiritwolf)
							|| CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.chainlightning, enemy))) continue;
			if (unit == vanguard && enemy != null && unit.canReach(enemy, 215)
					&& HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.stomp)) continue;
			if (unit != thrall && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) continue;
			if (unit == thrall && capturedGrom == null && nearbyAttacker && thrallStep >= 18 && nearbyEscort >= 5
					&& lastThrallDamage >= 0 && tick - lastThrallDamage < 100) {
				final float[] rear = route[thrallStep - 1];
				CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, rear[0], rear[1]);
				continue;
			}
			if (capturedGrom == null && escortRegrouping && !thrallRecovering) {
				if (unit == thrall) {
					if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.MOVEMENT) {
						HumanCampaignPlaythroughProbe.immediate(viewer, orders, unit, OrderIds.stop);
					}
				}
				else if (!unit.canReach(thrall, approachingGrom ? 320 : 450)) {
					final float[] rally = route[escortRallyStep];
					CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, rally[0], rally[1]);
				}
				continue;
			}
			if (capturedGrom == null && thrallRecovering) {
				CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, Math.min(route.length - 1, refugeStep + 1),
						165, waypoints);
				continue;
			}
			final int end = capturedGrom != null ? route.length - 1
					: Math.min(route.length - 1, thrallStep + (unit == thrall ? 1 : 2));
			CampaignPlaythroughProbe.followRoute(viewer, orders, unit, route, end, 165, waypoints);
		}
		ui.getCameraManager().setTarget(thrall.getX(), thrall.getY());
		if (tick >= nextReport) {
			nextReport = tick + 360;
			System.out.printf("[OrcFinaleProbe] Thrall=%.0f,%.0f life=%.0f step=%d escort=%d near=%d regroup=%s captured=%s%n",
					thrall.getX(), thrall.getY(), thrall.getLife(), thrallStep, escort.size(), nearbyEscort,
					escortRegrouping, capturedGrom != null);
		}
	}

	private static boolean prepareExpedition(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
			final CUnit thrall, final List<CUnit> escort, final int tick) {
		final var game = viewer.simulation;
		final int player = thrall.getPlayerIndex();
		final var owner = game.getPlayer(player);
		final var allies = game.getUnits().stream().filter(unit -> !unit.isDead() && unit.getPlayerIndex() == player).toList();
		final var workers = allies.stream().filter(unit -> PEON.equals(unit.getTypeId()) && !unit.isHidden() && !unit.isPaused()).toList();
		final CUnit mine = game.getUnits().stream().filter(unit -> GOLD_MINE.equals(unit.getTypeId()) && !unit.isDead())
				.min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(-1920, -8832))).orElse(null);
		if (lumberPeon == null || lumberPeon.isDead()) lumberPeon = workers.stream().findFirst().orElse(null);
		if (secondLumberPeon == null || secondLumberPeon.isDead()) {
			secondLumberPeon = workers.stream().filter(worker -> worker != lumberPeon).findFirst().orElse(null);
		}
		for (final CUnit worker : workers) {
			if (worker.getCurrentBehavior() != null && worker.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.IDLE) continue;
			if (worker == lumberPeon || worker == secondLumberPeon) {
				game.getDestructables().stream().filter(tree -> !tree.isDead() && worker.canReach(tree, 850))
						.sorted(Comparator.comparingDouble(worker::distanceSquaredNoCollision))
						.anyMatch(tree -> CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, tree));
			}
			else {
				CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, mine);
			}
		}
		final long siege = escort.stream().filter(unit -> CATAPULT.equals(unit.getTypeId())).count();
		final long ranged = escort.stream().filter(unit -> HEADHUNTER.equals(unit.getTypeId())).count();
		final long melee = escort.stream().filter(unit -> GRUNT.equals(unit.getTypeId())).count();
		if (!expeditionReady && siege >= 3 && ranged >= 7 && melee >= 7) {
			expeditionReady = true;
			System.out.println("[OrcFinaleProbe] trained the siege, ranged, and melee escort at the original fortress");
			thrall.addDamageTakenListener((simulation, attacker, target, attack, rangedHit, type, damage, bonus, actual) -> {
				final int damageTick = simulation.getGameTurnTick();
				if (attacker != null && actual > 0) {
					lastThrallDamage = damageTick;
					if (!attacker.isBuilding()) thrallPursuer = attacker;
				}
				if (attacker != null && target.getLife() < target.getMaximumLife() * 0.75f
						&& (damageTick >= nextDamageReport || target.getLife() <= 0)) {
					nextDamageReport = damageTick + 90;
					System.out.printf("[OrcFinaleProbe] Thrall took %.0f from %s at %.0f,%.0f, life=%.0f%n", actual,
							attacker.getTypeId(), attacker.getX(), attacker.getY(), target.getLife());
				}
			});
		}
		if (owner.getFoodCap() - owner.getFoodUsed() < 6 && owner.getFoodCap() < 90 && owner.getTechtreeInProgress(BURROW) == 0
				&& secondLumberPeon != null) {
			for (final float[] site : new float[][] { { -640, -8736 }, { -448, -8736 }, { -832, -9024 }, { -1216, -9216 } }) {
				if (CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, secondLumberPeon, BURROW.getValue(), site[0], site[1])) break;
			}
		}
		if (siege < 3 || ranged < 9 || melee < 7) {
			final String recruit = siege < 3 ? CATAPULT.toString() : ranged < 7 || melee >= 7 ? HEADHUNTER.toString() : GRUNT.toString();
			allies.stream().filter(unit -> BARRACKS.equals(unit.getTypeId()) && unit.getBuildQueue()[0] == null)
					.findFirst().ifPresent(unit -> HumanCampaignPlaythroughProbe.train(viewer, orders, unit, recruit));
		}
		if (!expeditionReady && tick >= nextReport) {
			nextReport = tick + 360;
			System.out.printf("[OrcFinaleProbe] preparing siege=%d ranged=%d melee=%d gold=%d lumber=%d food=%d/%d%n",
					siege, ranged, melee, owner.getGold(), owner.getLumber(), owner.getFoodUsed(), owner.getFoodCap());
		}
		return expeditionReady;
	}

	private static final float[][] GROM_RETURN = {
			{3600, 6064}, {3312, 5776}, {2736, 5200}, {2160, 4624}, {1872, 4496}, {1296, 4368},
			{720, 4176}, {144, 4176}, {-432, 4176}, {-1008, 4016}, {-1296, 3792}, {-1872, 3728},
			{-2448, 3728}, {-3024, 3728}, {-3600, 3600}, {-3888, 3440}, {-4176, 3152}, {-4464, 2864},
			{-4688, 2576}, {-4912, 2288}, {-4912, 1712}, {-4912, 1136}, {-4816, 560}, {-4560, 272},
			{-4560, -304}, {-4560, -880}, {-4560, -1456}, {-4560, -2032}, {-4560, -2608},
			{-4560, -3184}, {-4560, -3760}, {-4560, -4336}, {-4560, -4912}, {-4496, -5488},
			{-4496, -5776}, {-4336, -6064}, {-4048, -6352}, {-3984, -6640}, {-3984, -7216},
			{-4208, -7504}, {-4496, -7792}, {-4656, -8112}
	};

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Orc finale audit: " + message);
	}
}
