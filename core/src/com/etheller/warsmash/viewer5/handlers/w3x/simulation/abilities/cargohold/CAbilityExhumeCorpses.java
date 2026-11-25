package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;

public final class CAbilityExhumeCorpses extends CAbilityPassiveSpellBase {
	private static final War3ID CODE = War3ID.fromString("Aexh");
	private List<CUnitTypeRequirement> requirements;
	private War3ID corpseType;
	private int interval;
	private int limit;
	private int nextCorpseTick;

	public CAbilityExhumeCorpses(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList(AbilityFields.REQUIREMENTS),
				definition.getFieldAsList(AbilityFields.REQUIREMENT_LEVELS));
		this.corpseType = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
		this.limit = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.interval = Math.max(1, (int)Math.ceil(definition.getFieldAsFloat(AbilityFields.DURATION + level, 0)
				/ WarsmashConstants.SIMULATION_STEP_TIME));
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit wagon) {
		final var owner = game.getPlayer(wagon.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit wagon) {
		this.nextCorpseTick = game.getGameTurnTick() + this.interval;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit wagon) {
		if (isDisabled() || game.getGameTurnTick() < this.nextCorpseTick || !isRequirementsMet(game, wagon)) return;
		this.nextCorpseTick = game.getGameTurnTick() + this.interval;
		if (wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo && cargo.hasCapacity(1)
				&& cargo.getCargoCount() < this.limit && this.corpseType != null && !War3ID.NONE.equals(this.corpseType)) {
			final var type = game.getUnitData().getUnitType(this.corpseType);
			if (type == null || !type.isRaise() || !type.isDecay() || type.isHero() || type.isBuilding()) return;
			final CUnit corpse = game.createUnit(this.corpseType, wagon.getPlayerIndex(),
					wagon.getX(), wagon.getY(), wagon.getFacing());
			if (corpse != null) {
				corpse.initializeAsCorpse(game);
				cargo.storeCorpse(game, wagon, corpse);
			}
		}
	}
}
