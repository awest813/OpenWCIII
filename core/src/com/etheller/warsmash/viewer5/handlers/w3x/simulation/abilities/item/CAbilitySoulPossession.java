package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilitySoulPossession extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Asou");
	private static final War3ID FILLED_GEM = War3ID.fromString("soul");
	private static final int ITEM_EVENT_GRACE_TICKS = 12;

	private CUnit carrier;
	private CItem filledGem;
	private int createGemTick;
	private boolean paused;
	private boolean invulnerable;

	public CAbilitySoulPossession(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	void capture(final CSimulation game, final CUnit target, final CUnit carrier) {
		this.carrier = carrier;
		this.paused = target.isPaused();
		this.invulnerable = target.isInvulnerable();
		this.createGemTick = game.getGameTurnTick() + ITEM_EVENT_GRACE_TICKS;
		target.order(game, OrderIds.stop, null);
		target.add(game, this);
		target.setPaused(true);
		target.setInvulnerable(true);
		target.setHidden(true);
	}

	boolean isCarriedBy(final CUnit unit) {
		return this.carrier == unit;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit target) {
		final CUnit holder = this.carrier;
		if (holder == null) return;
		if (!game.containsUnit(holder) || holder.isDead() || this.filledGem != null && this.filledGem.isDead()) {
			target.remove(game, this);
			return;
		}
		if (this.filledGem == null) {
			final var inventory = holder.getInventoryData();
			if (inventory == null) return;
			for (int slot = 0; slot < inventory.getItemCapacity(); slot++) {
				final CItem item = inventory.getItemInSlot(slot);
				if (item != null && FILLED_GEM.equals(item.getTypeId())) {
					this.filledGem = item;
					break;
				}
			}
			// A campaign trigger can supply the filled gem after the item-use event.
			if (this.filledGem == null && game.getGameTurnTick() >= this.createGemTick) {
				final CItem item = game.createItem(FILLED_GEM, holder.getX(), holder.getY());
				if (item == null || inventory.giveItem(game, holder, item, false) == -1) {
					if (item != null) game.removeItem(item);
					target.remove(game, this);
					return;
				}
				this.filledGem = item;
			}
			if (this.filledGem != null) this.filledGem.setDroppable(false);
		}
		target.setPaused(true);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit target) {
		final CUnit holder = this.carrier;
		if (holder == null) return;
		this.carrier = null;
		target.setPaused(this.paused);
		target.setInvulnerable(this.invulnerable);
		if (game.containsUnit(target) && !target.isDead()) {
			target.setHidden(false);
			target.setPointAndCheckUnstuck(holder.getX(), holder.getY(), game);
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit target) {
		if (this.carrier != null) {
			if (this.filledGem != null && !this.filledGem.isDead()) game.removeItem(this.filledGem);
			target.remove(game, this);
		}
	}
}
