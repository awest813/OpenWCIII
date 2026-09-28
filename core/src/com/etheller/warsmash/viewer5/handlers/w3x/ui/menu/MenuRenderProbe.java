package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.ModelViewer;
import com.etheller.warsmash.viewer5.Scene;

/** Opt-in, bounded menu capture for the desktop graphics audit. */
public final class MenuRenderProbe {
    private final String directory = System.getProperty("warsmash.menuAuditDirectory");
    private final int[][] sizes = { {800, 600}, {1280, 720}, {600, 800} };
    private int index = -1;
    private float elapsed;

    public void afterRender(final ModelViewer viewer, final Scene backdrop) {
        if (this.directory == null) return;
        this.elapsed += Gdx.graphics.getDeltaTime();
        if (this.elapsed < 2) return;
        this.elapsed = 0;
        if (this.index >= 0) {
            // Compare filtering at exactly the same animation frame, without updating
            // camera, particles or bones between the two renders.
            final boolean smooth = backdrop.smoothAdditiveTextures;
            try {
                backdrop.smoothAdditiveTextures = false;
                viewer.startFrame();
                viewer.render();
                final Pixmap baseline = ScreenUtils.getFrameBufferPixmap(0, 0,
                        Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                try { writeCapture("menu-" + this.index + "-bilinear.png", baseline); }
                finally { baseline.dispose(); }
            }
            finally { backdrop.smoothAdditiveTextures = smooth; }
            viewer.startFrame();
            viewer.render();
            final Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0, 0,
                    Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            try {
                writeCapture("menu-" + this.index + ".png", pixels);
                verifyBackdropCoverage(pixels);
            }
            finally { pixels.dispose(); }
            if (Gdx.gl.glGetError() != 0) throw new IllegalStateException("Menu audit OpenGL error");
        }
        if (++this.index == this.sizes.length) {
            System.out.println("[MenuRenderProbe] complete");
            Gdx.app.exit();
        }
        else if (!Gdx.graphics.setWindowedMode(this.sizes[this.index][0], this.sizes[this.index][1])) {
            throw new IllegalStateException("Menu audit resize rejected");
        }
    }

    private void writeCapture(final String name, final Pixmap pixels) {
        final PixmapIO.PNG writer = new PixmapIO.PNG();
        try {
            writer.setFlipY(true);
            writer.write(Gdx.files.absolute(this.directory).child(name), pixels);
        }
        catch (final java.io.IOException error) { throw new RuntimeException(error); }
        finally { writer.dispose(); }
    }

    private void verifyBackdropCoverage(final Pixmap pixels) {
        final Rectangle viewport = MenuViewport.fit(new Rectangle(), pixels.getWidth(), pixels.getHeight(),
                WarsmashConstants.FULL_SCREEN_MENU_BACKDROP);
        // The main-menu water must cover this strip, below the old exposed mesh edge.
        // Stay inside the fitted viewport and away from the logo, cursor and buttons.
        // Framebuffer pixmaps have their origin at the bottom left.
        final int left = (int) (viewport.x + viewport.width * 0.25f);
        final int right = (int) (viewport.x + viewport.width * 0.60f);
        final int bottom = (int) (viewport.y + viewport.height * 0.02f);
        final int top = (int) (viewport.y + viewport.height * 0.08f);
        int clearPixels = 0;
        int samples = 0;
        for (int y = bottom; y < top; y++) {
            for (int x = left; x < right; x++) {
                if ((pixels.getPixel(x, y) & 0xffffff00) == 0) clearPixels++;
                samples++;
            }
        }
        if (samples == 0 || clearPixels > samples / 100) {
            throw new IllegalStateException("Exposed menu backdrop edge in capture " + this.index
                    + ": " + clearPixels + "/" + samples + " pixels are clear black");
        }
        System.out.println("[MenuRenderProbe] coverage " + this.index + ": " + clearPixels + "/" + samples
                + " clear pixels");
    }
}
