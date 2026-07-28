package com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui;

import com.etheller.warsmash.viewer5.handlers.w3x.SplatModel.SplatMover;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;

/**
 * JASS {@code ubersplat} handle backed by a terrain splat when possible.
 */
public class CUbersplat {
	private final float x;
	private final float y;
	private final String name;
	private final int red, green, blue, alpha;
	private SplatMover splat;
	private boolean shown = true;

	public CUbersplat(final float x, final float y, final String name, final int red, final int green, final int blue,
			final int alpha) {
		this.x = x;
		this.y = y;
		this.name = name != null ? name : "";
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}

	public void create(final War3MapViewer viewer) {
		if ((viewer == null) || this.name.isEmpty() || (this.splat != null)) {
			return;
		}
		try {
			String path = this.name;
			if (!path.contains("\\") && !path.contains("/")) {
				path = "ReplaceableTextures\\Splats\\" + this.name + ".blp";
			}
			this.splat = viewer.addUberSplatIngame(this.x, this.y, path, 128f);
		}
		catch (final Exception e) {
			System.err.println("CreateUbersplat: failed for '" + this.name + "': " + e.getMessage());
		}
	}

	public void show(final War3MapViewer viewer, final boolean flag) {
		this.shown = flag;
		if (this.splat == null) {
			if (flag) {
				create(viewer);
			}
			return;
		}
		if (flag) {
			this.splat.show(viewer.terrain.centerOffset);
			this.splat.setLocation(this.x, this.y, viewer.terrain.centerOffset);
		}
		else {
			this.splat.hide();
		}
	}

	public void finish() {
		// Birth animation finish — no-op without UberSplatData timing.
	}

	public void reset() {
		// Reset birth animation — no-op MVP.
	}

	public void destroy(final War3MapViewer viewer) {
		if ((this.splat != null) && (viewer != null) && (viewer.terrain != null)) {
			this.splat.destroy(com.badlogic.gdx.Gdx.gl30, viewer.terrain.centerOffset);
			this.splat = null;
		}
	}

	public boolean isShown() {
		return this.shown;
	}

	public int getRed() {
		return this.red;
	}

	public int getGreen() {
		return this.green;
	}

	public int getBlue() {
		return this.blue;
	}

	public int getAlpha() {
		return this.alpha;
	}
}
