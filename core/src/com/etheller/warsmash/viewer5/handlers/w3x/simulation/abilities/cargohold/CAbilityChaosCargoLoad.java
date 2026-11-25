package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityResearchIndicator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;

public final class CAbilityChaosCargoLoad extends CAbilityResearchIndicator {
	public static final War3ID CODE = War3ID.fromString("Achl");
	private static final War3ID RESEARCH = War3ID.fromString("Roch");
	private War3ID allowedUnitType = War3ID.NONE;

	public CAbilityChaosCargoLoad(final int handleId, final War3ID alias) {
		super(handleId, alias, CODE, RESEARCH);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		super.populateData(data, level);
		this.allowedUnitType = data.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, -1);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}

	public boolean allows(final CUnit passenger) {
		return !isDisabled() && passenger.getTypeId().equals(this.allowedUnitType);
	}
}
