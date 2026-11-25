package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop;

import java.util.HashMap;
import java.util.Map;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

final class CShopStock {
	private final Map<War3ID, StockEntry> entries = new HashMap<>();

	void set(final War3ID id, final int currentStock, final int maximumStock) {
		this.entries.put(id, new StockEntry(currentStock, maximumStock, 0, 0, 0));
	}

	void addAuthored(final War3ID id, final int maximum, final int availabilityDelay, final int replenishSeconds,
			final int tick) {
		if (maximum <= 0) return;
		final int availabilityTick = toTicks(availabilityDelay);
		final int initialCount = tick >= availabilityTick ? maximum : 0;
		this.entries.putIfAbsent(id, new StockEntry(initialCount, maximum, toTicks(replenishSeconds), availabilityTick, tick));
	}

	private static int toTicks(final int seconds) {
		return Math.max(0, Math.round(seconds / WarsmashConstants.SIMULATION_STEP_TIME));
	}

	void remove(final War3ID id) {
		this.entries.remove(id);
	}

	void clear() {
		this.entries.clear();
	}

	int count(final War3ID id) {
		final StockEntry stock = this.entries.get(id);
		return stock == null ? Integer.MAX_VALUE : stock.count;
	}

	boolean checkAvailable(final War3ID id, final int tick, final AbilityActivationReceiver receiver) {
		final StockEntry stock = this.entries.get(id);
		if (stock == null || stock.count > 0) return true;
		if (stock.nextReplenishTick != Integer.MAX_VALUE) {
			final float remaining = Math.max(0, stock.nextReplenishTick - tick) * WarsmashConstants.SIMULATION_STEP_TIME;
			receiver.cooldownNotYetReady(remaining, stock.cooldownTicks() * WarsmashConstants.SIMULATION_STEP_TIME);
		}
		receiver.activationCheckFailed(CommandStringErrorKeys.OUT_OF_STOCK);
		return false;
	}

	void sold(final War3ID id, final int tick) {
		final StockEntry stock = this.entries.get(id);
		if (stock != null) stock.sell(tick);
	}

	boolean replenish(final int tick) {
		boolean replenished = false;
		for (final StockEntry stock : this.entries.values()) replenished |= stock.refill(tick);
		return replenished;
	}

	private static final class StockEntry {
		private final int maximum;
		private final int replenishTicks;
		private final int availabilityTick;
		private int count;
		private int nextReplenishTick;
		private boolean waitingForAvailability;

		private StockEntry(final int count, final int maximum, final int replenishTicks, final int availabilityTick,
				final int tick) {
			this.count = count;
			this.maximum = maximum;
			this.replenishTicks = replenishTicks;
			this.availabilityTick = availabilityTick;
			this.waitingForAvailability = tick < availabilityTick;
			this.nextReplenishTick = this.waitingForAvailability ? availabilityTick : Integer.MAX_VALUE;
		}

		private int cooldownTicks() {
			return this.waitingForAvailability ? this.availabilityTick : this.replenishTicks;
		}

		private void sell(final int tick) {
			this.count--;
			if (this.replenishTicks > 0 && this.nextReplenishTick == Integer.MAX_VALUE) {
				this.nextReplenishTick = tick + this.replenishTicks;
			}
		}

		private boolean refill(final int tick) {
			if (this.nextReplenishTick == Integer.MAX_VALUE || tick < this.nextReplenishTick) return false;
			if (this.waitingForAvailability) {
				this.count = this.maximum;
				this.waitingForAvailability = false;
				this.nextReplenishTick = Integer.MAX_VALUE;
			}
			else {
				final int restored = Math.min(this.maximum - this.count, 1 + (tick - this.nextReplenishTick) / this.replenishTicks);
				this.count += restored;
				this.nextReplenishTick = this.count < this.maximum ? this.nextReplenishTick + restored * this.replenishTicks
						: Integer.MAX_VALUE;
			}
			return true;
		}
	}
}
