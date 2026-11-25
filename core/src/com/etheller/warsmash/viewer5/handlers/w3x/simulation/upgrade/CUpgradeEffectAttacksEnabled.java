package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

public final class CUpgradeEffectAttacksEnabled implements CUpgradeEffect {
	private final int mask;

	public CUpgradeEffectAttacksEnabled(final int mask) {
		this.mask = mask;
	}

	public int getMask() {
		return this.mask;
	}

	@Override
	public void apply(final CSimulation simulation, final CUnit unit, final int level) {
		unit.computeUnitState(simulation, StateModBuffType.DISABLE_ATTACK);
	}

	@Override
	public void unapply(final CSimulation simulation, final CUnit unit, final int level) {
		unit.computeUnitState(simulation, StateModBuffType.DISABLE_ATTACK);
	}
}
