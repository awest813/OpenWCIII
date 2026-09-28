package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import mpq.MPQArchive;

class UltravisionSimulationTest {
    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void researchExtendsActualNightVisionAndReverses(boolean standardMeleeData) throws Exception {
        final List<MpqDataSource> archives = new ArrayList<>();
        final CRaceManager previous = WarsmashConstants.RACE_MANAGER;
        try {
            for (String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
                final Path path = RetailTestData.require(name);
                final SeekableByteChannel channel = Files.newByteChannel(path);
                archives.add(new MpqDataSource(new MPQArchive(channel), channel));
            }
            final CompoundDataSource source = new CompoundDataSource(new ArrayList<>(archives));
            try (War3Map map = new War3Map(source, "Maps\\Campaign\\Human01.w3m")) {
                final var data = standardMeleeData ? Warcraft3MapRuntimeObjectData.load(source, true) : map.readModifications();
                final CSimulation game = RetailSimulationTestSupport.simulation(map, data);
                final War3ID research = War3ID.fromString("Reuv");
                assertEquals("Reuv", data.getAbilities().get("Ault").getFieldAsString("Requires", 0));
                game.setFogEnabled(true);
                game.setFogMaskEnabled(true);
                game.setGameTimeOfDay(0);
                game.update();
                assertTrue(game.isNight());
                final CUnit archer = game.createUnitSimple(War3ID.fromString("earc"), 0, -640, 0, 0);
                assertTrue(archer.getUnitType().getUpgradesUsed().contains(research));
                assertFalse(visibleAtProbe(game, archer), "Baseline nighttime sight must not reach the probe");
                game.getPlayer(0).setTechResearched(game, research, 1);
                game.update();
                assertTrue(visibleAtProbe(game, archer), "Ultravision must reveal fog beyond base night sight");
                game.getPlayer(0).setTechResearched(game, research, 1);
                game.update();
                assertTrue(visibleAtProbe(game, archer));
                game.getPlayer(0).setTechResearched(game, research, 0);
                game.update();
                assertFalse(visibleAtProbe(game, archer), "Removing research must restore night fog");
                game.getPlayer(0).setTechResearched(game, research, 1);
                game.update();
                final CUnit trained = game.createUnitSimple(War3ID.fromString("earc"), 0, -640, 128, 0);
                final CUnit enemy = game.createUnitSimple(War3ID.fromString("earc"), 1, -640, -128, 0);
                final CUnit hall = game.createUnitSimple(War3ID.fromString("edob"), 0, -640, 384, 0);
                assertTrue(visibleAtProbe(game, trained), "Newly trained units inherit researched vision");
                assertFalse(visibleAtProbe(game, enemy), "Other players do not inherit research");
                final int[] enabledCalls = {0};
                final int[] removedCalls = {0};
                final var observer = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbilityGenericSingleIconPassiveAbility(
                        War3ID.fromString("Ault"), War3ID.fromString("Ault"), game.getHandleIdAllocator().createId()) {
                    @Override public boolean isRequirementsMet(CSimulation simulation, CUnit unit) {
                        return simulation.getPlayer(unit.getPlayerIndex()).getTechtreeUnlocked(research) > 0;
                    }
                    @Override public void onAdd(CSimulation simulation, CUnit unit) { enabledCalls[0]++; }
                    @Override public void onRemove(CSimulation simulation, CUnit unit) { removedCalls[0]++; }
                };
                enemy.add(game, observer);
                assertEquals(0, enabledCalls[0]);
                final int player0Food = game.getPlayer(0).getFoodUsed();
                final int player1Food = game.getPlayer(1).getFoodUsed();
                final int transferredFood = enemy.getFoodUsed();
                assertTrue(transferredFood > 0, "Retail archer must consume food for the ownership regression");
                enemy.setPlayerIndex(game, 0, false);
                assertEquals(player0Food + transferredFood, game.getPlayer(0).getFoodUsed());
                assertEquals(player1Food - transferredFood, game.getPlayer(1).getFoodUsed());
                assertEquals(transferredFood, enemy.getFoodUsed());
                assertTrue(visibleAtProbe(game, enemy), "Receiving a unit must clear its former owner's unmet requirement");
                enemy.setPlayerIndex(game, 1, false);
                assertEquals(player0Food, game.getPlayer(0).getFoodUsed());
                assertEquals(player1Food, game.getPlayer(1).getFoodUsed());
                assertFalse(visibleAtProbe(game, enemy), "Returning the unit must reinstate its owner's unmet requirement");
                assertEquals(1, enabledCalls[0], "Research activation must invoke the ability lifecycle once");
                assertEquals(1, removedCalls[0], "Lost requirements must remove the passive effect once");
                enemy.setPlayerIndex(game, 1, false);
                assertEquals(1, removedCalls[0], "Assigning the same owner must not repeat removal");
                enemy.remove(game, observer);
                assertEquals(hall.getUnitType().getSightRadiusNight(), hall.getSightRadius(game, false),
                        "Hunter's Hall research does not extend building sight");
                assertTrue(archer.getUnitType().getSightRadiusNight() < archer.getSightRadius(game, false),
                        "The shared unit definition must retain its original night sight");
                final var sharedFog = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CPlayerFogOfWar(game.getPathingGrid());
                final var modifier = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CUnitVisionFogModifier(archer, false);
                modifier.update(game, game.getPlayer(1), game.getPathingGrid(), sharedFog);
                assertTrue(sharedFog.isVisible(game, game.getPathingGrid(), 512, 0), "Shared unit vision uses Ultravision too");
                archer.setPlayerIndex(game, 1, false);
                assertFalse(visibleAtProbe(game, archer), "Transferred units use the new owner's research");
                archer.setPlayerIndex(game, 0, false);
                assertTrue(visibleAtProbe(game, archer));
                final var ability = archer.getFirstAbilityOfType(
                        com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityUltravision.class);
                assertNotNull(ability);
                ability.setDisabled(true, com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType.TRIGGER);
                assertFalse(visibleAtProbe(game, archer), "Disabling the passive removes its sight benefit");
                archer.setPlayerIndex(game, 1, false);
                archer.setPlayerIndex(game, 0, false);
                assertFalse(visibleAtProbe(game, archer), "Ownership refresh must preserve a script's explicit disable");
                ability.setDisabled(false, com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType.TRIGGER);
                assertTrue(visibleAtProbe(game, archer));
                game.getPlayer(1).setTechResearched(game, research, 1);
                game.getPlayer(1).setAbilityEnabled(game, War3ID.fromString("Ault"), false);
                game.update();
                archer.setPlayerIndex(game, 1, false);
                assertFalse(visibleAtProbe(game, archer), "The new owner's ability restriction must apply despite research");
                archer.setPlayerIndex(game, 0, false);
                assertTrue(visibleAtProbe(game, archer), "The former owner's player restriction must not follow the unit");
                archer.remove(game, ability);
                assertFalse(visibleAtProbe(game, archer), "Removing the ability removes its sight benefit");
                game.setGameTimeOfDay(12);
                game.update();
                assertTrue(game.isDay());
                assertTrue(visibleAtProbe(game, archer), "The same point is visible in daylight without research");
            }
        }
        finally {
            WarsmashConstants.RACE_MANAGER = previous;
            for (MpqDataSource archive : archives) archive.close();
        }
    }

    private static boolean visibleAtProbe(CSimulation game, CUnit unit) {
        final var fog = game.getPlayer(unit.getPlayerIndex()).getFogOfWar();
        fog.convertVisibleToFogged();
        unit.updateFogOfWar(game);
        return fog.isVisible(game, game.getPathingGrid(), 512, 0);
    }
}
