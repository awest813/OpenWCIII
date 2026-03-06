package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CGameCacheTest {

	private CGameCache cache;

	@BeforeEach
	void setUp() {
		this.cache = new CGameCache("testcache.w3v");
	}

	// ---- handle identity ----

	@Test
	void handleIdIsPositive() {
		assertTrue(this.cache.getHandleId() > 0);
	}

	@Test
	void distinctCachesHaveDistinctHandleIds() {
		final CGameCache other = new CGameCache("other.w3v");
		assertNotEquals(this.cache.getHandleId(), other.getHandleId());
	}

	@Test
	void cacheNameIsPreserved() {
		assertEquals("testcache.w3v", this.cache.getName());
	}

	// ---- integer ----

	@Test
	void storedIntegerIsRetrievedCorrectly() {
		this.cache.storeInteger("heroes", "arthas_level", 5);
		assertEquals(5, this.cache.getStoredInteger("heroes", "arthas_level"));
	}

	@Test
	void missingIntegerReturnsZero() {
		assertEquals(0, this.cache.getStoredInteger("heroes", "nonexistent"));
	}

	@Test
	void haveStoredIntegerReturnsTrueAfterStore() {
		this.cache.storeInteger("heroes", "arthas_level", 3);
		assertTrue(this.cache.haveStoredInteger("heroes", "arthas_level"));
	}

	@Test
	void haveStoredIntegerReturnsFalseBeforeStore() {
		assertFalse(this.cache.haveStoredInteger("heroes", "not_stored"));
	}

	@Test
	void flushStoredIntegerRemovesValue() {
		this.cache.storeInteger("heroes", "arthas_level", 5);
		this.cache.flushStoredInteger("heroes", "arthas_level");
		assertFalse(this.cache.haveStoredInteger("heroes", "arthas_level"));
		assertEquals(0, this.cache.getStoredInteger("heroes", "arthas_level"));
	}

	// ---- real ----

	@Test
	void storedRealIsRetrievedCorrectly() {
		this.cache.storeReal("heroes", "hp_pct", 0.75f);
		assertEquals(0.75f, this.cache.getStoredReal("heroes", "hp_pct"), 1e-6f);
	}

	@Test
	void missingRealReturnsZero() {
		assertEquals(0f, this.cache.getStoredReal("heroes", "nonexistent"), 1e-6f);
	}

	@Test
	void haveStoredRealReturnsTrueAfterStore() {
		this.cache.storeReal("heroes", "hp_pct", 1.0f);
		assertTrue(this.cache.haveStoredReal("heroes", "hp_pct"));
	}

	@Test
	void flushStoredRealRemovesValue() {
		this.cache.storeReal("heroes", "hp_pct", 0.5f);
		this.cache.flushStoredReal("heroes", "hp_pct");
		assertFalse(this.cache.haveStoredReal("heroes", "hp_pct"));
	}

	// ---- boolean ----

	@Test
	void storedBooleanIsRetrievedCorrectly() {
		this.cache.storeBoolean("flags", "mission_complete", true);
		assertTrue(this.cache.getStoredBoolean("flags", "mission_complete"));
	}

	@Test
	void missingBooleanReturnsFalse() {
		assertFalse(this.cache.getStoredBoolean("flags", "nonexistent"));
	}

	@Test
	void haveStoredBooleanReturnsTrueAfterStore() {
		this.cache.storeBoolean("flags", "unlocked", false);
		assertTrue(this.cache.haveStoredBoolean("flags", "unlocked"));
	}

	@Test
	void flushStoredBooleanRemovesValue() {
		this.cache.storeBoolean("flags", "unlocked", true);
		this.cache.flushStoredBoolean("flags", "unlocked");
		assertFalse(this.cache.haveStoredBoolean("flags", "unlocked"));
	}

	// ---- string ----

	@Test
	void storedStringIsRetrievedCorrectly() {
		this.cache.storeString("heroes", "name", "Arthas");
		assertEquals("Arthas", this.cache.getStoredString("heroes", "name"));
	}

	@Test
	void missingStringReturnsEmpty() {
		assertEquals("", this.cache.getStoredString("heroes", "nonexistent"));
	}

	@Test
	void haveStoredStringReturnsTrueAfterStore() {
		this.cache.storeString("heroes", "name", "Arthas");
		assertTrue(this.cache.haveStoredString("heroes", "name"));
	}

	@Test
	void flushStoredStringRemovesValue() {
		this.cache.storeString("heroes", "name", "Arthas");
		this.cache.flushStoredString("heroes", "name");
		assertFalse(this.cache.haveStoredString("heroes", "name"));
	}

	// ---- mission flush ----

	@Test
	void flushStoredMissionRemovesAllTypesForMission() {
		this.cache.storeInteger("mission1", "level", 5);
		this.cache.storeReal("mission1", "hp", 0.9f);
		this.cache.storeBoolean("mission1", "done", true);
		this.cache.storeString("mission1", "hero", "Arthas");

		// mission2 data should survive
		this.cache.storeInteger("mission2", "level", 3);

		this.cache.flushStoredMission("mission1");

		assertFalse(this.cache.haveStoredInteger("mission1", "level"));
		assertFalse(this.cache.haveStoredReal("mission1", "hp"));
		assertFalse(this.cache.haveStoredBoolean("mission1", "done"));
		assertFalse(this.cache.haveStoredString("mission1", "hero"));

		// mission2 data is unaffected
		assertTrue(this.cache.haveStoredInteger("mission2", "level"));
	}

	// ---- flush all ----

	@Test
	void flushAllClearsEverything() {
		this.cache.storeInteger("m1", "k1", 1);
		this.cache.storeString("m2", "k2", "hello");

		this.cache.flushAll();

		assertFalse(this.cache.haveStoredInteger("m1", "k1"));
		assertFalse(this.cache.haveStoredString("m2", "k2"));
	}

	// ---- overwrite ----

	@Test
	void storedIntegerCanBeOverwritten() {
		this.cache.storeInteger("heroes", "level", 3);
		this.cache.storeInteger("heroes", "level", 7);
		assertEquals(7, this.cache.getStoredInteger("heroes", "level"));
	}

	// ---- multiple mission keys are independent ----

	@Test
	void differentMissionKeysDontInterfere() {
		this.cache.storeInteger("mission_a", "x", 10);
		this.cache.storeInteger("mission_b", "x", 20);

		assertEquals(10, this.cache.getStoredInteger("mission_a", "x"));
		assertEquals(20, this.cache.getStoredInteger("mission_b", "x"));
	}
}
