package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

public final class CAbilityFrenzy extends CAbilityNoTargetSpellBase implements CAutocastAbility {
	private static final War3ID BLOODLUST_BUFF = War3ID.fromString("Bblo");
	private War3ID buffId;
	private float attackSpeed;
	private float movementSpeed;
	private float modelScale;
	private boolean autocast;

	public CAbilityFrenzy(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.frenzy; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.frenzyon; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.frenzyoff; }
	@Override public AutocastType getAutocastType() { return AutocastType.NOTARGET; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }

	@Override
	public void setAutoCastOn(final CUnit unit, final boolean enabled) {
		if (enabled == this.autocast) return;
		this.autocast = enabled;
		unit.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setAutoCastOn(unit, false);
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(ability, level);
		this.buffId = War3ID.NONE.equals(buff) ? BLOODLUST_BUFF : buff;
		this.attackSpeed = ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.movementSpeed = ability.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.modelScale = 1 + ability.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		CAbilityBloodlust.applyBuff(game, caster, this.buffId, getDuration(), this.attackSpeed, this.movementSpeed, this.modelScale);
		return false;
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId, final AbilityTarget target) {
		if (orderId != getBaseOrderId() || target != null) return super.checkBeforeQueue(game, caster, orderId, target);
		final var activation = new BooleanAbilityActivationReceiver();
		checkCanUse(game, caster, orderId, activation);
		if (activation.isOk() && caster.chargeMana(getManaCost())) {
			caster.beginCooldown(game, getCode(), getCooldown());
			doEffect(game, caster, null);
			game.unitSoundEffectEvent(caster, getAlias());
		}
		return false;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		if (this.autocast && !isDisabled() && getCooldownRemaining(game, caster) <= 0
				&& caster.getCurrentBehavior() != null && caster.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.ATTACK) {
			caster.orderAutomatically(game, getBaseOrderId(), null);
		}
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		if (caster.getCurrentBehavior() != null && caster.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.ATTACK) {
			checkCanTargetNoTarget(game, caster, orderId, receiver);
		}
		else receiver.orderIdNotAccepted();
	}
}
