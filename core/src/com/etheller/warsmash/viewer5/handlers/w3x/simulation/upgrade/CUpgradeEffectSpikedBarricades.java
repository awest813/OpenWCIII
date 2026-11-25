package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/** Flat retaliation damage read using the struck building's current owner. */
public record CUpgradeEffectSpikedBarricades(float base, float increment) implements CUpgradeEffect {
	public float getDamage(final int level) {
		return Util.levelValue(this.base, this.increment, level - 1);
	}

	@Override
	public void apply(final CSimulation simulation, final CUnit unit, final int level) {
	}

	@Override
	public void unapply(final CSimulation simulation, final CUnit unit, final int level) {
	}
}
