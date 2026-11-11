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
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the golden spinning mandala as a double-sided, full-bright translucent quad. */
public class MandalaRenderer extends EntityRenderer<MandalaEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MysticArts.MODID, "textures/entity/mandala.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public MandalaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(MandalaEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() / 2.0f, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 3.0f));

        float s = 1.25f;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, -s, -s, 0f, 1f);
        vertex(vc, m, n, s, -s, 1f, 1f);
        vertex(vc, m, n, s, s, 1f, 0f);
        vertex(vc, m, n, -s, s, 0f, 0f);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v) {
        vc.vertex(m, x, y, 0.0f)
                .color(255, 255, 255, 235)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(n, 0.0f, 0.0f, 1.0f)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MandalaEntity entity) {
        return TEXTURE;
    }
}
