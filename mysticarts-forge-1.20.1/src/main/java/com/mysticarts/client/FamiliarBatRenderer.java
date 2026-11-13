package com.mysticarts.client;

import com.mysticarts.MysticArts;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;

/** The vanilla bat model and animation with a purple witch-bat texture. */
public class FamiliarBatRenderer extends BatRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MysticArts.MODID, "textures/entity/familiar_bat.png");

    public FamiliarBatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Bat bat) {
        return TEXTURE;
    }
}
