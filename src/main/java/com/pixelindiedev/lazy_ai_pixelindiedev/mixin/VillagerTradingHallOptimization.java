package com.pixelindiedev.lazy_ai_pixelindiedev.mixin;

import com.pixelindiedev.lazy_ai_pixelindiedev.config.DistanceType;

final class VillagerTradingHallOptimization {
    private static final int WALL_COUNT = 4;

    private VillagerTradingHallOptimization() {
    }

    static int selectCooldown(int[] cooldowns, DistanceType distanceType) {
        if (cooldowns == null || cooldowns.length == 0) {
            throw new IllegalArgumentException("Cooldown list must not be empty");
        }
        if (distanceType == null) {
            return cooldowns[cooldowns.length - 1];
        }

        int index = Math.max(0, Math.min(distanceType.ordinal(), cooldowns.length - 1));
        return cooldowns[index];
    }

    static boolean isTradingCell(boolean[] baseSolid, boolean[] upperSolid, boolean ceilingSolid) {
        if (baseSolid == null || upperSolid == null) {
            throw new IllegalArgumentException("Trading cell walls must not be null");
        }
        if (baseSolid.length != WALL_COUNT || upperSolid.length != WALL_COUNT) {
            throw new IllegalArgumentException("Trading cell checks require exactly four side walls");
        }

        int fullyBlockedDirections = 0;
        int halfBlockedDirections = 0;
        for (int i = 0; i < WALL_COUNT; i++) {
            if (baseSolid[i]) {
                if (upperSolid[i]) fullyBlockedDirections++;
                else halfBlockedDirections++;
            } else {
                if (upperSolid[i]) fullyBlockedDirections++;
                else return false;
            }
        }

        if (fullyBlockedDirections >= WALL_COUNT) {
            return true;
        }

        return halfBlockedDirections > 0 && ceilingSolid;
    }
}
