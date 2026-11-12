package com.mysticarts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mysticarts.MysticArts;
import com.mysticarts.registry.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

/** Draws the Cloak of Levitation on the player's back, with vanilla-style cape physics. */
public class CloakLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MysticArts.MODID, "textures/entity/cloak.png");
    private final ModelPart root;

    public CloakLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.root = models.bakeLayer(ModModelLayers.CLOAK);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CLOAK.get())) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        boolean crouching = player.isCrouching();

        // --- mantle + collar: they stay on the shoulders, following the body when crouching ---
        ps.pushPose();
        if (crouching) {
            ps.translate(0.0F, 0.2F, 0.0F);
            ps.mulPose(Axis.XP.rotation(0.5F));
        }
        ps.translate(0.0F, 0.0F, 0.125F);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
        for (String name : CloakModel.STATIC_PARTS) {
            root.getChild(name).render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        }
        ps.popPose();

        // --- swinging back panel (same physics as the vanilla cape) ---
        ModelPart panel = root.getChild(CloakModel.PANEL);
        panel.y = crouching ? 1.85F : 0.0F;
        panel.z = crouching ? 1.4F : 0.0F;

        ps.pushPose();
        ps.translate(0.0F, 0.0F, 0.125F);
        double dx = Mth.lerp(partialTick, player.xCloakO, player.xCloak) - Mth.lerp(partialTick, player.xo, player.getX());
        double dy = Mth.lerp(partialTick, player.yCloakO, player.yCloak) - Mth.lerp(partialTick, player.yo, player.getY());
        double dz = Mth.lerp(partialTick, player.zCloakO, player.zCloak) - Mth.lerp(partialTick, player.zo, player.getZ());
        float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        double sin = Mth.sin(bodyRot * ((float) Math.PI / 180F));
        double cos = -Mth.cos(bodyRot * ((float) Math.PI / 180F));
        float lift = Mth.clamp((float) dy * 10.0F, -6.0F, 32.0F);
        float back = Mth.clamp((float) (dx * sin + dz * cos) * 100.0F, 0.0F, 150.0F);
        float side = Mth.clamp((float) (dx * cos - dz * sin) * 100.0F, -20.0F, 20.0F);
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        lift += Mth.sin(Mth.lerp(partialTick, player.walkDistO, player.walkDist) * 6.0F) * 32.0F * bob;
        if (crouching) lift += 25.0F;
        if (player.getAbilities().flying) lift += 12.0F;

        ps.mulPose(Axis.XP.rotationDegrees(6.0F + back / 2.0F + lift));
        ps.mulPose(Axis.ZP.rotationDegrees(side / 2.0F));
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - side / 2.0F));
        panel.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
