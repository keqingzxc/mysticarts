package com.mysticarts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mysticarts.MysticArts;
import com.mysticarts.entity.MirrorRiftEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Draws the mirror rift: a cracked mirror disc plus ~46 loose mirror shards that hover and slowly
 * spin around it. When the rift opens the shards fly in and assemble, when it closes they scatter.
 * Emissive translucent render type, so water and glass stay visible through it.
 */
public class MirrorRiftRenderer extends EntityRenderer<MirrorRiftEntity> {
    private static final ResourceLocation GLASS = new ResourceLocation(MysticArts.MODID, "textures/entity/mirror_rift_glass.png");
    private static final ResourceLocation SHARD = new ResourceLocation(MysticArts.MODID, "textures/entity/mirror_rift_shard.png");

    private static final int N = 46;
    /** per shard: angle, radius, z, size, spinZ, spinY, phase, u0, v0, then 3 triangle vertices (x,y) */
    private static final float[][] S = new float[N][15];

    static {
        Random r = new Random(1234L);
        for (int i = 0; i < N; i++) {
            S[i][0] = (float) (r.nextDouble() * Math.PI * 2);
            S[i][1] = (float) (0.25 + 1.5 * Math.pow(r.nextDouble(), 0.7));
            S[i][2] = (r.nextFloat() - 0.5f) * 1.2f;
            S[i][3] = 0.16f + r.nextFloat() * 0.30f;
            S[i][4] = (r.nextFloat() - 0.5f) * 3.0f;
            S[i][5] = (r.nextFloat() - 0.5f) * 2.0f;
            S[i][6] = r.nextFloat() * 360f;
            S[i][7] = r.nextFloat();
            S[i][8] = r.nextFloat();
            for (int k = 0; k < 3; k++) {
                double a = k * Math.PI * 2 / 3 + (r.nextDouble() - 0.5) * 0.9;
                double rad = 0.6 + 0.7 * r.nextDouble();
                S[i][9 + 2 * k] = (float) (Math.cos(a) * rad);
                S[i][10 + 2 * k] = (float) (Math.sin(a) * rad);
            }
        }
    }

    public MirrorRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(MirrorRiftEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float t = entity.tickCount + partialTick;
        int life = entity.getLifetime();
        float open = Mth.clamp(t / 14.0f, 0.0f, 1.0f);
        float close = Mth.clamp((life - t) / 12.0f, 0.0f, 1.0f);
        float k = Math.min(open, close);
        float e = 1.0f - (1.0f - k) * (1.0f - k) * (1.0f - k);
        if (e <= 0.001f) return;

        ps.pushPose();
        ps.translate(0.0, entity.getBbHeight() / 2.0f, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - entity.getYRot()));

        // --- cracked mirror disc ---
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(t * 0.25f));
        float gs = 1.95f * (0.55f + 0.45f * e);
        int ga = (int) (200 * e * (0.92f + 0.08f * Mth.sin(t * 0.2f)));
        VertexConsumer gv = buffer.getBuffer(RenderType.entityTranslucentEmissive(GLASS));
        quad(gv, ps, -gs, -gs, gs, -gs, gs, gs, -gs, gs, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, ga);
        ps.popPose();

        // --- floating shards ---
        VertexConsumer sv = buffer.getBuffer(RenderType.entityTranslucentEmissive(SHARD));
        int sa = (int) (240 * e);
        for (int i = 0; i < N; i++) {
            float[] s = S[i];
            float fx = s[1] * Mth.cos(s[0]) + 0.10f * Mth.sin(t * 0.06f + s[6]);
            float fy = s[1] * Mth.sin(s[0]) + 0.08f * Mth.cos(t * 0.05f + s[6]);
            float fz = s[2];
            float x = Mth.lerp(e, fx * 2.8f, fx);
            float y = Mth.lerp(e, fy * 2.8f + 0.5f, fy);
            float z = Mth.lerp(e, fz * 2.8f - 1.5f, fz);
            float size = s[3] * (0.5f + 0.5f * e);

            ps.pushPose();
            ps.translate(x, y, z);
            ps.mulPose(Axis.ZP.rotationDegrees(s[6] + t * s[4]));
            ps.mulPose(Axis.XP.rotationDegrees(25.0f * Mth.sin(t * 0.04f + s[6])));
            ps.mulPose(Axis.YP.rotationDegrees(t * s[5] * 3.0f));
            PoseStack.Pose pose = ps.last();
            Matrix4f m = pose.pose();
            Matrix3f n = pose.normal();
            float x0 = s[9] * size, y0 = s[10] * size, x1 = s[11] * size, y1 = s[12] * size, x2 = s[13] * size, y2 = s[14] * size;
            float u0 = s[7], v0 = s[8];
            vertex(sv, m, n, x0, y0, clamp01(u0 * 0.5f + 0.25f + s[9] * 0.3f), clamp01(v0 * 0.5f + 0.25f + s[10] * 0.3f), sa);
            vertex(sv, m, n, x1, y1, clamp01(u0 * 0.5f + 0.25f + s[11] * 0.3f), clamp01(v0 * 0.5f + 0.25f + s[12] * 0.3f), sa);
            vertex(sv, m, n, x2, y2, clamp01(u0 * 0.5f + 0.25f + s[13] * 0.3f), clamp01(v0 * 0.5f + 0.25f + s[14] * 0.3f), sa);
            vertex(sv, m, n, x2, y2, clamp01(u0 * 0.5f + 0.25f + s[13] * 0.3f), clamp01(v0 * 0.5f + 0.25f + s[14] * 0.3f), sa);
            ps.popPose();
        }
        ps.popPose();
    }

    private static float clamp01(float v) { return Mth.clamp(v, 0.0f, 1.0f); }

    private static void quad(VertexConsumer vc, PoseStack ps, float x0, float y0, float x1, float y1, float x2, float y2,
                             float x3, float y3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                             int alpha) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, x0, y0, u0, v0, alpha);
        vertex(vc, m, n, x1, y1, u1, v1, alpha);
        vertex(vc, m, n, x2, y2, u2, v2, alpha);
        vertex(vc, m, n, x3, y3, u3, v3, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int alpha) {
        vc.vertex(m, x, y, 0.0f)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(n, 0.0f, 0.0f, 1.0f)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MirrorRiftEntity entity) { return GLASS; }
}
