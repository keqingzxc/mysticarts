package com.mysticarts.magic;

import com.mysticarts.entity.FamiliarBatEntity;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.registry.ModEntities;
import com.mysticarts.util.MagicUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Summons and expires the witch's bat swarm. While bats are alive the owner enjoys "echolocation"
 * (night vision); each bat dives at the owner's enemies (see {@link FamiliarBatEntity}).
 */
public final class FamiliarManager {
    private FamiliarManager() {}

    public static final String EXPIRE_TAG = "MysticFamiliarExpire";
    private static final int MAX_BATS_PER_PLAYER = 10;

    private record Tracked(UUID owner, long expire, ResourceKey<Level> dim) {}

    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();

    public static boolean summon(ServerPlayer p, ServerLevel level, int lifetimeTicks) {
        long owned = TRACKED.values().stream().filter(t -> t.owner().equals(p.getUUID())).count();
        if (owned >= MAX_BATS_PER_PLAYER) return false;

        int magicLevel = MagicCapability.of(p).getLevel();
        int count = 3 + Math.min(2, magicLevel / 6);
        long expire = level.getGameTime() + lifetimeTicks;
        for (int i = 0; i < count; i++) {
            FamiliarBatEntity bat = new FamiliarBatEntity(ModEntities.FAMILIAR_BAT.get(), level);
            double a = i * 2 * Math.PI / count;
            bat.moveTo(p.getX() + Math.cos(a) * 1.2, p.getY() + 1.4, p.getZ() + Math.sin(a) * 1.2, p.getYRot(), 0);
            bat.setOwner(p);
            bat.setSlot(i);
            bat.setBiteDamage(2.0F + magicLevel * 0.25F);
            if (bat.getAttribute(Attributes.MAX_HEALTH) != null) {
                bat.getAttribute(Attributes.MAX_HEALTH).setBaseValue(6.0 + magicLevel * 0.5);
            }
            bat.setHealth(bat.getMaxHealth());
            bat.getPersistentData().putLong(EXPIRE_TAG, expire);
            level.addFreshEntity(bat);
            TRACKED.put(bat.getUUID(), new Tracked(p.getUUID(), expire, level.dimension()));
        }
        return true;
    }

    /** Called when a tagged bat joins a level (e.g. after a restart). True = it should be removed. */
    public static boolean shouldRemoveOnJoin(FamiliarBatEntity bat, long gameTime) {
        if (!bat.getPersistentData().contains(EXPIRE_TAG)) return false;
        long expire = bat.getPersistentData().getLong(EXPIRE_TAG);
        if (gameTime >= expire) return true;
        UUID owner = bat.getOwnerUUID();
        if (owner != null) TRACKED.putIfAbsent(bat.getUUID(), new Tracked(owner, expire, bat.level().dimension()));
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
                Vec3 pos = entity.position().add(0, 0.3, 0);
                MagicUtil.explode(level, pos, MagicUtil.purple(), 14, 0.2);
                entity.discard();
                it.remove();
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(t.owner());
            if (owner != null && owner.level() == level) {
                owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
            }
        }
    }

    public static void clear() { TRACKED.clear(); }
}
