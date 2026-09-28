package com.etheller.warsmash.desktop.tools;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxShaders;
import com.badlogic.gdx.utils.BufferUtils;
import com.etheller.warsmash.viewer5.CanvasProvider;
import com.etheller.warsmash.viewer5.ModelViewer;
import com.etheller.warsmash.viewer5.RawOpenGLTextureResource;
import com.etheller.warsmash.viewer5.SceneLightManager;
import com.etheller.warsmash.viewer5.handlers.mdx.GeosetAnimation;
import com.etheller.warsmash.viewer5.handlers.mdx.Material;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.mdx.SetupGeosets;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;
import com.hiveworkshop.rms.parsers.mdlx.MdlxGeoset;
import com.hiveworkshop.rms.parsers.mdlx.MdlxGeosetAnimation;

/** Real GPU regression for lossless texture quality switches and authored model detail. */
public final class GraphicsSettingsRenderAudit {
	public static void main(final String[] args) throws Exception {
		final CountDownLatch finished = new CountDownLatch(1);
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Graphics settings regression";
		config.width = 64;
		config.height = 64;
		config.useGL30 = true;
		config.gles30ContextMajorVersion = 3;
		config.gles30ContextMinorVersion = 3;
		config.forceExit = false;
		final LwjglApplication application = new LwjglApplication(new ApplicationAdapter() {
			@Override public void create() {
				try {
					verify();
					System.out.println("PASS: GPU texture quality, LOD animation identity, and tree fade/cutouts");
				}
				catch (final Throwable error) { failure.set(error); }
				finally { Gdx.app.exit(); }
			}
			@Override public void dispose() { finished.countDown(); }
		}, config);
		if (!finished.await(30, TimeUnit.SECONDS)) {
			application.exit();
			throw new AssertionError("Graphics audit timed out");
		}
		if (failure.get() != null) throw new AssertionError("Graphics audit failed", failure.get());
	}

	private static void verify() {
		final ModelViewer viewer = new ModelViewer(null, new CanvasProvider() {
			public float getWidth() { return 64; }
			public float getHeight() { return 64; }
		}) {
			public SceneLightManager createLightManager(final boolean simple) { return null; }
		};
		final OptionsSettingsStore options = OptionsSettingsStore.get();
		final OptionsSettingsStore saved = new OptionsSettingsStore();
		saved.copyFrom(options);
		final RawOpenGLTextureResource texture = new RawOpenGLTextureResource(viewer, "", null, "audit", null) {
			protected void lateLoad() { }
			protected void load(final InputStream input, final Object opts) { }
		};
		try {
			final BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
				image.setRGB(x, y, ((x + y) & 1) == 0 ? 0xffff0000 : 0xff0000ff);
			}
			texture.update(image, false);
			for (final int quality : new int[] { 2, 0, 1, 2 }) {
				options.setTextureQuality(quality);
				texture.bind(0);
				if (GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL) != 2 - quality) {
					throw new AssertionError("GPU texture base level did not change");
				}
				final ByteBuffer pixel = drawTexturePixel();
				final int expectedRed = quality == 2 ? 255 : 128;
				final int expectedBlue = quality == 2 ? 0 : 128;
				if (Math.abs((pixel.get(0) & 255) - expectedRed) > 2
						|| Math.abs((pixel.get(2) & 255) - expectedBlue) > 2) {
					throw new AssertionError("Wrong texture sample at quality " + quality + ": "
							+ (pixel.get(0) & 255) + "," + (pixel.get(2) & 255));
				}
			}
			verifyLodUpload(viewer);
			verifyOcclusionCutouts(viewer, texture);
			verifyGlowSampling(texture);
			verifyBackdropTiles();
			final int error = Gdx.gl.glGetError();
			if (error != GL20.GL_NO_ERROR) throw new AssertionError("OpenGL error " + error);
		}
		finally {
			options.copyFrom(saved);
			Gdx.gl.glDeleteTexture(texture.getGlHandle());
			viewer.webGL.emptyTexture.dispose();
		}
	}

	private static void verifyBackdropTiles() {
		final com.badlogic.gdx.graphics.Pixmap pixels = new com.badlogic.gdx.graphics.Pixmap(64, 8,
				com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
		for (int y = 0; y < 8; y++) for (int x = 0; x < 64; x++) {
			pixels.drawPixel(x, y, (((y + 1) * 24) << 24) | 0xff);
		}
		final com.badlogic.gdx.graphics.Texture atlas = new com.badlogic.gdx.graphics.Texture(pixels);
		pixels.dispose();
		final com.badlogic.gdx.graphics.g2d.SpriteBatch batch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
		try {
			Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
			Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
			Gdx.gl.glClearColor(0, 0, 0, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, 64, 64));
			final var frame = new com.etheller.warsmash.parsers.fdf.frames.BackdropFrame("audit", null, false,
					false, null, java.util.EnumSet.allOf(com.etheller.warsmash.parsers.fdf.datamodel.BackdropCornerFlags.class),
					8, 8, new com.etheller.warsmash.parsers.fdf.datamodel.Vector4Definition(0, 0, 0, 0), atlas, false);
			frame.setWidth(37.5f);
			frame.setHeight(37.5f);
			batch.begin();
			frame.render(batch, null, null);
			batch.end();
			// The final 5.5-pixel edge tile must crop the gradient, without clamping
			// pixel-valued UVs or truncating its fractional geometry/texture extent.
			for (final int[] point : new int[][] {{2, 24}, {34, 24}, {24, 2}, {24, 34}}) {
				final ByteBuffer sample = BufferUtils.newByteBuffer(4);
				GL11.glReadPixels(point[0], point[1], 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, sample);
				if ((sample.get(0) & 255) != 144) throw new AssertionError("Backdrop partial edge sample at "
						+ Arrays.toString(point) + " was " + (sample.get(0) & 255) + ", expected 144");
			}
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			final var background = new com.etheller.warsmash.parsers.fdf.frames.BackdropFrame("background", null, false,
					true, atlas, java.util.EnumSet.noneOf(com.etheller.warsmash.parsers.fdf.datamodel.BackdropCornerFlags.class),
					0, 8, new com.etheller.warsmash.parsers.fdf.datamodel.Vector4Definition(2, 2, 2, 2), null, false);
			background.setWidth(20);
			background.setHeight(20);
			batch.begin();
			background.render(batch, null, null);
			batch.end();
			final ByteBuffer sample = BufferUtils.newByteBuffer(4);
			GL11.glReadPixels(4, 4, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, sample);
			if ((sample.get(0) & 255) != 144) throw new AssertionError("Backdrop insets changed the authored tile size");
			System.out.println("PASS: fractional menu border tiles on all four edges and inset background tiling");
		}
		finally {
			batch.dispose();
			atlas.dispose();
		}
	}

	private static ByteBuffer drawTexturePixel() {
		GL11.glViewport(0, 0, 64, 64);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		final com.badlogic.gdx.graphics.glutils.ShaderProgram shader = new com.badlogic.gdx.graphics.glutils.ShaderProgram(
				"#version 330 core\n in vec2 a_position; void main() { gl_Position = vec4(a_position, 0, 1); }",
				"#version 330 core\n uniform sampler2D u_texture; out vec4 color; void main() { color = texture(u_texture, vec2(0.0625)); }");
		if (!shader.isCompiled()) throw new AssertionError(shader.getLog());
		final com.badlogic.gdx.graphics.Mesh mesh = new com.badlogic.gdx.graphics.Mesh(true, 4, 6,
				new com.badlogic.gdx.graphics.VertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position, 2, "a_position"));
		mesh.setVertices(new float[] { -1, -1, 1, -1, 1, 1, -1, 1 });
		mesh.setIndices(new short[] { 0, 1, 2, 0, 2, 3 });
		shader.bind();
		shader.setUniformi("u_texture", 0);
		mesh.render(shader, GL20.GL_TRIANGLES);
		mesh.dispose();
		shader.dispose();
		Gdx.gl.glUseProgram(0);
		final ByteBuffer pixel = BufferUtils.newByteBuffer(4);
		GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
		return pixel;
	}

	private static void verifyOcclusionCutouts(final ModelViewer viewer, final RawOpenGLTextureResource texture) {
		final ShaderProgram shader = viewer.webGL.createShaderProgram(
				"attribute vec2 a_position; varying vec2 v_uv; varying vec4 v_color; "
				+ "varying vec4 v_uvTransRot; varying float v_uvScale; void main() { "
				+ "gl_Position=vec4(a_position,0,1); v_uv=vec2(0.5); v_color=vec4(1); "
				+ "v_uvTransRot=vec4(0,0,0,1); v_uvScale=1.0; }", MdxShaders.fsComplex);
		if (!shader.isCompiled()) throw new AssertionError(shader.getLog());
		final Mesh mesh = new Mesh(true, 4, 6,
				new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
		try {
			mesh.setVertices(new float[] { -1, -1, 1, -1, 1, 1, -1, 1 });
			mesh.setIndices(new short[] { 0, 1, 2, 0, 2, 3 });
			shader.bind();
			shader.setUniformi("u_texture", 0);
			shader.setUniformi("u_smoothGlow", 0);
			shader.setUniformf("u_vertexColor", 1, 1, 1, 1);
			shader.setUniformf("u_filterMode", 1);
			shader.setUniformi("u_unfogged", 1);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			for (final float opacity : new float[] { 1, 0.35f, 1 }) {
				shader.setUniformf("u_occlusionAlpha", opacity);
				for (final int alpha : new int[] { 128, 255 }) {
					final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
					image.setRGB(0, 0, (alpha << 24) | 0x00ff0000);
					texture.update(image, false);
					texture.bind(0);
					GL11.glClearColor(0, 0, 1, 1);
					GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
					mesh.render(shader, GL20.GL_TRIANGLES);
					final ByteBuffer pixel = BufferUtils.newByteBuffer(4);
					GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
					final int red = alpha < 192 ? 0 : Math.round(opacity * 255);
					if (Math.abs((pixel.get(0) & 255) - red) > 2
							|| Math.abs((pixel.get(2) & 255) - (255 - red)) > 2) {
						throw new AssertionError("Tree cutout/fade changed at opacity " + opacity + ", alpha " + alpha);
					}
				}
			}
		}
		finally {
			mesh.dispose();
			shader.dispose();
			Gdx.gl.glUseProgram(0);
		}
	}

	private static void verifyGlowSampling(final RawOpenGLTextureResource texture) {
		// This audit owns/disposes its shader; do not reuse the viewer cache's
		// identical program already disposed by the preceding cutout audit.
		final ShaderProgram shader = new ShaderProgram(
				"attribute vec2 a_position; varying vec2 v_uv; varying vec4 v_color; "
				+ "varying vec4 v_uvTransRot; varying float v_uvScale; void main() { "
				+ "gl_Position=vec4(a_position,0,1); v_uv=vec2(0.5); v_color=vec4(1); "
				+ "v_uvTransRot=vec4(0,0,0,1); v_uvScale=1.0; }", MdxShaders.fsComplex);
		if (!shader.isCompiled()) throw new AssertionError(shader.getLog());
		final Mesh mesh = new Mesh(true, 4, 6,
				new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
		try {
			mesh.setVertices(new float[] { -1, -1, 1, -1, 1, 1, -1, 1 });
			mesh.setIndices(new short[] { 0, 1, 2, 0, 2, 3 });
			shader.bind();
			shader.setUniformi("u_texture", 0);
			shader.setUniformf("u_vertexColor", 1, 1, 1, 1);
			shader.setUniformf("u_occlusionAlpha", 1);
			shader.setUniformf("u_filterMode", 3);
			shader.setUniformi("u_unfogged", 1);
			GL11.glDisable(GL11.GL_BLEND);
			// A magnified isolated texel must lose its bilinear cusp; constant black
			// and white must stay constant (no dark-edge ringing or changed alpha).
			for (final int kind : new int[] { 0, 1, 2 }) {
				final BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
				for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
					image.setRGB(x, y, kind == 2 || (kind == 1 && x == 1 && y == 1) ? 0xffffffff : 0xff000000);
				}
				texture.update(image, false);
				texture.bind(0);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				for (final int smooth : new int[] { 0, 1, 0 }) {
					shader.setUniformi("u_smoothGlow", smooth);
					mesh.render(shader, GL20.GL_TRIANGLES);
					final ByteBuffer pixel = BufferUtils.newByteBuffer(4);
					GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
					final int expected = kind == 0 ? 0 : kind == 1 && smooth == 1 ? 113 : 255;
					for (int channel = 0; channel < 4; channel++) {
						if (Math.abs((pixel.get(channel) & 255) - (channel == 3 ? 255 : expected)) > 2) {
							throw new AssertionError("Glow sample changed: pattern=" + kind + ", smooth=" + smooth
									+ ", channel=" + channel + ", value=" + (pixel.get(channel) & 255));
						}
					}
				}
			}
			System.out.println("PASS: glow smoothing, neutral black, constant brightness/alpha and ordinary sampling restoration");
		}
		finally {
			mesh.dispose();
			shader.dispose();
			Gdx.gl.glUseProgram(0);
		}
	}

	private static void verifyLodUpload(final ModelViewer viewer) {
		final MdxModel model = new MdxModel(null, viewer, "", null, "audit");
		model.materials.add(new Material(model, "", Collections.emptyList()));
		final MdlxGeosetAnimation animation = new MdlxGeosetAnimation();
		animation.geosetId = 1;
		model.geosetAnimations.add(new GeosetAnimation(model, animation));
		SetupGeosets.setupGeosets(model, Arrays.asList(triangle(2), triangle(0), triangle(-1)), false);
		if (model.geosets.size() != 3 || model.geosets.get(1).geosetAnimation == null
				|| model.geosets.get(0).geosetAnimation != null) {
			throw new AssertionError("LOD upload lost geometry or remapped animation IDs");
		}
		for (final int quality : new int[] { 0, 2 }) {
			OptionsSettingsStore.get().setModelDetail(quality);
			if (!model.geosets.get(2).isSelectedDetail()
					|| model.geosets.get(0).isSelectedDetail() != (quality == 0)
					|| model.geosets.get(1).isSelectedDetail() != (quality == 2)) {
				throw new AssertionError("Incorrect geosets selected for quality " + quality);
			}
		}
		Gdx.gl.glDeleteBuffer(model.arrayBuffer);
		Gdx.gl.glDeleteBuffer(model.elementBuffer);
	}

	private static MdlxGeoset triangle(final int lod) {
		final MdlxGeoset geoset = new MdlxGeoset();
		geoset.lod = lod;
		geoset.vertices = new float[] { 0, 0, 0, 1, 0, 0, 0, 1, 0 };
		geoset.normals = new float[9];
		geoset.uvSets = new float[][] { new float[6] };
		geoset.vertexGroups = new short[3];
		geoset.matrixGroups = new long[0];
		geoset.matrixIndices = new long[0];
		geoset.faces = new int[] { 0, 1, 2 };
		return geoset;
	}
}
