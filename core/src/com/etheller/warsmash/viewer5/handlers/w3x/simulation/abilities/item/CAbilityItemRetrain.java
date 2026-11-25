package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityItemRetrain extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("Aret");

	public CAbilityItemRetrain(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemretrain;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (unit.isHero() && !unit.isIllusion()) receiver.useOk();
		else receiver.activationCheckFailed(CommandStringErrorKeys.UNABLE_TO_USE_THIS_ITEM);
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		if (orderId != getBaseOrderId() || !caster.isHero() || caster.isIllusion()) return true;
		doEffect(game, caster, target);
		return false;
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		caster.getHeroData().retrain(game, caster);
		return false;
	}
}
