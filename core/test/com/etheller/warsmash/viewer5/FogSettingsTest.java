package com.etheller.warsmash.viewer5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.etheller.warsmash.units.DataTable;

class FogSettingsTest {
    private static FogSettings parse(String fields, int version) throws Exception {
        DataTable table = new DataTable(null);
        table.readTXT(new ByteArrayInputStream(("[Fog]\n" + fields).getBytes(StandardCharsets.UTF_8)), true);
        return FogSettings.parse(table.get("Fog"), version);
    }

    @Test void expansionUsesSingleRetailDefaultFogRecord() throws Exception {
        FogSettings fog = parse("Style=0\nStart=20000\nEnd=50000\nDensity=0\nColor=0,0,0,0\n", 1);
        assertEquals(FogStyle.LINEAR, fog.style);
        assertEquals(20000f, fog.start);
        assertEquals(50000f, fog.end);
        assertEquals(0f, fog.density);
    }

    @Test void expansionMenuKeepsItsOwnRecordIncludingZeroStart() throws Exception {
        FogSettings fog = parse("Style=0,0\nStart=100,0\nEnd=5500,7000\nDensity=1,1\n"
                + "Color=255,11,20,11,255,178,178,204\n", 1);
        assertEquals(0f, fog.start);
        assertEquals(7000f, fog.end);
        assertEquals(178f / 255f, fog.color.r);
        assertEquals(204f / 255f, fog.color.b);
    }

    @Test void singleColorRecordFallsBackAsAWhole() throws Exception {
        FogSettings fog = parse("Style=0\nStart=100\nEnd=5500\nDensity=1\nColor=255,11,20,33\n", 1);
        assertEquals(1f, fog.color.a);
        assertEquals(11f / 255f, fog.color.r);
        assertEquals(20f / 255f, fog.color.g);
        assertEquals(33f / 255f, fog.color.b);
    }
}
