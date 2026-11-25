package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

/** Displays the authored barricades when the existing retaliatory damage is enabled. */
public final class CAbilitySpikedBarricades extends CAbilityPassiveSpellBase {
	private static final War3ID CODE = War3ID.fromString("Aspi");
	private SimulationRenderComponent barricades;

	public CAbilitySpikedBarricades(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		final boolean active = !isDisabled() && !unit.isConstructing() && !unit.isHidden()
				&& game.getUpgradeData().getSpikedBarricadesDamage(unit.getUnitType(), game.getPlayer(unit.getPlayerIndex())) > 0;
		if (active && this.barricades == null) {
			this.barricades = game.createPersistentSpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		}
		else if (!active) clearBarricades();
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		clearBarricades();
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		clearBarricades();
	}

	private void clearBarricades() {
		if (this.barricades != null) {
			this.barricades.remove();
			this.barricades = null;
		}
	}
}
