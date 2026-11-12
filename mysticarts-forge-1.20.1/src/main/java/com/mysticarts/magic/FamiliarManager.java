package com.mysticarts.magic;

import com.mysticarts.util.MagicUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Summons and runs black-cat familiars (Agatha's school). While a familiar lives it
 * grants its owner night vision and regeneration, and it hexes the nearest hostile mob.
 */
public final class FamiliarManager {
    private FamiliarManager() {}

    public static final String EXPIRE_TAG = "MysticFamiliarExpire";
    private static final int MAX_PER_PLAYER = 2;

    private record Tracked(UUID owner, long expire, ResourceKey<Level> dim) {}

    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();

    public static boolean summon(ServerPlayer p, ServerLevel level, int lifetimeTicks) {
        long owned = TRACKED.values().stream().filter(t -> t.owner().equals(p.getUUID())).count();
        if (owned >= MAX_PER_PLAYER) return false;

        Cat cat = EntityType.CAT.create(level);
        if (cat == null) return false;
        cat.moveTo(p.getX() + p.getRandom().nextDouble() - 0.5, p.getY(), p.getZ() + p.getRandom().nextDouble() - 0.5,
                p.getYRot(), 0);
        CatVariant black = BuiltInRegistries.CAT_VARIANT.get(CatVariant.ALL_BLACK);
        if (black != null) cat.setVariant(black);
        cat.tame(p);
        cat.setCollarColor(DyeColor.PURPLE);
        cat.setCustomName(Component.translatable("entity.mysticarts.familiar"));
        cat.setPersistenceRequired();
        cat.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, lifetimeTicks, 1));
        cat.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
        long expire = level.getGameTime() + lifetimeTicks;
        cat.getPersistentData().putLong(EXPIRE_TAG, expire);
        level.addFreshEntity(cat);
        TRACKED.put(cat.getUUID(), new Tracked(p.getUUID(), expire, level.dimension()));
        return true;
    }

    /** Called when a tagged familiar joins a level (e.g. after a restart). True = it should be removed. */
    public static boolean shouldRemoveOnJoin(Cat cat, long gameTime) {
        if (!cat.getPersistentData().contains(EXPIRE_TAG)) return false;
        long expire = cat.getPersistentData().getLong(EXPIRE_TAG);
        if (gameTime >= expire) return true;
        UUID owner = cat.getOwnerUUID();
        if (owner != null) TRACKED.putIfAbsent(cat.getUUID(), new Tracked(owner, expire, cat.level().dimension()));
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
                Vec3 pos = entity.position().add(0, 0.4, 0);
                MagicUtil.explode(level, pos, MagicUtil.purple(), 24, 0.25);
                MagicUtil.helix(level, entity.position(), 1.6, 0.4, MagicUtil.purple(), 2);
                entity.discard();
                it.remove();
                continue;
            }
            // owner perks
            ServerPlayer owner = server.getPlayerList().getPlayer(t.owner());
            if (owner != null && owner.level() == level) {
                owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, false));
            }
            // hex the nearest hostile every 3 seconds
            if ((level.getGameTime() / 20) % 3 == 0 && entity instanceof Cat cat) {
                List<Mob> foes = level.getEntitiesOfClass(Mob.class, cat.getBoundingBox().inflate(10),
                        m -> m instanceof Enemy && m.isAlive());
                foes.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(cat))).ifPresent(foe -> {
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
                    Vec3 from = cat.position().add(0, 0.4, 0);
                    Vec3 to = foe.position().add(0, foe.getBbHeight() / 2, 0);
                    MagicUtil.lightning(level, from, to, MagicUtil.purple(), 0.5);
                    MagicUtil.explode(level, to, MagicUtil.purple(), 8, 0.15);
                });
            }
        }
    }

    public static void clear() { TRACKED.clear(); }
}
