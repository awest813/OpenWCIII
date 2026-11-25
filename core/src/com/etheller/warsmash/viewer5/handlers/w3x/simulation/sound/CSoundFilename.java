package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.util.function.LongSupplier;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.warsmash.viewer5.AudioBufferSource;
import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.AudioPanner;
import com.etheller.warsmash.viewer5.gl.Extensions;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

public class CSoundFilename implements CSound {
	private final Sound sound;
	private final boolean looping;
	private final boolean stopWhenOutOfRange;
	private final int fadeInRate;
	private final int fadeOutRate;
	private final AudioContext audioContext;
	private final LongSupplier clock;
	private float x;
	private float y;
	private float z;
	private float volume = 1.0f;
	private float pitch = 1.0f;
	private float minDistance = 99999;
	private Float maxDistance;
	private boolean spatial = true;
	private static final java.util.Map<CSoundFilename, Boolean> ACTIVE = new java.util.WeakHashMap<>();
	private static int lastMasterVolume = -1;
	private static boolean lastPositionalAudio;
	private static boolean lastAmbientSounds;
	private static boolean lastMovementSounds;
	private static boolean lastUnitSounds;
	private float distanceCutoff = 99999;
	private final String eaxSetting;
	// These fields describe the local voice. JASS playback queries are journaled during checkpoint replay.
	private transient long playbackTimestamp;
	private transient float playbackPositionSeconds;
	private transient long lastSoundInstanceId = -1;
	private transient boolean playing;
	private CSoundVolumeGroup volumeGroup;
	private transient float volumeGroupScale = 1;
	private transient AudioPanner activePanner;
	private transient long fadeStartTime;
	private transient int fadeDuration;
	private transient float fadeStartGain = 1;
	private transient float fadeTargetGain = 1;

	public CSoundFilename(final Sound sound, final AudioContext audioContext, final boolean looping,
			final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate, final String eaxSetting) {
		this(sound, audioContext, looping, stopWhenOutOfRange, fadeInRate, fadeOutRate, eaxSetting, TimeUtils::millis);
	}

	CSoundFilename(final Sound sound, final AudioContext audioContext, final boolean looping,
			final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate, final String eaxSetting,
			final LongSupplier clock) {
		this.sound = sound;
		this.audioContext = audioContext;
		this.clock = clock;
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
		panner.setDistances(attenuationDistance(), this.minDistance);
		panner.setDistanceCutoff(this.distanceCutoff);
		panner.connect(this.audioContext.destination);

		// Source.
		source.buffer = this.sound;
		source.spatial = this.spatial;
		source.connect(panner);

		// Make a sound.
		final long now = this.clock.getAsLong();
		this.fadeStartTime = now;
		this.fadeDuration = Math.max(0, this.fadeInRate);
		this.fadeStartGain = this.fadeDuration > 0 ? 0 : 1;
		this.fadeTargetGain = 1;
		this.lastSoundInstanceId = source.start(0, this.volume, this.pitch, this.looping,
				this.volumeGroupScale * preferenceGain() * this.fadeStartGain);
		this.playing = this.lastSoundInstanceId != -1;
		if (this.playing) {
			this.activePanner = panner;
			ACTIVE.put(this, Boolean.TRUE);
		}

		this.playbackTimestamp = now;
		this.playbackPositionSeconds = 0;
	}

	@Override
	public void stop() {
		stopVoice();
		ACTIVE.remove(this);
	}

	@Override
	public void stop(final boolean fadeOut) {
		if (!fadeOut || this.fadeOutRate <= 0 || !isPlaying()) {
			stop();
		}
		else if (this.fadeTargetGain != 0) {
			final long now = this.clock.getAsLong();
			this.fadeStartGain = fadeGainAt(now);
			this.fadeStartTime = now;
			this.fadeDuration = this.fadeOutRate;
			this.fadeTargetGain = 0;
		}
	}

	private void stopVoice() {
		if (this.sound != null && this.lastSoundInstanceId != -1) this.sound.stop(this.lastSoundInstanceId);
		this.playing = false;
		this.lastSoundInstanceId = -1;
		this.activePanner = null;
		this.fadeDuration = 0;
		this.fadeTargetGain = 1;
	}

	@Override
	public void setVolume(final int volume) {
		// WC3 uses 0-127 range; normalize to 0.0-1.0
		setNormalizedVolume(Math.max(0, Math.min(127, volume)) / 127.0f);
	}

	public void setNormalizedVolume(final float volume) {
		this.volume = Float.isFinite(volume) ? Math.max(0, Math.min(1, volume)) : 0;
		updateGain();
	}

	@Override
	public void setVolumeGroup(final CSoundVolumeGroup group) {
		this.volumeGroup = group;
		updateGain();
	}

	@Override
	public void setVolumeGroupScale(final float scale) {
		if (this.volumeGroupScale != scale) {
			this.volumeGroupScale = scale;
			updateGain();
		}
	}

	private void updateGain() {
		if (this.sound != null && this.lastSoundInstanceId != -1) {
			this.sound.setVolume(this.lastSoundInstanceId,
					AudioBufferSource.effectiveVolume(this.volume * this.volumeGroupScale * preferenceGain()
							* fadeGainAt(this.clock.getAsLong())));
		}
	}

	private float preferenceGain() {
		final var options = OptionsSettingsStore.get();
		if (this.volumeGroup == CSoundVolumeGroup.AMBIENTSOUNDS && !options.isAmbientSounds()
				|| this.volumeGroup == CSoundVolumeGroup.UNITMOVEMENT && !options.isMovementSounds()
				|| this.volumeGroup == CSoundVolumeGroup.UNITSOUNDS && !options.isUnitSounds()) return 0;
		return 1;
	}

	private float fadeGainAt(final long now) {
		if (this.fadeDuration <= 0) return this.fadeTargetGain;
		final float progress = Math.min(1, Math.max(0, (now - this.fadeStartTime) / (float) this.fadeDuration));
		return this.fadeStartGain + (this.fadeTargetGain - this.fadeStartGain) * progress;
	}

	@Override
	public void setPitch(final float pitch) {
		final float newPitch = Float.isFinite(pitch) ? Math.max(0.01f, Math.min(4, pitch)) : 1;
		if (isPlaying()) {
			final long now = this.clock.getAsLong();
			this.playbackPositionSeconds = positionAt(now);
			this.playbackTimestamp = now;
			this.pitch = newPitch;
			this.sound.setPitch(this.lastSoundInstanceId, this.pitch);
		}
		else {
			this.pitch = newPitch;
		}
	}

	@Override
	public void setPlayPosition(final int milliseconds) {
		final float seconds = milliseconds / 1000f;
		if (milliseconds >= 0 && isPlaying() && seconds < getBufferDuration()
				&& Extensions.audio.seek(this.sound, this.lastSoundInstanceId, seconds)) {
			this.playbackPositionSeconds = seconds;
			this.playbackTimestamp = this.clock.getAsLong();
		}
	}

	@Override
	public void setPosition(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		if (this.activePanner != null) this.activePanner.setPosition(x, y, z);
		if (this.lastSoundInstanceId != -1 && Extensions.audio != null) {
			Extensions.audio.setPosition(this.sound, this.lastSoundInstanceId, x, y, z,
					this.spatial && this.audioContext.listener.is3DSupported()
							&& OptionsSettingsStore.get().isPositionalAudio(),
					attenuationDistance(), this.minDistance);
		}
	}

	@Override
	public void setDistanceCutoff(final float cutoff) {
		this.distanceCutoff = cutoff;
		if (this.activePanner != null) this.activePanner.setDistanceCutoff(cutoff);
		if (this.maxDistance == null) setPosition(this.x, this.y, this.z);
	}

	public void setSpatial(final boolean spatial) { this.spatial = spatial; }
	@Override
	public void setMinDistance(final float distance) {
		this.minDistance = distance;
		setPosition(this.x, this.y, this.z);
	}
	@Override
	public void setMaxDistance(final float distance) {
		this.maxDistance = distance;
		setPosition(this.x, this.y, this.z);
	}

	private float attenuationDistance() {
		return this.maxDistance == null ? this.distanceCutoff : this.maxDistance;
	}

	public long getPlaybackId() {
		return this.lastSoundInstanceId;
	}

	/** Render-thread refresh; weak ownership does not retain discarded script sound handles. */
	public static void refreshSettings() {
		final var options = OptionsSettingsStore.get();
		final int master = options.getEffectiveSoundVolume();
		final boolean positional = options.isPositionalAudio();
		final boolean settingsChanged = master != lastMasterVolume || options.isAmbientSounds() != lastAmbientSounds
				|| options.isMovementSounds() != lastMovementSounds || options.isUnitSounds() != lastUnitSounds;
		final boolean positionalChanged = positional != lastPositionalAudio;
		lastMasterVolume = master;
		lastPositionalAudio = positional;
		lastAmbientSounds = options.isAmbientSounds();
		lastMovementSounds = options.isMovementSounds();
		lastUnitSounds = options.isUnitSounds();
		final var entries = ACTIVE.keySet().iterator();
		while (entries.hasNext()) {
			final CSoundFilename sound = entries.next();
			if (!sound.isPlaying()) entries.remove();
			else {
				if (settingsChanged || sound.fadeDuration > 0) sound.updateGain();
				if (positionalChanged) sound.setPosition(sound.x, sound.y, sound.z);
				if (sound.fadeTargetGain == 1 && sound.fadeDuration > 0
						&& sound.clock.getAsLong() - sound.fadeStartTime >= sound.fadeDuration) {
					sound.fadeDuration = 0;
				}
			}
		}
	}

	@Override
	public boolean isPlaying() {
		if (!this.playing) {
			return false;
		}
		if (this.stopWhenOutOfRange && this.spatial && this.activePanner.listener.is3DSupported()
				&& !this.activePanner.isWithinListenerDistance()) {
			stopVoice();
			return false;
		}
		final long now = this.clock.getAsLong();
		if (this.fadeTargetGain == 0 && now - this.fadeStartTime >= this.fadeDuration) {
			stopVoice();
			return false;
		}
		if (!this.looping && positionAt(now) >= getBufferDuration()) {
			this.playing = false;
			return false;
		}
		return true;
	}

	@Override
	public float getPredictedDuration() {
		return getBufferDuration() / this.pitch;
	}

	@Override
	public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
		if (!isPlaying()) return 0;
		final long now = this.clock.getAsLong();
		final float natural = Math.max(0, getBufferDuration() - positionAt(now)) / this.pitch;
		if (this.fadeTargetGain != 0) return natural;
		final float fade = Math.max(0, this.fadeDuration - (now - this.fadeStartTime)) / 1000f;
		return this.looping ? fade : Math.min(natural, fade);
	}

	private float positionAt(final long timestamp) {
		return this.playbackPositionSeconds + (timestamp - this.playbackTimestamp) / 1000f * this.pitch;
	}

	private float getBufferDuration() {
		return this.sound == null || Extensions.audio == null ? 0 : Extensions.audio.getDuration(this.sound);
	}
}
