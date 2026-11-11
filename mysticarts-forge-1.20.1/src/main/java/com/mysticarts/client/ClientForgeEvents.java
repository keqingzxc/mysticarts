package com.mysticarts.client;

import com.mysticarts.MysticArts;
import com.mysticarts.network.CastSpellPacket;
import com.mysticarts.network.CycleSpellPacket;
import com.mysticarts.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArts.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (ModKeys.CAST.consumeClick()) ModNetwork.sendToServer(new CastSpellPacket());
        while (ModKeys.NEXT.consumeClick()) ModNetwork.sendToServer(new CycleSpellPacket(1));
        while (ModKeys.PREV.consumeClick()) ModNetwork.sendToServer(new CycleSpellPacket(-1));
    }
}
