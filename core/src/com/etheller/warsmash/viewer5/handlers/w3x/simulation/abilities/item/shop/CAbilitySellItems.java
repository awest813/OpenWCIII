package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItemType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.AbstractCAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilitySellItems extends AbstractCAbility {
	private final List<War3ID> itemsSold;
	private final CShopStock itemStock = new CShopStock();

	public CAbilitySellItems(final int handleId, final List<War3ID> itemsSold) {
		super(handleId, War3ID.fromString("Asei"));
		this.itemsSold = new ArrayList<>(itemsSold);
	}

	public List<War3ID> getItemsSold() {
		return this.itemsSold;
	}

	public void addItemToStock(final War3ID itemId, final int currentStock, final int stockMax) {
		if (!this.itemsSold.contains(itemId)) {
			this.itemsSold.add(itemId);
		}
		this.itemStock.set(itemId, currentStock, stockMax);
	}

	public void removeItemFromStock(final War3ID itemId) {
		this.itemsSold.remove(itemId);
		this.itemStock.remove(itemId);
	}

	public int getItemStockCurrent(final War3ID itemId) {
		return this.itemStock.count(itemId);
	}

	private void loadObjectStock(final CSimulation game) {
		for (final War3ID itemId : this.itemsSold) {
			final CItemType type = game.getItemData().getItemType(itemId);
			if (type != null) this.itemStock.addAuthored(itemId, type.getStockMax(), type.getStockStartDelay(),
					type.getStockReplenishInterval(), game.getGameTurnTick());
		}
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		final int playerIndex = orderId & 0xFF; // TODO this is stupid, and should be passed as some "acting player" arg
		final int itemIndex = ((orderId & 0xFF00) >> 8) - 1;
		if ((itemIndex >= 0) && (itemIndex < this.itemsSold.size())) {
			final War3ID itemTypeId = this.itemsSold.get(itemIndex);
			final CItemType itemType = game.getItemData().getItemType(itemTypeId);
			if (itemType != null) {
				if (!this.itemStock.checkAvailable(itemTypeId, game.getGameTurnTick(), receiver)) return;
				final CPlayer player = game.getPlayer(playerIndex);
				if (player.getGold() < itemType.getGoldCost()) {
					receiver.activationCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_GOLD);
					return;
				}
				if (player.getLumber() < itemType.getLumberCost()) {
					receiver.activationCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_LUMBER);
					return;
				}
			}
		}
		receiver.useOk();
	}

	@Override
	public final void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public final void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public final void checkCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		final int itemIndex = ((orderId & 0xFF00) >> 8) - 1;
		if ((itemIndex >= 0) && (itemIndex < this.itemsSold.size())) {
			receiver.targetOk(null);
		}
		else {
			receiver.orderIdNotAccepted();
		}
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		return true;
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		loadObjectStock(game);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {

	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		if (this.itemStock.replenish(game.getGameTurnTick())) unit.notifyOrdersChanged();
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final CWidget target) {
		return null;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget point) {
		return null;
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		final int playerIndex = orderId & 0xFF; // TODO this is stupid, and should be passed as some "acting player" arg
		final int itemIndex = ((orderId & 0xFF00) >> 8) - 1;
		final CAbilityNeutralBuilding neutralBuildingData = caster.getNeutralBuildingData();
		if ((itemIndex >= 0) && (itemIndex < this.itemsSold.size())) {
			final War3ID itemTypeId = this.itemsSold.get(itemIndex);
			final CItemType itemType = game.getItemData().getItemType(itemTypeId);
			if (itemType == null || this.itemStock.count(itemTypeId) <= 0) return null;
			final CUnit buyer = neutralBuildingData == null ? null : neutralBuildingData.getSelectedPlayerUnit(playerIndex);
			final CAbilityInventory inventory = buyer == null ? null : buyer.getInventoryData();
			if (buyer != null && inventory == null) return null;
			if (!game.getPlayer(playerIndex).charge(itemType.getGoldCost(), itemType.getLumberCost())) return null;
			this.itemStock.sold(itemTypeId, game.getGameTurnTick());
			final CItem item = game.createItem(itemTypeId, caster.getX(), caster.getY());
			if (inventory != null) inventory.giveItem(game, buyer, item, false);
			caster.fireItemSaleEvents(game, buyer, item);
			caster.notifyOrdersChanged();
		}
		return null;
	}

	@Override
	public <T> T visit(final CAbilityVisitor<T> visitor) {
		return visitor.accept(this);
	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int orderId) {
	}

	@Override
	public void onSetUnitType(final CSimulation game, final CUnit cUnit) {
		final CUnitType unitType = cUnit.getUnitType();
		this.itemsSold.clear();
		this.itemsSold.addAll(unitType.getItemsSold());
		this.itemStock.clear();
		loadObjectStock(game);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit cUnit) {
	}

	@Override
	public boolean isPhysical() {
		return false;
	}

	@Override
	public boolean isUniversal() {
		return false;
	}

	@Override
	public CAbilityCategory getAbilityCategory() {
		return CAbilityCategory.CORE;
	}
}
