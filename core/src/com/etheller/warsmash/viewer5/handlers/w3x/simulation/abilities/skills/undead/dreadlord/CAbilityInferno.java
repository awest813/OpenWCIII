package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.dreadlord;

import java.util.ArrayList;
import java.util.EnumSet;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

/** A delayed impact followed by a player-controlled Infernal. */
public final class CAbilityInferno extends CAbilityPointTargetSpellBase {
	private static final EnumSet<CTargetType> TREE_TARGET = EnumSet.of(CTargetType.TREE);
	private War3ID summonedType;
	private War3ID stunBuff;
	private float radius;
	private float damage;
	private float summonDuration;
	private float impactDelay;

	public CAbilityInferno(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.inferno;
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.summonedType = ability.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
		this.stunBuff = AbstractCAbilityTypeDefinition.getBuffId(ability, level);
		this.radius = ability.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.damage = ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.summonDuration = ability.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.impactDelay = ability.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			final var allowed = CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED));
			allowed.addAll(getTargetsAllowed());
			setTargetsAllowed(allowed);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final float x = target.getX();
		final float y = target.getY();
		game.spawnTemporarySpellEffectOnPoint(x, y, 0, getAlias(), CEffectType.EFFECT, 0);
		if (this.impactDelay > 0) {
			final CTimer impactTimer = new CTimer() {
				@Override
				public void onFire(final CSimulation simulation) {
					land(simulation, caster, x, y);
				}
			};
			impactTimer.setTimeoutTime(this.impactDelay);
			impactTimer.start(game);
		}
		else {
			land(game, caster, x, y);
		}
		return false;
	}

	private void land(final CSimulation game, final CUnit caster, final float x, final float y) {
		final var affectedUnits = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(x, y, this.radius, unit -> {
			if (!unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& !unit.isUnitAlly(game.getPlayer(caster.getPlayerIndex()))
					&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				affectedUnits.add(unit);
			}
			return false;
		});
		for (final CUnit unit : affectedUnits) {
			unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.UNIVERSAL, null, this.damage);
			if (!unit.isDead() && !unit.isBuilding()) {
				unit.add(game, new CBuffStun(game.getHandleIdAllocator().createId(), this.stunBuff,
						getDurationForTarget(unit)));
			}
		}
		final var scenery = new ArrayList<CDestructable>();
		game.getWorldCollision().enumDestructablesInRange(x, y, this.radius, destructable -> {
			if (!destructable.isDead() && !destructable.isInvulnerable()
					&& (destructable.canBeTargetedBy(game, caster, TREE_TARGET)
							|| destructable.canBeTargetedBy(game, caster, getTargetsAllowed()))) {
				scenery.add(destructable);
			}
			return false;
		});
		for (final CDestructable obstacle : scenery) {
			if (obstacle.canBeTargetedBy(game, caster, TREE_TARGET)) obstacle.setLife(game, 0);
			else obstacle.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.UNIVERSAL, null, this.damage);
		}
		if (this.summonedType == null || this.summonedType.equals(War3ID.NONE)
				|| game.getUnitData().getUnitType(this.summonedType) == null) return;
		final CUnit summoned = game.createUnitSimple(this.summonedType, caster.getPlayerIndex(), x, y, caster.getFacing());
		summoned.setPointAndCheckUnstuck(x, y, game);
		summoned.addClassification(CUnitClassification.SUMMONED);
		summoned.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), War3ID.NONE,
				this.summonDuration, false));
		caster.fireSummonEvents(game, summoned);
	}
}
