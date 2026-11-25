package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;

public interface CSound {
	void start();

	void stop();

	default void stop(final boolean fadeOut) {
		stop();
	}

	float getPredictedDuration();

	float getRemainingTimeToPlayOnTheDesyncLocalComputer();

	void setVolume(int volume);

	void setPitch(float pitch);

	void setPosition(float x, float y, float z);

	default void setPlayPosition(int milliseconds) { }

	default void setMinDistance(float distance) { }

	default void setMaxDistance(float distance) { }

	default void setDistanceCutoff(float cutoff) { }

	default void setVolumeGroupScale(float scale) { }

	default void setVolumeGroup(CSoundVolumeGroup group) { }

	boolean isPlaying();
}
