package com.mysticarts.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Summons and expires tamed wolf "familiars" (Agatha's witchcraft school). */
public final class FamiliarManager {
    private FamiliarManager() {}

    public static final String EXPIRE_TAG = "MysticFamiliarExpire";
    private static final int MAX_PER_PLAYER = 2;

    private record Tracked(UUID owner, long expire, ResourceKey<Level> dim) {}

    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();

    public static boolean summon(ServerPlayer p, ServerLevel level, int lifetimeTicks) {
        long owned = TRACKED.values().stream().filter(t -> t.owner().equals(p.getUUID())).count();
        if (owned >= MAX_PER_PLAYER) return false;

        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) return false;
        wolf.moveTo(p.getX() + p.getRandom().nextDouble() - 0.5, p.getY(), p.getZ() + p.getRandom().nextDouble() - 0.5,
                p.getYRot(), 0);
        wolf.tame(p);
        wolf.setCollarColor(DyeColor.PURPLE);
        wolf.setCustomName(Component.translatable("entity.mysticarts.familiar"));
        wolf.setHealth(wolf.getMaxHealth());
        wolf.setPersistenceRequired();
        wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, lifetimeTicks, 1));
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, lifetimeTicks, 1));
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, lifetimeTicks, 1));
        long expire = level.getGameTime() + lifetimeTicks;
        wolf.getPersistentData().putLong(EXPIRE_TAG, expire);
        level.addFreshEntity(wolf);
        TRACKED.put(wolf.getUUID(), new Tracked(p.getUUID(), expire, level.dimension()));
        return true;
    }

    /** Called when a familiar wolf (tagged) joins a level, e.g. after a server restart. */
    public static boolean shouldRemoveOnJoin(Wolf wolf, long gameTime) {
        if (!wolf.getPersistentData().contains(EXPIRE_TAG)) return false;
        long expire = wolf.getPersistentData().getLong(EXPIRE_TAG);
        if (gameTime >= expire) return true;
        UUID owner = wolf.getOwnerUUID();
        if (owner != null) TRACKED.putIfAbsent(wolf.getUUID(), new Tracked(owner, expire, wolf.level().dimension()));
        return false;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Tracked>> it = TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Tracked> e = it.next();
            Tracked t = e.getValue();
            ServerLevel level = server.getLevel(t.dim());
            if (level == null) { it.remove(); continue; }
            Entity entity = level.getEntity(e.getKey());
            if (entity == null || !entity.isAlive()) {
                if (level.getGameTime() >= t.expire() || entity != null) it.remove();
                continue;
            }
            if (level.getGameTime() >= t.expire()) {
                level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        15, 0.3, 0.4, 0.3, 0.02);
                entity.discard();
                it.remove();
            }
        }
    }

    public static void clear() { TRACKED.clear(); }
}
