package com.mysticarts.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Full-bright, fading, optionally spinning glow sprite used by all Mystic Arts effects. */
public class GlowParticle extends TextureSheetParticle {
    private final float baseSize;
    private final boolean spin;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           SpriteSet sprites, float size, int life, float gravity, boolean spin) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.94F;
        this.gravity = gravity;
        this.hasPhysics = false;
        this.baseSize = size;
        this.quadSize = size;
        this.lifetime = life + this.random.nextInt(Math.max(1, life / 3));
        this.spin = spin;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        if (spin) this.roll += 0.06F;
        super.tick();
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = Mth.clamp(1.0F - t * t, 0.0F, 1.0F);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = ((float) this.age + partialTick) / (float) this.lifetime;
        return this.baseSize * (1.0F - 0.5F * Mth.clamp(t, 0.0F, 1.0F));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;
        private final int life;
        private final float gravity;
        private final boolean spin;

        public Provider(SpriteSet sprites, float size, int life, float gravity, boolean spin) {
            this.sprites = sprites;
            this.size = size;
            this.life = life;
            this.gravity = gravity;
            this.spin = spin;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprites, size, life, gravity, spin);
        }
    }
}
