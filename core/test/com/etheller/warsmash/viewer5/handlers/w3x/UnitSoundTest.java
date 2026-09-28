package com.etheller.warsmash.viewer5.handlers.w3x;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.StringBundle;
import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.gl.AudioExtension;
import com.etheller.warsmash.viewer5.gl.Extensions;

class UnitSoundTest {
    @Test
    void repeatedMenuPlaybackKeepsOneLoopAndStopSilencesIt() {
        final Audio previousAudio = Gdx.audio;
        final AudioExtension previousExtension = Extensions.audio;
        final int[] active = {0};
        final Sound sound = (Sound) Proxy.newProxyInstance(Sound.class.getClassLoader(),
                new Class<?>[] {Sound.class}, (proxy, method, args) -> {
                    if (method.getName().equals("stop")) { active[0] = 0; return null; }
                    throw new UnsupportedOperationException(method.getName());
                });
        try {
            Gdx.audio = (Audio) Proxy.newProxyInstance(Audio.class.getClassLoader(),
                    new Class<?>[] {Audio.class}, (proxy, method, args) -> {
                        if (method.getName().equals("newSound")) return sound;
                        throw new UnsupportedOperationException(method.getName());
                    });
            Extensions.audio = new AudioExtension() {
                public AudioContext createContext(boolean world) { return null; }
                public float getDuration(Sound buffer) { return 1; }
                public long play(Sound buffer, float volume, float pitch, float x, float y, float z,
                        boolean spatial, float maxDistance, float refDistance, boolean looping) {
                    assertSame(sound, buffer);
                    assertTrue(looping);
                    return ++active[0];
                }
            };
            final DataSource source = (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                    new Class<?>[] {DataSource.class}, (proxy, method, args) -> {
                        if (method.getName().equals("has")) return true;
                        return null;
                    });
            final DataTable table = new DataTable(StringBundle.EMPTY);
            final Element row = new Element("MenuLoop", table);
            row.setField("FileNames", "menu.wav");
            row.setField("Flags", "LOOPING");
            row.setField("Volume", "127");
            row.setField("Pitch", "1");
            table.put("MenuLoop", row);
            final UnitSound loop = UnitSound.create(source, table, "MenuLoop", "");
            assertEquals(1, loop.getSoundCount());
            final AudioContext context = new AudioContext(AudioContext.Listener.DO_NOTHING, null);
            loop.playExclusive(context, 0, 0, 0);
            loop.playExclusive(context, 0, 0, 0);
            assertEquals(1, active[0], "Showing a menu again must not layer another loop");
            loop.stop();
            assertEquals(0, active[0]);
            loop.playExclusive(context, 0, 0, 0);
            assertEquals(1, active[0], "Returning from a mission must restart the loop");
            loop.stop();
            // Ordinary shared sound effects must retain their ability to overlap.
            loop.play(context, 0, 0, 0);
            loop.play(context, 0, 0, 0);
            assertEquals(2, active[0]);
        }
        finally {
            Gdx.audio = previousAudio;
            Extensions.audio = previousExtension;
        }
    }
}
