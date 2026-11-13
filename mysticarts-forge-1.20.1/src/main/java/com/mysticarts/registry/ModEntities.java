package com.mysticarts.registry;

import com.mysticarts.MysticArts;
import com.mysticarts.entity.ChaosOrbEntity;
import com.mysticarts.entity.FamiliarBatEntity;
import com.mysticarts.entity.MandalaEntity;
import com.mysticarts.entity.MirrorRiftEntity;
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

    public static final RegistryObject<EntityType<FamiliarBatEntity>> FAMILIAR_BAT = ENTITIES.register("familiar_bat",
            () -> EntityType.Builder.<FamiliarBatEntity>of(FamiliarBatEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.9f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(MysticArts.MODID + ":familiar_bat"));

    public static final RegistryObject<EntityType<MirrorRiftEntity>> MIRROR_RIFT = ENTITIES.register("mirror_rift",
            () -> EntityType.Builder.<MirrorRiftEntity>of(MirrorRiftEntity::new, MobCategory.MISC)
                    .sized(2.8f, 3.6f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build(MysticArts.MODID + ":mirror_rift"));
}
