package com.mysticarts.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometry of the Cloak of Levitation: a long swinging back panel, a short mantle over the
 * shoulders and a raised collar behind the head. It does NOT cover the torso.
 */
public class CloakModel {
    public static final String[] STATIC_PARTS = {"mantle", "collar_back", "collar_left", "collar_right"};
    public static final String PANEL = "panel";

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(PANEL, CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6.0F, 0.0F, -1.0F, 12.0F, 22.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-6.0F, -0.5F, -1.0F, 12.0F, 3.0F, 6.0F), PartPose.ZERO);
        root.addOrReplaceChild("collar_back", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-6.0F, -6.0F, -3.0F, 12.0F, 6.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("collar_left", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-6.0F, -6.0F, -3.0F, 1.0F, 6.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("collar_right", CubeListBuilder.create().texOffs(10, 44)
                .addBox(5.0F, -6.0F, -3.0F, 1.0F, 6.0F, 4.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
