package com.mysticarts.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public enum MagicSchool {
    NONE("none", 0xAAAAAA),
    STRANGE("strange", 0xFFAA00),
    CHAOS("chaos", 0xFF2030),
    WITCHCRAFT("witchcraft", 0xB04CE8),
    WICCAN("wiccan", 0xFFE04A);

    private final String id;
    private final int color;

    MagicSchool(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() { return id; }
    public int getColor() { return color; }

    public MutableComponent getDisplayName() {
        return Component.translatable("school.mysticarts." + id)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    public static MagicSchool fromName(String name) {
        for (MagicSchool s : values()) {
            if (s.id.equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
