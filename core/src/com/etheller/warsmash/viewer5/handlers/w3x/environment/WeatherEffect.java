package com.etheller.warsmash.viewer5.handlers.w3x.environment;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.warsmash.util.War3ID;

public class WeatherEffect implements CHandle {
	private final int handleId;
	private final Rectangle bounds;
	private final War3ID effectId;
	private boolean enabled;

	public WeatherEffect(final int handleId, final Rectangle bounds, final War3ID effectId) {
		this.handleId = handleId;
		this.bounds = bounds != null ? new Rectangle(bounds) : new Rectangle();
		this.effectId = effectId;
		this.enabled = false;
	}

	@Override
	public int getHandleId() {
		return this.handleId;
	}

	public Rectangle getBounds() {
		return this.bounds;
	}

	public War3ID getEffectId() {
		return this.effectId;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}
}
