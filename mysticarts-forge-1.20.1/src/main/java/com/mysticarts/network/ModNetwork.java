package com.mysticarts.network;

import com.mysticarts.MysticArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MysticArts.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CastSpellPacket.class,
                CastSpellPacket::encode, CastSpellPacket::decode, CastSpellPacket::handle);
        CHANNEL.registerMessage(id++, CycleSpellPacket.class,
                CycleSpellPacket::encode, CycleSpellPacket::decode, CycleSpellPacket::handle);
        CHANNEL.registerMessage(id++, SyncMagicPacket.class,
                SyncMagicPacket::encode, SyncMagicPacket::decode, SyncMagicPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
