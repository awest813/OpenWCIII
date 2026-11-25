package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityItemWandOfManaStealing extends CAbilityTargetSpellBase {
	private float manaToSteal;
	private boolean useFixedAmount;

	public CAbilityItemWandOfManaStealing(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.steal;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.manaToSteal = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.useFixedAmount = definition.getFieldAsBoolean(AbilityFields.DATA_C + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (caster.getMana() >= caster.getMaximumMana()) receiver.activationCheckFailed(CommandStringErrorKeys.ALREADY_AT_FULL_MANA);
		else receiver.useOk();
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit targetUnit)) receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		else if (targetUnit == caster) receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_SELF);
		else if (targetUnit.getMana() <= 0) receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_MANA);
		else receiver.targetOk(target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CUnit unit)) return false;
		final float amount = this.useFixedAmount ? this.manaToSteal : this.manaToSteal * unit.getMana();
		final float stolen = Math.max(0, Math.min(unit.getMana(), amount));
		unit.setMana(unit.getMana() - stolen);
		caster.restoreMana(game, stolen);
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		return false;
	}
}
