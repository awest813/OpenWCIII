package com.etheller.warsmash.viewer5.handlers.w3x.simulation;
import com.etheller.warsmash.testutil.RetailTestData;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import mpq.MPQArchive;

class StoredUnitDataSimulationTest {
    @TempDir Path directory;

    @Test
    void equippedHeroSurvivesDiskCarryoverWithoutStackingStats() throws Exception {
        final Path archive = RetailTestData.require("war3.mpq");
        final CRaceManager previousRaces = WarsmashConstants.RACE_MANAGER;
        final List<MpqDataSource> archives = new ArrayList<>();
        try {
            for (final String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
                final Path path = archive.resolveSibling(name);
                RetailTestData.require(name);
                final SeekableByteChannel channel = Files.newByteChannel(path);
                archives.add(new MpqDataSource(new MPQArchive(channel), channel));
            }
            final CompoundDataSource source = new CompoundDataSource(new ArrayList<>(archives));
            try (War3Map map = new War3Map(source, "Maps\\Campaign\\Human01.w3m")) {
                final CSimulation simulation = RetailSimulationTestSupport.simulation(map);
                for (final String id : new String[] { "AHhb", "AHds", "AHre", "AHwe", "AHbz", "AHab",
                        "AHmt", "AHtb", "AHtc", "AHav", "AHfs" }) {
                    assertNotNull(simulation.getAbilityData().getAbilityType(War3ID.fromString(id)),
                            "Campaign hero skill must have an implementation: " + id);
                }
                final CUnit hero = simulation.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 90);
                hero.getHeroData().setHeroLevel(simulation, hero, 4, false);
                hero.getHeroData().setProperName("Arthas");
                hero.getHeroData().selectHeroSkill(simulation, hero, War3ID.fromString("AHhb"));
                final CItem belt = simulation.createItem(War3ID.fromString("bgst"), 0, 0);
                assertEquals(4, hero.getInventoryData().giveItem(simulation, hero, belt, 4, false));
                final CItem potion = simulation.createItem(War3ID.fromString("phea"), 0, 0);
                potion.setCharges(3);
                assertEquals(1, hero.getInventoryData().giveItem(simulation, hero, potion, 1, false));
                assertTrue(hero.getHeroData().getStrength().getBonus() > 0, "Fixture must exercise real item stats");
                hero.getHeroData().addStrengthBonus(simulation, hero, 2);
                final StoredUnitData snapshot = CGameSave.snapshotUnit(hero);
                final CGameCache cache = new CGameCache("heroes");
                cache.storeUnit("human", "arthas", snapshot);
                final Path file = this.directory.resolve("heroes.w3v");
                cache.save(file.toFile());
                final StoredUnitData loaded = CGameCache.tryLoadFromFile(file.toFile(), "heroes")
                        .getStoredUnit("human", "arthas");
                final CUnit restored = loaded.createUnit(simulation, 0, 256, 256, 180);
                assertEquals(snapshot.strengthBonus, restored.getHeroData().getStrength().getBonus());
                assertEquals(hero.getHeroData().getStrength().getCurrent(), restored.getHeroData().getStrength().getCurrent());
                assertEquals(hero.getMaximumLife(), restored.getMaximumLife());
                assertEquals(hero.getMaximumMana(), restored.getMaximumMana());
                assertEquals(hero.getHeroData().getHeroLevel(), restored.getHeroData().getHeroLevel());
                assertEquals(snapshot.xp, restored.getHeroData().getXp());
                assertEquals(snapshot.skillPoints, restored.getHeroData().getSkillPoints());
                final com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityHolyLight light =
                        restored.getFirstAbilityOfType(com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin.CAbilityHolyLight.class);
                assertNotNull(light);
                assertEquals(1, light.getLevel());
                hero.setLife(simulation, 100);
                light.doEffect(simulation, restored, hero);
                assertTrue(hero.getLife() > 100, "Restored Holy Light must actually heal");
                assertEquals("Arthas", restored.getHeroData().getProperName());
                assertEquals(3, restored.getInventoryData().getItemInSlot(1).getCharges());
                assertNull(restored.getInventoryData().getItemInSlot(0));
                assertEquals(belt.getTypeId(), restored.getInventoryData().getItemInSlot(4).getTypeId());
                assertEquals(2, simulation.getPlayer(0).getTechtreeUnlocked(hero.getTypeId()));
                hero.getInventoryData().dropItem(simulation, hero, 4, 0, 0, false);
                restored.getInventoryData().dropItem(simulation, restored, 4, 256, 256, false);
                assertEquals(2, restored.getHeroData().getStrength().getBonus());
                assertEquals(hero.getMaximumLife(), restored.getMaximumLife());
                final CGameSave battlefield = new CGameSave("checkpoint", WarsmashConstants.MAX_PLAYERS);
                battlefield.collectSimulation(simulation);
                assertEquals(2, battlefield.savedUnits.size(), "Units created this tick must be captured");
                simulation.removeUnit(restored);
                battlefield.collectSimulation(simulation);
                assertEquals(1, battlefield.savedUnits.size(), "Pending removal must be excluded");
            }
        }
        finally {
            WarsmashConstants.RACE_MANAGER = previousRaces;
            for (final MpqDataSource source : archives) source.close();
        }
    }

}
