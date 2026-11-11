package com.mysticarts.registry;

import com.mysticarts.MysticArts;
import com.mysticarts.item.*;
import com.mysticarts.magic.MagicSchool;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MysticArts.MODID);

    private static Item.Properties epic() { return new Item.Properties().stacksTo(1).rarity(Rarity.EPIC); }
    private static Item.Properties rare() { return new Item.Properties().stacksTo(1).rarity(Rarity.RARE); }

    public static final RegistryObject<Item> TOME_STRANGE = ITEMS.register("tome_strange",
            () -> new TomeItem(MagicSchool.STRANGE, rare()));
    public static final RegistryObject<Item> TOME_CHAOS = ITEMS.register("tome_chaos",
            () -> new TomeItem(MagicSchool.CHAOS, rare()));
    public static final RegistryObject<Item> TOME_WITCHCRAFT = ITEMS.register("tome_witchcraft",
            () -> new TomeItem(MagicSchool.WITCHCRAFT, rare()));
    public static final RegistryObject<Item> TOME_WICCAN = ITEMS.register("tome_wiccan",
            () -> new TomeItem(MagicSchool.WICCAN, rare()));

    public static final RegistryObject<Item> DARKHOLD = ITEMS.register("darkhold",
            () -> new DarkholdItem(epic()));
    public static final RegistryObject<Item> PORTAL_RING = ITEMS.register("portal_ring",
            () -> new PortalRingItem(rare()));
    public static final RegistryObject<Item> EYE_OF_AGAMOTTO = ITEMS.register("eye_of_agamotto",
            () -> new EyeOfAgamottoItem(epic()));
    public static final RegistryObject<Item> CLOAK = ITEMS.register("cloak_of_levitation",
            () -> new CloakOfLevitationItem(epic()));

    public static final RegistryObject<Item> MYSTIC_ALTAR = ITEMS.register("mystic_altar",
            () -> new BlockItem(ModBlocks.MYSTIC_ALTAR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
}
