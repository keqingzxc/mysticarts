package com.mysticarts.magic.skill;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.spell.Spell;
import com.mysticarts.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The skill tree: every node is a small permanent upgrade bought with skill points
 * (one point per magic level). Each school has its own eight nodes; three more are shared.
 */
public final class Skills {
    private Skills() {}

    public enum Kind {
        SPELL_POWER, SPELL_COOLDOWN, SPELL_COST,
        SCHOOL_POWER, SCHOOL_COOLDOWN, SCHOOL_COST,
        MAX_MANA, MANA_REGEN, CORRUPTION_RESIST, FIZZLE_RESIST
    }

    /**
     * @param tier     row of the tree (0 = shared row); tier N needs magic level (N-1)*3
     * @param col      column 0..2 in the screen
     * @param requires id of the node that must have at least one rank (or null)
     * @param target   spell id, school id or "" depending on the kind
     */
    public record Skill(String id, MagicSchool school, int tier, int col, int maxRank, int cost, String requires,
                        Kind kind, String target, double perRank, Supplier<Item> icon) {

        public int minLevel() { return tier <= 1 ? 0 : (tier - 1) * 3; }

        public Component name() { return Component.translatable("skill.mysticarts." + id); }

        public Component description() {
            String key = "skill.mysticarts.desc." + kind.name().toLowerCase(Locale.ROOT);
            return switch (kind) {
                case SPELL_POWER, SPELL_COOLDOWN, SPELL_COST ->
                        Component.translatable(key, Component.translatable("spell.mysticarts." + target), pct(perRank));
                case SCHOOL_POWER, SCHOOL_COOLDOWN, SCHOOL_COST ->
                        Component.translatable(key, MagicSchool.fromName(target).getDisplayName(), pct(perRank));
                case MAX_MANA, MANA_REGEN -> Component.translatable(key, (int) perRank);
                default -> Component.translatable(key, pct(perRank));
            };
        }
    }

    private static String pct(double v) { return String.valueOf((int) Math.round(v * 100)); }

    private static final Map<String, Skill> BY_ID = new LinkedHashMap<>();

    private static void add(String id, MagicSchool school, int tier, int col, int maxRank, int cost, String requires,
                            Kind kind, String target, double perRank, Supplier<Item> icon) {
        BY_ID.put(id, new Skill(id, school, tier, col, maxRank, cost, requires, kind, target, perRank, icon));
    }

    static {
        // ---------------- Mystic Arts ----------------
        MagicSchool S = MagicSchool.STRANGE;
        add("mandala_mastery", S, 1, 0, 3, 1, null, Kind.SPELL_POWER, "mandala", 0.20, () -> ModItems.STAFF_OF_VISHANTI.get());
        add("sling_mastery", S, 1, 2, 3, 1, null, Kind.SPELL_COOLDOWN, "portal_step", 0.12, () -> ModItems.PORTAL_RING.get());
        add("bands_grip", S, 2, 0, 3, 1, "mandala_mastery", Kind.SPELL_POWER, "crimson_bands", 0.20, () -> ModItems.TOME_STRANGE.get());
        add("vishanti_reserve", S, 2, 1, 5, 1, null, Kind.MAX_MANA, "", 20, () -> ModItems.EYE_OF_AGAMOTTO.get());
        add("mirror_walker", S, 2, 2, 3, 1, "sling_mastery", Kind.SPELL_COST, "mirror_dimension", 0.10, () -> ModItems.MIRROR_SHARD.get());
        add("agamotto_flow", S, 3, 1, 3, 1, "vishanti_reserve", Kind.MANA_REGEN, "", 1, () -> ModItems.CLOAK.get());
        add("time_dilation", S, 3, 2, 3, 1, "mirror_walker", Kind.SCHOOL_COOLDOWN, "strange", 0.05, () -> Items.CLOCK);
        add("sorcerer_supreme", S, 4, 1, 1, 3, "time_dilation", Kind.SCHOOL_POWER, "strange", 0.25, () -> Items.NETHER_STAR);

        // ---------------- Chaos Magic ----------------
        MagicSchool C = MagicSchool.CHAOS;
        add("scarlet_surge", C, 1, 0, 3, 1, null, Kind.SPELL_POWER, "scarlet_pulse", 0.20, () -> ModItems.SCARLET_SCEPTER.get());
        add("chaos_edge", C, 1, 2, 3, 1, null, Kind.SPELL_POWER, "chaos_bolt", 0.20, () -> ModItems.CHAOS_ORB.get());
        add("telekinetic_might", C, 2, 0, 3, 1, "scarlet_surge", Kind.SPELL_COOLDOWN, "telekinesis", 0.12, () -> ModItems.TOME_CHAOS.get());
        add("chaos_heart", C, 2, 1, 5, 1, null, Kind.MAX_MANA, "", 20, () -> Items.REDSTONE_BLOCK);
        add("reality_shaper", C, 2, 2, 3, 1, "chaos_edge", Kind.SPELL_COST, "reality_warp", 0.10, () -> Items.DIAMOND_ORE);
        add("chaos_control", C, 3, 1, 3, 1, "chaos_heart", Kind.FIZZLE_RESIST, "", 0.15, () -> ModItems.DARKHOLD.get());
        add("wanda_will", C, 3, 2, 3, 1, "reality_shaper", Kind.SCHOOL_COOLDOWN, "chaos", 0.05, () -> Items.CLOCK);
        add("nexus_being", C, 4, 1, 1, 3, "wanda_will", Kind.SCHOOL_POWER, "chaos", 0.25, () -> Items.NETHER_STAR);

        // ---------------- Witchcraft ----------------
        MagicSchool W = MagicSchool.WITCHCRAFT;
        add("drain_mastery", W, 1, 0, 3, 1, null, Kind.SPELL_POWER, "life_drain", 0.20, () -> ModItems.WITCHS_ATHAME.get());
        add("hex_potency", W, 1, 2, 3, 1, null, Kind.SPELL_POWER, "hex_curse", 0.20, () -> ModItems.WITCHS_BREW.get());
        add("coven_bond", W, 2, 0, 3, 1, "drain_mastery", Kind.SPELL_COOLDOWN, "coven_circle", 0.12, () -> ModItems.TOME_WITCHCRAFT.get());
        add("crone_reserve", W, 2, 1, 5, 1, null, Kind.MAX_MANA, "", 20, () -> Items.CAULDRON);
        add("bat_mastery", W, 2, 2, 3, 1, "hex_potency", Kind.SPELL_POWER, "familiar", 0.25, () -> Items.BAT_SPAWN_EGG);
        add("moonlit_flow", W, 3, 1, 3, 1, "crone_reserve", Kind.MANA_REGEN, "", 1, () -> Items.AMETHYST_SHARD);
        add("arcane_efficiency", W, 3, 2, 3, 1, "bat_mastery", Kind.SCHOOL_COST, "witchcraft", 0.05, () -> Items.ENCHANTED_BOOK);
        add("harkness_legacy", W, 4, 1, 1, 3, "arcane_efficiency", Kind.SCHOOL_POWER, "witchcraft", 0.25, () -> Items.NETHER_STAR);

        // ---------------- Wiccan ----------------
        MagicSchool X = MagicSchool.WICCAN;
        add("bolt_mastery", X, 1, 0, 3, 1, null, Kind.SPELL_POWER, "hex_bolt", 0.20, () -> ModItems.WICCAN_SIGIL.get());
        add("dash_mastery", X, 1, 2, 3, 1, null, Kind.SPELL_COOLDOWN, "wiccan_dash", 0.12, () -> Items.FEATHER);
        add("mind_sight", X, 2, 0, 3, 1, "bolt_mastery", Kind.SPELL_COST, "telepathy", 0.10, () -> Items.ENDER_EYE);
        add("star_reserve", X, 2, 1, 5, 1, null, Kind.MAX_MANA, "", 20, () -> Items.GLOWSTONE);
        add("soul_bond", X, 2, 2, 3, 1, "dash_mastery", Kind.SPELL_POWER, "soul_link", 0.20, () -> ModItems.HEARTSTONE.get());
        add("hearts_flow", X, 3, 1, 3, 1, "star_reserve", Kind.MANA_REGEN, "", 1, () -> Items.GOLDEN_APPLE);
        add("reality_flux", X, 3, 2, 3, 1, "soul_bond", Kind.SCHOOL_COOLDOWN, "wiccan", 0.05, () -> Items.CLOCK);
        add("demiurge", X, 4, 1, 1, 3, "reality_flux", Kind.SCHOOL_POWER, "wiccan", 0.25, () -> Items.NETHER_STAR);

        // ---------------- shared by every mage (tier 0) ----------------
        MagicSchool N = MagicSchool.NONE;
        add("inner_calm", N, 0, 0, 3, 1, null, Kind.CORRUPTION_RESIST, "", 0.10, () -> Items.MILK_BUCKET);
        add("deep_well", N, 0, 1, 5, 1, null, Kind.MAX_MANA, "", 10, () -> Items.LAPIS_BLOCK);
        add("quick_recovery", N, 0, 2, 3, 1, null, Kind.MANA_REGEN, "", 1, () -> Items.HONEY_BOTTLE);
    }

    // ------------------------------------------------------------------ lookups
    public static Collection<Skill> all() { return BY_ID.values(); }

    public static Skill get(String id) { return BY_ID.get(id); }

    public static List<Skill> forSchool(MagicSchool school) {
        List<Skill> out = new ArrayList<>();
        for (Skill s : BY_ID.values()) if (s.school() == school) out.add(s);
        return out;
    }

    public static List<Skill> shared() { return forSchool(MagicSchool.NONE); }

    public static int spent(PlayerMagic m) {
        int total = 0;
        for (Map.Entry<String, Integer> e : m.getSkills().entrySet()) {
            Skill s = BY_ID.get(e.getKey());
            if (s != null) total += s.cost() * e.getValue();
        }
        return total;
    }

    // ------------------------------------------------------------------ modifiers
    private static double sum(PlayerMagic m, Kind kind, String target) {
        double total = 0;
        for (Map.Entry<String, Integer> e : m.getSkills().entrySet()) {
            Skill s = BY_ID.get(e.getKey());
            if (s != null && s.kind() == kind && s.target().equals(target)) total += s.perRank() * e.getValue();
        }
        return total;
    }

    public static float spellPower(PlayerMagic m, Spell spell) {
        return (float) (1.0 + sum(m, Kind.SPELL_POWER, spell.id()) + sum(m, Kind.SCHOOL_POWER, spell.school().getId()));
    }

    public static double cooldownFactor(PlayerMagic m, Spell spell) {
        return Math.max(0.3, 1.0 - sum(m, Kind.SPELL_COOLDOWN, spell.id()) - sum(m, Kind.SCHOOL_COOLDOWN, spell.school().getId()));
    }

    public static double costFactor(PlayerMagic m, Spell spell) {
        return Math.max(0.4, 1.0 - sum(m, Kind.SPELL_COST, spell.id()) - sum(m, Kind.SCHOOL_COST, spell.school().getId()));
    }

    public static int maxManaBonus(PlayerMagic m) { return (int) sum(m, Kind.MAX_MANA, ""); }

    public static int regenBonus(PlayerMagic m) { return (int) sum(m, Kind.MANA_REGEN, ""); }

    public static double corruptionFactor(PlayerMagic m) { return Math.max(0.3, 1.0 - sum(m, Kind.CORRUPTION_RESIST, "")); }

    public static double fizzleFactor(PlayerMagic m) { return Math.max(0.2, 1.0 - sum(m, Kind.FIZZLE_RESIST, "")); }
}
