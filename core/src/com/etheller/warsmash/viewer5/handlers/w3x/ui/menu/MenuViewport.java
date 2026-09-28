package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import com.badlogic.gdx.math.Rectangle;

/** Fits the authored 4:3 menu backdrop to the same centered area as the UI. */
public final class MenuViewport {
    private MenuViewport() { }

    public static Rectangle fit(final Rectangle result, final int width, final int height, final boolean fullscreen) {
        final float w = Math.max(1, width);
        final float h = Math.max(1, height);
        if (fullscreen) return result.set(0, 0, w, h);
        final float fittedWidth = Math.min(w, h * 4f / 3f);
        final float fittedHeight = fittedWidth * 3f / 4f;
        return result.set((w - fittedWidth) / 2, (h - fittedHeight) / 2, fittedWidth, fittedHeight);
    }
}
