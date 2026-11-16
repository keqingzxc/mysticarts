package com.mysticarts.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-side visual settings (config/mysticarts-client.toml). */
public final class ClientConfig {
    private ClientConfig() {}

    public enum Quality {
        LOW(0.35), MEDIUM(0.6), HIGH(1.0);

        public final double factor;

        Quality(double factor) { this.factor = factor; }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Quality> QUALITY;
    public static final ForgeConfigSpec.BooleanValue STREAKS;
    public static final ForgeConfigSpec.BooleanValue FAMILIAR_TRAILS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("effects");
        QUALITY = b.comment("How many particles spells and artifacts spawn: LOW, MEDIUM or HIGH.")
                .defineEnum("quality", Quality.MEDIUM);
        STREAKS = b.comment("Draw bursts, shockwaves and energy streams as stretched streaks (otherwise as plain glows).")
                .define("streaks", true);
        FAMILIAR_TRAILS = b.comment("Leave a faint trail behind the witch bats while they dive at an enemy.")
                .define("familiarTrails", false);
        b.pop();
        SPEC = b.build();
    }

    public static double density() {
        return QUALITY.get().factor;
    }
}
