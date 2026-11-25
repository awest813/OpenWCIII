package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSourceDescriptor;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.CAbilityAbilityBuilderActiveUnitTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorPatrol;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;

class UnitAiSimulationTest {
    private static DataSource source;
    private static War3Map map;
    private static CRaceManager previousRaces;
    private CSimulation game;

    @BeforeAll static void open() throws Exception {
        previousRaces = WarsmashConstants.RACE_MANAGER;
        final List<DataSource> archives = new ArrayList<>();
        for (final String path : RetailTestData.requireAll()) {
            archives.add(new MpqDataSourceDescriptor(path).createDataSource());
        }
        source = new CompoundDataSource(archives);
        map = new War3Map(source, "Maps\\Campaign\\Human01.w3m");
    }

    @AfterAll static void close() throws Exception {
        WarsmashConstants.RACE_MANAGER = previousRaces;
        if (map != null) map.close();
        if (source != null) source.close();
    }

    @BeforeEach void setup() throws Exception {
        game = RetailSimulationTestSupport.simulation(map);
        game.setFogEnabled(false);
        game.setFogMaskEnabled(false);
    }

    private CUnit footman(final int owner, final float x, final float y) {
        return game.createUnitSimple(War3ID.fromString("hfoo"), owner, x, y, 0);
    }

    @Test void countdownFreezesDuringPauseAndCompletesAtItsRescheduledTick() {
        final AtomicInteger fires = new AtomicInteger();
        final CTimer timer = new CTimer() {
            @Override public void onFire(final CSimulation simulation) {
                fires.incrementAndGet();
            }
        };
        timer.setTimeoutTime(2);
        timer.start(game);
        for (int tick = 0; tick < 10; tick++) game.update();
        timer.pause(game);
        for (int tick = 0; tick < 50; tick++) game.update();
        timer.pause(game);
        assertFalse(timer.isRunning());
        assertEquals(0.5f, timer.getElapsed(game));
        assertEquals(1.5f, timer.getRemaining(game));
        assertEquals(0, fires.get());

        timer.resume(game);
        timer.pause(game);
        timer.resume(game);
        assertTrue(timer.isRunning());
        for (int tick = 0; tick < 29; tick++) game.update();
        assertEquals(0, fires.get());
        game.update();
        assertEquals(1, fires.get());
        assertEquals(0, timer.getRemaining(game));

        timer.startRepeatingTimerWithDelay(game, 1);
        for (int tick = 0; tick < 10; tick++) game.update();
        timer.pause(game);
        assertEquals(2, timer.getTimeoutTime());
        assertEquals(0.5f, timer.getRemaining(game));
        timer.resume(game);
        for (int tick = 0; tick < 10; tick++) game.update();
        assertEquals(2, fires.get());
        assertEquals(2, timer.getRemaining(game));

        timer.setTimeoutTime(0.5f);
        timer.setRepeats(false);
        timer.start(game);
        for (int tick = 0; tick < 10; tick++) game.update();
        assertEquals(3, fires.get());
        assertFalse(timer.isRunning());
    }

    @Test void legacyUnitNamesResolveBeforeTheUnitIsCreated() {
        final var unitData = game.getUnitData();
        final var hydralisk = unitData.getUnitTypeByJassLegacyName("hydralisk");
        assertNotNull(hydralisk);
        assertEquals(War3ID.fromString("zhyd"), hydralisk.getTypeId());
        assertSame(hydralisk, unitData.getUnitTypeByJassLegacyName("Hydralisk"));
        assertNull(unitData.getUnitTypeByJassLegacyName("unknown-legacy-unit"));
        assertNull(unitData.getUnitTypeByJassLegacyName(null));
    }

    @Test void scriptedItemPositionDropsHeldItemsAndMovesGroundItems() {
        final CUnit hero = game.createUnitSimple(War3ID.fromString("Hjai"), 0, 0, 0, 0);
        final CItem item = game.createItem(War3ID.fromString("phea"), 0, 0);
        final var inventory = hero.getInventoryData();
        assertEquals(0, inventory.giveItem(game, hero, item, 0, false));
        assertTrue(item.isHidden());
        game.getWorldCollision().enumItemsInRect(new Rectangle(-32, -32, 64, 64), held -> {
            fail("Item enumeration must exclude carried items");
            return false;
        });
        item.setScriptedPosition(game, 96, 128);
        assertNull(inventory.getItemInSlot(0));
        assertNull(item.getContainedUnit());
        assertFalse(item.isHidden());
        assertEquals(96f, item.getX());
        assertEquals(128f, item.getY());
        item.setHidden(true);
        item.setScriptedPosition(game, 160, 192);
        assertEquals(160f, item.getX());
        assertEquals(192f, item.getY());
        final List<CItem> enumerated = new ArrayList<>();
        game.getWorldCollision().enumItemsInRect(new Rectangle(144, 176, 32, 32), hidden -> {
            enumerated.add(hidden);
            hidden.setHidden(false);
            return false;
        });
        assertEquals(List.of(item), enumerated);
        assertFalse(item.isHidden());
    }

    @ParameterizedTest
    @CsvSource({ "hmpr,20,true", "nitp,12,false", "nith,25,false" })
    void priestHealUsesRetailDataAndPlayerAutocastOrders(final String type, final int amount,
            final boolean startsAutocasting) {
        final CUnit priest = game.createUnitSimple(War3ID.fromString(type), 0, 0, 0, 0);
        final CUnit ally = footman(0, 80, 0);
        final CUnit enemy = footman(1, 100, 0);
        final CUnit machine = game.createUnitSimple(War3ID.fromString("hmtt"), 0, 120, 0, 0);
        priest.setUnitAnimationListener(CUnitAnimationListener.DO_NOTHING);
        enemy.setPaused(true);
        ally.setPaused(true);
        machine.setPaused(true);
        for (final CUnit unit : List.of(ally, enemy, machine)) unit.setLife(game, unit.getMaxLife() - 100);
        priest.setMana(50);
        final var heal = priest.getAbilities().stream().filter(CAbilityAbilityBuilderActiveUnitTarget.class::isInstance)
                .map(CAbilityAbilityBuilderActiveUnitTarget.class::cast)
                .filter(ability -> ability.getBaseOrderId() == OrderIds.heal).findFirst().orElse(null);
        assertNotNull(heal, () -> "Priest abilities: " + priest.getUnitType().getAbilityList());
        assertEquals(startsAutocasting, heal.isAutoCastOn());
        final CPlayerUnitOrderExecutor orders = new CPlayerUnitOrderExecutor(game, 0);
        orders.issueImmediateOrder(priest.getHandleId(), heal.getHandleId(), OrderIds.healoff, false);
        assertFalse(heal.isAutoCastOn());
        assertFalse(priest.autoAcquireAutocastTargets(game, false));
        final CWidgetAbilityTargetCheckReceiver receiver = new CWidgetAbilityTargetCheckReceiver();
        heal.checkCanTarget(game, priest, OrderIds.heal, enemy, receiver);
        assertNull(receiver.getTarget());
        heal.checkCanTarget(game, priest, OrderIds.heal, machine, receiver);
        assertNull(receiver.getTarget());
        heal.checkCanTarget(game, priest, OrderIds.heal, ally, receiver);
        assertSame(ally, receiver.getTarget());

        final float lifeBefore = ally.getLife();
        orders.issueTargetOrder(priest.getHandleId(), heal.getHandleId(), OrderIds.heal, ally.getHandleId(), false);
        for (int tick = 0; tick < 25; tick++) game.update();
        assertEquals(lifeBefore + amount, ally.getLife(), 1);
        assertTrue(priest.getMana() < 50);
        for (int tick = 0; tick < 40; tick++) game.update();

        orders.issueImmediateOrder(priest.getHandleId(), heal.getHandleId(), OrderIds.healon, false);
        assertTrue(heal.isAutoCastOn());
        final CAbilityMove move = priest.getFirstAbilityOfType(CAbilityMove.class);
        orders.issueImmediateOrder(priest.getHandleId(), move.getHandleId(), OrderIds.holdposition, false);
        game.update();
        assertTrue(priest.isHoldingPosition());
        assertFalse(priest.autoAcquireAutocastTargets(game, true));
        orders.issueImmediateOrder(priest.getHandleId(), 0, OrderIds.stop, false);
        assertTrue(priest.autoAcquireAutocastTargets(game, false));
    }

    @Test void attackMovePriestHealsAndContinuesTowardDestination() {
        final CUnit priest = game.createUnitSimple(War3ID.fromString("hmpr"), 0, 0, 0, 0);
        final CUnit ally = footman(0, 80, 0);
        priest.setUnitAnimationListener(CUnitAnimationListener.DO_NOTHING);
        priest.setMana(50);
        ally.setPaused(true);
        ally.setLife(game, ally.getMaxLife() - 20);
        final CAbilityAttack attack = priest.getFirstAbilityOfType(CAbilityAttack.class);
        new CPlayerUnitOrderExecutor(game, 0).issuePointOrder(priest.getHandleId(), attack.getHandleId(),
                OrderIds.attack, 500, 0, false);
        for (int tick = 0; tick < 35; tick++) game.update();
        assertEquals(ally.getMaxLife(), ally.getLife());
        for (int tick = 0; tick < 80; tick++) game.update();
        assertTrue(priest.getX() > 400);
    }

    @Test void repairOrderCanFinishConstructionAtFullHealth() {
        final CUnit barracks = game.createUnitSimple(War3ID.fromString("hbar"), 0, 0, 0, 0);
        final CUnit worker = game.createUnitSimple(War3ID.fromString("hpea"), 0, -80, 0, 0);
        barracks.setUnitAnimationListener(CUnitAnimationListener.DO_NOTHING);
        barracks.setConstructing(true);
        barracks.setConstructingPaused(true);
        barracks.setConstructionProgress(barracks.getUnitType().getBuildTime() - 2 * WarsmashConstants.SIMULATION_STEP_TIME);
        assertEquals(barracks.getMaxLife(), barracks.getLife());

        final CAbilityHumanRepair repair = worker.getFirstAbilityOfType(CAbilityHumanRepair.class);
        final CWidgetAbilityTargetCheckReceiver receiver = new CWidgetAbilityTargetCheckReceiver();
        repair.checkCanTarget(game, worker, OrderIds.repair, barracks, receiver);
        assertSame(barracks, receiver.getTarget());
        new CPlayerUnitOrderExecutor(game, 0).issueTargetOrder(worker.getHandleId(), repair.getHandleId(),
                OrderIds.repair, barracks.getHandleId(), false);
        for (int tick = 0; tick < 64 && barracks.isConstructing(); tick++) game.update();
        assertFalse(barracks.isConstructing());
    }

    @Test void configuredRepairCanRestoreBridgeHealth() {
        final CUnit worker = game.createUnitSimple(War3ID.fromString("hpea"), 0, 0, 0, 0);
        final CDestructableType type = new CDestructableType(War3ID.fromString("DTbr"), "Bridge", 100,
                EnumSet.of(CTargetType.BRIDGE), "wood", 10, 10, 10, 10, 0, null, null);
        final CDestructable bridge = new CDestructable(game.getHandleIdAllocator().createId(),
                32, 0, 50, type, null, null);
        game.getPlayer(0).setGold(100);
        game.getPlayer(0).setLumber(100);
        final War3ID repairId = War3ID.fromString("Ahrp");
        final CAbilityHumanRepair repair = new CAbilityHumanRepair(game.getHandleIdAllocator().createId(),
                repairId, repairId, EnumSet.of(CTargetType.BRIDGE), 0, 1, 1, 1, 1, 100);
        worker.add(game, repair);
        final CWidgetAbilityTargetCheckReceiver receiver = new CWidgetAbilityTargetCheckReceiver();
        repair.checkCanTarget(game, worker, OrderIds.repair, bridge, receiver);
        assertSame(bridge, receiver.getTarget());
        final var behavior = repair.begin(game, worker, OrderIds.repair, bridge);
        behavior.begin(game);
        behavior.update(game);
        assertTrue(bridge.getLife() > 50);
    }

    @Test void holdPositionDoesNotAcquireAnEnemyOutsideWeaponRange() {
        final CUnit unit = footman(0, 0, 0);
        footman(1, 400, 0);
        assertFalse(unit.autoAcquireAttackTargets(game, true));
        assertTrue(unit.autoAcquireAttackTargets(game, false), "Moving units may pursue this enemy");
    }

    @ParameterizedTest
    @CsvSource({ "true, 1", "false, 1", "true, 0" })
    void onlyGuardedNeutralCreepsAbandonAnAutonomousChase(final boolean guarding, final int neutral) {
        final int owner = neutral == 1 ? WarsmashConstants.MAX_PLAYERS - 4 : 1;
        final CUnit chaser = footman(owner, -768, 0);
        final CUnit target = footman(0, -480, 0);
        target.setDisableAttacks(true);
        chaser.setCreepGuard(guarding);
        assertEquals(600, game.getGameplayConstants().getGuardDistance());
        assertEquals(1000, game.getGameplayConstants().getMaxGuardDistance());
        assertTrue(chaser.autoAcquireAttackTargets(game, false));
        target.setPointAndCheckUnstuck(896, 0, game);
        float furthest = chaser.getX();
        for (int tick = 0; tick < 340; tick++) {
            game.update();
            furthest = Math.max(furthest, chaser.getX());
        }
        if (neutral == 1 && guarding) {
            assertTrue(furthest > 150 && furthest < 255, "the hard limit bounds the pursuit");
            assertEquals(-768, chaser.getX(), 35, "the creep returns to its original position");
        }
        else {
            assertTrue(chaser.getX() > 650, "the unguarded chaser reaches its target");
        }
    }

    @Test void holdPositionStillFindsAnEnemyInWeaponRange() {
        final CUnit unit = footman(0, 0, 0);
        footman(1, 400, 0);
        final CUnit nearby = footman(1, 64, 0);
        assertTrue(unit.autoAcquireAttackTargets(game, true));
        assertSame(unit.getAttackBehavior(), unit.getCurrentBehavior());
        assertSame(nearby, unit.getAttackBehavior().getTarget());
        assertEquals(0, unit.getX());
        assertEquals(0, unit.getY());
    }

    @Test void patrolKeepsItsDestinationUntilItArrives() {
        final CUnit unit = footman(0, 0, 0);
        final CBehaviorPatrol patrol = unit.getPatrolBehavior();
        final AbilityPointTarget destination = new AbilityPointTarget(512, 0);
        patrol.reset(destination);
        assertSame(unit.getMoveBehavior(), patrol.update(game));
        assertSame(destination, patrol.getTarget(), "First update must not skip the first leg");
        patrol.update(game);
        assertSame(destination, patrol.getTarget(), "Resuming patrol must keep its unfinished leg");
        unit.setPointAndCheckUnstuck(512, 0, game);
        patrol.update(game);
        assertEquals(0, patrol.getTarget().getX());
        assertEquals(0, patrol.getTarget().getY());
    }

    @Test void rejectedAutocastDoesNotSuppressNormalTargetAcquisition() {
        final CUnit unit = footman(0, 0, 0);
        final CUnit enemy = footman(1, 64, 0);
        unit.setAutocastAbility(autocast(true, true, new AtomicInteger()));
        assertFalse(unit.autoAcquireAutocastTargets(game, false), "No installed ability accepts the synthetic order");
        assertTrue(unit.autoAcquireTargets(game, false));
        assertSame(enemy, unit.getAttackBehavior().getTarget());
    }

    @Test void unavailableAutocastDoesNotScanTargets() {
        final CUnit unit = footman(0, 0, 0);
        footman(1, 64, 0);
        final AtomicInteger checked = new AtomicInteger();
        unit.setAutocastAbility(autocast(true, false, checked));
        assertFalse(unit.autoAcquireAutocastTargets(game, false));
        assertEquals(0, checked.get(), "Cooldown/mana failures should be checked before scanning");
    }

    @Test void switchedOffAutocastDoesNotScanTargets() {
        final CUnit unit = footman(0, 0, 0);
        footman(1, 64, 0);
        final AtomicInteger checked = new AtomicInteger();
        unit.setAutocastAbility(autocast(false, true, checked));
        assertFalse(unit.autoAcquireAutocastTargets(game, false));
        assertEquals(0, checked.get());
    }

    @Test void holdPositionDoesNotAutocastBeyondCastRange() {
        final CUnit unit = footman(0, 0, 0);
        footman(1, 400, 0);
        final AtomicInteger checked = new AtomicInteger();
        unit.setAutocastAbility(autocast(true, true, checked));
        unit.autoAcquireAutocastTargets(game, true);
        assertEquals(1, checked.get(), "Only the caster is in range; the distant unit must not be checked");
        checked.set(0);
        unit.autoAcquireAutocastTargets(game, false);
        assertEquals(2, checked.get(), "Moving casters may consider the distant target");
    }

    @Test void enablingTheSameAutocastTwiceDoesNotSwitchItOff() {
        final CUnit worker = game.createUnitSimple(War3ID.fromString("hpea"), 0, 0, 0, 0);
        final var repair = worker.getFirstAbilityOfType(
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityHumanRepair.class);
        assertNotNull(repair);
        repair.setAutoCastOn(worker, true);
        repair.setAutoCastOn(worker, true);
        assertTrue(repair.isAutoCastOn());
        repair.setAutoCastOn(worker, false);
        assertFalse(repair.isAutoCastOn());
    }

    @Test void automaticRepairPreservesHoldPositionAndQueuedOrders() {
        final CUnit worker = game.createUnitSimple(War3ID.fromString("hpea"), 0, 0, 0, 0);
        final CUnit farm = game.createUnitSimple(War3ID.fromString("hhou"), 0, 64, 0, 0);
        farm.setLife(game, farm.getMaximumLife() / 2);
        final var repair = worker.getFirstAbilityOfType(
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityHumanRepair.class);
        repair.setAutoCastOn(worker, true);
        assertTrue(worker.order(game,
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.holdposition, null));
        final var queued = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderNoTarget(
                0, com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.stop, true);
        worker.getOrderQueue().add(queued);
        assertTrue(worker.autoAcquireAutocastTargets(game, true));
        assertEquals(List.of(queued), new ArrayList<>(worker.getOrderQueue()));
        worker.getOrderQueue().clear();
        assertSame(worker.getHoldPositionBehavior(), worker.pollNextOrderBehavior(game));
    }

    @Test void newPatrolAndAttackMoveOrdersClearPendingAcquisitionState() {
        final CUnit unit = footman(0, 0, 0);
        final CUnit enemy = footman(1, 64, 0);
        final AbilityPointTarget destination = new AbilityPointTarget(512, 0);
        final var attackMove = unit.getAttackMoveBehavior();
        attackMove.reset(destination);
        assertTrue(attackMove.isWithinRange(game));
        enemy.setHidden(true);
        attackMove.reset(destination);
        assertSame(unit.getMoveBehavior(), attackMove.update(game));
        enemy.setHidden(false);
        final var patrol = unit.getPatrolBehavior();
        patrol.reset(destination);
        assertTrue(patrol.isWithinRange(game));
        enemy.setHidden(true);
        patrol.reset(destination);
        assertSame(unit.getMoveBehavior(), patrol.update(game));
        assertSame(destination, patrol.getTarget());
    }

    @Test void cancellingQueuedStopDoesNotRequireAnAbilityHandle() {
        final CUnit unit = footman(0, 0, 0);
        final var stop = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderNoTarget(
                0, com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.stop, true);
        unit.getOrderQueue().add(stop);
        assertDoesNotThrow(() -> unit.order(game,
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.move,
                new AbilityPointTarget(512, 0)));
        assertTrue(unit.getOrderQueue().isEmpty());
        unit.getOrderQueue().add(stop);
        unit.setAcceptingOrders(false);
        assertDoesNotThrow(() -> unit.order(game,
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.stop, null));
        assertEquals(1, unit.getOrderQueue().size());
    }

    @Test void followHandsOffToCombatInsteadOfOverwritingTheAttack() {
        final CUnit unit = footman(0, 0, 0);
        final CUnit ally = footman(0, 256, 0);
        final var follow = unit.getFollowBehavior();
        follow.reset(game, com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.move, ally);
        final CUnit enemy = footman(1, 64, 0);
        assertSame(unit.getAttackBehavior(), follow.update(game));
        assertSame(enemy, unit.getAttackBehavior().getTarget());
    }

    @Test void startingFollowDoesNotAcquireCombatInsideOrderInitialization() {
        final CUnit unit = footman(0, 0, 0);
        final CUnit ally = footman(0, 256, 0);
        footman(1, 64, 0);
        final var before = unit.getCurrentBehavior();
        unit.getFollowBehavior().reset(game,
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.move, ally);
        assertSame(before, unit.getCurrentBehavior());
    }

    @Test void distantFollowKeepsFollowAsItsResumeBehavior() {
        final CUnit unit = footman(0, 0, 0);
        final CUnit ally = footman(0, 900, 0);
        assertTrue(unit.order(game,
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.move, ally));
        assertSame(unit.getMoveBehavior(), unit.getCurrentBehavior());
        assertSame(unit.getFollowBehavior(), unit.pollNextOrderBehavior(game));
    }

    @Test void patrolSkipsAnExhaustedLegButKeepsACombatInterruptedLeg() {
        final CUnit unit = footman(0, 0, 0);
        final var patrol = unit.getPatrolBehavior();
        final AbilityPointTarget destination = new AbilityPointTarget(512, 0);
        patrol.reset(destination);
        patrol.endMove(game, true);
        patrol.update(game);
        assertSame(destination, patrol.getTarget(), "Combat interruption must retain the destination");
        patrol.onMoveGiveUp(game);
        patrol.update(game);
        assertEquals(0, patrol.getTarget().getX(), "Exhausted route should advance to the next leg");
        patrol.reset(destination);
        patrol.update(game);
        assertSame(destination, patrol.getTarget(), "New patrol must clear old failure state");
    }

    @Test void exhaustedAttackMoveAllowsTheNextQueuedOrderToRun() {
        final CUnit unit = footman(0, 0, 0);
        final var attackMove = unit.getAttackMoveBehavior();
        attackMove.reset(new AbilityPointTarget(512, 0));
        unit.setDefaultBehavior(attackMove);
        final var move = unit.getFirstAbilityOfType(
                com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityMove.class);
        final var queued = new com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint(
                move.getHandleId(), com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds.move,
                new AbilityPointTarget(0, 512), true);
        unit.getOrderQueue().add(queued);
        attackMove.endMove(game, true);
        assertSame(attackMove, unit.pollNextOrderBehavior(game));
        assertEquals(1, unit.getOrderQueue().size());
        attackMove.onMoveGiveUp(game);
        assertSame(unit.getMoveBehavior(), unit.pollNextOrderBehavior(game));
        assertTrue(unit.getOrderQueue().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private CAutocastAbility autocast(final boolean enabled, final boolean ready, final AtomicInteger checked) {
        return (CAutocastAbility) Proxy.newProxyInstance(CAutocastAbility.class.getClassLoader(),
                new Class<?>[] { CAutocastAbility.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "isDisabled": return false;
                    case "isAutoCastOn": return enabled;
                    case "getAutocastType": return AutocastType.NEARESTVALID;
                    case "getBaseOrderId": return Integer.MAX_VALUE;
                    case "getCastRange": return 100f;
                    case "checkCanUse":
                        if (ready) ((AbilityActivationReceiver) args[3]).useOk();
                        return null;
                    case "checkCanAutoTarget":
                        checked.incrementAndGet();
                        ((AbilityTargetCheckReceiver<CWidget>) args[4]).targetOk((CWidget) args[3]);
                        return null;
                    case "setAutoCastOff": return null;
                    default: throw new AssertionError(method.getName());
                    }
                });
    }
}
