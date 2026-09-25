package com.etheller.warsmash.viewer5.handlers.w3x.camera;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class CinematicCameraPlayerTest {

	@Test
	void zeroOffsetsYieldBase() {
		final float[] outPos = new float[3];
		final float[] outTgt = new float[3];
		CinematicCameraPlayer.evaluateTrack(new float[] { 100f, 200f, 300f }, new float[] { 0f, 0f, 50f },
				new float[] { 0f, 0f, 0f }, new float[] { 0f, 0f, 0f }, outPos, outTgt);
		assertArrayEquals(new float[] { 100f, 200f, 300f }, outPos, 1e-6f);
		assertArrayEquals(new float[] { 0f, 0f, 50f }, outTgt, 1e-6f);
	}

	@Test
	void offsetsAddToBase() {
		final float[] outPos = new float[3];
		final float[] outTgt = new float[3];
		CinematicCameraPlayer.evaluateTrack(new float[] { 1000f, 0f, 500f }, new float[] { 0f, 0f, 100f },
				new float[] { -250f, 125.5f, 0f }, new float[] { 10f, -20f, 5f }, outPos, outTgt);
		assertArrayEquals(new float[] { 750f, 125.5f, 500f }, outPos, 1e-6f);
		assertArrayEquals(new float[] { 10f, -20f, 105f }, outTgt, 1e-6f);
	}

	@Test
	void doesNotMutateInputs() {
		final float[] base = new float[] { 1f, 2f, 3f };
		final float[] target = new float[] { 4f, 5f, 6f };
		final float[] posOff = new float[] { 7f, 8f, 9f };
		final float[] tgtOff = new float[] { 10f, 11f, 12f };
		final float[] outPos = new float[3];
		final float[] outTgt = new float[3];
		CinematicCameraPlayer.evaluateTrack(base, target, posOff, tgtOff, outPos, outTgt);
		assertArrayEquals(new float[] { 1f, 2f, 3f }, base, 1e-6f);
		assertArrayEquals(new float[] { 4f, 5f, 6f }, target, 1e-6f);
		assertArrayEquals(new float[] { 8f, 10f, 12f }, outPos, 1e-6f);
		assertArrayEquals(new float[] { 14f, 16f, 18f }, outTgt, 1e-6f);
	}
}
