package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.dreadlord.CAbilityInferno;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityRainOfChaos extends CAbilityPointTargetSpellBase {
	private War3ID impactAbility;
	private int impactCount;
	private float radius;

	public CAbilityRainOfChaos(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.rainofchaos;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.impactAbility = definition.getFieldAsWar3ID(AbilityFields.DATA_A + level, 0);
		this.impactCount = definition.getFieldAsInteger(AbilityFields.DATA_B + level, 0);
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final var type = game.getAbilityData().getAbilityType(this.impactAbility);
		if (type == null || this.impactCount <= 0) return false;
		if (!(type.createAbility(game.getHandleIdAllocator().createId()) instanceof CAbilityInferno inferno)) return false;
		final var random = game.getSeededRandom();
		for (int impact = 0; impact < this.impactCount; impact++) {
			final double angle = random.nextDouble() * 2 * Math.PI;
			final double distance = Math.sqrt(random.nextDouble()) * this.radius;
			final float x = target.getX() + (float) (Math.cos(angle) * distance);
			final float y = target.getY() + (float) (Math.sin(angle) * distance);
			inferno.doEffect(game, caster, new AbilityPointTarget(x, y));
		}
		return false;
	}
}
