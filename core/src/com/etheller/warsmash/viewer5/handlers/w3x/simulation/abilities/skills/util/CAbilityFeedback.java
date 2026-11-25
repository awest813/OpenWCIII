package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util;

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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityFeedback extends CAbilityPassiveSpellBase {
	private final CUnitAttackPostDamageListener listener = this::onAttackHit;
	private float unitManaLoss;
	private float unitDamagePerMana;
	private float heroManaLoss;
	private float heroDamagePerMana;

	public CAbilityFeedback(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.unitManaLoss = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.unitDamagePerMana = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.heroManaLoss = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.heroDamagePerMana = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.addPostDamageListener(this.listener);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this.listener);
	}

	private void onAttackHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float attackDamage) {
		if (isDisabled() || attackDamage <= 0 || !(target instanceof CUnit unit) || unit.isDead() || unit.isMagicImmune()
				|| !unit.canBeTargetedBy(game, attacker, false, getTargetsAllowed(), BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return;
		final float mana = Math.min(unit.getMana(), unit.isHero() ? this.heroManaLoss : this.unitManaLoss);
		if (mana <= 0) return;
		unit.setMana(unit.getMana() - mana);
		final float damage = mana * (unit.isHero() ? this.heroDamagePerMana : this.unitDamagePerMana);
		if (damage > 0) unit.damage(game, attacker, false, false, CAttackType.SPELLS, CDamageType.MAGIC, "", damage);
		game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
	}
}
