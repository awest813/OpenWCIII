package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityDoom extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BNdm");
	private static final int DAMAGE_INTERVAL = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
	private War3ID buff;
	private War3ID demonType;
	private float damage;
	private int demonCount;
	private float demonLifetime;

	public CAbilityDoom(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.doom; }
	@Override public boolean isUniversal() { return true; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.buff = War3ID.NONE.equals(authoredBuff) ? DEFAULT_BUFF : authoredBuff;
		this.demonType = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
		this.damage = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.demonCount = definition.getFieldAsInteger(AbilityFields.DATA_B + level, 0);
		this.demonLifetime = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit unit) || unit.isHero() || unit.getFirstAbilityOfType(DoomBuff.class) != null) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		}
		else if (unit.getPlayerIndex() == WarsmashConstants.MAX_PLAYERS - 4 && unit.getUnitType().getLevel() > 5) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_CREATURE_IS_TOO_POWERFUL);
		}
		else receiver.targetOk(target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (target instanceof CUnit unit && unit.getFirstAbilityOfType(DoomBuff.class) == null
				&& unit.checkForAbilityEffectReaction(game, caster, this)) {
			unit.add(game, new DoomBuff(game.getHandleIdAllocator().createId(), this.buff, caster, this.damage,
					this.demonType, this.demonCount, this.demonLifetime));
		}
		return false;
	}

	private static final class DoomBuff extends CBuffTimed {
		private final StateModBuff silence = new StateModBuff(StateModBuffType.DISABLE_SPELLS, 1);
		private final CUnit caster;
		private final int casterPlayer;
		private final float damage;
		private final War3ID demon;
		private final int count;
		private final float lifetime;
		private int nextDamage;

		private DoomBuff(final int handleId, final War3ID alias, final CUnit caster, final float damage,
				final War3ID demon, final int count, final float lifetime) {
			super(handleId, alias, alias, 0);
			this.caster = caster;
			this.casterPlayer = caster.getPlayerIndex();
			this.damage = damage;
			this.demon = demon;
			this.count = count;
			this.lifetime = lifetime;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.nextDamage = game.getGameTurnTick() + DAMAGE_INTERVAL;
			unit.addStateModBuff(this.silence);
			unit.computeUnitState(game, this.silence.getBuffType());
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeStateModBuff(this.silence);
			unit.computeUnitState(game, this.silence.getBuffType());
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (game.getGameTurnTick() >= this.nextDamage) {
				this.nextDamage += DAMAGE_INTERVAL;
				unit.damage(game, this.caster, false, true, CAttackType.SPELLS, CDamageType.UNIVERSAL, "", this.damage);
			}
		}

		@Override
		public void onDeath(final CSimulation game, final CUnit unit) {
			if (this.demon != null && !War3ID.NONE.equals(this.demon) && this.count > 0) {
				unit.setRaisable(false);
				unit.setDecays(false);
				for (int index = 0; index < this.count; index++) {
					final CUnit summoned = game.createUnitSimple(this.demon, this.casterPlayer, unit.getX(), unit.getY(), unit.getFacing());
					if (summoned == null) continue;
					summoned.addClassification(CUnitClassification.SUMMONED);
					if (this.lifetime > 0) {
						summoned.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), getAlias(), this.lifetime, true));
					}
					this.caster.fireSummonEvents(game, summoned);
				}
			}
			super.onDeath(game, unit);
		}

		@Override public boolean isTimedLifeBar() { return false; }
	}
}
