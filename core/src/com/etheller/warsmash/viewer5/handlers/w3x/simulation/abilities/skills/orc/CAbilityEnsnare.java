package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import java.util.EnumSet;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityEntanglingProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;

public final class CAbilityEnsnare extends CAbilityEntanglingProjectile {
	private static final War3ID GROUND_BUFF = War3ID.fromString("Beng");
	private static final War3ID AIR_BUFF = War3ID.fromString("Bena");

	public CAbilityEnsnare(final int handleId, final War3ID alias) {
		super(handleId, alias, GROUND_BUFF, AIR_BUFF, StateModBuffType.SNARED);
	}

	@Override
	protected void populateDefaultTargets(final GameObject data) {
		setTargetsAllowed(EnumSet.of(CTargetType.AIR, CTargetType.GROUND, CTargetType.ENEMIES, CTargetType.NON_ETHEREAL));
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.ensnare;
	}
}
