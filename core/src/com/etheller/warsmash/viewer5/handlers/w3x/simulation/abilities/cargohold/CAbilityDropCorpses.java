package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;

public final class CAbilityDropCorpses extends CAbilityNoTargetSpellBase {
	public CAbilityDropCorpses(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.unloadallcorpses;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit wagon, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo && !cargo.isEmpty()) receiver.useOk();
		else receiver.disabled();
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit wagon, final int orderId, final AbilityTarget target) {
		if (orderId == getBaseOrderId()) {
			doEffect(game, wagon, target);
			return false;
		}
		return super.checkBeforeQueue(game, wagon, orderId, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit wagon, final AbilityTarget target) {
		if (wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo) cargo.unloadAllInstant(game, wagon);
		return false;
	}
}
