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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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

    private static void sound(ServerLevel level, Vec3 pos, SoundEvent ev, float vol, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, ev, SoundSource.PLAYERS, vol, pitch);
    }

    private static Vec3 chest(ServerPlayer p) { return p.position().add(0, 1.1, 0); }
    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().subtract(0, 0.35, 0).add(p.getLookAngle().scale(0.4)); }
    private static Vec3 mid(LivingEntity e) { return e.position().add(0, e.getBbHeight() / 2, 0); }

    // =====================================================================
    //  MYSTIC ARTS (Doctor Strange): golden sling-ring sparks and mandalas
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

        Vec3 look = p.getLookAngle();
        Vec3 center = mandala.position().add(0, mandala.getBbHeight() / 2.0, 0);
        MagicUtil.portalRing(level, center, look, 1.25, MagicUtil.gold(), 36);
        MagicUtil.portalRing(level, center, look, 0.7, MagicUtil.goldRune(), 8);
        MagicUtil.explode(level, hand(p), MagicUtil.gold(), 18, 0.25);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f, 1.3f);
        sound(level, p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 1.6f);
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
                Vec3 from = chest(p);
                MagicUtil.portalRing(level, from.add(look.scale(0.8)), look, 1.1, MagicUtil.gold(), 40);
                MagicUtil.portalRing(level, from.add(look.scale(0.8)), look, 0.6, MagicUtil.goldRune(), 6);
                MagicUtil.explode(level, from, MagicUtil.gold(), 25, 0.3);
                p.connection.teleport(dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
                p.fallDistance = 0;
                Vec3 to = dest.add(0, 1.1, 0);
                MagicUtil.portalRing(level, to.subtract(look.scale(0.8)), look, 1.1, MagicUtil.gold(), 40);
                MagicUtil.shockwave(level, dest.add(0, 0.2, 0), MagicUtil.gold(), 0.18, 28);
                sound(level, dest, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.5f);
                sound(level, dest, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                return true;
            }
        }
        return MagicUtil.fail(p, "no_target");
    }

    static boolean crimsonBands(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 20);
        if (target.isEmpty()) return MagicUtil.fail(p, "no_target");
        LivingEntity t = target.get();
        int dur = 100 + c.magic().getLevel() * 10;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 9));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, 2));
        MagicUtil.stream(level, hand(p), mid(t), MagicUtil.gold(), 0.5);
        Vec3 feet = t.position();
        for (int i = 0; i < 4; i++) {
            MagicUtil.swirlRing(level, feet.add(0, 0.2 + i * (t.getBbHeight() / 3.2), 0), 0.75, 18, MagicUtil.gold(), 0.18);
        }
        MagicUtil.magicCircle(level, feet.add(0, 0.05, 0), 1.6, MagicUtil.gold(), MagicUtil.goldRune(), 0.3);
        sound(level, t.position(), SoundEvents.CHAIN_PLACE, 1.0f, 0.7f);
        sound(level, t.position(), SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0f, 0.6f);
        return true;
    }

    static boolean mirrorDimension(CastContext c) {
        return MirrorDimension.openRift(c.player());
    }

    // =====================================================================
    //  CHAOS MAGIC (Scarlet Witch): scarlet energy, tendrils and shockwaves
    // =====================================================================

    static boolean scarletPulse(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Vec3 center = chest(p);
        for (LivingEntity e : MagicUtil.nearby(p, center, 6)) {
            MagicUtil.magicHurt(p, e, 4.0f * c.power());
            Vec3 dir = e.position().subtract(p.position()).multiply(1, 0, 1);
            dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 1.6, 0.5, dir.z * 1.6));
            e.hurtMarked = true;
        }
        Vec3 ground = p.position().add(0, 0.15, 0);
        MagicUtil.shockwave(level, ground, MagicUtil.scarlet(), 0.16, 40);
        MagicUtil.shockwave(level, ground.add(0, 0.5, 0), MagicUtil.scarletWisp(), 0.24, 36);
        MagicUtil.shockwave(level, ground.add(0, 1.0, 0), MagicUtil.scarlet(), 0.30, 40);
        MagicUtil.explode(level, center, MagicUtil.scarletWisp(), 30, 0.35);
        MagicUtil.magicCircle(level, ground, 2.2, MagicUtil.scarlet(), MagicUtil.scarletWisp(), 0);
        sound(level, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 1.7f);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.2f, 0.5f);
        return true;
    }

    static boolean chaosBolt(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 30);
        Vec3 from = hand(p);
        Vec3 to = target.map(SpellEffects::mid).orElse(MagicUtil.beamEnd(p, 30));
        MagicUtil.lightning(level, from, to, MagicUtil.scarlet(), 0.7);
        MagicUtil.stream(level, from, to, MagicUtil.scarletWisp(), 0.6);
        target.ifPresent(t -> {
            MagicUtil.magicHurt(p, t, 9.0f * c.power());
            t.setSecondsOnFire(3);
        });
        MagicUtil.explode(level, to, MagicUtil.scarlet(), 22, 0.35);
        MagicUtil.shockwave(level, to, MagicUtil.scarletWisp(), 0.2, 20);
        sound(level, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35f, 2.0f);
        sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 0.7f, 0.6f);
        return true;
    }

    static boolean telekinesis(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 20);
        if (target.isEmpty()) return MagicUtil.fail(p, "no_target");
        LivingEntity t = target.get();
        Vec3 push = p.getLookAngle().scale(2.4 * Math.min(2.0, c.power())).add(0, 0.35, 0);
        t.setDeltaMovement(push);
        t.hurtMarked = true;
        MagicUtil.magicHurt(p, t, 3.0f * c.power());
        // two intertwined tendrils from the caster's hands
        MagicUtil.stream(level, hand(p), mid(t), MagicUtil.scarletWisp(), 0.55);
        MagicUtil.lightning(level, hand(p), mid(t), MagicUtil.scarlet(), 0.9);
        for (int i = 0; i < 3; i++) {
            MagicUtil.swirlRing(level, t.position().add(0, 0.3 + i * 0.6, 0), 0.7, 14, MagicUtil.scarlet(), 0.2);
        }
        sound(level, t.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.6f);
        sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
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
                MagicUtil.explode(level, Vec3.atCenterOf(pos), MagicUtil.scarlet(), 4, 0.15);
                changed++;
            }
        }
        if (changed == 0) return MagicUtil.fail(p, "no_target");
        Vec3 cv = Vec3.atCenterOf(center);
        MagicUtil.stream(level, hand(p), cv, MagicUtil.scarletWisp(), 0.5);
        MagicUtil.magicCircle(level, cv.add(0, 0.2, 0), 3.2, MagicUtil.scarlet(), MagicUtil.scarletWisp(), 0.4);
        MagicUtil.helix(level, cv.subtract(0, 1.5, 0), 4, 2.2, MagicUtil.scarletWisp(), 2.5);
        MagicUtil.shockwave(level, cv, MagicUtil.scarlet(), 0.14, 32);
        sound(level, cv, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 0.5f);
        sound(level, cv, SoundEvents.ENDERMAN_STARE, 0.6f, 0.5f);
        return true;
    }

    // =====================================================================
    //  WITCHCRAFT (Agatha Harkness): purple smoke, sigils and curses
    // =====================================================================

    static boolean lifeDrain(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 16);
        if (target.isEmpty()) return MagicUtil.fail(p, "no_target");
        LivingEntity t = target.get();
        float dmg = 5.0f * c.power();
        MagicUtil.magicHurt(p, t, dmg);
        p.heal(dmg * 0.5f);
        c.magic().addMana(10);
        MagicUtil.stream(level, mid(t), p.getEyePosition().subtract(0, 0.4, 0), MagicUtil.purple(), 0.45);
        MagicUtil.lightning(level, mid(t), p.getEyePosition().subtract(0, 0.4, 0), MagicUtil.purpleRune(), 0.5);
        MagicUtil.explode(level, mid(t), MagicUtil.purple(), 16, 0.2);
        MagicUtil.helix(level, p.position(), 2.0, 0.6, MagicUtil.purple(), 1.5);
        sound(level, t.position(), SoundEvents.WITCH_DRINK, 0.9f, 0.6f);
        sound(level, t.position(), SoundEvents.SOUL_ESCAPE, 0.8f, 1.0f);
        return true;
    }

    static boolean hexCurse(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Vec3 center = MagicUtil.beamEnd(p, 20);
        List<LivingEntity> targets = MagicUtil.nearby(p, center, 5);
        if (targets.isEmpty()) return MagicUtil.fail(p, "no_target");
        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            MagicUtil.helix(level, e.position(), e.getBbHeight() + 0.5, 0.5, MagicUtil.purple(), 2);
        }
        Vec3 ground = center.add(0, 0.1, 0);
        MagicUtil.magicCircle(level, ground, 5, MagicUtil.purple(), MagicUtil.purpleRune(), 0.0);
        MagicUtil.magicCircle(level, ground.add(0, 0.05, 0), 2.6, MagicUtil.purple(), MagicUtil.purpleRune(), 0.5);
        MagicUtil.stream(level, hand(p), ground, MagicUtil.purple(), 0.5);
        sound(level, center, SoundEvents.WITCH_CELEBRATE, 1.0f, 0.7f);
        sound(level, center, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0f, 0.8f);
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
        int amp = (int) Math.max(0, Math.min(witches - 1, 2));
        for (ServerPlayer o : inCircle) {
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, amp));
            PlayerMagic om = MagicCapability.of(o);
            if (om.getSchool() != MagicSchool.NONE) {
                om.addMana(15 + (int) witches * 5);
                if (o != p) MagicSync.sync(o);
            }
            MagicUtil.helix(level, o.position(), 2.2, 0.7, MagicUtil.purple(), 2);
        }
        Vec3 g = p.position().add(0, 0.1, 0);
        MagicUtil.magicCircle(level, g, 10, MagicUtil.purple(), MagicUtil.purpleRune(), 0.0);
        MagicUtil.magicCircle(level, g.add(0, 0.05, 0), 5.5, MagicUtil.purple(), MagicUtil.purpleRune(), Math.PI / 6);
        for (int i = 0; i < 5; i++) {
            double a = i * 2 * Math.PI / 5;
            MagicUtil.helix(level, g.add(Math.cos(a) * 9, 0, Math.sin(a) * 9), 3.5, 0.5, MagicUtil.purple(), 3);
        }
        MagicUtil.shockwave(level, g, MagicUtil.purple(), 0.3, 48);
        sound(level, p.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.2f, 0.8f);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.6f);
        return true;
    }

    static boolean familiar(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        int life = 1200 + c.magic().getLevel() * 60;
        if (!FamiliarManager.summon(p, level, life)) return MagicUtil.fail(p, "familiar_limit");
        Vec3 g = p.position().add(0, 0.1, 0);
        MagicUtil.magicCircle(level, g, 2.4, MagicUtil.purple(), MagicUtil.purpleRune(), 0.0);
        MagicUtil.helix(level, g, 2.6, 1.2, MagicUtil.purple(), 2.5);
        MagicUtil.explode(level, p.position().add(0, 0.8, 0), MagicUtil.purple(), 28, 0.3);
        sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
        sound(level, p.position(), SoundEvents.BAT_TAKEOFF, 1.2f, 0.8f);
        sound(level, p.position(), SoundEvents.BAT_AMBIENT, 1.0f, 0.6f);
        return true;
    }

    // =====================================================================
    //  WICCAN (Billy Kaplan): bright golden stars
    // =====================================================================

    static boolean hexBolt(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Optional<LivingEntity> target = MagicUtil.targetEntity(p, 25);
        Vec3 from = hand(p);
        Vec3 to = target.map(SpellEffects::mid).orElse(MagicUtil.beamEnd(p, 25));
        MagicUtil.lightning(level, from, to, MagicUtil.yellow(), 0.25);
        MagicUtil.stream(level, from, to, MagicUtil.gold(), 0.7);
        target.ifPresent(t -> {
            MagicUtil.magicHurt(p, t, 6.0f * c.power());
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        });
        MagicUtil.explode(level, to, MagicUtil.yellow(), 18, 0.3);
        MagicUtil.shockwave(level, to, MagicUtil.yellow(), 0.18, 20);
        sound(level, p.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
        return true;
    }

    static boolean wiccanDash(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        Vec3 look = p.getLookAngle();
        Vec3 start = chest(p);
        p.setDeltaMovement(look.scale(1.8 * Math.min(1.6, c.power())).add(0, 0.25, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        MagicUtil.portalRing(level, start.subtract(look.scale(0.6)), look, 1.0, MagicUtil.yellow(), 28);
        MagicUtil.stream(level, start, start.add(look.scale(-6)), MagicUtil.yellow(), -0.1);
        MagicUtil.explode(level, start, MagicUtil.gold(), 20, 0.3);
        sound(level, p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.3f);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
        return true;
    }

    /** Reveals every living creature nearby (a stand-in for seeing through others' eyes). */
    static boolean telepathy(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(32), e -> e != p && e.isAlive() && !e.isSpectator());
        if (found.isEmpty()) return MagicUtil.fail(p, "no_target");
        for (LivingEntity e : found) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            MagicUtil.emit(level, MagicUtil.yellow(), e.position().add(0, e.getBbHeight() + 0.5, 0), new Vec3(0, 0.05, 0));
        }
        Vec3 g = p.position().add(0, 1.0, 0);
        MagicUtil.shockwave(level, g, MagicUtil.yellow(), 0.55, 56);
        MagicUtil.shockwave(level, g, MagicUtil.gold(), 0.35, 40);
        MagicUtil.helix(level, p.position(), 2.4, 0.8, MagicUtil.yellow(), 2);
        sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
        sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.6f, 1.8f);
        return true;
    }

    /** Billy and Wanda: link with a nearby Chaos mage to share power. */
    static boolean soulLink(CastContext c) {
        ServerPlayer p = c.player();
        ServerLevel level = c.level();
        List<ServerPlayer> links = level.players().stream()
                .filter(o -> o != p && o.isAlive() && o.distanceToSqr(p) <= 900
                        && MagicCapability.of(o).getSchool() == MagicSchool.CHAOS).toList();
        if (links.isEmpty()) return MagicUtil.fail(p, "no_link");
        buffLinked(p);
        MagicUtil.helix(level, p.position(), 2.4, 0.7, MagicUtil.yellow(), 2);
        for (ServerPlayer o : links) {
            buffLinked(o);
            MagicSync.sync(o);
            MagicUtil.lightning(level, p.getEyePosition(), o.getEyePosition(), MagicUtil.scarlet(), 0.8);
            MagicUtil.lightning(level, p.getEyePosition(), o.getEyePosition(), MagicUtil.yellow(), 0.8);
            MagicUtil.stream(level, p.getEyePosition(), o.getEyePosition(), MagicUtil.yellow(), 0.5);
            MagicUtil.helix(level, o.position(), 2.4, 0.7, MagicUtil.scarlet(), 2);
        }
        sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.6f);
        return true;
    }

    private static void buffLinked(ServerPlayer o) {
        MagicCapability.of(o).addMana(40);
        o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
        o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
    }
}
