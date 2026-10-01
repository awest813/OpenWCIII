package com.etheller.warsmash.desktop.tools;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.etheller.warsmash.viewer5.*;
import com.etheller.warsmash.viewer5.handlers.ModelHandler;

/** Real OpenGL regression: a nearby sky must not hide more distant terrain. */
public final class SkyBackgroundRenderAudit {
    public static void main(String[] args) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Sky background rendering regression";
        config.width = 64;
        config.height = 64;
        config.forceExit = false;
        LwjglApplication application = new LwjglApplication(new ApplicationAdapter() {
            @Override public void create() {
                try {
                    verify();
                    System.out.println("PASS: sky color preserved; distant terrain visible; sky drawn once per pass; hidden scenes preserve framebuffer");
                }
                catch (Throwable error) { failure.set(error); }
                finally { Gdx.app.exit(); }
            }
            @Override public void dispose() { finished.countDown(); }
        }, config);
        if (!finished.await(30, TimeUnit.SECONDS)) {
            application.exit();
            throw new AssertionError("Rendering regression timed out");
        }
        if (failure.get() != null) { throw new AssertionError("Sky rendering regression failed", failure.get()); }
    }

    private static void verify() {
        ModelViewer viewer = new ModelViewer(null, new CanvasProvider() {
            public float getWidth() { return 64; }
            public float getHeight() { return 64; }
        }) {
            public SceneLightManager createLightManager(boolean simple) { return null; }
        };
        Scene scene = new SimpleScene(viewer, null);
        TestModel model = new TestModel(viewer);
        TestInstance sky = new TestInstance(model, true);
        TestInstance terrain = new TestInstance(model, false);
        scene.instances.add(sky);
        scene.instances.add(terrain);
        GL11.glViewport(0, 0, 64, 64);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        scene.renderBackground(sky);
        scene.renderOpaque(sky);
        scene.renderTranslucent(sky);
        // Left: terrain farther than the sky's authored geometry. Right: sky.
        assertPixel(16, 32, 0, 255, 0);
        assertPixel(48, 32, 255, 0, 0);
        // Hidden portraits must not clear a black rectangle over the world.
        Scene hiddenPortrait = new SimpleScene(viewer, null);
        hiddenPortrait.show = false;
        hiddenPortrait.startFrame();
        hiddenPortrait.renderOpaque();
        hiddenPortrait.renderTranslucent();
        assertPixel(16, 32, 0, 255, 0);
        assertPixel(48, 32, 255, 0, 0);
        if (sky.opaqueCalls != 1 || sky.translucentCalls != 1) {
            throw new AssertionError("Sky rendered again in the world pass");
        }
        if (terrain.opaqueCalls != 1 || terrain.translucentCalls != 1) {
            throw new AssertionError("Ordinary world geometry lost a render pass");
        }
        // Clearing the sky must leave ordinary scene rendering intact.
        scene.renderOpaque();
        scene.renderTranslucent();
        if (sky.opaqueCalls != 2 || terrain.opaqueCalls != 2
                || sky.translucentCalls != 2 || terrain.translucentCalls != 2) {
            throw new AssertionError("Default scene passes changed");
        }
        int error = Gdx.gl.glGetError();
        if (error != GL20.GL_NO_ERROR) { throw new AssertionError("OpenGL error " + error); }
        viewer.webGL.emptyTexture.dispose();
    }

    private static void assertPixel(int x, int y, int r, int g, int b) {
        ByteBuffer pixel = BufferUtils.newByteBuffer(4);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        if ((pixel.get(0) & 255) != r || (pixel.get(1) & 255) != g || (pixel.get(2) & 255) != b) {
            throw new AssertionError("Unexpected pixel at " + x + "," + y + ": "
                    + (pixel.get(0) & 255) + "," + (pixel.get(1) & 255) + "," + (pixel.get(2) & 255));
        }
    }

    private static final class TestModel extends Model<ModelHandler> {
        TestModel(ModelViewer viewer) { super(null, viewer, "", null, ""); }
        protected ModelInstance createInstance(int type) { return null; }
        protected void lateLoad() { }
        protected void load(InputStream input, Object options) { }
        protected void error(Exception error) { throw new AssertionError(error); }
    }

    private static final class TestInstance extends ModelInstance {
        final boolean sky;
        int opaqueCalls;
        int translucentCalls;
        TestInstance(Model<?> model, boolean sky) { super(model); this.sky = sky; }
        public void renderOpaque(Matrix4 matrix) {
            opaqueCalls++;
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            GL11.glColor3f(sky ? 1 : 0, sky ? 0 : 1, 0);
            float right = sky ? 1 : 0;
            float depth = sky ? 0 : 0.8f;
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(-1, -1, depth);
            GL11.glVertex3f(right, -1, depth);
            GL11.glVertex3f(right, 1, depth);
            GL11.glVertex3f(-1, 1, depth);
            GL11.glEnd();
        }
        public void renderTranslucent() {
            translucentCalls++;
            // Real transparent sky layers leave depth writes disabled.
            GL11.glDepthMask(false);
        }
        public void updateAnimations(float dt) { }
        public void clearEmittedObjects() { }
        protected void updateLights(Scene scene) { }
        protected void removeLights(Scene scene) { }
        public void load() { }
        protected RenderBatch getBatch(TextureMapper mapper) { return null; }
        public void setReplaceableTexture(int id, String path) { }
        public void setReplaceableTextureHD(int id, String path) { }
    }
}
