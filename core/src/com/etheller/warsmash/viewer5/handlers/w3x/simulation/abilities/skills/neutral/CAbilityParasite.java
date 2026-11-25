package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityParasite extends CAbilityTargetSpellBase implements CAutocastAbility {
	public static final War3ID CODE = War3ID.fromString("ANpa");
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BNpa");
	private static final War3ID MINION_BUFF = War3ID.fromString("BNpm");
	private static final float MINION_LIFETIME = 90;
	private static final int DAMAGE_INTERVAL = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
	private static final int LETHAL_POISON_FLAG = 8;
	private boolean autocast;
	private War3ID buff;
	private War3ID minion;
	private int minionCount;
	private float damage;
	private float attackSlow;
	private float movementSlow;
	private float projectileSpeed;
	private boolean projectileHoming;
	private boolean lethal;

	public CAbilityParasite(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.parasite; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.parasiteon; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.parasiteoff; }
	@Override public AutocastType getAutocastType() { return AutocastType.FARTHESTENEMY; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }

	@Override
	public void setAutoCastOn(final CUnit unit, final boolean enabled) {
		if (this.autocast == enabled) return;
		this.autocast = enabled;
		unit.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setAutoCastOn(unit, false);
		super.onRemove(game, unit);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.buff = War3ID.NONE.equals(authoredBuff) ? DEFAULT_BUFF : authoredBuff;
		this.minion = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
		this.damage = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.attackSlow = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.movementSlow = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.lethal = (definition.getFieldAsInteger(AbilityFields.DATA_D + level, 0) & LETHAL_POISON_FLAG) != 0;
		this.minionCount = definition.getFieldAsInteger(AbilityFields.DATA_E + level, 0);
		this.projectileSpeed = definition.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		this.projectileHoming = definition.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && unit.getFirstAbilityOfType(ParasiteBuff.class) == null
				&& unit.isVisible(game, caster.getPlayerIndex()) && caster.canReach(unit, getCastRange())) {
			checkCanTarget(game, caster, orderId, target, receiver);
		}
		else receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CUnit unit)) return false;
		final float duration = getDurationForTarget(unit);
		final var effect = new ParasiteBuff(game.getHandleIdAllocator().createId(), this.buff, duration, caster,
				this.minion, this.minionCount, this.damage, this.lethal, this.attackSlow, this.movementSlow);
		if (this.projectileSpeed > 0) {
			game.createProjectile(caster, getAlias(), caster.getX(), caster.getY(), (float) caster.angleTo(target),
					this.projectileSpeed, this.projectileHoming, target, new CAbilityProjectileListener() {
						@Override
						public void onLaunch(final CSimulation simulation, final CProjectile projectile, final AbilityTarget launchTarget) { }

						@Override
						public void onHit(final CSimulation simulation, final CProjectile projectile, final AbilityTarget hit) {
							if (hit instanceof CUnit victim) infest(simulation, caster, victim, effect);
						}
					});
		}
		else infest(game, caster, unit, effect);
		return false;
	}

	private void infest(final CSimulation game, final CUnit caster, final CUnit unit, final ParasiteBuff effect) {
		if (unit.isDead() || unit.isHidden() || unit.isInvulnerable() || effect.getDurationMax() <= 0
				|| !unit.canBeTargetedBy(game, caster, getTargetsAllowed()) || !unit.checkForAbilityEffectReaction(game, caster, this)) return;
		final ParasiteBuff previous = unit.getFirstAbilityOfType(ParasiteBuff.class);
		if (previous != null) unit.remove(game, previous);
		unit.add(game, effect);
	}

	private static final class ParasiteBuff extends CBuffTimed {
		private final CUnit caster;
		private final int owner;
		private final War3ID minion;
		private final int count;
		private final float damage;
		private final boolean lethal;
		private final NonStackingStatBuff attack;
		private final NonStackingStatBuff movement;
		private int nextDamage;

		private ParasiteBuff(final int handleId, final War3ID alias, final float duration, final CUnit caster,
				final War3ID minion, final int count, final float damage, final boolean lethal, final float attackSlow,
				final float movementSlow) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.caster = caster;
			this.owner = caster.getPlayerIndex();
			this.minion = minion;
			this.count = count;
			this.damage = damage;
			this.lethal = lethal;
			this.attack = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "parasite", -attackSlow);
			this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "parasite", -movementSlow);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.nextDamage = game.getGameTurnTick() + DAMAGE_INTERVAL;
			if (this.attack.getValue() != 0) unit.addNonStackingStatBuff(this.attack);
			if (this.movement.getValue() != 0) unit.addNonStackingStatBuff(this.movement);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			if (this.attack.getValue() != 0) unit.removeNonStackingStatBuff(this.attack);
			if (this.movement.getValue() != 0) unit.removeNonStackingStatBuff(this.movement);
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (this.damage > 0 && game.getGameTurnTick() >= this.nextDamage) {
				this.nextDamage += DAMAGE_INTERVAL;
				final float amount = this.lethal ? this.damage : Math.min(this.damage, unit.getLife() - 1);
				if (amount > 0) unit.damage(game, this.caster, false, true, CAttackType.SPELLS, CDamageType.POISON, "", amount);
			}
			if (!unit.isDead()) super.onTick(game, unit);
		}

		@Override
		public void onDeath(final CSimulation game, final CUnit unit) {
			if (this.minion != null && !War3ID.NONE.equals(this.minion)) {
				for (int index = 0; index < this.count; index++) {
					final CUnit summoned = game.createUnitSimple(this.minion, this.owner, unit.getX(), unit.getY(), unit.getFacing());
					if (summoned == null) continue;
					summoned.addClassification(CUnitClassification.SUMMONED);
					summoned.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), MINION_BUFF, MINION_LIFETIME, true));
					this.caster.fireSummonEvents(game, summoned);
				}
			}
			super.onDeath(game, unit);
		}

		@Override public boolean isTimedLifeBar() { return false; }
	}
}
