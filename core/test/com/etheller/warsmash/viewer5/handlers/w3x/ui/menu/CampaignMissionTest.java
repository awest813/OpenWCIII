package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CampaignMissionTest {
    @Test void cinematicEntriesDistinguishScriptedMapsFromMovieNames() {
        assertTrue(mission("Maps\\Campaign\\Human01.w3m").isMap());
        assertTrue(mission("Maps\\FrozenThrone\\NightElfXInterlude.w3x").isMap());
        assertTrue(mission("custom.W3X").isMap());
        assertFalse(mission("IntroX").isMap());
        assertFalse(mission("Movies\\Tutorial.mpq").isMap());
        assertFalse(mission("Movies\\intro.avi").isMap());
    }
    private CampaignMission mission(String path) { return new CampaignMission("", "", path); }
}
