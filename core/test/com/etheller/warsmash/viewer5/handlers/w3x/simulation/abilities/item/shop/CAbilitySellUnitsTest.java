package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.util.War3ID;

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
}
