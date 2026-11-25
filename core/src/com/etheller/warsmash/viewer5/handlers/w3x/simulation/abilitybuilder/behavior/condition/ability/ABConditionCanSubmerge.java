package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.condition.ability;

import java.util.Map;

import com.etheller.warsmash.parsers.jass.JassTextGenerator;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorType;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.PathingType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.AbilityBuilderActiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABCondition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;

public final class ABConditionCanSubmerge implements ABCondition {
	private static final War3ID SUBMERGE = War3ID.fromString("ANsu");

	@Override
	public boolean evaluate(final CSimulation game, final CUnit unit, final Map<String, Object> localStore, final int castId) {
		final var ability = (AbilityBuilderActiveAbility) localStore.get(ABLocalStoreKeys.ABILITY);
		if (!SUBMERGE.equals(ability.getCode()) || ability.isToggleOn()) return true;
		final var pathing = game.getPathingGrid();
		return pathing.isPathable(unit.getX(), unit.getY(), PathingType.SWIMMABLE)
				&& !pathing.isPathable(unit.getX(), unit.getY(), PathingType.WALKABLE);
	}

	@Override
	public String generateJassEquivalent(final JassTextGenerator generator) {
		final String ability = generator.getUserDataExpr("AB_LOCAL_STORE_KEY_ABILITY", JassTextGeneratorType.AbilityHandle);
		final String coordinates = "GetUnitX(" + generator.getCaster() + "), GetUnitY(" + generator.getCaster() + "), ";
		return "(GetAbilityCodeId(" + ability + ") != 'ANsu' or IsToggleAbilityActive(" + ability
				+ ") or (not IsTerrainPathable(" + coordinates + "PATHING_TYPE_FLOATABILITY) and IsTerrainPathable("
				+ coordinates + "PATHING_TYPE_WALKABILITY)))";
	}
}
