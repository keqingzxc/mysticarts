package com.mysticarts.magic;

import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.network.ModNetwork;
import com.mysticarts.network.SyncMagicPacket;
import net.minecraft.server.level.ServerPlayer;

public final class MagicSync {
    private MagicSync() {}

    public static void sync(ServerPlayer player) {
        PlayerMagic magic = MagicCapability.of(player);
        ModNetwork.sendToPlayer(player, new SyncMagicPacket(magic.save()));
        magic.clearDirty();
    }
}
