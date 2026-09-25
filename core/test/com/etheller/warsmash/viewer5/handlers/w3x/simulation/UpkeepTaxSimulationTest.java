package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfigPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;

class UpkeepTaxSimulationTest {

	private static final class TestPlayerStateListener implements CPlayerStateListener {
		int upkeepChanges = 0;
		int foodChanges = 0;
		int goldChanges = 0;
		int lumberChanges = 0;

		@Override
		public void goldChanged() {
			this.goldChanges++;
		}

		@Override
		public void lumberChanged() {
			this.lumberChanges++;
		}

		@Override
		public void foodChanged() {
			this.foodChanges++;
		}

		@Override
		public void upkeepChanged() {
			this.upkeepChanges++;
		}

		@Override
		public void heroDeath() {
		}

		@Override
		public void heroTokensChanged() {
		}
	}

	@Test
	void testDefaultFoodUpkeepTransitions() {
		final CPlayer player = new CPlayer(null, new float[] { 0, 0 }, new War3MapConfigPlayer(0), null);
		final TestPlayerStateListener listener = new TestPlayerStateListener();
		player.addStateListener(listener);

		// Default state: 0 food -> 0% upkeep (No Upkeep)
		assertEquals(0, player.getGoldUpkeepRate());

		// 50 food -> Still 0% upkeep (No Upkeep)
		player.setFoodUsed(50);
		assertEquals(0, player.getGoldUpkeepRate());
		assertEquals(0, listener.upkeepChanges);

		// 51 food -> 30% upkeep (Low Upkeep)
		player.setFoodUsed(51);
		assertEquals(30, player.getGoldUpkeepRate());
		assertEquals(1, listener.upkeepChanges);

		// 80 food -> Still 30% upkeep (Low Upkeep)
		player.setFoodUsed(80);
		assertEquals(30, player.getGoldUpkeepRate());
		assertEquals(1, listener.upkeepChanges);

		// 81 food -> 60% upkeep (High Upkeep)
		player.setFoodUsed(81);
		assertEquals(60, player.getGoldUpkeepRate());
		assertEquals(2, listener.upkeepChanges);

		// Drop back down to 40 food -> 0% upkeep
		player.setFoodUsed(40);
		assertEquals(0, player.getGoldUpkeepRate());
		assertEquals(3, listener.upkeepChanges);
	}

	@Test
	void testPlayerStateUpkeepOverride() {
		final CPlayer player = new CPlayer(null, new float[] { 0, 0 }, new War3MapConfigPlayer(0), null);
		final TestPlayerStateListener listener = new TestPlayerStateListener();
		player.addStateListener(listener);

		player.setPlayerState(CPlayerState.GOLD_UPKEEP_RATE, 45);
		assertEquals(45, player.getGoldUpkeepRate());
		assertEquals(1, listener.upkeepChanges);

		player.setPlayerState(CPlayerState.LUMBER_UPKEEP_RATE, 20);
		assertEquals(20, player.getLumberUpkeepRate());
		assertEquals(2, listener.upkeepChanges);
	}

	@Test
	void testUpkeepTaxMath() {
		final CPlayer player = new CPlayer(null, new float[] { 0, 0 }, new War3MapConfigPlayer(0), null);
		player.setGold(100);

		// No Upkeep (0% tax): 10 mined -> 10 gained
		int goldMined = 10;
		int goldTax = Math.round(goldMined * (player.getGoldUpkeepRate() / 100.0f));
		int goldGained = goldMined - goldTax;
		player.addGold(goldGained);
		assertEquals(110, player.getGold());

		// Low Upkeep (30% tax): 10 mined -> 7 gained
		player.setFoodUsed(60);
		assertEquals(30, player.getGoldUpkeepRate());
		goldTax = Math.round(goldMined * (player.getGoldUpkeepRate() / 100.0f));
		goldGained = goldMined - goldTax;
		player.addGold(goldGained);
		assertEquals(117, player.getGold());

		// High Upkeep (60% tax): 10 mined -> 4 gained
		player.setFoodUsed(90);
		assertEquals(60, player.getGoldUpkeepRate());
		goldTax = Math.round(goldMined * (player.getGoldUpkeepRate() / 100.0f));
		goldGained = goldMined - goldTax;
		player.addGold(goldGained);
		assertEquals(121, player.getGold());
	}
}
