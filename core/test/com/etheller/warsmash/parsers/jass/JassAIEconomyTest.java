package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.RetailSimulationTestSupport;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityQueue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;

import mpq.MPQArchive;

class JassAIEconomyTest {
    @Test
    void mixedNinetySixUnitArmiesEngageThroughCaptainOrders() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            final List<CUnit> army = new ArrayList<>();
            for (int side = 2; side <= 3; side++) {
                for (int i = 0; i < 48; i++) {
                    final String id = new String[] { "hrif", "ohun", "ucry", "earc" }[i / 12];
                    army.add(game.createUnitSimple(War3ID.fromString(id), side,
                            (side == 2 ? 1 : -1) * (128 + (i / 12) * 48), -352 + (i % 12) * 64, 0));
                }
            }
            game.update();
            for (final String id : new String[] { "hrif", "ohun", "ucry", "earc" }) fixture.call("AddAssault", 12, War3ID.fromString(id).getValue());
            final var real = com.etheller.interpreter.ast.value.RealJassValue.ZERO;
            fixture.program.getJassNativeManager().getNative("CaptainAttack").call(List.of(real, real), fixture.program.getGlobals(), TriggerExecutionScope.EMPTY);
            for (int tick = 0; tick < 1600; tick++) game.update();
            final long deaths = army.stream().filter(CUnit::isDead).count();
            System.out.println("Mass battle deaths=" + deaths + " first=" + army.get(0).getX() + "," + army.get(0).getY() + "/" + army.get(0).getCurrentBehavior());
            assertTrue(deaths >= 32, "Mass combat must cause casualties");
        }
    }

    @Test
    void rootedTreeEntanglesAnInRangeMineThroughAnOrdinaryOrder() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            final var town = game.createUnitSimple(War3ID.fromString("etol"), 3, 0, 0, 0);
            final var parent = game.createUnitSimple(War3ID.fromString("ngol"), 15, -384, 0, 0);
            parent.setGold(10000);
            game.update();
            final var entangle = town.getFirstAbilityOfType(com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.root.CAbilityEntangleGoldMine.class);
            assertNotNull(entangle);
            game.getPlayer(3).getFogOfWar().setVisible(game.getPathingGrid(), parent.getX(), parent.getY(), com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState.VISIBLE);
            final var receiver = com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver.<com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget>getInstance().reset();
            entangle.checkCanTarget(game, town, com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.entangleinstant, parent, receiver);
            assertSame(parent, receiver.getTarget(), receiver.getExternStringKey());
            game.getDefaultPlayerUnitOrderExecutor(3).issueTargetOrder(town.getHandleId(), entangle.getHandleId(),
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.entangleinstant, parent.getHandleId(), false);
            for (int tick = 0; tick < 40; tick++) game.update();
            final var mine = game.getUnitsIncludingPending().stream().filter(u -> u.getTypeId().equals(War3ID.fromString("egol"))).findFirst().orElseThrow();
            assertSame(parent, mine.getOverlayedGoldMineData().getParentGoldMineUnit());
            assertTrue(parent.isHidden());
        }
    }
    @Test
    void hauntedMineConstructionUsesTheExactParentCenter() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            game.createUnitSimple(War3ID.fromString("unpl"), 3, 0, 0, 0);
            final var builder = game.createUnitSimple(War3ID.fromString("uaco"), 3, -256, 0, 0);
            final var parent = game.createUnitSimple(War3ID.fromString("ngol"), 15, -640, 0, 0);
            parent.setGold(10000);
            assertTrue(game.getUnitData().getUnitType(War3ID.fromString("ugol")).isCanBeBuiltOnThem());
            assertTrue(parent.getUnitType().isCanBuildOnMe());
            game.update();
            assertTrue(fixture.produce(1, "ugol"));
            final var order = (com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint) builder.getCurrentOrder();
            assertEquals(parent.getX(), order.getTarget(game).getX());
            assertEquals(parent.getY(), order.getTarget(game).getY());
        }
    }
    @ParameterizedTest
    @CsvSource({"opeo,opeo,ogre,obar,ofor,otrb,ogru,Rome,ngol", "uaco,ugho,unpl,usep,ugrv,uzig,ugho,Rume,ugol", "ewsp,ewsp,etol,eaom,edob,emow,earc,Resm,egol"})
    void raceSpecificHarvestersProduceRealIncome(final String goldId, final String woodId, final String townId,
            final String barracksId, final String smithId, final String supplyId, final String trainedId,
            final String researchId, final String mineId) throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            game.createUnitSimple(War3ID.fromString(townId), 3, 0, 0, 0);
            final var barracks = game.createUnitSimple(War3ID.fromString(barracksId), 3, 512, 512, 0);
            final var smith = game.createUnitSimple(War3ID.fromString(smithId), 3, 512, -512, 0);
            final var gold = game.createUnitSimple(War3ID.fromString(goldId), 3, -256, 0, 0);
            final var wood = game.createUnitSimple(War3ID.fromString(woodId), 3, 0, 256, 0);
            final var baseMine = game.createUnitSimple(War3ID.fromString("ngol"), 15, -640, 0, 0);
            baseMine.setGold(10000);
            final var mine = mineId.equals("ngol") ? baseMine : game.createUnitSimple(War3ID.fromString(mineId), 3, -640, 0, 0);
            for (final var ability : mine.getAbilities()) {
                if (ability instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityOverlayedMine)
                    ((com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityOverlayedMine) ability).setParentMine(baseMine, baseMine.getGoldMineData());
            }
            if (mine != baseMine) { baseMine.setHidden(true); baseMine.setPaused(true); }
            game.internalCreateDestructable(War3ID.fromString("LTlt"), 0, 512, null, null);
            System.out.println("Race fixture " + goldId + ": " + gold.getAbilities().stream().map(a -> a.getClass().getSimpleName() + ":" + a.getAlias()).collect(java.util.stream.Collectors.toList()));
            System.out.println("Production " + trainedId + " requirements=" + game.getUnitData().getUnitType(War3ID.fromString(trainedId)).getRequirements() + " upgrades=" + game.getUnitData().getUnitType(War3ID.fromString(trainedId)).getUpgradesUsed());
            game.update();
            fixture.call("HarvestGold", 0, 1);
            fixture.call("HarvestWood", 0, 1);
            assertNotNull(gold.getCurrentOrder(), "Gold worker was not assigned");
            assertNotNull(wood.getCurrentOrder(), "Lumber worker was not assigned");
            final var goldOrder = gold.getCurrentOrder();
            final var woodOrder = wood.getCurrentOrder();
            fixture.call("HarvestGold", 0, 1);
            fixture.call("HarvestWood", 0, 1);
            assertSame(goldOrder, gold.getCurrentOrder());
            assertSame(woodOrder, wood.getCurrentOrder());
            for (int tick = 0; tick < 400; tick++) game.update();
            assertTrue(game.getPlayer(3).getGold() > 5000, "Mine must supply actual gold");
            assertTrue(game.getPlayer(3).getLumber() > 5000, "Harvest must supply actual lumber");
            assertTrue(baseMine.getGold() < 10000);
            if (mineId.equals("egol")) {
                assertTrue(gold.isHidden(), "Gold wisp should be inside the entangled mine");
                final var spare = game.createUnitSimple(War3ID.fromString("ewsp"), 3, 256, 256, 0);
                game.update();
                fixture.call("HarvestGold", 0, 1);
                assertNull(spare.getCurrentOrder(), "A boarded wisp must count toward the requested gold workforce");
            }
            assertTrue(fixture.produce(2, trainedId));
            assertTrue(fixture.produce(2, trainedId));
            assertTrue(fixture.produce(1, researchId));
            assertEquals(War3ID.fromString(trainedId), barracks.getBuildQueue()[0]);
            assertEquals(War3ID.fromString(researchId), smith.getBuildQueue()[0]);
        }
    }
    @Test
    void goldAndWoodUseSeparateWorkersAndRepeatedRequestsPreserveJobs() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            final CUnit gold = game.createUnitSimple(War3ID.fromString("hpea"), 3, -128, 0, 0);
            final CUnit wood = game.createUnitSimple(War3ID.fromString("hpea"), 3, 128, 0, 0);
            final CUnit builder = game.createUnitSimple(War3ID.fromString("hpea"), 3, 256, 256, 0);
            final CUnit mine = game.createUnitSimple(War3ID.fromString("ngol"), 15, -384, 0, 0);
            mine.setGold(10000);
            final var tree = game.internalCreateDestructable(War3ID.fromString("LTlt"), 384, 0, null, null);
            game.update();
            fixture.call("HarvestGold", 0, 1);
            final var goldBehavior = gold.getCurrentBehavior();
            final var goldOrder = gold.getCurrentOrder();
            fixture.call("HarvestWood", 0, 1);
            assertSame(mine, gold.getFirstAbilityOfType(CAbilityHarvest.class).getLastHarvestTarget());
            assertSame(tree, wood.getFirstAbilityOfType(CAbilityHarvest.class).getLastHarvestTarget());
            assertTrue(fixture.produce(1, "hhou"));
            assertEquals(War3ID.fromString("hhou").getValue(), builder.getCurrentOrder().getOrderId(), "Construction must use the idle builder");
            final var buildOrder = builder.getCurrentOrder();
            fixture.call("HarvestGold", 0, 1);
            fixture.call("HarvestWood", 0, 1);
            assertSame(goldOrder, gold.getCurrentOrder(), "Repeated allocation must not restart harvesting or queue an order inside a mine");
            assertSame(goldBehavior, gold.getCurrentBehavior());
            assertSame(buildOrder, builder.getCurrentOrder(), "Harvest allocation must preserve construction");
        }
    }

    @Test
    void repeatedTreeRemovalClearsHandlesAndSpatialQueriesOnce() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            final var tree = game.internalCreateDestructable(War3ID.fromString("LTlt"), 0, 0, null, null);
            game.removeDestructable(tree);
            final var state = com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionStateFingerprint.of(game);
            game.removeDestructable(tree);
            assertEquals(state, com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionStateFingerprint.of(game));
            game.update();
            assertNull(game.getWidget(tree.getHandleId()));
            final List<Object> remaining = new ArrayList<>();
            game.getWorldCollision().enumDestructablesInRect(new com.badlogic.gdx.math.Rectangle(-128, -128, 256, 256), dest -> { remaining.add(dest); return false; });
            assertTrue(remaining.isEmpty());
            game.removeDestructable(tree);
            game.update();
        }
    }

    @Test
    void trainingTargetIncludesQueuedUnits() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final CUnit barracks = fixture.game.createUnitSimple(War3ID.fromString("hbar"), 3, 0, 0, 0);
            assertTrue(fixture.produce(2, "hfoo"));
            assertTrue(fixture.produce(2, "hfoo"));
            final int gold = fixture.game.getPlayer(3).getGold();
            assertTrue(fixture.produce(2, "hfoo"));
            assertEquals(2, Arrays.stream(barracks.getBuildQueue()).filter(War3ID.fromString("hfoo")::equals).count());
            assertEquals(gold, fixture.game.getPlayer(3).getGold(), "Satisfied production must not charge for extra units");
        }
    }

    @Test
    void unavailableProducerIsSkippedAndMissingRequirementsReturnFalse() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final CUnit disabled = fixture.game.createUnitSimple(War3ID.fromString("hbar"), 3, -256, 0, 0);
            disabled.getFirstAbilityOfType(CAbilityQueue.class).setDisabled(true, CAbilityDisableType.CONSTRUCTION);
            final CUnit available = fixture.game.createUnitSimple(War3ID.fromString("hbar"), 3, 256, 0, 0);
            assertTrue(fixture.produce(1, "hfoo"));
            assertNull(disabled.getBuildQueue()[0]);
            assertEquals(War3ID.fromString("hfoo"), available.getBuildQueue()[0]);
            final int gold = fixture.game.getPlayer(3).getGold();
            assertFalse(fixture.produce(1, "hkni"), "A listed unit with unmet tech requirements must not report success");
            assertEquals(gold, fixture.game.getPlayer(3).getGold());
        }
    }

    @Test
    void fullProducerIsSkippedWithoutChargingForRejectedTraining() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final CUnit full = fixture.game.createUnitSimple(War3ID.fromString("hbar"), 3, -256, 0, 0);
            final CUnit available = fixture.game.createUnitSimple(War3ID.fromString("hbar"), 3, 256, 0, 0);
            for (int i = 0; i < full.getBuildQueue().length; i++) full.queueTrainingUnit(fixture.game, War3ID.fromString("hfoo"));
            assertTrue(fixture.produce(100, "hfoo"));
            assertEquals(War3ID.fromString("hfoo"), available.getBuildQueue()[0]);
            for (int i = 1; i < available.getBuildQueue().length; i++) available.queueTrainingUnit(fixture.game, War3ID.fromString("hfoo"));
            final int gold = fixture.game.getPlayer(3).getGold();
            assertFalse(fixture.produce(100, "hfoo"));
            assertEquals(gold, fixture.game.getPlayer(3).getGold());
        }
    }

    @Test
    void researchAlreadyQueuedSatisfiesRequestWithoutDuplicateOrders() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final CUnit smith = fixture.game.createUnitSimple(War3ID.fromString("hbla"), 3, 0, 0, 0);
            assertTrue(fixture.produce(1, "Rhme"));
            final var order = smith.getCurrentOrder();
            final int gold = fixture.game.getPlayer(3).getGold();
            assertTrue(fixture.produce(1, "Rhme"));
            assertSame(order, smith.getCurrentOrder());
            assertEquals(1, fixture.game.getPlayer(3).getTechtreeInProgress(War3ID.fromString("Rhme")));
            assertEquals(gold, fixture.game.getPlayer(3).getGold());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final List<MpqDataSource> archives = new ArrayList<>();
        final com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager previous = WarsmashConstants.RACE_MANAGER;
        final War3Map map;
        final CSimulation game;
        final JassProgram program = new JassProgram();
        Fixture() throws Exception {
            for (final String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
                final var channel = Files.newByteChannel(RetailTestData.require(name));
                archives.add(new MpqDataSource(new MPQArchive(channel), channel));
            }
            map = new War3Map(new CompoundDataSource(new ArrayList<>(archives)), "Maps\\Campaign\\Human01.w3m");
            game = RetailSimulationTestSupport.simulation(map);
            game.getPlayer(3).setGold(5000);
            game.getPlayer(3).setLumber(5000);
            game.getPlayer(3).setFoodCap(100);
            new JassAIEnvironment(program, null, null, null, null, new War3MapConfig(WarsmashConstants.MAX_PLAYERS), game, 3);
        }
        com.etheller.interpreter.ast.value.JassValue call(final String nativeName, final int... arguments) {
            return program.getJassNativeManager().getNative(nativeName).call(
                    Arrays.stream(arguments).mapToObj(IntegerJassValue::of).collect(java.util.stream.Collectors.toList()),
                    program.getGlobals(), TriggerExecutionScope.EMPTY);
        }
        boolean produce(final int qty, final String id) {
            return call("SetProduce", qty, War3ID.fromString(id).getValue(), 0).visit(BooleanJassValueVisitor.getInstance());
        }
        @Override public void close() throws Exception {
            map.close();
            WarsmashConstants.RACE_MANAGER = previous;
            for (final var archive : archives) archive.close();
        }
    }
}
