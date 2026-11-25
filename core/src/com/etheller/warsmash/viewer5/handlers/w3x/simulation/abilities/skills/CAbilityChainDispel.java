package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityChainDispel extends CAbilityTargetSpellBase {
	private float radius;
	private float manaLoss;
	private float summonDamage;
	private int maximumUnits;
	private float missileSpeed;
	private boolean missileHoming;

	public CAbilityChainDispel(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemchaindispel;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.readSLKTagFloat(AbilityFields.AREA_OF_EFFECT + level);
		this.manaLoss = definition.readSLKTagFloat(AbilityFields.DATA_A + level);
		this.summonDamage = definition.readSLKTagFloat(AbilityFields.DATA_B + level);
		this.maximumUnits = definition.readSLKTagInt(AbilityFields.DATA_C + level);
		this.missileSpeed = definition.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		this.missileHoming = definition.getFieldAsBoolean(AbilityFields.PROJECTILE_HOMING_ENABLED, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int order,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit primary = target.visit(AbilityTargetVisitor.UNIT);
		if (primary == null || this.maximumUnits <= 0) return false;
		final var owner = game.getPlayer(caster.getPlayerIndex());
		final boolean friendly = owner.hasAlliance(primary.getPlayerIndex(), CAllianceType.PASSIVE);
		final var recipients = new ArrayList<CUnit>();
		recipients.add(primary);
		game.getWorldCollision().enumUnitsInRange(primary.getX(), primary.getY(), this.radius, nearby -> {
			if (nearby != primary && !nearby.isDead() && !nearby.isHidden()
					&& owner.hasAlliance(nearby.getPlayerIndex(), CAllianceType.PASSIVE) == friendly
					&& nearby.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				recipients.add(nearby);
			}
			return false;
		});
		recipients.sort(Comparator.comparing((CUnit unit) -> unit != primary)
				.thenComparingDouble(primary::distanceSquaredNoCollision).thenComparingInt(CUnit::getHandleId));
		final List<CUnit> affected = recipients.subList(0, Math.min(this.maximumUnits, recipients.size()));
		if (this.missileSpeed > 0) launchMissile(game, caster, caster, affected, 0, !friendly);
		else for (final CUnit unit : affected) dispel(game, caster, unit, !friendly);
		return false;
	}

	private void launchMissile(final CSimulation game, final CUnit caster, final CUnit source, final List<CUnit> targets,
			final int index, final boolean enemy) {
		if (index >= targets.size()) return;
		final CUnit target = targets.get(index);
		game.createProjectile(source, getAlias(), source.getX(), source.getY(), (float) source.angleTo(target),
				this.missileSpeed, this.missileHoming, target, new CAbilityProjectileListener() {
					@Override public void onLaunch(final CSimulation simulation, final CProjectile projectile, final AbilityTarget impact) { }

					@Override
					public void onHit(final CSimulation simulation, final CProjectile projectile, final AbilityTarget impact) {
						dispel(simulation, caster, target, enemy);
						launchMissile(simulation, caster, target, targets, index + 1, enemy);
					}
				});
	}

	private void dispel(final CSimulation game, final CUnit caster, final CUnit target, final boolean enemy) {
		if (!target.isDead() && !target.isHidden() && target.checkForAbilityEffectReaction(game, caster, this)) {
			CAbilityAreaDispel.dispelUnit(game, caster, target, this.manaLoss, this.summonDamage, enemy);
			game.createTemporarySpellEffectOnUnit(target, getAlias(), CEffectType.TARGET);
		}
	}
}
