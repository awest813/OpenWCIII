package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CUnitVisionFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

/** Armor reduction and shared sight for the Druid and Storm Crow variants. */
public final class CAbilityFaerieFire extends CAbilityTargetSpellBase implements CAutocastAbility {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bfae");
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID buffId;
	private float armorReduction;
	private boolean autocast;

	public CAbilityFaerieFire(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.faeriefire;
	}

	@Override
	public int getAutoCastOnOrderId() {
		return OrderIds.faeriefireon;
	}

	@Override
	public int getAutoCastOffOrderId() {
		return OrderIds.faeriefireoff;
	}

	@Override
	public boolean isAutoCastOn() {
		return this.autocast;
	}

	@Override
	public AutocastType getAutocastType() {
		return AutocastType.NEARESTENEMY;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.armorReduction = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		final String authoredBuff = definition.getField(AbilityFields.BUFF + level);
		this.buffId = authoredBuff.length() == 4 ? War3ID.fromString(authoredBuff) : DEFAULT_BUFF;
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
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
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		setAutoCastOn(unit, true);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setAutoCastOn(unit, false);
		super.onRemove(game, unit);
	}

	@Override
	public void setAutoCastOn(final CUnit unit, final boolean enabled) {
		if (this.autocast == enabled) return;
		this.autocast = enabled;
		unit.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void setAutoCastOff() {
		this.autocast = false;
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && unit.isVisible(game, caster.getPlayerIndex())
				&& unit.getFirstAbilityOfType(FaerieFireBuff.class) == null) {
			checkCanTarget(game, caster, orderId, target, receiver);
		}
		else receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit == null || !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		final CPlayer owner = game.getPlayer(caster.getPlayerIndex());
		for (final var ability : unit.getAbilities()) {
			if (ability instanceof FaerieFireBuff previous && previous.owner == owner) {
				unit.remove(game, previous);
				break;
			}
		}
		unit.add(game, new FaerieFireBuff(game.getHandleIdAllocator().createId(), this.buffId,
				getDurationForTarget(unit), this.armorReduction, owner, unit));
		return false;
	}

	private static final class FaerieFireBuff extends CBuffTimed {
		private final CPlayer owner;
		private final NonStackingStatBuff armor;
		private final CUnitVisionFogModifier sharedSight;
		private final StateModBuff detection;

		FaerieFireBuff(final int handleId, final War3ID alias, final float duration, final float armorReduction,
				final CPlayer owner, final CUnit target) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.owner = owner;
			this.armor = new NonStackingStatBuff(NonStackingStatBuffType.DEF, "faerieFire", -armorReduction);
			this.sharedSight = new CUnitVisionFogModifier(target, true);
			this.detection = new StateModBuff(StateModBuffType.DETECTED, 127L | (1L << (owner.getId() + 8)));
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.armor);
			this.owner.addFogModifer(game, this.sharedSight, false);
			unit.addStateModBuff(this.detection);
			unit.computeUnitState(game, StateModBuffType.DETECTED);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.armor);
			this.owner.removeFogModifer(game, this.sharedSight);
			unit.removeStateModBuff(this.detection);
			unit.computeUnitState(game, StateModBuffType.DETECTED);
		}

		@Override public boolean isDispellable() { return true; }
		@Override public DispelPolarity getDispelPolarity() { return DispelPolarity.HARMFUL; }
		@Override public boolean isTimedLifeBar() { return false; }
	}
}
