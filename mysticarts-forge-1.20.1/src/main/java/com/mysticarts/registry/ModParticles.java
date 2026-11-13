package com.mysticarts.registry;

import com.mysticarts.MysticArts;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MysticArts.MODID);

    private static RegistryObject<SimpleParticleType> reg(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false));
    }

    public static final RegistryObject<SimpleParticleType> GOLD_SPARK = reg("gold_spark");
    public static final RegistryObject<SimpleParticleType> GOLD_RUNE = reg("gold_rune");
    public static final RegistryObject<SimpleParticleType> SCARLET_SPARK = reg("scarlet_spark");
    public static final RegistryObject<SimpleParticleType> SCARLET_WISP = reg("scarlet_wisp");
    public static final RegistryObject<SimpleParticleType> PURPLE_WISP = reg("purple_wisp");
    public static final RegistryObject<SimpleParticleType> PURPLE_RUNE = reg("purple_rune");
    public static final RegistryObject<SimpleParticleType> YELLOW_STAR = reg("yellow_star");
    public static final RegistryObject<SimpleParticleType> GREEN_TIME = reg("green_time");

    // elongated "streak" variants, used for shockwaves, bursts and energy streams
    public static final RegistryObject<SimpleParticleType> GOLD_STREAK = reg("gold_streak");
    public static final RegistryObject<SimpleParticleType> SCARLET_STREAK = reg("scarlet_streak");
    public static final RegistryObject<SimpleParticleType> PURPLE_STREAK = reg("purple_streak");
    public static final RegistryObject<SimpleParticleType> YELLOW_STREAK = reg("yellow_streak");
    public static final RegistryObject<SimpleParticleType> GREEN_STREAK = reg("green_streak");
}
