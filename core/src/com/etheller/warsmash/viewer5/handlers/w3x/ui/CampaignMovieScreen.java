package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.etheller.warsmash.WarsmashGdxMultiScreenGame;
import com.etheller.warsmash.datasources.DataSource;

/** A menu movie owns its decoder and returns to the same campaign menu on exit. */
public final class CampaignMovieScreen extends ScreenAdapter {
    private final WarsmashGdxMultiScreenGame game;
    private final Screen returnScreen;
    private final MoviePlayer.Session session;
    private final MovieAudioPlayback audio;
    private final Matrix4 projection = new Matrix4();
    private SpriteBatch batch;
    private Pixmap pixels;
    private Texture texture;
    private float elapsed;
    private double nextFrame;
    private boolean hasFrame;
    private boolean closed;

    public static CampaignMovieScreen open(WarsmashGdxMultiScreenGame game, DataSource source, String path)
            throws IOException {
        File file = null;
        for (String candidate : MoviePlayer.candidatePaths(path)) {
            if (source.has(candidate)) {
                file = source.getFile(candidate);
                if (file != null) break;
            }
        }
        if (file == null) throw new IOException("The cinematic file is missing: " + path);
        final String decoder = MoviePlayer.findFfmpeg();
        if (decoder == null) throw new IOException("Cinematic playback requires FFmpeg. Configure WARSMASH_FFMPEG and restart the game.");
        final String probe = MoviePlayer.probeOutput(decoder, file);
        final int[] size = MoviePlayer.parseVideoSize(probe);
        final double fps = MoviePlayer.parseFps(probe);
        final double duration = MoviePlayer.parseDurationSeconds(probe);
        if (size == null || fps <= 0 || duration <= 0) throw new IOException("The cinematic could not be decoded: " + path);
        final MoviePlayer.Session session = MoviePlayer.Session.start(decoder, file, size[0], size[1], fps, duration);
        if (session == null) throw new IOException("The cinematic could not be started: " + path);
        final MovieAudioPlayback audio = MoviePlayer.hasAudioStream(probe) ? new MovieAudioPlayback(decoder, file) : null;
        return new CampaignMovieScreen(game, session, audio);
    }

    private CampaignMovieScreen(WarsmashGdxMultiScreenGame game, MoviePlayer.Session session, MovieAudioPlayback audio) {
        this.game = game;
        this.returnScreen = game.getScreen();
        this.session = session;
        this.audio = audio;
    }

    @Override public void show() {
        this.batch = new SpriteBatch();
        this.pixels = new Pixmap(session.getWidth(), session.getHeight(), Pixmap.Format.RGB888);
        this.texture = new Texture(pixels);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int key) {
                if (key == Input.Keys.ESCAPE) finish();
                return true;
            }
        });
    }

    @Override public void render(float delta) {
        final int width = Gdx.graphics.getBackBufferWidth();
        final int height = Gdx.graphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, width, height);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (hasFrame) elapsed += delta;
        byte[] frame;
        int uploaded = 0;
        while (elapsed >= nextFrame && uploaded++ < 8 && (frame = session.pollFrame()) != null) {
            pixels.getPixels().clear();
            pixels.getPixels().put(frame).flip();
            texture.draw(pixels, 0, 0);
            if (!hasFrame && this.audio != null) this.audio.start();
            hasFrame = true;
            nextFrame += 1.0 / session.getFps();
        }
        if (elapsed >= session.getDurationSeconds() || (session.isFinished() && elapsed >= nextFrame)) {
            finish();
            return;
        }
        if (hasFrame) {
            final float scale = Math.min((float) width / session.getWidth(), (float) height / session.getHeight());
            final float w = session.getWidth() * scale, h = session.getHeight() * scale;
            batch.setProjectionMatrix(this.projection.setToOrtho2D(0, 0, width, height));
            batch.begin();
            // Decoder rows start at the top; the lower screen edge samples v=1.
            batch.draw(texture, (width - w) / 2, (height - h) / 2, w, h, 0, 1, 1, 0);
            batch.end();
        }
    }

    private void finish() {
        if (!closed) {
            dispose();
            game.setScreen(returnScreen);
        }
    }

    @Override public void hide() { dispose(); }

    @Override public void dispose() {
        if (closed) return;
        closed = true;
        if (this.audio != null) this.audio.stop();
        session.stop();
        if (texture != null) texture.dispose();
        if (pixels != null) pixels.dispose();
        if (batch != null) batch.dispose();
    }
}
