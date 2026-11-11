package com.mysticarts.client;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.magic.spell.Spell;
import com.mysticarts.magic.spell.SpellCaster;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Mana bar, corruption bar and selected spell, drawn bottom-left. */
public class MagicHud {
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        MagicCapability.get(mc.player).ifPresent(m -> {
            if (m.getSchool() == MagicSchool.NONE) return;

            int x = 10;
            int y = height - 58;
            int barW = 96;

            // header: school + level
            g.drawString(mc.font, Component.translatable("hud.mysticarts.level",
                    m.getSchool().getDisplayName(), m.getLevel()), x, y - 12, 0xFFFFFFFF, true);

            // mana bar
            int maxMana = Math.max(1, m.getMaxMana());
            int fill = (int) (barW * (m.getMana() / (float) maxMana));
            g.fill(x - 1, y - 1, x + barW + 1, y + 7, 0xAA000000);
            g.fill(x, y, x + fill, y + 6, 0xFF2E7BFF);
            g.drawString(mc.font, m.getMana() + "/" + m.getMaxMana(), x + barW + 5, y - 1, 0xFF9FC4FF, true);

            // corruption bar
            if (m.getCorruption() > 0) {
                int cf = (int) (barW * (m.getCorruption() / 100.0f));
                g.fill(x - 1, y + 9, x + barW + 1, y + 15, 0xAA000000);
                g.fill(x, y + 10, x + cf, y + 14, 0xFFB000D0);
            }

            // selected spell
            Spell spell = SpellCaster.selected(m);
            if (spell != null) {
                boolean locked = m.getLevel() < spell.minLevel();
                Component line = Component.translatable("hud.mysticarts.spell", spell.displayName(), spell.manaCost());
                g.drawString(mc.font, line, x, y + 18, locked ? 0xFF777777 : m.getSchool().getColor() | 0xFF000000, true);
            }
        });
    };
}
