package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;

/** Displays a passive on the command card while its research is active. */
public class CAbilityResearchIndicator extends CAbilityPassiveSpellBase {
	private final CUnitTypeRequirement defaultRequirement;
	private List<CUnitTypeRequirement> requirements = List.of();

	public CAbilityResearchIndicator(final int handleId, final War3ID alias, final War3ID code, final War3ID research) {
		super(handleId, code, alias);
		this.defaultRequirement = new CUnitTypeRequirement(research, 1);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		if (this.requirements.isEmpty()) {
			this.requirements = List.of(this.defaultRequirement);
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void onAddDisabled(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		setIconShowing(true);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}
}
