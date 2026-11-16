package com.mysticarts.network;

import com.mysticarts.client.ClientFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * One visual effect, described compactly. The server sends a single small packet and every nearby
 * client draws the effect itself (see ClientFx), honouring its own quality settings.
 */
public class FxPacket {
    public static final byte EMIT = 0, SHOCKWAVE = 1, EXPLODE = 2, SWIRL = 3, PORTAL = 4,
            CIRCLE = 5, HELIX = 6, BEAM = 7, LIGHTNING = 8, STREAM = 9;

    public final byte kind;
    public final byte p1, p2;
    public final double x, y, z;
    public final float x2, y2, z2;
    public final float f1, f2;
    public final short n;

    public FxPacket(byte kind, byte p1, byte p2, double x, double y, double z,
                    float x2, float y2, float z2, float f1, float f2, short n) {
        this.kind = kind;
        this.p1 = p1;
        this.p2 = p2;
        this.x = x;
        this.y = y;
        this.z = z;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.f1 = f1;
        this.f2 = f2;
        this.n = n;
    }

    public static void encode(FxPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.kind);
        buf.writeByte(m.p1);
        buf.writeByte(m.p2);
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
        buf.writeFloat(m.x2);
        buf.writeFloat(m.y2);
        buf.writeFloat(m.z2);
        buf.writeFloat(m.f1);
        buf.writeFloat(m.f2);
        buf.writeShort(m.n);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readByte(), buf.readByte(), buf.readByte(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readShort());
    }

    public static void handle(FxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.play(msg)));
        ctx.get().setPacketHandled(true);
    }
}
