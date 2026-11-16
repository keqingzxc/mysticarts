package com.mysticarts.client;

import com.mysticarts.config.ClientConfig;
import com.mysticarts.entity.FamiliarBatEntity;
import com.mysticarts.entity.MandalaEntity;
import com.mysticarts.entity.MirrorRiftEntity;
import com.mysticarts.registry.ModParticles;
import net.minecraft.world.phys.Vec3;

/** Continuous particles of entities. They are drawn on the client only, so they cost no network traffic. */
public final class EntityFx {
    private EntityFx() {}

    /** A ring of golden sparks orbiting the mandala shield. */
    public static void mandala(MandalaEntity e) {
        if (e.tickCount % 2 != 0) return;
        int pts = Math.max(6, (int) Math.round(16 * ClientConfig.density()));
        double yaw = Math.toRadians(e.getYRot());
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 c = e.position().add(0, e.getBbHeight() / 2.0, 0);
        for (int i = 0; i < pts; i++) {
            double a = 2 * Math.PI * i / pts + e.tickCount * 0.12;
            Vec3 pt = c.add(right.scale(Math.cos(a) * 1.15)).add(0, Math.sin(a) * 1.15, 0);
            e.level().addParticle(ModParticles.GOLD_SPARK.get(), pt.x, pt.y, pt.z, 0, 0, 0);
        }
    }

    /** Golden glints drifting around the mirror rift. */
    public static void rift(MirrorRiftEntity e) {
        if (e.tickCount % 3 != 0) return;
        int count = ClientConfig.density() < 0.5 ? 1 : 2;
        double yaw = Math.toRadians(e.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 center = e.position().add(0, e.getBbHeight() / 2.0, 0);
        for (int i = 0; i < count; i++) {
            double a = e.getRandom().nextDouble() * Math.PI * 2;
            double r = 0.4 + e.getRandom().nextDouble() * 1.4;
            Vec3 p = center.add(right.scale(Math.cos(a) * r)).add(0, Math.sin(a) * r, 0)
                    .add(forward.scale((e.getRandom().nextDouble() - 0.5) * 0.6));
            e.level().addParticle(e.getRandom().nextBoolean() ? ModParticles.GOLD_SPARK.get() : ModParticles.YELLOW_STAR.get(),
                    p.x, p.y, p.z, 0, 0.01, 0);
        }
    }

    /** Optional faint trail behind a bat that is diving fast. Off by default (see the client config). */
    public static void batTrail(FamiliarBatEntity e) {
        if (!ClientConfig.FAMILIAR_TRAILS.get() || e.tickCount % 5 != 0) return;
        if (e.getDeltaMovement().lengthSqr() < 0.12) return;
        e.level().addParticle(ModParticles.PURPLE_WISP.get(), e.getX(), e.getY() + 0.3, e.getZ(), 0, 0, 0);
    }
}
