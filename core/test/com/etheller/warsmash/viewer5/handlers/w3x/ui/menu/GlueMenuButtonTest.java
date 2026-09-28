package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.parsers.fdf.frames.GlueButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;

class GlueMenuButtonTest {
    @Test void hiddenAncestorAndDisabledButtonBlockDirectActivation() {
        final SimpleFrame panel = new SimpleFrame("panel", null);
        final GlueButtonFrame button = new GlueButtonFrame("button", panel);
        final int[] clicks = {0};
        button.setOnClick(() -> clicks[0]++);
        panel.setVisible(false);
        button.onClick(0);
        assertEquals(0, clicks[0]);
        panel.setVisible(true);
        button.setEnabled(false);
        button.onClick(0);
        assertEquals(0, clicks[0]);
        button.setEnabled(true);
        button.onClick(0);
        assertEquals(1, clicks[0]);
    }
}
