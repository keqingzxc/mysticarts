package com.mysticarts.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public class ModKeys {
    public static final String CATEGORY = "key.categories.mysticarts";

    public static final KeyMapping CAST = new KeyMapping("key.mysticarts.cast", InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping NEXT = new KeyMapping("key.mysticarts.next_spell", InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping PREV = new KeyMapping("key.mysticarts.prev_spell", InputConstants.KEY_C, CATEGORY);
}
