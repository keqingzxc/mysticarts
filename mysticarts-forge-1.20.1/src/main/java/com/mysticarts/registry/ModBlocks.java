package com.mysticarts.registry;

import com.mysticarts.MysticArts;
import com.mysticarts.block.MysticAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MysticArts.MODID);

    public static final RegistryObject<Block> MYSTIC_ALTAR = BLOCKS.register("mystic_altar",
            () -> new MysticAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)));
}
