package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat;

import java.util.EnumSet;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetWidgetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CWeaponType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public abstract class CAbilityAttackArrow extends CAbilityTargetSpellBase {
	private float bonusDamage;
	private String missileArt;
	private int missileSpeed;
	private boolean missileHoming;
	private boolean autoCastOn;
	private boolean customTargets;

	protected CAbilityAttackArrow(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.bonusDamage = ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.missileArt = ability.getFieldAsString("Missileart", 0);
		this.missileSpeed = ability.getFieldAsInteger(AbilityFields.PROJECTILE_SPEED, 0);
		this.missileHoming = ability.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		this.customTargets = !CTargetType.parseTargetTypeSet(
				ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty();
	}

	protected final void setArrowTargets(final EnumSet<CTargetType> targets) {
		getTargetsAllowed().addAll(targets);
		this.customTargets = !targets.isEmpty();
	}

	@Override public boolean isAutoCastOn() { return this.autoCastOn; }

	@Override
	public void setAutoCastOn(final CUnit unit, final boolean enabled) {
		this.autoCastOn = enabled;
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		// Both manual and automatic shots use the normal attack windup.
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (this.customTargets && !target.canBeTargetedBy(game, unit, getTargetsAllowed(), receiver)) return;
		final CUnitAttack attack = selectAttack(game, unit, target);
		if (attack == null) {
			receiver.orderIdNotAccepted();
		}
		else if (unit.isMovementDisabled() && !unit.canReach(target, Math.max(attack.getRange(), getCastRange()))) {
			receiver.targetCheckFailed(CommandStringErrorKeys.TARGET_IS_OUTSIDE_RANGE);
		}
		else {
			receiver.targetOk(target);
		}
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final CWidget target) {
		final CUnitAttack attack = selectAttack(game, caster, target);
		return attack == null ? caster.pollNextOrderBehavior(game)
				: caster.getAttackBehavior().resetForArrow(game, this, attack, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		// The attack behavior applies the effect at launch.
		return false;
	}

	public boolean tryLaunch(final CSimulation game, final CUnit caster, final int orderId,
			final CUnitAttackMissile attack, final AbilityTarget target, final float damage,
			final CUnitAttackListener listener) {
		if (isDisabled() || !supportsAttack(attack) || (orderId != getBaseOrderId() && !isAutoCastOn())
				|| getCooldownRemaining(game, caster) > 0) {
			return false;
		}
		final CWidget widget = target.visit(AbilityTargetWidgetVisitor.INSTANCE);
		if (widget == null || (this.customTargets && !widget.canBeTargetedBy(game, caster, getTargetsAllowed()))
				|| !caster.chargeMana(getManaCost())) {
			return false;
		}
		if (getCooldown() > 0) caster.beginCooldown(game, getCode(), getCooldown());
		prepareAttack(attack).launch(game, caster, target, damage + this.bonusDamage, impactListener(game, caster, listener));
		return true;
	}

	protected CUnitAttackMissile prepareAttack(final CUnitAttackMissile attack) {
		CUnitAttackMissile launchAttack = attack;
		if (!this.missileArt.isEmpty() && !"_".equals(this.missileArt)) {
			launchAttack = (CUnitAttackMissile) attack.copy();
			launchAttack.setProjectileArt(this.missileArt);
			if (this.missileSpeed > 0) launchAttack.setProjectileSpeed(this.missileSpeed);
			launchAttack.setProjectileHomingEnabled(this.missileHoming);
		}
		return launchAttack;
	}

	protected CUnitAttackListener impactListener(final CSimulation game, final CUnit caster,
			final CUnitAttackListener listener) {
		return listener;
	}

	private CUnitAttack selectAttack(final CSimulation game, final CUnit unit, final CWidget target) {
		for (final CUnitAttack attack : unit.getCurrentAttacks()) {
			if (supportsAttack(attack) && target.canBeAttackedBy(game, unit, attack.getTargetsAllowed())) {
				return attack;
			}
		}
		return null;
	}

	protected boolean supportsAttack(final CUnitAttack attack) {
		return attack.getWeaponType() == CWeaponType.MISSILE;
	}

}
