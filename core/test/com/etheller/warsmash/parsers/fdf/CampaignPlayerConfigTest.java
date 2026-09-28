package com.etheller.warsmash.parsers.fdf;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.datasources.*;
import com.etheller.warsmash.parsers.jass.Jass2;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import mpq.MPQArchive;

class CampaignPlayerConfigTest {
    @Test void openingMissionConfigUsesTheRetailHumanSlot() throws Exception {
        List<DataSource> sources = new ArrayList<>();
        for (String path : RetailTestData.requireAll()) {
            SeekableByteChannel channel = Files.newByteChannel(Paths.get(path));
            sources.add(new MpqDataSource(new MPQArchive(channel), channel));
        }
        CompoundDataSource source = new CompoundDataSource(sources);
        try {
            for (String name : new String[] {"Maps\\Campaign\\Human01.w3m",
                    "Maps\\FrozenThrone\\Campaign\\NightElfX01.w3x"}) {
                try (War3Map map = War3MapViewer.beginLoadingMap(source, name)) {
                    GameUI ui = new GameUI(null);
                    ui.setMapStrings(key -> "String " + key);
                    War3MapConfig config = new War3MapConfig(WarsmashConstants.MAX_PLAYERS);
                    Jass2.loadConfig(map, null, null, ui, config, WarsmashConstants.JASS_FILE_LIST).config();
                    int count = 0;
                    for (com.etheller.warsmash.parsers.w3x.w3i.Player player : map.readMapInformation().getPlayers()) {
                        System.out.println(name + " slot " + player.getId() + " type " + player.getType()
                                + " config " + config.getPlayer(player.getId()).getController());
                        if (player.getType() == 1) {
                            assertEquals(CMapControl.USER, config.getPlayer(player.getId()).getController(), name);
                            count++;
                        }
                    }
                    assertEquals(1, count, name);
                }
            }
        }
        finally { source.close(); }
    }
}
