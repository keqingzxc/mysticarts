package com.mysticarts.block;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.util.MagicUtil;
import net.minecraft.core.BlockPos;
import com.mysticarts.registry.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A source of power: restores a mage's mana and eases corruption (once every 2 minutes per player). */
public class MysticAltarBlock extends Block {
    private static final long COOLDOWN_TICKS = 2400;

    public MysticAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerMagic m = MagicCapability.of(sp);
            long now = level.getGameTime();
            if (m.getSchool() == MagicSchool.NONE) {
                MagicUtil.msg(sp, "altar_silent");
            } else if (now < m.getAltarReadyAt()) {
                long minutes = (m.getAltarReadyAt() - now + 1199) / 1200;
                MagicUtil.msg(sp, "altar_wait", minutes);
            } else {
                m.setMana(m.getMaxMana());
                m.addCorruption(-15);
                m.setAltarReadyAt(now + COOLDOWN_TICKS);
                MagicSync.sync(sp);
                MagicUtil.msg(sp, "altar_used");
                Vec3 c = Vec3.atCenterOf(pos).add(0, 0.8, 0);
                ServerLevel sl = (ServerLevel) level;
                MagicUtil.helix(sl, Vec3.atBottomCenterOf(pos).add(0, 1, 0), 2.5, 0.8, MagicUtil.gold(), 3);
                MagicUtil.explode(sl, c, MagicUtil.purple(), 30, 0.35);
                MagicUtil.shockwave(sl, c.subtract(0, 0.6, 0), MagicUtil.gold(), 0.25, 36);
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 1.2f);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.GOLD_SPARK.get(),
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.6, 0.4, (random.nextDouble() - 0.5) * 0.6);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.altar").withStyle(s -> s.withColor(0xAAAAAA)));
    }
}
