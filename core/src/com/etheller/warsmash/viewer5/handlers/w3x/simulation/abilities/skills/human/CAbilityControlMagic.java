package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.darkranger.CAbilityCharm;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityControlMagic extends CAbilityTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("Acmg");
	private List<CUnitTypeRequirement> requirements = List.of();
	private int maximumLevel;
	private float manaPerHitPoint;

	public CAbilityControlMagic(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.controlmagic;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.maximumLevel = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.manaPerHitPoint = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public int getManaCost(final AbilityTarget target) {
		// The original expansion prices Control Magic by maximum life.
		return super.getManaCost() + (target instanceof CUnit unit ? Math.round(unit.getMaximumLife() * this.manaPerHitPoint) : 0);
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		return this.requirements.stream().allMatch(requirement -> isRequirementMet(game, unit, requirement));
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		for (final var requirement : this.requirements) {
			if (!isRequirementMet(game, unit, requirement)) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	private static boolean isRequirementMet(final CSimulation game, final CUnit unit, final CUnitTypeRequirement requirement) {
		return game.getPlayer(unit.getPlayerIndex()).getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel();
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit unit) || !unit.isSummonedOrWard() || unit.isHero() || unit.isBuilding()) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_SUMMONED_UNITS);
		}
		else if (unit.getUnitType().getLevel() > this.maximumLevel) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_CREATURE_IS_TOO_POWERFUL);
		}
		else if (caster.getMana() < getManaCost(unit)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_MANA);
		}
		else receiver.targetOk(unit);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (target instanceof CUnit unit && !game.getPlayer(caster.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(),
				CAllianceType.PASSIVE) && unit.checkForAbilityEffectReaction(game, caster, this)) {
			game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
			CAbilityCharm.transferUnit(game, caster, unit);
		}
		return false;
	}
}
