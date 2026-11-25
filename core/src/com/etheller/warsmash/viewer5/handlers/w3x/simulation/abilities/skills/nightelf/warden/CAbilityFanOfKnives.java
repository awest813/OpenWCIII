package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.warden;

import java.util.ArrayList;
import java.util.Comparator;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public final class CAbilityFanOfKnives extends CAbilityNoTargetSpellBase {
	private float damage;
	private float maximumDamage;
	private float radius;
	private int maximumTargets;
	private float projectileSpeed;
	private boolean homing;

	public CAbilityFanOfKnives(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.fanofknives;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.damage = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.maximumDamage = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.maximumTargets = data.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.projectileSpeed = data.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		this.homing = data.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		if (data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).stream().allMatch(String::isBlank)) {
			getTargetsAllowed().add(CTargetType.AIR);
			getTargetsAllowed().add(CTargetType.GROUND);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final var owner = game.getPlayer(caster.getPlayerIndex());
		final var targets = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
			if (!unit.isUnitAlly(owner) && unit.canBeTargetedBy(game, caster, getTargetsAllowed())) targets.add(unit);
			return false;
		});
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.EFFECT);
		if (targets.isEmpty()) return false;
		if (this.maximumTargets > 0 && targets.size() > this.maximumTargets) {
			targets.sort(Comparator.<CUnit>comparingDouble(caster::distanceSquaredNoCollision).thenComparingInt(CUnit::getHandleId));
			targets.subList(this.maximumTargets, targets.size()).clear();
		}
		final float damagePerTarget = this.maximumDamage > 0 ? Math.min(this.damage, this.maximumDamage / targets.size())
				: this.damage;
		for (final CUnit victim : targets) {
			if (this.projectileSpeed <= 0) {
				damageTarget(game, caster, victim, damagePerTarget);
				continue;
			}
			game.createProjectile(caster, getAlias(), caster.getX(), caster.getY(), (float) caster.angleTo(victim),
					this.projectileSpeed, this.homing, victim, new CAbilityProjectileListener() {
						@Override public void onLaunch(final CSimulation simulation, final CProjectile projectile,
								final AbilityTarget launchedAt) {
						}

						@Override public void onHit(final CSimulation simulation, final CProjectile projectile,
								final AbilityTarget hit) {
							if (victim.checkForAbilityProjReaction(simulation, caster, projectile)) {
								damageTarget(simulation, caster, victim, damagePerTarget);
							}
						}
					});
		}
		return false;
	}

	private void damageTarget(final CSimulation game, final CUnit caster, final CUnit target, final float dealt) {
		if (!target.isDead() && target.checkForAbilityEffectReaction(game, caster, this)) {
			target.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, dealt);
		}
	}
}
