package com.mysticarts.magic.spell;

import com.mysticarts.entity.MandalaEntity;
import com.mysticarts.magic.FamiliarManager;
import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.MirrorDimension;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.magic.spell.Spell.CastContext;
import com.mysticarts.registry.ModEntities;
import com.mysticarts.util.MagicUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Implementations of every spell. Each returns true only if the spell really happened. */
public final class SpellEffects {
    private SpellEffects() {}

    private static void sound(ServerLevel level, Vec3 pos, net.minecraft.sounds.SoundEvent ev, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, ev, SoundSource.PLAYERS, 1.0f, pitch);
    }

    // =====================================================================
    //  MYSTIC ARTS (Doctor Strange)
    // =====================================================================

    static boolean mandala(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        for (MandalaEntity old : level.getEntitiesOfClass(MandalaEntity.class, p.getBoundingBox().inflate(10),
                m -> p.getUUID().equals(m.getOwnerUUID()))) {
            old.discard();
        }
        MandalaEntity mandala = new MandalaEntity(ModEntities.MANDALA.get(), level);
        mandala.setOwner(p);
        mandala.setLifetime(100 + c.magic().getLevel() * 10);
        mandala.followOwner();
        level.addFreshEntity(mandala);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f);
        return true;
    }

    static boolean portalStep(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        double max = MagicUtil.clipDistance(p, 18 + c.magic().getLevel());
        for (double d = max; d >= 2.0; d -= 1.0) {
            Vec3 dest = eye.add(look.scale(d)).subtract(0, p.getEyeHeight(), 0);
            AABB box = p.getBoundingBox().move(dest.subtract(p.position()));
            if (level.noCollision(p, box)) {
                MagicUtil.burst(level, p.position().add(0, 1, 0), MagicUtil.GOLD, 30);
                p.connection.teleport(dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
                p.fallDistance = 0;
                MagicUtil.burst(level, dest.add(0, 1, 0), MagicUtil.GOLD, 30);
                sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
                return true;
            }
        }
        return MagicUtil.fail(p, "no_target");
    }

    static boolean crimsonBands(CastContext c) {
        ServerPlayer p = c.player();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 20);
        if (target.isEmpty()) return MagicUtil.fail(p, "no_target");
        LivingEntity t = target.get();
        int dur = 100 + c.magic().getLevel() * 10;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 9));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, 2));
        MagicUtil.beam(c.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() / 2, 0), MagicUtil.GOLD);
        MagicUtil.ring(c.level(), t.position().add(0, 0.3, 0), 0.8, 20, MagicUtil.GOLD);
        MagicUtil.ring(c.level(), t.position().add(0, 1.0, 0), 0.8, 20, MagicUtil.GOLD);
        sound(c.level(), t.position(), SoundEvents.CHAIN_PLACE, 0.8f);
        return true;
    }

    static boolean mirrorDimension(CastContext c) {
        return MirrorDimension.toggle(c.player(), c.magic());
    }

    // =====================================================================
    //  CHAOS MAGIC (Scarlet Witch)
    // =====================================================================

    static boolean scarletPulse(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Vec3 center = p.position().add(0, 1, 0);
        for (LivingEntity e : MagicUtil.nearby(p, center, 6)) {
            MagicUtil.magicHurt(p, e, 4.0f * c.power());
            Vec3 dir = e.position().subtract(p.position()).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 1.6, 0.5, dir.z * 1.6));
            e.hurtMarked = true;
        }
        for (double r = 1; r <= 6; r += 1.5) {
            MagicUtil.ring(level, center, r, (int) (r * 10), MagicUtil.SCARLET);
        }
        sound(level, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.6f);
        return true;
    }

    static boolean chaosBolt(CastContext c) {
        ServerPlayer p = c.player();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 30);
        Vec3 from = p.getEyePosition().subtract(0, 0.3, 0);
        Vec3 to = target.map(t -> t.position().add(0, t.getBbHeight() / 2, 0)).orElse(MagicUtil.beamEnd(p, 30));
        MagicUtil.beam(c.level(), from, to, MagicUtil.SCARLET);
        target.ifPresent(t -> {
            MagicUtil.magicHurt(p, t, 9.0f * c.power());
            t.setSecondsOnFire(3);
        });
        MagicUtil.burst(c.level(), to, MagicUtil.SCARLET, 20);
        sound(c.level(), p.position(), SoundEvents.BLAZE_SHOOT, 0.7f);
        return true;
    }

    static boolean telekinesis(CastContext c) {
        ServerPlayer p = c.player();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 20);
        if (target.isEmpty()) return MagicUtil.fail(p, "no_target");
        LivingEntity t = target.get();
        Vec3 push = p.getLookAngle().scale(2.4 * Math.min(2.0, c.power())).add(0, 0.35, 0);
        t.setDeltaMovement(push);
        t.hurtMarked = true;
        MagicUtil.magicHurt(p, t, 3.0f * c.power());
        MagicUtil.beam(c.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() / 2, 0), MagicUtil.SCARLET);
        sound(c.level(), t.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f);
        return true;
    }

    /** Turns natural blocks in a small sphere into ores - or, unpredictably, into magma or air. */
    static boolean realityWarp(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        BlockHitResult hit = MagicUtil.targetBlock(p, 24);
        if (hit.getType() != HitResult.Type.BLOCK) return MagicUtil.fail(p, "no_target");
        BlockPos center = hit.getBlockPos();
        RandomSource rnd = p.getRandom();
        int r = 2;
        int changed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distSqr(center) > r * r + 1) continue;
            BlockState st = level.getBlockState(pos);
            boolean natural = st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.BASE_STONE_NETHER)
                    || st.is(BlockTags.SAND) || st.is(BlockTags.DIRT) || st.is(Blocks.GRAVEL);
            if (!natural || !level.mayInteract(p, pos)) continue;
            float roll = rnd.nextFloat();
            BlockState ns = null;
            if (roll < 0.05f) ns = Blocks.DIAMOND_ORE.defaultBlockState();
            else if (roll < 0.12f) ns = Blocks.GOLD_ORE.defaultBlockState();
            else if (roll < 0.22f) ns = Blocks.LAPIS_ORE.defaultBlockState();
            else if (roll < 0.34f) ns = Blocks.IRON_ORE.defaultBlockState();
            else if (roll < 0.40f) ns = Blocks.MAGMA_BLOCK.defaultBlockState(); // instability
            else if (roll < 0.44f) ns = Blocks.AIR.defaultBlockState();         // instability
            if (ns != null) {
                level.setBlock(pos, ns, 3);
                changed++;
            }
        }
        if (changed == 0) return MagicUtil.fail(p, "no_target");
        Vec3 cv = Vec3.atCenterOf(center);
        MagicUtil.burst(level, cv, MagicUtil.SCARLET, 60);
        sound(level, cv, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f);
        return true;
    }

    // =====================================================================
    //  WITCHCRAFT (Agatha Harkness)
    // =====================================================================

    static boolean lifeDrain(CastContext c) {
        ServerPlayer p = c.player();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 16);
        if (target.isEmpty()) return MagicUtil.fail(p, "no_target");
        LivingEntity t = target.get();
        float dmg = 5.0f * c.power();
        MagicUtil.magicHurt(p, t, dmg);
        p.heal(dmg * 0.5f);
        c.magic().addMana(10);
        MagicUtil.beam(c.level(), t.position().add(0, t.getBbHeight() / 2, 0), p.getEyePosition().subtract(0, 0.3, 0), MagicUtil.PURPLE);
        sound(c.level(), t.position(), SoundEvents.WITCH_DRINK, 0.7f);
        return true;
    }

    static boolean hexCurse(CastContext c) {
        ServerPlayer p = c.player();
        Vec3 center = MagicUtil.beamEnd(p, 20);
        List<LivingEntity> targets = MagicUtil.nearby(p, center, 5);
        if (targets.isEmpty()) return MagicUtil.fail(p, "no_target");
        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }
        for (double r = 1; r <= 5; r += 2) MagicUtil.ring(c.level(), center.add(0, 0.2, 0), r, (int) (r * 8), MagicUtil.PURPLE);
        sound(c.level(), center, SoundEvents.WITCH_CELEBRATE, 0.8f);
        return true;
    }

    /** Witches in the circle empower each other: more witches = stronger regeneration and mana. */
    static boolean covenCircle(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        List<ServerPlayer> inCircle = level.players().stream()
                .filter(o -> o.distanceToSqr(p) <= 100 && o.isAlive()).toList();
        long witches = inCircle.stream()
                .filter(o -> MagicCapability.of(o).getSchool() == MagicSchool.WITCHCRAFT).count();
        int amp = (int) Math.min(witches - 1, 2);
        for (ServerPlayer o : inCircle) {
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, Math.max(0, amp)));
            PlayerMagic om = MagicCapability.of(o);
            if (om.getSchool() != MagicSchool.NONE) {
                om.addMana(15 + (int) witches * 5);
                if (o != p) MagicSync.sync(o);
            }
        }
        MagicUtil.ring(level, p.position().add(0, 0.1, 0), 10, 60, MagicUtil.PURPLE);
        MagicUtil.ring(level, p.position().add(0, 0.1, 0), 5, 30, MagicUtil.PURPLE);
        sound(level, p.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0f);
        return true;
    }

    static boolean familiar(CastContext c) {
        ServerPlayer p = c.player();
        int life = 1200 + c.magic().getLevel() * 60;
        if (!FamiliarManager.summon(p, c.level(), life)) return MagicUtil.fail(p, "familiar_limit");
        MagicUtil.burst(c.level(), p.position().add(0, 1, 0), MagicUtil.PURPLE, 40);
        sound(c.level(), p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f);
        return true;
    }

    // =====================================================================
    //  WICCAN (Billy Kaplan)
    // =====================================================================

    static boolean hexBolt(CastContext c) {
        ServerPlayer p = c.player();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 25);
        Vec3 from = p.getEyePosition().subtract(0, 0.3, 0);
        Vec3 to = target.map(t -> t.position().add(0, t.getBbHeight() / 2, 0)).orElse(MagicUtil.beamEnd(p, 25));
        MagicUtil.beam(c.level(), from, to, MagicUtil.YELLOW);
        target.ifPresent(t -> {
            MagicUtil.magicHurt(p, t, 6.0f * c.power());
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        });
        MagicUtil.burst(c.level(), to, MagicUtil.YELLOW, 15);
        sound(c.level(), p.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.3f);
        return true;
    }

    static boolean wiccanDash(CastContext c) {
        ServerPlayer p = c.player();
        p.setDeltaMovement(p.getLookAngle().scale(1.8 * Math.min(1.6, c.power())).add(0, 0.25, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        MagicUtil.burst(c.level(), p.position().add(0, 1, 0), MagicUtil.YELLOW, 35);
        sound(c.level(), p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f);
        return true;
    }

    /** Reveals every living creature nearby (a stand-in for seeing through others' eyes). */
    static boolean telepathy(CastContext c) {
        ServerPlayer p = c.player();
        List<LivingEntity> found = c.level().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(32), e -> e != p && e.isAlive() && !e.isSpectator());
        if (found.isEmpty()) return MagicUtil.fail(p, "no_target");
        for (LivingEntity e : found) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        MagicUtil.ring(c.level(), p.position().add(0, 1.0, 0), 2, 24, MagicUtil.YELLOW);
        sound(c.level(), p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
        return true;
    }

    /** Billy and Wanda: link with a nearby Chaos mage to share power. */
    static boolean soulLink(CastContext c) {
        ServerPlayer p = c.player();
        List<ServerPlayer> links = c.level().players().stream()
                .filter(o -> o != p && o.isAlive() && o.distanceToSqr(p) <= 900
                        && MagicCapability.of(o).getSchool() == MagicSchool.CHAOS).toList();
        if (links.isEmpty()) return MagicUtil.fail(p, "no_link");
        buffLinked(p);
        for (ServerPlayer o : links) {
            buffLinked(o);
            MagicSync.sync(o);
            MagicUtil.beam(c.level(), p.getEyePosition(), o.getEyePosition(), MagicUtil.YELLOW);
        }
        sound(c.level(), p.position(), SoundEvents.BEACON_POWER_SELECT, 1.4f);
        return true;
    }

    private static void buffLinked(ServerPlayer o) {
        MagicCapability.of(o).addMana(40);
        o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
        o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
    }
}
