package com.mysticarts.event;

import com.mysticarts.MysticArts;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.registry.ModEntities;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArts.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonModEvents {
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(PlayerMagic.class);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FAMILIAR_BAT.get(), Bat.createAttributes().build());
    }
}
