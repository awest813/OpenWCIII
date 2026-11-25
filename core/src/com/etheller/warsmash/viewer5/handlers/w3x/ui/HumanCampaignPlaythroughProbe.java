package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorHumanBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityQueue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.PointAbilityTargetCheckReceiver;

/** Objective-driven retail Human campaign audit. All gameplay changes use validated player orders. */
public final class HumanCampaignPlaythroughProbe {
    private static final boolean ENABLED = Boolean.getBoolean("warsmash.humanCampaignAudit");
    private static final String STOP = System.getProperty("warsmash.humanCampaignStop", "human04").toLowerCase();
    private static final boolean REQUIRE_CARAVAN = Boolean.parseBoolean(
            System.getProperty("warsmash.humanCampaignRequireCaravan", "true"));
    private static final float[][] HUMAN03 = {
        {3360,-3200}, {3000,-1800}, {4300,-700}, {6400,700}, // Discover the Fountain quest.
        {4000,600}, {5000,2200}, {3500,3600}, {2000,4000}, {1000,3500}, {0,2400}, {-1300,2500}, {-3400,2300},
        {-5200,2600}, {-6800,3800}, {-5300,2500}, {-3500,1500},
        {-2700,-600}, {-2200,-3000}, // The priests discover the infected granary.
        {-4100,-2450}, {-5400,-4100}, {-6200,-3000}, {-6100,-1600}, {-6200,600}
    };
    private static String mission = "";
    private static int waypoint, orderTick, reportTick;
    private static int frostmourneRetreatWaypoint = -1;
    private static boolean victory, finished;
    private static boolean regrouping;
    private static boolean citadelMarching;
    private static boolean resupplyingShipAssault;
    private static boolean recoveringShipAssault;
    private static int frostRegroupTick;
    private static int frostAssaultSize;
    private static int valleyLootHandle;
    private static int valleyLootDeadline;
    private static final Set<Integer> skippedValleyLoot = new HashSet<>();
    private static int andorhalRegroupTick;
    private static int andorhalAssaultSize;
    private static boolean muradinRecovering;
    private static boolean arthasRecovering;
    private static long started;
    private static final Map<String, Properties> expectedHeroes = new HashMap<>();
    private static CUnit human05RestoredJaina;
    private static Properties human05JainaBeforeSetup;
    private static String capture;
    private static int captureFrames;
    private static CUnit builder;
    private static CUnit objectiveTarget;
    private static CUnit canyonSapper;
    private static int canyonBlast;
    private static boolean canyonDetonating;
    private static final Map<Integer, Integer> cullingRetryTick = new HashMap<>();
    private static final Map<Integer, Integer> hearthglenCaravanSteps = new HashMap<>();
    private static int cullingTargetProgressTick;
    private static float cullingTargetLife;
    private static final Set<Integer> assignedWorkers = new HashSet<>();
    private static final Set<Integer> visitedMercenaryCamps = new HashSet<>();
    private static final float[][] HUMAN04 = {{-2300,-3500},{-3500,-2300},{-4200,-800},{-4200,1100}};
    private static final float[] ANDORHAL_EXPANSION = {3840, -3328};
    private static final float[] HEARTHGLEN_RALLY = {5600, -4900};
    private static final float[][] HEARTHGLEN_CARAVAN_ROUTE = {
            {5264,-4912}, {4688,-4912}, {4112,-4912}, {3536,-4912}, {3248,-5264}, {2688,-5312}, {2384,-4912},
            {1808,-4912}, {1232,-4912}, {656,-4912}, {80,-4912}, {-496,-4912}, {-1072,-4912},
            {-1648,-4912}, {-2224,-4912}, {-2800,-4912}, {-3376,-4912}, {-3952,-4784}, {-4528,-4208},
            {-5104,-3632}, {-5680,-3056}, {-5968,-2768}, {-6256,-2480}
    };
    private static final float[] NORTHREND_OUTPOST = {4736, 4736};
    private static final float[] FROSTMOURNE_RALLY = {-3328, -6080};
    private static final float[][] FROSTMOURNE_VALLEY = {{-6200,-1056},{-6016,1600},{-5328,3696},{-6192,5280},
            {-5000,6304},{-3408,5840},{-2880,6368}};
    private static final float[] MURADIN_SHELTER = {-6320, -2416};
    private static boolean observingMuradin;
    private static final float[] FROSTMOURNE_DEFENSE = {-3136, -6080};
    private static final float[] FROSTMOURNE_EXPANSION = {-512, -2688};
    private static final float[] FROSTMOURNE_FORWARD_RALLY = {-960, -2896};
    private static final Set<String> MALGANIS_PRODUCTION = Set.of("unpl", "unp1", "unp2", "usep", "utod", "ugrv", "ubon", "uaod");
    private static final Set<String> VALLEY_LOOT = Set.of("rde2", "prvt", "pgma", "tdex", "pghe");
    private static final int CULLING_STALLED_TARGET_TICKS = 800;
    private static final int CULLING_RETRY_DELAY_TICKS = 2400;
    private static final String[] HUMAN08_SHIPS = {
            null, "gg_unit_nbsp_0108", null, "gg_unit_nbsp_0098", null, null, null, null, null,
            "gg_unit_nbsp_0110", "gg_unit_nbsp_0111", null, null, null, null, null, null, null, "gg_unit_nbsp_0112"
    };
    private static final float[][] HUMAN08_ROUTE = {
            {-1728,4288},{-4720,752},{-3536,-4512},{-3824,-3536},{-1456,336},{-1840,-816},{-624,-96},
            {-592,1136},{-1440,1632},
            {-304,-336},{2064,-4432},{2480,-4432},{3600,-5552},{4048,-6448},
            {4592,-6448},{5040,-6000},{5040,-3024},{4304,-2288},{4432,-496}
    };
    private static final int HUMAN08_CANYON_RALLY = 5;
    private static final int HUMAN08_MERCENARY_RALLY = 8;
    private static final int HUMAN08_SOUTHERN_COAST = 11;
    private static final Set<String> HUMAN08_RANGED_MERCENARIES = Set.of("nitr", "nitp", "nitt", "nith", "nits");
    private static final float[][] HUMAN08_BLASTS = {{-1552,-704},{-1344,-672},{-1248,-496}};
    private HumanCampaignPlaythroughProbe() { }

    public static void observeHeroRestore(final String missionKey, final String heroKey, final CUnit unit) {
        if (ENABLED && "Human05".equalsIgnoreCase(missionKey) && "Jaina".equalsIgnoreCase(heroKey)) {
            human05RestoredJaina = unit;
            human05JainaBeforeSetup = CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit));
        }
    }

    public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
        if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
        final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase();
        final String name = map.substring(map.lastIndexOf('/') + 1).replace(".w3m", "");
        if (name.equals("human01") || name.equals("human02") || name.equals("human02interlude")) return;
        final var game = viewer.simulation;
        final var globals = game.getGlobalScope();
        if (!mission.equals(name)) {
            mission = name; waypoint = orderTick = reportTick = 0; victory = regrouping = false; started = System.nanoTime(); builder=null; objectiveTarget=null; assignedWorkers.clear();
            frostRegroupTick = frostAssaultSize = andorhalRegroupTick = andorhalAssaultSize = 0;
            valleyLootHandle = valleyLootDeadline = 0;
            skippedValleyLoot.clear();
            cullingRetryTick.clear();
            hearthglenCaravanSteps.clear();
            visitedMercenaryCamps.clear();
            canyonBlast = 0;
            canyonSapper = null;
            canyonDetonating = false;
            muradinRecovering = false;
            arthasRecovering = false;
            citadelMarching = false;
            resupplyingShipAssault = false;
            recoveringShipAssault = false;
            boolean verifiedCarryover = false;
            if (!name.contains("interlude")) for (final var hero : expectedHeroes.entrySet()) {
                if (name.equals("human06") && hero.getKey().equals("Jaina")) continue; // Jaina leaves in the preceding story scene.
                final CUnit unit = hero(globals, hero.getKey());
                if (name.equals("human05") && hero.getKey().equals("Jaina")) {
                    check(unit != null && unit == human05RestoredJaina && hero.getValue().equals(human05JainaBeforeSetup),
                            "human05 lost Jaina carryover before the chapter's hero setup");
                    check(unit.getHeroData().getHeroLevel() >= 6 && unit.getAbilityLevel(War3ID.fromString("AHmt")) >= 1,
                            "human05 did not grant Jaina the scripted Mass Teleport skill");
                    writeHeroProperties(name + "-Jaina-before-setup", human05JainaBeforeSetup);
                    human05RestoredJaina = null;
                    human05JainaBeforeSetup = null;
                }
                else {
                    check(unit != null && hero.getValue().equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit))),
                            name + " lost " + hero.getKey() + " carryover");
                }
                writeHero(name + "-" + hero.getKey() + "-restored", unit);
                verifiedCarryover = true;
            }
            if (!name.contains("interlude")) expectedHeroes.clear();
            System.out.println("[HumanCampaignProbe] entered " + name + (verifiedCarryover ? "; carryover verified" : ""));
            if (name.equals(STOP)) {
                capture = name + "-arrival"; captureFrames = 0; finished = true;
                return;
            }
        }
        check(System.nanoTime() - started < 3_600_000_000_000L, "Timed out in " + name + " waypoint=" + waypoint);
        if ("Defeat".equals(ui.getScoreDialogTitle())) throw new IllegalStateException("Human campaign audit: defeat in " + name);
        if (name.contains("interlude")) { continueVictory(ui); return; }
        final CUnit arthas = hero(globals, "Arthas");
        if (arthas == null) return;
        if (name.equals("human03")) {
            skip(globals, ui, "gg_trg_OpeningCancelled", "gg_trg_GrainCinematicCancelled", "gg_trg_EncounterCancelled");
            if (flag(globals, "udg_GameOver")) {
                check(object(globals, "gg_unit_ngwr_0074", CUnit.class).isDead(), "Granary did not die through combat");
                if (!completed(globals, "udg_QuestDestroyGranary")) return;
                if (!victory) {
                    check(completed(globals, "udg_QuestInvestigateVillages") && completed(globals, "udg_QuestFountain"), "Human03 objectives incomplete");
                    remember(globals, "Arthas", "Jaina"); victory = true;
                    capture = name + "-victory"; captureFrames = 0;
                    System.out.println("[HumanCampaignProbe] objectives complete " + name + "; main villages/granary and optional fountain; xp=" + arthas.getHeroData().getXp());
                }
                skip(globals, ui, "gg_trg_EndingCancelled"); continueVictory(ui); return;
            }
            check(!arthas.isDead(), "Arthas died in " + name);
            final CUnit jaina = object(globals, "udg_Jaina");
            check(jaina == null || !jaina.isDead(), "Jaina died in " + name);
            if (!ui.isUserControlEnabled() || arthas.isPaused() || game.isGamePaused() || game.getGameTurnTick() < orderTick) return;
            orderTick = game.getGameTurnTick() + 20;
            final int arrivalRadius = waypoint == 8 ? 320 : 260;
            if (waypoint < HUMAN03.length && arthas.distanceSquaredNoCollision(HUMAN03[waypoint][0], HUMAN03[waypoint][1])
                    < arrivalRadius * arrivalRadius) waypoint++;
            final float[] destination = waypoint < HUMAN03.length ? HUMAN03[waypoint] : new float[]{-6240,1184};
            micro(viewer, arthas, destination);
            ui.getCameraManager().setTarget(arthas.getX(), arthas.getY());
            if (game.getGameTurnTick() >= reportTick) {
                reportTick = game.getGameTurnTick() + 400;
                System.out.println("[HumanCampaignProbe] " + name + " tick=" + game.getGameTurnTick() + " waypoint=" + waypoint
                    + " hero=" + arthas.getX() + "," + arthas.getY() + " life=" + arthas.getLife() + " army=" + army(viewer).size()
                    + " fountain=" + completed(globals,"udg_QuestFountain") + " villages=" + completed(globals,"udg_QuestInvestigateVillages"));
            }
        }
        else if (name.equals("human04")) {
            skip(globals,ui,"gg_trg_OpeningCancelled","gg_trg_GranaryCancelled");
            if (game.getGameTurnTick() >= reportTick) {
                System.out.println("[HumanCampaignProbe] opening skipped="+flag(globals,"udg_OpeningSkipped")+" cancel="+object(globals,"gg_trg_OpeningCancelled",Trigger.class).isEnabled()+" opening01="+object(globals,"gg_trg_Opening01",Trigger.class).isEnabled()+" opening02="+object(globals,"gg_trg_Opening02",Trigger.class).isEnabled()+" control="+ui.isUserControlEnabled());
                report(viewer,ui,arthas);
            }
            if (flag(globals,"udg_GAMEOVER")) {
                final CUnit kel=object(globals,"gg_unit_uktn_0133");
                final CUnit cinematicKel = object(globals, "udg_KelThuzad");
                check(kel != null && cinematicKel != null && kel != cinematicKel
                        && (kel.isDead() || game.getUnit(kel.getHandleId()) == null), "Human04 ended before Kel'Thuzad was defeated");
                check(completed(globals, "udg_QuestAndorhal"), "Human04 defeated Kel'Thuzad before completing the investigation");
                if (!victory) { remember(globals,"Arthas","Jaina"); victory=true; capture=name+"-victory"; captureFrames=0; System.out.println("[HumanCampaignProbe] objectives complete "+name+"; Andorhal and Kel'Thuzad; xp="+arthas.getHeroData().getXp()); }
                skip(globals,ui,"gg_trg_EndingCancelled"); continueVictory(ui); return;
            }
            check(!arthas.isDead(), "Arthas died in " + name);
            final CQuest andorhal=object(globals,"udg_QuestAndorhal");
            if (andorhal==null || !andorhal.isDiscovered() || !ui.isUserControlEnabled() || arthas.isPaused() || game.isGamePaused() || game.getGameTurnTick()<orderTick) return;
            orderTick=game.getGameTurnTick()+20;
            economy(viewer,false);
            final com.badlogic.gdx.math.Rectangle investigation = object(globals, "gg_rct_GranaryKelThuzadArea01");
            advanceOnAndorhal(viewer, arthas, object(globals, "gg_unit_uktn_0133"), investigation,
                    andorhal.isCompleted());
            report(viewer,ui,arthas);
        }
        else if (name.equals("human05")) {
            skip(globals,ui,"gg_trg_Intro_Cancelled");
            if (flag(globals,"udg_GAMEOVER")) {
                if (!victory) {
                    final boolean caravanCompleted = completed(globals, "udg_GrainCaravanQuest");
                    check(!REQUIRE_CARAVAN || caravanCompleted, "Human05 grain caravan optional objective incomplete");
                    check(!object(globals,"gg_unit_hkee_0043",CUnit.class).isDead(),"Hearthglen keep destroyed");
                    win(globals, name, caravanCompleted ? "Hearthglen defended and grain caravan destroyed"
                            : "Hearthglen defended", "Arthas", "Jaina");
                }
                skip(globals,ui,"gg_trg_Victory_Cancel"); continueVictory(ui); return;
            }
            if (!ready(viewer,ui,arthas)) return;
            economy(viewer,true);
            final List<CUnit> wagons=object(globals,"udg_TheCaravanWagons");
            final CUnit wagon = wagons == null ? null : wagons.stream()
                    .filter(u -> !u.isDead() && !u.isHidden() && !u.isInvulnerable())
                    .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
            final boolean caravanEngaged = objectiveTarget != null && !objectiveTarget.isDead()
                    && !objectiveTarget.isHidden() && !objectiveTarget.isInvulnerable();
            if (REQUIRE_CARAVAN && !caravanEngaged) {
                objectiveTarget = army(viewer).size() >= 7 ? wagon : null;
            }
            if (game.getGameTurnTick() >= reportTick && wagon != null) {
                System.out.printf("[HumanCampaignProbe] grain caravan surviving=%d nearest=%.0f,%.0f life=%.0f engaged=%s approach=%d%n",
                        wagons.stream().filter(unit -> !unit.isDead()).count(), wagon.getX(), wagon.getY(),
                        wagon.getLife(), objectiveTarget != null, hearthglenCaravanSteps.getOrDefault(arthas.getHandleId(), 0));
            }
            micro(viewer, arthas, objectiveTarget == null ? HEARTHGLEN_RALLY
                    : new float[] {objectiveTarget.getX(), objectiveTarget.getY()});
            report(viewer,ui,arthas);
        }
        else if (name.equals("human06")) {
            skip(globals,ui,"gg_trg_Intro_Cancel","gg_trg_Malganis_First_Teleport_Cin_Skip");
            if (flag(globals,"udg_EndCinematicPlaying") || "Victory".equals(ui.getScoreDialogTitle())) {
                if (!victory) { check(real(globals,"udg_ArthasDenials")>=100,"Culling ended without 100 native denials"); win(globals,name,"100 citizens denied through combat","Arthas"); }
                skip(globals,ui,"gg_trg_Victory_Cinematic_Skip"); continueVictory(ui); return;
            }
            check(!flag(globals,"udg_GameOver"), "Culling ended in defeat with " + real(globals,"udg_ArthasDenials")
                    + " denials and " + real(globals,"udg_MalganisConversions") + " conversions");
            if (!ready(viewer,ui,arthas)) return;
            economy(viewer,false);
            final float[][] cityApproach={{10000,4000},{8700,3000},{8200,1600},{6800,1000},{6000,0}};
            if(waypoint<cityApproach.length) {
                if(arthas.distanceSquaredNoCollision(cityApproach[waypoint][0],cityApproach[waypoint][1])<300*300) waypoint++;
                if(waypoint<cityApproach.length) { objectiveTarget=null; micro(viewer,arthas,cityApproach[waypoint]); report(viewer,ui,arthas); return; }
            }
            selectCullingTarget(viewer, arthas, object(globals, "udg_MalganisVariable"));
            if(objectiveTarget!=null) micro(viewer,arthas,new float[]{objectiveTarget.getX(),objectiveTarget.getY()});
            report(viewer,ui,arthas);
            if(game.getGameTurnTick()%400<20) System.out.println("[HumanCampaignProbe] culling denials="+real(globals,"udg_ArthasDenials")+" conversions="+real(globals,"udg_MalganisConversions"));
        }
        else if (name.equals("human07")) {
            skip(globals,ui,"gg_trg_IntroCinematicEscape","gg_trg_Muradin_CinematicEscape");
            if(flag(globals,"udg_GameOver")) {
                if(!victory) { check(completed(globals,"udg_QuestTownHall") && completed(globals,"udg_QuestMuradinMen") && completed(globals,"udg_QuestUndeadCitadel"),"Human07 main/optional objectives incomplete"); win(globals,name,"base established, dwarves rescued and undead citadel destroyed","Arthas","Muradin"); }
                skip(globals,ui,"gg_trg_ExitCinematicEscape"); continueVictory(ui); return;
            }
            if(!ready(viewer,ui,arthas)) return;
            final float[][] route={{-2500,-5300},{-500,-5400},{1700,-5100},{4100,-5200},{4600,-2300},
                    {4592,-848},{2832,912},{2832,4080},{3920,5168},{4768,5056}};
            if(waypoint<route.length && arthas.distanceSquaredNoCollision(route[waypoint][0],route[waypoint][1])<384*384) waypoint++;
            if(waypoint>=4) { establishTown(viewer,4608,-2304); economy(viewer,false); }
            if(waypoint==5 && army(viewer).size()<20) micro(viewer,arthas,new float[]{4600,-2300});
            else if(waypoint<route.length) micro(viewer,arthas,route[waypoint]);
            else {
                final List<CUnit> force = army(viewer);
                final List<CUnit> assembled = force.stream().filter(unit -> unit.distanceSquaredNoCollision(
                        NORTHREND_OUTPOST[0], NORTHREND_OUTPOST[1]) < 1800 * 1800).toList();
                citadelMarching = citadelMarching ? force.size() >= 12
                        : assembled.size() >= 17 && count(assembled, "hmtm") >= 2
                                && count(assembled, "hmpr") >= 2 && arthas.getLife() >= arthas.getMaximumLife() * 0.85f;
                objectiveTarget = citadelMarching ? game.getUnitsIncludingPending().stream()
                        .filter(unit -> !unit.isDead() && !unit.isHidden() && unit.isBuilding() && unit.getPlayerIndex() == 6)
                        .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null) : null;
                micro(viewer, arthas, objectiveTarget == null ? NORTHREND_OUTPOST
                        : new float[] {objectiveTarget.getX(), objectiveTarget.getY()}, null, force);
            }
            report(viewer,ui,arthas);
        }
        else if(name.equals("human08")) {
            skip(globals,ui,"gg_trg_IntroCinematicEscape","gg_trg_GauntletCinematicEscape");
            if(completed(globals,"udg_QuestDestroyShips")) {
                if(!victory) { check(integer(globals,"udg_NumberShipsRemaining")==0,"Ships survived Human08"); win(globals,name,"all five ships destroyed through combat","Arthas","Muradin"); }
                skip(globals,ui,"gg_trg_ExitVictoryCinematicEscape"); continueVictory(ui); return;
            }
            check(!flag(globals, "udg_GameOver"), "Human08 ended with "
                    + integer(globals, "udg_NumberShipsRemaining") + " ships remaining");
            if(!ready(viewer,ui,arthas)) return;
            hireNearbyMercenaries(viewer);
            if (waypoint >= HUMAN08_MERCENARY_RALLY && resupplyShipAssault(viewer, arthas)) {
                report(viewer, ui, arthas);
                return;
            }
            while (waypoint < HUMAN08_ROUTE.length) {
                final float[] approach = HUMAN08_ROUTE[waypoint];
                final String shipName = HUMAN08_SHIPS[waypoint];
                if (shipName == null) {
                    if (waypoint == HUMAN08_CANYON_RALLY && !clearHuman08Canyon(viewer)) {
                        final boolean sapperAvailable = game.getUnitsIncludingPending().stream().anyMatch(unit ->
                                unit.getPlayerIndex() == arthas.getPlayerIndex() && !unit.isDead() && !unit.isHidden()
                                        && "ngsp".equals(unit.getTypeId().toString()));
                        micro(viewer, arthas, sapperAvailable ? approach : HUMAN08_ROUTE[HUMAN08_CANYON_RALLY - 1]);
                        break;
                    }
                    final int radius = waypoint == HUMAN08_CANYON_RALLY ? 160 : waypoint == HUMAN08_MERCENARY_RALLY ? 400 : 300;
                    final boolean rangedInPosition = waypoint >= HUMAN08_SOUTHERN_COAST && army(viewer).stream()
                            .anyMatch(unit -> unit.distanceSquaredNoCollision(approach[0], approach[1]) < radius * radius
                                    && unit.getCurrentAttacks().stream().anyMatch(attack -> attack.getRange() >= 200));
                    final boolean heroInPosition = arthas.distanceSquaredNoCollision(approach[0], approach[1]) < radius * radius;
                    final boolean escortedAdvance = rangedInPosition
                            && arthas.distanceSquaredNoCollision(approach[0], approach[1]) < 520 * 520;
                    if (heroInPosition || escortedAdvance) {
                        waypoint++;
                        continue;
                    }
                    objectiveTarget = null;
                }
                else {
                    final CUnit ship = object(globals, shipName);
                    if (ship == null || ship.isDead()) {
                        waypoint++;
                        continue;
                    }
                    objectiveTarget = ship.isHidden() || ship.isInvulnerable() ? null : ship;
                }
                micro(viewer, arthas, approach);
                break;
            }
            report(viewer,ui,arthas);
        }
        else if(name.equals("human09")) {
            skip(globals,ui,"gg_trg_IntroCinematicEscape","gg_trg_FrostmourneCinematicEscapeGood","gg_trg_FrostmourneCinematicEscapeEvil");
            if(completed(globals,"udg_QuestMalganis")) {
                if(!victory) { check(completed(globals,"udg_QuestFrostmourne"),"Frostmourne objective incomplete"); win(globals,name,"Frostmourne acquired and Mal'Ganis defeated","Arthas"); }
                skip(globals,ui,"gg_trg_ExitCinematicEscape","gg_trg_ExitCinematicEscapeNoFade");
                if("Victory".equals(ui.getScoreDialogTitle()) && capture==null) {
                    final var progress = CampaignProgressStore.get().snapshot();
                    check("true".equals(progress.get("campaign.2")), "Undead campaign was not unlocked by Human09");
                    check("true".equals(progress.get("ending." + (1L << 32))), "Human ending movie was not unlocked");
                    check("true".equals(progress.get("mission." + (2L << 32))), "Undead interlude was not unlocked");
                    finished=true; System.out.println("[HumanCampaignProbe] final native Human campaign victory; Undead campaign unlocked");
                }
                return;
            }
            if (flag(globals, "udg_GameOver")) {
                final CUnit muradin = hero(globals, "Muradin");
                check(false, "Human09 ended before the final victory. Arthas life " + arthas.getLife()
                        + ", Muradin " + (muradin == null ? "unavailable" : Math.round(muradin.getX()) + ","
                                + Math.round(muradin.getY()) + " life " + muradin.getLife()));
            }
            if(!ready(viewer,ui,arthas)) return;
            economy(viewer,true);
            if(!completed(globals,"udg_QuestFrostmourne")) {
                final float[][] route = FROSTMOURNE_VALLEY;
                if(waypoint<route.length && arthas.distanceSquaredNoCollision(route[waypoint][0],route[waypoint][1])<300*300) waypoint++;
                final List<CUnit> expedition = object(globals, "udg_ValleyGroup");
                if (expedition != null) {
                    final CUnit muradin = hero(globals, "Muradin");
                    check(muradin != null && !muradin.isDead(), "Muradin was lost before claiming Frostmourne");
                    if (!observingMuradin) {
                        observingMuradin = true;
                        muradin.addDamageTakenListener((sim, attacker, victim, attack, ranged, type, damage, bonus, actual) -> {
                            if (actual > 0 && victim.getLife() < victim.getMaximumLife() * 0.28f) {
                                System.out.printf("[HumanCampaignProbe] Muradin hit at tick=%d %.0f,%.0f life=%.0f by %s for %.0f%n",
                                        sim.getGameTurnTick(), victim.getX(), victim.getY(), victim.getLife(),
                                        attacker == null ? "script or environment" : attacker.getTypeId(), actual);
                            }
                        });
                    }
                    final float muradinHealth = muradin.getLife() / muradin.getMaximumLife();
                    final float arthasHealth = arthas.getLife() / arthas.getMaximumLife();
                    final var orders = game.getDefaultPlayerUnitOrderExecutor(viewer.getLocalPlayerIndex());
                    muradinRecovering = muradinHealth < (muradinRecovering ? 0.92f : 0.80f);
                    final boolean shielding = waypoint >= 6 && arthasHealth < 0.81f && !arthas.isInvulnerable()
                            && immediate(viewer, orders, arthas, OrderIds.divineshield);
                    arthasRecovering = waypoint >= 5 && !shielding && !arthas.isInvulnerable()
                            && arthasHealth < (arthasRecovering ? 0.88f : 0.70f);
                    final boolean holdingMuradinAtPass = waypoint >= 6;
                    final boolean fallingBack = waypoint >= 5 && (arthasRecovering || muradinRecovering && !holdingMuradinAtPass);
                    final boolean guardMuradin = muradinRecovering || fallingBack;
                    if (holdingMuradinAtPass && !guardMuradin) {
                        final float[] shelter = route[3];
                        CampaignPlaythroughProbe.pointOrder(viewer, orders, muradin, shelter[0], shelter[1]);
                    }
                    final CUnit wolfThreat = game.getUnitsIncludingPending().stream()
                            .filter(unit -> !unit.isDead() && !unit.isHidden()
                                    && ("nwwd".equals(unit.getTypeId().toString()) || "nwwg".equals(unit.getTypeId().toString()))
                                    && (muradin.distanceSquaredNoCollision(unit) <= 650 * 650
                                            || waypoint == 1 && unit.distanceSquaredNoCollision(-5569, -743) <= 900 * 900))
                            .min(Comparator.comparingDouble(muradin::distanceSquaredNoCollision)).orElse(null);
                    if (!fallingBack) {
                        frostmourneRetreatWaypoint = -1;
                    }
                    else if (frostmourneRetreatWaypoint < 0) {
                        frostmourneRetreatWaypoint = Math.min(3, Math.max(0, waypoint - 2));
                    }
                    if (fallingBack && frostmourneRetreatWaypoint > 0) {
                        final float[] refuge = route[frostmourneRetreatWaypoint];
                        final boolean pursued = game.getUnits().stream().anyMatch(unit -> !unit.isDead()
                                && !unit.isHidden() && !unit.getCurrentAttacks().isEmpty()
                                && !game.getPlayer(arthas.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
                                && (arthas.distanceSquaredNoCollision(unit) < 620 * 620
                                        || muradin.distanceSquaredNoCollision(unit) < 620 * 620));
                        if (pursued && muradin.distanceSquaredNoCollision(refuge[0], refuge[1]) < 260 * 260
                                && arthas.distanceSquaredNoCollision(refuge[0], refuge[1]) < 500 * 500) {
                            frostmourneRetreatWaypoint--;
                        }
                    }
                    final float[] arthasFallback = route[Math.max(0, frostmourneRetreatWaypoint)];
                    if (guardMuradin || arthasRecovering) {
                        if (guardMuradin) {
                            final boolean healed = muradinRecovering && (CampaignPlaythroughProbe.useInventoryItem(
                                    viewer, orders, muradin, "phea") || CampaignPlaythroughProbe.useInventoryItem(
                                    viewer, orders, muradin, "pghe"));
                            final var support = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
                                    && !unit.isInvulnerable() && !unit.getCurrentAttacks().isEmpty()
                                    && unit.getPlayerIndex() != muradin.getPlayerIndex()
                                    && !game.getPlayer(muradin.getPlayerIndex()).hasAlliance(
                                            unit.getPlayerIndex(), CAllianceType.PASSIVE)
                                    && muradin.canReach(unit, 410))
                                    .min(Comparator.comparingDouble(muradin::distanceSquaredNoCollision)).orElse(null);
                            if (!healed && !CampaignPlaythroughProbe.targetOrder(viewer, orders, muradin,
                                    OrderIds.thunderbolt, wolfThreat == null ? support : wolfThreat)) {
                                final float[] shelter = fallingBack ? arthasFallback : waypoint < 2 ? MURADIN_SHELTER
                                        : route[Math.max(0, Math.min(waypoint - 2, route.length - 1))];
                                CampaignPlaythroughProbe.pointOrder(viewer, orders, muradin, shelter[0], shelter[1]);
                            }
                        }
                        if (fallingBack) {
                            final boolean healing = arthasHealth < 0.62f
                                    && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")
                                            || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea"));
                            final boolean healedMuradin = !healing && muradinRecovering && arthas.canReach(muradin, 600)
                                    && CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt, muradin);
                            if (!healing && !healedMuradin && (!arthasRecovering || arthas.isInvulnerable()
                                    || !immediate(viewer, orders, arthas, OrderIds.divineshield))) {
                                CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas,
                                        arthasFallback[0], arthasFallback[1]);
                            }
                        }
                    }
                    final CUnit guardian = object(globals, "udg_Guardian");
                    objectiveTarget = waypoint >= 6 && guardian != null && !guardian.isDead()
                            && !guardian.isHidden() && !guardian.isInvulnerable() ? guardian : null;
                    if (wolfThreat != null) objectiveTarget = wolfThreat;
                    final var party = new ArrayList<>(expedition.stream().filter(unit -> !unit.isDead() && !unit.isHidden()
                            && (!(guardMuradin || holdingMuradinAtPass) || unit != muradin)
                            && (!(fallingBack || shielding) || unit != arthas)).toList());
                    final var garrison = army(viewer).stream().filter(unit -> !expedition.contains(unit)).toList();
                    final float[] next = wolfThreat != null ? new float[] {wolfThreat.getX(), wolfThreat.getY()}
                            : guardMuradin && !holdingMuradinAtPass ? new float[] {muradin.getX(), muradin.getY()}
                                    : route[Math.min(waypoint, route.length - 1)];
                    preserveValleyEscort(viewer, orders, arthas, muradin, party, next,
                            route[Math.max(0, Math.min(waypoint - 1, route.length - 1))]);
                    if (party.contains(arthas) && objectiveTarget == guardian && guardian != null
                            && arthas.canReach(guardian, 600)
                            && CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt, guardian)) {
                        party.remove(arthas);
                    }
                    if (!fallingBack && !shielding && collectValleyLoot(viewer, arthas)) party.remove(arthas);
                    micro(viewer, arthas, next, null, party);
                    defendFrostmourneTown(viewer, arthas, garrison);
                }
            }
            else {
                assaultMalganis(viewer, arthas, object(globals, "udg_Malganis"));
            }
            report(viewer,ui,arthas);
        }
        else throw new IllegalStateException("Human campaign audit: no objective driver for " + name);
    }

    private static void preserveValleyEscort(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit arthas, final CUnit muradin, final List<CUnit> party, final float[] next, final float[] shelter) {
        final var game = viewer.simulation;
        final List<CUnit> escort = party.stream().filter(unit -> unit != arthas && unit != muradin).toList();
        final CUnit wounded = escort.stream().filter(unit -> unit.canReach(arthas, 500)
                && unit.getLife() < unit.getMaximumLife() * 0.78f)
                .min(Comparator.comparingDouble(unit -> unit.getLife() / unit.getMaximumLife())).orElse(null);
        final CUnit healingTarget = muradin.getLife() < muradin.getMaximumLife() * 0.83f && arthas.canReach(muradin, 600)
                ? muradin : wounded;
        if (party.contains(arthas) && CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt,
                healingTarget)) party.remove(arthas);
        for (final CUnit unit : escort) {
            if (unit.getLife() < unit.getMaximumLife() * 0.52f) {
                CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, shelter[0], shelter[1]);
                party.remove(unit);
                continue;
            }
            if (!"hrif".equals(unit.getTypeId().toString())) continue;
            final int range = unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0);
            final CUnit enemy = game.getUnits().stream().filter(other -> !other.isDead() && !other.isHidden()
                    && !other.isInvulnerable() && other.getPlayerIndex() != unit.getPlayerIndex()
                    && !game.getPlayer(unit.getPlayerIndex()).hasAlliance(other.getPlayerIndex(), CAllianceType.PASSIVE)
                    && unit.canReach(other, range))
                    .min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
            if (!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) {
                final double length = Math.hypot(next[0] - arthas.getX(), next[1] - arthas.getY());
                final float spacing = length > 0 ? Math.min(1, 200 / (float) length) : 0;
                CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit,
                        arthas.getX() - (next[0] - arthas.getX()) * spacing,
                        arthas.getY() - (next[1] - arthas.getY()) * spacing);
            }
            party.remove(unit);
        }
    }

    private static boolean collectValleyLoot(final War3MapViewer viewer, final CUnit arthas) {
        final var game = viewer.simulation;
        final var orders = game.getDefaultPlayerUnitOrderExecutor(viewer.getLocalPlayerIndex());
        if (arthas.getMana() < arthas.getMaximumMana() * 0.5f
                && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pgma")) return true;
        if (arthas.getLife() < arthas.getMaximumLife() * 0.65f
                && CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")) return true;
        if (objectiveTarget != null && arthas.distanceSquaredNoCollision(objectiveTarget) < 650 * 650) return false;
        final int tick = game.getGameTurnTick();
        if (valleyLootHandle != 0 && tick >= valleyLootDeadline) skippedValleyLoot.add(valleyLootHandle);
        final var item = game.getItems().stream().filter(candidate -> !candidate.isDead() && !candidate.isHidden()
                && candidate.getContainedInventory() == null && VALLEY_LOOT.contains(candidate.getTypeId().toString())
                && !skippedValleyLoot.contains(candidate.getHandleId())
                && arthas.distanceSquaredNoCollision(candidate) < 650 * 650)
                .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
        if (item == null) return false;
        if (valleyLootHandle != item.getHandleId()) {
            valleyLootHandle = item.getHandleId();
            valleyLootDeadline = tick + 320;
        }
        return CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.smart, item);
    }

    private static void assaultMalganis(final War3MapViewer viewer, final CUnit arthas, final CUnit malganis) {
        final var game = viewer.simulation;
        final var orders = game.getDefaultPlayerUnitOrderExecutor(viewer.getLocalPlayerIndex());
        final var force = army(viewer);
        final boolean forwardTownReady = game.getUnits().stream().anyMatch(unit -> unit.getPlayerIndex() == arthas.getPlayerIndex()
                && !unit.isDead() && !unit.isConstructing() && List.of("htow", "hkee", "hcas").contains(unit.getTypeId().toString())
                && unit.getLife() >= 0.75f * unit.getMaximumLife()
                && unit.distanceSquaredNoCollision(FROSTMOURNE_EXPANSION[0], FROSTMOURNE_EXPANSION[1]) < 800 * 800);
        final float[] rally = forwardTownReady ? FROSTMOURNE_FORWARD_RALLY : FROSTMOURNE_RALLY;
        final float health = arthas.getLife() / arthas.getMaximumLife();
        final boolean healing = health < 0.6f && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")
                || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea")
                || !arthas.isInvulnerable() && immediate(viewer, orders, arthas, OrderIds.divineshield));
        if (!regrouping && (waypoint < 8 || force.size() * 3 < frostAssaultSize * 2)) {
            regrouping = true;
            frostRegroupTick = game.getGameTurnTick();
            waypoint = 8;
        }
        if (regrouping) {
            objectiveTarget = null;
            if (!healing) {
                CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, rally[0], rally[1]);
            }
            final List<CUnit> regroupingTroops = force.stream().filter(unit -> unit != arthas).toList();
            if (forwardTownReady) micro(viewer, arthas, rally, null, regroupingTroops);
            else defendFrostmourneTown(viewer, arthas, regroupingTroops);
            final int regroupTicks = game.getGameTurnTick() - frostRegroupTick;
            final CUnit startingMine = object(game.getGlobalScope(), "gg_unit_ngol_0003");
            final var startingGold = startingMine == null ? null : startingMine.getFirstAbilityOfType(CAbilityGoldMine.class);
            final boolean exhausted = startingGold == null || startingGold.getGold() == 0;
            final int readyCount = exhausted && regroupTicks >= 4800 ? 12
                    : regroupTicks >= 7200 ? 14 : regroupTicks >= 4800 ? 18 : 24;
            if (regroupTicks < 700 || health < 0.9f || arthas.distanceSquaredNoCollision(rally[0], rally[1]) > 600 * 600
                    || !exhausted && regroupTicks < 4800 && count(force, "hmtm") < 2
                    || force.stream().filter(unit -> unit.distanceSquaredNoCollision(
                            rally[0], rally[1]) < 1200 * 1200).count() < readyCount) return;
            regrouping = false;
            frostAssaultSize = force.size();
        }
        final var productionFirst = Comparator.<CUnit>comparingInt(unit ->
                "ugol".equals(unit.getTypeId().toString()) ? -1 : MALGANIS_PRODUCTION.contains(unit.getTypeId().toString()) ? 0 : 1)
                .thenComparingDouble(arthas::distanceSquaredNoCollision);
        final CUnit priorityTarget = objectiveTarget != null && objectiveTarget.isBuilding()
                && !objectiveTarget.isDead() && !objectiveTarget.isHidden() && !objectiveTarget.isInvulnerable()
                ? objectiveTarget
                : game.getUnitsIncludingPending().stream()
                        .filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
                                && unit.getPlayerIndex() == 6 && unit.isBuilding()
                                && !"uzig".equals(unit.getTypeId().toString()) && !"uzg1".equals(unit.getTypeId().toString()))
                        .min(productionFirst).orElse(malganis);
        final var garrison = force.stream().filter(unit -> unit != arthas
                && ("hfoo".equals(unit.getTypeId().toString()) || "hrif".equals(unit.getTypeId().toString())
                        || "hmpr".equals(unit.getTypeId().toString()) || "hkni".equals(unit.getTypeId().toString())))
                .sorted(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(
                        FROSTMOURNE_DEFENSE[0], FROSTMOURNE_DEFENSE[1])))
                .limit(Math.min(10, (force.size() + 2) / 3)).toList();
        defendFrostmourneTown(viewer, arthas, garrison);
        objectiveTarget = priorityTarget;
        arthasRecovering = health < (arthasRecovering ? 0.9f : 0.65f);
        if (arthasRecovering && !healing) {
            CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, rally[0], rally[1]);
        }
        if (objectiveTarget == null || objectiveTarget.isDead()) return;
        final var assault = force.stream().filter(unit -> !garrison.contains(unit)
                && (!(arthasRecovering || healing) || unit != arthas)).toList();
        micro(viewer, arthas, new float[] {objectiveTarget.getX(), objectiveTarget.getY()}, null, assault);
    }

    private static void defendFrostmourneTown(final War3MapViewer viewer, final CUnit arthas,
            final List<CUnit> garrison) {
        final var game = viewer.simulation;
        final int player = viewer.getLocalPlayerIndex();
        final var units = game.getUnitsIncludingPending();
        final var buildings = units.stream().filter(unit -> !unit.isDead() && !unit.isHidden()
                && unit.isBuilding() && unit.getPlayerIndex() == player).toList();
        objectiveTarget = units.stream().filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
                && unit.getPlayerIndex() != player && !unit.getCurrentAttacks().isEmpty()
                && !game.getPlayer(player).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE))
                .filter(unit -> buildings.stream().anyMatch(building -> unit.distanceSquaredNoCollision(building) < 950 * 950))
                .min(Comparator.comparingDouble(unit -> unit.distanceSquaredNoCollision(
                        FROSTMOURNE_DEFENSE[0], FROSTMOURNE_DEFENSE[1]))).orElse(null);
        final float[] destination = objectiveTarget == null ? FROSTMOURNE_DEFENSE
                : new float[] {objectiveTarget.getX(), objectiveTarget.getY()};
        micro(viewer, arthas, destination, null, garrison);
        objectiveTarget = null;
    }

    private static boolean ready(final War3MapViewer viewer,final MeleeUI ui,final CUnit hero) {
        final var globals=viewer.simulation.getGlobalScope();
        final String intro=mission.equals("human05")?"udg_IntroCancelled":mission.equals("human06")?"udg_IntroCancel":"udg_IntroCinematicEscape";
        if(!flag(globals,intro)) return false;
        if(!ui.isUserControlEnabled() || hero.isPaused() || viewer.simulation.isGamePaused() || viewer.simulation.getGameTurnTick()<orderTick) return false;
        check(!hero.isDead(),"Arthas died in "+mission);
        orderTick=viewer.simulation.getGameTurnTick()+20; return true;
    }
    private static void win(final GlobalScope globals,final String name,final String objectives,final String... heroes) {
        remember(globals,heroes); victory=true; capture=name+"-victory"; captureFrames=0;
        System.out.println("[HumanCampaignProbe] objectives complete "+name+"; "+objectives);
    }
    private static CUnit hero(final GlobalScope globals,final String name) {
        if(name.equals("Arthas")) {
            final CUnit evil=object(globals,"udg_EvilArthas"); if(evil!=null && !evil.isHidden()) return evil;
            final CUnit alias=object(globals,"udg_ArthasVariable"); if(alias!=null) return alias;
        }
        return object(globals,"udg_"+name);
    }
    private static double real(final GlobalScope globals,final String name) { return globals.getGlobal(name).visit(RealJassValueVisitor.getInstance()); }
    private static int integer(final GlobalScope globals,final String name) { return globals.getGlobal(name).visit(IntegerJassValueVisitor.getInstance()); }
    private static void establishTown(final War3MapViewer viewer,final float x,final float y) {
        final var units=viewer.simulation.getUnitsIncludingPending();
        if(units.stream().anyMatch(u->u.getPlayerIndex()==viewer.getLocalPlayerIndex() && !u.isDead() && List.of("htow","hkee","hcas").contains(u.getTypeId().toString()))) return;
        final CUnit peasant=units.stream().filter(u->u.getPlayerIndex()==viewer.getLocalPlayerIndex() && !u.isDead() && u.getTypeId().equals(War3ID.fromString("hpea"))).findFirst().orElse(null);
        if(peasant==null || peasant.getCurrentOrder()!=null && peasant.getCurrentOrder().getOrderId()==War3ID.fromString("htow").getValue()) return;
        final var build=peasant.getFirstAbilityOfType(AbstractCAbilityBuild.class); final var activation=new BooleanAbilityActivationReceiver();
        final int id=War3ID.fromString("htow").getValue(); build.checkCanUse(viewer.simulation,peasant,id,activation); if(!activation.isOk()) return;
        final var target=new PointAbilityTargetCheckReceiver(); build.checkCanTarget(viewer.simulation,peasant,id,new AbilityPointTarget(x,y),target);
        if(target.getTarget()!=null) new CPlayerUnitOrderExecutor(viewer.simulation,viewer.getLocalPlayerIndex()).issuePointOrder(peasant.getHandleId(),build.getHandleId(),id,x,y,false);
    }

    private static void advanceOnAndorhal(final War3MapViewer viewer, final CUnit arthas, final CUnit commander,
            final com.badlogic.gdx.math.Rectangle investigation, final boolean investigated) {
        final List<CUnit> army = army(viewer);
        final float[] rally = HUMAN04[0];
        objectiveTarget = investigated ? commander : null;
        final CUnit protectedTarget = investigated ? null : commander;
        final boolean atRally = arthas.distanceSquaredNoCollision(rally[0], rally[1]) < 450 * 450;
        final float health = arthas.getLife() / arthas.getMaximumLife();
        final int tick = viewer.simulation.getGameTurnTick();
        if (waypoint > 0 && !regrouping && (health < 0.74f || army.size() * 2 < andorhalAssaultSize)) {
            regrouping = true;
            andorhalRegroupTick = tick;
            System.out.println("[HumanCampaignProbe] withdrew the Andorhal force to recover and reinforce");
        }
        if (regrouping && !atRally) {
            final var orders = viewer.simulation.getDefaultPlayerUnitOrderExecutor(arthas.getPlayerIndex());
            final boolean protectingArthas = arthas.getCurrentBehavior() != null
                    && arthas.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL
                    || health < 0.61f && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea")
                            || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe"))
                    || health < 0.8f && !arthas.isInvulnerable() && immediate(viewer, orders, arthas, OrderIds.divineshield);
            for (final CUnit unit : army) {
                if (unit == arthas && protectingArthas) continue;
                CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, rally[0], rally[1]);
            }
            return;
        }
        if (waypoint == 0 || regrouping) {
            final boolean ready = count(army, "hrif") >= 4 && count(army, "hmpr") >= 2
                    && count(army, "hmtm") >= 1 && army.size() >= 15;
            final boolean fallback = regrouping && tick - andorhalRegroupTick >= 3600 && army.size() >= 5
                    && count(army, "hmtm") >= 1 && count(army, "hmpr") >= 1;
            if (!atRally || !(ready || fallback) || health < 0.85f) {
                micro(viewer, arthas, rally, protectedTarget);
                return;
            }
            regrouping = false;
            andorhalAssaultSize = army.size();
            waypoint = 1;
            System.out.println("[HumanCampaignProbe] advanced with riflemen, priests and mortar support");
        }
        if (waypoint < HUMAN04.length
                && arthas.distanceSquaredNoCollision(HUMAN04[waypoint][0], HUMAN04[waypoint][1]) < 450 * 450) {
            waypoint++;
        }
        final float[] destination = waypoint < HUMAN04.length ? HUMAN04[waypoint]
                : investigated ? new float[] {commander.getX(), commander.getY()}
                : new float[] {investigation.x + investigation.width / 2, investigation.y + investigation.height / 2};
        micro(viewer, arthas, destination, protectedTarget);
        if (!investigated && waypoint >= 3 && arthas.getY() >= 500) {
            final CUnit scout = army.stream().filter(unit -> List.of("hwat", "hwt2", "hwt3")
                    .contains(unit.getTypeId().toString())).min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision))
                    .orElse(null);
            if (scout != null) {
                final var orders = viewer.simulation.getDefaultPlayerUnitOrderExecutor(viewer.getLocalPlayerIndex());
                CampaignPlaythroughProbe.pointOrder(viewer, orders, scout,
                        investigation.x + investigation.width / 2, investigation.y + investigation.height / 2);
            }
        }
    }

    private static void micro(final War3MapViewer viewer, final CUnit arthas, final float[] destination) {
        micro(viewer, arthas, destination, null);
    }

    private static void micro(final War3MapViewer viewer, final CUnit arthas, final float[] destination,
            final CUnit protectedTarget) {
        micro(viewer, arthas, destination, protectedTarget, army(viewer));
    }

    private static void micro(final War3MapViewer viewer, final CUnit arthas, final float[] destination,
            final CUnit protectedTarget, final List<CUnit> army) {
        final var game = viewer.simulation;
        final var orders = new CPlayerUnitOrderExecutor(game, viewer.getLocalPlayerIndex());
        final boolean andorhal = "human04".equals(mission);
        final boolean hearthglen = "human05".equals(mission);
        final boolean caravanAssault = hearthglen && objectiveTarget != null;
        final boolean culling = "human06".equals(mission);
        final boolean citadelAssault = "human07".equals(mission) && objectiveTarget != null;
        final boolean canyonPassage = "human08".equals(mission) && waypoint >= 4 && waypoint <= HUMAN08_CANYON_RALLY + 1;
        final boolean shipAssault = "human08".equals(mission) && objectiveTarget != null;
        final boolean coastMarch = "human08".equals(mission) && waypoint > HUMAN08_MERCENARY_RALLY;
        final boolean malganisAssault = "human09".equals(mission);
        final boolean defendingTown = destination == HEARTHGLEN_RALLY || destination == FROSTMOURNE_DEFENSE;
        if (hearthglen) {
            final float health = arthas.getLife() / arthas.getMaximumLife();
            regrouping = health < (regrouping ? 0.84f : 0.62f);
        }
        for (final CUnit unit : army) {
            if (unit.isPaused() || canyonPassage && "ngsp".equals(unit.getTypeId().toString())) continue;
            final int rangedAttack = shipAssault || coastMarch
                    ? unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0) : 0;
            if (shipAssault && rangedAttack >= 200 && unit.canReach(objectiveTarget, rangedAttack)) {
                final var order = unit.getCurrentOrder();
                if ((order != null && order.getOrderId() == OrderIds.attack && order.getTarget(game) == objectiveTarget)
                        || CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, objectiveTarget)) continue;
            }
            final String type = unit.getTypeId().toString();
            if ((malganisAssault || hearthglen) && "hmpr".equals(type)
                    && (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL
                            || dispelFrostmourneThreats(viewer, orders, unit, army))) continue;
            if ((andorhal || citadelAssault || malganisAssault || hearthglen) && "hmpr".equals(type)
                    || (shipAssault || coastMarch) && ("nith".equals(type) || "nitp".equals(type))) {
                if (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getHighlightOrderId() == OrderIds.heal) continue;
                final CUnit injured = citadelAssault && unit.canReach(arthas, 500)
                        && arthas.getMaximumLife() - arthas.getLife() >= 80 ? arthas : army.stream()
                        .filter(ally -> unit.canReach(ally, 250) && ally.getMaximumLife() - ally.getLife() >= 20)
                        .max(Comparator.comparingDouble(ally -> ally.getMaximumLife() - ally.getLife())).orElse(null);
                if (injured != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.heal, injured)) continue;
            }
            if (coastMarch && rangedAttack >= 200
                    && (!shipAssault || !unit.canReach(objectiveTarget, rangedAttack))) {
                CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, destination[0], destination[1]);
                continue;
            }
            if ((andorhal || citadelAssault || malganisAssault) && unit.getTypeId().equals(War3ID.fromString("hmtm"))) {
                final int player = unit.getPlayerIndex();
                final int range = unit.getCurrentAttacks().stream().mapToInt(attack -> attack.getRange()).max().orElse(0);
                final CUnit structure = game.getUnitsIncludingPending().stream()
                        .filter(target -> target.isBuilding() && !target.isDead() && !target.isHidden() && !target.isInvulnerable())
                        .filter(target -> player != target.getPlayerIndex() && target.isVisible(game, player)
                                && !game.getPlayer(player).hasAlliance(target.getPlayerIndex(), CAllianceType.PASSIVE)
                                && (protectedTarget == null || target.distanceSquaredNoCollision(protectedTarget) > 300 * 300)
                                && unit.canReach(target, range))
                        .min(Comparator.comparingDouble(unit::distanceSquaredNoCollision)).orElse(null);
                if (structure != null) {
                    final var order = unit.getCurrentOrder();
                    if (order != null && order.getOrderId() == OrderIds.attack && order.getTarget(game) == structure) continue;
                    if (CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, structure)) continue;
                }
            }
            if (unit.getHeroData() != null) {
                for (final String skill : unit == arthas ? new String[]{"AHds","AHhb","AHad","AHre"} : unit.getTypeId().equals(War3ID.fromString("Hmbr")) ? new String[]{"AHtb","AHtc","AHbh","AHav"} : new String[]{"AHwe","AHab","AHbz"}) {
                    final int id = War3ID.fromString(skill).getValue();
                    final var activation = new BooleanAbilityActivationReceiver();
                    unit.getHeroData().checkCanUse(game, unit, id, activation);
                    if (activation.isOk()) { orders.issueImmediateOrder(unit.getHandleId(), unit.getHeroData().getHandleId(), id, false); break; }
                }
            }
            if (unit == arthas && hearthglen && regrouping) {
                if (arthas.getCurrentBehavior() != null
                        && arthas.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) continue;
                if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea")
                        || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")
                        || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "shea")) continue;
                if (!arthas.isInvulnerable() && immediate(viewer, orders, arthas, OrderIds.divineshield)) continue;
                final int caravanStep = hearthglenCaravanSteps.getOrDefault(arthas.getHandleId(), 0);
                final float[] refuge = caravanAssault && caravanStep >= 12
                        ? HEARTHGLEN_CARAVAN_ROUTE[caravanStep - 2] : HEARTHGLEN_RALLY;
                CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, refuge[0], refuge[1]);
                continue;
            }
            if (caravanAssault && unit.canReach(objectiveTarget, 400)
                    && CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, objectiveTarget)) {
                continue;
            }
            if (caravanAssault && unit.distanceSquaredNoCollision(objectiveTarget) > 780 * 780
                    && hearthglenCaravanSteps.getOrDefault(unit.getHandleId(), 0) < HEARTHGLEN_CARAVAN_ROUTE.length - 1) {
                if (unit == arthas && !unit.isInvulnerable() && unit.getLife() < unit.getMaximumLife() * 0.86f
                        && immediate(viewer, orders, unit, OrderIds.divineshield)) continue;
                CampaignPlaythroughProbe.followRoute(viewer, orders, unit, HEARTHGLEN_CARAVAN_ROUTE,
                        HEARTHGLEN_CARAVAN_ROUTE.length - 1, 220, hearthglenCaravanSteps);
                continue;
            }
            final CUnit engagementUnit = caravanAssault || citadelAssault || shipAssault || malganisAssault ? unit : arthas;
            final CUnit enemy = !shipAssault && objectiveTarget!=null && !objectiveTarget.isDead() && engagementUnit.distanceSquaredNoCollision(objectiveTarget)<650*650 ? objectiveTarget : game.getUnitsIncludingPending().stream().filter(u -> u != protectedTarget && (!shipAssault || u != objectiveTarget) && !u.isDead() && !u.isHidden() && !u.isInvulnerable()
                    && (!culling || cullingRetryTick.getOrDefault(u.getHandleId(), 0) <= game.getGameTurnTick())
                    && !game.getPlayer(unit.getPlayerIndex()).hasAlliance(u.getPlayerIndex(), CAllianceType.PASSIVE)
                    && u.getPlayerIndex() != unit.getPlayerIndex()
                    && (defendingTown ? u.distanceSquaredNoCollision(destination[0], destination[1])
                            : engagementUnit.distanceSquaredNoCollision(u)) < 650 * 650)
                    .min(Comparator.comparingDouble(engagementUnit::distanceSquaredNoCollision)).orElse(null);
            if (unit == arthas) {
                if (malganisAssault && waypoint >= 8 && game.getUnits().stream().filter(corpse -> corpse.isBoneCorpse()
                        && corpse.getPlayerIndex() == arthas.getPlayerIndex() && arthas.canReach(corpse, 900)).limit(3).count() == 3
                        && immediate(viewer, orders, arthas, OrderIds.resurrection)) continue;
                final float shieldThreshold = citadelAssault ? 0.8f : 0.6f;
                if (!arthas.isInvulnerable() && arthas.getLife() < arthas.getMaximumLife() * shieldThreshold
                        && immediate(viewer, orders, arthas, OrderIds.divineshield)) continue;
                if (citadelAssault) {
                    final float health = arthas.getLife() / arthas.getMaximumLife();
                    regrouping = health < (regrouping ? 0.9f : 0.77f);
                    if (regrouping) {
                        if (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea")
                                || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")) continue;
                        CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, NORTHREND_OUTPOST[0], NORTHREND_OUTPOST[1]);
                        continue;
                    }
                }
                final CUnit injured = army.stream().filter(u -> u != arthas && u.getMaximumLife() - u.getLife() > CampaignPlaythroughProbe.healingThreshold(arthas)
                    && arthas.distanceSquaredNoCollision(u) < 600 * 600).max(Comparator.comparingDouble(u -> u.getMaximumLife() - u.getLife())).orElse(null);
                if (injured != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt, injured)) continue;
                if (coastMarch && waypoint >= HUMAN08_SOUTHERN_COAST
                        && arthas.distanceSquaredNoCollision(destination[0], destination[1]) > 310 * 310) {
                    CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, destination[0], destination[1]);
                    continue;
                }
                if (malganisAssault && enemy != null && enemy == object(game.getGlobalScope(), "udg_Guardian")
                        && CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt, enemy)) continue;
                if (enemy != null && CampaignPlaythroughProbe.supportOrder(viewer, orders, arthas, enemy)) continue;
            }
            else if (unit.getHeroData() != null && enemy != null) {
                if (unit.getTypeId().equals(War3ID.fromString("Hmbr"))) {
                    if(CampaignPlaythroughProbe.targetOrder(viewer,orders,unit,OrderIds.thunderbolt,enemy) || immediate(viewer,orders,unit,OrderIds.thunderclap)) continue;
                }
                if (immediate(viewer, orders, unit, OrderIds.waterelemental)) continue;
                if (unit.getLife() < unit.getMaximumLife() * 0.45f && CampaignPlaythroughProbe.supportOrder(viewer, orders, unit, enemy)) continue;
            }
            if (enemy != null) {
                if (unit.getCurrentBehavior() == null || unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.ATTACK)
                    if(!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) CampaignPlaythroughProbe.pointOrder(viewer,orders,unit,destination[0],destination[1]);
            }
            else if (!malganisAssault || waypoint < 8 || objectiveTarget == null
                    || !CampaignPlaythroughProbe.attackMoveOrder(viewer, orders, unit, destination[0], destination[1])) {
                CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, destination[0], destination[1]);
            }
        }
    }

    private static boolean dispelFrostmourneThreats(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit priest, final List<CUnit> army) {
        if (priest.getMana() < 75) return false;
        final var game = viewer.simulation;
        final var player = game.getPlayer(priest.getPlayerIndex());
        if (player.getTechtreeUnlocked(War3ID.fromString("Rhpt")) < 1) return false;
        final CUnit crippled = army.stream().filter(ally -> priest.canReach(ally, 500)
                && ally.getAbilities().stream().anyMatch(ability -> War3ID.fromString("Bcri").equals(ability.getAlias())))
                .min(Comparator.comparingDouble(priest::distanceSquaredNoCollision)).orElse(null);
        if (crippled != null && CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, priest, OrderIds.dispel,
                crippled.getX(), crippled.getY())) return true;
        final var summons = game.getUnits().stream().filter(unit -> !unit.isDead() && !unit.isHidden()
                && unit.isUnitType(CUnitTypeJass.SUMMONED) && unit.isVisible(game, player.getId())
                && !player.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE) && priest.canReach(unit, 530)).toList();
        final CUnit cluster = summons.stream().filter(unit -> summons.stream()
                .filter(other -> unit.distanceSquaredNoCollision(other) < 200 * 200).limit(2).count() == 2)
                .min(Comparator.comparingDouble(priest::distanceSquaredNoCollision)).orElse(null);
        return cluster != null && CampaignPlaythroughProbe.pointAbilityOrder(viewer, orders, priest, OrderIds.dispel,
                cluster.getX(), cluster.getY());
    }

    private static void selectCullingTarget(final War3MapViewer viewer, final CUnit arthas, final CUnit rival) {
        final var game = viewer.simulation;
        final int tick = game.getGameTurnTick();
        if (rival != null && rival != objectiveTarget && !rival.isDead() && !rival.isHidden()
                && !rival.isInvulnerable() && arthas.distanceSquaredNoCollision(rival) < 900 * 900
                && cullingRetryTick.getOrDefault(rival.getHandleId(), 0) <= tick) {
            objectiveTarget = rival;
            cullingTargetProgressTick = tick;
            cullingTargetLife = rival.getLife();
            return;
        }
        if (objectiveTarget != null && !objectiveTarget.isDead() && !objectiveTarget.isHidden()
                && !objectiveTarget.isInvulnerable()) {
            if (objectiveTarget.getLife() < cullingTargetLife) {
                cullingTargetProgressTick = tick;
                cullingTargetLife = objectiveTarget.getLife();
            }
            if (tick - cullingTargetProgressTick < CULLING_STALLED_TARGET_TICKS) {
                return;
            }
            cullingRetryTick.put(objectiveTarget.getHandleId(), tick + CULLING_RETRY_DELAY_TICKS);
        }
        objectiveTarget = game.getUnitsIncludingPending().stream()
                .filter(unit -> !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable())
                .filter(HumanCampaignPlaythroughProbe::isCullingTarget)
                .filter(unit -> cullingRetryTick.getOrDefault(unit.getHandleId(), 0) <= tick)
                .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
        if (objectiveTarget != null) {
            cullingTargetProgressTick = tick;
            cullingTargetLife = objectiveTarget.getLife();
        }
    }

    private static boolean isCullingTarget(final CUnit unit) {
        final String typeId = unit.getTypeId().toString();
        return switch (unit.getPlayerIndex()) {
        case 2 -> "nzom".equals(typeId);
        case 8 -> switch (typeId) {
            case "nC00", "nC01", "nC02", "nC04", "nzom" -> true;
            default -> false;
        };
        case 10, 11 -> typeId.startsWith("ncb");
        default -> false;
        };
    }

    private static void hireNearbyMercenaries(final War3MapViewer viewer) {
        final var game = viewer.simulation;
        final int player = viewer.getLocalPlayerIndex();
        final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
        for (final CUnit camp : game.getUnitsIncludingPending()) {
            if (camp.isDead() || camp.isHidden() || camp.getNeutralBuildingData() == null
                    || camp.getNeutralBuildingData().getSelectedPlayerUnit(player) == null) continue;
            final var sale = camp.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellUnits.class);
            if (sale == null) continue;
            final boolean firstVisit = visitedMercenaryCamps.add(camp.getHandleId());
            final boolean northCamp = "nC01".equals(camp.getTypeId().toString());
            for (int index = 0; index < sale.getUnitsSold().size(); index++) {
                final int orderId = player | ((index + 1) << 8);
                final String type = sale.getUnitsSold().get(index).toString();
                final boolean sapper = "ngsp".equals(type);
                final boolean rangedReplacement = northCamp && resupplyingShipAssault && HUMAN08_RANGED_MERCENARIES.contains(type);
                if (!firstVisit && !sapper && !rangedReplacement) continue;
                final long ownedSappers = sapper ? game.getUnitsIncludingPending().stream().filter(unit ->
                        unit.getPlayerIndex() == player && !unit.isDead() && "ngsp".equals(unit.getTypeId().toString())).count() : 0;
                final int count = sapper ? Math.max(0, HUMAN08_BLASTS.length - canyonBlast - (int) ownedSappers)
                        : rangedReplacement ? Math.max(0, 4 - rangedShipEscort(viewer)) : northCamp ? 3 : 1;
                for (int purchase = 0; purchase < count; purchase++) {
                    final var activation = new BooleanAbilityActivationReceiver();
                    sale.checkCanUse(game, camp, orderId, activation);
                    if (!activation.isOk()) break;
                    orders.issueImmediateOrder(camp.getHandleId(), sale.getHandleId(), orderId, false);
                    if (rangedReplacement) System.out.println("[HumanCampaignProbe] hired ranged replacement " + type);
                }
            }
        }
    }

    private static int rangedShipEscort(final War3MapViewer viewer) {
        return (int) army(viewer).stream().filter(unit ->
                unit.getCurrentAttacks().stream().anyMatch(attack -> attack.getRange() >= 200)).count();
    }

    private static boolean resupplyShipAssault(final War3MapViewer viewer, final CUnit arthas) {
        final int ranged = rangedShipEscort(viewer);
        final float health = arthas.getLife() / arthas.getMaximumLife();
        recoveringShipAssault = health < (recoveringShipAssault ? 0.78f : 0.52f);
        final boolean finalApproach = waypoint == HUMAN08_ROUTE.length - 1 && objectiveTarget != null;
        if (!resupplyingShipAssault && (recoveringShipAssault && !finalApproach || ranged == 0
                || waypoint == HUMAN08_MERCENARY_RALLY && ranged < 3)) {
            resupplyingShipAssault = true;
            System.out.println("[HumanCampaignProbe] returned to the northern mercenary camp for ranged replacements");
        }
        if (resupplyingShipAssault && ranged >= 3 && !recoveringShipAssault) {
            resupplyingShipAssault = false;
            if (waypoint == HUMAN08_ROUTE.length - 1) waypoint = HUMAN08_SOUTHERN_COAST;
        }
        if (!resupplyingShipAssault) return false;
        final var orders = viewer.simulation.getDefaultPlayerUnitOrderExecutor(arthas.getPlayerIndex());
        if (recoveringShipAssault) {
            for (final CUnit unit : army(viewer)) {
                final String type = unit.getTypeId().toString();
                if (("nith".equals(type) || "nitp".equals(type)) && unit.canReach(arthas, 400)
                        && (unit.getCurrentBehavior() == null
                                || unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.SPELL)) {
                    CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.heal, arthas);
                }
            }
        }
        if (arthas.getCurrentBehavior() != null
                && arthas.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL) return true;
        if (recoveringShipAssault && (CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "phea")
                || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "pghe")
                || CampaignPlaythroughProbe.useInventoryItem(viewer, orders, arthas, "shea"))) return true;
        if (health < 0.43f && !arthas.isInvulnerable() && immediate(viewer, orders, arthas, OrderIds.divineshield)) return true;
        final float[] camp = HUMAN08_ROUTE[HUMAN08_MERCENARY_RALLY];
        CampaignPlaythroughProbe.pointOrder(viewer, orders, arthas, camp[0], camp[1]);
        objectiveTarget = null;
        return true;
    }

    private static boolean clearHuman08Canyon(final War3MapViewer viewer) {
        if (canyonSapper != null && canyonSapper.isDead()) {
            canyonSapper = null;
            if (canyonDetonating) canyonBlast++;
            canyonDetonating = false;
        }
        if (canyonBlast >= HUMAN08_BLASTS.length) return true;
        final var game = viewer.simulation;
        final int player = viewer.getLocalPlayerIndex();
        final var orders = game.getDefaultPlayerUnitOrderExecutor(player);
        final float[] blastPoint = HUMAN08_BLASTS[canyonBlast];
        final var sappers = game.getUnitsIncludingPending().stream()
                .filter(unit -> unit.getPlayerIndex() == player && !unit.isDead() && !unit.isHidden()
                        && "ngsp".equals(unit.getTypeId().toString())).toList();
        if (canyonSapper == null) {
            canyonSapper = sappers.stream().min(Comparator.comparingDouble(unit ->
                    unit.distanceSquaredNoCollision(blastPoint[0], blastPoint[1]))).orElse(null);
        }
        final float[] staging = HUMAN08_ROUTE[HUMAN08_CANYON_RALLY];
        for (final CUnit sapper : sappers) {
            if (sapper != canyonSapper) {
                CampaignPlaythroughProbe.pointOrder(viewer, orders, sapper, staging[0] - 64, staging[1]);
            }
        }
        final CUnit sapper = canyonSapper;
        if (sapper == null || canyonDetonating) return false;
        if (sapper.distanceSquaredNoCollision(blastPoint[0], blastPoint[1]) > 40 * 40) {
            CampaignPlaythroughProbe.pointOrder(viewer, orders, sapper, blastPoint[0], blastPoint[1]);
            return false;
        }
        final var kaboom = sapper.getFirstAbilityOfType(
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.sappers.CAbilityKaboom.class);
        if (kaboom == null) return false;
        final var blast = new AbilityPointTarget(sapper.getX(), sapper.getY());
        final var receiver = new PointAbilityTargetCheckReceiver();
        kaboom.checkCanTarget(game, sapper, OrderIds.selfdestruct, blast, receiver);
        if (receiver.getTarget() != null) {
            canyonDetonating = true;
            orders.issuePointOrder(sapper.getHandleId(), kaboom.getHandleId(), OrderIds.selfdestruct,
                    blast.getX(), blast.getY(), false);
            System.out.println("[HumanCampaignProbe] canyon sapper blast at " + blast);
        }
        return false;
    }

    static boolean immediate(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit, final int id) {
        for (final var ability : unit.getAbilities()) {
            final var activation = new BooleanAbilityActivationReceiver(); ability.checkCanUse(viewer.simulation, unit, id, activation);
            if (!activation.isOk()) continue;
            final var target = com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver.<Void>getInstance().reset();
            ability.checkCanTargetNoTarget(viewer.simulation,unit,id,target);
            if (target.isTargetable()) { orders.issueImmediateOrder(unit.getHandleId(), ability.getHandleId(), id, false); return true; }
        }
        return false;
    }
    private static void report(final War3MapViewer viewer,final MeleeUI ui,final CUnit hero) {
        ui.getCameraManager().setTarget(hero.getX(),hero.getY());
        if(viewer.simulation.getGameTurnTick()>=reportTick) {
            reportTick=viewer.simulation.getGameTurnTick()+400;
            System.out.println("[HumanCampaignProbe] "+mission+" tick="+viewer.simulation.getGameTurnTick()+" waypoint="+waypoint+" hero="+hero.getX()+","+hero.getY()+" life="+hero.getLife()+" army="+army(viewer).size()+" gold="+viewer.simulation.getPlayer(viewer.getLocalPlayerIndex()).getGold()+" lumber="+viewer.simulation.getPlayer(viewer.getLocalPlayerIndex()).getLumber());
            System.out.println("[HumanCampaignProbe] target="+(objectiveTarget==null?"route":objectiveTarget.getTypeId()+":"+objectiveTarget.getPlayerIndex()+":"+objectiveTarget.getX()+","+objectiveTarget.getY()+":"+objectiveTarget.getLife())+" behavior="+hero.getCurrentBehavior()+" order="+hero.getCurrentOrder());
            if ("human08".equals(mission) && waypoint > HUMAN08_MERCENARY_RALLY) {
                for (final CUnit unit : army(viewer)) {
                    if (unit.getCurrentAttacks().stream().anyMatch(attack -> attack.getRange() >= 200)) {
                        System.out.println("[HumanCampaignProbe] ranged " + unit.getTypeId() + " at " + unit.getX()
                                + "," + unit.getY() + " life=" + unit.getLife() + " order=" + unit.getCurrentOrder());
                    }
                }
            }
            if ("human09".equals(mission) && waypoint >= 8) {
                final var owner = viewer.simulation.getPlayer(viewer.getLocalPlayerIndex());
                final long remaining = viewer.simulation.getUnitsIncludingPending().stream()
                        .filter(unit -> unit.getPlayerIndex() == 6 && unit.isBuilding() && !unit.isDead()
                                && !"uzig".equals(unit.getTypeId().toString()) && !"uzg1".equals(unit.getTypeId().toString()))
                        .count();
                System.out.println("[HumanCampaignProbe] food=" + owner.getFoodUsed() + "/" + owner.getFoodCap()
                        + " objectivesRemaining=" + remaining + " regrouping=" + regrouping);
                System.out.println("[HumanCampaignProbe] mines=" + viewer.simulation.getUnitsIncludingPending().stream()
                        .filter(unit -> !unit.isDead() && unit.getFirstAbilityOfType(CAbilityGoldMine.class) != null)
                        .map(unit -> Math.round(unit.getX()) + "," + Math.round(unit.getY()) + " gold "
                                + unit.getFirstAbilityOfType(CAbilityGoldMine.class).getGold()).toList()
                        + " surviving workers=" + viewer.simulation.getUnitsIncludingPending().stream()
                                .filter(unit -> !unit.isDead() && unit.getPlayerIndex() == owner.getId()
                                        && unit.getTypeId().equals(War3ID.fromString("hpea"))).count());
            }
            else if ("human09".equals(mission) && waypoint >= 4) {
                final CUnit guardian = object(viewer.simulation.getGlobalScope(), "udg_Guardian");
                final CUnit muradin = object(viewer.simulation.getGlobalScope(), "udg_Muradin");
                final List<CUnit> expedition = object(viewer.simulation.getGlobalScope(), "udg_ValleyGroup");
                System.out.println("[HumanCampaignProbe] valley guardian=" + (guardian == null ? "missing" : guardian.getLife())
                        + " original expedition=" + (expedition == null ? 0 : expedition.stream().filter(unit -> !unit.isDead()).count())
                        + " Muradin=" + (muradin == null ? "missing" : Math.round(muradin.getX()) + ","
                                + Math.round(muradin.getY()) + " life " + Math.round(muradin.getLife()))
                        + " retreat=" + (frostmourneRetreatWaypoint >= 0 ? frostmourneRetreatWaypoint : "no"));
            }
            if ("human04".equals(mission)) {
                final var force = army(viewer);
                System.out.println("[HumanCampaignProbe] riflemen=" + count(force, "hrif") + " priests=" + count(force, "hmpr")
                        + " mortars=" + count(force, "hmtm"));
                for (final CUnit structure : viewer.simulation.getUnitsIncludingPending()) {
                    if (structure.getPlayerIndex() == viewer.getLocalPlayerIndex()
                            && !structure.isDead() && structure.isConstructing()) {
                        System.out.println("[HumanCampaignProbe] construction=" + structure.getTypeId()
                                + " progress=" + structure.getConstructionProgress() + " life=" + structure.getLife()
                                + " hasWorker=" + (structure.getWorker() != null));
                    }
                }
            }
            if(capture==null) { capture=mission+"-progress"; captureFrames=85; }
        }
    }
    private static void economy(final War3MapViewer viewer,final boolean defend) {
        final var game=viewer.simulation; final int player=viewer.getLocalPlayerIndex();
        final boolean andorhal = "human04".equals(mission);
        final boolean northrend = "human07".equals(mission) && waypoint >= 8;
        final boolean finale = "human09".equals(mission);
        final float[] expansionSite = andorhal ? ANDORHAL_EXPANSION : northrend ? NORTHREND_OUTPOST : FROSTMOURNE_EXPANSION;
        final var orders=new CPlayerUnitOrderExecutor(game,player);
        final var units=game.getUnitsIncludingPending().stream().filter(u->u.getPlayerIndex()==player && !u.isDead()).toList();
        final boolean hearthglen = defend && "human05".equals(mission);
        final long towers = count(units, "hgtw") + count(units, "hwtw");
        // Hearthglen transfers seven existing towers to the player. Reserve funds for three more.
        final boolean fortifying = hearthglen && (count(units, "hlum") == 0 || towers < 10);
        final var workers=units.stream().filter(u->u.getTypeId().equals(War3ID.fromString("hpea"))).toList();
        final CUnit town=units.stream().filter(u->List.of("htow","hkee","hcas").contains(u.getTypeId().toString())).findFirst().orElse(null);
        if(builder==null || builder.isDead()) builder=workers.isEmpty()?null:workers.get(workers.size()-1);
        if (town == null && (andorhal && waypoint > 0 || finale && waypoint >= 8) && builder != null
                && !(builder.getCurrentBehavior() instanceof CBehaviorHumanBuild)) {
            buildNear(viewer, orders, builder, "htow", expansionSite[0], expansionSite[1], 256, 640);
        }
        if(town==null || town.isConstructing()) return;
        final var owner=game.getPlayer(player);
        final var goldMines = game.getUnitsIncludingPending().stream().filter(unit -> !unit.isDead())
                .filter(unit -> {
                    final CAbilityGoldMine goldMine = unit.getFirstAbilityOfType(CAbilityGoldMine.class);
                    return goldMine != null && goldMine.getGold() > 0;
                }).toList();
        final CUnit mine = goldMines.stream().min(Comparator.comparingDouble(town::distanceSquaredNoCollision)).orElse(null);
        final CUnit expansion = finale || andorhal || northrend ? units.stream()
                .filter(unit -> List.of("htow", "hkee", "hcas").contains(unit.getTypeId().toString())
                        && unit.distanceSquaredNoCollision(expansionSite[0], expansionSite[1]) < 800 * 800)
                .findFirst().orElse(null) : null;
        final boolean addAndorhalTown = andorhal && waypoint > 0 && (mine == null
                || mine.getFirstAbilityOfType(CAbilityGoldMine.class).getGold() < 4800);
        final boolean readyForFrostmourneExpansion = finale && waypoint >= 5 && (mine == null || army(viewer).size() >= 14);
        final boolean needsExpansion = (readyForFrostmourneExpansion || addAndorhalTown || northrend)
                && expansion == null && builder != null;
        final int expansionCost = needsExpansion ? game.getUnitData().getUnitType(War3ID.fromString("htow")).getGoldCost() : 0;
        final boolean reservingExpansion = needsExpansion && (andorhal || owner.getGold() < expansionCost) && mine != null
                && town.distanceSquaredNoCollision(mine) < 1600 * 1600;
        if (needsExpansion && owner.getGold() >= expansionCost
                && !(builder.getCurrentBehavior() instanceof CBehaviorHumanBuild)) {
            buildNear(viewer, orders, builder, "htow", expansionSite[0], expansionSite[1], 256, 640);
        }
        final CUnit expansionMine = expansion == null || expansion.isConstructing() ? null
                : goldMines.stream().min(Comparator.comparingDouble(expansion::distanceSquaredNoCollision)).orElse(null);
        if (expansionMine != null && workers.stream().filter(unit ->
                expansion.distanceSquaredNoCollision(unit) < 1100 * 1100).count()
                + queued(List.of(expansion), "hpea") < 5) {
            train(viewer, orders, expansion, "hpea");
        }
        if(!reservingExpansion && workers.size()+Arrays.stream(town.getBuildQueue()).filter(War3ID.fromString("hpea")::equals).count()<(fortifying ? 8 : 12)) train(viewer,orders,town,"hpea");
        final List<CUnit> frostGoldWorkers = finale && mine != null ? workers.stream().filter(worker -> worker != builder)
                .filter(worker -> expansionMine == null || worker.distanceSquaredNoCollision(expansion) > 1100 * 1100)
                .sorted(Comparator.comparingInt(CUnit::getHandleId)).limit(5).toList() : List.of();
        int workerIndex=0;
        for(final CUnit worker:workers) {
            if (frostGoldWorkers.contains(worker)) {
                final var currentOrder = worker.getCurrentOrder();
                final var behavior = worker.getCurrentBehavior();
                final boolean returning = behavior != null && behavior.getHighlightOrderId() == OrderIds.returnresources;
                if (!returning && (currentOrder == null || currentOrder.getOrderId() != OrderIds.harvest
                        || currentOrder.getTarget(game) != mine)) {
                    CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, mine);
                }
                assignedWorkers.add(worker.getHandleId());
                workerIndex++;
                continue;
            }
            final boolean idleAfterRepair = (finale && waypoint >= 8 || (andorhal || northrend) && expansion != null)
                    && worker.getCurrentBehavior() != null
                    && worker.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.IDLE;
            if(worker!=builder && (assignedWorkers.add(worker.getHandleId()) || idleAfterRepair)) {
                if (expansionMine != null && expansion.distanceSquaredNoCollision(worker) < 1100 * 1100) {
                    CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.harvest, expansionMine);
                }
                else if(workerIndex<5 && mine!=null) CampaignPlaythroughProbe.targetOrder(viewer,orders,worker,OrderIds.harvest,mine);
                else for(final var tree:game.getDestructables().stream().filter(d->!d.isDead()).sorted(Comparator.comparingDouble(town::distanceSquaredNoCollision)).toList()) {
                    if(CampaignPlaythroughProbe.targetOrder(viewer,orders,worker,OrderIds.harvest,tree)) break;
                }
            }
            workerIndex++;
        }
        if (reservingExpansion) return;
        for(final CUnit unit:units) if(!unit.isConstructing()) {
            if((!fortifying || owner.getGold() >= 300) && unit.getTypeId().equals(War3ID.fromString("hbar"))) {
                if(finale && count(units,"hrif")+queued(units,"hrif")<6 && train(viewer,orders,unit,"hrif")) continue;
                if(finale && count(units,"hkni")+queued(units,"hkni")<6 && train(viewer,orders,unit,"hkni")) continue;
                if(count(units,"hfoo")+queued(units,"hfoo")<(andorhal || finale ? 8 : hearthglen ? 10 : 14)) train(viewer,orders,unit,"hfoo");
                else if(count(units,"hrif")+queued(units,"hrif")<8) train(viewer,orders,unit,"hrif");
                else if(!andorhal && count(units,"hmtm")+queued(units,"hmtm")<4) train(viewer,orders,unit,"hmtm");
            }
            if((andorhal || northrend) && unit.getTypeId().equals(War3ID.fromString("harm"))
                    && count(units,"hmtm")+queued(units,"hmtm")<(northrend ? 4 : 2)) train(viewer,orders,unit,"hmtm");
            if(finale && unit.getTypeId().equals(War3ID.fromString("harm"))
                    && count(units,"hmtm")+queued(units,"hmtm")<5) train(viewer,orders,unit,"hmtm");
            if (unit.getTypeId().equals(War3ID.fromString("hars"))) {
                final War3ID priestTraining = War3ID.fromString("Rhpt");
                if ((finale || hearthglen) && count(units, "hmpr") >= 2 && unit.getBuildQueue()[0] == null
                        && owner.getTechtreeUnlocked(priestTraining) < 1
                        && immediate(viewer, orders, unit, priestTraining.getValue())) continue;
                if (count(units, "hmpr") + queued(units, "hmpr") < 4) train(viewer, orders, unit, "hmpr");
            }
            if(unit.getTypeId().equals(War3ID.fromString("hwtw"))) immediate(viewer,orders,unit,War3ID.fromString("hgtw").getValue());
        }
        if (finale && owner.getGold() > 275) {
            final var damaged = units.stream().filter(unit -> unit.isBuilding() && !unit.isConstructing()
                    && unit.getLife() < unit.getMaximumLife() * 0.72f
                    && (unit == town || "hgtw".equals(unit.getTypeId().toString()))).toList();
            for (final CUnit worker : workers.stream().filter(worker -> !frostGoldWorkers.contains(worker))
                    .filter(worker -> worker != builder).limit(3).toList()) {
                if (worker == builder || worker.getCurrentOrder() != null
                        && worker.getCurrentOrder().getOrderId() == OrderIds.repair) continue;
                final CUnit repair = damaged.stream().filter(unit -> worker.distanceSquaredNoCollision(unit) < 1400 * 1400)
                        .min(Comparator.comparingDouble(worker::distanceSquaredNoCollision)).orElse(null);
                if (repair != null) CampaignPlaythroughProbe.targetOrder(viewer, orders, worker, OrderIds.repair, repair);
            }
        }
        if ((andorhal || hearthglen || finale || northrend) && builder != null) {
            final CUnit unfinished = units.stream().filter(unit -> unit.isConstructing() && unit.isConstructingPaused())
                    .filter(unit -> unit.getWorker() == null).findFirst().orElse(null);
            if (unfinished != null) {
                CampaignPlaythroughProbe.targetOrder(viewer, orders, builder, OrderIds.repair, unfinished);
                return;
            }
        }
        if (builder == null) return;
        final boolean needsFood = owner.getFoodCap() - owner.getFoodUsed() < 8 && count(units, "hhou") < 14;
        if (finale && waypoint >= 8) {
            if (builder.getCurrentBehavior() instanceof CBehaviorHumanBuild
                    || units.stream().anyMatch(CUnit::isConstructingOrUpgrading)) return;
        }
        else if (units.stream().anyMatch(CUnit::isConstructingOrUpgrading) || builder.getCurrentOrder() != null
                && builder.getCurrentOrder().getOrderId() != OrderIds.harvest
                && builder.getCurrentOrder().getOrderId() != OrderIds.stop) return;
        if (hearthglen && town.getLife() < town.getMaximumLife() * 0.8f
                && CampaignPlaythroughProbe.targetOrder(viewer, orders, builder, OrderIds.repair, town)) return;
        String building=null;
        if (hearthglen && count(units, "hbar") == 0 && count(units, "hlum") > 0) building = "hbar";
        else if (fortifying) building = count(units, "hlum") == 0 ? "hlum" : "hwtw";
        else if(needsFood) building="hhou";
        else if (finale && towers < 5 && count(units, "hlum") > 0
                && owner.getGold() >= 225) building = "hwtw";
        else if(count(units,"hbar")<(andorhal ? 1 : 2)) building="hbar";
        else if(count(units,"hlum")<1) building="hlum";
        else if(count(units,"hbla")<1) building="hbla";
        else if(town.getTypeId().equals(War3ID.fromString("htow"))) { immediate(viewer,orders,town,War3ID.fromString("hkee").getValue()); return; }
        else if(count(units,"hars")<1) building="hars";
        else if((andorhal || finale || northrend) && count(units,"harm")<1) building="harm";
        else if(defend && count(units,"hwtw")+count(units,"hgtw")<(finale ? 10 : 6)) building="hwtw";
        if(building==null) return;
        final boolean frontTower = (hearthglen || finale) && "hwtw".equals(building);
        final float[] front = finale ? FROSTMOURNE_DEFENSE : HEARTHGLEN_RALLY;
        final boolean protectedFarm = finale && "hhou".equals(building);
        final float buildX = frontTower ? front[0] : protectedFarm ? FROSTMOURNE_RALLY[0] : town.getX();
        final float buildY = frontTower ? front[1] : protectedFarm ? FROSTMOURNE_RALLY[1] : town.getY();
        buildNear(viewer, orders, builder, building, buildX, buildY, finale && frontTower ? 256 : 512, 1536);
    }

    private static void buildNear(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders,
            final CUnit worker, final String building, final float buildX, final float buildY,
            final int minimumRadius, final int maximumRadius) {
        final var game = viewer.simulation;
        final var ability=worker.getFirstAbilityOfType(AbstractCAbilityBuild.class);
        final int id=War3ID.fromString(building).getValue();
        final var current = worker.getCurrentBehavior();
        final var order = worker.getCurrentOrder();
        if (ability == null || current instanceof CBehaviorHumanBuild || order != null && order.getOrderId() == id
                && current != null && current.getBehaviorCategory() == CBehaviorCategory.MOVEMENT) {
            return;
        }
        final var activation=new BooleanAbilityActivationReceiver(); ability.checkCanUse(game,worker,id,activation);
        if(!activation.isOk()) return;
        for(int radius=minimumRadius;radius<=maximumRadius;radius+=64) for(int dx=-radius;dx<=radius;dx+=128) for(int dy=-radius;dy<=radius;dy+=128) {
            if(Math.max(Math.abs(dx),Math.abs(dy))!=radius) continue;
            final var target=new PointAbilityTargetCheckReceiver(); ability.checkCanTarget(game,worker,id,new AbilityPointTarget(buildX+dx,buildY+dy),target);
            if(target.getTarget()!=null) {
                orders.issuePointOrder(worker.getHandleId(),ability.getHandleId(),id,target.getTarget().getX(),target.getTarget().getY(),false);
                System.out.println("[HumanCampaignProbe] ordinary build "+building+" at "+target.getTarget()); return;
            }
        }
    }
    private static long count(final List<CUnit> units,final String id) { return units.stream().filter(u->u.getTypeId().equals(War3ID.fromString(id))).count(); }
    private static long queued(final List<CUnit> units,final String id) { return units.stream().mapToLong(u->Arrays.stream(u.getBuildQueue()).filter(War3ID.fromString(id)::equals).count()).sum(); }
    static boolean train(final War3MapViewer viewer,final CPlayerUnitOrderExecutor orders,final CUnit unit,final String type) {
        final var ability=unit.getFirstAbilityOfType(CAbilityQueue.class); if(ability==null) return false;
        final int id=War3ID.fromString(type).getValue(); final var activation=new BooleanAbilityActivationReceiver(); ability.checkCanUse(viewer.simulation,unit,id,activation);
        if(!activation.isOk()) return false; orders.issueImmediateOrder(unit.getHandleId(),ability.getHandleId(),id,false); return true;
    }
    private static List<CUnit> army(final War3MapViewer viewer) {
        return viewer.simulation.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == viewer.getLocalPlayerIndex()
            && !u.isDead() && !u.isHidden() && !u.isBuilding() && u.getFirstAbilityOfType(CAbilityAttack.class) != null
            && u.getFirstAbilityOfType(AbstractCAbilityBuild.class) == null).toList();
    }
    private static void remember(final GlobalScope globals, final String... names) {
        for (final String name : names) { final CUnit unit = hero(globals,name); expectedHeroes.put(name, CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit))); writeHero(mission+"-"+name+"-victory",unit); }
    }
    private static void writeHero(final String name, final CUnit unit) {
        writeHeroProperties(name, CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit)));
    }
    private static void writeHeroProperties(final String name, final Properties properties) {
        try {
            final Path file = Path.of(System.getProperty("user.home"),"human-campaign", name+".properties"); Files.createDirectories(file.getParent());
            try (var out=Files.newOutputStream(file)) { properties.store(out,"Retail objective-driven hero"); }
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static void skip(final GlobalScope globals, final MeleeUI ui, final String... names) {
        for (final String name : names) { final Trigger trigger = object(globals,name); if (trigger != null && trigger.isEnabled()) { ui.keyDown(Input.Keys.ESCAPE); return; } }
    }
    static void continueVictory(final MeleeUI ui) {
        if (capture != null) return;
        if ("Victory".equals(ui.getScoreDialogTitle())) ui.keyDown(Input.Keys.C);
        else if (ui.getVisibleScriptDialog() != null) ui.getVisibleScriptDialog().getButtons().get(0).getButtonFrame().onClick(Input.Buttons.LEFT);
    }
    public static void afterUIRender() {
        if (!ENABLED) return;
        if (capture != null && ++captureFrames >= 90) {
            final var dir=Gdx.files.absolute(Path.of(System.getProperty("user.home"),"human-campaign").toString()); dir.mkdirs();
            final Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            final PixmapIO.PNG writer=new PixmapIO.PNG();
            try { writer.setFlipY(true); writer.write(dir.child(capture+".png"),pixels); }
            catch(IOException e) { throw new IllegalStateException(e); }
            finally { writer.dispose(); pixels.dispose(); }
            System.out.println("[HumanCampaignProbe] captured "+capture); capture=null;
        }
        if (finished && capture == null) { System.out.println("[HumanCampaignProbe] complete: reached "+mission); Gdx.app.exit(); }
    }
    private static boolean completed(final GlobalScope globals, final String name) { final CQuest quest=object(globals,name); return quest!=null && quest.isCompleted(); }
    private static boolean flag(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(BooleanJassValueVisitor.getInstance()); }
    private static <T> T object(final GlobalScope globals, final String name) { return globals.getGlobalId(name)<0 ? null : globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance()); }
    private static <T> T object(final GlobalScope globals, final String name, final Class<T> type) { return object(globals,name); }
    private static void check(boolean condition,String message) { if(!condition) throw new IllegalStateException("Human campaign audit: "+message); }
}
