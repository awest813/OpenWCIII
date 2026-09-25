package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CinematicPresentationStateTest {

	@Test
	void skyModelIsTrackedAndNormalized() {
		final CinematicPresentationState state = new CinematicPresentationState();
		assertEquals("", state.getSkyModel());

		state.setSkyModel("Environment\\Sky\\SkyModel.mdx");
		assertEquals("Environment\\Sky\\SkyModel.mdx", state.getSkyModel());

		state.setSkyModel("Environment/Sky/OtherSky.mdx");
		assertEquals("Environment\\Sky\\OtherSky.mdx", state.getSkyModel());

		state.setSkyModel(null);
		assertEquals("", state.getSkyModel());
	}

	@Test
	void cinematicCameraIsTracked() {
		final CinematicPresentationState state = new CinematicPresentationState();
		state.setCinematicCamera("Cameras\\Intro.mdx");
		assertEquals("Cameras\\Intro.mdx", state.getCinematicCamera());

		state.setCinematicCamera(null);
		assertEquals("", state.getCinematicCamera());
	}

	@Test
	void introShotsAreTracked() {
		final CinematicPresentationState state = new CinematicPresentationState();
		state.setIntroShotText("The Scourge of Lordaeron");
		state.setIntroShotModel("UI\\IntroShot.mdx");
		assertEquals("The Scourge of Lordaeron", state.getIntroShotText());
		assertEquals("UI\\IntroShot.mdx", state.getIntroShotModel());

		state.setIntroShotText(null);
		state.setIntroShotModel(null);
		assertEquals("", state.getIntroShotText());
		assertEquals("", state.getIntroShotModel());
	}

	@Test
	void modelCinematicLifecycle() {
		final CinematicPresentationState state = new CinematicPresentationState();
		assertFalse(state.isModelCinematicPlaying());

		state.startModelCinematic("Cinematic\\HumanIntro.mdx");
		assertTrue(state.isModelCinematicPlaying());
		assertEquals("Cinematic\\HumanIntro.mdx", state.getModelCinematic());

		// Empty path never reports playing (matches PlayModelCinematic("") no-op).
		state.startModelCinematic("");
		assertFalse(state.isModelCinematicPlaying());
		assertEquals("", state.getModelCinematic());

		state.startModelCinematic("Cinematic\\OrcIntro.mdx");
		assertTrue(state.isModelCinematicPlaying());
		state.endModelCinematic();
		assertFalse(state.isModelCinematicPlaying());
		assertEquals("", state.getModelCinematic());
	}

	@Test
	void resetClearsEverything() {
		final CinematicPresentationState state = new CinematicPresentationState();
		state.setSkyModel("Sky.mdx");
		state.setCinematicCamera("Cam.mdx");
		state.setIntroShotText("text");
		state.setIntroShotModel("model.mdx");
		state.startModelCinematic("cine.mdx");

		state.reset();

		assertEquals("", state.getSkyModel());
		assertEquals("", state.getCinematicCamera());
		assertEquals("", state.getIntroShotText());
		assertEquals("", state.getIntroShotModel());
		assertEquals("", state.getModelCinematic());
		assertFalse(state.isModelCinematicPlaying());
	}
}
