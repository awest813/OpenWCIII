package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public class CAbilityDarkRitual extends CAbilityDeathPact {

	public CAbilityDarkRitual(int handleId, War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.darkritual;
	}

	@Override
	protected boolean requiresUndeadTarget() {
		return false;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		super.populateData(data, level);
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}
}
