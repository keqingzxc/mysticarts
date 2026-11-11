package com.mysticarts.magic.spell;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.PlayerMagic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record Spell(String id, MagicSchool school, int manaCost, int cooldown, int minLevel,
                    int corruption, Effect effect) {

    public record CastContext(ServerPlayer player, ServerLevel level, PlayerMagic magic, float power) {}

    @FunctionalInterface
    public interface Effect {
        /** @return true if the spell was actually cast (mana and cooldown are only spent then). */
        boolean cast(CastContext ctx);
    }

    public Component displayName() {
        return Component.translatable("spell.mysticarts." + id);
    }
}
