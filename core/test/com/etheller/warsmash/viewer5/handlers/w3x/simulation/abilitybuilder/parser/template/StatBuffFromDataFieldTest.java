package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.template;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.impl.CAbilityTypeAbilityBuilderLevelData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;

class StatBuffFromDataFieldTest {
    @Test
    void legacyDefaultsSurviveCopyAndExplicitMapValuesOverrideThem() {
        final StatBuffFromDataField trueshot = new StatBuffFromDataField(new Gson().fromJson(
                "{\"type\":\"ATK\",\"flatBooleanField\":\"D\",\"targetMeleeField\":\"B\","
                + "\"targetRangeField\":\"C\",\"targetRangeDefault\":true}", StatBuffFromDataField.class));
        assertEquals(NonStackingStatBuffType.RNGDATKPCT, trueshot.convertToNonStackingType(data(" - ", "", "-")));
        assertEquals(NonStackingStatBuffType.MELEEATK, trueshot.convertToNonStackingType(data("1", "0", "1")));
        assertEquals(NonStackingStatBuffType.ALLATKPCT, trueshot.convertToNonStackingType(data("1", "1", "0")));
        assertThrows(NumberFormatException.class, () -> trueshot.convertToNonStackingType(data("broken", "1", "0")));
    }

    private static CAbilityTypeAbilityBuilderLevelData data(String melee, String ranged, String flat) {
        return new CAbilityTypeAbilityBuilderLevelData(EnumSet.noneOf(CTargetType.class), 0, 0, 0, 0, 0, 0,
                Collections.emptyList(), Collections.emptyList(), 0, Arrays.asList("0.1", melee, ranged, flat),
                null, Collections.emptyList());
    }
}
