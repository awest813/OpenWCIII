package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

class WorkerAutoharvestTest {

	@Test
	void testAutoharvestOrdersAcceptedByHarvestAbility() {
		// Peon/Peasant harvest ability: handleId=1, alias=Ahar, code=Ahar, damageToTree=1, goldCapacity=10, lumberCapacity=10, castRange=64, duration=0.433
		final War3ID ahar = War3ID.fromString("Ahar");
		final CAbilityHarvest harvest = new CAbilityHarvest(1, ahar, ahar, 1, 10, 10, 64f, 0.433f);

		// 1. Check can use for autoharvestgold
		final BooleanAbilityActivationReceiver goldUse = new BooleanAbilityActivationReceiver().reset();
		harvest.checkCanUse(null, null, OrderIds.autoharvestgold, goldUse);
		assertTrue(goldUse.isOk(), "autoharvestgold order must be accepted by checkCanUse");

		// 2. Check can use for autoharvestlumber
		final BooleanAbilityActivationReceiver lumberUse = new BooleanAbilityActivationReceiver().reset();
		harvest.checkCanUse(null, null, OrderIds.autoharvestlumber, lumberUse);
		assertTrue(lumberUse.isOk(), "autoharvestlumber order must be accepted by checkCanUse");

		// 3. Check can target no target for autoharvestgold
		final BooleanAbilityTargetCheckReceiver<Void> goldTarget = BooleanAbilityTargetCheckReceiver.<Void>getInstance().reset();
		harvest.checkCanTargetNoTarget(null, null, OrderIds.autoharvestgold, goldTarget);
		assertTrue(goldTarget.isTargetable(), "autoharvestgold must be accepted by checkCanTargetNoTarget");

		// 4. Check can target no target for autoharvestlumber
		final BooleanAbilityTargetCheckReceiver<Void> lumberTarget = BooleanAbilityTargetCheckReceiver.<Void>getInstance().reset();
		harvest.checkCanTargetNoTarget(null, null, OrderIds.autoharvestlumber, lumberTarget);
		assertTrue(lumberTarget.isTargetable(), "autoharvestlumber must be accepted by checkCanTargetNoTarget");
	}

	@Test
	void testActivationReceiverResetClearsOkState() {
		final BooleanAbilityActivationReceiver receiver = BooleanAbilityActivationReceiver.INSTANCE;
		receiver.useOk();
		assertTrue(receiver.isOk());

		receiver.reset();
		assertFalse(receiver.isOk(), "reset() must clear the ok state to false");
	}
}
