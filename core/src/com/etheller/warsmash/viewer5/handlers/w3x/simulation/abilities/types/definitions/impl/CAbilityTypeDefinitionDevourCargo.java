package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityDevourCargo;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.impl.CAbilityTypeCargoHold;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.impl.CAbilityTypeCargoHoldLevelData;

public final class CAbilityTypeDefinitionDevourCargo extends CAbilityTypeDefinitionCargoHold {
	@Override
	protected CAbilityType<?> innerCreateAbilityType(final War3ID alias, final GameObject data,
			final List<CAbilityTypeCargoHoldLevelData> levels) {
		return new CAbilityTypeCargoHold(alias, data.getFieldAsWar3ID(CODE, -1), levels) {
			@Override
			public CAbility createAbility(final int handleId) {
				final CAbilityTypeCargoHoldLevelData level = getLevelData(0);
				return new CAbilityDevourCargo(handleId, getCode(), getAlias(), level.getCargoCapcity(),
						level.getDuration(), level.getCastRange(), level.getTargetsAllowed());
			}
		};
	}
}
