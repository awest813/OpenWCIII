package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackFinalDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;

public final class CAbilityHardenedSkin extends CAbilityPassiveSpellBase
		implements CUnitAttackFinalDamageTakenModificationListener {
	public static final War3ID CODE = War3ID.fromString("Assk");
	private List<CUnitTypeRequirement> requirements = List.of();
	private float chance;
	private float minimum;
	private float reduction;
	private boolean melee;
	private boolean ranged;

	public CAbilityHardenedSkin(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.chance = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0) / 100;
		this.minimum = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.reduction = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.melee = definition.getFieldAsBoolean(AbilityFields.DATA_D + level, 0);
		this.ranged = definition.getFieldAsBoolean(AbilityFields.DATA_E + level, 0);
		this.requirements = definition.getFieldAsInteger("checkDep", 0) == 0 ? List.of()
				: CUnitData.parseRequirements(definition.getFieldAsList("Requires"), definition.getFieldAsList("Requiresamount"));
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.addFinalDamageTakenModificationListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removeFinalDamageTakenModificationListener(this);
	}

	@Override
	public float onDamage(final CSimulation game, final CUnit attacker, final CUnit target, final boolean isAttack,
			final boolean isRanged, final CAttackType attackType, final CDamageType damageType, final float amount) {
		if (!isAttack || attacker == null || amount <= this.minimum || this.chance <= 0
				|| !(isRanged ? this.ranged : this.melee)) return amount;
		final boolean allied = game.getPlayer(target.getPlayerIndex()).hasAlliance(attacker.getPlayerIndex(), CAllianceType.PASSIVE);
		final boolean allowsAllies = getTargetsAllowed().contains(CTargetType.ALLIES) || getTargetsAllowed().contains(CTargetType.FRIEND);
		final boolean allowsEnemies = getTargetsAllowed().contains(CTargetType.ENEMIES);
		if ((allowsAllies || allowsEnemies) && !(allied ? allowsAllies : allowsEnemies)) return amount;
		if (this.chance < 1 && game.getSeededRandom().nextFloat() >= this.chance) return amount;
		return Math.max(this.minimum, amount - this.reduction);
	}
}
