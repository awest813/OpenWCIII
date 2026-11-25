package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityResearchIndicator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityFlakCannons extends CAbilityResearchIndicator implements CUnitAttackPostDamageListener {
	public static final War3ID CODE = War3ID.fromString("Aflk");
	private static final War3ID RESEARCH = War3ID.fromString("Rhfc");
	private float innerRadius;
	private float middleRadius;
	private float outerRadius;
	private float innerDamage;
	private float middleDamage;
	private float outerDamage;

	public CAbilityFlakCannons(final int handleId, final War3ID alias) {
		super(handleId, alias, CODE, RESEARCH);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		super.populateData(data, level);
		this.innerRadius = getAreaOfEffect();
		this.middleRadius = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.outerRadius = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.innerDamage = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.middleDamage = data.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.outerDamage = data.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		unit.addPostDamageListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this);
		super.onRemove(game, unit);
	}

	@Override
	public void onHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float actualDamage) {
		if (actualDamage <= 0 || !(target instanceof CUnit primary) || primary.getMovementType() != MovementType.FLY) return;
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		game.getWorldCollision().enumUnitsInRange(primary.getX(), primary.getY(), this.outerRadius, unit -> {
			if (unit != primary && unit != attacker && !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& unit.canBeTargetedBy(game, attacker, false, getTargetsAllowed(), receiver.reset())) {
				final double distance = primary.distanceSquaredNoCollision(unit);
				if (distance <= this.outerRadius * this.outerRadius) {
					final float damage = distance <= this.innerRadius * this.innerRadius ? this.innerDamage
							: distance <= this.middleRadius * this.middleRadius ? this.middleDamage : this.outerDamage;
					unit.damage(game, attacker, false, true, CAttackType.PIERCE, CDamageType.NORMAL, null, damage);
				}
			}
			return false;
		});
	}
}
