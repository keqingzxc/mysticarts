package com.mysticarts.fx;

import com.mysticarts.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;

/** Maps particle types to small ids for network packets and to their streak variants. */
public final class ParticleKit {
    private ParticleKit() {}

    // registration order: 0 gold_spark 1 gold_rune 2 scarlet_spark 3 scarlet_wisp 4 purple_wisp 5 purple_rune
    // 6 yellow_star 7 green_time 8 gold_streak 9 scarlet_streak 10 purple_streak 11 yellow_streak 12 green_streak
    private static final int[] STREAK_OF = {8, 8, 9, 9, 10, 10, 11, 12, 8, 9, 10, 11, 12};

    public static byte idOf(ParticleOptions p) {
        for (int i = 0; i < ModParticles.ALL.size(); i++) {
            if (ModParticles.ALL.get(i).get() == p) return (byte) i;
        }
        return 0;
    }

    public static ParticleOptions byId(int id) {
        if (id < 0 || id >= ModParticles.ALL.size()) id = 0;
        return ModParticles.ALL.get(id).get();
    }

    public static int streakId(int id) {
        return id >= 0 && id < STREAK_OF.length ? STREAK_OF[id] : id;
    }
}
