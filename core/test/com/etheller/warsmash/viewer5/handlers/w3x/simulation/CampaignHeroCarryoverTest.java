package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData.StoredAbilityData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData.StoredItemData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;

class CampaignHeroCarryoverTest {

	@TempDir
	File tmpDir;

	@Test
	void testGameCacheSerializationRoundTrip() throws IOException {
		final CGameCache cache = new CGameCache("CampaignData.w3v");

		// Primitives
		cache.storeInteger("Mission1", "KillCount", 123);
		cache.storeReal("Mission1", "TimeElapsed", 456.78f);
		cache.storeBoolean("Mission1", "ObjectiveComplete", true);
		cache.storeString("Mission1", "SecretCode", "Frostmourne");

		// StoredUnitData: Hero Arthas
		final War3ID heroTypeId = War3ID.fromString("Hpal");
		final StoredItemData[] items = new StoredItemData[6];
		items[0] = new StoredItemData(War3ID.fromString("phea"), 3); // Potion of Healing, 3 charges
		items[1] = new StoredItemData(War3ID.fromString("bspd"), 1); // Boots of Speed
		items[4] = new StoredItemData(War3ID.fromString("stwp"), 1); // Scroll of Town Portal

		final StoredAbilityData[] abilities = new StoredAbilityData[3];
		abilities[0] = new StoredAbilityData(War3ID.fromString("AHhb"), 2); // Holy Light level 2
		abilities[1] = new StoredAbilityData(War3ID.fromString("AHds"), 1); // Divine Shield level 1
		abilities[2] = new StoredAbilityData(War3ID.fromString("AHad"), 1); // Devotion Aura level 1

		final StoredUnitData arthas = new StoredUnitData(
				heroTypeId,
				2500, // XP (level 3)
				1,    // 1 unspent skill point
				24, 15, 17, // base str, agi, int
				2, 0, 0,    // bonus str, agi, int
				"Arthas",
				items,
				abilities
		);
		cache.storeUnit("HumanCampaign", "Arthas", arthas);

		// Save to disk
		final File file = new File(this.tmpDir, "CampaignData.w3v");
		cache.save(file);
		assertTrue(file.exists());
		assertTrue(file.length() > 0);

		// Reload from disk
		final CGameCache loaded = CGameCache.tryLoadFromFile(file, "CampaignData.w3v");
		assertNotNull(loaded);
		assertEquals("CampaignData.w3v", loaded.getName());

		// Verify primitives
		assertEquals(123, loaded.getStoredInteger("Mission1", "KillCount"));
		assertEquals(456.78f, loaded.getStoredReal("Mission1", "TimeElapsed"), 1e-4f);
		assertTrue(loaded.getStoredBoolean("Mission1", "ObjectiveComplete"));
		assertEquals("Frostmourne", loaded.getStoredString("Mission1", "SecretCode"));

		// Verify unit
		assertTrue(loaded.haveStoredUnit("HumanCampaign", "Arthas"));
		final StoredUnitData restoredArthas = loaded.getStoredUnit("HumanCampaign", "Arthas");
		assertNotNull(restoredArthas);
		assertEquals(heroTypeId, restoredArthas.unitTypeId);
		assertEquals(2500, restoredArthas.xp);
		assertEquals(1, restoredArthas.skillPoints);
		assertEquals(24, restoredArthas.strengthBase);
		assertEquals(15, restoredArthas.agilityBase);
		assertEquals(17, restoredArthas.intelligenceBase);
		assertEquals(2, restoredArthas.strengthBonus);
		assertEquals(0, restoredArthas.agilityBonus);
		assertEquals(0, restoredArthas.intelligenceBonus);
		assertEquals("Arthas", restoredArthas.properName);

		// Verify inventory
		assertNotNull(restoredArthas.items);
		assertEquals(6, restoredArthas.items.length);
		assertNotNull(restoredArthas.items[0]);
		assertEquals(War3ID.fromString("phea"), restoredArthas.items[0].typeId);
		assertEquals(3, restoredArthas.items[0].charges);
		assertNotNull(restoredArthas.items[1]);
		assertEquals(War3ID.fromString("bspd"), restoredArthas.items[1].typeId);
		assertEquals(1, restoredArthas.items[1].charges);
		assertNotNull(restoredArthas.items[4]);
		assertEquals(War3ID.fromString("stwp"), restoredArthas.items[4].typeId);
		assertEquals(1, restoredArthas.items[4].charges);

		// Verify learned abilities
		assertNotNull(restoredArthas.abilities);
		assertEquals(3, restoredArthas.abilities.length);
		assertEquals(War3ID.fromString("AHhb"), restoredArthas.abilities[0].abilityId);
		assertEquals(2, restoredArthas.abilities[0].level);
		assertEquals(War3ID.fromString("AHds"), restoredArthas.abilities[1].abilityId);
		assertEquals(1, restoredArthas.abilities[1].level);
		assertEquals(War3ID.fromString("AHad"), restoredArthas.abilities[2].abilityId);
		assertEquals(1, restoredArthas.abilities[2].level);
	}

	@Test
	void testGameCacheFlushOperations() {
		final CGameCache cache = new CGameCache("Test.w3v");
		cache.storeInteger("M1", "A", 10);
		cache.storeReal("M1", "B", 20f);
		cache.storeUnit("M1", "Hero", new StoredUnitData(War3ID.fromString("Hpal"), 0, 0, 0, 0, 0, 0, 0, 0, "Test", null, null));

		assertTrue(cache.haveStoredMission("M1"));
		assertTrue(cache.haveStoredInteger("M1", "A"));
		assertTrue(cache.haveStoredReal("M1", "B"));
		assertTrue(cache.haveStoredUnit("M1", "Hero"));

		cache.flushStoredInteger("M1", "A");
		assertFalse(cache.haveStoredInteger("M1", "A"));

		cache.flushStoredUnit("M1", "Hero");
		assertFalse(cache.haveStoredUnit("M1", "Hero"));

		assertTrue(cache.haveStoredMission("M1")); // "B" still present
		cache.flushStoredMission("M1");
		assertFalse(cache.haveStoredMission("M1"));
	}

	@Test
	void testWar3MapConfigDefaultDifficulty() {
		final War3MapConfig config = new War3MapConfig(12);
		assertEquals(CMapDifficulty.NORMAL, config.getGameDifficulty());

		config.setGameDifficulty(CMapDifficulty.HARD);
		assertEquals(CMapDifficulty.HARD, config.getGameDifficulty());

		config.setGameDifficulty(CMapDifficulty.EASY);
		assertEquals(CMapDifficulty.EASY, config.getGameDifficulty());
	}
}
