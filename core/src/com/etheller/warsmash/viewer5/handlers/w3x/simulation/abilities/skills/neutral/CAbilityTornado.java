package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityTornado extends CAbilityPointTargetSpellBase {
	private War3ID tornadoType;
	private CUnit tornado;
	private int endTick;

	public CAbilityTornado(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.tornado;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.tornadoType = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.tornadoType == null || War3ID.NONE.equals(this.tornadoType) || getDuration() <= 0) {
			return false;
		}
		this.tornado = game.createUnitSimple(this.tornadoType, caster.getPlayerIndex(), target.getX(), target.getY(),
				caster.getFacing());
		this.tornado.addClassification(CUnitClassification.SUMMONED);
		this.endTick = game.getGameTurnTick() + (int) Math.ceil(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
		caster.fireSummonEvents(game, this.tornado);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		return this.tornado != null && !this.tornado.isDead() && game.getGameTurnTick() < this.endTick;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target, final boolean interrupted) {
		endTornado(game);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit caster) {
		endTornado(game);
		super.onRemove(game, caster);
	}

	private void endTornado(final CSimulation game) {
		if (this.tornado != null && !this.tornado.isDead()) {
			this.tornado.kill(game);
		}
		this.tornado = null;
	}
}
