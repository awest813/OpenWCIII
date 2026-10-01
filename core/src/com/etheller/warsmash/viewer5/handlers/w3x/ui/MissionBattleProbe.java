package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.List;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/** Opt-in mass combat in a cleared walkable arena, using retail ranged units and actual AI orders. */
final class MissionBattleProbe {
    static final boolean ENABLED = "battle".equals(System.getProperty("warsmash.missionResumeScenario", ""));
    static final int CONTINUATION_TICKS = 2400;
    static final String SCRIPT = """
native CheckpointBattleArena takes real x, real y returns nothing
globals
    group cpLargeArmy = null
    integer cpLargeDeaths = 0
    integer cpLargeDeaths2 = 0
    integer cpLargeDeaths3 = 0
endglobals
function CheckpointBattle_Clear takes nothing returns nothing
    call RemoveDestructable(GetEnumDestructable())
endfunction
function CheckpointBattle_Death takes nothing returns nothing
    set cpLargeDeaths = cpLargeDeaths + 1
    if GetOwningPlayer(GetTriggerUnit()) == Player(2) then
        set cpLargeDeaths2 = cpLargeDeaths2 + 1
    else
        set cpLargeDeaths3 = cpLargeDeaths3 + 1
    endif
endfunction
function CheckpointAudit_LargeBattle takes nothing returns nothing
    local real centerX = GetUnitX(udg_Arthas) + 768
    local real centerY = GetUnitY(udg_Arthas)
    local rect area = Rect(centerX - 700, centerY - 700, centerX + 700, centerY + 700)
    local trigger death = CreateTrigger()
    local unit soldier
    local integer side = 2
    local integer i
    local integer id
    local real x
    call EnumDestructablesInRect(area, null, function CheckpointBattle_Clear)
    call RemoveRect(area)
    call CheckpointBattleArena(centerX, centerY)
    call TriggerSleepAction(0.10)
    call SetPlayerAlliance(Player(2), Player(3), ALLIANCE_PASSIVE, false)
    call SetPlayerAlliance(Player(3), Player(2), ALLIANCE_PASSIVE, false)
    call SetPlayerAlliance(Player(1), Player(2), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(2), Player(1), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(1), Player(3), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(3), Player(1), ALLIANCE_PASSIVE, true)
    call FogEnable(false)
    call FogMaskEnable(false)
    call SetCameraPosition(centerX, centerY)
    set cpLargeArmy = CreateGroup()
    call TriggerAddAction(death, function CheckpointBattle_Death)
    loop
        set i = 0
        loop
            set id = 'hrif'
            if i >= 12 then
                set id = 'ohun'
            endif
            if i >= 24 then
                set id = 'ucry'
            endif
            if i >= 36 then
                set id = 'earc'
            endif
            set x = 128 + (i / 12) * 48
            if side == 3 then
                set x = -x
            endif
            set soldier = CreateUnit(Player(side), id, centerX + x, centerY - 352 + ModuloInteger(i, 12) * 64, 0)
            call GroupAddUnit(cpLargeArmy, soldier)
            call TriggerRegisterUnitEvent(death, soldier, EVENT_UNIT_DEATH)
            set i = i + 1
            exitwhen i == 48
        endloop
        call DefineStartLocation(side, centerX, centerY)
        call StartCampaignAI(Player(side), "CheckpointBattle.ai")
        set side = side + 1
        exitwhen side == 4
    endloop
endfunction
""";
    static final String AI_SCRIPT = """
globals
    integer cpBattleAiPulse = 0
    integer cpBattleAiLocal = 0
    integer cpBattleAiGroup = 0
endglobals
function CheckpointBattleAI_Pulse takes nothing returns nothing
    loop
        call Sleep(1.0)
        set cpBattleAiPulse = cpBattleAiPulse + 1
    endloop
endfunction
function main takes nothing returns nothing
    local integer remembered = 923
    local real x = GetStartLocationX(GetAiPlayer())
    local real y = GetStartLocationY(GetAiPlayer())
    call SetCaptainHome(0, x, y)
    call AddAssault(12, 'hrif')
    call AddAssault(12, 'ohun')
    call AddAssault(12, 'ucry')
    call AddAssault(12, 'earc')
    set cpBattleAiGroup = CaptainGroupSize()
    call CaptainAttack(x, y)
    call StartThread(function CheckpointBattleAI_Pulse)
    call Sleep(20.0)
    set cpBattleAiLocal = remembered
endfunction
""";
    static void install(final com.etheller.interpreter.ast.util.JassProgram program, final War3MapViewer viewer) {
        program.getJassNativeManager().createNative("CheckpointBattleArena", (arguments, globals, scope) -> {
            final float x = arguments.get(0).visit(com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor.getInstance()).floatValue();
            final float y = arguments.get(1).visit(com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor.getInstance()).floatValue();
            final var grid = viewer.simulation.getPathingGrid();
            for (int row = grid.getCellY(y - 700); row <= grid.getCellY(y + 700); row++) {
                for (int col = grid.getCellX(x - 700); col <= grid.getCellX(x + 700); col++) {
                    if (col >= 0 && row >= 0 && col < grid.getWidth() && row < grid.getHeight()) grid.setCellPathing(col, row, (short) 0);
                }
            }
            return null;
        });
    }
    static boolean ready(final War3MapViewer viewer) {
        return viewer.simulation.getActiveProjectileCount() >= 4 && army(viewer).stream().filter(u -> !u.isDead()).count() >= 70;
    }
    static void assertCheckpoint(final War3MapViewer viewer) {
        check(army(viewer).size() == 96 && ready(viewer), "Expected 96 combatants and live missiles: " + describe(viewer));
        for (int player = 2; player <= 3; player++) {
            final var globals = viewer.simulation.getAiEnvironment(player).getGlobalScope();
            check(integer(globals, "cpBattleAiGroup") == 48, "Captain lost its army");
        }
        System.out.println("[MissionResumeProbe] large battle checkpoint verified: " + describe(viewer));
    }
    static void assertContinuation(final War3MapViewer viewer) {
        final var globals = viewer.simulation.getGlobalScope();
        final long deaths = army(viewer).stream().filter(CUnit::isDead).count();
        check(deaths >= 32, "Mass combat did not cause enough casualties: " + describe(viewer));
        check(integer(globals, "cpLargeDeaths") == deaths, "Combat death events lost or duplicated");
        check(integer(globals, "cpLargeDeaths2") > 0 && integer(globals, "cpLargeDeaths3") > 0, "Both armies must take casualties");
        for (int player = 2; player <= 3; player++) {
            final var ai = viewer.simulation.getAiEnvironment(player).getGlobalScope();
            check(integer(ai, "cpBattleAiLocal") == 923 && integer(ai, "cpBattleAiPulse") >= 50, "Captain threads did not continue");
        }
        System.out.println("[MissionResumeProbe] large battle continuation verified: " + describe(viewer));
    }
    static String describe(final War3MapViewer viewer) {
        return "tick=" + viewer.simulation.getGameTurnTick() + " living=" + army(viewer).stream().filter(u -> !u.isDead()).count()
                + " first=" + army(viewer).get(0).getX() + "," + army(viewer).get(0).getY() + "/" + army(viewer).get(0).getCurrentBehavior()
                + " last=" + army(viewer).get(95).getX() + "," + army(viewer).get(95).getY()
                + " deaths=" + integer(viewer.simulation.getGlobalScope(), "cpLargeDeaths") + " missiles=" + viewer.simulation.getActiveProjectileCount();
    }
    private static List<CUnit> army(final War3MapViewer viewer) {
        return viewer.simulation.getGlobalScope().getGlobal("cpLargeArmy").visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
    }
    private static int integer(final GlobalScope globals, final String name) {
        return globals.getGlobal(name).visit(IntegerJassValueVisitor.getInstance());
    }
    private static void check(final boolean condition, final String message) {
        if (!condition) throw new IllegalStateException("Mission battle audit: " + message);
    }
}
