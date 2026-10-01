package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CinematicSceneTimerTest {
    @Test void transmissionExpiresAtItsDeadline() {
        final CinematicSceneTimer timer = new CinematicSceneTimer();
        timer.start(2);
        timer.advance(1.5f);
        assertTrue(timer.isActive());
        timer.advance(0.5f);
        assertFalse(timer.isActive());
    }

    @Test void replacingAndEndingTransmissionDoNotCarryOldElapsedTime() {
        final CinematicSceneTimer timer = new CinematicSceneTimer();
        timer.start(10);
        timer.advance(9);
        timer.start(2);
        timer.advance(1.5f);
        assertTrue(timer.isActive());
        timer.end();
        timer.advance(0.1f);
        assertFalse(timer.isActive());
        timer.start(1);
        assertTrue(timer.isActive());
    }

    @Test void invalidDurationsCannotLeavePermanentSubtitles() {
        final CinematicSceneTimer timer = new CinematicSceneTimer();
        for (float duration : new float[] {0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            timer.start(duration);
            assertFalse(timer.isActive());
        }
        timer.start(1);
        timer.advance(Float.NaN);
        timer.advance(-1);
        timer.advance(1);
        assertFalse(timer.isActive());
    }

    @Test void resumedTransmissionKeepsItsRemainingLifetime() {
        final CinematicSceneTimer original = new CinematicSceneTimer();
        original.start(6); original.advance(2.5f);
        final CinematicSceneTimer resumed = new CinematicSceneTimer();
        resumed.restore(original.getRemaining());
        resumed.advance(3.49f); assertTrue(resumed.isActive());
        resumed.advance(0.02f); assertFalse(resumed.isActive());
        assertThrows(IllegalArgumentException.class, () -> resumed.restore(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> resumed.restore(-1));
    }
}
