package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.function.ObjIntConsumer;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public final class CAbilityManaFlare extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("Amfl");
	private static final float FLARE_INTERVAL = 0.75f;
	private float radius;
	private float damagePerMana;
	private float heroDamagePerMana;
	private float damageCap;
	private float heroDamageCap;
	private float armorBonus;
	private int endTick;
	private int nextFlareTick;
	private NonStackingStatBuff armor;
	private ObjIntConsumer<CUnit> listener;
	private SimulationRenderComponent art;

	public CAbilityManaFlare(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return isToggleOn() ? OrderIds.manaflareoff : OrderIds.manaflareon; }
	@Override public boolean isToggleOn() { return this.listener != null; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.damagePerMana = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.heroDamagePerMana = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.damageCap = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.heroDamageCap = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.armorBonus = definition.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		final var targets = CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level));
		if (targets.isEmpty()) getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (isToggleOn() && orderId == OrderIds.manaflareoff) receiver.useOk();
		else super.innerCheckCanUse(game, unit, orderId, receiver);
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		if (orderId == OrderIds.manaflareoff) {
			clearFlare(game, caster);
			return caster.getStopBehavior();
		}
		return super.beginNoTarget(game, caster, orderId);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		clearFlare(game, caster);
		this.endTick = game.getGameTurnTick() + ticks(getDuration());
		this.nextFlareTick = game.getGameTurnTick();
		this.armor = new NonStackingStatBuff(NonStackingStatBuffType.DEF, "manaflare", this.armorBonus);
		caster.addNonStackingStatBuff(this.armor);
		this.art = game.createPersistentSpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		this.listener = (enemy, mana) -> retaliate(game, caster, enemy, mana);
		game.addSpellManaListener(this.listener);
		caster.notifyOrdersChanged();
		return true;
	}

	private void retaliate(final CSimulation game, final CUnit caster, final CUnit enemy, final int mana) {
		final int tick = game.getGameTurnTick();
		if (tick < this.nextFlareTick || caster.isDead() || caster.isHidden() || caster.isPaused() || isDisabled()
				|| enemy.getMaximumMana() <= 0 || !caster.canReach(enemy, this.radius) || !isValidEnemy(game, caster, enemy)) return;
		this.nextFlareTick = tick + ticks(FLARE_INTERVAL);
		final float damage = damageFor(enemy, mana);
		game.createTemporarySpellEffectOnUnit(enemy, getAlias(), CEffectType.TARGET);
		enemy.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, damage);
		if (getCastRange() <= 0) return;
		game.getWorldCollision().enumUnitsInRange(enemy.getX(), enemy.getY(), getCastRange(), unit -> {
			if (unit != enemy && unit.getMaximumMana() > 0 && isValidEnemy(game, caster, unit)) {
				unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, damageFor(unit, mana));
			}
			return false;
		});
	}

	private float damageFor(final CUnit enemy, final int mana) {
		return enemy.isHero() ? Math.min(this.heroDamageCap, mana * this.heroDamagePerMana)
				: Math.min(this.damageCap, mana * this.damagePerMana);
	}

	private boolean isValidEnemy(final CSimulation game, final CUnit caster, final CUnit unit) {
		return !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable() && !unit.isBuilding()
				&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(), new BooleanAbilityTargetCheckReceiver<CWidget>());
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		return isToggleOn() && !isDisabled() && game.getGameTurnTick() < this.endTick;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target, final boolean interrupted) {
		clearFlare(game, caster);
	}

	@Override public void onRemove(final CSimulation game, final CUnit caster) { clearFlare(game, caster); }
	@Override public void onDeath(final CSimulation game, final CUnit caster) { clearFlare(game, caster); }

	private void clearFlare(final CSimulation game, final CUnit caster) {
		if (this.listener == null) return;
		game.removeSpellManaListener(this.listener);
		this.listener = null;
		caster.removeNonStackingStatBuff(this.armor);
		this.armor = null;
		this.art.remove();
		this.art = null;
		caster.notifyOrdersChanged();
	}

	private static int ticks(final float seconds) {
		return Math.max(1, (int) Math.ceil(seconds / WarsmashConstants.SIMULATION_STEP_TIME));
	}
}
