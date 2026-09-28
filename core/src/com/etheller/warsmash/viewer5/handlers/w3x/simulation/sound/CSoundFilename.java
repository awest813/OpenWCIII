package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.warsmash.viewer5.AudioBufferSource;
import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.AudioPanner;
import com.etheller.warsmash.viewer5.gl.Extensions;

public class CSoundFilename implements CSound {
	private final Sound sound;
	private final boolean looping;
	private final boolean stopWhenOutOfRange;
	private final int fadeInRate;
	private final int fadeOutRate;
	private final AudioContext audioContext;
	private float x;
	private float y;
	private float z;
	private float volume = 1.0f;
	private float pitch = 1.0f;
	private float minDistance = 99999;
	private boolean spatial = true;
	private static final java.util.Map<CSoundFilename, Boolean> ACTIVE = new java.util.WeakHashMap<>();
	private static int lastMasterVolume = -1;
	private static boolean lastPositionalAudio;
	private float distanceCutoff = 99999;
	private final String eaxSetting;
	private long lastStartTimestamp;
	private long lastSoundInstanceId = -1;
	private boolean playing = false;

	public CSoundFilename(final Sound sound, final AudioContext audioContext, final boolean looping,
			final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate, final String eaxSetting) {
		this.sound = sound;
		this.audioContext = audioContext;
		this.looping = looping;
		this.stopWhenOutOfRange = stopWhenOutOfRange;
		this.fadeInRate = fadeInRate;
		this.fadeOutRate = fadeOutRate;
		this.eaxSetting = eaxSetting;
	}

	@Override
	public void start() {
		stop();
		if (this.audioContext == null) {
			return;
		}
		final AudioPanner panner = this.audioContext.createPanner(this.stopWhenOutOfRange);
		final AudioBufferSource source = this.audioContext.createBufferSource();

		// Panner settings
		panner.setPosition(this.x, this.y, this.z);
		panner.setDistances(this.distanceCutoff, this.minDistance);
		panner.connect(this.audioContext.destination);

		// Source.
		source.buffer = this.sound;
		source.spatial = this.spatial;
		source.connect(panner);

		// Make a sound.
		this.lastSoundInstanceId = source.start(0, this.volume, this.pitch, this.looping);
		this.playing = this.lastSoundInstanceId != -1;
		if (this.playing) ACTIVE.put(this, Boolean.TRUE);

		this.lastStartTimestamp = TimeUtils.millis();
	}

	@Override
	public void stop() {
		if (this.sound != null) {
			if (this.lastSoundInstanceId != -1) {
				this.sound.stop(this.lastSoundInstanceId);
			}
		}
		this.playing = false;
		this.lastSoundInstanceId = -1;
		ACTIVE.remove(this);
	}

	@Override
	public void setVolume(final int volume) {
		// WC3 uses 0-127 range; normalize to 0.0-1.0
		setNormalizedVolume(Math.max(0, Math.min(127, volume)) / 127.0f);
	}

	public void setNormalizedVolume(final float volume) {
		this.volume = Float.isFinite(volume) ? Math.max(0, Math.min(1, volume)) : 0;
		if (this.sound != null && this.lastSoundInstanceId != -1) {
			this.sound.setVolume(this.lastSoundInstanceId, AudioBufferSource.effectiveVolume(this.volume));
		}
	}

	@Override
	public void setPitch(final float pitch) {
		this.pitch = Float.isFinite(pitch) ? Math.max(0.01f, Math.min(4, pitch)) : 1;
		if (this.sound != null && this.lastSoundInstanceId != -1) {
			this.sound.setPitch(this.lastSoundInstanceId, this.pitch);
		}
	}

	@Override
	public void setPosition(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		if (this.lastSoundInstanceId != -1 && Extensions.audio != null) {
			Extensions.audio.setPosition(this.sound, this.lastSoundInstanceId, x, y, z,
					this.spatial && this.audioContext.listener.is3DSupported()
							&& com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore.get().isPositionalAudio(),
					this.distanceCutoff, this.minDistance);
		}
	}

	public void setDistanceCutoff(final float cutoff) {
		this.distanceCutoff = cutoff;
	}

	public void setSpatial(final boolean spatial) { this.spatial = spatial; }
	public void setMinDistance(final float distance) { this.minDistance = distance; }

	/** Render-thread refresh; weak ownership does not retain discarded script sound handles. */
	public static void refreshSettings() {
		final int master = com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore.get().getEffectiveSoundVolume();
		final boolean positional = com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore.get().isPositionalAudio();
		if (master == lastMasterVolume && positional == lastPositionalAudio) return;
		lastMasterVolume = master;
		lastPositionalAudio = positional;
		final var entries = ACTIVE.keySet().iterator();
		while (entries.hasNext()) {
			final CSoundFilename sound = entries.next();
			if (!sound.isPlaying()) entries.remove();
			else {
				sound.setNormalizedVolume(sound.volume);
				sound.setPosition(sound.x, sound.y, sound.z);
			}
		}
	}

	@Override
	public boolean isPlaying() {
		if (!this.playing) {
			return false;
		}
		final long currentTime = TimeUtils.millis();
		final long deltaTime = currentTime - this.lastStartTimestamp;
		final float deltaTimeSeconds = deltaTime / 1000f;
		if (!this.looping && (deltaTimeSeconds >= getPredictedDuration())) {
			this.playing = false;
			return false;
		}
		return true;
	}

	@Override
	public float getPredictedDuration() {
		return this.sound == null || Extensions.audio == null ? 0 : Extensions.audio.getDuration(this.sound) / this.pitch;
	}

	@Override
	public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
		if (!isPlaying()) return 0;
		final long currentTime = TimeUtils.millis();
		final long deltaTime = currentTime - this.lastStartTimestamp;
		final float deltaTimeSeconds = deltaTime / 1000f;
		final float predictedDuration = getPredictedDuration();
		if (deltaTimeSeconds > predictedDuration) {
			return 0;
		}
		return predictedDuration - deltaTimeSeconds;
	}

}
