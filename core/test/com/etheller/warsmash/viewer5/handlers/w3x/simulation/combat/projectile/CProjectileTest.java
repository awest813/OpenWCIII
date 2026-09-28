package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;

class CProjectileTest {
    @Test
    void zeroDistanceImpactHasFiniteCoordinatesAndHitsExactlyOnce() {
        final int[] hits = {0};
        final CProjectile shot = new CProjectile(12, -7, 100, new AbilityPointTarget(12, -7), true, null) {
            @Override protected void onHitTarget(CSimulation game) {
                assertEquals(12, getX());
                assertEquals(-7, getY());
                hits[0]++;
            }
        };
        assertTrue(shot.update(null));
        assertTrue(shot.update(null));
        assertEquals(1, hits[0]);
        assertEquals(12, shot.getX());
        assertEquals(-7, shot.getY());
    }
}
