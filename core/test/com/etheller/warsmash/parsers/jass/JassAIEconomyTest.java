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
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.RetailSimulationTestSupport;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellUnits;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityQueue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.CAbilityDarkConversion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.sappers.CAbilityKaboom;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman.CAbilityBloodlust;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionStateFingerprint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;

import mpq.MPQArchive;

@SuppressWarnings("try")
class JassAIEconomyTest {
    @Test
    void warDrumsResearchUpdatesFriendlyMeleeAndRangedDamage() throws Exception {
        try (Fixture fixture = new Fixture("Orc08")) {
            final var game = fixture.game;
            final War3ID research = War3ID.fromString("Rwdm");
            final War3ID kodoType = War3ID.fromString("okod");
            final CUnit melee = game.createUnitSimple(War3ID.fromString("ogru"), 3, -96, 0, 0);
            final CUnit ranged = game.createUnitSimple(War3ID.fromString("ohun"), 3, 96, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 0, 192, 0);
            enemy.setDisableAttacks(true);
            final int meleeBase = melee.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus();
            final int rangedBase = ranged.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus();
            final CUnit kodo = game.createUnitSimple(kodoType, 3, 0, 0, 0);
            assertTrue(kodo.getUnitType().getUpgradesUsed().contains(research));
            for (int tick = 0; tick < 12; tick++) game.update();
            final int meleeBaseAura = melee.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus();
            final int rangedBaseAura = ranged.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus();
            assertTrue(meleeBaseAura > meleeBase);
            assertTrue(rangedBaseAura > rangedBase);
            assertTrue(enemy.getUnitSpecificAttacks().get(0).getNonStackingPctBuffs().isEmpty());
            game.getPlayer(3).setTechResearched(game, research, 1);
            for (int tick = 0; tick < 12; tick++) game.update();
            final int meleeImproved = melee.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus();
            final int rangedImproved = ranged.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus();
            assertTrue(meleeImproved > meleeBaseAura);
            assertTrue(rangedImproved > rangedBaseAura);
            game.createUnitSimple(kodoType, 3, 0, -96, 0);
            for (int tick = 0; tick < 12; tick++) game.update();
            assertEquals(meleeImproved, melee.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus());
            game.getPlayer(3).setTechResearched(game, research, 0);
            for (int tick = 0; tick < 12; tick++) game.update();
            assertEquals(meleeBaseAura, melee.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus());
            assertEquals(rangedBaseAura, ranged.getCurrentAttacks().get(0).getTotalTemporaryDamageBonus());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,0,40", "1,0,55", "0,1,40", "1,1,55"})
    void raisedSkeletonsUseTheResearchedTypesAndTimedLife(final int longevityLevel, final int masteryLevel, final float duration)
            throws Exception {
        try (Fixture fixture = new Fixture("Undead02")) {
            final var game = fixture.game;
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final War3ID longevity = War3ID.fromString("Rusl");
            final War3ID mastery = War3ID.fromString("Rusm");
            final War3ID skeletonType = War3ID.fromString("uske");
            final War3ID mageType = War3ID.fromString("uskm");
            final CUnit necromancer = game.createUnitSimple(War3ID.fromString("unec"), 3, 0, 0, 0);
            final CUnit corpse = game.createUnitSimple(War3ID.fromString("hfoo"), 3, 128, 0, 0);
            game.update();
            corpse.kill(game);
            for (int tick = 0; tick < 180; tick++) game.update();
            assertTrue(corpse.isBoneCorpse() && corpse.isRaisable());
            game.getPlayer(3).setTechResearched(game, longevity, longevityLevel);
            game.getPlayer(3).setTechResearched(game, mastery, masteryLevel);
            necromancer.setMana(necromancer.getMaximumMana());
            final var raiseDead = necromancer.getAbilities().stream()
                    .filter(ability -> War3ID.fromString("Arai").equals(ability.getAlias())).findFirst().orElseThrow();
            game.getDefaultPlayerUnitOrderExecutor(3).issueImmediateOrder(necromancer.getHandleId(),
                    raiseDead.getHandleId(), OrderIds.raisedead, false);
            for (int tick = 0; tick < 100; tick++) game.update();
            final var skeletons = game.getUnitsIncludingPending().stream()
                    .filter(unit -> !unit.isDead() && (skeletonType.equals(unit.getTypeId()) || mageType.equals(unit.getTypeId())))
                    .toList();
            assertEquals(2, skeletons.size());
            assertEquals(masteryLevel, skeletons.stream().filter(unit -> mageType.equals(unit.getTypeId())).count());
            final var first = skeletons.get(0).getFirstAbilityOfType(CBuffTimedLife.class);
            final var second = skeletons.get(1).getFirstAbilityOfType(CBuffTimedLife.class);
            assertNotNull(first);
            assertNotNull(second);
            assertNotSame(first, second);
            assertEquals(duration, first.getDurationMax());
            assertEquals(duration, second.getDurationMax());
            game.getPlayer(3).setTechResearched(game, longevity, 0);
            game.getPlayer(3).setTechResearched(game, mastery, 0);
            assertEquals(duration, first.getDurationMax());
            final int expiration = game.getGameTurnTick()
                    + Math.round(first.getDurationRemaining(game, skeletons.get(0)) / WarsmashConstants.SIMULATION_STEP_TIME);
            while (game.getGameTurnTick() < expiration - 1) game.update();
            assertTrue(skeletons.stream().noneMatch(CUnit::isDead));
            for (int tick = 0; tick < 3; tick++) game.update();
            assertTrue(skeletons.stream().allMatch(CUnit::isDead));
        }
    }

    @Test
    void campaignOgreMagiCastsItsConfiguredBloodlustWithoutStacking() throws Exception {
        try (Fixture fixture = new Fixture("Human08")) {
            final var game = fixture.game;
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit ogre = game.createUnitSimple(War3ID.fromString("nomg"), 3, 0, 0, 0);
            final CUnit ally = game.createUnitSimple(War3ID.fromString("hfoo"), 3, 96, 0, 0);
            final CUnit enemy = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 768, 0, 0);
            final CUnit machine = game.createUnitSimple(War3ID.fromString("hmtt"), 3, 128, 0, 0);
            final var bloodlust = ogre.getFirstAbilityOfType(CAbilityBloodlust.class);
            assertNotNull(bloodlust);
            assertEquals(War3ID.fromString("ACbb"), bloodlust.getAlias());
            ogre.setMana(ogre.getMaximumMana());
            game.update();
            final var receiver = new CWidgetAbilityTargetCheckReceiver();
            bloodlust.checkCanTarget(game, ogre, OrderIds.bloodlust, enemy, receiver);
            assertNull(receiver.getTarget());
            bloodlust.checkCanTarget(game, ogre, OrderIds.bloodlust, machine, receiver);
            assertNull(receiver.getTarget());
            bloodlust.checkCanTarget(game, ogre, OrderIds.bloodlust, ally, receiver);
            assertSame(ally, receiver.getTarget());

            final int speed = ally.getSpeed();
            final var attack = ally.getUnitSpecificAttacks().get(0);
            final var orders = game.getDefaultPlayerUnitOrderExecutor(3);
            orders.issueImmediateOrder(ogre.getHandleId(), bloodlust.getHandleId(), OrderIds.bloodluston, false);
            assertTrue(bloodlust.isAutoCastOn());
            orders.issueImmediateOrder(ogre.getHandleId(), bloodlust.getHandleId(), OrderIds.bloodlustoff, false);
            assertFalse(bloodlust.isAutoCastOn());
            orders.issueTargetOrder(ogre.getHandleId(), bloodlust.getHandleId(), OrderIds.bloodlust,
                    ally.getHandleId(), false);
            for (int tick = 0; tick < 200; tick++) game.update();
            assertEquals(speed * 1.25f, ally.getSpeed(), 1f);
            assertEquals(0.4f, attack.getAttackSpeedModifier(), 0.001f);
            assertEquals(1.3f, ally.getModelScaleMultiplier(), 0.001f);
            final int manaAfterFirstCast = (int) ogre.getMana();
            orders.issueTargetOrder(ogre.getHandleId(), bloodlust.getHandleId(), OrderIds.bloodlust,
                    ally.getHandleId(), false);
            for (int tick = 0; tick < 45; tick++) game.update();
            assertTrue(ogre.getMana() < manaAfterFirstCast);
            assertEquals(speed * 1.25f, ally.getSpeed(), 1f);
            assertEquals(0.4f, attack.getAttackSpeedModifier(), 0.001f);
            assertEquals(1.3f, ally.getModelScaleMultiplier(), 0.001f);
            for (int tick = 0; tick < 820; tick++) game.update();
            assertEquals(speed, ally.getSpeed());
            assertEquals(0f, attack.getAttackSpeedModifier(), 0.001f);
            assertEquals(1f, ally.getModelScaleMultiplier(), 0.001f);

            enemy.setPoint(220, 0, game.getWorldCollision(), game.getRegionManager());
            enemy.setDisableAttacks(true);
            ogre.setDisableAttacks(true);
            ogre.setMana(ogre.getMaximumMana());
            orders.issueTargetOrder(ally.getHandleId(), ally.getFirstAbilityOfType(CAbilityAttack.class).getHandleId(),
                    OrderIds.attack, enemy.getHandleId(), false);
            orders.issueImmediateOrder(ogre.getHandleId(), bloodlust.getHandleId(), OrderIds.bloodluston, false);
            assertEquals(OrderIds.attack, ally.getCurrentBehavior().getHighlightOrderId());
            for (int tick = 0; tick < 80; tick++) game.update();
            assertEquals(0.4f, attack.getAttackSpeedModifier(), 0.001f);
            assertEquals(1f, machine.getModelScaleMultiplier(), 0.001f);
        }
    }

    @Test
    void campaignAssaultOrdersOnlyTheFormedWaveAndKeepsDefendersAtHome() throws Exception {
        try (Fixture fixture = new Fixture("Human09")) {
            final var game = fixture.game;
            final War3ID ghoul = War3ID.fromString("ugho");
            final CUnit defender = game.createUnitSimple(ghoul, 3, 0, 0, 0);
            final CUnit attacker = game.createUnitSimple(ghoul, 3, 0, 128, 0);
            final CUnit reserve = game.createUnitSimple(ghoul, 3, 0, 256, 0);
            final CUnit town = game.createUnitSimple(War3ID.fromString("htow"), 1, 1232, 0, 0);
            final CUnit soldier = game.createUnitSimple(War3ID.fromString("hfoo"), 1, 1040, 512, 0);
            game.update();

            fixture.call("InitDefense");
            fixture.call("AddDefenders", 1, ghoul.getValue());
            fixture.call("InitAssault");
            fixture.call("AddAssault", 1, ghoul.getValue());
            assertTrue(fixture.call("CaptainIsFull").visit(BooleanJassValueVisitor.getInstance()));
            assertEquals(100, fixture.commandValue("CaptainReadiness"));
            assertEquals(1, fixture.commandValue("CaptainGroupSize"));
            assertTrue(fixture.callAgainstPlayer("SuicidePlayer", town.getPlayerIndex()));
            var order = assertInstanceOf(COrderTargetPoint.class, attacker.getCurrentOrder());
            assertEquals(OrderIds.attack, order.getOrderId());
            assertEquals(town.getX(), order.getTarget(game).getX());
            assertEquals(town.getY(), order.getTarget(game).getY());
            assertNull(defender.getCurrentOrder());
            assertNull(reserve.getCurrentOrder());

            assertTrue(fixture.callAgainstPlayer("SuicidePlayerUnits", soldier.getPlayerIndex()));
            order = assertInstanceOf(COrderTargetPoint.class, attacker.getCurrentOrder());
            assertEquals(soldier.getX(), order.getTarget(game).getX());
            assertEquals(soldier.getY(), order.getTarget(game).getY());
            attacker.kill(game);
            assertFalse(fixture.callAgainstPlayer("SuicidePlayer", town.getPlayerIndex()));
            assertTrue(fixture.call("CaptainIsEmpty").visit(BooleanJassValueVisitor.getInstance()));
        }
    }

    @Test
    void scriptedAttackWaveOrdersOnlyTheRequestedUnitCountAndType() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            final War3ID ghoul = War3ID.fromString("ugho");
            final CUnit first = game.createUnitSimple(ghoul, 3, 0, 0, 0);
            final CUnit second = game.createUnitSimple(ghoul, 3, 0, 96, 0);
            final CUnit otherType = game.createUnitSimple(War3ID.fromString("hfoo"), 3, 0, 192, 0);
            final CUnit town = game.createUnitSimple(War3ID.fromString("htow"), 1, 1056, 0, 0);
            final CUnit explicitTarget = game.createUnitSimple(War3ID.fromString("hfoo"), 2, -1056, 0, 0);
            game.update();
            fixture.call("SuicideUnit", 1, ghoul.getValue());
            final var initial = assertInstanceOf(COrderTargetPoint.class, first.getCurrentOrder());
            assertEquals(OrderIds.attack, initial.getOrderId());
            assertEquals(town.getX(), initial.getTarget(game).getX());
            assertNull(second.getCurrentOrder());
            assertNull(otherType.getCurrentOrder());

            fixture.call("SuicideUnit", 1, ghoul.getValue());
            assertInstanceOf(COrderTargetPoint.class, second.getCurrentOrder());
            fixture.call("SuicideUnitEx", 1, ghoul.getValue(), explicitTarget.getPlayerIndex());
            final var redirected = assertInstanceOf(COrderTargetPoint.class, first.getCurrentOrder());
            assertEquals(explicitTarget.getX(), redirected.getTarget(game).getX());
            final var stillAttackingTown = assertInstanceOf(COrderTargetPoint.class, second.getCurrentOrder());
            assertEquals(town.getX(), stillAttackingTown.getTarget(game).getX());
        }
    }

    @Test
    void campaignSapperDestroysTreesWithinItsBlastRadius() throws Exception {
        try (Fixture fixture = new Fixture("Human08")) {
            final var game = fixture.game;
            final CUnit sapper = game.createUnitSimple(War3ID.fromString("ngsp"), 3, 0, 0, 0);
            final var ability = sapper.getFirstAbilityOfType(CAbilityKaboom.class);
            assertNotNull(ability);
            final War3ID treeId = War3ID.fromString("NTtw");
            final var target = game.internalCreateDestructable(treeId, 96, 0, null, null);
            final var nearbyTree = game.internalCreateDestructable(treeId, 0, 160, null, null);
            final var distantTree = game.internalCreateDestructable(treeId, 0, 448, null, null);
            game.update();
            game.getDefaultPlayerUnitOrderExecutor(3).issueTargetOrder(sapper.getHandleId(), ability.getHandleId(),
                    OrderIds.selfdestruct, target.getHandleId(), false);
            for (int tick = 0; tick < 60 && !sapper.isDead(); tick++) game.update();
            assertTrue(sapper.isDead());
            assertTrue(target.isDead());
            assertTrue(nearbyTree.isDead());
            assertFalse(distantTree.isDead());
        }
    }

    @Test
    void campaignMortarTeamCanClearTreesThroughItsAttackOrder() throws Exception {
        try (Fixture fixture = new Fixture("Human08")) {
            final var game = fixture.game;
            final CUnit mortar = game.createUnitSimple(War3ID.fromString("hmtm"), 3, 0, 0, 0);
            final var attack = mortar.getFirstAbilityOfType(CAbilityAttack.class);
            assertNotNull(attack);
            final var tree = game.internalCreateDestructable(War3ID.fromString("NTtw"), 480, 0, null, null);
            game.update();
            game.getDefaultPlayerUnitOrderExecutor(3).issueTargetOrder(mortar.getHandleId(), attack.getHandleId(),
                    OrderIds.attack, tree.getHandleId(), false);
            for (int tick = 0; tick < 500 && !tree.isDead(); tick++) game.update();
            assertTrue(tree.isDead());
        }
    }

    @Test
    void hiringAndLosingACampaignMercenaryUpdatesFoodUsed() throws Exception {
        try (Fixture fixture = new Fixture("Human08")) {
            final var game = fixture.game;
            final var player = game.getPlayer(3);
            final CUnit buyer = game.createUnitSimple(War3ID.fromString("Hpal"), player.getId(), 0, 0, 0);
            final CUnit camp = game.createUnitSimple(War3ID.fromString("nC00"),
                    WarsmashConstants.MAX_PLAYERS - 1, 160, 0, 0);
            final CAbilitySellUnits sale = camp.getFirstAbilityOfType(CAbilitySellUnits.class);
            assertNotNull(sale);
            for (int tick = 0; tick < 40; tick++) game.update();
            assertSame(buyer, camp.getNeutralBuildingData().getSelectedPlayerUnit(player.getId()));
            final int foodBefore = player.getFoodUsed();
            final War3ID mercenaryId = sale.getUnitsSold().get(0);
            final int mercenaryFood = game.getUnitData().getUnitType(mercenaryId).getFoodUsed();
            assertTrue(mercenaryFood > 0);
            game.getDefaultPlayerUnitOrderExecutor(player.getId()).issueImmediateOrder(camp.getHandleId(),
                    sale.getHandleId(), player.getId() | (1 << 8), false);
            game.update();
            final CUnit hired = game.getUnitsIncludingPending().stream()
                    .filter(unit -> unit.getPlayerIndex() == player.getId() && unit.getTypeId().equals(mercenaryId))
                    .findFirst().orElseThrow();
            assertEquals(foodBefore + mercenaryFood, player.getFoodUsed());
            hired.kill(game);
            game.update();
            assertEquals(foodBefore, player.getFoodUsed());
        }
    }

    @ParameterizedTest
    @CsvSource({ "SNdc,false", "ANdc,true" })
    void campaignConversionUsesTheMapRaceAndFiresTheSummonEvent(final String abilityId, final boolean automatic)
            throws Exception {
        try (Fixture fixture = new Fixture("Human06")) {
            final var game = fixture.game;
            game.setFogEnabled(false);
            game.setFogMaskEnabled(false);
            final CUnit caster = game.createUnitSimple(War3ID.fromString("UC05"), 3, 0, 0, 0);
            final CUnit villager = game.createUnitSimple(War3ID.fromString("nC00"), 8, 160, 0, 0);
            final var spell = assertInstanceOf(CAbilityDarkConversion.class, game.getAbilityData()
                    .createAbility(War3ID.fromString(abilityId), game.getHandleIdAllocator().createId()));
            caster.add(game, spell);
            final var player = game.getPlayer(3);
            final List<CommonTriggerExecutionScope> summons = new ArrayList<>();
            player.addEvent(new CPlayerEvent(fixture.program.getGlobals(), player, new Trigger(),
                    JassGameEventsWar3.EVENT_PLAYER_UNIT_SUMMON, null) {
                @Override
                public void fire(final CUnit unit, final TriggerExecutionScope scope) {
                    assertSame(caster, unit);
                    summons.add((CommonTriggerExecutionScope) scope);
                }
            });
            if (automatic) {
                player.setController(CMapControl.COMPUTER);
                game.update();
                assertEquals(5f, spell.getCastDelay());
                for (int tick = 0; tick < 80; tick++) game.update();
                assertFalse(villager.isUnitType(CUnitTypeJass.SLEEPING), "The casting delay must elapse first");
            }
            else {
                game.update();
                game.getDefaultPlayerUnitOrderExecutor(3).issueTargetOrder(caster.getHandleId(), spell.getHandleId(),
                        OrderIds.darkconversion, villager.getHandleId(), false);
            }
            for (int tick = 0; tick < 140 && !villager.isUnitType(CUnitTypeJass.SLEEPING); tick++) game.update();
            assertTrue(villager.isUnitType(CUnitTypeJass.SLEEPING));
            assertTrue(summons.isEmpty());
            for (int tick = 0; tick < 240; tick++) game.update();
            assertNull(game.getUnit(villager.getHandleId()));
            assertEquals(1, summons.size());
            final var summon = summons.get(0);
            assertSame(caster, summon.getSummoningUnit());
            assertEquals(War3ID.fromString("nzom"), summon.getSummonedUnit().getTypeId());
            assertEquals(player.getId(), summon.getSummonedUnit().getPlayerIndex());
        }
    }

    @Test
    void aiDistinguishesInvisibleTargetsFromTargetsRevealedToItsPlayer() throws Exception {
        try (Fixture fixture = new Fixture()) {
            final var game = fixture.game;
            final var globals = fixture.program.getGlobals();
            final var natives = fixture.program.getJassNativeManager();
            final var unit = game.createUnitSimple(War3ID.fromString("hfoo"), 2, 64, 64, 0);
            final var target = new HandleJassValue(globals.getHandleType("unit"), unit);
            final var observer = new HandleJassValue(globals.getHandleType("player"), game.getPlayer(3));
            final var other = new HandleJassValue(globals.getHandleType("player"), game.getPlayer(4));
            final var invisible = natives.getNative("UnitInvis");
            final var detected = natives.getNative("IsUnitDetected");
            final List<JassValue> targetAndObserver = List.of(target, observer);
            assertFalse(invisible.call(List.of(target), globals, TriggerExecutionScope.EMPTY)
                    .visit(BooleanJassValueVisitor.getInstance()));
            unit.addStateModBuff(new StateModBuff(StateModBuffType.INVISIBLE, 1));
            unit.computeUnitState(game, StateModBuffType.INVISIBLE);
            assertTrue(invisible.call(List.of(target), globals, TriggerExecutionScope.EMPTY)
                    .visit(BooleanJassValueVisitor.getInstance()));
            game.getPlayer(3).getFogOfWar().setVisible(game.getPathingGrid(), 64, 64, CFogState.VISIBLE);
            assertTrue(unit.isInvisibleTo(game, 3));
            assertFalse(detected.call(targetAndObserver, globals, TriggerExecutionScope.EMPTY)
                    .visit(BooleanJassValueVisitor.getInstance()));
            game.getPlayer(3).getFogOfWar().setDetecting(game.getPathingGrid(), 64, 64, CFogState.VISIBLE, (byte) 1);
            assertTrue(detected.call(targetAndObserver, globals, TriggerExecutionScope.EMPTY)
                    .visit(BooleanJassValueVisitor.getInstance()));
            assertFalse(unit.isInvisibleTo(game, 3));
            assertTrue(unit.isInvisibleTo(game, 4));
            assertFalse(detected.call(List.of(target, other), globals, TriggerExecutionScope.EMPTY)
                    .visit(BooleanJassValueVisitor.getInstance()));
        }
    }

    @Test
    void campaignAiCommandsReachTheirPlayerAndConsumeNewestFirst() throws Exception {
        try (Fixture live = new Fixture(); Fixture replay = new Fixture()) {
            live.game.addAiEnvironment(live.environment);
            replay.game.addAiEnvironment(replay.environment);
            final String empty = MissionStateFingerprint.ofRuntime(live.environment);
            live.game.commandAi(5, 99, 99);
            live.game.commandAi(3, 7, 0);
            live.game.commandAi(3, 6, -400);
            assertEquals(2, live.commandValue("CommandsWaiting"));
            assertEquals(6, live.commandValue("GetLastCommand"));
            assertEquals(-400, live.commandValue("GetLastData"));
            final String queued = MissionStateFingerprint.ofRuntime(live.environment);
            assertNotEquals(empty, queued);

            replay.game.commandAi(3, 7, 0);
            replay.game.commandAi(3, 6, -400);
            assertEquals(queued, MissionStateFingerprint.ofRuntime(replay.environment));
            replay.call("PopLastCommand");
            assertEquals(7, replay.commandValue("GetLastCommand"));
            assertEquals(0, replay.commandValue("GetLastData"));
            replay.call("PopLastCommand");
            replay.call("PopLastCommand");
            assertEquals(0, replay.commandValue("CommandsWaiting"));
            assertEquals(0, replay.commandValue("GetLastCommand"));
            assertEquals(empty, MissionStateFingerprint.ofRuntime(replay.environment));
        }
    }

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
        final JassAIEnvironment environment;
        Fixture() throws Exception {
            this("Human01");
        }

        Fixture(final String chapter) throws Exception {
            for (final String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
                final var channel = Files.newByteChannel(RetailTestData.require(name));
                archives.add(new MpqDataSource(new MPQArchive(channel), channel));
            }
            map = new War3Map(new CompoundDataSource(new ArrayList<>(archives)), "Maps\\Campaign\\" + chapter + ".w3m");
            game = RetailSimulationTestSupport.simulation(map);
            game.getPlayer(3).setGold(5000);
            game.getPlayer(3).setLumber(5000);
            game.getPlayer(3).setFoodCap(100);
            environment = new JassAIEnvironment(program, null, null, null, null,
                    new War3MapConfig(WarsmashConstants.MAX_PLAYERS), game, 3);
        }
        com.etheller.interpreter.ast.value.JassValue call(final String nativeName, final int... arguments) {
            return program.getJassNativeManager().getNative(nativeName).call(
                    Arrays.stream(arguments).mapToObj(IntegerJassValue::of).collect(java.util.stream.Collectors.toList()),
                    program.getGlobals(), TriggerExecutionScope.EMPTY);
        }
        boolean callAgainstPlayer(final String nativeName, final int playerIndex) {
            final var globals = program.getGlobals();
            final var target = new HandleJassValue(globals.getHandleType("player"), game.getPlayer(playerIndex));
            return program.getJassNativeManager().getNative(nativeName)
                    .call(List.of(target, BooleanJassValue.TRUE), globals, TriggerExecutionScope.EMPTY)
                    .visit(BooleanJassValueVisitor.getInstance());
        }
        boolean produce(final int qty, final String id) {
            return call("SetProduce", qty, War3ID.fromString(id).getValue(), 0).visit(BooleanJassValueVisitor.getInstance());
        }
        int commandValue(final String name) {
            return call(name).visit(IntegerJassValueVisitor.getInstance());
        }
        @Override public void close() throws Exception {
            map.close();
            WarsmashConstants.RACE_MANAGER = previous;
            for (final var archive : archives) archive.close();
        }
    }
}
