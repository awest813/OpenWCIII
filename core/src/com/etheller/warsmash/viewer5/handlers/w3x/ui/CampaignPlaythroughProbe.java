package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.util.Properties;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.PointAbilityTargetCheckReceiver;

/** Opt-in Human01 playthrough. Only ordinary player orders and UI input mutate gameplay. */
public final class CampaignPlaythroughProbe {
    private static final boolean ENABLED = Boolean.getBoolean("warsmash.campaignPlaythroughAudit");
    private static final boolean FULL = Boolean.getBoolean("warsmash.campaignPlaythroughFull");
    private static final String PHASE = System.getProperty("warsmash.campaignPlaythroughPhase", "");
    private static final float[][] ROUTE = { {-2400, 1480}, {-896, -32}, {240, 1984}, {2048, 560}, {80, 3856} };
    private static final float[][] CAMP_ROAD = { {-768, -3328}, {384, -1408}, {1632, -2656}, {2912, -2656}, {3616, -3360}, {3616, -4192} };
    private static final float[] WARLORD_RALLY = {1408, 2240};
    private static final float[] WARLORD_FALLBACK = {-896, -32};
    private static final float[] WARLORD_SOUTH_FALLBACK = {284, -1225};
    private static final float[][] WARLORD_RETREAT = {WARLORD_RALLY, WARLORD_SOUTH_FALLBACK,
            WARLORD_FALLBACK, ROUTE[0]};
    private static final String[] SUPPLY_CRATES = {"gg_dest_LTcr_0287", "gg_dest_LTcr_1991"};
    private static final String[] SUPPLY_ITEMS = {"phea", "spro"};
    private static int stage, waypoint, optionalStep, campWaypoint, orderTick, reportTick;
    private static int strengthBeforeReturn;
    private static long started;
    private static War3MapViewer firstViewer;
    private static StoredUnitData expectedHero;
    private static boolean travelSeen, introSkipped, orcSkipped, victorySkipped;
    private static boolean checkpointSaved;
    private static boolean retreatingFromWarlord;
    private static int retreatWaypoint;
    private static boolean restedAfterLedger;
    private static int supplyIndex;
    private CampaignPlaythroughProbe() { }

    public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
        if (!ENABLED || stage >= 5) return;
        if (started == 0) started = System.nanoTime();
        check(System.nanoTime() - started < (FULL ? 1_020_000_000_000L : 600_000_000_000L), "Timed out at stage " + stage + " waypoint " + waypoint + " optional " + optionalStep);
        if (!initialized) return;
        final var game = viewer.simulation;
        if (game.getMissionCheckpoint().isRestoring()) return;
        final var globals = game.getGlobalScope();
        final CUnit arthas = object(globals, "udg_Arthas");
        if (arthas == null) return;
        if (stage == 0) {
            check(viewer.getCurrentMapPath().toLowerCase().contains("human01"), "Expected Human01");
            if (PHASE.equals("read")) {
                final Properties saved = readProperties("driver.properties");
                check(game.getMissionCheckpoint().isLoading(), "Reader did not load a checkpoint");
                check(game.getGameTurnTick() == Integer.parseInt(saved.getProperty("tick")), "Checkpoint tick changed");
                waypoint = Integer.parseInt(saved.getProperty("waypoint"));
                optionalStep = Integer.parseInt(saved.getProperty("optionalStep"));
                strengthBeforeReturn = Integer.parseInt(saved.getProperty("strengthBeforeReturn"));
                orderTick = Integer.parseInt(saved.getProperty("orderTick"));
                introSkipped = true;
                orcSkipped = Boolean.parseBoolean(saved.getProperty("orcSkipped"));
                travelSeen = Boolean.parseBoolean(saved.getProperty("travelSeen"));
                checkpointSaved = true;
                check(heroProperties(CGameSave.snapshotUnit(arthas)).equals(readProperties("checkpoint-hero.properties")), "Checkpoint hero or inventory changed");
                check(questCompleted(globals, "udg_QuestTimmy") && !questCompleted(globals, "udg_QuestLedger")
                        && hasItem(arthas, "rde0") && hasItem(arthas, "ledg"), "Optional quest checkpoint changed");
                firstViewer = viewer;
                stage = 1;
                System.out.println("[CampaignPlaythroughProbe] optional-quest checkpoint restored tick=" + game.getGameTurnTick());
            }
            else {
            if (!flag(globals, "udg_HasArthasArrived")) {
                final Trigger intro = object(globals, "gg_trg_Intro_Cancel");
                if (!introSkipped && intro != null && intro.isEnabled()) { introSkipped = true; ui.keyDown(Input.Keys.ESCAPE); }
                return;
            }
            check(!CampaignProgressStore.get().isMissionAvailable(1, 1), "Fresh campaign has Human02 unlocked");
            firstViewer = viewer;
            stage = 1;
            System.out.println("[CampaignPlaythroughProbe] ordinary player-order playthrough started");
            }
        }
        if (stage == 1) {
            check(!arthas.isDead(), "Arthas died during ordinary combat");
            final CQuestItem travel = object(globals, "udg_RequirementVillageTravel");
            if (travel != null && travel.isCompleted() && !travelSeen) {
                travelSeen = true;
                System.out.println("[CampaignPlaythroughProbe] travel objective completed through walking and retail Warlord orders");
            }
            if (flag(globals, "udg_GAMEOVER")) {
                check(travelSeen, "Victory skipped the travel objective");
                final List<CUnit> crew = object(globals, "udg_WarlordAndCrewGroup");
                check(!crew.isEmpty() && crew.stream().allMatch(CUnit::isDead), "Victory before all enemies died");
                expectedHero = CGameSave.snapshotUnit(arthas);
                check(expectedHero.xp > 0, "Combat did not award experience");
                if (FULL) {
                    check(questCompleted(globals, "udg_QuestTimmy") && questCompleted(globals, "udg_QuestLedger"), "Optional quests unfinished at victory");
                    check(hasItem(arthas, "rde0") && !hasItem(arthas, "ledg") && flag(globals, "udg_IsLedgerReturned"), "Optional rewards or ledger return missing");
                    check(expectedHero.strengthBase == strengthBeforeReturn + 1 && !hasItem(arthas, "tstr"), "Strength tome was not consumed exactly once");
                    if (PHASE.equals("write")) writeProperties("outcome.properties", heroProperties(expectedHero));
                    if (PHASE.equals("read")) check(heroProperties(expectedHero).equals(readProperties("outcome.properties")), "Resumed earned hero differs from uninterrupted play");
                }
                stage = 2;
                System.out.println("[CampaignPlaythroughProbe] actual combat victory; xp=" + expectedHero.xp + " crew=" + crew.size());
            }
            else {
                if (game.getGameTurnTick() >= reportTick) {
                    reportTick = game.getGameTurnTick() + 200;
                    System.out.println("[CampaignPlaythroughProbe] tick=" + game.getGameTurnTick() + " waypoint=" + waypoint
                            + " hero=" + arthas.getX() + "," + arthas.getY() + " life=" + arthas.getLife()
                            + " target=" + targetDescription(arthas)
                            + " army=" + army(viewer).size() + " travel=" + travelSeen + " optional=" + optionalStep
                            + " timmy=" + questCompleted(globals, "udg_QuestTimmy") + " ledger=" + questCompleted(globals, "udg_QuestLedger")
                            + " behavior=" + arthas.getCurrentBehavior());
                }
                final Trigger orcScene = object(globals, "gg_trg_Orc_Cinematic_Cancel");
                if (!orcSkipped && orcScene != null && orcScene.isEnabled()) { orcSkipped = true; ui.keyDown(Input.Keys.ESCAPE); return; }
                if (!ui.isUserControlEnabled() || arthas.isPaused() || game.isGamePaused() || game.getGameTurnTick() < orderTick) return;
                orderTick = game.getGameTurnTick() + 20;
                final var orders = new CPlayerUnitOrderExecutor(game, viewer.getLocalPlayerIndex());
                final var heroAbility = arthas.getHeroData();
                if (FULL && waypoint == 0 && collectSupplies(viewer, orders, arthas)) return;
                for (final String skill : new String[] { "AHhb", "AHds", "AHad" }) {
                    final int id = War3ID.fromString(skill).getValue();
                    final var activation = new BooleanAbilityActivationReceiver();
                    heroAbility.checkCanUse(game, arthas, id, activation);
                    if (activation.isOk()) { orders.issueImmediateOrder(arthas.getHandleId(), heroAbility.getHandleId(), id, false); break; }
                }
                boolean shielding = false;
                if (!arthas.isInvulnerable() && arthas.getLife() < arthas.getMaximumLife() * 0.6f) {
                    for (final var ability : arthas.getAbilities()) {
                        if (!ability.getAlias().equals(War3ID.fromString("AHds"))) continue;
                        final var activation = new BooleanAbilityActivationReceiver();
                        ability.checkCanUse(game, arthas, OrderIds.divineshield, activation);
                        if (activation.isOk()) {
                            orders.issueImmediateOrder(arthas.getHandleId(), ability.getHandleId(), OrderIds.divineshield, false);
                            shielding = true;
                            System.out.println("[CampaignPlaythroughProbe] Divine Shield ordered at tick " + game.getGameTurnTick());
                        }
                    }
                }
                final var army = army(viewer);
                if (arthas.getLife() < arthas.getMaximumLife() * 0.5f
                        && (useInventoryItem(viewer, orders, arthas, "phea")
                                || useInventoryItem(viewer, orders, arthas, "pghe")
                                || useInventoryItem(viewer, orders, arthas, "shea"))) return;
                if (arthas.getMana() < arthas.getMaximumMana() * 0.25f
                        && useInventoryItem(viewer, orders, arthas, "pman")) return;
                if (army.stream().filter(unit -> unit.getMaximumLife() - unit.getLife() > 90
                        && arthas.distanceSquaredNoCollision(unit) < 500 * 500).count() >= 2
                        && useInventoryItem(viewer, orders, arthas, "shea")) return;
                if (travelSeen || waypoint >= ROUTE.length - 1 || FULL && optionalStep >= 6) {
                    final List<CUnit> crew = object(globals, "udg_WarlordAndCrewGroup");
                    if (crew.stream().anyMatch(unit -> !unit.isDead()
                            && arthas.distanceSquaredNoCollision(unit) < 620 * 620)
                            && useInventoryItem(viewer, orders, arthas, "spro")) return;
                    final float health = arthas.getLife() / arthas.getMaximumLife();
                    final boolean retreat = health < (retreatingFromWarlord ? 0.87f : 0.57f);
                    if (retreat && !retreatingFromWarlord) {
                        retreatWaypoint = arthas.getY() > 1800 ? 0 : 1;
                    }
                    retreatingFromWarlord = retreat;
                }
                if ((waypoint < 2 || optionalStep >= 6) && !retreatingFromWarlord && !shielding) {
                    final CItem dropped = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
                            && item.getContainedInventory() == null && arthas.distanceSquaredNoCollision(item) < 280 * 280)
                            .filter(item -> List.of("phea", "pman", "shea").contains(item.getTypeId().toString()))
                            .min(java.util.Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
                    if (dropped != null && targetOrder(viewer, orders, arthas, OrderIds.smart, dropped)) return;
                }
                final float healAt = Math.min(healingThreshold(arthas), 105);
                final CUnit injured = army.stream().filter(u -> u != arthas && u.getMaximumLife() - u.getLife() > healAt
                        && arthas.distanceSquaredNoCollision(u) < 600 * 600)
                        .max(java.util.Comparator.comparingDouble(u -> u.getMaximumLife() - u.getLife())).orElse(null);
                final boolean healing = !shielding && !retreatingFromWarlord && injured != null
                        && targetOrder(viewer, orders, arthas, OrderIds.holybolt, injured);
                final boolean previouslySaved = checkpointSaved;
                final float[] optionalDestination = FULL && waypoint == 2 && optionalStep < 6 ? optionalDestination(viewer, ui, arthas, orders) : null;
                // Stop at the captured boundary. Both branches issue their next orders
                // after the same saved input interval rather than only the writer moving now.
                if (!previouslySaved && checkpointSaved) return;
                if (optionalDestination == null && waypoint < ROUTE.length && arthas.distanceSquaredNoCollision(ROUTE[waypoint][0], ROUTE[waypoint][1]) < 220 * 220) {
                    waypoint++;
                    System.out.println("[CampaignPlaythroughProbe] reached waypoint " + waypoint);
                }
                final var opposition = game.getUnitsIncludingPending().stream().filter(u -> !u.isDead() && !u.isHidden()
                        && !u.isInvulnerable() && u.getPlayerIndex() != arthas.getPlayerIndex()
                        && !game.getPlayer(arthas.getPlayerIndex()).hasAlliance(u.getPlayerIndex(),
                                com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType.PASSIVE)).toList();
                final CUnit pursuingEnemy = opposition.stream().filter(enemy -> !enemy.getCurrentAttacks().isEmpty()
                        && enemy.isVisible(game, arthas.getPlayerIndex()) && arthas.distanceSquaredNoCollision(enemy) < 720 * 720)
                        .min(java.util.Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
                if (retreatingFromWarlord && retreatWaypoint < WARLORD_RETREAT.length - 1) {
                    final float[] refuge = WARLORD_RETREAT[retreatWaypoint];
                    if (arthas.distanceSquaredNoCollision(refuge[0], refuge[1]) < 300 * 300
                            && opposition.stream().anyMatch(enemy -> !enemy.getCurrentAttacks().isEmpty()
                                    && arthas.distanceSquaredNoCollision(enemy) < 560 * 560)) {
                        retreatWaypoint++;
                    }
                }
                if (FULL && optionalStep >= 6 && !restedAfterLedger && !travelSeen
                        && arthas.getLife() >= arthas.getMaximumLife() * 0.95f
                        && arthas.getMana() >= arthas.getMaximumMana() * 0.75f) restedAfterLedger = true;
                for (int position = 0; position < army.size(); position++) {
                    final var unit = army.get(position);
                    if (unit.isPaused() || (unit == arthas && (healing || shielding
                            || (optionalStep == 4 && unit.getCurrentOrder() != null && unit.getCurrentOrder().getTarget(game) instanceof CItem)))) continue;
                    if (retreatingFromWarlord || FULL && optionalStep >= 6 && !restedAfterLedger && !travelSeen) {
                        final float[] rally = retreatingFromWarlord ? WARLORD_RETREAT[retreatWaypoint] : WARLORD_SOUTH_FALLBACK;
                        if (unit == arthas && retreatingFromWarlord && arthas.isInvulnerable() && pursuingEnemy != null
                                && arthas.distanceSquaredNoCollision(pursuingEnemy) < 240 * 240
                                && targetOrder(viewer, orders, arthas, OrderIds.attack, pursuingEnemy)) continue;
                        if (unit != arthas && retreatingFromWarlord && pursuingEnemy != null
                                && unit.getLife() > unit.getMaximumLife() * 0.5f
                                && unit.distanceSquaredNoCollision(pursuingEnemy) < 480 * 480
                                && targetOrder(viewer, orders, unit, OrderIds.attack, pursuingEnemy)) continue;
                        final int column = unit == arthas ? 0 : position % 3 - 1;
                        final int row = unit == arthas ? 0 : 1 + position / 3;
                        pointOrder(viewer, orders, unit, rally[0] + column * 84, rally[1] + row * 82);
                        continue;
                    }
                    final var enemyNearby = opposition.stream().filter(u -> unit.distanceSquaredNoCollision(u) < 500 * 500
                            && u.isVisible(game, unit.getPlayerIndex()))
                            .min(java.util.Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
                    if (enemyNearby != null) {
                        if (FULL && unit != arthas && unit.getLife() < unit.getMaximumLife() * 0.45f) {
                            final double direction = enemyNearby.angleTo(arthas);
                            final float x = arthas.getX() + (float) Math.cos(direction) * (160 + position % 3 * 56);
                            final float y = arthas.getY() + (float) Math.sin(direction) * (160 + position % 3 * 56);
                            pointOrder(viewer, orders, unit, x, y);
                            continue;
                        }
                        if (unit == arthas && supportOrder(viewer, orders, arthas, enemyNearby)) continue;
                        if (unit.getCurrentBehavior() == null || unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.ATTACK) targetOrder(viewer, orders, unit, OrderIds.attack, enemyNearby);
                        continue;
                    }
                    final float[] destination = optionalDestination != null ? optionalDestination
                            : waypoint < ROUTE.length ? ROUTE[waypoint] : null;
                    if (destination != null) {
                        if (unit == arthas || waypoint < 1) {
                            pointOrder(viewer, orders, unit, destination[0], destination[1]);
                        }
                        else {
                            final int column = (position % 3) - 1;
                            final int row = position / 3;
                            attackMoveOrder(viewer, orders, unit, destination[0] + column * 80,
                                    destination[1] + 88 + row * 76);
                        }
                    }
                    else {
                        final List<CUnit> crew = object(globals, "udg_WarlordAndCrewGroup");
                        final var enemy = crew.stream().filter(u -> !u.isDead() && !u.isHidden())
                                .min(java.util.Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
                        if (enemy != null && !targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) {
                            // The native target order requires vision. Advance until the remaining crew is visible.
                            attackMoveOrder(viewer, orders, unit, enemy.getX(), enemy.getY());
                        }
                    }
                }
            }
        }
        if (stage == 2) {
            final Trigger victory = object(globals, "gg_trg_Victory_Cancel");
            if (!victorySkipped && victory != null && victory.isEnabled()) { victorySkipped = true; ui.keyDown(Input.Keys.ESCAPE); }
            final CQuestItem slay = object(globals, "udg_RequirementVillageSlay");
            final CQuestItem alive = object(globals, "udg_RequirementVillageArthas");
            if (slay == null || !slay.isCompleted() || !alive.isCompleted() || ui.getVisibleScriptDialog() == null) return;
            check(CampaignProgressStore.get().isMissionAvailable(1, 1), "Victory did not unlock Human02");
            ui.getVisibleScriptDialog().getButtons().get(0).getButtonFrame().onClick(Input.Buttons.LEFT);
            stage = 3;
        }
        else if (stage == 3 && "Victory".equals(ui.getScoreDialogTitle())) { stage = 4; ui.keyDown(Input.Keys.C); }
        else if (stage == 4 && viewer != firstViewer) {
            check(viewer.getCurrentMapPath().toLowerCase().contains("human02"), "Next chapter did not load");
            final var restored = CGameSave.snapshotUnit(arthas);
            check(restored.xp == expectedHero.xp && restored.skillPoints == expectedHero.skillPoints, "Earned hero progression lost");
            check(heroProperties(restored).equals(heroProperties(expectedHero)), "Hero stats, skills or inventory changed in Human02");
            for (final var skill : expectedHero.abilities) {
                check(java.util.Arrays.stream(restored.abilities).anyMatch(a -> a.abilityId.equals(skill.abilityId) && a.level == skill.level), "Learned ability lost");
            }
            for (int slot = 0; slot < expectedHero.items.length; slot++) {
                final var item = expectedHero.items[slot];
                final var carried = restored.items[slot];
                check(item == null ? carried == null : carried != null && item.typeId.equals(carried.typeId) && item.charges == carried.charges, "Inventory changed");
            }
            PlayerProfileManager.loadFromGdx();
            CampaignProgressStore.get().seedCampaignEntries(1, 12);
            check(CampaignProgressStore.get().isMissionAvailable(1, 1) && !CampaignProgressStore.get().isMissionAvailable(1, 2)
                    && !CampaignProgressStore.get().isMissionAvailable(1, 3), "Unlock persistence is wrong");
            System.out.println("[CampaignPlaythroughProbe] complete: Human01 " + (FULL ? "main and optional objectives" : "main objectives")
                    + " through normal orders, victory menus, earned carryover, Human02 and persistent unlock; phase=" + PHASE);
            if (!Boolean.getBoolean("warsmash.human02PlaythroughAudit")) Gdx.app.exit();
            stage = 5;
        }
    }

    private static boolean collectSupplies(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit hero) {
        final var game = viewer.simulation;
        while (supplyIndex < SUPPLY_CRATES.length && hasItem(hero, SUPPLY_ITEMS[supplyIndex])) {
            supplyIndex++;
        }
        if (supplyIndex >= SUPPLY_CRATES.length) return false;
        final CDestructable crate = object(game.getGlobalScope(), SUPPLY_CRATES[supplyIndex]);
        if (crate == null) return false;
        if (!crate.isDead()) {
            if (hero.getCurrentOrder() != null && hero.getCurrentOrder().getTarget(game) == crate) return true;
            return targetOrder(viewer, orders, hero, OrderIds.attack, crate);
        }
        final String itemId = SUPPLY_ITEMS[supplyIndex];
        final CItem supply = game.getItems().stream().filter(item -> !item.isDead() && !item.isHidden()
                && item.getContainedInventory() == null && itemId.equals(item.getTypeId().toString())
                && item.distanceSquaredNoCollision(crate) < 192 * 192).findFirst().orElse(null);
        return supply != null && targetOrder(viewer, orders, hero, OrderIds.smart, supply);
    }

    static boolean useInventoryItem(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit hero, final String itemId) {
        final var inventory = hero.getInventoryData();
        if (inventory == null) return false;
        for (int slot = 0; slot < inventory.getItemCapacity(); slot++) {
            final CItem item = inventory.getItemInSlot(slot);
            if (item == null || !itemId.equals(item.getTypeId().toString())) continue;
            final int use = OrderIds.itemuse00 + slot;
            final var activation = new BooleanAbilityActivationReceiver();
            inventory.checkCanUse(viewer.simulation, hero, use, activation);
            if (!activation.isOk()) continue;
            orders.issueImmediateOrder(hero.getHandleId(), inventory.getHandleId(), use, false);
            return true;
        }
        return false;
    }

    private static float[] optionalDestination(final War3MapViewer viewer, final MeleeUI ui, final CUnit arthas, final CPlayerUnitOrderExecutor orders) {
        final var globals = viewer.simulation.getGlobalScope();
        check(!flag(globals, "udg_TimmyQuestFailed") && !flag(globals, "udg_BanditQuestFailed"), "Optional quest failed");
        if (optionalStep == 0 && questDiscovered(globals, "udg_QuestLedger")) { optionalStep++; System.out.println("[CampaignPlaythroughProbe] ledger quest discovered through walking"); }
        if (optionalStep == 1 && questDiscovered(globals, "udg_QuestTimmy")) { optionalStep++; System.out.println("[CampaignPlaythroughProbe] Timmy quest discovered through walking"); }
        if (optionalStep == 2 && questCompleted(globals, "udg_QuestTimmy") && hasItem(arthas, "rde0")) { optionalStep++; System.out.println("[CampaignPlaythroughProbe] Timmy rescued through combat; ring earned"); }
        final CUnit menag = object(globals, "udg_Menag");
        final CItem ledger = object(globals, "udg_Ledger");
        if (optionalStep == 3 && menag.isDead() && ledger != null) { optionalStep++; System.out.println("[CampaignPlaythroughProbe] Menag defeated through combat; ledger dropped"); }
        if (optionalStep == 4 && hasItem(arthas, "ledg")) {
            optionalStep++;
            System.out.println("[CampaignPlaythroughProbe] ledger picked up through ordinary inventory order");
            if (PHASE.equals("write") && !checkpointSaved) {
                check(ui.keyDown(Input.Keys.F6), "F6 did not invoke Quick Save");
                final CGameSave checkpoint = CGameSave.tryLoad(new File(com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionSaveStore.currentDirectory(), "QuickSave.w3s"));
                check(checkpoint != null && checkpoint.checkpoint != null && checkpoint.checkpoint.targetTick == viewer.simulation.getGameTurnTick(), "Quick Save did not capture the actual mission tick");
                final Properties saved = new Properties();
                saved.setProperty("tick", Integer.toString(viewer.simulation.getGameTurnTick()));
                saved.setProperty("waypoint", Integer.toString(waypoint));
                saved.setProperty("optionalStep", Integer.toString(optionalStep));
                saved.setProperty("strengthBeforeReturn", Integer.toString(arthas.getHeroData().getStrength().getBase()));
                saved.setProperty("orderTick", Integer.toString(orderTick));
                saved.setProperty("orcSkipped", Boolean.toString(orcSkipped));
                saved.setProperty("travelSeen", Boolean.toString(travelSeen));
                writeProperties("driver.properties", saved);
                writeProperties("checkpoint-hero.properties", heroProperties(CGameSave.snapshotUnit(arthas)));
                checkpointSaved = true;
                System.out.println("[CampaignPlaythroughProbe] actual mid-mission Quick Save tick=" + viewer.simulation.getGameTurnTick() + " state=" + checkpoint.checkpoint.stateFingerprint);
            }
            strengthBeforeReturn = arthas.getHeroData().getStrength().getBase();
        }
        if (optionalStep == 5 && questCompleted(globals, "udg_QuestLedger")) { optionalStep++; System.out.println("[CampaignPlaythroughProbe] ledger returned; strength reward earned"); }
        switch (optionalStep) {
        case 0: return new float[] { 284, -1225 };
        case 1: return new float[] { -2948, -4232 };
        case 2: return new float[] { 384, -4128 };
        case 3:
            if (campWaypoint < CAMP_ROAD.length && arthas.distanceSquaredNoCollision(CAMP_ROAD[campWaypoint][0], CAMP_ROAD[campWaypoint][1]) < 220 * 220) campWaypoint++;
            return campWaypoint < CAMP_ROAD.length ? CAMP_ROAD[campWaypoint] : new float[] { menag.getX(), menag.getY() };
        case 4:
            targetOrder(viewer, orders, arthas, OrderIds.smart, ledger);
            return new float[] { ledger.getX(), ledger.getY() };
        case 5: return new float[] { 284, -1225 };
        default: return null;
        }
    }
    static boolean hasItem(final CUnit unit, final String id) {
        final var inventory = unit.getInventoryData();
        if (inventory == null) return false;
        for (int i = 0; i < inventory.getItemCapacity(); i++) {
            final CItem item = inventory.getItemInSlot(i);
            if (item != null && item.getTypeId().equals(War3ID.fromString(id))) return true;
        }
        return false;
    }
    private static boolean questCompleted(final GlobalScope globals, final String name) { final CQuest quest = object(globals, name); return quest != null && quest.isCompleted(); }
    private static boolean questDiscovered(final GlobalScope globals, final String name) { final CQuest quest = object(globals, name); return quest != null && quest.isDiscovered(); }
    static Properties heroProperties(final StoredUnitData hero) {
        final Properties data = new Properties();
        data.setProperty("type", hero.unitTypeId.toString());
        data.setProperty("xp", Integer.toString(hero.xp)); data.setProperty("points", Integer.toString(hero.skillPoints)); data.setProperty("name", hero.properName);
        data.setProperty("stats", hero.strengthBase + "," + hero.agilityBase + "," + hero.intelligenceBase + "," + hero.strengthBonus + "," + hero.agilityBonus + "," + hero.intelligenceBonus);
        if (hero.abilities != null) {
            for (final var ability : hero.abilities) data.setProperty("ability." + ability.abilityId, Integer.toString(ability.level));
        }
        final int slots = hero.items == null ? 6 : hero.items.length;
        for (int i = 0; i < slots; i++) {
            final var item = hero.items == null ? null : hero.items[i];
            data.setProperty("item." + i, item == null ? "empty" : item.typeId + ":" + item.charges);
        }
        return data;
    }
    private static File evidenceFile(final String name) { return new File(System.getProperty("user.home"), "playthrough/" + name); }
    private static Properties readProperties(final String name) {
        final Properties data = new Properties();
        try (var stream = Files.newInputStream(evidenceFile(name).toPath())) { data.load(stream); return data; }
        catch (final java.io.IOException error) { throw new IllegalStateException(error); }
    }
    private static void writeProperties(final String name, final Properties data) {
        try { Files.createDirectories(evidenceFile(name).toPath().getParent());
            try (var stream = Files.newOutputStream(evidenceFile(name).toPath())) { data.store(stream, "Ordinary-order campaign audit evidence"); }
        } catch (final java.io.IOException error) { throw new IllegalStateException(error); }
    }
    private static String targetDescription(final CUnit unit) {
        if (unit.getCurrentBehavior() instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CRangedBehavior ranged
                && ranged.getTarget() instanceof CUnit target) return target.getTypeId() + ":" + target.getPlayerIndex() + ":" + target.getLife() + ":" + target.getX() + "," + target.getY() + ":inv=" + target.isInvulnerable();
        return "none";
    }
    private static List<CUnit> army(final War3MapViewer viewer) {
        return viewer.simulation.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == viewer.getLocalPlayerIndex()
                && !u.isDead() && !u.isHidden() && !u.isBuilding() && u.getFirstAbilityOfType(CAbilityAttack.class) != null).toList();
    }
    static int healingThreshold(final CUnit hero) {
        final var holy = hero.getFirstAbilityOfType(com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityHolyLight.class);
        return holy == null || holy.getLevel() == 1 ? 140 : 250;
    }
    /** Keep an injured, vulnerable hero behind the front line while the troops fight. */
    static boolean supportOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit hero, final CUnit enemy) {
        if (hero.isInvulnerable() || hero.getLife() >= hero.getMaximumLife() * 0.55f) return false;
        final float dx = hero.getX() - enemy.getX(), dy = hero.getY() - enemy.getY();
        final float distance = (float) Math.sqrt(dx * dx + dy * dy);
        final float safeDistance = Math.max(350, enemy.getUnitSpecificAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(150) + 200);
        if (distance < safeDistance - 50) {
            final float scale = safeDistance / Math.max(1, distance);
            pointOrder(viewer, orders, hero, enemy.getX() + dx * scale, enemy.getY() + dy * scale);
        }
        // Preserve a safe position rather than re-entering melee during Shield's cooldown.
        return true;
    }
    static void followRoute(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit,
            final float[][] route, final int end, final float arrivalRadius, final Map<Integer, Integer> waypoints) {
        int next = Math.min(end, waypoints.computeIfAbsent(unit.getHandleId(), handle -> {
            int nearest = 0;
            for (int index = 1; index <= end; index++) {
                final float[] point = route[index];
                final float[] previous = route[nearest];
                if (unit.distanceSquaredNoCollision(point[0], point[1]) < unit.distanceSquaredNoCollision(previous[0], previous[1])) {
                    nearest = index;
                }
            }
            return nearest;
        }));
        if (next < end && unit.distanceSquaredNoCollision(route[next][0], route[next][1]) < arrivalRadius * arrivalRadius) next++;
        waypoints.put(unit.getHandleId(), next);
        pointOrder(viewer, orders, unit, route[next][0], route[next][1]);
    }

    static void pointOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit, final float x, final float y) {
        if (unit.distanceSquaredNoCollision(x, y) < 1 && unit.getCurrentBehavior() != null
                && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.IDLE) return;
        if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.MOVEMENT
                && unit.getCurrentOrder() instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint point
                && point.getTarget(viewer.simulation).getX() == x && point.getTarget(viewer.simulation).getY() == y) return;
        final var ability = unit.getFirstAbilityOfType(CAbilityMove.class);
        if (ability == null) return;
        final var activation = new BooleanAbilityActivationReceiver();
        ability.checkCanUse(viewer.simulation, unit, OrderIds.move, activation);
        if (!activation.isOk()) return;
        final var target = new PointAbilityTargetCheckReceiver();
        ability.checkCanTarget(viewer.simulation, unit, OrderIds.move, new AbilityPointTarget(x, y), target);
        if (target.getTarget() != null) orders.issuePointOrder(unit.getHandleId(), ability.getHandleId(), OrderIds.move, x, y, false);
    }
    static boolean attackMoveOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit unit, final float x, final float y) {
        return attackPointOrder(viewer, orders, unit, OrderIds.attack, x, y);
    }
    static boolean attackPointOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit unit, final int orderId, final float x, final float y) {
        final var game = viewer.simulation;
        if (unit.getCurrentOrder() instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint point
                && point.getOrderId() == orderId && point.getTarget(game).getX() == x
                && point.getTarget(game).getY() == y) return true;
        final var ability = unit.getFirstAbilityOfType(CAbilityAttack.class);
        if (ability == null) return false;
        final var activation = new BooleanAbilityActivationReceiver();
        ability.checkCanUse(game, unit, orderId, activation);
        if (!activation.isOk()) return false;
        final var target = new PointAbilityTargetCheckReceiver();
        ability.checkCanTarget(game, unit, orderId, new AbilityPointTarget(x, y), target);
        if (target.getTarget() == null) return false;
        orders.issuePointOrder(unit.getHandleId(), ability.getHandleId(), orderId, x, y, false);
        return true;
    }
    static boolean pointAbilityOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit unit, final int orderId, final float x, final float y) {
        final var game = viewer.simulation;
        if (unit.getCurrentOrder() != null && unit.getCurrentOrder().getOrderId() == orderId
                && unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.IDLE) {
            return true;
        }
        for (final var ability : unit.getAbilities()) {
            final var activation = new BooleanAbilityActivationReceiver();
            ability.checkCanUse(game, unit, orderId, activation);
            if (!activation.isOk()) continue;
            final var target = new PointAbilityTargetCheckReceiver();
            ability.checkCanTarget(game, unit, orderId, new AbilityPointTarget(x, y), target);
            if (target.getTarget() != null) {
                orders.issuePointOrder(unit.getHandleId(), ability.getHandleId(), orderId,
                        target.getTarget().getX(), target.getTarget().getY(), false);
                return true;
            }
        }
        return false;
    }
    static boolean targetOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit, final int orderId, final CWidget target) {
        if (target == null) return false;
        if (unit.getCurrentOrder() instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetWidget order
                && order.getOrderId() == orderId && order.getTarget(viewer.simulation) == target) return true;
        for (final var ability : unit.getAbilities()) {
            final var activation = new BooleanAbilityActivationReceiver();
            ability.checkCanUse(viewer.simulation, unit, orderId, activation);
            if (!activation.isOk()) continue;
            final var receiver = new CWidgetAbilityTargetCheckReceiver();
            ability.checkCanTarget(viewer.simulation, unit, orderId, target, receiver);
            if (receiver.getTarget() != null) { orders.issueTargetOrder(unit.getHandleId(), ability.getHandleId(), orderId, target.getHandleId(), false); return true; }
        }
        return false;
    }
    private static boolean flag(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(BooleanJassValueVisitor.getInstance()); }
    private static <T> T object(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance()); }
    private static void check(final boolean condition, final String message) { if (!condition) throw new IllegalStateException("Campaign playthrough audit: " + message); }
}
