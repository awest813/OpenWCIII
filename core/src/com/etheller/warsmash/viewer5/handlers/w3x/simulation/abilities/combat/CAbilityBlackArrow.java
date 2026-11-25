package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat;

import java.util.EnumSet;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityBlackArrow extends CAbilityAttackArrow {

	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BNba");
	private static final War3ID TIMED_LIFE = War3ID.fromString("BTLF");
	private War3ID arrowBuff;
	private War3ID minion;
	private int minionCount;
	private float minionLifetime;

	public CAbilityBlackArrow(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.blackarrow; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.blackarrowon; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.blackarrowoff; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		super.populateData(definition, level);
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.arrowBuff = War3ID.NONE.equals(buff) ? DEFAULT_BUFF : buff;
		this.minion = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
		this.minionCount = definition.getFieldAsInteger(AbilityFields.DATA_B + level, 0);
		this.minionLifetime = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			setArrowTargets(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected CUnitAttackListener impactListener(final CSimulation game, final CUnit caster,
			final CUnitAttackListener listener) {
		final War3ID buff = this.arrowBuff;
		final War3ID minionType = this.minion;
		final int count = this.minionCount;
		final float lifetime = this.minionLifetime;
		final float duration = getDuration();
		final int owner = caster.getPlayerIndex();
		final EnumSet<CTargetType> targets = getTargetsAllowed().clone();
		return new CUnitAttackListener() {
			@Override public void onLaunch() { listener.onLaunch(); }
			@Override public void onHit(final AbilityTarget target, final float damage) { listener.onHit(target, damage); }

			@Override
			public void onBeforeDamage(final AbilityTarget target) {
				listener.onBeforeDamage(target);
				if (target instanceof CUnit unit && !unit.isDead() && !unit.isHero() && !unit.isSummonedOrWard()
						&& minionType != null && !War3ID.NONE.equals(minionType) && duration > 0 && count > 0
						&& unit.canBeTargetedBy(game, caster, targets) && unit.checkForAbilityEffectReaction(game, caster, CAbilityBlackArrow.this)) {
					final BlackArrowMark existing = unit.getFirstAbilityOfType(BlackArrowMark.class);
					if (existing != null) unit.remove(game, existing);
					unit.add(game, new BlackArrowMark(game.getHandleIdAllocator().createId(), buff, duration,
							caster, owner, minionType, count, lifetime));
				}
			}
		};
	}

	private static final class BlackArrowMark extends CBuffTimed {
		private final CUnit caster;
		private final int owner;
		private final War3ID minionType;
		private final int count;
		private final float lifetime;

		private BlackArrowMark(final int handleId, final War3ID buff, final float duration, final CUnit caster,
				final int owner, final War3ID minionType, final int count, final float lifetime) {
			super(handleId, buff, buff, duration);
			this.caster = caster;
			this.owner = owner;
			this.minionType = minionType;
			this.count = count;
			this.lifetime = lifetime;
		}

		@Override protected void onBuffAdd(final CSimulation game, final CUnit unit) { }
		@Override protected void onBuffRemove(final CSimulation game, final CUnit unit) { }
		@Override public boolean isDispellable() { return true; }
		@Override public DispelPolarity getDispelPolarity() { return DispelPolarity.HARMFUL; }
		@Override public boolean isHeroSpellBuff() { return true; }
		@Override public boolean isTimedLifeBar() { return false; }

		@Override
		public void onDeath(final CSimulation game, final CUnit unit) {
			unit.setRaisable(false);
			unit.setDecays(false);
			for (int index = 0; index < this.count; index++) {
				final CUnit minion = game.createUnitSimple(this.minionType, this.owner, unit.getX(), unit.getY(), unit.getFacing());
				if (minion == null) continue;
				minion.addClassification(CUnitClassification.SUMMONED);
				if (this.lifetime > 0) {
					minion.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), TIMED_LIFE, this.lifetime, true));
				}
				this.caster.fireSummonEvents(game, minion);
			}
			super.onDeath(game, unit);
		}
	}
}
