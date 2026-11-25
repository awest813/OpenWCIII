package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;

/** Faerie Dragons can leave combat briefly after taking a hit. */
public final class CAbilityPhaseShift extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("Apsh");
	private boolean autocast = true;
	private CTimer returnTimer;
	private final CUnitAttackDamageTakenListener reactToDamage = (game, attacker, unit, attack, ranged, type, damage,
			bonus, actual) -> {
		if (this.autocast && actual > 0 && !isDisabled() && canShift(unit)
				&& getCooldownRemaining(game, unit) <= 0 && unit.chargeManaForSpell(game, getManaCost())) {
			unit.beginCooldown(game, getCode(), getCooldown());
			doEffect(game, unit, null);
			game.unitSoundEffectEvent(unit, getAlias());
		}
	};

	public CAbilityPhaseShift(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.phaseshift; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.phaseshifton; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.phaseshiftoff; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOn(final CUnit unit, final boolean enabled) { this.autocast = enabled; }

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		unit.addDamageTakenListener(this.reactToDamage);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removeDamageTakenListener(this.reactToDamage);
		onDeath(game, unit);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		if (this.returnTimer != null) {
			this.returnTimer.pause(game);
			returnToCombat(unit);
		}
	}

	private static boolean canShift(final CUnit unit) {
		final CAbilityManaFlare flare = unit.getFirstAbilityOfType(CAbilityManaFlare.class);
		return !unit.isDead() && !unit.isHidden() && !unit.isPaused() && !unit.isInvulnerable()
				&& (flare == null || !flare.isToggleOn())
				&& (unit.getCurrentBehavior() == null || unit.getCurrentBehavior().getHighlightOrderId() != OrderIds.manaflareon);
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (canShift(unit)) receiver.useOk();
		else receiver.notAnActiveAbility();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!canShift(caster)) return false;
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.SPECIAL);
		caster.setInvulnerable(true);
		caster.setPaused(true);
		caster.setHidden(true);
		this.returnTimer = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				returnToCombat(caster);
			}
		};
		this.returnTimer.setTimeoutTime(getDuration());
		this.returnTimer.start(game);
		return false;
	}

	private void returnToCombat(final CUnit unit) {
		this.returnTimer = null;
		unit.setInvulnerable(false);
		unit.setPaused(false);
		unit.setHidden(false);
	}
}
