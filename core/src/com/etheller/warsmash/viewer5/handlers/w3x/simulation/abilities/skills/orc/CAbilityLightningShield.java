package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityLightningShield extends CAbilityTargetSpellBase {
	private static final War3ID SHIELD_BUFF = War3ID.fromString("Blsh");
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID buffId;
	private float radius;
	private float damage;

	public CAbilityLightningShield(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.lightningshield;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.damage = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.buffId = authoredBuff == null || War3ID.NONE.equals(authoredBuff) ? SHIELD_BUFF : authoredBuff;
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			getTargetsAllowed().addAll(
					CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (owner.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit carrier = target.visit(AbilityTargetVisitor.UNIT);
		if (carrier == null || !carrier.checkForAbilityEffectReaction(game, caster, this)) return false;
		final float duration = getDurationForTarget(carrier);
		if (duration <= 0) return false;
		final LiveShield previous = carrier.getFirstAbilityOfType(LiveShield.class);
		if (previous != null) carrier.remove(game, previous);
		carrier.add(game, new LiveShield(game.getHandleIdAllocator().createId(), this.buffId, duration, caster,
				this.radius, this.damage, getTargetsAllowed().clone()));
		return false;
	}

	private static final class LiveShield extends CBuffTimed {
		private static final int PULSE_TICKS = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		private final CUnit caster;
		private final float radius;
		private final float damage;
		private final EnumSet<CTargetType> targetsAllowed;
		private int nextPulse;

		private LiveShield(final int handleId, final War3ID buffId, final float duration, final CUnit caster,
				final float radius, final float damage, final EnumSet<CTargetType> targetsAllowed) {
			super(handleId, SHIELD_BUFF, buffId, duration);
			this.caster = caster;
			this.radius = radius;
			this.damage = damage;
			this.targetsAllowed = targetsAllowed;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit carrier) {
			this.nextPulse = game.getGameTurnTick() + PULSE_TICKS;
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit carrier) {
		}

		@Override
		public void onTick(final CSimulation game, final CUnit carrier) {
			final int tick = game.getGameTurnTick();
			if (tick >= this.nextPulse && (tick == this.nextPulse || getDurationRemaining(game, carrier) > 0)) {
				this.nextPulse = tick + PULSE_TICKS;
				final var nearby = new ArrayList<CUnit>();
				game.getWorldCollision().enumUnitsInRange(carrier.getX(), carrier.getY(), this.radius, unit -> {
					if (unit != carrier && !unit.isHidden() && !unit.isDead()
							&& unit.canBeTargetedBy(game, this.caster, false, this.targetsAllowed,
									BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) nearby.add(unit);
					return false;
				});
				for (final CUnit unit : nearby) {
					if (unit.isDead() || unit.isHidden()) continue;
					game.spawnSpellEffectOnPoint(unit.getX(), unit.getY(), 0, getAlias(), CEffectType.SPECIAL, 0);
					unit.damage(game, this.caster, false, false, CAttackType.SPELLS, CDamageType.MAGIC, null, this.damage);
				}
			}
			if (!carrier.isDead()) super.onTick(game, carrier);
		}

		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}
