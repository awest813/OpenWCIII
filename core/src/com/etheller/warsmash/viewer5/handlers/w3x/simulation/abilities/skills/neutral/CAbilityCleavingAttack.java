package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityCleavingAttack extends CAbilityPassiveSpellBase implements CUnitAttackPostDamageListener {
	public static final War3ID CODE = War3ID.fromString("ANca");
	private float damageFraction;

	public CAbilityCleavingAttack(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.damageFraction = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.addPostDamageListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this);
	}

	@Override
	public void onHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float actualDamage) {
		if (actualDamage <= 0 || this.damageFraction <= 0 || !(target instanceof CUnit primary)) return;
		final float radius = getAreaOfEffect();
		final float damage = actualDamage * this.damageFraction;
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		game.getWorldCollision().enumUnitsInRange(primary.getX(), primary.getY(), radius, nearby -> {
			if (nearby != primary && nearby != attacker && !nearby.isDead() && !nearby.isHidden()
					&& !nearby.isUnitType(CUnitTypeJass.ETHEREAL)
					&& primary.distanceSquaredNoCollision(nearby) <= radius * radius
					&& nearby.canBeTargetedBy(game, attacker, false, getTargetsAllowed(), receiver.reset())) {
				nearby.damage(game, attacker, false, false, CAttackType.HERO, CDamageType.ENHANCED, null, damage);
			}
			return false;
		});
	}
}
