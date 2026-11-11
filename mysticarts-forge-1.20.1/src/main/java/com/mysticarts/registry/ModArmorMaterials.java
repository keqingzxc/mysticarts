package com.mysticarts.registry;

import com.mysticarts.MysticArts;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum ModArmorMaterials implements ArmorMaterial {
    CLOAK;

    @Override public int getDurabilityForType(ArmorItem.Type type) { return 330; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return type == ArmorItem.Type.CHESTPLATE ? 4 : 0; }
    @Override public int getEnchantmentValue() { return 25; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_ELYTRA; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.PHANTOM_MEMBRANE); }
    @Override public String getName() { return MysticArts.MODID + ":cloak"; }
    @Override public float getToughness() { return 1.0f; }
    @Override public float getKnockbackResistance() { return 0.0f; }
}
