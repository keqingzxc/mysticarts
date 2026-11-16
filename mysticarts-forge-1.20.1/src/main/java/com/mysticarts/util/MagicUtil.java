package com.mysticarts.util;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.fx.ParticleKit;
import com.mysticarts.network.FxPacket;
import com.mysticarts.network.ModNetwork;
import com.mysticarts.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/** Shared helpers for spells: ray casting, targeting, messages and the particle "vocabulary". */
public final class MagicUtil {
    private MagicUtil() {}

    // ---------- particle palette (custom glowing sprites) ----------
    public static ParticleOptions gold() { return ModParticles.GOLD_SPARK.get(); }
    public static ParticleOptions goldRune() { return ModParticles.GOLD_RUNE.get(); }
    public static ParticleOptions scarlet() { return ModParticles.SCARLET_SPARK.get(); }
    public static ParticleOptions scarletWisp() { return ModParticles.SCARLET_WISP.get(); }
    public static ParticleOptions purple() { return ModParticles.PURPLE_WISP.get(); }
    public static ParticleOptions purpleRune() { return ModParticles.PURPLE_RUNE.get(); }
    public static ParticleOptions yellow() { return ModParticles.YELLOW_STAR.get(); }
    public static ParticleOptions green() { return ModParticles.GREEN_TIME.get(); }

    public static ParticleOptions forSchool(MagicSchool s) {
        return switch (s) {
            case STRANGE -> gold();
            case CHAOS -> scarlet();
            case WITCHCRAFT -> purple();
            case WICCAN -> yellow();
            default -> gold();
        };
    }

    public static ParticleOptions runeFor(MagicSchool s) {
        return switch (s) {
            case STRANGE -> goldRune();
            case WITCHCRAFT -> purpleRune();
            case CHAOS -> scarletWisp();
            default -> yellow();
        };
    }

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

    // =====================================================================
    //  Effect vocabulary. Each call sends ONE small packet to nearby players; their clients draw the
    //  effect (see ClientFx), applying their own quality setting and animating it over a few ticks.
    // =====================================================================

    private static void send(ServerLevel l, FxPacket pk) {
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(pk.x, pk.y, pk.z, 64.0, l.dimension())), pk);
    }

    private static FxPacket pkt(byte kind, ParticleOptions p1, ParticleOptions p2, Vec3 a, Vec3 b, float f1, float f2, int n) {
        return new FxPacket(kind, ParticleKit.idOf(p1), ParticleKit.idOf(p2), a.x, a.y, a.z,
                (float) b.x, (float) b.y, (float) b.z, f1, f2, (short) n);
    }

    /** A single particle with a velocity. */
    public static void emit(ServerLevel l, ParticleOptions p, Vec3 pos, Vec3 vel) {
        send(l, pkt(FxPacket.EMIT, p, p, pos, vel, 0, 0, 0));
    }

    /** A single still particle (a "pixel" of a drawing). */
    public static void dot(ServerLevel l, ParticleOptions p, Vec3 pos) {
        emit(l, p, pos, Vec3.ZERO);
    }

    /** Outward-flying ring of streaks. */
    public static void shockwave(ServerLevel l, Vec3 c, ParticleOptions p, double speed, int points) {
        send(l, pkt(FxPacket.SHOCKWAVE, p, p, c, Vec3.ZERO, (float) speed, 0, points));
    }

    /** Random radial burst of rays. */
    public static void explode(ServerLevel l, Vec3 c, ParticleOptions p, int count, double speed) {
        send(l, pkt(FxPacket.EXPLODE, p, p, c, Vec3.ZERO, (float) speed, 0, count));
    }

    /** Backwards-compatible alias. */
    public static void burst(ServerLevel l, Vec3 c, ParticleOptions p, int count) {
        explode(l, c, p, count, 0.3);
    }

    /** Horizontal ring that slowly swirls. */
    public static void swirlRing(ServerLevel l, Vec3 c, double radius, int points, ParticleOptions p, double tangential) {
        send(l, pkt(FxPacket.SWIRL, p, p, c, Vec3.ZERO, (float) radius, (float) tangential, points));
    }

    /** Static horizontal ring. */
    public static void ring(ServerLevel l, Vec3 c, double radius, int points, ParticleOptions p) {
        swirlRing(l, c, radius, points, p, 0.0);
    }

    /** Ring facing `dir` (like a sling-ring portal); sparks fly around the rim. */
    public static void portalRing(ServerLevel l, Vec3 c, Vec3 dir, double radius, ParticleOptions p, int points) {
        send(l, pkt(FxPacket.PORTAL, p, p, c, dir, (float) radius, 0, points));
    }

    /** Flat ground sigil: outer ring, hexagram and runes, sketched out around the circle. */
    public static void magicCircle(ServerLevel l, Vec3 c, double radius, ParticleOptions p, ParticleOptions rune, double rot) {
        send(l, pkt(FxPacket.CIRCLE, p, rune, c, Vec3.ZERO, (float) radius, (float) rot, 0));
    }

    /** Rising spiral column. */
    public static void helix(ServerLevel l, Vec3 base, double height, double radius, ParticleOptions p, double turns) {
        send(l, pkt(FxPacket.HELIX, p, p, base, new Vec3(turns, 0, 0), (float) height, (float) radius, 0));
    }

    /** Straight static line of particles. */
    public static void beam(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p) {
        send(l, pkt(FxPacket.BEAM, p, p, from, to, 0, 0, 0));
    }

    /** Jagged energy bolt that travels from `from` to `to`. */
    public static void lightning(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p, double jitter) {
        send(l, pkt(FxPacket.LIGHTNING, p, p, from, to, (float) jitter, 0, 0));
    }

    /** Streaks flowing from `from` towards `to`. */
    public static void stream(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p, double speed) {
        send(l, pkt(FxPacket.STREAM, p, p, from, to, (float) speed, 0, 0));
    }
}
