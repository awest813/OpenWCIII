package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

/**
 * Headless state for the thematic-music layer.
 *
 * <p>Retail Warcraft III keeps thematic music ({@code PlayThematicMusic} /
 * {@code PlayThematicMusicEx} / {@code PlayThematic}) on its own layer above
 * the map-music playlist: starting a thematic track does not rewrite the map
 * default, and {@code EndThematicMusic} / {@code EndThematic} drop back to it.
 * This class tracks that layer (current track, play position, fade intent) so
 * campaign scripts observe the layer state. The audio backend applies the
 * requested fade-in duration during playback.</p>
 *
 * <p>Pure Java with no LibGDX dependency so it can be unit tested headlessly.
 * {@code MeleeUI} owns an instance and drives the real {@code MusicPlayer}
 * alongside it.</p>
 */
public final class ThematicMusicState {
	private String track = "";
	private boolean playing;
	private int playPositionMs;
	private int fadeInMs;

	public void playThematic(final String musicField, final int fromMSecs, final int fadeInMSecs) {
		this.track = musicField == null ? "" : musicField;
		this.playing = !this.track.isEmpty();
		this.playPositionMs = Math.max(0, fromMSecs);
		this.fadeInMs = Math.max(0, fadeInMSecs);
	}

	public void endThematic() {
		this.track = "";
		this.playing = false;
		this.playPositionMs = 0;
		this.fadeInMs = 0;
	}

	public void setPlayPosition(final int millisecs) {
		if (this.playing) {
			this.playPositionMs = Math.max(0, millisecs);
		}
	}

	public String getTrack() {
		return this.track;
	}

	public boolean isPlaying() {
		return this.playing;
	}

	public int getPlayPositionMs() {
		return this.playPositionMs;
	}

	public int getFadeInMs() {
		return this.fadeInMs;
	}

	/** Clears all thematic state (useful for tests and map transitions). */
	public void reset() {
		endThematic();
	}
}
