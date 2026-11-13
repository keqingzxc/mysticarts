package com.mysticarts.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A glow that is stretched along its direction of travel: shockwaves, bursts and energy streams. */
public class StreakParticle extends GlowParticle {

    protected StreakParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                             SpriteSet sprites, float size, int life, float tr, float tg, float tb) {
        super(level, x, y, z, vx, vy, vz, sprites, size, life, 0.0F, false, true, tr, tg, tb);
        this.friction = 0.9F;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x);
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y);
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z);

        Vec3 vel = new Vec3(this.xd, this.yd, this.zd);
        double speed = vel.length();
        if (speed < 1.0E-3) {
            super.render(vc, camera, partialTicks);
            return;
        }
        Vec3 along = vel.scale(1.0 / speed);
        Vec3 toCam = new Vec3(-px, -py, -pz);
        Vec3 side = along.cross(toCam);
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(0, 1, 0);
        float width = this.getQuadSize(partialTicks) * 0.45F;
        side = side.normalize().scale(width);
        float halfLen = Mth.clamp((float) speed * 2.6F, 0.18F, 1.5F);
        Vec3 a = along.scale(halfLen);

        float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        quadVertex(vc, px - (float) a.x - (float) side.x, py - (float) a.y - (float) side.y, pz - (float) a.z - (float) side.z, u0, v1, light);
        quadVertex(vc, px - (float) a.x + (float) side.x, py - (float) a.y + (float) side.y, pz - (float) a.z + (float) side.z, u1, v1, light);
        quadVertex(vc, px + (float) a.x + (float) side.x, py + (float) a.y + (float) side.y, pz + (float) a.z + (float) side.z, u1, v0, light);
        quadVertex(vc, px + (float) a.x - (float) side.x, py + (float) a.y - (float) side.y, pz + (float) a.z - (float) side.z, u0, v0, light);
    }

    private void quadVertex(VertexConsumer vc, float x, float y, float z, float u, float v, int light) {
        vc.vertex(x, y, z).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;
        private final int life;
        private final float tr, tg, tb;

        public Provider(SpriteSet sprites, float size, int life, float tr, float tg, float tb) {
            this.sprites = sprites;
            this.size = size;
            this.life = life;
            this.tr = tr;
            this.tg = tg;
            this.tb = tb;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new StreakParticle(level, x, y, z, vx, vy, vz, sprites, size, life, tr, tg, tb);
        }
    }
}
