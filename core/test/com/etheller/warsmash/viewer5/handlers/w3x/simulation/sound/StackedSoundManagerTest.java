package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StackedSoundManagerTest {

	private static final class DummySound implements CSound {
		boolean playing = false;

		@Override
		public void start() {
			this.playing = true;
		}

		@Override
		public void stop() {
			this.playing = false;
		}

		@Override
		public float getPredictedDuration() {
			return 1.0f;
		}

		@Override
		public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
			return 1.0f;
		}

		@Override
		public void setVolume(int volume) {
		}

		@Override
		public void setPitch(float pitch) {
		}

		@Override
		public void setPosition(float x, float y, float z) {
		}

		@Override
		public boolean isPlaying() {
			return this.playing;
		}
	}

	@Test
	void testRegisterAndUnregister() {
		final StackedSoundManager manager = new StackedSoundManager();
		final DummySound sound1 = new DummySound();
		final DummySound sound2 = new DummySound();

		assertEquals(0, manager.getRegisteredCount());
		assertFalse(manager.isRegistered(sound1));

		manager.register(sound1, true, 200f, 200f);
		assertEquals(1, manager.getRegisteredCount());
		assertTrue(manager.isRegistered(sound1));
		assertFalse(manager.isRegistered(sound2));

		// Re-registering updates existing registration without incrementing count
		manager.register(sound1, false, 300f, 300f);
		assertEquals(1, manager.getRegisteredCount());

		manager.register(sound2, false, 100f, 100f);
		assertEquals(2, manager.getRegisteredCount());

		manager.unregister(sound1, false, 0f, 0f);
		assertEquals(1, manager.getRegisteredCount());
		assertFalse(manager.isRegistered(sound1));
		assertTrue(manager.isRegistered(sound2));

		manager.clear();
		assertEquals(0, manager.getRegisteredCount());
	}
}
