package com.etheller.warsmash.viewer5.handlers.w3x.simulation;
import com.etheller.warsmash.testutil.RetailTestData;


import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectileMissile;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.PathingFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityLoad;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityExhumeCorpses;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilitySearingArrows;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat.CAbilityBlackArrow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemStatBonus;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilitySoulPossession;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityBlightedGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.CAbilityShadowMeld;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.CAbilitySentinel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityNightElfChannel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityFaerieFire;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityFlare;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityFingerOfDeath;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityRainOfChaos;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.warden.CAbilityFanOfKnives;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer.CAbilityFarSight;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CAbilityFrostArmor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CBuffFrost;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CAbilityAttackPoison;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffSlow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityEnsnare;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityPillage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityWeb;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileSplash;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilitySpiderAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileLine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileBounce;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerScore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CUnitState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade.CUpgradeEffectDefenseType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;
import mpq.MPQArchive;

class CombatUpgradeSimulationTest {
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void originalDamageWavesRespectShapeTargetsAndBudget(final boolean shockwave) throws Exception {
        withFixture("Maps\\Campaign\\Orc02.w3m", false, (game, projectiles) -> {
            final String heroType = shockwave ? "Otch" : "Udre";
            final War3ID spell = War3ID.fromString(shockwave ? "AOsh" : "AUcs");
            final CUnit caster = game.createUnitSimple(War3ID.fromString(heroType), 0, 0, 0, 0);
            final List<CUnit> foes = new ArrayList<>();
            for (int index = 0; index < 6; index++) {
                foes.add(game.createUnitSimple(War3ID.fromString("hfoo"), 1, index * 105 + 120, 0, 0));
            }
            final CUnit air = game.createUnitSimple(War3ID.fromString("hgry"), 1, 145, 0, 0);
            final CUnit immune = game.createUnitSimple(War3ID.fromString("edry"), 1, 95, -55, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 200, 70, 0);
            final CUnit outside = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 320, 460, 0);
            final CUnit building = game.createUnitSimple(War3ID.fromString("hbla"), 1, 690, -115, 0);
            final List<CUnit> participants = new ArrayList<>(foes);
            participants.addAll(List.of(caster, air, immune, ally, outside, building));
            for (final CUnit unit : participants) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
                if (unit != caster) {
                    unit.setPaused(true);
                }
            }
            game.update();
            assertTrue(immune.isMagicImmune(), "The original dryad must be spell immune");
            assertTrue(caster.order(game, spell.getValue(), null));
            assertEquals(1, caster.getAbilityLevel(spell));
            caster.setMana(caster.getMaximumMana());
            final int order = shockwave ? OrderIds.shockwave : OrderIds.carrionswarm;
            assertTrue(caster.order(game, order, new AbilityPointTarget(400, 0)));
            boolean projectileLaunched = false;
            for (int tick = 0; tick < 60; tick++) {
                game.update();
                projectileLaunched |= game.getActiveProjectileCount() > 0;
            }
            assertTrue(projectileLaunched, "The cast never launched a projectile; mana=" + caster.getMana());
            assertEquals(ally.getMaximumLife(), ally.getLife(), 0.001f);
            assertEquals(immune.getMaximumLife(), immune.getLife(), 0.001f);
            assertEquals(outside.getMaximumLife(), outside.getLife(), 0.001f);
            if (shockwave) {
                for (final CUnit foe : foes) {
                    assertEquals(foe.getMaximumLife() - 75, foe.getLife(), 0.001f, "Enemy at " + foe.getX());
                }
                assertEquals(air.getMaximumLife(), air.getLife(), 0.001f);
                assertTrue(building.getLife() < building.getMaximumLife());
            }
            else {
                final double unitDamage = foes.stream().mapToDouble(unit -> unit.getMaximumLife() - unit.getLife()).sum();
                assertTrue(air.getLife() < air.getMaximumLife(), "Carrion Swarm can hit air units");
                assertEquals(375, unitDamage + air.getMaximumLife() - air.getLife(), 0.001);
                assertEquals(building.getMaximumLife(), building.getLife(), 0.001f);
            }
        });
    }

    @Test
    void originalPillageRequiresResearchAndPaysForEnemyStructureHealthRemoved() throws Exception {
        withFixture("Maps\\Campaign\\Orc03.w3m", false, (game, projectiles) -> {
            final var grunt = game.createUnitSimple(War3ID.fromString("ogru"), 0, 0, 0, 0);
            final var building = game.createUnitSimple(War3ID.fromString("hbar"), 1, 150, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hbar"), 0, -150, 0, 0);
            grunt.setDisableAttacks(true);
            game.update();
            final var owner = game.getPlayer(grunt.getPlayerIndex());
            final var research = War3ID.fromString("Ropg");
            final var pillage = grunt.getFirstAbilityOfType(CAbilityPillage.class);
            assertNotNull(pillage, "The campaign Grunt lacks Pillage");
            assertTrue(building.isBuilding());
            assertFalse(owner.hasAlliance(building.getPlayerIndex(), CAllianceType.PASSIVE));
            owner.setTechtreeUnlocked(game, research, 0);
            final int initialGold = owner.getGold();
            final int initialLumber = owner.getLumber();
            final float quarterHealth = building.getMaximumLife() / 4;
            building.damage(game, grunt, true, false, CAttackType.CHAOS, CDamageType.UNIVERSAL, null, quarterHealth);
            assertEquals(initialGold, owner.getGold());
            assertEquals(initialLumber, owner.getLumber());
            building.setLife(game, building.getMaximumLife());
            owner.setTechtreeUnlocked(game, research, 1);
            game.update();
            assertTrue(pillage.isRequirementsMet(game, grunt), "Pillage research requirements were not satisfied");
            assertFalse(pillage.isDisabled(), "Pillage remained disabled after research");
            ally.damage(game, grunt, true, false, CAttackType.CHAOS, CDamageType.UNIVERSAL, null, quarterHealth);
            building.damage(game, grunt, false, false, CAttackType.SPELLS, CDamageType.UNIVERSAL, null, quarterHealth);
            assertEquals(initialGold, owner.getGold());
            assertEquals(initialLumber, owner.getLumber());
            building.setLife(game, building.getMaximumLife());
            building.damage(game, grunt, true, false, CAttackType.CHAOS, CDamageType.UNIVERSAL, null, quarterHealth);
            assertTrue(owner.getGold() > initialGold,
                    () -> "No gold from " + building.getUnitType().getGoldCost() + " gold structure with targets "
                            + building.getUnitType().getTargetedAs() + " against Pillage targets " + pillage.getTargetsAllowed());
            assertTrue(owner.getLumber() > initialLumber);
            building.damage(game, grunt, true, false, CAttackType.CHAOS, CDamageType.UNIVERSAL, null,
                    building.getMaximumLife() * 2);
            assertTrue(building.isDead());
            assertEquals(initialGold + building.getUnitType().getGoldCost() / 2, owner.getGold());
            assertEquals(initialLumber + building.getUnitType().getLumberCost() / 2, owner.getLumber());
        });
    }

    @Test
    void originalInstantEntangleCompletesBeforeTheFirstTickAndKeepsTheMinesGold() throws Exception {
        withFixture("Maps\\Campaign\\NightElf03.w3m", false, (game, projectiles) -> {
            final var tree = game.createUnitSimple(War3ID.fromString("etol"), 0, -1472, -5376, 0);
            final var base = game.createUnitSimple(War3ID.fromString("ngol"), 1, -1024, -4864, 0);
            base.getGoldMineData().setGold(6_000);
            tree.setPaused(true);

            assertTrue(tree.order(game, OrderIds.entangleinstant, base));
            final var overlay = game.getUnitsIncludingPending().stream()
                    .filter(unit -> "egol".equals(unit.getTypeId().toString())).findFirst().orElseThrow();
            assertEquals(0, overlay.getPlayerIndex());
            assertFalse(overlay.isConstructing());
            assertEquals(6_000, overlay.getOverlayedGoldMineData().getGold());
            assertTrue(base.isHidden());
            assertFalse(tree.order(game, OrderIds.entangleinstant, base));
        });
    }

    @Test
    void originalAcolyteUnsummonsOwnedBuildingsAndSalvagesOnlyLifeItRemoves() throws Exception {
        withFixture("Maps\\Campaign\\Undead04.w3m", false, (game, projectiles) -> {
            final var worker = game.createUnitSimple(War3ID.fromString("uaco"), 0, 0, 0, 0);
            final var crypt = game.createUnitSimple(War3ID.fromString("usep"), 0, 96, 0, 0);
            final var enemyCrypt = game.createUnitSimple(War3ID.fromString("usep"), 1, 0, 350, 0);
            final var humanBarracks = game.createUnitSimple(War3ID.fromString("hbar"), 0, 0, -350, 0);
            worker.setDisableAttacks(true);
            game.update();
            assertFalse(worker.order(game, OrderIds.unsummon, enemyCrypt));
            assertFalse(worker.order(game, OrderIds.unsummon, humanBarracks));

            final var owner = game.getPlayer(0);
            final int goldBefore = owner.getGold();
            final int lumberBefore = owner.getLumber();
            final float fullLife = crypt.getMaximumLife();
            crypt.setLife(game, fullLife / 2);
            assertTrue(worker.order(game, OrderIds.unsummon, crypt));
            for (int tick = 0; tick < 80 && !crypt.isUnsummoning(); tick++) game.update();
            assertTrue(crypt.isUnsummoning());
            assertEquals(fullLife / 2 - 50, crypt.getLife());
            assertFalse(worker.order(game, OrderIds.unsummon, crypt));
            assertFalse(worker.order(game, OrderIds.repair, crypt));
            assertFalse(crypt.order(game, OrderIds.stop, null));

            worker.order(game, OrderIds.move, new AbilityPointTarget(-300, 0));
            int ticks = Math.round(fullLife / 50 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < ticks && !crypt.isDead(); tick++) game.update();
            assertTrue(crypt.isDead());
            assertFalse(crypt.isUnsummoning());
            assertEquals(goldBefore + crypt.getUnitType().getGoldCost() / 4, owner.getGold());
            assertEquals(lumberBefore + crypt.getUnitType().getLumberCost() / 4, owner.getLumber());
        });
    }

    @Test
    void originalAcolyteUnsummonsTheHauntedMineAfterMiningItsLastGold() throws Exception {
        withFixture("Maps\\Campaign\\Undead04.w3m", false, (game, projectiles) -> {
            final var baseMine = game.createUnitSimple(War3ID.fromString("ngol"), 1, 0, 0, 0);
            final var haunted = game.createUnitSimple(War3ID.fromString("ugol"), 0, 0, 0, 0);
            final var worker = game.createUnitSimple(War3ID.fromString("uaco"), 0, 0, 175, 0);
            final var gold = baseMine.getFirstAbilityOfType(CAbilityGoldMine.class);
            final var overlay = haunted.getFirstAbilityOfType(CAbilityBlightedGoldMine.class);
            assertNotNull(gold);
            assertNotNull(overlay);
            gold.setGold(1);
            overlay.setParentMine(baseMine, gold);
            baseMine.setHidden(true);
            baseMine.setPaused(true);
            worker.setDisableAttacks(true);
            game.update();
            assertTrue(worker.order(game, OrderIds.acolyteharvest, haunted));
            for (int tick = 0; tick < 640 && !haunted.isDead(); tick++) game.update();
            assertEquals(0, gold.getGold());
            assertEquals(1, game.getPlayer(0).getMissionScore(CPlayerScore.GOLD_MINED_TOTAL));
            assertTrue(haunted.isDead(), "The worker did not unsummon the exhausted overlay");
            assertFalse(baseMine.isHidden());
            assertFalse(baseMine.isPaused());
        });
    }

    @Test
    void originalInfernoLandsAfterTheDelayAndSummonsAnInfernalWithImmolation() throws Exception {
        withFixture("Maps\\Campaign\\Undead08.w3m", false, (game, projectiles) -> {
            final var dreadlord = game.createUnitSimple(War3ID.fromString("Udre"), 0, 0, 0, 0);
            final var soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 500, 0, 0);
            final var hero = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 515, 120, 0);
            final var air = game.createUnitSimple(War3ID.fromString("hgry"), 1, 420, -80, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 420, 100, 0);
            final var tree = game.internalCreateDestructable(War3ID.fromString("LTlt"), 360, -60, null, null);
            for (final CUnit unit : List.of(dreadlord, soldier, hero, air, ally)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
                if (unit != dreadlord) unit.setPaused(true);
            }
            game.update();
            dreadlord.getHeroData().setHeroLevel(game, dreadlord, 6, false);
            final var inferno = War3ID.fromString("AUin");
            assertTrue(dreadlord.order(game, inferno.getValue(), null));
            assertEquals(1, dreadlord.getAbilityLevel(inferno));
            dreadlord.setMana(dreadlord.getMaximumMana());
            assertTrue(dreadlord.order(game, OrderIds.inferno, new AbilityPointTarget(400, 0)));
            for (int tick = 0; tick < 32 && dreadlord.getMana() == dreadlord.getMaximumMana(); tick++) game.update();
            assertTrue(dreadlord.getMana() < dreadlord.getMaximumMana(), "Inferno did not start casting");
            final var infernalType = War3ID.fromString("ninf");
            assertTrue(game.getUnits().stream().noneMatch(unit -> infernalType.equals(unit.getTypeId())));
            assertFalse(tree.isDead());
            assertEquals(soldier.getMaximumLife(), soldier.getLife(), 0.001f);
            for (int tick = 0; tick < 10; tick++) game.update();
            assertFalse(tree.isDead(), "The impact ignored its authored one-second delay");
            for (int tick = 0; tick < 14 && !tree.isDead(); tick++) game.update();
            assertTrue(tree.isDead());
            assertEquals(soldier.getMaximumLife() - 50, soldier.getLife(), 0.001f);
            assertEquals(ally.getMaximumLife(), ally.getLife(), 0.001f);
            assertEquals(air.getMaximumLife(), air.getLife(), 0.001f);
            assertTrue(soldier.isUnitType(CUnitTypeJass.STUNNED));
            assertTrue(hero.isUnitType(CUnitTypeJass.STUNNED));
            soldier.setPaused(false);
            hero.setPaused(false);
            game.update();
            final var infernal = game.getUnits().stream().filter(unit -> infernalType.equals(unit.getTypeId()))
                    .findFirst().orElseThrow();
            assertTrue(infernal.isUnitType(CUnitTypeJass.SUMMONED));
            assertTrue(infernal.isMagicImmune());
            assertEquals(dreadlord.getPlayerIndex(), infernal.getPlayerIndex());
            infernal.setDisableAttacks(true);
            for (int tick = 0; tick < 22; tick++) game.update();
            assertEquals(soldier.getMaximumLife() - 60, soldier.getLife(), 0.001f);
            for (int tick = 0; tick < 21; tick++) game.update();
            assertFalse(hero.isUnitType(CUnitTypeJass.STUNNED));
            assertTrue(soldier.isUnitType(CUnitTypeJass.STUNNED));
            for (int tick = 0; tick < 40; tick++) game.update();
            assertFalse(soldier.isUnitType(CUnitTypeJass.STUNNED));
            assertFalse(infernal.isDead());
            for (int tick = 0; tick < 3600 && !infernal.isDead(); tick++) game.update();
            assertTrue(infernal.isDead(), "Infernal exceeded its authored lifetime");
        });
    }

    @Test
    void originalAnimateDeadRaisesTheStrongestEligibleCorpsesTemporarily() throws Exception {
        withFixture("Maps\\Campaign\\Undead08.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var knight = War3ID.fromString("hkni");
            final var footman = War3ID.fromString("hfoo");
            final var deathKnight = game.createUnitSimple(War3ID.fromString("Udea"), 0, 0, 0, 0);
            deathKnight.setDisableAttacks(true);
            final List<CUnit> casualties = new ArrayList<>();
            for (int index = 0; index < 6; index++) {
                casualties.add(game.createUnitSimple(footman, index % 2, 100 * (index + 1), 160, 0));
            }
            final CUnit strongCorpse = game.createUnitSimple(knight, 1, -560, 0, 0);
            final CUnit outside = game.createUnitSimple(knight, 1, -1280, 0, 0);
            casualties.add(strongCorpse);
            casualties.add(outside);
            casualties.forEach(unit -> unit.setDisableAttacks(true));
            game.update();
            deathKnight.getHeroData().setHeroLevel(game, deathKnight, 6, false);
            final War3ID spell = War3ID.fromString("AUan");
            assertTrue(deathKnight.order(game, spell.getValue(), null));
            assertEquals(1, deathKnight.getAbilityLevel(spell));
            deathKnight.setMana(deathKnight.getMaximumMana());
            casualties.forEach(unit -> unit.kill(game));
            for (int tick = 0; tick < 1600 && !casualties.stream().allMatch(CUnit::isBoneCorpse); tick++) game.update();
            assertTrue(casualties.stream().allMatch(CUnit::isBoneCorpse), "The original corpse-ready phase never completed");
            final int food = game.getPlayer(0).getFoodUsed();
            assertTrue(deathKnight.order(game, OrderIds.animatedead, null));
            for (int tick = 0; tick < 50; tick++) game.update();
            final var raised = game.getUnits().stream().filter(unit -> unit != deathKnight && !unit.isDead())
                    .filter(unit -> unit.getPlayerIndex() == 0 && unit.isUnitType(CUnitTypeJass.SUMMONED)).toList();
            assertEquals(6, raised.size());
            assertEquals(1, raised.stream().filter(unit -> unit.getTypeId().equals(knight)).count());
            assertTrue(raised.stream().allMatch(CUnit::isInvulnerable));
            assertTrue(outside.isCorpse());
            assertEquals(food, game.getPlayer(0).getFoodUsed());
            for (int tick = 0; tick < 800; tick++) game.update();
            assertTrue(raised.stream().allMatch(CUnit::isDead));
            assertFalse(raised.stream().anyMatch(CUnit::isRaisable));
        });
    }

    @Test
    void repeatingAFormationMoveAllowsTheCurrentPathToProgress() throws Exception {
        withFixture("Maps\\Campaign\\Orc02.w3m", false, (game, projectiles) -> {
            final CUnit grunt = game.createUnitSimple(War3ID.fromString("ogru"), 0, -416, -64, 180);
            game.update();
            final var goal = new AbilityPointTarget(384, -64);
            for (int tick = 0; tick < 100; tick++) {
                assertTrue(grunt.order(game, OrderIds.move, goal));
                game.update();
            }
            assertEquals(goal.getX(), grunt.getX(), 1f);
            assertEquals(goal.getY(), grunt.getY(), 1f);
        });
    }

    @Test
    void originalSnapdragonPoisonStacksByAttackerAndCannotKill() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX01.w3x", false, (game, projectiles) -> {
            final var snapdragon = War3ID.fromString("nsnp");
            final var first = game.createUnitSimple(snapdragon, 0, -410, 0, 0);
            final var second = game.createUnitSimple(snapdragon, 0, -410, 140, 0);
            final var soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 0, 0);
            final var hero = game.createUnitSimple(War3ID.fromString("Ewrd"), 1, 0, 140, 0);
            for (final CUnit unit : List.of(first, second, soldier, hero)) unit.setDisableAttacks(true);
            soldier.setLifeRegenBonus(-soldier.getUnitType().getLifeRegen());
            hero.setLifeRegenBonus(-hero.getUnitType().getLifeRegen());
            hero.setLifeRegenStrengthBonus(0);
            game.update();
            assertNotNull(first.getFirstAbilityOfType(CAbilityAttackPoison.class), "Snapdragon lacks its original Slow Poison");
            final var firstAttack = first.getUnitSpecificAttacks().get(0);
            final var secondAttack = second.getUnitSpecificAttacks().get(0);
            final int soldierSpeed = soldier.getSpeed();
            final float soldierAttackSpeed = soldier.getUnitSpecificAttacks().get(0).getAttackSpeedModifier();
            final float soldierBeforeHits = soldier.getLife();
            for (int hit = 0; hit < 2; hit++) {
                firstAttack.launch(game, first, soldier, 18, CUnitAttackListener.DO_NOTHING);
                finish(game, projectiles.remove(0));
            }
            secondAttack.launch(game, second, soldier, 18, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            firstAttack.launch(game, first, hero, 18, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            assertTrue(soldier.getLife() < soldierBeforeHits, "The weapon projectiles did not hit");
            assertTrue(soldier.isUnitType(CUnitTypeJass.POISONED));
            assertTrue(hero.isUnitType(CUnitTypeJass.POISONED));
            assertTrue(soldier.getSpeed() < soldierSpeed);
            assertEquals(soldierAttackSpeed - 0.25f, soldier.getUnitSpecificAttacks().get(0).getAttackSpeedModifier(), 0.001f);
            final float afterHits = soldier.getLife();
            final float heroAfterHit = hero.getLife();
            for (int tick = 0; tick < 22; tick++) game.update();
            assertEquals(afterHits - 8, soldier.getLife(), 0.01f);
            assertEquals(heroAfterHit - 4, hero.getLife(), 0.1f);
            assertFalse(hero.isUnitType(CUnitTypeJass.POISONED));
            soldier.setLife(game, 3);
            for (int tick = 0; tick < 100; tick++) game.update();
            assertEquals(2, soldier.getLife(), 0.01f);
            assertFalse(soldier.isUnitType(CUnitTypeJass.POISONED));
            assertEquals(soldierSpeed, soldier.getSpeed());
            assertEquals(soldierAttackSpeed, soldier.getUnitSpecificAttacks().get(0).getAttackSpeedModifier(), 0.001f);
        });
    }

    @Test
    void frozenThroneWardenCastsFanOfKnivesWithinTheAuthoredDamageCap() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var warden = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, 0, 0, 0);
            warden.setDisableAttacks(true);
            final War3ID fan = War3ID.fromString("AEfk");
            final War3ID footman = War3ID.fromString("hfoo");
            final var victims = List.of(game.createUnitSimple(footman, 1, -216, 0, 0),
                    game.createUnitSimple(footman, 1, 0, -216, 0), game.createUnitSimple(footman, 1, 0, 216, 0),
                    game.createUnitSimple(footman, 1, 200, 0, 0),
                    game.createUnitSimple(War3ID.fromString("hgry"), 1, 150, 150, 0));
            final var ally = game.createUnitSimple(footman, 0, 150, -150, 0);
            final var distant = game.createUnitSimple(footman, 1, 736, 0, 0);
            final List<CUnit> hitUnits = new ArrayList<>();
            for (final CUnit victim : victims) {
                victim.setPaused(true);
                victim.addDamageTakenListener((simulation, attacker, target, isAttack, isRanged, type, damage, bonus, actual) -> {
                    assertSame(warden, attacker);
                    assertEquals(70, damage, 0.01, "the 350-point cap must be shared across the five eligible targets");
                    assertTrue(actual > 0);
                    hitUnits.add(target);
                });
            }
            ally.setPaused(true);
            distant.setPaused(true);
            game.update();
            assertTrue(warden.order(game, fan.getValue(), null), "Maiev must be able to learn her original spell");
            assertNotNull(warden.getFirstAbilityOfType(CAbilityFanOfKnives.class));
            warden.setMana(warden.getMaximumMana());
            assertTrue(warden.order(game, OrderIds.fanofknives, null));
            for (int tick = 0; tick < 75; tick++) game.update();
            assertEquals(victims.size(), hitUnits.size());
            assertTrue(hitUnits.containsAll(victims));
            assertEquals(ally.getMaximumLife(), ally.getLife());
            assertEquals(distant.getMaximumLife(), distant.getLife());
        });
    }

    @Test
    void originalFrostArmorAddsDefenseAndChillsOnlyMeleeAttackers() throws Exception {
        withFixture("Maps\\Campaign\\Human05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID footman = War3ID.fromString("hfoo");
            final var lich = game.createUnitSimple(War3ID.fromString("Ulic"), 0, 0, 0, 0);
            final var ally = game.createUnitSimple(footman, 0, 300, 0, 0);
            final var melee = game.createUnitSimple(footman, 1, 360, 0, 0);
            final var ranged = game.createUnitSimple(War3ID.fromString("hrif"), 1, 500, 256, 0);
            for (final CUnit unit : List.of(lich, ally, melee, ranged)) unit.setDisableAttacks(true);
            final float armor = ally.getDefense();
            final int meleeSpeed = melee.getSpeed();
            final int rangedSpeed = ranged.getSpeed();
            game.update();
            final War3ID manualArmor = War3ID.fromString("AUfa");
            assertTrue(lich.getHeroData().getSkillsAvailable().contains(War3ID.fromString("AUfu")));
            lich.getHeroData().selectHeroSkill(game, lich, manualArmor);
            assertEquals(1, lich.getAbilityLevel(manualArmor));
            assertTrue(lich.getHeroData().getSkillsAvailable().contains(manualArmor));
            final var manual = lich.getFirstAbilityOfType(CAbilityFrostArmor.class);
            assertNotNull(manual);
            assertEquals(0, manual.getAutoCastOnOrderId());
            lich.setMana(lich.getMaximumMana());
            assertFalse(lich.order(game, OrderIds.frostarmor, melee));
            assertTrue(lich.order(game, OrderIds.frostarmor, ally));
            for (int tick = 0; tick < 60 && ally.getDefense() == armor; tick++) game.update();
            assertEquals(armor + 3, ally.getDefense());
            assertEquals(3.0f, ally.getTotalTemporaryDefenseBonus());
            ally.damage(game, ranged, true, true, CAttackType.NORMAL, CDamageType.NORMAL, null, 10);
            assertEquals(rangedSpeed, ranged.getSpeed());
            ally.damage(game, melee, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 10);
            assertEquals(meleeSpeed / 2, melee.getSpeed());
            for (int tick = 0; tick < 103; tick++) game.update();
            assertEquals(meleeSpeed, melee.getSpeed());
            assertEquals(armor + 3, ally.getDefense());
            for (int tick = 0; tick < 801; tick++) game.update();
            assertEquals(armor, ally.getDefense());

            lich.remove(game, manual);
            final var autocast = (CAbilityFrostArmor) game.getAbilityData().createAbility(War3ID.fromString("AUfu"),
                    game.getHandleIdAllocator().createId());
            lich.add(game, autocast);
            assertTrue(lich.order(game, OrderIds.frostarmoron, null));
            assertTrue(autocast.isAutoCastOn());
            assertTrue(lich.order(game, OrderIds.frostarmoroff, null));
            assertFalse(autocast.isAutoCastOn());
        });
    }

    @Test
    void originalMirrorImageCopiesTheBlademasterWithoutAddingARealHero() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit master = game.createUnitSimple(War3ID.fromString("Obla"), 0, 0, 0, 0);
            final CUnit target = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 410, 0, 0);
            master.setDisableAttacks(true);
            target.setDisableAttacks(true);
            game.update();
            final War3ID mirror = War3ID.fromString("AOmi");
            assertTrue(master.order(game, mirror.getValue(), null));
            assertEquals(1, master.getAbilityLevel(mirror));
            master.getHeroData().setHeroLevel(game, master, 3, false);
            master.setLife(game, 400);
            master.setMana(master.getMaximumMana());
            final int food = game.getPlayer(0).getFoodUsed();
            assertTrue(master.order(game, OrderIds.mirrorimage, null));
            int splitTicks = 0;
            for (int tick = 0; tick < 65 && game.getUnitsIncludingPending().stream().noneMatch(CUnit::isIllusion); tick++) {
                game.update();
                if (master.isHidden()) {
                    splitTicks++;
                    assertTrue(master.isInvulnerable());
                }
            }
            // The cast starts before this step advances the tick, and the timer fires during the tenth step.
            assertEquals(9, splitTicks);
            assertFalse(master.isHidden());
            assertFalse(master.isInvulnerable());
            final CUnit first = game.getUnitsIncludingPending().stream().filter(CUnit::isIllusion).findFirst().orElseThrow();
            first.setDisableAttacks(true);
            assertEquals(master.getTypeId(), first.getTypeId());
            assertEquals(master.getLife(), first.getLife(), 0.1f);
            assertEquals(master.getHeroData().getHeroLevel(), first.getHeroData().getHeroLevel());
            assertEquals(master.getHeroData().getProperName(), first.getHeroData().getProperName());
            assertEquals(0, first.getAbilityLevel(mirror));
            assertEquals(List.of(master), game.getPlayerHeroes(0));
            assertEquals(food, game.getPlayer(0).getFoodUsed());
            final float targetLife = target.getLife();
            target.damage(game, first, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 50);
            assertEquals(targetLife, target.getLife());
            final float realDamage = master.damage(game, target, false, false, CAttackType.SPELLS, CDamageType.UNIVERSAL, null, 20);
            assertEquals(2 * realDamage,
                    first.damage(game, target, false, false, CAttackType.SPELLS, CDamageType.UNIVERSAL, null, 20), 0.1f);
            master.getHeroData().setHeroLevel(game, master, 4, false);
            assertEquals(4, first.getHeroData().getHeroLevel());
            for (int tick = 0; tick < 80; tick++) game.update();
            master.setMana(master.getMaximumMana());
            assertTrue(master.order(game, OrderIds.mirrorimage, null));
            for (int tick = 0; tick < 65; tick++) game.update();
            assertTrue(first.isDead());
            assertEquals(1, game.getUnitsIncludingPending().stream().filter(CUnit::isIllusion).filter(unit -> !unit.isDead()).count());
            for (int tick = 0; tick < 1250; tick++) game.update();
            assertTrue(game.getUnitsIncludingPending().stream().noneMatch(unit -> unit.isIllusion() && !unit.isDead()));
            assertEquals(List.of(master), game.getPlayerHeroes(0));
        });
    }

    @Test
    void originalSiphonManaDrainsAnEnemyInsideTheLeashAndStopsOutsideIt() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX03.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit mage = game.createUnitSimple(War3ID.fromString("Hblm"), 0, 0, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("Hamg"), 0, 350, 160, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 500, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 400, -150, 0);
            mage.setDisableAttacks(true);
            for (final CUnit unit : List.of(ally, enemy, footman)) unit.setPaused(true);
            game.update();
            final War3ID siphon = War3ID.fromString("AHdr");
            assertTrue(mage.order(game, siphon.getValue(), null));
            assertEquals(1, mage.getAbilityLevel(siphon));
            for (final CUnit unit : List.of(mage, enemy)) {
                unit.setManaRegen(0);
                unit.setManaRegenIntelligenceBonus(0);
            }
            mage.setMana(50);
            enemy.setMana(100);
            assertFalse(mage.order(game, OrderIds.drain, ally));
            assertFalse(mage.order(game, OrderIds.drain, footman));
            assertTrue(mage.order(game, OrderIds.drain, enemy));
            for (int tick = 0; tick < 55 && enemy.getMana() == 100; tick++) game.update();
            assertEquals(82f, enemy.getMana(), 0.1f);
            assertEquals(58f, mage.getMana(), 0.1f);
            enemy.setPointAndCheckUnstuck(780, 0, game);
            for (int tick = 0; tick < 25 && enemy.getMana() == 82; tick++) game.update();
            assertEquals(64f, enemy.getMana(), 0.1f);
            enemy.setPointAndCheckUnstuck(990, 0, game);
            for (int tick = 0; tick < 50; tick++) game.update();
            assertEquals(64f, enemy.getMana(), 0.1f);
            assertEquals(76f, mage.getMana(), 0.1f);
        });
    }

    @Test
    void originalDarkRitualSacrificesAnOwnedUnitAndRestoresManaFromItsRemainingLife() throws Exception {
        withFixture("Maps\\Campaign\\Human05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(game.getPlayer(1), CAllianceType.PASSIVE, true);
            final CUnit lich = game.createUnitSimple(War3ID.fromString("Ulic"), 0, 0, 0, 0);
            final CUnit sacrifice = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 300, 0, 0);
            final CUnit otherOwner = game.createUnitSimple(War3ID.fromString("ugho"), 1, 400, 110, 0);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, 300, -190, 0);
            lich.setDisableAttacks(true);
            for (final CUnit unit : List.of(sacrifice, otherOwner, hero)) unit.setPaused(true);
            sacrifice.setLifeRegenBonus(-sacrifice.getUnitType().getLifeRegen());
            sacrifice.setLife(game, 120);
            game.update();
            final War3ID ritual = War3ID.fromString("AUdr");
            assertTrue(lich.order(game, ritual.getValue(), null));
            assertEquals(1, lich.getAbilityLevel(ritual));
            lich.setManaRegen(0);
            lich.setManaRegenIntelligenceBonus(0);
            lich.setMana(30);
            assertFalse(lich.order(game, OrderIds.darkritual, otherOwner));
            assertFalse(lich.order(game, OrderIds.darkritual, hero));
            assertTrue(lich.order(game, OrderIds.darkritual, sacrifice));
            for (int tick = 0; tick < 60 && !sacrifice.isDead(); tick++) game.update();
            assertTrue(sacrifice.isDead());
            assertFalse(otherOwner.isDead());
            assertFalse(hero.isDead());
            assertEquals(30 - 25 + 120 * .33f, lich.getMana(), 0.1f);
        });
    }

    @Test
    void originalDeathAndDecayUsesMaximumLifeAndDamagesAlliesBuildingsAndTrees() throws Exception {
        withFixture("Maps\\Campaign\\Human05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit lich = game.createUnitSimple(War3ID.fromString("Ulic"), 0, 0, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 425, 160, 0);
            final CUnit air = game.createUnitSimple(War3ID.fromString("hgry"), 1, 480, -160, 0);
            final CUnit farm = game.createUnitSimple(War3ID.fromString("hhou"), 1, 525, 0, 0);
            final CUnit outside = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 900, 0, 0);
            final CDestructable tree = game.internalCreateDestructable(War3ID.fromString("ATtr"), 525, -96, null, null);
            final List<Float> damageTaken = new ArrayList<>();
            final float treeLife = tree.getLife();
            lich.setDisableAttacks(true);
            for (final CUnit unit : List.of(ally, air, farm, outside)) {
                unit.setPaused(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            ally.setLife(game, ally.getMaximumLife() / 2f);
            ally.addDamageTakenListener((simulation, attacker, victim, isAttack, isRanged, type, amount, bonus, actual) -> {
                assertSame(lich, attacker);
                assertEquals(CDamageType.UNIVERSAL, type);
                damageTaken.add(actual);
            });
            game.update();
            lich.add(game, game.getAbilityData().createAbility(War3ID.fromString("AUdd"),
                    game.getHandleIdAllocator().createId()));
            lich.setMana(lich.getMaximumMana());
            assertTrue(lich.order(game, OrderIds.deathanddecay, new AbilityPointTarget(450, 0)));
            for (int tick = 0; tick < 80 && damageTaken.size() < 2; tick++) game.update();
            assertEquals(2, damageTaken.size());
            assertEquals(ally.getMaximumLife() * .42f, ally.getLife(), 0.1f);
            assertEquals(air.getMaximumLife() * .92f, air.getLife(), 0.1f);
            assertEquals(farm.getMaximumLife() * .92f, farm.getLife(), 0.1f);
            assertEquals(treeLife - 2f * tree.getMaxLife() * .04f, tree.getLife(), 0.1f);
            assertEquals(outside.getMaximumLife(), outside.getLife());
            assertTrue(lich.order(game, OrderIds.stop, null));
            for (int tick = 0; tick < 60; tick++) game.update();
            assertEquals(2, damageTaken.size());
        });
    }

    @Test
    void orcSettlementPeonCanDeliverLumberFromTheOriginalBarrensTree() throws Exception {
        withFixture("Maps\\Campaign\\Orc03.w3m", false, true, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit hall = game.createUnitSimple(War3ID.fromString("ogre"), 0, 1792, -6784, 0);
            final CUnit peon = game.createUnitSimple(War3ID.fromString("opeo"), 0, 1792, -6980, 270);
            final CDestructable tree = game.internalCreateDestructable(War3ID.fromString("BTtw"), 1984, -7232, null, null);
            final var player = game.getPlayer(hall.getPlayerIndex());
            final int initialLumber = player.getLumber();
            game.update();
            assertTrue(peon.order(game, OrderIds.harvest, tree));
            for (int tick = 0; tick < 900 && player.getLumber() == initialLumber; tick++) game.update();
            assertTrue(player.getLumber() > initialLumber,
                    () -> "Peon never returned lumber from the native forest. Position " + peon.getX() + ", " + peon.getY()
                            + " and current behavior " + peon.getCurrentBehavior());
        });
    }

    @Test
    void originalEarthquakeSlowsGroundUnitsAndStopsDamageWhenInterrupted() throws Exception {
        withFixture("Maps\\Campaign\\Orc03.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit farSeer = game.createUnitSimple(War3ID.fromString("Ofar"), 0, 0, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 450, 130, 0);
            final CUnit gryphon = game.createUnitSimple(War3ID.fromString("hgry"), 1, 450, -160, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 840, 0, 0);
            final CUnit farm = game.createUnitSimple(War3ID.fromString("hhou"), 1, 525, 0, 0);
            final CDestructable tree = game.internalCreateDestructable(War3ID.fromString("ATtr"), 450, -95, null, null);
            final float treeLife = tree.getLife();
            final int footmanSpeed = footman.getSpeed();
            final int gryphonSpeed = gryphon.getSpeed();
            final int distantSpeed = distant.getSpeed();
            final List<Float> farmDamage = new ArrayList<>();
            farm.addDamageTakenListener((simulation, attacker, victim, isAttack, isRanged, type, amount, bonus, actual) -> {
                assertSame(farSeer, attacker);
                farmDamage.add(amount);
            });
            farSeer.setDisableAttacks(true);
            for (final CUnit unit : List.of(footman, gryphon, distant, farm)) unit.setPaused(true);
            game.update();
            farSeer.add(game, game.getAbilityData().createAbility(War3ID.fromString("AOeq"),
                    game.getHandleIdAllocator().createId()));
            farSeer.setMana(farSeer.getMaximumMana());
            assertTrue(farSeer.order(game, OrderIds.earthquake, new AbilityPointTarget(450, 0)));
            for (int tick = 0; tick < 50 && farmDamage.isEmpty(); tick++) game.update();
            assertEquals(List.of(50f), farmDamage);
            assertEquals(150, footman.getSpeed());
            assertEquals(footman.getMaximumLife(), footman.getLife());
            assertEquals(gryphonSpeed, gryphon.getSpeed());
            assertEquals(distantSpeed, distant.getSpeed());
            assertTrue(tree.getLife() < treeLife);
            for (int tick = 0; tick < 25; tick++) game.update();
            assertEquals(List.of(50f, 50f), farmDamage);
            assertTrue(farSeer.order(game, OrderIds.stop, null));
            assertEquals(footmanSpeed, footman.getSpeed());
            for (int tick = 0; tick < 45; tick++) game.update();
            assertEquals(List.of(50f, 50f), farmDamage);
        });
    }

    @Test
    void originalCritterWandersNearItsStartingPointAndHonorsMoveOrders() throws Exception {
        withFixture("Maps\\Campaign\\Orc04Interlude.w3m", false, (game, projectiles) -> {
            final CUnit pig = game.createUnitSimple(War3ID.fromString("npig"), 0, 0, 0, 0);
            game.update();
            final float startX = pig.getX();
            final float startY = pig.getY();
            for (int tick = 0; tick < 200 && pig.distanceSquaredNoCollision(startX, startY) < 32 * 32; tick++) {
                game.update();
            }
            assertTrue(pig.distanceSquaredNoCollision(startX, startY) >= 32 * 32);
            for (int tick = 0; tick < 300; tick++) {
                game.update();
                assertTrue(pig.distanceSquaredNoCollision(startX, startY) <= 256 * 256);
            }
            final var destination = new AbilityPointTarget(startX + 480, startY);
            assertTrue(pig.order(game, OrderIds.move, destination));
            for (int tick = 0; tick < 220 && pig.distance(destination) > 16; tick++) game.update();
            assertTrue(pig.distance(destination) <= 16);
        });
    }

    @Test
    void originalTornadoMovesAndDamagesBuildingsUntilTheChannelEnds() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit seaWitch = game.createUnitSimple(War3ID.fromString("Nngs"), 0, 0, 0, 0);
            final CUnit near = game.createUnitSimple(War3ID.fromString("hhou"), 1, 350, 60, 0);
            final CUnit far = game.createUnitSimple(War3ID.fromString("hhou"), 1, 700, 60, 0);
            final CUnit friendly = game.createUnitSimple(War3ID.fromString("hhou"), 0, 400, -140, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 540, 300, 0);
            seaWitch.setDisableAttacks(true);
            footman.setDisableAttacks(true);
            final List<Float> closeDamage = new ArrayList<>();
            final List<Float> distantDamage = new ArrayList<>();
            near.addDamageTakenListener((sim, attacker, victim, attack, ranged, type, damage, bonus, actual) -> closeDamage.add(damage));
            far.addDamageTakenListener((sim, attacker, victim, attack, ranged, type, damage, bonus, actual) -> distantDamage.add(damage));
            final int originalSpeed = footman.getSpeed();
            final War3ID tornadoId = War3ID.fromString("ntor");
            seaWitch.add(game, game.getAbilityData().createAbility(War3ID.fromString("ANto"), game.getHandleIdAllocator().createId()));
            seaWitch.setMana(seaWitch.getMaximumMana());
            game.update();
            assertTrue(seaWitch.order(game, OrderIds.tornado, new AbilityPointTarget(250, 50)));
            for (int tick = 0; tick < 60 && closeDamage.isEmpty(); tick++) game.update();
            final CUnit tornado = game.getUnits().stream().filter(unit -> tornadoId.equals(unit.getTypeId()) && !unit.isDead())
                    .findFirst().orElseThrow();
            assertEquals(List.of(50f), closeDamage);
            assertEquals(List.of(7f), distantDamage);
            assertEquals(friendly.getMaximumLife(), friendly.getLife(), 0.001f);
            assertEquals(footman.getMaximumLife(), footman.getLife(), 0.001f);
            assertTrue(footman.getSpeed() < originalSpeed);

            final float startY = tornado.getY();
            assertTrue(tornado.order(game, OrderIds.move, new AbilityPointTarget(250, -310)));
            for (int tick = 0; tick < 25; tick++) game.update();
            assertTrue(tornado.getY() < startY - 20);
            final CUnit eligible = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 130, -230, 0);
            eligible.setDisableAttacks(true);
            final float groundHeight = eligible.getFlyHeight();
            final int manualOrderBudget = (int) Math.ceil(7 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < manualOrderBudget
                    && tornado.distanceSquaredNoCollision(250, -310) > 52 * 52; tick++) game.update();
            assertTrue(tornado.distanceSquaredNoCollision(250, -310) <= 52 * 52,
                    "wandering must preserve an active player move");
            assertTrue(eligible.getFlyHeight() > groundHeight);
            assertTrue(eligible.isInvulnerable());
            final float idleX = tornado.getX();
            final float idleY = tornado.getY();
            for (int tick = 0; tick < 90; tick++) game.update();
            assertTrue(tornado.distanceSquaredNoCollision(idleX, idleY) > 16 * 16, "the idle original Tornado wanders");
            assertTrue(seaWitch.order(game, OrderIds.stop, null));
            assertTrue(tornado.isDead(), "interrupting the channel removes the original summon");
            final int pulses = distantDamage.size();
            for (int tick = 0; tick < 45; tick++) game.update();
            assertEquals(pulses, distantDamage.size());
            assertEquals(originalSpeed, footman.getSpeed());
            for (int tick = 0; tick < manualOrderBudget * 2; tick++) game.update();
            assertEquals(groundHeight, eligible.getFlyHeight(), 0.001f);
            assertFalse(eligible.isInvulnerable());
        });
    }

    @Test
    void originalChaosGruntBerserkPreservesMovementAndExpiresBeforeCooldown() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            final CUnit grunt = game.createUnitSimple(War3ID.fromString("nchg"), 0, 0, 0, 0);
            final CUnit opponent = game.createUnitSimple(War3ID.fromString("earc"), 1, -200, 0, 0);
            opponent.setDisableAttacks(true);
            game.update();
            final int speed = grunt.getSpeed();
            final float attackCooldown = grunt.getCurrentAttacks().get(0).getCooldownTime();
            final float normalDamage = grunt.damage(game, opponent, false, false, CAttackType.SPELLS,
                    CDamageType.UNIVERSAL, null, 30);
            assertTrue(grunt.order(game, OrderIds.move, new AbilityPointTarget(320, 0)));
            assertTrue(grunt.order(game, OrderIds.berserk, null));
            assertEquals(OrderIds.move, grunt.getCurrentOrder().getOrderId());
            assertEquals(Math.round(speed * 1.2f), grunt.getSpeed());
            assertEquals(attackCooldown / 1.5f, grunt.getCurrentAttacks().get(0).getCooldownTime(), 0.001f);
            assertEquals(normalDamage * 1.5f, grunt.damage(game, opponent, false, false, CAttackType.SPELLS,
                    CDamageType.UNIVERSAL, null, 30), 0.001f);
            assertFalse(grunt.order(game, OrderIds.berserk, null));
            for (int tick = 0; tick < (int) Math.ceil(12.1f / WarsmashConstants.SIMULATION_STEP_TIME); tick++) game.update();
            assertEquals(speed, grunt.getSpeed());
            assertEquals(attackCooldown, grunt.getCurrentAttacks().get(0).getCooldownTime(), 0.001f);
            assertEquals(normalDamage, grunt.damage(game, opponent, false, false, CAttackType.SPELLS,
                    CDamageType.UNIVERSAL, null, 30), 0.001f);
            assertTrue(grunt.getCooldownRemainingTicks(game, War3ID.fromString("Absk")) > 0);
        });
    }

    @Test
    void originalKodoDevourDigestsOneEnemyAndReleasesItWhenRemovedOrKilled() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID kodoId = War3ID.fromString("okod");
            final War3ID footmanId = War3ID.fromString("hfoo");
            final CUnit kodo = game.createUnitSimple(kodoId, 0, 0, 0, 0);
            final CUnit victim = game.createUnitSimple(footmanId, 1, 104, 0, 0);
            final CUnit other = game.createUnitSimple(footmanId, 1, 112, 80, 0);
            final CUnit ally = game.createUnitSimple(footmanId, 0, 112, -80, 0);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 224, 0, 0);
            for (final CUnit unit : List.of(kodo, victim, other, ally, hero)) unit.setDisableAttacks(true);
            victim.setLifeRegenBonus(-victim.getUnitType().getLifeRegen());
            game.update();
            assertFalse(kodo.order(game, OrderIds.devour, ally));
            assertFalse(kodo.order(game, OrderIds.devour, hero));
            assertTrue(kodo.order(game, OrderIds.devour, victim));
            for (int tick = 0; tick < 30 && !victim.isHidden(); tick++) game.update();
            assertTrue(victim.isHidden() && victim.isPaused());
            final var cargo = kodo.getCargoData();
            assertSame(victim, cargo.getUnit(0));
            assertFalse(kodo.order(game, OrderIds.devour, other));
            assertFalse(kodo.order(game, OrderIds.unload, victim));
            final float originalLife = victim.getLife();
            for (int tick = 0; tick < (int) Math.ceil(1 / WarsmashConstants.SIMULATION_STEP_TIME); tick++) game.update();
            assertEquals(originalLife - 5, victim.getLife(), 0.01f);
            kodo.kill(game);
            assertTrue(cargo.isEmpty());
            assertFalse(victim.isHidden() || victim.isPaused() || victim.isDead());
            assertTrue(victim.distanceSquaredNoCollision(kodo) < 192 * 192);

            final CUnit secondKodo = game.createUnitSimple(kodoId, 0, 0, 160, 0);
            secondKodo.setDisableAttacks(true);
            game.update();
            assertTrue(secondKodo.order(game, OrderIds.devour, other));
            for (int tick = 0; tick < 30 && !other.isHidden(); tick++) game.update();
            assertTrue(other.isHidden());
            game.removeUnit(secondKodo);
            game.update();
            assertFalse(other.isHidden() || other.isPaused() || other.isDead());

            final CUnit thirdKodo = game.createUnitSimple(kodoId, 0, 0, 320, 0);
            final CUnit digested = game.createUnitSimple(footmanId, 1, 100, 320, 0);
            thirdKodo.setDisableAttacks(true);
            digested.setDisableAttacks(true);
            digested.setLifeRegenBonus(-digested.getUnitType().getLifeRegen());
            digested.setLife(game, 6);
            game.update();
            assertTrue(thirdKodo.order(game, OrderIds.devour, digested));
            for (int tick = 0; tick < 90 && !digested.isDead(); tick++) game.update();
            assertTrue(digested.isDead());
            assertTrue(thirdKodo.getCargoData().isEmpty());
        });
    }

    @Test
    void rexxarCampaignHealingWaveBouncesToThreeEligibleAllies() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit hunter = game.createUnitSimple(War3ID.fromString("Oshd"), 0, -700, 0, 0);
            final CUnit first = game.createUnitSimple(footman, 0, -350, 0, 0);
            final CUnit second = game.createUnitSimple(footman, 0, 50, 0, 0);
            final CUnit third = game.createUnitSimple(footman, 0, 450, 0, 0);
            final CUnit fourth = game.createUnitSimple(footman, 0, 850, 0, 0);
            final CUnit enemy = game.createUnitSimple(footman, 1, -230, 120, 0);
            final CUnit machine = game.createUnitSimple(War3ID.fromString("hmtt"), 0, -210, -120, 0);
            final CUnit atFullHealth = game.createUnitSimple(footman, 0, -220, 0, 0);
            for (final CUnit unit : List.of(first, second, third, fourth, enemy, machine)) {
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
                unit.setLife(game, 100);
                unit.setPaused(true);
            }
            atFullHealth.setPaused(true);
            hunter.setDisableAttacks(true);
            game.update();
            final War3ID wave = War3ID.fromString("ANhw");
            final var ability = game.getAbilityData().createAbility(wave, game.getHandleIdAllocator().createId());
            assertNotNull(ability);
            hunter.add(game, ability);
            hunter.setMana(hunter.getMaximumMana());
            assertFalse(hunter.order(game, OrderIds.healingwave, enemy));
            assertFalse(hunter.order(game, OrderIds.healingwave, machine));
            assertTrue(hunter.order(game, OrderIds.healingwave, first));
            for (int tick = 0; tick < 70; tick++) game.update();
            assertEquals(230f, first.getLife(), 0.1f);
            assertEquals(197.5f, second.getLife(), 0.1f);
            assertEquals(173.125f, third.getLife(), 0.1f);
            assertEquals(100f, fourth.getLife(), 0.1f);
            assertEquals(100f, enemy.getLife(), 0.1f);
            assertEquals(100f, machine.getLife(), 0.1f);
        });
    }

    @Test
    void originalForkedLightningHitsItsTargetAndTwoEnemiesInTheCone() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX02.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID footman = War3ID.fromString("hfoo");
            final var seaWitch = game.createUnitSimple(War3ID.fromString("Nngs"), 0, 0, 0, 0);
            final var primary = game.createUnitSimple(footman, 1, 500, 0, 0);
            final var insideGround = game.createUnitSimple(footman, 1, 320, 106, 0);
            final var insideAir = game.createUnitSimple(War3ID.fromString("hgry"), 1, 390, -105, 0);
            final var fourth = game.createUnitSimple(footman, 1, 650, 112, 0);
            final var outside = game.createUnitSimple(footman, 1, 300, 300, 0);
            final var behind = game.createUnitSimple(footman, 1, -220, 0, 0);
            final var ally = game.createUnitSimple(footman, 0, 260, -96, 0);
            seaWitch.setDisableAttacks(true);
            final List<CUnit> struck = new ArrayList<>();
            for (final CUnit unit : List.of(primary, insideGround, insideAir, fourth, outside, behind, ally)) {
                unit.setPaused(true);
                unit.addDamageTakenListener((simulation, attacker, victim, isAttack, isRanged, type, damage, bonus, actual) -> {
                    assertSame(seaWitch, attacker);
                    assertEquals(CDamageType.LIGHTNING, type);
                    assertEquals(100, damage);
                    struck.add(victim);
                });
            }
            final War3ID spell = War3ID.fromString("ANfl");
            game.update();
            assertTrue(seaWitch.order(game, spell.getValue(), null));
            assertEquals(1, seaWitch.getAbilityLevel(spell));
            seaWitch.setMana(seaWitch.getMaximumMana());
            assertFalse(seaWitch.order(game, OrderIds.forkedlightning, ally));
            assertTrue(seaWitch.order(game, OrderIds.forkedlightning, primary));
            for (int tick = 0; tick < 60 && struck.size() < 3; tick++) game.update();
            assertEquals(List.of(primary, insideGround, insideAir), struck);
        });
    }

    @Test
    void originalEntanglingRootsDisarmsAndDamagesGroundHeroesForTheAuthoredDuration() throws Exception {
        withFixture("Maps\\Campaign\\NightElf03.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var keeper = game.createUnitSimple(War3ID.fromString("Ekee"), 0, 0, 0, 0);
            final var hero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 360, 0, 180);
            final var air = game.createUnitSimple(War3ID.fromString("hgry"), 1, 280, 300, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 280, -300, 0);
            keeper.setDisableAttacks(true);
            air.setDisableAttacks(true);
            ally.setDisableAttacks(true);
            final List<Float> damageTaken = new ArrayList<>();
            hero.addDamageTakenListener((simulation, attacker, victim, isAttack, isRanged, type, damage, bonus, actual) -> {
                assertSame(keeper, attacker);
                assertEquals(CDamageType.PLANT, type);
                damageTaken.add(damage);
            });
            game.update();
            assertTrue(keeper.order(game, War3ID.fromString("AEer").getValue(), null));
            keeper.setMana(keeper.getMaximumMana());
            assertFalse(keeper.order(game, OrderIds.entanglingroots, ally));
            assertFalse(keeper.order(game, OrderIds.entanglingroots, air));
            assertTrue(keeper.order(game, OrderIds.entanglingroots, hero));
            for (int tick = 0; tick < 48 && !hero.isMovementDisabled(); tick++) game.update();
            assertTrue(hero.isMovementDisabled());
            assertTrue(hero.getCurrentAttacks().isEmpty());
            assertTrue(damageTaken.isEmpty());
            final float rootedX = hero.getX();
            final float rootedY = hero.getY();
            for (int tick = 0; tick < 35; tick++) game.update();
            assertEquals(rootedX, hero.getX());
            assertEquals(rootedY, hero.getY());
            assertEquals(List.of(15.0f), damageTaken);
            for (int tick = 0; tick < 30; tick++) game.update();
            assertFalse(hero.isMovementDisabled());
            assertFalse(hero.getCurrentAttacks().isEmpty());
            assertEquals(List.of(15.0f, 15.0f, 15.0f), damageTaken);
        });
    }

    @Test
    void originalPriestDispelRemovesRootsAndDamagesOnlyEnemySummonsInsideTheArea() throws Exception {
        withFixture("Maps\\Campaign\\Human04.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var priest = game.createUnitSimple(War3ID.fromString("hmpr"), 0, 0, 0, 0);
            final var keeper = game.createUnitSimple(War3ID.fromString("Ekee"), 1, 0, 600, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 200, 260, 0);
            final var summon = game.createUnitSimple(War3ID.fromString("hwat"), 1, 110, 210, 0);
            final var friendlySummon = game.createUnitSimple(War3ID.fromString("hwat"), 0, 250, 210, 0);
            final var distantSummon = game.createUnitSimple(War3ID.fromString("hwat"), 1, 530, 260, 0);
            final var ordinaryEnemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 290, 260, 0);
            for (final CUnit unit : List.of(priest, keeper, ally, summon, friendlySummon, distantSummon, ordinaryEnemy)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            for (final CUnit unit : List.of(summon, friendlySummon, distantSummon)) {
                unit.addUnitType(game, CUnitTypeJass.SUMMONED);
            }
            game.update();
            final var point = new AbilityPointTarget(200, 260);
            priest.setMana(priest.getMaximumMana());
            assertFalse(priest.order(game, OrderIds.dispel, point));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhpt"), 1);
            game.update();
            assertTrue(keeper.order(game, War3ID.fromString("AEer").getValue(), null));
            keeper.setMana(keeper.getMaximumMana());
            assertTrue(keeper.order(game, OrderIds.entanglingroots, ally));
            for (int tick = 0; tick < 48 && !ally.isMovementDisabled(); tick++) game.update();
            assertTrue(ally.isMovementDisabled());
            final float summonLife = summon.getLife();
            assertTrue(priest.order(game, OrderIds.dispel, point));
            for (int tick = 0; tick < 20 && ally.isMovementDisabled(); tick++) game.update();
            assertFalse(ally.isMovementDisabled());
            assertEquals(200, summonLife - summon.getLife(), 0.001f);
            assertEquals(friendlySummon.getMaximumLife(), friendlySummon.getLife());
            assertEquals(distantSummon.getMaximumLife(), distantSummon.getLife());
            assertEquals(ordinaryEnemy.getMaximumLife(), ordinaryEnemy.getLife());
        });
    }

    @Test
    void originalZeppelinCountsTheAuthoredCargoSizeOfEveryPassenger() throws Exception {
        withFixture("Maps\\Campaign\\Orc03.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit transport = game.createUnitSimple(War3ID.fromString("nzep"), 0, 0, 0, 0);
            final var cargo = transport.getCargoData();
            assertNotNull(cargo);
            assertEquals(8, cargo.getCargoCapacity());
            CUnit firstCatapult = null;
            for (int index = 0; index < 4; index++) {
                final CUnit catapult = game.createUnitSimple(War3ID.fromString("ocat"), 0, 80, 40 * index, 0);
                if (firstCatapult == null) firstCatapult = catapult;
                assertEquals(2, catapult.getUnitType().getCargoCapacity());
                game.update();
                assertTrue(transport.order(game, OrderIds.load, catapult));
                for (int tick = 0; tick < 60 && !catapult.isHidden(); tick++) game.update();
                assertTrue(catapult.isHidden());
                assertSame(transport, catapult.getTransport());
            }
            assertEquals(4, cargo.getCargoCount());
            assertFalse(cargo.hasCapacity(1));
            assertTrue(transport.order(game, OrderIds.unload, firstCatapult));
            game.update();
            assertFalse(firstCatapult.isHidden());
            assertNull(firstCatapult.getTransport());
            assertTrue(cargo.hasCapacity(2));
            assertFalse(cargo.hasCapacity(3));
            final CUnit removedPassenger = cargo.getUnit(0);
            game.removeUnit(removedPassenger);
            assertNull(removedPassenger.getTransport());
            assertEquals(2, cargo.getCargoCount());
            final CUnit killedPassenger = cargo.getUnit(0);
            killedPassenger.kill(game);
            assertNull(killedPassenger.getTransport());
            assertEquals(1, cargo.getCargoCount());
        });
    }

    @Test
    void originalZeppelinCrashDazesSurvivorsOnLandAndLosesCargoOverDeepWater() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX02.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID zeppelin = War3ID.fromString("nzep");
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit carrier = game.createUnitSimple(zeppelin, 0, -720, -560, 0);
            final CUnit soldier = game.createUnitSimple(footman, 0, -656, -560, 0);
            final CUnit warden = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, -656, -640, 0);
            final War3ID dizzy = War3ID.fromString("Bchd");
            game.update();
            final int soldierSpeed = soldier.getSpeed();
            final int heroSpeed = warden.getSpeed();
            final var soldierAttack = soldier.getUnitSpecificAttacks().get(0);
            final float attackSpeed = soldierAttack.getAttackSpeedModifier();
            for (final CUnit passenger : List.of(soldier, warden)) {
                assertTrue(carrier.order(game, OrderIds.load, passenger));
                for (int tick = 0; tick < 45 && passenger.getTransport() == null; tick++) game.update();
                assertSame(carrier, passenger.getTransport());
            }
            carrier.kill(game);
            for (final CUnit survivor : List.of(soldier, warden)) {
                assertFalse(survivor.isDead() || survivor.isHidden() || survivor.isPaused());
                assertNull(survivor.getTransport());
                final CBuff debuff = survivor.getAbilities().stream().filter(a -> dizzy.equals(a.getAlias()))
                        .map(CBuff.class::cast).findFirst().orElseThrow();
                assertEquals(survivor.isHero() ? 30 : 60, debuff.getDurationMax());
                assertFalse(debuff.isDispellable());
            }
            assertTrue(soldier.getSpeed() < soldierSpeed && warden.getSpeed() < heroSpeed);
            assertEquals(attackSpeed - .9f, soldierAttack.getAttackSpeedModifier(), .001f);
            for (int tick = 0; tick < 610; tick++) game.update();
            assertEquals(heroSpeed, warden.getSpeed());
            assertTrue(soldier.getSpeed() < soldierSpeed);
            for (int tick = 0; tick < 610; tick++) game.update();
            assertEquals(soldierSpeed, soldier.getSpeed());
            assertEquals(attackSpeed, soldierAttack.getAttackSpeedModifier(), .001f);

            final CUnit lostCarrier = game.createUnitSimple(zeppelin, 0, -720, -560, 0);
            final CUnit lostSoldier = game.createUnitSimple(footman, 0, -590, -560, 0);
            game.update();
            assertTrue(lostCarrier.order(game, OrderIds.load, lostSoldier));
            for (int tick = 0; tick < 45 && lostSoldier.getTransport() == null; tick++) game.update();
            assertSame(lostCarrier, lostSoldier.getTransport());
            lostCarrier.relocate(-1200, -8112, game);
            assertNull(lostSoldier.findUnstuckPoint(lostCarrier.getX(), lostCarrier.getY(), game));
            lostCarrier.kill(game);
            assertTrue(lostSoldier.isDead());
            assertNull(lostSoldier.getTransport());
        });
    }

    @Test
    void originalNightElfTransportKeepsPassengersAboardUntilALandingIsAvailable() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX02.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var ship = game.createUnitSimple(War3ID.fromString("etrs"), 0, -720, -560, 0);
            final var warden = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, -656, -560, 0);
            game.update();
            assertTrue(ship.order(game, OrderIds.load, warden));
            for (int tick = 0; tick < 50 && !warden.isHidden(); tick++) game.update();
            assertTrue(warden.isHidden());
            final var deepWater = new AbilityPointTarget(-1200, -8112);
            ship.relocate(deepWater.getX(), deepWater.getY(), game);
            assertNull(warden.findUnstuckPoint(ship.getX(), ship.getY(), game));
            assertTrue(ship.order(game, OrderIds.unloadall, deepWater));
            for (int tick = 0; tick < 50; tick++) game.update();
            assertTrue(warden.isHidden());
            assertSame(ship, warden.getTransport());
            assertEquals(1, ship.getCargoData().getCargoCount());

            final var landing = new AbilityPointTarget(-640, -560);
            ship.relocate(-720, -560, game);
            assertTrue(ship.order(game, OrderIds.unloadall, landing));
            for (int tick = 0; tick < 50 && warden.isHidden(); tick++) game.update();
            assertFalse(warden.isHidden());
            assertNull(warden.getTransport());
            assertTrue(ship.getCargoData().isEmpty());
            assertEquals(0, warden.distanceSquaredNoCollision(landing.getX(), landing.getY()), 0.001);
        });
    }

    @Test
    void originalShamanPurgeCleansesAlliesAndTemporarilySlowsAndDamagesEnemies() throws Exception {
        withFixture("Maps\\Campaign\\Orc04.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var shaman = game.createUnitSimple(War3ID.fromString("oshm"), 0, 0, 0, 0);
            final var keeper = game.createUnitSimple(War3ID.fromString("Ekee"), 1, 0, 600, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("ogru"), 0, 100, 300, 0);
            final var summon = game.createUnitSimple(War3ID.fromString("hwat"), 1, -150, 200, 0);
            for (final CUnit unit : List.of(shaman, keeper, ally, summon)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            summon.addUnitType(game, CUnitTypeJass.SUMMONED);
            game.update();
            final int allySpeed = ally.getSpeed();
            final int summonSpeed = summon.getSpeed();
            final float summonLife = summon.getLife();
            assertTrue(keeper.order(game, War3ID.fromString("AEer").getValue(), null));
            keeper.setMana(keeper.getMaximumMana());
            assertTrue(keeper.order(game, OrderIds.entanglingroots, ally));
            for (int tick = 0; tick < 48 && !ally.isMovementDisabled(); tick++) game.update();
            assertTrue(ally.isMovementDisabled());
            shaman.setMana(shaman.getMaximumMana());
            assertTrue(shaman.order(game, OrderIds.purge, ally));
            for (int tick = 0; tick < 30 && ally.isMovementDisabled(); tick++) game.update();
            assertFalse(ally.isMovementDisabled());
            assertEquals(allySpeed, ally.getSpeed());
            for (int tick = 0; tick < 32; tick++) game.update();
            assertTrue(shaman.order(game, OrderIds.purge, summon));
            for (int tick = 0; tick < 30 && summon.getLife() == summonLife; tick++) game.update();
            assertEquals(400, summonLife - summon.getLife(), 0.001f);
            assertTrue(summon.getSpeed() < summonSpeed / 3);
            summon.dispelBuffs(game);
            assertTrue(summon.getSpeed() < summonSpeed / 3);
            for (int tick = 0; tick < 140; tick++) game.update();
            assertTrue(summon.getSpeed() > summonSpeed / 3 && summon.getSpeed() < summonSpeed);
            for (int tick = 0; tick < 420; tick++) game.update();
            assertEquals(summonSpeed, summon.getSpeed());
        });
    }

    @Test
    void originalLightningShieldRequiresTrainingAndDamagesNearbyGroundUnitsExceptBearer() throws Exception {
        withFixture("Maps\\Campaign\\Orc04.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var shaman = game.createUnitSimple(War3ID.fromString("oshm"), 0, 0, 0, 0);
            final var bearer = game.createUnitSimple(War3ID.fromString("ogru"), 1, 400, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("ogru"), 0, 400, 100, 0);
            final var enemy = game.createUnitSimple(War3ID.fromString("ogru"), 1, 500, 0, 0);
            final var aircraft = game.createUnitSimple(War3ID.fromString("hgyr"), 1, 400, -100, 0);
            final var distant = game.createUnitSimple(War3ID.fromString("ogru"), 1, 650, 0, 0);
            for (final CUnit unit : List.of(shaman, bearer, ally, enemy, aircraft, distant)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            game.update();
            shaman.setMana(shaman.getMaximumMana());
            assertFalse(shaman.order(game, OrderIds.lightningshield, bearer));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rost"), 1);
            game.update();
            assertTrue(shaman.order(game, OrderIds.lightningshield, bearer));
            for (int tick = 0; tick < 70 && ally.getLife() == ally.getMaximumLife(); tick++) game.update();
            assertEquals(20, ally.getMaximumLife() - ally.getLife(), 0.001f);
            assertEquals(20, enemy.getMaximumLife() - enemy.getLife(), 0.001f);
            assertEquals(bearer.getMaximumLife(), bearer.getLife());
            assertEquals(aircraft.getMaximumLife(), aircraft.getLife());
            assertEquals(distant.getMaximumLife(), distant.getLife());
            bearer.dispelBuffs(game);
            for (int tick = 0; tick < 64; tick++) game.update();
            assertEquals(20, ally.getMaximumLife() - ally.getLife(), 0.001f);
        });
    }

    @Test
    void originalSentryWardRevealsInvisibleUnitsForItsOwnerWhileRemainingHidden() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            final var doctor = game.createUnitSimple(War3ID.fromString("odoc"), 0, 0, 0, 0);
            final var shade = game.createUnitSimple(War3ID.fromString("ushd"), 1, 850, 0, 0);
            final var scout = game.createUnitSimple(War3ID.fromString("ogru"), 2, 450, 150, 0);
            doctor.setDisableAttacks(true);
            scout.setDisableAttacks(true);
            game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
            doctor.setMana(doctor.getMaximumMana());
            assertTrue(doctor.order(game, OrderIds.evileye, new AbilityPointTarget(300, 0)));
            final War3ID wardType = War3ID.fromString("oeye");
            for (int tick = 0; tick < 70 && !shade.isVisible(game, 0); tick++) game.update();
            final CUnit ward = game.getUnits().stream().filter(unit -> wardType.equals(unit.getTypeId()))
                    .findFirst().orElseThrow();
            assertEquals(0, ward.getPlayerIndex());
            assertTrue(ward.isMovementDisabled());
            assertTrue(ward.isInvisibleTo(game, 2));
            assertTrue(shade.isVisible(game, 0));
            assertTrue(shade.isInvisibleTo(game, 2));
            final var lifetime = ward.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife.class);
            assertNotNull(lifetime);
            assertEquals(600, lifetime.getDurationMax());
            ward.kill(game);
            for (int tick = 0; tick < 40; tick++) game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
        });
    }

    @Test
    void originalHealingWardRequiresMasterTrainingAndDoesNotStackItsHealing() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            final var doctor = game.createUnitSimple(War3ID.fromString("odoc"), 0, 0, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("earc"), 0, 270, 100, 0);
            final var mechanical = game.createUnitSimple(War3ID.fromString("ocat"), 0, 360, 120, 0);
            final var enemy = game.createUnitSimple(War3ID.fromString("ogru"), 1, 340, -100, 0);
            for (final CUnit unit : List.of(doctor, ally, mechanical, enemy)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            game.update();
            final var point = new AbilityPointTarget(320, 0);
            doctor.setMana(doctor.getMaximumMana());
            final var training = War3ID.fromString("Rowd");
            game.getPlayer(0).setTechResearched(game, training, 1);
            game.update();
            assertFalse(doctor.order(game, OrderIds.healingward, point));
            game.getPlayer(0).setTechResearched(game, training, 2);
            game.update();
            doctor.setMana(doctor.getMaximumMana());
            assertTrue(doctor.order(game, OrderIds.healingward, point));
            final War3ID wardType = War3ID.fromString("ohwd");
            for (int tick = 0; tick < 32 && game.getUnits().stream().noneMatch(unit -> wardType.equals(unit.getTypeId())); tick++) {
                game.update();
            }
            doctor.setMana(doctor.getMaximumMana());
            assertTrue(doctor.order(game, OrderIds.healingward, new AbilityPointTarget(440, 0)));
            for (int tick = 0; tick < 32 && game.getUnits().stream().filter(unit -> wardType.equals(unit.getTypeId())).count() < 2; tick++) {
                game.update();
            }
            final var wards = game.getUnits().stream().filter(unit -> wardType.equals(unit.getTypeId())).toList();
            assertEquals(2, wards.size());
            assertTrue(mechanical.getClassifications().contains(CUnitClassification.MECHANICAL));
            ally.setLife(game, ally.getMaximumLife() / 2);
            mechanical.setLife(game, mechanical.getMaximumLife() / 2);
            enemy.setLife(game, enemy.getMaximumLife() / 2);
            for (int tick = 0; tick < Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME); tick++) game.update();
            assertEquals(ally.getMaximumLife() * 0.52f, ally.getLife(), 0.001f);
            assertEquals(mechanical.getMaximumLife() / 2, mechanical.getLife());
            assertEquals(enemy.getMaximumLife() / 2, enemy.getLife());
            wards.forEach(ward -> ward.kill(game));
            for (int tick = 0; tick < 100; tick++) game.update();
            assertEquals(ally.getMaximumLife() * 0.52f, ally.getLife(), 0.001f);

            final CUnit priestess = game.createUnitSimple(War3ID.fromString("Emoo"), 2, 0, -240, 0);
            priestess.setDisableAttacks(true);
            final CItem wardsItem = game.createItem(War3ID.fromString("whwd"), 0, -240);
            final int charges = wardsItem.getCharges();
            final int slot = priestess.getInventoryData().giveItem(game, priestess, wardsItem, 0, false);
            assertEquals(0, slot);
            assertEquals(0, game.getPlayer(2).getTechtreeUnlocked(training));
            game.update();
            assertTrue(priestess.order(game, OrderIds.itemuse00 + slot, new AbilityPointTarget(310, -240)));
            for (int tick = 0; tick < 32 && game.getUnits().stream().noneMatch(ward -> wardType.equals(ward.getTypeId())
                    && ward.getPlayerIndex() == 2 && !ward.isDead()); tick++) game.update();
            assertEquals(charges - 1, wardsItem.getCharges());
            assertTrue(game.getUnits().stream().anyMatch(ward -> wardType.equals(ward.getTypeId())
                    && ward.getPlayerIndex() == 2 && !ward.isDead()));
        });
    }

    @Test
    void originalStasisTrapArmsBeforeStunningHostileGroundUnits() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            final var doctor = game.createUnitSimple(War3ID.fromString("odoc"), 0, 0, 0, 0);
            final var ground = game.createUnitSimple(War3ID.fromString("ogru"), 1, 440, 0, 0);
            final var hero = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 645, 0, 0);
            final var flyer = game.createUnitSimple(War3ID.fromString("hgyr"), 1, 320, -120, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("ogru"), 0, 320, 140, 0);
            for (final CUnit unit : List.of(doctor, ground, hero, flyer, ally)) unit.setDisableAttacks(true);
            game.update();
            final var point = new AbilityPointTarget(300, 0);
            doctor.setMana(doctor.getMaximumMana());
            assertFalse(doctor.order(game, OrderIds.stasistrap, point));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rowd"), 1);
            game.update();
            doctor.setMana(doctor.getMaximumMana());
            assertTrue(doctor.order(game, OrderIds.stasistrap, point));
            final War3ID totemType = War3ID.fromString("otot");
            for (int tick = 0; tick < 32 && game.getUnits().stream().noneMatch(unit -> totemType.equals(unit.getTypeId())); tick++) {
                game.update();
            }
            final CUnit trap = game.getUnits().stream().filter(unit -> totemType.equals(unit.getTypeId())).findFirst().orElseThrow();
            assertFalse(trap.isInvisibleTo(game, 1));
            final int second = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < 9 * second; tick++) game.update();
            assertFalse(ground.isUnitType(CUnitTypeJass.STUNNED));
            for (int tick = 0; tick < 2 * second && !trap.isDead(); tick++) game.update();
            assertTrue(trap.isDead());
            assertTrue(ground.isUnitType(CUnitTypeJass.STUNNED));
            assertTrue(hero.isUnitType(CUnitTypeJass.STUNNED));
            assertFalse(flyer.isUnitType(CUnitTypeJass.STUNNED));
            assertFalse(ally.isUnitType(CUnitTypeJass.STUNNED));
            for (int tick = 0; tick < 3 * second; tick++) game.update();
            assertFalse(hero.isUnitType(CUnitTypeJass.STUNNED));
            assertTrue(ground.isUnitType(CUnitTypeJass.STUNNED));
            for (int tick = 0; tick < 4 * second; tick++) game.update();
            assertFalse(ground.isUnitType(CUnitTypeJass.STUNNED));
        });
    }

    @Test
    void originalWispDetonateMovesIntoRangeAndConsumesItWhileDrainingBothSides() throws Exception {
        withFixture("Maps\\Campaign\\NightElf03.w3m", false, (game, projectiles) -> {
            final var wisp = game.createUnitSimple(War3ID.fromString("ewsp"), 0, 0, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("Ekee"), 0, 410, -140, 0);
            final var enemy = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 450, 120, 0);
            final var distant = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 850, 0, 0);
            final var summoned = game.createUnitSimple(War3ID.fromString("hwat"), 1, 480, 0, 0);
            for (final CUnit unit : List.of(ally, enemy, distant, summoned)) {
                unit.setDisableAttacks(true);
                unit.setManaRegen(0);
                unit.setManaRegenIntelligenceBonus(0);
                unit.setMana(unit.getMaximumMana());
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            summoned.addUnitType(game, CUnitTypeJass.SUMMONED);
            game.update();
            assertTrue(wisp.order(game, OrderIds.detonate, new AbilityPointTarget(480, 0)));
            game.update();
            assertFalse(wisp.isDead());
            for (int tick = 0; tick < 80 && !wisp.isDead(); tick++) game.update();
            assertTrue(wisp.isDead());
            assertTrue(wisp.getX() > 220);
            assertEquals(100, ally.getMaximumMana() - ally.getMana(), 0.001f);
            assertEquals(100, enemy.getMaximumMana() - enemy.getMana(), 0.001f);
            assertEquals(distant.getMaximumMana(), distant.getMana(), 0.001f);
            assertEquals(225, summoned.getMaximumLife() - summoned.getLife(), 0.001f);
        });
    }

    @Test
    void originalThornsReflectsAuthoredMeleeDamageAndExpiresWithItsAuraSource() throws Exception {
        withFixture("Maps\\Campaign\\NightElf04.w3m", false, (game, projectiles) -> {
            final var furion = game.createUnitSimple(War3ID.fromString("Efur"), 0, 0, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 300, 0, 0);
            final var targetOutside = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 1200, 0, 0);
            final var enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 400, 0, 0);
            for (final CUnit unit : List.of(furion, ally, targetOutside, enemy)) unit.setDisableAttacks(true);
            final List<Float> returned = new ArrayList<>();
            enemy.addDamageTakenListener((simulation, attacker, victim, attack, ranged, type, damage, bonus, actual) -> {
                assertEquals(CDamageType.DEFENSIVE, type);
                assertSame(ally, attacker);
                assertFalse(attack);
                returned.add(damage);
            });
            game.update();
            assertTrue(furion.order(game, War3ID.fromString("AEah").getValue(), null));
            final int auraTicks = Math.round(2 / WarsmashConstants.SIMULATION_STEP_TIME) + 2;
            for (int tick = 0; tick < auraTicks; tick++) game.update();
            targetOutside.damage(game, enemy, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 50);
            ally.damage(game, enemy, true, true, CAttackType.NORMAL, CDamageType.NORMAL, null, 50);
            ally.damage(game, enemy, false, false, CAttackType.SPELLS, CDamageType.MAGIC, null, 50);
            assertTrue(returned.isEmpty());
            final float taken = ally.damage(game, enemy, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 50);
            assertEquals(1, returned.size());
            assertEquals(taken * 0.10f, returned.get(0), 0.001f);
            furion.kill(game);
            for (int tick = 0; tick < auraTicks; tick++) game.update();
            ally.damage(game, enemy, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 50);
            assertEquals(1, returned.size());
        });
    }

    @Test
    void originalGraveyardProducesUsableGhoulCorpsesOnItsAuthoredSchedule() throws Exception {
        withFixture("Maps\\Campaign\\Undead05.w3m", false, (game, projectiles) -> {
            final var graveyard = game.createUnitSimple(War3ID.fromString("ugrv"), 0, 0, 0, 0);
            final var necromancer = game.createUnitSimple(War3ID.fromString("unec"), 0, -360, 0, 0);
            final var ghoul = War3ID.fromString("ugho");
            final var skeleton = War3ID.fromString("uske");
            final int food = game.getPlayer(0).getFoodUsed();
            final int second = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < 14 * second; tick++) game.update();
            assertFalse(game.getUnits().stream().anyMatch(unit -> ghoul.equals(unit.getTypeId())));
            for (int tick = 0; tick < 4 * second; tick++) game.update();
            final var first = game.getUnits().stream().filter(unit -> ghoul.equals(unit.getTypeId())).findFirst().orElseThrow();
            assertTrue(first.isCorpse());
            assertTrue(first.isRaisable());
            assertTrue(graveyard.distanceSquaredNoCollision(first) <= 250 * 250);
            assertEquals(food, game.getPlayer(0).getFoodUsed());
            for (int tick = 0; tick < 74 * second; tick++) game.update();
            assertEquals(5, game.getUnits().stream().filter(unit -> ghoul.equals(unit.getTypeId()) && unit.isCorpse()).count());
            necromancer.setMana(necromancer.getMaximumMana());
            assertTrue(necromancer.order(game, OrderIds.raisedead, null));
            for (int tick = 0; tick < 40 && game.getUnits().stream().noneMatch(unit -> skeleton.equals(unit.getTypeId())); tick++) {
                game.update();
            }
            assertEquals(2, game.getUnits().stream().filter(unit -> skeleton.equals(unit.getTypeId()) && !unit.isDead()).count());
        });
    }

    @Test
    void originalNecromancerTrainingUnlocksFrenzyAndCrippleAtTheirAuthoredTiers() throws Exception {
        withFixture("Maps\\Campaign\\Undead05.w3m", false, (game, projectiles) -> {
            final var necromancer = game.createUnitSimple(War3ID.fromString("unec"), 0, 0, 0, 0);
            final var ghoul = game.createUnitSimple(War3ID.fromString("ugho"), 0, 165, 0, 0);
            final var enemy = game.createUnitSimple(War3ID.fromString("hkni"), 1, 300, 40, 0);
            for (final CUnit unit : List.of(necromancer, ghoul, enemy)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            final var rune = War3ID.fromString("Rune");
            game.update();
            necromancer.setMana(necromancer.getMaximumMana());
            assertFalse(necromancer.order(game, OrderIds.unholyfrenzy, ghoul));
            assertFalse(necromancer.order(game, OrderIds.cripple, enemy));
            game.getPlayer(0).setTechResearched(game, rune, 1);
            game.update();
            necromancer.setMana(necromancer.getMaximumMana());
            final var ghoulAttack = ghoul.getUnitSpecificAttacks().get(0);
            final float baseGhoulSpeed = ghoulAttack.getAttackSpeedModifier();
            final float ghoulLife = ghoul.getLife();
            assertTrue(necromancer.order(game, OrderIds.unholyfrenzy, ghoul));
            for (int tick = 0; tick < 50 && ghoul.getLife() == ghoulLife; tick++) game.update();
            assertEquals(baseGhoulSpeed + 0.75f, ghoulAttack.getAttackSpeedModifier(), 0.001f);
            assertEquals(ghoulLife - 4, ghoul.getLife(), 0.001f);
            assertFalse(necromancer.order(game, OrderIds.cripple, enemy));
            game.getPlayer(0).setTechResearched(game, rune, 2);
            game.update();
            necromancer.setMana(necromancer.getMaximumMana());
            final int speed = enemy.getSpeed();
            final var enemyAttack = enemy.getUnitSpecificAttacks().get(0);
            final float damage = (enemyAttack.getMinDamageDisplay() + enemyAttack.getMaxDamageDisplay()) * 0.5f;
            final int baseDamageBonus = enemyAttack.getTotalTemporaryDamageBonus();
            final float baseEnemySpeed = enemyAttack.getAttackSpeedModifier();
            assertFalse(necromancer.order(game, OrderIds.cripple, ghoul));
            assertTrue(necromancer.order(game, OrderIds.cripple, enemy));
            for (int tick = 0; tick < 60 && enemy.getSpeed() == speed; tick++) game.update();
            assertEquals(speed * 0.25f, enemy.getSpeed(), 1f);
            assertEquals(baseEnemySpeed - 0.5f, enemyAttack.getAttackSpeedModifier(), 0.001f);
            assertEquals(-damage * 0.5f, enemyAttack.getTotalTemporaryDamageBonus() - baseDamageBonus, 1.5f);
        });
    }

    @Test
    void lethalPeriodicBuffCanRemoveSeveralAbilitiesDuringTheUnitTick() throws Exception {
        withFixture("Maps\\Campaign\\NightElf04.w3m", false, (game, projectiles) -> {
            final var unit = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, 0, 0);
            final var alias = War3ID.fromString("Btes");
            class PeriodicBuff extends CBuffTimed {
                private final Runnable tick;

                PeriodicBuff(final Runnable tick) {
                    super(game.getHandleIdAllocator().createId(), alias, alias, 10);
                    this.tick = tick;
                }

                @Override protected void onBuffAdd(CSimulation game, CUnit target) { }
                @Override protected void onBuffRemove(CSimulation game, CUnit target) { }
                @Override public boolean isTimedLifeBar() { return false; }
                @Override public void onTick(CSimulation game, CUnit target) { this.tick.run(); }
            }
            game.update();
            unit.add(game, new PeriodicBuff(() -> fail("A removed buff must stop ticking")));
            unit.add(game, new PeriodicBuff(() -> fail("A removed buff must stop ticking")));
            unit.add(game, new PeriodicBuff(() -> unit.kill(game)));
            game.update();
            assertTrue(unit.isDead());
            assertTrue(unit.getAbilities().stream().noneMatch(PeriodicBuff.class::isInstance));
        });
    }

    @Test
    void originalFrostNovaHitsTheTargetAndNearbyEnemiesWithDifferentDamage() throws Exception {
        withFixture("Maps\\Campaign\\Human05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID footman = War3ID.fromString("hfoo");
            final var lich = game.createUnitSimple(War3ID.fromString("Ulic"), 0, 0, 0, 0);
            final var target = game.createUnitSimple(footman, 1, 280, 0, 0);
            final var splash = game.createUnitSimple(War3ID.fromString("hgry"), 1, 280, 152, 0);
            final var ally = game.createUnitSimple(footman, 0, 280, -152, 0);
            final var distant = game.createUnitSimple(footman, 1, 640, 0, 0);
            for (final CUnit unit : List.of(lich, target, splash, ally, distant)) unit.setDisableAttacks(true);
            final List<Float> damageTaken = new ArrayList<>();
            for (final CUnit unit : List.of(target, splash)) {
                unit.addDamageTakenListener((simulation, attacker, victim, isAttack, isRanged, type, damage, bonus, actual) -> {
                    assertSame(lich, attacker);
                    assertEquals(CDamageType.COLD, type);
                    damageTaken.add(damage);
                });
            }
            final int baseSpeed = target.getSpeed();
            final var attack = target.getUnitSpecificAttacks().get(0);
            final float normalAttackSpeed = attack.getTotalAttackSpeedPercent();
            final War3ID nova = War3ID.fromString("AUfn");
            game.update();
            assertTrue(lich.order(game, nova.getValue(), null));
            lich.setMana(lich.getMaximumMana());
            assertFalse(lich.order(game, OrderIds.frostnova, ally));
            assertTrue(lich.order(game, OrderIds.frostnova, target));
            for (int tick = 0; tick < 60 && damageTaken.size() < 2; tick++) game.update();
            assertEquals(List.of(150.0f, 50.0f), damageTaken);
            assertEquals(baseSpeed / 2, target.getSpeed());
            assertEquals(normalAttackSpeed - 0.25f, attack.getTotalAttackSpeedPercent(), 0.001f);
            assertEquals(ally.getMaximumLife(), ally.getLife());
            assertEquals(distant.getMaximumLife(), distant.getLife());
            for (int tick = 0; tick < 85; tick++) game.update();
            assertEquals(baseSpeed, target.getSpeed());
            assertEquals(normalAttackSpeed, attack.getTotalAttackSpeedPercent(), 0.001f);
        });
    }

    @Test
    void originalOrcChainLightningUsesItsOwnOrderAndJumpsAcrossEnemies() throws Exception {
        withFixture("Maps\\Campaign\\Orc03.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var thrall = game.createUnitSimple(War3ID.fromString("Ofar"), 0, 0, 0, 0);
            final var target = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 280, 0, 0);
            final var next = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 536, 0, 0);
            thrall.setDisableAttacks(true);
            target.setPaused(true);
            next.setPaused(true);
            final List<Float> damageTaken = new ArrayList<>();
            for (final CUnit enemy : List.of(target, next)) {
                enemy.addDamageTakenListener((simulation, attacker, victim, isAttack, isRanged, type, damage, bonus, actual) -> {
                    assertSame(thrall, attacker);
                    assertEquals(CDamageType.LIGHTNING, type);
                    damageTaken.add(damage);
                });
            }
            game.update();
            assertTrue(thrall.order(game, War3ID.fromString("AOcl").getValue(), null));
            thrall.setMana(thrall.getMaximumMana());
            assertFalse(thrall.order(game, OrderIds.manaburn, target));
            assertTrue(thrall.order(game, OrderIds.chainlightning, target));
            for (int tick = 0; tick < 70; tick++) game.update();
            assertEquals(List.of(100.0f, 85.0f), damageTaken);
            assertEquals(thrall.getMaximumLife(), thrall.getLife());
        });
    }

    @Test
    void originalRexxarMishaCastDispatchesTheCampaignSummonEvent() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01.w3x", false, (game, projectiles) -> {
            final CUnit rexxar = game.createUnitSimple(War3ID.fromString("Orex"), 0, 0, 0, 0);
            final War3ID mishaSpell = War3ID.fromString("Arsg");
            final List<CUnit> summons = new ArrayList<>();
            final var player = game.getPlayer(0);
            player.addEvent(new CPlayerEvent(game.getGlobalScope(), player, new Trigger(),
                    JassGameEventsWar3.EVENT_PLAYER_UNIT_SUMMON, null) {
                @Override
                public void fire(final CUnit unit, final TriggerExecutionScope scope) {
                    assertSame(rexxar, unit);
                    summons.add(((CommonTriggerExecutionScope) scope).getSummonedUnit());
                }
            });
            game.update();
            assertTrue(rexxar.order(game, mishaSpell.getValue(), null));
            rexxar.setMana(rexxar.getMaximumMana());
            assertTrue(rexxar.order(game, OrderIds.spiritwolf, null));
            for (int tick = 0; tick < 40 && summons.isEmpty(); tick++) game.update();
            assertEquals(1, summons.size());
            final CUnit misha = summons.get(0);
            assertEquals(War3ID.fromString("ngzc"), misha.getTypeId());
            assertEquals(0, misha.getPlayerIndex());
            assertTrue(misha.getClassifications().contains(CUnitClassification.SUMMONED));
            for (int tick = 0; tick < 40; tick++) game.update();
            assertFalse(misha.isDead());
        });
    }

    @Test
    void tombManaRunesRestoreAuthoredAmountsOnPickupEvenWithAFullInventory() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX03.w3x", false, (game, projectiles) -> {
            final CUnit maiev = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, -640, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("edot"), 0, 288, 0, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("edot"), 0, 864, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("edot"), 1, -192, 0, 0);
            for (final CUnit unit : List.of(maiev, ally, distant, enemy)) unit.setDisableAttacks(true);
            final var inventory = maiev.getInventoryData();
            for (int slot = 0; slot < 6; slot++) {
                final CItem potion = game.createItem(War3ID.fromString("phea"), maiev.getX(), maiev.getY());
                assertEquals(slot, inventory.giveItem(game, maiev, potion, slot, false));
            }
            game.update();
            for (final CUnit unit : List.of(maiev, ally, distant, enemy)) unit.setMana(0);

            final CItem lesser = game.createItem(War3ID.fromString("rman"), maiev.getX(), maiev.getY());
            assertEquals(-1, inventory.giveItem(game, maiev, lesser, false));
            assertTrue(lesser.isDead());
            assertEquals(125, maiev.getMana(), 0.001f);
            assertEquals(125, ally.getMana(), 0.001f);
            assertEquals(0, enemy.getMana(), 0.001f);
            assertEquals(0, distant.getMana(), 0.001f);

            final CItem greater = game.createItem(War3ID.fromString("rma2"), maiev.getX(), maiev.getY());
            assertEquals(-1, inventory.giveItem(game, maiev, greater, false));
            assertTrue(greater.isDead());
            assertEquals(Math.min(425, maiev.getMaximumMana()), maiev.getMana(), 0.001f);
            assertEquals(Math.min(425, ally.getMaximumMana()), ally.getMana(), 0.001f);
            assertEquals(0, enemy.getMana(), 0.001f);
            assertEquals(0, distant.getMana(), 0.001f);
        });
    }

    @Test
    void originalNegationStaffAndDispelRuneUseTheirOwnAreaAndSummonDamage() throws Exception {
        withFixture("Maps\\Campaign\\NightElf05.w3m", false, (game, projectiles) -> {
            final CUnit tyrande = game.createUnitSimple(War3ID.fromString("Emoo"), 0, 0, 0, 0);
            final CUnit hostile = game.createUnitSimple(War3ID.fromString("nfrl"), 1, 384, 0, 0);
            final CUnit farHostile = game.createUnitSimple(War3ID.fromString("nfrl"), 1, -544, 0, 0);
            final CUnit friendly = game.createUnitSimple(War3ID.fromString("nfrl"), 0, 440, 80, 0);
            for (final CUnit unit : List.of(hostile, farHostile, friendly)) unit.addClassification(CUnitClassification.SUMMONED);
            for (final CUnit unit : List.of(tyrande, hostile, farHostile, friendly)) unit.setDisableAttacks(true);
            final var hostileDamage = new ArrayList<Float>();
            final var farDamage = new ArrayList<Float>();
            final var friendlyDamage = new ArrayList<Float>();
            hostile.addDamageTakenListener((sim, attacker, target, attack, ranged, type, damage, bonus, actual) -> hostileDamage.add(damage));
            farHostile.addDamageTakenListener((sim, attacker, target, attack, ranged, type, damage, bonus, actual) -> farDamage.add(damage));
            friendly.addDamageTakenListener((sim, attacker, target, attack, ranged, type, damage, bonus, actual) -> friendlyDamage.add(damage));
            game.update();
            final var inventory = tyrande.getInventoryData();
            final CItem staff = game.createItem(War3ID.fromString("wneg"), 0, 0);
            final int staffSlot = inventory.giveItem(game, tyrande, staff, 0, false);
            assertTrue(tyrande.order(game, OrderIds.itemuse00 + staffSlot, new AbilityPointTarget(392, 0)));
            for (int step = 0; step < 40 && hostileDamage.isEmpty(); step++) game.update();
            assertEquals(List.of(200f), hostileDamage);
            assertTrue(farDamage.isEmpty());
            assertTrue(friendlyDamage.isEmpty());

            final CItem rune = game.createItem(War3ID.fromString("rdis"), tyrande.getX(), tyrande.getY());
            assertEquals(-1, inventory.giveItem(game, tyrande, rune, false));
            assertTrue(rune.isDead());
            assertEquals(List.of(200f, 250f), hostileDamage);
            assertEquals(List.of(250f), farDamage);
            assertTrue(friendlyDamage.isEmpty());
        });
    }

    @Test
    void originalTownPortalProtectsTheCasterAndBringsOnlyEligibleNearbyUnits() throws Exception {
        withFixture("Maps\\Campaign\\Human05.w3m", false, (game, projectiles) -> {
            final CUnit arthas = game.createUnitSimple(War3ID.fromString("Hart"), 0, 0, -416, 0);
            final CUnit soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 236, -416, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("hfoo"), 0, -848, 784, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 1, -200, -416, 0);
            final CUnit hall = game.createUnitSimple(War3ID.fromString("hkee"), 1, 820, 620, 0);
            final CUnit enemyHall = game.createUnitSimple(War3ID.fromString("htow"), 2, -780, 500, 0);
            final CUnit worker = game.createUnitSimple(War3ID.fromString("hpea"), 0, 200, -180, 0);
            final CUnit mine = game.createUnitSimple(War3ID.fromString("ngol"), 3, 720, -180, 0);
            mine.getFirstAbilityOfType(CAbilityGoldMine.class).setGold(6000);
            game.createUnitSimple(War3ID.fromString("htow"), 0, 500, -640, 0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, true);
            game.getPlayer(0).setAlliance(1, CAllianceType.HELP_REQUEST, true);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, true);
            for (final CUnit unit : List.of(arthas, soldier, distant, ally)) unit.setDisableAttacks(true);
            game.update();
            assertTrue(distant.distanceSquaredNoCollision(arthas) > 1100 * 1100);
            assertTrue(worker.order(game, OrderIds.harvest, mine));
            final CItem scroll = game.createItem(War3ID.fromString("stwp"), 0, -416);
            final var inventory = arthas.getInventoryData();
            final int slot = inventory.giveItem(game, arthas, scroll, 0, false);
            assertFalse(arthas.order(game, OrderIds.itemuse00 + slot, enemyHall));
            assertSame(scroll, inventory.getItemInSlot(slot));

            final AbilityPointTarget landing = new AbilityPointTarget(760, 416);
            assertTrue(arthas.order(game, OrderIds.itemuse00 + slot, landing));
            for (int tick = 0; tick < 50 && !arthas.isInvulnerable(); tick++) game.update();
            assertTrue(arthas.isInvulnerable());
            assertFalse(soldier.isInvulnerable());
            arthas.order(game, OrderIds.move, new AbilityPointTarget(-500, 0));
            final int ticksPerSecond = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < 4 * ticksPerSecond; tick++) game.update();
            assertTrue(arthas.isInvulnerable());
            assertEquals(0, arthas.getX(), 1);
            for (int tick = 0; tick < 2 * ticksPerSecond; tick++) game.update();
            assertFalse(arthas.isInvulnerable());
            assertTrue(arthas.distanceSquaredNoCollision(landing) < 256 * 256);
            assertTrue(soldier.distanceSquaredNoCollision(landing) < 256 * 256);
            assertEquals(-200, ally.getX(), 1);
            assertEquals(-848, distant.getX(), 1);
            assertTrue(worker.getY() < 0);
            assertNull(inventory.getItemInSlot(slot));

            final CUnit castle = game.createUnitSimple(War3ID.fromString("hcas"), 0, -656, -604, 0);
            game.createUnitSimple(War3ID.fromString("htow"), 0, 300, 300, 0);
            game.update();
            final CItem secondScroll = game.createItem(War3ID.fromString("stwp"), arthas.getX(), arthas.getY());
            final int secondSlot = inventory.giveItem(game, arthas, secondScroll, slot, false);
            assertTrue(arthas.order(game, OrderIds.itemuse00 + secondSlot, null));
            for (int tick = 0; tick < 6 * ticksPerSecond; tick++) game.update();
            assertTrue(arthas.distanceSquaredNoCollision(castle) < 296 * 296);
        });
    }



    @Test
    void originalHexPreservesHeroAndInventoryWhileSuppressingOrders() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_02.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID hex = War3ID.fromString("AOhx");
            final War3ID victimType = War3ID.fromString("Hpal");
            final CUnit shaman = game.createUnitSimple(War3ID.fromString("Oshd"), 0, -260, 0, 0);
            final CUnit hero = game.createUnitSimple(victimType, 1, 260, 0, 180);
            shaman.setDisableAttacks(true);
            hero.setLifeRegenBonus(-hero.getUnitType().getLifeRegen());
            game.update();
            final var inventory = hero.getInventoryData();
            final CItem potion = game.createItem(War3ID.fromString("phea"), hero.getX(), hero.getY());
            final int slot = inventory.giveItem(game, hero, potion, 0, false);
            hero.setLife(game, hero.getMaximumLife() - 100);
            final float woundedLife = hero.getLife();
            final int originalSpeed = hero.getSpeed();

            assertTrue(shaman.order(game, hex.getValue(), null));
            shaman.setMana(shaman.getMaximumMana());
            assertTrue(shaman.order(game, OrderIds.hex, hero));
            for (int tick = 0; tick < 55 && !hero.hasCritterForm(); tick++) game.update();

            assertTrue(hero.hasCritterForm());
            assertEquals(victimType, hero.getTypeId());
            assertEquals(99, hero.getSpeed());
            assertTrue(hero.getCurrentAttacks().isEmpty());
            assertFalse(hero.order(game, OrderIds.itemuse00 + slot, null));
            assertSame(potion, inventory.getItemInSlot(slot));
            assertEquals(woundedLife, hero.getLife(), 1);
            for (int tick = 0; tick < 95; tick++) game.update();

            assertFalse(hero.hasCritterForm());
            assertEquals(originalSpeed, hero.getSpeed());
            assertFalse(hero.getCurrentAttacks().isEmpty());
            final float beforePotion = hero.getLife();
            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, null));
            assertTrue(hero.getLife() > beforePotion);
        });
    }

    @Test
    void originalPolymorphRequiresMasterTrainingAndRestoresArmorWhenDispelled() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_02.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit sorceress = game.createUnitSimple(War3ID.fromString("hsor"), 0, -220, 0, 0);
            final CUnit knight = game.createUnitSimple(War3ID.fromString("hkni"), 1, 220, 0, 180);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 280, 90, 180);
            for (final CUnit unit : List.of(sorceress, knight, hero)) unit.setDisableAttacks(true);
            game.update();
            final var originalArmor = knight.getDefenseType();

            assertFalse(sorceress.order(game, OrderIds.polymorph, knight));
            game.getPlayer(0).setTechtreeUnlocked(game, War3ID.fromString("Rhst"), 2);
            game.update();
            sorceress.setMaximumMana(400);
            sorceress.setMana(400);
            assertFalse(sorceress.order(game, OrderIds.polymorph, hero));
            assertTrue(sorceress.order(game, OrderIds.polymorph, knight));
            for (int tick = 0; tick < 55 && !knight.hasCritterForm(); tick++) game.update();

            assertTrue(knight.hasCritterForm());
            assertEquals(com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType.MEDIUM, knight.getDefenseType());
            knight.dispelBuffs(game);
            assertFalse(knight.hasCritterForm());
            assertEquals(originalArmor, knight.getDefenseType());
        });
    }



    @Test
    void originalMountHyjalSplashTreeIsNotAUnitTargetForAttackEffects() throws Exception {
        withFixture("Maps\\Campaign\\NightElf07.w3m", false, (game, projectiles) -> {
            final var tree = game.internalCreateDestructable(War3ID.fromString("FTtw"), 128, 0, null, null);
            final var factory = com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.AbilityBuilderGsonBuilder.create();
            final var condition = factory.fromJson("""
                    {"type":"isUnitValidTarget","target":{"type":"getAttackedUnit"}}
                    """, com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABCondition.class);
            final var store = new java.util.HashMap<String, Object>();
            final String attacked = com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys.ATTACKEDUNIT + 1;
            store.put(attacked, tree);
            assertFalse(condition.evaluate(game, null, store, 1));
        });
    }

    @Test
    void originalCampaignStoneTokenSummonsAndAntiMagicPotionProtectsTheCaster() throws Exception {
        withFixture("Maps\\Campaign\\NightElf03.w3m", false, (game, projectiles) -> {
            final var hero = game.createUnitSimple(War3ID.fromString("Etyr"), 0, 0, 0, 0);
            final var inventory = hero.getInventoryData();
            final CItem token = game.createItem(War3ID.fromString("fgrg"), 0, 0);
            final CItem potion = game.createItem(War3ID.fromString("pams"), 0, 0);
            final int tokenSlot = inventory.giveItem(game, hero, token, 0, false);
            final int potionSlot = inventory.giveItem(game, hero, potion, 1, false);
            game.update();

            assertTrue(hero.order(game, OrderIds.itemuse00 + tokenSlot, null));
            for (int tick = 0; tick < 35; tick++) game.update();
            final CUnit golem = game.getUnits().stream().filter(unit -> "ngst".equals(unit.getTypeId().toString()))
                    .findFirst().orElseThrow();
            assertTrue(golem.isUnitType(CUnitTypeJass.SUMMONED));
            assertEquals(0, golem.getPlayerIndex());
            assertNull(inventory.getItemInSlot(tokenSlot));

            assertTrue(hero.order(game, OrderIds.itemuse00 + potionSlot, null));
            for (int tick = 0; tick < 35 && !hero.isMagicImmune(); tick++) game.update();
            assertTrue(hero.isMagicImmune());
            assertNull(inventory.getItemInSlot(potionSlot));
            for (int tick = 0; tick < 220; tick++) game.update();
            assertFalse(hero.isMagicImmune());
            assertFalse(golem.isDead());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "HumanX06,fgdg,nbal,1", "UndeadX07a,fgfh,nfel,1",
            "OrcX01_04,axas,otbk,2", "OrcX01_04,rots,nmrr,2"
    })
    void originalCampaignSummonItemsCreateTheirAuthoredUnits(final String fixture) throws Exception {
        final String[] fields = fixture.split(",");
        withFixture("Maps\\FrozenThrone\\Campaign\\" + fields[0] + ".w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, 0, 0, 0);
            final CItem item = game.createItem(War3ID.fromString(fields[1]), 0, 0);
            final var inventory = hero.getInventoryData();
            final int slot = inventory.giveItem(game, hero, item, 0, false);
            final int charges = item.getCharges();
            final War3ID expected = War3ID.fromString(fields[2]);
            game.update();

            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, null));
            for (int tick = 0; tick < 50; tick++) game.update();
            final List<CUnit> summoned = game.getUnits().stream().filter(unit -> expected.equals(unit.getTypeId())).toList();
            assertEquals(Integer.parseInt(fields[3]), summoned.size());
            assertTrue(summoned.stream().allMatch(unit -> unit.getPlayerIndex() == 0 && unit.isSummonedOrWard()
                    && unit.getFirstAbilityOfType(CBuffTimedLife.class) != null));
            if (charges == 1) assertNull(inventory.getItemInSlot(slot));
            else if (charges > 1) assertEquals(charges - 1, item.getCharges());
            else assertSame(item, inventory.getItemInSlot(slot));
        });
    }

    @Test
    void originalSpiderSilkBroachGroundsOnlyEligibleFlyingTargets() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, 0, 0, 0);
            final CUnit rider = game.createUnitSimple(War3ID.fromString("owyv"), 1, 180, 40, 0);
            final CUnit grunt = game.createUnitSimple(War3ID.fromString("ogru"), 1, 180, -80, 0);
            List.of(hero, rider, grunt).forEach(unit -> unit.setDisableAttacks(true));
            final CItem silk = game.createItem(War3ID.fromString("silk"), 0, 0);
            final var inventory = hero.getInventoryData();
            final int slot = inventory.giveItem(game, hero, silk, 0, false);
            game.update();

            assertFalse(hero.order(game, OrderIds.itemuse00 + slot, grunt));
            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, rider));
            for (int tick = 0; tick < 80 && !rider.isWebbed(); tick++) game.update();
            assertTrue(rider.isWebbed());
            assertTrue(rider.isMovementDisabled());
            assertFalse(grunt.isWebbed());
            for (int tick = 0; tick < 430; tick++) game.update();
            assertFalse(rider.isWebbed());
            assertFalse(rider.isMovementDisabled());
        });
    }

    @Test
    void originalScrollResurrectsTheSixStrongestNearbyAlliesAndRestoresTheirFood() throws Exception {
        withFixture("Maps\\Campaign\\NightElf03.w3m", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Etyr"), 0, 0, 0, 0);
            hero.setDisableAttacks(true);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, true);
            final CItem scroll = game.createItem(War3ID.fromString("srrc"), 0, 0);
            final var inventory = hero.getInventoryData();
            final int slot = inventory.giveItem(game, hero, scroll, 0, false);
            final War3ID footman = War3ID.fromString("hfoo");
            final War3ID knight = War3ID.fromString("hkni");
            final var casualties = new ArrayList<CUnit>();
            for (int index = 0; index < 6; index++) {
                casualties.add(game.createUnitSimple(footman, 0, 96 + index * 90, 144, 0));
            }
            final CUnit strongAlly = game.createUnitSimple(knight, 1, 680, 30, 0);
            final CUnit enemy = game.createUnitSimple(knight, 2, 420, -100, 0);
            final CUnit distant = game.createUnitSimple(knight, 0, -1100, 0, 0);
            casualties.addAll(List.of(strongAlly, enemy, distant));
            casualties.forEach(unit -> unit.setDisableAttacks(true));
            game.update();
            assertFalse(hero.order(game, OrderIds.itemuse00 + slot, null));
            assertSame(scroll, inventory.getItemInSlot(slot));
            casualties.forEach(unit -> unit.kill(game));
            for (int tick = 0; tick < 1600 && !casualties.stream().allMatch(CUnit::isBoneCorpse); tick++) game.update();

            final int allyFood = game.getPlayer(1).getFoodUsed();
            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, null));
            for (int tick = 0; tick < 35; tick++) game.update();
            assertFalse(strongAlly.isDead());
            assertEquals(1, strongAlly.getPlayerIndex());
            assertEquals(allyFood + strongAlly.getUnitType().getFoodUsed(), game.getPlayer(1).getFoodUsed());
            assertEquals(6, casualties.stream().filter(unit -> !unit.isDead()).count());
            assertTrue(enemy.isDead());
            assertTrue(distant.isDead());
            assertNull(inventory.getItemInSlot(slot));
        });
    }


    @Test
    void originalFireOrbsStackBonusesAndEnableTheAuthoredAirAttack() throws Exception {
        withFixture("Maps\\Campaign\\NightElf01.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit primary = game.createUnitSimple(footman, 1, 117, 0, 180);
            final CUnit splash = game.createUnitSimple(footman, 1, 144, 83, 180);
            final CUnit friendly = game.createUnitSimple(footman, 0, 172, -55, 180);
            final CUnit outside = game.createUnitSimple(footman, 1, 290, 150, 180);
            final CUnit air = game.createUnitSimple(War3ID.fromString("hgry"), 1, 450, 0, 180);
            for (final CUnit unit : List.of(primary, splash, friendly, outside, air)) unit.setDisableAttacks(true);
            game.update();
            assertFalse(hero.order(game, OrderIds.attack, air));
            assertEquals(1, hero.getCurrentAttacks().size());
            final var inventory = hero.getInventoryData();
            final var melee = hero.getCurrentAttacks().get(0);
            final int previousBonus = melee.getTotalTemporaryDamageBonus();
            final CItem orb = game.createItem(War3ID.fromString("ofir"), 0, 0);
            final CItem artifact = game.createItem(War3ID.fromString("gldo"), 0, 0);
            final int orbSlot = inventory.giveItem(game, hero, orb, 0, false);
            final int artifactSlot = inventory.giveItem(game, hero, artifact, 1, false);

            assertEquals(previousBonus + 11, melee.getTotalTemporaryDamageBonus());
            assertEquals(2, hero.getCurrentAttacks().size());
            final var alternate = assertInstanceOf(CUnitAttackMissile.class, hero.getCurrentAttacks().get(1));
            assertTrue(alternate.getProjectileArt(hero).contains("FireBallMissile"));
            assertTrue(hero.order(game, OrderIds.attack, air));
            melee.launch(game, hero, primary, 40, CUnitAttackListener.DO_NOTHING);
            assertEquals(splash.getMaximumLife() - 11, splash.getLife(), 0.001f);
            assertEquals(friendly.getMaximumLife(), friendly.getLife(), 0.001f);
            assertEquals(outside.getMaximumLife(), outside.getLife(), 0.001f);

            inventory.dropItem(game, hero, orbSlot, -240, 0, false);
            assertEquals(previousBonus + 6, melee.getTotalTemporaryDamageBonus());
            assertTrue(alternate.getProjectileArt(hero).contains("IllidanMissile"));
            inventory.dropItem(game, hero, artifactSlot, -320, 0, false);
            assertEquals(previousBonus, melee.getTotalTemporaryDamageBonus());
            assertEquals(1, hero.getCurrentAttacks().size());
            assertFalse(hero.order(game, OrderIds.attack, air));

            final int newSlot = inventory.giveItem(game, hero, artifact, 1, false);
            hero.setTypeId(game, War3ID.fromString("Hmbr"));
            final var newMelee = hero.getCurrentAttacks().get(0);
            final int equippedAfterTransform = newMelee.getTotalTemporaryDamageBonus();
            assertTrue(hero.order(game, OrderIds.attack, air));
            hero.getInventoryData().dropItem(game, hero, newSlot, -320, 0, false);
            assertEquals(equippedAfterTransform - 6, newMelee.getTotalTemporaryDamageBonus());
            assertFalse(hero.order(game, OrderIds.attack, air));
        });
    }


    @Test
    void originalFrostAndVenomOrbsShareTheInventoryPriorityForTheirEffects() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_03.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final CUnit target = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 112, 0, 180);
            final var attack = hero.getCurrentAttacks().get(0);
            hero.setDisableAttacks(true);
            target.setDisableAttacks(true);
            target.setLifeRegenBonus(-target.getUnitType().getLifeRegen());
            game.update();

            final var inventory = hero.getInventoryData();
            final int originalSpeed = target.getSpeed();
            final int frostSlot = inventory.giveItem(game, hero, game.createItem(War3ID.fromString("ofro"), 0, 0), 0, false);
            inventory.giveItem(game, hero, game.createItem(War3ID.fromString("oven"), 0, 0), 1, false);
            attack.launch(game, hero, target, 35, CUnitAttackListener.DO_NOTHING);
            assertEquals(originalSpeed - Math.round(originalSpeed * game.getGameplayConstants().getFrostMoveSpeedDecrease()),
                    target.getSpeed());
            assertNull(target.getFirstAbilityOfType(CAbilityAttackPoison.Poison.class));
            for (int tick = 0; tick < 64; tick++) game.update();
            assertEquals(originalSpeed, target.getSpeed());

            inventory.dropItem(game, hero, frostSlot, -220, 0, false);
            attack.launch(game, hero, target, 35, CUnitAttackListener.DO_NOTHING);
            final var poison = target.getFirstAbilityOfType(CAbilityAttackPoison.Poison.class);
            assertNotNull(poison);
            assertEquals(10, poison.getDurationMax());
            final float afterHit = target.getLife();
            for (int tick = 0; tick < 24; tick++) game.update();
            assertEquals(afterHit - 6, target.getLife(), 0.01f);
        });
    }

    @Test
    void originalSlowOrbUsesTheItemSpellDurationsAndCanBeDispelled() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX03Secret.w3x", false, (game, projectiles) -> {
            final CUnit paladin = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 112, 0, 180);
            final CUnit enemyHero = game.createUnitSimple(War3ID.fromString("Hblm"), 1, 112, 128, 180);
            final var attack = paladin.getCurrentAttacks().get(0);
            for (final CUnit unit : List.of(paladin, footman, enemyHero)) unit.setDisableAttacks(true);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final var inventory = paladin.getInventoryData();
            inventory.giveItem(game, paladin, game.createItem(War3ID.fromString("oslo"), 0, 0), 0, false);
            game.update();

            final War3ID slow = War3ID.fromString("Bslo");
            for (final CUnit target : List.of(footman, enemyHero)) {
                final int speed = target.getSpeed();
                for (int hit = 0; hit < 240 && target.getAbilityLevel(slow) == 0; hit++) {
                    target.setLife(game, target.getMaximumLife());
                    attack.launch(game, paladin, target, 10, CUnitAttackListener.DO_NOTHING);
                }
                assertTrue(target.getSpeed() < speed);
                final var buff = target.getAbilities().stream().filter(ability -> slow.equals(ability.getAlias()))
                        .map(CBuff.class::cast).findFirst().orElseThrow();
                assertEquals(target.isHero() ? 20 : 60, buff.getDurationMax());
                target.dispelBuffs(game);
                assertEquals(speed, target.getSpeed());
            }
        });
    }

    @Test
    void originalLightningOrbUsesTheItemPurgeAgainstSummons() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_05.w3x", false, (game, projectiles) -> {
            final CUnit paladin = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final CUnit summon = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 112, 0, 180);
            final var attack = paladin.getCurrentAttacks().get(0);
            paladin.setDisableAttacks(true);
            summon.setDisableAttacks(true);
            summon.addClassification(CUnitClassification.SUMMONED);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final var inventory = paladin.getInventoryData();
            inventory.giveItem(game, paladin, game.createItem(War3ID.fromString("oli2"), 0, 0), 0, false);
            game.update();

            final int originalSpeed = summon.getSpeed();
            for (int hit = 0; hit < 100 && summon.getSpeed() == originalSpeed; hit++) {
                summon.setLife(game, summon.getMaximumLife());
                attack.launch(game, paladin, summon, 10, CUnitAttackListener.DO_NOTHING);
            }
            assertTrue(summon.getSpeed() < originalSpeed);
            assertTrue(summon.getMaximumLife() - summon.getLife() >= 150);
            assertFalse(summon.isDead());
        });
    }

    @Test
    void originalMountHyjalLightningOrbUsesTheLegacyBonusAndPurgeTogether() throws Exception {
        withFixture("Maps\\Campaign\\NightElf07.w3m", false, (game, projectiles) -> {
            final CUnit paladin = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 112, 0, 180);
            final var attack = paladin.getCurrentAttacks().get(0);
            paladin.setDisableAttacks(true);
            footman.setDisableAttacks(true);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final var inventory = paladin.getInventoryData();
            final int speed = footman.getSpeed();
            final int damageBonus = attack.getTotalTemporaryDamageBonus();
            inventory.giveItem(game, paladin, game.createItem(War3ID.fromString("olig"), 0, 0), 0, false);
            game.update();

            assertEquals(damageBonus + 6, attack.getTotalTemporaryDamageBonus());
            attack.launch(game, paladin, footman, 10, CUnitAttackListener.DO_NOTHING);
            assertTrue(footman.getSpeed() < speed);
            final var purge = footman.getAbilities().stream().filter(ability -> War3ID.fromString("Bprg").equals(ability.getAlias()))
                    .map(CBuff.class::cast).findFirst().orElseThrow();
            assertEquals(3, purge.getDurationMax());
            for (int tick = 0; tick < 66; tick++) game.update();
            assertEquals(speed, footman.getSpeed());
        });
    }

    @Test
    void originalDarknessOrbRaisesItsAuthoredTimedMinionOnTheKillingHit() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX06.w3x", false, (game, projectiles) -> {
            final CUnit arthas = game.createUnitSimple(War3ID.fromString("Udea"), 0, 0, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 112, 0, 180);
            final var attack = arthas.getCurrentAttacks().get(0);
            arthas.setDisableAttacks(true);
            footman.setDisableAttacks(true);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final var inventory = arthas.getInventoryData();
            inventory.giveItem(game, arthas, game.createItem(War3ID.fromString("odef"), 0, 0), 0, false);
            game.update();

            final War3ID minionType = War3ID.fromString("ndr1");
            attack.launch(game, arthas, footman, 10, CUnitAttackListener.DO_NOTHING);
            assertFalse(game.getUnitsIncludingPending().stream().anyMatch(unit -> minionType.equals(unit.getTypeId())));
            attack.launch(game, arthas, footman, 1_000, CUnitAttackListener.DO_NOTHING);
            assertTrue(footman.isDead());
            final CUnit minion = game.getUnitsIncludingPending().stream()
                    .filter(unit -> minionType.equals(unit.getTypeId()) && unit.getPlayerIndex() == 0).findFirst().orElseThrow();
            assertTrue(minion.isSummonedOrWard());
            final var timedLife = minion.getFirstAbilityOfType(CBuffTimedLife.class);
            assertNotNull(timedLife);
            assertEquals(80, timedLife.getDurationMax());
        });
    }

    @Test
    void originalTinyGreatHallConsumesTheItemOnlyWhenConstructionStarts() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX01.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final var player = game.getPlayer(0);
            final var inventory = hero.getInventoryData();
            final CItem tinyHall = game.createItem(War3ID.fromString("tgrh"), 0, 0);
            final int slot = inventory.giveItem(game, hero, tinyHall, 0, false);
            final int order = OrderIds.itemuse00 + slot;
            game.update();

            assertFalse(hero.order(game, order, new AbilityPointTarget(4_096, 4_096)));
            assertTrue(hero.order(game, order, new AbilityPointTarget(720, 640)));
            game.update();
            assertTrue(hero.order(game, OrderIds.stop, null));
            assertSame(tinyHall, inventory.getItemInSlot(slot));
            final int cap = player.getFoodCap();
            final int gold = player.getGold();
            final int lumber = player.getLumber();

            assertTrue(hero.order(game, order, new AbilityPointTarget(342, 12)));
            CUnit building = null;
            for (int tick = 0; tick < 140 && building == null; tick++) {
                game.update();
                building = game.getUnitsIncludingPending().stream().filter(CUnit::isConstructing).findFirst().orElse(null);
            }
            assertNotNull(building);
            assertEquals("htow", building.getTypeId().toString());
            assertEquals(20, building.getConstructionDuration());
            assertTrue(building.getLife() < building.getMaximumLife());
            assertEquals(cap, player.getFoodCap());
            assertNull(inventory.getItemInSlot(slot));
            assertEquals(gold, player.getGold());
            assertEquals(lumber, player.getLumber());
            for (int tick = 0; tick < 380; tick++) game.update();
            assertTrue(building.isConstructing());
            for (int tick = 0; tick < 24; tick++) game.update();
            assertFalse(building.isConstructing());
            assertEquals(cap + building.getUnitType().getFoodMade(), player.getFoodCap());
            assertEquals(1, player.getMissionScore(CPlayerScore.STRUCT_BUILT));
            building.setLife(game, 0);
            assertEquals(cap, player.getFoodCap());
        });
    }

    @Test
    void originalSoulGemFiresItemUseForTheConsumedGemAndTrapsTheExistingHero() throws Exception {
        withFixture("Maps\\Campaign\\Orc08.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit thrall = game.createUnitSimple(War3ID.fromString("Ofar"), 0, 0, 0, 0);
            final CUnit grom = game.createUnitSimple(War3ID.fromString("Ogrh"), 1, 344, 0, 180);
            final CUnit grunt = game.createUnitSimple(War3ID.fromString("ogru"), 1, 120, 0, 180);
            assertNotNull(grom.getHeroData());
            thrall.setDisableAttacks(true);
            grom.setDisableAttacks(true);
            grunt.setDisableAttacks(true);
            final var inventory = thrall.getInventoryData();
            final CItem vacantSlot = inventory.getItemInSlot(0);
            assertNull(vacantSlot);
            assertEquals(-1, inventory.getSlot(vacantSlot));
            inventory.dropItem(game, thrall, vacantSlot, thrall.getX(), thrall.getY(), true);
            final CItem emptyGem = game.createItem(War3ID.fromString("gsou"), 0, 0);
            final int slot = inventory.giveItem(game, thrall, emptyGem, 0, false);
            final int order = OrderIds.itemuse00 + slot;
            final List<CItem> useEvents = new ArrayList<>();
            final var player = game.getPlayer(0);
            player.addEvent(new CPlayerEvent(game.getGlobalScope(), player, new Trigger(),
                    JassGameEventsWar3.EVENT_PLAYER_UNIT_USE_ITEM, null) {
                @Override
                public void fire(final CUnit unit, final TriggerExecutionScope scope) {
                    final var itemScope = (CommonTriggerExecutionScope) scope;
                    assertSame(thrall, unit);
                    assertSame(thrall, itemScope.getManipulatingUnit());
                    assertEquals(JassGameEventsWar3.EVENT_PLAYER_UNIT_USE_ITEM, itemScope.getTriggerEventId());
                    useEvents.add(itemScope.getManipulatedItem());
                }
            });
            game.update();
            assertFalse(thrall.order(game, order, grunt));
            assertFalse(thrall.order(game, order, thrall));
            assertTrue(useEvents.isEmpty());
            assertSame(emptyGem, inventory.getItemInSlot(slot));
            assertTrue(thrall.order(game, order, grom));
            for (int tick = 0; tick < 48 && useEvents.isEmpty(); tick++) game.update();

            assertEquals(List.of(emptyGem), useEvents);
            assertEquals(War3ID.fromString("gsou"), useEvents.get(0).getTypeId());
            assertTrue(grom.isHidden());
            assertTrue(grom.isPaused());
            assertFalse(grom.isDead());
            assertSame(grom, game.getUnit(grom.getHandleId()));
            assertEquals(1, grom.getPlayerIndex());
            final CItem scriptGem = game.createItem(War3ID.fromString("soul"), thrall.getX(), thrall.getY());
            assertEquals(slot, inventory.giveItem(game, thrall, scriptGem, false));
            for (int tick = 0; tick < 20; tick++) game.update();
            assertSame(scriptGem, inventory.getItemInSlot(slot));
            assertFalse(scriptGem.isDroppable());
            final var possession = grom.getFirstAbilityOfType(CAbilitySoulPossession.class);
            assertNotNull(possession);
            game.removeItem(scriptGem);
            grom.remove(game, possession);
            assertFalse(grom.isHidden());
            assertFalse(grom.isPaused());
            assertFalse(grom.isInvulnerable());
            assertFalse(grom.isDead());
        });
    }

    @Test
    void originalSoulPreservationRemovesAZombieWithoutADeathEvent() throws Exception {
        withFixture("Maps\\Campaign\\Human06.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit malganis = game.createUnitSimple(War3ID.fromString("Umal"), 0, 0, 0, 0);
            final CUnit zombie = game.createUnitSimple(War3ID.fromString("nzom"), 0, 160, 0, 0);
            final CUnit soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 256, 0, 0);
            for (final CUnit unit : List.of(malganis, zombie, soldier)) unit.setDisableAttacks(true);
            final War3ID spell = War3ID.fromString("ANsl");
            final var preserve = game.getAbilityData().createAbility(spell, game.getHandleIdAllocator().createId());
            assertNotNull(preserve);
            malganis.add(game, preserve);
            final var player = game.getPlayer(0);
            final List<CUnit> deaths = new ArrayList<>();
            player.addEvent(new CPlayerEvent(game.getGlobalScope(), player, new Trigger(), JassGameEventsWar3.EVENT_PLAYER_UNIT_DEATH, null) {
                @Override
                public void fire(final CUnit unit, final TriggerExecutionScope scope) {
                    deaths.add(unit);
                }
            });
            game.update();
            assertFalse(malganis.order(game, OrderIds.soulpreservation, soldier));
            assertTrue(malganis.order(game, OrderIds.soulpreservation, zombie));
            for (int tick = 0; tick < 36; tick++) game.update();
            assertTrue(game.containsUnit(zombie));
            assertFalse(malganis.order(game, OrderIds.soulpreservation, zombie));
            for (int tick = 0; tick < 72 && game.containsUnit(zombie); tick++) game.update();
            assertFalse(game.containsUnit(zombie));
            assertTrue(game.containsUnit(soldier));
            assertTrue(deaths.isEmpty());
        });
    }

    @Test
    void originalShimmerweedFiresPickupWithoutAnActiveItemAbility() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_05.w3x", false, (game, projectiles) -> {
            final CUnit rexxar = game.createUnitSimple(War3ID.fromString("Orex"), 0, 0, 0, 0);
            final CItem shimmerweed = game.createItem(War3ID.fromString("shwd"), 320, 0);
            assertTrue(shimmerweed.getItemType().isUseAutomaticallyWhenAcquired());
            assertFalse(shimmerweed.getItemType().isActivelyUsed());
            final List<CItem> pickups = new ArrayList<>();
            final var player = game.getPlayer(0);
            player.addEvent(new CPlayerEvent(game.getGlobalScope(), player, new Trigger(),
                    JassGameEventsWar3.EVENT_PLAYER_UNIT_PICKUP_ITEM, null) {
                @Override
                public void fire(final CUnit unit, final TriggerExecutionScope scope) {
                    final var itemScope = (CommonTriggerExecutionScope) scope;
                    assertSame(rexxar, unit);
                    assertSame(rexxar, itemScope.getManipulatingUnit());
                    pickups.add(itemScope.getManipulatedItem());
                }
            });
            game.update();
            assertTrue(rexxar.order(game, OrderIds.getitem, shimmerweed));
            for (int tick = 0; tick < 120 && pickups.isEmpty(); tick++) game.update();
            assertEquals(List.of(shimmerweed), pickups);
            assertTrue(shimmerweed.isDead());
            assertNull(shimmerweed.getContainedUnit());
            assertEquals(-1, rexxar.getInventoryData().getSlot(shimmerweed));
            assertFalse(rexxar.order(game, OrderIds.getitem, shimmerweed));
        });
    }

    @Test
    void originalSpeedRuneAcceleratesNearbyAlliesAndRestoresTheirSpeedOnExpiry() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX03Secret.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, -512, 0, 0);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit ally = game.createUnitSimple(footman, 0, 0, 0, 0);
            final CUnit enemy = game.createUnitSimple(footman, 1, 100, 100, 180);
            final CUnit distant = game.createUnitSimple(footman, 0, 850, 0, 180);
            for (final CUnit unit : List.of(hero, ally, enemy, distant)) unit.setDisableAttacks(true);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final int heroSpeed = hero.getMovementSpeed(game);
            final int escortSpeed = ally.getMovementSpeed(game);
            final int maximum = (int) game.getGameplayConstants().getMaxUnitSpeed();
            final var inventory = hero.getInventoryData();
            final var potion = game.createItem(War3ID.fromString("pspd"), hero.getX(), hero.getY());
            final int potionSlot = inventory.giveItem(game, hero, potion, false);
            game.update();

            assertTrue(hero.order(game, OrderIds.itemuse00 + potionSlot, null));
            for (int tick = 0; tick < 30 && hero.getMovementSpeed(game) == heroSpeed; tick++) game.update();
            assertEquals(Math.min(maximum, Math.round(heroSpeed * 1.6f)), hero.getMovementSpeed(game));
            assertEquals(escortSpeed, ally.getMovementSpeed(game));

            final CItem rune = game.createItem(War3ID.fromString("rspd"), hero.getX(), hero.getY());
            inventory.giveItem(game, hero, rune, false);
            assertTrue(rune.isDead());
            assertEquals(maximum, hero.getMovementSpeed(game));
            assertEquals(maximum, ally.getMovementSpeed(game));
            assertEquals(escortSpeed, enemy.getMovementSpeed(game));
            assertEquals(escortSpeed, distant.getMovementSpeed(game));
            for (int tick = 0; tick < 1190; tick++) game.update();
            assertEquals(maximum, ally.getMovementSpeed(game));
            for (int tick = 0; tick < 20; tick++) game.update();
            assertEquals(heroSpeed, hero.getMovementSpeed(game));
            assertEquals(escortSpeed, ally.getMovementSpeed(game));
        });
    }

    @Test
    void originalTomeOfRetrainingRefundsLearnedRanksWithoutChangingTheHero() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX01.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final var heroData = hero.getHeroData();
            final var inventory = hero.getInventoryData();
            heroData.setHeroLevel(game, hero, 4, false);
            final War3ID holyLight = War3ID.fromString("AHhb");
            final War3ID divineShield = War3ID.fromString("AHds");
            final War3ID devotion = War3ID.fromString("AHad");
            heroData.selectHeroSkill(game, hero, holyLight);
            heroData.selectHeroSkill(game, hero, holyLight);
            heroData.selectHeroSkill(game, hero, divineShield);
            assertEquals(2, hero.getAbilityLevel(holyLight));
            assertEquals(1, hero.getAbilityLevel(divineShield));
            final int points = heroData.getSkillPoints();
            final int experience = heroData.getXp();
            final String name = heroData.getProperName();
            final CItem ring = game.createItem(War3ID.fromString("rde1"), 0, 0);
            final int ringSlot = inventory.giveItem(game, hero, ring, false);
            final float defense = hero.getDefense();
            final CItem tome = game.createItem(War3ID.fromString("tret"), 0, 0);
            inventory.giveItem(game, hero, tome, false);

            assertTrue(tome.isDead());
            assertEquals(0, hero.getAbilityLevel(holyLight));
            assertEquals(0, hero.getAbilityLevel(divineShield));
            assertEquals(points + 3, heroData.getSkillPoints());
            assertEquals(experience, heroData.getXp());
            assertEquals(4, heroData.getHeroLevel());
            assertEquals(name, heroData.getProperName());
            assertEquals(defense, hero.getDefense());
            assertSame(ring, inventory.getItemInSlot(ringSlot));
            heroData.selectHeroSkill(game, hero, devotion);
            assertEquals(1, hero.getAbilityLevel(devotion));
        });
    }

    @Test
    void originalAmuletOfRecallBringsTwelveOwnedUnitsFromTheTargetArea() throws Exception {
        withFixture("Maps\\Campaign\\Human09.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, -760, 0, 0);
            final War3ID footman = War3ID.fromString("hfoo");
            final var army = new ArrayList<CUnit>();
            for (int index = 0; index < 12; index++) {
                army.add(game.createUnitSimple(footman, 0, 352 + index % 4 * 48, 64 + index / 4 * 48, 180));
            }
            final CUnit excess = game.createUnitSimple(footman, 0, 900, 0, 180);
            final CUnit distant = game.createUnitSimple(footman, 0, -550, 600, 180);
            final CUnit enemy = game.createUnitSimple(footman, 1, 460, -96, 180);
            final CUnit barracks = game.createUnitSimple(War3ID.fromString("hbar"), 0, 500, -256, 180);
            for (final CUnit unit : List.of(hero, excess, distant, enemy)) unit.setDisableAttacks(true);
            for (final CUnit unit : army) unit.setDisableAttacks(true);
            final var inventory = hero.getInventoryData();
            final CItem amulet = game.createItem(War3ID.fromString("amrc"), hero.getX(), hero.getY());
            final int slot = inventory.giveItem(game, hero, amulet, false);
            game.update();

            final int order = OrderIds.itemuse00 + slot;
            assertFalse(hero.order(game, order, enemy));
            assertSame(amulet, inventory.getItemInSlot(slot));
            assertTrue(hero.order(game, order, new AbilityPointTarget(400, 100)));
            for (int tick = 0; tick < 48 && army.get(11).getX() > 0; tick++) game.update();

            assertEquals(-760, hero.getX());
            assertTrue(army.stream().allMatch(unit -> unit.distanceSquaredNoCollision(hero) < 220 * 220));
            assertTrue(excess.getX() > 600);
            assertTrue(distant.getY() > 500);
            assertTrue(enemy.getX() > 300);
            assertTrue(barracks.getX() > 300);
            assertNull(inventory.getItemInSlot(slot));
        });
    }

    @Test
    void originalTombDruidNeedsAdeptTrainingAndHealsOverTwelveSeconds() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX03.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit druid = game.createUnitSimple(War3ID.fromString("edoc"), 0, -192, 0, 0);
            final CUnit maiev = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, 80, 0, 0);
            final CUnit dryad = game.createUnitSimple(War3ID.fromString("edry"), 0, 96, 128, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, -96, 0);
            for (final CUnit unit : List.of(druid, maiev, dryad, enemy)) unit.setDisableAttacks(true);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            maiev.setLifeRegenBonus(-maiev.getUnitType().getLifeRegen());
            maiev.setLifeRegenStrengthBonus(0);
            game.update();
            assertFalse(druid.order(game, OrderIds.rejuvination, maiev));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Redc"), 1);
            druid.setMana(druid.getMaximumMana());
            maiev.setLife(game, 100);
            game.update();
            assertEquals(1, druid.getAbilityLevel(War3ID.fromString("Arej")));
            assertFalse(druid.order(game, OrderIds.rejuvination, enemy));
            assertTrue(druid.order(game, OrderIds.rejuvination, maiev));
            final War3ID healing = War3ID.fromString("Brej");
            for (int tick = 0; tick < 100 && maiev.getAbilities().stream().noneMatch(ability -> healing.equals(ability.getAlias())); tick++) game.update();
            assertTrue(maiev.getAbilities().stream().anyMatch(ability -> healing.equals(ability.getAlias())),
                    () -> "Expected the Rejuvenation buff on Maiev. Abilities: " + maiev.getAbilities().stream()
                            .map(ability -> ability.getClass().getSimpleName() + "=" + ability.getAlias()).toList()
                            + ", life=" + maiev.getLife() + ", Druid mana=" + druid.getMana() + ", Druid behavior="
                            + druid.getCurrentBehavior().getClass().getSimpleName());
            assertEquals(100, maiev.getLife(), 34);
            for (int tick = 0; tick < 120; tick++) game.update();
            assertEquals(300, maiev.getLife(), 34);
            for (int tick = 0; tick < 122; tick++) game.update();
            assertEquals(500, maiev.getLife(), 0.02f);
            assertFalse(maiev.getAbilities().stream().anyMatch(ability -> healing.equals(ability.getAlias())));

            assertTrue(dryad.isMagicImmune());
            druid.setMana(druid.getMaximumMana());
            assertTrue(druid.order(game, OrderIds.rejuvination, dryad));
            for (int tick = 0; tick < 100 && dryad.getAbilities().stream().noneMatch(ability -> healing.equals(ability.getAlias())); tick++) game.update();
            assertTrue(dryad.getAbilities().stream().anyMatch(ability -> healing.equals(ability.getAlias())));
            dryad.dispelBuffs(game);
            assertFalse(dryad.getAbilities().stream().anyMatch(ability -> healing.equals(ability.getAlias())));
        });
    }

    @Test
    void originalDestroyerAbsorbsOwnedManaAndDevoursEnemyBuffsForTheAuthoredResources() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX07Interlude.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(0).setAlliance(2, CAllianceType.PASSIVE, true);
            final CUnit destroyer = game.createUnitSimple(War3ID.fromString("ubsp"), 0, 0, 0, 0);
            final CUnit donor = game.createUnitSimple(War3ID.fromString("uobs"), 0, -220, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hmpr"), 2, 250, 128, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 280, 64, 0);
            final CUnit summoned = game.createUnitSimple(War3ID.fromString("hwat"), 1, 356, -40, 0);
            summoned.addClassification(CUnitClassification.SUMMONED);
            for (final CUnit unit : List.of(destroyer, donor, ally, enemy, summoned)) {
                unit.setDisableAttacks(true);
                unit.setManaRegen(0);
            }
            game.update();
            destroyer.setMana(0);
            donor.setMana(100);
            ally.setMana(100);
            assertFalse(destroyer.order(game, OrderIds.absorb, ally));
            assertTrue(destroyer.order(game, OrderIds.absorb, donor));
            for (int tick = 0; tick < 40 && donor.getMana() > 0; tick++) game.update();
            assertEquals(0, donor.getMana());
            assertEquals(100, destroyer.getMana());
            assertEquals(100, ally.getMana());

            final War3ID slow = War3ID.fromString("Bfro");
            for (final CUnit unit : List.of(enemy, ally)) {
                unit.add(game, new CBuffSlow(game.getHandleIdAllocator().createId(), slow, 20, 0.25f, 0.5f));
            }
            destroyer.setLife(game, 500);
            assertTrue(destroyer.order(game, OrderIds.devourmagic, new AbilityPointTarget(300, 64)));
            for (int tick = 0; tick < 45 && destroyer.getMana() == 100; tick++) game.update();
            assertEquals(150, destroyer.getMana());
            assertEquals(525, destroyer.getLife(), 0.03f);
            assertTrue(summoned.getMaximumLife() - summoned.getLife() >= 179.9);
            for (final CUnit unit : List.of(enemy, ally)) {
                assertFalse(unit.getAbilities().stream().anyMatch(ability -> slow.equals(ability.getAlias())));
            }
        });
    }

    @Test
    void originalMagtheridonHowlLowersNearbyEnemyAttackDamageAndCanBeDispelled() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX06.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit magtheridon = game.createUnitSimple(War3ID.fromString("Nmag"), 0, 0, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 184, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, -184, 0, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 864, 0, 0);
            List.of(magtheridon, enemy, ally, distant).forEach(unit -> unit.setDisableAttacks(true));
            final var howl = game.getAbilityData().getAbilityType(War3ID.fromString("ANht"));
            magtheridon.add(game, howl.createAbility(game.getHandleIdAllocator().createId()));
            game.update();
            magtheridon.setMana(150);

            final var attack = enemy.getUnitSpecificAttacks().get(0);
            assertTrue(magtheridon.order(game, OrderIds.howlofterror, null));
            for (int tick = 0; tick < 40 && attack.getTotalTemporaryDamageBonus() == 0; tick++) game.update();

            assertTrue(attack.getTotalTemporaryDamageBonus() < 0);
            assertEquals(0, ally.getUnitSpecificAttacks().get(0).getTotalTemporaryDamageBonus());
            assertEquals(0, distant.getUnitSpecificAttacks().get(0).getTotalTemporaryDamageBonus());
            enemy.dispelBuffs(game);
            assertEquals(0, attack.getTotalTemporaryDamageBonus());
        });
    }

    @Test
    void originalFrenzyAutocastBuffsAttackWithoutReplacingTheAttackOrder() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX06.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit magtheridon = game.createUnitSimple(War3ID.fromString("Nmag"), 0, 0, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 272, 0, 0);
            enemy.setDisableAttacks(true);
            game.update();

            assertTrue(magtheridon.order(game, OrderIds.frenzyon, null));
            assertEquals(1, magtheridon.getModelScaleMultiplier());
            final int normalSpeed = magtheridon.getSpeed();
            assertTrue(magtheridon.order(game, OrderIds.attack, enemy));
            for (int tick = 0; tick < 80 && magtheridon.getModelScaleMultiplier() == 1; tick++) game.update();

            assertTrue(magtheridon.getSpeed() > normalSpeed);
            assertTrue(magtheridon.getModelScaleMultiplier() > 1);
            assertSame(enemy, magtheridon.getCurrentOrder().getTarget(game));
            magtheridon.dispelBuffs(game);
            assertEquals(normalSpeed, magtheridon.getSpeed());
            assertEquals(1, magtheridon.getModelScaleMultiplier());
        });
    }

    @Test
    void originalCreepSlamUsesItsOwnOrderAndOnlyHitsNearbyGroundEnemies() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\War3XRegularCreditsIce.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit creep = game.createUnitSimple(War3ID.fromString("nmgd"), 0, 0, 0, 0);
            final CUnit nearby = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, 0, 0);
            final CUnit flier = game.createUnitSimple(War3ID.fromString("hgry"), 1, 145, 64, 0);
            final CUnit far = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 400, 0, 0);
            List.of(creep, nearby, flier, far).forEach(unit -> unit.setDisableAttacks(true));
            game.update();
            creep.setMana(150);

            assertTrue(creep.order(game, OrderIds.creepthunderclap, null));
            for (int tick = 0; tick < 35 && nearby.getLife() == nearby.getMaximumLife(); tick++) game.update();

            assertEquals(70, nearby.getMaximumLife() - nearby.getLife(), 0.02f);
            assertEquals(flier.getMaximumLife(), flier.getLife());
            assertEquals(far.getMaximumLife(), far.getLife());
        });
    }

    @Test
    void originalRokhanSerpentWardUsesTheCampaignAliasAndExpiresAfterFortySeconds() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01.w3x", false, (game, projectiles) -> {
            final CUnit rokhan = game.createUnitSimple(War3ID.fromString("Orkn"), 0, 0, 0, 0);
            final var type = game.getAbilityData().getAbilityType(War3ID.fromString("Arsw"));
            assertNotNull(type);
            rokhan.add(game, type.createAbility(game.getHandleIdAllocator().createId()));
            game.update();
            rokhan.setMana(130);
            final var point = new AbilityPointTarget(224, 96);
            assertTrue(rokhan.order(game, OrderIds.ward, point));
            for (int tick = 0; tick < 40; tick++) game.update();

            final CUnit ward = game.getUnits().stream().filter(unit -> "osp1".equals(unit.getTypeId().toString()))
                    .findFirst().orElseThrow();
            assertEquals(0, ward.getPlayerIndex());
            assertTrue(ward.isSummonedOrWard());
            assertTrue(ward.isMagicImmune());
            assertTrue(ward.isMovementDisabled());
            assertFalse(ward.getCurrentAttacks().isEmpty());
            assertTrue(ward.distanceSquaredNoCollision(point.getX(), point.getY()) < 32 * 32);
            for (int tick = 0; tick < 800 && !ward.isDead(); tick++) game.update();
            assertTrue(ward.isDead());
        });
    }

    @Test
    void originalObservatoryLifeLimitFiresOnCrossingWithTheWatchedUnitInScope() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX01.w3x", false, (game, projectiles) -> {
            final CUnit tower = game.createUnitSimple(War3ID.fromString("haro"), 0, 0, 0, 0);
            final CUnitState state = CUnitState.LIFE;
            final var trigger = new Trigger();
            final var observedUnits = new ArrayList<CUnit>();
            trigger.addAction((arguments, globals, event) -> {
                final var scope = (CommonTriggerExecutionScope) event;
                assertEquals(state, scope.getEventUnitState());
                assertEquals(JassGameEventsWar3.EVENT_UNIT_STATE_LIMIT, scope.getTriggerEventId());
                observedUnits.add(scope.getTriggeringUnit());
                return null;
            });
            final var registration = tower.registerStateEvent(new GlobalScope(), trigger, state,
                    CLimitOp.GREATER_THAN_OR_EQUAL, 2499);
            assertTrue(observedUnits.isEmpty());

            tower.setLife(game, 1125);
            tower.setLife(game, 2498);
            assertTrue(observedUnits.isEmpty());
            tower.setLife(game, 2499);
            tower.setLife(game, 2500);
            assertEquals(List.of(tower), observedUnits);

            tower.setLife(game, 2498);
            tower.setLife(game, 2500);
            assertEquals(List.of(tower, tower), observedUnits);
            registration.remove();
            tower.setLife(game, 2498);
            tower.setLife(game, 2500);
            assertEquals(List.of(tower, tower), observedUnits);
        });
    }

    @Test
    void originalMountainGiantTauntRedirectsOnlyTheTenNearestAttackers() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX06.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, false);
            final CUnit giant = game.createUnitSimple(War3ID.fromString("emtg"), 0, 0, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("emtg"), 0, 0, 90, 0);
            final List<CUnit> enemies = new ArrayList<>();
            for (int index = 0; index < 11; index++) {
                final double angle = index * Math.PI / 5;
                final int distance = 112 + index * 17;
                enemies.add(game.createUnitSimple(War3ID.fromString("hrif"), 1,
                        (float) (Math.cos(angle) * distance), (float) (Math.sin(angle) * distance), 0));
            }
            game.update();
            for (final CUnit enemy : enemies) assertTrue(enemy.order(game, OrderIds.attack, ally));

            assertTrue(giant.order(game, OrderIds.taunt, null));
            for (int tick = 0; tick < 25 && enemies.get(0).getCurrentOrder().getTarget(game) != giant; tick++) game.update();

            assertEquals(10, enemies.stream().filter(enemy -> enemy.getCurrentOrder().getTarget(game) == giant).count());
            assertTrue(enemies.subList(0, 8).stream().allMatch(enemy -> enemy.getCurrentOrder().getTarget(game) == giant));
            assertTrue(enemies.get(0).order(game, OrderIds.attack, ally));
            assertSame(ally, enemies.get(0).getCurrentOrder().getTarget(game));
        });
    }

    @Test
    void originalAttributeBonusUpdatesEveryStatWhenRankChangesAndWhenRemoved() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, 0, 0, 0);
            final var stats = hero.getHeroData();
            final var attributes = List.of(stats.getStrength(), stats.getAgility(), stats.getIntelligence());
            final var initialBonuses = attributes.stream().map(stat -> stat.getBonus()).toList();
            final var type = game.getAbilityData().getAbilityType(War3ID.fromString("Aamk"));
            final var bonus = (CAbilityItemStatBonus) type.createAbility(game.getHandleIdAllocator().createId());

            hero.add(game, bonus);
            assertEquals(initialBonuses.stream().map(value -> value + 3).toList(),
                    attributes.stream().map(stat -> stat.getBonus()).toList());

            type.setLevel(game, hero, bonus, 2);
            assertEquals(2, bonus.getLevel());
            assertEquals(initialBonuses.stream().map(value -> value + 6).toList(),
                    attributes.stream().map(stat -> stat.getBonus()).toList());

            hero.remove(game, bonus);
            assertEquals(initialBonuses, attributes.stream().map(stat -> stat.getBonus()).toList());
        });
    }

    @Test
    void originalFrostAttacksChillEnemyDirectAndSplashVictimsForTheirAuthoredDurations() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX05.w3x", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit tower = game.createUnitSimple(War3ID.fromString("uzg2"), 0, 0, 0, 0);
            final CUnit wyrm = game.createUnitSimple(War3ID.fromString("ufro"), 0, 400, 0, 0);
            final CUnit towerVictim = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, 0, 0);
            final CUnit primary = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 590, 0, 0);
            final CUnit secondary = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 640, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 590, 55, 0);
            final CUnit outside = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 905, 0, 0);
            final var towerAttack = (CUnitAttackMissile) tower.getCurrentAttacks().get(0);
            final var breath = (CUnitAttackMissile) wyrm.getCurrentAttacks().get(0);
            List.of(tower, wyrm, towerVictim, primary, secondary, ally, outside).forEach(unit -> unit.setDisableAttacks(true));
            game.update();

            towerAttack.doDamage(game, tower, towerVictim, 10, towerVictim.getX(), towerVictim.getY(), 0, CUnitAttackListener.DO_NOTHING);
            breath.doDamage(game, wyrm, primary, 100, primary.getX(), primary.getY(), 0, CUnitAttackListener.DO_NOTHING);

            assertEquals(5, towerVictim.getFirstAbilityOfType(CBuffFrost.class).getDurationRemaining(game, towerVictim), 0.03f);
            assertEquals(10, primary.getFirstAbilityOfType(CBuffFrost.class).getDurationRemaining(game, primary), 0.03f);
            assertEquals(10, secondary.getFirstAbilityOfType(CBuffFrost.class).getDurationRemaining(game, secondary), 0.03f);
            assertNull(ally.getFirstAbilityOfType(CBuffFrost.class));
            assertNull(outside.getFirstAbilityOfType(CBuffFrost.class));
            final int elapsedTicks = Math.round(6 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < elapsedTicks; tick++) game.update();
            assertNull(towerVictim.getFirstAbilityOfType(CBuffFrost.class));
            assertNotNull(secondary.getFirstAbilityOfType(CBuffFrost.class));
        });
    }

    @Test
    void originalFreezingBreathRequiresResearchAndSuspendsBuildingProduction() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX07Interlude.w3x", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit wyrm = game.createUnitSimple(War3ID.fromString("ufro"), 0, 0, 0, 0);
            final CUnit barracks = game.createUnitSimple(War3ID.fromString("hbar"), 1, 240, 0, 0);
            final var attack = (CUnitAttackMissile) wyrm.getCurrentAttacks().get(0);
            wyrm.setDisableAttacks(true);
            game.getPlayer(1).setGold(500);
            game.getPlayer(1).setFoodCap(20);
            barracks.queueTrainingUnit(game, War3ID.fromString("hfoo"));
            game.update();
            final float initialProgress = barracks.getConstructionProgress();

            attack.doDamage(game, wyrm, barracks, 25, barracks.getX(), barracks.getY(), 0, CUnitAttackListener.DO_NOTHING);
            assertFalse(barracks.isFrozenBuilding());
            game.update();
            assertTrue(barracks.getConstructionProgress() > initialProgress);

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rufb"), 1);
            game.update();
            attack.doDamage(game, wyrm, barracks, 25, barracks.getX(), barracks.getY(), 0, CUnitAttackListener.DO_NOTHING);
            assertTrue(barracks.isFrozenBuilding());
            final float frozenProgress = barracks.getConstructionProgress();
            final int second = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < 3 * second; tick++) game.update();
            assertEquals(frozenProgress, barracks.getConstructionProgress());

            barracks.setPaused(true);
            for (int tick = 0; tick < 3 * second; tick++) game.update();
            assertFalse(barracks.isFrozenBuilding());
            assertTrue(barracks.isPaused());
            assertEquals(frozenProgress, barracks.getConstructionProgress());
            barracks.setPaused(false);
            game.update();
            assertTrue(barracks.getConstructionProgress() > frozenProgress);
            for (int tick = 0; tick < 40 * second && barracks.getBuildQueue()[0] != null; tick++) game.update();
            assertEquals(1, game.getPlayer(1).getMissionScore(CPlayerScore.UNITS_TRAINED));
            attack.doDamage(game, wyrm, barracks, 10_000, barracks.getX(), barracks.getY(), 0, CUnitAttackListener.DO_NOTHING);
            assertTrue(barracks.isDead());
            assertEquals(1, game.getPlayer(0).getMissionScore(CPlayerScore.STRUCT_RAZED));
        });
    }

    @Test
    void originalMissionScoresKeepThePeakArmyAndCountEnemyHeroDeaths() throws Exception {
        withFixture("Maps\\Campaign\\Human01.w3m", false, (game, projectiles) -> {
            final var player = game.getPlayer(0);
            player.setAlliance(1, CAllianceType.PASSIVE, false);
            player.setAlliance(2, CAllianceType.PASSIVE, true);
            final CUnit soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, 0, 0);
            final CUnit enemyHero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 230, 0, 0);
            final CUnit allyHero = game.createUnitSimple(War3ID.fromString("Hpal"), 2, -230, 0, 0);
            game.update();
            player.setFoodCap(16);
            player.setFoodUsed(8);
            player.setUnitFoodUsed(soldier, soldier.getFoodUsed() + 2);
            player.setFoodUsed(4);
            player.setFoodCap(10);
            assertEquals(10, player.getMissionScore(CPlayerScore.FOOD_MAXUSED));
            assertEquals(16, player.getMissionScore(CPlayerScore.FOOD_MAXPROD));
            for (final CUnit hero : List.of(enemyHero, allyHero)) {
                hero.damage(game, soldier, true, false, CAttackType.CHAOS, CDamageType.NORMAL, null, 10_000);
                assertTrue(hero.isDead());
            }
            assertEquals(1, player.getMissionScore(CPlayerScore.HEROES_KILLED));
        });
    }

    @Test
    void originalNagaParasiteAutocastsOnAnEligibleEnemyAndSpawnsItsWateryMinion() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX05.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit siren = game.createUnitSimple(War3ID.fromString("nnsw"), 0, 0, 0, 0);
            final CUnit near = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 150, 0, 0);
            final CUnit far = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 460, 0, 0);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 570, 0, 0);
            final War3ID parasiteBuff = War3ID.fromString("BNpa");
            final int second = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            List.of(siren, near, far, hero).forEach(unit -> unit.setDisableAttacks(true));
            siren.setMana(100);
            siren.setManaRegen(0);
            game.update();

            assertFalse(siren.order(game, OrderIds.parasite, hero));
            siren.order(game, OrderIds.parasiteon, null);
            for (int tick = 0; tick < 2 * second; tick++) game.update();
            assertEquals(50, siren.getMana());
            assertTrue(far.getAbilityLevel(parasiteBuff) > 0);
            assertEquals(0, near.getAbilityLevel(parasiteBuff));
            assertEquals(0, hero.getAbilityLevel(parasiteBuff));
            assertEquals(5, far.getMaximumLife() - far.getLife(), 0.6f);
            assertEquals(0, far.dispelBuffs(game));

            siren.order(game, OrderIds.parasiteoff, null);
            far.setLife(game, 4);
            for (int tick = 0; tick < 2 * second; tick++) game.update();
            assertTrue(far.isDead());
            final CUnit minion = game.getUnitsIncludingPending().stream()
                    .filter(unit -> War3ID.fromString("ncfs").equals(unit.getTypeId()) && !unit.isDead()).findFirst().orElseThrow();
            assertEquals(0, minion.getPlayerIndex());
            assertTrue(minion.isSummonedOrWard());
            assertEquals(90, minion.getFirstAbilityOfType(CBuffTimedLife.class).getDurationMax());
        });
    }

    @Test
    void originalSpellBreakerFeedbackBurnsTheAuthoredHeroAndUnitManaAmounts() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX03.w3x", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit breaker = game.createUnitSimple(War3ID.fromString("hspt"), 0, 0, 0, 0);
            final CUnit caster = game.createUnitSimple(War3ID.fromString("hsor"), 1, 112, 90, 0);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hblm"), 1, 180, -85, 0);
            final CUnit immune = game.createUnitSimple(War3ID.fromString("hspt"), 1, 190, 130, 0);
            final var attack = breaker.getCurrentAttacks().get(0);
            List.of(breaker, caster, hero, immune).forEach(unit -> unit.setDisableAttacks(true));
            for (final CUnit unit : List.of(caster, hero, immune)) {
                unit.setMana(100);
                unit.setManaRegen(0);
                unit.setManaRegenIntelligenceBonus(0);
            }
            game.update();

            for (final CUnit target : List.of(caster, hero, immune)) {
                attack.launch(game, breaker, target, 10, CUnitAttackListener.DO_NOTHING);
            }
            for (int tick = 0; tick < 45; tick++) game.update();
            assertEquals(80, caster.getMana());
            assertEquals(95, hero.getMana());
            assertEquals(100, immune.getMana());
            assertTrue(caster.getMaximumLife() - caster.getLife() >= 20);
        });
    }

    @Test
    void originalBansheePossessionRequiresMasterTrainingAndTransfersTheSameUnitAfterChanneling() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit banshee = game.createUnitSimple(War3ID.fromString("uban"), 0, -128, 0, 0);
            final CUnit victim = game.createUnitSimple(War3ID.fromString("hkni"), 1, 80, 40, 0);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 380, -180, 0);
            List.of(banshee, victim, hero).forEach(unit -> unit.setDisableAttacks(true));
            game.update();

            assertFalse(banshee.order(game, OrderIds.possession, victim));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Ruba"), 2);
            game.update();
            banshee.setMana(banshee.getMaximumMana());
            assertFalse(banshee.order(game, OrderIds.possession, hero));
            assertTrue(banshee.order(game, OrderIds.possession, victim));
            for (int tick = 0; tick < 40 && !victim.isUnitType(CUnitTypeJass.STUNNED); tick++) game.update();
            assertTrue(victim.isUnitType(CUnitTypeJass.STUNNED));
            assertEquals(1, victim.getPlayerIndex());

            final StateModBuff interrupt = new StateModBuff(StateModBuffType.STUN, 1);
            banshee.addStateModBuff(interrupt);
            banshee.computeUnitState(game, StateModBuffType.STUN);
            game.update();
            assertFalse(victim.isUnitType(CUnitTypeJass.STUNNED));
            assertEquals(1, victim.getPlayerIndex());
            assertTrue(game.containsUnit(banshee));
            banshee.removeStateModBuff(interrupt);
            banshee.computeUnitState(game, StateModBuffType.STUN);

            banshee.setMana(banshee.getMaximumMana());
            assertTrue(banshee.order(game, OrderIds.possession, victim));
            for (int tick = 0; tick < 150 && game.containsUnit(banshee); tick++) game.update();
            assertFalse(game.containsUnit(banshee));
            assertTrue(game.containsUnit(victim));
            assertEquals(0, victim.getPlayerIndex());
            assertFalse(victim.isUnitType(CUnitTypeJass.STUNNED));
        });
    }

    @Test
    void originalDoomSurvivesDispelAndCreatesADoomGuardOnDeath() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit pitLord = game.createUnitSimple(War3ID.fromString("Nplh"), 0, -144, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 144, 120, 0);
            final CUnit enemyHero = game.createUnitSimple(War3ID.fromString("Hpal"), 1, 410, -60, 0);
            List.of(pitLord, footman, enemyHero).forEach(unit -> unit.setDisableAttacks(true));
            pitLord.add(game, game.getAbilityData().createAbility(War3ID.fromString("ANdo"), game.getHandleIdAllocator().createId()));
            pitLord.setMana(pitLord.getMaximumMana());
            game.update();

            assertFalse(pitLord.order(game, OrderIds.doom, enemyHero));
            assertTrue(pitLord.order(game, OrderIds.doom, footman));
            final War3ID buff = War3ID.fromString("BNdm");
            for (int tick = 0; tick < 50 && footman.getAbilities().stream().noneMatch(ability -> buff.equals(ability.getAlias())); tick++) game.update();
            assertTrue(footman.getAbilities().stream().anyMatch(ability -> buff.equals(ability.getAlias())));
            final float lifeBeforeDamage = footman.getLife();
            footman.dispelBuffs(game);
            for (int tick = 0; tick < 21; tick++) game.update();
            assertEquals(40, lifeBeforeDamage - footman.getLife(), 1);
            assertTrue(footman.getAbilities().stream().anyMatch(ability -> buff.equals(ability.getAlias())));
            footman.kill(game);
            final CUnit demon = game.getUnitsIncludingPending().stream()
                    .filter(unit -> "nbal".equals(unit.getTypeId().toString())).findFirst().orElseThrow();
            assertEquals(0, demon.getPlayerIndex());
            assertTrue(demon.isSummonedOrWard());
            assertEquals(120, demon.getFirstAbilityOfType(CBuffTimedLife.class).getDurationMax());
        });
    }

    @Test
    void originalVengeanceSummonsAnAvatarWithItsOwnVengeanceAbility() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX03.w3x", false, (game, projectiles) -> {
            final CUnit maiev = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, 0, 0, 0);
            final War3ID vengeance = War3ID.fromString("AEsv");
            maiev.add(game, game.getAbilityData().createAbility(vengeance, game.getHandleIdAllocator().createId()));
            maiev.setMana(maiev.getMaximumMana());
            game.update();

            assertTrue(maiev.order(game, OrderIds.spiritofvengeance, null));
            for (int tick = 0; tick < 85; tick++) game.update();
            final CUnit avatar = game.getUnitsIncludingPending().stream()
                    .filter(unit -> "espv".equals(unit.getTypeId().toString())).findFirst().orElseThrow();
            assertEquals(0, avatar.getPlayerIndex());
            assertTrue(avatar.isSummonedOrWard());
            assertNotNull(avatar.getFirstAbilityOfType(CBuffTimedLife.class));
            assertTrue(avatar.getAbilities().stream().anyMatch(ability -> "Avng".equals(ability.getAlias().toString())));
        });
    }

    @Test
    void originalUndeadSleepAbilityWakesOnDamageAndRespectsScriptedDisable() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX05.w3x", false, (game, projectiles) -> {
            final War3ID sleepId = War3ID.fromString("A000");
            final CUnit sentry = game.createUnitSimple(War3ID.fromString("hkni"), 0, 0, 0, 0);
            final CUnit secondSentry = game.createUnitSimple(War3ID.fromString("hkni"), 0, 280, 144, 0);
            final CUnit attacker = game.createUnitSimple(War3ID.fromString("uske"), 1, 400, -120, 0);
            attacker.setDisableAttacks(true);
            for (final CUnit unit : List.of(sentry, secondSentry)) {
                if (unit.getAbilities().stream().noneMatch(ability -> sleepId.equals(ability.getAlias()))) {
                    unit.add(game, game.getAbilityData().createAbility(sleepId, game.getHandleIdAllocator().createId()));
                }
            }
            for (int tick = 0; tick < 5; tick++) game.update();
            assertTrue(sentry.isUnitType(CUnitTypeJass.SLEEPING));
            assertTrue(secondSentry.isUnitType(CUnitTypeJass.SLEEPING));
            sentry.damage(game, attacker, true, false, CAttackType.NORMAL, CDamageType.NORMAL, "", 15);
            for (int tick = 0; tick < 400; tick++) game.update();
            assertFalse(sentry.isUnitType(CUnitTypeJass.SLEEPING));
            assertTrue(secondSentry.isUnitType(CUnitTypeJass.SLEEPING));
            game.getPlayer(0).setAbilityEnabled(game, sleepId, false);
            game.update();
            assertFalse(secondSentry.isUnitType(CUnitTypeJass.SLEEPING));
        });
    }

    @Test
    void originalMeatWagonCollectsAndDropsCorpsesAndExhumesWithResearch() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit wagon = game.createUnitSimple(War3ID.fromString("umtw"), 0, 0, 0, 0);
            wagon.setDisableAttacks(true);
            final CUnit corpse = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 180, 64, 0);
            corpse.kill(game);
            game.getWorldCollision().removeUnit(corpse);
            corpse.prepareCorpseForDecay(game);
            game.getWorldCollision().addUnit(corpse);
            final var cargo = wagon.getCargoData();
            game.update();

            assertNotNull(cargo);
            assertEquals(8, cargo.getCargoCapacity());
            assertTrue(wagon.order(game, OrderIds.loadcorpse, null));
            for (int tick = 0; tick < 100 && cargo.isEmpty(); tick++) game.update();
            assertEquals(1, cargo.getCargoCount());
            assertSame(corpse, cargo.getUnit(0));
            assertTrue(corpse.isHidden());
            assertSame(wagon, corpse.getTransport());

            wagon.setPointAndCheckUnstuck(440, -260, game);
            assertTrue(wagon.order(game, OrderIds.unloadallcorpses, null));
            assertTrue(cargo.isEmpty());
            assertFalse(corpse.isHidden());
            assertNull(corpse.getTransport());
            assertTrue(corpse.isBoneCorpse());
            assertTrue(wagon.canReach(corpse, 140));
            for (int tick = 0; tick < 320; tick++) game.update();
            assertTrue(cargo.isEmpty());
            game.getPlayer(0).setTechtreeUnlocked(game, War3ID.fromString("Ruex"), 1);
            game.update();
            final var exhume = wagon.getFirstAbilityOfType(CAbilityExhumeCorpses.class);
            assertNotNull(exhume);
            assertTrue(exhume.isRequirementsMet(game, wagon));
            assertFalse(exhume.isDisabled());
            for (int tick = 0; tick < 310; tick++) game.update();
            assertEquals(1, cargo.getCargoCount(), () -> game.getUnitsIncludingPending().stream()
                    .map(unit -> unit.getTypeId() + " dead=" + unit.isDead() + " raises=" + unit.isRaisable()
                            + " decays=" + unit.isDecays()).toList().toString());
            assertEquals(War3ID.fromString("ugho"), cargo.getUnit(0).getTypeId());
            for (int tick = 0; tick < 1_650; tick++) game.update();
            assertEquals(5, cargo.getCargoCount());
        });
    }

    @Test
    void originalNecromancerCanRaiseDeadDirectlyFromNearbyOwnedMeatWagon() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit wagon = game.createUnitSimple(War3ID.fromString("umtw"), 0, 144, 64, 0);
            final CUnit necromancer = game.createUnitSimple(War3ID.fromString("unec"), 0, -140, -64, 0);
            final CUnit corpse = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 144, 150, 0);
            List.of(wagon, necromancer, corpse).forEach(unit -> unit.setDisableAttacks(true));
            corpse.kill(game);
            game.getWorldCollision().removeUnit(corpse);
            corpse.prepareCorpseForDecay(game);
            game.getWorldCollision().addUnit(corpse);
            game.update();
            assertTrue(wagon.order(game, OrderIds.loadcorpse, null));
            for (int tick = 0; tick < 90 && wagon.getCargoData().isEmpty(); tick++) game.update();
            assertEquals(1, wagon.getCargoData().getCargoCount());
            necromancer.setMana(necromancer.getMaximumMana());
            assertTrue(necromancer.order(game, OrderIds.raisedead, null));
            for (int tick = 0; tick < 140 && !wagon.getCargoData().isEmpty(); tick++) game.update();
            assertTrue(wagon.getCargoData().isEmpty());
            assertEquals(2, game.getUnitsIncludingPending().stream()
                    .filter(unit -> unit.getPlayerIndex() == 0 && "uske".equals(unit.getTypeId().toString())).count());
        });
    }

    @Test
    void originalBattleStationsCallsFourOwnedPeonsIntoOnlyTheSelectedBurrow() throws Exception {
        withFixture("Maps\\Campaign\\Orc06.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID burrowId = War3ID.fromString("otrb");
            final War3ID peonId = War3ID.fromString("opeo");
            final CUnit burrow = game.createUnitSimple(burrowId, 0, 0, 0, 0);
            final CUnit otherBurrow = game.createUnitSimple(burrowId, 0, 600, 0, 0);
            final CUnit grunt = game.createUnitSimple(War3ID.fromString("ogru"), 0, 144, 0, 0);
            final CUnit enemyPeon = game.createUnitSimple(peonId, 1, -144, 80, 0);
            grunt.setDisableAttacks(true);
            enemyPeon.setDisableAttacks(true);
            final var workers = new ArrayList<CUnit>();
            for (int index = 0; index < 5; index++) {
                final CUnit worker = game.createUnitSimple(peonId, 0, -166 - 88 * index, -96 - 55 * index, 0);
                worker.setDisableAttacks(true);
                workers.add(worker);
            }
            game.update();
            assertTrue(burrow.getCurrentAttacks().isEmpty());
            assertNotNull(CAbilityLoad.getTransportLoad(game, workers.get(0), burrow, true, false),
                    () -> "Expected the original Burrow to accept a Peon. Abilities: "
                            + burrow.getAbilities().stream().map(ability -> ability.getClass().getSimpleName() + "=" + ability.getAlias()).toList());
            assertTrue(burrow.order(game, OrderIds.battlestations, null));
            for (int tick = 0; tick < 320 && burrow.getCargoData().getCargoCount() < 4; tick++) game.update();
            assertEquals(4, burrow.getCargoData().getCargoCount());
            assertFalse(burrow.getCurrentAttacks().isEmpty());
            assertTrue(workers.subList(0, 4).stream().allMatch(worker -> worker.getTransport() == burrow));
            assertNull(workers.get(4).getTransport());
            assertNull(grunt.getTransport());
            assertNull(enemyPeon.getTransport());
            assertTrue(otherBurrow.getCargoData().isEmpty());

            assertTrue(burrow.order(game, OrderIds.standdown, null));
            assertTrue(burrow.getCargoData().isEmpty());
            assertTrue(burrow.getCurrentAttacks().isEmpty());
            assertTrue(workers.stream().noneMatch(worker -> worker.isHidden() || worker.getTransport() != null));
        });
    }

    @Test
    void originalChaosBurrowAcceptsFelPeonsAfterChaosResearch() throws Exception {
        withFixture("Maps\\Campaign\\Orc05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit burrow = game.createUnitSimple(War3ID.fromString("otrb"), 0, 0, 0, 0);
            final CUnit felPeon = game.createUnitSimple(War3ID.fromString("ncpn"), 0, 190, 35, 180);
            final CUnit grunt = game.createUnitSimple(War3ID.fromString("ogru"), 0, 148, -95, 180);
            felPeon.setDisableAttacks(true);
            grunt.setDisableAttacks(true);
            game.update();
            assertNull(CAbilityLoad.getTransportLoad(game, felPeon, burrow, true, false));

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Roch"), 1);
            game.update();
            assertNotNull(CAbilityLoad.getTransportLoad(game, felPeon, burrow, true, false));
            assertNull(CAbilityLoad.getTransportLoad(game, grunt, burrow, true, false));
            assertTrue(burrow.order(game, OrderIds.battlestations, null));
            for (int tick = 0; tick < 120 && burrow.getCargoData().isEmpty(); tick++) game.update();
            assertSame(burrow, felPeon.getTransport());
            assertNull(grunt.getTransport());
            assertTrue(burrow.order(game, OrderIds.standdown, null));
            assertNull(felPeon.getTransport());
            assertFalse(felPeon.isHidden());
        });
    }

    @Test
    void originalWandOfNeutralizationOnlyDispelsUnitsOnTheTargetsSide() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, 0, -144, 0);
            final CUnit primary = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, 144, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 160, 240, 0);
            final CUnit summon = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 272, 144, 0);
            final CUnit outside = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 720, 144, 0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            summon.addClassification(CUnitClassification.SUMMONED);
            final War3ID slow = War3ID.fromString("Bslo");
            for (final CUnit unit : List.of(hero, primary, ally, summon, outside)) {
                unit.setDisableAttacks(true);
                unit.add(game, new CBuffSlow(game.getHandleIdAllocator().createId(), slow, 30, 0.25f, 0.6f));
            }
            final var inventory = hero.getInventoryData();
            final CItem wand = game.createItem(War3ID.fromString("wneu"), 0, 0);
            final int slot = inventory.giveItem(game, hero, wand, 0, false);
            final int charges = wand.getCharges();
            game.update();

            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, primary));
            for (int tick = 0; tick < 70 && summon.getFirstAbilityOfType(CBuffSlow.class) != null; tick++) game.update();
            assertNull(primary.getFirstAbilityOfType(CBuffSlow.class));
            assertNull(summon.getFirstAbilityOfType(CBuffSlow.class));
            assertNotNull(ally.getFirstAbilityOfType(CBuffSlow.class));
            assertNotNull(hero.getFirstAbilityOfType(CBuffSlow.class));
            assertNotNull(outside.getFirstAbilityOfType(CBuffSlow.class));
            assertEquals(summon.getMaximumLife(), summon.getLife());
            assertEquals(charges - 1, wand.getCharges());
        });
    }

    @Test
    void originalRuneOfRebirthGrantsItsSourceCreatureOnPickup() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX01.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hblm"), 0, 0, 0, 0);
            final War3ID ogre = War3ID.fromString("noga");
            final CItem rune = game.createItem(War3ID.fromString("rreb"), 80, 180);
            rune.setDropSourceUnitType(ogre);
            final CItem unattachedRune = game.createItem(War3ID.fromString("rreb"), 150, 180);
            game.update();

            assertEquals(-1, hero.getInventoryData().giveItem(game, hero, unattachedRune, 0, false));
            assertFalse(game.getUnitsIncludingPending().stream().anyMatch(unit -> ogre.equals(unit.getTypeId())));
            assertEquals(-1, hero.getInventoryData().giveItem(game, hero, rune, 0, false));
            final CUnit reborn = game.getUnitsIncludingPending().stream().filter(unit -> ogre.equals(unit.getTypeId()))
                    .findFirst().orElseThrow();
            assertTrue(rune.isDead());
            assertEquals(hero.getPlayerIndex(), reborn.getPlayerIndex());
            assertEquals(reborn.getMaximumLife(), reborn.getLife());
            assertFalse(reborn.isSummonedOrWard());
            assertTrue(reborn.canReach(80, 180, 64));
            assertEquals(-1, hero.getInventoryData().giveItem(game, hero, rune, 0, false));
            assertEquals(1, game.getUnitsIncludingPending().stream().filter(unit -> ogre.equals(unit.getTypeId())).count());
        });
    }

    @Test
    void originalWandOfManaStealingTransfersOnlyManaTheTargetHas() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX06.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit maiev = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, -140, -80, 0);
            final CUnit sorceress = game.createUnitSimple(War3ID.fromString("hsor"), 1, 144, 120, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, -120, 0);
            List.of(maiev, sorceress, footman).forEach(unit -> unit.setDisableAttacks(true));
            final CItem wand = game.createItem(War3ID.fromString("woms"), 0, 0);
            final var inventory = maiev.getInventoryData();
            final int slot = inventory.giveItem(game, maiev, wand, 0, false);
            final int charges = wand.getCharges();
            game.update();
            maiev.setMana(0);
            maiev.setManaRegen(0);
            maiev.setManaRegenIntelligenceBonus(0);
            sorceress.setMana(32);
            sorceress.setManaRegen(0);

            assertFalse(maiev.order(game, OrderIds.itemuse00 + slot, maiev));
            assertFalse(maiev.order(game, OrderIds.itemuse00 + slot, footman));
            assertTrue(maiev.order(game, OrderIds.itemuse00 + slot, sorceress));
            for (int tick = 0; tick < 60 && sorceress.getMana() >= 32; tick++) game.update();
            assertEquals(32, maiev.getMana());
            assertEquals(0, sorceress.getMana());
            assertEquals(sorceress.getMaximumLife(), sorceress.getLife());
            assertEquals(charges - 1, wand.getCharges());
        });
    }

    @Test
    void originalDiamondOfSummoningRecallsTwelveOwnedUnitsAfterItsDelay() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX06.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, -1400, -1100, 0);
            final var inventory = hero.getInventoryData();
            final CItem diamond = game.createItem(War3ID.fromString("dsum"), 0, 0);
            final int slot = inventory.giveItem(game, hero, diamond, 0, false);
            final var owned = new ArrayList<CUnit>();
            for (int index = 0; index < 13; index++) {
                owned.add(game.createUnitSimple(War3ID.fromString("hfoo"), 0, 1130 + index % 5 * 75,
                        1100 + index / 5 * 84, 0));
            }
            final CUnit allied = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 1500, 1150, 0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, true);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, true);
            game.update();

            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, new AbilityPointTarget(1300, 1200)));
            for (int tick = 0; tick < 15; tick++) game.update();
            assertEquals(0, owned.stream().filter(unit -> hero.canReach(unit, 350)).count());
            for (int tick = 0; tick < 90; tick++) game.update();
            assertEquals(12, owned.stream().filter(unit -> hero.canReach(unit, 350)).count());
            assertFalse(hero.canReach(allied, 350));
            assertSame(diamond, inventory.getItemInSlot(slot));
        });
    }

    @Test
    void originalHornOfTheCloudsSuppressesNearbyEnemyTowerFireUntilInterrupted() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX04.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, -550, 0, 0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final War3ID guardTower = War3ID.fromString("hgtw");
            final CUnit tower = game.createUnitSimple(guardTower, 1, 144, 0, 0);
            final CUnit friendlyTower = game.createUnitSimple(guardTower, 0, 280, 130, 0);
            final CUnit rifleman = game.createUnitSimple(War3ID.fromString("hrif"), 1, 220, -150, 0);
            final CUnit distantTower = game.createUnitSimple(guardTower, 1, 720, 0, 0);
            final var inventory = hero.getInventoryData();
            final CItem horn = game.createItem(War3ID.fromString("sfog"), 0, 0);
            final int slot = inventory.giveItem(game, hero, horn, 0, false);
            game.update();

            assertFalse(tower.getCurrentAttacks().isEmpty());
            assertTrue(hero.order(game, OrderIds.itemuse00 + slot, new AbilityPointTarget(150, 0)));
            for (int tick = 0; tick < 80 && !tower.getCurrentAttacks().isEmpty(); tick++) game.update();
            assertTrue(tower.getCurrentAttacks().isEmpty());
            assertFalse(friendlyTower.getCurrentAttacks().isEmpty());
            assertFalse(rifleman.getCurrentAttacks().isEmpty());
            assertFalse(distantTower.getCurrentAttacks().isEmpty());
            assertTrue(hero.order(game, OrderIds.stop, null));
            game.update();
            assertFalse(tower.getCurrentAttacks().isEmpty());
            assertSame(horn, inventory.getItemInSlot(slot));
        });
    }

    @Test
    void originalSacrificialSkullExpandsBlightAroundTheChosenPoint() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX08.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Udea"), 0, -200, 0, 0);
            final var inventory = hero.getInventoryData();
            final CItem skull = game.createItem(War3ID.fromString("skul"), 0, 0);
            final int slot = inventory.giveItem(game, hero, skull, 0, false);
            final int order = OrderIds.itemuse00 + slot;
            game.update();

            assertFalse(hero.order(game, order, null));
            assertTrue(hero.order(game, order, new AbilityPointTarget(160, 200)));
            final java.util.function.IntPredicate blighted = x -> PathingFlags.isPathingFlag(
                    game.getPathingGrid().getPathing(x, 200), PathingFlags.BLIGHTED);
            for (int tick = 0; tick < 140 && !blighted.test(160); tick++) game.update();
            assertTrue(blighted.test(160));
            assertFalse(blighted.test(416));
            for (int tick = 0; tick < 25; tick++) game.update();
            assertTrue(blighted.test(416));
            assertFalse(blighted.test(576));
            assertNull(inventory.getItemInSlot(slot));
        });
    }

    @Test
    void originalMountHyjalDarkPortalSpawnsTheAuthoredDemonTypesAndCount() throws Exception {
        withFixture("Maps\\Campaign\\NightElf07.w3m", false, (game, projectiles) -> {
            final var portalId = War3ID.fromString("ANdp");
            final var demonTypes = List.of(War3ID.fromString("nbal"), War3ID.fromString("nfel"));
            final CUnit archimonde = game.createUnitSimple(War3ID.fromString("Uwar"), 0, 0, 0, 0);
            archimonde.setDisableAttacks(true);
            archimonde.add(game, game.getAbilityData().createAbility(portalId, game.getHandleIdAllocator().createId()));
            game.update();

            assertTrue(archimonde.order(game, OrderIds.darkportal, new AbilityPointTarget(288, 160)));
            assertFalse(game.getUnitsIncludingPending().stream().anyMatch(unit -> demonTypes.contains(unit.getTypeId())));
            for (int tick = 0; tick < 100; tick++) game.update();
            final var demons = game.getUnitsIncludingPending().stream().filter(unit -> demonTypes.contains(unit.getTypeId())).toList();
            assertTrue(demons.size() >= 3 && demons.size() <= 5);
            assertTrue(demons.stream().allMatch(unit -> unit.getPlayerIndex() == 0 && unit.isSummonedOrWard()
                    && unit.distanceSquaredNoCollision(288, 160) < 300 * 300));
            assertFalse(archimonde.order(game, OrderIds.darkportal, new AbilityPointTarget(288, 160)));
        });
    }

    @Test
    void originalRainOfFireCapsWaveDamageAndKeepsBurningAfterInterruption() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_02.w3x", false, (game, projectiles) -> {
            final War3ID rainOfFire = War3ID.fromString("ANrf");
            final CUnit caster = game.createUnitSimple(War3ID.fromString("Nplh"), 0, -192, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 400, 0, 0);
            final CUnit immune = game.createUnitSimple(War3ID.fromString("edry"), 1, 510, 0, 0);
            final CUnit building = game.createUnitSimple(War3ID.fromString("hbla"), 1, 365, -120, 0);
            final List<CUnit> targets = new ArrayList<>(List.of(ally, building));
            for (int index = 0; index < 4; index++) {
                targets.add(game.createUnitSimple(War3ID.fromString("hfoo"), 1, 335 + index * 48, 86, 0));
            }
            final var participants = new ArrayList<>(targets);
            participants.addAll(List.of(caster, immune));
            for (final CUnit unit : participants) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            game.update();
            assertTrue(caster.order(game, rainOfFire.getValue(), null));
            caster.setMana(caster.getMaximumMana());
            assertTrue(caster.order(game, OrderIds.rainoffire, new AbilityPointTarget(400, 0)));
            for (int tick = 0; tick < 100 && ally.getLife() == ally.getMaximumLife(); tick++) game.update();

            final float cappedWaveDamage = 125f / targets.size();
            assertEquals(ally.getMaximumLife() - cappedWaveDamage, ally.getLife(), 0.01f);
            assertEquals(building.getMaximumLife() - cappedWaveDamage * 0.5f, building.getLife(), 0.01f);
            assertEquals(immune.getMaximumLife(), immune.getLife(), 0.01f);
            assertTrue(caster.order(game, OrderIds.stop, null));
            for (int tick = 0; tick < 100; tick++) game.update();

            assertEquals(ally.getMaximumLife() - cappedWaveDamage - 15, ally.getLife(), 0.01f);
            assertEquals(building.getMaximumLife() - cappedWaveDamage * 0.5f - 7.5f, building.getLife(), 0.01f);
            assertEquals(immune.getMaximumLife(), immune.getLife(), 0.01f);
        });
    }

    @Test
    void originalGoblinSapperCanDestroyTheAuthoredTunnelSupportColumn() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01_02.w3x", false, (game, projectiles) -> {
            final CUnit sapper = game.createUnitSimple(War3ID.fromString("ngsp"), 0, 0, 0, 0);
            final War3ID columnType = War3ID.fromString("BTsc");
            final CDestructable support = game.internalCreateDestructable(columnType, 92, 0, null, null);
            final CDestructable distant = game.internalCreateDestructable(columnType, 420, 0, null, null);
            game.update();
            assertEquals(50, support.getLife());
            assertTrue(sapper.order(game, OrderIds.selfdestruct, support));
            for (int tick = 0; tick < 60 && !support.isDead(); tick++) game.update();
            assertTrue(sapper.isDead());
            assertTrue(support.isDead());
            assertEquals(50, distant.getLife());
        });
    }

    @Test
    void originalGoblinMineUsesChargesArmingAndBothDamageRadii() throws Exception {
        withFixture("Maps\\Campaign\\NightElf05.w3m", false, (game, projectiles) -> {
            final CUnit tyrande = game.createUnitSimple(War3ID.fromString("Emoo"), 0, 0, 0, 0);
            final CUnit near = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 432, 0, 0);
            final CUnit outer = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 608, 0, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 800, 0, 0);
            final CUnit friendly = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 408, 104, 0);
            for (final CUnit unit : List.of(tyrande, near, outer, distant, friendly)) unit.setDisableAttacks(true);
            final var nearDamage = new ArrayList<Float>();
            final var outerDamage = new ArrayList<Float>();
            final var otherDamage = new ArrayList<Float>();
            near.addDamageTakenListener((sim, source, target, attack, ranged, type, damage, bonus, actual) -> nearDamage.add(damage));
            outer.addDamageTakenListener((sim, source, target, attack, ranged, type, damage, bonus, actual) -> outerDamage.add(damage));
            for (final CUnit unit : List.of(distant, friendly)) {
                unit.addDamageTakenListener((sim, source, target, attack, ranged, type, damage, bonus, actual) -> otherDamage.add(damage));
            }
            game.update();
            final CItem mines = game.createItem(War3ID.fromString("gobm"), 0, 0);
            final int charges = mines.getCharges();
            final int slot = tyrande.getInventoryData().giveItem(game, tyrande, mines, 0, false);
            final War3ID mineType = War3ID.fromString("nglm");
            assertTrue(tyrande.order(game, OrderIds.itemuse00 + slot, new AbilityPointTarget(320, 0)));
            CUnit mine = null;
            for (int step = 0; step < 140 && mine == null; step++) {
                game.update();
                mine = game.getUnitsIncludingPending().stream().filter(unit -> mineType.equals(unit.getTypeId()))
                        .findFirst().orElse(null);
            }
            assertNotNull(mine);
            assertEquals(tyrande.getPlayerIndex(), mine.getPlayerIndex());
            assertEquals(charges - 1, mines.getCharges());
            final int ticksPerSecond = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int step = 0; step < 9 * ticksPerSecond; step++) game.update();
            assertFalse(mine.isDead());
            assertTrue(mine.isInvisibleTo(game, 1));
            assertTrue(nearDamage.isEmpty());
            for (int step = 0; step < 2 * ticksPerSecond; step++) game.update();
            assertTrue(mine.isDead());
            assertEquals(List.of(700f), nearDamage);
            assertEquals(List.of(300f), outerDamage);
            assertTrue(otherDamage.isEmpty());
        });
    }

    @Test
    void originalProtectionScrollAndRegenerationRingApplyAndRemoveTheirBonuses() throws Exception {
        withFixture("Maps\\Campaign\\Human02.w3m", false, (game, projectiles) -> {
            final CUnit arthas = game.createUnitSimple(War3ID.fromString("Hart"), 0, 0, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 220, 0, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 840, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 180, 0, 0);
            for (final CUnit unit : List.of(arthas, ally, distant, enemy)) unit.setDisableAttacks(true);
            game.update();
            final float heroArmor = arthas.getDefense();
            final float allyArmor = ally.getDefense();
            final float distantArmor = distant.getDefense();
            final float enemyArmor = enemy.getDefense();
            final var inventory = arthas.getInventoryData();
            final CItem scroll = game.createItem(War3ID.fromString("spro"), 0, 0);
            final int scrollSlot = inventory.giveItem(game, arthas, scroll, 0, false);
            assertTrue(arthas.order(game, OrderIds.itemuse00 + scrollSlot, null));
            for (int tick = 0; tick < 36 && ally.getDefense() < allyArmor + 1; tick++) game.update();
            assertEquals(heroArmor + 2, arthas.getDefense(), 0.01f);
            assertEquals(allyArmor + 2, ally.getDefense(), 0.01f);
            assertEquals(enemyArmor, enemy.getDefense(), 0.01f);
            assertEquals(distantArmor, distant.getDefense(), 0.01f);
            assertNull(inventory.getItemInSlot(scrollSlot));
            ally.dispelBuffs(game);
            assertEquals(allyArmor, ally.getDefense(), 0.01f);
            for (int tick = 0; tick < 601; tick++) game.update();
            assertEquals(heroArmor, arthas.getDefense(), 0.01f);

            final CItem ring = game.createItem(War3ID.fromString("rlif"), 0, 0);
            final float baseRegen = arthas.getLifeRegenBonus();
            final int ringSlot = inventory.giveItem(game, arthas, ring, 0, false);
            assertEquals(baseRegen + 2, arthas.getLifeRegenBonus(), 0.01f);
            arthas.setLife(game, 100);
            final int second = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < second; tick++) game.update();
            final float withRing = arthas.getLife() - 100;
            inventory.dropItem(game, arthas, ringSlot, 0, 100, false);
            assertEquals(baseRegen, arthas.getLifeRegenBonus(), 0.01f);
            arthas.setLife(game, 100);
            for (int tick = 0; tick < second; tick++) game.update();
            assertEquals(2, withRing - (arthas.getLife() - 100), 0.01f);
        });
    }

    @Test
    void originalBarrowDeepsItemsHealTheEscortAndFireTheFurbolgSummonEvent() throws Exception {
        withFixture("Maps\\Campaign\\NightElf05.w3m", false, (game, projectiles) -> {
            final CUnit tyrande = game.createUnitSimple(War3ID.fromString("Emoo"), 0, 0, 0, 0);
            final CUnit escort = game.createUnitSimple(War3ID.fromString("edry"), 0, 300, 30, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("earc"), 1, 300, -90, 0);
            final CUnit machine = game.createUnitSimple(War3ID.fromString("ocat"), 0, 250, 175, 0);
            final CUnit distant = game.createUnitSimple(War3ID.fromString("earc"), 0, 850, 0, 0);
            for (final CUnit unit : List.of(tyrande, escort, enemy, machine, distant)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
                unit.setLife(game, 90);
            }
            final CItem scroll = game.createItem(War3ID.fromString("shea"), 0, 0);
            final CItem talisman = game.createItem(War3ID.fromString("totw"), 0, 0);
            final var inventory = tyrande.getInventoryData();
            final int healingSlot = inventory.giveItem(game, tyrande, scroll, 0, false);
            final int talismanSlot = inventory.giveItem(game, tyrande, talisman, 1, false);
            game.update();
            assertTrue(tyrande.order(game, OrderIds.itemuse00 + healingSlot, null));
            for (int tick = 0; tick < 32 && escort.getLife() < 235; tick++) game.update();
            assertEquals(240, escort.getLife(), 0.01f);
            assertEquals(240, tyrande.getLife(), 1f);
            assertEquals(90, enemy.getLife(), 0.01f);
            assertEquals(90, machine.getLife(), 0.01f);
            assertEquals(90, distant.getLife(), 0.01f);
            assertNull(inventory.getItemInSlot(healingSlot));

            final List<CUnit> summons = new ArrayList<>();
            final var player = game.getPlayer(0);
            player.addEvent(new CPlayerEvent(game.getGlobalScope(), player, new Trigger(),
                    JassGameEventsWar3.EVENT_PLAYER_UNIT_SUMMON, null) {
                @Override
                public void fire(final CUnit unit, final TriggerExecutionScope scope) {
                    assertSame(tyrande, unit);
                    summons.add(((CommonTriggerExecutionScope) scope).getSummonedUnit());
                }
            });
            assertTrue(tyrande.order(game, OrderIds.itemuse00 + talismanSlot, null));
            for (int tick = 0; tick < 32 && summons.isEmpty(); tick++) game.update();
            assertEquals(1, summons.size());
            final CUnit furbolg = summons.get(0);
            assertEquals(War3ID.fromString("nfrl"), furbolg.getTypeId());
            assertEquals(0, furbolg.getPlayerIndex());
            assertTrue(furbolg.getClassifications().contains(CUnitClassification.SUMMONED));
        });
    }

    @Test
    void originalMortarFlareUsesOneResearchedChargeAndRevealsAnInvisibleTargetAfterImpact() throws Exception {
        withFixture("Maps\\Campaign\\Human04.w3m", false, (game, projectiles) -> {
            final var mortar = game.createUnitSimple(War3ID.fromString("hmtm"), 0, -820, -820, 0);
            final var shade = game.createUnitSimple(War3ID.fromString("ushd"), 1, 700, 700, 0);
            final var point = new AbilityPointTarget(shade.getX(), shade.getY());
            mortar.setDisableAttacks(true);
            game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
            assertFalse(mortar.order(game, OrderIds.flare, point));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhfl"), 1);
            game.update();
            final var flare = mortar.getFirstAbilityOfType(CAbilityFlare.class);
            assertNotNull(flare);
            assertEquals(1, flare.getUsesRemaining());
            assertTrue(mortar.order(game, OrderIds.flare, point));
            for (int tick = 0; tick < 30 && flare.getUsesRemaining() > 0; tick++) game.update();
            assertEquals(0, flare.getUsesRemaining());
            assertTrue(shade.isInvisibleTo(game, 0), "the flare has not impacted at launch");
            for (int tick = 0; tick < 25 && !shade.isVisible(game, 0); tick++) game.update();
            assertTrue(shade.isVisible(game, 0));
            assertFalse(shade.isVisible(game, 2));
            assertFalse(mortar.order(game, OrderIds.flare, point));
            assertEquals(-820, mortar.getX());
            assertEquals(-820, mortar.getY());
            for (int tick = 0; tick <= Math.ceil(15.1f / WarsmashConstants.SIMULATION_STEP_TIME); tick++) game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
        });
    }

    @Test
    void originalOrcFarSightRevealsADistantShadeUntilTheSpellExpires() throws Exception {
        withFixture("Maps\\Campaign\\Orc03.w3m", false, (game, projectiles) -> {
            final var thrall = game.createUnitSimple(War3ID.fromString("Ofar"), 0, -820, -820, 0);
            final var shade = game.createUnitSimple(War3ID.fromString("ushd"), 1, 700, 700, 0);
            final var point = new AbilityPointTarget(shade.getX(), shade.getY());
            thrall.setDisableAttacks(true);
            game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
            assertFalse(shade.isVisible(game, 2));
            final War3ID farSight = War3ID.fromString("AOfs");
            assertTrue(thrall.order(game, farSight.getValue(), null));
            final var ability = thrall.getFirstAbilityOfType(CAbilityFarSight.class);
            assertNotNull(ability);
            assertEquals(8, ability.getDuration());
            thrall.setMana(thrall.getMaximumMana());
            assertTrue(thrall.order(game, OrderIds.farsight, point));
            for (int tick = 0; tick < 60 && !shade.isVisible(game, 0); tick++) game.update();
            assertTrue(shade.isVisible(game, 0));
            assertFalse(shade.isVisible(game, 2));
            assertEquals(-820, thrall.getX());
            assertEquals(-820, thrall.getY());
            final int durationTicks = Math.round(ability.getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick <= durationTicks + 1; tick++) game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
        });
    }

    @Test
    void originalHardenedSkinAppliesAfterResearchWithTheAttackDamageFloor() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX08.w3x", false, (game, projectiles) -> {
            final CUnit giant = game.createUnitSimple(War3ID.fromString("emtg"), 0, 0, 0, 0);
            final CUnit hydra = game.createUnitSimple(War3ID.fromString("nhyc"), 0, -240, 0, 0);
            final CUnit attacker = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 184, 0, 0);
            giant.setDisableAttacks(true);
            hydra.setDisableAttacks(true);
            attacker.setDisableAttacks(true);
            game.update();
            final float withoutResearch = giant.damage(game, attacker, true, false, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 72);
            final float hydraWithoutResearch = hydra.damage(game, attacker, true, false, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 72);
            giant.setLife(game, giant.getMaximumLife());
            game.getPlayer(0).addTechResearched(game, War3ID.fromString("Rehs"), 1);
            game.update();
            final float melee = giant.damage(game, attacker, true, false, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 72);
            final float ranged = giant.damage(game, attacker, true, true, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 72);
            assertEquals(withoutResearch - 15, melee, 0.001f);
            assertEquals(melee, ranged, 0.001f);
            assertEquals(hydraWithoutResearch - 12, hydra.damage(game, attacker, true, false, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 72), 0.001f);
            final float weakAttack = giant.damage(game, attacker, true, false, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 10);
            assertEquals(3, weakAttack, 0.001f);
            final float spell = giant.damage(game, attacker, false, false, CAttackType.NORMAL,
                    CDamageType.NORMAL, "WHOKNOWS", 72);
            assertEquals(withoutResearch, spell, 0.001f);
        });
    }

    @Test
    void originalCrystalBallRevealsInvisibilityAndLumberBundleGrantsItsStockAmount() throws Exception {
        withFixture("Maps\\Campaign\\Orc04.w3m", false, (game, projectiles) -> {
            final CUnit grom = game.createUnitSimple(War3ID.fromString("Ogrh"), 0, -820, -820, 0);
            final CUnit shade = game.createUnitSimple(War3ID.fromString("ushd"), 1, 700, 700, 0);
            grom.setDisableAttacks(true);
            game.update();
            final CItem crystalBall = game.createItem(War3ID.fromString("crys"), grom.getX(), grom.getY());
            final var inventory = grom.getInventoryData();
            final int slot = inventory.giveItem(game, grom, crystalBall, 0, false);
            assertTrue(shade.isInvisibleTo(game, 0));
            final int order = OrderIds.itemuse00 + slot;
            final var spot = new AbilityPointTarget(shade.getX(), shade.getY());
            assertTrue(grom.order(game, order, spot));
            for (int tick = 0; tick < 60 && !shade.isVisible(game, 0); tick++) game.update();
            assertTrue(shade.isVisible(game, 0));
            assertFalse(shade.isVisible(game, 2));
            assertSame(crystalBall, inventory.getItemInSlot(slot));
            assertFalse(grom.order(game, order, spot));
            assertEquals(-820, grom.getX());
            for (int tick = 0; tick <= Math.ceil(10.1f / WarsmashConstants.SIMULATION_STEP_TIME); tick++) game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
        });
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX01.w3x", false, (game, projectiles) -> {
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hpal"), 0, 0, 0, 0);
            final var owner = game.getPlayer(hero.getPlayerIndex());
            final var inventory = hero.getInventoryData();
            final int initialLumber = owner.getLumber();
            final int initialGold = owner.getGold();
            final CItem lumber = game.createItem(War3ID.fromString("lmbr"), 0, 0);
            assertEquals(-1, inventory.giveItem(game, hero, lumber, 0, false));
            assertTrue(lumber.isDead());
            assertEquals(initialLumber + 250, owner.getLumber());
            assertEquals(initialGold, owner.getGold());
        });
    }

    @Test
    void frozenThroneWardenShadowStrikeHitsAnAirUnitAndRestoresItsSpeedAfterPoison() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var warden = game.createUnitSimple(War3ID.fromString("Ewrd"), 0, 0, 0, 0);
            final var enemy = game.createUnitSimple(War3ID.fromString("hgry"), 1, 240, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, -240, 0, 0);
            warden.setDisableAttacks(true);
            enemy.setDisableAttacks(true);
            ally.setDisableAttacks(true);
            final int originalSpeed = enemy.getSpeed();
            final List<Float> damageTaken = new ArrayList<>();
            enemy.addDamageTakenListener((simulation, attacker, target, isAttack, isRanged, type, damage, bonus, actual) -> {
                assertSame(warden, attacker);
                assertEquals(CDamageType.SHADOW_STRIKE, type);
                damageTaken.add(damage);
            });
            final War3ID spell = War3ID.fromString("AEsh");
            game.update();
            assertTrue(warden.order(game, spell.getValue(), null));
            assertEquals(1, warden.getAbilityLevel(spell));
            warden.setMana(warden.getMaximumMana());
            final float startingMana = warden.getMana();
            assertFalse(warden.order(game, OrderIds.shadowstrike, ally));
            assertEquals(startingMana, warden.getMana());
            assertTrue(warden.order(game, OrderIds.shadowstrike, enemy));
            for (int tick = 0; tick < 48 && damageTaken.isEmpty(); tick++) game.update();
            assertEquals(List.of(75.0f), damageTaken);
            assertEquals(originalSpeed / 2, enemy.getSpeed());
            for (int tick = 0; tick < 330; tick++) game.update();
            assertEquals(List.of(75.0f, 15.0f, 15.0f, 15.0f, 15.0f, 15.0f), damageTaken);
            assertEquals(originalSpeed, enemy.getSpeed());
            assertEquals(0, enemy.getAbilityLevel(War3ID.fromString("BEsh")));
        });
    }

    @Test
    void retailFountainRestoresWoundedUnitsInsideItsOasis() throws Exception {
        withFixture(true, (game, projectiles) -> {
            final War3ID wispType = War3ID.fromString("ewsp");
            final var fountain = game.createUnitSimple(War3ID.fromString("nfoh"), 2, 0, 0, 0);
            final var nearby = game.createUnitSimple(wispType, 0, -256, 0, 0);
            final var distant = game.createUnitSimple(wispType, 0, 768, 0, 0);
            assertTrue(nearby.getCurrentAttacks().isEmpty());
            assertTrue(fountain.getAbilityLevel(War3ID.fromString("ACnr")) > 0);
            nearby.setLife(game, nearby.getMaximumLife() / 2);
            distant.setLife(game, distant.getMaximumLife() / 2);
            for (int tick = 0; tick < 220; tick++) game.update();
            assertTrue(nearby.getLife() - distant.getLife() > 12,
                    "the oasis must heal beyond natural regeneration");
        });
    }

    @Test
    void researchedEnsnareTemporarilyGroundsAirUnitsForGroundWeapons() throws Exception {
        withFixture(true, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var raider = game.createUnitSimple(War3ID.fromString("orai"), 0, -384, 0, 0);
            final var rider = game.createUnitSimple(War3ID.fromString("hgry"), 1, 0, 0, 0);
            raider.setDisableAttacks(true);
            rider.setDisableAttacks(true);
            final var ability = raider.getFirstAbilityOfType(CAbilityEnsnare.class);
            assertNotNull(ability);
            game.update();
            final var groundWeapon = raider.getUnitSpecificAttacks().get(0).getTargetsAllowed();
            final float height = rider.getFlyHeight();
            assertTrue(height > 0);
            assertFalse(rider.canBeTargetedBy(game, raider, groundWeapon));
            assertFalse(raider.order(game, OrderIds.ensnare, rider));

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Roen"), 1);
            game.update();
            assertTrue(ability.isRequirementsMet(game, raider));
            assertTrue(raider.order(game, OrderIds.ensnare, rider));
            for (int tick = 0; tick < 80 && !rider.isUnitType(CUnitTypeJass.SNARED); tick++) game.update();
            assertTrue(rider.isUnitType(CUnitTypeJass.SNARED));
            assertTrue(rider.isMovementDisabled());
            assertEquals(0, rider.getFlyHeight());
            assertTrue(rider.canBeTargetedBy(game, raider, groundWeapon));

            final int durationTicks = (int) (ability.getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick <= durationTicks + 1; tick++) game.update();
            assertFalse(rider.isUnitType(CUnitTypeJass.SNARED));
            assertFalse(rider.isMovementDisabled());
            assertEquals(height, rider.getFlyHeight());
            assertFalse(rider.canBeTargetedBy(game, raider, groundWeapon));
        });
    }

    @Test
    void originalWebAutocastGroundsFlyersForWeaponsWhilePreservingSpellTargets() throws Exception {
        withFixture("Maps\\Campaign\\Undead04.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var fiend = game.createUnitSimple(War3ID.fromString("ucry"), 0, -350, 0, 0);
            final var flyer = game.createUnitSimple(War3ID.fromString("hgry"), 1, 0, 0, 0);
            final var footman = game.createUnitSimple(War3ID.fromString("hfoo"), 0, -70, 0, 0);
            final var ground = game.createUnitSimple(War3ID.fromString("hfoo"), 1, -200, 100, 0);
            for (final var unit : List.of(fiend, flyer, footman, ground)) unit.setDisableAttacks(true);
            game.update();
            final var web = fiend.getFirstAbilityOfType(CAbilityWeb.class);
            assertNotNull(web);
            final float flightHeight = flyer.getFlyHeight();
            final var footmanAttack = footman.getUnitSpecificAttacks().get(0);
            final var groundWeapon = footmanAttack.getTargetsAllowed();
            final var air = EnumSet.of(CTargetType.AIR, CTargetType.ENEMIES);
            final var groundSpell = EnumSet.of(CTargetType.GROUND, CTargetType.ENEMIES);
            assertFalse(fiend.order(game, OrderIds.web, flyer));
            assertFalse(flyer.canBeAttackedBy(game, footman, groundWeapon));

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Ruwb"), 1);
            game.update();
            assertFalse(web.isDisabled());
            assertFalse(fiend.order(game, OrderIds.web, ground));
            fiend.order(game, OrderIds.webon, null);
            assertTrue(web.isAutoCastOn());
            fiend.order(game, OrderIds.holdposition, null);
            for (int tick = 0; tick < 55 && !flyer.isWebbed(); tick++) game.update();
            assertTrue(flyer.isWebbed(), "The Crypt Fiend did not autocast Web at the flyer");
            assertTrue(flyer.isMovementDisabled());
            assertEquals(128, footmanAttack.getRangeAgainst(flyer));
            assertFalse(ground.isWebbed());
            assertTrue(flyer.canBeAttackedBy(game, footman, groundWeapon));
            assertFalse(flyer.canBeAttackedBy(game, footman, air));
            assertFalse(flyer.canBeTargetedBy(game, fiend, groundSpell));
            assertTrue(flyer.canBeTargetedBy(game, fiend, air));

            fiend.order(game, OrderIds.weboff, null);
            assertFalse(web.isAutoCastOn());
            footman.setDisableAttacks(false);
            final float beforeAttack = flyer.getLife();
            assertTrue(footman.order(game, OrderIds.attack, flyer));
            for (int tick = 0; tick < 40 && flyer.getLife() >= beforeAttack; tick++) game.update();
            assertTrue(flyer.getLife() < beforeAttack);
            for (int tick = 0; tick < 16 && flyer.getFlyHeight() > 0; tick++) game.update();
            assertEquals(0, flyer.getFlyHeight());
            final int durationTicks = Math.round(web.getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick <= durationTicks; tick++) game.update();
            assertFalse(flyer.isWebbed());
            assertFalse(flyer.isMovementDisabled());
            assertEquals(flightHeight, flyer.getFlyHeight());
            assertFalse(flyer.canBeAttackedBy(game, footman, groundWeapon));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void originalNightElfUltimatePulsesAtItsAuthoredIntervalAndStopsWhenInterrupted(final boolean tranquility)
            throws Exception {
        withFixture("Maps\\Campaign\\NightElf03.w3m", false, (game, projectiles) -> {
            final var caster = game.createUnitSimple(War3ID.fromString(tranquility ? "Efur" : "Etyr"), 0, 0, 0, 0);
            final var ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 240, 0, 0);
            final var foe = game.createUnitSimple(War3ID.fromString("hfoo"), 1, -240, 0, 0);
            final var air = game.createUnitSimple(War3ID.fromString("hgry"), 1, 0, 240, 0);
            final var building = game.createUnitSimple(War3ID.fromString("hbar"), tranquility ? 0 : 1, 0, -350, 0);
            final var distance = game.createUnitSimple(War3ID.fromString("hfoo"), tranquility ? 0 : 1, 1375, 0, 0);
            final var units = List.of(caster, ally, foe, air, building, distance);
            for (final CUnit unit : units) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            game.update();
            caster.getHeroData().setHeroLevel(game, caster, 10, false);
            caster.setLifeRegenStrengthBonus(0);
            for (final CUnit unit : units) unit.setLife(game, unit.getMaximumLife() - 120);
            final var starts = units.stream().map(CUnit::getLife).toList();
            final var ability = War3ID.fromString(tranquility ? "AEtq" : "AEsf");
            assertTrue(caster.order(game, ability.getValue(), null));
            assertEquals(1, caster.getAbilityLevel(ability));
            final var spell = caster.getFirstAbilityOfType(CAbilityNightElfChannel.class);
            assertNotNull(spell);
            assertTrue(caster.order(game, tranquility ? OrderIds.tranquility : OrderIds.starfall, null));
            for (int tick = 0; tick < 45 && spell.getCooldownRemaining(game, caster) <= 0; tick++) game.update();
            assertTrue(spell.getCooldownRemaining(game, caster) > 0);
            final int interval = Math.round((tranquility ? 1f : 1.5f) / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < interval - 1; tick++) game.update();
            for (int index = 0; index < units.size(); index++) {
                assertEquals(starts.get(index), units.get(index).getLife(), 0.001f);
            }
            game.update();
            final float[] changes = tranquility ? new float[] {20, 20, 0, 0, 0, 0}
                    : new float[] {0, 0, -50, -50, -17.5f, 0};
            for (int index = 0; index < units.size(); index++) {
                assertEquals(starts.get(index) + changes[index], units.get(index).getLife(), 0.001f,
                        units.get(index).getTypeId().toString());
            }
            assertTrue(caster.order(game, OrderIds.stop, null));
            for (int tick = 0; tick < interval * 2; tick++) game.update();
            for (int index = 0; index < units.size(); index++) {
                assertEquals(starts.get(index) + changes[index], units.get(index).getLife(), 0.001f);
            }
        });
    }

    @Test
    void originalDemonHeroFingerOfDeathKillsAndRainOfChaosSummonsTwoInfernals() throws Exception {
        withFixture("Maps\\Campaign\\NightElf07.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var demon = game.createUnitSimple(War3ID.fromString("Uwar"), 0, 0, 0, 0);
            final var victim = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 320, 0, 0);
            final var knight = game.createUnitSimple(War3ID.fromString("hkni"), 1, 350, -150, 0);
            final var hero = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 300, 150, 0);
            for (final CUnit unit : List.of(demon, victim, knight, hero)) {
                unit.setDisableAttacks(true);
                unit.setLifeRegenBonus(-unit.getUnitType().getLifeRegen());
            }
            game.update();
            demon.getHeroData().setHeroLevel(game, demon, 10, false);
            final var finger = War3ID.fromString("ANfd");
            final var rain = War3ID.fromString("ANrc");
            assertTrue(demon.order(game, finger.getValue(), null));
            assertTrue(demon.order(game, rain.getValue(), null));
            assertEquals(1, demon.getAbilityLevel(finger));
            assertEquals(1, demon.getAbilityLevel(rain));
            final var fingerSpell = demon.getFirstAbilityOfType(CAbilityFingerOfDeath.class);
            assertNotNull(fingerSpell);
            final var receiver = new ExternStringMsgTargetCheckReceiver<CWidget>();
            fingerSpell.checkCanTarget(game, demon, OrderIds.fingerofdeath, victim, receiver);
            assertSame(victim, receiver.getTarget(), () -> "Finger of Death rejected the footman with targets "
                    + fingerSpell.getTargetsAllowed() + " and " + receiver.getExternStringKey());
            assertFalse(demon.order(game, OrderIds.fingerofdeath, hero));
            assertTrue(demon.order(game, OrderIds.fingerofdeath, victim));
            for (int tick = 0; tick < 45 && !victim.isDead(); tick++) game.update();
            assertTrue(victim.isDead());
            assertTrue(victim.isHidden(), "A lethal Finger of Death must destroy the corpse");
            assertEquals(knight.getMaximumLife(), knight.getLife());

            final var point = new AbilityPointTarget(700, 500);
            final var spell = demon.getFirstAbilityOfType(CAbilityRainOfChaos.class);
            assertNotNull(spell);
            assertTrue(demon.order(game, OrderIds.rainofchaos, point));
            for (int tick = 0; tick < 45 && spell.getCooldownRemaining(game, demon) <= 0; tick++) game.update();
            assertTrue(spell.getCooldownRemaining(game, demon) > 0);
            final War3ID infernal = War3ID.fromString("ninf");
            for (int tick = 0; tick < 16; tick++) game.update();
            assertFalse(game.getUnits().stream().anyMatch(unit -> infernal.equals(unit.getTypeId())));
            for (int tick = 0; tick < 14; tick++) game.update();
            final var summoned = game.getUnits().stream().filter(unit -> infernal.equals(unit.getTypeId())).toList();
            assertEquals(2, summoned.size());
            for (final CUnit unit : summoned) {
                assertEquals(demon.getPlayerIndex(), unit.getPlayerIndex());
                assertTrue(unit.isUnitType(CUnitTypeJass.SUMMONED));
                assertTrue(unit.distanceSquaredNoCollision(point) <= 960 * 960);
            }
        });
    }

    @Test
    void originalSylvanasBlackArrowMarksAtImpactAndRaisesTheAuthoredDarkMinion() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX01.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit sylvanas = game.createUnitSimple(War3ID.fromString("Usyl"), 0, -384, 0, 0);
            final CUnit target = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 0, 0);
            target.setDisableAttacks(true);
            final var type = game.getAbilityData().getAbilityType(War3ID.fromString("ANba"));
            final var arrow = (CAbilityBlackArrow) type.createAbility(game.getHandleIdAllocator().createId());
            sylvanas.add(game, arrow);
            game.update();
            sylvanas.setMana(100);
            sylvanas.setManaRegen(0);
            sylvanas.setManaRegenIntelligenceBonus(0);
            assertTrue(sylvanas.order(game, OrderIds.blackarrow, target));
            for (int tick = 0; tick < 60 && projectiles.isEmpty(); tick++) game.update();
            assertEquals(1, projectiles.size());
            assertEquals(94, sylvanas.getMana());
            assertTrue(target.getAbilities().stream().noneMatch(ability -> "BNba".equals(ability.getAlias().toString())));
            finish(game, projectiles.get(0));
            assertTrue(target.getAbilities().stream().anyMatch(ability -> "BNba".equals(ability.getAlias().toString())));
            target.kill(game);

            final CUnit minion = game.getUnitsIncludingPending().stream()
                    .filter(unit -> "ndr1".equals(unit.getTypeId().toString())).findFirst().orElseThrow();
            assertEquals(sylvanas.getPlayerIndex(), minion.getPlayerIndex());
            assertTrue(minion.isSummonedOrWard());
            assertFalse(target.isRaisable());
        });
    }

    @Test
    void searingArrowsEmpowersOnlyEligibleShotsAndChargesManaAtLaunch() throws Exception {
        withFixture(true, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var archer = game.createUnitSimple(War3ID.fromString("Emoo"), 0, -384, 0, 0);
            final var opponent = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 0, 0);
            opponent.setDisableAttacks(true);
            final var type = game.getAbilityData().getAbilityType(War3ID.fromString("AHfa"));
            final var ability = assertInstanceOf(CAbilitySearingArrows.class,
                    type.createAbility(game.getHandleIdAllocator().createId()));
            archer.add(game, ability);
            game.update();
            final var normalShot = assertInstanceOf(CUnitAttackMissile.class, archer.getCurrentAttacks().get(0));
            archer.setMana(100);
            assertFalse(ability.isDisabled());
            assertEquals(0f, ability.getCooldownRemaining(game, archer));
            assertTrue(opponent.canBeTargetedBy(game, archer, normalShot.getTargetsAllowed()), "the ordinary arrow must have a valid target");
            final var target = new CWidgetAbilityTargetCheckReceiver();
            ability.checkCanTarget(game, archer, OrderIds.flamingarrowstarg, opponent, target);
            assertSame(opponent, target.getTarget());

            assertFalse(ability.tryLaunch(game, archer, OrderIds.attack, normalShot, opponent, 30,
                    CUnitAttackListener.DO_NOTHING));
            assertTrue(ability.tryLaunch(game, archer, OrderIds.flamingarrowstarg, normalShot, opponent, 30,
                    CUnitAttackListener.DO_NOTHING));
            assertEquals(40f, projectiles.get(0).getDamage());
            assertEquals(100 - ability.getManaCost(), archer.getMana());

            ability.checkBeforeQueue(game, archer, OrderIds.flamingarrows, null);
            assertTrue(ability.isAutoCastOn());
            assertTrue(ability.tryLaunch(game, archer, OrderIds.attack, normalShot, opponent, 30,
                    CUnitAttackListener.DO_NOTHING));
            assertEquals(100 - 2 * ability.getManaCost(), archer.getMana());

            archer.setMana(0);
            assertFalse(ability.tryLaunch(game, archer, OrderIds.attack, normalShot, opponent, 30,
                    CUnitAttackListener.DO_NOTHING));
            assertEquals(2, projectiles.size());

            archer.setMana(100);
            ability.checkBeforeQueue(game, archer, OrderIds.unflamingarrows, null);
            assertFalse(ability.isAutoCastOn());
            assertTrue(archer.order(game, OrderIds.flamingarrowstarg, opponent));
            for (int tick = 0; tick < 80 && projectiles.size() == 2; tick++) game.update();
            assertEquals(3, projectiles.size(), "a manual Searing Arrows order must launch an attack");
            assertTrue(archer.getMana() < 95);

            assertTrue(archer.order(game, OrderIds.holdposition, null));
            type.setLevel(game, archer, ability, 2);
            opponent.setPaused(true);
            opponent.setPointAndCheckUnstuck(300, 0, game);
            assertFalse(archer.canReach(opponent, normalShot.getRange()));
            assertTrue(archer.canReach(opponent, ability.getCastRange()));
            final float x = archer.getX();
            assertTrue(archer.order(game, OrderIds.flamingarrowstarg, opponent));
            for (int tick = 0; tick < 100 && projectiles.size() == 3; tick++) game.update();
            assertEquals(4, projectiles.size());
            assertEquals(x, archer.getX(), 0.001f, "rank two can launch outside ordinary weapon range");
        });
    }

    @Test
    void movementKeepsTheFullStepWhenItCrossesAPathWaypoint() throws Exception {
        withFixture(false, (game, projectiles) -> {
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, 0, 0);
            game.update();
            final float step = footman.getSpeed() * WarsmashConstants.SIMULATION_STEP_TIME;
            final var move = footman.getMoveBehavior().reset(OrderIds.move, new AbilityPointTarget(96, 0));
            move.pathFound(new ArrayList<>(List.of(new Point2D.Float(step / 2, 0), new Point2D.Float(96, 0))), game);
            move.update(game);
            assertEquals(step, footman.getX(), 0.001f);
            assertEquals(0, footman.getY(), 0.001f);
        });
    }

    @Test
    void smoothingKeepsAPathAroundAnObstacleBeyondTheShortcutMidpoint() throws Exception {
        withFixture(false, (game, projectiles) -> {
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, 0, 0);
            game.update();
            final var grid = game.getPathingGrid();
            grid.setCellPathing(grid.getCellX(272), grid.getCellY(0), (short) 2);
            assertFalse(grid.isPathable(272, 0, footman.getMovementType(), footman.getUnitType().getCollisionSize()));
            final var waypoints = new ArrayList<>(List.of(new Point2D.Float(128, 0), new Point2D.Float(192, 64),
                    new Point2D.Float(320, 64), new Point2D.Float(384, 0)));
            final var move = footman.getMoveBehavior().reset(OrderIds.move, new AbilityPointTarget(384, 0));
            move.update(game);
            move.pathFound(waypoints, game);

            Point2D.Float previous = new Point2D.Float(0, 0);
            for (final Point2D.Float next : waypoints) {
                final int samples = Math.max(1, (int) Math.ceil(previous.distance(next) / 8));
                for (int sample = 1; sample <= samples; sample++) {
                    final float ratio = (float) sample / samples;
                    final float x = previous.x + (next.x - previous.x) * ratio;
                    final float y = previous.y + (next.y - previous.y) * ratio;
                    assertTrue(grid.isPathable(x, y, footman.getMovementType(), footman.getUnitType().getCollisionSize()),
                            "shortcut crosses blocked terrain at " + x + "," + y + " along " + waypoints);
                }
                previous = next;
            }
        });
    }

    @Test
    void aReplacementMoveOrderDiscardsTheOldPendingRoute() throws Exception {
        withFixture(false, (game, projectiles) -> {
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, 0, 0);
            game.update();
            assertTrue(footman.order(game, OrderIds.move, new AbilityPointTarget(768, 0)));
            footman.getMoveBehavior().update(game);
            assertTrue(footman.order(game, OrderIds.move, new AbilityPointTarget(-256, 0)));
            for (int tick = 0; tick < 30; tick++) {
                game.update();
                assertTrue(footman.getX() <= 0.001f, "the unit followed the replaced eastbound route");
            }
            assertTrue(footman.getX() < -100, "the unit must follow the new westbound route");
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void sentinelRevealsShadeFromItsTreeAndUsesOneOwl(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            final var type = War3ID.fromString("esen");
            final var huntress = game.createUnitSimple(type, 0, -600, 0, 0);
            final var second = game.createUnitSimple(type, 0, -600, 150, 0);
            huntress.setDisableAttacks(true);
            second.setDisableAttacks(true);
            final var shade = game.createUnitSimple(War3ID.fromString("ushd"), 1, 400, 0, 0);
            final var tree = game.internalCreateDestructable(War3ID.fromString("LTlt"), 0, 0, null, null);
            final var sentinel = huntress.getFirstAbilityOfType(CAbilitySentinel.class);
            assertNotNull(sentinel);
            game.update();
            assertTrue(shade.isInvisibleTo(game, 0));
            assertFalse(huntress.order(game, OrderIds.sentinel, tree), "Sentinel requires its research");
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Resc"), 1);
            game.update();
            assertTrue(sentinel.isRequirementsMet(game, huntress));
            assertFalse(sentinel.isDisabled());
            assertEquals(1, sentinel.getUsesRemaining());
            assertTrue(sentinel.getTargetsAllowed().contains(CTargetType.TREE), sentinel.getTargetsAllowed().toString());
            assertTrue(huntress.order(game, OrderIds.sentinel, tree));
            for (int tick = 0; tick < 200; tick++) game.update();
            assertEquals(0, sentinel.getUsesRemaining());
            assertFalse(shade.isInvisibleTo(game, 0), "the landed owl must detect the Shade");
            assertFalse(second.order(game, OrderIds.sentinel, tree), "a tree can hold one Sentinel");
            tree.damage(game, huntress, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 1);
            assertFalse(tree.isDead());
            for (int tick = 0; tick < 80; tick++) game.update();
            assertTrue(shade.isInvisibleTo(game, 0), "damage to the tree removes the owl's detection");
            assertFalse(huntress.order(game, OrderIds.sentinel, tree), "the first Huntress spent her owl");
            assertTrue(second.order(game, OrderIds.sentinel, tree));
            for (int tick = 0; tick < 200; tick++) game.update();
            assertFalse(shade.isInvisibleTo(game, 0));
            tree.setLife(game, 0);
            for (int tick = 0; tick < 80; tick++) game.update();
            assertTrue(shade.isInvisibleTo(game, 0), "a dead tree grants no detection");
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void shadowMeldHidesAtNightAndHideSuppressesAutoAttacks(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            final var archer = game.createUnitSimple(War3ID.fromString("earc"), 0, -512, 0, 0);
            assertTrue(archer.getAbilities().stream().anyMatch(CAbilityShadowMeld.class::isInstance));
            game.setGameTimeOfDay(12);
            game.update();
            assertFalse(archer.order(game, OrderIds.ambush, null));
            assertFalse(archer.isInvisibleTo(game, 1));

            game.setGameTimeOfDay(21);
            for (int tick = 0; tick < 70; tick++) game.update();
            assertTrue(archer.isInvisibleTo(game, 1), "idle archer should fade after dusk");
            assertFalse(archer.isInvisibleTo(game, 0), "the owner sees her");
            assertTrue(archer.order(game, OrderIds.move, new AbilityPointTarget(0, 0)));
            game.update();
            assertFalse(archer.isInvisibleTo(game, 1), "movement breaks Shadow Meld");
            assertTrue(archer.order(game, OrderIds.ambush, null));
            final var enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, -320, 128, 0);
            enemy.setDisableAttacks(true);
            final float life = enemy.getLife();
            for (int tick = 0; tick < 90; tick++) game.update();
            assertEquals(OrderIds.ambush, archer.getCurrentBehavior().getHighlightOrderId());
            assertEquals(life, enemy.getLife(), "Hide holds fire when the enemy moves into range");
            assertTrue(projectiles.isEmpty());
            assertTrue(archer.isInvisibleTo(game, 1));
            assertTrue(archer.order(game, OrderIds.attack, enemy));
            game.update();
            assertFalse(archer.isInvisibleTo(game, 1), "an attack order cancels Hide");
            assertTrue(archer.order(game, OrderIds.ambush, null));

            game.setGameTimeOfDay(8);
            game.update();
            game.update();
            assertFalse(archer.isInvisibleTo(game, 1));
            assertNotEquals(OrderIds.ambush, archer.getCurrentBehavior().getHighlightOrderId());
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void lineWeaponResearchDamagesTargetsBehindThePrimaryImpact(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit glaive = game.createUnitSimple(War3ID.fromString("ebal"), 0, -640, 0, 0);
            final CUnit initial = game.createUnitSimple(footman, 1, -256, 0, 0);
            final CUnit behind = game.createUnitSimple(footman, 1, -64, 0, 0);
            final CUnit outside = game.createUnitSimple(footman, 1, -64, 96, 0);
            final CUnit tooFar = game.createUnitSimple(footman, 1, -32, 0, 0);
            final var tree = game.internalCreateDestructable(War3ID.fromString("LTlt"), -64, -32, null, null);
            game.update();
            final float behindLife = behind.getLife();
            final float outsideLife = outside.getLife();
            final float tooFarLife = tooFar.getLife();
            final float treeLife = tree.getLife();
            glaive.getCurrentAttacks().get(0).launch(game, glaive, initial, 40, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            assertEquals(behindLife, behind.getLife());
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Repb"), 1);
            final var vorpal = assertInstanceOf(CUnitAttackMissileLine.class, glaive.getCurrentAttacks().get(0));
            assertEquals(200f, game.getUpgradeData().getAttackSpillDistanceBonus(glaive.getUnitType(), game.getPlayer(0)));
            vorpal.launch(game, glaive, initial, 40, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            assertTrue(behind.getLife() < behindLife);
            assertEquals(outsideLife, outside.getLife());
            assertEquals(tooFarLife, tooFar.getLife());
            assertEquals(treeLife, tree.getLife());

            final float afterSpill = behind.getLife();
            glaive.getCurrentAttacks().get(0).launch(game, glaive, new AbilityPointTarget(-256, 0), 40,
                    CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            assertEquals(afterSpill, behind.getLife(), "Attack Ground applies impact splash without follow-through damage");

            final CUnit gryphon = game.createUnitSimple(War3ID.fromString("hgry"), 0, -640, 384, 0);
            final var stormHammers = gryphon.getAbilities().stream().filter(ability -> "Asth".equals(ability.getAlias().toString()))
                    .findFirst().orElseThrow();
            assertFalse(stormHammers.isIconShowing());
            final CUnit ground = game.createUnitSimple(footman, 1, -256, 384, 0);
            final CUnit next = game.createUnitSimple(footman, 1, -128, 384, 0);
            final CUnit third = game.createUnitSimple(footman, 1, -80, 384, 0);
            game.update();
            final float nextLife = next.getLife();
            final float thirdLife = third.getLife();
            gryphon.getCurrentAttacks().get(0).launch(game, gryphon, ground, 40, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            assertEquals(nextLife, next.getLife());
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhhb"), 1);
            game.update();
            assertTrue(stormHammers.isIconShowing());
            gryphon.getCurrentAttacks().get(0).launch(game, gryphon, ground, 40, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.remove(0));
            assertTrue(next.getLife() < nextLife);
            assertEquals(thirdLife, third.getLife());
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void spikedBarricadesRetaliatesAgainstEnemyMeleeAtEveryLevel(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            final War3ID research = War3ID.fromString("Rosp");
            final War3ID barracks = War3ID.fromString("obar");
            final CUnit building = game.createUnitSimple(barracks, 0, 0, 0, 0);
            final CUnit melee = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 128, 0, 0);
            final CUnit ranged = game.createUnitSimple(War3ID.fromString("hrif"), 1, 320, 0, 0);
            final CUnit allied = game.createUnitSimple(War3ID.fromString("hfoo"), 0, -128, 0, 0);
            final var owner = game.getPlayer(0);
            assertTrue(building.getUnitType().getUpgradesUsed().contains(research));
            final int[] expectedDamage = {0, 4, 10, 16};
            for (int level = 0; level < expectedDamage.length; level++) {
                owner.setTechResearched(game, research, level);
                final float life = melee.getLife();
                melee.getCurrentAttacks().get(0).launch(game, melee, building, 30, CUnitAttackListener.DO_NOTHING);
                assertEquals(expectedDamage[level], life - melee.getLife(), 0.001f);
            }
            final float rangedLife = ranged.getLife();
            ranged.getCurrentAttacks().get(0).launch(game, ranged, building, 30, CUnitAttackListener.DO_NOTHING);
            for (final var projectile : projectiles) finish(game, projectile);
            assertEquals(rangedLife, ranged.getLife());
            final float alliedLife = allied.getLife();
            allied.getCurrentAttacks().get(0).launch(game, allied, building, 30, CUnitAttackListener.DO_NOTHING);
            assertEquals(alliedLife, allied.getLife());
            final CUnit createdAfterResearch = game.createUnitSimple(barracks, 0, -320, 0, 0);
            final float life = melee.getLife();
            melee.getCurrentAttacks().get(0).launch(game, melee, createdAfterResearch, 30, CUnitAttackListener.DO_NOTHING);
            assertEquals(life - 16, melee.getLife(), 0.001f);
            createdAfterResearch.setPlayerIndex(game, 2, false);
            melee.getCurrentAttacks().get(0).launch(game, melee, createdAfterResearch, 30, CUnitAttackListener.DO_NOTHING);
            assertEquals(life - 16, melee.getLife(), 0.001f);
            owner.setTechResearched(game, research, 0);
            melee.getCurrentAttacks().get(0).launch(game, melee, building, 30, CUnitAttackListener.DO_NOTHING);
            assertEquals(life - 16, melee.getLife(), 0.001f);
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void weaponResearchEnablesTheConfiguredAttackSlots(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var owner = game.getPlayer(0);
            final War3ID bombs = War3ID.fromString("Rhgb");
            final War3ID vorpal = War3ID.fromString("Repb");
            final War3ID corrosiveBreath = War3ID.fromString("Recb");
            final CUnit flyer = game.createUnitSimple(War3ID.fromString("hgyr"), 0, 0, 0, 0);
            final CUnit target = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, 0, 0);
            final CAbilityAttack attack = flyer.getFirstAbilityOfType(CAbilityAttack.class);
            final var receiver = new CWidgetAbilityTargetCheckReceiver();
            game.update();
            assertEquals(List.of(flyer.getUnitSpecificAttacks().get(0)), flyer.getCurrentAttacks());
            attack.checkCanTarget(game, flyer, OrderIds.attack, target, receiver);
            assertNull(receiver.getTarget());
            owner.setTechResearched(game, bombs, 1);
            assertEquals(flyer.getUnitSpecificAttacks(), flyer.getCurrentAttacks());
            attack.checkCanTarget(game, flyer, OrderIds.attack, target, receiver);
            assertSame(target, receiver.getTarget());
            final CUnit trained = game.createUnitSimple(War3ID.fromString("hgyr"), 0, -320, 0, 0);
            assertEquals(2, trained.getCurrentAttacks().size());
            flyer.setPlayerIndex(game, 1, false);
            assertEquals(1, flyer.getCurrentAttacks().size());
            flyer.setPlayerIndex(game, 0, false);
            assertEquals(2, flyer.getCurrentAttacks().size());
            owner.setTechResearched(game, bombs, 0);
            assertEquals(List.of(flyer.getUnitSpecificAttacks().get(0)), flyer.getCurrentAttacks());
            assertEquals(1, trained.getCurrentAttacks().size());

            final CUnit glaive = game.createUnitSimple(War3ID.fromString("ebal"), 0, 0, 320, 0);
            assertFalse(glaive.getCurrentAttacks().get(0).getTargetsAllowed().contains(CTargetType.TREE));
            owner.setTechResearched(game, vorpal, 1);
            assertEquals(List.of(glaive.getUnitSpecificAttacks().get(1)), glaive.getCurrentAttacks());
            assertTrue(glaive.getCurrentAttacks().get(0).getTargetsAllowed().contains(CTargetType.TREE));

            final CUnit chimaera = game.createUnitSimple(War3ID.fromString("echm"), 0, 0, -320, 0);
            final var breath = chimaera.getAbilities().stream().filter(ability -> War3ID.fromString("Acor").equals(ability.getAlias()))
                    .findFirst().orElseThrow();
            assertFalse(breath.isIconShowing());
            assertEquals(List.of(chimaera.getUnitSpecificAttacks().get(1)), chimaera.getCurrentAttacks());
            owner.setTechResearched(game, corrosiveBreath, 1);
            assertTrue(breath.isIconShowing());
            assertEquals(chimaera.getUnitSpecificAttacks(), chimaera.getCurrentAttacks());
        });
    }

    @ParameterizedTest(name = "standardMeleeData={0}")
    @ValueSource(booleans = {false, true})
    void reinforcedDefensesChangesDamageAndRestoresUnderlyingArmor(boolean standardMeleeData) throws Exception {
        withFixture(standardMeleeData, (game, projectiles) -> {
            final War3ID research = War3ID.fromString("Rorb");
            final CUnit burrow = game.createUnitSimple(War3ID.fromString("otrb"), 0, 0, 0, 0);
            final CUnit attacker = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 256, 0, 0);
            final CDefenseType original = burrow.getDefenseType();
            final var indicator = burrow.getAbilities().stream().filter(ability -> War3ID.fromString("Arbr").equals(ability.getAlias()))
                    .findFirst().orElseThrow();
            assertNotEquals(CDefenseType.FORT, original);
            assertFalse(indicator.isIconShowing());
            assertTrue(burrow.getUnitType().getUpgradesUsed().contains(research));
            final float originalDamage = hit(game, attacker, burrow);
            final CAttackType[] types = { CAttackType.NORMAL, CAttackType.PIERCE, CAttackType.SIEGE, CAttackType.MAGIC };
            final float[] baseline = new float[types.length];
            for (int i = 0; i < types.length; i++) baseline[i] = hit(game, attacker, burrow, types[i]);
            final float armor = burrow.getDefense();
            final CUnit otherOwner = game.createUnitSimple(War3ID.fromString("otrb"), 1, -512, 0, 0);
            game.getPlayer(0).setTechResearched(game, research, 1);
            assertEquals(CDefenseType.FORT, burrow.getDefenseType());
            game.update();
            assertTrue(indicator.isIconShowing());
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
            game.update();
            assertFalse(indicator.isIconShowing());
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

    @Test
    void originalFaerieFireAutocastsReducesArmorAndSharesSightUntilDispel() throws Exception {
        withFixture("Maps\\Campaign\\NightElf05.w3m", false, true, (game, projectiles) -> {
            game.setFogEnabled(true);
            game.setFogMaskEnabled(true);
            final CUnit druid = game.createUnitSimple(War3ID.fromString("edot"), 0, 0, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 200, 0, 0);
            footman.setDisableAttacks(true);
            druid.setDisableAttacks(true);
            final float originalArmor = footman.getDefense();
            druid.setMana(druid.getMaximumMana());
            assertTrue(druid.order(game, OrderIds.faeriefireoff, null));
            for (int tick = 0; tick < 30; tick++) game.update();
            assertEquals(originalArmor, footman.getDefense(), 0.001f);

            assertTrue(druid.order(game, OrderIds.faeriefireon, null));
            final var ability = druid.getFirstAbilityOfType(CAbilityFaerieFire.class);
            assertNotNull(ability);
            assertTrue(ability.isAutoCastOn(), "the native autocast order toggles Faerie Fire");
            assertTrue(footman.isVisible(game, 0), "the autocast target starts in sight");
            assertFalse(game.getPlayer(0).hasAlliance(1, CAllianceType.PASSIVE));
            assertTrue(druid.canReach(footman, druid.getAcquisitionRange()), "the target is within acquisition range");
            assertTrue(druid.autoAcquireAutocastTargets(game, true), "the Druid's autocast AI acquires the Footman");
            for (int tick = 0; tick < 45 && footman.getDefense() == originalArmor; tick++) game.update();
            assertEquals(originalArmor - 5, footman.getDefense(), 0.001f);
            assertTrue(druid.order(game, OrderIds.faeriefireoff, null));
            footman.relocate(2400, 0, game);
            for (int tick = 0; tick < 25; tick++) game.update();
            assertTrue(footman.isVisible(game, 0), "Faerie Fire follows the affected unit into fog");
            footman.addStateModBuff(new StateModBuff(StateModBuffType.INVISIBLE, 1));
            footman.computeUnitState(game, StateModBuffType.INVISIBLE);
            assertTrue(footman.isVisible(game, 0), "Faerie Fire keeps an affected unit visible when it turns invisible");
            footman.dispelBuffs(game);
            assertEquals(originalArmor, footman.getDefense(), 0.001f);
            for (int tick = 0; tick < 25; tick++) game.update();
            assertFalse(footman.isVisible(game, 0));
        });
    }

    @Test
    void originalAntiMagicShellBlocksSpellsAndCanBeDispelled() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX05.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit banshee = game.createUnitSimple(War3ID.fromString("uban"), 0, 0, 0, 0);
            final CUnit ghoul = game.createUnitSimple(War3ID.fromString("ugho"), 0, 160, 0, 0);
            final CUnit priest = game.createUnitSimple(War3ID.fromString("hmpr"), 1, 340, 0, 0);
            final CUnit sorceress = game.createUnitSimple(War3ID.fromString("hsor"), 1, 320, 128, 0);
            for (final CUnit unit : List.of(banshee, ghoul, priest, sorceress)) {
                unit.setDisableAttacks(true);
                unit.setMana(unit.getMaximumMana());
            }
            ghoul.addClassification(CUnitClassification.SUMMONED);
            assertFalse(banshee.order(game, OrderIds.antimagicshell, ghoul));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Ruba"), 1);
            game.getPlayer(1).setTechResearched(game, War3ID.fromString("Rhpt"), 1);
            game.update();
            banshee.setMana(banshee.getMaximumMana());
            priest.setMana(priest.getMaximumMana());
            assertTrue(banshee.order(game, OrderIds.antimagicshell, ghoul));
            for (int tick = 0; tick < 45 && !ghoul.isMagicImmune(); tick++) game.update();
            assertTrue(ghoul.isMagicImmune());
            assertFalse(sorceress.order(game, OrderIds.slow, ghoul));
            final float fullLife = ghoul.getLife();
            ghoul.damage(game, sorceress, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, 60);
            assertEquals(fullLife, ghoul.getLife(), 0.001f, "the original shell blocks spell damage");
            ghoul.damage(game, sorceress, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 40);
            assertTrue(ghoul.getLife() < fullLife, "physical attacks still hit");
            final float lifeBeforeDispel = ghoul.getLife();
            assertTrue(priest.order(game, OrderIds.dispel, new AbilityPointTarget(ghoul.getX(), ghoul.getY())));
            for (int tick = 0; tick < 45 && ghoul.isMagicImmune(); tick++) game.update();
            assertFalse(ghoul.isMagicImmune());
            assertEquals(lifeBeforeDispel, ghoul.getLife(), 0.001f, "Dispel strips the shell without damaging a protected summon");
            assertTrue(sorceress.order(game, OrderIds.slow, ghoul));
        });
    }

    @Test
    void originalCycloneLiftsProtectsAndAllowsDispel() throws Exception {
        withFixture("Maps\\Campaign\\NightElf05.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit druid = game.createUnitSimple(War3ID.fromString("edot"), 0, 0, 0, 0);
            final CUnit footman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 176, 0, 0);
            final CUnit hero = game.createUnitSimple(War3ID.fromString("Hamg"), 1, 240, 56, 0);
            final CUnit priest = game.createUnitSimple(War3ID.fromString("hmpr"), 1, 288, 0, 0);
            for (final CUnit unit : List.of(druid, footman, hero, priest)) unit.setDisableAttacks(true);
            final War3ID masterTraining = War3ID.fromString("Redt");
            game.getPlayer(1).setTechResearched(game, War3ID.fromString("Rhpt"), 1);
            game.getPlayer(0).setTechResearched(game, masterTraining, 1);
            game.update();
            druid.setMana(druid.getMaximumMana());
            assertFalse(druid.order(game, OrderIds.cyclone, footman));
            game.getPlayer(0).setTechResearched(game, masterTraining, 2);
            game.update();
            druid.setMana(druid.getMaximumMana());
            final float originalHeight = footman.getFlyHeight();
            game.getPlayer(0).setAlliance(game.getPlayer(1), CAllianceType.PASSIVE, true);
            game.getPlayer(0).setAlliance(game.getPlayer(1), CAllianceType.HELP_REQUEST, true);
            assertFalse(druid.order(game, OrderIds.cyclone, footman), "Cyclone cannot target an ally");
            game.getPlayer(0).setAlliance(game.getPlayer(1), CAllianceType.HELP_REQUEST, false);
            assertTrue(druid.order(game, OrderIds.cyclone, footman));
            for (int tick = 0; tick < 40 && !footman.isInvulnerable(); tick++) game.update();
            assertTrue(footman.isInvulnerable());
            assertTrue(footman.getFlyHeight() > originalHeight);
            assertEquals(OrderIds.stunned, footman.getCurrentBehavior().getHighlightOrderId());
            assertFalse(druid.order(game, OrderIds.cyclone, footman));

            footman.setInvulnerable(true);
            priest.setMana(priest.getMaximumMana());
            assertTrue(priest.order(game, OrderIds.dispel, new AbilityPointTarget(footman.getX(), footman.getY())));
            for (int tick = 0; tick < 35 && footman.getFlyHeight() > originalHeight; tick++) game.update();
            assertEquals(originalHeight, footman.getFlyHeight(), 0.001f);
            assertTrue(footman.isInvulnerable(), "dispel preserves independently scripted invulnerability");
            footman.setInvulnerable(false);
            assertFalse(footman.isInvulnerable());

            for (int tick = 0; tick < 120; tick++) game.update();
            druid.setMana(druid.getMaximumMana());
            assertTrue(druid.order(game, OrderIds.cyclone, hero));
            for (int tick = 0; tick < 40 && !hero.isInvulnerable(); tick++) game.update();
            assertTrue(hero.isInvulnerable());
            for (int tick = 0; tick < 105; tick++) game.update();
            assertTrue(hero.isInvulnerable());
            for (int tick = 0; tick < 22; tick++) game.update();
            assertFalse(hero.isInvulnerable(), "the hero lands after the authored six seconds");
        });
    }

    @Test
    void originalDiseaseCloudStaysNonlethalAndCreatesAuthoredWards() throws Exception {
        withFixture("Maps\\Campaign\\Undead03.w3m", false, (game, projectiles) -> {
            final War3ID research = War3ID.fromString("Rupc");
            final War3ID cloudType = War3ID.fromString("uplg");
            final CUnit abomination = game.createUnitSimple(War3ID.fromString("uabo"), 0, 0, 0, 0);
            final CUnit second = game.createUnitSimple(War3ID.fromString("uabo"), 0, 0, 120, 0);
            final CUnit wagon = game.createUnitSimple(War3ID.fromString("umtw"), 0, 512, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 120, 24, 0);
            final CUnit undead = game.createUnitSimple(War3ID.fromString("ugho"), 1, 120, 110, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, -120, 0);
            for (final CUnit unit : List.of(abomination, second, wagon, enemy, undead, ally)) unit.setAcquisitionRange(0);
            enemy.setLifeRegenBonus(-enemy.getUnitType().getLifeRegen());
            enemy.setLife(game, 4);
            final float undeadLife = undead.getLife();
            final float allyLife = ally.getLife();
            for (int i = 0; i < 22; i++) game.update();
            assertEquals(4f, enemy.getLife(), 0.001f);
            game.getPlayer(0).setTechResearched(game, research, 1);
            for (int i = 0; i < 23; i++) game.update();
            assertEquals(3f, enemy.getLife(), 0.001f, "overlapping clouds deal a single pulse");
            assertEquals(undeadLife, undead.getLife(), 0.001f);
            assertEquals(allyLife, ally.getLife(), 0.001f);
            enemy.dispelBuffs(game);
            enemy.relocate(1024, 0, game);
            for (int i = 0; i < 65; i++) game.update();
            assertEquals(2f, enemy.getLife(), 0.001f, "the lingering disease survives dispel and cannot kill");

            abomination.kill(game);
            final var attack = wagon.getUnitSpecificAttacks().get(0);
            attack.launch(game, wagon, new AbilityPointTarget(754, 64), 40, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.get(projectiles.size() - 1));
            final var clouds = game.getUnitsIncludingPending().stream().filter(unit -> cloudType.equals(unit.getTypeId())
                    && !unit.isDead()).toList();
            assertEquals(2, clouds.size(), "abomination death and one wagon impact each spawn a ward");
            assertTrue(clouds.stream().anyMatch(unit -> unit.distanceSquaredNoCollision(754, 64) < 32 * 32));
            final CUnit nearImpact = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 754, 254, 0);
            nearImpact.setDisableAttacks(true);
            nearImpact.setLifeRegenBonus(-nearImpact.getUnitType().getLifeRegen());
            final float lifeBeforeCloud = nearImpact.getLife();
            for (int i = 0; i < 25; i++) game.update();
            assertEquals(lifeBeforeCloud - 1, nearImpact.getLife(), 0.001f, "the spawned ward applies its own aura");
            for (int i = 0; i < 185; i++) game.update();
            assertTrue(clouds.stream().allMatch(CUnit::isDead));
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
            final var passive = huntress.getAbilities().stream().filter(ability -> War3ID.fromString("Amgl").equals(ability.getAlias()))
                    .findFirst().orElseThrow();
            final CUnit first = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 128, 0, 0);
            game.createUnitSimple(War3ID.fromString("hfoo"), 1, 160, 0, 0);
            game.createUnitSimple(War3ID.fromString("hfoo"), 1, 192, 0, 0);
            final CUnitAttackMissileBounce attack = (CUnitAttackMissileBounce) huntress.getUnitSpecificAttacks()
                    .stream().filter(a -> a instanceof CUnitAttackMissileBounce).findFirst().orElseThrow();
            assertTrue(huntress.getUnitType().getUpgradesUsed().contains(research));
            final int original = attack.getMaximumNumberOfTargets();
            assertEquals(2, original, "Classic Huntress hits the initial target and one secondary target");
            assertEquals(0.5f, attack.getDamageLossFactor());
            assertTrue(passive.isDisabled());
            assertFalse(passive.isIconShowing());
            assertEquals(original, hits(game, huntress, first, attack, projectiles));
            game.getPlayer(0).setTechResearched(game, research, 1);
            game.update();
            assertFalse(passive.isDisabled());
            assertTrue(passive.isIconShowing());
            assertEquals(original + 1, attack.getMaximumNumberOfTargets());
            assertEquals(original + 1, hits(game, huntress, first, attack, projectiles));
            game.getPlayer(0).setTechResearched(game, research, 1);
            assertEquals(original + 1, attack.getMaximumNumberOfTargets());
            game.getPlayer(0).setTechResearched(game, research, 0);
            game.update();
            assertTrue(passive.isDisabled());
            assertFalse(passive.isIconShowing());
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

    @Test
    void originalHippogryphMountAndDismountPreserveFoodAndHealth() throws Exception {
        withFixture("Maps\\Campaign\\NightElf07.w3m", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final var riderId = War3ID.fromString("ehpr");
            final var archerId = War3ID.fromString("earc");
            final var hippogryphId = War3ID.fromString("ehip");
            final var player = game.getPlayer(0);
            final CUnit unresearched = game.createUnitSimple(riderId, 0, -320, 0, 0);
            final CUnit archer = game.createUnitSimple(archerId, 0, 0, 0, 0);
            final CUnit hippogryph = game.createUnitSimple(hippogryphId, 0, 96, 0, 0);
            game.update();
            assertFalse(unresearched.order(game, OrderIds.decouple, null));
            assertFalse(archer.order(game, OrderIds.coupleinstant, null));
            game.removeUnit(unresearched);
            game.update();
            final int food = player.getFoodUsed();
            for (final CUnit unit : List.of(archer, hippogryph)) unit.setLife(game, unit.getMaximumLife() * 0.60f);
            player.setTechResearched(game, War3ID.fromString("Reht"), 1);
            game.update();
            assertTrue(archer.order(game, OrderIds.coupleinstant, null));
            for (int tick = 0; tick < 160 && game.containsUnit(archer); tick++) game.update();
            final CUnit rider = game.getUnitsIncludingPending().stream()
                    .filter(unit -> riderId.equals(unit.getTypeId()) && !unit.isHidden()).findFirst().orElseThrow();
            assertEquals(food, player.getFoodUsed());
            assertEquals(0.60f, rider.getLife() / rider.getMaximumLife(), 0.02f);
            assertEquals(0, rider.getMana());
            assertTrue(rider.order(game, OrderIds.decouple, null));
            for (int tick = 0; tick < 100 && game.containsUnit(rider); tick++) game.update();
            assertFalse(game.containsUnit(rider));
            assertEquals(food, player.getFoodUsed());
            for (final War3ID type : List.of(archerId, hippogryphId)) {
                final CUnit split = game.getUnitsIncludingPending().stream()
                        .filter(unit -> type.equals(unit.getTypeId()) && !unit.isHidden()).findFirst().orElseThrow();
                assertEquals(0.60f, split.getLife() / split.getMaximumLife(), 0.02f);
            }
        });
    }

    @Test
    void originalFaerieDragonShiftsAfterDamageAndReturnsWhenTheDurationEnds() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX04.w3x", false, (game, projectiles) -> {
            final CUnit dragon = game.createUnitSimple(War3ID.fromString("efdr"), 0, 0, 0, 0);
            final CUnit attacker = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 192, 0, 0);
            for (final CUnit unit : List.of(dragon, attacker)) unit.setAcquisitionRange(0);
            final var shift = dragon.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityPhaseShift.class);
            assertNotNull(shift);
            dragon.setMana(75);
            game.update();

            assertTrue(shift.isAutoCastOn());
            assertTrue(dragon.order(game, OrderIds.phaseshiftoff, null));
            assertFalse(shift.isAutoCastOn());
            dragon.damage(game, attacker, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 10);
            assertFalse(dragon.isHidden());

            assertTrue(dragon.order(game, OrderIds.phaseshifton, null));
            final float beforeHit = dragon.getLife();
            final float beforeShift = dragon.getMana();
            final float damage = dragon.damage(game, attacker, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 10);
            assertTrue(damage > 0);
            assertEquals(beforeHit - damage, dragon.getLife(), 0.01f);
            assertEquals(beforeShift - shift.getManaCost(), dragon.getMana(), 0.01f);
            assertTrue(dragon.isHidden() && dragon.isPaused() && dragon.isInvulnerable());
            assertEquals(0, dragon.damage(game, attacker, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 10));

            final int durationTicks = (int) (shift.getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
            assertTrue(durationTicks > 0);
            for (int tick = 1; tick < durationTicks; tick++) game.update();
            assertTrue(dragon.isHidden());
            for (int tick = 0; tick < 3; tick++) game.update();
            assertFalse(dragon.isHidden() || dragon.isPaused() || dragon.isInvulnerable());
            assertTrue(shift.getCooldownRemaining(game, dragon) > 0);
            assertTrue(dragon.damage(game, attacker, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 10) > 0);
            assertFalse(dragon.isHidden());
        });
    }

    @Test
    void originalTaurenResearchEnablesPulverizeWithTwoDamageRings() throws Exception {
        withFixture("Maps\\Campaign\\Orc07.w3m", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit tauren = game.createUnitSimple(War3ID.fromString("otau"), 0, 0, 0, 0);
            final CUnit primary = game.createUnitSimple(footman, 1, 96, 0, 0);
            final CUnit near = game.createUnitSimple(footman, 1, 128, 0, 0);
            final CUnit far = game.createUnitSimple(footman, 1, 312, 0, 0);
            final CUnit outside = game.createUnitSimple(footman, 1, 430, 0, 0);
            final CUnit ally = game.createUnitSimple(footman, 0, -128, 0, 0);
            for (final CUnit unit : List.of(tauren, primary, near, far, outside, ally)) unit.setAcquisitionRange(0);
            final var pulverize = tauren.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityPulverize.class);
            assertNotNull(pulverize);
            game.update();
            final var attack = tauren.getCurrentAttacks().get(0);
            final float nearLife = near.getLife();
            final float farLife = far.getLife();
            final float outsideLife = outside.getLife();
            final float allyLife = ally.getLife();
            assertFalse(pulverize.isIconShowing());
            for (int hits = 0; hits < 24; hits++) attack.launch(game, tauren, primary, 1, CUnitAttackListener.DO_NOTHING);
            assertEquals(nearLife, near.getLife());

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rows"), 1);
            game.update();
            assertTrue(pulverize.isIconShowing());
            for (int hits = 0; hits < 90 && near.getLife() == nearLife; hits++) {
                attack.launch(game, tauren, primary, 1, CUnitAttackListener.DO_NOTHING);
            }
            assertEquals(nearLife - 60, near.getLife(), 0.01f);
            assertEquals(farLife - 30, far.getLife(), 0.01f);
            assertEquals(outsideLife, outside.getLife());
            assertEquals(allyLife, ally.getLife());
        });
    }

    @Test
    void originalFlyingMachineBombResearchEnablesItsGroundAttack() throws Exception {
        withFixture("Maps\\Campaign\\Human09.w3m", false, (game, projectiles) -> {
            final CUnit machine = game.createUnitSimple(War3ID.fromString("hgyr"), 0, 0, 0, 0);
            final var bombs = machine.getAbilities().stream().filter(ability -> "Agyb".equals(ability.getAlias().toString()))
                    .findFirst().orElseThrow();
            assertFalse(bombs.isIconShowing());
            assertFalse(machine.getCurrentAttacks().stream().anyMatch(attack -> attack.getTargetsAllowed().contains(CTargetType.GROUND)));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhgb"), 1);
            game.update();
            assertTrue(bombs.isIconShowing());
            assertTrue(machine.getCurrentAttacks().stream().anyMatch(attack -> attack.getTargetsAllowed().contains(CTargetType.GROUND)));
        });
    }

    @Test
    void originalFlyingMachineFlakHitsThreeRingsOfEnemyAirUnits() throws Exception {
        withFixture("Maps\\Campaign\\Human09.w3m", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit machine = game.createUnitSimple(War3ID.fromString("hgyr"), 0, -320, 0, 0);
            final War3ID flying = War3ID.fromString("ehip");
            final CUnit primary = game.createUnitSimple(flying, 1, 0, 0, 0);
            final CUnit inner = game.createUnitSimple(flying, 1, 64, 0, 0);
            final CUnit middle = game.createUnitSimple(flying, 1, 116, 20, 0);
            final CUnit outer = game.createUnitSimple(flying, 1, 225, 0, 0);
            final CUnit outside = game.createUnitSimple(flying, 1, 355, 0, 0);
            final CUnit ally = game.createUnitSimple(flying, 0, 0, 100, 0);
            final CUnit ground = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, -60, 0);
            for (final CUnit unit : List.of(machine, primary, inner, middle, outer, outside, ally, ground)) unit.setAcquisitionRange(0);
            game.update();
            final var flak = machine.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityFlakCannons.class);
            assertNotNull(flak);
            assertFalse(flak.isIconShowing());
            final var airAttack = machine.getCurrentAttacks().stream().filter(attack -> attack.getTargetsAllowed().contains(CTargetType.AIR))
                    .findFirst().orElseThrow();
            final float fullLife = inner.getLife();
            final float groundLife = ground.getLife();
            airAttack.launch(game, machine, primary, 10, CUnitAttackListener.DO_NOTHING);
            for (final var projectile : List.copyOf(projectiles)) finish(game, projectile);
            projectiles.clear();
            assertEquals(fullLife, inner.getLife());

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhfc"), 1);
            game.update();
            assertTrue(flak.isIconShowing());
            airAttack.launch(game, machine, primary, 10, CUnitAttackListener.DO_NOTHING);
            for (final var projectile : List.copyOf(projectiles)) finish(game, projectile);
            final float damageMultiplier = game.getGameplayConstants().getDamageRatioAgainst(CAttackType.PIERCE, inner.getDefenseType())
                    / (1 + inner.getDefense() * game.getGameplayConstants().getDefenseArmor());
            assertEquals(fullLife - 7 * damageMultiplier, inner.getLife(), 0.01f);
            assertEquals(fullLife - 6 * damageMultiplier, middle.getLife(), 0.01f);
            assertEquals(fullLife - 5 * damageMultiplier, outer.getLife(), 0.01f);
            assertEquals(fullLife, outside.getLife());
            assertEquals(fullLife, ally.getLife());
            assertEquals(groundLife, ground.getLife());
        });
    }

    @Test
    void originalChimaeraKeepsItsLightningAndCorrosiveProjectilesSeparate() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX07.w3x", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit chimaera = game.createUnitSimple(War3ID.fromString("echm"), 0, -260, 0, 0);
            final CUnit infantry = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 0, 0);
            final CUnit tower = game.createUnitSimple(War3ID.fromString("hgtw"), 1, 0, 180, 0);
            for (final CUnit unit : List.of(chimaera, infantry, tower)) unit.setAcquisitionRange(0);
            game.update();
            assertEquals(1, chimaera.getCurrentAttacks().size());
            final var lightning = (CUnitAttackMissile) chimaera.getCurrentAttacks().get(0);
            lightning.setProjectileSpeed(900);
            lightning.launch(game, chimaera, infantry, 30, CUnitAttackListener.DO_NOTHING);
            final var normal = projectiles.get(0);
            assertEquals(1500, normal.getSpeed());
            assertTrue(((CUnitAttackMissile) normal.getUnitAttack()).getProjectileArt().contains("ChimaeraLightningMissile"));
            assertEquals(900, lightning.getProjectileSpeed());
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Recb"), 1);
            game.update();
            final var corrosive = (CUnitAttackMissile) chimaera.getCurrentAttacks().stream()
                    .filter(attack -> attack.getAttackType() == CAttackType.SIEGE).findFirst().orElseThrow();
            corrosive.launch(game, chimaera, tower, 30, CUnitAttackListener.DO_NOTHING);
            final var siege = projectiles.get(1);
            assertEquals(1200, siege.getSpeed());
            assertTrue(((CUnitAttackMissile) siege.getUnitAttack()).getProjectileArt().contains("ChimaeraAcidMissile"));
            for (int tick = 0; tick < 20; tick++) game.update();
        });
    }

    @Test
    void originalDestroyerOrbAddsSplashOnlyWhenItSpendsMana() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\UndeadX07Interlude.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit destroyer = game.createUnitSimple(War3ID.fromString("ubsp"), 0, -320, 0, 0);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit primary = game.createUnitSimple(footman, 1, 0, 0, 0);
            final CUnit inner = game.createUnitSimple(footman, 1, 64, 16, 0);
            final CUnit outer = game.createUnitSimple(footman, 1, 135, 36, 0);
            final CUnit outside = game.createUnitSimple(footman, 1, 222, 0, 0);
            final CUnit ally = game.createUnitSimple(footman, 0, 0, -64, 0);
            for (final CUnit unit : List.of(destroyer, primary, inner, outer, outside, ally)) {
                unit.setAcquisitionRange(0);
                if (unit != destroyer) {
                    unit.setDisableAttacks(true);
                    unit.setPaused(true);
                }
            }
            destroyer.setManaRegenBonus(-destroyer.getUnitType().getManaRegen());
            destroyer.setMana(0);
            game.update();
            assertTrue(destroyer.order(game, OrderIds.flamingattack, null));
            assertTrue(destroyer.order(game, OrderIds.attack, primary));
            for (int tick = 0; tick < 50 && projectiles.isEmpty(); tick++) game.update();
            assertEquals(1, projectiles.size());
            destroyer.order(game, OrderIds.stop, null);
            finish(game, projectiles.get(0));
            assertEquals(inner.getMaximumLife(), inner.getLife());
            assertEquals(outer.getMaximumLife(), outer.getLife());

            destroyer.setMana(75);
            assertTrue(destroyer.order(game, OrderIds.attack, primary));
            for (int tick = 0; tick < 60 && projectiles.size() == 1; tick++) game.update();
            assertEquals(2, projectiles.size());
            assertEquals(50, destroyer.getMana(), .001f);
            final var orb = projectiles.get(1);
            assertTrue(orb.getDamage() >= projectiles.get(0).getDamage() + 15);
            destroyer.order(game, OrderIds.stop, null);
            finish(game, orb);
            final float innerDamage = inner.getMaximumLife() - inner.getLife();
            final float outerDamage = outer.getMaximumLife() - outer.getLife();
            assertTrue(innerDamage > outerDamage && outerDamage > 0,
                    () -> "impact " + orb.getX() + "," + orb.getY() + ", inner " + inner.getX() + "," + inner.getY()
                            + " damage " + innerDamage + ", outer " + outer.getX() + "," + outer.getY() + " damage " + outerDamage);
            assertEquals(outside.getMaximumLife(), outside.getLife());
            assertEquals(ally.getMaximumLife(), ally.getLife());

            assertTrue(destroyer.order(game, OrderIds.unflamingattack, null));
            assertTrue(destroyer.order(game, OrderIds.flamingattacktarg, primary));
            for (int tick = 0; tick < 60 && projectiles.size() == 2; tick++) game.update();
            assertEquals(3, projectiles.size());
            assertEquals(25, destroyer.getMana(), .001f);
        });
    }

    @Test
    void originalSiegeEngineBarrageFiresHomingRocketsAtNearbyAirUnits() throws Exception {
        final var rockets = new ArrayList<CAbilityProjectile>();
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX06.w3x", false, false, rockets::add, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit tank = game.createUnitSimple(War3ID.fromString("hmtt"), 0, 0, 0, 0);
            final War3ID hippogryph = War3ID.fromString("ehip");
            final CUnit primary = game.createUnitSimple(hippogryph, 1, 155, 0, 0);
            final CUnit ally = game.createUnitSimple(hippogryph, 0, 0, 70, 0);
            final CUnit outOfRange = game.createUnitSimple(hippogryph, 1, 680, 0, 0);
            final CUnit infantry = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 80, -55, 0);
            final var nearbyFlyers = new ArrayList<CUnit>();
            for (int i = 0; i < 11; i++) {
                final double angle = i * Math.PI * 2 / 11;
                nearbyFlyers.add(game.createUnitSimple(hippogryph, 1, (float) (350 * Math.cos(angle)),
                        (float) (350 * Math.sin(angle)), 0));
            }
            for (final CUnit unit : game.getUnitsIncludingPending()) {
                unit.setAcquisitionRange(0);
                if (unit != tank) unit.setDisableAttacks(true);
            }
            game.update();
            assertFalse(tank.order(game, OrderIds.attack, primary));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhrt"), 1);
            game.update();
            assertEquals(War3ID.fromString("hrtt"), tank.getTypeId());
            assertTrue(tank.order(game, OrderIds.attack, primary));
            for (int i = 0; i < 36 && rockets.isEmpty(); i++) game.update();
            assertEquals(8, rockets.size());
            assertTrue(rockets.stream().map(CAbilityProjectile::getTarget).allMatch(nearbyFlyers::contains));
            assertEquals(8, rockets.stream().map(CAbilityProjectile::getTarget).distinct().count());
            tank.order(game, OrderIds.stop, null);
            for (int i = 0; i < 20; i++) game.update();
            assertEquals(8, nearbyFlyers.stream().filter(unit -> unit.getLife() < unit.getMaximumLife()).count());
            assertEquals(ally.getMaximumLife(), ally.getLife());
            assertEquals(outOfRange.getMaximumLife(), outOfRange.getLife());
            assertEquals(infantry.getMaximumLife(), infantry.getLife());
        });
    }

    @Test
    void originalPitLordCleaveUsesLearnedRanksAndNearbyGroundTargets() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX06.w3x", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit lord = game.createUnitSimple(War3ID.fromString("Nmag"), 0, -100, 0, 0);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit primary = game.createUnitSimple(footman, 1, 0, 0, 0);
            final CUnit nearby = game.createUnitSimple(footman, 1, 80, 0, 0);
            final CUnit outside = game.createUnitSimple(footman, 1, 230, 0, 0);
            final CUnit ally = game.createUnitSimple(footman, 0, 0, 80, 0);
            final CUnit flyer = game.createUnitSimple(War3ID.fromString("ehip"), 1, 0, -80, 0);
            for (final CUnit unit : List.of(lord, primary, nearby, outside, ally, flyer)) unit.setAcquisitionRange(0);
            game.update();
            final var attack = lord.getCurrentAttacks().get(0);
            final float nearbyLife = nearby.getLife();
            final float outsideLife = outside.getLife();
            final float allyLife = ally.getLife();
            final float flyerLife = flyer.getLife();
            attack.launch(game, lord, primary, 40, CUnitAttackListener.DO_NOTHING);
            assertEquals(nearbyLife, nearby.getLife());

            final War3ID cleaveId = War3ID.fromString("ANca");
            lord.getHeroData().setHeroLevel(game, lord, 5, false);
            lord.getHeroData().selectHeroSkill(game, lord, cleaveId);
            attack.launch(game, lord, primary, 40, CUnitAttackListener.DO_NOTHING);
            final float firstRankDamage = nearbyLife - nearby.getLife();
            assertTrue(firstRankDamage > 0);
            nearby.setPermanentDefenseBonus(40);
            lord.getHeroData().selectHeroSkill(game, lord, cleaveId);
            lord.getHeroData().selectHeroSkill(game, lord, cleaveId);
            nearby.setLife(game, nearbyLife);
            attack.launch(game, lord, primary, 40, CUnitAttackListener.DO_NOTHING);
            assertEquals(firstRankDamage * .65f / .25f, nearbyLife - nearby.getLife(), .01f);
            assertEquals(outsideLife, outside.getLife());
            assertEquals(allyLife, ally.getLife());
            assertEquals(flyerLife, flyer.getLife());
        });
    }

    @Test
    void originalBatriderLiquidFireInterruptsAndPreventsRepairs() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX06.w3x", false, (game, projectiles) -> {
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setGold(1000);
            game.getPlayer(1).setLumber(1000);
            final CUnit rider = game.createUnitSimple(War3ID.fromString("otbr"), 0, -300, 0, 0);
            final CUnit tower = game.createUnitSimple(War3ID.fromString("hgtw"), 1, 0, 0, 0);
            final CUnit peasant = game.createUnitSimple(War3ID.fromString("hpea"), 1, 90, 0, 0);
            for (final CUnit unit : List.of(rider, tower, peasant)) unit.setAcquisitionRange(0);
            game.update();
            final var liquidFire = rider.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityLiquidFire.class);
            assertNotNull(liquidFire);
            assertFalse(liquidFire.isIconShowing());
            final var raid = rider.getCurrentAttacks().stream()
                    .filter(attack -> attack.getTargetsAllowed().contains(CTargetType.STRUCTURE))
                    .findFirst().orElseThrow();
            final var towerAttack = tower.getCurrentAttacks().get(0);
            rider.setDisableAttacks(true);
            tower.setDisableAttacks(true);
            raid.launch(game, rider, tower, 20, CUnitAttackListener.DO_NOTHING);
            for (final var projectile : List.copyOf(projectiles)) finish(game, projectile);
            projectiles.clear();
            final float attackSpeed = towerAttack.getAttackSpeedModifier();
            tower.setLife(game, tower.getMaxLife() - 125);
            assertTrue(peasant.order(game, OrderIds.repair, tower));
            game.update();

            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rolf"), 1);
            game.update();
            assertTrue(liquidFire.isIconShowing());
            raid.launch(game, rider, tower, 20, CUnitAttackListener.DO_NOTHING);
            for (final var projectile : List.copyOf(projectiles)) finish(game, projectile);
            assertEquals(attackSpeed - .8f, towerAttack.getAttackSpeedModifier(), .001f);
            final float burningLife = tower.getLife();
            game.update();
            assertFalse(peasant.order(game, OrderIds.repair, tower));
            for (int i = 0; i < 20; i++) game.update();
            assertTrue(tower.getLife() < burningLife, "The burning structure takes damage while its active repair is stopped");
            for (int i = 0; i < 65; i++) game.update();
            assertEquals(attackSpeed, towerAttack.getAttackSpeedModifier(), .001f);
            final float damagedLife = tower.getLife();
            assertTrue(peasant.order(game, OrderIds.repair, tower));
            for (int i = 0; i < 25; i++) game.update();
            assertTrue(tower.getLife() > damagedLife);
        });
    }

    @Test
    void originalManaFlarePunishesSpellCastsAndEndsOnMovement() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, false);
            final CUnit dragon = game.createUnitSimple(War3ID.fromString("efdr"), 0, 0, 0, 0);
            final CUnit sorceress = game.createUnitSimple(War3ID.fromString("hsor"), 1, 460, 0, 0);
            final CUnit priest = game.createUnitSimple(War3ID.fromString("hmpr"), 1, 460, 110, 0);
            final CUnit target = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 300, 0, 0);
            final CUnit enemyFootman = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 460, -110, 0);
            for (final CUnit unit : List.of(dragon, sorceress, priest, target, enemyFootman)) unit.setAcquisitionRange(0);
            final var flare = dragon.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityManaFlare.class);
            assertNotNull(flare);
            sorceress.order(game, OrderIds.slowoff, null);
            dragon.setMana(110);
            sorceress.setMana(150);
            game.update();
            final float ordinaryArmor = dragon.getDefense();
            assertTrue(dragon.order(game, OrderIds.manaflareon, null));
            for (int tick = 0; tick < 40 && !flare.isToggleOn(); tick++) game.update();
            assertTrue(flare.isToggleOn());
            assertEquals(ordinaryArmor + 12, dragon.getDefense(), 0.01f);
            dragon.damage(game, enemyFootman, true, false, CAttackType.NORMAL, CDamageType.NORMAL, null, 10);
            assertFalse(dragon.isHidden(), "The original channel prevents Phase Shift");

            final float sorceressLife = sorceress.getLife();
            final float priestLife = priest.getLife();
            final float footmanLife = enemyFootman.getLife();
            assertTrue(sorceress.order(game, OrderIds.slow, target));
            for (int tick = 0; tick < 50 && sorceress.getLife() == sorceressLife; tick++) game.update();
            assertEquals(sorceressLife - 90, sorceress.getLife(), 1f);
            assertEquals(priestLife - 90, priest.getLife(), 1f);
            assertEquals(footmanLife, enemyFootman.getLife(), 0.01f);
            assertTrue(dragon.order(game, OrderIds.move, new AbilityPointTarget(-320, 0)));
            game.update();
            assertFalse(flare.isToggleOn());
            assertEquals(ordinaryArmor, dragon.getDefense(), 0.01f);
        });
    }

    @Test
    void originalBrewmasterHazeSlowsAndIgnitesWithBreathOfFire() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, false);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit brewer = game.createUnitSimple(War3ID.fromString("Npbm"), 0, 0, 0, 0);
            final CUnit enemy = game.createUnitSimple(footman, 1, 320, 0, 0);
            final CUnit splash = game.createUnitSimple(footman, 1, 385, 90, 0);
            final CUnit plain = game.createUnitSimple(footman, 1, 105, -154, 0);
            final CUnit ally = game.createUnitSimple(footman, 0, 330, -80, 0);
            for (final CUnit unit : List.of(brewer, enemy, splash, plain, ally)) unit.setAcquisitionRange(0);
            for (final CUnit unit : List.of(brewer, splash, plain, ally)) unit.setDisableAttacks(true);
            for (final String id : List.of("ANdh", "ANbf")) {
                brewer.add(game, game.getAbilityData().createAbility(War3ID.fromString(id), game.getHandleIdAllocator().createId()));
            }
            brewer.setMana(300);
            game.update();
            final int normalSpeed = enemy.getMovementSpeed(game);
            assertTrue(brewer.order(game, OrderIds.drunkenhaze, enemy));
            for (int tick = 0; tick < 45 && enemy.getMovementSpeed(game) == normalSpeed; tick++) game.update();
            assertTrue(enemy.getMovementSpeed(game) < normalSpeed);
            assertTrue(splash.getMovementSpeed(game) < normalSpeed);
            assertEquals(normalSpeed, plain.getMovementSpeed(game));
            assertEquals(normalSpeed, ally.getMovementSpeed(game));

            final var attack = enemy.getCurrentAttacks().get(0);
            int misses = 0;
            for (int hit = 0; hit < 45; hit++) {
                final float life = ally.getLife();
                attack.launch(game, enemy, ally, 1, CUnitAttackListener.DO_NOTHING);
                if (ally.getLife() == life) misses++;
            }
            assertTrue(misses > 0 && misses < 45);
            enemy.setDisableAttacks(true);

            final float enemyLife = enemy.getLife();
            final float plainLife = plain.getLife();
            assertTrue(brewer.order(game, OrderIds.breathoffire, new AbilityPointTarget(496, 0)));
            for (int tick = 0; tick < 50 && enemy.getLife() == enemyLife; tick++) game.update();
            assertEquals(enemyLife - 50, enemy.getLife(), 0.1f);
            assertEquals(plainLife - 50, plain.getLife(), 0.1f);
            for (int tick = 0; tick < 110; tick++) game.update();
            assertEquals(enemyLife - 50 - 35, enemy.getLife(), 1.5f);
            assertEquals(plainLife - 50, plain.getLife(), 1.5f);
        });
    }

    @Test
    void originalBrewmasterReturnsAtTheSurvivingElementalWithTheSameInventory() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\OrcX01.w3x", false, (game, projectiles) -> {
            final CUnit chen = game.createUnitSimple(War3ID.fromString("Nsjs"), 0, 0, 0, 0);
            final War3ID splitId = War3ID.fromString("Acef");
            final var split = game.getAbilityData().createAbility(splitId, game.getHandleIdAllocator().createId());
            assertNotNull(split);
            chen.add(game, split);
            chen.setMana(300);
            final CItem potion = game.createItem(War3ID.fromString("phea"), 0, 0);
            final int slot = chen.getInventoryData().giveItem(game, chen, potion, false);
            assertTrue(slot >= 0);
            game.update();
            assertTrue(chen.order(game, OrderIds.elementalfury, null));
            for (int tick = 0; tick < 45 && !chen.isHidden(); tick++) game.update();
            assertTrue(chen.isHidden() && chen.isPaused() && chen.isInvulnerable());
            final var elementals = game.getUnitsIncludingPending().stream().filter(unit -> unit != chen).toList();
            assertEquals(List.of("npn1", "npn2", "npn3"),
                    elementals.stream().map(unit -> unit.getTypeId().toString()).sorted().toList());
            final CUnit survivor = elementals.stream().filter(unit -> "npn2".equals(unit.getTypeId().toString())).findFirst().orElseThrow();
            for (final CUnit unit : elementals) if (unit != survivor) unit.kill(game);
            for (int tick = 0; tick < 1220 && chen.isHidden(); tick++) game.update();
            assertFalse(chen.isDead() || chen.isHidden() || chen.isPaused() || chen.isInvulnerable());
            assertSame(chen, game.getUnit(chen.getHandleId()));
            assertSame(potion, chen.getInventoryData().getItemInSlot(slot));
            assertEquals(survivor.getX(), chen.getX(), 1f);
            assertEquals(survivor.getY(), chen.getY(), 1f);
            assertTrue(elementals.stream().noneMatch(game::containsUnit));

            final CUnit second = game.createUnitSimple(War3ID.fromString("Nsjs"), 0, 480, 0, 0);
            second.add(game, game.getAbilityData().createAbility(splitId, game.getHandleIdAllocator().createId()));
            second.setMana(300);
            game.update();
            assertTrue(second.order(game, OrderIds.elementalfury, null));
            for (int tick = 0; tick < 45 && !second.isHidden(); tick++) game.update();
            assertTrue(second.isHidden());
            game.getUnitsIncludingPending().stream().filter(unit -> unit != chen && unit != second).forEach(unit -> unit.kill(game));
            for (int tick = 0; tick < 6 && !second.isDead(); tick++) game.update();
            assertTrue(second.isDead());
        });
    }

    @Test
    void originalControlMagicUsesMaximumLifeAndPreservesTheSummonsExpiry() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX03Interlude.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit breaker = game.createUnitSimple(War3ID.fromString("hspt"), 0, 0, 0, 0);
            final CUnit soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 128, 0, 0);
            final CUnit elemental = game.createUnitSimple(War3ID.fromString("hwat"), 1, -144, 0, 0);
            for (final CUnit unit : List.of(breaker, soldier, elemental)) unit.setAcquisitionRange(0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            elemental.addUnitType(game, CUnitTypeJass.SUMMONED);
            elemental.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), War3ID.fromString("BHwe"), 5, false));
            elemental.setLife(game, elemental.getMaximumLife() / 2f);
            breaker.setMana(250);
            game.update();
            assertFalse(breaker.order(game, OrderIds.controlmagic, elemental));
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rhss"), 1);
            game.update();
            assertFalse(breaker.order(game, OrderIds.controlmagic, soldier));
            final int requiredMana = 25 + Math.round(elemental.getMaximumLife() * 0.3f);
            breaker.setMana(requiredMana - 1);
            assertFalse(breaker.order(game, OrderIds.controlmagic, elemental));
            breaker.setMana(250);
            final int originalHandle = elemental.getHandleId();
            final var control = breaker.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityControlMagic.class);
            assertNotNull(control);
            assertFalse(control.isDisabled());
            final var receiver = new ExternStringMsgTargetCheckReceiver<CWidget>();
            control.checkCanTarget(game, breaker, OrderIds.controlmagic, elemental, receiver);
            assertSame(elemental, receiver.getTarget(), receiver::getExternStringKey);
            assertTrue(breaker.order(game, OrderIds.controlmagic, elemental));
            for (int tick = 0; tick < 75 && elemental.getPlayerIndex() == 1; tick++) game.update();
            assertEquals(0, elemental.getPlayerIndex());
            assertEquals(originalHandle, elemental.getHandleId());
            assertEquals(250 - requiredMana, breaker.getMana(), 1);
            for (int tick = 0; tick < 110; tick++) game.update();
            assertTrue(elemental.isDead());
        });
    }

    @Test
    void originalSpellStealMovesBloodlustAndSlowWithoutRefreshingTheirDuration() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX03Interlude.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, false);
            final War3ID footman = War3ID.fromString("hfoo");
            final CUnit breaker = game.createUnitSimple(War3ID.fromString("hspt"), 0, 0, 0, 0);
            final CUnit ally = game.createUnitSimple(footman, 0, 250, 0, 0);
            final CUnit enemy = game.createUnitSimple(footman, 1, 340, 0, 0);
            final CUnit shaman = game.createUnitSimple(War3ID.fromString("oshm"), 1, 550, 300, 0);
            final CUnit sorceress = game.createUnitSimple(War3ID.fromString("hsor"), 1, 650, -300, 0);
            breaker.setDisableAttacks(true);
            breaker.setMana(245);
            sorceress.setMana(150);
            for (final CUnit unit : List.of(ally, enemy, shaman, sorceress)) unit.setAcquisitionRange(0);
            game.update();
            final var steal = breaker.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilitySpellSteal.class);
            assertNotNull(steal);
            assertTrue(breaker.order(game, OrderIds.spellstealoff, null));
            assertFalse(steal.isAutoCastOn());
            assertFalse(breaker.order(game, OrderIds.spellsteal, enemy));

            final var cast = shaman.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman.CAbilityBloodlust.class);
            cast.doEffect(game, shaman, enemy);
            final CBuff bloodlust = enemy.getAbilities().stream().filter(CBuff.class::isInstance).map(CBuff.class::cast)
                    .filter(buff -> "Bblo".equals(buff.getCode().toString())).findFirst().orElseThrow();
            for (int tick = 0; tick < 50; tick++) game.update();
            final float bloodlustRemaining = bloodlust.getDurationRemaining(game, enemy);
            assertTrue(breaker.order(game, OrderIds.spellsteal, enemy));
            for (int tick = 0; tick < 45 && !ally.getAbilities().contains(bloodlust); tick++) game.update();
            assertTrue(ally.getAbilities().contains(bloodlust));
            assertFalse(enemy.getAbilities().contains(bloodlust));
            assertTrue(bloodlust.getDurationRemaining(game, ally) <= bloodlustRemaining);

            for (int tick = 0; tick < 120; tick++) game.update();
            assertTrue(sorceress.order(game, OrderIds.slowoff, null));
            assertTrue(sorceress.order(game, OrderIds.slow, ally));
            final War3ID slowAlias = War3ID.fromString("Bslo");
            for (int tick = 0; tick < 100 && ally.getAbilities().stream().noneMatch(a -> slowAlias.equals(a.getAlias())); tick++) game.update();
            final CBuff slow = ally.getAbilities().stream().filter(CBuff.class::isInstance).map(CBuff.class::cast)
                    .filter(buff -> slowAlias.equals(buff.getAlias())).findFirst().orElseThrow();
            final float slowRemaining = slow.getDurationRemaining(game, ally);
            assertTrue(breaker.order(game, OrderIds.spellstealon, null));
            for (int tick = 0; tick < 80 && !enemy.getAbilities().contains(slow); tick++) game.update();
            assertTrue(enemy.getAbilities().contains(slow));
            assertFalse(ally.getAbilities().contains(slow));
            assertTrue(slow.getDurationRemaining(game, enemy) <= slowRemaining);
        });
    }

    @Test
    void originalDryadDispelsByAllegianceAndManuallyDamagesSummons() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX04.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit dryad = game.createUnitSimple(War3ID.fromString("edry"), 0, 0, 0, 0);
            final CUnit friendly = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 128, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, -128, 0, 0);
            final CUnit summon = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 210, 0);
            final CUnit shaman = game.createUnitSimple(War3ID.fromString("oshm"), 1, -288, 0, 0);
            dryad.setDisableAttacks(true);
            for (final CUnit unit : List.of(friendly, enemy, summon, shaman)) unit.setAcquisitionRange(0);
            final var abolish = dryad.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityAbolishMagic.class);
            final var bloodlust = shaman.getFirstAbilityOfType(
                    com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman.CAbilityBloodlust.class);
            summon.addUnitType(game, CUnitTypeJass.SUMMONED);
            dryad.setMana(200);
            game.update();
            assertFalse(dryad.order(game, OrderIds.autodispel, summon));

            final War3ID slowAlias = War3ID.fromString("Bslo");
            final CBuffSlow friendlySlow = new CBuffSlow(game.getHandleIdAllocator().createId(), slowAlias, 30, 0.4f, 0.4f);
            final CBuffSlow enemySlow = new CBuffSlow(game.getHandleIdAllocator().createId(), slowAlias, 30, 0.4f, 0.4f);
            friendly.add(game, friendlySlow);
            enemy.add(game, enemySlow);
            bloodlust.doEffect(game, shaman, friendly);
            bloodlust.doEffect(game, shaman, enemy);
            final var friendlyBloodlust = friendly.getAbilities().stream().filter(ability -> ability instanceof CBuff buff
                    && buff.getDispelPolarity() == CBuff.DispelPolarity.BENEFICIAL).findFirst().orElseThrow();
            final var enemyBloodlust = enemy.getAbilities().stream().filter(ability -> ability instanceof CBuff buff
                    && buff.getDispelPolarity() == CBuff.DispelPolarity.BENEFICIAL).findFirst().orElseThrow();
            final float summonLife = summon.getLife();
            game.getPlayer(0).setTechResearched(game, War3ID.fromString("Resi"), 1);
            game.update();
            assertTrue(abolish.isAutoCastOn());
            for (int tick = 0; tick < 120; tick++) game.update();
            assertFalse(friendly.getAbilities().contains(friendlySlow));
            assertTrue(friendly.getAbilities().contains(friendlyBloodlust));
            assertTrue(enemy.getAbilities().contains(enemySlow));
            assertFalse(enemy.getAbilities().contains(enemyBloodlust));
            assertEquals(summonLife, summon.getLife());
            assertTrue(dryad.order(game, OrderIds.autodispel, summon));
            for (int tick = 0; tick < 85; tick++) game.update();
            assertEquals(summonLife - 300, summon.getLife(), 1);
        });
    }

    @Test
    void originalMountainGiantUsesWarClubForFifteenSiegeAttacks() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX06.w3x", false, (game, projectiles) -> {
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit giant = game.createUnitSimple(War3ID.fromString("emtg"), 0, 0, 0, 0);
            final CDestructable tree = game.internalCreateDestructable(War3ID.fromString("LTlt"), 112, 0, null, null);
            final CDestructable nextTree = game.internalCreateDestructable(War3ID.fromString("LTlt"), 144, -128, null, null);
            final CUnit castle = game.createUnitSimple(War3ID.fromString("hcas"), 1, 320, 0, 0);
            giant.setAcquisitionRange(0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.update();
            final var unarmed = giant.getCurrentAttacks().get(0);
            assertEquals(CAttackType.NORMAL, unarmed.getAttackType());
            assertTrue(giant.getUnitSpecificAttacks().stream().anyMatch(attack -> attack.getAttackType() == CAttackType.SIEGE));
            assertFalse(giant.order(game, OrderIds.attack, tree));
            assertTrue(giant.order(game, OrderIds.smart, tree));
            for (int tick = 0; tick < 160 && !tree.isDead(); tick++) game.update();
            assertTrue(tree.isDead(), () -> "position=" + giant.getX() + "," + giant.getY()
                    + " behavior=" + giant.getCurrentBehavior().getClass().getSimpleName()
                    + " order=" + giant.getCurrentOrder());
            final var club = giant.getCurrentAttacks().get(0);
            assertEquals(CAttackType.SIEGE, club.getAttackType());
            assertEquals(250, club.getRange());
            assertTrue(giant.order(game, OrderIds.attack, nextTree));
            for (int strike = 0; strike < 14; strike++) club.launch(game, giant, castle, 24, CUnitAttackListener.DO_NOTHING);
            assertSame(club, giant.getCurrentAttacks().get(0));
            club.launch(game, giant, castle, 24, CUnitAttackListener.DO_NOTHING);
            assertSame(unarmed, giant.getCurrentAttacks().get(0));
            assertFalse(giant.order(game, OrderIds.attack, nextTree));
        });
    }

    @ParameterizedTest
    @CsvSource({ "nnrg, nnrs, false", "nmyr, nmys, true", "nsnp, nsbs, true" })
    void originalNagaCanSubmergeInDeepWaterAndSurfaceWithTheSameHandle(String baseType, String submergedType,
            boolean requiresResearch) throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\NightElfX04.w3x", false, true, (game, projectiles) -> {
            final var surface = War3ID.fromString(baseType);
            final var submerged = War3ID.fromString(submergedType);
            final CUnit guard = game.createUnitSimple(surface, 0, -3056, -3120, 0);
            game.getPlayer(0).setAlliance(1, CAllianceType.PASSIVE, false);
            game.getPlayer(1).setAlliance(0, CAllianceType.PASSIVE, false);
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            game.update();
            guard.relocate(-4368, 48, game);
            if (requiresResearch) {
                assertFalse(guard.order(game, OrderIds.submerge, null), "Submerge is unavailable before research");
                game.getPlayer(0).setTechResearched(game, War3ID.fromString("Rnsb"), 1);
                game.update();
            }
            guard.relocate(-3056, -3120, game);
            assertFalse(guard.order(game, OrderIds.submerge, null), "This shallow shore is walkable");

            guard.relocate(-4368, 48, game);
            assertTrue(guard.order(game, OrderIds.submerge, null));
            for (int tick = 0; tick < 45 && !submerged.equals(guard.getTypeId()); tick++) game.update();
            assertEquals(submerged, guard.getTypeId());
            assertEquals(0, guard.getSpeed());
            assertTrue(guard.getCurrentAttacks().isEmpty());
            assertTrue(guard.isInvisibleTo(game, 1));

            final int handle = guard.getHandleId();
            assertTrue(guard.order(game, OrderIds.unsubmerge, null));
            for (int tick = 0; tick < 45 && !surface.equals(guard.getTypeId()); tick++) game.update();
            assertEquals(surface, guard.getTypeId());
            assertEquals(handle, guard.getHandleId());
            assertFalse(guard.isInvisibleTo(game, 1));
            assertFalse(guard.getCurrentAttacks().isEmpty());
        });
    }

    @Test
    void originalMortarFragmentationAddsResearchGatedDamageToTheAuthoredArmorClasses() throws Exception {
        withFixture("Maps\\FrozenThrone\\Campaign\\HumanX02.w3x", false, (game, projectiles) -> {
            final var player = game.getPlayer(0);
            player.setAlliance(1, CAllianceType.PASSIVE, false);
            final CUnit mortar = game.createUnitSimple(War3ID.fromString("hmtm"), 0, -600, 0, 0);
            final CUnit unarmored = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 0, 0);
            final CUnit medium = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 175, 0);
            final CUnit heavy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 175, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 0, 0, -175, 0);
            unarmored.setDefenseType(CDefenseType.NONE);
            medium.setDefenseType(CDefenseType.MEDIUM);
            heavy.setDefenseType(CDefenseType.LARGE);
            ally.setDefenseType(CDefenseType.MEDIUM);
            for (final CUnit unit : List.of(mortar, unarmored, medium, heavy, ally)) unit.setAcquisitionRange(0);
            game.update();
            final float[] bonus = new float[4];
            final List<CUnit> targets = List.of(unarmored, medium, heavy, ally);
            for (int index = 0; index < targets.size(); index++) {
                final int slot = index;
                targets.get(index).addDamageTakenListener((simulation, source, victim, attack, ranged, type, damage, extra, dealt) ->
                        bonus[slot] += extra);
            }
            final var attack = (CUnitAttackMissileSplash) mortar.getCurrentAttacks().get(0);
            attack.doDamage(game, mortar, new AbilityPointTarget(0, 0), 40, 0, 0, 0, CUnitAttackListener.DO_NOTHING);
            assertArrayEquals(new float[] {0, 0, 0, 0}, bonus);
            player.setTechResearched(game, War3ID.fromString("Rhfs"), 1);
            game.update();
            attack.doDamage(game, mortar, new AbilityPointTarget(0, 0), 40, 0, 0, 0, CUnitAttackListener.DO_NOTHING);
            assertArrayEquals(new float[] {25, 18, 0, 0}, bonus);
        });
    }

    @Test
    void originalCryptFiendAttackReturnsAHarmlessSpiderlingAfterImpact() throws Exception {
        final List<CAbilityProjectile> returns = new ArrayList<>();
        withFixture("Maps\\Campaign\\Undead05.w3m", false, false, returns::add, (game, projectiles) -> {
            final CUnit fiend = game.createUnitSimple(War3ID.fromString("ucry"), 0, 0, 0, 0);
            final CUnit target = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 192, 0, 0);
            fiend.setPaused(true);
            target.setPaused(true);
            fiend.setLifeRegenBonus(-fiend.getUnitType().getLifeRegen());
            target.setLifeRegenBonus(-target.getUnitType().getLifeRegen());
            final CUnitAttackMissile attack = (CUnitAttackMissile) fiend.getCurrentAttacks().get(0);
            assertNotNull(fiend.getFirstAbilityOfType(CAbilitySpiderAttack.class));
            final float targetLife = target.getLife();
            final float fiendLife = fiend.getLife();
            attack.launch(game, fiend, target, 40, CUnitAttackListener.DO_NOTHING);
            finish(game, projectiles.get(0));
            assertTrue(target.getLife() < targetLife);
            final float targetLifeAfterHit = target.getLife();
            assertTrue(returns.isEmpty());

            final int ticksPerSecond = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
            for (int tick = 0; tick < ticksPerSecond - 1; tick++) game.update();
            assertTrue(returns.isEmpty());
            for (int tick = 0; tick < 3 && returns.isEmpty(); tick++) game.update();
            assertEquals(1, returns.size());
            assertSame(target, returns.get(0).getSource());
            assertSame(fiend, returns.get(0).getTarget());
            for (int tick = 0; tick < ticksPerSecond; tick++) game.update();
            assertTrue(returns.get(0).isDone());
            assertEquals(fiendLife, fiend.getLife());
            assertEquals(targetLifeAfterHit, target.getLife());
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
        withFixture("Maps\\Campaign\\Human01.w3m", standardMeleeData, scenario);
    }

    private static void withFixture(String mapPath, boolean standardMeleeData, Scenario scenario) throws Exception {
        withFixture(mapPath, standardMeleeData, false, scenario);
    }

    private static void withFixture(String mapPath, boolean standardMeleeData, boolean originalTerrain, Scenario scenario) throws Exception {
        withFixture(mapPath, standardMeleeData, originalTerrain, projectile -> { }, scenario);
    }

    private static void withFixture(String mapPath, boolean standardMeleeData, boolean originalTerrain,
            java.util.function.Consumer<CAbilityProjectile> abilityProjectiles, Scenario scenario) throws Exception {
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
                try (War3Map map = new War3Map(source, mapPath)) {
                    System.out.println("Combat research fixture: " + (standardMeleeData ? "standard melee data" : mapPath));
                    final var data = standardMeleeData ? Warcraft3MapRuntimeObjectData.load(source, true) : map.readModifications();
                    final List<CAttackProjectileMissile> projectiles = new ArrayList<>();
                    final CSimulation game = originalTerrain
                            ? RetailSimulationTestSupport.simulationWithOriginalTerrain(map, data, projectiles::add)
                            : RetailSimulationTestSupport.simulation(map, data, projectiles::add, abilityProjectiles);
                    scenario.run(game, projectiles);
                }
            }
        }
        finally {
            WarsmashConstants.RACE_MANAGER = previous;
            for (MpqDataSource archive : archives) archive.close();
        }
    }
}
