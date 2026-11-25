package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityBattleStations extends CAbilityNoTargetSpellBase {
	private static final War3ID LOAD = War3ID.fromString("Aloa");
	private War3ID workerType;
	private float radius;
	private CAbilityLoad boarding;

	public CAbilityBattleStations(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.battlestations;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final String unitId = definition.readSLKTag(AbilityFields.UNIT_ID + level);
		this.workerType = unitId.length() == 4 ? War3ID.fromString(unitId) : null;
		this.radius = definition.readSLKTagFloat(AbilityFields.AREA_OF_EFFECT + level);
		if (this.boarding != null && this.workerType != null) this.boarding.setAllowedUnitTypes(Set.of(this.workerType));
	}

	@Override
	public void onAddDisabled(final CSimulation game, final CUnit burrow) {
		// The stock Burrow lists Battle Stations and Cargo Hold without a separate Load entry.
		final CAbilityCargoHold cargo = burrow.getCargoData();
		if (cargo != null && this.workerType != null && burrow.getFirstAbilityOfType(CAbilityLoad.class) == null) {
			this.boarding = new CAbilityLoad(game.getHandleIdAllocator().createId(), LOAD, getAlias(), cargo.getCastRange(),
					Set.of(this.workerType));
			this.boarding.setIconShowing(false);
			burrow.add(game, this.boarding);
		}
	}

	@Override
	public void onRemoveDisabled(final CSimulation game, final CUnit burrow) {
		if (this.boarding != null && burrow.getAbilities().contains(this.boarding)) burrow.remove(game, this.boarding);
		this.boarding = null;
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit burrow, final int orderId,
			final AbilityTarget target) {
		if (orderId == getBaseOrderId()) {
			doEffect(game, burrow, null);
			return false;
		}
		return super.checkBeforeQueue(game, burrow, orderId, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit burrow, final AbilityTarget target) {
		final CAbilityCargoHold cargo = burrow.getCargoData();
		if (cargo == null || this.workerType == null || this.radius <= 0) return false;
		int freeSlots = cargo.getCargoCapacity();
		for (int index = 0; index < cargo.getCargoCount(); index++) {
			freeSlots -= cargo.getUnit(index).getUnitType().getCargoCapacity();
		}
		if (freeSlots <= 0) return false;
		final var workers = new ArrayList<CUnit>();
		final CAbilityChaosCargoLoad chaosLoad = burrow.getFirstAbilityOfType(CAbilityChaosCargoLoad.class);
		game.getWorldCollision().enumUnitsInRange(burrow.getX(), burrow.getY(), this.radius, worker -> {
			if ((this.workerType.equals(worker.getTypeId()) || chaosLoad != null && chaosLoad.allows(worker))
					&& worker.getPlayerIndex() == burrow.getPlayerIndex()
					&& !worker.isDead() && !worker.isHidden() && !worker.isPaused() && worker.getTransport() == null
					&& !worker.isMovementDisabled()
					&& CAbilityLoad.getTransportLoad(game, worker, burrow, true, false) != null) workers.add(worker);
			return false;
		});
		workers.sort(Comparator.comparingDouble((CUnit worker) -> burrow.distanceSquaredNoCollision(worker))
				.thenComparingInt(CUnit::getHandleId));
		for (final CUnit worker : workers) {
			final int size = worker.getUnitType().getCargoCapacity();
			if (size > 0 && size <= freeSlots && worker.order(game, OrderIds.smart, burrow)) {
				freeSlots -= size;
				if (freeSlots == 0) break;
			}
		}
		return false;
	}
}
