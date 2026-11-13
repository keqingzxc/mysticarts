package com.mysticarts.entity;

import com.mysticarts.util.MagicUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * One bat of a witch's swarm. It circles its owner and dives at whatever the owner is fighting
 * (or at the nearest hostile mob), biting for magic damage and draining a little health back.
 */
public class FamiliarBatEntity extends Bat {
    private UUID ownerUUID;
    private UUID targetUUID;
    private float biteDamage = 2.0F;
    private int slot;
    private int biteCooldown;

    public FamiliarBatEntity(EntityType<? extends FamiliarBatEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 0;
        if (!level.isClientSide) this.setResting(false);
    }

    // ------------------------------------------------------------------ data
    public void setOwner(Player owner) { this.ownerUUID = owner.getUUID(); }
    @Nullable public UUID getOwnerUUID() { return ownerUUID; }
    public void setBiteDamage(float d) { this.biteDamage = d; }
    public void setSlot(int s) { this.slot = s; }

    @Nullable
    private Player getOwner() {
        return ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
    }

    // ------------------------------------------------------------------ AI
    @Override
    protected void customServerAiStep() {
        // intentionally NOT calling super: the vanilla roaming/resting AI is replaced
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (biteCooldown > 0) biteCooldown--;

        if ((tickCount + slot * 3) % 10 == 0) updateTarget(owner);
        LivingEntity target = resolveTarget(owner);

        Vec3 desired;
        double speed;
        if (target != null) {
            double a = tickCount * 0.45 + slot * 1.7;
            desired = target.position().add(Math.cos(a) * 0.45, target.getBbHeight() * 0.6 + Math.sin(a * 1.3) * 0.3, Math.sin(a) * 0.45);
            speed = 0.62;
            if (biteCooldown == 0 && getBoundingBox().inflate(0.45).intersects(target.getBoundingBox())) {
                bite(target, owner);
            }
        } else {
            double a = tickCount * 0.06 + slot * 1.25;
            desired = owner.position().add(Math.cos(a) * 2.6, 1.9 + Math.sin(tickCount * 0.11 + slot) * 0.45, Math.sin(a) * 2.6);
            speed = distanceToSqr(owner) > 36 ? 0.8 : 0.32;
            if (distanceToSqr(owner) > 24 * 24) {
                this.moveTo(owner.getX(), owner.getY() + 2, owner.getZ(), getYRot(), 0);
                return;
            }
        }

        Vec3 to = desired.subtract(position());
        double len = to.length();
        Vec3 vel = len < 1.0E-3 ? Vec3.ZERO : to.scale(1.0 / len).scale(Math.min(speed, len * 0.6));
        Vec3 motion = getDeltaMovement().scale(0.55).add(vel.scale(0.45));
        if (horizontalCollision) motion = motion.add(0, 0.25, 0);
        setDeltaMovement(motion);

        if (motion.horizontalDistanceSqr() > 4.0E-4) {
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * (180.0 / Math.PI)) - 90.0F;
            setYRot(getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -30.0F, 30.0F));
            this.yHeadRot = getYRot();
            this.yBodyRot = getYRot();
        }

        // a faint trail only while diving at a target; idle bats are silent
        if (target != null && tickCount % 6 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(MagicUtil.purple(), getX(), getY() + 0.3, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private boolean valid(Player owner, LivingEntity e) {
        return e != null && e.isAlive() && e != owner && e != this
                && !(e instanceof FamiliarBatEntity) && !(e instanceof ArmorStand)
                && !(e instanceof TamableAnimal t && t.isOwnedBy(owner))
                && !(e instanceof Player && !owner.getServer().isPvpAllowed())
                && e.distanceToSqr(owner) < 22 * 22;
    }

    private void updateTarget(Player owner) {
        LivingEntity best = null;
        LivingEntity hurt = owner.getLastHurtMob();
        LivingEntity attacker = owner.getLastHurtByMob();
        if (valid(owner, attacker) && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) best = attacker;
        else if (valid(owner, hurt) && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) best = hurt;
        if (best == null) {
            AABB box = owner.getBoundingBox().inflate(12);
            List<Mob> foes = level().getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && valid(owner, m));
            best = foes.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(owner))).orElse(null);
        }
        targetUUID = best == null ? null : best.getUUID();
    }

    @Nullable
    private LivingEntity resolveTarget(Player owner) {
        if (targetUUID == null || !(level() instanceof ServerLevel sl)) return null;
        if (sl.getEntity(targetUUID) instanceof LivingEntity le && valid(owner, le)) return le;
        targetUUID = null;
        return null;
    }

    private void bite(LivingEntity target, Player owner) {
        target.hurt(level().damageSources().indirectMagic(this, owner), biteDamage);
        if (!(target instanceof Player)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        owner.heal(0.5F);
        biteCooldown = 12;
        if (level() instanceof ServerLevel sl) {
            Vec3 mid = target.position().add(0, target.getBbHeight() * 0.6, 0);
            MagicUtil.explode(sl, mid, MagicUtil.purple(), 3, 0.12);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BAT_HURT, SoundSource.NEUTRAL, 0.4F, 1.6F);
        }
    }

    // ------------------------------------------------------------------ misc
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity().getUUID().equals(ownerUUID)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Nullable
    @Override
    public SoundEvent getAmbientSound() { return null; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putFloat("BiteDamage", biteDamage);
        tag.putInt("Slot", slot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        biteDamage = tag.contains("BiteDamage") ? tag.getFloat("BiteDamage") : 2.0F;
        slot = tag.getInt("Slot");
    }
}
