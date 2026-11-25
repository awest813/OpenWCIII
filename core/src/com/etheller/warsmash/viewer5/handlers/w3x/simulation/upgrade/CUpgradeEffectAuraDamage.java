package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/** Extra attack damage granted by the named aura on a researched unit. */
public record CUpgradeEffectAuraDamage(War3ID ability, float base, float increment) implements CUpgradeEffect {
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
