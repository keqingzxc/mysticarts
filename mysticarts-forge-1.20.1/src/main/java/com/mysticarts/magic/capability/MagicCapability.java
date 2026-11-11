package com.mysticarts.magic.capability;

import com.mysticarts.magic.PlayerMagic;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public class MagicCapability {
    public static final Capability<PlayerMagic> PLAYER_MAGIC =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static LazyOptional<PlayerMagic> get(Player player) {
        return player.getCapability(PLAYER_MAGIC);
    }

    /** Capability is always attached to players; throws if something is badly wrong. */
    public static PlayerMagic of(Player player) {
        return player.getCapability(PLAYER_MAGIC)
                .orElseThrow(() -> new IllegalStateException("Player is missing the Mystic Arts capability"));
    }
}
