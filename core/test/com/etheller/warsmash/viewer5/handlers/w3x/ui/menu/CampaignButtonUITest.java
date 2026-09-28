package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.graphics.Color;
import com.etheller.warsmash.parsers.fdf.frames.GlueButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.datamodel.TextJustify;

class CampaignButtonUITest {
    @Test void chapterLockedAfterMouseDownCannotBeLaunchedByStaleClick() {
        final CampaignButtonUI button = new CampaignButtonUI("chapter", null);
        final GlueButtonFrame arrow = new GlueButtonFrame("arrow", button);
        button.setButtonArt(arrow);
        final int[] launches = {0};
        button.setOnClick(() -> launches[0]++);
        button.setVisible(true);
        button.setEnabled(false);
        button.onClick(0);
        arrow.onClick(0);
        assertEquals(0, launches[0]);
        button.setEnabled(true);
        button.onClick(0);
        assertEquals(1, launches[0]);
        button.setVisible(false);
        button.onClick(0);
        arrow.onClick(0);
        assertEquals(1, launches[0]);
    }

    @Test void lockedLabelsStayDimOnHoverAndRestoreTheirOriginalColors() {
        final CampaignButtonUI button = new CampaignButtonUI("chapter", null);
        button.setButtonArt(new GlueButtonFrame("arrow", button));
        final StringFrame header = label(Color.YELLOW);
        final StringFrame name = label(Color.WHITE);
        button.setHeaderText(header);
        button.setNameText(name);
        button.setEnabled(false);
        final Color dim = new Color(header.getColor());
        assertNotEquals(Color.YELLOW, dim);
        button.mouseEnter(null, null);
        button.mouseExit(null, null);
        assertEquals(dim, header.getColor());
        button.setEnabled(true);
        assertEquals(Color.YELLOW, header.getColor());
        assertEquals(Color.WHITE, name.getColor());
    }

    private static StringFrame label(Color color) {
        return new StringFrame("label", null, color, TextJustify.LEFT, TextJustify.MIDDLE, null, "Chapter", null, null);
    }
}
