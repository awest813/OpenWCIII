package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

/** Suspends and protects the target for the installed spell duration. */
public final class CAbilityCyclone extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bcyc");
	private static final float LIFT_HEIGHT = 200f;
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID buffId;
	private boolean dispellable;

	public CAbilityCyclone(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.cyclone;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		final String authoredBuff = definition.getField(AbilityFields.BUFF + level);
		this.buffId = authoredBuff.length() == 4 ? War3ID.fromString(authoredBuff) : DEFAULT_BUFF;
		this.dispellable = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0) != 0;
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (player.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit != null) {
			lift(game, unit, this.buffId, getDurationForTarget(unit), this.dispellable);
		}
		return false;
	}

	public static boolean lift(final CSimulation game, final CUnit unit, final War3ID buff, final float duration,
			final boolean dispellable) {
		if (duration <= 0 || unit.isDead() || unit.isInvulnerable() || unit.getFirstAbilityOfType(Lift.class) != null) {
			return false;
		}
		unit.add(game, new Lift(game.getHandleIdAllocator().createId(), buff, duration, dispellable));
		return true;
	}

	public static boolean hasDispellableLift(final CUnit unit) {
		final Lift lift = unit.getFirstAbilityOfType(Lift.class);
		return lift != null && lift.isDispellable();
	}

	private static final class Lift extends CBuffStun {
		private final StateModBuff protection = new StateModBuff(StateModBuffType.INVULNERABLE, 1);
		private final boolean dispellable;
		private float originalHeight;

		Lift(final int handleId, final War3ID alias, final float duration, final boolean dispellable) {
			super(handleId, alias, duration);
			this.dispellable = dispellable;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			super.onBuffAdd(game, unit);
			this.originalHeight = unit.getFlyHeight();
			unit.setFlyHeight(this.originalHeight + LIFT_HEIGHT);
			unit.addStateModBuff(this.protection);
			unit.computeUnitState(game, this.protection.getBuffType());
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.setFlyHeight(this.originalHeight);
			unit.removeStateModBuff(this.protection);
			unit.computeUnitState(game, this.protection.getBuffType());
			super.onBuffRemove(game, unit);
		}

		@Override
		protected SimulationRenderComponent createArt(final CSimulation game, final CUnit unit) {
			return game.createPersistentSpellEffectOnUnit(unit, getAlias(), CEffectType.EFFECT);
		}

		@Override
		public boolean isDispellable() {
			return this.dispellable;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.HARMFUL;
		}
	}
}
