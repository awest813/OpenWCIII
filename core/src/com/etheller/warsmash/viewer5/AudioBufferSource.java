package com.etheller.warsmash.viewer5;

import com.badlogic.gdx.audio.Sound;
import com.etheller.warsmash.viewer5.gl.Extensions;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

public class AudioBufferSource {
	public Sound buffer;
	public boolean spatial = true;
	private AudioPanner panner;

	public void connect(final AudioPanner panner) {
		this.panner = panner;
	}

	public long start(final int value, final float volume, final float pitch, final boolean looping) {
		final float gain = effectiveVolume(volume);
		if (gain <= 0 || this.panner == null || Extensions.audio == null) return -1;
		if (this.buffer != null) {
			if (!this.spatial || !this.panner.listener.is3DSupported() || this.panner.isWithinListenerDistance()) {
				return Extensions.audio.play(this.buffer, gain, pitch, this.panner.x, this.panner.y, this.panner.z,
						this.spatial && this.panner.listener.is3DSupported() && OptionsSettingsStore.get().isPositionalAudio(), this.panner.maxDistance, this.panner.refDistance,
						looping);
			}
		}
		return -1;
	}
	public static float effectiveVolume(final float volume) {
		if (!Float.isFinite(volume)) return 0;
		return Math.max(0, Math.min(1, volume)) * OptionsSettingsStore.get().getEffectiveSoundVolume() / 100f;
	}
}
