package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.seawitch;

import java.util.ArrayList;
import java.util.Comparator;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityForkedLightning extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BOLT = War3ID.fromString("FORK");
	private float damage;
	private int maximumTargets;
	private float coneLength;
	private float startWidth;
	private float endWidth;
	private War3ID bolt;

	public CAbilityForkedLightning(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.forkedlightning;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.damage = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.maximumTargets = data.getFieldAsInteger(AbilityFields.DATA_B + level, 0);
		this.coneLength = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.startWidth = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.endWidth = data.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		final War3ID lightning = AbstractCAbilityTypeDefinition.getLightningId(data, 0);
		this.bolt = War3ID.NONE.equals(lightning) ? DEFAULT_BOLT : lightning;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CUnit primary) || this.maximumTargets <= 0) return false;
		final var additionalTargets = new ArrayList<CUnit>();
		if (this.maximumTargets > 1 && this.coneLength > 0) {
			final double direction = caster.angleTo(primary);
			final double forwardX = StrictMath.cos(direction);
			final double forwardY = StrictMath.sin(direction);
			game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(),
					this.coneLength + Math.max(this.startWidth, this.endWidth), unit -> {
						if (unit != primary && unit.canBeTargetedBy(game, caster, getTargetsAllowed())
								&& isInCone(unit.getX() - caster.getX(), unit.getY() - caster.getY(), forwardX, forwardY)) {
							additionalTargets.add(unit);
						}
						return false;
					});
			additionalTargets.sort(Comparator.<CUnit>comparingDouble(caster::distanceSquaredNoCollision)
					.thenComparingInt(CUnit::getHandleId));
		}
		strike(game, caster, primary);
		for (int index = 0; index < Math.min(additionalTargets.size(), this.maximumTargets - 1); index++) {
			strike(game, caster, additionalTargets.get(index));
		}
		return false;
	}

	private boolean isInCone(final double dx, final double dy, final double forwardX, final double forwardY) {
		final double distance = dx * forwardX + dy * forwardY;
		if (distance < 0 || distance > this.coneLength) return false;
		final double width = this.startWidth + (this.endWidth - this.startWidth) * distance / this.coneLength;
		return Math.abs(dx * forwardY - dy * forwardX) <= width;
	}

	private void strike(final CSimulation game, final CUnit caster, final CUnit target) {
		if (target.isDead() || !target.checkForAbilityEffectReaction(game, caster, this)) return;
		game.createLightning(caster, this.bolt, target, getDuration());
		game.createTemporarySpellEffectOnUnit(target, getAlias(), CEffectType.SPECIAL);
		target.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.LIGHTNING, null, this.damage);
	}
}
