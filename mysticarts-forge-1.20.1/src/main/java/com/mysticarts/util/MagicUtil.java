package com.mysticarts.util;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/** Shared helpers for spells: ray casting, particles, targeting, messages. */
public final class MagicUtil {
    private MagicUtil() {}

    public static final DustParticleOptions SCARLET = new DustParticleOptions(new Vector3f(0.92f, 0.05f, 0.15f), 1.4f);
    public static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.70f, 0.10f), 1.3f);
    public static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.62f, 0.20f, 0.88f), 1.3f);
    public static final DustParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1.0f, 0.93f, 0.30f), 1.2f);
    public static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.20f, 1.0f, 0.45f), 1.2f);

    // ---------- messages ----------
    public static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("message.mysticarts." + key, args), true);
    }

    /** Shows a message and returns false so spell effects can `return fail(...)`. */
    public static boolean fail(ServerPlayer p, String key, Object... args) {
        msg(p, key, args);
        return false;
    }

    // ---------- ray casting ----------
    public static double clipDistance(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 end = start.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? range : start.distanceTo(hit.getLocation());
    }

    public static Vec3 beamEnd(ServerPlayer p, double range) {
        return p.getEyePosition().add(p.getLookAngle().scale(clipDistance(p, range)));
    }

    public static BlockHitResult targetBlock(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 end = start.add(p.getLookAngle().scale(range));
        return p.level().clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
    }

    public static Optional<LivingEntity> targetEntity(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = start.add(look.scale(clipDistance(p, range)));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        boolean pvp = p.server.isPvpAllowed();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end, box,
                e -> e instanceof LivingEntity le && le.isAlive() && !le.isSpectator() && e.isPickable()
                        && !(e instanceof Player && !pvp)
                        && !(e instanceof TamableAnimal t && t.isOwnedBy(p)));
        return hit != null && hit.getEntity() instanceof LivingEntity le ? Optional.of(le) : Optional.empty();
    }

    // ---------- targeting ----------
    public static List<LivingEntity> nearby(ServerPlayer p, Vec3 center, double r) {
        AABB box = new AABB(center, center).inflate(r);
        boolean pvp = p.server.isPvpAllowed();
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e != p && e.isAlive() && !e.isSpectator()
                        && e.distanceToSqr(center) <= r * r
                        && !(e instanceof ArmorStand)
                        && !(e instanceof Player && !pvp)
                        && !(e instanceof TamableAnimal t && t.isOwnedBy(p)));
    }

    public static boolean magicHurt(ServerPlayer p, LivingEntity target, float damage) {
        return target.hurt(p.level().damageSources().indirectMagic(p, p), damage);
    }

    // ---------- particles ----------
    public static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.1) return;
        Vec3 step = delta.normalize().scale(0.5);
        Vec3 pos = from;
        for (double d = 0; d < len; d += 0.5) {
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            pos = pos.add(step);
        }
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y,
                    center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void burst(ServerLevel level, Vec3 pos, ParticleOptions particle, int count) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, 0.5, 0.6, 0.5, 0.05);
    }
}
