package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CTimedCircleFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;

public final class CAbilityFlare extends CAbilityPointTargetSpellBase {
	private List<CUnitTypeRequirement> requirements = List.of();
	private byte detectionTypes;
	private float effectDelay;
	private float radius;
	private int configuredUses;
	private int usesRemaining;

	public CAbilityFlare(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.flare;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.detectionTypes = (byte) definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.effectDelay = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.configuredUses = definition.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		this.requirements = definition.getFieldAsInteger("checkDep", 0) == 0 ? List.of()
				: CUnitData.parseRequirements(definition.getFieldAsList("Requires"), definition.getFieldAsList("Requiresamount"));
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		this.usesRemaining = this.configuredUses > 0 ? this.configuredUses : -1;
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (owner.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	public int getUsesRemaining() {
		return this.usesRemaining;
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (this.usesRemaining == 0) receiver.noChargesRemaining();
		else receiver.useOk();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.usesRemaining == 0) return false;
		if (this.usesRemaining > 0) {
			this.usesRemaining--;
			caster.fireCooldownsChangedEvent();
		}
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		final int owner = caster.getPlayerIndex();
		final float x = target.getX();
		final float y = target.getY();
		final float radius = this.radius;
		final float duration = getDuration();
		final byte detection = this.detectionTypes;
		if (this.effectDelay > 0) {
			final CTimer impact = new CTimer() {
				@Override
				public void onFire(final CSimulation simulation) {
					reveal(simulation, owner, x, y, radius, duration, detection);
				}
			};
			impact.setTimeoutTime(this.effectDelay);
			impact.start(game);
		}
		else {
			reveal(game, owner, x, y, radius, duration, detection);
		}
		return false;
	}

	private void reveal(final CSimulation game, final int owner, final float x, final float y, final float radius,
			final float duration, final byte detection) {
		game.getPlayer(owner).addFogModifer(game, new CTimedCircleFogModifier(CFogState.VISIBLE,
				radius, x, y, duration, detection), false);
		game.spawnSpellEffectOnPoint(x, y, 0, getAlias(), CEffectType.EFFECT, 0).remove();
	}
}
