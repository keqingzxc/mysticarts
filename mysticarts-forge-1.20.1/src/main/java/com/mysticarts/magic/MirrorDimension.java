package com.mysticarts.magic;

import com.mysticarts.MysticArts;
import com.mysticarts.util.MagicUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Enter / leave the data-driven Mirror Dimension (data/mysticarts/dimension/mirror.json). */
public final class MirrorDimension {
    private MirrorDimension() {}

    public static final ResourceKey<Level> KEY =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MysticArts.MODID, "mirror"));

    public static boolean isIn(ServerPlayer p) {
        return p.level().dimension().equals(KEY);
    }

    public static boolean toggle(ServerPlayer p, PlayerMagic m) {
        MinecraftServer server = p.server;
        ServerLevel from = p.serverLevel();
        MagicUtil.explode(from, p.position().add(0, 1, 0), MagicUtil.gold(), 50, 0.5);
        MagicUtil.magicCircle(from, p.position().add(0, 0.1, 0), 2.0, MagicUtil.gold(), MagicUtil.goldRune(), 0.0);
        MagicUtil.helix(from, p.position(), 2.6, 1.0, MagicUtil.gold(), 3);

        if (isIn(p)) {
            ServerLevel dest = null;
            if (m.hasOrigin()) {
                dest = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(m.getOriginDim())));
            }
            double x, y, z;
            if (dest != null) {
                x = m.getOriginX(); y = m.getOriginY(); z = m.getOriginZ();
            } else {
                dest = server.overworld();
                BlockPos spawn = dest.getSharedSpawnPos();
                x = spawn.getX() + 0.5; y = spawn.getY(); z = spawn.getZ() + 0.5;
            }
            p.teleportTo(dest, x, y, z, p.getYRot(), p.getXRot());
            m.clearOrigin();
            MagicUtil.explode(dest, new Vec3(x, y + 1, z), MagicUtil.gold(), 50, 0.5);
            MagicUtil.magicCircle(dest, new Vec3(x, y + 0.1, z), 2.0, MagicUtil.gold(), MagicUtil.goldRune(), 0.0);
            return true;
        }

        ServerLevel mirror = server.getLevel(KEY);
        if (mirror == null) return MagicUtil.fail(p, "mirror_unavailable");

        m.setOrigin(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ());
        int x = p.getBlockX();
        int z = p.getBlockZ();
        mirror.getChunk(x >> 4, z >> 4); // make sure the chunk is generated before reading the height
        int y = mirror.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        p.teleportTo(mirror, x + 0.5, y, z + 0.5, p.getYRot(), p.getXRot());
        MagicUtil.explode(mirror, new Vec3(x + 0.5, y + 1, z + 0.5), MagicUtil.gold(), 50, 0.5);
        MagicUtil.magicCircle(mirror, new Vec3(x + 0.5, y + 0.1, z + 0.5), 2.0, MagicUtil.gold(), MagicUtil.goldRune(), 0.0);
        return true;
    }
}
