package com.mysticarts.item;

import com.mysticarts.registry.ModArmorMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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

    /** The vanilla armor layer is made fully transparent; the real cloak is rendered on the back by CloakLayer. */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "mysticarts:textures/models/armor/empty_layer_1.png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.cloak").withStyle(s -> s.withColor(0xFF5555)));
    }
}
