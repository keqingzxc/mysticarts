package com.mysticarts.item;

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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A support relic: heals the user and nearby players and shares a little mana. */
public class HeartstoneItem extends Item {
    private static final int MANA_COST = 30;

    public HeartstoneItem(Properties properties) {
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
        m.consumeMana(MANA_COST);
        ServerLevel sl = sp.serverLevel();
        for (ServerPlayer o : sl.players()) {
            if (o.distanceToSqr(sp) > 64 || !o.isAlive()) continue;
            o.heal(8.0f);
            o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0));
            if (o != sp) {
                MagicCapability.of(o).addMana(10);
                MagicSync.sync(o);
            }
            MagicUtil.helix(sl, o.position(), 2.2, 0.7, MagicUtil.yellow(), 2);
            MagicUtil.explode(sl, o.position().add(0, 1.0, 0), MagicUtil.yellow(), 14, 0.15);
        }
        Vec3 g = sp.position().add(0, 0.2, 0);
        MagicUtil.shockwave(sl, g, MagicUtil.yellow(), 0.3, 48);
        MagicUtil.magicCircle(sl, g, 3.0, MagicUtil.gold(), MagicUtil.yellow(), 0.0);
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.5f);
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        sp.getCooldowns().addCooldown(this, 600);
        MagicSync.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.heartstone").withStyle(s -> s.withColor(0xFFE04A)));
    }
}
