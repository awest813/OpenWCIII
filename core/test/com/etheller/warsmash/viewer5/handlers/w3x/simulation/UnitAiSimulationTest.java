package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;

import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSourceDescriptor;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorPatrol;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

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

    @Test void holdPositionDoesNotAcquireAnEnemyOutsideWeaponRange() {
        final CUnit unit = footman(0, 0, 0);
        footman(1, 400, 0);
        assertFalse(unit.autoAcquireAttackTargets(game, true));
        assertTrue(unit.autoAcquireAttackTargets(game, false), "Moving units may pursue this enemy");
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
