package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityDeathExplosion extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Adda");
	private float fullRadius;
	private float fullDamage;
	private float partialRadius;
	private float partialDamage;

	public CAbilityDeathExplosion(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.fullRadius = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.fullDamage = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.partialRadius = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.partialDamage = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit source) {
		final float x = source.getX();
		final float y = source.getY();
		if (getDuration() <= 0) {
			burst(game, source, x, y);
			return;
		}
		final CTimer explosion = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				burst(simulation, source, x, y);
			}
		};
		explosion.setTimeoutTime(getDuration());
		explosion.start(game);
	}

	private void burst(final CSimulation game, final CUnit source, final float x, final float y) {
		final float radius = Math.max(this.fullRadius, this.partialRadius);
		game.getWorldCollision().enumUnitsInRange(x, y, radius, unit -> damage(game, source, unit, x, y));
		game.getWorldCollision().enumDestructablesInRange(x, y, radius, target -> damage(game, source, target, x, y));
	}

	private boolean damage(final CSimulation game, final CUnit source, final CWidget target, final float x, final float y) {
		final var receiver = BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset();
		final boolean allowed = target instanceof CUnit unit
				? !unit.isHidden() && unit.canBeTargetedBy(game, source, false, getTargetsAllowed(), receiver)
				: target.canBeTargetedBy(game, source, getTargetsAllowed(), receiver);
		if (target != source && allowed) {
			final float amount = target.distanceSquaredNoCollision(x, y) <= this.fullRadius * this.fullRadius
					? this.fullDamage : this.partialDamage;
			target.damage(game, source, false, true, CAttackType.SPELLS, CDamageType.DEMOLITION,
					CWeaponSoundTypeJass.WHOKNOWS.name(), amount);
		}
		return false;
	}
}
