package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityDismount extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("Adec");
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID archerId;
	private War3ID hippogryphId;

	public CAbilityDismount(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.decouple; }
	@Override public boolean isPhysical() { return true; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.archerId = definition.getFieldAsWar3ID(AbilityFields.DATA_A + level, 0);
		this.hippogryphId = definition.getFieldAsWar3ID(AbilityFields.DATA_B + level, 0);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		return this.requirements.stream().allMatch(requirement -> isRequirementMet(game, unit, requirement));
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		for (final var requirement : this.requirements) {
			if (!isRequirementMet(game, unit, requirement)) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	private static boolean isRequirementMet(final CSimulation game, final CUnit unit, final CUnitTypeRequirement requirement) {
		return game.getPlayer(unit.getPlayerIndex()).getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel();
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		if (canDismount(game, unit)) receiver.targetOk(null);
		else receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_LAND_THERE);
	}

	private boolean canDismount(final CSimulation game, final CUnit rider) {
		if (this.archerId == null || this.hippogryphId == null || War3ID.NONE.equals(this.archerId)
				|| War3ID.NONE.equals(this.hippogryphId)) return false;
		final var archer = game.getUnitData().getUnitType(this.archerId);
		return archer != null && game.getUnitData().getUnitType(this.hippogryphId) != null
				&& game.getPathingGrid().isPathable(rider.getX(), rider.getY(), archer.getMovementType(), archer.getCollisionSize());
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit rider, final AbilityTarget target) {
		if (!canDismount(game, rider)) return false;
		final float lifeRatio = rider.getLife() / rider.getMaximumLife();
		final int player = rider.getPlayerIndex();
		final float x = rider.getX();
		final float y = rider.getY();
		final float facing = rider.getFacing();
		final CUnit archer = game.createUnitSimple(this.archerId, player, x, y, facing);
		if (archer == null) return false;
		final CUnit hippogryph = game.createUnitSimple(this.hippogryphId, player, x, y, facing);
		if (hippogryph == null) {
			game.removeUnit(archer);
			return false;
		}
		archer.setLife(game, lifeRatio * archer.getMaximumLife());
		hippogryph.setLife(game, lifeRatio * hippogryph.getMaximumLife());
		game.unitPreferredSelectionReplacement(rider, hippogryph);
		game.removeUnit(rider);
		return false;
	}
}
