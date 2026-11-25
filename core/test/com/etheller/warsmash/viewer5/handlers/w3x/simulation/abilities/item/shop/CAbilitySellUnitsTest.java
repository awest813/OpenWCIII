package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.RetailSimulationTestSupport;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CWidgetEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import mpq.MPQArchive;

class CAbilitySellUnitsTest {

	@Test
	void testStockTracking() {
		final List<War3ID> initialUnits = new ArrayList<>();
		final War3ID footman = War3ID.fromString("hfoo");
		final War3ID rifleman = War3ID.fromString("hrif");
		initialUnits.add(footman);

		final CAbilitySellUnits ability = new CAbilitySellUnits(1, initialUnits);
		assertEquals(1, ability.getUnitsSold().size());
		assertTrue(ability.getUnitsSold().contains(footman));

		// Not in stock map yet -> returns Integer.MAX_VALUE
		assertEquals(Integer.MAX_VALUE, ability.getUnitStockCurrent(footman));

		// Add rifleman with stock 2, max 5
		ability.addUnitToStock(rifleman, 2, 5);
		assertEquals(2, ability.getUnitsSold().size());
		assertTrue(ability.getUnitsSold().contains(rifleman));
		assertEquals(2, ability.getUnitStockCurrent(rifleman));

		// Update footman stock to 3, max 3
		ability.addUnitToStock(footman, 3, 3);
		assertEquals(3, ability.getUnitStockCurrent(footman));

		// Remove footman
		ability.removeUnitFromStock(footman);
		assertEquals(1, ability.getUnitsSold().size());
		assertFalse(ability.getUnitsSold().contains(footman));
		assertEquals(Integer.MAX_VALUE, ability.getUnitStockCurrent(footman));
	}

	@Test
	void originalOrcLaboratorySellsTwoZeppelinsAndRestocksEachIndependently() throws Exception {
		withOriginalOrcMap(game -> {
			final War3ID laboratory = War3ID.fromString("nC00");
			final War3ID zeppelin = War3ID.fromString("nzep");
			final var lab = game.createUnitSimple(laboratory, 2, 0, 0, 0);
			final var separateLab = game.createUnitSimple(laboratory, 2, 832, 0, 0);
			final var buyer = game.createUnitSimple(War3ID.fromString("Ofar"), 0, -220, 0, 0);
			final var player = game.getPlayer(0);
			player.setGold(1200);
			player.setLumber(500);
			player.setFoodCap(20);
			game.update();
			game.update();
			assertEquals(buyer, lab.getNeutralBuildingData().getSelectedPlayerUnit(0));
			final var sales = lab.getFirstAbilityOfType(CAbilitySellUnits.class);
			final var otherSales = separateLab.getFirstAbilityOfType(CAbilitySellUnits.class);
			assertNotNull(sales);
			assertNotNull(otherSales);
			assertEquals(2, sales.getUnitStockCurrent(zeppelin));
			final int orderId = (sales.getUnitsSold().indexOf(zeppelin) + 1) << 8;
			final var orders = game.getDefaultPlayerUnitOrderExecutor(0);
			orders.issueImmediateOrder(lab.getHandleId(), sales.getHandleId(), orderId, false);
			orders.issueImmediateOrder(lab.getHandleId(), sales.getHandleId(), orderId, false);
			assertEquals(0, sales.getUnitStockCurrent(zeppelin));
			assertEquals(2, otherSales.getUnitStockCurrent(zeppelin));
			assertEquals(2, game.getUnitsIncludingPending().stream().filter(unit -> zeppelin.equals(unit.getTypeId())
					&& unit.getPlayerIndex() == 0).count());
			assertEquals(1200 - 2 * game.getUnitData().getUnitType(zeppelin).getGoldCost(), player.getGold());
			final var activation = new BooleanAbilityActivationReceiver();
			sales.checkCanUse(game, lab, orderId, activation);
			assertFalse(activation.isOk());
			final int rechargeTick = game.getGameTurnTick() + Math.round(5 / WarsmashConstants.SIMULATION_STEP_TIME);
			advanceTo(game, rechargeTick - 1);
			assertEquals(0, sales.getUnitStockCurrent(zeppelin));
			advanceTo(game, rechargeTick + 1);
			assertEquals(1, sales.getUnitStockCurrent(zeppelin));
			sales.checkCanUse(game, lab, orderId, activation.reset());
			assertTrue(activation.isOk());
		});
	}

	@Test
	void originalGoblinMerchantDelaysAndReplenishesItsItemStock() throws Exception {
		withOriginalOrcMap(game -> {
			final War3ID merchant = War3ID.fromString("ngme");
			final War3ID boots = War3ID.fromString("bspd");
			final War3ID scroll = War3ID.fromString("stwp");
			final var shop = game.createUnitSimple(merchant, 2, 0, 0, 0);
			final var separateShop = game.createUnitSimple(merchant, 2, 832, 0, 0);
			final var buyer = game.createUnitSimple(War3ID.fromString("Ofar"), 0, -220, 0, 0);
			final var player = game.getPlayer(0);
			player.setGold(1200);
			advanceTo(game, 2);
			assertEquals(buyer, shop.getNeutralBuildingData().getSelectedPlayerUnit(0));
			final var sales = shop.getFirstAbilityOfType(CAbilitySellItems.class);
			final var otherSales = separateShop.getFirstAbilityOfType(CAbilitySellItems.class);
			assertNotNull(sales);
			assertNotNull(otherSales);
			assertEquals(1, sales.getItemStockCurrent(boots));
			assertEquals(0, sales.getItemStockCurrent(scroll));
			final var seller = game.getPlayer(shop.getPlayerIndex());
			final List<CommonTriggerExecutionScope> saleEvents = new ArrayList<>();
			shop.addEvent(new CWidgetEvent(game.getGlobalScope(), shop, new Trigger(), JassGameEventsWar3.EVENT_UNIT_SELL_ITEM, null) {
				@Override
				public void fire(final CWidget triggerWidget, final TriggerExecutionScope scope) {
					assertSame(shop, triggerWidget);
					saleEvents.add((CommonTriggerExecutionScope) scope);
				}
			});
			seller.addEvent(new CPlayerEvent(game.getGlobalScope(), seller, new Trigger(), JassGameEventsWar3.EVENT_PLAYER_UNIT_SELL_ITEM, null) {
				@Override
				public void fire(final CUnit unit, final TriggerExecutionScope scope) {
					assertSame(shop, unit);
					saleEvents.add((CommonTriggerExecutionScope) scope);
				}
			});
			final int orderId = (sales.getItemsSold().indexOf(boots) + 1) << 8;
			final var orders = game.getDefaultPlayerUnitOrderExecutor(0);
			orders.issueImmediateOrder(shop.getHandleId(), sales.getHandleId(), orderId, false);
			final var purchased = buyer.getInventoryData().getItemInSlot(0);
			assertEquals(boots, purchased.getTypeId());
			assertEquals(List.of(JassGameEventsWar3.EVENT_UNIT_SELL_ITEM, JassGameEventsWar3.EVENT_PLAYER_UNIT_SELL_ITEM),
					saleEvents.stream().map(CommonTriggerExecutionScope::getTriggerEventId).toList());
			for (final CommonTriggerExecutionScope scope : saleEvents) {
				assertSame(shop, scope.getTriggeringUnit());
				assertSame(shop, scope.getSellingUnit());
				assertSame(seller, scope.getTriggeringPlayer());
				assertSame(buyer, scope.getBuyingUnit());
				assertSame(purchased, scope.getSoldItem());
			}
			assertEquals(0, sales.getItemStockCurrent(boots));
			assertEquals(1, otherSales.getItemStockCurrent(boots));
			final int goldAfterPurchase = 1200 - game.getItemData().getItemType(boots).getGoldCost();
			final var unavailable = assertThrows(AssertionError.class,
					() -> orders.issueImmediateOrder(shop.getHandleId(), sales.getHandleId(), orderId, false));
			assertTrue(unavailable.getMessage().contains(CommandStringErrorKeys.OUT_OF_STOCK));
			assertEquals(2, saleEvents.size());
			assertEquals(goldAfterPurchase, player.getGold());
			final int rechargeTick = game.getGameTurnTick() + Math.round(60 / WarsmashConstants.SIMULATION_STEP_TIME);
			advanceTo(game, rechargeTick - 1);
			assertEquals(0, sales.getItemStockCurrent(boots));
			advanceTo(game, rechargeTick + 1);
			assertEquals(1, sales.getItemStockCurrent(boots));
			final int availabilityTick = Math.round(440 / WarsmashConstants.SIMULATION_STEP_TIME);
			advanceTo(game, availabilityTick - 1);
			assertEquals(0, sales.getItemStockCurrent(scroll));
			advanceTo(game, availabilityTick + 1);
			assertEquals(2, sales.getItemStockCurrent(scroll));
		});
	}

	private static void advanceTo(final CSimulation game, final int tick) {
		while (game.getGameTurnTick() < tick) game.update();
	}

	private static void withOriginalOrcMap(final Consumer<CSimulation> test) throws Exception {
		final var previousRaceManager = WarsmashConstants.RACE_MANAGER;
		final var archives = new ArrayList<MpqDataSource>();
		try {
			for (final String name : List.of("war3.mpq", "War3x.mpq", "War3xlocal.mpq")) {
				final var channel = Files.newByteChannel(RetailTestData.require(name));
				archives.add(new MpqDataSource(new MPQArchive(channel), channel));
			}
			final var source = new CompoundDataSource(new ArrayList<>(archives));
			try (var map = new War3Map(source, "Maps\\Campaign\\Orc03.w3m")) {
				test.accept(RetailSimulationTestSupport.simulation(map));
			}
		}
		finally {
			WarsmashConstants.RACE_MANAGER = previousRaceManager;
			for (final MpqDataSource archive : archives) archive.close();
		}
	}
}
