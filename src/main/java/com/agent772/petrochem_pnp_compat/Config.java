package com.agent772.petrochem_pnp_compat;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue VACUUM_PASSIVE_DRAIN_MB;
    public static final ModConfigSpec.DoubleValue VACUUM_MIN_PUMP_RPM;
    public static final ModConfigSpec.BooleanValue DEBUG_VACUUM_LOGGING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VACUUM_PASSIVE_DRAIN_MB = builder
                .comment("Air a vacuum tower lets a pipe pull per drain call while no pump is running, in mB.",
                        "Pipes n' Physics applies the same trickle to liquids sitting below a pipe lip,",
                        "but it skips that rule for gases, which is why a bare pipe could hold a vacuum.",
                        "Must stay above 0 so Pipes n' Physics still classifies the tower as drainable.")
                .defineInRange("vacuumPassiveDrainMb", 4, 1, 8192);
        VACUUM_MIN_PUMP_RPM = builder
                .comment("Pump RPM needed to lift that trickle and drain the tower at the full endpoint rate.",
                        "Any pump anywhere on the attached pipe network counts, regardless of where it sits",
                        "or which way it faces, so a single fast pump on the network unlocks every vacuum",
                        "tower on it. 0 accepts any pump that is actually turning.")
                .defineInRange("vacuumMinPumpRpm", 0.0, 0.0, 4096.0);
        DEBUG_VACUUM_LOGGING = builder
                .comment("Log vacuum tower air level, pump suction and the drain rate Pipes n' Physics",
                        "actually achieves, once per second per tower. Emitted at DEBUG level, so also enable",
                        "debug logging for the petrochem_pnp_compat logger to see it.")
                .define("debugVacuumLogging", false);
        SPEC = builder.build();
    }

    private Config() {
    }
}
