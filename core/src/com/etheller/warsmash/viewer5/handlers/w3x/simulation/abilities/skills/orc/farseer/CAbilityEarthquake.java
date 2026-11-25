package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public final class CAbilityEarthquake extends CAbilityPointTargetSpellBase {
	private static final War3ID DEFAULT_EFFECT = War3ID.fromString("XOeq");
	private static final War3ID DEFAULT_SLOW = War3ID.fromString("BOeq");
	private final Map<CUnit, EarthquakeSlow> affected = new LinkedHashMap<>();
	private float area;
	private float finalArea;
	private float effectDelay;
	private float damagePerSecond;
	private float slowFraction;
	private int effectTick;
	private int damageTick;
	private int endTick;
	private War3ID effectId;
	private War3ID slowId;
	private SimulationRenderComponent effect;

	public CAbilityEarthquake(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.earthquake; }
	@Override public float getUIAreaOfEffect() { return this.area; }

	@Override
	public void populateData(final GameObject data, final int level) {
		this.area = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		final float configuredFinalArea = data.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.finalArea = configuredFinalArea > 0 ? configuredFinalArea : this.area;
		this.effectDelay = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damagePerSecond = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.slowFraction = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		final War3ID authoredEffect = AbstractCAbilityTypeDefinition.getEffectId(data, level);
		final War3ID authoredSlow = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.effectId = War3ID.NONE.equals(authoredEffect) ? DEFAULT_EFFECT : authoredEffect;
		this.slowId = War3ID.NONE.equals(authoredSlow) ? DEFAULT_SLOW : authoredSlow;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		this.effectTick = tick + secondsToTicks(this.effectDelay);
		this.damageTick = this.effectTick;
		this.endTick = tick + secondsToTicks(getDuration());
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		if (tick >= this.endTick) return false;
		if (tick < this.effectTick) return true;
		if (this.effect == null) {
			this.effect = game.spawnSpellEffectOnPoint(target.getX(), target.getY(), 0, this.effectId, CEffectType.EFFECT, 0);
			game.unitLoopSoundEffectEvent(caster, this.effectId);
		}
		final float progress = (float) (tick - this.effectTick) / (this.endTick - this.effectTick);
		final float radius = this.area + (this.finalArea - this.area) * progress;
		final boolean damage = tick >= this.damageTick;
		final Set<CUnit> groundUnits = new LinkedHashSet<>();
		final List<CUnit> structures = new ArrayList<>();
		game.getWorldCollision().enumUnitsInRange(target.getX(), target.getY(), radius, unit -> {
			if (!unit.isDead() && !unit.isHidden() && unit.canBeTargetedBy(game, caster, false,
					getTargetsAllowed(), BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				if (unit.isBuilding()) {
					if (damage) structures.add(unit);
				}
				else groundUnits.add(unit);
			}
			return false;
		});
		this.affected.entrySet().removeIf(entry -> {
			if (groundUnits.contains(entry.getKey())) return false;
			entry.getKey().remove(game, entry.getValue());
			return true;
		});
		for (final CUnit unit : groundUnits) {
			if (!this.affected.containsKey(unit)) {
				final float speed = unit.getSpeed();
				final float slowed = Math.max(game.getGameplayConstants().getMinUnitSpeed(), speed * (1f - this.slowFraction));
				if (slowed >= speed) continue;
				final var buff = new EarthquakeSlow(game.getHandleIdAllocator().createId(), this.slowId,
						getDuration(), slowed - speed);
				this.affected.put(unit, buff);
				unit.add(game, buff);
			}
		}
		if (damage) {
			this.damageTick += secondsToTicks(1f);
			for (final CUnit structure : structures) {
				structure.damage(game, caster, false, false, CAttackType.SPELLS, CDamageType.DEMOLITION,
						CWeaponSoundTypeJass.WHOKNOWS.name(), this.damagePerSecond);
			}
			final List<CDestructable> debris = new ArrayList<>();
			game.getWorldCollision().enumDestructablesInRange(target.getX(), target.getY(), radius, destructable -> {
				if (!destructable.isDead() && destructable.canBeTargetedBy(game, caster, getTargetsAllowed())) {
					debris.add(destructable);
				}
				return false;
			});
			for (final CDestructable destructable : debris) {
				destructable.damage(game, caster, false, false, CAttackType.SPELLS, CDamageType.DEMOLITION,
						CWeaponSoundTypeJass.WHOKNOWS.name(), this.damagePerSecond);
			}
		}
		return true;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target,
			final boolean interrupted) {
		for (final var entry : this.affected.entrySet()) {
			entry.getKey().remove(game, entry.getValue());
		}
		this.affected.clear();
		if (this.effect != null) {
			this.effect.remove();
			this.effect = null;
			game.unitStopSoundEffectEvent(caster, this.effectId);
		}
	}

	private static int secondsToTicks(final float seconds) {
		return (int) StrictMath.ceil(seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	private static final class EarthquakeSlow extends CBuffTimed {
		private final NonStackingStatBuff slow;

		private EarthquakeSlow(final int handleId, final War3ID alias, final float duration, final float amount) {
			super(handleId, DEFAULT_SLOW, alias, duration);
			this.slow = new NonStackingStatBuff(NonStackingStatBuffType.MVSPD, "earthquake", amount);
		}

		@Override protected void onBuffAdd(final CSimulation game, final CUnit unit) { unit.addNonStackingStatBuff(this.slow); }
		@Override protected void onBuffRemove(final CSimulation game, final CUnit unit) { unit.removeNonStackingStatBuff(this.slow); }
		@Override public boolean isTimedLifeBar() { return false; }
	}
}
