package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import java.util.ArrayList;
import java.util.Comparator;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityResearchIndicator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityBarrage extends CAbilityResearchIndicator {
	public static final War3ID CODE = War3ID.fromString("Aroc");
	private static final War3ID RESEARCH = War3ID.fromString("Rhrt");
	private float rocketDamage;
	private float damageLimit;
	private int targetLimit;
	private int cooldownTicks;
	private int nextVolley;
	private float projectileSpeed;
	private boolean homing;

	public CAbilityBarrage(final int handleId, final War3ID alias) {
		super(handleId, alias, CODE, RESEARCH);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		super.populateData(data, level);
		this.rocketDamage = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damageLimit = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.targetLimit = data.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		this.cooldownTicks = Math.round(data.getFieldAsFloat(AbilityFields.COOLDOWN + level, 0)
				/ WarsmashConstants.SIMULATION_STEP_TIME);
		this.projectileSpeed = data.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		this.homing = data.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	public void launchExtraRockets(final CSimulation game, final CUnit launcher, final AbilityTarget target,
			final CAttackType attackType) {
		if (isDisabled() || !(target instanceof CUnit primary) || this.rocketDamage <= 0 || this.targetLimit < 2
				|| game.getGameTurnTick() < this.nextVolley) return;
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		if (!primary.canBeTargetedBy(game, launcher, false, getTargetsAllowed(), receiver)) return;
		final float radius = getAreaOfEffect();
		final var candidates = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(launcher.getX(), launcher.getY(), radius, unit -> {
			if (unit != primary && unit != launcher && !unit.isDead() && !unit.isHidden()
					&& launcher.distanceSquaredNoCollision(unit) <= radius * radius
					&& unit.canBeTargetedBy(game, launcher, true, getTargetsAllowed(), receiver.reset())) {
				candidates.add(unit);
			}
			return false;
		});
		if (candidates.isEmpty()) return;
		candidates.sort(Comparator.comparingDouble((CUnit unit) -> launcher.distanceSquaredNoCollision(unit))
				.thenComparingInt(CUnit::getHandleId));
		this.nextVolley = game.getGameTurnTick() + this.cooldownTicks;
		float remainingDamage = this.damageLimit > 0 ? this.damageLimit : Float.POSITIVE_INFINITY;
		final int rockets = Math.min(candidates.size(), this.targetLimit - 1);
		for (int i = 0; i < rockets && remainingDamage > 0; i++) {
			final CUnit candidate = candidates.get(i);
			final float damage = Math.min(this.rocketDamage, remainingDamage);
			remainingDamage -= damage;
			if (this.projectileSpeed > 0) {
				game.createProjectile(launcher, getAlias(), launcher.getX(), launcher.getY(), (float) launcher.angleTo(candidate),
						this.projectileSpeed, this.homing, candidate, new Rocket(launcher, attackType, damage));
			}
			else candidate.damage(game, launcher, true, true, attackType, CDamageType.NORMAL, null, damage);
		}
	}

	private record Rocket(CUnit launcher, CAttackType attackType, float damage) implements CAbilityProjectileListener {
		@Override
		public void onLaunch(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
		}

		@Override
		public void onHit(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
			if (target instanceof CUnit unit && !unit.isHidden()
					&& unit.checkForAbilityProjReaction(game, this.launcher, projectile)) {
				unit.damage(game, this.launcher, true, true, this.attackType, CDamageType.NORMAL, null, this.damage);
			}
		}
	}
}
