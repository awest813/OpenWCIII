package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityResearchIndicator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityLiquidFire extends CAbilityResearchIndicator implements CUnitAttackPostDamageListener {
	public static final War3ID CODE = War3ID.fromString("Aliq");
	private static final War3ID RESEARCH = War3ID.fromString("Rolf");
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bliq");
	private float damage;
	private float attackSlow;
	private War3ID buffId;

	public CAbilityLiquidFire(final int handleId, final War3ID alias) {
		super(handleId, alias, CODE, RESEARCH);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		super.populateData(data, level);
		this.damage = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.attackSlow = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.buffId = War3ID.NONE.equals(buff) ? DEFAULT_BUFF : buff;
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		unit.addPostDamageListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this);
		super.onRemove(game, unit);
	}

	@Override
	public void onHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float actualDamage) {
		if (actualDamage <= 0 || !(target instanceof CUnit building) || !building.isBuilding() || building.isDead()
				|| building.isInvulnerable() || !building.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
						new BooleanAbilityTargetCheckReceiver<CWidget>())) return;
		final Burning current = building.getFirstAbilityOfType(Burning.class);
		if (current == null) {
			building.add(game, new Burning(game.getHandleIdAllocator().createId(), this.buffId, getDurationForTarget(building),
					attacker, this.damage, this.attackSlow));
		}
		else current.refresh(game, attacker);
	}

	public static boolean blocksRepair(final AbilityTarget target) {
		return target instanceof CUnit unit && unit.getFirstAbilityOfType(Burning.class) != null;
	}

	private static final class Burning extends CBuffTimed {
		private static final int SECOND = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		private CUnit source;
		private final float damage;
		private final NonStackingStatBuff slow;
		private int nextDamage;

		private Burning(final int handleId, final War3ID alias, final float duration, final CUnit source,
				final float damage, final float attackSlow) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.source = source;
			this.damage = damage;
			this.slow = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "liquidfire", -attackSlow);
		}

		private void refresh(final CSimulation game, final CUnit source) {
			this.source = source;
			refreshDuration(game);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.nextDamage = game.getGameTurnTick() + SECOND;
			unit.addNonStackingStatBuff(this.slow);
		}

		@Override protected void onBuffRemove(final CSimulation game, final CUnit unit) { unit.removeNonStackingStatBuff(this.slow); }

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (game.getGameTurnTick() >= this.nextDamage) {
				this.nextDamage += SECOND;
				unit.damage(game, this.source, false, true, CAttackType.SPELLS, CDamageType.FIRE, null, this.damage);
			}
			if (!unit.isDead()) super.onTick(game, unit);
		}

		@Override public boolean isTimedLifeBar() { return false; }
		@Override public boolean isDispellable() { return true; }
		@Override public DispelPolarity getDispelPolarity() { return DispelPolarity.HARMFUL; }
	}
}
