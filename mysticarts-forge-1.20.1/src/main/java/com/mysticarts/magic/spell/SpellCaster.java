package com.mysticarts.magic.spell;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.magic.spell.Spell.CastContext;
import com.mysticarts.util.MagicUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Server-side spell pipeline: validation, mana, cooldowns, chaos fizzle, XP and corruption. */
public final class SpellCaster {
    private SpellCaster() {}

    public static Spell selected(PlayerMagic m) {
        List<Spell> list = Spells.forSchool(m.getSchool());
        if (list.isEmpty()) return null;
        return list.get(Math.floorMod(m.getSelectedSpell(), list.size()));
    }

    public static void cycle(ServerPlayer p, int dir) {
        PlayerMagic m = MagicCapability.of(p);
        if (m.getSchool() == MagicSchool.NONE) return;
        List<Spell> list = Spells.forSchool(m.getSchool());
        m.setSelectedSpell(Math.floorMod(m.getSelectedSpell() + dir, list.size()));
        MagicUtil.msg(p, "spell_selected", list.get(m.getSelectedSpell()).displayName());
        MagicSync.sync(p);
    }

    public static float power(ServerPlayer p, PlayerMagic m) {
        float power = 1.0f + m.getLevel() * 0.05f;
        if (m.getSchool() == MagicSchool.CHAOS) {
            power *= 1.0f + m.getCorruption() / 200.0f; // corruption feeds chaos power...
        }
        if (m.getSchool() == MagicSchool.WICCAN) {
            boolean chaosNearby = !p.level().getEntitiesOfClass(ServerPlayer.class,
                    p.getBoundingBox().inflate(16),
                    o -> o != p && MagicCapability.of(o).getSchool() == MagicSchool.CHAOS).isEmpty();
            if (chaosNearby) power *= 1.25f; // ...and Billy is stronger near Wanda
        }
        return power;
    }

    public static void cast(ServerPlayer p) {
        PlayerMagic m = MagicCapability.of(p);
        if (m.getSchool() == MagicSchool.NONE) {
            MagicUtil.msg(p, "no_school");
            return;
        }
        Spell spell = selected(m);
        if (spell == null) return;

        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();

        if (m.getLevel() < spell.minLevel()) {
            MagicUtil.msg(p, "low_level", spell.minLevel());
            return;
        }
        if (m.isOnCooldown(spell.id(), now)) {
            MagicUtil.msg(p, "cooldown", (m.cooldownLeft(spell.id(), now) + 19) / 20);
            return;
        }
        if (m.getMana() < spell.manaCost()) {
            MagicUtil.msg(p, "not_enough_mana", spell.manaCost());
            return;
        }

        // Chaos magic is unstable: the more corruption, the higher the backfire chance.
        if (m.getSchool() == MagicSchool.CHAOS && p.getRandom().nextFloat() < m.getCorruption() / 250.0f) {
            m.consumeMana(spell.manaCost() / 2);
            m.setCooldown(spell.id(), now + 20);
            p.hurt(p.damageSources().magic(), 2.0f);
            MagicUtil.burst(level, p.position().add(0, 1, 0), MagicUtil.SCARLET, 25);
            MagicUtil.msg(p, "fizzle");
            MagicSync.sync(p);
            return;
        }

        CastContext ctx = new CastContext(p, level, m, power(p, m));
        if (!spell.effect().cast(ctx)) return;

        m.consumeMana(spell.manaCost());
        m.setCooldown(spell.id(), now + spell.cooldown());
        m.addCorruption(spell.corruption());
        boolean levelUp = m.addXp(spell.manaCost() / 5 + 1);
        if (levelUp) {
            p.sendSystemMessage(Component.translatable("message.mysticarts.level_up", m.getLevel())
                    .withStyle(s -> s.withColor(m.getSchool().getColor())));
        }
        MagicSync.sync(p);
    }
}
