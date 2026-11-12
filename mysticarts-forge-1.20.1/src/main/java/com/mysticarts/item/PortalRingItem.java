package com.mysticarts.item;

import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.util.MagicUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

/** Sling Ring: sneak + use binds a location (any dimension); use teleports back to it. */
public class PortalRingItem extends Item {
    private static final int MANA_COST = 30;
    private static final String TAG = "Bound";

    public PortalRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = (ServerLevel) level;

        if (sp.isShiftKeyDown()) {
            CompoundTag bound = stack.getOrCreateTagElement(TAG);
            bound.putString("Dim", sl.dimension().location().toString());
            bound.putDouble("X", sp.getX());
            bound.putDouble("Y", sp.getY());
            bound.putDouble("Z", sp.getZ());
            bound.putFloat("Yaw", sp.getYRot());
            bound.putFloat("Pitch", sp.getXRot());
            MagicUtil.msg(sp, "ring_bound");
            MagicUtil.portalRing(sl, sp.position().add(0, 1.1, 0).add(sp.getLookAngle().scale(0.8)), sp.getLookAngle(), 0.9, MagicUtil.gold(), 30);
            MagicUtil.explode(sl, sp.position().add(0, 1, 0), MagicUtil.gold(), 20, 0.25);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            return InteractionResultHolder.consume(stack);
        }

        CompoundTag bound = stack.getTagElement(TAG);
        if (bound == null) {
            MagicUtil.msg(sp, "no_binding");
            return InteractionResultHolder.fail(stack);
        }
        PlayerMagic m = MagicCapability.of(sp);
        if (m.getMana() < MANA_COST) {
            MagicUtil.msg(sp, "not_enough_mana", MANA_COST);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel dest = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(bound.getString("Dim"))));
        if (dest == null) {
            MagicUtil.msg(sp, "no_binding");
            return InteractionResultHolder.fail(stack);
        }

        m.consumeMana(MANA_COST);
        Vec3 look = sp.getLookAngle();
        MagicUtil.portalRing(sl, sp.position().add(0, 1.1, 0).add(look.scale(0.8)), look, 1.2, MagicUtil.gold(), 44);
        MagicUtil.portalRing(sl, sp.position().add(0, 1.1, 0).add(look.scale(0.8)), look, 0.7, MagicUtil.goldRune(), 8);
        MagicUtil.explode(sl, sp.position().add(0, 1, 0), MagicUtil.gold(), 30, 0.3);
        sp.teleportTo(dest, bound.getDouble("X"), bound.getDouble("Y"), bound.getDouble("Z"),
                bound.getFloat("Yaw"), bound.getFloat("Pitch"));
        Vec3 arrive = new Vec3(bound.getDouble("X"), bound.getDouble("Y") + 1, bound.getDouble("Z"));
        MagicUtil.portalRing(dest, arrive.subtract(look.scale(0.8)), look, 1.2, MagicUtil.gold(), 44);
        MagicUtil.shockwave(dest, arrive.subtract(0, 0.8, 0), MagicUtil.gold(), 0.2, 30);
        MagicUtil.explode(dest, arrive, MagicUtil.gold(), 30, 0.3);
        dest.playSound(null, bound.getDouble("X"), bound.getDouble("Y"), bound.getDouble("Z"),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        sp.getCooldowns().addCooldown(this, 100);
        MagicSync.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return stack.getTagElement(TAG) != null; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.ring").withStyle(s -> s.withColor(0xAAAAAA)));
        CompoundTag bound = stack.getTagElement(TAG);
        if (bound != null) {
            tooltip.add(Component.translatable("tooltip.mysticarts.ring_bound", bound.getString("Dim"),
                    (int) bound.getDouble("X"), (int) bound.getDouble("Y"), (int) bound.getDouble("Z"))
                    .withStyle(s -> s.withColor(0xFFAA00)));
        }
    }
}
