package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

/**
 * Headless presentation state for in-map cinematics.
 *
 * <p>Tracks the script-visible cinematic knobs that retail Warcraft III exposes
 * through JASS ({@code SetSkyModel}, {@code SetCinematicCamera},
 * {@code PlayModelCinematic}, {@code SetIntroShotText}/{@code SetIntroShotModel})
 * so campaign scripts keep running with retail-plausible state even before full
 * sky-mesh swaps or MDX camera-track playback exist.</p>
 *
 * <p>Pure Java with no LibGDX dependency so it can be unit tested headlessly.
 * {@code MeleeUI} owns an instance and mirrors these values into its overlay.</p>
 */
public final class CinematicPresentationState {
	private String skyModel = "";
	private String cinematicCamera = "";
	private String introShotText = "";
	private String introShotModel = "";
	private String modelCinematic = "";
	private boolean modelCinematicPlaying;

	/** Normalizes a script path: null becomes "", forward slashes become backslashes. */
	public static String normalize(final String path) {
		if (path == null) {
			return "";
		}
		return path.replace('/', '\\');
	}

	public void setSkyModel(final String modelPath) {
		this.skyModel = normalize(modelPath);
	}

	public String getSkyModel() {
		return this.skyModel;
	}

	public void setCinematicCamera(final String cameraModelFile) {
		this.cinematicCamera = normalize(cameraModelFile);
	}

	public String getCinematicCamera() {
		return this.cinematicCamera;
	}

	public void setIntroShotText(final String text) {
		this.introShotText = text == null ? "" : text;
	}

	public String getIntroShotText() {
		return this.introShotText;
	}

	public void setIntroShotModel(final String modelPath) {
		this.introShotModel = normalize(modelPath);
	}

	public String getIntroShotModel() {
		return this.introShotModel;
	}

	/** Records a model-cinematic start; empty path is ignored and reports not playing. */
	public void startModelCinematic(final String modelPath) {
		final String normalized = normalize(modelPath);
		if (normalized.isEmpty()) {
			this.modelCinematic = "";
			this.modelCinematicPlaying = false;
		}
		else {
			this.modelCinematic = normalized;
			this.modelCinematicPlaying = true;
		}
	}

	public void endModelCinematic() {
		this.modelCinematic = "";
		this.modelCinematicPlaying = false;
	}

	public String getModelCinematic() {
		return this.modelCinematic;
	}

	public boolean isModelCinematicPlaying() {
		return this.modelCinematicPlaying;
	}

	/** Clears all presentation state (useful for tests and map transitions). */
	public void reset() {
		this.skyModel = "";
		this.cinematicCamera = "";
		this.introShotText = "";
		this.introShotModel = "";
		this.modelCinematic = "";
		this.modelCinematicPlaying = false;
	}
}
