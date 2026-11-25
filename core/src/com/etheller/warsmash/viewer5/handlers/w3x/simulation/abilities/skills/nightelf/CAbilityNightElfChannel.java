package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.ArrayList;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

/** Pulses around the caster while Starfall or Tranquility remains channeled. */
public final class CAbilityNightElfChannel extends CAbilityNoTargetSpellBase {
	private static final String STARFALL = "Abilities\\Spells\\NightElf\\Starfall\\";
	private static final String TRANQUILITY = "Abilities\\Spells\\NightElf\\Tranquility\\";
	private final boolean tranquility;
	private float radius;
	private float pulseAmount;
	private float buildingMultiplier;
	private int pulseTicks;
	private int nextPulse;
	private int finishTick;
	private SimulationRenderComponent channelArt;

	public CAbilityNightElfChannel(final int handleId, final War3ID alias, final boolean tranquility) {
		super(handleId, alias);
		this.tranquility = tranquility;
	}

	@Override
	public int getBaseOrderId() {
		return this.tranquility ? OrderIds.tranquility : OrderIds.starfall;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.pulseAmount = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		final float interval = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.pulseTicks = Math.max(1, Math.round(interval / WarsmashConstants.SIMULATION_STEP_TIME));
		this.buildingMultiplier = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			final var targets = CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED));
			targets.addAll(getTargetsAllowed());
			setTargetsAllowed(targets);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		this.nextPulse = tick + this.pulseTicks;
		this.finishTick = tick + Math.round(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
		this.channelArt = game.createPersistentEffectOnUnit(caster,
				this.tranquility ? TRANQUILITY + "Tranquility.mdl" : STARFALL + "StarfallCaster.mdl");
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		if (tick >= this.nextPulse && this.nextPulse <= this.finishTick) {
			this.nextPulse += this.pulseTicks;
			final var recipients = new ArrayList<CUnit>();
			game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
				if (!unit.isDead() && !unit.isHidden() && (this.tranquility || unit != caster)
						&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
								BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
					recipients.add(unit);
				}
				return false;
			});
			for (final CUnit unit : recipients) {
				if (this.tranquility) {
					if (unit.getLife() < unit.getMaximumLife()) {
						unit.heal(game, this.pulseAmount);
						game.createEffectOnUnit(unit, TRANQUILITY + "TranquilityTarget.mdl");
					}
				}
				else {
					game.createEffectOnUnit(unit, STARFALL + "StarfallTarget.mdl");
					unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null,
							this.pulseAmount * (unit.isBuilding() ? this.buildingMultiplier : 1));
				}
			}
		}
		return tick < this.finishTick;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target,
			final boolean interrupted) {
		if (this.channelArt != null) {
			this.channelArt.remove();
			this.channelArt = null;
		}
	}
}
