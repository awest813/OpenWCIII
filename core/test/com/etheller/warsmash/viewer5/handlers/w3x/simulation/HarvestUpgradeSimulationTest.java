package com.etheller.warsmash.viewer5.handlers.w3x.simulation;
import com.etheller.warsmash.testutil.RetailTestData;


import static org.junit.jupiter.api.Assertions.*;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import mpq.MPQArchive;

class HarvestUpgradeSimulationTest {
    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void researchedLumberCapacityAppliesToExistingAndNewWorkersAndCanBeReversed(boolean standardMeleeData) throws Exception {
        final Path root = Path.of(System.getProperty("warsmash.test.assets", "F:/WC3Data"));
        final List<MpqDataSource> archives = new ArrayList<>();
        final CRaceManager previous = WarsmashConstants.RACE_MANAGER;
        try {
            for (String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
                RetailTestData.require(name);
                final SeekableByteChannel channel = Files.newByteChannel(root.resolve(name));
                archives.add(new MpqDataSource(new MPQArchive(channel), channel));
            }
            final CompoundDataSource source = new CompoundDataSource(new ArrayList<>(archives));
            {
                try (War3Map map = new War3Map(source, "Maps\\Campaign\\Human01.w3m")) {
                    System.out.println("Harvest upgrade fixture: " + (standardMeleeData ? "standard melee object data" : "Human01"));
                    final var objectData = standardMeleeData
                            ? com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData.load(source, true)
                            : map.readModifications();
                    final CSimulation game = RetailSimulationTestSupport.simulation(map, objectData);
                    final War3ID upgrade = War3ID.fromString("Rhlh");
                    final CUnit worker = game.createUnitSimple(War3ID.fromString("hpea"), 0, 0, 0, 0);
                    final CUnit enemy = game.createUnitSimple(War3ID.fromString("hpea"), 1, 128, 0, 0);
                    final CAbilityHarvest harvest = worker.getFirstAbilityOfType(CAbilityHarvest.class);
                    assertNotNull(harvest);
                    assertTrue(worker.getUnitType().getUpgradesUsed().contains(upgrade));
                    final int capacity = harvest.getLumberCapacity();
                    final int gold = harvest.getGoldCapacity();
                    final int chop = harvest.getDamageToTree();
                    final var data = objectData.getUpgrades().get(upgrade);
                    assertEquals("rlum", data.readSLKTag("effect1"));
                    final int base = data.readSLKTagInt("base1");
                    final int increment = data.readSLKTagInt("mod1");
                    assertTrue(base > 0);
                    game.getPlayer(0).setTechResearched(game, upgrade, 1);
                    assertEquals(capacity + base, harvest.getLumberCapacity(), "Worker created this tick must receive research");
                    assertEquals(capacity, enemy.getFirstAbilityOfType(CAbilityHarvest.class).getLumberCapacity());
                    final CUnit trained = game.createUnitSimple(War3ID.fromString("hpea"), 0, 256, 0, 0);
                    assertEquals(capacity + base, trained.getFirstAbilityOfType(CAbilityHarvest.class).getLumberCapacity());
                    game.getPlayer(0).setTechResearched(game, upgrade, 2);
                    assertEquals(capacity + base + increment, harvest.getLumberCapacity());
                    harvest.setCarriedResources(com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType.LUMBER, 0);
                    for (int hit = 0; hit < 100; hit++) harvest.getBehaviorHarvest().onHit(null, chop);
                    assertEquals(capacity + base + increment, harvest.getCarriedResourceAmount(),
                            "Actual harvest hits must fill the upgraded capacity and stop there");
                    game.getPlayer(0).setTechResearched(game, upgrade, 2);
                    assertEquals(capacity + base + increment, harvest.getLumberCapacity(), "Repeated level must not stack");
                    game.getPlayer(0).setTechResearched(game, upgrade, 1);
                    assertEquals(capacity + base, harvest.getLumberCapacity());
                    game.getPlayer(0).setTechResearched(game, upgrade, 0);
                    assertEquals(capacity, harvest.getLumberCapacity());
                    assertEquals(capacity, trained.getFirstAbilityOfType(CAbilityHarvest.class).getLumberCapacity());
                    assertEquals(gold, harvest.getGoldCapacity());
                    assertEquals(chop, harvest.getDamageToTree());
                    verifySpellLevelResearch(game);
                }
            }
        }
        finally {
            WarsmashConstants.RACE_MANAGER = previous;
            for (MpqDataSource archive : archives) archive.close();
        }
    }

    private static void verifySpellLevelResearch(CSimulation game) {
        final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 512, 0, 0);
        final War3ID holyLight = War3ID.fromString("AHhb");
        hero.getHeroData().selectHeroSkill(game, hero, holyLight);
        final var ability = hero.getFirstAbilityOfType(
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityHolyLight.class);
        assertNotNull(ability);
        final var configured = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade.CUpgradeEffectSpellLevel(2, 0, holyLight);
        configured.apply(game, hero, 1);
        assertEquals(3, ability.getLevel(), "Research must honor the configured spell-level increment");
        configured.unapply(game, hero, 1);
        assertEquals(1, ability.getLevel());
        final var training = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade.CUpgradeEffectSpellLevel(1, 1, holyLight);
        training.apply(game, hero, 2);
        assertEquals(3, ability.getLevel());
        training.unapply(game, hero, 2);
        assertEquals(1, ability.getLevel(), "Removing two research levels must restore the original spell level");
    }
}
