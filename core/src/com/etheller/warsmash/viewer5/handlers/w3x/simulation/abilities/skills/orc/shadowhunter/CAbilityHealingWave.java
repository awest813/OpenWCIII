package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.shadowhunter;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityHealingWave extends CAbilityTargetSpellBase {
	private static final War3ID PRIMARY_BOLT = War3ID.fromString("HWPB");
	private static final War3ID SECONDARY_BOLT = War3ID.fromString("HWSB");
	private static final float BOLT_DURATION = 2f;
	private static final float BOUNCE_DELAY = 0.25f;

	private War3ID primaryBolt;
	private War3ID secondaryBolt;
	private float healing;
	private float remainingFraction;
	private int targetLimit;
	private float bounceRadius;

	public CAbilityHealingWave(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.healingwave;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.healing = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.targetLimit = data.getFieldAsInteger(AbilityFields.DATA_B + level, 0);
		this.remainingFraction = 1 - data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.bounceRadius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		final War3ID primary = AbstractCAbilityTypeDefinition.getLightningId(data, level, 0);
		final War3ID secondary = AbstractCAbilityTypeDefinition.getLightningId(data, level, 1);
		this.primaryBolt = War3ID.NONE.equals(primary) ? PRIMARY_BOLT : primary;
		this.secondaryBolt = War3ID.NONE.equals(secondary) ? SECONDARY_BOLT : secondary;
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
		if (target instanceof CUnit unit) {
			heal(game, caster, unit, getAlias(), this.primaryBolt, this.healing);
			if (this.targetLimit > 1) {
				new Bounce(caster, unit, getAlias(), this.secondaryBolt, getTargetsAllowed(), this.bounceRadius,
						this.targetLimit - 1, this.healing * this.remainingFraction, this.remainingFraction).start(game);
			}
		}
		return false;
	}

	private static void heal(final CSimulation game, final CUnit from, final CUnit to, final War3ID alias,
			final War3ID bolt, final float amount) {
		game.createLightning(from, bolt, to, BOLT_DURATION);
		game.createTemporarySpellEffectOnUnit(to, alias, CEffectType.TARGET);
		to.heal(game, amount);
	}

	private static final class Bounce extends CTimer {
		private final CUnit caster;
		private final War3ID alias;
		private final War3ID bolt;
		private final EnumSet<CTargetType> allowed;
		private final float radius;
		private final float remainingFraction;
		private final Set<CUnit> visited = new HashSet<>();
		private int remainingTargets;
		private float amount;
		private CUnit previous;

		private Bounce(final CUnit caster, final CUnit first, final War3ID alias, final War3ID bolt,
				final EnumSet<CTargetType> allowed, final float radius, final int remainingTargets,
				final float amount, final float remainingFraction) {
			this.caster = caster;
			this.alias = alias;
			this.bolt = bolt;
			this.allowed = allowed.clone();
			this.radius = radius;
			this.remainingTargets = remainingTargets;
			this.amount = amount;
			this.remainingFraction = remainingFraction;
			this.previous = first;
			this.visited.add(first);
			setTimeoutTime(BOUNCE_DELAY);
		}

		@Override
		public void onFire(final CSimulation game) {
			final CUnit[] closest = { null };
			game.getWorldCollision().enumUnitsInRange(this.previous.getX(), this.previous.getY(), this.radius, unit -> {
				if (!unit.isDead() && !unit.isHidden() && unit.getLife() < unit.getMaximumLife()
						&& !this.visited.contains(unit) && unit.canBeTargetedBy(game, this.caster, this.allowed)
						&& (closest[0] == null || this.previous.distanceSquaredNoCollision(unit)
								< this.previous.distanceSquaredNoCollision(closest[0]))) {
					closest[0] = unit;
				}
				return false;
			});
			final CUnit next = closest[0];
			if (next == null) return;
			heal(game, this.previous, next, this.alias, this.bolt, this.amount);
			this.visited.add(next);
			this.previous = next;
			this.amount *= this.remainingFraction;
			if (--this.remainingTargets > 0) start(game);
		}
	}
}
