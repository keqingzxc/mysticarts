package com.mysticarts;

import com.mojang.logging.LogUtils;
import com.mysticarts.network.ModNetwork;
import com.mysticarts.registry.ModBlocks;
import com.mysticarts.registry.ModCreativeTabs;
import com.mysticarts.registry.ModEntities;
import com.mysticarts.registry.ModItems;
import com.mysticarts.registry.ModParticles;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MysticArts.MODID)
public class MysticArts {
    public static final String MODID = "mysticarts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MysticArts() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
