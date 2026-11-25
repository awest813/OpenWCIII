package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks;

import java.util.EnumSet;
import java.util.function.Predicate;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetWidgetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CWeaponType;

public class CUnitAttackMissileLine extends CUnitAttackMissileSplash {
	private float damageSpillDistance;
	private float damageSpillRadius;

	public CUnitAttackMissileLine(final float animationBackswingPoint, final float animationDamagePoint,
			final CAttackType attackType, final float cooldownTime, final int damageBase, final int damageDice,
			final int damageSidesPerDie, final int damageUpgradeAmount, final int range, final float rangeMotionBuffer,
			final boolean showUI, final EnumSet<CTargetType> targetsAllowed, final String weaponSound,
			final CWeaponType weaponType, final float projectileArc, final String projectileArt,
			final boolean projectileHomingEnabled, final int projectileSpeed, final float damageSpillDistance,
			final float damageSpillRadius, final int fullDamageArea, final int mediumDamageArea, final int smallDamageArea,
			final EnumSet<CTargetType> areaTargets, final float mediumDamageFactor, final float smallDamageFactor) {
		super(animationBackswingPoint, animationDamagePoint, attackType, cooldownTime, damageBase, damageDice,
				damageSidesPerDie, damageUpgradeAmount, range, rangeMotionBuffer, showUI, targetsAllowed, weaponSound,
				weaponType, projectileArc, projectileArt, projectileHomingEnabled, projectileSpeed, fullDamageArea,
				mediumDamageArea, smallDamageArea, areaTargets, mediumDamageFactor, smallDamageFactor);
		this.damageSpillDistance = damageSpillDistance;
		this.damageSpillRadius = damageSpillRadius;
	}

	@Override
	public CUnitAttack copy() {
		return new CUnitAttackMissileLine(getAnimationBackswingPoint(), getAnimationDamagePoint(), getAttackType(),
				getCooldownTime(), getDamageBase(), getDamageDice(), getDamageSidesPerDie(), getDamageUpgradeAmount(),
				getRange(), getRangeMotionBuffer(), isShowUI(), getTargetsAllowed(), getWeaponSound(), getWeaponType(),
				getProjectileArc(), getProjectileArt(), isProjectileHomingEnabled(), getProjectileSpeed(),
				this.damageSpillDistance, this.damageSpillRadius, getAreaOfEffectFullDamage(), getAreaOfEffectMediumDamage(),
				getAreaOfEffectSmallDamage(), getAreaOfEffectTargets(), getDamageFactorMedium(), getDamageFactorSmall());
	}

	@Override
	public void doProjectileDamage(final CSimulation game, final CUnit source, final AbilityTarget target,
			final float damage, final float x, final float y, final float launchX, final float launchY,
			final int bounceIndex, final CUnitAttackListener attackListener) {
		final CWidget primary = target.visit(AbilityTargetWidgetVisitor.INSTANCE);
		final float distance = this.damageSpillDistance + game.getUpgradeData().getAttackSpillDistanceBonus(
				source.getUnitType(), game.getPlayer(source.getPlayerIndex()));
		final double travel = Math.hypot(x - launchX, y - launchY);
		if (primary == null || distance <= 0 || travel == 0) {
			super.doProjectileDamage(game, source, target, damage, x, y, launchX, launchY, bounceIndex, attackListener);
			return;
		}
		final var line = new SpillLine(x, y, (x - launchX) / travel, (y - launchY) / travel, distance, this.damageSpillRadius);
		final float reach = distance + this.damageSpillRadius;
		final Predicate<CWidget> spill;
		if (getWeaponType() == CWeaponType.MLINE) {
			final CUnit next = firstUnitBehind(game, source, primary, line, reach);
			spill = widget -> widget == next;
		}
		else {
			spill = widget -> widget != primary && line.contains(widget)
					&& !(widget instanceof CDestructable destructable
							&& destructable.getDestType().getTargetedAs().contains(CTargetType.TREE));
		}
		doDamageWithSpill(game, source, target, damage, x, y, attackListener, reach, spill);
	}

	private CUnit firstUnitBehind(final CSimulation game, final CUnit source, final CWidget primary,
			final SpillLine line, final float reach) {
		final CUnit[] next = { null };
		game.getWorldCollision().enumUnitsInRange(line.x(), line.y(), reach, unit -> {
			if (unit != primary && line.contains(unit) && canDamageUnitInArea(game, source, unit)) {
				if (next[0] == null || line.along(unit) < line.along(next[0])
						|| line.along(unit) == line.along(next[0]) && unit.getHandleId() < next[0].getHandleId()) {
					next[0] = unit;
				}
			}
			return false;
		});
		return next[0];
	}

	private record SpillLine(float x, float y, double dx, double dy, float distance, float radius) {
		double along(final CWidget widget) {
			return (widget.getX() - this.x) * this.dx + (widget.getY() - this.y) * this.dy;
		}

		boolean contains(final CWidget widget) {
			final double along = along(widget);
			final double across = (widget.getX() - this.x) * this.dy - (widget.getY() - this.y) * this.dx;
			return along > 0 && along <= this.distance && Math.abs(across) <= this.radius;
		}
	}

	public float getDamageSpillDistance() {
		return this.damageSpillDistance;
	}

	public float getDamageSpillRadius() {
		return this.damageSpillRadius;
	}

	public void setDamageSpillDistance(final float damageSpillDistance) {
		this.damageSpillDistance = damageSpillDistance;
	}

	public void setDamageSpillRadius(final float damageSpillRadius) {
		this.damageSpillRadius = damageSpillRadius;
	}

}
