package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;

public final class CAbilityItemLifeRegeneration extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Arel");
	private float lifePerSecond;
	private CUnit holder;

	public CAbilityItemLifeRegeneration(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		final float bonus = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		if (this.holder != null) {
			this.holder.setLifeRegenBonus(this.holder.getLifeRegenBonus() + bonus - this.lifePerSecond);
		}
		this.lifePerSecond = bonus;
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		if (this.holder == null) {
			this.holder = unit;
			unit.setLifeRegenBonus(unit.getLifeRegenBonus() + this.lifePerSecond);
		}
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		if (this.holder == unit) {
			unit.setLifeRegenBonus(unit.getLifeRegenBonus() - this.lifePerSecond);
			this.holder = null;
		}
	}
}
