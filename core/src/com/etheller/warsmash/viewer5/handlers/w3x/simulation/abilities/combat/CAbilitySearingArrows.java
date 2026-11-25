package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilitySearingArrows extends CAbilityAttackArrow {
	public CAbilitySearingArrows(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.flamingarrowstarg; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.flamingarrows; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.unflamingarrows; }
}
