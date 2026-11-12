package com.mysticarts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mysticarts.MysticArts;
import com.mysticarts.entity.MandalaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Two counter-rotating golden layers. Uses the emissive translucent render type, which does not
 * write to the depth buffer, so water and other translucent blocks stay visible through the shield.
 */
public class MandalaRenderer extends EntityRenderer<MandalaEntity> {
    private static final ResourceLocation OUTER = new ResourceLocation(MysticArts.MODID, "textures/entity/mandala_outer.png");
    private static final ResourceLocation INNER = new ResourceLocation(MysticArts.MODID, "textures/entity/mandala_inner.png");

    public MandalaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(MandalaEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float t = entity.tickCount + partialTick;
        float grow = Mth.clamp(t / 5.0f, 0.0f, 1.0f);
        float size = 1.3f * (0.4f + 0.6f * grow);
        int alpha = (int) (205 + 35 * Mth.sin(t * 0.25f));

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() / 2.0f, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entity.getYRot()));

        layer(poseStack, buffer, OUTER, t * 1.6f, size, alpha, 0.0f);
        layer(poseStack, buffer, INNER, -t * 3.2f, size, alpha, 0.01f);

        poseStack.popPose();
    }

    private static void layer(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, float angle,
                              float s, int alpha, float zOffset) {
        ps.pushPose();
        ps.translate(0.0, 0.0, zOffset);
        ps.mulPose(Axis.ZP.rotationDegrees(angle));
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(tex));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, -s, -s, 0f, 1f, alpha);
        vertex(vc, m, n, s, -s, 1f, 1f, alpha);
        vertex(vc, m, n, s, s, 1f, 0f, alpha);
        vertex(vc, m, n, -s, s, 0f, 0f, alpha);
        ps.popPose();
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
    public ResourceLocation getTextureLocation(MandalaEntity entity) {
        return OUTER;
    }
}
