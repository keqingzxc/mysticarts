package com.mysticarts.entity;

import com.mysticarts.registry.ModEntities;
import com.mysticarts.registry.ModItems;
import com.mysticarts.registry.ModParticles;
import com.mysticarts.util.MagicUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Thrown Chaos Orb: bursts into a scarlet explosion on impact. */
public class ChaosOrbEntity extends ThrowableItemProjectile {
    public ChaosOrbEntity(EntityType<? extends ChaosOrbEntity> type, Level level) {
        super(type, level);
    }

    public ChaosOrbEntity(Level level, LivingEntity owner) {
        super(ModEntities.CHAOS_ORB.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.CHAOS_ORB.get(); }

    @Override
    protected float getGravity() { return 0.03F; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ModParticles.SCARLET_WISP.get(), getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ModParticles.SCARLET_SPARK.get(), getX(), getY(), getZ(),
                    (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = position();
        double radius = 4.0;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius),
                en -> en.isAlive() && en != getOwner() && en.distanceToSqr(c) <= radius * radius)) {
            e.hurt(sl.damageSources().indirectMagic(this, getOwner()), 8.0f);
            Vec3 dir = e.position().subtract(c).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 1.2, 0.45, dir.z * 1.2));
            e.hurtMarked = true;
            e.setSecondsOnFire(2);
        }
        MagicUtil.shockwave(sl, c, MagicUtil.scarlet(), 0.15, 40);
        MagicUtil.shockwave(sl, c.add(0, 0.4, 0), MagicUtil.scarletWisp(), 0.25, 36);
        MagicUtil.explode(sl, c, MagicUtil.scarlet(), 40, 0.45);
        MagicUtil.explode(sl, c, MagicUtil.scarletWisp(), 20, 0.25);
        MagicUtil.magicCircle(sl, c.add(0, 0.15, 0), 2.0, MagicUtil.scarlet(), MagicUtil.scarletWisp(), 0);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.5f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
        discard();
    }
}
