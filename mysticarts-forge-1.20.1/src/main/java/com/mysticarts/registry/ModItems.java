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

    // ---- casting foci (one per school) ----
    public static final RegistryObject<Item> STAFF_OF_VISHANTI = ITEMS.register("staff_of_vishanti",
            () -> new FocusItem(MagicSchool.STRANGE, epic()));
    public static final RegistryObject<Item> SCARLET_SCEPTER = ITEMS.register("scarlet_scepter",
            () -> new FocusItem(MagicSchool.CHAOS, epic()));
    public static final RegistryObject<Item> WITCHS_ATHAME = ITEMS.register("witchs_athame",
            () -> new FocusItem(MagicSchool.WITCHCRAFT, epic()));
    public static final RegistryObject<Item> WICCAN_SIGIL = ITEMS.register("wiccan_sigil",
            () -> new FocusItem(MagicSchool.WICCAN, epic()));

    // ---- school artifacts ----
    public static final RegistryObject<Item> MIRROR_SHARD = ITEMS.register("mirror_shard",
            () -> new MirrorShardItem(new Item.Properties().durability(6).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CHAOS_ORB = ITEMS.register("chaos_orb",
            () -> new ChaosOrbItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> WITCHS_BREW = ITEMS.register("witchs_brew",
            () -> new WitchsBrewItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> HEARTSTONE = ITEMS.register("heartstone",
            () -> new HeartstoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> MYSTIC_ALTAR = ITEMS.register("mystic_altar",
            () -> new BlockItem(ModBlocks.MYSTIC_ALTAR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
}
