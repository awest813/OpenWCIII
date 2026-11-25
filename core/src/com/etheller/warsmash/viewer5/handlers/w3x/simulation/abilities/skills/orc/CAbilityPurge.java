package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityPurge extends CAbilityTargetSpellBase {
	private static final War3ID PURGE_BUFF = War3ID.fromString("Bprg");
	private War3ID buffId;
	private float slow;
	private float summonDamage;
	private float unitPause;
	private float heroPause;

	public CAbilityPurge(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.purge;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final float movementFactor = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.slow = 1 - 1 / Math.max(1, movementFactor);
		this.summonDamage = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.unitPause = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.heroPause = definition.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.buffId = authoredBuff == null || War3ID.NONE.equals(authoredBuff) ? PURGE_BUFF : authoredBuff;
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			getTargetsAllowed().addAll(
					CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit || target instanceof CDestructable) receiver.targetOk(target);
		else receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CDestructable tree = target.visit(AbilityTargetVisitor.DESTRUCTABLE);
		if (tree != null) {
			tree.dispelBuffs(game);
			game.spawnSpellEffectOnPoint(tree.getX(), tree.getY(), 0, getAlias(), CEffectType.SPECIAL, 0);
			return false;
		}
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit == null || !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		unit.dispelBuffs(game);
		final boolean enemy = !game.getPlayer(caster.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE);
		if (enemy && unit.isSummonedOrWard() && this.summonDamage > 0) {
			unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, this.summonDamage);
		}
		final float duration = getDurationForTarget(unit);
		final float pause = Math.max(0, unit.isHero() ? this.heroPause : this.unitPause);
		if (enemy && !unit.isDead() && duration > 0 && (this.slow > 0 || pause > 0)) {
			final PurgedMovement prior = unit.getFirstAbilityOfType(PurgedMovement.class);
			if (prior != null) unit.remove(game, prior);
			unit.add(game, new PurgedMovement(game.getHandleIdAllocator().createId(), this.buffId, duration, pause, this.slow));
		}
		else {
			game.spawnSpellEffectOnPoint(unit.getX(), unit.getY(), 0, getAlias(), CEffectType.SPECIAL, 0);
		}
		return false;
	}

	private static final class PurgedMovement extends CBuffTimed {
		private final NonStackingStatBuff movement;
		private final float pause;
		private final float slow;

		private PurgedMovement(final int handleId, final War3ID alias, final float duration, final float pause,
				final float slow) {
			super(handleId, PURGE_BUFF, alias, duration);
			this.pause = Math.min(pause, duration);
			this.slow = slow;
			this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, PURGE_BUFF.toString(),
					pause > 0 ? -1 : -slow);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.movement);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.movement);
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			final float remaining = getDurationRemaining(game, unit);
			final float recoveryWindow = getDurationMax() - this.pause;
			if (remaining > 0 && remaining <= recoveryWindow) {
				this.movement.setValue(-this.slow * remaining / recoveryWindow);
				unit.computeDerivedFields(this.movement.getBuffType());
			}
			super.onTick(game, unit);
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
