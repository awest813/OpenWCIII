package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public final class CAbilityDeathAndDecay extends CAbilityPointTargetSpellBase {
	private static final War3ID DEFAULT_AREA_EFFECT = War3ID.fromString("XUdd");
	private static final War3ID DEFAULT_IMPACT_EFFECT = War3ID.fromString("BUdd");
	private static final int TICKS_PER_SECOND = (int) StrictMath.ceil(1f / WarsmashConstants.SIMULATION_STEP_TIME);
	private float radius;
	private float lifeFraction;
	private float buildingFactor;
	private int nextPulseTick;
	private int endTick;
	private War3ID areaEffectId;
	private War3ID impactEffectId;
	private SimulationRenderComponent areaEffect;

	public CAbilityDeathAndDecay(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.deathanddecay; }
	@Override public float getUIAreaOfEffect() { return this.radius; }

	@Override
	public void populateData(final GameObject data, final int level) {
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.lifeFraction = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		final String buildingMultiplier = data.getField(AbilityFields.DATA_B + level);
		this.buildingFactor = buildingMultiplier == null || buildingMultiplier.isBlank() || "-".equals(buildingMultiplier)
				? 1f : data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		final War3ID area = AbstractCAbilityTypeDefinition.getEffectId(data, level);
		final War3ID impact = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.areaEffectId = War3ID.NONE.equals(area) ? DEFAULT_AREA_EFFECT : area;
		this.impactEffectId = War3ID.NONE.equals(impact) ? DEFAULT_IMPACT_EFFECT : impact;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
			getTargetsAllowed().add(CTargetType.TREE);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		this.nextPulseTick = tick + TICKS_PER_SECOND;
		this.endTick = tick + (int) StrictMath.ceil(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
		this.areaEffect = game.spawnSpellEffectOnPoint(target.getX(), target.getY(), 0,
				this.areaEffectId, CEffectType.EFFECT, 0);
		game.unitLoopSoundEffectEvent(caster, this.areaEffectId);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		if (tick < this.nextPulseTick) return tick < this.endTick;
		this.nextPulseTick += TICKS_PER_SECOND;
		final List<CUnit> units = new ArrayList<>();
		game.getWorldCollision().enumUnitsInRange(target.getX(), target.getY(), this.radius, unit -> {
			if (!unit.isDead() && !unit.isHidden() && unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) units.add(unit);
			return false;
		});
		for (final CUnit unit : units) {
			final float strength = unit.isBuilding() ? this.buildingFactor : 1f;
			game.createTemporarySpellEffectOnUnit(unit, this.impactEffectId, CEffectType.TARGET);
			inflict(game, caster, unit, unit.getMaximumLife() * strength);
		}
		final List<CDestructable> scenery = new ArrayList<>();
		game.getWorldCollision().enumDestructablesInRange(target.getX(), target.getY(), this.radius, destructable -> {
			if (!destructable.isDead() && destructable.canBeTargetedBy(game, caster, getTargetsAllowed())) {
				scenery.add(destructable);
			}
			return false;
		});
		for (final CDestructable destructable : scenery) {
			inflict(game, caster, destructable, destructable.getMaxLife());
		}
		return tick < this.endTick;
	}

	private void inflict(final CSimulation game, final CUnit caster, final CWidget target, final float life) {
		target.damage(game, caster, false, false, CAttackType.SPELLS, CDamageType.UNIVERSAL,
				CWeaponSoundTypeJass.WHOKNOWS.name(), life * this.lifeFraction);
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target,
			final boolean interrupted) {
		if (this.areaEffect != null) {
			this.areaEffect.remove();
			this.areaEffect = null;
		}
		game.unitStopSoundEffectEvent(caster, this.areaEffectId);
	}
}
