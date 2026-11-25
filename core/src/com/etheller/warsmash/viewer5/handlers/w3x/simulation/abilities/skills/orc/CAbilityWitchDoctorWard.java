package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CPlayerFogOfWar;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityWitchDoctorWard extends CAbilityPointTargetSpellBase {
	public enum Kind {
		SENTRY("oeye", "Beye", OrderIds.evileye),
		HEALING("ohwd", "Bhwd", OrderIds.healingward),
		STASIS("otot", "Bsta", OrderIds.stasistrap),
		SERPENT("osp1", "BTLF", OrderIds.ward);

		private final War3ID unitId;
		private final War3ID buffId;
		private final int orderId;

		Kind(final String unitId, final String buffId, final int orderId) {
			this.unitId = War3ID.fromString(unitId);
			this.buffId = War3ID.fromString(buffId);
			this.orderId = orderId;
		}
	}

	private final Kind kind;
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID unitId;
	private War3ID buffId;
	private int count;
	private float armTime;
	private float triggerRadius;
	private float stunRadius;
	private float stunDuration;

	public CAbilityWitchDoctorWard(final int handleId, final War3ID alias, final Kind kind) {
		super(handleId, alias);
		this.kind = kind;
	}

	@Override
	public int getBaseOrderId() {
		return this.kind.orderId;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final War3ID authoredUnit = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
		this.unitId = authoredUnit == null || War3ID.NONE.equals(authoredUnit) ? this.kind.unitId : authoredUnit;
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.buffId = authoredBuff == null || War3ID.NONE.equals(authoredBuff) ? this.kind.buffId : authoredBuff;
		this.count = this.kind == Kind.SERPENT ? Math.max(1, definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0)) : 1;
		this.requirements = definition.getFieldAsInteger("checkDep", 0) == 0 ? List.of()
				: CUnitData.parseRequirements(definition.getFieldAsList("Requires"), definition.getFieldAsList("Requiresamount"));
		if (this.kind == Kind.STASIS) {
			this.armTime = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
			this.triggerRadius = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
			this.stunRadius = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
			this.stunDuration = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
			if (CTargetType.parseTargetTypeSet(definition.getField(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
				getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getField(AbilityFields.TARGETS_ALLOWED)));
			}
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
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		for (int index = 0; index < this.count; index++) {
			final CUnit ward = game.createUnitSimple(this.unitId, caster.getPlayerIndex(), target.getX(), target.getY(),
					caster.getFacing());
			if (ward == null) continue;
			ward.addClassification(CUnitClassification.SUMMONED);
			if (this.kind == Kind.SENTRY) activateSentry(game, ward);
			final int handle = game.getHandleIdAllocator().createId();
			if (this.kind == Kind.STASIS) {
				ward.add(game, new StasisTrap(handle, this.buffId, getDuration(), this.armTime, this.triggerRadius,
						this.stunRadius, this.stunDuration, getHeroDuration(), getTargetsAllowed().clone()));
			}
			else {
				ward.add(game, new CBuffTimedLife(handle, this.buffId, getDuration(), false));
			}
			caster.fireSummonEvents(game, ward);
		}
		return false;
	}

	private static void activateSentry(final CSimulation game, final CUnit ward) {
		conceal(game, ward);
		final float sight = Math.max(ward.getSightRadius(game, true), ward.getSightRadius(game, false));
		final int detectionCells = (int) (sight / CPlayerFogOfWar.GRID_STEP) + 1;
		// Detector state stores its range in fog cells and its invisibility mask in the low byte.
		ward.addStateModBuff(new StateModBuff(StateModBuffType.DETECTOR, (detectionCells << 8) | 0xff));
		ward.computeUnitState(game, StateModBuffType.DETECTOR);
	}

	private static void conceal(final CSimulation game, final CUnit ward) {
		ward.addStateModBuff(new StateModBuff(StateModBuffType.INVISIBLE, 1));
		ward.computeUnitState(game, StateModBuffType.INVISIBLE);
	}

	private static final class StasisTrap extends CBuffTimedLife {
		private static final War3ID TIMED_LIFE = War3ID.fromString("BTLF");
		private final War3ID stunBuff;
		private final int armingTicks;
		private final float triggerRadius;
		private final float stunRadius;
		private final float stunDuration;
		private final float heroStunDuration;
		private final EnumSet<CTargetType> targetsAllowed;
		private int armedAt;
		private boolean armed;
		private boolean triggered;

		private StasisTrap(final int handle, final War3ID buffId, final float duration, final float armTime,
				final float triggerRadius, final float stunRadius, final float stunDuration, final float heroStunDuration,
				final EnumSet<CTargetType> targetsAllowed) {
			super(handle, TIMED_LIFE, duration, false);
			this.stunBuff = buffId;
			this.armingTicks = (int) Math.ceil(armTime / WarsmashConstants.SIMULATION_STEP_TIME);
			this.triggerRadius = triggerRadius;
			this.stunRadius = stunRadius;
			this.stunDuration = stunDuration;
			this.heroStunDuration = heroStunDuration;
			this.targetsAllowed = targetsAllowed;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit ward) {
			super.onBuffAdd(game, ward);
			this.armedAt = game.getGameTurnTick() + this.armingTicks;
		}

		@Override
		public void onTick(final CSimulation game, final CUnit ward) {
			super.onTick(game, ward);
			if (ward.isDead() || game.getGameTurnTick() < this.armedAt) return;
			if (!this.armed) {
				this.armed = true;
				conceal(game, ward);
			}
			game.getWorldCollision().enumUnitsInRange(ward.getX(), ward.getY(), this.triggerRadius, unit -> {
				this.triggered |= canStun(game, ward, unit);
				return this.triggered;
			});
			if (!this.triggered) return;

			final var affected = new ArrayList<CUnit>();
			game.getWorldCollision().enumUnitsInRange(ward.getX(), ward.getY(), this.stunRadius, unit -> {
				if (unit != ward && !unit.isDead()
						&& (unit.getFirstAbilityOfType(StasisTrap.class) != null || canStun(game, ward, unit))) affected.add(unit);
				return false;
			});
			for (final CUnit unit : affected) {
				if (unit.getFirstAbilityOfType(StasisTrap.class) != null) unit.kill(game);
				else if (!unit.isDead()) {
					unit.add(game, new CBuffStun(game.getHandleIdAllocator().createId(), this.stunBuff,
							unit.isHero() ? this.heroStunDuration : this.stunDuration));
				}
			}
			ward.kill(game);
		}

		private boolean canStun(final CSimulation game, final CUnit ward, final CUnit unit) {
			return unit != ward && !unit.isHidden() && !unit.isDead()
					&& unit.canBeTargetedBy(game, ward, false, this.targetsAllowed,
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset());
		}
	}
}
