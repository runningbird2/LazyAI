package com.pixelindiedev.lazy_ai_pixelindiedev.config;

public class ModConfigDefaults {
    public static final DistanceScalingType Defaults_DistanceScaling = DistanceScalingType.Medium;
    public static final DistanceThresholdMode Defaults_DistanceThresholdMode = DistanceThresholdMode.SimulationScaled;
    public static final OptimalizationType Defaults_AIOptimizationType = OptimalizationType.Dynamic;
    //    Distance in squared blocks
    //    distance is based on simulation distance
    public static final int Defaults_BlockDistance_Close = 64;
    public static final int Defaults_BlockDistance_Far = 196;
    public static final int Defaults_FixedDistance_CloseBlocks = 64;
    public static final int Defaults_FixedDistance_FarBlocks = 96;
    public static final TemptDelayEnum Defaults_TemptDelay = TemptDelayEnum.Low;
    public static final boolean Defaults_DisableZombieEggStomping = false;
    public static final boolean Defaults_NeverSlowdownDistantMobs = false;
    public static final boolean Defaults_EnableVillagerTradingHallOptimization = true;
}
