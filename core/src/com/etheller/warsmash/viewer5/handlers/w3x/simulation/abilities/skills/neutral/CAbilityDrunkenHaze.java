package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackEffectListenerStacking;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListenerPriority;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityDrunkenHaze extends CAbilityTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("ANdh");
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BNdh");
	private float radius;
	private float missChance;
	private float movementSlow;
	private float attackSlow;
	private float projectileSpeed;
	private boolean homing;
	private War3ID buffId;

	public CAbilityDrunkenHaze(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.drunkenhaze; }

	@Override
	public void populateData(final GameObject data, final int level) {
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.missChance = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.movementSlow = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.attackSlow = data.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.projectileSpeed = data.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		this.homing = data.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.buffId = War3ID.NONE.equals(buff) ? DEFAULT_BUFF : buff;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.projectileSpeed > 0) {
			game.createProjectile(caster, getAlias(), caster.getX(), caster.getY(), (float) caster.angleTo(target),
					this.projectileSpeed, this.homing, target, new CAbilityProjectileListener() {
						@Override public void onLaunch(final CSimulation game, final CProjectile projectile, final AbilityTarget target) { }

						@Override
						public void onHit(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
							applyHaze(game, caster, target.getX(), target.getY());
						}
					});
		}
		else applyHaze(game, caster, target.getX(), target.getY());
		return false;
	}

	private void applyHaze(final CSimulation game, final CUnit caster, final float x, final float y) {
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		game.getWorldCollision().enumUnitsInRange(x, y, this.radius, unit -> {
			if (!unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(), receiver.reset())
					&& unit.checkForAbilityEffectReaction(game, caster, this)) {
				final Haze previous = unit.getFirstAbilityOfType(Haze.class);
				if (previous != null) unit.remove(game, previous);
				unit.add(game, new Haze(game.getHandleIdAllocator().createId(), this.buffId, getDurationForTarget(unit),
						this.missChance, this.movementSlow, this.attackSlow));
			}
			return false;
		});
	}

	static boolean hasHaze(final CUnit unit) {
		return unit.getFirstAbilityOfType(Haze.class) != null;
	}

	private static final class Haze extends CBuffTimed {
		private final NonStackingStatBuff movement;
		private final NonStackingStatBuff attack;
		private final CUnitAttackPreDamageListener accuracy;

		private Haze(final int handleId, final War3ID alias, final float duration, final float miss,
				final float movementSlow, final float attackSlow) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "drunkenhaze", -movementSlow);
			this.attack = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "drunkenhaze", -attackSlow);
			this.accuracy = (game, attacker, target, weapon, attackType, damageType, result) -> {
				if (miss > 0 && game.getSeededRandom().nextFloat() < miss) {
					result.setMiss(true);
					return new CUnitAttackEffectListenerStacking(false, true);
				}
				return new CUnitAttackEffectListenerStacking();
			};
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.movement);
			unit.addNonStackingStatBuff(this.attack);
			unit.addPreDamageListener(CUnitAttackPreDamageListenerPriority.ACCURACY, this.accuracy);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.movement);
			unit.removeNonStackingStatBuff(this.attack);
			unit.removePreDamageListener(CUnitAttackPreDamageListenerPriority.ACCURACY, this.accuracy);
		}

		@Override public boolean isTimedLifeBar() { return false; }
		@Override public boolean isDispellable() { return true; }
		@Override public boolean isHeroSpellBuff() { return true; }
		@Override public DispelPolarity getDispelPolarity() { return DispelPolarity.HARMFUL; }
	}
}
