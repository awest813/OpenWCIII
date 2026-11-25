package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissileSplash;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityOrbOfAnnihilation extends CAbilityAttackArrow {
	public static final War3ID CODE = War3ID.fromString("Afak");
	private int splashRadius;
	private float mediumDamageFactor;
	private float smallDamageFactor;

	public CAbilityOrbOfAnnihilation(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.flamingattacktarg; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.flamingattack; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.unflamingattack; }

	@Override
	public void populateData(final GameObject data, final int level) {
		super.populateData(data, level);
		this.splashRadius = Math.round(data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0));
		this.mediumDamageFactor = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.smallDamageFactor = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			setArrowTargets(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected boolean supportsAttack(final CUnitAttack attack) {
		return attack instanceof CUnitAttackMissileSplash;
	}

	@Override
	protected CUnitAttackMissile prepareAttack(final CUnitAttackMissile attack) {
		final CUnitAttackMissile prepared = super.prepareAttack(attack);
		final var splash = (CUnitAttackMissileSplash) (prepared == attack ? attack.copy() : prepared);
		splash.setAreaOfEffectFullDamage(0);
		splash.setAreaOfEffectMediumDamage(this.splashRadius / 2);
		splash.setAreaOfEffectSmallDamage(this.splashRadius);
		splash.setAreaOfEffectTargets(getTargetsAllowed().isEmpty() ? splash.getAreaOfEffectTargets() : getTargetsAllowed().clone());
		splash.setDamageFactorMedium(this.mediumDamageFactor);
		splash.setDamageFactorSmall(this.smallDamageFactor);
		return splash;
	}
}
