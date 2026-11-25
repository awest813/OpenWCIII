package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityFragmentationShards extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Afsh");
	private List<CUnitTypeRequirement> requirements = List.of();
	private float fullRadius;
	private float mediumRadius;
	private float smallRadius;
	private float fullBonus;
	private float mediumBonus;
	private float smallBonus;

	public CAbilityFragmentationShards(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		this.fullRadius = getAreaOfEffect();
		this.mediumRadius = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.smallRadius = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.fullBonus = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.mediumBonus = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.smallBonus = definition.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	public float getMaximumRadius() {
		return Math.max(this.fullRadius, Math.max(this.mediumRadius, this.smallRadius));
	}

	public float getBonus(final CSimulation game, final CUnit source, final CUnit target, final double distance) {
		final CDefenseType defense = target.getDefenseType();
		if (defense != CDefenseType.MEDIUM && defense != CDefenseType.NONE) return 0;
		if (!target.canBeTargetedBy(game, source, false, getTargetsAllowed(),
				BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return 0;
		if (distance <= this.fullRadius) return this.fullBonus;
		if (distance <= this.mediumRadius) return this.mediumBonus;
		return distance <= this.smallRadius ? this.smallBonus : 0;
	}
}
