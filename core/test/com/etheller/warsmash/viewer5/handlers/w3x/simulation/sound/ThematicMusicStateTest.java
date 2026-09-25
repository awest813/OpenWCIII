package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThematicMusicStateTest {

	@Test
	void startsEmptyAndIdle() {
		final ThematicMusicState state = new ThematicMusicState();
		assertEquals("", state.getTrack());
		assertFalse(state.isPlaying());
		assertEquals(0, state.getPlayPositionMs());
	}

	@Test
	void playTracksLabelPositionAndFade() {
		final ThematicMusicState state = new ThematicMusicState();
		state.playThematic("Sound\\Music\\OrcTheme.mp3", 15000, 2000);
		assertEquals("Sound\\Music\\OrcTheme.mp3", state.getTrack());
		assertTrue(state.isPlaying());
		assertEquals(15000, state.getPlayPositionMs());
		assertEquals(2000, state.getFadeInMs());
	}

	@Test
	void negativePositionAndFadeClampToZero() {
		final ThematicMusicState state = new ThematicMusicState();
		state.playThematic("Sound\\Music\\HumanTheme.mp3", -500, -1);
		assertTrue(state.isPlaying());
		assertEquals(0, state.getPlayPositionMs());
		assertEquals(0, state.getFadeInMs());
	}

	@Test
	void nullTrackReportsNotPlaying() {
		final ThematicMusicState state = new ThematicMusicState();
		state.playThematic(null, 0, 0);
		assertEquals("", state.getTrack());
		assertFalse(state.isPlaying());
	}

	@Test
	void playPositionOnlyAppliesWhilePlaying() {
		final ThematicMusicState state = new ThematicMusicState();
		state.setPlayPosition(30000);
		assertEquals(0, state.getPlayPositionMs());

		state.playThematic("Sound\\Music\\NightElfTheme.mp3", 0, 0);
		state.setPlayPosition(30000);
		assertEquals(30000, state.getPlayPositionMs());
	}

	@Test
	void endClearsLayer() {
		final ThematicMusicState state = new ThematicMusicState();
		state.playThematic("Sound\\Music\\UndeadTheme.mp3", 5000, 1000);
		assertTrue(state.isPlaying());

		state.endThematic();
		assertEquals("", state.getTrack());
		assertFalse(state.isPlaying());
		assertEquals(0, state.getPlayPositionMs());
		assertEquals(0, state.getFadeInMs());
	}

	@Test
	void resetClearsLayer() {
		final ThematicMusicState state = new ThematicMusicState();
		state.playThematic("Sound\\Music\\OrcTheme.mp3", 1000, 500);
		state.reset();
		assertEquals("", state.getTrack());
		assertFalse(state.isPlaying());
	}
}
