package com.etheller.warsmash.viewer5;

import com.etheller.warsmash.viewer5.AudioContext.Listener;

public abstract class AudioPanner {
	public Listener listener;
	public float x;
	public float y;
	public float z;

	public AudioPanner(final Listener listener) {
		this.listener = listener;
	}

	public void setPosition(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setDistances(final float maxDistance, final float refDistance) {
		this.maxDistance = maxDistance;
		this.refDistance = refDistance;
		setDistanceCutoff(maxDistance);
	}

	public void setDistanceCutoff(final float cutoffDistance) {
		this.cutoffDistanceSquared = cutoffDistance * cutoffDistance;
	}

	public float maxDistance;
	public float refDistance;
	private float cutoffDistanceSquared;

	public abstract void connect(AudioDestination destination);

	public boolean isWithinListenerDistance() {
		final float dx = this.listener.getX() - this.x;
		final float dy = this.listener.getY() - this.y;
		final float dz = this.listener.getZ() - this.z;
		return ((dx * dx) + (dy * dy) + (dz * dz)) <= this.cutoffDistanceSquared;
	}
}
