package com.mysticarts.entity;

import com.mysticarts.magic.MirrorDimension;
import com.mysticarts.registry.ModParticles;
import com.mysticarts.util.MagicUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A rift of shattered mirrors standing in the air (no portal blocks). Anyone who walks through it
 * crosses into the Mirror Dimension, or back out of it. The visuals live in MirrorRiftRenderer.
 */
public class MirrorRiftEntity extends Entity {
    private static final EntityDataAccessor<Integer> LIFETIME =
            SynchedEntityData.defineId(MirrorRiftEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;

    public MirrorRiftEntity(EntityType<? extends MirrorRiftEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(Player owner) { this.ownerUUID = owner.getUUID(); }
    @Nullable public UUID getOwnerUUID() { return ownerUUID; }
    public int getLifetime() { return this.entityData.get(LIFETIME); }
    public void setLifetime(int ticks) { this.entityData.set(LIFETIME, ticks); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFETIME, 240);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();

        if (tickCount > getLifetime()) {
            Vec3 c = position().add(0, getBbHeight() / 2, 0);
            MagicUtil.explode(sl, c, MagicUtil.gold(), 30, 0.35);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.1f);
            discard();
            return;
        }

        double yaw = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 center = position().add(0, getBbHeight() / 2, 0);

        if (tickCount % 3 == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.4 + random.nextDouble() * 1.4;
            Vec3 p = center.add(right.scale(Math.cos(a) * r)).add(0, Math.sin(a) * r, 0)
                    .add(forward.scale((random.nextDouble() - 0.5) * 0.6));
            sl.sendParticles(random.nextBoolean() ? ModParticles.GOLD_SPARK.get() : ModParticles.YELLOW_STAR.get(),
                    p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (tickCount % 40 == 5) {
            sl.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 0.6f);
        }

        if (tickCount < 10) return; // still forming
        AABB box = getBoundingBox().inflate(0.6);
        for (ServerPlayer pl : sl.getEntitiesOfClass(ServerPlayer.class, box, pl -> !pl.isSpectator() && pl.isAlive())) {
            Vec3 d = pl.position().add(0, pl.getBbHeight() / 2, 0).subtract(center);
            if (Math.abs(d.dot(forward)) < 0.85 && Math.abs(d.dot(right)) < 1.55 && Math.abs(d.y) < 1.9) {
                MirrorDimension.pass(pl);
                // the rift starts to close shortly after someone went through
                if (getLifetime() > tickCount + 30) setLifetime(tickCount + 30);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(1.5); }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
