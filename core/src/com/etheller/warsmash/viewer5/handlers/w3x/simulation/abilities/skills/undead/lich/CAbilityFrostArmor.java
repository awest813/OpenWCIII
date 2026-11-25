package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityFrostArmor extends CAbilityTargetSpellBase implements CAutocastAbility {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BUfa");
	private final boolean supportsAutocast;
	private boolean autocast;
	private float armorDuration;
	private float armorBonus;
	private War3ID buff;

	public CAbilityFrostArmor(final int handleId, final War3ID alias, final boolean supportsAutocast) {
		super(handleId, alias);
		this.supportsAutocast = supportsAutocast;
	}

	@Override public int getBaseOrderId() { return OrderIds.frostarmor; }
	@Override public int getAutoCastOnOrderId() { return this.supportsAutocast ? OrderIds.frostarmoron : 0; }
	@Override public int getAutoCastOffOrderId() { return this.supportsAutocast ? OrderIds.frostarmoroff : 0; }
	@Override public AutocastType getAutocastType() { return AutocastType.ATTACKINGENEMY; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }

	@Override
	public void populateData(final GameObject data, final int level) {
		this.armorDuration = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.armorBonus = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.buff = War3ID.NONE.equals(authoredBuff) ? DEFAULT_BUFF : authoredBuff;
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
		if (!(target instanceof CUnit unit) || !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		final FrostArmorBuff previous = unit.getFirstAbilityOfType(FrostArmorBuff.class);
		if (previous != null) unit.remove(game, previous);
		if (this.armorDuration > 0) {
			unit.add(game, new FrostArmorBuff(game.getHandleIdAllocator().createId(), this.buff, this.armorDuration,
					this.armorBonus, getDuration(), getHeroDuration()));
		}
		return false;
	}

	@Override
	public void setAutoCastOn(final CUnit caster, final boolean enabled) {
		if (!this.supportsAutocast || enabled == this.autocast) return;
		this.autocast = enabled;
		caster.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit caster) {
		setAutoCastOn(caster, false);
		super.onRemove(game, caster);
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && unit.getFirstAbilityOfType(FrostArmorBuff.class) == null) {
			checkCanTarget(game, caster, orderId, unit, receiver);
		}
		else receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	private static final class FrostArmorBuff extends CBuffTimed implements CUnitAttackDamageTakenListener {
		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.BENEFICIAL;
		}

		@Override
		public boolean isHeroSpellBuff() {
			return true;
		}

		private final NonStackingStatBuff defense;
		private final float chillDuration;
		private final float chillHeroDuration;

		private FrostArmorBuff(final int handleId, final War3ID alias, final float duration, final float armor,
				final float chillDuration, final float chillHeroDuration) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.defense = new NonStackingStatBuff(NonStackingStatBuffType.DEF, "frostarmor", armor);
			this.chillDuration = chillDuration;
			this.chillHeroDuration = chillHeroDuration;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.defense);
			unit.addDamageTakenListener(this);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeDamageTakenListener(this);
			unit.removeNonStackingStatBuff(this.defense);
		}

		@Override
		public void onDamage(final CSimulation game, final CUnit attacker, final CUnit defender, final boolean isAttack,
				final boolean isRanged, final CDamageType type, final float damage, final float bonusDamage, final float trueDamage) {
			if (isAttack && !isRanged && attacker != null && !attacker.isDead() && !attacker.isMagicImmune()
					&& !attacker.isUnitAlly(game.getPlayer(defender.getPlayerIndex()))) {
				game.createTemporarySpellEffectOnUnit(attacker, getAlias(), CEffectType.SPECIAL);
				CBuffFrost.apply(game, attacker, CBuffFrost.DEFAULT_BUFF,
						attacker.isHero() ? this.chillHeroDuration : this.chillDuration);
			}
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
