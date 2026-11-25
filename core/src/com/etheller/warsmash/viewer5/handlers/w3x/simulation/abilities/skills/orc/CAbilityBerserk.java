package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;

public final class CAbilityBerserk extends CAbilityNoTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bbsk");
	private float movementSpeed;
	private float attackSpeed;
	private float additionalDamageTaken;
	private War3ID buffId;

	public CAbilityBerserk(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.berserk;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.movementSpeed = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.attackSpeed = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.additionalDamageTaken = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		final String buff = definition.getField(AbilityFields.BUFF + level);
		this.buffId = buff.length() == 4 ? War3ID.fromString(buff) : DEFAULT_BUFF;
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId, final AbilityTarget target) {
		if (orderId != getBaseOrderId() || target != null) return true;
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
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final BerserkBuff previous = caster.getFirstAbilityOfType(BerserkBuff.class);
		if (previous != null) caster.remove(game, previous);
		if (getDuration() > 0) {
			caster.add(game, new BerserkBuff(game.getHandleIdAllocator().createId(), this.buffId, getDuration(),
					this.movementSpeed, this.attackSpeed, this.additionalDamageTaken));
		}
		return false;
	}

	private static final class BerserkBuff extends CBuffTimed {
		private final NonStackingStatBuff movementSpeed;
		private final NonStackingStatBuff attackSpeed;
		private final CUnitAttackDamageTakenModificationListener damageTaken;

		BerserkBuff(final int handleId, final War3ID alias, final float duration, final float movementSpeed,
				final float attackSpeed, final float additionalDamageTaken) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.movementSpeed = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "berserk", movementSpeed);
			this.attackSpeed = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "berserk", attackSpeed);
			this.damageTaken = (game, source, target, attack, ranged, attackType, damageType, result) -> {
				result.addDamageMultiplier(1 + additionalDamageTaken);
				result.setBonusDamage(result.getBonusDamage() * (1 + additionalDamageTaken));
				return result;
			};
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.movementSpeed);
			unit.addNonStackingStatBuff(this.attackSpeed);
			unit.addDamageTakenModificationListener(this.damageTaken);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.movementSpeed);
			unit.removeNonStackingStatBuff(this.attackSpeed);
			unit.removeDamageTakenModificationListener(this.damageTaken);
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
