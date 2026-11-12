package com.mysticarts.registry;

import com.mysticarts.MysticArts;
import com.mysticarts.entity.ChaosOrbEntity;
import com.mysticarts.entity.MandalaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MysticArts.MODID);

    public static final RegistryObject<EntityType<MandalaEntity>> MANDALA = ENTITIES.register("mandala",
            () -> EntityType.Builder.<MandalaEntity>of(MandalaEntity::new, MobCategory.MISC)
                    .sized(2.6f, 2.6f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build(MysticArts.MODID + ":mandala"));

    public static final RegistryObject<EntityType<ChaosOrbEntity>> CHAOS_ORB = ENTITIES.register("chaos_orb",
            () -> EntityType.Builder.<ChaosOrbEntity>of(ChaosOrbEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(MysticArts.MODID + ":chaos_orb"));
}
