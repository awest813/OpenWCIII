package com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui;

import com.etheller.warsmash.viewer5.handlers.w3x.SplatModel.SplatMover;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;

/**
 * MVP JASS {@code image} handle: optional ground splat when shown.
 */
public class CImage {
	private final String file;
	private final float sizeX;
	private final float sizeY;
	private float x;
	private float y;
	private float z;
	private final int imageType;
	private boolean shown;
	private SplatMover splat;

	public CImage(final String file, final float sizeX, final float sizeY, final float sizeZ, final float posX,
			final float posY, final float posZ, final int imageType) {
		this.file = file != null ? file : "";
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.x = posX;
		this.y = posY;
		this.z = posZ;
		this.imageType = imageType;
	}

	public void show(final War3MapViewer viewer, final boolean flag) {
		if (flag == this.shown) {
			return;
		}
		this.shown = flag;
		if (flag) {
			if (this.splat != null) {
				this.splat.show(viewer.terrain.centerOffset);
			}
			else {
				ensureSplat(viewer);
			}
		}
		else if (this.splat != null) {
			this.splat.hide();
		}
	}

	public void setPosition(final War3MapViewer viewer, final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		if (this.shown) {
			if (this.splat != null) {
				this.splat.setLocation(x, y, viewer.terrain.centerOffset);
			}
			else {
				ensureSplat(viewer);
			}
		}
	}

	public void destroy(final War3MapViewer viewer) {
		this.shown = false;
		if ((this.splat != null) && (viewer != null) && (viewer.terrain != null)) {
			this.splat.destroy(com.badlogic.gdx.Gdx.gl30, viewer.terrain.centerOffset);
			this.splat = null;
		}
	}

	private void ensureSplat(final War3MapViewer viewer) {
		if ((viewer == null) || this.file.isEmpty()) {
			return;
		}
		if (this.splat != null) {
			this.splat.setLocation(this.x, this.y, viewer.terrain.centerOffset);
			return;
		}
		try {
			final float scale = Math.max(16f, Math.max(this.sizeX, this.sizeY) * 0.5f);
			this.splat = viewer.addUberSplatIngame(this.x, this.y, this.file, scale);
		}
		catch (final Exception e) {
			System.err.println("CreateImage: failed to create splat for '" + this.file + "': " + e.getMessage());
		}
	}

	public boolean isShown() {
		return this.shown;
	}

	public int getImageType() {
		return this.imageType;
	}
}
