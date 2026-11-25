package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

public final class CAbilityMineTrigger extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Amin");
	private final StateModBuff walkOver = new StateModBuff(StateModBuffType.DISABLE_UNIT_COLLISION, 1);
	private int armingTicks;
	private int armedAt;
	private boolean active;

	public CAbilityMineTrigger(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final float delay = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.armingTicks = (int) Math.ceil(delay / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit mine) {
		this.active = true;
		this.armedAt = game.getGameTurnTick() + this.armingTicks;
		mine.addStateModBuff(this.walkOver);
		mine.computeUnitState(game, StateModBuffType.DISABLE_UNIT_COLLISION);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit mine) {
		if (!this.active || game.getGameTurnTick() < this.armedAt) return;
		final var owner = game.getPlayer(mine.getPlayerIndex());
		game.getWorldCollision().enumUnitsInRange(mine.getX(), mine.getY(), getCastRange(), unit -> {
			if (!unit.isDead() && !unit.isHidden() && !unit.isBuilding() && !unit.isUnitType(CUnitTypeJass.FLYING)
					&& mine.getPlayerIndex() != unit.getPlayerIndex()
					&& !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
				mine.kill(game);
				return true;
			}
			return false;
		});
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit mine) {
		this.active = false;
		mine.removeStateModBuff(this.walkOver);
		mine.computeUnitState(game, StateModBuffType.DISABLE_UNIT_COLLISION);
	}
}
