package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

public final class CAbilityCloudOfFog extends CAbilityPointTargetSpellBase {
	private final Set<CUnit> affected = new LinkedHashSet<>();
	private final StateModBuff disarm = new StateModBuff(StateModBuffType.DISABLE_RANGED_ATTACK, 1);
	private List<CUnitTypeRequirement> requirements;
	private float radius;
	private int endTick;
	private SimulationRenderComponentModel effect;

	public CAbilityCloudOfFog(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.cloudoffog;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA + level, 0);
		this.requirements = definition.getFieldAsBoolean(AbilityFields.CHECK_DEPENDENCIES, 0)
				? CUnitData.parseRequirements(definition.getFieldAsList(AbilityFields.REQUIREMENTS),
						definition.getFieldAsList(AbilityFields.REQUIREMENT_LEVELS))
				: List.of();
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit caster) {
		final var owner = game.getPlayer(caster.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit caster, final AbilityActivationReceiver receiver) {
		final var owner = game.getPlayer(caster.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (owner.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		this.effect = game.spawnSpellEffectOnPoint(target.getX(), target.getY(), 0, getAlias(), CEffectType.AREA_EFFECT, 0);
		this.endTick = game.getGameTurnTick() + (int) Math.ceil(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
		return doChannelTick(game, caster, target);
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (game.getGameTurnTick() >= this.endTick) return false;
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		final Set<CUnit> inCloud = new LinkedHashSet<>();
		final var owner = game.getPlayer(caster.getPlayerIndex());
		game.getWorldCollision().enumUnitsInRange(target.getX(), target.getY(), this.radius, unit -> {
			if (unit.isBuilding() && !unit.isDead() && !unit.isHidden()
					&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
					&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(), receiver.reset())) inCloud.add(unit);
			return false;
		});
		this.affected.removeIf(unit -> {
			if (inCloud.contains(unit)) return false;
			removeDisarm(game, unit);
			return true;
		});
		for (final CUnit unit : inCloud) {
			if (this.affected.add(unit)) {
				unit.addStateModBuff(this.disarm);
				unit.computeUnitState(game, this.disarm.getBuffType());
			}
		}
		return true;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target, final boolean interrupted) {
		clearCloud(game);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit caster) {
		clearCloud(game);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		if (this.effect != null && (caster.getCurrentBehavior() == null
				|| caster.getCurrentBehavior().getHighlightOrderId() != getBaseOrderId())) clearCloud(game);
	}

	private void clearCloud(final CSimulation game) {
		if (this.effect != null) {
			this.effect.remove();
			this.effect = null;
		}
		this.affected.forEach(unit -> removeDisarm(game, unit));
		this.affected.clear();
	}

	private void removeDisarm(final CSimulation game, final CUnit unit) {
		unit.removeStateModBuff(this.disarm);
		unit.computeUnitState(game, this.disarm.getBuffType());
	}
}
