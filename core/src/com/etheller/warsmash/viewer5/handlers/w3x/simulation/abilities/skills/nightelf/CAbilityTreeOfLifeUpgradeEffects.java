package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

public final class CAbilityTreeOfLifeUpgradeEffects extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Atol");
	private SimulationRenderComponentModel effect;

	public CAbilityTreeOfLifeUpgradeEffects(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	public void beginUpgrade(final CSimulation game, final CUnit tree) {
		endUpgrade();
		this.effect = game.createPersistentSpellEffectOnUnit(tree, getAlias(), CEffectType.TARGET);
	}

	public void endUpgrade() {
		if (this.effect != null) {
			this.effect.remove();
			this.effect = null;
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit tree) {
		endUpgrade();
	}

	@Override
	public void onRemoveDisabled(final CSimulation game, final CUnit tree) {
		endUpgrade();
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit tree) {
		endUpgrade();
	}
}
