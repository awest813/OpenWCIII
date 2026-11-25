package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;

/** Returns a share of the attacked structure's cost in proportion to health removed. */
public final class CAbilityPillage extends CAbilityPassiveSpellBase {
	private List<CUnitTypeRequirement> requirements;
	private float costRatio;
	private int accumulationStep;
	private double accumulatedGold;
	private double accumulatedLumber;

	public CAbilityPillage(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.costRatio = ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.accumulationStep = Math.max(1, ability.getFieldAsInteger(AbilityFields.DATA_B + level, 0));
		this.requirements = CUnitData.parseRequirements(ability.getFieldAsList(AbilityFields.REQUIREMENTS),
				ability.getFieldAsList(AbilityFields.REQUIREMENT_LEVELS));
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement -> owner.getTechtreeUnlocked(
				requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		for (final CUnitTypeRequirement requirement : this.requirements) {
			if (owner.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	public void collect(final CSimulation game, final CUnit attacker, final CUnit structure, final float damage) {
		if (isDisabled() || attacker.isIllusion() || !isRequirementsMet(game, attacker)
				|| !structure.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
						BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return;

		final double portion = Math.min(damage, structure.getLife()) / (double) structure.getMaximumLife() * this.costRatio;
		this.accumulatedGold += portion * structure.getUnitType().getGoldCost();
		this.accumulatedLumber += portion * structure.getUnitType().getLumberCost();
		final int gold = (int) (this.accumulatedGold / this.accumulationStep) * this.accumulationStep;
		final int lumber = (int) (this.accumulatedLumber / this.accumulationStep) * this.accumulationStep;
		final var owner = game.getPlayer(attacker.getPlayerIndex());
		if (gold > 0) {
			this.accumulatedGold -= gold;
			owner.addGold(gold);
			game.unitGainResourceEvent(structure, owner.getId(), ResourceType.GOLD, gold);
		}
		if (lumber > 0) {
			this.accumulatedLumber -= lumber;
			owner.addLumber(lumber);
			game.unitGainResourceEvent(structure, owner.getId(), ResourceType.LUMBER, lumber);
		}
	}
}
