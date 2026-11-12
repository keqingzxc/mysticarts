package com.mysticarts.item;

import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.util.MagicUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Drinkable brew: mana, regeneration and a little less corruption. Returns an empty bottle. */
public class WitchsBrewItem extends Item {
    public WitchsBrewItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            PlayerMagic m = MagicCapability.of(sp);
            m.addMana(60);
            m.addCorruption(-10);
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            MagicSync.sync(sp);
            ServerLevel sl = sp.serverLevel();
            MagicUtil.helix(sl, sp.position(), 2.0, 0.6, MagicUtil.purple(), 2);
            MagicUtil.explode(sl, sp.position().add(0, 1.2, 0), MagicUtil.purple(), 20, 0.2);
        }
        if (!(entity instanceof Player p && p.getAbilities().instabuild)) {
            stack.shrink(1);
            if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
            if (entity instanceof Player pl) pl.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.brew").withStyle(s -> s.withColor(0xB04CE8)));
    }
}
