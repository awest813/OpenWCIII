package com.etheller.warsmash.viewer5.gl;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

/** Per-texture mip selection; never apply to data textures, fonts, or UI atlases. */
public final class TextureQuality {
	private boolean mipmaps;
	private int appliedLevel = -1;

	public TextureQuality(final boolean mipmaps) { this.mipmaps = mipmaps; }

	public static int baseLevel(final int width, final int height, final int quality) {
		final int maxLevel = 31 - Integer.numberOfLeadingZeros(Math.max(1, Math.max(width, height)));
		return Math.min(maxLevel, 2 - Math.max(0, Math.min(2, quality)));
	}

	/** Call with this texture bound. Retain level zero so switching back is lossless. */
	public void apply(final GL20 gl, final int target, final int width, final int height) {
		final int level = baseLevel(width, height, OptionsSettingsStore.get().getTextureQuality());
		if (level == this.appliedLevel) return;
		if (level > 0 && !this.mipmaps) {
			gl.glTexParameteri(target, GL30.GL_TEXTURE_BASE_LEVEL, 0);
			gl.glGenerateMipmap(target);
			this.mipmaps = true;
		}
		gl.glTexParameteri(target, GL30.GL_TEXTURE_BASE_LEVEL, level);
		this.appliedLevel = level;
	}

	public void invalidate() {
		this.mipmaps = false;
		this.appliedLevel = -1;
	}
}
