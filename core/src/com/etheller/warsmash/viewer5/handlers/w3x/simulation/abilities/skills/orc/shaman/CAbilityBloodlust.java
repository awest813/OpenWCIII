package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shaman;

import java.util.Collections;

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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

/** Shared spell behavior for the Shaman and the neutral Bloodlust variants. */
public final class CAbilityBloodlust extends CAbilityTargetSpellBase implements CAutocastAbility {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bblo");
	private float attackSpeed;
	private float movementSpeed;
	private float modelScale;
	private War3ID buffId;
	private boolean autocast;

	public CAbilityBloodlust(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.bloodlust;
	}

	@Override
	public int getAutoCastOnOrderId() {
		return OrderIds.bloodluston;
	}

	@Override
	public int getAutoCastOffOrderId() {
		return OrderIds.bloodlustoff;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.attackSpeed = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.movementSpeed = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.modelScale = 1 + definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		final String configuredBuff = definition.getFieldAsString(AbilityFields.BUFF + level, 0).trim();
		this.buffId = configuredBuff.length() == 4 ? War3ID.fromString(configuredBuff) : DEFAULT_BUFF;
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			Collections.addAll(getTargetsAllowed(), CTargetType.AIR, CTargetType.GROUND, CTargetType.FRIEND,
					CTargetType.SELF, CTargetType.ORGANIC, CTargetType.NON_MAGIC_IMMUNE);
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) {
			receiver.targetOk(target);
		}
		else {
			receiver.orderIdNotAccepted();
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit != null && unit.checkForAbilityEffectReaction(game, caster, this)) {
			applyBuff(game, unit, this.buffId, getDurationForTarget(unit), this.attackSpeed, this.movementSpeed, this.modelScale);
		}
		return false;
	}

	static void applyBuff(final CSimulation game, final CUnit unit, final War3ID buffId, final float duration,
			final float attackSpeed, final float movementSpeed, final float modelScale) {
		final BloodlustBuff previous = unit.getFirstAbilityOfType(BloodlustBuff.class);
		if (previous != null) unit.remove(game, previous);
		unit.add(game, new BloodlustBuff(game.getHandleIdAllocator().createId(), buffId, duration,
				attackSpeed, movementSpeed, modelScale));
	}

	@Override
	public boolean isAutoCastOn() {
		return this.autocast;
	}

	@Override
	public void setAutoCastOn(final CUnit caster, final boolean enabled) {
		if (enabled == this.autocast) {
			return;
		}
		this.autocast = enabled;
		caster.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit caster) {
		setAutoCastOn(caster, false);
		super.onRemove(game, caster);
	}

	@Override
	public void setAutoCastOff() {
		this.autocast = false;
	}

	@Override
	public AutocastType getAutocastType() {
		return AutocastType.ATTACKINGENEMY;
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && unit.getFirstAbilityOfType(BloodlustBuff.class) == null) {
			checkCanTarget(game, caster, orderId, target, receiver);
		}
		else {
			receiver.orderIdNotAccepted();
		}
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

	private static final class BloodlustBuff extends CBuffTimed {
		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.BENEFICIAL;
		}

		private final NonStackingStatBuff attackSpeed;
		private final NonStackingStatBuff movementSpeed;
		private final float modelScale;

		BloodlustBuff(final int handleId, final War3ID alias, final float duration, final float attack,
				final float movement, final float scale) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.attackSpeed = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "bloodlust", attack);
			this.movementSpeed = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "bloodlust", movement);
			this.modelScale = scale;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.attackSpeed);
			unit.addNonStackingStatBuff(this.movementSpeed);
			unit.setModelScaleMultiplier(this.modelScale);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.attackSpeed);
			unit.removeNonStackingStatBuff(this.movementSpeed);
			unit.setModelScaleMultiplier(1);
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
