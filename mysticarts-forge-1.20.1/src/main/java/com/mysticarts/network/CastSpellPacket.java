package com.mysticarts.network;

import com.mysticarts.magic.spell.SpellCaster;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CastSpellPacket {
    public static void encode(CastSpellPacket msg, FriendlyByteBuf buf) {}

    public static CastSpellPacket decode(FriendlyByteBuf buf) { return new CastSpellPacket(); }

    public static void handle(CastSpellPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) SpellCaster.cast(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
