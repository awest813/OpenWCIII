package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitRace;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityDarkConversion extends CAbilityTargetSpellBase {
	private static final int AUTOCAST_INTERVAL_TICKS = 10;
	private CUnitRace targetRace;
	private War3ID convertedUnitId;
	private int nextAutocastTick;

	public CAbilityDarkConversion(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject abilityData, final int level) {
		this.targetRace = CUnitRace.parseRace(abilityData.getFieldAsString(AbilityFields.DATA_A + level, 0));
		final String unitId = abilityData.getFieldAsString(AbilityFields.UNIT_ID + level, 0);
		this.convertedUnitId = unitId.length() == 4 ? War3ID.fromString(unitId) : null;
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.darkconversion;
	}

	@Override
	public float getCastDelay() {
		return getCastingTime();
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (canConvert(caster, target.visit(AbilityTargetVisitor.UNIT))) {
			receiver.targetOk(target);
		}
		else {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		}
	}

	private boolean canConvert(final CUnit caster, final CUnit target) {
		return target != null && target != caster && !target.isDead() && !target.isHidden()
				&& !target.isUnitType(CUnitTypeJass.SLEEPING) && target.getUnitType().getRace() == this.targetRace;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		final int tick = game.getGameTurnTick();
		if (tick < this.nextAutocastTick || isDisabled() || caster.isHidden() || caster.isPaused()
				|| game.getPlayer(caster.getPlayerIndex()).getController() != CMapControl.COMPUTER
				|| (caster.getCurrentBehavior() != null
						&& caster.getCurrentBehavior().getBehaviorCategory() == CBehaviorCategory.SPELL)) {
			return;
		}
		this.nextAutocastTick = tick + AUTOCAST_INTERVAL_TICKS;
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), getCastRange(),
				target -> target.getPlayerIndex() != caster.getPlayerIndex() && canConvert(caster, target)
						&& caster.orderAutomatically(game, getBaseOrderId(), target));
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit victim = target.visit(AbilityTargetVisitor.UNIT);
		final War3ID replacementId = this.convertedUnitId;
		if (!canConvert(caster, victim) || replacementId == null || game.getUnitData().getUnitType(replacementId) == null) {
			return false;
		}
		final StateModBuff sleeping = new StateModBuff(StateModBuffType.SLEEPING, 1);
		victim.addStateModBuff(sleeping);
		victim.computeUnitState(game, StateModBuffType.SLEEPING);
		final CTimer conversion = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				victim.removeStateModBuff(sleeping);
				if (victim.isDead() || simulation.getUnit(victim.getHandleId()) != victim) {
					return;
				}
				victim.computeUnitState(simulation, StateModBuffType.SLEEPING);
				if (!victim.isHidden()) {
					final CUnit converted = simulation.createUnitSimple(replacementId, caster.getPlayerIndex(),
							victim.getX(), victim.getY(), victim.getFacing());
					if (converted != null) {
						simulation.removeUnit(victim);
						caster.fireSummonEvents(simulation, converted);
					}
				}
			}
		};
		conversion.setTimeoutTime(getDurationForTarget(victim));
		conversion.start(game);
		return false;
	}
}
