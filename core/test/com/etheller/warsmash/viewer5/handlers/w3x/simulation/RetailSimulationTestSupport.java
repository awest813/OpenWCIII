package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Random;

import com.badlogic.gdx.math.Rectangle;
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

/** Headless flat-map fixture using supplied retail object data, not a played retail mission. */
public final class RetailSimulationTestSupport {
    private RetailSimulationTestSupport() { }
    public static CSimulation simulation(final War3Map map) throws Exception {
        return simulation(map, map.readModifications());
    }

    public static CSimulation simulation(final War3Map map, final Warcraft3MapRuntimeObjectData data) throws Exception {
        return simulation(map, data, args -> { });
    }

    public static CSimulation simulation(final War3Map map, final Warcraft3MapRuntimeObjectData data,
            final java.util.function.Consumer<com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectileMissile> projectiles) throws Exception {
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
        final War3MapWpm pathing = new War3MapWpm(null);
        pathing.getSize()[0] = 64;
        pathing.getSize()[1] = 64;
        pathing.setPathing(new short[64 * 64]);
        final SimulationRenderController renderer = (SimulationRenderController) Proxy.newProxyInstance(
                SimulationRenderController.class.getClassLoader(), new Class<?>[] { SimulationRenderController.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "createAttackProjectile": {
                        final var attack = (com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile) args[5];
                        final var projectile = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectileMissile(
                                (Float) args[1], (Float) args[2], attack.getProjectileSpeed(),
                                (com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget) args[6],
                                (CUnit) args[4], (Float) args[7], attack, (Integer) args[8],
                                (com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener) args[9]);
                        projectiles.accept(projectile);
                        return projectile;
                    }
                    case "createUnit": return ((CSimulation) args[0]).internalCreateUnit((War3ID) args[1],
                            (Integer) args[2], (Float) args[3], (Float) args[4], (Float) args[5], null);
                    case "createItem": return ((CSimulation) args[0]).internalCreateItem((War3ID) args[1], (Float) args[2], (Float) args[3]);
                    case "getBuildingPathingPixelMap":
                    case "getDestructablePathingPixelMap":
                    case "getDestructablePathingDeathPixelMap": return PathingGrid.BLANK_PATHING;
                    }
                    if (method.getReturnType() == SimulationRenderComponentModel.class) return SimulationRenderComponentModel.DO_NOTHING;
                    if (method.getReturnType() == SimulationRenderComponent.class) return SimulationRenderComponent.DO_NOTHING;
                    if (method.getName().equals("getTerrainHeight")) return 0;
                    if (method.getName().equals("isTerrainRomp") || method.getName().equals("isTerrainWater")) return false;
                    if (method.getName().equals("spawnTextTag") || method.getReturnType() == void.class) return null;
                    throw new AssertionError("Unsupported headless renderer call: " + method.getName());
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
