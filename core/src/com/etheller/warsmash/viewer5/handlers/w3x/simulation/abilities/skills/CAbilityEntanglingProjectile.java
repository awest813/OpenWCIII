package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public abstract class CAbilityEntanglingProjectile extends CAbilityTargetSpellBase {
	private final War3ID defaultGroundBuff;
	private final War3ID defaultAirBuff;
	private final StateModBuffType entangledState;
	private List<CUnitTypeRequirement> requirements;
	private float projectileSpeed;
	private boolean homing;
	private War3ID groundBuff;
	private War3ID airBuff;
	private float lowerDuration;
	private float lowerHeight;
	private int meleeRange;

	protected CAbilityEntanglingProjectile(final int handleId, final War3ID alias, final War3ID defaultGroundBuff,
			final War3ID defaultAirBuff, final StateModBuffType entangledState) {
		super(handleId, alias);
		this.defaultGroundBuff = defaultGroundBuff;
		this.defaultAirBuff = defaultAirBuff;
		this.entangledState = entangledState;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.requirements = CUnitData.parseRequirements(data.getFieldAsList(AbilityFields.REQUIREMENTS),
				data.getFieldAsList(AbilityFields.REQUIREMENT_LEVELS));
		this.projectileSpeed = data.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		this.homing = data.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		if (this.entangledState == StateModBuffType.WEBBED) {
			this.lowerDuration = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
			this.lowerHeight = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
			this.meleeRange = data.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		}
		final War3ID ground = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		final War3ID air = AbstractCAbilityTypeDefinition.getBuffId(data, level, 1);
		this.groundBuff = War3ID.NONE.equals(ground) ? this.defaultGroundBuff : ground;
		this.airBuff = War3ID.NONE.equals(air) ? this.defaultAirBuff : air;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			populateDefaultTargets(data);
		}
	}

	protected abstract void populateDefaultTargets(GameObject data);

	protected String getFallbackArt() {
		return null;
	}

	@Override public boolean isPhysical() { return true; }

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(
				requirement -> owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
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
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CUnit unit)) return false;
		final float duration = getDurationForTarget(unit);
		final War3ID buff = unit.isUnitType(CUnitTypeJass.FLYING) ? this.airBuff : this.groundBuff;
		if (this.projectileSpeed > 0) {
			game.createProjectile(caster, getAlias(), caster.getX(), caster.getY(), (float) caster.angleTo(target),
					this.projectileSpeed, this.homing, target, new CAbilityProjectileListener() {
						@Override public void onLaunch(final CSimulation game, final CProjectile projectile, final AbilityTarget target) { }

						@Override
						public void onHit(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
							if (target instanceof CUnit hit) applyNet(game, hit, buff, duration);
						}
					});
		}
		else applyNet(game, unit, buff, duration);
		return false;
	}

	private void applyNet(final CSimulation game, final CUnit unit, final War3ID buff, final float duration) {
		if (unit.isDead() || unit.isHidden() || unit.isInvulnerable() || duration <= 0) return;
		if (this.entangledState == StateModBuffType.WEBBED && unit.isWebbed()) return;
		final String art = buff.equals(getAlias()) ? getFallbackArt() : null;
		unit.add(game, new EntangledBuff(game.getHandleIdAllocator().createId(), buff, duration, this.entangledState,
				this.lowerDuration, this.lowerHeight, this.meleeRange, art));
		final var behavior = unit.getCurrentBehavior();
		if (behavior != null && (behavior.getBehaviorCategory() == CBehaviorCategory.MOVEMENT
				|| behavior.getBehaviorCategory() == CBehaviorCategory.SPELL)) {
			unit.order(game, OrderIds.stop, null);
		}
	}

	private static final class EntangledBuff extends CBuffTimed {
		private final StateModBuff entangled;
		private final int loweringTicks;
		private final float maximumLoweringHeight;
		private final String fallbackArt;
		private int appliedTick;
		private float initialHeight;

		private EntangledBuff(final int handleId, final War3ID alias, final float duration,
				final StateModBuffType state, final float lowerDuration, final float maximumLoweringHeight,
				final int meleeRange, final String fallbackArt) {
			super(handleId, alias, alias, duration);
			this.entangled = new StateModBuff(state, state == StateModBuffType.WEBBED ? Math.max(1, meleeRange) : 1);
			this.loweringTicks = Math.round(lowerDuration / WarsmashConstants.SIMULATION_STEP_TIME);
			this.maximumLoweringHeight = maximumLoweringHeight;
			this.fallbackArt = fallbackArt;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.appliedTick = game.getGameTurnTick();
			this.initialHeight = Math.min(unit.getFlyHeight(), this.maximumLoweringHeight);
			unit.addStateModBuff(this.entangled);
			unit.computeUnitState(game, this.entangled.getBuffType());
			if (this.loweringTicks > 0 && !unit.isUnitType(CUnitTypeJass.SNARED)) unit.setFlyHeight(this.initialHeight);
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			final int elapsed = game.getGameTurnTick() - this.appliedTick;
			if (this.loweringTicks > 0 && elapsed <= this.loweringTicks && !unit.isUnitType(CUnitTypeJass.SNARED)) {
				unit.setFlyHeight(this.initialHeight * (this.loweringTicks - elapsed) / this.loweringTicks);
			}
			super.onTick(game, unit);
		}

		@Override
		protected SimulationRenderComponent createArt(final CSimulation game, final CUnit unit) {
			return this.fallbackArt == null ? super.createArt(game, unit)
					: game.createPersistentEffectOnUnit(unit, this.fallbackArt);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeStateModBuff(this.entangled);
			unit.computeUnitState(game, this.entangled.getBuffType());
		}

		@Override public boolean isTimedLifeBar() { return false; }
	}
}
