package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.harvest.CBehaviorAcolyteHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.harvest.CBehaviorWispHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/** Controlled economy fixture, installed only by the separate-process mission audit. */
final class MissionEconomyProbe {
    private static final String SCENARIO = System.getProperty("warsmash.missionResumeScenario", "");
    static final boolean ENABLED = java.util.Set.of("economy", "economy-orc", "economy-undead", "economy-nightelf").contains(SCENARIO);
    private static final String[] IDS = switch (SCENARIO) {
        case "economy-orc" -> new String[] { "ogre", "obar", "ofor", "opeo", "opeo", "otrb", "ogru", "Rome" };
        case "economy-undead" -> new String[] { "unpl", "usep", "ugrv", "uaco", "ugho", "uzig", "ugho", "Rume" };
        case "economy-nightelf" -> new String[] { "etol", "eaom", "edob", "ewsp", "ewsp", "emow", "earc", "Resm" };
        default -> new String[] { "htow", "hbar", "hbla", "hpea", "hpea", "hhou", "hfoo", "Rhme" };
    };
    private static final int INITIAL_TRAINED = SCENARIO.equals("economy-undead") ? 1 : 0;
    static final int CONTINUATION_TICKS = 1500;
    static final String SCRIPT = """
globals
    unit cpGoldWorker = null
    unit cpWoodWorker = null
    unit cpBuilder = null
    unit cpBarracks = null
    unit cpSmith = null
    unit cpMine = null
    unit cpTown = null
endglobals
function CheckpointEconomy_Clear takes nothing returns nothing
    call RemoveDestructable(GetEnumDestructable())
endfunction
function CheckpointAudit_Economy takes nothing returns nothing
    local rect area = Rect(-960, -960, 960, 960)
    // An isolated, cleared work area; production uses unmodified retail object data.
    call EnumDestructablesInRect(area, null, function CheckpointEconomy_Clear)
    call RemoveRect(area)
    call TriggerSleepAction(0.10)
    call SetPlayerAlliance(Player(1), Player(3), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(3), Player(1), ALLIANCE_PASSIVE, true)
    call SetPlayerState(Player(3), PLAYER_STATE_RESOURCE_GOLD, 5000)
    call SetPlayerState(Player(3), PLAYER_STATE_RESOURCE_LUMBER, 5000)
    set cpTown = CreateUnit(Player(3), 'htow', 0, 0, 0)
    set cpBarracks = CreateUnit(Player(3), 'hbar', 512, 512, 0)
    set cpSmith = CreateUnit(Player(3), 'hbla', 512, -512, 0)
    set cpGoldWorker = CreateUnit(Player(3), 'hpea', -256, 0, 180)
    set cpWoodWorker = CreateUnit(Player(3), 'hpea', 0, 256, 90)
    set cpBuilder = CreateUnit(Player(3), 'hpea', 256, 0, 0)
    set cpMine = CreateUnit(Player(PLAYER_NEUTRAL_PASSIVE), 'ngol', -640, 0, 0)
    call SetResourceAmount(cpMine, 10000)
    call CreateDestructable('LTlt', 0, 512, 0, 1, 0)
    call CreateDestructable('LTlt', -128, 512, 0, 1, 0)
    call SetPlayerState(Player(3), PLAYER_STATE_RESOURCE_FOOD_CAP, 100)
    call DefineStartLocation(3, 0, 0)
    call TriggerSleepAction(0.50)
    // RACE_PREPARATION
    call StartCampaignAI(Player(3), "CheckpointEconomy.ai")
endfunction
""".replace("'htow'", "'" + IDS[0] + "'").replace("'hbar'", "'" + IDS[1] + "'")
            .replace("'hbla'", "'" + IDS[2] + "'").replace("'hpea'", "'" + IDS[3] + "'")
            .replace("set cpWoodWorker = CreateUnit(Player(3), '" + IDS[3] + "'", "set cpWoodWorker = CreateUnit(Player(3), '" + IDS[4] + "'")
            .replace("-640, 0, 0", SCENARIO.equals("economy-nightelf") ? "-384, 0, 0" : "-640, 0, 0")
            .replace("// RACE_PREPARATION", SCENARIO.equals("economy-undead") ? "call SetBlight(Player(3), 0, 0, 1024, true)"
                    : SCENARIO.equals("economy-nightelf") ? "call FogEnable(false)\n    call FogMaskEnable(false)\n    call IssueTargetOrderById(cpTown, " + OrderIds.entangleinstant + ", cpMine)" : "");
    static final String AI_SCRIPT = """
globals
    integer cpEconomyPulse = 0
    integer cpEconomyLocal = 0
endglobals
function main takes nothing returns nothing
    local integer remembered = 927
    call Sleep(1.0)
    call SetCaptainHome(0, 64, 64)
    // PREPARE_MINE
    call HarvestGold(0, 1)
    call HarvestWood(0, 1)
    call SetProduce(1, 'hhou', 0)
    call SetProduce(2, 'hfoo', 0)
    call SetProduce(2, 'hfoo', 0)
    call SetUpgrade('Rhme')
    loop
        call Sleep(1.0)
        set cpEconomyPulse = cpEconomyPulse + 1
        call HarvestGold(0, 1)
        call HarvestWood(0, 1)
        call SetProduce(2, 'hfoo', 0)
        call SetUpgrade('Rhme')
        exitwhen cpEconomyPulse >= 12
    endloop
    set cpEconomyLocal = remembered
endfunction
""".replace("'hhou'", "'" + IDS[5] + "'").replace("'hfoo'", "'" + IDS[6] + "'").replace("'Rhme'", "'" + IDS[7] + "'")
            .replace("SetProduce(2,", "SetProduce(" + (2 + INITIAL_TRAINED) + ",")
            .replace("// PREPARE_MINE", SCENARIO.equals("economy-undead") ? """
    loop
        exitwhen GetUnitCountDone('ugol') > 0
        call SetProduce(1, 'ugol', 0)
        call Sleep(1.0)
    endloop
""" : SCENARIO.equals("economy-nightelf") ? """
    loop
        exitwhen GetUnitCountDone('egol') > 0
        call Sleep(1.0)
    endloop
""" : "");

    static boolean ready(final War3MapViewer viewer) {
        final var game = viewer.simulation;
        final GlobalScope globals = game.getGlobalScope();
        final CUnit barracks = unit(globals, "cpBarracks"), smith = unit(globals, "cpSmith");
        return barracks != null && smith != null && barracks.isBuildQueueActive() && smith.isBuildQueueActive()
                && barracks.getConstructionProgress() > 0 && smith.getConstructionProgress() > 0
                && game.getUnitsIncludingPending().stream().anyMatch(u -> u.getPlayerIndex() == 3 && u.isConstructing())
                && (unit(globals, "cpGoldWorker").isHidden()
                    || unit(globals, "cpGoldWorker").getCurrentBehavior() instanceof CBehaviorAcolyteHarvest
                    || carried(unit(globals, "cpGoldWorker")) > 0)
                && (unit(globals, "cpWoodWorker").getCurrentBehavior() instanceof CBehaviorWispHarvest
                    || carried(unit(globals, "cpWoodWorker")) > 0);
    }

    static void assertCheckpoint(final War3MapViewer viewer) {
        check(ready(viewer), "Checkpoint must contain harvesting, construction, training and research together: " + describe(viewer));
        System.out.println("[MissionResumeProbe] economy checkpoint verified: " + describe(viewer));
    }

    static void assertContinuation(final War3MapViewer viewer) {
        final var game = viewer.simulation;
        final GlobalScope globals = game.getGlobalScope(), ai = game.getAiEnvironment(3).getGlobalScope();
        final var player = game.getPlayer(3);
        check(ai.getGlobal("cpEconomyLocal").visit(IntegerJassValueVisitor.getInstance()) == 927, "Sleeping economy AI lost locals");
        check(ai.getGlobal("cpEconomyPulse").visit(IntegerJassValueVisitor.getInstance()) == 12, "Economy AI loop did not finish");
        check(count(viewer, IDS[6]) == 2 + INITIAL_TRAINED, "AI trained the wrong number of units: " + describe(viewer));
        check(count(viewer, IDS[5]) == 1 && game.getUnitsIncludingPending().stream().noneMatch(u -> u.getPlayerIndex() == 3 && u.isConstructing()), "Supply construction did not finish");
        check(player.getTechtreeUnlocked(War3ID.fromString(IDS[7])) == 1, "Research did not finish");
        check(!unit(globals, "cpBarracks").isBuildQueueActive() && !unit(globals, "cpSmith").isBuildQueueActive(), "Production queues did not drain");
        final int spentGold = game.getUnitData().getUnitType(War3ID.fromString(IDS[5])).getGoldCost()
                + 2 * game.getUnitData().getUnitType(War3ID.fromString(IDS[6])).getGoldCost()
                + game.getUpgradeData().getType(War3ID.fromString(IDS[7])).getGoldCost(0)
                + (SCENARIO.equals("economy-undead") ? game.getUnitData().getUnitType(War3ID.fromString("ugol")).getGoldCost() : 0);
        final int spentWood = game.getUnitData().getUnitType(War3ID.fromString(IDS[5])).getLumberCost()
                + 2 * game.getUnitData().getUnitType(War3ID.fromString(IDS[6])).getLumberCost()
                + game.getUpgradeData().getType(War3ID.fromString(IDS[7])).getLumberCost(0)
                + (SCENARIO.equals("economy-undead") ? game.getUnitData().getUnitType(War3ID.fromString("ugol")).getLumberCost() : 0);
        check(player.getGold() > 5000 - spentGold && player.getLumber() > 5000 - spentWood, "Mixed economy did not deposit both resources");
        check(unit(globals, "cpMine").getGold() < 10000, "Income did not consume actual mine resources");
        final CUnit trained = game.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == 3 && u.getTypeId().equals(War3ID.fromString(IDS[6]))).findFirst().orElseThrow();
        check(trained.getCurrentAttacks().get(0).getMinDamageDisplay() > trained.getUnitType().getAttacks().get(0).getMinDamageDisplay(), "Research did not apply its combat bonus");
        System.out.println("[MissionResumeProbe] economy continuation verified: " + describe(viewer));
    }

    static String describe(final War3MapViewer viewer) {
        final var game = viewer.simulation;
        final var globals = game.getGlobalScope();
        final CUnit gold = unit(globals, "cpGoldWorker"), wood = unit(globals, "cpWoodWorker");
        return "race=" + IDS[3] + " tick=" + game.getGameTurnTick() + " gold=" + game.getPlayer(3).getGold() + " lumber=" + game.getPlayer(3).getLumber()
                + " workers=" + (gold == null ? "none" : gold.getCurrentBehavior() + "/" + carried(gold)
                + "," + wood.getCurrentBehavior() + "/" + carried(wood))
                + " town=" + unit(globals, "cpTown").getX() + "," + unit(globals, "cpTown").getY() + " mine=" + unit(globals, "cpMine").getX() + "," + unit(globals, "cpMine").getY() + "/" + unit(globals, "cpTown").getCurrentBehavior()
                + " mines=" + count(viewer, "egol") + "/" + count(viewer, "ugol") + " builder=" + unit(globals, "cpBuilder").isHidden() + "/" + unit(globals, "cpBuilder").getCurrentBehavior()
                + " constructing=" + game.getUnitsIncludingPending().stream().filter(u -> u.getPlayerIndex() == 3 && u.isConstructing()).count()
                + " trained=" + count(viewer, IDS[6]) + " research=" + game.getPlayer(3).getTechtreeUnlocked(War3ID.fromString(IDS[7]));
    }
    private static int carried(final CUnit worker) {
        final var harvest = worker.getFirstAbilityOfType(CAbilityHarvest.class);
        return harvest == null ? 0 : harvest.getCarriedResourceAmount();
    }
    private static long count(final War3MapViewer viewer, final String id) {
        return viewer.simulation.getUnitsIncludingPending().stream().filter(u -> !u.isDead() && u.getPlayerIndex() == 3 && u.getTypeId().equals(War3ID.fromString(id))).count();
    }
    private static CUnit unit(final GlobalScope globals, final String name) {
        return globals.getGlobal(name).visit(ObjectJassValueVisitor.<CUnit>getInstance());
    }
    private static void check(final boolean condition, final String message) {
        if (!condition) throw new IllegalStateException("Mission economy audit: " + message);
    }
}
