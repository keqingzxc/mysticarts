package com.mysticarts.network;

import com.mysticarts.magic.skill.SkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: buy a skill rank by id, or "reset". */
public class SkillActionPacket {
    private final String id;

    public SkillActionPacket(String id) { this.id = id; }

    public static void encode(SkillActionPacket m, FriendlyByteBuf buf) { buf.writeUtf(m.id, 64); }

    public static SkillActionPacket decode(FriendlyByteBuf buf) { return new SkillActionPacket(buf.readUtf(64)); }

    public static void handle(SkillActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) SkillManager.handle(player, msg.id);
        });
        ctx.get().setPacketHandled(true);
    }
}
