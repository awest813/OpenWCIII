package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.DataTable;

class MusicPlayerLibGDXTest {
    private final Audio previousAudio = Gdx.audio;

    @AfterEach
    void restoreAudio() {
        Gdx.audio = this.previousAudio;
    }

    private static final class Track {
        boolean playing;
        boolean disposed;
        float volume;
        float position;
        final Music music = (Music) Proxy.newProxyInstance(Music.class.getClassLoader(),
                new Class<?>[] { Music.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "play": this.playing = true; return null;
                    case "pause": this.playing = false; return null;
                    case "isPlaying": return this.playing;
                    case "dispose": this.disposed = true; return null;
                    case "setVolume": this.volume = (Float) args[0]; return null;
                    case "setPosition": this.position = (Float) args[0]; return null;
                    default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private MusicPlayerLibGDX player(final List<Track> tracks) {
        Gdx.audio = (Audio) Proxy.newProxyInstance(Audio.class.getClassLoader(),
                new Class<?>[] { Audio.class }, (proxy, method, args) -> {
                    if (method.getName().equals("newMusic")) {
                        final Track track = new Track();
                        tracks.add(track);
                        return track.music;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        final DataSource source = (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                new Class<?>[] { DataSource.class }, (proxy, method, args) -> {
                    if (method.getName().equals("has")) {
                        return !args[0].equals("missing.mp3");
                    }
                    return null;
                });
        return new MusicPlayerLibGDX(source, new DataTable(com.etheller.warsmash.util.StringBundle.EMPTY));
    }

    @Test
    void emptyMissingAndInvalidPlaylistIndexesDoNotCrash() {
        final List<Track> tracks = new ArrayList<>();
        final MusicPlayerLibGDX player = player(tracks);
        assertNull(player.playMusicEx("missing.mp3", false, 4, 0, 0));
        player.update(1f);
        assertNull(player.playMusicEx(" ; , ", false, -1, 0, 0));
        assertNotNull(player.playMusicEx("missing.mp3; track.mp3", false, 8, 0, 0));
        assertEquals(1, tracks.size());
        assertTrue(tracks.get(0).playing);
    }

    @Test
    void fadeRespectsVolumePauseAndResumeWithoutSkippingTrack() {
        final List<Track> tracks = new ArrayList<>();
        final MusicPlayerLibGDX player = player(tracks);
        player.playMusicEx("one.mp3;two.mp3", false, 0, 2000, 1000);
        final Track first = tracks.get(0);
        assertEquals(0f, first.volume);
        assertEquals(2f, first.position);
        player.update(0.5f);
        assertEquals(0.5f, first.volume, 0.001f);
        player.setVolume(63);
        assertEquals((63f / 127f) * 0.5f, first.volume, 0.001f);
        player.stopMusic();
        player.update(1f);
        assertFalse(first.playing);
        player.resumeMusic();
        player.update(0.5f);
        assertTrue(first.playing);
        assertFalse(tracks.get(1).playing);
        assertEquals(63f / 127f, first.volume, 0.001f);
        player.playMusicEx(null, false, 0, 0, 0);
        assertTrue(first.disposed);
        assertTrue(tracks.get(1).disposed);
    }
}
