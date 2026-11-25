package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import com.badlogic.gdx.audio.Sound;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.*;
import com.etheller.warsmash.viewer5.gl.*;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionStateFingerprint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;
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
    private final List<Float> maxDistances = new ArrayList<>();
    private final List<Float> referenceDistances = new ArrayList<>();
    private final List<Long> seekHandles = new ArrayList<>();
    private final List<Float> seekPositions = new ArrayList<>();
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
            public boolean seek(Sound sound, long id, float seconds) {
                seekHandles.add(id);
                seekPositions.add(seconds);
                return true;
            }
            public long play(Sound sound, float volume, float pitch, float x, float y, float z,
                    boolean spatial, float maxDistance, float refDistance, boolean looping) {
                gains.add(volume);
                pitches.add(pitch);
                loops.add(looping);
                spatialModes.add(spatial);
                positions.add(x);
                maxDistances.add(maxDistance);
                referenceDistances.add(refDistance);
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

    @Test void authoredSoundFadesKeepOneVoiceAndRespectVolumeGroups() {
        final long[] now = {1000};
        final CSoundFilename fading = new CSoundFilename(sound, context, true, false, 200, 400, "", () -> now[0]);
        owned.add(fading);
        fading.setVolumeGroupScale(0.5f);
        fading.start();
        final long voice = fading.getPlaybackId();
        assertTrue(fading.isPlaying());
        assertEquals(0f, gains.get(0));

        now[0] = 1100;
        CSoundFilename.refreshSettings();
        assertEquals(0.25f, gains.get(gains.size() - 1), 0.0001f);
        fading.stop(true);
        assertTrue(stopped.isEmpty());
        now[0] = 1300;
        CSoundFilename.refreshSettings();
        assertEquals(0.125f, gains.get(gains.size() - 1), 0.0001f);
        assertEquals(voice, fading.getPlaybackId());
        assertEquals(0.2f, fading.getRemainingTimeToPlayOnTheDesyncLocalComputer(), 0.0001f);

        fading.stop(true);
        now[0] = 1500;
        CSoundFilename.refreshSettings();
        assertFalse(fading.isPlaying());
        assertEquals(List.of(voice), stopped);
    }

    @Test void mutedVolumeGroupKeepsItsVoiceAndRestoresTheAuthoredGain() {
        final CSoundFilename grouped = loop();
        final CSoundFilename dialogue = loop();
        grouped.setVolume(64);
        grouped.setVolumeGroupScale(0);
        grouped.start();
        dialogue.start();

        assertTrue(grouped.isPlaying());
        assertEquals(List.of(0f, 1f), gains);
        final long voice = grouped.getPlaybackId();
        grouped.setVolumeGroupScale(0.5f);
        assertEquals(32f / 127, gains.get(2), 0.00001f);
        assertEquals(voice, grouped.getPlaybackId());
        assertTrue(stopped.isEmpty());
    }

    @Test void applyingSoundLabelUsesItsAuthoredSettingsAndPreservesMissingFields() {
        OptionsSettingsStore.get().setSoundVolume(100);
        final CSoundFilename sound = loop();
        sound.setPitch(1.25f);
        final Element dialogue = new Element("Dialogue", null);
        dialogue.setField("Volume", "120");
        UnitSound.applyLabelSettings(sound, dialogue);
        sound.start();
        assertEquals(120f / 127, gains.get(0), 0.00001f);
        assertEquals(1.25f, pitches.get(0));
        sound.stop();

        final Element ambience = new Element("Ambience", null);
        ambience.setField("Pitch", "0.8");
        ambience.setField("MinDistance", "100");
        ambience.setField("MaxDistance", "5000");
        ambience.setField("DistanceCutoff", "2500");
        UnitSound.applyLabelSettings(sound, ambience);
        sound.start();
        assertEquals(120f / 127, gains.get(1), 0.00001f);
        assertEquals(0.8f, pitches.get(1));
        assertEquals(100f, referenceDistances.get(1));
        assertEquals(5000f, maxDistances.get(1));
    }

    @Test void channelPreferencesChangeAudibleGainWithoutRestartingScriptPlayback() {
        final var options = OptionsSettingsStore.get();
        options.setAmbientSounds(false);
        final CSoundFilename playback = loop();
        playback.setVolumeGroup(CSoundVolumeGroup.AMBIENTSOUNDS);
        playback.setVolumeGroupScale(0.5f);
        playback.start();
        final long voice = playback.getPlaybackId();
        assertEquals(List.of(0f), gains);
        assertTrue(playback.isPlaying());

        CSoundFilename.refreshSettings();
        options.setAmbientSounds(true);
        CSoundFilename.refreshSettings();
        assertEquals(0.5f, gains.get(gains.size() - 1), 0.00001f);

        playback.setVolumeGroup(CSoundVolumeGroup.UNITMOVEMENT);
        options.setMovementSounds(false);
        CSoundFilename.refreshSettings();
        assertEquals(0f, gains.get(gains.size() - 1), 0.00001f);

        playback.setVolumeGroup(CSoundVolumeGroup.COMBAT);
        assertEquals(0.5f, gains.get(gains.size() - 1), 0.00001f);
        assertEquals(voice, playback.getPlaybackId());
        assertTrue(stopped.isEmpty());
    }

    @Test void audibleCutoffCanDifferFromTheAttenuationDistance() {
        final AudioPanner panner = context.createPanner(true);
        panner.setDistances(5000, 100);
        panner.setDistanceCutoff(2500);
        panner.setPosition(context.listener.getX() + 3000, context.listener.getY(), context.listener.getZ());
        assertFalse(panner.isWithinListenerDistance());
        assertEquals(5000f, panner.maxDistance);
    }

    @Test void rangeLimitedVoiceStopsWhenTheListenerOrSoundLeavesItsCutoff() {
        final float[] listenerX = { 0 };
        final AudioContext.Listener listener = (AudioContext.Listener) Proxy.newProxyInstance(
                AudioContext.Listener.class.getClassLoader(), new Class<?>[] { AudioContext.Listener.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "is3DSupported" -> true;
                    case "getX" -> listenerX[0];
                    case "getY", "getZ" -> 0f;
                    default -> null;
                });
        final var world = new AudioContext(listener, null);
        final var limited = new CSoundFilename(sound, world, true, true, 0, 0, "");
        final var unlimited = new CSoundFilename(sound, world, true, false, 0, 0, "");
        owned.add(limited);
        owned.add(unlimited);
        for (final CSoundFilename voice : List.of(limited, unlimited)) {
            voice.setMaxDistance(500);
            voice.setDistanceCutoff(200);
            voice.start();
        }
        final long firstId = limited.getPlaybackId();
        CSoundFilename.refreshSettings();
        listenerX[0] = 201;
        CSoundFilename.refreshSettings();
        assertEquals(List.of(firstId), stopped);
        assertFalse(limited.isPlaying());
        assertTrue(unlimited.isPlaying());
        listenerX[0] = 0;
        CSoundFilename.refreshSettings();
        assertFalse(limited.isPlaying());

        limited.start();
        final long secondId = limited.getPlaybackId();
        limited.setPosition(-190, 0, 0);
        assertTrue(limited.isPlaying());
        limited.setDistanceCutoff(180);
        CSoundFilename.refreshSettings();
        assertEquals(List.of(firstId, secondId), stopped);
        assertFalse(limited.isPlaying());
    }

    @Test void seekingAffectsOnlyAnActiveVoiceAndUsesBufferTimeAtTheCurrentPitch() {
        final CSoundFilename first = loop();
        final CSoundFilename second = loop();
        first.setPlayPosition(200);
        assertTrue(seekHandles.isEmpty());

        first.start();
        second.start();
        first.setPlayPosition(-1);
        first.setPlayPosition(10000);
        assertTrue(seekHandles.isEmpty());

        first.setPitch(2);
        first.setPlayPosition(6500);
        assertEquals(List.of(first.getPlaybackId()), seekHandles);
        assertEquals(List.of(6.5f), seekPositions);
        assertEquals(1.75f, first.getRemainingTimeToPlayOnTheDesyncLocalComputer(), 0.15f);
        assertTrue(second.getRemainingTimeToPlayOnTheDesyncLocalComputer() > 8f);
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

    @Test void checkpointDigestIgnoresLocalVoiceStateWhileRetainingScriptSettings() {
        final CSoundFilename voice = loop();
        final String initial = MissionStateFingerprint.ofRuntime(voice);
        voice.start();
        assertTrue(voice.isPlaying());
        voice.setPlayPosition(6500);
        assertEquals(initial, MissionStateFingerprint.ofRuntime(voice));

        voice.setVolume(64);
        assertNotEquals(initial, MissionStateFingerprint.ofRuntime(voice));
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

    @Test void attachedSoundStartsAtTheCurrentPositionAndTracksMovementUntilDetached() {
        final var manager = new AttachedSoundManager();
        final CSoundFilename attached = loop();
        final CSoundFilename independent = loop();
        final float[] x = { 10 };
        manager.attach(attached, sound -> sound.setPosition(x[0], 0, 0));
        x[0] = 25;
        manager.updatePosition(attached);
        attached.start();
        final long attachedId = attached.getPlaybackId();
        independent.start();
        assertEquals(List.of(25f, 0f), positions);
        x[0] = 40;
        manager.updatePlayback();
        assertEquals(List.of(25f, 0f, 40f), positions);
        manager.detach(attached);
        x[0] = 60;
        manager.updatePlayback();
        assertEquals(List.of(25f, 0f, 40f), positions);
        manager.attach(attached, sound -> sound.setPosition(x[0], 0, 0));
        assertEquals(60f, positions.get(positions.size() - 1));
        manager.stopAll();
        assertEquals(List.of(attachedId), stopped);
        assertFalse(attached.isPlaying());
        assertTrue(independent.isPlaying());
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

    @Test void directLabelLoopUsesLiveMasterGainAndStopsByItsOwnId() {
        final UnitSound label = label();
        final long id = label.play(context, 0, 0, 0, true);
        assertEquals(1L, id);
        try {
            OptionsSettingsStore.get().setSoundVolume(32);
            CSoundFilename.refreshSettings();
            assertEquals(0.32f, gains.get(gains.size() - 1), 0.00001f);
            OptionsSettingsStore.get().setSoundEnabled(false);
            CSoundFilename.refreshSettings();
            assertEquals(0f, gains.get(gains.size() - 1));
            OptionsSettingsStore.get().setSoundEnabled(true);
            CSoundFilename.refreshSettings();
            assertEquals(0.32f, gains.get(gains.size() - 1), 0.00001f);
            assertEquals(2, nextId, "Audio options must preserve the existing loop");
        }
        finally {
            label.stop(id);
        }
        assertEquals(List.of(id), stopped);
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
