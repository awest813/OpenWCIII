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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectileMissile;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileBounce;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade.CUpgradeEffectDefenseType;
import mpq.MPQArchive;

class CombatUpgradeSimulationTest {
    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void reinforcedDefensesChangesDamageAndRestoresUnderlyingArmor(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            final War3ID research = War3ID.fromString("Rorb");
            final CUnit burrow = game.createUnitSimple(War3ID.fromString("otrb"), 0, 0, 0, 0);
            final CUnit attacker = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 256, 0, 0);
            final CDefenseType original = burrow.getDefenseType();
            assertNotEquals(CDefenseType.FORT, original);
            assertTrue(burrow.getUnitType().getUpgradesUsed().contains(research));
            final float originalDamage = hit(game, attacker, burrow);
            final CAttackType[] types = { CAttackType.NORMAL, CAttackType.PIERCE, CAttackType.SIEGE, CAttackType.MAGIC };
            final float[] baseline = new float[types.length];
            for (int i = 0; i < types.length; i++) baseline[i] = hit(game, attacker, burrow, types[i]);
            final float armor = burrow.getDefense();
            final CUnit otherOwner = game.createUnitSimple(War3ID.fromString("otrb"), 1, -512, 0, 0);
            game.getPlayer(0).setTechResearched(game, research, 1);
            assertEquals(CDefenseType.FORT, burrow.getDefenseType());
            final float fortifiedDamage = hit(game, attacker, burrow);
            assertTrue(fortifiedDamage < originalDamage, "Fortified armor must reduce normal attack damage");
            assertEquals(armor, burrow.getDefense(), "Armor class research must not change armor points");
            assertEquals(original, otherOwner.getDefenseType(), "Research belongs to one player");
            for (int i = 0; i < types.length; i++) {
                final float expectedRatio = game.getGameplayConstants().getDamageRatioAgainst(types[i], CDefenseType.FORT)
                        / game.getGameplayConstants().getDamageRatioAgainst(types[i], original);
                assertEquals(baseline[i] * expectedRatio, hit(game, attacker, burrow, types[i]), 0.001f,
                        "Damage must follow the supplied retail armor table for " + types[i]);
            }
            final CUnit trained = game.createUnitSimple(War3ID.fromString("otrb"), 0, 512, 0, 0);
            assertEquals(CDefenseType.FORT, trained.getDefenseType());
            assertEquals(original, burrow.getUnitType().getDefenseType(), "Shared unit definition must not change");
            game.getPlayer(0).setTechResearched(game, research, 0);
            assertEquals(original, burrow.getDefenseType());
            assertEquals(originalDamage, hit(game, attacker, burrow), 0.001f);
            assertEquals(original, trained.getDefenseType());

            final CUpgradeEffectDefenseType first = new CUpgradeEffectDefenseType(CDefenseType.FORT.ordinal());
            final CUpgradeEffectDefenseType second = new CUpgradeEffectDefenseType(CDefenseType.HERO.ordinal());
            first.apply(game, burrow, 1);
            second.apply(game, burrow, 1);
            first.unapply(game, burrow, 1);
            assertEquals(CDefenseType.HERO, burrow.getDefenseType());
            burrow.setDefenseType(CDefenseType.MEDIUM);
            second.unapply(game, burrow, 1);
            assertEquals(CDefenseType.MEDIUM, burrow.getDefenseType(), "Research removal must preserve other armor changes");
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void moonGlaivesAddsOneHitAndStopsWhenResearchIsRemovedMidFlight(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
			game.setFogEnabled(false);
			game.setFogMaskEnabled(false);
            final War3ID research = War3ID.fromString("Remg");
            final CUnit huntress = game.createUnitSimple(War3ID.fromString("esen"), 0, 0, 0, 0);
            final CUnit first = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 128, 0, 0);
            game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, 0, 0);
            game.createUnitSimple(War3ID.fromString("hfoo"), 1, 192, 0, 0);
            final CUnitAttackMissileBounce attack = (CUnitAttackMissileBounce) huntress.getUnitSpecificAttacks()
                    .stream().filter(a -> a instanceof CUnitAttackMissileBounce).findFirst().orElseThrow();
            assertTrue(huntress.getUnitType().getUpgradesUsed().contains(research));
            final int original = attack.getMaximumNumberOfTargets();
            assertEquals(2, original, "Classic Huntress hits the initial target and one secondary target");
            assertEquals(0.5f, attack.getDamageLossFactor());
            assertEquals(original, hits(game, huntress, first, attack, projectiles));
            game.getPlayer(0).setTechResearched(game, research, 1);
            assertEquals(original + 1, attack.getMaximumNumberOfTargets());
            assertEquals(original + 1, hits(game, huntress, first, attack, projectiles));
            game.getPlayer(0).setTechResearched(game, research, 1);
            assertEquals(original + 1, attack.getMaximumNumberOfTargets());
            game.getPlayer(0).setTechResearched(game, research, 0);
            assertEquals(original, attack.getMaximumNumberOfTargets());
            assertEquals(original, hits(game, huntress, first, attack, projectiles));
            game.getPlayer(0).setTechResearched(game, research, 1);
            projectiles.clear();
            attack.launch(game, huntress, first, 20, CUnitAttackListener.DO_NOTHING);
            // Reach the last baseline target, leaving its newly launched extra bounce in flight.
            for (int i = 0; i < original; i++) finish(game, projectiles.get(i));
            assertEquals(original + 1, projectiles.size());
            game.getPlayer(0).setTechResearched(game, research, 0);
            finish(game, projectiles.get(original));
            assertEquals(original + 1, projectiles.size(),
                    "An in-flight bounce beyond a reduced cap must not launch another missile");
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void overlappingGlaivesKeepSeparateInitialTargetsAndStopWhenNoOtherTargetExists(boolean standardMeleeData)
            throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit huntress = game.createUnitSimple(War3ID.fromString("esen"), 0, 0, 0, 0);
            final CUnit first = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 128, 0, 0);
            final CUnit second = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 192, 0, 0);
            final CUnitAttackMissileBounce attack = (CUnitAttackMissileBounce) huntress.getUnitSpecificAttacks()
                    .stream().filter(a -> a instanceof CUnitAttackMissileBounce).findFirst().orElseThrow();
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Remg"), 1);
            assertTrue(attack.getMaximumNumberOfTargets() >= 3);
            final List<com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget> firstHits = new ArrayList<>();
            final List<com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget> secondHits = new ArrayList<>();
            attack.launch(game, huntress, first, 20, recording(firstHits));
            attack.launch(game, huntress, second, 20, recording(secondHits));
            for (int i = 0; i < projectiles.size(); i++) {
                assertTrue(i < 6, "Overlapping chains must terminate");
                finish(game, projectiles.get(i));
            }
            assertEquals(List.of(first, second), firstHits);
            assertEquals(List.of(second, first), secondHits);
            assertEquals(4, projectiles.size(), "Neither chain may bounce back to its own initial target");
        });
    }

    private static CUnitAttackListener recording(
            List<com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget> hits) {
        return new CUnitAttackListener() {
            @Override public void onLaunch() { }
            @Override public void onHit(com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget target,
                    float damage) { hits.add(target); }
        };
    }

    private static float hit(CSimulation game, CUnit attacker, CUnit target) {
        return hit(game, attacker, target, CAttackType.NORMAL);
    }

    private static float hit(CSimulation game, CUnit attacker, CUnit target, CAttackType type) {
        target.setLife(game, target.getMaximumLife());
        final float before = target.getLife();
        target.damage(game, attacker, true, false, type, CDamageType.NORMAL, "", 100);
        return before - target.getLife();
    }

    private static int hits(CSimulation game, CUnit source, CUnit first, CUnitAttackMissileBounce attack,
            List<CAttackProjectileMissile> projectiles) {
        projectiles.clear();
        attack.launch(game, source, first, 20, CUnitAttackListener.DO_NOTHING);
        int count = 0;
        for (int i = 0; i < projectiles.size(); i++) {
            assertTrue(i < 10, "Bounce chain must terminate");
            final CAttackProjectileMissile shot = projectiles.get(i);
            final CUnit target = (CUnit) shot.getTarget();
            if (i > 0) assertNotSame(first, target, "A glaive must never return to its initial target");
            assertEquals(20 * Math.pow(1 - attack.getDamageLossFactor(), i), shot.getDamage(), 0.0001);
            final float before = target.getLife();
            finish(game, shot);
            assertTrue(Float.isFinite(shot.getX()) && Float.isFinite(shot.getY()));
            assertTrue(target.getLife() < before, "Bounce must deal damage");
            final float after = target.getLife();
            shot.update(game);
            assertEquals(after, target.getLife(), "Completed missile must not deal damage twice");
            count++;
        }
        return count;
    }

    private static void finish(CSimulation game, CAttackProjectileMissile shot) {
        for (int tick = 0; tick < 100 && !shot.isDone(); tick++) shot.update(game);
        assertTrue(shot.isDone(), "Projectile must reach its target within the tick budget");
    }

    private interface Scenario { void run(CSimulation game, List<CAttackProjectileMissile> projectiles); }

    private static void withFixture(boolean standardMeleeData, Scenario scenario) throws Exception {
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
                    System.out.println("Combat research fixture: " + (standardMeleeData ? "standard melee data" : "Human01"));
                    final var data = standardMeleeData ? Warcraft3MapRuntimeObjectData.load(source, true) : map.readModifications();
                    final List<CAttackProjectileMissile> projectiles = new ArrayList<>();
                    scenario.run(RetailSimulationTestSupport.simulation(map, data, projectiles::add), projectiles);
                }
            }
        }
        finally {
            WarsmashConstants.RACE_MANAGER = previous;
            for (MpqDataSource archive : archives) archive.close();
        }
    }
}
