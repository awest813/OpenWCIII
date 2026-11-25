package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.keeper;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityEntanglingRoots extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BEer");
	private float damagePerSecond;
	private War3ID buff;

	public CAbilityEntanglingRoots(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.entanglingroots;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.damagePerSecond = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
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
		final float duration = getDurationForTarget(unit);
		if (duration <= 0) return false;
		final RootsBuff previous = unit.getFirstAbilityOfType(RootsBuff.class);
		if (previous != null) unit.remove(game, previous);
		unit.add(game, new RootsBuff(game.getHandleIdAllocator().createId(), this.buff, duration, caster, this.damagePerSecond));
		unit.order(game, OrderIds.stop, null);
		return false;
	}

	private static final class RootsBuff extends CBuffTimed {
		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.HARMFUL;
		}

		@Override
		public boolean isHeroSpellBuff() {
			return true;
		}

		private static final int TICKS_PER_DAMAGE = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		private final StateModBuff snared = new StateModBuff(StateModBuffType.SNARED, 1);
		private final StateModBuff disarmed = new StateModBuff(StateModBuffType.DISABLE_ATTACK, 1);
		private final CUnit caster;
		private final float damage;
		private int nextDamageTick;

		private RootsBuff(final int handleId, final War3ID alias, final float duration, final CUnit caster, final float damage) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.caster = caster;
			this.damage = damage;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addStateModBuff(this.snared);
			unit.addStateModBuff(this.disarmed);
			unit.computeUnitState(game, StateModBuffType.SNARED);
			unit.computeUnitState(game, StateModBuffType.DISABLE_ATTACK);
			this.nextDamageTick = game.getGameTurnTick() + TICKS_PER_DAMAGE;
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeStateModBuff(this.snared);
			unit.removeStateModBuff(this.disarmed);
			unit.computeUnitState(game, StateModBuffType.SNARED);
			unit.computeUnitState(game, StateModBuffType.DISABLE_ATTACK);
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (this.damage > 0 && game.getGameTurnTick() >= this.nextDamageTick) {
				this.nextDamageTick += TICKS_PER_DAMAGE;
				unit.damage(game, this.caster, false, true, CAttackType.SPELLS, CDamageType.PLANT, null, this.damage);
				if (unit.isDead()) return;
			}
			super.onTick(game, unit);
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
