package com.mysticarts.item;

import com.mysticarts.magic.MagicSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A casting focus: held in either hand it boosts spells of its own school. */
public class FocusItem extends Item {
    public static final float POWER_BONUS = 1.25f;
    public static final float COST_FACTOR = 0.85f;

    private final MagicSchool school;

    public FocusItem(MagicSchool school, Properties properties) {
        super(properties);
        this.school = school;
    }

    public MagicSchool getSchool() { return school; }

    public static boolean isHolding(ServerPlayer p, MagicSchool school) {
        return matches(p.getMainHandItem(), school) || matches(p.getOffhandItem(), school);
    }

    private static boolean matches(ItemStack stack, MagicSchool school) {
        return stack.getItem() instanceof FocusItem f && f.school == school;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mysticarts.focus", school.getDisplayName())
                .withStyle(s -> s.withColor(0xAAAAAA)));
    }
}
