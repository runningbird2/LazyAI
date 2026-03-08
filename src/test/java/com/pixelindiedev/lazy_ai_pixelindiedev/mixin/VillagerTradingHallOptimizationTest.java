package com.pixelindiedev.lazy_ai_pixelindiedev.mixin;

import com.pixelindiedev.lazy_ai_pixelindiedev.config.DistanceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerTradingHallOptimizationTest {
    @Test
    void enclosedCellCountsAsTradingCell() {
        assertTrue(VillagerTradingHallOptimization.isTradingCell(
                new boolean[]{true, true, true, true},
                new boolean[]{true, true, true, true},
                false
        ));
    }

    @Test
    void openWallDisablesTradingCellOptimization() {
        assertFalse(VillagerTradingHallOptimization.isTradingCell(
                new boolean[]{true, false, true, true},
                new boolean[]{true, false, true, true},
                true
        ));
    }

    @Test
    void partialWallRequiresCeiling() {
        assertTrue(VillagerTradingHallOptimization.isTradingCell(
                new boolean[]{true, true, true, true},
                new boolean[]{false, true, true, true},
                true
        ));
        assertFalse(VillagerTradingHallOptimization.isTradingCell(
                new boolean[]{true, true, true, true},
                new boolean[]{false, true, true, true},
                false
        ));
    }

    @Test
    void cooldownSelectionClampsUnknownDistanceTiers() {
        int[] cooldowns = {50, 90, 150};

        assertEquals(50, VillagerTradingHallOptimization.selectCooldown(cooldowns, DistanceType.CloseRange));
        assertEquals(90, VillagerTradingHallOptimization.selectCooldown(cooldowns, DistanceType.MediumRange));
        assertEquals(150, VillagerTradingHallOptimization.selectCooldown(cooldowns, DistanceType.FarRange));
        assertEquals(50, VillagerTradingHallOptimization.selectCooldown(new int[]{50}, DistanceType.FarRange));
    }

    @Test
    void invalidWallCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VillagerTradingHallOptimization.isTradingCell(
                new boolean[]{true, true, true},
                new boolean[]{true, true, true},
                true
        ));
    }
}
