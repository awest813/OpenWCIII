package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.combat;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public final class CAbilityLightningAttack extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Alit");
	private String missileArt;
	private int missileSpeed;
	private float graphicDelay;
	private float graphicDuration;

	public CAbilityLightningAttack(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.missileArt = data.getFieldAsString("Missileart", 0);
		this.missileSpeed = data.getFieldAsInteger(AbilityFields.PROJECTILE_SPEED, 0);
		this.graphicDelay = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.graphicDuration = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
	}

	public CUnitAttackMissile prepareProjectile(final CSimulation game, final CUnitAttackMissile attack,
			final AbilityTarget target) {
		final var weaponTargets = attack.getTargetsAllowed();
		if (isDisabled() || (!weaponTargets.contains(CTargetType.GROUND) && !weaponTargets.contains(CTargetType.AIR))) return attack;
		if (this.graphicDuration > 0 && target instanceof CUnit victim) {
			final float duration = this.graphicDuration;
			final CTimer impact = new CTimer() {
				@Override
				public void onFire(final CSimulation simulation) {
					if (!simulation.containsUnit(victim) || victim.isHidden()) return;
					final var effect = simulation.createPersistentSpellEffectOnUnit(victim, getAlias(), CEffectType.TARGET);
					final CTimer expiry = new CTimer() {
						@Override public void onFire(final CSimulation ignored) { effect.remove(); }
					};
					expiry.setTimeoutTime(duration);
					expiry.start(simulation);
				}
			};
			impact.setTimeoutTime(Math.max(0, this.graphicDelay));
			impact.start(game);
		}
		final boolean replaceArt = !this.missileArt.isBlank() && !"_".equals(this.missileArt)
				&& !this.missileArt.equalsIgnoreCase(attack.getProjectileArt());
		final boolean replaceSpeed = this.missileSpeed > 0 && this.missileSpeed != attack.getProjectileSpeed();
		if (!replaceArt && !replaceSpeed) return attack;
		final CUnitAttackMissile projectile = (CUnitAttackMissile) attack.copy();
		if (replaceArt) projectile.setProjectileArt(this.missileArt);
		if (replaceSpeed) projectile.setProjectileSpeed(this.missileSpeed);
		return projectile;
	}
}
