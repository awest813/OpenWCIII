package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks;

import java.util.EnumSet;
import java.util.function.Predicate;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructableEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.CAbilityFragmentationShards;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetWidgetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CWeaponType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListenerDamageModResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public class CUnitAttackMissileSplash extends CUnitAttackMissile {
	private int areaOfEffectFullDamage;
	private int areaOfEffectMediumDamage;
	private int areaOfEffectSmallDamage;
	private EnumSet<CTargetType> areaOfEffectTargets;
	private float damageFactorMedium;
	private float damageFactorSmall;

	public CUnitAttackMissileSplash(final float animationBackswingPoint, final float animationDamagePoint,
			final CAttackType attackType, final float cooldownTime, final int damageBase, final int damageDice,
			final int damageSidesPerDie, final int damageUpgradeAmount, final int range, final float rangeMotionBuffer,
			final boolean showUI, final EnumSet<CTargetType> targetsAllowed, final String weaponSound,
			final CWeaponType weaponType, final float projectileArc, final String projectileArt,
			final boolean projectileHomingEnabled, final int projectileSpeed, final int areaOfEffectFullDamage,
			final int areaOfEffectMediumDamage, final int areaOfEffectSmallDamage,
			final EnumSet<CTargetType> areaOfEffectTargets, final float damageFactorMedium,
			final float damageFactorSmall) {
		super(animationBackswingPoint, animationDamagePoint, attackType, cooldownTime, damageBase, damageDice,
				damageSidesPerDie, damageUpgradeAmount, range, rangeMotionBuffer, showUI, targetsAllowed, weaponSound,
				weaponType, projectileArc, projectileArt, projectileHomingEnabled, projectileSpeed);
		this.areaOfEffectFullDamage = areaOfEffectFullDamage;
		this.areaOfEffectMediumDamage = areaOfEffectMediumDamage;
		this.areaOfEffectSmallDamage = areaOfEffectSmallDamage;
		this.areaOfEffectTargets = areaOfEffectTargets;
		this.damageFactorMedium = damageFactorMedium;
		this.damageFactorSmall = damageFactorSmall;
	}

	@Override
	public CUnitAttack copy() {
		return new CUnitAttackMissileSplash(getAnimationBackswingPoint(), getAnimationDamagePoint(), getAttackType(),
				getCooldownTime(), getDamageBase(), getDamageDice(), getDamageSidesPerDie(), getDamageUpgradeAmount(),
				getRange(), getRangeMotionBuffer(), isShowUI(), getTargetsAllowed(), getWeaponSound(), getWeaponType(),
				getProjectileArc(), getProjectileArt(), isProjectileHomingEnabled(), getProjectileSpeed(),
				this.areaOfEffectFullDamage, this.areaOfEffectMediumDamage, this.areaOfEffectSmallDamage,
				this.areaOfEffectTargets, this.damageFactorMedium, this.damageFactorSmall);
	}

	@Override
	public int getRange() {
		return super.getRange();
	}

	public int getAreaOfEffectFullDamage() {
		return this.areaOfEffectFullDamage;
	}

	public int getAreaOfEffectMediumDamage() {
		return this.areaOfEffectMediumDamage;
	}

	public int getAreaOfEffectSmallDamage() {
		return this.areaOfEffectSmallDamage;
	}

	public EnumSet<CTargetType> getAreaOfEffectTargets() {
		return this.areaOfEffectTargets;
	}

	public float getDamageFactorMedium() {
		return this.damageFactorMedium;
	}

	public float getDamageFactorSmall() {
		return this.damageFactorSmall;
	}

	public void setAreaOfEffectFullDamage(final int areaOfEffectFullDamage) {
		this.areaOfEffectFullDamage = areaOfEffectFullDamage;
	}

	public void setAreaOfEffectMediumDamage(final int areaOfEffectMediumDamage) {
		this.areaOfEffectMediumDamage = areaOfEffectMediumDamage;
	}

	public void setAreaOfEffectSmallDamage(final int areaOfEffectSmallDamage) {
		this.areaOfEffectSmallDamage = areaOfEffectSmallDamage;
	}

	public void setAreaOfEffectTargets(final EnumSet<CTargetType> areaOfEffectTargets) {
		this.areaOfEffectTargets = areaOfEffectTargets;
	}

	public void setDamageFactorMedium(final float damageFactorMedium) {
		this.damageFactorMedium = damageFactorMedium;
	}

	public void setDamageFactorSmall(final float damageFactorSmall) {
		this.damageFactorSmall = damageFactorSmall;
	}

	@Override
	public void doDamage(final CSimulation cSimulation, final CUnit source, final AbilityTarget target,
			final float damage, final float x, final float y, final int bounceIndex,
			final CUnitAttackListener attackListener) {
		doDamageWithSpill(cSimulation, source, target, damage, x, y, attackListener, 0, null);
	}

	protected void doDamageWithSpill(final CSimulation game, final CUnit source, final AbilityTarget target,
			final float damage, final float x, final float y, final CUnitAttackListener attackListener,
			final float spillReach, final Predicate<CWidget> spill) {
		final var consumer = new SplashDamageConsumer();
		consumer.doDamage(game, source, target, this, x, y, damage, attackListener, spillReach, spill);
		if (!consumer.hitTarget) {
			final CWidget widget = target.visit(AbilityTargetWidgetVisitor.INSTANCE);
			if (widget != null) {
				final float actualDamage = consumer.damageTarget(widget);
				if (actualDamage > 0) runPostDamageListeners(game, source, widget, actualDamage);
			}
		}
	}

	protected boolean canDamageUnitInArea(final CSimulation game, final CUnit source, final CUnit unit) {
		return unit != source && unit.canBeAttackedBy(game, source, false, this.areaOfEffectTargets,
				BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset());
	}

	private static final class SplashDamageConsumer implements CUnitEnumFunction, CDestructableEnumFunction {
		private final Rectangle rect = new Rectangle();
		private CUnitAttackMissileSplash attack;
		private CSimulation simulation;
		private CUnit source;
		private AbilityTarget target;
		private float x;
		private float y;
		private float damage;
		private float bonusDamage;
		private CUnitAttackListener attackListener;
		private CAbilityFragmentationShards fragmentation;
		private boolean hitTarget;
		private Predicate<CWidget> spill;

		public void doDamage(final CSimulation simulation, final CUnit source, final AbilityTarget target,
				final CUnitAttackMissileSplash attack, final float x, final float y, final float damage,
				final CUnitAttackListener attackListener, final float spillReach, final Predicate<CWidget> spill) {
			this.simulation = simulation;
			this.source = source;
			this.target = target;
			this.attack = attack;
			this.x = x;
			this.y = y;
			this.damage = damage;
			this.attackListener = attackListener;
			this.hitTarget = false;
			this.spill = spill;
			CUnitAttackPreDamageListenerDamageModResult result = this.attack.runPreDamageListeners(simulation, this.source, this.target, this.damage);
			this.damage = result.computeFinalDamage();
			this.bonusDamage = result.getBonusDamage();
			final var upgrade = source.getFirstAbilityOfType(CAbilityFragmentationShards.class);
			if (upgrade != null && !upgrade.isDisabled() && !result.isMiss()) this.fragmentation = upgrade;
			final float fragmentRadius = this.fragmentation == null ? 0 : this.fragmentation.getMaximumRadius();
			final float maxArea = Math.max(Math.max(spillReach, fragmentRadius), Math.max(attack.areaOfEffectFullDamage,
					Math.max(attack.areaOfEffectMediumDamage, attack.areaOfEffectSmallDamage)));
			this.rect.set(x - maxArea, y - maxArea, maxArea * 2, maxArea * 2);
			simulation.getWorldCollision().enumUnitsInRect(this.rect, this);
			simulation.getWorldCollision().enumDestructablesInRect(this.rect, this);
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.attack.canDamageUnitInArea(this.simulation, this.source, enumUnit)) {
				float damageDealt = damageTarget(enumUnit);
				if (enumUnit == this.target) {
					this.hitTarget = true;
					this.attack.runPostDamageListeners(simulation, this.source, target, damageDealt);
				}
				else if (damageDealt > 0) {
					for (final var listener : this.source.getPostDamageListeners()) {
						listener.onSplashHit(this.simulation, this.source, enumUnit, damageDealt);
					}
				}
			}
			return false;
		}

		@Override
		public boolean call(final CDestructable enumDestructable) {
			if (enumDestructable.canBeTargetedBy(this.simulation, this.source, this.attack.areaOfEffectTargets)) {
				float damageDealt = damageTarget(enumDestructable);
				if (enumDestructable == this.target) {
					this.hitTarget = true;
					this.attack.runPostDamageListeners(simulation, this.source, target, damageDealt);
				}
			}
			return false;
		}

		public float damageTarget(final CWidget victim) {
			final double distance = victim.distance(this.x, this.y);
			final float fragments = this.fragmentation != null && victim instanceof CUnit unit
					? this.fragmentation.getBonus(this.simulation, this.source, unit, distance) : 0;
			final float factor;
			if ((this.spill != null && this.spill.test(victim)) || distance <= this.attack.areaOfEffectFullDamage) factor = 1;
			else if (distance <= this.attack.areaOfEffectMediumDamage) factor = this.attack.damageFactorMedium;
			else if (distance <= this.attack.areaOfEffectSmallDamage) factor = this.attack.damageFactorSmall;
			else if (fragments > 0) factor = 0;
			else return 0;
			final float damageDealt = victim.damage(this.simulation, this.source, true, true, this.attack.getAttackType(),
					this.attack.getWeaponType().getDamageType(), this.attack.getWeaponSound(), this.damage * factor,
					this.bonusDamage * factor + fragments);
			this.attackListener.onHit(victim, this.damage + this.bonusDamage + fragments);
			return damageDealt;
		}
	}
}
