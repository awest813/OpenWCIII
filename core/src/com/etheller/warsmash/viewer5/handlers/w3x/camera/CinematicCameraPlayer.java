package com.etheller.warsmash.viewer5.handlers.w3x.camera;

/**
 * Pure track math for MDX cinematic cameras (JASS {@code SetCinematicCamera}).
 *
 * <p>An MDX camera contributes a static base (position / target) plus animated
 * offsets sampled from its {@code KCTR} / {@code KTTR} timelines for the
 * instance's current sequence, frame and counter. This helper combines them;
 * callers sample the offsets via
 * {@code com.etheller.warsmash.viewer5.handlers.mdx.Camera} and drive a
 * {@code com.etheller.warsmash.viewer5.Camera} with the result. Kept
 * LibGDX-free so it can be unit tested headlessly.</p>
 */
public final class CinematicCameraPlayer {
	private CinematicCameraPlayer() {
	}

	/**
	 * Combines a camera base with its sampled track offsets.
	 *
	 * @param basePosition  camera base position (length &ge; 3)
	 * @param baseTarget    camera base target (length &ge; 3)
	 * @param positionOffset sampled {@code KCTR} offset, may be all zeros (length &ge; 3)
	 * @param targetOffset  sampled {@code KTTR} offset, may be all zeros (length &ge; 3)
	 * @param outPosition   receives world position (length &ge; 3)
	 * @param outTarget     receives world target (length &ge; 3)
	 */
	public static void evaluateTrack(final float[] basePosition, final float[] baseTarget,
			final float[] positionOffset, final float[] targetOffset, final float[] outPosition,
			final float[] outTarget) {
		outPosition[0] = basePosition[0] + positionOffset[0];
		outPosition[1] = basePosition[1] + positionOffset[1];
		outPosition[2] = basePosition[2] + positionOffset[2];
		outTarget[0] = baseTarget[0] + targetOffset[0];
		outTarget[1] = baseTarget[1] + targetOffset[1];
		outTarget[2] = baseTarget[2] + targetOffset[2];
	}
}
