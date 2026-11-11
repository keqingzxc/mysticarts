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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Darkhold: huge power at a price. Mages gain mana, strength and corruption;
 * Chaos mages also learn faster. Everyone else is punished.
 */
public class DarkholdItem extends Item {
    public DarkholdItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        PlayerMagic m = MagicCapability.of(sp);
        ServerLevel sl = (ServerLevel) level;

        if (m.getSchool() == MagicSchool.NONE) {
            sp.hurt(sp.damageSources().magic(), 4.0f);
            sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            MagicUtil.msg(sp, "darkhold_rejects");
        } else {
            m.addCorruption(12);
            m.addMana(40);
            boolean up = m.addXp(m.getSchool() == MagicSchool.CHAOS ? 40 : 20);
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            MagicUtil.msg(sp, "darkhold_read");
            if (up) {
                sp.sendSystemMessage(Component.translatable("message.mysticarts.level_up", m.getLevel()));
            }
            MagicSync.sync(sp);
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX(), sp.getY() + 1.0, sp.getZ(), 40, 0.5, 0.7, 0.5, 0.05);
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WITHER_AMBIENT,
                SoundSource.PLAYERS, 0.8f, 0.6f);
        sp.getCooldowns().addCooldown(this, 600);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.darkhold").withStyle(s -> s.withColor(0xB04CE8)));
        tooltip.add(Component.translatable("tooltip.mysticarts.darkhold2").withStyle(s -> s.withColor(0x777777)));
    }
}
