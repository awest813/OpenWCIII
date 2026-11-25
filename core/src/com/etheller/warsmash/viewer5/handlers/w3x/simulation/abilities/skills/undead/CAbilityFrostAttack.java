package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CBuffFrost;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityFrostAttack extends CAbilityPassiveSpellBase implements CUnitAttackPostDamageListener {
	public static final War3ID ATTACK = War3ID.fromString("Afra");
	public static final War3ID BREATH = War3ID.fromString("Afrb");
	public static final War3ID FREEZING_BREATH = War3ID.fromString("Afrz");
	private List<CUnitTypeRequirement> requirements = List.of();

	public CAbilityFrostAttack(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
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
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.addPostDamageListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this);
	}

	@Override
	public void onHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float damage) {
		if (target instanceof CUnit unit) applyFrost(game, attacker, unit, damage);
	}

	@Override
	public void onSplashHit(final CSimulation game, final CUnit attacker, final CUnit target, final float damage) {
		if (BREATH.equals(getCode())) applyFrost(game, attacker, target, damage);
	}

	private void applyFrost(final CSimulation game, final CUnit attacker, final CUnit target, final float damage) {
		if (isDisabled() || damage <= 0 || target.isDead() || target.isBuilding() != FREEZING_BREATH.equals(getCode())
				|| game.getPlayer(attacker.getPlayerIndex()).hasAlliance(target.getPlayerIndex(), CAllianceType.PASSIVE)
				|| !target.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
						BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return;
		final float duration = getDurationForTarget(target);
		if (target.isBuilding()) {
			if (duration <= 0) return;
			final var previous = target.getFirstAbilityOfType(FrozenBuilding.class);
			if (previous != null) {
				if (previous.getDurationRemaining(game, target) >= duration) return;
				target.remove(game, previous);
			}
			target.add(game, new FrozenBuilding(game.getHandleIdAllocator().createId(), getAlias(), duration));
		}
		else CBuffFrost.apply(game, target, duration);
	}

	private static final class FrozenBuilding extends CBuffTimed {
		private FrozenBuilding(final int handleId, final War3ID alias, final float duration) {
			super(handleId, FREEZING_BREATH, alias, duration);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.setFrozenBuilding(true);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.setFrozenBuilding(false);
		}

		@Override
		public boolean ticksWhilePaused() {
			return true;
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
