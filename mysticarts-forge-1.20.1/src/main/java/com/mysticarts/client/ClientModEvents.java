package com.mysticarts.client;

import com.mysticarts.MysticArts;
import com.mysticarts.registry.ModEntities;
import com.mysticarts.registry.ModParticles;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MysticArts.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.CAST);
        event.register(ModKeys.NEXT);
        event.register(ModKeys.PREV);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MANDALA.get(), MandalaRenderer::new);
        event.<Bat>registerEntityRenderer(ModEntities.FAMILIAR_BAT.get(), FamiliarBatRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_RIFT.get(), MirrorRiftRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAOS_ORB.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.6F, true));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CLOAK, CloakModel::createLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new CloakLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        // glow sprites: (sprites, size, lifetime, gravity, spin, additive, tint r, g, b)
        event.registerSpriteSet(ModParticles.GOLD_SPARK.get(), s -> new GlowParticle.Provider(s, 0.20f, 22, 0.0f, false, true, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.GOLD_RUNE.get(), s -> new GlowParticle.Provider(s, 0.55f, 40, 0.0f, true, false, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.SCARLET_SPARK.get(), s -> new GlowParticle.Provider(s, 0.22f, 20, 0.0f, false, true, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.SCARLET_WISP.get(), s -> new GlowParticle.Provider(s, 0.42f, 26, -0.01f, true, false, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.PURPLE_WISP.get(), s -> new GlowParticle.Provider(s, 0.40f, 30, -0.015f, true, false, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.PURPLE_RUNE.get(), s -> new GlowParticle.Provider(s, 0.55f, 40, 0.0f, true, false, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.YELLOW_STAR.get(), s -> new GlowParticle.Provider(s, 0.26f, 24, 0.0f, true, true, 1f, 1f, 1f));
        event.registerSpriteSet(ModParticles.GREEN_TIME.get(), s -> new GlowParticle.Provider(s, 0.30f, 28, -0.01f, true, false, 1f, 1f, 1f));
        // streaks: (sprites, size, lifetime, tint r, g, b)
        event.registerSpriteSet(ModParticles.GOLD_STREAK.get(), s -> new StreakParticle.Provider(s, 0.22f, 14, 1.00f, 0.82f, 0.45f));
        event.registerSpriteSet(ModParticles.SCARLET_STREAK.get(), s -> new StreakParticle.Provider(s, 0.22f, 14, 1.00f, 0.25f, 0.30f));
        event.registerSpriteSet(ModParticles.PURPLE_STREAK.get(), s -> new StreakParticle.Provider(s, 0.22f, 15, 0.75f, 0.40f, 1.00f));
        event.registerSpriteSet(ModParticles.YELLOW_STREAK.get(), s -> new StreakParticle.Provider(s, 0.22f, 14, 1.00f, 0.92f, 0.40f));
        event.registerSpriteSet(ModParticles.GREEN_STREAK.get(), s -> new StreakParticle.Provider(s, 0.22f, 14, 0.40f, 1.00f, 0.60f));
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(MirrorSkyEffects.ID, new MirrorSkyEffects());
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("mystic_hud", MagicHud.OVERLAY);
    }
}
