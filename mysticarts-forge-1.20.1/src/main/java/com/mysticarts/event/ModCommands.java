package com.mysticarts.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mysticarts.MysticArts;
import com.mysticarts.magic.MagicSchool;
import com.mysticarts.magic.MagicSync;
import com.mysticarts.magic.PlayerMagic;
import com.mysticarts.magic.capability.MagicCapability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.function.BiConsumer;

/** /mysticarts school|mana|corruption|level  (operators only, handy for testing). */
@Mod.EventBusSubscriber(modid = MysticArts.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mysticarts")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("school")
                        .then(Commands.argument("school", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(MagicSchool.values()).map(MagicSchool::getId), b))
                                .executes(ModCommands::setSchool)))
                .then(intCommand("mana", 0, 100000, PlayerMagic::setMana))
                .then(intCommand("corruption", 0, 100, PlayerMagic::setCorruption))
                .then(intCommand("level", 0, PlayerMagic.MAX_LEVEL, PlayerMagic::setLevel)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> intCommand(
            String name, int min, int max, BiConsumer<PlayerMagic, Integer> setter) {
        return Commands.literal(name).then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    PlayerMagic m = MagicCapability.of(p);
                    setter.accept(m, IntegerArgumentType.getInteger(ctx, "value"));
                    MagicSync.sync(p);
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.mysticarts.command_ok"), false);
                    return 1;
                }));
    }

    private static int setSchool(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        MagicSchool school = MagicSchool.fromName(StringArgumentType.getString(ctx, "school"));
        if (school == null) {
            ctx.getSource().sendFailure(Component.translatable("message.mysticarts.bad_school"));
            return 0;
        }
        PlayerMagic m = MagicCapability.of(p);
        m.setSchool(school);
        m.setMana(m.getMaxMana());
        MagicSync.sync(p);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.mysticarts.command_ok"), false);
        return 1;
    }
}
