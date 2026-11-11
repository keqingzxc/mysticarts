package com.mysticarts.magic.spell;

import com.mysticarts.magic.MagicSchool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of all 16 spells (4 per school). Order here is the order in the spell wheel. */
public final class Spells {
    private static final Map<String, Spell> BY_ID = new LinkedHashMap<>();

    private static Spell reg(Spell s) {
        BY_ID.put(s.id(), s);
        return s;
    }

    // ---- Mystic Arts (Doctor Strange) ----
    public static final Spell MANDALA = reg(new Spell("mandala", MagicSchool.STRANGE, 20, 60, 0, 0, SpellEffects::mandala));
    public static final Spell PORTAL_STEP = reg(new Spell("portal_step", MagicSchool.STRANGE, 25, 80, 1, 0, SpellEffects::portalStep));
    public static final Spell CRIMSON_BANDS = reg(new Spell("crimson_bands", MagicSchool.STRANGE, 30, 200, 3, 0, SpellEffects::crimsonBands));
    public static final Spell MIRROR_DIMENSION = reg(new Spell("mirror_dimension", MagicSchool.STRANGE, 60, 400, 5, 0, SpellEffects::mirrorDimension));

    // ---- Chaos Magic (Scarlet Witch) ----
    public static final Spell SCARLET_PULSE = reg(new Spell("scarlet_pulse", MagicSchool.CHAOS, 25, 60, 0, 3, SpellEffects::scarletPulse));
    public static final Spell CHAOS_BOLT = reg(new Spell("chaos_bolt", MagicSchool.CHAOS, 30, 40, 1, 4, SpellEffects::chaosBolt));
    public static final Spell TELEKINESIS = reg(new Spell("telekinesis", MagicSchool.CHAOS, 20, 60, 3, 3, SpellEffects::telekinesis));
    public static final Spell REALITY_WARP = reg(new Spell("reality_warp", MagicSchool.CHAOS, 50, 200, 5, 8, SpellEffects::realityWarp));

    // ---- Witchcraft (Agatha Harkness) ----
    public static final Spell LIFE_DRAIN = reg(new Spell("life_drain", MagicSchool.WITCHCRAFT, 15, 40, 0, 0, SpellEffects::lifeDrain));
    public static final Spell HEX_CURSE = reg(new Spell("hex_curse", MagicSchool.WITCHCRAFT, 30, 160, 1, 0, SpellEffects::hexCurse));
    public static final Spell COVEN_CIRCLE = reg(new Spell("coven_circle", MagicSchool.WITCHCRAFT, 40, 300, 3, 0, SpellEffects::covenCircle));
    public static final Spell FAMILIAR = reg(new Spell("familiar", MagicSchool.WITCHCRAFT, 45, 400, 4, 0, SpellEffects::familiar));

    // ---- Wiccan (Billy Kaplan) ----
    public static final Spell HEX_BOLT = reg(new Spell("hex_bolt", MagicSchool.WICCAN, 15, 20, 0, 0, SpellEffects::hexBolt));
    public static final Spell WICCAN_DASH = reg(new Spell("wiccan_dash", MagicSchool.WICCAN, 20, 40, 1, 0, SpellEffects::wiccanDash));
    public static final Spell TELEPATHY = reg(new Spell("telepathy", MagicSchool.WICCAN, 25, 200, 2, 0, SpellEffects::telepathy));
    public static final Spell SOUL_LINK = reg(new Spell("soul_link", MagicSchool.WICCAN, 35, 300, 3, 0, SpellEffects::soulLink));

    private Spells() {}

    public static Collection<Spell> all() { return BY_ID.values(); }

    public static List<Spell> forSchool(MagicSchool school) {
        return BY_ID.values().stream().filter(s -> s.school() == school).toList();
    }
}
