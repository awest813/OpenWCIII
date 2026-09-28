package com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;

public final class CUpgradeEffectDefenseType implements CUpgradeEffect {
	private final CDefenseType defenseType;

	public CUpgradeEffectDefenseType(final int defenseType) {
		if (defenseType < 0 || defenseType >= CDefenseType.VALUES.length) {
			throw new IllegalArgumentException("Invalid upgrade armor type: " + defenseType);
		}
		this.defenseType = CDefenseType.VALUES[defenseType];
	}

	@Override
	public void apply(final CSimulation simulation, final CUnit unit, final int level) {
		unit.addDefenseTypeUpgrade(this, this.defenseType);
	}

	@Override
	public void unapply(final CSimulation simulation, final CUnit unit, final int level) {
		unit.removeDefenseTypeUpgrade(this);
	}
}
