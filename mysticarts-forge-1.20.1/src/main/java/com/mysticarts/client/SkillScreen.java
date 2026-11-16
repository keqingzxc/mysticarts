package com.mysticarts.client;

import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import com.mysticarts.magic.skill.SkillManager;
import com.mysticarts.magic.skill.Skills;
import com.mysticarts.magic.skill.Skills.Skill;
import com.mysticarts.network.ModNetwork;
import com.mysticarts.network.SkillActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The skill tree window (key K): school nodes in four tiers, shared nodes at the bottom. */
public class SkillScreen extends Screen {
    private static final int W = 300, H = 270, NODE = 24;
    private static final int RESET_W = 112, RESET_H = 16;

    private int left, top;

    public SkillScreen() {
        super(Component.translatable("skill.mysticarts.screen.title"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PlayerMagic m = MagicCapability.get(mc.player).orElse(null);
        if (m == null || m.getSchool() == MagicSchool.NONE) {
            mc.player.displayClientMessage(Component.translatable("message.mysticarts.no_school"), true);
            return;
        }
        mc.setScreen(new SkillScreen());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private PlayerMagic magic() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : MagicCapability.get(mc.player).orElse(null);
    }

    private int nodeX(Skill s) { return left + 60 + s.col() * 78; }

    private int nodeY(Skill s) { return s.tier() == 0 ? top + 216 : top + 46 + (s.tier() - 1) * 40; }

    private boolean over(Skill s, double mx, double my) {
        int x = nodeX(s), y = nodeY(s);
        return mx >= x && mx < x + NODE && my >= y && my < y + NODE;
    }

    private boolean overReset(double mx, double my) {
        int x = left + W - RESET_W - 12, y = top + H - RESET_H - 8;
        return mx >= x && mx < x + RESET_W && my >= y && my < y + RESET_H;
    }

    private static boolean locked(Skill s, PlayerMagic m) {
        return m.getLevel() < s.minLevel() || (s.requires() != null && m.getRank(s.requires()) < 1);
    }

    private static boolean buyable(Skill s, PlayerMagic m) {
        return m.getRank(s.id()) < s.maxRank() && !locked(s, m) && m.getSkillPoints() >= s.cost();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        PlayerMagic m = magic();
        if (m == null) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        int accent = m.getSchool().getColor() | 0xFF000000;
        g.fill(left, top, left + W, top + H, 0xF0121018);
        g.renderOutline(left, top, W, H, accent);
        g.drawCenteredString(font, title, left + W / 2, top + 8, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("skill.mysticarts.screen.points", m.getSkillPoints()),
                left + W / 2, top + 22, 0xFFFFD34D);
        g.drawCenteredString(font, m.getSchool().getDisplayName(), left + W / 2, top + 33, accent);

        List<Skill> school = Skills.forSchool(m.getSchool());
        List<Skill> shared = Skills.shared();

        for (Skill s : school) {
            if (s.requires() == null) continue;
            Skill r = Skills.get(s.requires());
            if (r != null) drawLink(g, r, s, m.getRank(r.id()) >= 1 ? 0xFFB89A3C : 0xFF3A3A48);
        }

        Skill hovered = null;
        for (Skill s : school) {
            drawNode(g, s, m);
            if (over(s, mouseX, mouseY)) hovered = s;
        }
        g.drawString(font, Component.translatable("skill.mysticarts.screen.shared"), left + 12, top + 203, 0xFF9A9AA8, false);
        for (Skill s : shared) {
            drawNode(g, s, m);
            if (over(s, mouseX, mouseY)) hovered = s;
        }

        // reset button
        int rx = left + W - RESET_W - 12, ry = top + H - RESET_H - 8;
        boolean rh = overReset(mouseX, mouseY);
        g.fill(rx, ry, rx + RESET_W, ry + RESET_H, rh ? 0xFF5A2430 : 0xFF2A1A22);
        g.renderOutline(rx, ry, RESET_W, RESET_H, 0xFF8A4A58);
        g.drawCenteredString(font, Component.translatable("skill.mysticarts.screen.reset", SkillManager.RESET_COST_LEVELS),
                rx + RESET_W / 2, ry + 4, 0xFFFFC0C8);

        super.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderComponentTooltip(font, tooltip(hovered, m), mouseX, mouseY);
        } else if (rh) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("skill.mysticarts.screen.reset_tip", SkillManager.RESET_COST_LEVELS)
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private void drawLink(GuiGraphics g, Skill from, Skill to, int color) {
        int x1 = nodeX(from) + NODE / 2, y1 = nodeY(from) + NODE;
        int x2 = nodeX(to) + NODE / 2, y2 = nodeY(to);
        int midY = (y1 + y2) / 2;
        g.fill(x1 - 1, y1, x1 + 1, midY + 1, color);
        g.fill(Math.min(x1, x2) - 1, midY - 1, Math.max(x1, x2) + 1, midY + 1, color);
        g.fill(x2 - 1, midY, x2 + 1, y2, color);
    }

    private void drawNode(GuiGraphics g, Skill s, PlayerMagic m) {
        int x = nodeX(s), y = nodeY(s);
        int rank = m.getRank(s.id());
        int border;
        if (rank >= s.maxRank()) border = 0xFFFFD34D;          // maxed
        else if (buyable(s, m)) border = 0xFF55FF55;           // can be bought now
        else if (rank > 0) border = 0xFF55D8FF;                // owned, not affordable right now
        else border = 0xFF6A6A6A;                              // locked
        g.fill(x, y, x + NODE, y + NODE, locked(s, m) ? 0xFF14141C : 0xFF222230);
        g.renderOutline(x, y, NODE, NODE, border);
        g.renderItem(new ItemStack(s.icon().get()), x + 4, y + 4);
        String txt = rank + "/" + s.maxRank();
        g.drawString(font, txt, x + NODE - font.width(txt), y + NODE - 8, border, true);
    }

    private List<Component> tooltip(Skill s, PlayerMagic m) {
        int rank = m.getRank(s.id());
        List<Component> tip = new ArrayList<>();
        tip.add(s.name().copy().withStyle(ChatFormatting.BOLD));
        tip.add(s.description().copy().withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("skill.mysticarts.screen.rank", rank, s.maxRank()).withStyle(ChatFormatting.AQUA));
        if (rank < s.maxRank()) {
            tip.add(Component.translatable("skill.mysticarts.screen.cost", s.cost()).withStyle(ChatFormatting.GOLD));
        }
        if (m.getLevel() < s.minLevel()) {
            tip.add(Component.translatable("message.mysticarts.low_level", s.minLevel()).withStyle(ChatFormatting.RED));
        }
        if (s.requires() != null && m.getRank(s.requires()) < 1) {
            Skill r = Skills.get(s.requires());
            if (r != null) {
                tip.add(Component.translatable("skill.mysticarts.screen.requires", r.name()).withStyle(ChatFormatting.RED));
            }
        }
        return tip;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            PlayerMagic m = magic();
            if (m != null) {
                for (Skill s : Skills.forSchool(m.getSchool())) {
                    if (over(s, mx, my)) { ModNetwork.sendToServer(new SkillActionPacket(s.id())); return true; }
                }
                for (Skill s : Skills.shared()) {
                    if (over(s, mx, my)) { ModNetwork.sendToServer(new SkillActionPacket(s.id())); return true; }
                }
                if (overReset(mx, my)) {
                    ModNetwork.sendToServer(new SkillActionPacket(SkillManager.RESET));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (ModKeys.SKILLS.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
