package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;

public final class CAbilitySiphonMana extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BEAM = War3ID.fromString("DRAM");
	private float manaPerPulse;
	private float leashRange;
	private int pulseInterval;
	private int nextPulseTick;
	private int endTick;
	private War3ID beamId;
	private SimulationRenderComponentLightning beam;

	public CAbilitySiphonMana(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.drain;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.manaPerPulse = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.pulseInterval = Math.max(1, toTicks(data.getFieldAsFloat(AbilityFields.DATA_C + level, 0)));
		final float leash = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.leashRange = Math.max(leash, getCastRange());
		final War3ID authoredBeam = AbstractCAbilityTypeDefinition.getLightningId(data, level);
		this.beamId = War3ID.NONE.equals(authoredBeam) ? DEFAULT_BEAM : authoredBeam;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && unit.getMaximumMana() > 0) receiver.targetOk(target);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_MANA);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CUnit unit) || !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		final int tick = game.getGameTurnTick();
		this.nextPulseTick = tick + this.pulseInterval;
		this.endTick = tick + toTicks(getDurationForTarget(unit));
		this.beam = game.createLightning(unit, this.beamId, caster);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = (CUnit) target;
		if (unit.isDead() || unit.isHidden() || !caster.canReach(unit, this.leashRange) || unit.getMana() <= 0) return false;
		final int tick = game.getGameTurnTick();
		if (tick >= this.nextPulseTick && tick <= this.endTick) {
			final float drained = Math.min(this.manaPerPulse, unit.getMana());
			unit.setMana(unit.getMana() - drained);
			caster.restoreMana(game, drained);
			this.nextPulseTick += this.pulseInterval;
		}
		return tick < this.endTick && unit.getMana() > 0;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target,
			final boolean interrupted) {
		if (this.beam != null) {
			this.beam.remove();
			this.beam = null;
		}
	}

	private static int toTicks(final float seconds) {
		return (int) StrictMath.ceil(seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}
}
