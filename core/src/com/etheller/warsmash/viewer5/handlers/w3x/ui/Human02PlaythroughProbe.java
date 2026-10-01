package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityQueue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.PointAbilityTargetCheckReceiver;

/** Opt-in retail Human02 audit: validated player orders and actual cinematic/menu input. */
public final class Human02PlaythroughProbe {
    private static final boolean INTERLUDE_ONLY = Boolean.getBoolean("warsmash.human02InterludeAudit");
    private static final boolean ENABLED = Boolean.getBoolean("warsmash.human02PlaythroughAudit") || INTERLUDE_ONLY;
    private static final boolean FULL = Boolean.getBoolean("warsmash.humanCampaignAudit") || Boolean.getBoolean("warsmash.human02OptionalAudit");
    private static final String PHASE = System.getProperty("warsmash.human02PlaythroughPhase", "");
    private static final float[][] ROUTE = { {-2000, -2400}, {-896, -1376}, {384, -512},
            {1536, 512}, {2400, 1344}, {3104, 2112} };
    private static int stage = INTERLUDE_ONLY ? 3 : 0;
    private static int project, waypoint, orderTick, reportTick, optionalStep, optionalWaypoint;
    private static long started;
    private static boolean introSkipped, bladeSkipped, victorySkipped, buildIssued, interludeSeen;
    private static CUnit builder;
    private static StoredUnitData expectedHero;
    private static String pendingCapture;
    private static int captureFrames;
    private static boolean checkpointSaved;
    private Human02PlaythroughProbe() { }

    public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
        if (!ENABLED || stage >= 5 || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
        final String map = viewer.getCurrentMapPath().toLowerCase();
        if (map.contains("human01")) return;
        if (started == 0) started = System.nanoTime();
        check(System.nanoTime() - started < 1_500_000_000_000L, "Timed out at stage=" + stage + " project=" + project + " waypoint=" + waypoint);
        final var game = viewer.simulation;
        final var globals = game.getGlobalScope();
        if (map.contains("human02interlude")) {
            check(stage >= 3, "Interlude loaded before mission victory");
            if (!interludeSeen) { interludeSeen = true; capture("interlude"); System.out.println("[Human02PlaythroughProbe] retail story interlude loaded"); }
            // The story script advances naturally; its own victory dialog remains real UI.
            continueVictory(ui);
            return;
        }
        final CUnit arthas = object(globals, "udg_Arthas");
        if (arthas == null) return;
        if (map.contains("human03")) {
            check(interludeSeen, "Human03 bypassed the story interlude");
            if (INTERLUDE_ONLY) {
                capture("human03");
                stage = 5;
                System.out.println("[Human02PlaythroughProbe] complete: standalone story interlude and Human03");
                return;
            }
            check(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(arthas))
                    .equals(CampaignPlaythroughProbe.heroProperties(expectedHero)), "Human03 lost earned hero progression or inventory");
            PlayerProfileManager.loadFromGdx();
            CampaignProgressStore.get().seedCampaignEntries(1, 12);
            // The retail menu includes Human02Interlude at index 2, Human03 at 3.
            check(CampaignProgressStore.get().isMissionAvailable(1, 2)
                    && CampaignProgressStore.get().isMissionAvailable(1, 3)
                    && !CampaignProgressStore.get().isMissionAvailable(1, 4), "Interlude/Human03 unlock persistence is wrong");
            writeHero("human03", CGameSave.snapshotUnit(arthas));
            capture("human03");
            System.out.println("[Human02PlaythroughProbe] complete: real economy, main objectives, Blademaster combat, victory menus, interlude and Human03 carryover; xp=" + expectedHero.xp);
            stage = 5;
            return;
        }
        check(map.contains("human02"), "Unexpected map " + map);
        if (stage == 0 && PHASE.equals("read")) {
            final Properties saved = properties("driver");
            check(game.getMissionCheckpoint().isLoading() && game.getGameTurnTick() == Integer.parseInt(saved.getProperty("tick")), "Reader did not reconstruct the quest checkpoint tick");
            stage = 2; project = 3; optionalStep = 3; optionalWaypoint = 0;
            orderTick = Integer.parseInt(saved.getProperty("orderTick"));
            builder = object(globals, "udg_Lumber02"); introSkipped = bladeSkipped = checkpointSaved = true;
            check(completed(globals, "udg_QuestBase") && !completed(globals, "udg_QuestSearinox") && CampaignPlaythroughProbe.hasItem(arthas,"sehr"), "Searinox checkpoint lost objective/inventory state");
            check(properties("checkpoint-hero").equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(arthas))), "Checkpoint hero changed");
            System.out.println("[Human02PlaythroughProbe] Searinox checkpoint restored tick=" + game.getGameTurnTick());
        }
        if (stage == 0) {
            if (!flag(globals, "udg_IntroCinematicDone")) {
                final Trigger intro = object(globals, "gg_trg_Intro_Cinematic_Cancel");
                if (!introSkipped && intro != null && intro.isEnabled()) { introSkipped = true; ui.keyDown(Input.Keys.ESCAPE); }
                return;
            }
            if (game.getGameTurnTick() < 60) return; // Let the retail worker replacement and auto-harvest orders finish.
            builder = object(globals, "udg_Lumber02");
            check(builder != null && !builder.isDead(), "No ordinary peasant available to build");
            stage = 1;
            System.out.println("[Human02PlaythroughProbe] ordinary economy started; hero xp=" + arthas.getHeroData().getXp());
        }
        if (stage <= 2) {
            check(!arthas.isDead(), "Arthas died in ordinary combat");
            if (FULL && stage==2 && ui.isUserControlEnabled() && !arthas.isPaused()) ui.getCameraManager().setTarget(arthas.getX(),arthas.getY());
            if (game.getGameTurnTick() >= reportTick) {
                reportTick = game.getGameTurnTick() + 200;
                System.out.println("[Human02PlaythroughProbe] tick=" + game.getGameTurnTick() + " stage=" + stage
                        + " project=" + project + " waypoint=" + waypoint + " optional=" + optionalStep + "/" + optionalWaypoint + " gold=" + game.getPlayer(1).getGold()
                        + " lumber=" + game.getPlayer(1).getLumber() + " trained=" + number(globals, "udg_FootmenTrained")
                        + " farms=" + number(globals, "udg_NumberOfFarms") + " hero=" + arthas.getX() + "," + arthas.getY()
                        + " life=" + arthas.getLife() + " army=" + army(viewer).size()
                        + " behavior=" + arthas.getCurrentBehavior() + " order=" + arthas.getCurrentOrder()
                        + " builder=" + builder.getCurrentBehavior());
                if (FULL && stage==2 && pendingCapture==null) capture("optional-progress");
            }
            if (flag(globals, "udg_GameOver")) {
                final CUnit blade = object(globals, "udg_Blademaster");
                check(blade.isDead() && completed(globals, "udg_QuestBase") && completed(globals, "udg_QuestBlademaster"), "Victory did not complete both main quests");
                check(number(globals, "udg_FootmenTrained") == 6 && number(globals, "udg_NumberOfFarms") == 2, "Construction/training requirements changed");
                if (FULL) check(completed(globals,"udg_QuestSearinox") && CampaignPlaythroughProbe.hasItem(arthas,"ofir") && !CampaignPlaythroughProbe.hasItem(arthas,"sehr"), "Searinox quest/reward incomplete");
                expectedHero = CGameSave.snapshotUnit(arthas);
                if (PHASE.equals("write")) properties("outcome",CampaignPlaythroughProbe.heroProperties(expectedHero));
                if (PHASE.equals("read")) check(properties("outcome").equals(CampaignPlaythroughProbe.heroProperties(expectedHero)), "Resumed earned hero differs from uninterrupted play");
                writeHero("human02-victory", expectedHero);
                stage = 3;
                System.out.println("[Human02PlaythroughProbe] actual mission victory; trained=6 farms=2 xp=" + expectedHero.xp);
            }
            else {
                final Trigger bladeScene = object(globals, "gg_trg_Blademaster_Cancel");
                if (!bladeSkipped && bladeScene != null && bladeScene.isEnabled()) { bladeSkipped = true; ui.keyDown(Input.Keys.ESCAPE); return; }
                if (!ui.isUserControlEnabled() || arthas.isPaused() || game.isGamePaused() || game.getGameTurnTick() < orderTick) return;
                orderTick = game.getGameTurnTick() + 20;
                final var orders = new CPlayerUnitOrderExecutor(game, viewer.getLocalPlayerIndex());
                if (stage == 1) {
                    economy(viewer, orders);
                    if (completed(globals, "udg_QuestBase") && flag(globals, "udg_BlademasterQuestRecieved")) {
                        stage = 2;
                        capture("base-objectives");
                        System.out.println("[Human02PlaythroughProbe] barracks, two farms and six trained footmen completed through ordinary orders");
                    }
                }
                if (stage == 2 && FULL && optionalStep < 4 && optional(viewer, ui, orders, arthas)) return;
                micro(viewer, orders, arthas);
            }
        }
        if (stage >= 3) {
            final Trigger victory = object(globals, "gg_trg_Victory_Cinematic_Cancel");
            if (!victorySkipped && victory != null && victory.isEnabled()) { victorySkipped = true; ui.keyDown(Input.Keys.ESCAPE); }
            continueVictory(ui);
        }
    }

    private static void economy(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders) {
        final var game = viewer.simulation;
        final String[] structures = { "hbar", "hhou", "hhou" };
        final int[] desired = { 1, 1, 2 };
        if (project < structures.length) {
            final War3ID type = War3ID.fromString(structures[project]);
            final long complete = game.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == 1
                    && !u.isDead() && !u.isConstructing() && u.getTypeId().equals(type)).count();
            if (complete >= desired[project]) { project++; buildIssued = false; }
            else if (!buildIssued) {
                final var ability = builder.getFirstAbilityOfType(AbstractCAbilityBuild.class);
                final var activation = new BooleanAbilityActivationReceiver();
                ability.checkCanUse(game, builder, type.getValue(), activation);
                if (activation.isOk()) {
                    for (int radius = 0; radius <= 768 && !buildIssued; radius += 64) {
                        for (int dx = -radius; dx <= radius && !buildIssued; dx += 64) {
                            for (int dy = -radius; dy <= radius && !buildIssued; dy += 64) {
                                if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                                final var target = new PointAbilityTargetCheckReceiver();
                                ability.checkCanTarget(game, builder, type.getValue(), new AbilityPointTarget(-3008 + project * 256 + dx, -4672 + dy), target);
                                if (target.getTarget() != null) {
                                    orders.issuePointOrder(builder.getHandleId(), ability.getHandleId(), type.getValue(), target.getTarget().getX(), target.getTarget().getY(), false);
                                    buildIssued = true;
                                    System.out.println("[Human02PlaythroughProbe] ordinary build ordered " + type + " at " + target.getTarget());
                                }
                            }
                        }
                    }
                }
            }
        }
        final War3ID footman = War3ID.fromString("hfoo");
        final CUnit barracks = game.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == 1
                && !u.isDead() && !u.isConstructing() && u.getTypeId().equals(War3ID.fromString("hbar"))).findFirst().orElse(null);
        if (barracks != null && number(game.getGlobalScope(), "udg_FootmenTrained")
                + Arrays.stream(barracks.getBuildQueue()).filter(footman::equals).count() < 6) {
            final var ability = barracks.getFirstAbilityOfType(CAbilityQueue.class);
            final var activation = new BooleanAbilityActivationReceiver();
            ability.checkCanUse(game, barracks, footman.getValue(), activation);
            if (activation.isOk()) orders.issueImmediateOrder(barracks.getHandleId(), ability.getHandleId(), footman.getValue(), false);
        }
    }

    private static void micro(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit arthas) {
        final var game = viewer.simulation;
        for (final String skill : new String[] { "AHds", "AHhb", "AHad" }) {
            final int id = War3ID.fromString(skill).getValue();
            final var activation = new BooleanAbilityActivationReceiver();
            arthas.getHeroData().checkCanUse(game, arthas, id, activation);
            if (activation.isOk()) { orders.issueImmediateOrder(arthas.getHandleId(), arthas.getHeroData().getHandleId(), id, false); break; }
        }
        boolean casting = false;
        if (!arthas.isInvulnerable() && arthas.getLife() < arthas.getMaximumLife() * 0.6f) {
            for (final var ability : arthas.getAbilities()) {
                if (!ability.getAlias().equals(War3ID.fromString("AHds"))) continue;
                final var activation = new BooleanAbilityActivationReceiver();
                ability.checkCanUse(game, arthas, OrderIds.divineshield, activation);
                if (activation.isOk()) { orders.issueImmediateOrder(arthas.getHandleId(), ability.getHandleId(), OrderIds.divineshield, false); casting = true; }
            }
        }
        final var army = army(viewer);
        final var injured = army.stream().filter(u -> u != arthas && u.getMaximumLife() - u.getLife() > CampaignPlaythroughProbe.healingThreshold(arthas)
                && arthas.distanceSquaredNoCollision(u) < 600 * 600).max(Comparator.comparingDouble(u -> u.getMaximumLife() - u.getLife())).orElse(null);
        if (!casting && injured != null) casting = CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt, injured);
        final float[] optionalDestination = stage == 2 && FULL && optionalStep < 4 ? optionalDestination(viewer, arthas) : null;
        if (stage == 2 && optionalDestination == null && waypoint < ROUTE.length && arthas.distanceSquaredNoCollision(ROUTE[waypoint][0], ROUTE[waypoint][1]) < 220 * 220) waypoint++;
        for (final var unit : army) {
            if (unit.isPaused() || (unit == arthas && casting)) continue;
            final var enemy = game.getUnitsIncludingPending().stream().filter(u -> !u.isDead() && !u.isHidden() && !u.isInvulnerable()
                    && u.getPlayerIndex() != unit.getPlayerIndex() && !game.getPlayer(unit.getPlayerIndex()).hasAlliance(u.getPlayerIndex(), CAllianceType.PASSIVE)
                    && arthas.distanceSquaredNoCollision(u) < 550 * 550)
                    .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
            if (enemy != null) {
                if (unit == arthas && CampaignPlaythroughProbe.supportOrder(viewer, orders, arthas, enemy)) continue;
                if (unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.ATTACK
                        && !CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)
                        && optionalDestination != null) CampaignPlaythroughProbe.pointOrder(viewer,orders,unit,optionalDestination[0],optionalDestination[1]);
            }
            else if (stage == 1) CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, -3200, -3850);
            else if (optionalDestination != null) CampaignPlaythroughProbe.pointOrder(viewer,orders,unit,optionalDestination[0],optionalDestination[1]);
            else if (waypoint < ROUTE.length) CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, ROUTE[waypoint][0], ROUTE[waypoint][1]);
            else CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, object(game.getGlobalScope(), "udg_Blademaster"));
        }
    }

    private static boolean optional(final War3MapViewer viewer, final MeleeUI ui, final CPlayerUnitOrderExecutor orders, final CUnit arthas) {
        final var globals=viewer.simulation.getGlobalScope();
        final CQuest quest=object(globals,"udg_QuestSearinox");
        final CUnit searinox=object(globals,"udg_Searinox");
        final CItem heart=object(globals,"udg_HeartofSearinox");
        if (optionalStep == 0 && quest.isDiscovered()) { optionalStep=1; optionalWaypoint=0; System.out.println("[Human02PlaythroughProbe] Searinox quest discovered; retail riflemen recruited"); }
        if (optionalStep == 1 && searinox.isDead() && heart!=null) { optionalStep=2; optionalWaypoint=0; System.out.println("[Human02PlaythroughProbe] Searinox defeated through ranged combat"); }
        if (optionalStep == 2) {
            if (CampaignPlaythroughProbe.hasItem(arthas,"sehr")) {
                optionalStep=3; optionalWaypoint=0;
                if (PHASE.equals("write") && !checkpointSaved) {
                    ((com.etheller.warsmash.parsers.fdf.frames.SimpleButtonFrame)viewer.getGameUI().getFrameByName("UpperButtonBarMenuButton",0)).onClick(Input.Buttons.LEFT);
                    ((com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame)viewer.getGameUI().getFrameByName("SaveGameButton",0)).onClick(Input.Buttons.LEFT);
                    ((com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame)viewer.getGameUI().getFrameByName("ReturnButton",0)).onClick(Input.Buttons.LEFT);
                    final CGameSave save=CGameSave.tryLoad(new File(com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionSaveStore.currentDirectory(),"QuickSave.w3s"));
                    check(save!=null && save.checkpoint!=null && save.checkpoint.targetTick==viewer.simulation.getGameTurnTick(),"Quest checkpoint not saved");
                    final Properties driver=new Properties(); driver.setProperty("tick",Integer.toString(viewer.simulation.getGameTurnTick())); driver.setProperty("orderTick",Integer.toString(orderTick));
                    properties("driver",driver); properties("checkpoint-hero",CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(arthas)));
                    checkpointSaved=true;
                    System.out.println("[Human02PlaythroughProbe] actual Searinox Quick Save tick="+save.checkpoint.targetTick+" state="+save.checkpoint.stateFingerprint);
                    return true;
                }
            } else { CampaignPlaythroughProbe.targetOrder(viewer,orders,arthas,OrderIds.smart,heart); return true; }
        }
        if (optionalStep == 3 && quest.isCompleted()) {
            check(CampaignPlaythroughProbe.hasItem(arthas,"ofir") && !CampaignPlaythroughProbe.hasItem(arthas,"sehr"),"Searinox reward not exchanged");
            optionalStep=4; System.out.println("[Human02PlaythroughProbe] Searinox heart returned; Orb of Fire earned");
        }
        return false;
    }
    private static float[] optionalDestination(final War3MapViewer viewer, final CUnit arthas) {
        final float[][] route = optionalStep == 0 ? new float[][]{{-2000,-3800},{-600,-3500},{0,-2700}}
            : optionalStep == 1 ? new float[][]{{800,-3600},{2800,-3600},{3800,-4300},{3500,-5500},{2672,-5808}}
            : new float[][]{{3500,-5500},{3800,-4300},{2800,-3600},{800,-3600},{0,-2700}};
        if (optionalWaypoint < route.length && arthas.distanceSquaredNoCollision(route[optionalWaypoint][0],route[optionalWaypoint][1])<220*220) optionalWaypoint++;
        return route[Math.min(optionalWaypoint,route.length-1)];
    }
    private static Properties properties(final String name) {
        final Properties value=new Properties();
        try(var in=Files.newInputStream(Path.of(System.getProperty("user.home"),"human02",name+".properties"))) { value.load(in); return value; }
        catch(IOException e) { throw new IllegalStateException(e); }
    }
    private static void properties(final String name, final Properties value) {
        final Path file=Path.of(System.getProperty("user.home"),"human02",name+".properties");
        try { Files.createDirectories(file.getParent()); try(var out=Files.newOutputStream(file)) { value.store(out,"Searinox playthrough checkpoint evidence"); } }
        catch(IOException e) { throw new IllegalStateException(e); }
    }

    private static List<CUnit> army(final War3MapViewer viewer) {
        return viewer.simulation.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == viewer.getLocalPlayerIndex()
                && !u.isDead() && !u.isHidden() && !u.isBuilding() && u.getFirstAbilityOfType(CAbilityAttack.class) != null
                && u.getFirstAbilityOfType(AbstractCAbilityBuild.class) == null).toList();
    }
    private static void continueVictory(final MeleeUI ui) {
        if (pendingCapture != null) return;
        if (ui.getVisibleScriptDialog() != null) ui.getVisibleScriptDialog().getButtons().get(0).getButtonFrame().onClick(Input.Buttons.LEFT);
        else if ("Victory".equals(ui.getScoreDialogTitle())) ui.keyDown(Input.Keys.C);
    }
    private static void writeHero(final String name, final StoredUnitData hero) {
        final Path file = Path.of(System.getProperty("user.home"), "human02", name + ".properties");
        try {
            Files.createDirectories(file.getParent());
            try (var stream = Files.newOutputStream(file)) { CampaignPlaythroughProbe.heroProperties(hero).store(stream, "Earned retail campaign hero"); }
        }
        catch (final IOException error) { throw new IllegalStateException(error); }
    }
    private static void capture(final String name) { pendingCapture = name; captureFrames = 0; }
    public static void afterUIRender() {
        if (!ENABLED) return;
        if (pendingCapture != null && ++captureFrames >= (pendingCapture.equals("interlude") || pendingCapture.equals("human03") ? 120 : 3)) {
            final var directory = Gdx.files.absolute(Path.of(System.getProperty("user.home"), "human02").toString());
            directory.mkdirs();
            final Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            final PixmapIO.PNG writer = new PixmapIO.PNG();
            try { writer.setFlipY(true); writer.write(directory.child(pendingCapture + ".png"), pixels); }
            catch (final IOException error) { throw new IllegalStateException(error); }
            finally { writer.dispose(); pixels.dispose(); }
            System.out.println("[Human02PlaythroughProbe] captured " + pendingCapture);
            pendingCapture = null;
        }
        if (stage == 5 && pendingCapture == null && !Boolean.getBoolean("warsmash.humanCampaignAudit")) Gdx.app.exit();
    }
    private static boolean completed(final GlobalScope globals, final String name) { final CQuest q = object(globals, name); return q != null && q.isCompleted(); }
    private static boolean flag(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(BooleanJassValueVisitor.getInstance()); }
    private static int number(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(IntegerJassValueVisitor.getInstance()); }
    private static <T> T object(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance()); }
    private static void check(final boolean condition, final String message) { if (!condition) throw new IllegalStateException("Human02 playthrough audit: " + message); }
}
