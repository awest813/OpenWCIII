package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.List;
import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;

/** Ault and its item aliases grant daytime sight at night while their requirements are met. */
public final class CAbilityUltravision extends CAbilityPassiveSpellBase {
    private List<CUnitTypeRequirement> requirements;

    public CAbilityUltravision(final int handleId, final War3ID alias) {
        super(handleId, War3ID.fromString("Ault"), alias);
    }

    @Override
    public void populateData(final GameObject ability, final int level) {
        this.requirements = CUnitData.parseRequirements(ability.getFieldAsList("Requires"),
                ability.getFieldAsList("Requiresamount"));
    }

    public boolean grantsNightVision(final CSimulation game, final CUnit unit) {
        if (isDisabled()) return false;
        return isRequirementsMet(game, unit);
    }

    @Override
    public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
        // Evaluate the current owner so research removal, pending units, and ownership changes
        // take effect without mutating the shared unit type or caching stale requirements.
        final var player = game.getPlayer(unit.getPlayerIndex());
        for (final CUnitTypeRequirement requirement : this.requirements) {
            if (player.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) return false;
        }
        return true;
    }
}
