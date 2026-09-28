package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;

class VideoOptionsTest {
	private static final DisplayMode MODE = new DisplayMode(1920, 1080, 60, 32) { };
	private final List<String> calls = new ArrayList<>();
	private final OptionsSettingsStore saved = new OptionsSettingsStore();
	private final OptionsSettingsStore draft = new OptionsSettingsStore();

	private Graphics graphics(final boolean acceptDisplay) {
		return (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(), new Class<?>[] { Graphics.class },
				(proxy, method, args) -> {
					switch (method.getName()) {
					case "getWidth": return 1280;
					case "getHeight": return 720;
					case "isFullscreen": return false;
					case "getDisplayMode": return MODE;
					case "setWindowedMode":
						calls.add("window " + args[0] + "x" + args[1]);
						return acceptDisplay;
					case "setFullscreenMode":
						assertEquals(MODE, args[0]);
						calls.add("fullscreen");
						return acceptDisplay;
					default: throw new AssertionError(method.getName());
					}
				});
	}

	@Test void unchangedOptionsDoNotTouchDisplayOrRequireGammaSupport() {
		assertNull(VideoOptions.apply(draft, saved, false, graphics(false), null));
		assertTrue(calls.isEmpty());
	}

	@Test void appliesWindowSizeWithoutMutatingSavedPreferences() {
		draft.setWindowSize(1600, 900);
		assertNull(VideoOptions.apply(draft, saved, true, graphics(true), null));
		assertEquals("window 1600x900", calls.get(0));
		assertEquals(0, saved.getWindowWidth());
	}

	@Test void fullscreenUsesDesktopMode() {
		draft.setFullscreen(true);
		assertNull(VideoOptions.apply(draft, saved, true, graphics(true), null));
		assertEquals("fullscreen", calls.get(0));
	}

	@Test void rejectedDisplayDoesNotApplyGamma() {
		draft.setWindowSize(1600, 900);
		draft.setGamma(70);
		assertNotNull(VideoOptions.apply(draft, saved, true, graphics(false), gamma -> {
			throw new AssertionError("Gamma must not change after display rejection");
		}));
		assertEquals(50, saved.getGamma());
	}

	@Test void rejectedGammaRestoresDisplayAndGamma() {
		draft.setFullscreen(true);
		draft.setGamma(100);
		final List<Float> gammaCalls = new ArrayList<>();
		assertNotNull(VideoOptions.apply(draft, saved, true, graphics(true), gamma -> {
			gammaCalls.add(gamma);
			return gamma == 1f;
		}));
		assertEquals(java.util.Arrays.asList("fullscreen", "window 1280x720"), calls);
		assertEquals(java.util.Arrays.asList(2f, 1f), gammaCalls);
	}

	@Test void missingGammaBackendReportsFailureRatherThanSavingANoop() {
		draft.setGamma(80);
		assertNotNull(VideoOptions.apply(draft, saved, false, graphics(true), null));
	}

	@Test void modeChangeReappliesSavedGamma() {
		saved.setGamma(0);
		draft.copyFrom(saved);
		draft.setWindowSize(1600, 900);
		final List<Float> gammaCalls = new ArrayList<>();
		assertNull(VideoOptions.apply(draft, saved, true, graphics(true), gamma -> {
			gammaCalls.add(gamma);
			return true;
		}));
		assertEquals(java.util.Arrays.asList(0.5f), gammaCalls);
	}

	@Test void throwingGammaBackendCannotCrashTheMenuDuringRollback() {
		draft.setFullscreen(true);
		draft.setGamma(80);
		assertNotNull(VideoOptions.apply(draft, saved, true, graphics(true), gamma -> {
			throw new IllegalStateException("Driver unavailable");
		}));
		assertEquals(java.util.Arrays.asList("fullscreen", "window 1280x720"), calls);
	}
	@Test void displayQueryFailureIsReportedWithoutChangingGamma() {
		final Graphics unavailable = (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(),
				new Class<?>[] { Graphics.class }, (proxy, method, args) -> {
					throw new IllegalStateException("Display disconnected");
				});
		assertNotNull(VideoOptions.apply(draft, saved, true, unavailable, value -> {
			throw new AssertionError("No display or gamma changes were attempted");
		}));
		assertNull(VideoOptions.apply(draft, saved, false, unavailable, null));
	}

	@Test void failedSaveRestoresActualWindowAndKeepsCommittedPreferences(
			@org.junit.jupiter.api.io.TempDir final java.nio.file.Path directory) throws Exception {
		final java.nio.file.Path blocker = directory.resolve("not-a-directory");
		java.nio.file.Files.writeString(blocker, "keep");
		draft.setWindowSize(1600, 900);
		draft.setGamma(100);
		draft.setTextureQuality(0);
		final List<Float> gammaCalls = new ArrayList<>();
		assertNotNull(VideoOptions.applyAndSave(draft, saved, true, graphics(true), value -> {
			gammaCalls.add(value);
			return true;
		}, blocker.resolve("options.properties").toFile()));
		assertEquals(java.util.Arrays.asList("window 1600x900", "window 1280x720"), calls);
		assertEquals(java.util.Arrays.asList(2f, 1f), gammaCalls);
		assertEquals(2, saved.getTextureQuality());
		assertEquals(50, saved.getGamma());
		assertEquals("keep", java.nio.file.Files.readString(blocker));
	}

	@Test void successfulSaveCommitsAndPersistsDraft(
			@org.junit.jupiter.api.io.TempDir final java.nio.file.Path directory) {
		draft.setTextureQuality(0);
		final java.io.File file = directory.resolve("options.properties").toFile();
		assertNull(VideoOptions.applyAndSave(draft, saved, false, graphics(false), null, file));
		assertEquals(0, saved.getTextureQuality());
		final OptionsSettingsStore reloaded = new OptionsSettingsStore();
		reloaded.load(file);
		assertEquals(0, reloaded.getTextureQuality());
	}

	@Test void rollbackFailureIsVisibleToTheUser() {
		draft.setGamma(100);
		assertTrue(VideoOptions.apply(draft, saved, false, graphics(true), value -> false)
				.contains("could not be fully restored"));
	}

}
