package com.mysticarts.item;

import com.mysticarts.registry.ModArmorMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Chestplate that grants flight while the wearer has mana. The actual logic lives in
 * {@link com.mysticarts.event.ServerEvents} (flight, mana drain, no fall damage).
 */
public class CloakOfLevitationItem extends ArmorItem {
    public CloakOfLevitationItem(Properties properties) {
        super(ModArmorMaterials.CLOAK, Type.CHESTPLATE, properties.rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.cloak").withStyle(s -> s.withColor(0xFF5555)));
    }
}
