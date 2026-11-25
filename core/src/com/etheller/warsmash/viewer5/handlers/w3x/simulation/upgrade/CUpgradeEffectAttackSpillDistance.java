package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/** Additional reach beyond the first target of a line weapon. */
public record CUpgradeEffectAttackSpillDistance(float base, float increment) implements CUpgradeEffect {
	public float getBonus(final int level) {
		return Util.levelValue(this.base, this.increment, level - 1);
	}

	@Override
	public void apply(final CSimulation simulation, final CUnit unit, final int level) {
	}

	@Override
	public void unapply(final CSimulation simulation, final CUnit unit, final int level) {
	}
}
