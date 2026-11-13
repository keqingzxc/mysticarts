package com.mysticarts.magic;

import com.mysticarts.MysticArts;
import com.mysticarts.entity.MirrorRiftEntity;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.registry.ModEntities;
import com.mysticarts.util.MagicUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Mirror Dimension (data/mysticarts/dimension/mirror.json). It is entered through a rift of
 * shattered mirrors that the caster conjures in front of them: {@link #openRift} creates the rift,
 * {@link #pass} moves a player that walks into it to the other side.
 */
public final class MirrorDimension {
    private MirrorDimension() {}

    public static final ResourceKey<Level> KEY =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MysticArts.MODID, "mirror"));

    public static boolean isIn(ServerPlayer p) {
        return p.level().dimension().equals(KEY);
    }

    /** Conjures the rift 3 blocks in front of the player. */
    public static boolean openRift(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!isIn(p) && p.server.getLevel(KEY) == null) return MagicUtil.fail(p, "mirror_unavailable");

        for (MirrorRiftEntity old : level.getEntitiesOfClass(MirrorRiftEntity.class, p.getBoundingBox().inflate(12),
                r -> p.getUUID().equals(r.getOwnerUUID()))) {
            old.discard();
        }

        Vec3 h = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
        h = h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(h.scale(3.4)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double dist = hit.getType() == HitResult.Type.MISS ? 3.0 : Math.max(1.4, eye.distanceTo(hit.getLocation()) - 0.9);
        Vec3 pos = new Vec3(p.getX() + h.x * dist, p.getY(), p.getZ() + h.z * dist);

        MirrorRiftEntity rift = new MirrorRiftEntity(ModEntities.MIRROR_RIFT.get(), level);
        rift.setPos(pos.x, pos.y, pos.z);
        rift.setYRot(p.getYRot());
        rift.yRotO = p.getYRot();
        rift.setOwner(p);
        rift.setLifetime(240);
        level.addFreshEntity(rift);

        Vec3 center = pos.add(0, 1.8, 0);
        MagicUtil.portalRing(level, center, h, 1.8, MagicUtil.gold(), 56);
        MagicUtil.portalRing(level, center, h, 1.1, MagicUtil.goldRune(), 10);
        MagicUtil.explode(level, center, MagicUtil.gold(), 50, 0.45);
        MagicUtil.explode(level, center, MagicUtil.yellow(), 20, 0.3);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 0.5f);
        return true;
    }

    /** Moves a player who walked through a rift: into the Mirror Dimension, or back to where they came from. */
    public static void pass(ServerPlayer p) {
        MinecraftServer server = p.server;
        PlayerMagic m = MagicCapability.of(p);
        ServerLevel from = p.serverLevel();
        shatterBurst(from, p.position().add(0, 1, 0));

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
            shatterBurst(dest, new Vec3(x, y + 1, z));
            MagicSync.sync(p);
            return;
        }

        ServerLevel mirror = server.getLevel(KEY);
        if (mirror == null) {
            MagicUtil.msg(p, "mirror_unavailable");
            return;
        }
        m.setOrigin(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ());
        int x = p.getBlockX();
        int z = p.getBlockZ();
        mirror.getChunk(x >> 4, z >> 4); // make sure the chunk exists before reading the height
        int y = mirror.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        p.teleportTo(mirror, x + 0.5, y, z + 0.5, p.getYRot(), p.getXRot());
        shatterBurst(mirror, new Vec3(x + 0.5, y + 1, z + 0.5));
        MagicSync.sync(p);
    }

    private static void shatterBurst(ServerLevel level, Vec3 pos) {
        MagicUtil.explode(level, pos, MagicUtil.gold(), 50, 0.5);
        MagicUtil.explode(level, pos, MagicUtil.yellow(), 20, 0.35);
        MagicUtil.shockwave(level, pos.subtract(0, 0.8, 0), MagicUtil.gold(), 0.25, 32);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
