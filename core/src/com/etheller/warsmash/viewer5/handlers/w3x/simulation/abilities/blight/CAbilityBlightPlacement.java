package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.blight;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

public final class CAbilityBlightPlacement extends CAbilityPointTargetSpellBase {
	private float expansionAmount;
	private float radius;
	private boolean createsBlight;

	public CAbilityBlightPlacement(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds._allmodblightplacementsacrificialskullpassivesdetectionalsoflyersubmerge;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.expansionAmount = definition.readSLKTagFloat(AbilityFields.DATA_A + level);
		this.createsBlight = definition.readSLKTagBoolean(AbilityFields.DATA_B + level);
		this.radius = definition.readSLKTagFloat(AbilityFields.AREA_OF_EFFECT + level);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final float x = target.getX();
		final float y = target.getY();
		final float maximumRadius = this.radius;
		final float expansion = this.expansionAmount;
		final boolean blight = this.createsBlight;
		if (maximumRadius <= 0) return false;
		if (expansion <= 0 || getDuration() <= 0) {
			game.setBlight(x, y, maximumRadius, blight);
			return false;
		}
		final var expansionTimer = new CTimer() {
			private float currentRadius;

			@Override
			public void onFire(final CSimulation simulation) {
				this.currentRadius = Math.min(maximumRadius, this.currentRadius + expansion);
				simulation.setBlight(x, y, this.currentRadius, blight);
				if (this.currentRadius < maximumRadius) start(simulation);
			}
		};
		final float step = WarsmashConstants.SIMULATION_STEP_TIME;
		expansionTimer.setTimeoutTime((float) (StrictMath.ceil(getDuration() / step) * step));
		expansionTimer.start(game);
		return false;
	}
}
