package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityUnitOrPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityCollisionProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityBreathOfFire extends CAbilityUnitOrPointTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("ANbf");
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BNbf");
	private float damage;
	private float damageCap;
	private float distance;
	private float innerRadius;
	private float outerRadius;
	private float burnDamage;
	private float speed;
	private War3ID buffId;

	public CAbilityBreathOfFire(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.breathoffire; }

	@Override
	public void populateData(final GameObject data, final int level) {
		this.damage = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damageCap = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.distance = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.innerRadius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.outerRadius = data.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.burnDamage = data.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		this.speed = data.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.buffId = War3ID.NONE.equals(buff) ? DEFAULT_BUFF : buff;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final float facing = (float) caster.angleTo(target);
		final float directionX = (float) Math.cos(facing);
		final float directionY = (float) Math.sin(facing);
		final float range = this.distance > 0 ? this.distance : getCastRange();
		final AbilityPointTarget end = new AbilityPointTarget(caster.getX() + range * directionX, caster.getY() + range * directionY);
		game.createCollisionProjectile(caster, getAlias(), caster.getX() + 43 * directionX, caster.getY() + 43 * directionY,
				facing, this.speed > 0 ? this.speed : 1050, false, end, 0, 1, this.innerRadius, this.outerRadius, 0.05f,
				new Breath(caster), true);
		return false;
	}

	private final class Breath implements CAbilityCollisionProjectileListener {
		private final CUnit caster;
		private final BooleanAbilityTargetCheckReceiver<CWidget> receiver = new BooleanAbilityTargetCheckReceiver<>();
		private float remainingDamage = CAbilityBreathOfFire.this.damageCap;
		private float damageThisPass;
		private int targets;

		private Breath(final CUnit caster) {
			this.caster = caster;
		}

		@Override
		public boolean canHitTarget(final CSimulation game, final CWidget target) {
			return target instanceof CUnit unit && !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& !game.getPlayer(this.caster.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
					&& unit.canBeTargetedBy(game, this.caster, false, getTargetsAllowed(), this.receiver.reset());
		}

		@Override
		public void onPreHits(final CSimulation game, final CProjectile projectile, final AbilityPointTarget location) {
			this.damageThisPass = CAbilityBreathOfFire.this.damageCap <= 0 ? CAbilityBreathOfFire.this.damage
					: Math.min(CAbilityBreathOfFire.this.damage, this.remainingDamage / Math.max(1, this.targets));
		}

		@Override
		public void onHit(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
			if (!(target instanceof CUnit unit) || !unit.checkForAbilityEffectReaction(game, this.caster, CAbilityBreathOfFire.this)) return;
			if (this.damageThisPass > 0) {
				this.remainingDamage = Math.max(0, this.remainingDamage - this.damageThisPass);
				unit.damage(game, this.caster, false, true, CAttackType.SPELLS, CDamageType.FIRE, null, this.damageThisPass);
			}
			final float duration = getDurationForTarget(unit);
			if (!unit.isDead() && duration > 0 && CAbilityBreathOfFire.this.burnDamage > 0 && CAbilityDrunkenHaze.hasHaze(unit)) {
				final Burn previous = unit.getFirstAbilityOfType(Burn.class);
				if (previous != null) unit.remove(game, previous);
				unit.add(game, new Burn(game.getHandleIdAllocator().createId(), CAbilityBreathOfFire.this.buffId, duration,
						this.caster, CAbilityBreathOfFire.this.burnDamage));
			}
		}

		@Override public void setUnitTargets(final int count) { this.targets = count; }
		@Override public void setDestructableTargets(final int count) { }
		@Override public void setCurrentLocation(final AbilityPointTarget location) { }
		@Override public void onLaunch(final CSimulation game, final CProjectile projectile, final AbilityTarget target) { }
	}

	private static final class Burn extends CBuffTimed {
		private static final int SECOND = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		private final CUnit source;
		private final float damage;
		private int nextDamageTick;

		private Burn(final int handleId, final War3ID alias, final float duration, final CUnit source, final float damage) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.source = source;
			this.damage = damage;
		}

		@Override protected void onBuffAdd(final CSimulation game, final CUnit unit) { this.nextDamageTick = game.getGameTurnTick() + SECOND; }
		@Override protected void onBuffRemove(final CSimulation game, final CUnit unit) { }

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (game.getGameTurnTick() >= this.nextDamageTick) {
				this.nextDamageTick += SECOND;
				unit.damage(game, this.source, false, true, CAttackType.SPELLS, CDamageType.FIRE, null, this.damage);
			}
			if (!unit.isDead()) super.onTick(game, unit);
		}

		@Override public boolean isTimedLifeBar() { return false; }
		@Override public boolean isDispellable() { return true; }
		@Override public boolean isHeroSpellBuff() { return true; }
		@Override public DispelPolarity getDispelPolarity() { return DispelPolarity.HARMFUL; }
	}
}
