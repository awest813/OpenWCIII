package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.AbstractCAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilitySellUnits extends AbstractCAbility {
	private final List<War3ID> unitsSold;
	private final Map<Integer, int[]> unitStock = new HashMap<>();

	public CAbilitySellUnits(final int handleId, final List<War3ID> unitsSold) {
		super(handleId, War3ID.fromString("Asyu"));
		this.unitsSold = new ArrayList<>(unitsSold);
	}

	public List<War3ID> getUnitsSold() {
		return this.unitsSold;
	}

	public void addUnitToStock(final War3ID unitId, final int currentStock, final int stockMax) {
		if (!this.unitsSold.contains(unitId)) {
			this.unitsSold.add(unitId);
		}
		this.unitStock.put(unitId.getValue(), new int[] { currentStock, stockMax });
	}

	public void removeUnitFromStock(final War3ID unitId) {
		this.unitsSold.remove(unitId);
		this.unitStock.remove(unitId.getValue());
	}

	public int getUnitStockCurrent(final War3ID unitId) {
		final int[] counts = this.unitStock.get(unitId.getValue());
		return counts == null ? Integer.MAX_VALUE : counts[0];
	}

	private void recordSale(final War3ID unitId) {
		final int[] counts = this.unitStock.get(unitId.getValue());
		if (counts != null) {
			counts[0] = Math.max(0, counts[0] - 1);
		}
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		final int playerIndex = orderId & 0xFF;
		final int unitIndex = ((orderId & 0xFF00) >> 8) - 1;
		if ((unitIndex >= 0) && (unitIndex < this.unitsSold.size())) {
			final War3ID unitTypeId = this.unitsSold.get(unitIndex);
			final CUnitType unitType = game.getUnitData().getUnitType(unitTypeId);
			if (unitType != null) {
				final CPlayer player = game.getPlayer(playerIndex);
				final boolean isHeroType = unitType.isHero();
				final boolean hasResources = (isHeroType && (player.getHeroTokens() > 0))
						|| ((player.getGold() >= unitType.getGoldCost())
								&& (player.getLumber() >= unitType.getLumberCost()));
				if (!hasResources) {
					if (player.getGold() < unitType.getGoldCost()) {
						receiver.activationCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_GOLD);
					}
					else {
						receiver.activationCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_LUMBER);
					}
					return;
				}
				if ((player.getFoodUsed() + unitType.getFoodUsed()) > player.getFoodCap()) {
					receiver.activationCheckFailed(CommandStringErrorKeys.NOT_ENOUGH_FOOD);
					return;
				}
				if (getUnitStockCurrent(unitTypeId) <= 0) {
					receiver.activationCheckFailed(CommandStringErrorKeys.OUT_OF_STOCK);
					return;
				}
				receiver.useOk();
			}
			else {
				receiver.useOk();
			}
		}
		else {
			receiver.useOk();
		}
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
		final int unitIndex = ((orderId & 0xFF00) >> 8) - 1;
		if ((unitIndex >= 0) && (unitIndex < this.unitsSold.size())) {
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
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
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
		final int playerIndex = orderId & 0xFF;
		final int unitIndex = ((orderId & 0xFF00) >> 8) - 1;
		if ((unitIndex >= 0) && (unitIndex < this.unitsSold.size())) {
			final War3ID unitTypeId = this.unitsSold.get(unitIndex);
			final CUnitType unitType = game.getUnitData().getUnitType(unitTypeId);
			if (unitType != null) {
				final CPlayer player = game.getPlayer(playerIndex);
				final boolean isHeroType = unitType.isHero();
				final boolean chargeOk;
				if (isHeroType && (player.getHeroTokens() > 0)) {
					player.setHeroTokens(player.getHeroTokens() - 1);
					chargeOk = true;
				}
				else {
					chargeOk = player.charge(unitType.getGoldCost(), unitType.getLumberCost());
				}
				if (chargeOk) {
					final CUnit hiredUnit = game.createUnit(unitTypeId, playerIndex, caster.getX(), caster.getY(),
							game.getGameplayConstants().getBuildingAngle());
					hiredUnit.setFoodUsed(unitType.getFoodUsed());
					player.setUnitFoodMade(hiredUnit, unitType.getFoodMade());
					player.addTechtreeUnlocked(game, unitTypeId);
					recordSale(unitTypeId);
					caster.dispatchSpawnedUnit(game, hiredUnit);
				}
			}
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
		this.unitsSold.clear();
		this.unitsSold.addAll(unitType.getUnitsSold());
		this.unitStock.clear();
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
