package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData;
import com.etheller.warsmash.parsers.w3x.wpm.War3MapWpm;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.util.WorldEditStrings;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderController;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.CommandErrorListener;
import mpq.MPQArchive;

class StoredUnitDataSimulationTest {
    @TempDir Path directory;

    @Test
    void equippedHeroSurvivesDiskCarryoverWithoutStackingStats() throws Exception {
        final Path archive = Path.of("F:/WC3Data/war3.mpq");
        assumeTrue(Files.isRegularFile(archive), "Requires owned retail campaign data");
        final CRaceManager previousRaces = WarsmashConstants.RACE_MANAGER;
        final List<MpqDataSource> archives = new ArrayList<>();
        try {
            for (final String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
                final Path path = archive.resolveSibling(name);
                assumeTrue(Files.isRegularFile(path), "Requires owned RoC/TFT retail data");
                final SeekableByteChannel channel = Files.newByteChannel(path);
                archives.add(new MpqDataSource(new MPQArchive(channel), channel));
            }
            final CompoundDataSource source = new CompoundDataSource(new ArrayList<>(archives));
            try (War3Map map = new War3Map(source, "Maps\\Campaign\\Human01.w3m")) {
                final CSimulation simulation = simulation(map);
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

    private static CSimulation simulation(final War3Map map) throws Exception {
        WarsmashConstants.RACE_MANAGER = new CRaceManager();
        WarsmashConstants.RACE_MANAGER.addRace("Human", 1, 1);
        WarsmashConstants.RACE_MANAGER.addRace("Orc", 2, 2);
        WarsmashConstants.RACE_MANAGER.addRace("Undead", 3, 4);
        WarsmashConstants.RACE_MANAGER.addRace("NightElf", 4, 3);
        WarsmashConstants.RACE_MANAGER.build();
        final DataTable misc = new DataTable(new WorldEditStrings(map));
        for (final String path : new String[] { "UI\\MiscData.txt", "Units\\MiscData.txt", "Units\\MiscGame.txt", "UI\\MiscUI.txt" }) {
            if (map.has(path)) {
                try (InputStream stream = map.getResourceAsStream(path)) { misc.readTXT(stream, true); }
            }
        }
        final Warcraft3MapRuntimeObjectData data = map.readModifications();
        final War3MapWpm pathing = new War3MapWpm(null);
        pathing.getSize()[0] = 64;
        pathing.getSize()[1] = 64;
        pathing.setPathing(new short[64 * 64]);
        final SimulationRenderController renderer = (SimulationRenderController) Proxy.newProxyInstance(
                SimulationRenderController.class.getClassLoader(), new Class<?>[] { SimulationRenderController.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "createUnit": return ((CSimulation) args[0]).internalCreateUnit((War3ID) args[1],
                            (Integer) args[2], (Float) args[3], (Float) args[4], (Float) args[5], null);
                    case "createItem": return ((CSimulation) args[0]).internalCreateItem((War3ID) args[1], (Float) args[2], (Float) args[3]);
                    case "getBuildingPathingPixelMap": return PathingGrid.BLANK_PATHING;
                    }
                    if (method.getReturnType() == SimulationRenderComponentModel.class) return SimulationRenderComponentModel.DO_NOTHING;
                    if (method.getReturnType() == SimulationRenderComponent.class) return SimulationRenderComponent.DO_NOTHING;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        final CommandErrorListener errors = (CommandErrorListener) Proxy.newProxyInstance(
                CommandErrorListener.class.getClassLoader(), new Class<?>[] { CommandErrorListener.class },
                (proxy, method, args) -> { throw new AssertionError("Unexpected simulation error: " + java.util.Arrays.toString(args)); });
        return new CSimulation(new War3MapConfig(WarsmashConstants.MAX_PLAYERS), map.readMapInformation().getVersion(), misc,
                data.getUnits(), data.getItems(), data.getDestructibles(), data.getAbilities(), data.getUpgrades(),
                data.getStandardUpgradeEffectMeta(), renderer, new PathingGrid(pathing, new float[] { -1024, -1024 }),
                new Rectangle(-1024, -1024, 2048, 2048), new Random(1), errors);
    }
}
