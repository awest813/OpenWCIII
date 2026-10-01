package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
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
    private static final float[][] HUMAN03 = {
        {3360,-3200}, {3000,-1800}, {4300,-700}, {6400,700}, // Discover the Fountain quest.
        {4000,600}, {5000,2200}, {3500,3600}, {2000,4000}, {1000,3500}, {0,2400}, {-1300,2500}, {-3400,2300},
        {-5200,2600}, {-6800,3800}, {-5300,2500}, {-3500,1500},
        {-2700,-600}, {-2200,-3000}, // The priests discover the infected granary.
        {-4100,-2450}, {-5400,-4100}, {-6200,-3000}, {-6100,-1600}, {-6200,600}
    };
    private static String mission = "";
    private static int waypoint, orderTick, reportTick;
    private static boolean victory, finished;
    private static long started;
    private static final Map<String, Properties> expectedHeroes = new HashMap<>();
    private static String capture;
    private static int captureFrames;
    private static CUnit builder;
    private static CUnit objectiveTarget;
    private static final Set<Integer> assignedWorkers = new HashSet<>();
    private static final float[][] HUMAN04 = {{-2300,-3500},{-3500,-2300},{-4200,-800},{-4200,1100},{-3700,2400},{-2500,2700},{-2000,4100},{-2230,4900}};
    private HumanCampaignPlaythroughProbe() { }

    public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
        if (!ENABLED || finished || !initialized || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
        final String map = viewer.getCurrentMapPath().replace('\\', '/').toLowerCase();
        final String name = map.substring(map.lastIndexOf('/') + 1).replace(".w3m", "");
        if (name.equals("human01") || name.equals("human02") || name.equals("human02interlude")) return;
        final var game = viewer.simulation;
        final var globals = game.getGlobalScope();
        if (!mission.equals(name)) {
            mission = name; waypoint = orderTick = reportTick = 0; victory = false; started = System.nanoTime(); builder=null; objectiveTarget=null; assignedWorkers.clear();
            if (!name.contains("interlude")) for (final var hero : expectedHeroes.entrySet()) {
                if (name.equals("human06") && hero.getKey().equals("Jaina")) continue; // Jaina leaves in the preceding story scene.
                final CUnit unit = hero(globals, hero.getKey());
                check(unit != null && hero.getValue().equals(CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit))),
                        name + " lost " + hero.getKey() + " carryover");
                writeHero(name + "-" + hero.getKey() + "-restored", unit);
            }
            if (!name.contains("interlude")) expectedHeroes.clear();
            System.out.println("[HumanCampaignProbe] entered " + name + "; carryover verified");
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
            if (waypoint < HUMAN03.length && arthas.distanceSquaredNoCollision(HUMAN03[waypoint][0], HUMAN03[waypoint][1]) < 260 * 260) waypoint++;
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
                check(kel.isDead() && completed(globals,"udg_QuestAndorhal"),"Human04 did not defeat Kel'Thuzad after Andorhal");
                if (!victory) { remember(globals,"Arthas","Jaina"); victory=true; capture=name+"-victory"; captureFrames=0; System.out.println("[HumanCampaignProbe] objectives complete "+name+"; Andorhal and Kel'Thuzad; xp="+arthas.getHeroData().getXp()); }
                skip(globals,ui,"gg_trg_EndingCancelled"); continueVictory(ui); return;
            }
            final CQuest andorhal=object(globals,"udg_QuestAndorhal");
            if (andorhal==null || !andorhal.isDiscovered() || !ui.isUserControlEnabled() || arthas.isPaused() || game.isGamePaused() || game.getGameTurnTick()<orderTick) return;
            orderTick=game.getGameTurnTick()+20;
            economy(viewer,false);
            if (army(viewer).size()<18 && waypoint==0) micro(viewer,arthas,HUMAN04[0]);
            else {
                if (waypoint<HUMAN04.length && arthas.distanceSquaredNoCollision(HUMAN04[waypoint][0],HUMAN04[waypoint][1])<450*450) waypoint++;
                final CUnit kel=object(globals,"gg_unit_uktn_0133");
                micro(viewer,arthas,waypoint<HUMAN04.length ? HUMAN04[waypoint] : new float[]{kel.getX(),kel.getY()});
            }
            report(viewer,ui,arthas);
        }
        else if (name.equals("human05")) {
            skip(globals,ui,"gg_trg_Intro_Cancelled");
            if (flag(globals,"udg_GAMEOVER")) {
                if (!victory) {
                    check(completed(globals,"udg_GrainCaravanQuest"),"Human05 grain caravan optional objective incomplete");
                    check(!object(globals,"gg_unit_hkee_0043",CUnit.class).isDead(),"Hearthglen keep destroyed");
                    win(globals,name,"Hearthglen defended and grain caravan destroyed","Arthas","Jaina");
                }
                skip(globals,ui,"gg_trg_Victory_Cancel"); continueVictory(ui); return;
            }
            if (!ready(viewer,ui,arthas)) return;
            economy(viewer,true);
            final List<CUnit> wagons=object(globals,"udg_TheCaravanWagons");
            final CUnit wagon=wagons==null?null:wagons.stream().filter(u->!u.isDead()).min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
            objectiveTarget=army(viewer).size()>=18 ? wagon : null;
            micro(viewer,arthas,objectiveTarget==null?new float[]{5600,-4900}:new float[]{wagon.getX(),wagon.getY()});
            report(viewer,ui,arthas);
        }
        else if (name.equals("human06")) {
            skip(globals,ui,"gg_trg_Intro_Cancel","gg_trg_Malganis_First_Teleport_Cin_Skip");
            if (flag(globals,"udg_GameOver")) {
                if (!victory) { check(real(globals,"udg_ArthasDenials")>=100,"Culling ended without 100 native denials"); win(globals,name,"100 citizens denied through combat","Arthas"); }
                skip(globals,ui,"gg_trg_Victory_Cinematic_Skip"); continueVictory(ui); return;
            }
            if (!ready(viewer,ui,arthas)) return;
            economy(viewer,false);
            final float[][] cityApproach={{10000,4000},{8700,3000},{8200,1600},{6800,1000},{6000,0}};
            if(waypoint<cityApproach.length) {
                if(arthas.distanceSquaredNoCollision(cityApproach[waypoint][0],cityApproach[waypoint][1])<300*300) waypoint++;
                if(waypoint<cityApproach.length) { objectiveTarget=null; micro(viewer,arthas,cityApproach[waypoint]); report(viewer,ui,arthas); return; }
            }
            objectiveTarget=game.getUnitsIncludingPending().stream().filter(u->!u.isDead() && !u.isHidden() && !u.isInvulnerable()
                && (u.getPlayerIndex()==8 || ((u.getPlayerIndex()==10 || u.getPlayerIndex()==11) && u.getTypeId().toString().startsWith("ncb"))))
                .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
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
            final float[][] route={{-2500,-5300},{-500,-5400},{1700,-5100},{4100,-5200},{4600,-2300},{5000,0},{5200,2500},{5200,4800}};
            if(waypoint<route.length && arthas.distanceSquaredNoCollision(route[waypoint][0],route[waypoint][1])<300*300) waypoint++;
            if(waypoint>=4) { establishTown(viewer,4608,-2304); economy(viewer,false); }
            if(waypoint>=5 && army(viewer).size()<20) micro(viewer,arthas,new float[]{4600,-2300});
            else if(waypoint<route.length) micro(viewer,arthas,route[waypoint]);
            else {
                objectiveTarget=game.getUnitsIncludingPending().stream().filter(u->!u.isDead() && !u.isHidden() && u.isBuilding() && u.getPlayerIndex()==6).min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
                if(objectiveTarget!=null) micro(viewer,arthas,new float[]{objectiveTarget.getX(),objectiveTarget.getY()});
            }
            report(viewer,ui,arthas);
        }
        else if(name.equals("human08")) {
            skip(globals,ui,"gg_trg_IntroCinematicEscape","gg_trg_GauntletCinematicEscape");
            if(completed(globals,"udg_QuestDestroyShips")) {
                if(!victory) { check(integer(globals,"udg_NumberShipsRemaining")==0,"Ships survived Human08"); win(globals,name,"all five ships destroyed through combat","Arthas","Muradin"); }
                skip(globals,ui,"gg_trg_ExitVictoryCinematicEscape"); continueVictory(ui); return;
            }
            if(!ready(viewer,ui,arthas)) return;
            objectiveTarget=game.getUnitsIncludingPending().stream().filter(u->!u.isDead() && u.getTypeId().equals(War3ID.fromString("nbsp"))).min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
            if(objectiveTarget!=null) micro(viewer,arthas,new float[]{objectiveTarget.getX(),objectiveTarget.getY()});
            report(viewer,ui,arthas);
        }
        else if(name.equals("human09")) {
            skip(globals,ui,"gg_trg_IntroCinematicEscape","gg_trg_FrostmourneCinematicEscapeGood","gg_trg_FrostmourneCinematicEscapeEvil");
            if(completed(globals,"udg_QuestMalganis")) {
                if(!victory) { check(completed(globals,"udg_QuestFrostmourne"),"Frostmourne objective incomplete"); win(globals,name,"Frostmourne acquired and Mal'Ganis defeated","Arthas"); }
                skip(globals,ui,"gg_trg_ExitCinematicEscape","gg_trg_ExitCinematicEscapeNoFade");
                if("Victory".equals(ui.getScoreDialogTitle()) && capture==null) {
                    check(CampaignProgressStore.get().isCampaignAvailable(2),"Undead campaign was not unlocked by Human09");
                    finished=true; System.out.println("[HumanCampaignProbe] final native Human campaign victory; Undead campaign unlocked");
                }
                return;
            }
            if(!ready(viewer,ui,arthas)) return;
            economy(viewer,true);
            if(!completed(globals,"udg_QuestFrostmourne")) {
                final float[][] route={{-5500,-5800},{-6800,-4000},{-6500,-1800},{-6500,1200},{-6000,3500},{-5000,5500},{-3000,5800},{-2800,6368}};
                if(waypoint<route.length && arthas.distanceSquaredNoCollision(route[waypoint][0],route[waypoint][1])<300*300) waypoint++;
                micro(viewer,arthas,route[Math.min(waypoint,route.length-1)]);
            }
            else {
                objectiveTarget=game.getUnitsIncludingPending().stream().filter(u->!u.isDead() && !u.isHidden() && !u.isInvulnerable() && u.getPlayerIndex()==6 && u.isBuilding()).min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
                if(objectiveTarget==null) objectiveTarget=object(globals,"udg_Malganis");
                micro(viewer,arthas,new float[]{objectiveTarget.getX(),objectiveTarget.getY()});
            }
            report(viewer,ui,arthas);
        }
        else throw new IllegalStateException("Human campaign audit: no objective driver for " + name);
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

    private static void micro(final War3MapViewer viewer, final CUnit arthas, final float[] destination) {
        final var game = viewer.simulation;
        final var orders = new CPlayerUnitOrderExecutor(game, viewer.getLocalPlayerIndex());
        final var army = army(viewer);
        for (final CUnit unit : army) {
            if (unit.isPaused()) continue;
            if (unit.getHeroData() != null) {
                for (final String skill : unit == arthas ? new String[]{"AHds","AHhb","AHad","AHre"} : unit.getTypeId().equals(War3ID.fromString("Hmbr")) ? new String[]{"AHtb","AHtc","AHbh","AHav"} : new String[]{"AHwe","AHab","AHbz"}) {
                    final int id = War3ID.fromString(skill).getValue();
                    final var activation = new BooleanAbilityActivationReceiver();
                    unit.getHeroData().checkCanUse(game, unit, id, activation);
                    if (activation.isOk()) { orders.issueImmediateOrder(unit.getHandleId(), unit.getHeroData().getHandleId(), id, false); break; }
                }
            }
            final CUnit enemy = objectiveTarget!=null && !objectiveTarget.isDead() && arthas.distanceSquaredNoCollision(objectiveTarget)<650*650 ? objectiveTarget : game.getUnitsIncludingPending().stream().filter(u -> !u.isDead() && !u.isHidden() && !u.isInvulnerable()
                    && !game.getPlayer(unit.getPlayerIndex()).hasAlliance(u.getPlayerIndex(), CAllianceType.PASSIVE)
                    && u.getPlayerIndex() != unit.getPlayerIndex() && arthas.distanceSquaredNoCollision(u) < 650 * 650)
                    .min(Comparator.comparingDouble(arthas::distanceSquaredNoCollision)).orElse(null);
            if (unit == arthas) {
                if (!arthas.isInvulnerable() && arthas.getLife() < arthas.getMaximumLife() * 0.6f && immediate(viewer, orders, arthas, OrderIds.divineshield)) continue;
                final CUnit injured = army.stream().filter(u -> u != arthas && u.getMaximumLife() - u.getLife() > CampaignPlaythroughProbe.healingThreshold(arthas)
                    && arthas.distanceSquaredNoCollision(u) < 600 * 600).max(Comparator.comparingDouble(u -> u.getMaximumLife() - u.getLife())).orElse(null);
                if (injured != null && CampaignPlaythroughProbe.targetOrder(viewer, orders, arthas, OrderIds.holybolt, injured)) continue;
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
                if (unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.ATTACK)
                    if(!CampaignPlaythroughProbe.targetOrder(viewer, orders, unit, OrderIds.attack, enemy)) CampaignPlaythroughProbe.pointOrder(viewer,orders,unit,destination[0],destination[1]);
            }
            else CampaignPlaythroughProbe.pointOrder(viewer, orders, unit, destination[0], destination[1]);
        }
    }

    private static boolean immediate(final War3MapViewer viewer, final CPlayerUnitOrderExecutor orders, final CUnit unit, final int id) {
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
            if(capture==null) { capture=mission+"-progress"; captureFrames=85; }
        }
    }
    private static void economy(final War3MapViewer viewer,final boolean defend) {
        final var game=viewer.simulation; final int player=viewer.getLocalPlayerIndex();
        final var orders=new CPlayerUnitOrderExecutor(game,player);
        final var units=game.getUnitsIncludingPending().stream().filter(u->u.getPlayerIndex()==player && !u.isDead()).toList();
        final var workers=units.stream().filter(u->u.getTypeId().equals(War3ID.fromString("hpea"))).toList();
        final CUnit town=units.stream().filter(u->List.of("htow","hkee","hcas").contains(u.getTypeId().toString())).findFirst().orElse(null);
        if(town==null || town.isConstructing()) return;
        if(builder==null || builder.isDead()) builder=workers.isEmpty()?null:workers.get(workers.size()-1);
        if(workers.size()+Arrays.stream(town.getBuildQueue()).filter(War3ID.fromString("hpea")::equals).count()<12) train(viewer,orders,town,"hpea");
        final CUnit mine=game.getUnitsIncludingPending().stream().filter(u->!u.isDead() && u.getTypeId().equals(War3ID.fromString("ngol"))).min(Comparator.comparingDouble(town::distanceSquaredNoCollision)).orElse(null);
        int workerIndex=0;
        for(final CUnit worker:workers) {
            if(worker!=builder && assignedWorkers.add(worker.getHandleId())) {
                if(workerIndex<5 && mine!=null) CampaignPlaythroughProbe.targetOrder(viewer,orders,worker,OrderIds.harvest,mine);
                else for(final var tree:game.getDestructables().stream().filter(d->!d.isDead()).sorted(Comparator.comparingDouble(town::distanceSquaredNoCollision)).toList()) {
                    if(CampaignPlaythroughProbe.targetOrder(viewer,orders,worker,OrderIds.harvest,tree)) break;
                }
            }
            workerIndex++;
        }
        for(final CUnit unit:units) if(!unit.isConstructing()) {
            if(unit.getTypeId().equals(War3ID.fromString("hbar"))) {
                if(count(units,"hfoo")+queued(units,"hfoo")<14) train(viewer,orders,unit,"hfoo");
                else if(count(units,"hrif")+queued(units,"hrif")<8) train(viewer,orders,unit,"hrif");
                else if(count(units,"hmtm")+queued(units,"hmtm")<4) train(viewer,orders,unit,"hmtm");
            }
            if(unit.getTypeId().equals(War3ID.fromString("hars")) && count(units,"hmpr")+queued(units,"hmpr")<4) train(viewer,orders,unit,"hmpr");
            if(unit.getTypeId().equals(War3ID.fromString("hwtw"))) immediate(viewer,orders,unit,War3ID.fromString("hgtw").getValue());
        }
        if(builder==null || units.stream().anyMatch(CUnit::isConstructingOrUpgrading) || builder.getCurrentOrder()!=null && builder.getCurrentOrder().getOrderId()!=OrderIds.harvest && builder.getCurrentOrder().getOrderId()!=OrderIds.stop) return;
        final var owner=game.getPlayer(player);
        String building=null;
        if(owner.getFoodCap()-owner.getFoodUsed()<8 && count(units,"hhou")<14) building="hhou";
        else if(count(units,"hbar")<2) building="hbar";
        else if(count(units,"hlum")<1) building="hlum";
        else if(count(units,"hbla")<1) building="hbla";
        else if(town.getTypeId().equals(War3ID.fromString("htow"))) { immediate(viewer,orders,town,War3ID.fromString("hkee").getValue()); return; }
        else if(count(units,"hars")<1) building="hars";
        else if(defend && count(units,"hwtw")+count(units,"hgtw")<6) building="hwtw";
        if(building==null) return;
        final var ability=builder.getFirstAbilityOfType(AbstractCAbilityBuild.class);
        final int id=War3ID.fromString(building).getValue();
        final var activation=new BooleanAbilityActivationReceiver(); ability.checkCanUse(game,builder,id,activation);
        if(!activation.isOk()) return;
        for(int radius=512;radius<=1536;radius+=64) for(int dx=-radius;dx<=radius;dx+=128) for(int dy=-radius;dy<=radius;dy+=128) {
            if(Math.max(Math.abs(dx),Math.abs(dy))!=radius) continue;
            final var target=new PointAbilityTargetCheckReceiver(); ability.checkCanTarget(game,builder,id,new AbilityPointTarget(town.getX()+dx,town.getY()+dy),target);
            if(target.getTarget()!=null) {
                orders.issuePointOrder(builder.getHandleId(),ability.getHandleId(),id,target.getTarget().getX(),target.getTarget().getY(),false);
                System.out.println("[HumanCampaignProbe] ordinary build "+building+" at "+target.getTarget()); return;
            }
        }
    }
    private static long count(final List<CUnit> units,final String id) { return units.stream().filter(u->u.getTypeId().equals(War3ID.fromString(id))).count(); }
    private static long queued(final List<CUnit> units,final String id) { return units.stream().mapToLong(u->Arrays.stream(u.getBuildQueue()).filter(War3ID.fromString(id)::equals).count()).sum(); }
    private static boolean train(final War3MapViewer viewer,final CPlayerUnitOrderExecutor orders,final CUnit unit,final String type) {
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
        try {
            final Path file = Path.of(System.getProperty("user.home"),"human-campaign", name+".properties"); Files.createDirectories(file.getParent());
            try (var out=Files.newOutputStream(file)) { CampaignPlaythroughProbe.heroProperties(CGameSave.snapshotUnit(unit)).store(out,"Retail objective-driven hero"); }
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static void skip(final GlobalScope globals, final MeleeUI ui, final String... names) {
        for (final String name : names) { final Trigger trigger = object(globals,name); if (trigger != null && trigger.isEnabled()) { ui.keyDown(Input.Keys.ESCAPE); return; } }
    }
    private static void continueVictory(final MeleeUI ui) {
        if (capture != null) return;
        if (ui.getVisibleScriptDialog() != null) ui.getVisibleScriptDialog().getButtons().get(0).getButtonFrame().onClick(Input.Buttons.LEFT);
        else if ("Victory".equals(ui.getScoreDialogTitle())) ui.keyDown(Input.Keys.C);
    }
    public static void afterUIRender() {
        if (!ENABLED) return;
        if (capture != null && ++captureFrames >= 90) {
            final var dir=Gdx.files.absolute(Path.of(System.getProperty("user.home"),"human-campaign").toString()); dir.mkdirs();
            final Pixmap pixels=ScreenUtils.getFrameBufferPixmap(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
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
