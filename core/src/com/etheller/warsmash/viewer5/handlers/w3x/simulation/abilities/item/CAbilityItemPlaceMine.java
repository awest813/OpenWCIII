package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityItemPlaceMine extends CAbilityPointTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AIpm");
	private War3ID mineType;

	public CAbilityItemPlaceMine(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemplacemine;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.mineType = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		final var grid = game.getPathingGrid();
		final var type = this.mineType == null ? null : game.getUnitData().getUnitType(this.mineType);
		if (type != null && grid.contains(target.getX(), target.getY())
				&& grid.isPathable(target.getX(), target.getY(), type.getMovementType(), type.getCollisionSize())) {
			receiver.targetOk(target);
		}
		else receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_BUILD_THERE);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit mine = game.createUnitSimple(this.mineType, caster.getPlayerIndex(), target.getX(), target.getY(),
				caster.getFacing());
		if (mine != null) caster.fireSummonEvents(game, mine);
		return false;
	}
}
