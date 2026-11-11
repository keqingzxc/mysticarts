package com.mysticarts.event;

import com.mysticarts.MysticArts;
import com.mysticarts.magic.FamiliarManager;
import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.MirrorDimension;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.TimeHistory;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.magic.capability.PlayerMagicProvider;
import com.mysticarts.registry.ModItems;
import com.mysticarts.util.MagicUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = MysticArts.MODID)
public class ServerEvents {

    // ------------------------------------------------------------------ capability lifecycle
    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MysticArts.MODID, "magic"), new PlayerMagicProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        MagicCapability.get(event.getOriginal()).ifPresent(old ->
                MagicCapability.get(event.getEntity()).ifPresent(neu -> {
                    neu.copyFrom(old);
                    if (event.isWasDeath()) {
                        neu.setCorruption(neu.getCorruption() / 2);
                        neu.setMana(neu.getMaxMana());
                    }
                }));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            // Flight granted by the cloak must never survive a relog (it is re-granted below if still valid).
            if (!sp.isCreative() && !sp.isSpectator()) {
                Abilities ab = sp.getAbilities();
                ab.mayfly = false;
                ab.flying = false;
                sp.onUpdateAbilities();
            }
            MagicSync.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TimeHistory.clear(sp.getUUID());
            MagicSync.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) MagicSync.sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TimeHistory.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TimeHistory.clearAll();
        FamiliarManager.clear();
    }

    // ------------------------------------------------------------------ per-tick magic
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer p)) return;

        PlayerMagic m = MagicCapability.of(p);
        long t = p.level().getGameTime();

        handleCloak(p, m, t);

        if (m.getSchool() == MagicSchool.NONE) return;

        if (t % 20 == 0) {
            int regen = 1 + m.getLevel() / 4;
            if (MirrorDimension.isIn(p)) regen *= 2;
            if (m.getCorruption() > 60) regen = Math.max(0, regen - 1);
            m.addMana(regen);

            boolean holdingDarkhold = p.getMainHandItem().is(ModItems.DARKHOLD.get())
                    || p.getOffhandItem().is(ModItems.DARKHOLD.get());
            if (t % 400 == 0 && m.getCorruption() > 0 && !holdingDarkhold) m.addCorruption(-1);
        }

        corruptionEffects(p, m, t);

        if (t % 5 == 0) TimeHistory.record(p);
        if (t % 20 == 0 && m.isDirty()) MagicSync.sync(p);
    }

    private static void handleCloak(ServerPlayer p, PlayerMagic m, long t) {
        if (p.isCreative() || p.isSpectator()) {
            m.setCloakFlight(false);
            return;
        }
        boolean wearing = p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CLOAK.get());
        boolean should = wearing && m.getMana() > 0;
        Abilities ab = p.getAbilities();

        if (should) {
            if (!m.isCloakFlight() || !ab.mayfly) {
                ab.mayfly = true;
                m.setCloakFlight(true);
                p.onUpdateAbilities();
            }
            if (ab.flying && t % 20 == 0) {
                m.consumeMana(1);
                p.serverLevel().sendParticles(MagicUtil.SCARLET, p.getX(), p.getY() + 0.8, p.getZ(),
                        3, 0.3, 0.2, 0.3, 0.0);
            }
        } else if (m.isCloakFlight()) {
            ab.mayfly = false;
            ab.flying = false;
            m.setCloakFlight(false);
            p.onUpdateAbilities();
        }
    }

    private static void corruptionEffects(ServerPlayer p, PlayerMagic m, long t) {
        int c = m.getCorruption();
        if (c < 40 || t % 100 != 0) return;
        RandomSource r = p.getRandom();

        if (r.nextFloat() < 0.30f) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
        }
        if (c >= 60 && r.nextFloat() < 0.30f) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, true));
        }
        if (c >= 80 && r.nextFloat() < 0.35f && p.level().getDifficulty() != Difficulty.PEACEFUL) {
            spawnShade(p);
        }
        if (c >= 95) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
    }

    private static void spawnShade(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vex vex = EntityType.VEX.create(level);
        if (vex == null) return;
        RandomSource r = p.getRandom();
        vex.moveTo(p.getX() + r.nextInt(5) - 2, p.getY() + 1.5, p.getZ() + r.nextInt(5) - 2, 0, 0);
        vex.finalizeSpawn(level, level.getCurrentDifficultyAt(vex.blockPosition()), MobSpawnType.EVENT, null, null);
        vex.setLimitedLife(300);
        vex.setTarget(p);
        vex.setCustomName(Component.translatable("entity.mysticarts.shade"));
        level.addFreshEntity(vex);
    }

    // ------------------------------------------------------------------ misc
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CLOAK.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getTickCount() % 20 == 0) {
            FamiliarManager.tick(server);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Wolf wolf
                && FamiliarManager.shouldRemoveOnJoin(wolf, event.getLevel().getGameTime())) {
            event.setCanceled(true);
        }
    }
}
