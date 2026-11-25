package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityAntiMagicShell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityItemAntiMagicPotion extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("Aami");
	private War3ID buffId;

	public CAbilityItemAntiMagicPotion(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.antimagicshell;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		CAbilityAntiMagicShell.apply(game, caster, this.buffId, getDurationForTarget(caster), 0);
		return false;
	}
}
