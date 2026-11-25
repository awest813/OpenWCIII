package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityPulverize extends CAbilityPassiveSpellBase implements CUnitAttackPostDamageListener {
	public static final War3ID CODE = War3ID.fromString("Awar");
	private List<CUnitTypeRequirement> requirements = List.of();
	private float chance;
	private float damage;
	private float fullRadius;
	private float halfRadius;

	public CAbilityPulverize(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.chance = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damage = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.fullRadius = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.halfRadius = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"), definition.getFieldAsList("Requiresamount"));
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

	@Override
	public void onAddDisabled(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		setIconShowing(true);
		unit.addPostDamageListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this);
		setIconShowing(false);
	}

	@Override
	public void onHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float actualDamage) {
		if (actualDamage <= 0 || this.damage <= 0 || this.chance <= 0
				|| (this.chance < 100 && game.getSeededRandom().nextFloat() * 100 >= this.chance)) return;
		game.createTemporarySpellEffectOnUnit(attacker, getAlias(), CEffectType.SPECIAL);
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		final float radius = Math.max(this.fullRadius, this.halfRadius);
		game.getWorldCollision().enumUnitsInRange(attacker.getX(), attacker.getY(), radius, unit -> {
			if (unit != attacker && !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& unit.canBeTargetedBy(game, attacker, false, getTargetsAllowed(), receiver.reset())) {
				final float nearbyDamage = unit.canReach(attacker.getX(), attacker.getY(), this.fullRadius) ? this.damage : this.damage * 0.5f;
				unit.damage(game, attacker, false, false, CAttackType.SPELLS, CDamageType.MAGIC, null, nearbyDamage);
			}
			return false;
		});
	}
}
