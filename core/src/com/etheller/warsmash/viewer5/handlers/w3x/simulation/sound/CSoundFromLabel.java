package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;

public class CSoundFromLabel implements CSound {

	private final UnitSound sound;
	private final boolean looping;
	private final boolean is3d;
	private final boolean stopWhenOutOfRange;
	private final int fadeInRate;
	private final int fadeOutRate;
	private final AudioContext audioContext;

	public CSoundFromLabel(final UnitSound sound, final AudioContext audioContext, final boolean looping,
			final boolean is3d, final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate) {
		this.sound = sound;
		this.audioContext = audioContext;
		this.looping = looping;
		this.is3d = is3d;
		this.stopWhenOutOfRange = stopWhenOutOfRange;
		this.fadeInRate = fadeInRate;
		this.fadeOutRate = fadeOutRate;
		this.playback = sound.createPlayback(audioContext, looping, is3d, stopWhenOutOfRange, fadeInRate, fadeOutRate);
	}

	// Variant selection and pitch randomization belong to the local audio voice.
	private transient CSoundFilename playback;
	private transient float volumeGroupScale = 1;
	private Integer volume;
	private Float pitch;
	private Float minDistance;
	private Float maxDistance;
	private Float distanceCutoff;
	private float x, y, z;

	@Override
	public void start() {
		stop();
		if (this.playback == null) {
			this.playback = this.sound.createPlayback(this.audioContext, this.looping, this.is3d,
					this.stopWhenOutOfRange, this.fadeInRate, this.fadeOutRate);
		}
		if (this.playback == null) return;
		if (this.volume != null) this.playback.setVolume(this.volume);
		if (this.pitch != null) this.playback.setPitch(this.pitch);
		if (this.minDistance != null) this.playback.setMinDistance(this.minDistance);
		if (this.maxDistance != null) this.playback.setMaxDistance(this.maxDistance);
		if (this.distanceCutoff != null) this.playback.setDistanceCutoff(this.distanceCutoff);
		this.playback.setVolumeGroupScale(this.volumeGroupScale);
		this.playback.setPosition(this.x, this.y, this.z);
		this.playback.start();
	}

	@Override public void stop() { if (this.playback != null) this.playback.stop(); }
	@Override public void stop(final boolean fadeOut) { if (this.playback != null) this.playback.stop(fadeOut); }
	@Override public void setVolume(final int volume) {
		this.volume = volume;
		if (this.playback != null) this.playback.setVolume(volume);
	}
	@Override public void setPitch(final float pitch) {
		this.pitch = pitch;
		if (this.playback != null) this.playback.setPitch(pitch);
	}
	@Override public void setPosition(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		if (this.playback != null) this.playback.setPosition(x, y, z);
	}
	@Override public void setMinDistance(final float distance) {
		this.minDistance = distance;
		if (this.playback != null) this.playback.setMinDistance(distance);
	}
	@Override public void setMaxDistance(final float distance) {
		this.maxDistance = distance;
		if (this.playback != null) this.playback.setMaxDistance(distance);
	}
	@Override public void setDistanceCutoff(final float cutoff) {
		this.distanceCutoff = cutoff;
		if (this.playback != null) this.playback.setDistanceCutoff(cutoff);
	}
	@Override public void setPlayPosition(final int milliseconds) {
		if (this.playback != null) this.playback.setPlayPosition(milliseconds);
	}
	@Override public void setVolumeGroupScale(final float scale) {
		this.volumeGroupScale = scale;
		if (this.playback != null) this.playback.setVolumeGroupScale(scale);
	}
	@Override public boolean isPlaying() { return this.playback != null && this.playback.isPlaying(); }
	@Override public float getPredictedDuration() { return this.playback == null ? 0 : this.playback.getPredictedDuration(); }
	@Override public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
		return this.playback == null ? 0 : this.playback.getRemainingTimeToPlayOnTheDesyncLocalComputer();
	}
}
