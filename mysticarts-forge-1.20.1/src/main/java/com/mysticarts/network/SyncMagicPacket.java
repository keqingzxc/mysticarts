package com.mysticarts.network;

import com.mysticarts.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncMagicPacket {
    private final CompoundTag data;

    public SyncMagicPacket(CompoundTag data) { this.data = data; }

    public static void encode(SyncMagicPacket msg, FriendlyByteBuf buf) { buf.writeNbt(msg.data); }

    public static SyncMagicPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncMagicPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncMagicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(msg.data)));
        ctx.get().setPacketHandled(true);
    }
}
