package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import com.badlogic.gdx.audio.Sound;
import com.etheller.warsmash.viewer5.*;
import com.etheller.warsmash.viewer5.gl.*;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

class AudioPlaybackSettingsTest {
    private final OptionsSettingsStore saved = new OptionsSettingsStore();
    private AudioExtension previous;
    private final List<Long> stopped = new ArrayList<>();
    private final List<CSoundFilename> owned = new ArrayList<>();
    private final List<Float> gains = new ArrayList<>();
    private final List<Float> pitches = new ArrayList<>();
    private final List<Boolean> loops = new ArrayList<>();
    private final List<Boolean> spatialModes = new ArrayList<>();
    private final List<Float> positions = new ArrayList<>();
    private long nextId = 1;
    private boolean reject;
    private Sound sound;
    private final AudioContext context = new AudioContext(AudioContext.Listener.DO_NOTHING, null);

    @BeforeEach void setup() {
        saved.copyFrom(OptionsSettingsStore.get());
        OptionsSettingsStore.get().reset();
        previous = Extensions.audio;
        sound = (Sound) Proxy.newProxyInstance(Sound.class.getClassLoader(), new Class<?>[] { Sound.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("stop")) {
                        assertNotNull(args, "Must never stop all instances of a shared sound");
                        stopped.add((Long) args[0]);
                        return null;
                    }
                    if (method.getName().equals("setVolume")) { gains.add((Float) args[1]); return null; }
                    if (method.getName().equals("setPitch")) { pitches.add((Float) args[1]); return null; }
                    throw new AssertionError(method.getName());
                });
        Extensions.audio = new AudioExtension() {
            public void setPosition(Sound sound, long id, float x, float y, float z,
                    boolean spatial, float maxDistance, float refDistance) { positions.add(x); spatialModes.add(spatial); }
            public AudioContext createContext(boolean world) { return context; }
            public float getDuration(Sound sound) { return 10; }
            public long play(Sound sound, float volume, float pitch, float x, float y, float z,
                    boolean spatial, float maxDistance, float refDistance, boolean looping) {
                gains.add(volume);
                pitches.add(pitch);
                loops.add(looping);
                spatialModes.add(spatial);
                positions.add(x);
                return reject ? -1 : nextId++;
            }
        };
    }

    @AfterEach void restore() {
        for (final CSoundFilename playback : owned) playback.stop();
        OptionsSettingsStore.get().copyFrom(saved);
        Extensions.audio = previous;
    }

    private CSoundFilename loop() {
        final CSoundFilename result = new CSoundFilename(sound, context, true, false, 0, 0, "");
        owned.add(result);
        return result;
    }

    @Test void masterGainScalesPlaybackAndLiveScriptVolume() {
        OptionsSettingsStore.get().setSoundVolume(25);
        final CSoundFilename loop = loop();
        loop.start();
        assertTrue(loop.isPlaying());
        assertEquals(0.25f, gains.get(0));
        loop.setVolume(64);
        assertEquals((64f / 127) * 0.25f, gains.get(1), 0.00001f);
    }

    @Test void mutedOrRejectedPlaybackIsNotReportedAsPlaying() {
        final CSoundFilename loop = loop();
        OptionsSettingsStore.get().setSoundEnabled(false);
        loop.start();
        assertFalse(loop.isPlaying());
        assertTrue(gains.isEmpty());
        loop.stop();
        assertTrue(stopped.isEmpty());
        OptionsSettingsStore.get().setSoundEnabled(true);
        reject = true;
        loop.start();
        assertFalse(loop.isPlaying());
        loop.stop();
        assertTrue(stopped.isEmpty());
    }

    @Test void restartingLoopStopsOnlyItsOwnPreviousInstance() {
        final CSoundFilename first = loop();
        final CSoundFilename second = loop();
        first.start();
        second.start();
        first.start();
        assertEquals(List.of(1L), stopped);
        assertTrue(second.isPlaying());
        first.stop();
        first.stop();
        assertEquals(List.of(1L, 3L), stopped);
    }

    @Test void invalidGainsAreSafeAndDisabledUnitResponsesDoNotStart() {
        assertEquals(0, AudioBufferSource.effectiveVolume(Float.NaN));
        assertEquals(0, AudioBufferSource.effectiveVolume(-1));
        assertEquals(1, AudioBufferSource.effectiveVolume(2));
        OptionsSettingsStore.get().setUnitSounds(false);
        assertFalse(new UnitSound(1, 1, 0, 0, 0, 0, false).playUnitResponse(context, null, 0));
    }
    @Test void activeScriptLoopMutesAndUnmutesWithoutRestarting() {
        final CSoundFilename loop = loop();
        loop.start();
        OptionsSettingsStore.get().setSoundVolume(37);
        CSoundFilename.refreshSettings();
        assertEquals(0.37f, gains.get(gains.size() - 1), 0.00001f);
        OptionsSettingsStore.get().setSoundEnabled(false);
        CSoundFilename.refreshSettings();
        assertEquals(0f, gains.get(gains.size() - 1));
        assertTrue(loop.isPlaying());
        OptionsSettingsStore.get().setSoundEnabled(true);
        CSoundFilename.refreshSettings();
        assertEquals(0.37f, gains.get(gains.size() - 1), 0.00001f);
        assertEquals(2, nextId, "Volume changes must not restart the source");
        loop.stop();
    }

    @Test void labelledSoundsKeepIndependentSettingsAndPlaybackHandles() {
        final UnitSound label = label();
        final CSoundFromLabel first = new CSoundFromLabel(label, context, true, false, false, 0, 0);
        final CSoundFromLabel second = new CSoundFromLabel(label, context, false, false, false, 0, 0);
        assertFalse(first.isPlaying());
        assertEquals(10, first.getPredictedDuration(), "Duration must be available before first playback");
        assertEquals(0, first.getRemainingTimeToPlayOnTheDesyncLocalComputer());
        first.setVolume(64);
        first.setPitch(2);
        first.setPosition(123, 45, 6);
        first.start();
        second.start();
        assertEquals(List.of(true, false), loops);
        assertEquals(64f / 127, gains.get(0), 0.00001f);
        assertEquals(2f, pitches.get(0));
        assertEquals(123f, positions.get(0));
        assertEquals(5, first.getPredictedDuration());
        first.setPosition(456, 45, 6);
        assertEquals(456f, positions.get(positions.size() - 1));
        first.stop();
        assertFalse(first.isPlaying());
        assertTrue(second.isPlaying());
        assertEquals(List.of(1L), stopped);
        first.start();
        assertEquals(64f / 127, gains.get(gains.size() - 1), 0.00001f);
        first.stop();
        second.stop();
    }

    @Test void positionalToggleUpdatesNewAndActiveWorldSounds() {
        final AudioContext.Listener listener = (AudioContext.Listener) Proxy.newProxyInstance(
                AudioContext.Listener.class.getClassLoader(), new Class<?>[] { AudioContext.Listener.class },
                (proxy, method, args) -> method.getReturnType() == boolean.class ? true :
                        method.getReturnType() == float.class ? 0f : null);
        final CSoundFilename loop = new CSoundFilename(sound, new AudioContext(listener, null), true, false, 0, 0, "");
        owned.add(loop);
        loop.start();
        assertTrue(spatialModes.get(0));
        OptionsSettingsStore.get().setPositionalAudio(false);
        CSoundFilename.refreshSettings();
        assertFalse(spatialModes.get(spatialModes.size() - 1));
        loop.start();
        assertFalse(spatialModes.get(spatialModes.size() - 1));
        OptionsSettingsStore.get().setPositionalAudio(true);
        CSoundFilename.refreshSettings();
        assertTrue(spatialModes.get(spatialModes.size() - 1));
        loop.stop();
    }

    private UnitSound label() {
        final var previousAudio = com.badlogic.gdx.Gdx.audio;
        try {
            com.badlogic.gdx.Gdx.audio = (com.badlogic.gdx.Audio) Proxy.newProxyInstance(
                    com.badlogic.gdx.Audio.class.getClassLoader(), new Class<?>[] { com.badlogic.gdx.Audio.class },
                    (proxy, method, args) -> sound);
            final var source = (com.etheller.warsmash.datasources.DataSource) Proxy.newProxyInstance(
                    com.etheller.warsmash.datasources.DataSource.class.getClassLoader(),
                    new Class<?>[] { com.etheller.warsmash.datasources.DataSource.class },
                    (proxy, method, args) -> method.getName().equals("has") ? true : null);
            final var table = new com.etheller.warsmash.units.DataTable(com.etheller.warsmash.util.StringBundle.EMPTY);
            final var row = new com.etheller.warsmash.units.Element("Label", table);
            row.setField("FileNames", "test.wav");
            row.setField("Volume", "127");
            row.setField("Pitch", "1");
            table.put("Label", row);
            return UnitSound.create(source, table, "Label", "");
        }
        finally { com.badlogic.gdx.Gdx.audio = previousAudio; }
    }

}
