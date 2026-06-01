package com.etheller.warsmash.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringKeyTest {

	// -----------------------------------------------------------------------
	// equals contract
	// -----------------------------------------------------------------------

	@Test
	void equalsSelf() {
		final StringKey key = new StringKey("hfoo");
		assertTrue(key.equals(key));
	}

	@Test
	void equalsNull() {
		assertFalse(new StringKey("hfoo").equals(null));
	}

	@Test
	void equalsDifferentType() {
		assertFalse(new StringKey("hfoo").equals("hfoo"));
	}

	@Test
	void equalsCaseInsensitive() {
		assertTrue(new StringKey("hfoo").equals(new StringKey("HFOO")));
		assertTrue(new StringKey("HFOO").equals(new StringKey("hfoo")));
		assertTrue(new StringKey("HfOo").equals(new StringKey("hFoO")));
	}

	@Test
	void equalsDistinctKeys() {
		assertFalse(new StringKey("hfoo").equals(new StringKey("hpea")));
	}

	@Test
	void equalsNullBothSides() {
		assertTrue(new StringKey(null).equals(new StringKey(null)));
	}

	@Test
	void equalsNullVsNonNull() {
		// null key must not equal any non-null key
		assertFalse(new StringKey(null).equals(new StringKey("hfoo")));
	}

	@Test
	void equalsNonNullVsNull() {
		// non-null key must not equal null-string key (no NPE)
		assertFalse(new StringKey("hfoo").equals(new StringKey(null)));
	}

	// -----------------------------------------------------------------------
	// hashCode contract: equal objects must have equal hash codes
	// -----------------------------------------------------------------------

	@Test
	void hashCodeConsistentWithEquals() {
		final StringKey a = new StringKey("hfoo");
		final StringKey b = new StringKey("HFOO");
		assertTrue(a.equals(b));
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void hashCodeBothNull() {
		assertEquals(new StringKey(null).hashCode(), new StringKey(null).hashCode());
	}

	@Test
	void hashCodeStable() {
		final StringKey key = new StringKey("hpea");
		final int first = key.hashCode();
		assertEquals(first, key.hashCode(), "hashCode must be stable across calls");
	}

	// -----------------------------------------------------------------------
	// getString preserves original case
	// -----------------------------------------------------------------------

	@Test
	void getStringPreservesCase() {
		assertEquals("HFoo", new StringKey("HFoo").getString());
	}

	@Test
	void getStringNullRoundTrip() {
		assertNull(new StringKey(null).getString());
	}
}
