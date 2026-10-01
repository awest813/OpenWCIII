package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.List;
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
    private static int stage, waypoint, optionalStep, campWaypoint, orderTick, reportTick;
    private static int strengthBeforeReturn;
    private static long started;
    private static War3MapViewer firstViewer;
    private static StoredUnitData expectedHero;
    private static boolean travelSeen, introSkipped, orcSkipped, victorySkipped;
    private static boolean checkpointSaved;
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
                final CUnit injured = army.stream().filter(u -> u != arthas && u.getMaximumLife() - u.getLife() > healingThreshold(arthas) && arthas.distanceSquaredNoCollision(u) < 600 * 600)
                        .max(java.util.Comparator.comparingDouble(u -> u.getMaximumLife() - u.getLife())).orElse(null);
                final boolean healing = !shielding && injured != null && targetOrder(viewer, orders, arthas, OrderIds.holybolt, injured);
                final boolean previouslySaved = checkpointSaved;
                final float[] optionalDestination = FULL && waypoint == 2 && optionalStep < 6 ? optionalDestination(viewer, ui, arthas, orders) : null;
                // Stop at the captured boundary. Both branches issue their next orders
                // after the same saved input interval rather than only the writer moving now.
                if (!previouslySaved && checkpointSaved) return;
                if (optionalDestination == null && waypoint < ROUTE.length && arthas.distanceSquaredNoCollision(ROUTE[waypoint][0], ROUTE[waypoint][1]) < 220 * 220) {
                    waypoint++;
                    System.out.println("[CampaignPlaythroughProbe] reached waypoint " + waypoint);
                }
                for (final var unit : army) {
                    if (unit.isPaused() || (unit == arthas && (healing || shielding
                            || (optionalStep == 4 && unit.getCurrentOrder() != null && unit.getCurrentOrder().getTarget(game) instanceof CItem)))) continue;
                    final var enemyNearby = game.getUnitsIncludingPending().stream().filter(u -> !u.isDead() && !u.isHidden()
                            && !u.isInvulnerable() && u.getPlayerIndex() != unit.getPlayerIndex()
                            && !game.getPlayer(unit.getPlayerIndex()).hasAlliance(u.getPlayerIndex(), com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType.PASSIVE)
                            && arthas.distanceSquaredNoCollision(u) < 400 * 400)
                            .min(java.util.Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
                    if (enemyNearby != null) {
                        if (unit == arthas && supportOrder(viewer, orders, arthas, enemyNearby)) continue;
                        if (unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.ATTACK) targetOrder(viewer, orders, unit, OrderIds.attack, enemyNearby);
                        continue;
                    }
                    if (optionalDestination != null) pointOrder(viewer, orders, unit, optionalDestination[0], optionalDestination[1]);
                    else if (waypoint < ROUTE.length) pointOrder(viewer, orders, unit, ROUTE[waypoint][0], ROUTE[waypoint][1]);
                    else {
                        final List<CUnit> crew = object(globals, "udg_WarlordAndCrewGroup");
                        final var enemy = crew.stream().filter(u -> !u.isDead()).min(java.util.Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
                        if (enemy != null) targetOrder(viewer, orders, unit, OrderIds.attack, enemy);
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
                ((com.etheller.warsmash.parsers.fdf.frames.SimpleButtonFrame) viewer.getGameUI().getFrameByName("UpperButtonBarMenuButton", 0)).onClick(Input.Buttons.LEFT);
                ((com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame) viewer.getGameUI().getFrameByName("SaveGameButton", 0)).onClick(Input.Buttons.LEFT);
                ((com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame) viewer.getGameUI().getFrameByName("ReturnButton", 0)).onClick(Input.Buttons.LEFT);
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
        for (final var ability : hero.abilities) data.setProperty("ability." + ability.abilityId, Integer.toString(ability.level));
        for (int i = 0; i < hero.items.length; i++) data.setProperty("item." + i, hero.items[i] == null ? "empty" : hero.items[i].typeId + ":" + hero.items[i].charges);
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
    static void pointOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit, final float x, final float y) {
        if (unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.MOVEMENT
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
    static boolean targetOrder(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit, final int orderId, final CWidget target) {
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
