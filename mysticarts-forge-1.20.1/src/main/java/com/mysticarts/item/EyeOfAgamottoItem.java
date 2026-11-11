package com.mysticarts.item;

import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.TimeHistory;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.util.MagicUtil;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Time Stone: rolls the user back to where (and how healthy) they were ~6 seconds ago. */
public class EyeOfAgamottoItem extends Item {
    private static final int MANA_COST = 40;

    public EyeOfAgamottoItem(Properties properties) {
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
        TimeHistory.Snapshot s = TimeHistory.pick(sp);
        ServerLevel dest = s == null ? null : sp.server.getLevel(s.dim());
        if (s == null || dest == null) {
            MagicUtil.msg(sp, "no_history");
            return InteractionResultHolder.fail(stack);
        }

        m.consumeMana(MANA_COST);
        ServerLevel from = sp.serverLevel();
        from.sendParticles(ParticleTypes.HAPPY_VILLAGER, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        sp.teleportTo(dest, s.x(), s.y(), s.z(), s.yaw(), s.pitch());
        sp.setHealth(Math.max(sp.getHealth(), Math.min(s.health(), sp.getMaxHealth())));
        sp.getFoodData().setFoodLevel(Math.max(sp.getFoodData().getFoodLevel(), s.food()));
        sp.clearFire();
        sp.fallDistance = 0;
        sp.setDeltaMovement(Vec3.ZERO);
        sp.hurtMarked = true;
        dest.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.x(), s.y() + 1, s.z(), 40, 0.5, 0.8, 0.5, 0.1);
        dest.playSound(null, s.x(), s.y(), s.z(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);

        TimeHistory.clear(sp.getUUID());
        sp.getCooldowns().addCooldown(this, 600);
        MagicUtil.msg(sp, "rewind");
        MagicSync.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.eye").withStyle(s -> s.withColor(0x55FF55)));
    }
}
