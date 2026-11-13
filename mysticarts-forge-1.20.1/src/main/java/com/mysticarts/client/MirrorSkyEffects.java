package com.mysticarts.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.mysticarts.MysticArts;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Sky of the Mirror Dimension: a black violet void with a golden horizon, a drifting purple-gold
 * nebula, twinkling stars and ~70 giant mirror shards that slowly revolve across the dome and
 * flash with golden glints. There is no sun or moon.
 */
public class MirrorSkyEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = new ResourceLocation(MysticArts.MODID, "mirror");

    private static final ResourceLocation NEBULA = new ResourceLocation(MysticArts.MODID, "textures/environment/mirror_nebula.png");
    private static final ResourceLocation HALO = new ResourceLocation(MysticArts.MODID, "textures/environment/mirror_halo.png");
    private static final ResourceLocation SHARD = new ResourceLocation(MysticArts.MODID, "textures/environment/mirror_sky_shard.png");

    private static final float R = 100.0F;

    // stars: dir(3) u(3) v(3) size, r, g, b, phase
    private static final int STAR_COUNT = 1500;
    private static final int STAR_STRIDE = 14;
    private static final float[] STARS = new float[STAR_COUNT * STAR_STRIDE];

    // shards: 4 vertices * (x, y, z, u, v) = 20, then centre(3), base alpha, glint flag
    private static final int SHARD_COUNT = 70;
    private static final int SHARD_STRIDE = 25;
    private static final float[] SHARDS = new float[SHARD_COUNT * SHARD_STRIDE];

    // nebula billboards: dir(3), size, r, g, b, a, spin speed, spin phase
    private static final float[][] NEBULAE = {
            {0.60f, 0.50f, -0.60f, 85f, 0.65f, 0.25f, 0.95f, 0.60f, 0.0030f, 0.0f},
            {-0.70f, 0.30f, 0.40f, 90f, 1.00f, 0.70f, 0.30f, 0.38f, -0.0022f, 1.0f},
            {0.10f, 0.90f, 0.30f, 80f, 0.90f, 0.30f, 0.80f, 0.45f, 0.0035f, 2.0f},
            {-0.40f, 0.20f, -0.90f, 95f, 0.30f, 0.50f, 1.00f, 0.34f, -0.0028f, 3.0f},
            {0.90f, 0.10f, 0.30f, 85f, 0.80f, 0.30f, 1.00f, 0.42f, 0.0020f, 4.0f},
            {0.00f, -0.60f, -0.80f, 80f, 0.60f, 0.20f, 0.90f, 0.35f, -0.0030f, 5.0f},
            {-0.30f, 0.65f, 0.65f, 70f, 1.00f, 0.75f, 0.35f, 0.30f, 0.0025f, 6.0f},
    };

    static {
        Random r = new Random(4242L);
        for (int i = 0; i < STAR_COUNT; i++) {
            float[] d = randomDir(r);
            float[][] b = basis(d);
            int o = i * STAR_STRIDE;
            STARS[o] = d[0]; STARS[o + 1] = d[1]; STARS[o + 2] = d[2];
            STARS[o + 3] = b[0][0]; STARS[o + 4] = b[0][1]; STARS[o + 5] = b[0][2];
            STARS[o + 6] = b[1][0]; STARS[o + 7] = b[1][1]; STARS[o + 8] = b[1][2];
            STARS[o + 9] = 0.30f + (float) Math.pow(r.nextFloat(), 4) * 1.1f;
            float pick = r.nextFloat();
            float cr, cg, cb;
            if (pick < 0.60f) { cr = 0.85f; cg = 0.88f; cb = 1.00f; }
            else if (pick < 0.85f) { cr = 1.00f; cg = 0.85f; cb = 0.55f; }
            else { cr = 0.80f; cg = 0.65f; cb = 1.00f; }
            STARS[o + 10] = cr; STARS[o + 11] = cg; STARS[o + 12] = cb;
            STARS[o + 13] = r.nextFloat() * Mth.TWO_PI;
        }
        for (int i = 0; i < SHARD_COUNT; i++) {
            float[] d;
            do { d = randomDir(r); } while (d[1] < -0.2f);
            float[][] b = basis(d);
            float s = 0.06f + 0.2f * (float) Math.pow(r.nextFloat(), 2);
            float stretch = 0.45f + r.nextFloat() * 1.2f;
            int n = r.nextBoolean() ? 3 : 4;
            float base = r.nextFloat() * Mth.TWO_PI;
            float uo = (r.nextFloat() - 0.5f) * 0.1f, vo = (r.nextFloat() - 0.5f) * 0.1f;
            int o = i * SHARD_STRIDE;
            for (int k = 0; k < 4; k++) {
                int kk = Math.min(k, n - 1);
                float a = base + kk * Mth.TWO_PI / n + (r.nextFloat() - 0.5f) * 0.6f;
                float rad = s * (0.55f + 0.45f * r.nextFloat());
                float cu = Mth.cos(a) * stretch * rad, cv = Mth.sin(a) / stretch * rad;
                float px = d[0] + b[0][0] * cu + b[1][0] * cv;
                float py = d[1] + b[0][1] * cu + b[1][1] * cv;
                float pz = d[2] + b[0][2] * cu + b[1][2] * cv;
                float len = Mth.sqrt(px * px + py * py + pz * pz);
                SHARDS[o + k * 5] = px / len * R;
                SHARDS[o + k * 5 + 1] = py / len * R;
                SHARDS[o + k * 5 + 2] = pz / len * R;
                SHARDS[o + k * 5 + 3] = Mth.clamp(0.5f + Mth.cos(a) * 0.45f + uo, 0f, 1f);
                SHARDS[o + k * 5 + 4] = Mth.clamp(0.5f + Mth.sin(a) * 0.45f + vo, 0f, 1f);
            }
            SHARDS[o + 20] = d[0] * R; SHARDS[o + 21] = d[1] * R; SHARDS[o + 22] = d[2] * R;
            SHARDS[o + 23] = 0.45f + 0.35f * r.nextFloat();
            SHARDS[o + 24] = (s > 0.12f && r.nextFloat() < 0.6f) ? 1f : 0f;
        }
    }

    public MirrorSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return new Vec3(0.11, 0.05, 0.19);
    }

    @Override
    public boolean isFoggyAt(int x, int z) { return false; }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack ps, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        if (camera.getFluidInCamera() != FogType.NONE) return true; // the water fog hides the sky anyway
        float t = ticks + partialTick;

        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ps.pushPose();
        drawDome(ps.last().pose());

        // --- stars and nebula: one slowly turning group (additive) ---
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(20.0F));
        ps.mulPose(Axis.YP.rotationDegrees(t * 0.006F));
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        drawNebulae(ps.last().pose(), t);
        drawStars(ps.last().pose(), t);
        ps.popPose();

        // --- giant mirror shards: a faster group with its own axis ---
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(-25.0F));
        ps.mulPose(Axis.YP.rotationDegrees(t * 0.02F));
        Matrix4f shardMatrix = ps.last().pose();
        RenderSystem.defaultBlendFunc();
        drawShards(shardMatrix, t);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        drawGlints(shardMatrix, t);
        ps.popPose();

        ps.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    // ------------------------------------------------------------------ layers
    private static float[] domeColor(float e) {
        float up = Math.max(e, 0.0F);
        float k = (float) Math.pow(1.0F - up, 3.0);
        float r = 0.012F + (0.17F - 0.012F) * k;
        float g = 0.006F + (0.07F - 0.006F) * k;
        float b = 0.045F + (0.27F - 0.045F) * k;
        float gold = (float) Math.exp(-Math.pow(e / 0.09F, 2.0));
        r += 0.40F * gold; g += 0.24F * gold; b += 0.07F * gold;
        if (e < 0.0F) { r *= 0.5F; g *= 0.5F; b *= 0.6F; }
        return new float[]{r, g, b};
    }

    private static void drawDome(Matrix4f m) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int rings = 18, segs = 32;
        for (int i = 0; i < rings; i++) {
            float lat0 = -Mth.HALF_PI + Mth.PI * i / rings;
            float lat1 = -Mth.HALF_PI + Mth.PI * (i + 1) / rings;
            float[] c0 = domeColor(Mth.sin(lat0));
            float[] c1 = domeColor(Mth.sin(lat1));
            for (int j = 0; j < segs; j++) {
                float lon0 = Mth.TWO_PI * j / segs;
                float lon1 = Mth.TWO_PI * (j + 1) / segs;
                vert(bb, m, lat0, lon0, c0);
                vert(bb, m, lat0, lon1, c0);
                vert(bb, m, lat1, lon1, c1);
                vert(bb, m, lat1, lon0, c1);
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void vert(BufferBuilder bb, Matrix4f m, float lat, float lon, float[] c) {
        float cl = Mth.cos(lat);
        bb.vertex(m, R * cl * Mth.cos(lon), R * Mth.sin(lat), R * cl * Mth.sin(lon))
                .color(c[0], c[1], c[2], 1.0F).endVertex();
    }

    private static void drawNebulae(Matrix4f m, float t) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, NEBULA);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (float[] n : NEBULAE) {
            float[] d = normalize(n[0], n[1], n[2]);
            float[][] b = basis(d);
            float ang = n[9] + t * n[8];
            float ca = Mth.cos(ang), sa = Mth.sin(ang);
            float[] u = {b[0][0] * ca + b[1][0] * sa, b[0][1] * ca + b[1][1] * sa, b[0][2] * ca + b[1][2] * sa};
            float[] v = {-b[0][0] * sa + b[1][0] * ca, -b[0][1] * sa + b[1][1] * ca, -b[0][2] * sa + b[1][2] * ca};
            float pulse = 0.85F + 0.15F * Mth.sin(t * 0.01F + n[9]);
            quadTex(bb, m, d, u, v, n[3], 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, n[4], n[5], n[6], n[7] * pulse);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void drawStars(Matrix4f m, float t) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < STAR_COUNT; i++) {
            int o = i * STAR_STRIDE;
            float tw = Mth.sin(t * 0.04F + STARS[o + 13]);
            float a = 0.35F + 0.65F * tw * tw;
            float s = STARS[o + 9];
            float cx = STARS[o] * R, cy = STARS[o + 1] * R, cz = STARS[o + 2] * R;
            float ux = STARS[o + 3] * s, uy = STARS[o + 4] * s, uz = STARS[o + 5] * s;
            float vx = STARS[o + 6] * s, vy = STARS[o + 7] * s, vz = STARS[o + 8] * s;
            float r = STARS[o + 10], g = STARS[o + 11], bl = STARS[o + 12];
            bb.vertex(m, cx - ux - vx, cy - uy - vy, cz - uz - vz).color(r, g, bl, a).endVertex();
            bb.vertex(m, cx + ux - vx, cy + uy - vy, cz + uz - vz).color(r, g, bl, a).endVertex();
            bb.vertex(m, cx + ux + vx, cy + uy + vy, cz + uz + vz).color(r, g, bl, a).endVertex();
            bb.vertex(m, cx - ux + vx, cy - uy + vy, cz - uz + vz).color(r, g, bl, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void drawShards(Matrix4f m, float t) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHARD);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SHARD_COUNT; i++) {
            int o = i * SHARD_STRIDE;
            float a = SHARDS[o + 23] * (0.72F + 0.28F * Mth.sin(t * 0.03F + i));
            for (int k = 0; k < 4; k++) {
                bb.vertex(m, SHARDS[o + k * 5], SHARDS[o + k * 5 + 1], SHARDS[o + k * 5 + 2])
                        .uv(SHARDS[o + k * 5 + 3], SHARDS[o + k * 5 + 4])
                        .color(1.0F, 1.0F, 1.0F, a).endVertex();
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void drawGlints(Matrix4f m, float t) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, HALO);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SHARD_COUNT; i++) {
            int o = i * SHARD_STRIDE;
            if (SHARDS[o + 24] < 0.5F) continue;
            float g = Math.max(0.0F, Mth.sin(t * 0.07F + i * 1.9F));
            g = (float) Math.pow(g, 10.0);
            if (g < 0.03F) continue;
            float[] d = normalize(SHARDS[o + 20], SHARDS[o + 21], SHARDS[o + 22]);
            float[][] b = basis(d);
            quadTex(bb, m, d, b[0], b[1], 3.0F + 9.0F * g, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 1.0F, 0.86F, 0.52F, g);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    // ------------------------------------------------------------------ geometry helpers
    private static void quadTex(BufferBuilder bb, Matrix4f m, float[] d, float[] u, float[] v, float size,
                                float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                float r, float g, float b, float a) {
        float cx = d[0] * R, cy = d[1] * R, cz = d[2] * R;
        float ux = u[0] * size, uy = u[1] * size, uz = u[2] * size;
        float vx = v[0] * size, vy = v[1] * size, vz = v[2] * size;
        bb.vertex(m, cx - ux - vx, cy - uy - vy, cz - uz - vz).uv(u0, v0).color(r, g, b, a).endVertex();
        bb.vertex(m, cx + ux - vx, cy + uy - vy, cz + uz - vz).uv(u1, v1).color(r, g, b, a).endVertex();
        bb.vertex(m, cx + ux + vx, cy + uy + vy, cz + uz + vz).uv(u2, v2).color(r, g, b, a).endVertex();
        bb.vertex(m, cx - ux + vx, cy - uy + vy, cz - uz + vz).uv(u3, v3).color(r, g, b, a).endVertex();
    }

    private static float[] randomDir(Random r) {
        float y = r.nextFloat() * 2.0F - 1.0F;
        float a = r.nextFloat() * Mth.TWO_PI;
        float s = Mth.sqrt(1.0F - y * y);
        return new float[]{s * Mth.cos(a), y, s * Mth.sin(a)};
    }

    private static float[] normalize(float x, float y, float z) {
        float len = Mth.sqrt(x * x + y * y + z * z);
        return new float[]{x / len, y / len, z / len};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** Two unit vectors perpendicular to d (a tangent basis on the sky sphere). */
    private static float[][] basis(float[] d) {
        float[] ref = Math.abs(d[1]) < 0.9F ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
        float[] c = cross(d, ref);
        float[] u = normalize(c[0], c[1], c[2]);
        float[] v = cross(d, u);
        return new float[][]{u, v};
    }
}
