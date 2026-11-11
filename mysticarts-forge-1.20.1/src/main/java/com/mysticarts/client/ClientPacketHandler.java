package com.mysticarts.client;

import com.mysticarts.magic.capability.MagicCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Client-only code kept out of the packet class so dedicated servers never load it. */
public class ClientPacketHandler {
    public static void handleSync(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MagicCapability.get(mc.player).ifPresent(m -> m.load(tag));
    }
}
