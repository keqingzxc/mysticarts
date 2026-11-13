package com.mysticarts.item;

import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.MirrorDimension;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.util.MagicUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Opens the way into (and out of) the Mirror Dimension without needing the spell. Six uses. */
public class MirrorShardItem extends Item {
    private static final int MANA_COST = 40;

    public MirrorShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        PlayerMagic m = MagicCapability.of(sp);
        if (m.getMana() < MANA_COST) {
            MagicUtil.msg(sp, "not_enough_mana", MANA_COST);
            return InteractionResultHolder.fail(stack);
        }
        if (!MirrorDimension.openRift(sp)) return InteractionResultHolder.fail(stack);
        m.consumeMana(MANA_COST);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        sp.getCooldowns().addCooldown(this, 200);
        MagicSync.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.mirror_shard").withStyle(s -> s.withColor(0x9FDFFF)));
    }
}
