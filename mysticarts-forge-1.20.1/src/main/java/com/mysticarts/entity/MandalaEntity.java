package com.mysticarts.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Doctor Strange's mandala shield: a golden disc that floats in front of its caster,
 * deflects projectiles and pushes mobs away.
 */
public class MandalaEntity extends Entity {
    private UUID ownerUUID;
    private int lifetime = 100;

    public MandalaEntity(EntityType<? extends MandalaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(Player owner) { this.ownerUUID = owner.getUUID(); }

    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }

    public void setLifetime(int ticks) { this.lifetime = ticks; }

    @Nullable
    private Player getOwner() {
        return ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
    }

    /** Places the shield 2.2 blocks in front of the owner's eyes, facing the same way. */
    public void followOwner() {
        Player owner = getOwner();
        if (owner == null) return;
        Vec3 pos = owner.getEyePosition().add(owner.getLookAngle().scale(2.2));
        this.setPos(pos.x, pos.y - getBbHeight() / 2.0, pos.z);
        this.setYRot(owner.getYRot());
        this.yRotO = owner.getYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        Player owner = getOwner();
        if (owner == null || !owner.isAlive() || tickCount > lifetime) {
            discard();
            return;
        }
        followOwner();

        ServerLevel sl = (ServerLevel) level();
        AABB box = getBoundingBox().inflate(0.2);

        for (Entity e : sl.getEntities(this, box, ent -> ent instanceof Projectile)) {
            Projectile proj = (Projectile) e;
            if (proj.getOwner() == owner) continue;
            if (proj instanceof AbstractHurtingProjectile) {
                proj.discard();
            } else {
                proj.setDeltaMovement(proj.getDeltaMovement().scale(-0.6));
                proj.setOwner(owner);
                proj.hurtMarked = true;
            }
            sl.sendParticles(ParticleTypes.WAX_ON, proj.getX(), proj.getY(), proj.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        }

        for (Entity e : sl.getEntities(this, box, ent -> ent instanceof LivingEntity && !(ent instanceof Player))) {
            if (e instanceof TamableAnimal t && t.isOwnedBy(owner)) continue;
            Vec3 away = e.position().subtract(owner.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) continue;
            away = away.normalize().scale(0.3);
            e.push(away.x, 0.05, away.z);
            e.hurtMarked = true;
        }

        if (tickCount % 2 == 0) {
            double yaw = Math.toRadians(getYRot());
            Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            Vec3 center = position().add(0, getBbHeight() / 2.0, 0);
            for (int i = 0; i < 16; i++) {
                double a = 2 * Math.PI * i / 16 + tickCount * 0.12;
                Vec3 pt = center.add(right.scale(Math.cos(a) * 1.15)).add(0, Math.sin(a) * 1.15, 0);
                sl.sendParticles(ParticleTypes.WAX_ON, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        lifetime = tag.getInt("Lifetime");
        tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putInt("Lifetime", lifetime);
        tag.putInt("Age", tickCount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
