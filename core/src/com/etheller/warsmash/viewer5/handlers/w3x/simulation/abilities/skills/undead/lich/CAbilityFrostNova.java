package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich;

import java.util.ArrayList;

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

public final class CAbilityFrostNova extends CAbilityTargetSpellBase {
	private float areaDamage;
	private float targetDamage;
	private float radius;
	private War3ID buff;

	public CAbilityFrostNova(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.frostnova;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.areaDamage = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.targetDamage = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		final War3ID buffId = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.buff = War3ID.NONE.equals(buffId) ? CBuffFrost.DEFAULT_BUFF : buffId;
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
		if (!(target instanceof CUnit primary) || !primary.checkForAbilityEffectReaction(game, caster, this)) return false;
		final var splashTargets = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(primary.getX(), primary.getY(), this.radius, unit -> {
			if (unit != primary && unit.canBeTargetedBy(game, caster, getTargetsAllowed())) splashTargets.add(unit);
			return false;
		});
		game.createTemporarySpellEffectOnUnit(primary, getAlias(), CEffectType.EFFECT);
		hit(game, caster, primary, this.areaDamage + this.targetDamage);
		for (final CUnit secondary : splashTargets) {
			hit(game, caster, secondary, this.areaDamage);
		}
		return false;
	}

	private void hit(final CSimulation game, final CUnit caster, final CUnit target, final float damage) {
		if (target.isDead()) return;
		if (damage > 0) target.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.COLD, null, damage);
		CBuffFrost.apply(game, target, this.buff, getDurationForTarget(target));
	}
}
