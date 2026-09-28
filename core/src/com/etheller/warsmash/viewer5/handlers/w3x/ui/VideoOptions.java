package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.etheller.warsmash.viewer5.gl.GammaControl;

/** Applies video drafts without committing preferences when the driver rejects them. */
public final class VideoOptions {
	private VideoOptions() {
	}

	/** Returns an error for the menu, or null on success. Must run on the render thread. */
	public static String apply(final OptionsSettingsStore draft, final OptionsSettingsStore saved,
			final boolean changeDisplay, final Graphics graphics, final GammaControl gamma) {
		return applyAndSave(draft, saved, changeDisplay, graphics, gamma, null);
	}

	/** Applies and persists the draft as one transaction, restoring the actual display on failure. */
	public static String applyAndSave(final OptionsSettingsStore draft, final OptionsSettingsStore saved,
			final boolean changeDisplay, final Graphics graphics, final GammaControl gamma, final File file) {
		DisplayState previousDisplay = null;
		boolean displayAttempted = false;
		boolean gammaAttempted = false;
		try {
			if (changeDisplay) {
				previousDisplay = new DisplayState(graphics);
				displayAttempted = true;
				final boolean applied = draft.isFullscreen() ? graphics.setFullscreenMode(graphics.getDisplayMode())
						: graphics.setWindowedMode(draft.getWindowWidth(), draft.getWindowHeight());
				if (!applied) {
					return "This display mode could not be applied.";
				}
			}
			// Mode changes may reset the driver's gamma ramp.
			if (draft.getGamma() != saved.getGamma()
					|| (changeDisplay && draft.getGamma() != OptionsSettingsStore.DEFAULT_GAMMA)) {
				gammaAttempted = true;
				if (!setGamma(gamma, draft.getDisplayGamma())) {
					return "Gamma adjustment is not supported by this display driver."
							+ rollback(previousDisplay, graphics, gamma, saved, true);
				}
			}
			if (file != null) {
				try {
					draft.save(file);
				}
				catch (final IOException | SecurityException e) {
					System.err.println("Options: failed to save: " + e.getMessage());
					return "The options could not be saved. Please check disk space and file permissions."
							+ rollback(previousDisplay, graphics, gamma, saved, gammaAttempted);
				}
				saved.copyFrom(draft);
			}
			return null;
		}
		catch (final RuntimeException e) {
			System.err.println("Video options: " + e.getMessage());
			return "The display settings could not be applied."
					+ rollback(displayAttempted ? previousDisplay : null, graphics, gamma, saved,
							gammaAttempted || (displayAttempted && saved.getGamma() != OptionsSettingsStore.DEFAULT_GAMMA));
		}
	}

	private static String rollback(final DisplayState display, final Graphics graphics, final GammaControl gamma,
			final OptionsSettingsStore saved, final boolean restoreGamma) {
		boolean restored = display == null || display.restore(graphics);
		if (restoreGamma) restored = setGamma(gamma, saved.getDisplayGamma()) && restored;
		return restored ? "" : " The previous display settings could not be fully restored.";
	}

	private static boolean setGamma(final GammaControl gamma, final float value) {
		try {
			return gamma != null && gamma.setGamma(value);
		}
		catch (final RuntimeException e) {
			System.err.println("Video options: gamma adjustment failed: " + e.getMessage());
			return false;
		}
	}

	private static final class DisplayState {
		private final int width;
		private final int height;
		private final boolean fullscreen;
		private final DisplayMode mode;

		private DisplayState(final Graphics graphics) {
			this.width = graphics.getWidth();
			this.height = graphics.getHeight();
			this.fullscreen = graphics.isFullscreen();
			this.mode = graphics.getDisplayMode();
		}

		private boolean restore(final Graphics graphics) {
			try {
				return this.fullscreen ? graphics.setFullscreenMode(this.mode)
						: graphics.setWindowedMode(this.width, this.height);
			}
			catch (final RuntimeException e) {
				System.err.println("Video options: display rollback failed: " + e.getMessage());
				return false;
			}
		}
	}
}
