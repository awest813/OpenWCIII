package com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes;

import com.etheller.interpreter.ast.util.CHandle;

public enum CGameSpeed implements CHandle {
	SLOWEST(0.5f),
	SLOW(0.75f),
	NORMAL(1),
	FAST(1.25f),
	FASTEST(1.5f);

	private final float timeScale;

	CGameSpeed(final float timeScale) {
		this.timeScale = timeScale;
	}

	public float getTimeScale() {
		return this.timeScale;
	}

	public static CGameSpeed[] VALUES = values();

	@Override
	public int getHandleId() {
		return ordinal();
	}
}
