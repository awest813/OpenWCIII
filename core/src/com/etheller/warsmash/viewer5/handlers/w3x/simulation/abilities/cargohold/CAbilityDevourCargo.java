package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import java.util.EnumSet;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CUnitVisionFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityDevourCargo extends CAbilityCargoHold {
	private static final float DEFAULT_DAMAGE_PER_SECOND = 5;
	private int nextDamageTick;
	private int visionOwner;
	private float damagePerSecond;
	private boolean victimWasPaused;
	private CUnitVisionFogModifier sharedSight;

	public CAbilityDevourCargo(final int handleId, final War3ID code, final War3ID alias, final int capacity,
			final float interval, final float range, final EnumSet<CTargetType> targets) {
		super(handleId, code, alias, capacity, interval, range, targets);
	}

	public void swallow(final CSimulation game, final CUnit caster, final CUnit victim, final float configuredDamage) {
		this.damagePerSecond = configuredDamage > 0 ? configuredDamage : DEFAULT_DAMAGE_PER_SECOND;
		this.nextDamageTick = game.getGameTurnTick() + damageIntervalTicks();
		this.victimWasPaused = victim.isPaused();
		victim.order(game, OrderIds.stop, null);
		victim.setHidden(true);
		victim.setPaused(true);
		addUnit(caster, victim);
		this.visionOwner = victim.getPlayerIndex();
		this.sharedSight = new CUnitVisionFogModifier(caster, false);
		game.getPlayer(this.visionOwner).addFogModifer(game, this.sharedSight, false);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		if (isEmpty()) return;
		final CUnit victim = getUnit(0);
		if (victim.isDead() || !game.containsUnit(victim)) {
			releaseVictim(game, caster);
			return;
		}
		if (game.getGameTurnTick() < this.nextDamageTick) return;
		final int interval = damageIntervalTicks();
		this.nextDamageTick = game.getGameTurnTick() + interval;
		victim.damage(game, caster, false, false, CAttackType.SPELLS, CDamageType.UNIVERSAL, null,
				this.damagePerSecond * interval * WarsmashConstants.SIMULATION_STEP_TIME);
		if (victim.isDead()) releaseVictim(game, caster);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit caster) {
		releaseVictim(game, caster);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit caster) {
		releaseVictim(game, caster);
	}

	@Override
	public void removePassenger(final CSimulation game, final CUnit carrier, final CUnit passenger) {
		if (!isEmpty() && getUnit(0) == passenger) releaseVictim(game, carrier);
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId, final AbilityTarget target) {
		return orderId != OrderIds.unload;
	}

	private int damageIntervalTicks() {
		final float interval = getDuration() > 0 ? getDuration() : 0.5f;
		return (int) Math.ceil(interval / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	private void releaseVictim(final CSimulation game, final CUnit caster) {
		if (isEmpty()) return;
		final CUnit victim = removeUnitAtIndex(caster, 0);
		game.getPlayer(this.visionOwner).removeFogModifer(game, this.sharedSight);
		this.sharedSight = null;
		if (!game.containsUnit(victim)) return;
		victim.setPointAndCheckUnstuck(caster.getX(), caster.getY(), game);
		victim.setHidden(false);
		victim.setPaused(this.victimWasPaused);
	}
}
