package com.etheller.warsmash.viewer5.gl;

/** Platform display gamma. Returns false when the driver cannot apply it. */
public interface GammaControl {
	boolean setGamma(float gamma);
}
