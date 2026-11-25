package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

public final class CAbilitySleepAlways extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Asla");
	private final StateModBuff sleep = new StateModBuff(StateModBuffType.SLEEPING, 1);
	private boolean sleepOnce;
	private boolean allowAnyPlayer;
	private boolean hasSlept;
	private int nextSleepTick;

	public CAbilitySleepAlways(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.sleepOnce = definition.getFieldAsBoolean(AbilityFields.DATA_A + level, 0);
		this.allowAnyPlayer = definition.getFieldAsBoolean(AbilityFields.DATA_B + level, 0);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		if (isDisabled() || unit.isPaused() || unit.isHidden() || (this.sleepOnce && this.hasSlept)
				|| (!this.allowAnyPlayer && game.getPlayer(unit.getPlayerIndex()).getController() != CMapControl.NEUTRAL)) return;
		final int tick = game.getGameTurnTick();
		if (unit.isUnitType(CUnitTypeJass.SLEEPING)
				|| (unit.getCurrentBehavior() != null && unit.getCurrentBehavior().getBehaviorCategory() != CBehaviorCategory.IDLE)) {
			this.nextSleepTick = tick + game.getGameplayConstants().getGuardReturnTicks();
			return;
		}
		if (tick >= this.nextSleepTick) {
			this.hasSlept = true;
			unit.addStateModBuff(this.sleep);
			unit.computeUnitState(game, StateModBuffType.SLEEPING);
		}
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removeStateModBuff(this.sleep);
		unit.computeUnitState(game, StateModBuffType.SLEEPING);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		onRemove(game, unit);
	}
}
