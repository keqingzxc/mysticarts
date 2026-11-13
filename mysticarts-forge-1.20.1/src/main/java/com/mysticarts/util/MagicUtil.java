package com.mysticarts.util;

import com.mysticarts.magic.MagicSchool;
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

    // ---------- density / streaks ----------
    /** Global effect density (1.0 = everything). Lower means less visual noise. */
    private static final double DENSITY = 0.6;

    private static int scaled(int count) {
        return Math.max(1, (int) Math.round(count * DENSITY));
    }

    /** The stretched "streak" variant of a glow particle, used for anything that flies. */
    public static ParticleOptions streakOf(ParticleOptions p) {
        if (p == ModParticles.GOLD_SPARK.get() || p == ModParticles.GOLD_RUNE.get()) return ModParticles.GOLD_STREAK.get();
        if (p == ModParticles.SCARLET_SPARK.get() || p == ModParticles.SCARLET_WISP.get()) return ModParticles.SCARLET_STREAK.get();
        if (p == ModParticles.PURPLE_WISP.get() || p == ModParticles.PURPLE_RUNE.get()) return ModParticles.PURPLE_STREAK.get();
        if (p == ModParticles.YELLOW_STAR.get()) return ModParticles.YELLOW_STREAK.get();
        if (p == ModParticles.GREEN_TIME.get()) return ModParticles.GREEN_STREAK.get();
        return p;
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
    //  Particle vocabulary. count = 0 makes the client treat (dx,dy,dz) as a velocity,
    //  which lets particles fly: shockwaves, streams, spirals.
    // =====================================================================

    public static void emit(ServerLevel l, ParticleOptions p, Vec3 pos, Vec3 vel) {
        l.sendParticles(p, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1.0);
    }

    /** Still particle (a "pixel" of a drawing). */
    public static void dot(ServerLevel l, ParticleOptions p, Vec3 pos) {
        emit(l, p, pos, Vec3.ZERO);
    }

    /** Outward-flying ring: a shockwave. */
    public static void shockwave(ServerLevel l, Vec3 c, ParticleOptions p, double speed, int points) {
        int n = Math.max(10, scaled(points));
        ParticleOptions streak = streakOf(p);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            emit(l, streak, c, new Vec3(Math.cos(a) * speed, 0.0, Math.sin(a) * speed));
        }
    }

    /** Random radial burst. */
    public static void explode(ServerLevel l, Vec3 c, ParticleOptions p, int count, double speed) {
        RandomSource r = l.getRandom();
        ParticleOptions streak = streakOf(p);
        int n = scaled(count);
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
            if (d.lengthSqr() < 1.0E-6) continue;
            // mostly streaks (rays), with some still sparks for variety
            ParticleOptions use = (i % 3 == 2) ? p : streak;
            emit(l, use, c, d.normalize().scale(speed * (0.35 + 0.65 * r.nextDouble())));
        }
    }

    /** Backwards-compatible alias. */
    public static void burst(ServerLevel l, Vec3 c, ParticleOptions p, int count) {
        explode(l, c, p, count, 0.3);
    }

    /** Horizontal ring that slowly swirls. */
    public static void swirlRing(ServerLevel l, Vec3 c, double radius, int points, ParticleOptions p, double tangential) {
        int n = Math.max(8, scaled(points));
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            Vec3 pos = c.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            emit(l, p, pos, new Vec3(-Math.sin(a) * tangential, 0.01, Math.cos(a) * tangential));
        }
    }

    /** Alias for a static horizontal ring. */
    public static void ring(ServerLevel l, Vec3 c, double radius, int points, ParticleOptions p) {
        swirlRing(l, c, radius, points, p, 0.0);
    }

    /** Ring facing `dir` (like a sling-ring portal), sparks fly around the rim. */
    public static void portalRing(ServerLevel l, Vec3 c, Vec3 dir, double radius, ParticleOptions p, int points) {
        Vec3 n = dir.normalize();
        Vec3 up = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = n.cross(up).normalize();
        Vec3 v = n.cross(u).normalize();
        int pts = Math.max(10, (int) Math.round(points * 0.75));
        for (int i = 0; i < pts; i++) {
            double a = 2 * Math.PI * i / pts;
            Vec3 radial = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            Vec3 tangent = u.scale(-Math.sin(a)).add(v.scale(Math.cos(a)));
            emit(l, p, c.add(radial.scale(radius)), tangent.scale(0.10));
        }
    }

    /** Flat ground sigil: outer ring, hexagram and runes. */
    public static void magicCircle(ServerLevel l, Vec3 c, double radius, ParticleOptions p, ParticleOptions rune, double rot) {
        int n = Math.max(20, (int) (radius * 10));
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            dot(l, p, c.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
        }
        double hr = radius * 0.92;
        Vec3[] hex = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            double a = rot + Math.PI / 3 * i;
            hex[i] = c.add(Math.cos(a) * hr, 0, Math.sin(a) * hr);
        }
        for (int tri = 0; tri < 2; tri++) {
            for (int e = 0; e < 3; e++) {
                Vec3 from = hex[(tri + e * 2) % 6];
                Vec3 to = hex[(tri + (e + 1) * 2) % 6];
                int steps = Math.max(3, (int) (from.distanceTo(to) / 0.75));
                for (int s = 0; s <= steps; s++) dot(l, p, from.add(to.subtract(from).scale(s / (double) steps)));
            }
        }
        for (int i = 0; i < 6; i++) {
            double a = rot + Math.PI / 6 + Math.PI / 3 * i;
            emit(l, rune, c.add(Math.cos(a) * radius * 0.62, 0.08, Math.sin(a) * radius * 0.62), new Vec3(0, 0.01, 0));
        }
        emit(l, rune, c.add(0, 0.1, 0), new Vec3(0, 0.01, 0));
    }

    /** Rising spiral column. */
    public static void helix(ServerLevel l, Vec3 base, double height, double radius, ParticleOptions p, double turns) {
        int steps = Math.max(8, (int) (height * 6));
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            double a = t * turns * 2 * Math.PI;
            emit(l, p, base.add(Math.cos(a) * radius, t * height, Math.sin(a) * radius), new Vec3(0, 0.03, 0));
        }
    }

    /** Straight static line of particles. */
    public static void beam(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.1) return;
        Vec3 step = delta.normalize().scale(0.45);
        Vec3 pos = from;
        for (double d = 0; d < len; d += 0.45) {
            dot(l, p, pos);
            pos = pos.add(step);
        }
    }

    /** Jagged energy bolt. */
    public static void lightning(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p, double jitter) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.2) return;
        RandomSource r = l.getRandom();
        int segs = Math.max(2, (int) (len / 0.8));
        Vec3 prev = from;
        for (int s = 1; s <= segs; s++) {
            Vec3 base = from.add(delta.scale(s / (double) segs));
            Vec3 cur = s == segs ? base : base.add((r.nextDouble() - 0.5) * jitter, (r.nextDouble() - 0.5) * jitter,
                    (r.nextDouble() - 0.5) * jitter);
            beam(l, prev, cur, p);
            prev = cur;
        }
    }

    /** Particles flowing from `from` towards `to`. */
    public static void stream(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p, double speed) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.2) return;
        Vec3 dir = delta.normalize();
        ParticleOptions streak = streakOf(p);
        for (double t = 0; t < len; t += 0.9) emit(l, streak, from.add(dir.scale(t)), dir.scale(speed));
    }
}
