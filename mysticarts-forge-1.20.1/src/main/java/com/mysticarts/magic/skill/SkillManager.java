package com.mysticarts.magic.skill;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.magic.skill.Skills.Skill;
import com.mysticarts.util.MagicUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Server-side validation of skill purchases and resets. */
public final class SkillManager {
    private SkillManager() {}

    public static final String RESET = "reset";
    public static final int RESET_COST_LEVELS = 5;

    public static void handle(ServerPlayer p, String id) {
        PlayerMagic m = MagicCapability.of(p);
        if (m.getSchool() == MagicSchool.NONE) return;

        if (RESET.equals(id)) {
            if (m.getSkills().isEmpty()) return;
            if (!p.isCreative() && p.experienceLevel < RESET_COST_LEVELS) {
                MagicUtil.msg(p, "skill_reset_cost", RESET_COST_LEVELS);
                return;
            }
            if (!p.isCreative()) p.giveExperienceLevels(-RESET_COST_LEVELS);
            m.clearSkills();
            m.setMana(m.getMana()); // re-clamp to the new maximum
            MagicSync.sync(p);
            MagicUtil.msg(p, "skill_reset");
            return;
        }

        Skill s = Skills.get(id);
        if (s == null) return;
        if (s.school() != MagicSchool.NONE && s.school() != m.getSchool()) return;
        int rank = m.getRank(id);
        if (rank >= s.maxRank()) return;
        if (m.getLevel() < s.minLevel()) {
            MagicUtil.msg(p, "low_level", s.minLevel());
            return;
        }
        if (s.requires() != null && m.getRank(s.requires()) < 1) {
            MagicUtil.msg(p, "skill_locked");
            return;
        }
        if (m.getSkillPoints() < s.cost()) {
            MagicUtil.msg(p, "skill_no_points");
            return;
        }

        m.setRank(id, rank + 1);
        MagicSync.sync(p);
        MagicUtil.msg(p, "skill_bought", s.name(), rank + 1);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0f, 1.0f + 0.15f * rank);
        MagicUtil.explode(p.serverLevel(), p.position().add(0, 1.2, 0), MagicUtil.forSchool(m.getSchool()), 8, 0.2);
    }
}
