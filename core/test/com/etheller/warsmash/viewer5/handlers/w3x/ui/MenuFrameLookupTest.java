package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;

class MenuFrameLookupTest {
    @Test void findsNestedControlsWithoutCrossingIntoAnotherScreen() {
        final SimpleFrame root = new SimpleFrame("OptionsMenu", null);
        final SimpleFrame layer = new SimpleFrame("OptionsControlLayer", root);
        final SimpleFrame panel = new SimpleFrame("SoundPanel", layer);
        final SimpleFrame cancel = new SimpleFrame("CancelButton", layer);
        root.add(layer);
        layer.add(panel);
        layer.add(cancel);
        assertSame(root, MenuUI.findFrameIn(root, "OptionsMenu"));
        assertSame(panel, MenuUI.findFrameIn(root, "SoundPanel"));
        assertSame(cancel, MenuUI.findFrameIn(root, "CancelButton"));
        assertNull(MenuUI.findFrameIn(panel, "CancelButton"));
        assertNull(MenuUI.findFrameIn(root, "Missing"));
    }
}
