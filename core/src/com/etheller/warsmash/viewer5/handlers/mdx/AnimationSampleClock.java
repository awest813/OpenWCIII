package com.etheller.warsmash.viewer5.handlers.mdx;

/** Limits skeletal track sampling only; animation/event time still advances every frame. */
public final class AnimationSampleClock {
	private float elapsed;
	private int lastSequence = Integer.MIN_VALUE;
	private int lastQuality = -1;

	public boolean advance(final float dt, final int sequence, final int quality, final boolean forced) {
		this.elapsed += Math.max(0, dt);
		if (forced || sequence != this.lastSequence || quality != this.lastQuality || quality >= 2) {
			this.lastSequence = sequence;
			this.lastQuality = quality;
			this.elapsed = 0;
			return true;
		}
		final float interval = quality == 0 ? 1f / 15f : 1f / 30f;
		if (this.elapsed + 0.000001f >= interval) {
			this.elapsed = Math.max(0, this.elapsed - interval);
			this.elapsed %= interval;
			return true;
		}
		return false;
	}
}
