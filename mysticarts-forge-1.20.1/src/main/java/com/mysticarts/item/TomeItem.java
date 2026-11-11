package com.mysticarts.item;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.util.MagicUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Using a tome teaches the player one of the four schools of magic. */
public class TomeItem extends Item {
    private final MagicSchool school;

    public TomeItem(MagicSchool school, Properties properties) {
        super(properties);
        this.school = school;
    }

    public MagicSchool getSchool() { return school; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        PlayerMagic m = MagicCapability.of(sp);

        if (m.getSchool() == school) {
            MagicUtil.msg(sp, "school_already");
            return InteractionResultHolder.fail(stack);
        }
        if (m.getSchool() != MagicSchool.NONE) {
            if (!sp.isShiftKeyDown()) {
                MagicUtil.msg(sp, "school_switch_hint");
                return InteractionResultHolder.fail(stack);
            }
            if (!sp.isCreative() && sp.experienceLevel < 10) {
                MagicUtil.msg(sp, "school_switch_cost");
                return InteractionResultHolder.fail(stack);
            }
            if (!sp.isCreative()) sp.giveExperienceLevels(-10);
            m.resetProgress();
        }

        m.setSchool(school);
        m.setMana(m.getMaxMana());
        if (!sp.isCreative()) stack.shrink(1);

        MagicSync.sync(sp);
        sp.sendSystemMessage(Component.translatable("message.mysticarts.school_learned", school.getDisplayName()));
        ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                sp.getX(), sp.getY() + 1.2, sp.getZ(), 60, 0.6, 0.8, 0.6, 0.6);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0f, 0.8f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(school.getDisplayName());
        tooltip.add(Component.translatable("tooltip.mysticarts.tome").withStyle(s -> s.withColor(0xAAAAAA)));
        tooltip.add(Component.translatable("tooltip.mysticarts.tome_switch").withStyle(s -> s.withColor(0x777777)));
    }
}
