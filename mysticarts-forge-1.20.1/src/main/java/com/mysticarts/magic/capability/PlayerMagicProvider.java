package com.mysticarts.magic.capability;

import com.mysticarts.magic.PlayerMagic;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerMagicProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerMagic magic = new PlayerMagic();
    private final LazyOptional<PlayerMagic> optional = LazyOptional.of(() -> magic);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MagicCapability.PLAYER_MAGIC ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return magic.save(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { magic.load(nbt); }
}
