package com.mysticarts.network;

import com.mysticarts.magic.spell.SpellCaster;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CycleSpellPacket {
    private final int direction;

    public CycleSpellPacket(int direction) { this.direction = direction; }

    public static void encode(CycleSpellPacket msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.direction); }

    public static CycleSpellPacket decode(FriendlyByteBuf buf) { return new CycleSpellPacket(buf.readVarInt()); }

    public static void handle(CycleSpellPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) SpellCaster.cycle(player, msg.direction > 0 ? 1 : -1);
        });
        ctx.get().setPacketHandled(true);
    }
}
