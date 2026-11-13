package com.mysticarts.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Full-bright glow sprite used by all Mystic Arts effects.
 * <ul>
 *   <li>Never writes to the depth buffer, so a big soft particle no longer hides the ones behind it.</li>
 *   <li>Each particle gets its own size, brightness and flicker phase for a less uniform look.</li>
 *   <li>Sparks use additive blending (real glow), wisps and runes use normal alpha blending.</li>
 * </ul>
 */
public class GlowParticle extends TextureSheetParticle {

    public static final ParticleRenderType ALPHA_NO_DEPTH = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) { tesselator.end(); }

        @Override
        public String toString() { return "mysticarts:alpha_no_depth"; }
    };

    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.defaultBlendFunc();
        }

        @Override
        public String toString() { return "mysticarts:additive"; }
    };

    protected final float baseSize;
    protected final boolean spin;
    protected final boolean additive;
    protected final float phase;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           SpriteSet sprites, float size, int life, float gravity, boolean spin, boolean additive,
                           float tr, float tg, float tb) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.94F;
        this.gravity = gravity;
        this.hasPhysics = false;
        this.baseSize = size * (0.7F + this.random.nextFloat() * 0.6F);
        this.quadSize = this.baseSize;
        this.lifetime = life + this.random.nextInt(Math.max(1, life / 3));
        this.spin = spin;
        this.additive = additive;
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        float brightness = 0.85F + this.random.nextFloat() * 0.15F;
        this.rCol = tr * brightness;
        this.gCol = tg * brightness;
        this.bCol = tb * brightness;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        if (spin) this.roll += 0.06F;
        super.tick();
        float t = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
        float fade = (float) Math.pow(1.0F - t, 1.5);
        float flicker = additive ? 0.8F + 0.2F * Mth.sin(this.age * 0.9F + phase) : 1.0F;
        this.alpha = Mth.clamp(fade * flicker, 0.0F, 1.0F);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        return this.baseSize * (1.0F - 0.5F * t);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return additive ? ADDITIVE : ALPHA_NO_DEPTH;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;
        private final int life;
        private final float gravity;
        private final boolean spin;
        private final boolean additive;
        private final float tr, tg, tb;

        public Provider(SpriteSet sprites, float size, int life, float gravity, boolean spin, boolean additive,
                        float tr, float tg, float tb) {
            this.sprites = sprites;
            this.size = size;
            this.life = life;
            this.gravity = gravity;
            this.spin = spin;
            this.additive = additive;
            this.tr = tr;
            this.tg = tg;
            this.tb = tb;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprites, size, life, gravity, spin, additive, tr, tg, tb);
        }
    }
}
