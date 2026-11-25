package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityAbsorbMana extends CAbilityTargetSpellBase {
	private float maximumLifeDrain;
	private float maximumManaDrain;

	public CAbilityAbsorbMana(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.absorb; }
	@Override public boolean isUniversal() { return true; }

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.maximumLifeDrain = Math.max(0, ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0));
		this.maximumManaDrain = Math.max(0, ability.getFieldAsFloat(AbilityFields.DATA_B + level, 0));
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit unit)) receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_MANA);
		else if (unit == caster) receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_SELF);
		else if (unit.getPlayerIndex() != caster.getPlayerIndex()) receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_ONE_OF_YOUR_OWN_UNITS);
		else if (unit.getMana() <= 0 && this.maximumLifeDrain == 0) receiver.targetCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_MANA_TO_ABSORB);
		else receiver.targetOk(unit);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (target instanceof CUnit unit && unit != caster && unit.getPlayerIndex() == caster.getPlayerIndex()) {
			final float mana = Math.max(0, Math.min(unit.getMana(), this.maximumManaDrain));
			if (mana > 0) {
				unit.setMana(unit.getMana() - mana);
				caster.restoreMana(game, mana);
			}
			final float life = Math.min(unit.getLife(), this.maximumLifeDrain);
			if (life > 0) {
				unit.setLife(game, unit.getLife() - life);
				caster.heal(game, life);
			}
			game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
			game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		}
		return false;
	}
}
