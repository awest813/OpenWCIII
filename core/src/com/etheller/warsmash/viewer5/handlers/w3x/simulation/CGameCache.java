package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.etheller.interpreter.ast.util.CHandle;

/**
 * In-memory gamecache for campaign hero carry-over and inter-map persistent
 * state. Mirrors the WC3 gamecache handle behaviour: values are keyed by
 * (missionKey, key) string pairs and stored per type (integer, real, boolean,
 * string). Persistence to/from disk is not yet implemented; the cache lives
 * for the lifetime of the engine process.
 */
public final class CGameCache implements CHandle {
	private static final AtomicInteger HANDLE_ID_COUNTER = new AtomicInteger(0);

	private final String name;
	private final int handleId;
	private final Map<String, Map<String, Integer>> integers = new HashMap<>();
	private final Map<String, Map<String, Float>> reals = new HashMap<>();
	private final Map<String, Map<String, Boolean>> booleans = new HashMap<>();
	private final Map<String, Map<String, String>> strings = new HashMap<>();

	public CGameCache(final String name) {
		this.name = name;
		this.handleId = HANDLE_ID_COUNTER.incrementAndGet();
	}

	@Override
	public int getHandleId() {
		return this.handleId;
	}

	public String getName() {
		return this.name;
	}

	// ---- integer ----

	public void storeInteger(final String missionKey, final String key, final int value) {
		getOrCreate(this.integers, missionKey).put(key, value);
	}

	public int getStoredInteger(final String missionKey, final String key) {
		final Map<String, Integer> mission = this.integers.get(missionKey);
		if (mission == null) {
			return 0;
		}
		final Integer v = mission.get(key);
		return v != null ? v : 0;
	}

	public boolean haveStoredInteger(final String missionKey, final String key) {
		final Map<String, Integer> mission = this.integers.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredInteger(final String missionKey, final String key) {
		final Map<String, Integer> mission = this.integers.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- real ----

	public void storeReal(final String missionKey, final String key, final float value) {
		getOrCreate(this.reals, missionKey).put(key, value);
	}

	public float getStoredReal(final String missionKey, final String key) {
		final Map<String, Float> mission = this.reals.get(missionKey);
		if (mission == null) {
			return 0f;
		}
		final Float v = mission.get(key);
		return v != null ? v : 0f;
	}

	public boolean haveStoredReal(final String missionKey, final String key) {
		final Map<String, Float> mission = this.reals.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredReal(final String missionKey, final String key) {
		final Map<String, Float> mission = this.reals.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- boolean ----

	public void storeBoolean(final String missionKey, final String key, final boolean value) {
		getOrCreate(this.booleans, missionKey).put(key, value);
	}

	public boolean getStoredBoolean(final String missionKey, final String key) {
		final Map<String, Boolean> mission = this.booleans.get(missionKey);
		if (mission == null) {
			return false;
		}
		final Boolean v = mission.get(key);
		return v != null ? v : false;
	}

	public boolean haveStoredBoolean(final String missionKey, final String key) {
		final Map<String, Boolean> mission = this.booleans.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredBoolean(final String missionKey, final String key) {
		final Map<String, Boolean> mission = this.booleans.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- string ----

	public void storeString(final String missionKey, final String key, final String value) {
		getOrCreate(this.strings, missionKey).put(key, value != null ? value : "");
	}

	public String getStoredString(final String missionKey, final String key) {
		final Map<String, String> mission = this.strings.get(missionKey);
		if (mission == null) {
			return "";
		}
		final String v = mission.get(key);
		return v != null ? v : "";
	}

	public boolean haveStoredString(final String missionKey, final String key) {
		final Map<String, String> mission = this.strings.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredString(final String missionKey, final String key) {
		final Map<String, String> mission = this.strings.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- mission flush ----

	/**
	 * Removes all values for a given missionKey across all types.
	 */
	public void flushStoredMission(final String missionKey) {
		this.integers.remove(missionKey);
		this.reals.remove(missionKey);
		this.booleans.remove(missionKey);
		this.strings.remove(missionKey);
	}

	/**
	 * Removes all values across all mission keys and types.
	 */
	public void flushAll() {
		this.integers.clear();
		this.reals.clear();
		this.booleans.clear();
		this.strings.clear();
	}

	private static <V> Map<String, V> getOrCreate(final Map<String, Map<String, V>> parent, final String key) {
		Map<String, V> child = parent.get(key);
		if (child == null) {
			child = new HashMap<>();
			parent.put(key, child);
		}
		return child;
	}
}
