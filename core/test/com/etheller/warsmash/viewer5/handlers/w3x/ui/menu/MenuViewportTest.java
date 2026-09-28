package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.math.Rectangle;

class MenuViewportTest {
    @Test void fitsWideTallAndFourByThreeWindows() {
        assertEquals(new Rectangle(160, 0, 960, 720), MenuViewport.fit(new Rectangle(), 1280, 720, false));
        assertEquals(new Rectangle(0, 175, 600, 450), MenuViewport.fit(new Rectangle(), 600, 800, false));
        assertEquals(new Rectangle(0, 0, 800, 600), MenuViewport.fit(new Rectangle(), 800, 600, false));
    }

    @Test void fullscreenBackdropUsesTheEntireWindow() {
        assertEquals(new Rectangle(0, 0, 1280, 720), MenuViewport.fit(new Rectangle(), 1280, 720, true));
    }
}
