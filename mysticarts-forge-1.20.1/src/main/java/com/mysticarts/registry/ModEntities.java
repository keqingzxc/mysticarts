package com.mysticarts.registry;

import com.mysticarts.MysticArts;
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
}
