package com.mysticarts.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** Per-player magic data: school, mana, level, corruption, bound origin (Mirror Dimension). */
public class PlayerMagic {
    public static final int MAX_LEVEL = 20;

    private MagicSchool school = MagicSchool.NONE;
    private int mana;
    private int level;
    private int xp;
    private int corruption;
    private int selectedSpell;
    private long altarReadyAt;

    private boolean hasOrigin;
    private String originDim = "minecraft:overworld";
    private double originX, originY, originZ;

    // transient (not saved)
    private boolean cloakFlight;
    private boolean dirty = true;
    private final Map<String, Long> cooldowns = new HashMap<>();

    // ---- school / progression ----
    public MagicSchool getSchool() { return school; }
    public void setSchool(MagicSchool s) { this.school = s; this.selectedSpell = 0; dirty = true; }

    public int getLevel() { return level; }
    public void setLevel(int l) { this.level = Mth.clamp(l, 0, MAX_LEVEL); this.xp = 0; dirty = true; }
    public int getXp() { return xp; }
    public int xpForNext() { return 40 + level * 25; }

    /** @return true if a level was gained */
    public boolean addXp(int amount) {
        if (level >= MAX_LEVEL) return false;
        xp += amount;
        boolean up = false;
        while (level < MAX_LEVEL && xp >= xpForNext()) {
            xp -= xpForNext();
            level++;
            up = true;
        }
        dirty = true;
        return up;
    }

    public void resetProgress() {
        level = 0;
        xp = 0;
        corruption = 0;
        selectedSpell = 0;
        dirty = true;
    }

    // ---- mana ----
    public int getMaxMana() { return school == MagicSchool.NONE ? 0 : 100 + 15 * level; }
    public int getMana() { return mana; }
    public void setMana(int v) { mana = Mth.clamp(v, 0, getMaxMana()); dirty = true; }
    public void addMana(int v) { setMana(mana + v); }
    public boolean consumeMana(int v) {
        if (mana < v) return false;
        setMana(mana - v);
        return true;
    }

    // ---- corruption ----
    public int getCorruption() { return corruption; }
    public void setCorruption(int v) { corruption = Mth.clamp(v, 0, 100); dirty = true; }
    public void addCorruption(int v) { setCorruption(corruption + v); }

    // ---- spell selection ----
    public int getSelectedSpell() { return selectedSpell; }
    public void setSelectedSpell(int i) { selectedSpell = Math.max(0, i); dirty = true; }

    // ---- altar ----
    public long getAltarReadyAt() { return altarReadyAt; }
    public void setAltarReadyAt(long t) { altarReadyAt = t; dirty = true; }

    // ---- origin (Mirror Dimension return point) ----
    public boolean hasOrigin() { return hasOrigin; }
    public String getOriginDim() { return originDim; }
    public double getOriginX() { return originX; }
    public double getOriginY() { return originY; }
    public double getOriginZ() { return originZ; }
    public void setOrigin(String dim, double x, double y, double z) {
        hasOrigin = true; originDim = dim; originX = x; originY = y; originZ = z; dirty = true;
    }
    public void clearOrigin() { hasOrigin = false; dirty = true; }

    // ---- transient ----
    public boolean isCloakFlight() { return cloakFlight; }
    public void setCloakFlight(boolean b) { cloakFlight = b; }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    public boolean isOnCooldown(String spellId, long now) {
        Long until = cooldowns.get(spellId);
        return until != null && until > now;
    }
    public long cooldownLeft(String spellId, long now) {
        Long until = cooldowns.get(spellId);
        return until == null ? 0 : Math.max(0, until - now);
    }
    public void setCooldown(String spellId, long until) { cooldowns.put(spellId, until); }

    // ---- copy / persistence ----
    public void copyFrom(PlayerMagic o) {
        school = o.school; mana = o.mana; level = o.level; xp = o.xp; corruption = o.corruption;
        selectedSpell = o.selectedSpell; altarReadyAt = o.altarReadyAt;
        hasOrigin = o.hasOrigin; originDim = o.originDim; originX = o.originX; originY = o.originY; originZ = o.originZ;
        dirty = true;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("School", school.getId());
        t.putInt("Mana", mana);
        t.putInt("Level", level);
        t.putInt("Xp", xp);
        t.putInt("Corruption", corruption);
        t.putInt("Selected", selectedSpell);
        t.putLong("AltarReadyAt", altarReadyAt);
        t.putBoolean("HasOrigin", hasOrigin);
        t.putString("OriginDim", originDim);
        t.putDouble("OriginX", originX);
        t.putDouble("OriginY", originY);
        t.putDouble("OriginZ", originZ);
        return t;
    }

    public void load(CompoundTag t) {
        MagicSchool s = MagicSchool.fromName(t.getString("School"));
        school = s == null ? MagicSchool.NONE : s;
        level = Mth.clamp(t.getInt("Level"), 0, MAX_LEVEL);
        xp = t.getInt("Xp");
        corruption = Mth.clamp(t.getInt("Corruption"), 0, 100);
        selectedSpell = Math.max(0, t.getInt("Selected"));
        altarReadyAt = t.getLong("AltarReadyAt");
        hasOrigin = t.getBoolean("HasOrigin");
        originDim = t.contains("OriginDim") ? t.getString("OriginDim") : "minecraft:overworld";
        originX = t.getDouble("OriginX");
        originY = t.getDouble("OriginY");
        originZ = t.getDouble("OriginZ");
        mana = Mth.clamp(t.getInt("Mana"), 0, getMaxMana());
        dirty = true;
    }
}
